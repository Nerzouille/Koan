package dev.nerzouille.koan.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
 * account's (desktop) mixed, one card per day, newest first. Search and Clear (Clear browsing data)
 * at the bottom. Tap opens a page in a new tab; a swipe left removes it (a synced page is only
 * hidden on the phone).
 */
@Composable
fun HistoryScreen(
    dao: KoanDao,
    onOpen: (String) -> Unit,
    onRemove: (VisitEntity) -> Unit,
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
                item(key = "day:$day") { SettingsSection(day) }
                itemsIndexed(visits, key = { _, v -> v.url }) { i, v ->
                    HistoryRow(v, first = i == 0, last = i == visits.lastIndex, onOpen = onOpen, onRemove = onRemove)
                }
            }
        }
    }
}

/** One page: favicon, title, host and time; a desktop mark when it came from the account. */
@Composable
private fun HistoryRow(v: VisitEntity, first: Boolean, last: Boolean, onOpen: (String) -> Unit, onRemove: (VisitEntity) -> Unit) {
    val palette = KoanTheme.colors
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state) {
        // The list keeps a row's state by its key: a row back from Undo comes back swiped away.
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled)
        snapshotFlow { state.currentValue }.drop(1).collect { if (it == SwipeToDismissBoxValue.EndToStart) onRemove(v) }
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
    ) {
        Column(
            Modifier
                .background(palette.Card)
                .padding(horizontal = SettingsTokens.RowInset),
        ) {
            if (!first) SettingsDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(v.url) }
                    .padding(vertical = DownloadTokens.RowPaddingV),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(DownloadTokens.FileTile)
                        .clip(RoundedCornerShape(DownloadTokens.FileTileRadius))
                        .background(palette.GroupedRow),
                    contentAlignment = Alignment.Center,
                ) { Favicon(v.url, HistoryTokens.Favicon) }
                Spacer(Modifier.width(DownloadTokens.RowGap))
                Column(Modifier.weight(1f)) {
                    Text(
                        v.title.ifBlank { displayHost(v.url) },
                        color = palette.TextPrimary,
                        fontSize = SettingsTokens.RowTitleSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${displayHost(v.url)} · ${TIME.format(Instant.ofEpochMilli(v.lastVisit).atZone(ZoneId.systemDefault()))}",
                        color = palette.TextSecondary,
                        fontSize = SettingsTokens.RowSubtitleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (v.fromSync) {
                    Spacer(Modifier.width(DownloadTokens.ActionGap))
                    Icon(KoanIcons.Desktop, "From your computer", tint = palette.TextSecondary, modifier = Modifier.size(HistoryTokens.SyncGlyph))
                }
            }
        }
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
