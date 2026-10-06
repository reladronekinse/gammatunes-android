package com.gammatunes.app.ui.screens

import com.gammatunes.app.ui.components.dockPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.gammatunes.app.ui.components.GammaIcon
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.settings.AppIconManager
import com.gammatunes.app.ui.settings.AppIconPreset
import com.gammatunes.app.ui.i18n.LocalStrings
import com.gammatunes.app.ui.settings.AccentOption
import com.gammatunes.app.ui.settings.BackgroundStyle
import com.gammatunes.app.ui.settings.CoverStyle
import com.gammatunes.app.ui.settings.SeekBarStyle
import com.gammatunes.app.ui.settings.UiSettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    strings.uiSettingsSection,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = dockPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppearanceSettingsContent()
        }
    }
}

/**
 * Все настройки оформления (форма обложки, полоса перемотки, фон, акцент, иконка приложения).
 * Используется и на экране «Оформление», и в первичной настройке, чтобы они не расходились.
 */
@Composable
fun AppearanceSettingsContent() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val uiSettings by UiSettingsRepository.settings.collectAsState()
    var selectedIcon by remember { mutableStateOf(AppIconManager.current(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(strings.coverStyleLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        CoverStyle.SQUARE to strings.coverSquare,
                        CoverStyle.ROUNDED to strings.coverRounded,
                        CoverStyle.CIRCLE to strings.coverCircle,
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = uiSettings.coverStyle == style,
                            onClick = { UiSettingsRepository.setCoverStyle(context, style) },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                        )
                    }
                }

                Text(strings.seekBarStyleLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        SeekBarStyle.DEFAULT to strings.seekDefault,
                        SeekBarStyle.THIN to strings.seekThin,
                        SeekBarStyle.WAVE to strings.seekWave,
                        SeekBarStyle.SQUIGGLE to strings.seekSquiggle,
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = uiSettings.seekBarStyle == style,
                            onClick = { UiSettingsRepository.setSeekBarStyle(context, style) },
                            label = {
                                Text(label, maxLines = 1, softWrap = false)
                            },
                        )
                    }
                }

                Text(strings.backgroundStyleLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        BackgroundStyle.BLUR_ART to strings.bgBlurArt,
                        BackgroundStyle.FULL_COVER to strings.bgFullCover,
                        BackgroundStyle.SOLID_DARK to strings.bgSolid,
                        BackgroundStyle.GRADIENT to strings.bgGradient,
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = uiSettings.backgroundStyle == style,
                            onClick = {
                                UiSettingsRepository.setBackgroundStyle(context, style)
                            },
                            label = {
                                Text(label, maxLines = 1, softWrap = false)
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        strings.accentFromCoverLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiSettings.accentFromCover,
                        onCheckedChange = {
                            UiSettingsRepository.setAccentFromCover(context, it)
                        },
                    )
                }

                if (!uiSettings.accentFromCover) {
                    Text(strings.accentColorLabel, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AccentOption.entries.forEach { opt ->
                            val selected = uiSettings.accent == opt
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(opt.composeColor)
                                    .then(
                                        if (selected) {
                                            Modifier.border(3.dp, Color.White, CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable {
                                        UiSettingsRepository.setAccent(context, opt)
                                    },
                            )
                        }
                    }
                }
            }
        }

        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(strings.iconSectionLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GammaIcon(
                        background = Color(selectedIcon.background),
                        foreground = Color(selectedIcon.foreground),
                        size = 96.dp,
                    )
                }

                // Реальные варианты иконки приложения (activity-alias), а не ярлыки
                AppIconPreset.values().toList().chunked(5).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = preset == selectedIcon
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        3.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(16.dp),
                                    )
                                    .padding(4.dp)
                                    .clickable {
                                        if (AppIconManager.apply(context, preset)) {
                                            selectedIcon = preset
                                            Toast.makeText(context, strings.iconChanged, Toast.LENGTH_LONG).show()
                                        }
                                    },
                            ) {
                                GammaIcon(
                                    background = Color(preset.background),
                                    foreground = Color(preset.foreground),
                                    size = 44.dp,
                                )
                            }
                        }
                    }
                }
                Text(
                    strings.iconPresetHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}