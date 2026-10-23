package dev.nerzouille.koan.ui.menu

import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import dev.nerzouille.koan.ui.sheet.SheetState
import dev.nerzouille.koan.ui.theme.ButtonTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.PromptTokens
import dev.nerzouille.koan.ui.theme.SheetTokens
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.prompt.Choice
import mozilla.components.concept.engine.prompt.PromptRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle as DateTextStyle
import java.util.Date
import java.util.Locale

/**
 * Everything a page asks through the engine that Koan answers on its floating card (design 7j):
 * <select> lists, date/time/month/week pickers, colours, alert/confirm/prompt, "Leave this page?",
 * HTTP sign-in, pop-ups, resending a form. A share goes to Android's share sheet, a folder field to
 * its folder picker. Files and logins have their own UI (FilePrompt, logins). Anything else is
 * dismissed at once, so a page never hangs on a dialog nobody shows. [card]: the card's state,
 * shared with the overlays (an open card sends the toast away).
 */
@Composable
fun PagePrompts(components: Components, tabId: String?, card: SheetState) {
    val store = components.store
    val context = LocalContext.current
    val request by store.observeSlice(tabId) { s ->
        tabId?.let { s.findTab(it) }?.content?.promptRequests?.firstOrNull { it !is PromptRequest.File && it !is PromptRequest.SelectLoginPrompt && it !is PromptRequest.SaveLoginPrompt }
    }
    val pageUrl by store.observeSlice(tabId) { s -> tabId?.let { s.findTab(it) }?.content?.url.orEmpty() }
    val pageHost = displayHost(pageUrl)
    // The request on the card: it stays while the card closes, after it was answered.
    var shown by remember { mutableStateOf<PromptRequest?>(null) }
    var answered by remember { mutableStateOf(false) }
    // A page that keeps opening dialogs (the engine leaves this to the app, as Firefox does): a
    // dialog within a few seconds of the last one offers "Don't let this page show more dialogs";
    // ticked, the page's dialogs are dismissed unseen until its tab loads another address.
    val guard = remember { DialogGuard() }
    var manyDialogs by remember { mutableStateOf(false) }
    LaunchedEffect(tabId, pageUrl) { if (tabId != null && guard.blocked[tabId] != pageUrl) guard.blocked.remove(tabId) }

    fun consume(r: PromptRequest) {
        tabId?.let { store.dispatch(ContentAction.ConsumePromptRequestAction(it, r)) }
    }
    /** Answers the card's request ([reply] calls the page back) and closes the card. */
    fun answer(r: PromptRequest, reply: () -> Unit) {
        if (answered || shown !== r) return
        answered = true
        if (r.isDialog()) guard.lastAnswerAt = SystemClock.elapsedRealtime()
        reply()
        consume(r)
        card.animateTo(Detent.Closed)
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val r = request as? PromptRequest.Folder ?: return@rememberLauncherForActivityResult
        if (uri != null) r.onSelected(context, uri) else r.onDismiss()
        consume(r)
    }

    LaunchedEffect(request) {
        val r = request
        when {
            // Gone without an answer (the page moved on): the card goes too.
            r == null -> if (shown != null && !answered) card.animateTo(Detent.Closed)
            r is PromptRequest.Share -> {
                val d = r.data
                val text = listOfNotNull(d.text, d.url).joinToString("\n").ifEmpty { d.title.orEmpty() }
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    .apply { d.title?.let { putExtra(Intent.EXTRA_SUBJECT, it) } }
                runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    .onSuccess { r.onSuccess() }.onFailure { r.onFailure() }
                consume(r)
            }
            r is PromptRequest.Folder -> runCatching { folderPicker.launch(null) }.onFailure { r.onDismiss(); consume(r) }
            r.isDialog() && tabId != null && guard.blocked[tabId] == pageUrl -> {
                (r as PromptRequest.Dismissible).onDismiss()
                consume(r)
            }
            r.shownOnCard() -> {
                manyDialogs = r.isDialog() && SystemClock.elapsedRealtime() - guard.lastAnswerAt < PromptTokens.MANY_DIALOGS_MS
                shown = r
                answered = false
                card.animateTo(Detent.Compact)
            }
            r is PromptRequest.CertificateRequest -> {
                r.onComplete(null)
                consume(r)
            }
            else -> {
                (r as? PromptRequest.Dismissible)?.onDismiss?.invoke()
                consume(r)
            }
        }
    }
    // Closed by a tap outside, a drag or Back: the page hears "cancel".
    val current by rememberUpdatedState(shown)
    LaunchedEffect(card.open) {
        val r = current
        if (!card.open && r != null && !answered && request === r) {
            answered = true
            if (r.isDialog()) guard.lastAnswerAt = SystemClock.elapsedRealtime()
            (r as? PromptRequest.Dismissible)?.onDismiss?.invoke()
            consume(r)
        }
    }

    KoanCard(card) { interactive ->
        val r = shown ?: return@KoanCard
        val host = pageHost.ifEmpty { "This page" }
        // The engine's title ("The page at https://… says:") is long; the host says the same.
        val says = "$host says"
        fun blockIf(ticked: Boolean) { if (ticked && tabId != null) guard.blocked[tabId] = pageUrl }
        when (r) {
            is PromptRequest.SingleChoice -> ChoiceList(r.choices, multiple = false, interactive) { picked -> answer(r) { r.onConfirm(picked.first()) } }
            is PromptRequest.MenuChoice -> ChoiceList(r.choices, multiple = false, interactive) { picked -> answer(r) { r.onConfirm(picked.first()) } }
            is PromptRequest.MultipleChoice -> ChoiceList(r.choices, multiple = true, interactive) { picked -> answer(r) { r.onConfirm(picked.toTypedArray()) } }
            is PromptRequest.TimeSelection -> TimeSelection(r, interactive, onSet = { date -> answer(r) { r.onConfirm(date) } }, onClear = { answer(r) { r.onClear() } })
            is PromptRequest.Color -> ColorPicker(r.defaultColor, interactive) { color -> answer(r) { r.onConfirm(color) } }
            is PromptRequest.Alert -> {
                var noMore by remember(r) { mutableStateOf(false) }
                PromptTitle(says)
                PromptMessage(r.message)
                if (manyDialogs) NoMoreDialogs(noMore, interactive) { noMore = it }
                Buttons(interactive, primary = "OK" to { answer(r) { blockIf(noMore); r.onConfirm(noMore) } })
            }
            is PromptRequest.Confirm -> {
                var noMore by remember(r) { mutableStateOf(false) }
                PromptTitle(says)
                PromptMessage(r.message)
                if (manyDialogs) NoMoreDialogs(noMore, interactive) { noMore = it }
                Buttons(
                    interactive,
                    primary = r.positiveButtonTitle.ifBlank { "OK" } to { answer(r) { blockIf(noMore); r.onConfirmPositiveButton(noMore) } },
                    secondary = r.negativeButtonTitle.ifBlank { "Cancel" } to { answer(r) { blockIf(noMore); r.onConfirmNegativeButton(noMore) } },
                    third = r.neutralButtonTitle.takeIf { it.isNotBlank() }?.let { it to { answer(r) { blockIf(noMore); r.onConfirmNeutralButton(noMore) } } },
                )
            }
            is PromptRequest.TextPrompt -> {
                var text by remember(r) { mutableStateOf(r.inputValue) }
                var noMore by remember(r) { mutableStateOf(false) }
                PromptTitle(says)
                if (r.inputLabel.isNotBlank()) PromptMessage(r.inputLabel)
                PromptField(text, { text = it }, hint = "", focus = true)
                if (manyDialogs) NoMoreDialogs(noMore, interactive) { noMore = it }
                Buttons(interactive, primary = "OK" to { answer(r) { blockIf(noMore); r.onConfirm(noMore, text) } }, secondary = "Cancel" to { answer(r) { r.onDismiss() } })
            }
            is PromptRequest.BeforeUnload -> {
                PromptTitle("Leave this page?")
                PromptMessage("Changes you made may not be saved.")
                Buttons(interactive, primary = "Leave" to { answer(r) { r.onLeave() } }, secondary = "Stay" to { answer(r) { r.onStay() } })
            }
            is PromptRequest.Authentication -> {
                var user by remember(r) { mutableStateOf(r.userName) }
                var password by remember(r) { mutableStateOf(r.password) }
                PromptTitle("Sign in to ${displayHost(r.uri.orEmpty()).ifEmpty { host }}")
                PromptMessage(if (r.previousFailed) "That didn't work. Check the username and password." else r.message)
                if (!r.onlyShowPassword) PromptField(user, { user = it }, hint = "Username")
                PromptField(password, { password = it }, hint = "Password", password = true)
                Buttons(interactive, primary = "Sign in" to { answer(r) { r.onConfirm(user, password) } }, secondary = "Cancel" to { answer(r) { r.onDismiss() } })
            }
            is PromptRequest.Popup -> {
                PromptTitle("$host wants to open a pop-up")
                displayHost(r.targetUri).takeIf { it.isNotEmpty() }?.let { PromptMessage(it) }
                Buttons(interactive, primary = "Allow" to { answer(r) { r.onAllow() } }, secondary = "Block" to { answer(r) { r.onDeny() } })
            }
            is PromptRequest.Repost -> {
                PromptTitle("Send the form again?")
                PromptMessage("The page will send what you entered again, which could repeat an action such as a purchase.")
                Buttons(interactive, primary = "Resend" to { answer(r) { r.onConfirm() } }, secondary = "Cancel" to { answer(r) { r.onDismiss() } })
            }
            else -> Unit
        }
    }
}

/** When the last dialog was answered, and the pages (tab id → URL) that may show no more. */
private class DialogGuard {
    var lastAnswerAt = 0L
    val blocked = mutableMapOf<String, String>()
}

/** alert(), confirm(), prompt(): what a page can open again and again. */
private fun PromptRequest.isDialog() = this is PromptRequest.Alert || this is PromptRequest.Confirm || this is PromptRequest.TextPrompt

/** The requests Koan shows on its card; [PagePrompts] answers the others itself. */
private fun PromptRequest.shownOnCard() = when (this) {
    is PromptRequest.SingleChoice, is PromptRequest.MenuChoice, is PromptRequest.MultipleChoice,
    is PromptRequest.TimeSelection, is PromptRequest.Color, is PromptRequest.Alert, is PromptRequest.Confirm,
    is PromptRequest.TextPrompt, is PromptRequest.BeforeUnload, is PromptRequest.Authentication,
    is PromptRequest.Popup, is PromptRequest.Repost,
    -> true
    else -> false
}

// ---- building blocks -----------------------------------------------------------------------

@Composable
private fun PromptTitle(text: String) {
    Text(text, color = KoanTheme.colors.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun PromptMessage(text: String) {
    if (text.isBlank()) return
    Spacer(Modifier.height(PromptTokens.TextGap))
    // A page can say a lot: the text scrolls past a few lines.
    LazyColumn(Modifier.heightIn(max = PromptTokens.MessageMaxHeight)) {
        item { Text(text, color = KoanTheme.colors.TextSecondary, fontSize = SheetTokens.RowTitleSize) }
    }
}

/** A text field in the card's style (the address field's grey pill). */
@Composable
private fun PromptField(value: String, onChange: (String) -> Unit, hint: String, password: Boolean = false, focus: Boolean = false) {
    val palette = KoanTheme.colors
    val focuser = remember { FocusRequester() }
    // The keyboard comes up with the card, the page's text selected to be typed over.
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(0, value.length))) }
    // Changed from outside (a colour swatch): the cursor goes to the end.
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    if (focus) LaunchedEffect(Unit) { runCatching { focuser.requestFocus() } }
    Spacer(Modifier.height(SheetTokens.Gap))
    Box(
        Modifier
            .fillMaxWidth()
            .height(SheetTokens.NavFieldHeight)
            .clip(RoundedCornerShape(PromptTokens.FieldRadius))
            .background(palette.BarField)
            .padding(horizontal = SheetTokens.RowPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && hint.isNotEmpty()) Text(hint, color = palette.FieldHint, fontSize = SheetTokens.NavFieldTextSize)
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                onChange(it.text)
            },
            singleLine = true,
            textStyle = TextStyle(color = palette.TextPrimary, fontSize = SheetTokens.NavFieldTextSize),
            cursorBrush = SolidColor(palette.TextPrimary),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = if (password) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().focusRequester(focuser),
        )
    }
}

/** "Don't let this page show more dialogs": offered once a page keeps opening them. */
@Composable
private fun NoMoreDialogs(checked: Boolean, interactive: Boolean, onChange: (Boolean) -> Unit) {
    Spacer(Modifier.height(SheetTokens.Gap))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PromptTokens.FieldRadius))
            .clickable(enabled = interactive) { onChange(!checked) }
            .padding(vertical = PromptTokens.CheckRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(checked)
        Spacer(Modifier.width(SheetTokens.RowPadding))
        Text("Don't let this page show more dialogs", color = KoanTheme.colors.TextPrimary, fontSize = SheetTokens.RowTrailingSize)
    }
}

@Composable
private fun CheckMark(checked: Boolean) {
    val palette = KoanTheme.colors
    Box(
        Modifier
            .size(PromptTokens.Check)
            .clip(RoundedCornerShape(PromptTokens.CheckRadius))
            .background(if (checked) palette.Primary else Color.Transparent)
            .border(SheetTokens.Divider * 2, if (checked) palette.Primary else palette.TextSecondary, RoundedCornerShape(PromptTokens.CheckRadius)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(KoanIcons.Check, null, tint = palette.OnPrimary, modifier = Modifier.size(PromptTokens.CheckGlyph))
    }
}

/** The card's buttons: the main one full width, then the others side by side (as the permission card). */
@Composable
private fun Buttons(
    interactive: Boolean,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
    third: Pair<String, () -> Unit>? = null,
) {
    Spacer(Modifier.height(SheetTokens.Gap * 2))
    val others = listOfNotNull(secondary, third)
    if (others.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
            others.forEach { (text, onClick) -> SecondaryButton(text, interactive, onClick, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(SheetTokens.Gap))
    }
    PrimaryButton(primary.first, onClick = { if (interactive) primary.second() }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SecondaryButton(text: String, interactive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(ButtonTokens.Height)
            .clip(CircleShape)
            .background(KoanTheme.colors.GroupedRow)
            .clickable(enabled = interactive, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = KoanTheme.colors.TextPrimary, fontSize = ButtonTokens.TextSize, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ---- <select> ------------------------------------------------------------------------------

private sealed interface ChoiceRow {
    class Group(val label: String) : ChoiceRow
    class Option(val choice: Choice, val inGroup: Boolean) : ChoiceRow
    data object Separator : ChoiceRow
}

private fun flatten(choices: Array<out Choice>, inGroup: Boolean = false): List<ChoiceRow> = choices.flatMap { c ->
    when {
        c.isGroupType -> listOf(ChoiceRow.Group(c.label)) + flatten(c.children.orEmpty(), inGroup = true)
        c.isASeparator -> listOf(ChoiceRow.Separator)
        else -> listOf(ChoiceRow.Option(c, inGroup))
    }
}

/**
 * A <select>'s options: a tap picks one (single), or ticks them and Done sends them (multiple).
 * Optgroups are small titles, the selected option is ticked; a long list scrolls, opened on it.
 */
@Composable
private fun ColumnScope.ChoiceList(choices: Array<Choice>, multiple: Boolean, interactive: Boolean, onDone: (List<Choice>) -> Unit) {
    val palette = KoanTheme.colors
    val rows = remember(choices) { flatten(choices) }
    var picked by remember(choices) {
        mutableStateOf(rows.filterIsInstance<ChoiceRow.Option>().filter { it.choice.selected }.map { it.choice.id }.toSet())
    }
    val first = rows.indexOfFirst { it is ChoiceRow.Option && it.choice.selected }.coerceAtLeast(0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = (first - 2).coerceAtLeast(0))
    LazyColumn(Modifier.heightIn(max = PromptTokens.ListMaxHeight).fillMaxWidth(), state = listState) {
        itemsIndexed(rows) { _, row ->
            when (row) {
                is ChoiceRow.Group -> Text(
                    row.label,
                    color = palette.TextSecondary,
                    fontSize = SheetTokens.RowSubtitleSize,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = SheetTokens.Gap, bottom = PromptTokens.TextGap),
                )
                ChoiceRow.Separator -> Box(Modifier.fillMaxWidth().padding(vertical = PromptTokens.TextGap).height(SheetTokens.Divider).background(palette.GroupedRow))
                is ChoiceRow.Option -> {
                    val c = row.choice
                    val on = c.id in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = SheetTokens.RowHeight)
                            .clip(RoundedCornerShape(PromptTokens.FieldRadius))
                            .background(if (on && !multiple) palette.GroupedRow else Color.Transparent)
                            .clickable(enabled = interactive && c.enable) {
                                if (multiple) picked = if (on) picked - c.id else picked + c.id else onDone(listOf(c))
                            }
                            .padding(start = if (row.inGroup) PromptTokens.GroupIndent else SheetTokens.RowPadding, end = SheetTokens.RowPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (multiple) {
                            CheckMark(on)
                            Spacer(Modifier.width(SheetTokens.RowPadding))
                        }
                        Text(
                            c.label,
                            color = if (c.enable) palette.TextPrimary else palette.TextDisabled,
                            fontSize = SheetTokens.RowTitleSize,
                            fontWeight = if (on && !multiple) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (on && !multiple) Icon(KoanIcons.Check, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowGlyph))
                    }
                }
            }
        }
    }
    if (multiple) {
        val all = rows.filterIsInstance<ChoiceRow.Option>().map { it.choice }
        Spacer(Modifier.height(SheetTokens.Gap))
        PrimaryButton("Done", onClick = { if (interactive) onDone(all.filter { it.id in picked }) }, modifier = Modifier.fillMaxWidth())
    }
}

// ---- dates and times -----------------------------------------------------------------------

/**
 * <input type=date/time/datetime-local/month/week>. Dates use the system's calendar picker in
 * Koan's colours (a week field is a date: the engine turns it into its week), months a year and
 * a grid, times the clock. Clear empties the field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelection(r: PromptRequest.TimeSelection, interactive: Boolean, onSet: (Date) -> Unit, onClear: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val initial = remember(r) { LocalDateTime.ofInstant(r.initialDate.toInstant(), zone) }
    val min = remember(r) { r.minimumDate?.let { LocalDateTime.ofInstant(it.toInstant(), zone).toLocalDate() } }
    val max = remember(r) { r.maximumDate?.let { LocalDateTime.ofInstant(it.toInstant(), zone).toLocalDate() } }
    val type = r.type
    var date by remember(r) { mutableStateOf(initial.toLocalDate()) }
    // Date and time: the date first, then the time.
    var onTime by remember(r) { mutableStateOf(type == PromptRequest.TimeSelection.Type.TIME) }
    val palette = KoanTheme.colors

    if (r.title.isNotBlank()) PromptTitle(r.title)
    when {
        type == PromptRequest.TimeSelection.Type.MONTH -> MonthGrid(date, min, max, interactive) { date = it }
        !onTime -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                // Months outside the field's min/max can't be reached at all.
                yearRange = (min?.year ?: DatePickerDefaults.YearRange.first)..(max?.year ?: DatePickerDefaults.YearRange.last),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        return (min == null || !d.isBefore(min)) && (max == null || !d.isAfter(max))
                    }
                    override fun isSelectableYear(year: Int) = (min == null || year >= min.year) && (max == null || year <= max.year)
                },
            )
            LaunchedEffect(state.selectedDateMillis) {
                state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            }
            DatePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = palette.Dock,
                    weekdayContentColor = palette.TextSecondary,
                    subheadContentColor = palette.TextSecondary,
                    navigationContentColor = palette.TextPrimary,
                    yearContentColor = palette.TextPrimary,
                    currentYearContentColor = palette.TextPrimary,
                    selectedYearContentColor = palette.OnPrimary,
                    selectedYearContainerColor = palette.Primary,
                    dayContentColor = palette.TextPrimary,
                    disabledDayContentColor = palette.TextDisabled,
                    selectedDayContentColor = palette.OnPrimary,
                    selectedDayContainerColor = palette.Primary,
                    todayContentColor = palette.TextPrimary,
                    todayDateBorderColor = palette.TextSecondary,
                    dividerColor = Color.Transparent,
                ),
            )
        }
        else -> Unit
    }
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val time = rememberTimePickerState(initial.hour, initial.minute, is24)
    if (onTime) {
        Spacer(Modifier.height(SheetTokens.Gap))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimePicker(
                state = time,
                colors = TimePickerDefaults.colors(
                    clockDialColor = palette.GroupedRow,
                    clockDialSelectedContentColor = palette.OnPrimary,
                    clockDialUnselectedContentColor = palette.TextPrimary,
                    selectorColor = palette.Primary,
                    containerColor = palette.Dock,
                    periodSelectorBorderColor = palette.TextDisabled,
                    periodSelectorSelectedContainerColor = palette.Primary,
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = palette.OnPrimary,
                    periodSelectorUnselectedContentColor = palette.TextPrimary,
                    timeSelectorSelectedContainerColor = palette.Primary,
                    timeSelectorUnselectedContainerColor = palette.GroupedRow,
                    timeSelectorSelectedContentColor = palette.OnPrimary,
                    timeSelectorUnselectedContentColor = palette.TextPrimary,
                ),
            )
        }
    }
    val needsTime = type == PromptRequest.TimeSelection.Type.DATE_AND_TIME && !onTime
    fun result(): Date {
        val t = if (type == PromptRequest.TimeSelection.Type.TIME || type == PromptRequest.TimeSelection.Type.DATE_AND_TIME) LocalTime.of(time.hour, time.minute) else LocalTime.MIDNIGHT
        return Date.from(date.atTime(t).atZone(zone).toInstant())
    }
    Buttons(
        interactive,
        primary = (if (needsTime) "Next" else "Set") to { if (needsTime) onTime = true else onSet(result()) },
        secondary = "Clear" to onClear,
    )
}

/** <input type=month>: ‹ year › and the twelve months. */
@Composable
private fun MonthGrid(selected: LocalDate, min: LocalDate?, max: LocalDate?, interactive: Boolean, onPick: (LocalDate) -> Unit) {
    val palette = KoanTheme.colors
    var year by remember { mutableStateOf(selected.year) }
    Row(Modifier.fillMaxWidth().padding(vertical = SheetTokens.Gap), verticalAlignment = Alignment.CenterVertically) {
        YearStep("‹", interactive && (min == null || year > min.year)) { year-- }
        Text("$year", color = palette.TextPrimary, fontSize = SheetTokens.CardTitleSize, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        YearStep("›", interactive && (max == null || year < max.year)) { year++ }
    }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ENGLISH
    for (rowStart in 1..12 step 3) {
        Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
            for (m in rowStart until rowStart + 3) {
                val first = LocalDate.of(year, m, 1)
                val allowed = (min == null || !first.withDayOfMonth(first.lengthOfMonth()).isBefore(min)) && (max == null || !first.isAfter(max))
                val on = selected.year == year && selected.monthValue == m
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(PromptTokens.MONTH_ASPECT)
                        .clip(RoundedCornerShape(PromptTokens.FieldRadius))
                        .background(if (on) palette.Primary else palette.GroupedRow)
                        .clickable(enabled = interactive && allowed) { onPick(first) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        first.month.getDisplayName(DateTextStyle.SHORT, locale),
                        color = when {
                            on -> palette.OnPrimary
                            allowed -> palette.TextPrimary
                            else -> palette.TextDisabled
                        },
                        fontSize = SheetTokens.RowTitleSize,
                    )
                }
            }
        }
        Spacer(Modifier.height(SheetTokens.TileGap))
    }
}

@Composable
private fun YearStep(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(SheetTokens.NavFieldHeight).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) KoanTheme.colors.TextPrimary else KoanTheme.colors.TextDisabled, fontSize = SheetTokens.CardTitleSize)
    }
}

// ---- colour --------------------------------------------------------------------------------

/** <input type=color>: a set of swatches, or a hex value typed in. */
@Composable
private fun ColorPicker(initial: String, interactive: Boolean, onSet: (String) -> Unit) {
    val palette = KoanTheme.colors
    var hex by remember(initial) { mutableStateOf(initial.removePrefix("#").lowercase()) }
    val parsed = parseHex(hex)
    PromptTitle("Choose a colour")
    Spacer(Modifier.height(SheetTokens.Gap))
    for (row in PromptTokens.Swatches.chunked(PromptTokens.SWATCHES_PER_ROW)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
            row.forEach { c ->
                val code = "%06x".format(c.toArgb() and 0xFFFFFF)
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (code == hex) PromptTokens.SwatchRing else SheetTokens.Divider, if (code == hex) palette.TextPrimary else palette.GroupedRow, CircleShape)
                        .clickable(enabled = interactive) { hex = code },
                )
            }
        }
        Spacer(Modifier.height(SheetTokens.TileGap))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(SheetTokens.NavFieldHeight).clip(CircleShape).background(parsed ?: Color.Transparent).border(SheetTokens.Divider, palette.GroupedRow, CircleShape))
        Spacer(Modifier.width(SheetTokens.RowPadding))
        Box(Modifier.weight(1f)) { PromptField(hex, { hex = it.removePrefix("#").take(6) }, hint = "Hex, like 1a2b3c") }
    }
    Buttons(interactive, primary = "Set" to { if (parsed != null) onSet("#$hex") })
}

private fun parseHex(hex: String): Color? =
    hex.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
