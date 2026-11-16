package dev.nerzouille.koan.privacy

import dev.nerzouille.koan.data.SecretBox
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** One private tab as saved: its page and its engine state (page history, scroll; GeckoView's JSON). */
data class SavedPrivateTab(
    val id: String,
    val url: String,
    val title: String,
    val iconUrl: String?,
    val order: Int,
    val lastAccessed: Long,
    val engineState: String?,
)

/** Every private tab, and the one shown last. */
data class SavedPrivateTabs(val tabs: List<SavedPrivateTab>, val current: String?)

/**
 * The private tabs on disk, so they come back after a restart (OVERVIEW_PRIVATE.md, owner's
 * decision 2026-11-11): one file, sealed with a Keystore key ([box]), never in plain text. Cookies
 * and site data are not in it: GeckoView keeps those in memory only, so sites sign you out when
 * Koan restarts. [delete] when the last private tab closes.
 */
class PrivateVault(file: File, box: SecretBox) {
    private val sealed = SealedFile(file, box)

    fun read(): SavedPrivateTabs? = sealed.read()?.let { runCatching { decode(it) }.getOrNull() }

    fun write(saved: SavedPrivateTabs) = sealed.write(encode(saved))

    fun delete() = sealed.delete()

    companion object {
        fun encode(saved: SavedPrivateTabs): String = JSONObject().apply {
            put("v", 1)
            saved.current?.let { put("current", it) }
            put("tabs", JSONArray(saved.tabs.map { t ->
                JSONObject().apply {
                    put("id", t.id)
                    put("url", t.url)
                    put("title", t.title)
                    t.iconUrl?.let { put("icon", it) }
                    put("order", t.order)
                    put("lastAccessed", t.lastAccessed)
                    t.engineState?.let { put("state", it) }
                }
            }))
        }.toString()

        fun decode(json: String): SavedPrivateTabs {
            val o = JSONObject(json)
            val list = o.getJSONArray("tabs")
            val tabs = (0 until list.length()).map { i ->
                val t = list.getJSONObject(i)
                SavedPrivateTab(
                    id = t.getString("id"),
                    url = t.getString("url"),
                    title = t.optString("title"),
                    iconUrl = t.optString("icon").ifEmpty { null },
                    order = t.optInt("order"),
                    lastAccessed = t.optLong("lastAccessed"),
                    engineState = t.optString("state").ifEmpty { null },
                )
            }
            return SavedPrivateTabs(tabs, o.optString("current").ifEmpty { null })
        }
    }
}
