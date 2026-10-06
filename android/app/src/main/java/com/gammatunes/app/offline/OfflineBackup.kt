package com.gammatunes.app.offline

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.gammatunes.app.model.Track
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupKind { EXPORT, IMPORT }

sealed interface BackupState {
    data object Idle : BackupState
    data class Working(val kind: BackupKind, val done: Int, val total: Int) : BackupState
    data class Finished(val kind: BackupKind, val tracks: Int, val skipped: Int) : BackupState
    data class Failed(val kind: BackupKind, val invalidFile: Boolean, val message: String?) : BackupState
    data object Cancelled : BackupState
}

/**
 * Экспорт и импорт кеша (скачанных треков и альбомов) одним zip-файлом.
 *
 * Структура архива:
 *  - `manifest.json` — всегда первая запись: версия формата, треки (с путём к аудио внутри архива) и альбомы;
 *  - `audio/<n>.<ext>` — аудиофайлы (уже сжаты, поэтому в zip кладутся без пересжатия).
 *
 * Работа идёт в фоне на уровне приложения; прогресс виден через [state]. Обложки в архив не входят —
 * они подгружаются из интернета, как и раньше.
 */
object OfflineBackup {
    private const val TAG = "OfflineBackup"
    private const val FORMAT = "gammatunes-cache"
    private const val VERSION = 1
    private const val MANIFEST = "manifest.json"
    private const val MAX_MANIFEST_BYTES = 32L * 1024 * 1024
    private const val MAX_AUDIO_BYTES = 256L * 1024 * 1024
    private const val MIN_AUDIO_BYTES = 8L * 1024

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private data class ManifestTrack(val track: Track, val file: String, val mimeType: String)
    private data class Manifest(
        val format: String,
        val version: Int,
        val tracks: List<ManifestTrack>,
        val albums: List<OfflineAlbum>,
    )

    // «Сырая» версия для чтения: Gson не проверяет null у Kotlin-типов, поэтому валидируем вручную
    private data class RawTrack(val track: Track?, val file: String?, val mimeType: String?)
    private data class RawManifest(
        val format: String?,
        val version: Int?,
        val tracks: List<RawTrack>?,
        val albums: List<OfflineAlbum>?,
    )

    private class InvalidBackupException : IOException("Not a GammaTunes cache file")

    val isBusy: Boolean get() = _state.value is BackupState.Working

    /** Убирает сообщение о результате (идущую операцию не трогает). */
    fun dismiss() {
        if (!isBusy) _state.value = BackupState.Idle
    }

    fun cancel() {
        job?.cancel()
    }

    // ---- Экспорт ---------------------------------------------------------------------

    fun export(context: Context, uri: Uri) {
        if (isBusy) return
        val app = context.applicationContext
        val resolver = app.contentResolver
        _state.value = BackupState.Working(BackupKind.EXPORT, 0, 0)
        job = scope.launch {
            try {
                val tracks = OfflineRepository.index.value.values
                    .filter { File(it.filePath).let { f -> f.exists() && f.length() >= MIN_AUDIO_BYTES } }
                val ids = tracks.map { it.track.videoId }.toSet()
                val albums = OfflineRepository.albums.value.values
                    .filter { a -> a.trackIds.isNotEmpty() && a.trackIds.all { it in ids } }

                val manifestTracks = tracks.mapIndexed { i, t ->
                    val ext = File(t.filePath).extension.ifBlank { "m4a" }
                    ManifestTrack(t.track, "audio/$i.$ext", t.mimeType)
                }
                val manifest = Manifest(FORMAT, VERSION, manifestTracks, albums)

                _state.value = BackupState.Working(BackupKind.EXPORT, 0, tracks.size)
                val out = resolver.openOutputStream(uri, "w") ?: throw IOException("Can't open the file for writing")
                ZipOutputStream(BufferedOutputStream(out, 64 * 1024)).use { zip ->
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    zip.putNextEntry(ZipEntry(MANIFEST))
                    zip.write(gson.toJson(manifest).toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    tracks.forEachIndexed { i, offline ->
                        ensureActive()
                        zip.putNextEntry(ZipEntry(manifestTracks[i].file))
                        File(offline.filePath).inputStream().use { it.copyTo(zip, 64 * 1024) }
                        zip.closeEntry()
                        _state.value = BackupState.Working(BackupKind.EXPORT, i + 1, tracks.size)
                    }
                }
                _state.value = BackupState.Finished(BackupKind.EXPORT, tracks.size, 0)
            } catch (e: CancellationException) {
                deleteDocument(resolver, uri)
                _state.value = BackupState.Cancelled
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "Экспорт кеша не удался", t)
                deleteDocument(resolver, uri)
                _state.value = BackupState.Failed(BackupKind.EXPORT, false, t.message)
            }
        }
    }

    // ---- Импорт ----------------------------------------------------------------------

    fun import(context: Context, uri: Uri) {
        if (isBusy) return
        val app = context.applicationContext
        _state.value = BackupState.Working(BackupKind.IMPORT, 0, 0)
        job = scope.launch {
            // Файлы пишем сразу, а в индекс заносим только в конце: при отмене/ошибке просто удаляем их
            val written = mutableListOf<File>()
            try {
                val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Can't open the file")
                val imported = mutableListOf<OfflineTrack>()
                var manifest: Manifest? = null

                ZipInputStream(BufferedInputStream(input, 64 * 1024)).use { zin ->
                    val first = zin.nextEntry
                    if (first == null || first.name != MANIFEST) throw InvalidBackupException()
                    manifest = parseManifest(zin)
                    val m = manifest!!
                    val byFile = m.tracks.associateBy { it.file }
                    val dir = OfflineRepository.offlineDirectory()
                    var processed = 0
                    _state.value = BackupState.Working(BackupKind.IMPORT, 0, m.tracks.size)

                    while (true) {
                        ensureActive()
                        val entry = zin.nextEntry ?: break
                        val item = byFile[entry.name]
                        if (entry.isDirectory || item == null) continue
                        val id = item.track.videoId
                        if (id.isNotBlank() && !OfflineRepository.hasValidFile(id)) {
                            val dest = copyAudio(zin, dir, id, entry.name)
                            if (dest != null) {
                                written += dest
                                imported += OfflineTrack(item.track, dest.absolutePath, item.mimeType.ifBlank { "audio/mp4" })
                            }
                        }
                        processed++
                        _state.value = BackupState.Working(BackupKind.IMPORT, processed, m.tracks.size)
                    }
                }

                val m = manifest ?: throw InvalidBackupException()
                OfflineRepository.mergeImported(imported, m.albums)
                _state.value = BackupState.Finished(BackupKind.IMPORT, imported.size, m.tracks.size - imported.size)
            } catch (e: CancellationException) {
                written.forEach { runCatching { it.delete() } }
                _state.value = BackupState.Cancelled
                throw e
            } catch (e: InvalidBackupException) {
                written.forEach { runCatching { it.delete() } }
                _state.value = BackupState.Failed(BackupKind.IMPORT, true, null)
            } catch (t: Throwable) {
                Log.e(TAG, "Импорт кеша не удался", t)
                written.forEach { runCatching { it.delete() } }
                _state.value = BackupState.Failed(BackupKind.IMPORT, false, t.message)
            }
        }
    }

    // ---- Вспомогательное -------------------------------------------------------------

    private fun parseManifest(zin: ZipInputStream): Manifest {
        val bytes = readLimited(zin, MAX_MANIFEST_BYTES)
        val raw = runCatching { gson.fromJson(String(bytes, Charsets.UTF_8), RawManifest::class.java) }
            .getOrNull() ?: throw InvalidBackupException()
        val version = raw.version ?: throw InvalidBackupException()
        if (raw.format != FORMAT || version > VERSION) throw InvalidBackupException()
        val tracks = (raw.tracks ?: throw InvalidBackupException()).mapNotNull { t ->
            val track = t.track ?: return@mapNotNull null
            val file = t.file ?: return@mapNotNull null
            ManifestTrack(track, file, t.mimeType.orEmpty())
        }
        return Manifest(FORMAT, version, tracks, raw.albums.orEmpty())
    }

    private fun readLimited(input: InputStream, max: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > max) throw InvalidBackupException()
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /**
     * Копирует текущую запись архива в папку кеша. Имя файла строим сами из videoId, а не берём
     * из архива, поэтому выйти за пределы папки (zip-slip) нельзя. Возвращает null, если файл слишком мал.
     */
    private fun copyAudio(zin: ZipInputStream, dir: File, videoId: String, entryName: String): File? {
        val safeId = videoId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val ext = entryName.substringAfterLast('.', "m4a")
            .filter { it.isLetterOrDigit() }.take(5).ifEmpty { "m4a" }
        val dest = File(dir, "$safeId.$ext")
        val tmp = File(dir, "$safeId.$ext.part")
        try {
            var total = 0L
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = zin.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_AUDIO_BYTES) throw IOException("File too large")
                    out.write(buf, 0, n)
                }
            }
            if (total < MIN_AUDIO_BYTES) {
                tmp.delete()
                return null
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            return dest
        } catch (t: Throwable) {
            runCatching { tmp.delete() }
            throw t
        }
    }

    private fun deleteDocument(resolver: android.content.ContentResolver, uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }
    }
}
