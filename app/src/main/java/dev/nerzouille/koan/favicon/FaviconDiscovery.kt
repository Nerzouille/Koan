// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

import java.net.URI

/**
 * Port of `FaviconDiscovery`. Zen Browser uses Firefox's favicon loader:
 * `rel="icon"` / touch icons from the page, then `/favicon.ico` on that host.
 * No third-party icon service. SVG is skipped; BitmapFactory cannot decode it.
 *
 * A few hosts only publish the real product icon inside a signed-in document.
 * Those fallbacks are the public files that page links to.
 */
object FaviconDiscovery {
    private val interstitialHosts = setOf(
        "accounts.google.com",
        "myaccount.google.com",
        "workspace.google.com",
    )

    private val linkTag = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val baseTag = Regex("<base\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val comment = Regex("<!--[\\s\\S]*?-->")
    private val sizePattern = Regex("([1-9][0-9]*)x[1-9][0-9]*", RegexOption.IGNORE_CASE)

    /** Tab address we are willing to request: http(s), no userinfo, query, or fragment. */
    fun documentURL(pageURL: String): URI? {
        val trimmed = pageURL.trim()
        if (trimmed.isEmpty()) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val path = uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/"
        val port = when {
            uri.port == -1 -> -1
            scheme == "https" && uri.port == 443 -> -1
            scheme == "http" && uri.port == 80 -> -1
            else -> uri.port
        }
        val authority = if (port == -1) host else "$host:$port"
        // Build from raw parts: the multi-arg URI constructor re-quotes `%`
        // and would turn an already-encoded path into `%25` garbage.
        return runCatching { URI("$scheme://$authority$path") }.getOrNull()
    }

    fun siteKey(uri: URI): String? {
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val port = uri.port
        val authority = when {
            port == -1 || (scheme == "https" && port == 443) || (scheme == "http" && port == 80) -> host
            else -> "$host:$port"
        }
        var key = "$scheme://$authority"
        if (host == "docs.google.com") {
            val segment = uri.path.orEmpty().split('/').firstOrNull { it.isNotEmpty() }?.lowercase().orEmpty()
            if (segment.isNotEmpty()) key += "/$segment"
        }
        return key
    }

    /**
     * Icon URLs to try, best first. [pageURL] is the tab; [finalURL] is where
     * the document request landed. Icons on a login interstitial are ignored.
     */
    fun candidates(html: String?, finalURL: URI?, pageURL: URI): List<String> {
        val blocked = finalURL?.let(::isInterstitial) == true
        val document = if (blocked) null else finalURL
        val urls = mutableListOf<String>()
        if (html != null && document != null) {
            urls += declaredIcons(html, document)
        }
        knownIcon(pageURL)?.let { urls += it }
        originFavicon(document ?: pageURL)?.let { urls += it }
        return urls.distinct()
    }

    fun hasKnownIcon(pageURL: URI): Boolean = knownIcon(pageURL) != null

    /** Desktop `localhost` is not this phone. Skip it instead of waiting on a timeout. */
    fun isLoopback(uri: URI): Boolean {
        val host = uri.host?.lowercase()?.removePrefix("[")?.removeSuffix("]") ?: return false
        return host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "0.0.0.0"
    }

    /**
     * Public files the signed-in page links to. `docs.google.com/favicon.ico`
     * is the Drive mark, so Docs, Sheets, Slides, and Forms come from the path.
     * Calendar has no `/favicon.ico`. Shopify admin refuses an anonymous fetch.
     */
    fun knownIcon(pageURL: URI): String? {
        val host = pageURL.host?.lowercase() ?: return null
        val path = pageURL.path.orEmpty().lowercase()
        return when {
            host == "calendar.google.com" ->
                "https://calendar.google.com/googlecalendar/images/favicons_2020q4/calendar_31.ico"
            host == "docs.google.com" && path.contains("/spreadsheets") ->
                "https://ssl.gstatic.com/docs/spreadsheets/favicon3.ico"
            host == "docs.google.com" && path.contains("/presentation") ->
                "https://ssl.gstatic.com/docs/presentations/images/favicon5.ico"
            host == "docs.google.com" && path.contains("/forms") ->
                "https://ssl.gstatic.com/docs/forms/device_home/android_192.png"
            host == "docs.google.com" ->
                "https://ssl.gstatic.com/docs/documents/images/kix-favicon7.ico"
            host == "admin.shopify.com" ->
                "https://cdn.shopify.com/static/images/favicon.ico"
            else -> null
        }
    }

    private data class Icon(val url: String, val width: Int, val rich: Boolean, val kind: Kind)

    private enum class Kind { RASTER, ICO, SVG }

    private fun declaredIcons(html: String, document: URI): List<String> {
        val base = resolutionBase(html, document)
        val icons = parseIcons(head(html), base)
        val raster = icons.filter { it.kind != Kind.SVG }
        val rich = raster.filter { it.rich }.sortedWith(
            compareBy({ rank(it, 128).first }, { rank(it, 128).second }, { rank(it, 128).third }, { it.url }),
        )
        val plain = raster.filter { !it.rich && it.kind == Kind.RASTER }.sortedWith(
            compareBy({ rank(it, 64).first }, { rank(it, 64).second }, { rank(it, 64).third }, { it.url }),
        )
        val icos = raster.filter { !it.rich && it.kind == Kind.ICO }
        // SVG is a real declaration; the loader fetches and rasterizes it, so it
        // beats the guessed `/favicon.ico`. Many sites only ship an SVG icon.
        val svgs = icons.filter { it.kind == Kind.SVG }
        return (rich + plain + icos + svgs).map { it.url }
    }

    /** True for a declared `.svg` icon; the loader must rasterize it, not decode it. */
    fun isSvgIcon(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".svg") || path.contains("favicon.svg")
    }

    private fun rank(icon: Icon, target: Int): Triple<Int, Int, Int> {
        if (icon.width <= 0) return Triple(1, 0, 0)
        return Triple(0, kotlin.math.abs(icon.width - target), -icon.width)
    }

    private fun parseIcons(html: String, base: URI): List<Icon> {
        val icons = mutableListOf<Icon>()
        for (match in linkTag.findAll(html)) {
            icon(match.value, base)?.let { icons += it }
        }
        return icons
    }

    private fun icon(tag: String, base: URI): Icon? {
        if (attribute("color", tag) != null) return null
        val rel = attribute("rel", tag)?.lowercase() ?: return null
        if (rel.contains("mask")) return null
        val tokens = rel.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val rich = tokens.any { it == "apple-touch-icon" || it == "apple-touch-icon-precomposed" || it == "fluid-icon" }
        if (!rich && "icon" !in tokens) return null
        val href = attribute("href", tag) ?: return null
        val decoded = decodeEntities(href.trim())
        if (decoded.isEmpty()) return null
        val resolved = runCatching { base.resolve(decoded) }.getOrNull() ?: return null
        val url = httpURL(resolved) ?: return null
        return Icon(url.toString(), iconWidth(attribute("sizes", tag)), rich, kind(attribute("type", tag), url))
    }

    private fun kind(type: String?, url: URI): Kind {
        val declared = type?.lowercase().orEmpty()
        val path = url.path?.lowercase().orEmpty()
        if (declared.contains("svg") || path.endsWith(".svg")) return Kind.SVG
        if (declared.contains("icon") || declared.contains("ico") || path.endsWith(".ico")) return Kind.ICO
        return Kind.RASTER
    }

    private fun iconWidth(sizes: String?): Int {
        if (sizes.isNullOrBlank()) return 0
        return sizePattern.findAll(sizes).mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull() ?: 0
    }

    private fun resolutionBase(html: String, document: URI): URI {
        val scoped = head(html)
        val tag = baseTag.find(scoped)?.value ?: return document
        val href = attribute("href", tag) ?: return document
        val decoded = decodeEntities(href.trim())
        val resolved = runCatching { document.resolve(decoded) }.getOrNull() ?: return document
        return httpURL(resolved) ?: document
    }

    private fun head(html: String): String {
        val stripped = comment.replace(html, "")
        val end = Regex("</head>", RegexOption.IGNORE_CASE).find(stripped)?.range?.last ?: return stripped
        return stripped.substring(0, end + 1)
    }

    private fun originFavicon(page: URI): String? {
        val scheme = page.scheme?.lowercase() ?: return null
        val host = page.host ?: return null
        val port = page.port
        return runCatching { URI(scheme, null, host, port, "/favicon.ico", null, null).toString() }.getOrNull()
    }

    private fun isInterstitial(url: URI): Boolean {
        val host = url.host?.lowercase() ?: return false
        return host in interstitialHosts
    }

    private fun httpURL(uri: URI): URI? {
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.takeIf { it.isNotEmpty() } ?: return null
        val path = uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/"
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        val authority = if (uri.port == -1) host else "$host:${uri.port}"
        // Preserve already-encoded raw path/query (see documentURL).
        return runCatching { URI("$scheme://$authority$path$query") }.getOrNull()
    }

    private fun attribute(name: String, tag: String): String? {
        val pattern = Regex(
            "\\b${Regex.escape(name)}\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+))",
            RegexOption.IGNORE_CASE,
        )
        val match = pattern.find(tag) ?: return null
        for (index in 1..3) {
            val group = match.groups[index] ?: continue
            return group.value
        }
        return null
    }

    private fun decodeEntities(text: String): String =
        text.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
