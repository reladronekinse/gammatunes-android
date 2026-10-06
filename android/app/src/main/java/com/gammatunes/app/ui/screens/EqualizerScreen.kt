package com.gammatunes.app.ui.screens

import com.gammatunes.app.ui.components.dockPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gammatunes.app.player.EqProfile
import com.gammatunes.app.player.EqualizerManager
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.i18n.AppStrings
import com.gammatunes.app.ui.i18n.LocalStrings
import kotlin.math.roundToInt

fun eqProfileName(strings: AppStrings, profile: EqProfile): String = when (profile) {
    EqProfile.FLAT -> strings.eqFlat
    EqProfile.BASS_BOOST -> strings.eqBassBoost
    EqProfile.TREBLE_BOOST -> strings.eqTrebleBoost
    EqProfile.VOCAL -> strings.eqVocal
    EqProfile.ROCK -> strings.eqRock
    EqProfile.POP -> strings.eqPop
    EqProfile.JAZZ -> strings.eqJazz
    EqProfile.CLASSICAL -> strings.eqClassical
    EqProfile.ELECTRONIC -> strings.eqElectronic
    EqProfile.HIP_HOP -> strings.eqHipHop
    EqProfile.ACOUSTIC -> strings.eqAcoustic
    EqProfile.CUSTOM -> strings.eqCustom
}

private fun formatHz(hz: Int): String = when {
    hz < 1000 -> "$hz Hz"
    hz % 1000 == 0 -> "${hz / 1000} kHz"
    else -> "%.1f kHz".format(hz / 1000f)
}

private fun formatDb(mb: Int): String = "%+d dB".format((mb / 100f).roundToInt())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current
    val eq by EqualizerManager.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    strings.eqTitle,
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
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            strings.eqEnable,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = eq.enabled,
                            onCheckedChange = { EqualizerManager.setEnabled(it) },
                            enabled = eq.available,
                        )
                    }
                    if (!eq.available) {
                        Text(
                            strings.eqUnavailable,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.eqProfileLabel, style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EqProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = eq.profile == profile,
                                onClick = { EqualizerManager.setProfile(profile) },
                                enabled = eq.available,
                                label = {
                                    Text(
                                        eqProfileName(strings, profile),
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (eq.available && eq.centerFreqsHz.isNotEmpty()) {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(strings.eqBandsLabel, style = MaterialTheme.typography.labelLarge)
                        eq.centerFreqsHz.forEachIndexed { index, hz ->
                            val level = eq.bandLevelsMb.getOrElse(index) { 0 }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    formatHz(hz),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.width(60.dp),
                                )
                                Slider(
                                    value = level.toFloat(),
                                    onValueChange = { v ->
                                        val snapped = (v / 100f).roundToInt() * 100
                                        EqualizerManager.setBandLevel(index, snapped)
                                    },
                                    valueRange = eq.minLevelMb.toFloat()..eq.maxLevelMb.toFloat(),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatDb(level),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(56.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
