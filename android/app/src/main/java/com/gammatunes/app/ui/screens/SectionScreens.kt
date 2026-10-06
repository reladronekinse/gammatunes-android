package com.gammatunes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gammatunes.app.auth.AuthRepository
import com.gammatunes.app.auth.SoundCloudAuthRepository
import com.gammatunes.app.offline.OfflineRepository
import com.gammatunes.app.player.AudioQuality
import com.gammatunes.app.player.EqualizerManager
import com.gammatunes.app.player.PlaybackSettingsRepository
import com.gammatunes.app.together.TogetherSession
import com.gammatunes.app.together.TogetherState
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.components.dockPadding
import com.gammatunes.app.ui.i18n.AppLanguage
import com.gammatunes.app.ui.i18n.LocaleRepository
import com.gammatunes.app.ui.i18n.LocalStrings

/** Каркас экрана раздела: верхняя панель с «назад» и прокручиваемый список плиток. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SectionScaffold(
    title: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    title,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = dockPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Раздел «Музыка»: топы и кешированные (скачанные) треки и альбомы. */
@Composable
fun MusicSectionScreen(
    onBack: () -> Unit,
    onOpenOfflineTracks: () -> Unit,
    onOpenOfflineAlbums: () -> Unit,
    onOpenTopTracks: () -> Unit,
    onOpenTopArtists: () -> Unit,
) {
    val strings = LocalStrings.current
    val offlineIndex by OfflineRepository.index.collectAsState()
    val offlineAlbums by OfflineRepository.albums.collectAsState()

    SectionScaffold(title = strings.moreSectionMusic, onBack = onBack) {
        item { SectionTitle(strings.cachedSectionTitle) }
        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.OfflinePin,
                        title = strings.cachedTracksTitle,
                        subtitle = strings.cachedTracksSubtitle.format(offlineIndex.size),
                        onClick = onOpenOfflineTracks,
                    ),
                    MoreTileData(
                        icon = Icons.Default.Album,
                        title = strings.offlineAlbumsTitle,
                        subtitle = strings.offlineAlbumsSection.format(offlineAlbums.size),
                        onClick = onOpenOfflineAlbums,
                    ),
                ),
            )
        }

        item { SectionTitle(strings.topsSectionTitle) }
        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.Whatshot,
                        title = strings.topTracksTitle,
                        subtitle = strings.topTracksSubtitle,
                        onClick = onOpenTopTracks,
                    ),
                    MoreTileData(
                        icon = Icons.Default.Star,
                        title = strings.topArtistsTitle,
                        subtitle = strings.topArtistsSubtitle,
                        onClick = onOpenTopArtists,
                    ),
                ),
            )
        }
    }
}

/** Раздел «Настройки»: аккаунты, оформление, звук, эквалайзер, «Слушать вместе» и язык. */
@Composable
fun SettingsSectionScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTogether: () -> Unit,
    onOpenCache: () -> Unit,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val language by LocaleRepository.language.collectAsState()
    val ytmLoggedIn by AuthRepository.isLoggedIn.collectAsState()
    val scLoggedIn by SoundCloudAuthRepository.isLoggedIn.collectAsState()
    val isLoggedIn = ytmLoggedIn || scLoggedIn
    val playbackSettings by PlaybackSettingsRepository.settings.collectAsState()
    val eqState by EqualizerManager.state.collectAsState()
    val togetherState by TogetherSession.state.collectAsState()

    SectionScaffold(title = strings.moreSectionSettings, onBack = onBack) {
        item { SectionTitle(strings.accountSection) }
        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.AccountCircle,
                        title = strings.accountSection,
                        subtitle = if (isLoggedIn) strings.loggedIn else strings.guest,
                        onClick = onOpenAccount,
                    ),
                ),
            )
        }

        item { SectionTitle(strings.moreSectionSettings) }
        item {
            MoreTileGrid(
                listOf(
                    MoreTileData(
                        icon = Icons.Default.Palette,
                        title = strings.uiSettingsSection,
                        subtitle = strings.appearanceSubtitle,
                        onClick = onOpenAppearance,
                    ),
                    MoreTileData(
                        icon = Icons.Default.Tune,
                        title = strings.qualitySection,
                        subtitle = when (playbackSettings.quality) {
                            AudioQuality.HIGH -> strings.qualityHigh
                            AudioQuality.MEDIUM -> strings.qualityMedium
                            AudioQuality.LOW -> strings.qualityLow
                        },
                        onClick = onOpenPlayback,
                    ),
                    MoreTileData(
                        icon = Icons.Default.GraphicEq,
                        title = strings.eqTitle,
                        subtitle = if (eqState.enabled) eqProfileName(strings, eqState.profile) else strings.eqOff,
                        onClick = onOpenEqualizer,
                    ),
                    MoreTileData(
                        icon = Icons.Default.Group,
                        title = strings.togetherTitle,
                        subtitle = when (val s = togetherState) {
                            is TogetherState.Hosting -> strings.togetherHosting
                            is TogetherState.Joined -> strings.togetherJoinedTo.format(s.hostName)
                            is TogetherState.Starting, is TogetherState.Connecting -> strings.togetherConnecting
                            else -> strings.togetherSubtitle
                        },
                        onClick = onOpenTogether,
                    ),
                    MoreTileData(
                        icon = Icons.Default.Storage,
                        title = strings.cacheTitle,
                        subtitle = strings.cacheSubtitle,
                        onClick = onOpenCache,
                    ),
                ),
            )
        }

        item { SectionTitle(strings.languageSection) }
        item {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = language == AppLanguage.ENGLISH,
                        onClick = { LocaleRepository.setLanguage(context, AppLanguage.ENGLISH) },
                        label = { Text(strings.languageEnglish) },
                    )
                    FilterChip(
                        selected = language == AppLanguage.RUSSIAN,
                        onClick = { LocaleRepository.setLanguage(context, AppLanguage.RUSSIAN) },
                        label = { Text(strings.languageRussian) },
                    )
                }
            }
        }
    }
}
