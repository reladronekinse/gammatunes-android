package com.gammatunes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.components.dockPadding
import com.gammatunes.app.ui.i18n.LocalStrings
import com.gammatunes.app.update.AppUpdateRepository

/**
 * Главный экран «Прочее»: крупные кнопки разделов. Внутри каждого раздела — свои пункты
 * (см. [MusicSectionScreen], [SettingsSectionScreen], [CacheScreen]).
 */
@Composable
fun MoreScreen(
    onOpenMusic: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpdates: () -> Unit,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val updateState by AppUpdateRepository.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = dockPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = strings.moreTitle,
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.LibraryMusic,
                        title = strings.moreSectionMusic,
                        subtitle = strings.moreSectionMusicSubtitle,
                        onClick = onOpenMusic,
                    ),
                ),
            )
        }
        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.Settings,
                        title = strings.moreSectionSettings,
                        subtitle = strings.moreSectionSettingsSubtitle,
                        onClick = onOpenSettings,
                    ),
                ),
            )
        }
        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.SystemUpdate,
                        title = strings.updatesSection,
                        subtitle = when (val s = updateState) {
                            is AppUpdateRepository.UpdateState.Available ->
                                strings.updateAvailable.format(s.release.tag_name)
                            is AppUpdateRepository.UpdateState.Downloading ->
                                strings.downloading.format(s.progressPercent)
                            is AppUpdateRepository.UpdateState.UpToDate -> strings.upToDate
                            else -> strings.updatesSubtitle
                        },
                        onClick = onOpenUpdates,
                    ),
                ),
            )
        }

        item {
            SectionTitle(strings.aboutSection)
        }
        item {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val versionName = remember {
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull() ?: "0.5-unstable"
                    }
                    Text(
                        text = strings.versionLabel.format(versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.licenseLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
