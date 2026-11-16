package dev.nerzouille.koan.ui.search

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nerzouille.koan.data.BookmarkEntity
import dev.nerzouille.koan.data.KoanDatabase
import dev.nerzouille.koan.privacy.PrivateBookmark
import dev.nerzouille.koan.search.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Private's search reads no history, and normal search never sees Private's bookmarks. */
@RunWith(AndroidJUnit4::class)
class PrivateSearchTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KoanDatabase::class.java)
        .allowMainThreadQueries().build()
    private val dao = db.dao()
    private val privateMarks = listOf(PrivateBookmark("https://example.org/private-mark", "Example private mark", 10))

    @After
    fun close() = db.close()

    private suspend fun seed() {
        dao.recordVisit("https://example.com/visited", "Example visited", 5)
        dao.insertBookmarkIfNew(BookmarkEntity("b1", "https://example.net/normal-mark", "Example normal mark", 7))
    }

    @Test
    fun privateSearchReadsNoHistory() = runTest {
        seed()
        for (q in listOf("", "example", "visited")) {
            val (visits, _) = localHits(dao, q, private = true, privateMarks = privateMarks)
            assertTrue("query '$q'", visits.isEmpty())
        }
    }

    @Test
    fun privateSearchShowsItsOwnBookmarksFirstThenTheNormalOnes() = runTest {
        seed()
        val (_, marks) = localHits(dao, "example", private = true, privateMarks = privateMarks)
        assertEquals(listOf("https://example.org/private-mark", "https://example.net/normal-mark"), marks.map { it.url })
        assertTrue(marks.all { it.source == Source.Bookmark })
    }

    @Test
    fun normalSearchNeverSeesPrivateBookmarks() = runTest {
        seed()
        for (q in listOf("", "example", "private")) {
            val (visits, marks) = localHits(dao, q, private = false, privateMarks = privateMarks)
            assertTrue("query '$q'", marks.none { it.url.contains("private-mark") })
            if (q != "private") assertTrue(visits.isNotEmpty())
        }
    }
}
