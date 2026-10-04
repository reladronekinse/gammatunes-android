package com.gammatunes.app.auth

import android.content.Context
import android.util.Log
import com.gammatunes.app.model.PlaylistSummary
import com.gammatunes.app.model.Track
import com.gammatunes.app.network.ApiClient
import com.gammatunes.app.network.addToPlaylist
import com.gammatunes.app.network.backendMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File

/**
 * SoundCloud session: sign-in, liked tracks, playlists, likes. Mirrors
 * [AuthRepository]; the embedded backend does the actual SoundCloud calls
 * (all requests here carry `source=soundcloud`).
 *
 * The persisted file holds just `{"oauthToken": "..."}`; it is replayed to the
 * backend on every app start because the backend keeps the session in memory.
 */
object SoundCloudAuthRepository : MusicAccount {
    private const val TAG = "SoundCloudAuth"
    private const val AUTH_FILE = "sc_auth.json"
    private const val SOURCE = "soundcloud"

    private lateinit var appContext: Context

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _accountHint = MutableStateFlow<String?>(null)
    override val accountHint: StateFlow<String?> = _accountHint

    private val _likedTracks = MutableStateFlow<List<Track>>(emptyList())
    override val likedTracks: StateFlow<List<Track>> = _likedTracks

    private val _playlists = MutableStateFlow<List<PlaylistSummary>>(emptyList())
    override val playlists: StateFlow<List<PlaylistSummary>> = _playlists

    private val _statusMessage = MutableStateFlow<String?>(null)
    override val statusMessage: StateFlow<String?> = _statusMessage

    private val _isBusy = MutableStateFlow(false)
    override val isBusy: StateFlow<Boolean> = _isBusy

    fun init(context: Context) {
        appContext = context.applicationContext
        val file = authFile()
        if (file.exists() && file.length() > 10) {
            _isLoggedIn.value = true
            _accountHint.value = "Сессия сохранена"
        }
    }

    private fun authFile(): File = File(appContext.filesDir, AUTH_FILE)

    override suspend fun login(raw: String): Boolean = withContext(Dispatchers.IO) {
        _isBusy.value = true
        _statusMessage.value = null
        try {
            val response = ApiClient.api.authLogin(
                mapOf("headersRaw" to raw.trim(), "source" to SOURCE),
            )
            if (response.ok) {
                authFile().writeText(response.authJson ?: raw.trim())
                _isLoggedIn.value = true
                _accountHint.value = response.accountName ?: "Вход выполнен"
                _statusMessage.value = "Вход выполнен"
                true
            } else {
                _statusMessage.value = response.detail ?: "Не удалось войти"
                false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "login failed", t)
            _statusMessage.value = t.backendMessage()
            false
        } finally {
            _isBusy.value = false
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        _isBusy.value = true
        try {
            runCatching { ApiClient.api.authLogout(SOURCE) }
            authFile().delete()
            _isLoggedIn.value = false
            _accountHint.value = null
            _likedTracks.value = emptyList()
            _playlists.value = emptyList()
            _statusMessage.value = "Вы вышли из аккаунта"
        } finally {
            _isBusy.value = false
        }
    }

    override suspend fun refreshLiked(limit: Int) = withContext(Dispatchers.IO) {
        if (!_isLoggedIn.value) return@withContext
        _isBusy.value = true
        _statusMessage.value = null
        try {
            val response = ApiClient.api.likedSongs(limit, SOURCE)
            _likedTracks.value = response.results
            if (response.results.isEmpty()) {
                _statusMessage.value = "Лайкнутых треков нет или сессия устарела"
            }
        } catch (t: Throwable) {
            Log.e(TAG, "liked tracks failed", t)
            _statusMessage.value = t.backendMessage()
        } finally {
            _isBusy.value = false
        }
    }

    override suspend fun refreshPlaylists(limit: Int) = withContext(Dispatchers.IO) {
        if (!_isLoggedIn.value) return@withContext
        _isBusy.value = true
        try {
            _playlists.value = ApiClient.api.libraryPlaylists(limit, SOURCE).playlists
            _statusMessage.value = null
        } catch (t: Throwable) {
            Log.e(TAG, "playlists failed", t)
            _statusMessage.value = t.backendMessage()
        } finally {
            _isBusy.value = false
        }
    }

    override suspend fun loadPlaylistTracks(playlistId: String, limit: Int): List<Track> =
        withContext(Dispatchers.IO) {
            try {
                ApiClient.api.playlistTracks(playlistId, limit).tracks
            } catch (t: Throwable) {
                Log.e(TAG, "playlist tracks failed", t)
                _statusMessage.value = t.backendMessage()
                emptyList()
            }
        }

    override suspend fun likeTrack(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ApiClient.api.rateSongBody(mapOf("videoId" to videoId, "rating" to "LIKE")).ok
        } catch (t: Throwable) {
            Log.e(TAG, "like failed", t)
            false
        }
    }

    override suspend fun unlikeTrack(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ApiClient.api.rateSongBody(mapOf("videoId" to videoId, "rating" to "INDIFFERENT")).ok
        } catch (t: Throwable) {
            Log.e(TAG, "unlike failed", t)
            false
        }
    }

    override suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                ApiClient.api.addToPlaylist(playlistId, videoId).ok
            } catch (t: Throwable) {
                Log.e(TAG, "addToPlaylist failed", t)
                false
            }
        }

    /** Replays the saved token to the (freshly started) backend. */
    suspend fun restoreSessionIfNeeded() = withContext(Dispatchers.IO) {
        val file = authFile()
        if (!file.exists()) return@withContext
        try {
            val content = file.readText()
            if (content.isBlank()) return@withContext
            val response = ApiClient.api.authLogin(mapOf("headersRaw" to content, "source" to SOURCE))
            if (response.ok) {
                _isLoggedIn.value = true
                _accountHint.value = response.accountName ?: "Сессия восстановлена"
            } else {
                dropSession(file)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "restore session failed: ${t.message}")
            // 400/401 = token no longer valid; anything else (offline...) keeps the session.
            if (t is HttpException && (t.code() == 400 || t.code() == 401)) dropSession(file)
            return@withContext
        }
        // Restored: load likes so the heart in the player is right without opening Account.
        refreshLiked()
    }

    private fun dropSession(file: File) {
        _isLoggedIn.value = false
        _accountHint.value = null
        file.delete()
    }
}
