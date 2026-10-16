package dev.nerzouille.koan.search

import java.net.URLEncoder
import java.util.Locale

/**
 * "@ddg", "@wiki"…: search one engine once (design 7c/7d). [home] gives its icon. Suggestions:
 * DuckDuckGo, Google and Wikipedia as in android-components' search plugins; YouTube through
 * Google's YouTube suggestions (android-components has none for it).
 */
enum class Shortcut(
    val key: String,
    val label: String,
    val home: String,
    private val prefix: () -> String,
    private val suggestPrefix: () -> String?,
) {
    DuckDuckGo("ddg", "DuckDuckGo", "https://duckduckgo.com", { "https://duckduckgo.com/?q=" }, { "https://ac.duckduckgo.com/ac/?type=list&q=" }),
    Google("g", "Google", "https://www.google.com", { "https://www.google.com/search?q=" }, { "https://www.google.com/complete/search?client=firefox&q=" }),
    Wikipedia(
        "wiki", "Wikipedia", "https://www.wikipedia.org",
        { "https://${wikiLanguage()}.wikipedia.org/w/index.php?search=" },
        { "https://${wikiLanguage()}.wikipedia.org/w/api.php?action=opensearch&search=" },
    ),
    YouTube(
        "yt", "YouTube", "https://www.youtube.com",
        { "https://www.youtube.com/results?search_query=" },
        { "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=" },
    ),
    ;

    fun searchUrl(query: String): String = prefix() + URLEncoder.encode(query, Charsets.UTF_8)

    fun suggestUrl(query: String): String? = suggestPrefix()?.let { it + URLEncoder.encode(query, Charsets.UTF_8) }

    /** The setting's engine this shortcut is, for the picker's "default" label. */
    fun isDefault(engine: SearchEngine): Boolean =
        (this == DuckDuckGo && engine == SearchEngine.DuckDuckGo) || (this == Google && engine == SearchEngine.Google)
}

private fun wikiLanguage(): String = Locale.getDefault().language.ifEmpty { "en" }

/** "@tabs", "@history", "@bookmarks": search only there. */
enum class Scope(val key: String, val label: String) {
    Tabs("tabs", "Tabs"),
    History("history", "History"),
    Bookmarks("bookmarks", "Bookmarks"),
}

/**
 * What was typed: "@" alone (or "@wi", no space yet) = picking an engine or a scope; "@wiki koan"
 * = search "koan" there; anything else is a plain query ([rest]).
 */
data class Parsed(val shortcut: Shortcut? = null, val scope: Scope? = null, val rest: String, val picking: Boolean = false)

fun parseQuery(text: String): Parsed {
    val t = text.trimStart()
    if (!t.startsWith("@")) return Parsed(rest = text.trim())
    val word = t.drop(1).substringBefore(' ')
    if (' ' !in t) return Parsed(rest = word, picking = true)
    val rest = t.substringAfter(' ').trim()
    Shortcut.entries.firstOrNull { it.key.equals(word, ignoreCase = true) }?.let { return Parsed(shortcut = it, rest = rest) }
    Scope.entries.firstOrNull { it.key.equals(word, ignoreCase = true) }?.let { return Parsed(scope = it, rest = rest) }
    return Parsed(rest = text.trim())
}

/** Where a result comes from. */
enum class Source { Tab, History, Bookmark }

/** A page the search sheet can offer. [visits]: how often it was opened; [payload]: the caller's own (a tab). */
data class Hit(
    val url: String,
    val title: String,
    val source: Source,
    val visits: Int = 0,
    val lastUsed: Long = 0,
    val payload: Any? = null,
)

/**
 * The sheet's groups. [top]: the best page whose address starts with what was typed (inline
 * completion shows it; it also leads its group). [pages]: history and bookmarks together.
 */
data class Results(
    val top: Hit? = null,
    val tabs: List<Hit> = emptyList(),
    val pages: List<Hit> = emptyList(),
) {
    val isEmpty: Boolean get() = tabs.isEmpty() && pages.isEmpty()
}

/** The url without scheme and "www.", as typed: "github.com/koan". */
fun bareUrl(url: String): String =
    url.substringAfter("://", url).removePrefix("www.").removeSuffix("/")

private fun Hit.host() = bareUrl(url).substringBefore('/').lowercase()

/** One row per page: the address without query or fragment ("?usp=…", "?hl=…", "#…"). */
fun pageKey(url: String): String = bareUrl(url).substringBefore('#').substringBefore('?').removeSuffix("/").lowercase()

/** Words of a title or address: "Pull request #42 · GitHub" → pull, request, 42, github. */
private fun words(s: String): List<String> = s.lowercase().split(NON_WORD).filter { it.isNotEmpty() }

private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

/**
 * Under [MIN_WORD_QUERY] characters only the start of the host counts ("sa" → samsung.com, never a
 * title word). From there: the start of the address, or every typed word starting a word of the
 * title or of the host. No loose substrings.
 */
private fun Hit.matches(q: String): Boolean {
    if (host().startsWith(q, ignoreCase = true)) return true
    if (q.length < MIN_WORD_QUERY) return false
    if (prefixMatches(q)) return true
    val terms = words(q)
    if (terms.isEmpty()) return false
    val candidates = words(title) + words(host())
    return terms.all { t -> candidates.any { it.startsWith(t) } }
}
private fun Hit.prefixMatches(q: String) = bareUrl(url).startsWith(q, ignoreCase = true)

/**
 * Frecency: how often and how recently. Visits count with diminishing weight within a recency
 * bucket (the last days count most); a bookmark weighs a bit more than a plain visit.
 */
fun frecency(hit: Hit, now: Long): Double {
    val days = (now - hit.lastUsed).coerceAtLeast(0) / 86_400_000.0
    val recency = when {
        days < 4 -> 1.0
        days < 14 -> 0.7
        days < 31 -> 0.5
        days < 90 -> 0.3
        else -> 0.1
    }
    val kind = if (hit.source == Source.Bookmark) 1.4 else 1.0
    return (1 + hit.visits) * recency * kind
}

/**
 * Ranks [tabs], [history] and [bookmarks] for [query] ([scope]: only that source). Each page once
 * (by [pageKey]); a page open in a tab is only ever offered as that tab. Few rows: [TAB_LIMIT] tabs,
 * [PAGE_LIMIT] history/bookmarks (a scope shows more of its one source).
 */
fun rank(
    query: String,
    tabs: List<Hit>,
    history: List<Hit>,
    bookmarks: List<Hit>,
    scope: Scope? = null,
    now: Long = System.currentTimeMillis(),
): Results {
    val q = query.trim()
    if (q.isEmpty() && scope == null) return Results()
    val visits = HashMap<String, Int>().apply { history.forEach { h -> merge(pageKey(h.url), h.visits, ::maxOf) } }
    fun pick(hits: List<Hit>) = hits.filter { q.isEmpty() || it.matches(q) }
    fun best(hits: List<Hit>) = hits
        .sortedByDescending { frecency(it, now) }
        .distinctBy { pageKey(it.url) }
    val inTabs = if (scope == null || scope == Scope.Tabs) {
        best(pick(tabs).map { it.copy(visits = visits[pageKey(it.url)] ?: it.visits) })
    } else {
        emptyList()
    }
    // A page open in a tab (any tab, matching or not) is never a history or bookmark row.
    val open = tabs.mapTo(HashSet()) { pageKey(it.url) }
    val inPages = best(
        (if (scope == null || scope == Scope.History) pick(history) else emptyList()) +
            (if (scope == null || scope == Scope.Bookmarks) pick(bookmarks) else emptyList()),
    ).filter { pageKey(it.url) !in open }

    val many = scope != null
    val tabGroup = inTabs.take(if (many) SCOPE_LIMIT else TAB_LIMIT)
    val pageGroup = inPages.take(if (many) SCOPE_LIMIT else PAGE_LIMIT)
    val top = if (scope != null || q.isEmpty() || ' ' in q) {
        null
    } else {
        (tabGroup + pageGroup).filter { it.prefixMatches(q) }.maxByOrNull { frecency(it, now) + if (it.source == Source.Tab) 0.5 else 0.0 }
    }
    return Results(top = top, tabs = tabGroup, pages = pageGroup)
}

private const val MIN_WORD_QUERY = 3
private const val TAB_LIMIT = 2
private const val PAGE_LIMIT = 2
private const val SCOPE_LIMIT = 8

/**
 * Inline completion of [typed] from [top]: its host while the host is being typed ("git" →
 * "github.com"), then its full address. Null when it doesn't start with what was typed.
 */
fun completion(typed: String, top: Hit?): String? {
    if (top == null || typed.isEmpty() || ' ' in typed) return null
    val bare = bareUrl(top.url)
    if (!bare.startsWith(typed, ignoreCase = true)) return null
    val host = bare.substringBefore('/')
    val full = if (typed.length <= host.length) host else bare
    // Keep the case the owner typed for the part they typed.
    return typed + full.substring(typed.length)
}
