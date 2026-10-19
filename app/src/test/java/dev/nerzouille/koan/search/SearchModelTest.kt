package dev.nerzouille.koan.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchModelTest {
    private val now = 100L * 86_400_000L
    private fun daysAgo(d: Int) = now - d * 86_400_000L

    private val history = listOf(
        Hit("https://www.example.com/docs", "Example docs", Source.History, visits = 12, lastUsed = daysAgo(1)),
        Hit("https://example.org/", "Example org", Source.History, visits = 3, lastUsed = daysAgo(2)),
        Hit("https://news.example.net/today", "Made-up news", Source.History, visits = 1, lastUsed = daysAgo(3)),
    )
    private val tabs = listOf(Hit("https://example.org/", "Example org", Source.Tab, payload = "tab-1"))

    @Test fun `parses shortcuts, scopes and the picker`() {
        assertTrue(parseQuery("@").picking)
        assertTrue(parseQuery("@wi").picking)
        assertEquals(Parsed(shortcut = Shortcut.Wikipedia, rest = "koan"), parseQuery("@wiki koan"))
        assertEquals(Parsed(scope = Scope.History, rest = "docs"), parseQuery("@history docs"))
        assertEquals(Parsed(rest = "@nothing here"), parseQuery("@nothing here"))
        assertEquals(Parsed(rest = "plain words"), parseQuery(" plain words "))
    }

    @Test fun `under three characters only the host's start counts`() {
        val pages = listOf(
            Hit("https://samsung.example/", "Phones", Source.History, visits = 1, lastUsed = daysAgo(1)),
            Hit("https://other.example/", "Sale of the year", Source.History, visits = 9, lastUsed = daysAgo(1)),
        )
        assertEquals(listOf("https://samsung.example/"), rank("sa", emptyList(), pages, emptyList(), now = now).pages.map { it.url })
        // From three characters a title word counts too.
        assertEquals(2, rank("sal", emptyList(), pages, emptyList(), now = now).pages.size + rank("sam", emptyList(), pages, emptyList(), now = now).pages.size)
    }

    @Test fun `matches word starts, not loose substrings`() {
        val pages = listOf(
            Hit("https://github.com/koan", "Pull request #42", Source.History, visits = 2, lastUsed = daysAgo(1)),
            Hit("https://mail.example.com/", "Inbox", Source.History, visits = 1, lastUsed = daysAgo(1)),
        )
        assertTrue(rank("hub", emptyList(), pages, emptyList(), now = now).isEmpty)
        assertEquals(1, rank("req", emptyList(), pages, emptyList(), now = now).pages.size)
        assertEquals(1, rank("pull req", emptyList(), pages, emptyList(), now = now).pages.size)
        assertEquals("https://mail.example.com/", rank("example", emptyList(), pages, emptyList(), now = now).pages.firstOrNull()?.url)
    }

    @Test fun `one row per page, query ignored, and an open tab hides its history`() {
        val pages = listOf(
            Hit("https://docs.example.com/d/1?usp=sharing", "Doc one", Source.History, visits = 5, lastUsed = daysAgo(1)),
            Hit("https://docs.example.com/d/1?hl=fr", "Doc one", Source.History, visits = 2, lastUsed = daysAgo(1)),
            Hit("https://example.org/?ref=x", "Example org", Source.History, visits = 4, lastUsed = daysAgo(1)),
        )
        val r = rank("doc", tabs, pages, emptyList(), now = now)
        assertEquals(1, r.pages.count { it.url.startsWith("https://docs.example.com/d/1") })
        val s = rank("exa", tabs, pages, emptyList(), now = now)
        assertFalse(s.pages.any { pageKey(it.url) == "example.org" })
        assertEquals(listOf("https://example.org/"), s.tabs.map { it.url })
    }

    @Test fun `frecency puts recent and frequent first`() {
        val pages = listOf(
            Hit("https://example.com/old", "Example old", Source.History, visits = 20, lastUsed = daysAgo(200)),
            Hit("https://example.com/new", "Example new", Source.History, visits = 3, lastUsed = daysAgo(1)),
        )
        assertEquals("https://example.com/new", rank("example", emptyList(), pages, emptyList(), now = now).pages.first().url)
    }

    @Test fun `at most two tabs and two pages`() {
        val many = (1..6).map { Hit("https://example.com/p$it", "Example $it", Source.History, visits = it, lastUsed = daysAgo(1)) }
        val manyTabs = (1..5).map { Hit("https://example.org/t$it", "Example tab $it", Source.Tab) }
        val r = rank("example", manyTabs, many, emptyList(), now = now)
        assertEquals(2, r.tabs.size)
        assertEquals(2, r.pages.size)
    }

    @Test fun `scopes keep only their source and have no top hit`() {
        val bookmarks = listOf(Hit("https://example.com/docs", "Docs (bookmark)", Source.Bookmark))
        val r = rank("example", tabs, history, bookmarks, scope = Scope.Bookmarks, now = now)
        assertNull(r.top)
        assertTrue(r.tabs.isEmpty())
        assertTrue(r.pages.all { it.source == Source.Bookmark })
        // An empty "@history" lists it all, except the page open in a tab.
        assertEquals(2, rank("", tabs, history, emptyList(), scope = Scope.History, now = now).pages.size)
    }

    @Test fun `nothing typed, nothing shown`() {
        assertTrue(rank("  ", tabs, history, emptyList(), now = now).isEmpty)
    }

    @Test fun `completion fills the host first, then the address`() {
        val top = history.first()
        assertEquals("example.com", completion("exa", top))
        assertEquals("EXAmple.com", completion("EXA", top))
        assertEquals("example.com/docs", completion("example.com/", top))
        assertNull(completion("docs", top))
        assertNull(completion("exa mple", top))
    }

    @Test fun `only places visited enough complete inline, never sign-in or redirect hops`() {
        val pages = listOf(
            Hit("https://accounts.example.com/signin", "Sign in", Source.History, visits = 30, lastUsed = daysAgo(1)),
            Hit("https://www.example.com/accounts/SetSID", "Redirection", Source.History, visits = 30, lastUsed = daysAgo(1)),
            Hit("https://once.example/", "Once, long ago", Source.History, visits = 1, lastUsed = daysAgo(80)),
            Hit("https://app.example/login", "Log in", Source.History, visits = 9, lastUsed = daysAgo(1)),
        )
        assertNull(rank("acc", emptyList(), pages, emptyList(), now = now).top)
        assertNull(rank("www.example.com/acc", emptyList(), pages, emptyList(), now = now).top)
        assertNull(rank("onc", emptyList(), pages, emptyList(), now = now).top)
        assertNull(rank("app.example/l", emptyList(), pages, emptyList(), now = now).top)
        // Still offered as rows, just not typed-to.
        assertEquals(1, rank("acc", emptyList(), pages, emptyList(), now = now).pages.size)
        assertEquals("https://www.example.com/docs", rank("exa", emptyList(), history, emptyList(), now = now).top?.url)
    }

    @Test fun `redirect pages are recognised`() {
        assertTrue(HistoryFilter.isRedirect("https://accounts.example.fr/accounts/SetSID?ssdc=1", ""))
        assertTrue(HistoryFilter.isRedirect("https://example.com/r", "Redirection"))
        assertTrue(HistoryFilter.isRedirect("https://example.com/r", "Redirecting…"))
        assertTrue(HistoryFilter.isRedirect("https://example.com/r", "302 Found"))
        assertFalse(HistoryFilter.isRedirect("https://example.com/r", "Found: a story"))
        assertFalse(HistoryFilter.isRedirect("https://example.com/redirects-explained", "How redirects work"))
        assertTrue(HistoryFilter.isAuth("https://login.example.com/"))
        assertTrue(HistoryFilter.isAuth("https://example.com/oauth2/authorize?x=1"))
        assertFalse(HistoryFilter.isAuth("https://example.com/blog/login-tips-for-all"))
        assertFalse(HistoryFilter.isAuth("https://example.com/"))
    }
}
