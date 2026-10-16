package dev.nerzouille.koan.ui.kit

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.ToggleTokens

/**
 * Koan's switch. ON = the brand gradient track (slightly softened) with a white knob; OFF = a grey
 * track. The gradient flows in behind the knob as it springs across. Only draws per frame.
 */
@Composable
fun KoanToggle(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val pos = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) { pos.animateTo(if (checked) 1f else 0f, ToggleTokens.Spring) }
    val off = KoanTheme.colors.ToggleOff
    val interaction = remember { MutableInteractionSource() }
    val input = if (onCheckedChange == null) Modifier else Modifier.toggleable(
        value = checked,
        interactionSource = interaction,
        indication = null,
        role = Role.Switch,
        onValueChange = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onCheckedChange(it)
        },
    )
    androidx.compose.foundation.layout.Box(
        modifier
            .then(input)
            .size(ToggleTokens.Width, ToggleTokens.Height)
            .drawBehind {
                val p = pos.value
                val h = size.height
                val radius = CornerRadius(h / 2f)
                drawRoundRect(off, cornerRadius = radius)
                val inset = ToggleTokens.KnobInset.toPx()
                val r = h / 2f - inset
                // The knob overshoots a little with the spring; keep it inside the track.
                val cx = (inset + r + (size.width - 2f * (inset + r)) * p)
                    .coerceIn(inset + r, size.width - inset - r)
                // Gradient revealed from the left edge up to the knob's right side.
                val reveal = (p * 3f).coerceIn(0f, 1f)
                if (reveal > 0.001f) {
                    val track = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, h, radius)) }
                    clipPath(track) {
                        drawRect(
                            Brush.horizontalGradient(BrandTokens.Colors, startX = 0f, endX = size.width),
                            size = Size(cx + r + inset, h),
                            alpha = reveal * ToggleTokens.TRACK_ALPHA,
                        )
                    }
                }
                // Soft shadow under the knob, then the knob.
                val shadow = ToggleTokens.KnobShadow.toPx()
                val c = Offset(cx, h / 2f + shadow / 3f)
                drawCircle(
                    Brush.radialGradient(
                        0.75f to Color.Black.copy(alpha = ToggleTokens.KNOB_SHADOW_ALPHA),
                        1f to Color.Transparent,
                        center = c,
                        radius = r + shadow,
                    ),
                    radius = r + shadow,
                    center = c,
                )
                drawCircle(Color.White, radius = r, center = Offset(cx, h / 2f))
            },
    )
}
