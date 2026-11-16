// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

import java.net.URI

/**
 * Port of `FaviconResolver`, trimmed to what Koan runs (Koan: the synced-icon paths and the
 * prefetch requests are gone; [IconStore] decodes inline `data:` icons itself). An icon is the
 * site's own, cached under one key per site.
 */
object FaviconResolver {
    /** The key a page's icon is cached under (`site:<host>`), or null for a page without a web host. */
    fun cacheKey(pageURL: String): String? {
        val document = FaviconDiscovery.documentURL(pageURL) ?: return null
        val site = FaviconDiscovery.siteKey(document) ?: return null
        return "site:$site"
    }

    /** True for `about:`, `chrome:`, `resource:`, `data:` URLs that carry no
     *  web host — they render with a static placeholder icon, not a favicon. */
    fun isLocalURL(urlString: String): Boolean {
        val scheme = runCatching { URI(urlString).scheme }.getOrNull()?.lowercase() ?: return false
        return scheme != "http" && scheme != "https"
    }
}
