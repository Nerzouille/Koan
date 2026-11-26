package dev.nerzouille.koan.ui.start

import dev.nerzouille.koan.ui.browser.rubberBand
import dev.nerzouille.koan.ui.search.CopiedLinks
import mozilla.components.browser.state.state.content.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The start page's quick rows, the copied link, the prototypes' rubber band. */
class StartQuickTest {
    @Test fun `a copied link is one web address and nothing else`() {
        assertEquals("https://example.com/a?b=1", CopiedLinks.linkIn("  https://example.com/a?b=1 \n"))
        assertEquals("https://example.org/path", CopiedLinks.linkIn("example.org/path"))
        assertNull(CopiedLinks.linkIn("hello world"))
        assertNull(CopiedLinks.linkIn("see https://example.com"))
        assertNull(CopiedLinks.linkIn("localhost"))
        assertNull(CopiedLinks.linkIn("ftp://example.com"))
        assertNull(CopiedLinks.linkIn(""))
    }

    @Test fun `recent pages skip the tab under the start page and repeats`() {
        val visits = listOf(
            RecentPage("https://example.com/1", "One"),
            RecentPage("https://example.com/now", "Under"),
            RecentPage("https://example.com/1", "One again"),
            RecentPage("about:blank", ""),
            RecentPage("https://example.com/2", "Two"),
            RecentPage("https://example.com/3", "Three"),
            RecentPage("https://example.com/4", "Four"),
            RecentPage("https://example.com/5", "Five"),
        )
        val shown = recentPagesToShow(visits, skip = "https://example.com/now", max = 4)
        assertEquals(listOf("1", "2", "3", "4"), shown.map { it.url.substringAfterLast('/') })
    }

    @Test fun `the last download shows for an hour, finished and not private`() {
        val now = 10_000_000L
        fun d(id: String, at: Long, status: DownloadState.Status = DownloadState.Status.COMPLETED, private: Boolean = false) =
            DownloadState(
                url = "https://example.com/$id", id = id, createdTime = at, status = status, private = private, directoryPath = "Download",
            )
        assertEquals("b", lastRecentDownload(listOf(d("a", now - 5_000), d("b", now - 1_000)), now)?.id)
        assertNull(lastRecentDownload(listOf(d("a", now - 61 * 60 * 1000L)), now))
        assertNull(lastRecentDownload(listOf(d("a", now - 1_000, DownloadState.Status.DOWNLOADING)), now))
        assertNull(lastRecentDownload(listOf(d("a", now - 1_000, private = true)), now))
    }

    @Test fun `the page slides with the finger, never past the max`() {
        val max = 330f
        assertEquals(0f, rubberBand(-20f, max))
        assertTrue(rubberBand(10f, max) in 6.5f..7f)
        var last = 0f
        for (dy in 1..3000) {
            val y = rubberBand(dy.toFloat(), max)
            assertTrue(y > last && y < max)
            last = y
        }
    }
}
