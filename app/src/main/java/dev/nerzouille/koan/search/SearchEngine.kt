package dev.nerzouille.koan.search

import java.net.URLEncoder

/**
 * The engines a search typed in the address field can go to (Settings › Search engine), and where
 * each one's suggestions come from ([suggestPrefix], the query appended; null = it has none).
 * Suggestion endpoints: Google and DuckDuckGo as in android-components' bundled search plugins,
 * Brave from its OpenSearch description, Startpage's own.
 */
enum class SearchEngine(val label: String, private val prefix: String, private val suggestPrefix: String?) {
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q=", "https://ac.duckduckgo.com/ac/?type=list&q="),
    Google("Google", "https://www.google.com/search?q=", "https://www.google.com/complete/search?client=firefox&q="),
    Brave("Brave Search", "https://search.brave.com/search?q=", "https://search.brave.com/api/suggest?q="),
    Startpage("Startpage", "https://www.startpage.com/do/search?q=", "https://www.startpage.com/osuggestions?q="),
    ;

    fun searchUrl(query: String): String = prefix + URLEncoder.encode(query, Charsets.UTF_8)

    /** Its site, for its favicon: "https://duckduckgo.com". */
    val home: String get() = prefix.substring(0, prefix.indexOf('/', "https://".length))

    fun suggestUrl(query: String): String? = suggestPrefix?.let { it + URLEncoder.encode(query, Charsets.UTF_8) }
}
