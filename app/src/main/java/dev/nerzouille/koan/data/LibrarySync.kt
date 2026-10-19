package dev.nerzouille.koan.data

import android.util.Log
import dev.nerzouille.koan.search.HistoryFilter
import dev.nerzouille.koan.sync.SyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The Mozilla account's passwords, history and bookmarks, copied to the phone read-only (nothing
 * is ever written back). Runs after a Spaces pull, at most every [MIN_INTERVAL_MS]. A failure here
 * never fails the Spaces sync. Logs say how many records came and an error's class, nothing more:
 * records carry secrets.
 */
class LibrarySync(private val dao: KoanDao, private val logins: LoginStore) {
    private var lastPullAt = 0L

    suspend fun pull(client: SyncClient, force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - lastPullAt < MIN_INTERVAL_MS) return@withContext
        lastPullAt = System.currentTimeMillis()
        step("passwords") { pullLogins(client) }
        step("bookmarks") { pullBookmarks(client) }
        step("history") { pullHistory(client) }
    }

    private inline fun step(name: String, block: () -> Unit) {
        runCatching(block).onFailure { Log.w(TAG, "$name pull failed: ${it.javaClass.simpleName}") }
    }

    private suspend fun pullLogins(client: SyncClient) {
        val rows = decrypted(client, "passwords").mapNotNull { (id, r) -> loginOf(id, r) }
        dao.replaceSyncedLogins(rows)
        Log.i(TAG, "passwords: ${rows.size}")
    }

    private fun loginOf(id: String, r: JSONObject): LoginEntity? {
        val origin = r.optString("hostname").takeIf { it.isNotEmpty() } ?: return null
        val password = r.optString("password")
        if (password.isEmpty()) return null
        return LoginEntity(
            guid = id,
            origin = origin.trimEnd('/'),
            formActionOrigin = r.optString("formSubmitURL").ifEmpty { null },
            httpRealm = r.optString("httpRealm").ifEmpty { null },
            usernameField = r.optString("usernameField"),
            passwordField = r.optString("passwordField"),
            secret = logins.seal(r.optString("username"), password),
            timeCreated = r.optLong("timeCreated"),
            timeLastUsed = r.optLong("timeLastUsed"),
            timePasswordChanged = r.optLong("timePasswordChanged"),
            local = false,
        )
    }

    private suspend fun pullBookmarks(client: SyncClient) {
        val rows = decrypted(client, "bookmarks").mapNotNull { (id, r) ->
            val url = r.optString("bmkUri")
            if (r.optString("type") != "bookmark" || !url.startsWith("http")) return@mapNotNull null
            BookmarkEntity("sync:$id", url, r.optString("title").ifEmpty { url }, r.optLong("dateAdded"))
        }
        dao.replaceSyncedBookmarks(rows)
        Log.i(TAG, "bookmarks: ${rows.size}")
    }

    private suspend fun pullHistory(client: SyncClient) {
        // The most recently changed pages only: history can be huge, search needs the recent.
        val rows = client.getRecentRecords("history", HISTORY_LIMIT).mapNotNull { record ->
            val r = runCatching { client.decryptRecord("history", record) }.getOrNull() ?: return@mapNotNull null
            if (r.optBoolean("deleted")) return@mapNotNull null
            val url = r.optString("histUri")
            // Redirect pages are not history, here or on the phone.
            if (!url.startsWith("http") || HistoryFilter.isRedirect(url, r.optString("title"))) return@mapNotNull null
            val visits = r.optJSONArray("visits")
            val dates = if (visits == null) emptyList() else List(visits.length()) { visits.optJSONObject(it)?.optLong("date") ?: 0L }
            val last = (dates.maxOrNull() ?: 0L) / 1000 // microseconds → ms
            VisitEntity(url, r.optString("title"), last, dates.size.coerceAtLeast(1), fromSync = true)
        }
        dao.mergeSyncedVisits(rows)
        Log.i(TAG, "history: ${rows.size}")
    }

    /** A collection's live records, decrypted (deleted ones and unreadable ones skipped). */
    private fun decrypted(client: SyncClient, collection: String): List<Pair<String, JSONObject>> =
        client.getRecords(collection).mapNotNull { record ->
            val id = record.optString("id").ifEmpty { return@mapNotNull null }
            val r = runCatching { client.decryptRecord(collection, record) }.getOrNull() ?: return@mapNotNull null
            if (r.optBoolean("deleted")) null else id to r
        }

    private companion object {
        const val TAG = "KoanLibrary"
        const val MIN_INTERVAL_MS = 30 * 60_000L
        const val HISTORY_LIMIT = 5000
    }
}
