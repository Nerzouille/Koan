package dev.nerzouille.koan.data

import android.content.Context
import android.util.Log
import dev.nerzouille.koan.sync.HistoryWritePlan
import dev.nerzouille.koan.sync.LoginUpload
import dev.nerzouille.koan.sync.PasswordAdds
import dev.nerzouille.koan.sync.PasswordEdits
import dev.nerzouille.koan.sync.PostOutcome
import dev.nerzouille.koan.sync.ReadBack
import dev.nerzouille.koan.sync.SyncClient
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.WriteGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Two-way sync (M6, docs/SYNC_WRITE.md), for the collections the owner turned on: passwords
 * (new logins made on the phone only; edits and deletions behind their own switch, off) and
 * history (the phone's visits, and pages deleted on the phone as tombstones). Never bookmarks,
 * tabs, Spaces, prefs, keys or meta/global.
 *
 * Each collection, once: a backup of the whole collection as the server has it (still encrypted
 * with the account's keys, so it can be put back as is) in Koan's storage and a copy in its
 * external files folder. Then every run: meta/global checked ([WriteGuard], the collection's sync
 * id must not change), a dry run logged (counts and field names, never a URL, username or
 * password), a first batch of at most [FIRST_BATCH] records read back and compared, then the rest
 * in batches of [BATCH], each one conditioned on the collection's timestamp
 * (X-If-Unmodified-Since). A conflict or surprise stops that collection and turns its switch off
 * ([status] says why); a cancelled run or a plain network error leaves the switch on and retries
 * at the next run.
 */
class SyncWrite(
    private val context: Context,
    private val settings: Settings,
    private val dao: KoanDao,
    private val logins: LoginStore,
    private val hiddenLogins: HiddenLogins,
    private val deletions: HistoryDeletions,
) {
    private val prefs = context.getSharedPreferences("sync_write", Context.MODE_PRIVATE)
    private val _status = MutableStateFlow(readStatus())

    /** The last outcome per collection, for the Account page and the morning report (no content). */
    val status: StateFlow<Map<String, String>> get() = _status

    private class Stop(val reason: String) : Exception(reason)

    /** After a pull. [force]: "Sync now"; otherwise at most every [MIN_INTERVAL_MS]. */
    suspend fun run(client: SyncClient, force: Boolean) = withContext(Dispatchers.IO) {
        val on = settings.syncBack.first()
        if (!on.passwords && !on.history) return@withContext
        val last = prefs.getLong("last_run", 0)
        if (!force && System.currentTimeMillis() - last < MIN_INTERVAL_MS) return@withContext
        prefs.edit().putLong("last_run", System.currentTimeMillis()).apply()
        val meta = runCatching { client.metaGlobal() }.getOrNull()
        if (on.passwords) collection(client, "passwords", meta) { passwords(client, settings.syncBackPasswordEdits.first()) }
        if (on.history) collection(client, "history", meta) { history(client) }
    }

    private suspend fun collection(client: SyncClient, name: String, meta: JSONObject?, body: suspend () -> String) {
        try {
            val engine = meta?.optJSONObject("engines")?.optJSONObject(name)
            val known = prefs.getString("syncid_$name", null)
            WriteGuard.refusal(meta, name, known)?.let { throw Stop(it) }
            if (known == null) prefs.edit().putString("syncid_$name", engine?.optString("syncID")).apply()
            backup(client, name)
            setStatus(name, body())
        } catch (e: Stop) {
            stop(name, e.reason)
        } catch (e: CancellationException) {
            // Koan went to the background mid-run: not a problem with the account. Try again later.
            setStatus(name, "interrupted, will retry")
            throw e
        } catch (e: IOException) {
            // Offline, timeout, DNS…: the switch stays on, the next run tries again.
            log("$name: network error, will retry: ${e.javaClass.simpleName}")
            setStatus(name, "network error, will retry")
        } catch (e: Throwable) {
            if (e is SyncError.Network && isTransient(e)) {
                log("$name: server busy, will retry")
                setStatus(name, "server busy, will retry")
            } else {
                stop(name, e.javaClass.simpleName)
            }
        }
    }

    // ---- passwords: add only (edits and deletions behind their switch) ----

    private suspend fun passwords(client: SyncClient, edits: Boolean): String {
        var at = client.collectionLastModified("passwords") ?: throw Stop("no collection timestamp")
        val server = decrypted(client, "passwords")
        val serverIds = server.mapTo(HashSet()) { it.optString("id") }
        val serverPairs = server.mapTo(HashSet()) { LoginUpload.origin(it.optString("hostname")) to it.optString("username") }
        val phone = RoomPhoneData(dao, logins).logins()
        val plan = LoginUpload.plan(phone, serverIds, serverPairs)
        val adds = PasswordAdds.onlyNew(plan.records, serverIds, serverPairs)
        log("dry run: ${plan.summary()}; new only: ${adds.size}")
        var result = "nothing new to add"
        if (adds.isNotEmpty()) {
            at = send(client, "passwords", adds, at)
            result = "${adds.size} added"
        }
        if (edits) {
            val e = PasswordEdits.plan(phone, server, hiddenLogins.guids.value)
            log("dry run: ${e.summary()}")
            if (e.records.isNotEmpty()) {
                send(client, "passwords", e.records, at)
                result += "; ${e.summary()}"
            }
        }
        return result
    }

    // ---- history: the phone's visits up, the phone's deletions as tombstones ----

    private suspend fun history(client: SyncClient): String {
        var at = client.collectionLastModified("history") ?: throw Stop("no collection timestamp")
        val recent = client.getRecentRecords("history", HISTORY_LIMIT).mapNotNull { record ->
            runCatching { client.decryptRecord("history", record) }.getOrNull()?.takeIf { !it.optBoolean("deleted") }
        }
        val byUrl = recent.associateBy { it.optString("histUri") }.toMutableMap()
        val ids = deletions.ids() + byUrl.mapValues { it.value.optString("id") }
        // Only pages removed one by one while the switch was on; never the ones "Clear browsing
        // data" (or a removal with the switch off) only hid here (HistoryWritePlan).
        val deleted = deletions.pending()
        val phone = dao.phoneVisits(HISTORY_LIMIT).filter { it.url !in deleted }
        // A page the account has but older than the recent page: its record, so it isn't duplicated.
        phone.asSequence().filter { it.url !in byUrl && it.url in ids }.take(OLDER_LOOKUPS).forEach { v ->
            client.getRecord("history", ids.getValue(v.url))?.let { raw ->
                runCatching { client.decryptRecord("history", raw) }.getOrNull()?.takeIf { !it.optBoolean("deleted") }?.let { byUrl[v.url] = it }
            }
        }
        val write = HistoryWritePlan.plan(phone.map { dev.nerzouille.koan.sync.PhoneVisit(it.url, it.title, it.lastVisit) }, deleted, byUrl, ids)
        val plan = write.upload
        val tombstones = write.tombstones
        log("dry run: ${plan.summary()}; deletions: ${tombstones.size} of ${deleted.size} pending")
        if (plan.records.isEmpty() && tombstones.isEmpty()) {
            // Pending deletions the account doesn't have (never synced): nothing to do for them.
            deletions.remove(deleted.filter { it !in ids })
            return "up to date"
        }
        at = send(client, "history", plan.records + tombstones, at)
        deletions.updateIds(plan.records.associate { it.optString("histUri") to it.optString("id") }, gone = deleted)
        deletions.remove(deleted)
        dao.deleteVisits(deleted.toList())
        return "${plan.new} new, ${plan.changed} updated, ${tombstones.size} deleted"
    }

    /**
     * Sends [records]: a first batch of at most [FIRST_BATCH], read back and compared, then the
     * rest in batches of [BATCH], each conditioned on the timestamp the previous one left.
     * Returns the collection's timestamp after the last batch.
     */
    private fun send(client: SyncClient, collection: String, records: List<org.json.JSONObject>, since: String): String {
        var at = since
        val first = records.take(FIRST_BATCH)
        at = post(client, collection, first, at)
        first.forEach { sent ->
            val back = client.getRecord(collection, sent.optString("id"))?.let { client.decryptRecord(collection, it) }
            ReadBack.mismatch(sent, back)?.let { field -> throw Stop("read-back mismatch on field '$field'") }
        }
        log("$collection: first ${first.size} sent and read back")
        records.drop(FIRST_BATCH).chunked(BATCH).forEach { batch -> at = post(client, collection, batch, at) }
        log("$collection: ${records.size} sent")
        return at
    }

    private fun post(client: SyncClient, collection: String, batch: List<org.json.JSONObject>, since: String): String {
        val (outcome, modified) = client.postBatch(collection, batch, since)
        return when (outcome) {
            is PostOutcome.Applied -> modified ?: throw Stop("no timestamp after a batch")
            is PostOutcome.PreconditionFailed -> throw Stop("the account changed meanwhile (412)")
            is PostOutcome.PartialFailure -> throw Stop("server refused ${outcome.failed.size + outcome.missingIds.size} record(s)")
        }
    }

    // ---- backup, once per collection ----

    /**
     * The collection as the server has it (encrypted records, one JSON per line), before Koan's
     * first write to it: in Koan's private storage, and a copy in its external files folder
     * (Android/data/<app>/files/sync-backup, reachable over USB). Both stay encrypted with the
     * account's keys: putting a record back as is restores it.
     */
    private fun backup(client: SyncClient, collection: String) {
        if (prefs.getString("backup_$collection", null) != null) return
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
        val dir = File(context.filesDir, "sync-backup").apply { mkdirs() }
        val file = File(dir, "$collection-$stamp.jsonl")
        var count = 0
        file.bufferedWriter().use { out ->
            client.streamRecords(collection) { page ->
                page.forEach { record ->
                    out.write(record.toString())
                    out.newLine()
                    count++
                }
            }
        }
        if (count == 0 && collection == "passwords") throw Stop("backup read no records")
        context.getExternalFilesDir("sync-backup")?.let { ext -> runCatching { file.copyTo(File(ext, file.name), overwrite = true) } }
        prefs.edit().putString("backup_$collection", file.name).apply()
        log("$collection: backup of $count encrypted records written")
    }

    private fun decrypted(client: SyncClient, collection: String): List<JSONObject> =
        client.getRecords(collection).mapNotNull { record ->
            runCatching { client.decryptRecord(collection, record) }.getOrNull()?.takeIf { !it.optBoolean("deleted") }
        }

    private suspend fun stop(collection: String, reason: String) {
        log("$collection: stopped, switch turned off: $reason")
        setStatus(collection, "stopped: $reason")
        val c = settings.syncBack.first()
        settings.setSyncBack(if (collection == "passwords") c.copy(passwords = false) else c.copy(history = false))
    }

    private fun setStatus(collection: String, text: String) {
        val stamped = "$text (${SimpleDateFormat("d MMM HH:mm", Locale.ENGLISH).format(Date())})"
        prefs.edit().putString("status_$collection", stamped).apply()
        _status.value = readStatus()
    }

    private fun readStatus(): Map<String, String> =
        listOf("passwords", "history").mapNotNull { c -> prefs.getString("status_$c", null)?.let { c to it } }.toMap()

    private fun log(text: String) = Log.i(TAG, text)

    companion object {
        /** A server-side blip (5xx, 429): try again at the next run; any other HTTP error stops. */
        fun isTransient(e: SyncError.Network): Boolean {
            val m = e.message.orEmpty()
            return m.startsWith("HTTP 5") || m.startsWith("HTTP 429")
        }

        private const val TAG = "KoanSyncWrite"
        const val FIRST_BATCH = 3
        const val BATCH = 50
        private const val HISTORY_LIMIT = 5000
        private const val OLDER_LOOKUPS = 200
        private const val MIN_INTERVAL_MS = 30 * 60_000L
    }
}
