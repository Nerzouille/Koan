package dev.nerzouille.koan.engine

import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.concept.engine.translate.TranslationEngineState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store

/**
 * A tab's translation state belongs to one page. Gecko sends a new state only for a page it can
 * detect, so after a translated page, a page Gecko says nothing about kept the old one ("From
 * English" on a Chinese page, no translate icon). Each new page load clears it: no detected
 * language, no translated pair, nothing running. A same-page link or a pushState doesn't start a
 * load, so it keeps the translation; Gecko's own state for the new page comes after the load
 * starts and replaces the empty one.
 */
class TranslationResetMiddleware : Middleware<BrowserState, BrowserAction> {
    override fun invoke(store: Store<BrowserState, BrowserAction>, next: (BrowserAction) -> Unit, action: BrowserAction) {
        val tabId = translationResetFor(store.state, action)
        next(action)
        if (tabId != null) {
            store.dispatch(TranslationsAction.TranslateStateChangeAction(tabId, TranslationEngineState()))
            store.dispatch(TranslationsAction.SetTranslateProcessingAction(tabId, false))
        }
    }
}

/**
 * The tab whose translation state [action] makes stale: a page load starting in a tab that wasn't
 * loading, and that has something to clear. Null otherwise. Read before the action is applied.
 */
internal fun translationResetFor(state: BrowserState, action: BrowserAction): String? {
    if (action !is ContentAction.UpdateLoadingStateAction || !action.loading) return null
    val tab = state.findTab(action.sessionId) ?: return null
    if (tab.content.loading) return null
    val t = tab.translationsState
    val engine = t.translationEngineState
    val stale = t.isTranslated || t.isTranslateProcessing ||
        engine?.detectedLanguages != null || engine?.requestedTranslationPair?.fromLanguage != null
    return if (stale) tab.id else null
}
