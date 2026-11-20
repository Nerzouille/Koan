package dev.nerzouille.koan.engine.downloads

import mozilla.components.browser.state.state.content.DownloadState

/**
 * The finished downloads whose file is no longer there ([exists] false: moved or deleted outside
 * Koan). Only completed ones: a running download has no file yet. Checked once, when Downloads
 * opens (each check reads the disk).
 */
fun missingDownloads(downloads: Collection<DownloadState>, exists: (DownloadState) -> Boolean): Set<String> =
    downloads.filter { it.status == DownloadState.Status.COMPLETED && !exists(it) }.mapTo(HashSet()) { it.id }

/**
 * A missing file downloaded again: from the original address, under its name, in the same mode
 * (a private download stays private), without the confirmation card (it was asked for here).
 * A new download (new id); the folder is today's, as for any new download.
 */
fun downloadAgainOf(d: DownloadState): DownloadState = DownloadState(
    url = d.url,
    fileName = d.fileName,
    contentType = d.contentType,
    referrerUrl = d.referrerUrl,
    userAgent = d.userAgent,
    private = d.private,
    skipConfirmation = true,
)
