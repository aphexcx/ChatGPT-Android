package cx.aphex.chatgpt.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DotsLoadingIndicator(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val squareSize = 8.dp.toPx()
        val spacing = 8.dp.toPx()
        val yOffset = (size.height - squareSize) / 2

        for (i in 0..2) {
            val xOffset = -1 * i * (squareSize + spacing)
            val squareAlpha = (alpha + i * 0.33f) % 1f
            drawRect(
                color = color.copy(alpha = squareAlpha),
                topLeft = Offset(xOffset, yOffset),
                size = Size(squareSize, squareSize)
            )
        }
    }
}