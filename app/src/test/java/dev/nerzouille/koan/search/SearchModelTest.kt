package dev.nerzouille.koan.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchModelTest {
    private val history = listOf(
        Hit("https://www.example.com/docs", "Example docs", Source.History, visits = 12, lastUsed = 5),
        Hit("https://example.org/", "Example org", Source.History, visits = 3, lastUsed = 9),
        Hit("https://news.example.net/today", "Made-up news", Source.History, visits = 1, lastUsed = 1),
    )
    private val tabs = listOf(Hit("https://example.org/", "Example org", Source.Tab, payload = "tab-1"))
    private val bookmarks = listOf(Hit("https://example.com/docs", "Docs (bookmark)", Source.Bookmark))

    @Test fun `parses shortcuts, scopes and the picker`() {
        assertTrue(parseQuery("@").picking)
        assertTrue(parseQuery("@wi").picking)
        assertEquals(Parsed(shortcut = Shortcut.Wikipedia, rest = "koan"), parseQuery("@wiki koan"))
        assertEquals(Parsed(scope = Scope.History, rest = "docs"), parseQuery("@history docs"))
        assertEquals(Parsed(rest = "@nothing here"), parseQuery("@nothing here"))
        assertEquals(Parsed(rest = "plain words"), parseQuery(" plain words "))
    }

    @Test fun `top hit is the most visited page starting with what was typed`() {
        val r = rank("exa", tabs, history, bookmarks)
        assertEquals("https://www.example.com/docs", r.top?.url)
        // The open tab is offered as a tab, not again as history.
        assertEquals(listOf("https://example.org/"), r.tabs.map { it.url })
        assertFalse(r.history.any { it.url == "https://example.org/" })
        assertFalse(r.history.any { it.url == r.top?.url })
    }

    @Test fun `scopes keep only their source and have no top hit`() {
        val r = rank("example", tabs, history, bookmarks, scope = Scope.Bookmarks)
        assertNull(r.top)
        assertTrue(r.tabs.isEmpty() && r.history.isEmpty())
        assertEquals(1, r.bookmarks.size)
        // An empty scoped query lists everything there.
        assertEquals(3, rank("", tabs, history, bookmarks, scope = Scope.History).history.size)
    }

    @Test fun `nothing typed, nothing shown`() {
        assertTrue(rank("  ", tabs, history, bookmarks).isEmpty)
    }

    @Test fun `completion fills the host first, then the address`() {
        val top = history.first()
        assertEquals("example.com", completion("exa", top))
        assertEquals("EXAmple.com", completion("EXA", top))
        assertEquals("example.com/docs", completion("example.com/", top))
        assertNull(completion("docs", top))
        assertNull(completion("exa mple", top))
    }
}
