package dev.nerzouille.koan.search

import java.net.URLEncoder

/** The engines a search typed in the address field can go to (Settings › Search engine). */
enum class SearchEngine(val label: String, private val prefix: String) {
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q="),
    Google("Google", "https://www.google.com/search?q="),
    Brave("Brave Search", "https://search.brave.com/search?q="),
    Startpage("Startpage", "https://www.startpage.com/do/search?q="),
    ;

    fun searchUrl(query: String): String = prefix + URLEncoder.encode(query, Charsets.UTF_8)
}
