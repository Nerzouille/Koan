package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.PhoneBookmark
import dev.nerzouille.koan.sync.PhoneLogin
import dev.nerzouille.koan.sync.PhoneVisit
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.SyncCrypto
import dev.nerzouille.koan.sync.SyncHttpRequest
import dev.nerzouille.koan.sync.SyncHttpResponse
import dev.nerzouille.koan.sync.SyncHttpTransport
import dev.nerzouille.koan.sync.TokenServerCreds
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The dry run against a fake Sync server: it plans and reports, and only ever reads. */
class SyncBackTest {
    private val keys = SyncCrypto.KeyBundle(ByteArray(32) { 1 }, ByteArray(32) { 2 })
    private val now = 1_761_500_000_000L

    private fun bso(id: String, cleartext: JSONObject) =
        JSONObject().put("id", id).put("modified", 1.0).put("payload", SyncCrypto.encryptBSO(cleartext.toString().toByteArray(), keys))

    private inner class FakeStorage : SyncHttpTransport {
        val methods = mutableListOf<String>()
        override fun execute(request: SyncHttpRequest): SyncHttpResponse {
            methods += request.method
            val path = request.url.path
            val body = when {
                path.endsWith("/storage/meta/global") -> JSONObject().put("id", "global").put(
                    "payload",
                    JSONObject().put("storageVersion", 5).put("engines", JSONObject()
                        .put("passwords", JSONObject().put("version", 1).put("syncID", "aaaaaaaaaaaa"))
                        .put("history", JSONObject().put("version", 1).put("syncID", "bbbbbbbbbbbb"))
                        .put("bookmarks", JSONObject().put("version", 2).put("syncID", "cccccccccccc"))).toString(),
                ).toString()
                path.endsWith("/storage/passwords") -> JSONArray().put(
                    bso("server-login", JSONObject().put("id", "server-login").put("hostname", "https://example.org").put("username", "zoe").put("password", "fake")),
                ).toString()
                path.endsWith("/storage/history") -> JSONArray().toString()
                path.endsWith("/storage/bookmarks") -> JSONArray().put(
                    bso("mobile", JSONObject().put("id", "mobile").put("type", "folder").put("parentid", "places").put("children", JSONArray())),
                ).toString()
                else -> "{}"
            }
            return SyncHttpResponse(200, emptyMap(), body.toByteArray())
        }
    }

    private val phone = object : PhoneData {
        override suspend fun logins() = listOf(
            PhoneLogin("11111111-2222-3333-4444-555555555555", "https://example.com", null, null, "alice", "secret-fake", "u", "p", now, now, now),
            PhoneLogin("22222222-2222-3333-4444-555555555555", "https://example.org", null, null, "zoe", "secret-fake-2", "u", "p", now, now, now),
        )
        override suspend fun visits(limit: Int) = listOf(PhoneVisit("https://example.com/page", "Page", now))
        override suspend fun bookmarks() = listOf(PhoneBookmark("https://example.com/saved", "Saved", now))
    }

    private fun client(transport: SyncHttpTransport) =
        SyncClient(TokenServerCreds("1", "https://sync.example.com/1.5/1", "hawk-id", "hawk-key".toByteArray(), Long.MAX_VALUE), keys, emptyMap(), transport)

    @Test
    fun everythingOnPlansAllThreeAndOnlyReads() = runTest {
        val storage = FakeStorage()
        val reports = mutableListOf<String>()
        SyncBack({ SyncBackChoice(passwords = true, history = true, bookmarks = true) }, phone, reports::add).dryRun(client(storage))

        assertTrue(storage.methods.isNotEmpty())
        assertTrue("only GETs, got ${storage.methods}", storage.methods.all { it == "GET" })
        assertEquals(3, reports.size)
        assertTrue(reports[0], reports[0].startsWith("dry run, nothing sent: passwords: 2 record(s) to upload (new 1, changed 0, rewritten 0, merging into existing 1)"))
        assertTrue(reports[1], reports[1].startsWith("dry run, nothing sent: history: 1 record(s) to upload (new 1"))
        assertTrue(reports[2], reports[2].startsWith("dry run, nothing sent: bookmarks: 2 record(s) to upload (new 1, changed 0, rewritten 1"))
        reports.forEach { r ->
            assertFalse(r.contains("secret-fake"))
            assertFalse(r.contains("alice"))
            assertFalse(r.contains("example.com"))
            assertFalse(r.contains("refused"))
        }
    }

    @Test
    fun everythingOffDoesNothing() = runTest {
        val storage = FakeStorage()
        val reports = mutableListOf<String>()
        SyncBack({ SyncBackChoice() }, phone, reports::add).dryRun(client(storage))
        assertTrue(storage.methods.isEmpty())
        assertTrue(reports.isEmpty())
    }
}
