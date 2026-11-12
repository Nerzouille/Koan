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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * A page in a language that isn't on the phone: a translate icon in the address pill (TranslateCue),
 * and a small floating card, on its own only where it helps, else from the icon (once per page; it
 * leaves after a while, with ✕ or a tap outside). Translating says
 * so in the toast, and the toast that says it's done has "Show original". A language set to
 * "Always translate" gets no offer: its pages are translated on their own.
 */
@Composable
fun TranslateOfferHost(components: Components, tabId: String?, allowed: Boolean, onToast: (Toast) -> Unit, modifier: Modifier = Modifier) {
    val store = components.store
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val decay = remember(density) { splineBasedDecay<Float>(density) }
    val t by store.observeSlice(tabId) { s -> tabId?.let { s.findTab(it) }?.let { Triple(it.translationsState, it.content.url, it.readerState.readerable) } }
    val state = t?.first
    val url = t?.second.orEmpty()
    val readable = t?.third == true
    val detected = state?.translationEngineState?.detectedLanguages
    val from = detected?.documentLangTag?.takeIf { detected.supportedDocumentLang == true }
    val to = remember { Locale.getDefault().language }
    // Every language set on the phone (Settings › Languages), not only the first: never offered.
    val phoneLanguages = remember { phoneLanguages() }
    val offerOn by components.settings.translateOffer.collectAsStateWithLifecycle(initialValue = true)
    var dismissed by remember { mutableStateOf(emptySet<String>()) }
    // Done: the toast says so, with the way back ([autoFrom]: translated on its own, from that language).
    var asked by remember { mutableStateOf<String?>(null) }
    var autoFrom by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state?.isTranslated, asked) {
        if (state?.isTranslated == true && asked == tabId && tabId != null) {
            asked = null
            val what = autoFrom?.let { "Translated from ${languageName(it)}" } ?: "Page translated"
            autoFrom = null
            onToast(Toast(what, "Show original") { store.dispatch(TranslationsAction.TranslateRestoreAction(tabId)) })
        }
    }
    val key = "$tabId|$url"
    val always = state?.pageSettings?.alwaysTranslateLanguage == true
    // The icon in the address pill, and the card on its own only where it helps (TranslateCue).
    val cues = components.translateCue
    val engines by components.settings.searchEngines.collectAsStateWithLifecycle(initialValue = dev.nerzouille.koan.search.SearchEngine.builtIns)
    val site = remember(url) { runCatching { java.net.URI(url).host }.getOrNull().orEmpty().removePrefix("www.") }
    val searchPage = remember(url, engines) { isSearchResults(url, engines) }
    val signals = TranslateSignals(
        pageLanguage = from,
        geckoOffers = state?.isOfferTranslate == true,
        readable = readable,
        translated = state?.isTranslated == true,
        processing = state?.isTranslateProcessing == true,
        always = always,
        never = state?.pageSettings?.neverTranslateLanguage == true || state?.pageSettings?.neverTranslateSite == true,
    )
    val cue = if (tabId == null) TranslateCue.None else translateCue(signals, phoneLanguages, offerOn, searchPage, site in cues.dismissedSites)
    androidx.compose.runtime.SideEffect { cues.icon = cue != TranslateCue.None }
    // A tap on the icon: the card on this page, even where it wouldn't open by itself.
    var manual by remember { mutableStateOf<String?>(null) }
    val seenRequests = remember { androidx.compose.runtime.mutableIntStateOf(cues.openRequests) }
    LaunchedEffect(cues.openRequests) {
        if (cues.openRequests == seenRequests.intValue) return@LaunchedEffect
        seenRequests.intValue = cues.openRequests
        manual = key
        dismissed = dismissed - key
    }
    val offer = allowed && tabId != null && from != null && key !in dismissed &&
        (cue == TranslateCue.Card || (manual == key && cue != TranslateCue.None))

    // "Always translate" this language: the page is translated on its own (unless Gecko already
    // started it), and the toast says so, with the way back.
    var auto by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(key, always, from) {
        val id = tabId ?: return@LaunchedEffect
        val lang = from ?: return@LaunchedEffect
        if (!allowed || !always || key in auto || lang.equals(to, ignoreCase = true)) return@LaunchedEffect
        delay(TranslateTokens.AUTO_WAIT_MS)
        val now = store.state.findTab(id)?.translationsState ?: return@LaunchedEffect
        auto = auto + key
        asked = id
        autoFrom = lang
        if (!now.isTranslated && !now.isTranslateProcessing) {
            store.dispatch(TranslationsAction.TranslateAction(id, lang, to, TranslationOptions(downloadModel = true)))
        }
    }


    val card = remember { SheetState(scope, decay) }
    // It leaves on its own after a while: that isn't a dismissal (the site keeps its card).
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(offer, key) {
        if (offer) {
            timedOut = false
            card.animateTo(Detent.Compact)
            delay(TranslateTokens.OFFER_MS)
            timedOut = true
            card.animateTo(Detent.Closed)
        } else {
            card.animateTo(Detent.Closed)
        }
    }
    // Closed by a tap outside, Back or a drag down: not offered again on this page, and no card
    // by itself on this site until Koan restarts (the icon stays).
    LaunchedEffect(card.open) {
        if (!card.open && offer) {
            dismissed = dismissed + key
            if (!timedOut && site.isNotEmpty()) cues.dismissedSites += site
            if (manual == key) manual = null
        }
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
            onDismiss = {
                dismissed = dismissed + key
                if (site.isNotEmpty()) cues.dismissedSites += site
                if (manual == key) manual = null
            },
            onAlwaysLanguage = {
                dismissed = dismissed + key
                auto = auto + key
                asked = id
                autoFrom = null
                store.dispatch(TranslationsAction.UpdatePageSettingAction(id, TranslationPageSettingOperation.UPDATE_ALWAYS_TRANSLATE_LANGUAGE, true))
                store.dispatch(TranslationsAction.TranslateAction(id, lang, to, TranslationOptions(downloadModel = true)))
                onToast(Toast("${languageName(lang)} pages will be translated", detail = "On this phone; the first time downloads the language"))
            },
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

/** The phone's languages, in order (Settings › Languages): primary codes such as "en", "fr". */
private fun phoneLanguages(): List<String> {
    val list = android.os.LocaleList.getDefault()
    return (0 until list.size()).map { list[it].language }
}

/**
 * Whether a page in [pageLanguage] gets the translation card: the card is on ([offerOn]) and the
 * language is none of the phone's ([phoneLanguages], compared on the primary code: "fr-CA" is
 * French).
 */
internal fun offerFor(pageLanguage: String, phoneLanguages: List<String>, offerOn: Boolean): Boolean {
    if (!offerOn) return false
    val page = pageLanguage.substringBefore('-').lowercase()
    return phoneLanguages.none { it.substringBefore('-').lowercase() == page }
}
