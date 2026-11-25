package dev.nerzouille.koan.ui.downloads

import mozilla.components.browser.state.state.content.DownloadState

/** A finished row's second line: its size (or "Moved or deleted") and where it came from. */
internal fun doneRowDetail(d: DownloadState, missing: Boolean): String =
    listOfNotNull(
        if (missing) "Moved or deleted" else (d.contentLength ?: d.currentBytesCopied).let(::sizeText),
        host(d),
    ).joinToString(" · ")
