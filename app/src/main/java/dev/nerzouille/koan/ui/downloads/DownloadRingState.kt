package dev.nerzouille.koan.ui.downloads

import mozilla.components.browser.state.state.content.DownloadState

/**
 * The address pill's download ring, from the store: [progress] of what runs (null: nothing runs,
 * NaN: size unknown), [running] their ids, and [doneTick], raised each time a download that ran
 * ends completed. The finish is read from the same store update that ends the progress: the
 * "download stopped" broadcast comes after it, so a ring waiting for it had already gone, and came
 * back drawn at its faded-out end.
 */
data class DownloadRingState(val progress: Float? = null, val running: Set<String> = emptySet(), val doneTick: Int = 0)

/** The ring after the store's [downloads] changed, from [before]; [privateOn] filters private ones. */
fun nextRingState(before: DownloadRingState, downloads: Collection<DownloadState>, privateOn: Boolean): DownloadRingState {
    val visible = downloads.filter { visibleIn(it, privateOn) }
    val running = visible.filter { it.status == DownloadState.Status.DOWNLOADING }
    val progress = if (running.isEmpty()) {
        null
    } else {
        val total = running.sumOf { it.contentLength ?: 0L }
        val unknown = running.any { it.contentLength == null } || total <= 0L
        if (unknown) Float.NaN else running.sumOf { it.currentBytesCopied }.toFloat() / total
    }
    val ids = running.mapTo(HashSet()) { it.id }
    val completed = visible.filter { it.status == DownloadState.Status.COMPLETED }.mapTo(HashSet()) { it.id }
    val finished = (before.running - ids).any { it in completed }
    return DownloadRingState(progress, ids, if (finished) before.doneTick + 1 else before.doneTick)
}
