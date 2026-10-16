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

/** The sheet's groups, top to bottom (design 7a). */
data class Results(
    val top: Hit? = null,
    val tabs: List<Hit> = emptyList(),
    val history: List<Hit> = emptyList(),
    val bookmarks: List<Hit> = emptyList(),
) {
    val isEmpty: Boolean get() = top == null && tabs.isEmpty() && history.isEmpty() && bookmarks.isEmpty()
}

/** The url without scheme and "www.", as typed: "github.com/koan". */
fun bareUrl(url: String): String =
    url.substringAfter("://", url).removePrefix("www.").removeSuffix("/")

/** Words of a title or address: "Pull request #42 · GitHub" → pull, request, 42, github. */
private fun words(s: String): List<String> = s.lowercase().split(NON_WORD).filter { it.isNotEmpty() }

private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

/**
 * What was typed starts the address, or every typed word starts a word of the title or of the
 * host ("hub" doesn't match github.com, "git" does). No loose substrings.
 */
private fun Hit.matches(q: String): Boolean {
    if (prefixMatches(q)) return true
    val terms = words(q)
    if (terms.isEmpty()) return false
    val host = bareUrl(url).substringBefore('/')
    val candidates = words(title) + words(host)
    return terms.all { t -> candidates.any { it.startsWith(t) } }
}
private fun Hit.prefixMatches(q: String) = bareUrl(url).startsWith(q, ignoreCase = true)

/**
 * Ranks [tabs], [history] and [bookmarks] for [query] ([scope]: only that source). The top hit is
 * the most used page whose address starts with what was typed (it is what inline completion
 * shows); the rest are grouped by source, each page once, open tabs first.
 */
fun rank(query: String, tabs: List<Hit>, history: List<Hit>, bookmarks: List<Hit>, scope: Scope? = null): Results {
    val q = query.trim()
    if (q.isEmpty() && scope == null) return Results()
    val visits = history.associate { it.url to it.visits }
    fun pick(hits: List<Hit>) = hits.filter { q.isEmpty() || it.matches(q) }
    val inTabs = if (scope == null || scope == Scope.Tabs) pick(tabs).map { it.copy(visits = visits[it.url] ?: it.visits) } else emptyList()
    val inHistory = if (scope == null || scope == Scope.History) pick(history) else emptyList()
    val inBookmarks = if (scope == null || scope == Scope.Bookmarks) pick(bookmarks) else emptyList()

    val top = if (scope != null || q.isEmpty() || ' ' in q) {
        null
    } else {
        (inTabs + inHistory + inBookmarks)
            .filter { it.prefixMatches(q) }
            .maxWithOrNull(compareBy<Hit>({ it.visits }, { it.source == Source.Tab }, { it.lastUsed }))
    }
    val shown = HashSet<String>().apply { top?.let { add(it.url) } }
    // Fewer, better: a scope shows more of its one source.
    fun take(hits: List<Hit>, limit: Int) = hits
        .sortedWith(compareByDescending<Hit> { it.prefixMatches(q) }.thenByDescending { it.visits }.thenByDescending { it.lastUsed })
        .filter { shown.add(it.url) }
        .take(limit)
    // A page open in a tab is offered as that tab, never again as history or a bookmark.
    val many = scope != null
    val tabGroup = take(inTabs, if (many) SCOPE_LIMIT else TAB_LIMIT)
    inTabs.forEach { shown.add(it.url) }
    return Results(
        top = top,
        tabs = tabGroup,
        history = take(inHistory, if (many) SCOPE_LIMIT else PAGE_LIMIT),
        bookmarks = take(inBookmarks, if (many) SCOPE_LIMIT else PAGE_LIMIT),
    )
}

private const val TAB_LIMIT = 2
private const val PAGE_LIMIT = 3
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
