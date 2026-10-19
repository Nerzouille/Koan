package dev.nerzouille.koan.ui.history

import dev.nerzouille.koan.data.VisitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryGroupsTest {
    private var clock = 10_000L
    private fun v(url: String) = VisitEntity(url, "Title", clock--, 1)

    @Test fun `a lone visit stays a page`() {
        val rows = groupVisits(listOf(v("https://example.com/a"), v("https://example.org/b")))
        assertEquals(2, rows.size)
        assertTrue(rows.all { it is HistoryItem.Page })
    }

    @Test fun `back-to-back visits to one site group, newest first`() {
        val rows = groupVisits(
            listOf(v("https://example.com/c"), v("https://www.example.com/b"), v("https://example.com/a"), v("https://example.org/x")),
        )
        assertEquals(2, rows.size)
        val group = rows[0] as HistoryItem.Group
        assertEquals("example.com", group.host)
        assertEquals(listOf("https://example.com/c", "https://www.example.com/b", "https://example.com/a"), group.pages.map { it.visit.url })
        assertTrue(group.newest > group.oldest)
        assertTrue(rows[1] is HistoryItem.Page)
    }

    @Test fun `the same site later again is a new group`() {
        val rows = groupVisits(
            listOf(v("https://example.com/b"), v("https://example.com/a"), v("https://example.org/x"), v("https://example.com/z")),
        )
        assertEquals(listOf(HistoryItem.Group::class, HistoryItem.Page::class, HistoryItem.Page::class), rows.map { it::class })
    }

    @Test fun `the same url twice in a row shows once`() {
        val rows = groupVisits(listOf(v("https://example.com/a"), v("https://example.com/a"), v("https://example.org/x")))
        assertEquals(listOf("https://example.com/a", "https://example.org/x"), rows.map { (it as HistoryItem.Page).visit.url })
    }

    @Test fun `a url and the same with a trailing slash show once, the newer`() {
        val rows = groupVisits(listOf(v("https://example.com/"), v("https://example.com"), v("https://example.org/x")))
        assertEquals(listOf("https://example.com/", "https://example.org/x"), rows.map { (it as HistoryItem.Page).visit.url })
        // Removing the row removes both.
        assertEquals(listOf("https://example.com/", "https://example.com"), rows[0].all.map { it.url })
    }

    @Test fun `removing a group removes every page in it`() {
        val group = groupVisits(listOf(v("https://example.com/b"), v("https://example.com/a/"), v("https://example.com/a")))[0] as HistoryItem.Group
        assertEquals(2, group.pages.size)
        assertEquals(3, group.all.size)
    }

    @Test fun `pages without a host never group`() {
        val rows = groupVisits(listOf(v("about:blank"), v("about:blank#2")))
        assertTrue(rows.all { it is HistoryItem.Page })
    }
}
