package com.gammatunes.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gammatunes.app.ui.components.LiquidGlassSurface

// Общие плитки раздела «Прочее»: используются главным экраном и экранами разделов.

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

internal data class MoreTileData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

private val TileShape = RoundedCornerShape(22.dp)

/** Плитки по две в ряд; если в категории один пункт — он занимает всю ширину. */
@Composable
internal fun MoreTileGrid(items: List<MoreTileData>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (items.size == 1) {
            MoreWideTile(items.first(), Modifier.fillMaxWidth())
        } else {
            items.chunked(2).forEach { rowItems ->
                // IntrinsicSize.Max выравнивает высоту плиток в ряду по самой высокой
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        MoreTile(item, Modifier.weight(1f).fillMaxHeight())
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TileIcon(icon: ImageVector, circleSize: Dp, iconSize: Dp) {
    Box(
        modifier = Modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun MoreTile(data: MoreTileData, modifier: Modifier = Modifier) {
    LiquidGlassSurface(
        modifier = modifier
            .clip(TileShape)
            .clickable(onClick = data.onClick),
        shape = TileShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp)
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            TileIcon(data.icon, circleSize = 64.dp, iconSize = 36.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    data.title,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    data.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MoreWideTile(data: MoreTileData, modifier: Modifier = Modifier) {
    LiquidGlassSurface(
        modifier = modifier
            .clip(TileShape)
            .clickable(onClick = data.onClick),
        shape = TileShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TileIcon(data.icon, circleSize = 64.dp, iconSize = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    data.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    data.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
