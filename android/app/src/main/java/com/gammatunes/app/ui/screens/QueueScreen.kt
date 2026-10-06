@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.gammatunes.app.ui.screens

import com.gammatunes.app.ui.components.dockPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.gammatunes.app.player.PlayerState
import com.gammatunes.app.player.QueueEntry
import com.gammatunes.app.ui.i18n.LocalStrings

private val RowHeight = 64.dp

/** Full playback queue: tap to jump, drag the handle to reorder, ✕ to remove. */
@Composable
fun QueueScreen(
    player: PlayerState,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    val entries = player.queueEntries
    val currentIndex = player.queueIndex
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }

    var draggingUid by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(strings.queueTitle, maxLines = 1, style = MaterialTheme.typography.titleLarge)
                    Text(
                        strings.queueReorder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
        )

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    strings.queueEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = dockPadding()),
        ) {
            itemsIndexed(entries, key = { _, e -> e.uid }) { index, entry ->
                val isDragging = draggingUid == entry.uid
                QueueRow(
                    entry = entry,
                    isCurrent = index == currentIndex,
                    isPlayed = index < currentIndex,
                    isDragging = isDragging,
                    removeLabel = strings.queueRemove,
                    nowPlayingLabel = strings.queueNowPlaying,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                    onClick = { player.playQueueIndex(index) },
                    onRemove = { player.removeQueueItem(index) },
                    handleModifier = Modifier.pointerInput(entry.uid) {
                        detectDragGestures(
                            onDragStart = {
                                draggingUid = entry.uid
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                // The item keeps its identity (key = uid) while it moves, so
                                // swap with a neighbour each time the finger passes half a row.
                                val from = player.queueEntries.indexOfFirst { it.uid == entry.uid }
                                if (from < 0) return@detectDragGestures
                                if (dragOffset > rowPx / 2 && from < player.queueEntries.lastIndex) {
                                    player.moveQueueItem(from, from + 1)
                                    dragOffset -= rowPx
                                } else if (dragOffset < -rowPx / 2 && from > 0) {
                                    player.moveQueueItem(from, from - 1)
                                    dragOffset += rowPx
                                }
                            },
                            onDragEnd = {
                                draggingUid = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingUid = null
                                dragOffset = 0f
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    isPlayed: Boolean,
    isDragging: Boolean,
    removeLabel: String,
    nowPlayingLabel: String,
    modifier: Modifier,
    handleModifier: Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val track = entry.track
    val container = when {
        isDragging -> MaterialTheme.colorScheme.surfaceVariant
        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.background
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(container)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.thumbnail,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (isCurrent) "$nowPlayingLabel · ${track.artist}" else track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!isCurrent) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = removeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = handleModifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPlayed) 0.5f else 1f),
            )
        }
    }
}
