package dev.nerzouille.koan.engine

import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TranslationsState
import mozilla.components.browser.state.state.createTab
import mozilla.components.concept.engine.translate.DetectedLanguages
import mozilla.components.concept.engine.translate.TranslationEngineState
import mozilla.components.concept.engine.translate.TranslationPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationResetTest {
    private fun state(translations: TranslationsState, loading: Boolean = false): BrowserState {
        val tab = createTab("https://example.com/a", id = "t").let {
            it.copy(content = it.content.copy(loading = loading), translationsState = translations)
        }
        return BrowserState(tabs = listOf(tab))
    }

    private val translated = TranslationsState(
        isTranslated = true,
        translationEngineState = TranslationEngineState(
            detectedLanguages = DetectedLanguages(documentLangTag = "en", supportedDocumentLang = true, userPreferredLangTag = "fr"),
            requestedTranslationPair = TranslationPair("en", "fr"),
        ),
    )

    @Test
    fun aNewPageAfterATranslatedOneIsReset() {
        assertEquals("t", translationResetFor(state(translated), ContentAction.UpdateLoadingStateAction("t", true)))
    }

    @Test
    fun aDetectedLanguageAloneIsReset() {
        val detectedOnly = TranslationsState(translationEngineState = translated.translationEngineState!!.copy(requestedTranslationPair = null))
        assertEquals("t", translationResetFor(state(detectedOnly), ContentAction.UpdateLoadingStateAction("t", true)))
    }

    @Test
    fun aLoadAlreadyRunningOrEndingIsNot() {
        assertNull(translationResetFor(state(translated, loading = true), ContentAction.UpdateLoadingStateAction("t", true)))
        assertNull(translationResetFor(state(translated), ContentAction.UpdateLoadingStateAction("t", false)))
    }

    @Test
    fun nothingToClearOrAnotherActionIsNot() {
        assertNull(translationResetFor(state(TranslationsState()), ContentAction.UpdateLoadingStateAction("t", true)))
        assertNull(translationResetFor(state(translated), ContentAction.UpdateUrlAction("t", "https://example.com/a#b")))
        assertNull(translationResetFor(state(translated), ContentAction.UpdateLoadingStateAction("other", true)))
        assertNull(translationResetFor(state(translated), TabListAction.RemoveAllTabsAction()))
    }
}
