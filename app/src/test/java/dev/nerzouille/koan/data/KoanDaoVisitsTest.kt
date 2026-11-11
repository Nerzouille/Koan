package dev.nerzouille.koan.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The history queries two-way sync relies on, run on a real (in-memory) SQLite (audit 9.2). */
@RunWith(AndroidJUnit4::class)
class KoanDaoVisitsTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KoanDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.dao()

    @After
    fun close() = db.close()

    @Test
    fun phoneVisitsLeaveOutTheAccountsCopiesAndHiddenPages() = runTest {
        dao.recordVisit("https://example.com/phone", "Phone page", 30)
        dao.mergeSyncedVisits(listOf(VisitEntity("https://example.com/account", "Account page", 20, 4)))
        dao.recordVisit("https://example.com/removed", "Removed", 10)
        dao.mergeSyncedVisits(listOf(VisitEntity("https://example.com/removed-synced", "Synced", 5, 1)))
        dao.hideAllSyncedVisits()

        assertEquals(listOf("https://example.com/phone", "https://example.com/removed"), dao.phoneVisits(10).map { it.url })
    }

    @Test
    fun visitingAnAccountPageMakesItThePhones() = runTest {
        dao.mergeSyncedVisits(listOf(VisitEntity("https://example.com/a", "A", 20, 4)))
        dao.recordVisit("https://example.com/a", "", 50)

        val visit = dao.phoneVisits(10).single()
        assertEquals("A", visit.title)
        assertEquals(5, visit.visitCount)
        assertEquals(50, visit.lastVisit)
    }

    @Test
    fun mergeKeepsTheLargerFiguresAndDeleteVisitsRemovesOnlyTheGivenPages() = runTest {
        dao.recordVisit("https://example.com/a", "A", 100)
        dao.recordVisit("https://example.com/b", "B", 90)
        dao.mergeSyncedVisits(listOf(VisitEntity("https://example.com/a", "Other title", 40, 9)))
        dao.deleteVisits(listOf("https://example.com/b"))

        val a = dao.phoneVisits(10).single()
        assertEquals("A", a.title)
        assertEquals(100, a.lastVisit)
        assertEquals(9, a.visitCount)
    }

    @Test
    fun phoneVisitsAreNewestFirstAndLimited() = runTest {
        (1..5).forEach { dao.recordVisit("https://example.com/$it", "Page $it", it.toLong()) }
        assertEquals(listOf("https://example.com/5", "https://example.com/4"), dao.phoneVisits(2).map { it.url })
    }
}
