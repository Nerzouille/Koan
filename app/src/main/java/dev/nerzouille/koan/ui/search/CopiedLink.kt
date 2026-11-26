package dev.nerzouille.koan.ui.search

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassifier
import androidx.compose.runtime.Immutable
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.theme.SearchTokens

/** A link on the clipboard, offered as "Open link you copied" ([host] shown under it). */
@Immutable
data class CopiedLink(val url: String, val host: String)

/**
 * "Open link you copied": looks at the clipboard when the search field or the start page opens.
 * Only the clip's description (its type and when it was copied) is read first: that never shows
 * Android's "pasted from your clipboard" notice. The text itself is read only for a clip copied in
 * the last [SearchTokens.COPIED_LINK_MAX_AGE_MS] that Android already classed as a link (or a
 * uri-list), so the notice shows only when there is a link to offer. A link is offered once: not
 * again the next time the field opens, nor when it is the page already open.
 */
class CopiedLinks(private val context: Context) {
    private var offered: String? = null

    /** The link to offer now, if any; [current]: the open page's address. */
    fun check(current: String?): CopiedLink? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val description = clipboard?.let { runCatching { it.primaryClipDescription }.getOrNull() }
        val url = description?.takeIf { worthReading(it, System.currentTimeMillis()) }
            ?.let { runCatching { clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() }.getOrNull() }
            ?.let(::linkIn)
            ?.takeIf { it != offered && !sameUrl(it, current) }
            ?: return null
        offered = url
        return CopiedLink(url, displayHost(url))
    }

    companion object {
        /** From the description alone: copied lately, and a link (or possibly one, before Android 12). */
        fun worthReading(d: ClipDescription, now: Long): Boolean {
            val age = now - d.timestamp
            val recent = age in 0..SearchTokens.COPIED_LINK_MAX_AGE_MS
            val text = d.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || d.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
            return recent && (d.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) || (text && classedAsLink(d)))
        }

        /**
         * Android 12+ shows a notice on every read: only when it already knows the text is a link.
         * Android 11 has no notice (and no classification): the text is read and checked here.
         */
        private fun classedAsLink(d: ClipDescription): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                d.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE &&
                d.getConfidenceScore(TextClassifier.TYPE_URL) >= SearchTokens.COPIED_LINK_MIN_CONFIDENCE

        /** [text] as a web address, if it is one and nothing else (one word, http(s) or a host with a dot). */
        fun linkIn(text: String): String? {
            val t = text.trim()
            val oneWord = t.isNotEmpty() && t.length <= SearchTokens.COPIED_LINK_MAX_LENGTH && t.none { it.isWhitespace() }
            val web = t.startsWith("https://", ignoreCase = true) || t.startsWith("http://", ignoreCase = true)
            val url = when {
                !oneWord -> null
                web -> t
                t.contains("://") -> null
                else -> "https://$t"
            }
            val host = url?.let { runCatching { java.net.URI(it).host }.getOrNull() }
            return url?.takeIf { host != null && host.contains('.') && !host.startsWith('.') && !host.endsWith('.') }
        }

        private fun sameUrl(a: String, b: String?): Boolean =
            b != null && a.trimEnd('/').equals(b.trimEnd('/'), ignoreCase = true)
    }
}
