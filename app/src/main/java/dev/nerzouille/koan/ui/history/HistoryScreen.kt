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
import dev.nerzouille.koan.data.KoanDao
import dev.nerzouille.koan.data.VisitEntity
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.ListPage
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
 * Clear (Clear browsing data) at the bottom. Tap opens a page in a new tab; a swipe left removes
 * it, or a whole group (a synced page is only hidden on the phone).
 */
@Composable
fun HistoryScreen(
    dao: KoanDao,
    onOpen: (String) -> Unit,
    onRemove: (List<VisitEntity>) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = KoanTheme.colors
    var query by remember { mutableStateOf("") }
    // The last list stays on screen while the next query runs.
    val rows by produceState<List<VisitEntity>?>(null, query) {
        dao.observeHistory(query.trim(), HistoryTokens.LIMIT).collect { value = it }
    }
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
                                SwipeToRemove(first, last, onRemove = { onRemove(row.all) }) {
                                    PageRow(row.visit, first, indent = false, onOpen)
                                }
                            }
                        }
                        is HistoryItem.Group -> {
                            val open = row.key in expanded
                            item(key = row.key) {
                                Box(Modifier.animateItem()) {
                                    SwipeToRemove(first, last && !open, onRemove = { onRemove(row.all) }) {
                                        GroupRow(row, first, open) { expanded = if (open) expanded - row.key else expanded + row.key }
                                    }
                                }
                            }
                            if (open) {
                                row.pages.forEachIndexed { j, page ->
                                    item(key = page.key) {
                                        Box(Modifier.animateItem()) {
                                            SwipeToRemove(first = false, last = last && j == row.pages.lastIndex, onRemove = { onRemove(page.all) }) {
                                                PageRow(page.visit, first = false, indent = true, onOpen)
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

/** A row of a day's card that a swipe left removes (the red bin shows under it). */
@Composable
private fun SwipeToRemove(first: Boolean, last: Boolean, onRemove: () -> Unit, content: @Composable () -> Unit) {
    val palette = KoanTheme.colors
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state) {
        // The list keeps a row's state by its key: a row back from Undo comes back swiped away.
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled)
        snapshotFlow { state.currentValue }.drop(1).collect { if (it == SwipeToDismissBoxValue.EndToStart) onRemove() }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.clip(dayRowShape(first, last)),
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.Destructive)
                    .padding(end = SettingsTokens.Padding),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(KoanIcons.Trash, "Remove", tint = palette.OnPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
            }
        },
    ) { content() }
}

/** One page: favicon, title, host and time; a desktop mark when it came from the account. */
@Composable
private fun PageRow(v: VisitEntity, first: Boolean, indent: Boolean, onOpen: (String) -> Unit) {
    val palette = KoanTheme.colors
    RowFrame(first, indent, onClick = { onOpen(v.url) }) {
        FaviconTile(v.url)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            RowTitle(v.title.ifBlank { displayHost(v.url) })
            RowSubtitle("${displayHost(v.url)} · ${time(v.lastVisit)}")
        }
        if (v.fromSync) {
            Spacer(Modifier.width(DownloadTokens.ActionGap))
            Icon(KoanIcons.Desktop, "From your computer", tint = palette.TextSecondary, modifier = Modifier.size(HistoryTokens.SyncGlyph))
        }
    }
}

/** Several pages of one site in a row: favicon, site, "N pages · 14:02–14:35", a chevron that turns as it opens. */
@Composable
private fun GroupRow(group: HistoryItem.Group, first: Boolean, open: Boolean, onToggle: () -> Unit) {
    val palette = KoanTheme.colors
    val turn by animateFloatAsState(if (open) 90f else 0f, tween(HistoryTokens.CHEVRON_MS), label = "chevron")
    RowFrame(first, indent = false, onClick = onToggle) {
        FaviconTile(group.pages.first().visit.url)
        Spacer(Modifier.width(DownloadTokens.RowGap))
        Column(Modifier.weight(1f)) {
            RowTitle(group.host)
            val from = time(group.oldest)
            val to = time(group.newest)
            RowSubtitle("${group.pages.size} pages · ${if (from == to) to else "$from–$to"}")
        }
        Spacer(Modifier.width(DownloadTokens.ActionGap))
        Icon(
            KoanIcons.ChevronRight, if (open) "Collapse" else "Expand",
            tint = palette.TextSecondary,
            modifier = Modifier.size(HistoryTokens.Chevron).graphicsLayer { rotationZ = turn },
        )
    }
}

@Composable
private fun RowFrame(first: Boolean, indent: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Column(
        Modifier
            .background(KoanTheme.colors.Card)
            .padding(horizontal = SettingsTokens.RowInset),
    ) {
        if (!first) SettingsDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = if (indent) HistoryTokens.GroupIndent else 0.dp)
                .padding(vertical = DownloadTokens.RowPaddingV),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun FaviconTile(url: String) {
    Box(
        Modifier
            .size(DownloadTokens.FileTile)
            .clip(RoundedCornerShape(DownloadTokens.FileTileRadius))
            .background(KoanTheme.colors.GroupedRow),
        contentAlignment = Alignment.Center,
    ) { Favicon(url, HistoryTokens.Favicon) }
}

@Composable
private fun RowTitle(text: String) {
    Text(
        text,
        color = KoanTheme.colors.TextPrimary,
        fontSize = SettingsTokens.RowTitleSize,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowSubtitle(text: String) {
    Text(text, color = KoanTheme.colors.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun time(at: Long) = TIME.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))

private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
