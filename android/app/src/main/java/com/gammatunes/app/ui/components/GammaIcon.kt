package com.gammatunes.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Геометрия иконки приложения: точка в центре и три волны, расходящиеся от неё вверх.
 * Координаты заданы в системе 108x108 (как у adaptive-иконки) и ДОЛЖНЫ совпадать
 * с res/drawable/ic_launcher_foreground.xml.
 */
object GammaIconGeometry {
    const val VIEWPORT = 108f
    const val CENTER_X = 54f
    const val CENTER_Y = 67f
    const val DOT_RADIUS = 5f
    const val STROKE_WIDTH = 4.5f
    val WAVE_RADII = floatArrayOf(12f, 21f, 30f)

    /** Волна занимает сектор ±50° от вертикали (0° в Compose/Canvas — «на 3 часа», вверх = -90°). */
    const val WAVE_START_ANGLE = -140f
    const val WAVE_SWEEP_ANGLE = 100f

    /** Цвета по умолчанию: красный фон, белая иконка. */
    const val DEFAULT_BACKGROUND = 0xFFEA3323L
    const val DEFAULT_FOREGROUND = 0xFFFFFFFFL
}

/** Иконка приложения, нарисованная вектором. Цвета фона и самой иконки задаются отдельно. */
@Composable
fun GammaIcon(
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    cornerRadius: Dp = size * 0.28f,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background),
    ) {
        val scale = this.size.minDimension / GammaIconGeometry.VIEWPORT
        val center = Offset(
            GammaIconGeometry.CENTER_X * scale,
            GammaIconGeometry.CENTER_Y * scale,
        )
        drawCircle(
            color = foreground,
            radius = GammaIconGeometry.DOT_RADIUS * scale,
            center = center,
        )
        GammaIconGeometry.WAVE_RADII.forEach { r ->
            val rr = r * scale
            drawArc(
                color = foreground,
                startAngle = GammaIconGeometry.WAVE_START_ANGLE,
                sweepAngle = GammaIconGeometry.WAVE_SWEEP_ANGLE,
                useCenter = false,
                topLeft = Offset(center.x - rr, center.y - rr),
                size = Size(rr * 2f, rr * 2f),
                style = Stroke(
                    width = GammaIconGeometry.STROKE_WIDTH * scale,
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}
