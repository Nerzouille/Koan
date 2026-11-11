package dev.nerzouille.koan.ui.overview

import dev.nerzouille.koan.search.displayHost

/**
 * A card header's title line (OVERVIEW_PRIVATE.md §3.2): the page title without a trailing
 * " - <site name>" when the host line under it already says the same ("Koan - Wikipedia" on
 * en.wikipedia.org → "Koan"). Only a dash-separated suffix that matches one of the host's labels
 * is dropped, and never the whole title.
 */
fun cardTitle(title: String, url: String): String {
    val cut = SEPARATORS.map { title.lastIndexOf(it) }.maxOrNull() ?: -1
    if (cut <= 0) return title
    val sep = SEPARATORS.first { title.startsWith(it, cut) }
    val head = title.substring(0, cut).trimEnd()
    val site = title.substring(cut + sep.length).trim()
    if (head.isEmpty() || site.isEmpty()) return title
    val key = site.lowercase().filter { it.isLetterOrDigit() }
    if (key.isEmpty()) return title
    val labels = displayHost(url).lowercase().split('.').map { l -> l.filter { it.isLetterOrDigit() } }
    return if (labels.any { it == key }) head else title
}

private val SEPARATORS = listOf(" - ", " – ", " — ")
