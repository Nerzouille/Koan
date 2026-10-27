package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import dev.nerzouille.koan.search.CustomEngines
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.DestructiveButton
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens

/**
 * Settings › Search engine: Koan's engines and the owner's own. A tap makes one the default; a
 * custom one also has Edit (and Delete inside). "Add a search engine" at the end.
 */
@Composable
internal fun EnginesPage(ui: SettingsUi, on: Boolean, onBack: () -> Unit, push: (SettingsPage) -> Unit) {
    SettingsPageFrame("Search engine", on, onBack) {
        ui.engines.forEachIndexed { i, engine ->
            if (i > 0) SettingsDivider()
            SettingsRow(
                null, engine.label, on,
                subtitle = if (engine.custom) listOfNotNull(engine.shortcut?.let { "@$it" }, displayHost(engine.home)).joinToString(" · ") else null,
                onClick = { ui.onEngine(engine) },
            ) {
                if (engine.id == ui.engine.id) {
                    Icon(KoanIcons.Check, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
                }
                if (engine.custom) RowAction("Edit") { if (on) push(SettingsPage.EditEngine(engine.id)) }
            }
        }
        SettingsDivider()
        SettingsRow(KoanIcons.Plus, "Add a search engine", on, onClick = { push(SettingsPage.EditEngine(null)) })
    }
}

/**
 * Add or edit a custom engine ([page]'s id null = a new one, maybe prefilled from a page's
 * OpenSearch description). Each field says what is wrong as soon as it has text; Save checks all.
 */
@Composable
internal fun EngineEditPage(ui: SettingsUi, page: SettingsPage.EditEngine, on: Boolean, onBack: () -> Unit) {
    val editing = remember { ui.engines.firstOrNull { it.id == page.id && it.custom } }
    val draft = page.draft
    var name by rememberSaveable { mutableStateOf(editing?.label ?: draft?.name.orEmpty()) }
    var url by rememberSaveable { mutableStateOf(editing?.searchTemplate ?: draft?.url.orEmpty()) }
    var shortcut by rememberSaveable { mutableStateOf(editing?.shortcut?.let { "@$it" } ?: draft?.shortcut.orEmpty()) }
    var suggest by rememberSaveable { mutableStateOf(editing?.suggestTemplate ?: draft?.suggest.orEmpty()) }
    var tried by rememberSaveable { mutableStateOf(false) }
    val check = CustomEngines.check(name, url, shortcut, suggest, ui.engines.filter { it.id != editing?.id })
    fun shown(error: String?, value: String) = error.takeIf { tried || value.isNotBlank() }

    SettingsPageFrame(if (editing == null) "Add a search engine" else "Edit search engine", on, onBack) {
        Field("Name", name, "Example Search", shown(check.name, name)) { name = it }
        Field(
            "Search address", url, "https://example.com/search?q=%s", shown(check.url, url),
            help = "Search for anything on the site, copy the address of the results, and put %s where your words were.",
            keyboard = KeyboardType.Uri,
        ) { url = it }
        Field(
            "Shortcut (optional)", shortcut, "@example", shown(check.shortcut, shortcut),
            help = "Type it in the address field to search this engine once.",
        ) { shortcut = it }
        Field(
            "Suggestions address (optional)", suggest, "https://example.com/suggest?q=%s", shown(check.suggest, suggest),
            help = "Leave empty for no suggestions from this engine.",
            keyboard = KeyboardType.Uri,
        ) { suggest = it }
        Spacer(Modifier.height(SettingsTokens.FieldGap))
        PrimaryButton(
            if (editing == null) "Add" else "Save",
            onClick = {
                if (!on) return@PrimaryButton
                tried = true
                if (check.ok) {
                    ui.onSaveEngine(
                        SearchEngine(
                            id = editing?.id ?: CustomEngines.newId(),
                            label = name.trim(),
                            searchTemplate = url.trim(),
                            suggestTemplate = suggest.trim().ifEmpty { null },
                            shortcut = CustomEngines.cleanShortcut(shortcut).ifEmpty { null },
                            custom = true,
                        ),
                    )
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (editing != null) {
            Spacer(Modifier.height(SettingsTokens.FieldGap / 2))
            DestructiveButton("Delete", onClick = { if (on) { ui.onDeleteEngine(editing); onBack() } }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A labelled text field, its [error] (or [help]) under it. */
@Composable
private fun Field(
    label: String,
    value: String,
    hint: String,
    error: String?,
    help: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    val palette = KoanTheme.colors
    Column(Modifier.padding(top = SettingsTokens.FieldGap)) {
        Text(label, color = palette.TextSecondary, fontSize = SettingsTokens.SectionTitleSize, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SettingsTokens.FieldLabelGap))
        Box(
            Modifier
                .fillMaxWidth()
                .height(SettingsTokens.FieldHeight)
                .clip(RoundedCornerShape(SettingsTokens.FieldRadius))
                .background(palette.Glass)
                .padding(horizontal = SettingsTokens.RowInset),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) Text(hint, color = palette.FieldHint, fontSize = SettingsTokens.RowValueSize, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = false),
                textStyle = TextStyle(color = palette.TextPrimary, fontSize = SettingsTokens.RowValueSize),
                cursorBrush = SolidColor(palette.TextPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val note = error ?: help
        if (note != null) {
            Text(
                note,
                color = if (error != null) palette.Destructive else palette.TextSecondary,
                fontSize = SettingsTokens.RowSubtitleSize,
                modifier = Modifier.padding(top = SettingsTokens.FieldLabelGap),
            )
        }
    }
}
