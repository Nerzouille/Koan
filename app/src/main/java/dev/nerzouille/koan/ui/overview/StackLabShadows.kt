package dev.nerzouille.koan.ui.overview

import android.graphics.BlendMode
import android.graphics.ComposeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * A stack card's shadows in a dev look ([StackLab]): a layer just under the card, same size and
 * transform ([modifier]), not clipped. [depth] is the card's depth, [contact] how far its contact
 * shadow rises onto the card behind (px, 0 = none); both read in draw and layers only.
 */
@Composable
fun LabShadows(style: StackStyle, depth: () -> Float, contact: () -> Float, modifier: Modifier) {
    val palette = KoanTheme.colors
    val dark = palette.isDark
    val shape = remember { RoundedCornerShape(CardTokens.Radius) }
    Box(modifier) {
        when (style) {
            StackStyle.Before, StackStyle.Tilt -> ContactRise(
                color = if (dark) LabTokens.ContactDark else LabTokens.ContactLight,
                length = if (dark) LabTokens.ContactLengthDark else LabTokens.ContactLengthLight,
                rise = contact,
                strength = { if (style == StackStyle.Before) StackLab.strength else 1f },
            )
            StackStyle.Cast -> {
                val around = remember(dark) {
                    Shadow(
                        radius = LabTokens.CastBlur,
                        color = if (dark) Color.Black else LabTokens.CastInk,
                        spread = LabTokens.CastSpread,
                        offset = DpOffset(0.dp, LabTokens.CastY),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val base = if (dark) LabTokens.CAST_ALPHA_DARK else LabTokens.CAST_ALPHA_LIGHT
                            alpha = (base * StackLab.strength).coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                        .dropShadow(shape, around),
                )
                ContactRise(
                    color = if (dark) LabTokens.CastContactDark else LabTokens.CastContactLight,
                    length = LabTokens.CastContactLength,
                    rise = contact,
                    strength = { StackLab.strength },
                )
            }
            StackStyle.Haze -> {
                val fog = remember(dark) {
                    Shadow(
                        radius = LabTokens.HazeBlur,
                        color = if (dark) palette.Base else LabTokens.HazeLight,
                        spread = LabTokens.HazeSpread,
                        offset = DpOffset(0.dp, LabTokens.HazeY),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // The fog around this card lies over the one behind it: as dense as that card is deep.
                            val behind = depth() + 1f
                            alpha = (DepthCurve.lerp(LabTokens.Haze, behind.coerceAtLeast(0f)) * StackLab.strength).coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                        .dropShadow(shape, fog),
                )
            }
        }
    }
}

/**
 * beta.1's contact shadow: from under the card it rises [rise]() px above its top edge onto the
 * card behind, fading out there (alpha ∝ t²); its ends fade before the rounded corners.
 */
@Composable
private fun ContactRise(color: Color, length: Dp, rise: () -> Float, strength: () -> Float) {
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val len = length.toPx()
            // Strength rebuilds the cache (a slider move), never a frame of the stack.
            val c = color.copy(alpha = (color.alpha * strength()).coerceIn(0f, 1f))
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

private val RISE_STOPS = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/** How far a dev look's contact shadow rises (Cast: longer, over more of the sliver). */
internal fun labContactLength(style: StackStyle, dark: Boolean): Pair<Dp, Float> = when (style) {
    StackStyle.Cast -> LabTokens.CastContactLength to LabTokens.CAST_CONTACT_MAX_FRACTION
    StackStyle.Haze -> 0.dp to 0f
    else -> (if (dark) LabTokens.ContactLengthDark else LabTokens.ContactLengthLight) to LabTokens.CONTACT_MAX_FRACTION
}
