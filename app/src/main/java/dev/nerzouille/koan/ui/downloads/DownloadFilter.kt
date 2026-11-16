package dev.nerzouille.koan.ui.downloads

import mozilla.components.browser.state.state.content.DownloadState

/*
 * Which downloads a mode may see (docs/plans/FIXES_PLAN.md F7): normal mode never sees a private
 * download (not in the list, the pill's ring, the menu badge or a "downloaded" toast); Private
 * sees everything, private rows masked, and its Private pill narrows the list to private ones.
 */

/** True when [d] may show while Private is [privateOn]. */
fun visibleIn(d: DownloadState, privateOn: Boolean): Boolean = privateOn || !d.private

/** The downloads a mode shows; [privateOnly] is Private's pill (ignored outside Private). */
fun visibleDownloads(all: List<DownloadState>, privateOn: Boolean, privateOnly: Boolean = false): List<DownloadState> =
    when {
        !privateOn -> all.filterNot { it.private }
        privateOnly -> all.filter { it.private }
        else -> all
    }
