package com.gammatunes.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gammatunes.app.ui.i18n.LocalStrings

/** "Search history" block for a LazyColumn: tap to repeat, ✕ to forget one, button to clear all. */
fun LazyListScope.searchHistoryItems(
    history: List<String>,
    title: String,
    clearLabel: String,
    removeLabel: String,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (history.isEmpty()) return
    item(key = "history-header") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text(clearLabel) }
        }
    }
    items(history, key = { "history:$it" }) { query ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(query) }
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onRemove(query) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = removeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Compact one-line variant (used above a track list). */
@Composable
fun SearchHistoryChips(
    history: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(history, key = { it }) { query ->
            AssistChip(
                onClick = { onPick(query) },
                label = { Text(query, maxLines = 1) },
                leadingIcon = {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}
