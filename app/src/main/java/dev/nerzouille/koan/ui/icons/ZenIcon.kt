// Icon decoding ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.ui.icons

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
    val look = remember(clean) {
        emojiFromDataUrl(clean)?.let { ZenIconLook.Emoji(it) }
            ?: assetName(clean)?.let { name ->
                context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
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
