package dev.nerzouille.koan.sync

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Two-way sync planners (docs/SYNC_WRITE.md), fake data only. */
class SyncBackPlanTest {
    private val now = 1_761_500_000_000L

    private fun login(
        guid: String = "11111111-2222-3333-4444-555555555555",
        origin: String = "https://example.com/",
        formAction: String? = null,
        realm: String? = null,
        username: String = "alice",
        password: String = "hunter2-fake",
        changed: Long = now,
    ) = PhoneLogin(guid, origin, formAction, realm, username, password, "user", "pass", now - 1000, now, changed)

    // MARK: passwords

    @Test
    fun aFormLoginGetsAnEmptyFormActionNeverBothTargets() {
        val r = LoginUpload.record(login())
        assertEquals("https://example.com", r.getString("hostname"))
        assertEquals("", r.getString("formSubmitURL"))
        assertFalse(r.has("httpRealm"))
        assertNull(LoginUpload.problem(r))

        val http = LoginUpload.record(login(realm = "Staff area"))
        assertEquals("Staff area", http.getString("httpRealm"))
        assertFalse(http.has("formSubmitURL"))
        assertEquals("", http.getString("usernameField"))
        assertNull(LoginUpload.problem(http))
    }

    @Test
    fun formActionIsReducedToItsOrigin() {
        val r = LoginUpload.record(login(formAction = "https://Login.Example.com:8443/session/new"))
        assertEquals("https://login.example.com:8443", r.getString("formSubmitURL"))
    }

    @Test
    fun recordsFirefoxWouldRejectAreLeftOut() {
        val plan = LoginUpload.plan(
            listOf(
                login(guid = "a"),
                login(guid = "b", password = ""),
                login(guid = "c,d"),
                login(guid = "e", changed = now / 1000), // seconds by mistake
                login(guid = "f", origin = "https://accounts.firefox.com"),
            ),
            serverIds = emptySet(),
            serverLogins = emptySet(),
        )
        assertEquals(1, plan.records.size)
        assertEquals(listOf("empty password", "bad id", "timestamp not in ms", "Mozilla account login"), plan.invalid)
    }

    @Test
    fun countsNewChangedAndMergingLogins() {
        val plan = LoginUpload.plan(
            listOf(login(guid = "new-one", username = "bob"), login(guid = "already-up"), login(guid = "same-site", username = "carol")),
            serverIds = setOf("already-up"),
            serverLogins = setOf("https://example.com" to "carol"),
        )
        assertEquals(1, plan.new)
        assertEquals(1, plan.changed)
        assertEquals(1, plan.mergesIntoExisting)
    }

    @Test
    fun theSummaryNeverCarriesSecretsOrSites() {
        val plan = LoginUpload.plan(listOf(login(), login(guid = "x", password = "")), emptySet(), emptySet())
        val s = plan.summary()
        assertTrue(s.contains("passwords: 1 record(s)"))
        assertTrue(s.contains("password")) // the field name is fine…
        assertFalse(s.contains("hunter2-fake")) // …its value never
        assertFalse(s.contains("alice"))
        assertFalse(s.contains("example.com"))
        assertFalse(plan.toString().contains("hunter2-fake"))
    }

    // MARK: history

    @Test
    fun aNewPageGetsA12CharacterIdAndAVisitInMicroseconds() {
        val plan = HistoryUpload.plan(listOf(PhoneVisit("https://example.com/a", "A page", now)), emptyMap())
        val r = plan.records.single()
        assertTrue(isPlacesSyncId(r.getString("id")))
        assertEquals(now * 1000, r.getJSONArray("visits").getJSONObject(0).getLong("date"))
        assertEquals(1, plan.new)
        assertNull(HistoryUpload.problem(r))
    }

    @Test
    fun aKnownPageKeepsItsIdFieldsAndVisitsNewest20() {
        val visits = JSONArray()
        for (i in 0 until 20) visits.put(JSONObject().put("date", (now - 100_000L * (i + 1)) * 1000).put("type", 2))
        val server = JSONObject().put("id", "abcdefghijkl").put("histUri", "https://example.com/b").put("title", "Desk title")
            .put("visits", visits).put("futureField", 7)
        val plan = HistoryUpload.plan(listOf(PhoneVisit("https://example.com/b", "Phone title", now)), mapOf("https://example.com/b" to server))
        val r = plan.records.single()
        assertEquals("abcdefghijkl", r.getString("id"))
        assertEquals("Desk title", r.getString("title"))
        assertEquals(7, r.getInt("futureField"))
        val out = r.getJSONArray("visits")
        assertEquals(20, out.length())
        assertEquals(now * 1000, out.getJSONObject(0).getLong("date"))
        assertEquals(1, plan.changed)
        // The server copy given in is untouched.
        assertEquals(20, server.getJSONArray("visits").length())
    }

    @Test
    fun aVisitAlreadyThereIsNotSentAgainAndOddPagesAreLeftOut() {
        val server = JSONObject().put("id", "abcdefghijkl").put("histUri", "https://example.com/c")
            .put("visits", JSONArray().put(JSONObject().put("date", now * 1000).put("type", 1)))
        val plan = HistoryUpload.plan(
            listOf(PhoneVisit("https://example.com/c", "", now), PhoneVisit("about:blank", "", now)),
            mapOf("https://example.com/c" to server),
        )
        assertTrue(plan.records.isEmpty())
        assertEquals(listOf("not a web page"), plan.invalid)
    }

    @Test
    fun visitDatesInMillisecondsAreCaught() {
        val r = JSONObject().put("id", "abcdefghijkl").put("histUri", "https://example.com/")
            .put("visits", JSONArray().put(JSONObject().put("date", now).put("type", 1)))
        assertEquals("visit date not in µs", HistoryUpload.problem(r))
    }

    // MARK: bookmarks

    private val mobileFolder = JSONObject().put("id", "mobile").put("type", "folder").put("parentid", "places")
        .put("title", "mobile").put("children", JSONArray().put("existing0001")).put("unknownField", "keep")

    @Test
    fun newBookmarksGoUnderMobileWithTheFolderRewrittenInOneBatch() {
        val plan = BookmarkUpload.plan(
            listOf(PhoneBookmark("https://example.com/new", "New", now)),
            listOf(mobileFolder, JSONObject().put("id", "existing0001").put("type", "bookmark").put("bmkUri", "https://example.org/").put("parentid", "mobile")),
        )
        assertEquals(2, plan.records.size)
        val bookmark = plan.records[0]
        val folder = plan.records[1]
        assertEquals("mobile", bookmark.getString("parentid"))
        assertTrue(bookmark.getBoolean("hasDupe"))
        assertEquals("mobile", folder.getString("id"))
        assertEquals("keep", folder.getString("unknownField"))
        assertEquals(listOf("existing0001", bookmark.getString("id")), (0 until 2).map { folder.getJSONArray("children").getString(it) })
        assertEquals(1, plan.rewrites)
        assertNull(BookmarkUpload.problem(plan.records))
        // The folder read from the server is not modified in place.
        assertEquals(1, mobileFolder.getJSONArray("children").length())
    }

    @Test
    fun bookmarksTheAccountAlreadyHasAreSkipped() {
        val plan = BookmarkUpload.plan(
            listOf(PhoneBookmark("https://example.org/", "Dup", now)),
            listOf(mobileFolder, JSONObject().put("id", "existing0001").put("type", "bookmark").put("bmkUri", "https://example.org/")),
        )
        assertTrue(plan.records.isEmpty())
        assertEquals(1, plan.mergesIntoExisting)
    }

    @Test
    fun aTornTreeIsRefused() {
        val orphan = JSONObject().put("id", "abcdefghijkl").put("type", "bookmark").put("parentid", "mobile").put("bmkUri", "https://example.com/")
        assertEquals("parent folder not in the batch", BookmarkUpload.problem(listOf(orphan)))
        val folder = JSONObject().put("id", "mobile").put("type", "folder").put("children", JSONArray())
        assertEquals("not listed by its parent", BookmarkUpload.problem(listOf(orphan, folder)))
        assertEquals("places root", BookmarkUpload.problem(listOf(JSONObject().put("id", "places"))))
    }

    // MARK: guard

    private fun meta(version: Int = 5, passwordsVersion: Int = 1, declined: List<String> = emptyList()) = JSONObject()
        .put("storageVersion", version)
        .put("declined", JSONArray(declined))
        .put("engines", JSONObject().put("passwords", JSONObject().put("version", passwordsVersion).put("syncID", "pwSyncId0001")))

    @Test
    fun writesAreRefusedUnlessMetaGlobalIsWhatKoanExpects() {
        assertNull(WriteGuard.refusal(meta(), "passwords", "pwSyncId0001"))
        assertEquals("meta/global unreadable", WriteGuard.refusal(null, "passwords", null))
        assertTrue(WriteGuard.refusal(meta(version = 4), "passwords", null)!!.startsWith("storage version"))
        assertTrue(WriteGuard.refusal(meta(passwordsVersion = 2), "passwords", null)!!.contains("engine version"))
        assertEquals("passwords is declined", WriteGuard.refusal(meta(declined = listOf("passwords")), "passwords", null))
        assertEquals("history engine missing", WriteGuard.refusal(meta(), "history", null))
        assertEquals("passwords syncID changed since the last read", WriteGuard.refusal(meta(), "passwords", "other"))
    }
}
