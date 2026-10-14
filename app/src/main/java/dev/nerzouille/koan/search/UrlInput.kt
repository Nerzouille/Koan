package dev.nerzouille.koan.search

import java.net.URLEncoder

private const val SEARCH_URL = "https://duckduckgo.com/?q="

private val KNOWN_SCHEMES = listOf(
    "http:", "https:", "about:", "file:", "view-source:", "data:", "mailto:", "tel:",
    "moz-extension:", "resource:",
)
private val HOST_LIKE = Regex(
    // domain.tld[:port][/...], localhost[:port][/...] or an IPv4 address
    "^(localhost|([a-z0-9-]+\\.)+[a-z][a-z0-9-]*|\\d{1,3}(\\.\\d{1,3}){3})(:\\d{1,5})?([/?#].*)?$",
    RegexOption.IGNORE_CASE,
)

/** Turns what the user typed into a URL to load: the URL itself, or a DuckDuckGo search. */
fun resolveInput(raw: String): String {
    val text = raw.trim()
    if (text.isEmpty()) return "about:blank"
    if (text.any { it.isWhitespace() }) return searchUrl(text)
    if (KNOWN_SCHEMES.any { text.startsWith(it, ignoreCase = true) }) return text
    if (HOST_LIKE.matches(text)) return "https://$text"
    return searchUrl(text)
}

/**
 * Whether the phone can load [url]. Zen tabs can point at things only the desktop has (a local
 * file, a browser-internal page): those keep their placeholder instead of a blank page.
 */
fun loadsOnPhone(url: String): Boolean {
    val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme in PHONE_SCHEMES
}

private val PHONE_SCHEMES = setOf("http", "https", "about", "data", "view-source", "blob")

fun searchUrl(query: String): String = SEARCH_URL + URLEncoder.encode(query, Charsets.UTF_8)

/** What the address pill shows when not editing: the bare host, no scheme, no "www.". */
fun displayHost(url: String): String {
    if (url.isBlank() || url == "about:blank") return ""
    val scheme = url.substringBefore(':', missingDelimiterValue = "")
    // about:, data:, … show as-is (a data: page may contain "://" further on).
    if (scheme.isEmpty() || !url.startsWith("$scheme://")) return if (scheme == "data") "data:" else url
    val afterScheme = url.substring(scheme.length + 3)
    val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfter('@').substringBefore(':')
    return host.removePrefix("www.")
}
