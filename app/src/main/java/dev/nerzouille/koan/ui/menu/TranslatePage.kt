package dev.nerzouille.koan.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanField
import dev.nerzouille.koan.ui.kit.KoanHaptics
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.SecondaryButton
import dev.nerzouille.koan.ui.kit.TextLink
import dev.nerzouille.koan.ui.sheet.RowChevron
import dev.nerzouille.koan.ui.sheet.RowValue
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetPageScope
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.sheet.SheetToggleRow
import dev.nerzouille.koan.ui.theme.BrandHorizontal
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens
import dev.nerzouille.koan.ui.theme.TranslateTokens
import java.util.Locale
import mozilla.components.concept.engine.translate.ModelState

/** A language Firefox Translations offers: its code ("zh", "fr") and the name to show. */
@Immutable
data class TranslateLanguage(val code: String, val name: String)

/** Where the current page stands with translation (the tab's TranslationsState, simplified). */
@Immutable
data class TranslateUi(
    /** The page's language, if Gecko found one; null on an untranslatable page. */
    val pageLanguage: String?,
    val from: List<TranslateLanguage>,
    val to: List<TranslateLanguage>,
    val translated: Boolean,
    val working: Boolean,
    /** Bytes the language models still need, once; null unknown, 0 none. */
    val downloadBytes: Long?,
    val error: String?,
    val neverLanguage: Boolean,
    val neverSite: Boolean,
    /** Gecko can't translate on this phone at all. */
    val unsupported: Boolean,
    /** Pages in this page's language are translated on their own. */
    val alwaysLanguage: Boolean = false,
    /** Languages whose models are on the phone already. */
    val downloaded: Set<String> = emptySet(),
    /** Each language's model download, in bytes, when Gecko said. */
    val modelBytes: Map<String, Long> = emptyMap(),
    /** The last languages picked on each side, newest first. */
    val recentFrom: List<String> = emptyList(),
    val recentTo: List<String> = emptyList(),
)

@Immutable
class TranslateActions(
    val translate: (from: String, to: String) -> Unit,
    val showOriginal: () -> Unit,
    val neverLanguage: (Boolean) -> Unit,
    val neverSite: (Boolean) -> Unit,
    /** The pair changed: ask Gecko how much it would download. */
    val pairChanged: (from: String, to: String) -> Unit,
    val alwaysLanguage: (Boolean) -> Unit = {},
    /** A language picked in a list ("from" or "to"): it goes to that side's Recent. */
    val picked: (side: String, code: String) -> Unit = { _, _ -> },
    /** Long press on a downloaded language: asks, then deletes its model from the phone. */
    val deleteLanguage: (code: String) -> Unit = {},
)

/** A language code as a name, in English ("zh" → "Chinese"), for when Gecko gave none. */
fun languageName(code: String): String =
    Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).ifEmpty { code }

/**
 * ⋯ › Translate page: from (the page's language, detected) → to (the phone's language by default),
 * each one a picker; Translate (the models download once, with their size and a progress bar while
 * it works), Show original once translated, and Never for this language / this site.
 * Fully on the phone: Firefox Translations runs in Gecko.
 */
@Composable
fun TranslatePage(ui: TranslateUi, target: String, actions: TranslateActions, scope: SheetPageScope) {
    val on = scope.interactive
    val palette = KoanTheme.colors
    var from by remember(ui.pageLanguage) { mutableStateOf(ui.pageLanguage ?: ui.from.firstOrNull()?.code.orEmpty()) }
    var to by remember(target) { mutableStateOf(target) }
    var picking by remember { mutableStateOf<String?>(null) } // "from" or "to"
    fun name(code: String, list: List<TranslateLanguage>) = list.firstOrNull { it.code == code }?.name ?: languageName(code)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SheetTokens.Padding)
            .padding(bottom = SheetTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap),
    ) {
        Text("Translate page", color = palette.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize))
        if (ui.unsupported) {
            Text("Translation doesn't work on this phone.", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
            return@Column
        }
        val choosing = picking
        if (choosing != null) {
            BackHandler { picking = null }
            LanguagePicker(
                title = if (choosing == "from") "Translate from" else "Translate to",
                languages = if (choosing == "from") ui.from else ui.to,
                current = if (choosing == "from") from else to,
                recent = if (choosing == "from") ui.recentFrom else ui.recentTo,
                downloaded = ui.downloaded,
                sizes = ui.modelBytes,
                interactive = on,
                onDelete = actions.deleteLanguage,
                onBack = { picking = null },
                onPick = { code ->
                    if (choosing == "from") from = code else to = code
                    actions.picked(choosing, code)
                    picking = null
                    actions.pairChanged(from, to)
                },
            )
            return@Column
        }
        SheetGroup {
            SheetRow(null, "From", on, onClick = { picking = "from" }) { RowValue(if (from.isEmpty()) "Pick" else name(from, ui.from)); RowChevron() }
            SheetDivider()
            SheetRow(null, "To", on, onClick = { picking = "to" }) { RowValue(name(to, ui.to)); RowChevron() }
        }
        when {
            ui.working -> Working(ui.downloadBytes)
            ui.error != null -> Text(ui.error, color = palette.Destructive, fontSize = SheetTokens.CardTextSize)
            ui.downloadBytes != null && ui.downloadBytes > 0 ->
                Text("Downloads ${sizeLabel(ui.downloadBytes)} once. Pages are translated on this phone, never sent anywhere.", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
            else -> Text("Pages are translated on this phone, never sent anywhere.", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
        }
        if (ui.translated) {
            Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
                SecondaryButton("Show original", onClick = { if (on) actions.showOriginal() }, modifier = Modifier.weight(1f))
                PrimaryButton("Translate again", onClick = { if (on && from.isNotEmpty()) actions.translate(from, to) }, modifier = Modifier.weight(1f))
            }
        } else {
            PrimaryButton(
                if (ui.working) "Translating…" else "Translate to ${name(to, ui.to)}",
                onClick = { if (on && !ui.working && from.isNotEmpty() && from != to) actions.translate(from, to) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SheetGroup {
            SheetToggleRow(null, "Always translate ${name(from, ui.from)}", on, checked = ui.alwaysLanguage, onChange = actions.alwaysLanguage)
            SheetDivider()
            SheetToggleRow(null, "Never translate ${name(from, ui.from)}", on, checked = ui.neverLanguage, onChange = actions.neverLanguage)
            SheetDivider()
            SheetToggleRow(null, "Never translate this site", on, checked = ui.neverSite, onChange = actions.neverSite)
        }
    }
}

/**
 * A From / To list: ‹ back to the Translate page, a search field, then the languages in sections,
 * each one once, in the first that fits: Recent (the last 3 picked on that side), On this phone
 * (models already downloaded; a long press deletes one), All languages (A–Z, with the download
 * size when Gecko said). While searching, only the matches, without sections.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LanguagePicker(
    title: String,
    languages: List<TranslateLanguage>,
    current: String,
    recent: List<String>,
    downloaded: Set<String>,
    sizes: Map<String, Long>,
    interactive: Boolean,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onDelete: (String) -> Unit = {},
) {
    val view = LocalView.current
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(TranslateTokens.Close).clip(CircleShape).clickable(enabled = interactive, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(KoanIcons.Back, "Back", tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph)) }
        Spacer(Modifier.width(SheetTokens.Gap / 2))
        Text(title, color = palette.TextPrimary, style = KoanType.headline(SheetTokens.CardTitleSize))
    }
    KoanField(query, { query = it }, "Search languages", Modifier.fillMaxWidth(), search = true)
    val byCode = languages.associateBy { it.code }
    val q = query.trim()
    val sections: List<Pair<String?, List<TranslateLanguage>>> = if (q.isNotEmpty()) {
        listOf(null to languages.filter { it.name.contains(q, ignoreCase = true) || it.code.equals(q, ignoreCase = true) })
    } else {
        val recents = recent.mapNotNull(byCode::get).take(TranslateTokens.RECENT)
        val onPhone = languages.filter { it.code in downloaded && it !in recents }
        val rest = languages.filter { it !in recents && it !in onPhone }
        listOf("Recent" to recents, "On this phone" to onPhone, "All languages" to rest).filter { it.second.isNotEmpty() }
    }
    if (sections.all { it.second.isEmpty() }) {
        Text("No language matches “$q”.", color = palette.TextSecondary, fontSize = SheetTokens.CardTextSize)
    }
    sections.forEach { (heading, list) ->
        if (list.isEmpty()) return@forEach
        if (heading != null) Text(heading, color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize, fontWeight = FontWeight.SemiBold)
        SheetGroup {
            list.forEachIndexed { i, lang ->
                if (i > 0) SheetDivider()
                // A downloaded language: a long press offers to delete it from the phone.
                val onPhone = lang.code in downloaded
                SheetRow(
                    null, lang.name, interactive,
                    modifier = if (interactive) Modifier.combinedClickable(onClick = { onPick(lang.code) }, onLongClick = if (onPhone) ({ KoanHaptics.hold(view); onDelete(lang.code) }) else null) else Modifier,
                ) {
                    when {
                        lang.code == current -> Icon(KoanIcons.Check, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph))
                        lang.code in downloaded -> RowValue("On phone")
                        else -> sizes[lang.code]?.takeIf { it > 0 }?.let { RowValue(sizeLabel(it)) }
                    }
                }
            }
        }
    }
}

/** Koan at work: a gradient sweep along a thin track, the download size if there is one. */
@Composable
private fun Working(downloadBytes: Long?) {
    val palette = KoanTheme.colors
    val sweep by rememberInfiniteTransition(label = "translate").animateFloat(
        0f, 1f, infiniteRepeatable(tween(TranslateTokens.SWEEP_MS), RepeatMode.Restart), label = "sweep",
    )
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap / 2)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(TranslateTokens.Track)
                .drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(palette.Glass, cornerRadius = r)
                    val w = size.width * 0.35f
                    val x = (size.width + w) * sweep - w
                    drawRoundRect(BrandHorizontal, topLeft = Offset(x.coerceAtLeast(0f), 0f), size = Size((w + minOf(x, 0f)).coerceAtMost(size.width - x.coerceAtLeast(0f)), size.height), cornerRadius = r)
                },
        )
        Text(
            if (downloadBytes != null && downloadBytes > 0) "Downloading the language (${sizeLabel(downloadBytes)}) and translating…" else "Translating…",
            color = palette.TextSecondary,
            fontSize = SheetTokens.CardTextSize,
        )
    }
}

/**
 * A page in another language: the small offer above the dock. "Translate to French", ✕, and a
 * quiet "Always for Chinese" / "Never for Chinese" / "Never for this site".
 */
@Composable
fun TranslateOffer(
    pageLanguage: String,
    target: String,
    onTranslate: () -> Unit,
    onDismiss: () -> Unit,
    onNeverLanguage: () -> Unit,
    onNeverSite: () -> Unit,
    modifier: Modifier = Modifier,
    onAlwaysLanguage: () -> Unit = {},
) {
    val palette = KoanTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap / 2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(KoanIcons.Translate, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph))
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Text(
                "This page is in ${languageName(pageLanguage)}",
                color = palette.TextPrimary,
                fontSize = SheetTokens.RowTitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(TranslateTokens.Close).clip(CircleShape).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) { Icon(KoanIcons.Close, "Not now", tint = palette.TextSecondary, modifier = Modifier.size(SheetTokens.RowGlyph * 0.8f)) }
        }
        PrimaryButton("Translate to ${languageName(target)}", onClick = onTranslate, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            QuietLink("Always for ${languageName(pageLanguage)}", onAlwaysLanguage)
            QuietLink("Never for ${languageName(pageLanguage)}", onNeverLanguage)
            QuietLink("Never for this site", onNeverSite)
        }
    }
}

@Composable
private fun QuietLink(text: String, onClick: () -> Unit) =
    TextLink(text, onClick, SheetTokens.CardTextSize, PaddingValues(horizontal = SheetTokens.Gap / 2, vertical = SheetTokens.Gap / 2))

private fun sizeLabel(bytes: Long): String = when {
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
    else -> "${(bytes shr 10).coerceAtLeast(1)} KB"
}

/** The tab's translation state and Gecko's languages, as the Translate page shows them. */
fun translateUiOf(
    t: mozilla.components.browser.state.state.TranslationsState,
    engine: mozilla.components.browser.state.state.TranslationsBrowserState,
): TranslateUi {
    val detected = t.translationEngineState?.detectedLanguages
    val support = engine.supportedLanguages
    fun list(langs: List<mozilla.components.concept.engine.translate.Language>?) =
        langs.orEmpty().map { TranslateLanguage(it.code, it.localizedDisplayName?.ifBlank { null } ?: languageName(it.code)) }.sortedBy { it.name }
    val error = (t.translationError ?: engine.engineError)?.takeIf { it.displayError }?.let { "Couldn't translate this page." }
    return TranslateUi(
        pageLanguage = detected?.documentLangTag?.takeIf { detected.supportedDocumentLang == true },
        from = list(support?.fromLanguages),
        to = list(support?.toLanguages),
        translated = translatedNow(t.isTranslated, t.translationEngineState?.let { hasPair(it.requestedTranslationPair) }),
        working = workingNow(t.isTranslated, t.translationEngineState?.let { hasPair(it.requestedTranslationPair) }, t.isTranslateProcessing, t.isRestoreProcessing),
        downloadBytes = t.translationDownloadSize?.size,
        error = error,
        neverLanguage = t.pageSettings?.neverTranslateLanguage == true,
        neverSite = t.pageSettings?.neverTranslateSite == true,
        unsupported = engine.isEngineSupported == false,
        alwaysLanguage = t.pageSettings?.alwaysTranslateLanguage == true,
        downloaded = engine.languageModels.orEmpty().filter { it.status == ModelState.DOWNLOADED }.mapNotNullTo(HashSet()) { it.language?.code },
        modelBytes = engine.languageModels.orEmpty().mapNotNull { m -> m.language?.code?.let { code -> m.size?.let { code to it } } }.toMap(),
    )
}

/**
 * Gecko's state holds the pair the page was translated to. Android Components always builds a
 * pair, with null languages when the page isn't translated: only both languages count.
 */
internal fun hasPair(pair: mozilla.components.concept.engine.translate.TranslationPair?) =
    pair?.fromLanguage != null && pair.toLanguage != null

/**
 * The page shows a translation. [hasPair]: whether Gecko's own state for the page holds the pair it
 * translated to (null before Gecko said anything): that is what the page is, whatever the flags.
 */
internal fun translatedNow(isTranslated: Boolean, hasPair: Boolean?) = hasPair ?: isTranslated

/**
 * Gecko is still at it. Its processing flags can stay up once the page is done (Show original,
 * then Translate again, left "translating" stuck): a translation counts as running only until
 * the page is translated, a restore only while it still is.
 */
internal fun workingNow(isTranslated: Boolean, hasPair: Boolean?, translating: Boolean, restoring: Boolean): Boolean {
    val translated = translatedNow(isTranslated, hasPair)
    return (translating && !translated) || (restoring && translated)
}
