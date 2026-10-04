@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.gammatunes.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gammatunes.app.model.Track
import com.gammatunes.app.player.LocalPlayerState
import com.gammatunes.app.ui.i18n.LocalStrings

/** Inserts [track] right after the currently playing one ("Play next"). */
@Composable
fun PlayNextButton(
    track: Track,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 26.dp,
) {
    val player = LocalPlayerState.current ?: return
    val strings = LocalStrings.current
    val context = LocalContext.current
    IconButton(
        onClick = {
            player.enqueueNext(track)
            Toast.makeText(context, strings.playNextAdded, Toast.LENGTH_SHORT).show()
        },
        modifier = modifier.size(buttonSize),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
            contentDescription = strings.playNext,
            modifier = Modifier.size(iconSize),
        )
    }
}
