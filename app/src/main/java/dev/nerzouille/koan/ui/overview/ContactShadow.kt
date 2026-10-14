package dev.nerzouille.koan.ui.overview

import android.graphics.BlendMode
import android.graphics.ComposeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * The short, soft shadow a card casts upward onto the card behind it. A layer of its own, drawn
 * just under the casting card with the same size and transform ([modifier]), so it moves with the
 * card through every position. It starts under the card, rises [length] above its top edge and
 * fades out there; its ends fade before the rounded corners, so no edge shows anywhere.
 * [length] returns the rise in the card's own px (0 = nothing to draw), read at draw time only.
 */
@Composable
fun ContactShadow(length: () -> Float, modifier: Modifier) {
    val palette = KoanTheme.colors
    Box(
        modifier.drawWithCache {
            val len = palette.ContactShadowLength.toPx()
            val radius = CardTokens.Radius.toPx()
            val c = palette.ContactShadow
            // Vertical: eases in towards the edge (alpha ∝ t²), full strength under the card.
            val rise = LinearGradientShader(
                from = Offset(0f, -len),
                to = Offset(0f, 0f),
                colors = RISE_STOPS.map { c.copy(alpha = c.alpha * it * it) },
                colorStops = RISE_STOPS,
            )
            // Horizontal: fades out where the top edge curves into the corners.
            val w = size.width
            val ends = LinearGradientShader(
                from = Offset(0f, 0f),
                to = Offset(w, 0f),
                colors = listOf(Color.Transparent, Color.Black, Color.Black, Color.Transparent),
                colorStops = listOf(0.5f * radius / w, 1.5f * radius / w, 1f - 1.5f * radius / w, 1f - 0.5f * radius / w),
            )
            val brush = ShaderBrush(ComposeShader(rise, ends, BlendMode.DST_IN))
            onDrawBehind {
                val h = length()
                if (h < 0.5f) return@onDrawBehind
                // Shorter rise where the pile leaves only a sliver of the card behind.
                scale(scaleX = 1f, scaleY = h / len, pivot = Offset.Zero) {
                    drawRect(brush, topLeft = Offset(0f, -len), size = Size(w, len))
                }
                // Full strength down to the corner radius: under the card wherever the ends are not
                // faded out, so the shadow meets the card's own rounded edge, never a cut.
                drawRect(brush, size = Size(w, radius))
            }
        },
    )
}

private val RISE_STOPS = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
