package dev.nerzouille.koan.engine.downloads

import dev.nerzouille.koan.engine.DuplicateAsk
import kotlinx.coroutines.flow.MutableStateFlow
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.utils.DownloadFileUtils

/**
 * Every download a page starts passes here first, even the ones that skip the confirmation (the
 * PDF viewer's Download, Save image): one of a file already there waits on the duplicate card
 * ([ask]) instead of going on. The card's answer comes back as the same download (maybe renamed),
 * marked to pass this time.
 */
class DuplicateMiddleware(
    private val files: () -> DownloadFileUtils,
    private val ask: MutableStateFlow<DuplicateAsk?>,
) : Middleware<BrowserState, BrowserAction> {
    private val approved = mutableSetOf<String>()

    override fun invoke(store: Store<BrowserState, BrowserAction>, next: (BrowserAction) -> Unit, action: BrowserAction) {
        if (action is ContentAction.UpdateDownloadAction && action.download.id !in approved && !action.download.private) {
            val download = action.download.let { d ->
                if (!d.fileName.isNullOrBlank()) d else d.copy(fileName = runCatching { files().guessFileName(null, d.url, d.contentType) }.getOrNull())
            }
            val dup = DuplicateDownloads.find(download, store.state.downloads.values, files())
            if (dup != null) {
                ask.value = DuplicateAsk(
                    dup,
                    start = { chosen ->
                        approved += chosen.id
                        // Answered on the card: no second question.
                        store.dispatch(ContentAction.UpdateDownloadAction(action.sessionId, chosen.copy(skipConfirmation = true)))
                    },
                    cancel = {},
                )
                return
            }
        }
        next(action)
    }
}
