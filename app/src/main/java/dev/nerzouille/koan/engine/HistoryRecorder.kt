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
import mozilla.components.concept.storage.VisitType

/**
 * History as the engine reports it (GeckoView's HistoryDelegate, through android-components): one
 * visit per top-level page. Not history: redirect sources (the engine's visit flags), reloads,
 * redirect pages ("Redirection"), and a page that sent the tab to another site within moments of
 * loading (a sign-in hop done by script). A visit is held [HOLD_MS] before it is written, so the
 * last two can still drop it.
 */
class HistoryRecorder(private val dao: KoanDao, private val scope: CoroutineScope) : HistoryTrackingDelegate {
    private class Held(val url: String, val at: Long, var title: String) {
        var job: Job? = null
    }

    private var held: Held? = null

    override suspend fun onVisited(uri: String, visit: PageVisit) {
        if (!shouldStoreUri(uri)) return
        if (visit.redirectSource != null || visit.visitType == VisitType.RELOAD) return
        val now = System.currentTimeMillis()
        val next = Held(uri, now, "")
        synchronized(this) {
            val previous = held
            if (previous != null && now - previous.at < HOLD_MS && displayHost(previous.url) != displayHost(uri)) previous.job?.cancel()
            held = next
        }
        next.job = scope.launch {
            delay(HOLD_MS)
            if (HistoryFilter.isRedirect(next.url, next.title)) return@launch
            dao.recordVisit(next.url, next.title, next.at)
        }
    }

    override suspend fun onTitleChanged(uri: String, title: String) {
        synchronized(this) {
            val pending = held?.takeIf { it.url == uri && it.job?.isCompleted != true }
            if (pending != null) {
                pending.title = title
                return
            }
        }
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
