package com.gammatunes.app.offline

import android.content.Context
import android.util.Log
import com.gammatunes.app.model.Track
import com.gammatunes.app.network.ApiClient
import com.gammatunes.app.network.backendMessage
import com.gammatunes.app.backend.LocalBackend
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.gammatunes.app.network.NetworkMonitor
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class OfflineTrack(
    val track: Track,
    val filePath: String,
    val mimeType: String,
)

data class OfflineAlbum(
    val albumId: String,
    val title: String,
    val thumbnail: String? = null,
    val year: String? = null,
    val trackIds: List<String> = emptyList(),
)

/** Ошибка, при которой повтор бессмысленен (например, файл слишком большой). */
class PermanentDownloadException(message: String) : java.io.IOException(message)

object OfflineRepository {
    private const val TAG = "OfflineRepository"

    /** Повторять до успеха. */
    const val UNLIMITED_ATTEMPTS = Int.MAX_VALUE
    private const val ATTEMPT_TIMEOUT_MS = 180_000L
    private const val RETRY_BASE_DELAY_MS = 2_000L
    private const val RETRY_MAX_DELAY_MS = 20_000L
    private const val NETWORK_WAIT_MS = 30_000L

    private val trackJobs = ConcurrentHashMap<String, Job>()
    private val albumJobs = ConcurrentHashMap<String, Job>()


    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val INDEX_FILE = "offline_index.json"
    private const val ALBUMS_FILE = "offline_albums.json"
    private const val MAX_DOWNLOAD_BYTES = 80L * 1024L * 1024L

    private val gson = Gson()
    private lateinit var appContext: Context

    private val httpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.MINUTES)
        .build()

    private val _index = MutableStateFlow<Map<String, OfflineTrack>>(emptyMap())
    val index: StateFlow<Map<String, OfflineTrack>> = _index

    private val _albums = MutableStateFlow<Map<String, OfflineAlbum>>(emptyMap())
    val albums: StateFlow<Map<String, OfflineAlbum>> = _albums

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds

    private val _downloadingAlbumIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingAlbumIds: StateFlow<Set<String>> = _downloadingAlbumIds

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors

    fun init(context: Context) {
        appContext = context.applicationContext
        loadIndexFromDisk()
        loadAlbumsFromDisk()
        pruneMissing()
    }


    fun pruneMissing() {
        val before = _index.value
        val kept = before.filterValues { offline ->
            val f = File(offline.filePath)
            f.exists() && f.length() >= 8 * 1024L
        }
        if (kept.size != before.size) {
            _index.value = kept
            persistIndexToDisk()
            Log.i(TAG, "pruneMissing: ${before.size - kept.size} broken entries removed")
        }
        val albumsBefore = _albums.value
        val albumsKept = albumsBefore.mapValues { (_, album) ->
            album.copy(trackIds = album.trackIds.filter { kept.containsKey(it) })
        }.filterValues { it.trackIds.isNotEmpty() }
        if (albumsKept.size != albumsBefore.size ||
            albumsKept.any { (id, a) -> albumsBefore[id]?.trackIds != a.trackIds }
        ) {
            _albums.value = albumsKept
            persistAlbumsToDisk()
        }
    }

    fun isDownloaded(videoId: String): Boolean = _index.value.containsKey(videoId)

    fun isAlbumDownloaded(albumId: String): Boolean = _albums.value.containsKey(albumId)

    fun localTrack(videoId: String): OfflineTrack? {
        val offline = _index.value[videoId] ?: return null
        val f = File(offline.filePath)
        if (!f.exists() || f.length() < 1024L) {

            _index.update { it - videoId }
            persistIndexToDisk()
            return null
        }
        return offline
    }

    fun albumTracksOrdered(albumId: String): List<Track> {
        val album = _albums.value[albumId] ?: return emptyList()
        return album.trackIds.mapNotNull { id -> _index.value[id]?.track }
    }

    private fun offlineDir(): File =
        File(appContext.filesDir, "offline").apply { if (!exists()) mkdirs() }

    /** Папка с файлами скачанных треков (нужна экспорту/импорту кеша). */
    fun offlineDirectory(): File = offlineDir()

    /** Трек есть в кеше и его файл на месте. */
    fun hasValidFile(videoId: String): Boolean {
        val offline = _index.value[videoId] ?: return false
        val f = File(offline.filePath)
        return f.exists() && f.length() >= 8 * 1024L
    }

    /**
     * Добавляет импортированные треки и альбомы к текущим. Существующие записи не перезаписываются.
     * Альбом добавляется только если в кеше есть все его треки (как и при обычном скачивании).
     */
    fun mergeImported(tracks: Collection<OfflineTrack>, albums: Collection<OfflineAlbum>) {
        if (tracks.isNotEmpty()) {
            _index.update { current ->
                current + tracks
                    .filter { !current.containsKey(it.track.videoId) }
                    .associateBy { it.track.videoId }
            }
            persistIndexToDisk()
        }
        val idx = _index.value
        val toAdd = albums
            .filter { a -> !_albums.value.containsKey(a.albumId) && a.trackIds.isNotEmpty() && a.trackIds.all { idx.containsKey(it) } }
            .associateBy { it.albumId }
        if (toAdd.isNotEmpty()) {
            _albums.update { it + toAdd }
            persistAlbumsToDisk()
        }
    }


    /**
     * Скачивает трек, повторяя попытки (с нарастающей паузой), пока не получится
     * или пока не исчерпан [maxAttempts]. Отмена — через [cancelDownload].
     */
    fun download(track: Track, maxAttempts: Int = UNLIMITED_ATTEMPTS) {
        val videoId = track.videoId
        if (isDownloaded(videoId) || _downloadingIds.value.contains(videoId)) return

        _errors.update { it - videoId }
        _downloadingIds.update { it + videoId }
        val job = appScope.launch(start = CoroutineStart.LAZY) {
            try {
                downloadWithRetry(track, maxAttempts)
                _errors.update { it - videoId }
            } catch (e: CancellationException) {
                Log.w(TAG, "download cancelled $videoId")
                cleanupPart(videoId)
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "Не удалось скачать трек $videoId", t)
                _errors.update { it + (videoId to (t.message ?: "Не удалось скачать")) }
                cleanupPart(videoId)
            } finally {
                _downloadingIds.update { it - videoId }
                trackJobs.remove(videoId)
            }
        }
        trackJobs[videoId] = job
        job.start()
    }

    fun cancelDownload(videoId: String) {
        trackJobs[videoId]?.cancel()
    }

    fun cancelAlbumDownload(albumId: String) {
        albumJobs[albumId]?.cancel()
    }

    /**
     * Одна попытка = получить stream + скачать файл (с таймаутом).
     * При любой сетевой/временной ошибке ждём и пробуем ещё раз; наружу выходим
     * только при успехе, отмене, [PermanentDownloadException] или исчерпании попыток.
     */
    private suspend fun downloadWithRetry(track: Track, maxAttempts: Int) {
        val videoId = track.videoId
        var attempt = 0
        while (true) {
            attempt++
            try {
                kotlinx.coroutines.withTimeout(ATTEMPT_TIMEOUT_MS) {
                    downloadTrackFile(track)
                }
                return
            } catch (e: TimeoutCancellationException) {
                // Таймаут попытки — это не отмена пользователем.
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Таймаут скачивания $videoId (попытка $attempt)")
                _errors.update { it + (videoId to "Таймаут скачивания") }
                cleanupPart(videoId)
                if (attempt >= maxAttempts) throw java.io.IOException("Таймаут скачивания", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PermanentDownloadException) {
                throw e
            } catch (t: Throwable) {
                Log.w(TAG, "Ошибка скачивания $videoId (попытка $attempt): ${t.message}")
                _errors.update { it + (videoId to (t.message ?: "Не удалось скачать")) }
                cleanupPart(videoId)
                if (attempt >= maxAttempts) throw t
            }

            // Если сети нет — дожидаемся её (но не бесконечно, вдруг монитор ошибся).
            if (!NetworkMonitor.isOnline.value) {
                withTimeoutOrNull(NETWORK_WAIT_MS) { NetworkMonitor.isOnline.first { it } }
            }
            val backoff = (RETRY_BASE_DELAY_MS shl (attempt - 1).coerceAtMost(4))
                .coerceAtMost(RETRY_MAX_DELAY_MS)
            delay(backoff)
        }
    }


    fun downloadAlbum(
        albumId: String,
        title: String,
        thumbnail: String?,
        year: String? = null,
        tracks: List<Track>,
    ) {
        if (tracks.isEmpty()) return
        if (_downloadingAlbumIds.value.contains(albumId)) return

        _downloadingAlbumIds.update { it + albumId }
        val snapshot = tracks.toList()
        val job = appScope.launch(start = CoroutineStart.LAZY) {
            try {
                val orderedIds = mutableListOf<String>()
                for (track in snapshot) {
                    ensureActive()

                    // Если этот трек уже качается отдельно — дождёмся, а не будем дублировать.
                    trackJobs[track.videoId]?.join()

                    if (isDownloaded(track.videoId)) {
                        orderedIds.add(track.videoId)
                        continue
                    }
                    _downloadingIds.update { it + track.videoId }
                    try {
                        // Повторяем, пока трек не скачается; к следующему — только после успеха.
                        downloadWithRetry(track, UNLIMITED_ATTEMPTS)
                        _errors.update { it - track.videoId }
                        if (isDownloaded(track.videoId)) {
                            orderedIds.add(track.videoId)
                        }
                    } catch (e: CancellationException) {
                        cleanupPart(track.videoId)
                        throw e
                    } catch (t: Throwable) {
                        // Сюда попадаем только при заведомо неисправимой ошибке.
                        Log.e(TAG, "Альбом $albumId: трек ${track.videoId} скачать невозможно", t)
                        _errors.update {
                            it + (track.videoId to (t.message ?: "Не удалось скачать"))
                        }
                        cleanupPart(track.videoId)
                    } finally {
                        _downloadingIds.update { it - track.videoId }
                    }
                }

                if (orderedIds.size < snapshot.size) {
                    Log.w(
                        TAG,
                        "Альбом $albumId неполный: ${orderedIds.size}/${snapshot.size} — не помечаем скачанным",
                    )
                    return@launch
                }

                val offlineAlbum = OfflineAlbum(
                    albumId = albumId,
                    title = title,
                    thumbnail = thumbnail,
                    year = year,
                    trackIds = orderedIds,
                )
                _albums.update { it + (albumId to offlineAlbum) }
                persistAlbumsToDisk()
                Log.i(TAG, "Альбом $albumId скачан полностью (${orderedIds.size} треков)")
            } finally {
                _downloadingAlbumIds.update { it - albumId }
                albumJobs.remove(albumId)
            }
        }
        albumJobs[albumId] = job
        job.start()
    }

    suspend fun deleteAlbum(albumId: String, deleteTrackFiles: Boolean = false) {
        withContext(Dispatchers.IO) {
            val album = _albums.value[albumId]
            _albums.update { it - albumId }
            persistAlbumsToDisk()
            if (deleteTrackFiles && album != null) {
                for (id in album.trackIds) {
                    val usedElsewhere = _albums.value.values.any { it.trackIds.contains(id) }
                    if (!usedElsewhere) {
                        _index.value[id]?.let { runCatching { File(it.filePath).delete() } }
                        _index.update { it - id }
                    }
                }
                persistIndexToDisk()
            }
        }
    }

    suspend fun delete(videoId: String) {
        withContext(Dispatchers.IO) {
            _index.value[videoId]?.let { offlineTrack ->
                runCatching { File(offlineTrack.filePath).delete() }
            }
            _index.update { it - videoId }
            persistIndexToDisk()
            var albumsChanged = false
            val updated = _albums.value.mapValues { (_, album) ->
                if (album.trackIds.contains(videoId)) {
                    albumsChanged = true
                    album.copy(trackIds = album.trackIds.filter { it != videoId })
                } else album
            }.filterValues { it.trackIds.isNotEmpty() }
            if (albumsChanged) {
                _albums.value = updated
                persistAlbumsToDisk()
            }
        }
    }


    private suspend fun downloadTrackFile(track: Track) {
        val videoId = track.videoId

        val stream = try {
            ApiClient.api.stream(videoId)
        } catch (t: Throwable) {
            throw java.io.IOException("Не удалось получить stream: ${t.backendMessage()}", t)
        }
        if (stream.streamUrl.isBlank()) {
            throw java.io.IOException("Пустой streamUrl")
        }

        val extension = extensionFor(stream.mimeType)
        val outFile = File(offlineDir(), "$videoId.$extension")
        val tmpFile = File(offlineDir(), "$videoId.$extension.part")
        if (tmpFile.exists()) tmpFile.delete()

        // HLS tracks are a playlist of segments, so a plain byte copy would save
        // the playlist text. Let the embedded backend (yt-dlp) assemble the file.
        val reqBuilder = if (stream.isHls) {
            Request.Builder()
                .url("${LocalBackend.BASE_URL}download/$videoId")
                .post(ByteArray(0).toRequestBody())
        } else {
            Request.Builder().url(stream.streamUrl)
        }
        var hasUa = false
        for ((k, v) in (if (stream.isHls) emptyMap() else stream.httpHeaders)) {
            try {
                reqBuilder.header(k, v)
                if (k.equals("User-Agent", ignoreCase = true)) hasUa = true
            } catch (_: Throwable) {
            }
        }
        if (!hasUa) {
            reqBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            )
        }
        reqBuilder.header("Accept-Encoding", "identity")

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val err = try { response.body?.string()?.take(200) } catch (_: Throwable) { null }
                throw java.io.IOException("HTTP ${response.code}${err?.let { ": $it" } ?: ""}")
            }
            val body = response.body ?: throw java.io.IOException("Пустой body")
            val reported = body.contentLength()
            if (reported > MAX_DOWNLOAD_BYTES) {
                throw PermanentDownloadException("Файл слишком большой ($reported)")
            }

            tmpFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_DOWNLOAD_BYTES) {
                            throw PermanentDownloadException("Превышен лимит размера")
                        }
                        output.write(buffer, 0, read)
                    }
                    if (total < 8 * 1024L) {
                        throw java.io.IOException("Скачано слишком мало: $total байт")
                    }
                    Log.i(TAG, "downloaded $videoId: $total bytes")
                }
            }
        }

        if (outFile.exists()) outFile.delete()
        if (!tmpFile.renameTo(outFile)) {
            tmpFile.copyTo(outFile, overwrite = true)
            tmpFile.delete()
        }
        if (!outFile.exists() || outFile.length() < 8 * 1024L) {
            throw java.io.IOException("Файл не записался (${outFile.length()})")
        }
        if (isClearlyNotAudio(outFile)) {
            outFile.delete()
            throw java.io.IOException("CDN отдал не аудио (HTML/JSON)")
        }

        val offlineTrack = OfflineTrack(
            track = track,
            filePath = outFile.absolutePath,
            mimeType = stream.mimeType.ifBlank { "audio/mp4" },
        )
        _index.update { it + (videoId to offlineTrack) }
        persistIndexToDisk()
        Log.i(TAG, "indexed $videoId size=${outFile.length()} path=${outFile.absolutePath}")
    }


    private fun isClearlyNotAudio(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val buf = ByteArray(64)
                val n = input.read(buf)
                if (n <= 0) return true
                val head = buf.decodeToString(0, n).trimStart()
                head.startsWith("<") || head.startsWith("{") || head.startsWith("<?xml")
            }
        } catch (_: Throwable) {
            true
        }
    }

    private fun cleanupPart(videoId: String) {
        runCatching {
            offlineDir().listFiles()?.forEach { f ->
                if (f.name.startsWith(videoId) && f.name.endsWith(".part")) {
                    f.delete()
                }
            }
        }
    }

    private fun extensionFor(mimeType: String): String = when {
        mimeType.contains("webm", ignoreCase = true) -> "webm"
        mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "m4a"
        mimeType.contains("mpeg", ignoreCase = true) || mimeType.contains("mp3", ignoreCase = true) -> "mp3"
        else -> "m4a"
    }

    private fun loadIndexFromDisk() {
        runCatching {
            val file = File(appContext.filesDir, INDEX_FILE)
            if (!file.exists()) return
            val json = file.readText()
            val type = object : TypeToken<Map<String, OfflineTrack>>() {}.type
            val loaded: Map<String, OfflineTrack> = gson.fromJson(json, type) ?: emptyMap()
            _index.value = loaded.filterValues { offline ->
                val f = File(offline.filePath)
                f.exists() && f.length() >= 1024L
            }
        }.onFailure { Log.e(TAG, "Не удалось прочитать offline_index.json", it) }
    }

    private fun persistIndexToDisk() {
        runCatching {
            File(appContext.filesDir, INDEX_FILE).writeText(gson.toJson(_index.value))
        }.onFailure { Log.e(TAG, "Не удалось сохранить offline_index.json", it) }
    }

    private fun loadAlbumsFromDisk() {
        runCatching {
            val file = File(appContext.filesDir, ALBUMS_FILE)
            if (!file.exists()) return
            val type = object : TypeToken<Map<String, OfflineAlbum>>() {}.type
            val loaded: Map<String, OfflineAlbum> = gson.fromJson(file.readText(), type) ?: emptyMap()
            _albums.value = loaded
        }.onFailure { Log.e(TAG, "Не удалось прочитать offline_albums.json", it) }
    }

    private fun persistAlbumsToDisk() {
        runCatching {
            File(appContext.filesDir, ALBUMS_FILE).writeText(gson.toJson(_albums.value))
        }.onFailure { Log.e(TAG, "Не удалось сохранить offline_albums.json", it) }
    }
}
