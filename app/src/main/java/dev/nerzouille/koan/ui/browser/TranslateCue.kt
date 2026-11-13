package dev.nerzouille.koan.ui.browser

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.search.SearchEngine
import java.net.URI

/*
 * When Koan offers a translation (owner, 2026-11-12: the card alone was too pushy). By default only
 * a small translate icon in the address pill, for a page in a language that isn't on the phone; a
 * tap on it opens the card. The card opens by itself only where it helps: real readable text, a
 * confident detection, not a search engine's results, and not on a site where it was dismissed
 * since Koan started. "Offer to translate pages" off: neither.
 */

/** What a page gets: nothing, the icon in the address pill, or the icon and the card. */
enum class TranslateCue { None, Icon, Card }

/** What Gecko and Koan know about the page, for [translateCue]. */
data class TranslateSignals(
    /** Gecko's detected page language, when Gecko can translate it; null otherwise. */
    val pageLanguage: String?,
    /**
     * The language is sure: Gecko gives a page language only from the page's own declaration or a
     * confident detection. (Not Gecko's own offer flag: Gecko offers once per site and session, so
     * an article after a short page of the same site never got its card.)
     */
    val confident: Boolean,
    /** Firefox's reader-view check: an article-length text, not mostly links, forms or media. */
    val readable: Boolean,
    val translated: Boolean,
    val processing: Boolean,
    /** Always translated (it happens on its own), or never for this language / site. */
    val always: Boolean = false,
    val never: Boolean = false,
)

/**
 * The cue for a page: [phoneLanguages] the phone's languages, [offerOn] the Settings switch,
 * [searchPage] a search engine's results, [siteDismissed] the card closed on this site since
 * Koan started.
 */
fun translateCue(s: TranslateSignals, phoneLanguages: List<String>, offerOn: Boolean, searchPage: Boolean, siteDismissed: Boolean): TranslateCue {
    val lang = s.pageLanguage ?: return TranslateCue.None
    if (!offerFor(lang, phoneLanguages, offerOn) || s.translated || s.always || s.never) return TranslateCue.None
    val worthACard = s.confident && s.readable && !searchPage && !siteDismissed && !s.processing
    return if (worthACard) TranslateCue.Card else TranslateCue.Icon
}

/**
 * Whether [url] is a results page of one of [engines]: the engine's site (any country domain:
 * google.fr is Google) and path, with its query parameter filled in (or, for an engine whose
 * query sits in the path, that path with a query after it).
 */
fun isSearchResults(url: String, engines: List<SearchEngine>): Boolean {
    val page = runCatching { URI(url) }.getOrNull() ?: return false
    val pageHost = page.host?.lowercase() ?: return false
    return engines.any { e ->
        val t = runCatching { URI(e.searchTemplate.replace("%s", MARK)) }.getOrNull() ?: return@any false
        val host = t.host?.lowercase() ?: return@any false
        if (siteCore(host) != siteCore(pageHost)) return@any false
        val path = t.rawPath.orEmpty()
        if (MARK in path) {
            val before = path.substringBefore(MARK)
            return@any page.rawPath.orEmpty().let { it.startsWith(before) && it.length > before.length }
        }
        if (page.rawPath.orEmpty().trimEnd('/') != path.trimEnd('/')) return@any false
        val key = query(t.rawQuery).entries.firstOrNull { MARK in it.value }?.key ?: return@any false
        query(page.rawQuery)[key].orEmpty().isNotBlank()
    }
}

private const val MARK = "KOANQUERY"

/** "www.google.co.uk" → "google", "search.brave.com" → "search.brave". */
internal fun siteCore(host: String): String {
    var labels = host.removePrefix("www.").split('.')
    if (labels.size >= 2) labels = labels.dropLast(1)
    if (labels.size >= 2 && labels.last() in SECOND_LEVEL) labels = labels.dropLast(1)
    return labels.joinToString(".")
}

private val SECOND_LEVEL = setOf("co", "com", "net", "org", "ac", "gov")

private fun query(raw: String?): Map<String, String> =
    raw.orEmpty().split('&').filter { it.isNotEmpty() }.associate { it.substringBefore('=') to it.substringAfter('=', "") }

/** Shared by the address pill's icon and the card (Components; lives until Koan restarts). */
@Stable
class TranslateCueState {
    /** The current page has a cue: the pill shows the icon. */
    var icon by mutableStateOf(false)

    /** Each tap on the icon: the card opens on the current page. */
    var openRequests by mutableIntStateOf(0)

    /** Sites (hosts) where the card was dismissed: no card opens by itself there again. */
    val dismissedSites = HashSet<String>()
}
