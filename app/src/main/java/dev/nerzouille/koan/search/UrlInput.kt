package dev.nerzouille.koan.search


private val KNOWN_SCHEMES = listOf(
    "http:", "https:", "about:", "file:", "view-source:", "data:", "mailto:", "tel:",
    "moz-extension:", "resource:",
)
private val HOST_LIKE = Regex(
    // domain.tld[:port][/...], localhost[:port][/...] or an IPv4 address
    "^(localhost|([a-z0-9-]+\\.)+[a-z][a-z0-9-]*|\\d{1,3}(\\.\\d{1,3}){3})(:\\d{1,5})?([/?#].*)?$",
    RegexOption.IGNORE_CASE,
)

/** Turns what the user typed into a URL to load: the URL itself, or a search on [engine]. */
fun resolveInput(raw: String, engine: SearchEngine = SearchEngine.DuckDuckGo): String {
    val text = raw.trim()
    if (text.isEmpty()) return "about:blank"
    if (text.any { it.isWhitespace() }) return engine.searchUrl(text)
    if (KNOWN_SCHEMES.any { text.startsWith(it, ignoreCase = true) }) return text
    if (HOST_LIKE.matches(text)) return "https://$text"
    return engine.searchUrl(text)
}

/**
 * Text shared to Koan or a web search from another app: a link inside it opens as is, anything
 * else is searched.
 */
fun resolveShared(text: String, engine: SearchEngine): String {
    val link = WEB_LINK.find(text)?.value
    return link ?: resolveInput(text, engine)
}

private val WEB_LINK = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

/**
 * Whether the phone can load [url]. Zen tabs can point at things only the desktop has (a local
 * file, a browser-internal page): those keep their placeholder instead of a blank page.
 */
fun loadsOnPhone(url: String): Boolean {
    val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme in PHONE_SCHEMES
}

private val PHONE_SCHEMES = setOf("http", "https", "about", "data", "view-source", "blob")


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
