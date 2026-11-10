package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.core.Animatable
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
import dev.nerzouille.koan.favicon.IconStore
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PasswordTokens

/**
 * A site's real favicon, bare (no tile, no backing), in greyscale with a little less contrast so it
 * sits with plain glyphs; [colour] true crossfades it to its own colours. No favicon (or not loaded
 * yet): [fallback], drawn like a row glyph. Never a letter tile.
 */
@Composable
fun SiteIcon(url: String, size: Dp, fallback: ImageVector, colour: Boolean = false, modifier: Modifier = Modifier) {
    val key = remember(url) { IconStore.keyFor(url, null) }
    var icon by remember(key) { mutableStateOf(key?.let { IconStore.cached(it) }) }
    val reveal = remember(key) { Animatable(if (icon != null) 1f else 0f) }
    if (icon == null && key != null) {
        LaunchedEffect(key) {
            IconStore.load(url, null)?.let {
                icon = it
                reveal.animateTo(1f, tween(PasswordTokens.COLOUR_MS))
            }
        }
    }
    val tint by animateFloatAsState(if (colour) 1f else 0f, tween(PasswordTokens.COLOUR_MS), label = "colour")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val loaded = icon
        if (loaded == null) {
            Icon(fallback, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(size))
            return@Box
        }
        // Grey underneath, colour over it: the crossfade is a layer's alpha, never a recomposition.
        Image(
            loaded.image, null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Medium,
            colorFilter = GREY,
            modifier = Modifier.size(size).graphicsLayer { alpha = reveal.value * (1f - tint) },
        )
        Image(
            loaded.image, null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Medium,
            modifier = Modifier.size(size).graphicsLayer { alpha = reveal.value * tint },
        )
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
