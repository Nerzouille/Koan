package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.favicon.FaviconDiscovery
import dev.nerzouille.koan.favicon.IconStore
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.LocalPrivateBrowsing

/**
 * The one letter tile (docs/DESIGN_SYSTEM.md "Favicon"), shown until (or instead of) a real favicon,
 * the same everywhere: the site's first letter, white, on a muted tone picked from its host. The
 * colour is also used for the 8% tint of placeholder thumbnails.
 */
data class FaviconLook(val letters: String, val color: Color, val textColor: Color = Color.White)

/** Muted tones, picked by hash so a site always gets the same one. */
private val fallback = listOf(
    0xFF4F6D8F, 0xFF7A5C8E, 0xFF5E8C6A, 0xFF8E6A4F, 0xFF4F8A8B, 0xFF8B4F5E, 0xFF6B6F8F, 0xFF7F7A4A,
).map { Color(it) }

fun faviconLook(url: String): FaviconLook {
    // m.example.com and example.com are one site: same letter, same tone.
    val host = FaviconDiscovery.iconHost(displayHost(url))
    val name = host.split('.').let { if (it.size >= 2) it[it.size - 2] else it.firstOrNull().orEmpty() }
    val letter = name.firstOrNull()?.uppercase() ?: "•"
    return FaviconLook(letter, fallback[(host.hashCode() and 0x7fffffff) % fallback.size])
}

/**
 * A tab's icon: the Zen static icon if it has one, else the real favicon (synced inline by Zen or
 * fetched from the site), else a letter tile. An icon already in memory draws in the first frame;
 * one that has to load fades in over the letter tile, so cards never flash empty.
 */
@Composable
fun Favicon(url: String, size: Dp, modifier: Modifier = Modifier, icon: String? = null, staticIcon: Boolean = false) {
    if (staticIcon && !icon.isNullOrEmpty()) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            ZenIcon(icon, size, tint = KoanTheme.colors.TextPrimary)
        }
        return
    }
    val private = LocalPrivateBrowsing.current
    val key = remember(url, icon) { IconStore.keyFor(url, icon) }
    val initial = remember(key) { key?.let { IconStore.cached(it) } }
    var image by remember(key) { mutableStateOf(initial) }
    // 1 when the icon was in memory already: no fade, nothing to animate.
    val reveal = remember(key) { Animatable(if (initial != null) 1f else 0f) }
    if (image == null && key != null) {
        LaunchedEffect(key, private) {
            IconStore.load(url, icon, private)?.let {
                image = it
                reveal.animateTo(1f, tween(FADE_MS))
            }
        }
    }
    val shape = RoundedCornerShape(size * 0.28f)
    val backing = KoanTheme.colors.IconBacking
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val loaded = image
        // Dark icons get a light tile on dark surfaces, like Zen's light sidebar gives them.
        val backed = loaded != null && loaded.dark && backing.isSpecified
        // Composed only when the icon was not in memory; the fade is a layer property, so loading
        // never recomposes per frame.
        if (initial == null) LetterTile(url, size, shape, Modifier.graphicsLayer { alpha = 1f - reveal.value; compositingStrategy = CompositingStrategy.ModulateAlpha })
        if (loaded != null) {
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer { alpha = reveal.value; compositingStrategy = CompositingStrategy.ModulateAlpha }
                    .clip(shape)
                    .then(if (backed) Modifier.background(backing) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    loaded.image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.size(if (backed) size * 0.72f else size),
                )
            }
        }
    }
}

private const val FADE_MS = 140

@Composable
private fun LetterTile(url: String, size: Dp, shape: RoundedCornerShape, modifier: Modifier) {
    val look = faviconLook(url)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(look.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            look.letters,
            color = look.textColor,
            fontSize = (size.value * 0.55f).sp,
            // The tile is the line: no extra leading pushes the letter off centre.
            lineHeight = (size.value * 0.55f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
