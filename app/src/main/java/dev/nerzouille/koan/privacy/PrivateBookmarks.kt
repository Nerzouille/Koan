package dev.nerzouille.koan.privacy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** A bookmark made in a private tab. */
data class PrivateBookmark(val url: String, val title: String, val createdAt: Long)

/**
 * Bookmarks made in private tabs (docs/plans/FIXES_PLAN.md F10): kept apart from the normal ones,
 * only shown in Private, sealed on disk with the private tabs' Keystore key ([file]), never in
 * Room and never synced. They stay until unstarred or cleared (Clear browsing data, in Private),
 * through restarts and after the last private tab closes. Loaded once, off the main thread; every
 * change is written at once.
 */
class PrivateBookmarks(
    private val file: SealedFile,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _list = MutableStateFlow<List<PrivateBookmark>>(emptyList())

    /** Newest first. */
    val list: StateFlow<List<PrivateBookmark>> = _list.asStateFlow()

    private val lock = Mutex()
    private val loaded = scope.async(io) { file.read()?.let { runCatching { decode(it) }.getOrNull() }.orEmpty().also { _list.value = it } }

    /** How many there are, once loaded (the private lock asks when there are any). */
    suspend fun count(): Int {
        loaded.await()
        return _list.value.size
    }

    /** Bookmarks [url] in Private if it isn't yet, otherwise removes it. */
    fun toggle(url: String, title: String) = change { list ->
        if (list.any { it.url == url }) list.filterNot { it.url == url }
        else listOf(PrivateBookmark(url, title.ifBlank { url }, now())) + list
    }

    /** Removes every private bookmark and its file. */
    fun clear() = scope.launch(io) {
        loaded.await()
        lock.withLock {
            _list.value = emptyList()
            file.delete()
        }
    }

    private fun change(edit: (List<PrivateBookmark>) -> List<PrivateBookmark>) = scope.launch(io) {
        loaded.await()
        lock.withLock {
            val next = edit(_list.value)
            _list.value = next
            if (next.isEmpty()) file.delete() else file.write(encode(next))
        }
    }

    companion object {
        fun encode(list: List<PrivateBookmark>): String = JSONObject().apply {
            put("v", 1)
            put("bookmarks", JSONArray(list.map { b -> JSONObject().put("url", b.url).put("title", b.title).put("createdAt", b.createdAt) }))
        }.toString()

        fun decode(json: String): List<PrivateBookmark> {
            val arr = JSONObject(json).getJSONArray("bookmarks")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PrivateBookmark(o.getString("url"), o.optString("title"), o.optLong("createdAt"))
            }
        }

        /** The search sheet's rule (as the database's LIKE): [q] in the url or the title; empty = the newest. */
        fun search(list: List<PrivateBookmark>, q: String, limit: Int): List<PrivateBookmark> =
            list.asSequence()
                .filter { q.isEmpty() || it.url.contains(q, ignoreCase = true) || it.title.contains(q, ignoreCase = true) }
                .take(limit)
                .toList()
    }
}
