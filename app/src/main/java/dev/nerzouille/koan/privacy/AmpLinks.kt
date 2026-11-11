package dev.nerzouille.koan.privacy

/**
 * Google AMP links back to the article (owner's decision 2026-11-11): Google's AMP viewer
 * (`google.<tld>/amp/s/<site>/<path>`) and the AMP cache (`<x>.cdn.ampproject.org/c|v/s/<site>/<path>`)
 * open the original url instead. Publisher-hosted AMP pages (`<site>/amp/…`) are left alone.
 */
object AmpLinks {
    private val GOOGLE = Regex("""^(?:www\.)?google\.[a-z]{2,3}(?:\.[a-z]{2})?$""")

    /** The original url behind [url], or null if it isn't a Google AMP link. */
    fun original(url: String): String? {
        val scheme = url.substringBefore("://", "").lowercase()
        if (scheme != "https" && scheme != "http") return null
        val rest = url.substring(scheme.length + 3)
        val host = rest.substringBefore('/').substringBefore('?').substringBefore('#').lowercase()
        val path = rest.substring(host.length)
        val target = when {
            GOOGLE.matches(host) && path.startsWith("/amp/") -> path.removePrefix("/amp/")
            host.endsWith(".cdn.ampproject.org") -> listOf("/c/", "/v/").firstOrNull { path.startsWith(it) }?.let { path.removePrefix(it) }
            else -> null
        } ?: return null
        val secure = target.startsWith("s/")
        val site = if (secure) target.removePrefix("s/") else target
        val siteHost = site.substringBefore('/').substringBefore('?').substringBefore('#')
        if (siteHost.isEmpty() || !siteHost.contains('.') || siteHost.contains('@')) return null
        // The AMP viewer adds its own parameter; the article doesn't need it.
        val clean = site.replace(Regex("""([?&])amp_js_v=[^&#]*&?"""), "$1").replace(Regex("""[?&](#|$)"""), "$1")
        return (if (secure) "https://" else "http://") + clean
    }
}

/**
 * Removes [PrivacyEngine.STRIP_LIST]'s parameters from [url] (private tabs: links you type or paste
 * too, which Gecko's own stripping leaves alone). Null if there was nothing to remove.
 */
object LinkCleaner {
    fun clean(url: String, strip: Collection<String> = PrivacyEngine.STRIP_LIST): String? {
        val q = url.indexOf('?')
        if (q < 0) return null
        val hash = url.indexOf('#', q).let { if (it < 0) url.length else it }
        val params = url.substring(q + 1, hash).split('&')
        val kept = params.filter { p -> p.isNotEmpty() && p.substringBefore('=').lowercase() !in strip }
        if (kept.size == params.filter { it.isNotEmpty() }.size) return null
        val base = url.substring(0, q)
        return base + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + url.substring(hash)
    }
}
