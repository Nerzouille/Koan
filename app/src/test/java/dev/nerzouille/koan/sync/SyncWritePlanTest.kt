package dev.nerzouille.koan.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Two-way sync planners, on fake logins and pages (example.com). */
class SyncWritePlanTest {
    private fun phone(guid: String, origin: String, user: String, pass: String) =
        PhoneLogin(guid, origin, origin, null, user, pass, "u", "p", 1_700_000_000_000, 1_700_000_000_000, 1_700_000_000_000)

    private fun server(id: String, host: String, user: String, pass: String) = JSONObject()
        .put("id", id).put("hostname", host).put("formSubmitURL", host).put("username", user).put("password", pass)
        .put("usernameField", "u").put("passwordField", "p").put("timesUsed", 4)
        .put("timeCreated", 1_600_000_000_000).put("timeLastUsed", 1_600_000_000_000).put("timePasswordChanged", 1_600_000_000_000)

    @Test fun addsOnlyWhatTheAccountHasNoneOf() {
        val records = listOf(
            LoginUpload.record(phone("new-1", "https://a.example.com", "ann", "x")),
            LoginUpload.record(phone("known-id", "https://b.example.com", "bob", "y")),
            LoginUpload.record(phone("new-2", "https://c.example.com/login", "cat", "z")),
        )
        val kept = PasswordAdds.onlyNew(records, serverIds = setOf("known-id"), serverPairs = setOf("https://c.example.com" to "cat"))
        assertEquals(listOf("new-1"), kept.map { it.getString("id") })
    }

    @Test fun editsKeepTheRecordAndBumpCounters() {
        val now = 1_800_000_000_000
        val plan = PasswordEdits.plan(
            phone = listOf(phone("local", "https://a.example.com", "ann", "new-pass")),
            server = listOf(server("acct", "https://a.example.com", "ann", "old-pass"), server("gone", "https://d.example.com", "dan", "p")),
            deletedIds = setOf("gone"),
            now = now,
        )
        assertEquals(1, plan.edits.size)
        val e = plan.edits.single()
        assertEquals("acct", e.getString("id"))
        assertEquals("new-pass", e.getString("password"))
        assertEquals(5, e.getInt("timesUsed"))
        assertEquals(now, e.getLong("timePasswordChanged"))
        assertEquals(listOf("gone"), plan.tombstones.map { it.getString("id") })
        assertTrue(plan.tombstones.single().getBoolean("deleted"))
    }

    @Test fun samePasswordIsNoEdit() {
        val plan = PasswordEdits.plan(listOf(phone("l", "https://a.example.com", "ann", "same")), listOf(server("acct", "https://a.example.com", "ann", "same")), emptySet())
        assertTrue(plan.records.isEmpty())
    }

    @Test fun historyTombstonesOnlyForKnownPages() {
        val t = HistoryTombstones.plan(setOf("https://example.com/a", "https://example.com/never-synced"), mapOf("https://example.com/a" to "abcdefghijkl"))
        assertEquals(1, t.size)
        assertEquals("abcdefghijkl", t.single().getString("id"))
        assertTrue(t.single().getBoolean("deleted"))
    }

    @Test fun readBackComparesEveryFieldSent() {
        val sent = JSONObject().put("id", "x").put("password", "p").put("timesUsed", 1)
        assertNull(ReadBack.mismatch(sent, JSONObject(sent.toString()).put("extra", true)))
        assertEquals("password", ReadBack.mismatch(sent, JSONObject(sent.toString()).put("password", "q")))
        assertEquals("missing", ReadBack.mismatch(sent, null))
    }
}
