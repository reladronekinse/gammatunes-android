@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.gammatunes.app.ui.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gammatunes.app.model.Track
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.player.LocalPlayerState
import com.gammatunes.app.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/**
 * Context menu for a track shown on long-press:
 * - Play next
 * - Download / Delete (depending on offline state)
 *
 * Place next to the item that uses [combinedClickable] with onLongClick
 * that sets [expanded] to true.
 */
@Composable
fun TrackContextMenu(
    track: Track,
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val player = LocalPlayerState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val index by OfflineRepository.index.collectAsState()
    val downloadingIds by OfflineRepository.downloadingIds.collectAsState()
    val isDownloaded = index.containsKey(track.videoId)
    val isDownloading = downloadingIds.contains(track.videoId)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        if (player != null) {
            DropdownMenuItem(
                text = { Text(strings.playNext) },
                onClick = {
                    player.enqueueNext(track)
                    Toast.makeText(context, strings.playNextAdded, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                    )
                },
            )
        }

        DropdownMenuItem(
            text = {
                Text(
                    when {
                        isDownloading -> strings.downloaded
                        isDownloaded -> strings.downloaded
                        else -> strings.download
                    }
                )
            },
            onClick = {
                scope.launch {
                    when {
                        isDownloading -> Unit
                        isDownloaded -> OfflineRepository.delete(track.videoId)
                        else -> OfflineRepository.download(track)
                    }
                }
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isDownloaded || isDownloading) Icons.Default.Delete else Icons.Default.Download,
                    contentDescription = null,
                )
            },
            enabled = !isDownloading,
        )
    }
}
