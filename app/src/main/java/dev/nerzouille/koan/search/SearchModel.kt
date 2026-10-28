package dev.nerzouille.koan.search

import java.util.Locale

/**
 * "@ddg", "@wiki"…: search one engine once (design 7c/7d). [home] gives its icon. Templates hold
 * "%s" where the query goes. Suggestions: DuckDuckGo, Google and Wikipedia as in
 * android-components' search plugins; YouTube through Google's YouTube suggestions
 * (android-components has none for it); Baidu's own. Custom engines with a shortcut join them ([of]).
 */
class Shortcut(
    val key: String,
    val label: String,
    val home: String,
    private val template: () -> String,
    private val suggestTemplate: () -> String?,
    /** The engine it is, for the picker's "default" label. */
    private val engineId: String? = null,
) {
    fun searchUrl(query: String): String = fill(template(), query)

    fun suggestUrl(query: String): String? = suggestTemplate()?.let { fill(it, query) }

    fun isDefault(engine: SearchEngine): Boolean = engineId == engine.id

    companion object {
        val DuckDuckGo = Shortcut("ddg", "DuckDuckGo", "https://duckduckgo.com", { SearchEngine.DuckDuckGo.searchTemplate }, { SearchEngine.DuckDuckGo.suggestTemplate }, SearchEngine.DuckDuckGo.id)
        val Google = Shortcut("g", "Google", "https://www.google.com", { SearchEngine.Google.searchTemplate }, { SearchEngine.Google.suggestTemplate }, SearchEngine.Google.id)
        val Wikipedia = Shortcut(
            "wiki", "Wikipedia", "https://www.wikipedia.org",
            { "https://${wikiLanguage()}.wikipedia.org/w/index.php?search=%s" },
            { "https://${wikiLanguage()}.wikipedia.org/w/api.php?action=opensearch&search=%s" },
        )
        val YouTube = Shortcut(
            "yt", "YouTube", "https://www.youtube.com",
            { "https://www.youtube.com/results?search_query=%s" },
            { "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=%s" },
        )

        val Baidu = Shortcut("bd", "Baidu", "https://www.baidu.com", { SearchEngine.Baidu.searchTemplate }, { SearchEngine.Baidu.suggestTemplate }, SearchEngine.Baidu.id)

        val builtIns = listOf(DuckDuckGo, Google, Wikipedia, YouTube, Baidu)

        /** A custom engine's "@" shortcut, if it has one. */
        fun of(engine: SearchEngine): Shortcut? = engine.shortcut?.let { key ->
            Shortcut(key, engine.label, engine.home, { engine.searchTemplate }, { engine.suggestTemplate }, engine.id)
        }

        /** The built-in shortcuts, then the custom engines'. */
        fun all(engines: List<SearchEngine>): List<Shortcut> = builtIns + engines.mapNotNull(::of)
    }
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

fun parseQuery(text: String, shortcuts: List<Shortcut> = Shortcut.builtIns): Parsed {
    val t = text.trimStart()
    if (!t.startsWith("@")) return Parsed(rest = text.trim())
    val word = t.drop(1).substringBefore(' ')
    if (' ' !in t) return Parsed(rest = word, picking = true)
    val rest = t.substringAfter(' ').trim()
    shortcuts.firstOrNull { it.key.equals(word, ignoreCase = true) }?.let { return Parsed(shortcut = it, rest = rest) }
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
 * The sheet's groups. [top]: the best page whose address starts with what was typed and may be
 * completed inline (see completable). [pages]: history and bookmarks together.
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
        (inTabs + inPages).filter { it.prefixMatches(q) && it.completable(now) }
            .maxByOrNull { frecency(it, now) + if (it.source == Source.Tab) 0.5 else 0.0 }
    }
    return Results(top = top, tabs = tabGroup, pages = pageGroup)
}

/**
 * May be completed inline: an open tab or a bookmark, or a page from history visited often or
 * recently enough ([MIN_COMPLETE_FRECENCY]); never a sign-in or redirect endpoint.
 */
private fun Hit.completable(now: Long): Boolean {
    if (HistoryFilter.isAuth(url) || HistoryFilter.isRedirect(url, title)) return false
    return source != Source.History || frecency(this, now) >= MIN_COMPLETE_FRECENCY
}

/** One visit in the last month, a few in the last three. */
private const val MIN_COMPLETE_FRECENCY = 1.0
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
