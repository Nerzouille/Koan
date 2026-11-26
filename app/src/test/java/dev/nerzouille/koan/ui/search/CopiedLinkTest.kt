package dev.nerzouille.koan.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** "Open link you copied": only one web address, and nothing else, is offered. */
class CopiedLinkTest {
    @Test fun `a copied link is one web address and nothing else`() {
        assertEquals("https://example.com/a?b=1", CopiedLinks.linkIn("  https://example.com/a?b=1 \n"))
        assertEquals("https://example.org/path", CopiedLinks.linkIn("example.org/path"))
        assertNull(CopiedLinks.linkIn("hello world"))
        assertNull(CopiedLinks.linkIn("see https://example.com"))
        assertNull(CopiedLinks.linkIn("localhost"))
        assertNull(CopiedLinks.linkIn("ftp://example.com"))
        assertNull(CopiedLinks.linkIn(""))
    }
}
