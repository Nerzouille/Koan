package dev.nerzouille.koan.ui.history

import androidx.compose.runtime.Immutable
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.search.displayHost

/** One row of a day's History card: a page, or several pages of one site in a row. */
@Immutable
sealed interface HistoryItem {
    val key: String

    /** Everything removing the row removes (merged duplicates included). */
    val all: List<VisitEntity>

    /** [merged]: the same page visited right before, shown once (as [visit]). */
    data class Page(val visit: VisitEntity, val merged: List<VisitEntity> = emptyList()) : HistoryItem {
        override val key get() = visit.url
        override val all get() = listOf(visit) + merged
    }

    /** Consecutive visits to [host], newest first. */
    data class Group(val host: String, val pages: List<Page>) : HistoryItem {
        override val key get() = "group:${pages.last().visit.url}"
        override val all get() = pages.flatMap { it.all }
        val newest get() = pages.first().visit.lastVisit
        val oldest get() = pages.last().visit.lastVisit
    }
}

/**
 * A day's visits (newest first) as rows: back-to-back visits to the same site become one group
 * (Opera-style), a lone visit stays a page. The same page twice in a row (the url as typed, then
 * as loaded: "https://example.com", "https://example.com/") shows once, the newer one.
 */
fun groupVisits(visits: List<VisitEntity>): List<HistoryItem> {
    val runs = ArrayList<MutableList<Page>>()
    var host: String? = null
    for (v in visits) {
        val h = displayHost(v.url)
        val run = runs.lastOrNull()
        when {
            run != null && h.isNotEmpty() && h == host -> {
                val last = run.last()
                if (samePage(last.visit.url, v.url)) run[run.lastIndex] = last.copy(merged = last.merged + v) else run += Page(v)
            }
            run != null && samePage(run.last().visit.url, v.url) -> run[run.lastIndex] = run.last().let { it.copy(merged = it.merged + v) }
            else -> runs += mutableListOf(Page(v))
        }
        host = h
    }
    return runs.map { run -> if (run.size == 1) run[0] else HistoryItem.Group(host = displayHost(run[0].visit.url), pages = run) }
}

private typealias Page = HistoryItem.Page

private fun samePage(a: String, b: String) = a.trimEnd('/') == b.trimEnd('/')
