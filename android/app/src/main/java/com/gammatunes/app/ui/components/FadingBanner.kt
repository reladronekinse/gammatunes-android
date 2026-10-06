package com.gammatunes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Полная высота баннера: полоса статус-бара + [body].
 * Экраны с баннером рисуются от самого верха экрана (под статус-баром).
 */
@Composable
fun bannerHeight(body: Dp): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + body

/** Насколько прокручен список в пикселях (для баннера, который едет вместе со списком). */
fun LazyListState.bannerScrollPx(): Float =
    if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset.toFloat() else 100_000f

/**
 * 0f — баннер на виду, 1f — баннер почти уехал. Нужен, чтобы плавно проявлять
 * фон и заголовок верхней панели.
 */
@Composable
fun LazyListState.bannerBarAlpha(height: Dp): Float {
    val thresholdPx = with(LocalDensity.current) { (height - 72.dp).toPx().coerceAtLeast(1f) }
    val value by remember(this, thresholdPx) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) {
                1f
            } else {
                ((firstVisibleItemScrollOffset - 0.5f * thresholdPx) / (0.5f * thresholdPx)).coerceIn(0f, 1f)
            }
        }
    }
    return value
}

/**
 * Баннер сверху экрана: картинка плавно растворяется в фон приложения
 * (через маску прозрачности, а не цветной оверлей — под ним может быть любой фон).
 *
 * @param blurRadius > 0 — картинка размывается (для квадратных обложек).
 * @param foreground  контент поверх баннера (имя, обложка и т.п.).
 */
@Composable
fun FadingBanner(
    imageUrl: String?,
    height: Dp,
    scrollPx: () -> Float,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 0.dp,
    foreground: @Composable BoxScope.() -> Unit = {},
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { translationY = -scrollPx() },
    ) {
        // Слой с картинкой + маска: низ плавно уходит в прозрачность.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.45f to Color.Black,
                                0.65f to Color.Black.copy(alpha = 0.72f),
                                0.82f to Color.Black.copy(alpha = 0.30f),
                                0.93f to Color.Black.copy(alpha = 0.08f),
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurRadius > 0.dp) {
                                // Увеличиваем до размытия, чтобы затемнённые края ушли за рамку.
                                Modifier
                                    .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                                    .blur(blurRadius)
                            } else Modifier,
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.08f)),
                            ),
                        ),
                )
            }
        }

        // Лёгкое затемнение сверху, чтобы стрелка «назад» читалась на любой картинке.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusTop + 80.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent),
                    ),
                ),
        )

        foreground()
    }
}
