// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

import dev.nerzouille.koan.sync.ZenSpaces
import java.net.URI

data class FaviconRequest(val pageURL: String, val directURL: String?, val dataURL: String?, val cacheKey: String)

/** Where a tab's favicon comes from: an inline `data:` icon, a direct URL, or the site. */
sealed class FaviconSource {
    class Data(val dataURL: String, val cacheKey: String) : FaviconSource()
    class Direct(val url: String, val cacheKey: String) : FaviconSource()
    data object Unknown : FaviconSource()
}

/** Port of `FaviconResolver`. Inline sync icon, direct sync icon, otherwise the site's own icon. */
object FaviconResolver {
    fun url(pageURL: String, directURL: String?, dataURL: String? = null): String? {
        if (directURL.isNullOrEmpty()) return null
        val scheme = runCatching { URI(directURL) }.getOrNull()?.scheme
        if (scheme == "https" || scheme == "http") return directURL
        return null
    }

    /**
     * What the loader should use. Zen syncs the browser's own favicon as a
     * `data:` URI in the tab's `icon` field for most desktop tabs; that is
     * decoded on device (no network), so it beats any remote lookup.
     */
    fun source(pageURL: String, directURL: String?, dataURL: String? = null): FaviconSource {
        dataURL(dataURL)?.let { return FaviconSource.Data(it, "data:$pageURL") }
        url(pageURL, directURL)?.let { return FaviconSource.Direct(it, "direct:$it") }
        val document = FaviconDiscovery.documentURL(pageURL) ?: return FaviconSource.Unknown
        val site = FaviconDiscovery.siteKey(document) ?: return FaviconSource.Unknown
        return FaviconSource.Direct(document.toString(), "site:$site")
    }

    fun cacheKey(pageURL: String, directURL: String?, dataURL: String? = null): String? = when (val s = source(pageURL, directURL, dataURL)) {
        is FaviconSource.Data -> s.cacheKey
        is FaviconSource.Direct -> s.cacheKey
        FaviconSource.Unknown -> null
    }

    private fun dataURL(raw: String?): String? {
        val clean = raw?.trim() ?: return null
        if (!clean.startsWith("data:image/")) return null
        return clean
    }

    /** True for `about:`, `chrome:`, `resource:`, `data:` URLs that carry no
     *  web host — they render with a static placeholder icon, not a favicon. */
    fun isLocalURL(urlString: String): Boolean {
        val scheme = runCatching { URI(urlString).scheme }.getOrNull()?.lowercase() ?: return false
        return scheme != "http" && scheme != "https"
    }

    /** One request per distinct icon. Static emoji icons are not fetched. */
    fun requests(snapshot: ZenSpaces.ZenSnapshot): List<FaviconRequest> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<FaviconRequest>()
        fun consider(tab: ZenSpaces.ZenTab) {
            // Zen static icons (emoji/vector SVG) render via ZenIconDecoder,
            // never the favicon loader. Their payload is also a data: URL, so
            // this must be skipped before the data-url check below.
            if (tab.hasStaticIcon == true && !tab.icon.isNullOrEmpty()) return
            val dataURL = tab.icon?.trim()?.takeIf { it.startsWith("data:image/") }
            val key = cacheKey(pageURL = tab.url, directURL = tab.iconURL, dataURL = dataURL) ?: return
            if (!seen.add(key)) return
            result.add(FaviconRequest(tab.url, tab.iconURL, dataURL, key))
        }
        for (space in snapshot.spaces) {
            for (item in space.pinned) {
                when (item) {
                    is ZenSpaces.ZenItem.Tab -> consider(item.tab)
                    is ZenSpaces.ZenItem.Folder -> item.folder.tabs.forEach(::consider)
                    is ZenSpaces.ZenItem.Split -> item.split.tabs.forEach(::consider)
                }
            }
            for (item in space.tabs) {
                when (item) {
                    is ZenSpaces.ZenItem.Tab -> consider(item.tab)
                    is ZenSpaces.ZenItem.Folder -> item.folder.tabs.forEach(::consider)
                    is ZenSpaces.ZenItem.Split -> item.split.tabs.forEach(::consider)
                }
            }
        }
        snapshot.essentials.values.forEach { tabs -> tabs.forEach(::consider) }
        return result
    }
}
