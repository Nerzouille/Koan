package dev.nerzouille.koan.favicon

import dev.nerzouille.koan.ui.overview.faviconLook
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Test

class IconHostTest {
    @Test
    fun `mobile and www hosts share the site's icon`() {
        assertEquals("example.com", FaviconDiscovery.iconHost("m.example.com"))
        assertEquals("example.com", FaviconDiscovery.iconHost("mobile.example.com"))
        assertEquals("example.com", FaviconDiscovery.iconHost("WWW.Example.com"))
        assertEquals("news.example.org", FaviconDiscovery.iconHost("m.news.example.org"))
        assertEquals("en.example.org", FaviconDiscovery.iconHost("en.m.example.org"))
        assertEquals(
            FaviconDiscovery.siteKey(URI("https://example.com/a")),
            FaviconDiscovery.siteKey(URI("https://m.example.com/b")),
        )
    }

    @Test
    fun `short hosts are left alone`() {
        assertEquals("m.com", FaviconDiscovery.iconHost("m.com"))
        assertEquals("example.com", FaviconDiscovery.iconHost("example.com"))
        assertEquals("mail.example.com", FaviconDiscovery.iconHost("mail.example.com"))
    }

    @Test
    fun `one letter tile per site`() {
        assertEquals(faviconLook("https://example.com/"), faviconLook("https://m.example.com/page"))
        assertEquals("E", faviconLook("https://m.example.com/").letters)
    }
}
