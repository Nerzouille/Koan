package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens

/** What Settings › Translation lists: the languages set to Always or Never, and the Never sites. */
@Immutable
class TranslationSettings(
    /** Language code → its name. */
    val always: List<Pair<String, String>> = emptyList(),
    val never: List<Pair<String, String>> = emptyList(),
    val neverSites: List<String> = emptyList(),
    /** Languages downloaded on the phone: code, name, size in bytes (null unknown). */
    val onPhone: List<Triple<String, String, Long?>> = emptyList(),
    /** Asks, then deletes a downloaded language. */
    val onDeleteLanguage: (String) -> Unit = {},
    /** Back to "offer" for that language. */
    val onRemoveLanguage: (String) -> Unit = {},
    val onRemoveSite: (String) -> Unit = {},
    /** The page opened: Gecko's lists are fetched again. */
    val onOpen: () -> Unit = {},
)

/**
 * Settings › Translation: the languages always translated, the languages and the sites never
 * offered, each with Remove, and the languages downloaded on the phone, each with Delete. Pages are translated on the phone (Firefox Translations).
 */
@Composable
internal fun TranslationPage(t: TranslationSettings, on: Boolean, onBack: () -> Unit) {
    LaunchedEffect(Unit) { t.onOpen() }
    val palette = KoanTheme.colors
    SettingsPageFrame("Translation", on, onBack) {
        Text(
            "Pages are translated on this phone, never sent anywhere. Each language downloads once.",
            color = palette.TextSecondary,
            fontSize = SettingsTokens.RowValueSize,
        )
        Section("Always translate", t.always.map { it.second to { t.onRemoveLanguage(it.first) } }, "No language yet. Turn it on from ⋯ › Translate page.", on)
        Section("Never translate", t.never.map { it.second to { t.onRemoveLanguage(it.first) } }, "No language.", on)
        SettingsSection("On this phone")
        if (t.onPhone.isEmpty()) {
            Text("No language downloaded yet.", color = palette.TextSecondary, fontSize = SettingsTokens.RowValueSize, modifier = Modifier.padding(vertical = SettingsTokens.Divider * 4))
        }
        t.onPhone.forEachIndexed { i, (code, name, bytes) ->
            if (i > 0) SettingsDivider()
            SettingsRow(null, name, on, subtitle = bytes?.let { "%.0f MB".format(it / (1L shl 20).toDouble()) }) {
                RowAction("Delete") { if (on) t.onDeleteLanguage(code) }
            }
        }
        Section("Never translate these sites", t.neverSites.map { site -> site.removePrefix("https://").removePrefix("http://") to { t.onRemoveSite(site) } }, "No site.", on)
    }
}

@Composable
private fun Section(title: String, rows: List<Pair<String, () -> Unit>>, empty: String, on: Boolean) {
    SettingsSection(title)
    if (rows.isEmpty()) {
        Text(empty, color = KoanTheme.colors.TextSecondary, fontSize = SettingsTokens.RowValueSize, modifier = Modifier.padding(vertical = SettingsTokens.Divider * 4))
        return
    }
    rows.forEachIndexed { i, (label, remove) ->
        if (i > 0) SettingsDivider()
        SettingsRow(null, label, on) { RowAction("Remove") { if (on) remove() } }
    }
}
