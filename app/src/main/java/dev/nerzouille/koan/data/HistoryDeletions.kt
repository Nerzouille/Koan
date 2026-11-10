package dev.nerzouille.koan.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * History removed on the phone, waiting to be deleted from the Mozilla account too (two-way sync,
 * history switch on), and the account's history ids by page (from each pull), which a deletion
 * needs. Both stay on the phone, in its private storage.
 */
class HistoryDeletions(context: Context) {
    private val prefs = context.getSharedPreferences("history_deletions", Context.MODE_PRIVATE)
    private val idsFile = File(context.filesDir, "history_ids.json")

    fun pending(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun add(urls: Collection<String>) = save(pending() + urls)

    /** Undo on the phone, or the account has deleted them. */
    fun remove(urls: Collection<String>) = save(pending() - urls.toSet())

    private fun save(urls: Set<String>) {
        prefs.edit().putStringSet(KEY, urls).apply()
    }

    @Synchronized
    fun ids(): Map<String, String> = runCatching {
        val o = JSONObject(idsFile.readText())
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    /** The ids seen in a pull ([seen]: page → id) added to the known ones; [gone]: pages the account deleted. */
    @Synchronized
    fun updateIds(seen: Map<String, String>, gone: Collection<String> = emptyList()) {
        val all = ids().toMutableMap()
        all.putAll(seen)
        gone.forEach { all.remove(it) }
        runCatching { idsFile.writeText(JSONObject(all as Map<*, *>).toString()) }
    }

    private companion object {
        const val KEY = "urls"
    }
}
