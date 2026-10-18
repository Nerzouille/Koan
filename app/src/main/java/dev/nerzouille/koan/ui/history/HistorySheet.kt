package dev.nerzouille.koan.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.HistoryTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.SearchFieldTokens
import dev.nerzouille.koan.ui.theme.SheetTokens
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * History (from the ⋯ menu): a full-height sheet of the pages visited, the phone's and the
 * account's (desktop) mixed, newest first and by day. The search field sits at the bottom, in
 * thumb reach and on the keyboard. Tap opens a page in a new tab; a swipe left removes it (a
 * synced page is only hidden on the phone). Drag the top down, tap above or Back closes it.
 */
@Composable
fun HistorySheet(
    open: Boolean,
    dao: KoanDao,
    onOpen: (String) -> Unit,
    onRemove: (VisitEntity) -> Unit,
    onClearData: () -> Unit,
    onClose: () -> Unit,
) {
    val slide = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(open) { slide.animateTo(if (open) 1f else 0f, Motion.Sheet) }
    if (!open && slide.value == 0f) return
    if (open) BackHandler(onBack = onClose)

    val palette = KoanTheme.colors
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize.height
    val sheetPx = window - WindowInsets.statusBars.getTop(density) - with(density) { SheetTokens.FullTopGap.toPx() }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(open) { if (!open) query = "" }
    // The last list stays on screen while the next query runs.
    val rows by produceState<List<VisitEntity>?>(null, query) {
        dao.observeHistory(query.trim(), HistoryTokens.LIMIT).collect { value = it }
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(palette.SheetScrim, alpha = slide.value) }
            .then(if (open) Modifier.pointerInput(Unit) { detectTapGestures { onClose() } } else Modifier),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { sheetPx.toDp() })
                .graphicsLayer {
                    translationY = (1f - slide.value) * size.height
                    clip = true
                    shape = RoundedCornerShape(topStart = SheetTokens.TopRadius, topEnd = SheetTokens.TopRadius)
                }
                .background(palette.Dock)
                // Taps inside never reach the "close" behind.
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom)),
        ) {
            // The top drags the sheet down (1:1); past a quarter it closes.
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (slide.value < 1f - SheetTokens.COMMIT_FRACTION) onClose() else slide.animateTo(1f, Motion.Sheet)
                                }
                            },
                            onDragCancel = { scope.launch { slide.animateTo(1f, Motion.Sheet) } },
                        ) { change, dy ->
                            change.consume()
                            scope.launch { slide.snapTo((slide.value - dy / sheetPx).coerceIn(0f, 1f)) }
                        }
                    }
                    .padding(horizontal = SheetTokens.Padding),
            ) {
                Box(
                    Modifier
                        .padding(top = SheetTokens.HandleTop)
                        .align(Alignment.CenterHorizontally)
                        .size(BarTokens.HandleWidth, BarTokens.HandleHeight)
                        .clip(CircleShape)
                        .background(palette.DrawerHandle),
                )
                Text(
                    "History",
                    color = palette.TextPrimary,
                    fontSize = HistoryTokens.TitleSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = SheetTokens.Gap, bottom = SheetTokens.Gap),
                )
                SheetGroup {
                    SheetRow(KoanIcons.Trash, "Clear browsing data", open, titleColor = palette.Destructive, onClick = onClearData)
                }
            }

            val list = rows
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = SheetTokens.Padding, vertical = SheetTokens.Gap),
            ) {
                if (list != null && list.isEmpty()) {
                    item {
                        Text(
                            if (query.isBlank()) "Pages you visit show up here." else "Nothing in History matches “${query.trim()}”.",
                            color = palette.TextSecondary,
                            fontSize = SheetTokens.RowTrailingSize,
                            modifier = Modifier.padding(top = SheetTokens.Gap),
                        )
                    }
                }
                byDay(list.orEmpty()).forEach { (day, visits) ->
                    item(key = "day:$day") {
                        Text(
                            day,
                            color = palette.TextSecondary,
                            fontSize = HistoryTokens.DaySize,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = SheetTokens.RowPadding, top = SheetTokens.Gap * 1.5f, bottom = SheetTokens.Gap / 2),
                        )
                    }
                    items(visits, key = { it.url }) { v ->
                        val first = v === visits.first()
                        val last = v === visits.last()
                        HistoryRow(v, first, last, interactive = open, onOpen = onOpen, onRemove = onRemove)
                    }
                }
            }

            SearchRow(query, onQuery = { query = it })
        }
    }
}

/** One page: favicon, title, host and time; a desktop mark when it came from the account. */
@Composable
private fun HistoryRow(v: VisitEntity, first: Boolean, last: Boolean, interactive: Boolean, onOpen: (String) -> Unit, onRemove: (VisitEntity) -> Unit) {
    val palette = KoanTheme.colors
    val r = SheetTokens.GroupRadius
    val shape = RoundedCornerShape(
        topStart = if (first) r else HistoryTokens.Flat, topEnd = if (first) r else HistoryTokens.Flat,
        bottomStart = if (last) r else HistoryTokens.Flat, bottomEnd = if (last) r else HistoryTokens.Flat,
    )
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onRemove(v)
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = interactive,
        modifier = Modifier.clip(shape),
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.Destructive)
                    .padding(end = SheetTokens.Padding * 1.5f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(KoanIcons.Trash, "Remove", tint = palette.OnPrimary, modifier = Modifier.size(SheetTokens.RowGlyph))
            }
        },
    ) {
        Column(Modifier.background(palette.GroupedRow)) {
            if (!first) {
                Box(
                    Modifier
                        .padding(start = SheetTokens.RowPadding * 2 + HistoryTokens.Favicon)
                        .fillMaxWidth()
                        .height(SheetTokens.Divider)
                        .background(palette.CardBorder),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(SheetTokens.RowTallHeight)
                    .then(if (interactive) Modifier.clickable { onOpen(v.url) } else Modifier)
                    .padding(horizontal = SheetTokens.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Favicon(v.url, HistoryTokens.Favicon)
                Spacer(Modifier.width(SheetTokens.RowPadding))
                Column(Modifier.weight(1f)) {
                    Text(
                        v.title.ifBlank { displayHost(v.url) },
                        color = palette.TextPrimary,
                        fontSize = HistoryTokens.TitleRowSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${displayHost(v.url)} · ${TIME.format(Instant.ofEpochMilli(v.lastVisit).atZone(ZoneId.systemDefault()))}",
                        color = palette.TextSecondary,
                        fontSize = SheetTokens.RowSubtitleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (v.fromSync) {
                    Spacer(Modifier.width(SheetTokens.Gap))
                    Icon(KoanIcons.Desktop, "From your computer", tint = palette.TextSecondary, modifier = Modifier.size(HistoryTokens.SyncGlyph))
                }
            }
        }
    }
}

/** The search field at the bottom, like the address field's. */
@Composable
private fun SearchRow(query: String, onQuery: (String) -> Unit) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .padding(horizontal = SheetTokens.Padding, vertical = SheetTokens.Gap)
            .fillMaxWidth()
            .height(BarTokens.FieldHeight)
            .clip(CircleShape)
            .background(palette.BarField)
            .padding(horizontal = BarTokens.FieldPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(KoanIcons.Search, null, tint = palette.FieldHint, modifier = Modifier.size(SearchFieldTokens.Glyph))
        Spacer(Modifier.width(SearchFieldTokens.GlyphGap))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search history", color = palette.FieldHint, fontSize = BarTokens.FieldTextSize)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = palette.TextPrimary, fontSize = BarTokens.FieldTextSize),
                cursorBrush = SolidColor(palette.TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(Modifier.size(BarTokens.IconTouch).clip(CircleShape).clickable { onQuery("") }, contentAlignment = Alignment.Center) {
                Icon(KoanIcons.Close, "Clear", tint = palette.FieldHint, modifier = Modifier.size(SearchFieldTokens.Glyph))
            }
        }
    }
}

/** Newest first, by day: Today, Yesterday, then the date ("Monday 12 October", a year back on). */
internal fun byDay(visits: List<VisitEntity>, today: LocalDate = LocalDate.now()): List<Pair<String, List<VisitEntity>>> {
    val zone = ZoneId.systemDefault()
    return visits.groupBy { Instant.ofEpochMilli(it.lastVisit).atZone(zone).toLocalDate() }
        .toList()
        .sortedByDescending { it.first }
        .map { (date, list) -> dayLabel(date, today) to list }
}

internal fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> DAY.format(date)
    else -> DAY_YEAR.format(date)
}

private val DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val DAY_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
