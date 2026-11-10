package io.github.plpp555.spectracker.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val radiusDp: Float, val alpha: Float, val twinkles: Boolean)

/** Ultra 端末用の背景。画像ファイルは使わず、星雲・天の川・星の瞬きを描画で表現する。 */
@Composable
fun GalaxyBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val random = Random(2024) // 固定シードで、いつも同じ星空にする
        List(280) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radiusDp = 0.4f + random.nextFloat() * random.nextFloat() * 1.8f,
                alpha = 0.35f + random.nextFloat() * 0.65f,
                twinkles = random.nextInt(3) == 0,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "twinkle")
    val twinkle by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkleAlpha",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF04051A), Color(0xFF130C36), Color(0xFF05020F))),
        )
        nebula(0.15f, 0.20f, 0.95f, Color(0xFF6A3DE8), 0.55f)
        nebula(0.90f, 0.45f, 0.85f, Color(0xFF1E88E5), 0.45f)
        nebula(0.35f, 0.85f, 0.95f, Color(0xFFD81B8A), 0.40f)

        // 天の川: 斜めに走る淡い光の帯
        rotate(degrees = -28f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.30f to Color.Transparent,
                    0.45f to Color(0x33B39DFF),
                    0.50f to Color(0x55FFFFFF),
                    0.55f to Color(0x33B39DFF),
                    0.70f to Color.Transparent,
                ),
                topLeft = Offset(-size.width * 0.4f, 0f),
                size = Size(size.width * 1.8f, size.height),
            )
        }

        for (star in stars) {
            val alpha = star.alpha * (if (star.twinkles) twinkle else 1f)
            drawCircle(
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = star.radiusDp * density,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

private fun DrawScope.nebula(cx: Float, cy: Float, radiusRatio: Float, color: Color, alpha: Float) {
    val center = Offset(size.width * cx, size.height * cy)
    val radius = size.maxDimension * radiusRatio * 0.5f
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}
