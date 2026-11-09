package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.settings.SettingsDivider
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.HistoryTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.theme.SheetTokens
import kotlinx.coroutines.flow.drop

/*
 * One set of rules for the library pages (History, Downloads, Passwords): a tap does the thing
 * (opens the page, the file, the password), a swipe either way deletes (with Undo), a long press
 * opens the row's action menu ([RowMenu], on a floating card). Same row, same card, same motion.
 */

/** One action of a row's long-press menu. */
@Immutable
class RowMenuItem(val icon: ImageVector, val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** A row's long-press menu: what was pressed on top (favicon of [iconUrl], or a file glyph), then [groups] of actions. */
@Immutable
class RowMenu(val title: String, val subtitle: String, val iconUrl: String?, val groups: List<List<RowMenuItem>>)

/** The long-press menu's content on its card. [onPick] runs before the item's own action (closes the card). */
@Composable
fun RowMenuContent(menu: RowMenu, interactive: Boolean, onPick: () -> Unit) {
    val palette = KoanTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        Row(Modifier.padding(horizontal = SheetTokens.RowPadding / 2), verticalAlignment = Alignment.CenterVertically) {
            val url = menu.iconUrl
            if (url != null) {
                Favicon(url, SheetTokens.RowIconBox)
            } else {
                Icon(KoanIcons.File, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.RowIconBox))
            }
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column {
                Text(menu.title, color = palette.TextPrimary, fontSize = SheetTokens.RowTitleSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (menu.subtitle.isNotEmpty()) {
                    Text(menu.subtitle, color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        menu.groups.filter { it.isNotEmpty() }.forEach { group ->
            SheetGroup {
                group.forEachIndexed { i, item ->
                    if (i > 0) SheetDivider()
                    SheetRow(item.icon, item.label, interactive, titleColor = if (item.destructive) palette.Destructive else androidx.compose.ui.graphics.Color.Unspecified, onClick = {
                        onPick()
                        item.onClick()
                    })
                }
            }
        }
    }
}

/**
 * A row of a library card that a swipe either way deletes: the red bin shows under it on the side
 * it uncovers. [first]/[last]: the card's rounded ends. [onRemove] null: the row can't be swiped.
 */
@Composable
fun SwipeToDelete(first: Boolean, last: Boolean, onRemove: (() -> Unit)?, content: @Composable () -> Unit) {
    if (onRemove == null) {
        Box(Modifier.clip(dayRowShape(first, last))) { content() }
        return
    }
    val palette = KoanTheme.colors
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state) {
        // The list keeps a row's state by its key: a row back from Undo comes back swiped away.
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled)
        snapshotFlow { state.currentValue }.drop(1).collect { if (it != SwipeToDismissBoxValue.Settled) onRemove() }
    }
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.clip(dayRowShape(first, last)),
        backgroundContent = {
            val start = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.Destructive)
                    .padding(horizontal = SettingsTokens.Padding),
                contentAlignment = if (start) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(KoanIcons.Trash, "Delete", tint = palette.OnPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
            }
        },
    ) { content() }
}

/** A row's frame inside its card: the hairline above (but the first), tap and long press. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryRowFrame(
    first: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    indent: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .background(KoanTheme.colors.Card)
            .padding(horizontal = SettingsTokens.RowInset),
    ) {
        if (!first) SettingsDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick?.let { long ->
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            long()
                        }
                    },
                )
                .padding(start = if (indent) HistoryTokens.GroupIndent else 0.dp)
                .padding(vertical = DownloadTokens.RowPaddingV),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The site's icon on its small rounded tile, at a row's start. */
@Composable
fun FaviconTile(url: String) {
    Box(
        Modifier
            .size(DownloadTokens.FileTile)
            .clip(RoundedCornerShape(DownloadTokens.FileTileRadius))
            .background(KoanTheme.colors.GroupedRow),
        contentAlignment = Alignment.Center,
    ) { Favicon(url, HistoryTokens.Favicon) }
}

@Composable
fun LibraryRowTitle(text: String) {
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
fun LibraryRowSubtitle(text: String) {
    Text(text, color = KoanTheme.colors.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
