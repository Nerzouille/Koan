package dev.nerzouille.koan.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.data.ClosedTabEntity
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.ui.kit.dayLabel
import java.time.LocalDate
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.ListPage
import dev.nerzouille.koan.ui.kit.LibraryRowFrame
import dev.nerzouille.koan.ui.kit.LibraryRowSubtitle
import dev.nerzouille.koan.ui.kit.LibraryRowTitle
import dev.nerzouille.koan.ui.kit.FaviconTile
import dev.nerzouille.koan.ui.kit.RowMenu
import dev.nerzouille.koan.ui.kit.SwipeToDelete
import dev.nerzouille.koan.ui.kit.byDay
import dev.nerzouille.koan.ui.kit.dayRowShape
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.settings.SettingsDivider
import dev.nerzouille.koan.ui.settings.SettingsSection
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.HistoryTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * History (the ⋯ menu, next to Downloads, laid out like it): the pages visited, the phone's and the
 * account's (desktop) mixed, one card per day, newest first. Back-to-back visits to one site make a
 * group row ("N pages · time range") that opens inline; a search shows the pages flat. Search and
 * Clear (Clear browsing data) at the bottom. The library rules (ui/kit/LibraryRow): a tap opens a
 * page ([onOpen]: in the current tab when History was opened from a page, else a new tab), a swipe
 * either way removes it, or a whole group, with Undo; a long press opens its menu ([onMenu]).
 *
 * On top, "Recently closed": the last tabs closed on the phone, newest first (the first few, all of
 * them once shown, or from the start when [allClosed]: the long press on the dock's History). A tap
 * puts one back in its space and place and opens it ([onRestore]).
 */
@Composable
fun HistoryScreen(
    dao: KoanDao,
    onOpen: (String) -> Unit,
    onRemove: (List<VisitEntity>) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onRestore: (String) -> Unit,
    /** A row's long-press menu ([HistoryMenus] builds it). */
    onMenu: (RowMenu) -> Unit,
    menus: HistoryMenus,
    modifier: Modifier = Modifier,
    allClosed: Boolean = false,
) {
    fun pageMenu(v: VisitEntity, all: List<VisitEntity>) = menus.page(v, all)
    fun closedMenu(c: ClosedTabEntity) = menus.closed(c)
    fun groupMenu(g: HistoryItem.Group) = menus.group(g.host, g.pages.first().visit.url, g.all)
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
    ListPage(
        title = "History",
        query = query,
        searchHint = "Search history",
        onQuery = { query = it },
        onClear = onClear,
        onClose = onClose,
        modifier = modifier,
    ) {
        val list = rows
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = SettingsTokens.Padding, end = SettingsTokens.Padding, bottom = SettingsTokens.SectionTop),
        ) {
            if (!searching && closed.isNotEmpty()) {
                val shown = if (showAllClosed) closed else closed.take(HistoryTokens.CLOSED_PREVIEW)
                val more = closed.size - shown.size
                item(key = "closed") { Box(Modifier.animateItem()) { SettingsSection("Recently closed") } }
                shown.forEachIndexed { i, c ->
                    item(key = "closed:${c.koanTabId}") {
                        Box(Modifier.animateItem().clip(dayRowShape(i == 0, i == shown.lastIndex && more == 0))) {
                            ClosedRow(c, first = i == 0, onRestore) { onMenu(closedMenu(it)) }
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
                    Text(
                        if (query.isBlank()) "Pages you visit show up here." else "Nothing in History matches “${query.trim()}”.",
                        color = palette.TextSecondary,
                        fontSize = SettingsTokens.RowValueSize,
                        modifier = Modifier.padding(top = SettingsTokens.SectionTop),
                    )
                }
            }
            byDay(list.orEmpty(), { it.lastVisit }).forEach { (day, visits) ->
                item(key = "day:$day") { Box(Modifier.animateItem()) { SettingsSection(day) } }
                val items = if (searching) visits.map(HistoryItem::Page) else groupVisits(visits)
                items.forEachIndexed { i, row ->
                    val first = i == 0
                    val last = i == items.lastIndex
                    when (row) {
                        is HistoryItem.Page -> item(key = row.key) {
                            Box(Modifier.animateItem()) {
                                SwipeToDelete(first, last, onRemove = { onRemove(row.all) }) {
                                    PageRow(row.visit, first, indent = false, onOpen) { onMenu(pageMenu(it, row.all)) }
                                }
                            }
                        }
                        is HistoryItem.Group -> {
                            val open = row.key in expanded
                            item(key = row.key) {
                                Box(Modifier.animateItem()) {
                                    SwipeToDelete(first, last && !open, onRemove = { onRemove(row.all) }) {
                                        GroupRow(row, first, open, onLong = { onMenu(groupMenu(row)) }) { expanded = if (open) expanded - row.key else expanded + row.key }
                                    }
                                }
                            }
                            if (open) {
                                row.pages.forEachIndexed { j, page ->
                                    item(key = page.key) {
                                        Box(Modifier.animateItem()) {
                                            SwipeToDelete(first = false, last = last && j == row.pages.lastIndex, onRemove = { onRemove(page.all) }) {
                                                PageRow(page.visit, first = false, indent = true, onOpen) { onMenu(pageMenu(it, page.all)) }
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
}

/** One page: favicon, title, host and time; a desktop mark when it came from the account. */
@Composable
private fun PageRow(v: VisitEntity, first: Boolean, indent: Boolean, onOpen: (String) -> Unit, onPageMenu: (VisitEntity) -> Unit) {
    val palette = KoanTheme.colors
    LibraryRowFrame(first, onClick = { onOpen(v.url) }, onLongClick = { onPageMenu(v) }, indent = indent) {
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
private fun ClosedRow(c: ClosedTabEntity, first: Boolean, onRestore: (String) -> Unit, onClosedMenu: (ClosedTabEntity) -> Unit) {
    val url = c.tab.url
    LibraryRowFrame(first, onClick = { onRestore(c.koanTabId) }, onLongClick = { onClosedMenu(c) }) {
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
private fun GroupRow(group: HistoryItem.Group, first: Boolean, open: Boolean, onLong: () -> Unit, onToggle: () -> Unit) {
    val palette = KoanTheme.colors
    val turn by animateFloatAsState(if (open) 90f else 0f, tween(HistoryTokens.CHEVRON_MS), label = "chevron")
    LibraryRowFrame(first, onClick = onToggle, onLongClick = onLong) {
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
