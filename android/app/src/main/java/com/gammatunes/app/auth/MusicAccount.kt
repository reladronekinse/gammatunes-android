package com.gammatunes.app.auth

import com.gammatunes.app.model.MusicSource
import com.gammatunes.app.model.PlaylistSummary
import com.gammatunes.app.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * One signed-in account on one music service. Implemented by [AuthRepository]
 * (YouTube Music) and [SoundCloudAuthRepository], so screens can show liked
 * tracks / playlists / like buttons without caring which service they talk to.
 */
interface MusicAccount {
    val isLoggedIn: StateFlow<Boolean>
    val accountHint: StateFlow<String?>
    val likedTracks: StateFlow<List<Track>>
    val playlists: StateFlow<List<PlaylistSummary>>
    val statusMessage: StateFlow<String?>
    val isBusy: StateFlow<Boolean>

    /** [raw] is service specific: browser headers for YTM, an oauth_token for SoundCloud. */
    suspend fun login(raw: String): Boolean
    suspend fun logout()
    suspend fun refreshLiked(limit: Int = 5000)
    suspend fun refreshPlaylists(limit: Int = 100)
    suspend fun loadPlaylistTracks(playlistId: String, limit: Int = 5000): List<Track>
    suspend fun likeTrack(videoId: String): Boolean
    suspend fun unlikeTrack(videoId: String): Boolean
    suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean
}

/** The account a track belongs to: SoundCloud ids ("sc_...") vs YouTube ids. */
fun accountFor(track: Track): MusicAccount =
    if (track.isSoundCloud) SoundCloudAuthRepository else AuthRepository

fun accountFor(source: MusicSource): MusicAccount = when (source) {
    MusicSource.YTM -> AuthRepository
    MusicSource.SOUNDCLOUD -> SoundCloudAuthRepository
}
