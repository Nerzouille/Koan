package dev.nerzouille.koan.engine

import android.content.Context
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.readerview.view.ReaderViewControlsView

/**
 * What the ⋯ menu does to the page itself: reader view, find in page, and the long-press menu's
 * hit result. Koan draws its own UI for all of them; the engine does the work.
 */
class PageTools(context: Context, engine: Engine, private val store: BrowserStore) {

    /** Reader view's own controls (font, colours) are not shown: Koan follows its theme instead. */
    private val readerControls = object : ReaderViewControlsView {
        override var listener: ReaderViewControlsView.Listener? = null
        override fun setFont(font: ReaderViewFeature.FontType) = Unit
        override fun setFontSize(size: Int) = Unit
        override fun setColorScheme(scheme: ReaderViewFeature.ColorScheme) = Unit
        override fun showControls() = Unit
        override fun hideControls() = Unit
        override fun tryInflate(): Boolean = false
    }

    private val reader = ReaderViewFeature(context, engine, store, readerControls)

    /** Installs reader view's built-in extension and starts checking pages. Once, at launch. */
    fun start() = reader.start()

    fun toggleReader(tabId: String) {
        val tab = store.state.findTab(tabId) ?: return
        if (tab.readerState.active) reader.hideReaderView(tab) else reader.showReaderView(tab)
    }

    /** Reader pages in Koan's theme. */
    fun setReaderDark(dark: Boolean) {
        readerControls.listener?.onColorSchemeChanged(if (dark) ReaderViewFeature.ColorScheme.DARK else ReaderViewFeature.ColorScheme.LIGHT)
    }

    // ---- find in page ------------------------------------------------------------------------

    /** Gecko's latest answer to find in page (the find bar's counter: [findCount]). */
    val findHit = kotlinx.coroutines.flow.MutableStateFlow<FindHit?>(null)

    /** Highlights every match of [text] in the tab (empty = clear). The answer lands in [findHit]. */
    fun find(tabId: String, text: String) {
        val session = store.state.findTab(tabId)?.engineState?.engineSession ?: return
        findHit.value = null
        if (text.isEmpty()) {
            session.clearFindMatches()
            store.dispatch(ContentAction.ClearFindResultsAction(tabId))
        } else if (!GeckoFinder.find(session, text, false, answer(tabId, text))) {
            session.findAll(text)
        }
    }

    /** The next or previous match of [text], the current search. */
    fun findNext(tabId: String, text: String, forward: Boolean) {
        val session = store.state.findTab(tabId)?.engineState?.engineSession ?: return
        if (!GeckoFinder.find(session, null, !forward, answer(tabId, text))) session.findNext(forward)
    }

    // Gecko answers with the search it ran: an answer for an older query is kept but not shown.
    private fun answer(tabId: String, text: String) = GeckoFinder.Result { searched, found, current, total ->
        findHit.value = FindHit(tabId, searched ?: text, found, current, total)
    }

    fun stopFind(tabId: String) = find(tabId, "")

    // ---- long press --------------------------------------------------------------------------

    /** The long-press result was shown: the engine reports the next one afresh. */
    fun consumeHitResult(tabId: String) {
        store.dispatch(ContentAction.ConsumeHitResultAction(tabId))
    }
}
