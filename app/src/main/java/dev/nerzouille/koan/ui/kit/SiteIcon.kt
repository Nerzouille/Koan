package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.favicon.SiteGlyph
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.BitmapPainter
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PasswordTokens

/**
 * A site's favicon as the Passwords pages show it: only one already on the phone (never fetched,
 * [SiteGlyph]), cut to its drawing so every icon fills the same [size] box, in greyscale with a
 * little less contrast so it sits with plain glyphs; an opaque tile keeps its square, with rounded
 * corners. [colour] true crossfades it to its own colours. No usable icon (none on the phone, or
 * one that can't be seen on this page): [fallback], drawn like a row glyph. Never a letter tile.
 */
@Composable
fun SiteIcon(url: String, size: Dp, fallback: ImageVector, colour: Boolean = false, modifier: Modifier = Modifier) {
    var glyph by remember(url) { mutableStateOf(SiteGlyph.cached(url)) }
    LaunchedEffect(url) { if (glyph == null) glyph = SiteGlyph.load(url) }
    val tint by animateFloatAsState(if (colour) 1f else 0f, tween(PasswordTokens.COLOUR_MS), label = "colour")
    val dark = KoanTheme.colors.isDark
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val g = glyph?.takeIf { if (dark) it.seenOnDark else it.seenOnLight }
        if (g == null) {
            Icon(fallback, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(size))
            return@Box
        }
        val painter = remember(g) { BitmapPainter(g.image, g.src, g.size, FilterQuality.Medium) }
        val shape = if (g.tile) RoundedCornerShape(size * PasswordTokens.TILE_RADIUS) else RectangleShape
        Box(Modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
            // Grey underneath, colour over it: the crossfade is a layer's alpha, never a recomposition.
            Image(
                painter, null,
                contentScale = if (g.tile) ContentScale.Crop else ContentScale.Fit,
                colorFilter = GREY,
                modifier = Modifier.size(size).graphicsLayer { alpha = 1f - tint },
            )
            Image(
                painter, null,
                contentScale = if (g.tile) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.size(size).graphicsLayer { alpha = tint },
            )
        }
    }
}

/** No saturation, contrast scaled around mid-grey ([PasswordTokens.GREY_CONTRAST]). */
private val GREY: ColorFilter = run {
    val c = PasswordTokens.GREY_CONTRAST
    val grey = ColorMatrix().apply { setToSaturation(0f) }
    val shift = 128f * (1f - c)
    val contrast = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, shift,
            0f, c, 0f, 0f, shift,
            0f, 0f, c, 0f, shift,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    grey.timesAssign(contrast)
    ColorFilter.colorMatrix(grey)
}
