package com.gammatunes.app.model

data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,

    val albumId: String? = null,
    val thumbnail: String? = null,
    val durationSeconds: Int? = null,

    val artistId: String? = null,
    /** True for YouTube videos (not music catalogue songs). Player shows video surface. */
    val isVideo: Boolean = false,
) {
    /** True for tracks coming from SoundCloud (backend ids look like "sc_<id>"). */
    val isSoundCloud: Boolean get() = videoId.startsWith(SOUNDCLOUD_ID_PREFIX)

    companion object {
        const val SOUNDCLOUD_ID_PREFIX = "sc_"
    }
}

data class Album(
    val albumId: String,
    val title: String,
    val thumbnail: String? = null,
    val year: String? = null,
)

data class Artist(
    val artistId: String,
    val name: String,
    val thumbnail: String? = null,
    /** Широкая картинка для баннера на экране артиста (если бэкенд её отдал). */
    val banner: String? = null,
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList(),

    val songs: List<Track> = emptyList(),
)

data class ArtistSearchResponse(
    val artists: List<Artist> = emptyList(),
)

data class SearchResponse(
    val results: List<Track> = emptyList(),
)

data class AlbumTracksResponse(
    val albumId: String,
    val title: String,
    val thumbnail: String? = null,
    val tracks: List<Track> = emptyList(),
)

data class StreamResponse(
    val videoId: String,
    val streamUrl: String,
    val mimeType: String,
    val bitrate: Int,
    val quality: String = "high",
    val httpHeaders: Map<String, String> = emptyMap(),
    val isVideoStream: Boolean = false,
    /** True when [streamUrl] is an HLS playlist (some SoundCloud tracks). */
    val isHls: Boolean = false,
)

data class AuthStatusResponse(
    val loggedIn: Boolean = false,
    val accountName: String? = null,
)

data class AuthLoginResponse(
    val ok: Boolean = false,
    val accountName: String? = null,
    val authJson: String? = null,
    val detail: String? = null,
)

data class SimpleOkResponse(
    val ok: Boolean = false,
    val videoId: String? = null,
    val rating: String? = null,
    val detail: String? = null,
)

data class PlaylistSummary(
    val playlistId: String,
    val title: String,
    val thumbnail: String? = null,
    val count: Int? = null,
    /** SoundCloud only: someone else's playlist you liked (tracks can't be added to it). */
    val readOnly: Boolean = false,
)

data class PlaylistsResponse(
    val playlists: List<PlaylistSummary> = emptyList(),
)

data class PlaylistTracksResponse(
    val playlistId: String,
    val title: String,
    val tracks: List<Track> = emptyList(),
)

/** Id prefixes the backend uses for SoundCloud users ("artists") and playlists/albums. */
const val SOUNDCLOUD_USER_PREFIX = "scu_"
const val SOUNDCLOUD_PLAYLIST_PREFIX = "scp_"

fun String.isSoundCloudArtistId(): Boolean = startsWith(SOUNDCLOUD_USER_PREFIX)

data class LyricsApiResponse(
    val ok: Boolean = false,
    val synced: Boolean = false,
    val source: String? = null,
    val lrc: String? = null,
    val plain: String? = null,
    val error: String? = null,
)
