package com.gammatunes.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gammatunes.app.auth.accountFor
import com.gammatunes.app.model.MusicSource
import com.gammatunes.app.model.Track
import com.gammatunes.app.ui.components.BrowserLoginDialog
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.components.SoundCloudLoginDialog
import com.gammatunes.app.ui.components.clearSoundCloudWebSession
import com.gammatunes.app.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/**
 * Account screen for both services: a source switch on top, then the same
 * sign-in / playlists / liked-tracks layout bound to the selected account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    var source by rememberSaveable { mutableStateOf(MusicSource.YTM) }
    val isSoundCloud = source == MusicSource.SOUNDCLOUD
    val account = accountFor(source)

    val isLoggedIn by account.isLoggedIn.collectAsState()
    val accountHint by account.accountHint.collectAsState()
    val likedTracks by account.likedTracks.collectAsState()
    val playlists by account.playlists.collectAsState()
    val statusMessage by account.statusMessage.collectAsState()
    val isBusy by account.isBusy.collectAsState()
    val scope = rememberCoroutineScope()

    var headersText by remember { mutableStateOf("") }
    var showLoginField by remember { mutableStateOf(false) }
    var showBrowserLogin by remember { mutableStateOf(false) }
    var expandedLiked by remember { mutableStateOf(false) }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    var expandedPlaylistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlistLoading by remember { mutableStateOf(false) }

    // Switching service: drop everything that belonged to the other account.
    LaunchedEffect(source) {
        headersText = ""
        showLoginField = false
        showBrowserLogin = false
        expandedLiked = false
        expandedPlaylistId = null
        expandedPlaylistTracks = emptyList()
    }

    LaunchedEffect(isLoggedIn, source) {
        if (isLoggedIn) {
            if (likedTracks.isEmpty()) account.refreshLiked()
            if (playlists.isEmpty()) account.refreshPlaylists()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    strings.accountSection,
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 300.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MusicSource.entries.forEach { option ->
                        FilterChip(
                            selected = source == option,
                            onClick = { source = option },
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
            }

            item {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (isLoggedIn) strings.loggedIn else strings.guest,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        // SoundCloud knows the user name; for YouTube it is just a generic label.
                        if (isLoggedIn && isSoundCloud && !accountHint.isNullOrBlank()) {
                            Text(
                                text = accountHint.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }

                        val err = statusMessage.orEmpty()
                        val looksLikeError = err.isNotBlank() && (
                            !isLoggedIn ||
                                "fail" in err.lowercase() ||
                                "error" in err.lowercase() ||
                                "не " in err.lowercase() ||
                                "ошиб" in err.lowercase() ||
                                "denied" in err.lowercase() ||
                                "устарел" in err.lowercase()
                            )
                        if (looksLikeError) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        if (!isLoggedIn) {
                            Button(
                                onClick = { showBrowserLogin = true },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(strings.loginWithBrowser)
                            }
                            TextButton(
                                onClick = { showLoginField = !showLoginField },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (isSoundCloud) strings.soundCloudLoginWithToken
                                    else strings.loginWithHeaders,
                                )
                            }
                            if (showLoginField) {
                                OutlinedTextField(
                                    value = headersText,
                                    onValueChange = { headersText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp),
                                    placeholder = {
                                        Text(
                                            if (isSoundCloud) strings.soundCloudTokenPlaceholder
                                            else strings.needHeaders,
                                        )
                                    },
                                    maxLines = 8,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val ok = account.login(headersText)
                                                if (ok) {
                                                    showLoginField = false
                                                    headersText = ""
                                                }
                                            }
                                        },
                                        enabled = !isBusy && headersText.isNotBlank(),
                                    ) {
                                        if (isBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text(strings.saveAndLogin)
                                        }
                                    }
                                    TextButton(onClick = { showLoginField = false }) {
                                        Text(strings.cancel)
                                    }
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            account.logout()
                                            // Otherwise the in-app browser would sign straight back in.
                                            if (isSoundCloud) clearSoundCloudWebSession()
                                        }
                                    },
                                    enabled = !isBusy,
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.logout)
                                }
                                OutlinedButton(
                                    onClick = { scope.launch { account.refreshPlaylists() } },
                                    enabled = !isBusy,
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.refreshPlaylists)
                                }
                            }
                        }
                    }
                }
            }

            if (isLoggedIn) {

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = strings.playlistsSection.format(playlists.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { scope.launch { account.refreshPlaylists() } },
                            enabled = !isBusy,
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = strings.refreshPlaylists)
                        }
                    }
                }
                if (playlists.isEmpty()) {
                    item {
                        Text(
                            if (isSoundCloud) strings.soundCloudPlaylistsEmpty else strings.playlistsEmpty,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(playlists, key = { "pl:${it.playlistId}" }) { pl ->
                        val expanded = expandedPlaylistId == pl.playlistId
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (expanded) {
                                                expandedPlaylistId = null
                                                expandedPlaylistTracks = emptyList()
                                            } else {
                                                expandedPlaylistId = pl.playlistId
                                                playlistLoading = true
                                                scope.launch {
                                                    try {
                                                        expandedPlaylistTracks =
                                                            account.loadPlaylistTracks(pl.playlistId)
                                                    } finally {
                                                        playlistLoading = false
                                                    }
                                                }
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pl.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                        )
                                        pl.count?.let {
                                            Text(
                                                strings.tracksCount.format(it),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Icon(
                                        if (expanded) Icons.Default.ExpandLess
                                        else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                    )
                                }
                                if (expanded) {
                                    if (playlistLoading) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    } else if (expandedPlaylistTracks.isEmpty()) {
                                        Text(
                                            strings.playlistTracksEmpty,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    } else {
                                        expandedPlaylistTracks.forEach { track ->
                                            TrackRow(
                                                track = track,
                                                onClick = {
                                                    onTrackClick(track, expandedPlaylistTracks)
                                                },
                                            )
                                            Spacer(Modifier.height(6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                item {
                    Spacer(Modifier.height(8.dp))
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedLiked = !expandedLiked }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = strings.likedSection.format(likedTracks.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { scope.launch { account.refreshLiked() } },
                                    enabled = !isBusy,
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = strings.refreshLikes)
                                }
                                Icon(
                                    if (expandedLiked) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                )
                            }
                            if (expandedLiked) {
                                if (likedTracks.isEmpty()) {
                                    Text(
                                        strings.likedEmpty,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    likedTracks.forEach { track ->
                                        TrackRow(
                                            track = track,
                                            onClick = { onTrackClick(track, likedTracks) },
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBrowserLogin) {
        if (isSoundCloud) {
            SoundCloudLoginDialog(
                onDismiss = { showBrowserLogin = false },
                onTokenCaptured = { token ->
                    scope.launch {
                        if (account.login(token)) showBrowserLogin = false
                    }
                },
            )
        } else {
            BrowserLoginDialog(
                onDismiss = { showBrowserLogin = false },
                onCookiesCaptured = { headers ->
                    scope.launch {
                        if (account.login(headers)) showBrowserLogin = false
                    }
                },
            )
        }
    }
}
