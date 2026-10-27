package dev.nerzouille.koan.search

import java.net.URLEncoder

/**
 * An engine a search typed in the address field can go to (Settings › Search engine): one of
 * Koan's, or one the owner added ([custom]). Its templates hold "%s" where the query goes;
 * [suggestTemplate] null = it has no suggestions. Built-in suggestion endpoints: Google and
 * DuckDuckGo as in android-components' bundled search plugins, Brave from its OpenSearch
 * description, Startpage's own.
 */
data class SearchEngine(
    /** Built-ins keep their old enum names: the saved default survives updates. */
    val id: String,
    val label: String,
    val searchTemplate: String,
    val suggestTemplate: String? = null,
    /** "kagi" for "@kagi": search it once from the address field (custom engines). */
    val shortcut: String? = null,
    val custom: Boolean = false,
) {
    fun searchUrl(query: String): String = fill(searchTemplate, query)

    /** Its site, for its favicon: "https://duckduckgo.com". */
    val home: String get() = origin(searchTemplate)

    fun suggestUrl(query: String): String? = suggestTemplate?.let { fill(it, query) }

    companion object {
        val DuckDuckGo = SearchEngine("DuckDuckGo", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://ac.duckduckgo.com/ac/?type=list&q=%s")
        val Google = SearchEngine("Google", "Google", "https://www.google.com/search?q=%s", "https://www.google.com/complete/search?client=firefox&q=%s")
        val Brave = SearchEngine("Brave", "Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com/api/suggest?q=%s")
        val Startpage = SearchEngine("Startpage", "Startpage", "https://www.startpage.com/do/search?q=%s", "https://www.startpage.com/osuggestions?q=%s")

        val builtIns = listOf(DuckDuckGo, Google, Brave, Startpage)
    }
}

/** [template] with the encoded [query] in place of every "%s". */
internal fun fill(template: String, query: String): String = template.replace("%s", URLEncoder.encode(query, "UTF-8"))

/** "https://search.example.org/search?q=%s" → "https://search.example.org". */
fun origin(url: String): String {
    val start = url.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: return url
    val end = url.indexOfAny(charArrayOf('/', '?', '#'), start).takeIf { it >= 0 } ?: url.length
    return url.substring(0, end)
}
