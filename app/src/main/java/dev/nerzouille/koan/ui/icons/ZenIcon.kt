// Icon decoding ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.ui.icons

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import java.util.Base64

/**
 * A Zen icon as the desktop shows it: an emoji (plain or wrapped in Zen's emoji-as-SVG data URL)
 * or one of Zen's selectable vector icons (`chrome://browser/skin/zen-icons/selectable/<name>.svg`,
 * shipped as `zen_<name>` drawables). Draws nothing for an empty or unknown icon.
 */
@SuppressLint("DiscouragedApi") // Zen names its icons at runtime; the drawable set is fixed.
@Composable
fun ZenIcon(icon: String?, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    val clean = icon?.trim().orEmpty()
    if (clean.isEmpty()) return
    val context = LocalContext.current
    // A Zia mod icon: drawn from its bundled Tabler icons (off the main thread, once per icon).
    if (ZiaIcons.isZia(clean)) {
        val px = with(LocalDensity.current) { (size * 0.82f).roundToPx() }
        val image by produceState<ImageBitmap?>(null, clean, px) {
            value = withContext(Dispatchers.Default) { ZiaIcons.bitmap(context, clean, px)?.asImageBitmap() }
        }
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            image?.let { Image(it, null, Modifier.size(size * 0.82f), colorFilter = ColorFilter.tint(tint)) }
        }
        return
    }
    val look = remember(clean) {
        emojiFromDataUrl(clean)?.let { ZenIconLook.Emoji(it) }
            ?: assetName(clean)?.let { name ->
                context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
                    // A name Koan doesn't ship (a newer Zen, a mod's own name): the closest one by spelling.
                    ?: ZiaIcons.closest(name.removePrefix("zen_"), ZEN_ICON_NAMES)?.let { near ->
                        context.resources.getIdentifier("zen_$near", "drawable", context.packageName).takeIf { it != 0 }
                    }
            }?.let { ZenIconLook.Vector(it) }
            ?: clean.takeIf { looksLikeEmoji(it) }?.let { ZenIconLook.Emoji(it) }
    } ?: return
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (look) {
            is ZenIconLook.Emoji -> Text(look.text, fontSize = (size.value * 0.78f).sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
            is ZenIconLook.Vector -> Image(painterResource(look.res), null, Modifier.size(size * 0.82f), colorFilter = ColorFilter.tint(tint))
        }
    }
}

/** Zen's selectable icons Koan ships (res/drawable/zen_<name>.xml), for the closest-name fallback. */
private val ZEN_ICON_NAMES = listOf("airplane", "american_football", "baseball", "basket", "bed", "bell", "bookmark", "book", "briefcase", "brush", "bug", "build", "cafe", "call", "card", "chat", "checkbox", "circle", "cloud", "code", "coins", "construct", "cutlery", "egg", "extension_puzzle", "eye", "fast_food", "fish", "flag", "flame", "flask", "folder", "game_controller", "globe_1", "globe", "grid_2x2", "grid_3x3", "heart", "ice_cream", "image", "inbox", "key", "layers", "leaf", "lightning", "location", "lock_closed", "logo_github", "logo_rss", "logo_usd", "mail", "map", "megaphone", "moon", "music", "navigate", "nuclear", "page", "palette", "paw", "people", "pizza", "planet", "present", "rocket", "school", "shapes", "shirt", "skull", "squares", "square", "star_1", "star", "stats_chart", "sun", "tada", "terminal", "ticket", "time", "trash", "triangle", "video", "volume_high", "wallet", "warning", "water", "weight")

private sealed interface ZenIconLook {
    data class Emoji(val text: String) : ZenIconLook
    data class Vector(val res: Int) : ZenIconLook
}

/** A short plain-text icon (an emoji); never a URL or a base64 blob. */
private fun looksLikeEmoji(s: String) = s.length <= 16 && !s.contains(':') && !s.contains('/')

/** Port of Z-Sync `assetName`: `…/selectable/baseball.svg` → `zen_baseball`. */
internal fun assetName(raw: String): String? {
    if (!raw.contains('/') && !raw.endsWith(".svg")) return null
    val base = raw.substringAfterLast('/').removeSuffix(".svg").removeSuffix(".png").trim()
    if (base.isEmpty() || !base.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return null
    return "zen_${base.replace('-', '_')}"
}

private const val EMOJI_SVG_PREFIX = "data:image/svg+xml;base64,"
private val TEXT_NODE = Regex("<text[^>]*>(.*?)</text", RegexOption.DOT_MATCHES_ALL)
private val XML_ESCAPES = listOf("&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&#39;" to "'", "&amp;" to "&")

/** Port of Z-Sync `emojiFromDataUrl`: the emoji inside Zen's emoji-as-SVG data URL. */
internal fun emojiFromDataUrl(icon: String): String? {
    if (!icon.startsWith(EMOJI_SVG_PREFIX)) return null
    val bytes = runCatching { Base64.getMimeDecoder().decode(icon.removePrefix(EMOJI_SVG_PREFIX)) }.getOrNull() ?: return null
    val match = TEXT_NODE.find(String(bytes, Charsets.UTF_8)) ?: return null
    return XML_ESCAPES.fold(match.groupValues[1]) { acc, (escaped, raw) -> acc.replace(escaped, raw) }
        .trim().takeIf { it.isNotEmpty() }
}
