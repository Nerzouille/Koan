package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.settings.SettingsDivider
import dev.nerzouille.koan.ui.theme.DownloadTokens
import dev.nerzouille.koan.ui.theme.HistoryTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens

/*
 * One set of rules for the library pages (History, Downloads, Passwords): a tap does the thing
 * (opens the page, the file, the password); a swipe right to left deletes (Downloads: left to
 * right hides from the list) with Undo ([SwipeRow]); a long press enters selection mode
 * ([Selection]: check rows, act on them from the bar at the bottom). Same row, same motion.
 */

/**
 * A row's frame inside its card: the hairline above (but the first), tap and long press. [checked]
 * non-null: selection mode, the row shows its check mark (and a wash when checked).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryRowFrame(
    first: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    indent: Boolean = false,
    checked: Boolean? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val wash = selectionWash(checked == true)
    Column(
        Modifier
            .background(KoanTheme.colors.Card)
            .drawBehind { drawRect(wash.value) }
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
        ) {
            content()
            if (checked != null) {
                Spacer(Modifier.width(DownloadTokens.ActionGap))
                SelectionCheck(checked)
            }
        }
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
