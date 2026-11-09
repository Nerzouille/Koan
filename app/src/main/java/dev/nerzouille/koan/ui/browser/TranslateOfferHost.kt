package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.splineBasedDecay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.sheet.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.menu.TranslateOffer
import dev.nerzouille.koan.ui.menu.languageName
import dev.nerzouille.koan.ui.theme.TranslateTokens
import kotlinx.coroutines.delay
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.translate.TranslationOptions
import mozilla.components.concept.engine.translate.TranslationPageSettingOperation
import java.util.Locale

/**
 * A page in another language than the phone's: Gecko offers a translation, and Koan shows it as a
 * small floating card (once per page; it leaves after a while, with ✕ or a tap outside). Translating says
 * so in the toast, and the toast that says it's done has "Show original".
 */
@Composable
fun TranslateOfferHost(components: Components, tabId: String?, allowed: Boolean, onToast: (Toast) -> Unit, modifier: Modifier = Modifier) {
    val store = components.store
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val decay = remember(density) { splineBasedDecay<Float>(density) }
    val t by store.observeSlice(tabId) { s -> tabId?.let { s.findTab(it) }?.let { it.translationsState to it.content.url } }
    val state = t?.first
    val url = t?.second.orEmpty()
    val detected = state?.translationEngineState?.detectedLanguages
    val from = detected?.documentLangTag?.takeIf { detected.supportedDocumentLang == true }
    val to = remember { Locale.getDefault().language }
    var dismissed by remember { mutableStateOf(emptySet<String>()) }
    val key = "$tabId|$url"
    val offer = allowed && tabId != null && from != null && state.isOfferTranslate && !state.isTranslated &&
        !state.isTranslateProcessing && key !in dismissed && !from.equals(to, ignoreCase = true)

    // Done: the toast says so, with the way back.
    var asked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state?.isTranslated, asked) {
        if (state?.isTranslated == true && asked == tabId && tabId != null) {
            asked = null
            onToast(Toast("Page translated", "Show original") { store.dispatch(TranslationsAction.TranslateRestoreAction(tabId)) })
        }
    }

    val card = remember { SheetState(scope, decay) }
    LaunchedEffect(offer, key) {
        if (offer) {
            card.animateTo(Detent.Compact)
            delay(TranslateTokens.OFFER_MS)
            card.animateTo(Detent.Closed)
        } else {
            card.animateTo(Detent.Closed)
        }
    }
    // Closed by a tap outside, Back or a drag down: not offered again on this page.
    LaunchedEffect(card.open) {
        if (!card.open && offer) dismissed = dismissed + key
    }
    val lang = from ?: return
    KoanCard(card, modifier) { _ ->
        val id = tabId ?: return@KoanCard
        TranslateOffer(
            pageLanguage = lang,
            target = to,
            onTranslate = {
                dismissed = dismissed + key
                asked = id
                store.dispatch(TranslationsAction.TranslateAction(id, lang, to, TranslationOptions(downloadModel = true)))
                onToast(Toast("Translating to ${languageName(to)}…", detail = "On this phone; the first time downloads the language"))
            },
            onDismiss = { dismissed = dismissed + key },
            onNeverLanguage = {
                dismissed = dismissed + key
                store.dispatch(TranslationsAction.UpdatePageSettingAction(id, TranslationPageSettingOperation.UPDATE_NEVER_TRANSLATE_LANGUAGE, true))
                onToast(Toast("${languageName(lang)} pages won't be offered again"))
            },
            onNeverSite = {
                dismissed = dismissed + key
                store.dispatch(TranslationsAction.UpdatePageSettingAction(id, TranslationPageSettingOperation.UPDATE_NEVER_TRANSLATE_SITE, true))
                onToast(Toast("This site won't be offered again"))
            },
        )
    }
}
