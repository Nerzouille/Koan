package dev.nerzouille.koan.sync

import org.json.JSONObject

/*
 * Two-way sync, the parts beyond the first upload plans (SyncBackPlan.kt): what Koan actually
 * writes, as pure functions (tested without a server). Only counts and field names are ever
 * logged from here.
 */

/** Passwords, tonight's rule: add only. A phone login the account already has in any form is left out. */
object PasswordAdds {
    /**
     * [records]: the upload records ([LoginUpload.plan]). Kept: an id the account doesn't have,
     * and a site + username pair it doesn't have either (Firefox would merge it into the
     * existing one, which is an edit).
     */
    fun onlyNew(records: List<JSONObject>, serverIds: Set<String>, serverPairs: Set<Pair<String, String>>): List<JSONObject> =
        records.filter { r ->
            r.optString("id") !in serverIds && (LoginUpload.origin(r.optString("hostname")) to r.optString("username")) !in serverPairs
        }
}

/**
 * Password edits and deletions (behind their own switch, off): a login of the account deleted on
 * the phone becomes a tombstone; a phone login that matches an account login by site + username
 * but has another password updates that record (its id and other fields kept, the password,
 * timePasswordChanged and timesUsed changed).
 */
object PasswordEdits {
    class Plan(val tombstones: List<JSONObject>, val edits: List<JSONObject>) {
        val records get() = tombstones + edits
        fun summary() = "passwords edits: ${edits.size} changed, ${tombstones.size} deleted"
    }

    /** [server]: the account's live login records (cleartexts). [deletedIds]: account logins deleted on the phone. */
    fun plan(phone: List<PhoneLogin>, server: List<JSONObject>, deletedIds: Set<String>, now: Long = System.currentTimeMillis()): Plan {
        val live = server.filter { !it.optBoolean("deleted") }
        val tombstones = live.filter { it.optString("id") in deletedIds }.map { tombstone(it.optString("id")) }
        val byPair = live.filter { it.optString("id") !in deletedIds }
            .associateBy { LoginUpload.origin(it.optString("hostname")) to it.optString("username") }
        val edits = phone.mapNotNull { login ->
            val match = byPair[LoginUpload.origin(login.origin) to login.username] ?: return@mapNotNull null
            if (match.optString("password") == login.password) return@mapNotNull null
            JSONObject(match.toString()).apply {
                put("password", login.password)
                put("timePasswordChanged", maxOf(login.timePasswordChanged, now).coerceAtMost(now))
                put("timeLastUsed", maxOf(optLong("timeLastUsed"), login.timeLastUsed))
                put("timesUsed", optInt("timesUsed", 0) + 1)
            }
        }
        return Plan(tombstones, edits)
    }
}

/** History deletions made on the phone: a tombstone for each record of those pages. */
object HistoryTombstones {
    /** [deletedUrls]: pages removed on the phone. [urlToId]: the account's history ids by page. */
    fun plan(deletedUrls: Set<String>, urlToId: Map<String, String>): List<JSONObject> =
        deletedUrls.mapNotNull { url -> urlToId[url]?.let(::tombstone) }

}

/** One history run: the phone's visits to send, and the account's records to delete. */
class HistoryWrite(val upload: UploadPlan, val tombstones: List<JSONObject>, val deleted: Set<String>)

object HistoryWritePlan {
    /**
     * [explicitDeletions]: pages removed one by one in History while two-way history was on,
     * recorded at that moment: the only deletions ever sent. A synced page that is only hidden on
     * the phone ("Clear browsing data", or removed while the switch was off) is deliberately not
     * an input: it never reaches the account (docs/PRIVACY.md). [accountByUrl]: the account's
     * records already read; [accountIds]: its history ids by page.
     */
    fun plan(
        phoneVisits: List<PhoneVisit>,
        explicitDeletions: Set<String>,
        accountByUrl: Map<String, JSONObject>,
        accountIds: Map<String, String>,
    ): HistoryWrite = HistoryWrite(
        upload = HistoryUpload.plan(phoneVisits.filter { it.url !in explicitDeletions }, accountByUrl),
        tombstones = HistoryTombstones.plan(explicitDeletions, accountIds),
        deleted = explicitDeletions,
    )
}

/** A deleted record as Firefox writes it: the cleartext `{id, deleted: true}`, encrypted like any other. */
fun tombstone(id: String): JSONObject = JSONObject().put("id", id).put("deleted", true)

/** The check after the first small batch: what came back decrypts to exactly what was sent. */
object ReadBack {
    /** Null if [back] has every field of [sent] with the same value; else the first field that differs (its name only). */
    fun mismatch(sent: JSONObject, back: JSONObject?): String? {
        if (back == null) return "missing"
        for (key in sent.keys()) {
            val a = sent.opt(key)
            val b = back.opt(key)
            if (a.toString() != b?.toString()) return key
        }
        return null
    }
}
