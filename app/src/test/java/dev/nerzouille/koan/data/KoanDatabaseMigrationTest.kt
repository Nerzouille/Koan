package dev.nerzouille.koan.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every Room migration run against the exported schemas (`app/schemas`), with fake rows that must
 * survive (audit 9.2). Room validates each migrated schema against the next exported one.
 */
@RunWith(AndroidJUnit4::class)
class KoanDatabaseMigrationTest {
    private val db = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), KoanDatabase::class.java)

    private val all = arrayOf(
        KoanDatabase.MIGRATION_2_3, KoanDatabase.MIGRATION_3_4, KoanDatabase.MIGRATION_4_5,
        KoanDatabase.MIGRATION_5_6, KoanDatabase.MIGRATION_6_7, KoanDatabase.MIGRATION_7_8,
    )

    /** A v2 database with one space, one Zen tab and one phone tab (fake data only). */
    private fun createV2() = helper.createDatabase(db, 2).apply {
        execSQL("INSERT INTO spaces VALUES ('space-1', 'Example space', '🙂', '4278190335,4294901760', NULL, 0, 1)")
        execSQL(
            "INSERT INTO tabs VALUES ('tab-zen', 'zen-1', 'space-1', NULL, NULL, 'https://example.com/a', " +
                "'Example A', NULL, 0, 1, 0, 0, 0, 100, NULL, 'ZEN', 0, 0)",
        )
        execSQL(
            "INSERT INTO tabs VALUES ('tab-local', NULL, 'space-1', NULL, NULL, 'https://example.org/b', " +
                "'Example B', NULL, 0, 0, 0, 1, 0, 200, NULL, 'LOCAL', 0, 0)",
        )
        close()
    }

    @Test
    fun migrate2To3CopiesZenUrlOnlyForZenTabs() {
        createV2()
        helper.runMigrationsAndValidate(db, 3, true, KoanDatabase.MIGRATION_2_3).use { d ->
            d.query("SELECT koanTabId, zenUrl, cardOrder FROM tabs ORDER BY koanTabId").use { c ->
                assertTrue(c.moveToNext())
                assertEquals("tab-local", c.getString(0))
                assertTrue(c.isNull(1))
                assertTrue(c.isNull(2))
                assertTrue(c.moveToNext())
                assertEquals("tab-zen", c.getString(0))
                assertEquals("https://example.com/a", c.getString(1))
            }
        }
    }

    @Test
    fun migrate3To4AddsBookmarksWithUniqueUrl() {
        createV2()
        helper.runMigrationsAndValidate(db, 3, true, KoanDatabase.MIGRATION_2_3).close()
        helper.runMigrationsAndValidate(db, 4, true, KoanDatabase.MIGRATION_3_4).use { d ->
            d.execSQL("INSERT INTO bookmarks VALUES ('b1', 'https://example.com/', 'Example', 1)")
            val duplicate = runCatching { d.execSQL("INSERT INTO bookmarks VALUES ('b2', 'https://example.com/', 'Again', 2)") }
            assertTrue("the url index is unique", duplicate.isFailure)
        }
    }

    @Test
    fun migrate4To6KeepsVisitsAndMarksThemPhoneMade() {
        createV2()
        helper.runMigrationsAndValidate(db, 4, true, KoanDatabase.MIGRATION_2_3, KoanDatabase.MIGRATION_3_4).close()
        helper.runMigrationsAndValidate(db, 5, true, KoanDatabase.MIGRATION_4_5).use { d ->
            d.execSQL("INSERT INTO visits VALUES ('https://example.com/page', 'Example page', 10, 3)")
        }
        helper.runMigrationsAndValidate(db, 6, true, KoanDatabase.MIGRATION_5_6).use { d ->
            d.query("SELECT title, visitCount, fromSync FROM visits").use { c ->
                assertTrue(c.moveToNext())
                assertEquals("Example page", c.getString(0))
                assertEquals(3, c.getInt(1))
                assertEquals(0, c.getInt(2))
            }
            d.query("SELECT COUNT(*) FROM logins").use { c -> c.moveToNext(); assertEquals(0, c.getInt(0)) }
        }
    }

    @Test
    fun migrate6To7DropsOnlyRedirectPages() {
        createV2()
        helper.runMigrationsAndValidate(db, 6, true, *all.take(4).toTypedArray()).use { d ->
            d.execSQL("INSERT INTO visits VALUES ('https://example.com/kept', 'A real page', 10, 1, 0)")
            d.execSQL("INSERT INTO visits VALUES ('https://example.com/hop', 'Redirecting…', 11, 1, 0)")
        }
        helper.runMigrationsAndValidate(db, 7, true, KoanDatabase.MIGRATION_6_7).use { d ->
            d.query("SELECT url, hidden FROM visits").use { c ->
                assertTrue(c.moveToNext())
                assertEquals("https://example.com/kept", c.getString(0))
                assertEquals(0, c.getInt(1))
                assertFalse("the redirect page is gone", c.moveToNext())
            }
        }
    }

    @Test
    fun migrate7To8AddsAnEmptyClosedTabsTable() {
        createV2()
        helper.runMigrationsAndValidate(db, 7, true, *all.take(5).toTypedArray()).close()
        helper.runMigrationsAndValidate(db, 8, true, KoanDatabase.MIGRATION_7_8).use { d ->
            d.query("SELECT COUNT(*) FROM closed_tabs").use { c -> c.moveToNext(); assertEquals(0, c.getInt(0)) }
            d.query("SELECT COUNT(*) FROM tabs").use { c -> c.moveToNext(); assertEquals(2, c.getInt(0)) }
        }
    }

    /** The whole chain, then the real database class opens it and reads the rows through the DAO. */
    @Test
    fun fullChainFrom2OpensWithTheDaoAndKeepsTabsAndSpaces() = runTest {
        createV2()
        helper.runMigrationsAndValidate(db, 8, true, *all).close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(context, KoanDatabase::class.java, db)
            .addMigrations(*all)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = room.dao()
            assertEquals(listOf("Example space"), dao.spaces().map { it.name })
            val tabs = dao.tabs().associateBy { it.koanTabId }
            assertEquals(TabOrigin.ZEN, tabs.getValue("tab-zen").origin)
            assertEquals("https://example.com/a", tabs.getValue("tab-zen").zenUrl)
            assertEquals(TabOrigin.LOCAL, tabs.getValue("tab-local").origin)
            assertNull(tabs.getValue("tab-local").zenUrl)
        } finally {
            room.close()
        }
    }

    /** v1 only held M1's fake data: it is dropped and rebuilt, never migrated. */
    @Test
    fun version1IsRebuiltFromScratch() = runTest {
        helper.createDatabase(db, 1).close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(context, KoanDatabase::class.java, db)
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .addMigrations(*all)
            .allowMainThreadQueries()
            .build()
        try {
            assertTrue(room.dao().tabs().isEmpty())
        } finally {
            room.close()
        }
    }
}
