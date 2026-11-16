package dev.nerzouille.koan.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nerzouille.koan.sync.FakeStorage
import dev.nerzouille.koan.sync.SyncHttpRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import javax.crypto.spec.SecretKeySpec

/**
 * Two-way sync end to end (audit 9.1): [SyncWrite] with a real Room, DataStore and prefs, against
 * [FakeStorage]. Fake data only (example.com, made-up names).
 */
@RunWith(AndroidJUnit4::class)
class SyncWriteTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, KoanDatabase::class.java).allowMainThreadQueries().build()
    private val dao = db.dao()
    private val settings = Settings(context)
    private val hidden = HiddenLogins(context)
    private val logins = LoginStore(dao, AesGcmBox { SecretKeySpec(ByteArray(32) { 5 }, "AES") }, hidden)
    private val deletions = HistoryDeletions(context)
    private val server = FakeStorage()
    private val write = SyncWrite(context, settings, dao, logins, hidden, deletions)

    @After
    fun close() = db.close()

    private suspend fun switches(passwords: Boolean = false, history: Boolean = false, edits: Boolean = false) {
        settings.setSyncBack(SyncBackChoice(passwords = passwords, history = history))
        settings.setSyncBackPasswordEdits(edits)
    }

    /** An account login on another site, so the passwords backup has something to keep. */
    private fun accountLogin() = server.put(
        "passwords",
        JSONObject().put("id", "{account-login}").put("hostname", "https://example.org").put("formSubmitURL", "https://example.org")
            .put("username", "someone").put("password", "account-secret").put("usernameField", "").put("passwordField", ""),
    )

    private fun backupLines(collection: String): List<String> =
        File(context.filesDir, "sync-backup").listFiles().orEmpty().filter { it.name.startsWith("$collection-") }.flatMap { it.readLines() }

    private fun index(predicate: (SyncHttpRequest) -> Boolean) = server.log.indexOfFirst(predicate)

    private fun assertOnlyWrote(vararg collections: String) {
        val written = server.posts.map { it.url.path.substringAfterLast('/') }.toSet()
        assertTrue("wrote $written", collections.toSet().containsAll(written))
    }

    // ---- passwords ----

    @Test
    fun aPhoneLoginGoesUpAfterTheBackupConditionedOnTheTimestamp() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "user@example.com", "phone-secret")
        val before = server.lastModified("passwords")!!

        write.run(server.client(), force = true)

        // The backup (whole collection, still encrypted) is read before the first POST.
        val backupRead = index { it.method == "GET" && it.url.path.endsWith("/storage/passwords") && "limit=1000" in it.url.query.orEmpty() }
        val firstPost = index { it.method == "POST" }
        assertTrue(backupRead in 0 until firstPost)
        assertEquals(1, backupLines("passwords").size)
        assertFalse("the backup stays encrypted", backupLines("passwords").single().contains("account-secret"))

        val post = server.posts.single()
        assertEquals(before, post.headers["X-If-Unmodified-Since"])
        val sent = server.postedCleartexts(post).single()
        assertEquals("https://example.com", sent.getString("hostname"))
        assertEquals("user@example.com", sent.getString("username"))
        assertEquals(2, server.cleartexts("passwords").size)
        assertTrue(settings.syncBack.first().passwords)
        assertTrue(write.status.value.getValue("passwords").startsWith("1 added"))
        assertOnlyWrote("passwords")
    }

    @Test
    fun theBackupIsTakenOnlyOnce() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        write.run(server.client(), force = true)
        logins.addManual("example.net", "b", "two")
        write.run(server.client(), force = true)

        assertEquals(1, File(context.filesDir, "sync-backup").listFiles().orEmpty().size)
        assertEquals(2, server.posts.size)
    }

    @Test
    fun firstThreeAreReadBackThenBatchesOfFiftyEachOnThePreviousTimestamp() = runTest {
        switches(passwords = true)
        accountLogin()
        repeat(60) { logins.addManual("site$it.example.com", "user$it", "secret$it") }

        write.run(server.client(), force = true)

        val posts = server.posts
        assertEquals(listOf(3, 50, 7), posts.map { JSONArray(String(it.body!!)).length() })
        // The three first records are fetched back one by one between the first and second POST.
        val first = server.log.indexOf(posts[0])
        val second = server.log.indexOf(posts[1])
        val readBacks = server.log.subList(first + 1, second).filter { it.method == "GET" && it.url.path.count { c -> c == '/' } > 4 }
        assertEquals(3, readBacks.size)
        // Every POST is conditioned on what the server said after the previous one.
        posts.zipWithNext().forEach { (_, next) -> assertTrue(next.headers.containsKey("X-If-Unmodified-Since")) }
        assertEquals(61, server.cleartexts("passwords").size)
        assertTrue(settings.syncBack.first().passwords)
    }

    @Test
    fun aWrongEngineVersionStopsBeforeAnyWriteAndTurnsTheSwitchOff() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        server.metaGlobal = FakeStorage.meta(passwordsVersion = 2)

        write.run(server.client(), force = true)

        assertTrue(server.posts.isEmpty())
        assertTrue(backupLines("passwords").isEmpty())
        assertFalse(settings.syncBack.first().passwords)
        assertTrue(write.status.value.getValue("passwords").startsWith("stopped"))
    }

    @Test
    fun aSyncIdChangedSinceTheLastRunStops() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        write.run(server.client(), force = true)
        assertEquals(1, server.posts.size)

        server.metaGlobal = FakeStorage.meta(passwordsSyncId = "reset-on-another-device")
        logins.addManual("example.net", "b", "two")
        write.run(server.client(), force = true)

        assertEquals("no second write", 1, server.posts.size)
        assertFalse(settings.syncBack.first().passwords)
    }

    @Test
    fun anUnreadableMetaGlobalStops() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        server.metaGlobal = null

        write.run(server.client(), force = true)

        assertTrue(server.posts.isEmpty())
        assertFalse(settings.syncBack.first().passwords)
    }

    @Test
    fun aConflictOnPostStopsAndTurnsTheSwitchOff() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        server.concurrentWriteBeforeNextPost = "passwords"

        write.run(server.client(), force = true)

        assertFalse(settings.syncBack.first().passwords)
        assertTrue(write.status.value.getValue("passwords").contains("412"))
        assertFalse(server.cleartexts("passwords").values.any { it.optString("username") == "a" })
    }

    @Test
    fun aBusyServerKeepsTheSwitchOn() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        server.nextPostStatus = 503

        write.run(server.client(), force = true)

        assertTrue(settings.syncBack.first().passwords)
        assertTrue(write.status.value.getValue("passwords").startsWith("server busy"))
    }

    @Test
    fun aNetworkErrorKeepsTheSwitchOn() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        server.nextPostThrows = IOException("offline")

        write.run(server.client(), force = true)

        assertTrue(settings.syncBack.first().passwords)
        assertTrue(write.status.value.getValue("passwords").startsWith("network error"))
    }

    @Test
    fun anotherHttpErrorStops() = runTest {
        switches(passwords = true)
        accountLogin()
        logins.addManual("example.com", "a", "one")
        server.nextPostStatus = 400

        write.run(server.client(), force = true)

        assertFalse(settings.syncBack.first().passwords)
    }

    /** A passwords backup that read nothing means something is off with the account: stop. */
    @Test
    fun anEmptyPasswordsBackupStops() = runTest {
        switches(passwords = true)
        logins.addManual("example.com", "a", "one")

        write.run(server.client(), force = true)

        assertTrue(server.posts.isEmpty())
        assertFalse(settings.syncBack.first().passwords)
    }

    @Test
    fun accountLoginsAreNotChangedWhileEditsAreOff() = runTest {
        switches(passwords = true, edits = false)
        accountLogin()

        write.run(server.client(), force = true)

        assertTrue("nothing new on the phone: nothing sent", server.posts.isEmpty())
        assertEquals("account-secret", server.cleartexts("passwords").getValue("{account-login}").getString("password"))
    }

    @Test
    fun bothSwitchesOffMeansNoRequestAtAll() = runTest {
        switches()
        logins.addManual("example.com", "a", "one")
        dao.recordVisit("https://example.com/", "Example", 1)

        write.run(server.client(), force = true)

        assertTrue(server.log.isEmpty())
    }

    // ---- history ----

    private fun accountVisit(id: String, url: String) = server.put(
        "history",
        JSONObject().put("id", id).put("histUri", url).put("title", "Account $id")
            .put("visits", JSONArray().put(JSONObject().put("date", 1_750_000_000_000_000L).put("type", 1))),
    )

    @Test
    fun historyGoesUpAndARemovedPageGoesAsATombstone() = runTest {
        switches(history = true)
        accountVisit("hist-kept", "https://example.com/kept")
        accountVisit("hist-gone", "https://example.com/gone")
        dao.recordVisit("https://example.com/new", "New page", 1_760_000_000_000)
        deletions.updateIds(mapOf("https://example.com/kept" to "hist-kept", "https://example.com/gone" to "hist-gone"))
        deletions.add(listOf("https://example.com/gone"))

        write.run(server.client(), force = true)

        val sent = server.posts.flatMap { server.postedCleartexts(it) }
        assertEquals(listOf(JSONObject().put("id", "hist-gone").put("deleted", true).toString()), sent.filter { it.optBoolean("deleted") }.map { it.toString() })
        assertEquals(listOf("https://example.com/new"), sent.filterNot { it.optBoolean("deleted") }.map { it.getString("histUri") })
        assertTrue(server.cleartexts("history").getValue("hist-gone").optBoolean("deleted"))
        assertFalse(server.cleartexts("history").getValue("hist-kept").optBoolean("deleted"))
        assertTrue("the deletion is done", deletions.pending().isEmpty())
        assertFalse("https://example.com/gone" in deletions.ids())
        assertTrue("https://example.com/new" in deletions.ids())
        assertOnlyWrote("history")
    }

    /** "Clear browsing data" only hides synced pages here: they must never reach the account (PRIVACY.md). */
    @Test
    fun hiddenButNotRemovedPagesAreNeverDeletedFromTheAccount() = runTest {
        switches(history = true)
        accountVisit("hist-1", "https://example.com/one")
        dao.mergeSyncedVisits(listOf(VisitEntity("https://example.com/one", "Account hist-1", 1000, 1)))
        dao.hideAllSyncedVisits()
        deletions.updateIds(mapOf("https://example.com/one" to "hist-1"))

        write.run(server.client(), force = true)

        assertTrue(server.posts.isEmpty())
        assertFalse(server.cleartexts("history").getValue("hist-1").optBoolean("deleted"))
        assertEquals("up to date", write.status.value.getValue("history").substringBefore(" ("))
    }

    @Test
    fun aPendingDeletionTheAccountNeverHadIsDroppedWithoutWriting() = runTest {
        switches(history = true)
        accountVisit("hist-1", "https://example.com/one")
        dao.mergeSyncedVisits(listOf(VisitEntity("https://example.com/one", "Account hist-1", 1000, 1)))
        deletions.add(listOf("https://example.com/never-synced"))

        write.run(server.client(), force = true)

        assertTrue(server.posts.isEmpty())
        assertTrue(deletions.pending().isEmpty())
    }
}
