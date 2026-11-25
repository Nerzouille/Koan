package dev.nerzouille.koan.ui.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkTextTest {
    @Test fun `a link with words offers its text`() {
        assertEquals("Read the docs", LinkTarget("https://example.com/docs", null, "  Read the docs ").linkText)
    }

    @Test fun `no text, an image or a video alone, no Copy link text`() {
        assertNull(LinkTarget("https://example.com/", null, " ").linkText)
        assertNull(LinkTarget(null, "https://example.com/a.png", "A photo").linkText)
        assertNull(LinkTarget(null, null, "Clip", video = "https://example.com/v.mp4").linkText)
    }
}
