package dev.nerzouille.koan.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.data.ClosedTabEntity
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.EmptyLine
import dev.nerzouille.koan.ui.kit.FaviconTile
import dev.nerzouille.koan.ui.kit.LibraryRowFrame
import dev.nerzouille.koan.ui.kit.LibraryRowSubtitle
import dev.nerzouille.koan.ui.kit.LibraryRowTitle
import dev.nerzouille.koan.ui.kit.ListPage
import dev.nerzouille.koan.ui.kit.SelectionBar
import dev.nerzouille.koan.ui.kit.SelectionDayHeader
import dev.nerzouille.koan.ui.kit.SelectionHeader
import dev.nerzouille.koan.ui.kit.SwipeRow
import dev.nerzouille.koan.ui.kit.byDay
import dev.nerzouille.koan.ui.kit.dayLabel
import dev.nerzouille.koan.ui.kit.dayRowShape
import dev.nerzouille.koan.ui.kit.rememberSelection
import dev.nerzouille.koan.ui.settings.SettingsSection
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.HistoryTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * History (the ⋯ menu, next to Downloads, laid out like it): the pages visited, the phone's and the
 * account's (desktop) mixed, one card per day, newest first. Back-to-back visits to one site make a
 * group row ("N pages · time range") that opens inline; a search shows the pages flat. Search and
 * Clear (Clear browsing data) at the bottom. The library rules (ui/kit/LibraryRow): a tap opens a
 * page ([onOpen]: in the current tab when History was opened from a page, else a new tab), a swipe
 * right to left removes it, or a whole group, with Undo; a long press enters selection mode
 * ([actions] for what is checked).
 *
 * On top, "Recently closed": the last tabs closed on the phone, newest first (the first few, all of
 * them once shown, or from the start when [allClosed]: the long press on the dock's History). A tap
 * puts one back in its space and place and opens it ([onRestore]). One closed tab is checked at a
 * time, never with pages.
 */
@Composable
fun HistoryScreen(
    dao: KoanDao,
    onOpen: (String) -> Unit,
    onRemove: (List<VisitEntity>) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onRestore: (String) -> Unit,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
    allClosed: Boolean = false,
) {
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    // The last list stays on screen while the next query runs.
    val rows by produceState<List<VisitEntity>?>(null, query) {
        dao.observeHistory(query.trim(), HistoryTokens.LIMIT).collect { value = it }
    }
    val closed by dao.observeClosedTabs().collectAsStateWithLifecycle(emptyList())
    var showAllClosed by remember(allClosed) { mutableStateOf(allClosed) }
    var expanded by remember { mutableStateOf(emptySet<String>()) }
    val searching = query.isNotBlank()
    val selection = rememberSelection<String>()

    val list = rows
    val shownClosed = if (searching) emptyList() else if (showAllClosed) closed else closed.take(HistoryTokens.CLOSED_PREVIEW)
    val days = byDay(list.orEmpty(), { it.lastVisit }).map { (day, visits) ->
        day to (if (searching) visits.map(HistoryItem::Page) else groupVisits(visits))
    }
    // Every row that can be checked now, by key (a group's pages only while it is open).
    val checkable = LinkedHashMap<String, Checkable>()
    // The same keys per day, for the day header's "Select all".
    val dayKeys = HashMap<String, List<String>>()
    shownClosed.forEach { c -> "closed:${c.koanTabId}".let { checkable[it] = Checkable.Closed(it, c) } }
    days.forEach { (day, items) ->
        val keys = ArrayList<String>()
        items.forEach { row ->
            when (row) {
                is HistoryItem.Page -> checkable[row.key] = Checkable.Page(row.key, row.visit, row.all).also { keys += row.key }
                is HistoryItem.Group -> {
                    checkable[row.key] = Checkable.Group(row.key, row.all)
                    keys += row.key
                    if (row.key in expanded) row.pages.forEach { checkable[it.key] = Checkable.Page(it.key, it.visit, it.all); keys += it.key }
                }
            }
        }
        dayKeys[day] = keys
    }
    SideEffect { selection.keepOnly(checkable.keys) }
    val checked = selection.keys.mapNotNull { checkable[it] }
    val closedMode = checked.any { it is Checkable.Closed }

    /** In selection mode, the row's check mark; null for rows that can't join what is checked. */
    fun checkOf(key: String, isClosed: Boolean): Boolean? =
        if (!selection.active || isClosed != closedMode) null else key in selection

    fun press(key: String, isClosed: Boolean) {
        when {
            !selection.active -> selection.start(key)
            isClosed != closedMode -> Unit
            // One closed tab at a time: the check moves.
            isClosed -> selection.start(key)
            else -> selection.toggle(key)
        }
    }

    ListPage(
        title = "History",
        query = query,
        searchHint = "Search history",
        onQuery = { query = it },
        onClear = onClear,
        onClose = onClose,
        modifier = modifier,
        selectionHeader = if (!selection.active) null else {
            {
                SelectionHeader(
                    checked.size, interactive = true, onCancel = selection::clear,
                    onSelectAll = if (closedMode) null else { { selection.set(checkable.values.filter { it !is Checkable.Closed }.map { it.key }.toSet()) } },
                )
            }
        },
        selectionBar = if (!selection.active) null else {
            { SelectionBar(actions.forChecked(checked, done = selection::clear), interactive = true) }
        },
    ) {
        BackHandler(enabled = selection.active, onBack = selection::clear)
        val selecting = selection.active
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = SettingsTokens.Padding, end = SettingsTokens.Padding, bottom = SettingsTokens.SectionTop),
        ) {
            if (shownClosed.isNotEmpty()) {
                val more = closed.size - shownClosed.size
                item(key = "closed") { Box(Modifier.animateItem()) { SettingsSection("Recently closed") } }
                shownClosed.forEachIndexed { i, c ->
                    val key = "closed:${c.koanTabId}"
                    item(key = key) {
                        Box(Modifier.animateItem().clip(dayRowShape(i == 0, i == shownClosed.lastIndex && more == 0))) {
                            ClosedRow(
                                c, first = i == 0, checked = checkOf(key, true),
                                onTap = { if (selecting) press(key, true) else onRestore(c.koanTabId) },
                                onLong = { press(key, true) },
                            )
                        }
                    }
                }
                if (more > 0) {
                    item(key = "closed:more") {
                        Box(Modifier.animateItem().clip(dayRowShape(first = false, last = true))) {
                            LibraryRowFrame(first = false, onClick = { showAllClosed = true }) {
                                Text("Show all $more more", color = palette.TextPrimary, fontSize = SettingsTokens.RowTitleSize, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            if (list != null && list.isEmpty()) {
                item {
                    EmptyLine(
                        if (query.isBlank()) "Pages you visit show up here." else "Nothing in History matches “${query.trim()}”.",
                        Modifier.padding(top = SettingsTokens.SectionTop),
                    )
                }
            }
            days.forEach { (day, items) ->
                item(key = "day:$day") {
                    Box(Modifier.animateItem()) {
                        if (selecting) SelectionDayHeader(day, if (closedMode) emptyList() else dayKeys[day].orEmpty(), selection) else SettingsSection(day)
                    }
                }
                items.forEachIndexed { i, row ->
                    val first = i == 0
                    val last = i == items.lastIndex
                    when (row) {
                        is HistoryItem.Page -> item(key = row.key) {
                            SwipeRow(onDelete = { onRemove(row.all) }, Modifier.animateItem(), enabled = !selecting, shape = dayRowShape(first, last)) {
                                PageRow(
                                    row.visit, first, indent = false, checked = checkOf(row.key, false),
                                    onTap = { if (selecting) press(row.key, false) else onOpen(row.visit.url) },
                                    onLong = { press(row.key, false) },
                                )
                            }
                        }
                        is HistoryItem.Group -> {
                            val open = row.key in expanded
                            item(key = row.key) {
                                SwipeRow(onDelete = { onRemove(row.all) }, Modifier.animateItem(), enabled = !selecting, shape = dayRowShape(first, last && !open)) {
                                    GroupRow(
                                        row, first, open, checked = checkOf(row.key, false),
                                        onLong = { press(row.key, false) },
                                    ) {
                                        if (selecting) press(row.key, false) else expanded = if (open) expanded - row.key else expanded + row.key
                                    }
                                }
                            }
                            if (open) {
                                row.pages.forEachIndexed { j, page ->
                                    item(key = page.key) {
                                        val lastPage = last && j == row.pages.lastIndex
                                        SwipeRow(onDelete = { onRemove(page.all) }, Modifier.animateItem(), enabled = !selecting, shape = dayRowShape(false, lastPage)) {
                                            PageRow(
                                                page.visit, first = false, indent = true, checked = checkOf(page.key, false),
                                                onTap = { if (selecting) press(page.key, false) else onOpen(page.visit.url) },
                                                onLong = { press(page.key, false) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One page: favicon, title, host and time; a desktop mark when it came from the account. */
@Composable
private fun PageRow(v: VisitEntity, first: Boolean, indent: Boolean, checked: Boolean?, onTap: () -> Unit, onLong: () -> Unit) {
    val palette = KoanTheme.colors
    LibraryRowFrame(first, onClick = onTap, onLongClick = onLong, indent = indent, checked = checked) {
        FaviconTile(v.url)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            LibraryRowTitle(v.title.ifBlank { displayHost(v.url) })
            LibraryRowSubtitle("${displayHost(v.url)} · ${time(v.lastVisit)}")
        }
        if (v.fromSync) {
            Spacer(Modifier.width(DownloadTokens.ActionGap))
            Icon(KoanIcons.Desktop, "From your computer", tint = palette.TextSecondary, modifier = Modifier.size(HistoryTokens.SyncGlyph))
        }
    }
}

/** A closed tab: favicon, title, host and when it was closed. */
@Composable
private fun ClosedRow(c: ClosedTabEntity, first: Boolean, checked: Boolean?, onTap: () -> Unit, onLong: () -> Unit) {
    val url = c.tab.url
    LibraryRowFrame(first, onClick = onTap, onLongClick = onLong, checked = checked) {
        FaviconTile(url)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            LibraryRowTitle(c.tab.title.ifBlank { displayHost(url) })
            val day = dayLabel(Instant.ofEpochMilli(c.closedAt).atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now())
            LibraryRowSubtitle("${displayHost(url)} · ${if (day == "Today") time(c.closedAt) else day}")
        }
    }
}

/** Several pages of one site in a row: favicon, site, "N pages · 14:02–14:35", a chevron that turns as it opens. */
@Composable
private fun GroupRow(group: HistoryItem.Group, first: Boolean, open: Boolean, checked: Boolean?, onLong: () -> Unit, onToggle: () -> Unit) {
    val palette = KoanTheme.colors
    val turn by animateFloatAsState(if (open) 90f else 0f, tween(HistoryTokens.CHEVRON_MS), label = "chevron")
    LibraryRowFrame(first, onClick = onToggle, onLongClick = onLong, checked = checked) {
        FaviconTile(group.pages.first().visit.url)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            LibraryRowTitle(group.host)
            val from = time(group.oldest)
            val to = time(group.newest)
            LibraryRowSubtitle("${group.pages.size} pages · ${if (from == to) to else "$from–$to"}")
        }
        Spacer(Modifier.width(DownloadTokens.ActionGap))
        Icon(
            KoanIcons.ChevronRight, if (open) "Collapse" else "Expand",
            tint = palette.TextSecondary,
            modifier = Modifier.size(HistoryTokens.Chevron).graphicsLayer { rotationZ = turn },
        )
    }
}

private fun time(at: Long) = TIME.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))

private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
