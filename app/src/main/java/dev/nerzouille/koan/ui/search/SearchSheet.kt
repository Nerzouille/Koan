package dev.nerzouille.koan.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.search.Hit
import dev.nerzouille.koan.search.parseQuery
import dev.nerzouille.koan.search.Scope
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.Shortcut
import dev.nerzouille.koan.search.Source
import dev.nerzouille.koan.search.bareUrl
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SearchTokens

/** An open tab a result switches to: where it lives. */
@Immutable
class TabTarget(val spaceIndex: Int, val tabId: String, val spaceName: String, val spaceIcon: String?)

/** What the sheet does when something is picked. */
@Immutable
class SearchActions(
    /** A page: an open tab is switched to, anything else loads. */
    val open: (Hit) -> Unit,
    /** A search (a suggestion, "Search Wikipedia for…"): the url to load. */
    val search: (String) -> Unit,
    /** The "@" picker chose an engine or a scope: the field's new text ("@wiki "). */
    val pick: (String) -> Unit,
    val cancel: () -> Unit,
)

/**
 * The search sheet above the address field while typing (design 7a): the page dims behind it,
 * the results are laid out bottom-up from the field (the best match right above it, less relevant
 * groups further up), capped below the status bar (it scrolls inside). "@" shows the engines and
 * scopes instead (7d). A tap outside the results closes it. [bottom]: the dock's height above the
 * keyboard.
 */
@Composable
fun SearchSheet(
    field: SearchField,
    search: SearchState,
    engine: SearchEngine,
    bottom: Dp,
    currentSpace: Int,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    val palette = KoanTheme.colors
    val parsed = parseQuery(field.typed)
    val results = search.results
    val suggestions = search.suggestions
    val shortcut = parsed.shortcut
    val density = LocalDensity.current
    val screen = LocalWindowInfo.current.containerSize.height
    val statusTop = WindowInsets.statusBars.getTop(density)
    val cap = with(density) { (screen - statusTop - SearchTokens.StatusGap.toPx() - bottom.toPx() - SearchTokens.GroupGap.toPx()).toDp() }
    val pickable = parsed.picking && (
        Shortcut.entries.any { pickMatch(parsed.rest, it.key, it.label) } || Scope.entries.any { pickMatch(parsed.rest, it.key, it.label) }
        )
    val showing = pickable || (!parsed.picking && (!results.isEmpty || suggestions.isNotEmpty() || (shortcut != null && parsed.rest.isNotEmpty())))
    if (!showing) return
    Box(
        modifier
            .fillMaxSize()
            .drawBehind { drawRect(palette.SheetScrim) }
            .pointerInput(Unit) { detectTapGestures { actions.cancel() } },
    ) {
        if (pickable) {
            Picker(parsed.rest, engine, actions, Modifier.align(Alignment.BottomCenter).padding(bottom = bottom).heightIn(max = cap))
            return@Box
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottom + SearchTokens.GroupGap)
                .padding(horizontal = SearchTokens.Margin)
                .heightIn(max = cap)
                // Starts at its bottom (the best results), scrolls up to the rest.
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                // Taps between groups land here, not on the "close" behind.
                .pointerInput(Unit) { detectTapGestures { } },
            verticalArrangement = Arrangement.spacedBy(SearchTokens.GroupGap, Alignment.Bottom),
        ) {
            // Top to bottom = furthest to closest to the thumb: history and bookmarks, then open
            // tabs, then the engine's suggestions right on the field.
            if (results.pages.isNotEmpty()) {
                val label = when {
                    results.pages.all { it.source == Source.Bookmark } -> "Bookmarks"
                    results.pages.any { it.source == Source.Bookmark } -> "History & bookmarks"
                    else -> "History"
                }
                Group(label, KoanIcons.Reload) { results.pages.forEachIndexed { i, h -> if (i > 0) Divider(); HitRow(h, currentSpace, actions) } }
            }
            if (results.tabs.isNotEmpty()) Group("Switch to tab", KoanIcons.Essentials) { results.tabs.forEachIndexed { i, h -> if (i > 0) Divider(); HitRow(h, currentSpace, actions) } }
            if (suggestions.isNotEmpty()) {
                Group(search.suggestionsFrom, null) {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = SearchTokens.RowPadding, end = SearchTokens.RowPadding, bottom = SearchTokens.RowPadding),
                        horizontalArrangement = Arrangement.spacedBy(SearchTokens.ChipGap),
                    ) {
                        suggestions.forEach { s ->
                            Chip(s, KoanIcons.Search) {
                                actions.search(shortcut?.searchUrl(s) ?: engine.searchUrl(s))
                            }
                        }
                    }
                }
            }
            if (shortcut != null && parsed.rest.isNotEmpty()) {
                Group(null, null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(SearchTokens.RowHeight)
                            .clickable { actions.search(shortcut.searchUrl(parsed.rest)) }
                            .padding(horizontal = SearchTokens.RowPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Favicon(shortcut.home, SearchTokens.Favicon)
                        Spacer(Modifier.width(SearchTokens.RowPadding))
                        Text("Search ${shortcut.label} for “${parsed.rest}”", color = palette.TextPrimary, fontSize = SearchTokens.TitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(label: String?, icon: ImageVector?, content: @Composable ColumnScope.() -> Unit) {
    val palette = KoanTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SearchTokens.GroupRadius))
            .background(palette.Card),
    ) {
        if (label != null) {
            Row(Modifier.padding(start = SearchTokens.LabelPadding, top = SearchTokens.LabelPadding, bottom = SearchTokens.ChipGap / 2), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = palette.TextSecondary, modifier = Modifier.size(SearchTokens.ChipIcon))
                    Spacer(Modifier.width(SearchTokens.ChipGap / 2))
                }
                Text(label, color = palette.TextSecondary, fontSize = SearchTokens.LabelSize, fontWeight = FontWeight.Medium)
            }
        }
        content()
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(start = SearchTokens.RowPadding * 2 + SearchTokens.Favicon)
            .fillMaxWidth()
            .height(SearchTokens.ChipGap / 8)
            .background(KoanTheme.colors.CardBorder),
    )
}

/** A page: favicon, title and address; an open tab shows its space. */
@Composable
private fun HitRow(hit: Hit, currentSpace: Int, actions: SearchActions) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(SearchTokens.RowHeight)
            .clickable { actions.open(hit) }
            .padding(horizontal = SearchTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(hit.url, SearchTokens.Favicon)
        Spacer(Modifier.width(SearchTokens.RowPadding))
        Column(Modifier.weight(1f)) {
            Text(hit.title.ifBlank { displayHost(hit.url) }, color = palette.TextPrimary, fontSize = SearchTokens.TitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bareUrl(hit.url), color = palette.TextSecondary, fontSize = SearchTokens.UrlSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Its space only when it's another one than the space on screen.
        (hit.payload as? TabTarget)?.takeIf { it.spaceIndex != currentSpace }?.let { target ->
            Spacer(Modifier.width(SearchTokens.ChipGap))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(palette.GroupedRow)
                    .padding(horizontal = SearchTokens.ChipGap, vertical = SearchTokens.ChipGap / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!target.spaceIcon.isNullOrBlank()) {
                    ZenIcon(target.spaceIcon, SearchTokens.SpaceIcon, tint = palette.TextPrimary)
                    Spacer(Modifier.width(SearchTokens.ChipGap / 2))
                }
                Text(target.spaceName, color = palette.TextPrimary, fontSize = SearchTokens.UrlSize, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Chip(text: String, icon: ImageVector?, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .height(SearchTokens.ChipHeight)
            .clip(CircleShape)
            .background(palette.GroupedRow)
            .clickable(onClick = onClick)
            .padding(horizontal = SearchTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = palette.TextSecondary, modifier = Modifier.size(SearchTokens.ChipIcon))
            Spacer(Modifier.width(SearchTokens.ChipGap / 2))
        }
        Text(text, color = palette.TextPrimary, fontSize = SearchTokens.ChipTextSize, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** "@" typed (design 7d): the engines to search once, and where to search in Koan. */
@Composable
private fun Picker(typed: String, engine: SearchEngine, actions: SearchActions, modifier: Modifier) {
    val palette = KoanTheme.colors
    val engines = Shortcut.entries.filter { pickMatch(typed, it.key, it.label) }
    val scopes = Scope.entries.filter { pickMatch(typed, it.key, it.label) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = SearchTokens.PickerRadius, topEnd = SearchTokens.PickerRadius))
            .background(palette.Dock)
            // Taps inside the picker never reach the dim area's "close".
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(SearchTokens.PickerPadding),
        verticalArrangement = Arrangement.spacedBy(SearchTokens.ChipGap),
    ) {
        if (engines.isNotEmpty()) {
            Text("Search with", color = palette.TextSecondary, fontSize = SearchTokens.LabelSize, fontWeight = FontWeight.Medium)
            engines.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(SearchTokens.ChipGap)) {
                    pair.forEach { s ->
                        Row(
                            Modifier
                                .weight(1f)
                                .height(SearchTokens.TileHeight)
                                .clip(RoundedCornerShape(SearchTokens.GroupRadius))
                                .background(palette.GroupedRow)
                                .clickable { actions.pick("@${s.key} ") }
                                .padding(horizontal = SearchTokens.RowPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Favicon(s.home, SearchTokens.Favicon)
                            Spacer(Modifier.width(SearchTokens.RowPadding))
                            Column {
                                Text(s.label, color = palette.TextPrimary, fontSize = SearchTokens.TitleSize, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    "@${s.key}" + if (s.isDefault(engine)) " · default" else "",
                                    color = palette.TextSecondary, fontSize = SearchTokens.UrlSize, maxLines = 1,
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (scopes.isNotEmpty()) {
            Text("Search in", color = palette.TextSecondary, fontSize = SearchTokens.LabelSize, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = SearchTokens.ChipGap))
            Row(horizontalArrangement = Arrangement.spacedBy(SearchTokens.ChipGap)) {
                scopes.forEach { scope ->
                    Chip(scope.label, when (scope) { Scope.Tabs -> KoanIcons.Essentials; Scope.History -> KoanIcons.Reload; Scope.Bookmarks -> KoanIcons.Star }) {
                        actions.pick("@${scope.key} ")
                    }
                }
            }
        }
    }
}

/** "@wi" picks Wikipedia: what's typed after "@" starts its key or its name. */
private fun pickMatch(typed: String, key: String, label: String) =
    typed.isEmpty() || key.startsWith(typed, ignoreCase = true) || label.startsWith(typed, ignoreCase = true)
