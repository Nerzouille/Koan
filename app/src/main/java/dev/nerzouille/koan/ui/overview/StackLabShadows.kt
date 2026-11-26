package dev.nerzouille.koan.ui.overview

import android.graphics.BlendMode
import android.graphics.ComposeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * A stack card's shadows in its look ([StackLab]): a layer just under the card, same size and
 * transform ([modifier]), not clipped. [contact] is how far its contact shadow rises onto the card
 * behind (px, 0 = none), read in draw only.
 */
@Composable
fun LabShadows(style: StackStyle, contact: () -> Float, modifier: Modifier) {
    val palette = KoanTheme.colors
    val dark = palette.isDark
    val shape = remember { RoundedCornerShape(CardTokens.Radius) }
    Box(modifier) {
        when (style) {
            StackStyle.Before -> ContactRise(
                color = if (dark) LabTokens.ContactDark else LabTokens.ContactLight,
                length = if (dark) LabTokens.ContactLengthDark else LabTokens.ContactLengthLight,
                rise = contact,
            )
            StackStyle.Cast -> {
                val around = remember(dark) {
                    Shadow(
                        radius = LabTokens.CastBlur,
                        color = if (dark) LabTokens.CastAroundDark else LabTokens.CastAroundLight,
                        spread = LabTokens.CastSpread,
                        offset = DpOffset(0.dp, LabTokens.CastY),
                    )
                }
                Box(Modifier.fillMaxSize().dropShadow(shape, around))
                ContactRise(
                    color = if (dark) LabTokens.CastContactDark else LabTokens.CastContactLight,
                    length = LabTokens.CastContactLength,
                    rise = contact,
                )
                Thickness(if (dark) LabTokens.ThicknessDark else LabTokens.ThicknessLight)
            }
        }
    }
}

/**
 * beta.1's contact shadow: from under the card it rises [rise]() px above its top edge onto the
 * card behind, fading out there (alpha ∝ t²); its ends fade before the rounded corners.
 */
@Composable
private fun ContactRise(color: Color, length: Dp, rise: () -> Float) {
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val len = length.toPx()
            val c = color
            val radius = CardTokens.Radius.toPx()
            val shade = LinearGradientShader(
                from = Offset(0f, -len),
                to = Offset(0f, 0f),
                colors = RISE_STOPS.map { c.copy(alpha = c.alpha * it * it) },
                colorStops = RISE_STOPS,
            )
            val w = size.width
            val ends = LinearGradientShader(
                from = Offset(0f, 0f),
                to = Offset(w, 0f),
                colors = listOf(Color.Transparent, Color.Black, Color.Black, Color.Transparent),
                colorStops = listOf(0.5f * radius / w, 1.5f * radius / w, 1f - 1.5f * radius / w, 1f - 0.5f * radius / w),
            )
            val brush = ShaderBrush(ComposeShader(shade, ends, BlendMode.DST_IN))
            onDrawBehind {
                val h = rise()
                if (h < 0.5f) return@onDrawBehind
                scale(scaleX = 1f, scaleY = h / len, pivot = Offset.Zero) {
                    drawRect(brush, topLeft = Offset(0f, -len), size = Size(w, len))
                }
                drawRect(brush, size = Size(w, radius))
            }
        },
    )
}

/**
 * The card's thickness: its shape, [LabTokens.Thickness] higher, under the card,
 * so only a thin darker band shows just above its top edge, in the strip of the card behind.
 */
@Composable
private fun Thickness(color: Color) {
    Box(
        Modifier.fillMaxSize().drawBehind {
            val r = CardTokens.Radius.toPx()
            drawRoundRect(
                color,
                topLeft = Offset(0f, -LabTokens.Thickness.toPx()),
                size = size,
                cornerRadius = CornerRadius(r, r),
            )
        },
    )
}

private val RISE_STOPS = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/** How far a look's contact shadow rises (Cast: longer, over more of the sliver). */
internal fun labContactLength(style: StackStyle, dark: Boolean): Pair<Dp, Float> = when (style) {
    StackStyle.Cast -> LabTokens.CastContactLength to LabTokens.CAST_CONTACT_MAX_FRACTION
    StackStyle.Before -> (if (dark) LabTokens.ContactLengthDark else LabTokens.ContactLengthLight) to LabTokens.CONTACT_MAX_FRACTION
}
