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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.nerzouille.koan.favicon.IconStore
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.theme.KoanTheme

/**
 * Letter tile shown until (or instead of) a real favicon. The colour is also used for the 8% tint of
 * placeholder thumbnails.
 */
data class FaviconLook(val letters: String, val color: Color, val textColor: Color = Color.White)

private val known = mapOf(
    "github.com" to FaviconLook("gh", Color(0xFF24292F)),
    "mail.google.com" to FaviconLook("M", Color(0xFFF2F2F2), Color(0xFFD93025)),
    "calendar.google.com" to FaviconLook("31", Color(0xFFF2F2F2), Color(0xFF1A73E8)),
    "docs.google.com" to FaviconLook("S", Color(0xFF1E8E3E)),
    "drive.google.com" to FaviconLook("D", Color(0xFF1A73E8)),
    "notion.so" to FaviconLook("N", Color(0xFFF2F2F2), Color(0xFF111111)),
    "figma.com" to FaviconLook("F", Color(0xFFA259FF)),
    "linear.app" to FaviconLook("L", Color(0xFF5E6AD2)),
    "app.slack.com" to FaviconLook("S", Color(0xFF4A154B)),
    "chat.openai.com" to FaviconLook("AI", Color(0xFF10A37F)),
    "youtube.com" to FaviconLook("▶", Color(0xFFFF0000)),
    "reddit.com" to FaviconLook("r", Color(0xFFFF4500)),
    "open.spotify.com" to FaviconLook("S", Color(0xFF1DB954), Color.Black),
    "news.ycombinator.com" to FaviconLook("Y", Color(0xFFFF6600)),
    "tailwindcss.com" to FaviconLook("~", Color(0xFF0EA5E9)),
    "overleaf.com" to FaviconLook("O", Color(0xFF47A141)),
    "arxiv.org" to FaviconLook("X", Color(0xFFB31B1B)),
    "wikipedia.org" to FaviconLook("W", Color(0xFFF2F2F2), Color.Black),
    "en.wikipedia.org" to FaviconLook("W", Color(0xFFF2F2F2), Color.Black),
    "stackoverflow.com" to FaviconLook("so", Color(0xFFF48024)),
    "vercel.com" to FaviconLook("▲", Color(0xFF111111)),
    "docs.stripe.com" to FaviconLook("S", Color(0xFF635BFF)),
)

/** Muted tones for unknown hosts, picked by hash so a host always gets the same one. */
private val fallback = listOf(
    0xFF4F6D8F, 0xFF7A5C8E, 0xFF5E8C6A, 0xFF8E6A4F, 0xFF4F8A8B, 0xFF8B4F5E, 0xFF6B6F8F, 0xFF7F7A4A,
).map { Color(it) }

fun faviconLook(url: String): FaviconLook {
    val host = displayHost(url)
    known[host]?.let { return it }
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
    val key = remember(url, icon) { IconStore.keyFor(url, icon) }
    val initial = remember(key) { key?.let { IconStore.cached(it) } }
    var image by remember(key) { mutableStateOf(initial) }
    // 1 when the icon was in memory already: no fade, nothing to animate.
    val reveal = remember(key) { Animatable(if (initial != null) 1f else 0f) }
    if (image == null && key != null) {
        LaunchedEffect(key) {
            IconStore.load(url, icon)?.let {
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
            fontSize = (size.value * if (look.letters.length > 1) 0.42f else 0.55f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
