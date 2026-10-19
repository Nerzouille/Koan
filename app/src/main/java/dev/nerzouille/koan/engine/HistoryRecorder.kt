package dev.nerzouille.koan.engine

import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.search.HistoryFilter
import dev.nerzouille.koan.search.displayHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.history.HistoryTrackingDelegate
import mozilla.components.concept.storage.PageVisit

/**
 * History: one visit per page a tab settles on ([pageShown], from TabManager; GeckoView doesn't
 * report visits to Koan, and a tab's address never shows an HTTP redirect's source). Not history:
 * redirect pages ("Redirection", sign-in hops by url), and a page that sent its tab to another
 * site within moments of loading (a sign-in hop done by script). A visit is held [HOLD_MS] before
 * it is written, so the last one can still drop it. As the engine's history delegate it also keeps
 * titles current and makes visited links look visited.
 */
class HistoryRecorder(private val dao: KoanDao, private val scope: CoroutineScope) : HistoryTrackingDelegate {
    private class Held(val url: String, val at: Long, @Volatile var title: String) {
        var job: Job? = null
    }

    /** Per tab: the visit waiting to be written, or the last one written. */
    private val held = HashMap<String, Held>()

    /** Tab [tabId] shows [url] (titled [title]) now. Call on the main thread. */
    fun pageShown(tabId: String, url: String, title: String) {
        if (!shouldStoreUri(url)) return
        val previous = held[tabId]
        if (previous?.url == url) {
            // Same page, a new title.
            if (title.isNotEmpty() && title != previous.title) {
                previous.title = title
                if (previous.job?.isCompleted == true) scope.launch { onTitleChanged(url, title) }
            }
            return
        }
        val now = System.currentTimeMillis()
        // Left for another site within moments, untouched: that page was a hop, not a place.
        if (previous != null && now - previous.at < HOLD_MS && displayHost(previous.url) != displayHost(url)) previous.job?.cancel()
        val next = Held(url, now, title)
        held[tabId] = next
        next.job = scope.launch {
            delay(HOLD_MS)
            if (HistoryFilter.isRedirect(next.url, next.title)) return@launch
            val title = next.title
            dao.recordVisit(next.url, title, next.at)
            // The title arrived while the visit was being written.
            val now = next.title
            if (now != title) onTitleChanged(next.url, now)
        }
    }

    /** Tab [tabId] closed. */
    fun forget(tabId: String) {
        held.remove(tabId)
    }

    override suspend fun onVisited(uri: String, visit: PageVisit) = Unit

    override suspend fun onTitleChanged(uri: String, title: String) {
        if (HistoryFilter.isRedirect(uri, title)) dao.deleteLocalVisit(uri) else if (title.isNotEmpty()) dao.setVisitTitle(uri, title)
    }

    override suspend fun onPreviewImageChange(uri: String, previewImageUrl: String) = Unit

    /** Visited links look visited. */
    override suspend fun getVisited(uris: List<String>): List<Boolean> {
        val known = dao.visitedAmong(uris).toHashSet()
        return uris.map { it in known }
    }

    override suspend fun getVisited(): List<String> = emptyList()

    override fun shouldStoreUri(uri: String): Boolean = uri.startsWith("https://") || uri.startsWith("http://")

    private companion object {
        const val HOLD_MS = 1500L
    }
}
