package com.gammatunes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gammatunes.app.model.Track
import com.gammatunes.app.offline.OfflineModeRepository
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.offline.filterByQuery
import com.gammatunes.app.search.SearchHistoryRepository
import com.gammatunes.app.ui.components.SearchHistoryChips
import com.gammatunes.app.ui.i18n.LocalStrings

/**
 * "Cached" / "Кешированное" — tracks that finished downloading and are
 * available for offline listening. Searchable; also hosts the offline-mode switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineTracksScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    val offlineIndex by OfflineRepository.index.collectAsState()
    val offlineMode by OfflineModeRepository.enabled.collectAsState()
    val offlineTracks = offlineIndex.values.map { it.track }
    var query by remember { mutableStateOf("") }
    val history by SearchHistoryRepository.cached.items.collectAsState()
    val filtered = remember(offlineIndex, query) {
        offlineIndex.values.map { it.track }.filterByQuery(query)
    }

    LaunchedEffect(Unit) { OfflineRepository.pruneMissing() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    strings.cachedTracksTitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
        )
        if (offlineTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = strings.cachedTracksEmpty,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = strings.cachedTracksEmptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(strings.cachedSearchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { SearchHistoryRepository.cached.add(query) },
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
            )
            if (query.isBlank()) {
                SearchHistoryChips(
                    history = history,
                    onPick = { query = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.offlineModeSwitch,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = offlineMode, onCheckedChange = { OfflineModeRepository.set(it) })
            }
            if (filtered.isEmpty()) {
                Text(
                    text = strings.nothingFound,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                ) {
                    items(filtered, key = { it.videoId }) { track ->
                        TrackRow(
                            track = track,
                            onClick = {
                                SearchHistoryRepository.cached.add(query)
                                onTrackClick(track, filtered)
                            },
                        )
                    }
                }
            }
        }
    }
}
