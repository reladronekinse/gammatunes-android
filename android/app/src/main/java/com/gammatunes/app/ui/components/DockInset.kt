package com.gammatunes.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of the floating bottom dock (mini-player + tabs + system nav bar inset),
 * measured in MainActivity. The dock overlays the NavHost, so every scrollable
 * screen must reserve this much space at the end of its content, otherwise the
 * last items can never be scrolled out from under the capsule.
 */
val LocalDockInset = compositionLocalOf { 0.dp }

/** Bottom content padding for scrollable screens: dock height plus a little breathing room. */
@Composable
fun dockPadding(extra: Dp = 16.dp): Dp = LocalDockInset.current + extra
