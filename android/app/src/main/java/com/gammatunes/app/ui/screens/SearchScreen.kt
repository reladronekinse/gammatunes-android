@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.gammatunes.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.player.PlayHistoryRepository
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gammatunes.app.backend.LocalBackend
import com.gammatunes.app.model.Artist
import com.gammatunes.app.model.MusicSource
import com.gammatunes.app.model.Track
import com.gammatunes.app.offline.filterByQuery
import com.gammatunes.app.network.ApiClient
import com.gammatunes.app.search.SearchHistoryRepository
import com.gammatunes.app.ui.components.SearchHistoryChips
import com.gammatunes.app.ui.components.searchHistoryItems
import kotlinx.coroutines.delay
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.components.TrackContextMenu
import com.gammatunes.app.ui.i18n.LocalStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Composable
fun SearchScreen(onArtistClick: (Artist) -> Unit, onTrackClick: (Track, List<Track>) -> Unit) {
    val strings = LocalStrings.current
    val offlineMode by com.gammatunes.app.offline.OfflineModeRepository.enabled.collectAsState()
    if (offlineMode) {
        CachedSearchContent(onTrackClick)
        return
    }
    var query by remember { mutableStateOf("") }
    var artistResults by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var trackResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var source by rememberSaveable { mutableStateOf(MusicSource.YTM) }
    // Bumped on every search so a slow, outdated response (e.g. after the user
    // switched the source) can't overwrite newer results.
    val requestCounter = remember { intArrayOf(0) }
    // "source|query" of the latest request, so Enter + debounce never search twice.
    val lastKey = remember { arrayOf<String?>(null) }
    val searchHistory by SearchHistoryRepository.online.items.collectAsState()
    val scope = rememberCoroutineScope()

    fun runSearch(searchSource: MusicSource = source) {
        if (query.isBlank()) return
        lastKey[0] = "${searchSource.apiValue}|$query"
        val requestId = ++requestCounter[0]
        isLoading = true
        error = null
        hasSearched = true
        scope.launch {
            var newArtists: List<Artist> = emptyList()
            var newTracks: List<Track> = emptyList()
            var newError: String? = null
            try {
                if (LocalBackend.lastError != null) {
                    newError = LocalBackend.lastError
                } else if (!LocalBackend.awaitReady(timeoutMs = 8_000)) {
                    newError = strings.offlineOrBackendError
                } else {
                    coroutineScope {
                        val artistsDeferred = async {
                            runCatching { ApiClient.api.searchArtists(query, searchSource.apiValue).artists }
                                .getOrElse { emptyList() }
                        }
                        val tracksDeferred = async {
                            runCatching { ApiClient.api.searchTracks(query, searchSource.apiValue).results }
                                .getOrElse { emptyList() }
                        }
                        newArtists = artistsDeferred.await()
                        newTracks = tracksDeferred.await()
                        if (newArtists.isEmpty() && newTracks.isEmpty()) {
                            // Nothing came back: repeat the tracks request to tell
                            // "no results" apart from a network/backend error.
                            try {
                                ApiClient.api.searchTracks(query, searchSource.apiValue)
                            } catch (t: Throwable) {
                                newError = friendlyNetworkError(t, strings)
                            }
                        }
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                newArtists = emptyList()
                newTracks = emptyList()
                newError = friendlyNetworkError(t, strings)
            }
            if (requestId == requestCounter[0]) {
                artistResults = newArtists
                trackResults = newTracks
                error = newError
                isLoading = false
            }
        }
    }

    // Search as you type: wait for a short pause, then search (also re-runs on source change).
    LaunchedEffect(query, source) {
        if (query.isBlank()) {
            requestCounter[0]++
            lastKey[0] = null
            artistResults = emptyList()
            trackResults = emptyList()
            error = null
            isLoading = false
            hasSearched = false
            return@LaunchedEffect
        }
        delay(400)
        if (lastKey[0] != "${source.apiValue}|$query") runSearch()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (source == MusicSource.SOUNDCLOUD) strings.searchPlaceholderSoundCloud
                    else strings.searchPlaceholder,
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    SearchHistoryRepository.online.add(query)
                    if (lastKey[0] != "${source.apiValue}|$query") runSearch()
                },
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MusicSource.entries.forEach { option ->
                FilterChip(
                    selected = source == option,
                    onClick = {
                        if (source != option) {
                            source = option
                        }
                    },
                    label = {
                        Text(
                            when (option) {
                                MusicSource.YTM -> strings.sourceYtm
                                MusicSource.SOUNDCLOUD -> strings.sourceSoundCloud
                            },
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }

        error?.let {
            Text(
                text = "${strings.errorPrefix}$it",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (!isLoading && error == null && hasSearched && artistResults.isEmpty() && trackResults.isEmpty()) {
            Text(
                text = strings.nothingFound,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        val recent by PlayHistoryRepository.recent.collectAsState()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 300.dp),
        ) {

            if (query.isBlank()) {
                searchHistoryItems(
                    history = searchHistory,
                    title = strings.searchHistoryTitle,
                    clearLabel = strings.searchHistoryClear,
                    removeLabel = strings.searchHistoryRemove,
                    onPick = { query = it },
                    onRemove = { SearchHistoryRepository.online.remove(it) },
                    onClear = { SearchHistoryRepository.online.clear() },
                )
            }
            if (!hasSearched && !isLoading && recent.isNotEmpty()) {
                item {
                    Text(
                        text = strings.recentlyPlayed,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                items(recent, key = { "recent:${it.videoId}" }) { track ->
                    TrackRow(track = track, onClick = { onTrackClick(track, recent) })
                }
            }
            if (artistResults.isNotEmpty()) {
                item {
                    Text(
                        text = strings.artists,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                items(artistResults, key = { "artist:${it.artistId}" }) { artist ->
                    ArtistResultRow(artist = artist, onClick = {
                        SearchHistoryRepository.online.add(query)
                        onArtistClick(artist)
                    })
                }
            }
            if (trackResults.isNotEmpty()) {
                item {
                    Text(
                        text = strings.tracks,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(trackResults, key = { "track:${it.videoId}" }) { track ->
                    TrackRow(track = track, onClick = {
                        SearchHistoryRepository.online.add(query)
                        onTrackClick(track, trackResults)
                    })
                }
            }
        }
    }
}

private fun friendlyNetworkError(t: Throwable, strings: com.gammatunes.app.ui.i18n.AppStrings): String {
    val cause = generateSequence(t) { it.cause }.firstOrNull {
        it is UnknownHostException || it is SocketTimeoutException || it is IOException
    }
    return when {
        cause is UnknownHostException -> strings.offlineOrBackendError
        cause is SocketTimeoutException -> strings.offlineOrBackendError
        t.message?.contains("Failed to connect", ignoreCase = true) == true -> strings.offlineOrBackendError
        t.message?.contains("Unable to resolve host", ignoreCase = true) == true -> strings.offlineOrBackendError
        t.message?.contains("Connection refused", ignoreCase = true) == true -> strings.offlineOrBackendError
        else -> t.message ?: strings.offlineOrBackendError
    }
}

@Composable
private fun ArtistResultRow(artist: Artist, onClick: () -> Unit) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artist.thumbnail,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun TrackRow(track: Track, onClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true },
                ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    AsyncImage(
                        model = track.thumbnail,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                    if (track.isVideo) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                .padding(1.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            track.isVideo -> listOfNotNull(track.artist, "Video").joinToString(" · ")
                            track.isSoundCloud -> "${track.artist} · SoundCloud"
                            else -> track.artist
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TrackContextMenu(
            track = track,
            expanded = showMenu,
            onDismiss = { showMenu = false },
        )
    }
}


/** Offline-mode search: live filter over downloaded tracks, no network involved. */
@Composable
private fun CachedSearchContent(onTrackClick: (Track, List<Track>) -> Unit) {
    val strings = LocalStrings.current
    val index by OfflineRepository.index.collectAsState()
    var query by remember { mutableStateOf("") }
    val history by SearchHistoryRepository.cached.items.collectAsState()
    val cached = remember(index) { index.values.map { it.track }.sortedBy { it.title.lowercase() } }
    val results = remember(cached, query) {
        cached.filterByQuery(query)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(strings.cachedSearchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { SearchHistoryRepository.cached.add(query) },
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
        )
        Spacer(Modifier.height(8.dp))
        if (query.isBlank()) {
            SearchHistoryChips(history = history, onPick = { query = it }, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            text = strings.offlineSearchHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        when {
            cached.isEmpty() -> Text(
                text = strings.cachedTracksEmpty,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            results.isEmpty() -> Text(
                text = strings.nothingFound,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 300.dp),
            ) {
                items(results, key = { "cached:${it.videoId}" }) { track ->
                    TrackRow(track = track, onClick = {
                        SearchHistoryRepository.cached.add(query)
                        onTrackClick(track, results)
                    })
                }
            }
        }
    }
}
