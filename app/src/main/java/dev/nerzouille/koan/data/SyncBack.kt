package dev.nerzouille.koan.data

import android.util.Log
import dev.nerzouille.koan.sync.BookmarkUpload
import dev.nerzouille.koan.sync.HistoryUpload
import dev.nerzouille.koan.sync.LoginUpload
import dev.nerzouille.koan.sync.PhoneBookmark
import dev.nerzouille.koan.sync.PhoneLogin
import dev.nerzouille.koan.sync.PhoneVisit
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.UploadPlan
import dev.nerzouille.koan.sync.WriteGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Two-way sync, **dry run only** (docs/SYNC_WRITE.md): after a pull, for each collection the owner
 * turned on, plans what would be sent and logs the plan's summary (counts and field names, never a
 * password, username, URL or title). There is no code path that sends: the real upload (snapshot,
 * conditional batch, undo journal) comes after the owner's tests on a test account.
 */
class SyncBack(
    private val choice: suspend () -> SyncBackChoice,
    private val phone: PhoneData,
    /** Where summaries go (logcat in the app, a list in tests). */
    private val report: (String) -> Unit = { Log.i(TAG, it) },
) {
    suspend fun dryRun(client: SyncClient) = withContext(Dispatchers.IO) {
        val on = choice()
        if (!on.any) return@withContext
        // Read-only: is the account in a state where writing would be allowed at all?
        val meta = runCatching { client.metaGlobal() }.getOrNull()
        if (on.passwords) step("passwords", meta) { passwords(client) }
        if (on.history) step("history", meta) { history(client) }
        if (on.bookmarks) step("bookmarks", meta) { bookmarks(client) }
    }

    private inline fun step(collection: String, meta: JSONObject?, plan: () -> UploadPlan) {
        // The sync id Koan last saw isn't tracked yet: a real upload must (rule 4).
        val refusal = WriteGuard.refusal(meta, collection, knownSyncId = null)
        runCatching(plan)
            .onSuccess { report("dry run, nothing sent: ${it.summary()}" + (refusal?.let { r -> "; a real upload would be refused: $r" } ?: "")) }
            .onFailure { report("dry run $collection failed: ${it.javaClass.simpleName}") }
    }

    private suspend fun passwords(client: SyncClient): UploadPlan {
        val server = decrypted(client, "passwords")
        val serverIds = server.mapTo(HashSet()) { it.optString("id") }
        val serverPairs = server.mapTo(HashSet()) { LoginUpload.origin(it.optString("hostname")) to it.optString("username") }
        return LoginUpload.plan(phone.logins(), serverIds, serverPairs)
    }

    private suspend fun history(client: SyncClient): UploadPlan {
        val server = client.getRecentRecords("history", HISTORY_LIMIT).mapNotNull { record ->
            runCatching { client.decryptRecord("history", record) }.getOrNull()?.takeIf { !it.optBoolean("deleted") }
        }.associateBy { it.optString("histUri") }
        return HistoryUpload.plan(phone.visits(HISTORY_LIMIT), server)
    }

    private suspend fun bookmarks(client: SyncClient): UploadPlan {
        val plan = BookmarkUpload.plan(phone.bookmarks(), decrypted(client, "bookmarks"))
        BookmarkUpload.problem(plan.records)?.let { throw IllegalStateException("bookmark batch: $it") }
        return plan
    }

    private fun decrypted(client: SyncClient, collection: String): List<JSONObject> =
        client.getRecords(collection).mapNotNull { record ->
            runCatching { client.decryptRecord(collection, record) }.getOrNull()?.takeIf { !it.optBoolean("deleted") }
        }

    private companion object {
        const val TAG = "KoanSyncBack"
        const val HISTORY_LIMIT = 5000
    }
}

/** What the phone has of its own (logins saved here, pages visited here, bookmarks added here). */
interface PhoneData {
    suspend fun logins(): List<PhoneLogin>
    suspend fun visits(limit: Int): List<PhoneVisit>
    suspend fun bookmarks(): List<PhoneBookmark>
}

/** [PhoneData] from Room; logins are opened in memory (Keystore) for the plan only. */
class RoomPhoneData(private val dao: KoanDao, private val logins: LoginStore) : PhoneData {
    override suspend fun logins() = dao.allLogins().filter { it.local }.mapNotNull { row ->
        val login = logins.open(row) ?: return@mapNotNull null
        PhoneLogin(
            row.guid, row.origin, row.formActionOrigin, row.httpRealm, login.username, login.password,
            row.usernameField, row.passwordField, row.timeCreated, row.timeLastUsed, row.timePasswordChanged,
        )
    }

    override suspend fun visits(limit: Int) = dao.phoneVisits(limit).map { PhoneVisit(it.url, it.title, it.lastVisit) }

    override suspend fun bookmarks() = dao.phoneBookmarks().map { PhoneBookmark(it.url, it.title, it.createdAt) }
}
