package dev.nerzouille.koan.search

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

/**
 * The owner's own engines (Settings › Search engine › Add a search engine): how they are saved,
 * and what a new or edited one must be.
 */
object CustomEngines {
    private const val MAX_SHORTCUT = 16

    fun newId(): String = "custom:" + UUID.randomUUID()

    fun toJson(engines: List<SearchEngine>): String = JSONArray().apply {
        engines.forEach { e ->
            put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.label)
                    .put("url", e.searchTemplate)
                    .put("shortcut", e.shortcut ?: "")
                    .put("suggest", e.suggestTemplate ?: ""),
            )
        }
    }.toString()

    /** What [toJson] saved; a broken entry is skipped, a broken list gives none. */
    fun fromJson(raw: String?): List<SearchEngine> {
        if (raw.isNullOrEmpty()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").ifEmpty { return@mapNotNull null }
            val url = o.optString("url").ifEmpty { return@mapNotNull null }
            SearchEngine(
                id = id,
                label = o.optString("name").ifEmpty { origin(url) },
                searchTemplate = url,
                suggestTemplate = o.optString("suggest").ifEmpty { null },
                shortcut = o.optString("shortcut").ifEmpty { null },
                custom = true,
            )
        }
    }

    /** "@Kagi " → "kagi". */
    fun cleanShortcut(typed: String): String = typed.trim().removePrefix("@").trim().lowercase()

    /** "@" words Koan uses itself: the built-in shortcuts and the scopes. */
    private val reserved: Set<String> = (Shortcut.builtIns.map { it.key } + Scope.entries.map { it.key }).toSet()

    /**
     * What is wrong with an engine as typed (each field's message), checked against the [others]
     * already in the list (built-ins and custom ones, without the one being edited).
     */
    fun check(name: String, url: String, shortcut: String, suggest: String, others: List<SearchEngine>): EngineCheck {
        val key = cleanShortcut(shortcut)
        return EngineCheck(
            name = when {
                name.isBlank() -> "Give it a name"
                others.any { it.label.equals(name.trim(), ignoreCase = true) } -> "Another engine has this name"
                else -> null
            },
            url = templateError(url, required = true),
            shortcut = when {
                key.isEmpty() -> null
                key.length > MAX_SHORTCUT || !key.all { it.isLetterOrDigit() || it == '-' } -> "Letters and digits only, no spaces"
                key in reserved -> "@$key is used by Koan"
                others.any { it.shortcut == key } -> "@$key is used by another engine"
                else -> null
            },
            suggest = templateError(suggest, required = false),
        )
    }

    /** A search or suggestions url: https, with "%s" where the query goes, and a host. */
    private fun templateError(typed: String, required: Boolean): String? {
        val url = typed.trim()
        return when {
            url.isEmpty() -> if (required) "Paste the address of a search, with %s for the words" else null
            !url.startsWith("https://", ignoreCase = true) -> "Must start with https://"
            "%s" !in url -> "Put %s where the search words go"
            runCatching { URI(url.replace("%s", "x")).host }.getOrNull().isNullOrEmpty() -> "Not a valid address"
            else -> null
        }
    }
}

/** Each field's problem, null = fine. */
data class EngineCheck(
    val name: String? = null,
    val url: String? = null,
    val shortcut: String? = null,
    val suggest: String? = null,
) {
    val ok: Boolean get() = name == null && url == null && shortcut == null && suggest == null
}
