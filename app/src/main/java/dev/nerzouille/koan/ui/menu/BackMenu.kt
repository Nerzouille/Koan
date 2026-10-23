package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.TabMenuTokens
import mozilla.components.browser.state.state.content.HistoryState

/**
 * Long press on back: the pages before this one in the tab, most recent first (favicon, title),
 * attached to the dock's left end and growing out of back. A tap goes back to that page.
 */
@Composable
fun BackMenu(open: Boolean, history: HistoryState?, onDismiss: () -> Unit, onGoTo: (index: Int) -> Unit) {
    // What it showed stays while it closes, as the page moves on.
    var shown by remember { mutableStateOf(history) }
    if (open && history != null) shown = history
    val h = shown ?: return
    val entries = h.items.withIndex().take(h.currentIndex).reversed()
    DockPopup(
        open = open && entries.isNotEmpty(),
        onDismiss = onDismiss,
        end = false,
        edge = BarTokens.RowPadding,
        anchor = BarTokens.RowPadding + BarTokens.IconTouch / 2,
        modifier = Modifier.widthIn(max = TabMenuTokens.MaxWidth),
    ) {
        LazyColumn(Modifier.heightIn(max = TabMenuTokens.RowHeight * TabMenuTokens.MAX_ROWS + TabMenuTokens.RowHeight / 2)) {
            items(entries, key = { it.index }) { (index, item) ->
                DockPopupRow(open, onClick = { onGoTo(index) }) {
                    Favicon(item.uri, TabMenuTokens.Icon)
                    Spacer(Modifier.width(TabMenuTokens.IconGap))
                    Text(
                        item.title.ifBlank { displayHost(item.uri).ifEmpty { item.uri } },
                        color = KoanTheme.colors.TextPrimary,
                        fontSize = TabMenuTokens.TextSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
