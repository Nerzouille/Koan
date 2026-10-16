package dev.nerzouille.koan.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp

/*
 * The brand gradient as a design-language element: Koan is working, active, or holds your focus.
 * Allowed and forbidden places: docs/UI_SPEC.md "Brand gradient".
 */

/** Left → right across whatever it fills. */
val BrandHorizontal: Brush = Brush.horizontalGradient(BrandTokens.Colors)

/** A gradient outline around something that holds your focus (first-run card, account card). */
fun Modifier.brandOutline(shape: Shape, width: Dp = BrandTokens.Outline): Modifier =
    border(width, BrandHorizontal, shape)

/** Syncing: a gradient arc that turns. Turns in a draw lambda only, no recomposition per frame. */
@Composable
fun BrandSpinner(size: Dp, modifier: Modifier = Modifier) {
    val turn = rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(BrandTokens.SPINNER_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    Box(
        modifier.size(size).drawBehind {
            val stroke = BrandTokens.SpinnerStroke.toPx()
            val inset = stroke / 2f
            rotate(turn.value) {
                drawArc(
                    brush = Brush.sweepGradient(BrandTokens.Colors + BrandTokens.Colors.first()),
                    startAngle = 0f,
                    sweepAngle = BrandTokens.SPINNER_SWEEP,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        },
    )
}
