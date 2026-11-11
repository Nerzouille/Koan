package dev.nerzouille.koan.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmpLinksTest {
    @Test fun googleViewer() {
        assertEquals("https://example.com/news/a.html", AmpLinks.original("https://www.google.com/amp/s/example.com/news/a.html"))
        assertEquals("https://example.com/a?x=1", AmpLinks.original("https://www.google.co.uk/amp/s/example.com/a?x=1"))
        assertEquals("http://example.org/a", AmpLinks.original("https://google.fr/amp/example.org/a"))
    }

    @Test fun ampCache() {
        assertEquals("https://example.com/a/amp", AmpLinks.original("https://example-com.cdn.ampproject.org/c/s/example.com/a/amp"))
        assertEquals("https://example.com/a", AmpLinks.original("https://example-com.cdn.ampproject.org/v/s/example.com/a?amp_js_v=0.1"))
    }

    @Test fun leavesOthersAlone() {
        assertNull(AmpLinks.original("https://example.com/amp/a"))
        assertNull(AmpLinks.original("https://www.google.com/search?q=amp"))
        assertNull(AmpLinks.original("https://www.google.com/amp/"))
        assertNull(AmpLinks.original("https://example-com.cdn.ampproject.org/i/s/example.com/a.png"))
        assertNull(AmpLinks.original("https://www.google.com/amp/s/user@example.com/a"))
        assertNull(AmpLinks.original("intent://x"))
    }

    @Test fun cleansTrackingParameters() {
        assertEquals("https://example.com/?keep=1", LinkCleaner.clean("https://example.com/?utm_source=x&fbclid=abc&keep=1"))
        assertEquals("https://example.com/a#top", LinkCleaner.clean("https://example.com/a?gclid=1#top"))
        assertNull(LinkCleaner.clean("https://example.com/?q=utm_source"))
        assertNull(LinkCleaner.clean("https://example.com/a"))
    }
}
