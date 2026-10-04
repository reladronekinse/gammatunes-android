package com.gammatunes.app.player

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gammatunes.app.model.Track
import com.gammatunes.app.network.ApiClient
import com.gammatunes.app.network.backendMessage
import com.gammatunes.app.offline.OfflineRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class RepeatMode { OFF, ALL, ONE }

/** A queue slot. [uid] is unique per slot so the same track can appear twice and rows keep identity while reordering. */
data class QueueEntry(val uid: Long, val track: Track)

/** Lets any track row reach the player (queue actions) without threading it through every screen. */
val LocalPlayerState = staticCompositionLocalOf<PlayerState?> { null }

@UnstableApi
class PlayerState(private val context: Context, private val scope: CoroutineScope) {

    private var boundPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    var currentTrack by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isLoadingStream by mutableStateOf(false)
        private set
    var streamError by mutableStateOf<String?>(null)
        private set
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private var nextQueueUid = 0L

    /** The full queue (played, current and upcoming tracks), observable from Compose. */
    var queueEntries by mutableStateOf<List<QueueEntry>>(emptyList())
        private set
    var queueIndex by mutableIntStateOf(-1)
        private set

    private val queue: List<Track> get() = queueEntries.map { it.track }

    /** "Play next": insert right after the current track (moves it if it is already queued). */
    fun enqueueNext(track: Track) {
        val idx = queueIndex
        if (currentTrack == null || idx !in queueEntries.indices) {
            play(track)
            return
        }
        val list = queueEntries.toMutableList()
        var current = idx
        val existing = list.indexOfFirst { it.track.videoId == track.videoId }
        if (existing >= 0 && existing != current) {
            list.removeAt(existing)
            if (existing < current) current--
        }
        list.add(current + 1, QueueEntry(nextQueueUid++, track))
        queueEntries = list
        queueIndex = current
    }

    fun moveQueueItem(from: Int, to: Int) {
        val list = queueEntries.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        queueEntries = list
        queueIndex = when {
            from == queueIndex -> to
            from < queueIndex && to >= queueIndex -> queueIndex - 1
            from > queueIndex && to <= queueIndex -> queueIndex + 1
            else -> queueIndex
        }
    }

    /** Removes an upcoming/played item; the currently playing slot can't be removed. */
    fun removeQueueItem(index: Int) {
        if (index !in queueEntries.indices || index == queueIndex) return
        queueEntries = queueEntries.toMutableList().also { it.removeAt(index) }
        if (index < queueIndex) queueIndex--
    }

    fun playQueueIndex(index: Int) {
        val entry = queueEntries.getOrNull(index) ?: return
        if (index == queueIndex) {
            togglePlayPause()
            return
        }
        queueIndex = index
        PlayHistoryRepository.record(entry.track)
        PlayStatsRepository.record(entry.track)
        loadAndPlay(entry.track)
    }

    /** In offline mode only downloaded tracks can be played, so the rest are skipped. */
    private fun isPlayable(entry: QueueEntry): Boolean =
        !com.gammatunes.app.offline.OfflineModeRepository.enabled.value ||
            OfflineRepository.isDownloaded(entry.track.videoId)

    private fun nextPlayableIndex(): Int =
        if (queueIndex < 0) -1 else (queueIndex + 1 until queueEntries.size).firstOrNull { isPlayable(queueEntries[it]) } ?: -1

    private fun previousPlayableIndex(): Int =
        if (queueIndex <= 0) -1 else (queueIndex - 1 downTo 0).firstOrNull { isPlayable(queueEntries[it]) } ?: -1

    val hasNext: Boolean
        get() = nextPlayableIndex() >= 0
    val hasPrevious: Boolean
        get() = previousPlayableIndex() >= 0


    val positionMs: Long
        get() = boundPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L


    val durationMs: Long
        get() {
            val d = boundPlayer?.duration ?: 0L
            return if (d > 0L) d else 0L
        }

    fun seekTo(positionMs: Long) {
        boundPlayer?.seekTo(positionMs.coerceAtLeast(0L))
    }

    init {
        PlayerBridge.bindControls(
            onNext = { playNext() },
            onPrevious = { playPrevious() },
            hasNext = { hasNext },
            hasPrevious = { hasPrevious },
        )
        connectMediaSession()
    }

    private fun connectMediaSession() {
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    PlayerBridge.player?.let { attachToPlayer(it) }
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun attachToPlayer(player: ExoPlayer) {
        if (boundPlayer === player) return
        playerListener?.let { boundPlayer?.removeListener(it) }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    currentTrack?.let { finished ->
                        if (!OfflineRepository.isDownloaded(finished.videoId)) {
                            OfflineRepository.download(finished)
                        }
                    }
                    when (repeatMode) {
                        RepeatMode.ONE -> {
                            player.seekTo(0)
                            player.play()
                        }
                        RepeatMode.ALL -> {
                            if (hasNext) {
                                playNext()
                            } else if (queue.isNotEmpty()) {
                                queueIndex = 0
                                val t = queue[queueIndex]
                                PlayHistoryRepository.record(t)
                                PlayStatsRepository.record(t)
                                loadAndPlay(t)
                            }
                        }
                        RepeatMode.OFF -> playNext()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val track = currentTrack
                if (track != null && !retryAttempted) {
                    val position = player.currentPosition
                    loadAndPlay(track, resumePositionMs = position, isRetry = true)
                } else {
                    streamError = error.message ?: "Ошибка воспроизведения"
                }
            }
        }
        player.addListener(listener)
        boundPlayer = player
        playerListener = listener
        isPlaying = player.isPlaying
    }

    fun play(track: Track, queue: List<Track> = listOf(track)) {
        this.queueEntries = queue.map { QueueEntry(nextQueueUid++, it) }
        this.queueIndex = queue.indexOfFirst { it.videoId == track.videoId }.let {
            if (it >= 0) it else 0
        }
        PlayHistoryRepository.record(track)
        PlayStatsRepository.record(track)
        if (track.videoId == currentTrack?.videoId) {
            togglePlayPause()
            return
        }
        loadAndPlay(track)
    }

    fun playNext() {
        val next = nextPlayableIndex()
        if (next < 0) return
        queueIndex = next
        val t = queue[queueIndex]
        PlayHistoryRepository.record(t)
        PlayStatsRepository.record(t)
        loadAndPlay(t)
    }

    fun playPrevious() {
        val previous = previousPlayableIndex()
        if (previous < 0) return
        queueIndex = previous
        val t = queue[queueIndex]
        PlayHistoryRepository.record(t)
        PlayStatsRepository.record(t)
        loadAndPlay(t)
    }

    private var retryAttempted = false

    private fun mediaItemFor(track: Track, uri: android.net.Uri): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.thumbnail?.let { android.net.Uri.parse(it) })
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(track.videoId)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun loadAndPlay(track: Track, resumePositionMs: Long = 0L, isRetry: Boolean = false) {
        retryAttempted = if (isRetry) {
            if (retryAttempted) return
            true
        } else {
            false
        }
        currentTrack = track
        streamError = null

        val offlineTrack = OfflineRepository.localTrack(track.videoId)
        if (offlineTrack != null) {
            isLoadingStream = false
            scope.launch {
                val player = awaitPlayer() ?: run {
                    streamError = "Плеер не готов"
                    return@launch
                }
                try {
                    val uri = android.net.Uri.fromFile(java.io.File(offlineTrack.filePath))
                    player.setMediaItem(mediaItemFor(track, uri), resumePositionMs)
                    player.prepare()
                    player.playWhenReady = true
                } catch (e: Exception) {
                    streamError = e.message ?: "Не удалось воспроизвести скачанный трек"
                }
            }
            return
        }

        isLoadingStream = true
        scope.launch {
            try {
                val quality = PlaybackSettingsRepository.settings.value.quality.apiValue
                val stream = ApiClient.api.stream(
                    track.videoId,
                    quality = quality,
                    video = if (track.isVideo) 1 else null,
                )
                val player = awaitPlayer() ?: run {
                    streamError = "Плеер не готов"
                    return@launch
                }
                val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                    setAllowCrossProtocolRedirects(true)
                    setConnectTimeoutMs(20_000)
                    setReadTimeoutMs(20_000)
                }
                val item = mediaItemFor(track, android.net.Uri.parse(stream.streamUrl))
                val mediaSource = if (stream.isHls) {
                    HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
                } else {
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
                }
                player.setMediaSource(mediaSource, resumePositionMs)
                player.prepare()
                player.playWhenReady = true
            } catch (e: Exception) {
                streamError = e.backendMessage().ifBlank { "Не удалось получить аудиопоток" }
            } finally {
                isLoadingStream = false
            }
        }
    }

    private suspend fun awaitPlayer(): ExoPlayer? {
        PlayerBridge.player?.let {
            attachToPlayer(it)
            return it
        }
        if (mediaController == null && controllerFuture != null) {
            runCatching {
                suspendCancellableCoroutine { cont ->
                    val f = controllerFuture!!
                    f.addListener(
                        {
                            runCatching { f.get() }.onSuccess { mediaController = it }
                            if (cont.isActive) cont.resume(Unit)
                        },
                        MoreExecutors.directExecutor(),
                    )
                }
            }
        }
        repeat(50) {
            PlayerBridge.player?.let {
                attachToPlayer(it)
                return it
            }
            delay(100)
        }
        return PlayerBridge.player?.also { attachToPlayer(it) }
    }

    fun togglePlayPause() {
        scope.launch {
            val player = awaitPlayer() ?: return@launch
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun release() {
        playerListener?.let { boundPlayer?.removeListener(it) }
        playerListener = null
        boundPlayer = null
        PlayerBridge.unbindControls()
        mediaController?.release()
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayerState(): PlayerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { PlayerState(context.applicationContext, scope) }
    DisposableEffect(Unit) {
        onDispose { state.release() }
    }
    return state
}
