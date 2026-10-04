package com.gammatunes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gammatunes.app.model.Track
import com.gammatunes.app.network.ApiClient
import com.gammatunes.app.offline.OfflineModeRepository
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.player.PlayHistoryRepository
import com.gammatunes.app.player.PlayStatsRepository
import com.gammatunes.app.ui.components.TrackGridCell
import com.gammatunes.app.ui.i18n.LocalStrings
import kotlinx.coroutines.CancellationException

/** In-memory cache so switching tabs doesn't refetch (and reshuffle) recommendations. */
private object RecommendationsCache {
    private val entries = mutableMapOf<String, List<Track>>()
    fun get(key: String): List<Track>? = entries[key]
    fun put(key: String, tracks: List<Track>) { entries[key] = tracks }
    fun invalidate(key: String) { entries.remove(key) }
}

private sealed interface RecsState {
    data object Loading : RecsState
    data class Ready(val tracks: List<Track>) : RecsState
    data object Failed : RecsState
}

/**
 * Home tab: personalised track recommendations seeded from the user's most-played
 * and most-recent tracks, plus quick access to recent and top tracks.
 */
@Composable
fun HomeScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
) {
    val strings = LocalStrings.current
    val topAll by PlayStatsRepository.topTracks.collectAsState()
    val recentAll by PlayHistoryRepository.recent.collectAsState()
    val offlineMode by OfflineModeRepository.enabled.collectAsState()
    val cachedIndex by OfflineRepository.index.collectAsState()
    // In offline mode only downloaded tracks are playable, so only those are shown.
    val top = if (offlineMode) topAll.filter { cachedIndex.containsKey(it.videoId) } else topAll
    val recent = if (offlineMode) recentAll.filter { cachedIndex.containsKey(it.videoId) } else recentAll

    var refreshTick by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<RecsState>(RecsState.Loading) }

    // Seeds: favourites first (stable), then latest plays, from any source.
    // YouTube Music seeds feed the radio; SoundCloud seeds contribute their artists.
    val seedTracks = remember(top, recent) {
        (top + recent).distinctBy { it.videoId }.take(4)
    }
    val seedIds = seedTracks.map { it.videoId }
    val seedArtists = seedTracks.filter { it.isSoundCloud }.map { it.artist }.distinct()
    val cacheKey = seedIds.joinToString(",")

    LaunchedEffect(cacheKey, refreshTick, offlineMode) {
        if (offlineMode) return@LaunchedEffect
        val cached = RecommendationsCache.get(cacheKey)
        if (cached != null) {
            state = RecsState.Ready(cached)
            return@LaunchedEffect
        }
        state = RecsState.Loading
        state = try {
            val tracks = ApiClient.api.recommendations(
                seeds = seedIds,
                artists = seedArtists,
            ).results
            RecommendationsCache.put(cacheKey, tracks)
            RecsState.Ready(tracks)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            RecsState.Failed
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 300.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.homeTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    RecommendationsCache.invalidate(cacheKey)
                    refreshTick++
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = strings.homeRefresh)
                }
            }
        }
        if (offlineMode) {
            item {
                val cachedTracks = cachedIndex.values.map { it.track }
                SectionHeader(strings.cachedTracksTitle, strings.offlineSearchHint)
                if (cachedTracks.isEmpty()) {
                    Text(
                        strings.cachedTracksEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                } else {
                    TrackRow(cachedTracks, onTrackClick)
                }
            }
        } else item {
            SectionHeader(strings.homeRecommended, strings.homeRecommendedHint)
            when (val s = state) {
                RecsState.Loading -> Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                RecsState.Failed -> Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        strings.homeError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { refreshTick++ }) { Text(strings.homeRetry) }
                }

                is RecsState.Ready ->
                    if (s.tracks.isEmpty()) {
                        Text(
                            strings.homeEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    } else {
                        TrackRow(s.tracks, onTrackClick)
                    }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionHeader(strings.homeRecent)
                TrackRow(recent.take(15), onTrackClick)
            }
        }
        if (top.isNotEmpty()) {
            item {
                SectionHeader(strings.homeTopTracks)
                TrackRow(top.take(10), onTrackClick)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrackRow(tracks: List<Track>, onTrackClick: (Track, List<Track>) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tracks, key = { it.videoId }) { track ->
            TrackGridCell(
                track = track,
                onClick = { onTrackClick(track, tracks) },
                modifier = Modifier.width(148.dp),
                subtitle = track.artist,
            )
        }
    }
}
