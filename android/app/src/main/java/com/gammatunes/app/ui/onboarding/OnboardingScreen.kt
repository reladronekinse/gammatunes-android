package com.gammatunes.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gammatunes.app.player.AudioQuality
import com.gammatunes.app.player.PlaybackSettingsRepository
import com.gammatunes.app.ui.components.GammaIcon
import com.gammatunes.app.ui.components.LiquidGlassSurface
import com.gammatunes.app.ui.i18n.AppLanguage
import com.gammatunes.app.ui.i18n.LocalLanguage
import com.gammatunes.app.ui.i18n.LocalStrings
import com.gammatunes.app.ui.i18n.LocaleRepository
import com.gammatunes.app.ui.screens.AppearanceSettingsContent
import com.gammatunes.app.ui.settings.CoverStyle
import com.gammatunes.app.ui.settings.UiSettingsRepository
import kotlinx.coroutines.launch

private const val PAGE_WELCOME = 0
private const val PAGE_APPEARANCE = 1
private const val PAGE_QUALITY = 2
private const val PAGE_READY = 3
private const val PAGE_COUNT = 4
internal const val ONBOARDING_PAGE_COUNT = PAGE_COUNT

/**
 * Первая часть онбординга: приветствие -> оформление -> качество звука -> знакомство с функциями.
 * Все выбранные значения применяются сразу (через те же репозитории, что и экраны настроек),
 * поэтому интерфейс прямо во время онбординга меняет акцентный цвет и язык.
 * [onContinue] ведёт на отдельный экран разрешений, [onSkip] пропускает всё и пускает в приложение.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(initialPage: Int = 0, onContinue: () -> Unit, onSkip: () -> Unit) {
    val strings = LocalStrings.current
    val pagerState = rememberPagerState(initialPage = initialPage) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val isFirst = page == PAGE_WELCOME
    val isLast = page == PAGE_COUNT - 1

    BackHandler(enabled = !isFirst) {
        scope.launch { pagerState.animateScrollToPage(page - 1) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Верхняя строка: «Пропустить» (на последней странице место остаётся пустым, чтобы ничего не прыгало)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isLast) {
                TextButton(onClick = onSkip) { Text(strings.onbSkip) }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            when (index) {
                PAGE_WELCOME -> WelcomePage()
                PAGE_APPEARANCE -> AppearancePage()
                PAGE_QUALITY -> QualityPage()
                else -> ReadyPage()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageDots(count = PAGE_COUNT, current = page)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isFirst) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                        modifier = Modifier.weight(1f),
                    ) { Text(strings.onbBack, maxLines = 1) }
                }
                Button(
                    onClick = {
                        if (isLast) onContinue()
                        else scope.launch { pagerState.animateScrollToPage(page + 1) }
                    },
                    modifier = Modifier.weight(if (isFirst) 1f else 2f),
                ) {
                    Text(strings.onbNext, maxLines = 1)
                }
            }
        }
    }
}

// ---- Pages ---------------------------------------------------------------------------

/** Контейнер страницы: контент по центру, а если не влезает (мелкий экран / большой шрифт) — скроллится. */
@Composable
private fun PageContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WelcomePage() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val language = LocalLanguage.current

    PageContainer {
        GammaIcon(
            background = MaterialTheme.colorScheme.primary,
            foreground = Color.White,
            size = 128.dp,
        )
        Spacer(Modifier.height(8.dp))
        PageTitle(strings.onbWelcomeTitle, strings.onbWelcomeSubtitle)
        Spacer(Modifier.height(8.dp))
        Text(
            strings.onbLanguageLabel,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Названия языков намеренно на родном языке, чтобы их можно было найти при любом выбранном.
            FilterChip(
                selected = language == AppLanguage.ENGLISH,
                onClick = { LocaleRepository.setLanguage(context, AppLanguage.ENGLISH) },
                label = { Text("English", maxLines = 1, softWrap = false) },
            )
            FilterChip(
                selected = language == AppLanguage.RUSSIAN,
                onClick = { LocaleRepository.setLanguage(context, AppLanguage.RUSSIAN) },
                label = { Text("Русский", maxLines = 1, softWrap = false) },
            )
        }
    }
}

@Composable
private fun AppearancePage() {
    val strings = LocalStrings.current
    val ui by UiSettingsRepository.settings.collectAsState()

    val coverShape = when (ui.coverStyle) {
        CoverStyle.SQUARE -> RoundedCornerShape(4.dp)
        CoverStyle.ROUNDED -> RoundedCornerShape(28.dp)
        CoverStyle.CIRCLE -> CircleShape
    }

    PageContainer {
        PageTitle(strings.onbAppearanceTitle, strings.onbAppearanceHint)

        // Живой предпросмотр: форма обложки и акцентный цвет меняются сразу
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(coverShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp),
            )
        }

        // Те же настройки, что и в Прочее → Оформление (обложка, перемотка, фон, акцент, иконка)
        AppearanceSettingsContent()
    }
}

@Composable
private fun QualityPage() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val settings by PlaybackSettingsRepository.settings.collectAsState()

    PageContainer {
        PageTitle(strings.onbQualityTitle, strings.onbQualityHint)

        listOf(
            Triple(AudioQuality.HIGH, strings.qualityHigh, strings.qualityHighDesc),
            Triple(AudioQuality.MEDIUM, strings.qualityMedium, strings.qualityMediumDesc),
            Triple(AudioQuality.LOW, strings.qualityLow, strings.qualityLowDesc),
        ).forEach { (quality, label, desc) ->
            val selected = settings.quality == quality
            val borderColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "qualityBorder",
            )
            val shape = RoundedCornerShape(16.dp)
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, borderColor, shape)
                    .clickable { PlaybackSettingsRepository.setQuality(context, quality) },
                shape = shape,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadyPage() {
    val strings = LocalStrings.current

    PageContainer {
        PageTitle(strings.onbReadyTitle, strings.onbReadySubtitle)

        FeatureRow(Icons.Default.OfflinePin, strings.onbFeatureOfflineTitle, strings.onbFeatureOfflineDesc)
        FeatureRow(Icons.Default.Group, strings.onbFeatureTogetherTitle, strings.onbFeatureTogetherDesc)
        FeatureRow(Icons.Default.GraphicEq, strings.onbFeatureEqTitle, strings.onbFeatureEqDesc)
        FeatureRow(Icons.Default.Lyrics, strings.onbFeatureLyricsTitle, strings.onbFeatureLyricsDesc)

        Text(
            strings.onbReadyHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- Small building blocks -----------------------------------------------------------

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
