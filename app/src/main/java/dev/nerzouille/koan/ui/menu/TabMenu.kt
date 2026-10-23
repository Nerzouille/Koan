package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.TabMenuTokens

/**
 * The tab counter's long-press menu (New tab, Close tab), just above the dock: its left edge
 * where it was when its right edge sat on the counter's, widened to the right (within the dock's
 * margin) so the labels have room. It grows out of the counter.
 */
@Composable
fun TabMenu(open: Boolean, onDismiss: () -> Unit, onNewTab: () -> Unit, onCloseTab: () -> Unit) {
    DockPopup(
        open = open,
        onDismiss = onDismiss,
        end = true,
        // The counter's right edge from the screen's: margin, ⋯, gap.
        edge = BarTokens.RowPadding + BarTokens.IconTouch + BarTokens.RowGap,
        anchor = BarTokens.RowPadding + BarTokens.IconTouch + BarTokens.RowGap + BarTokens.IconTouch / 2,
        minWidth = TabMenuTokens.MinWidth,
    ) {
        MenuItem(KoanIcons.Plus, "New tab", open, onNewTab)
        MenuItem(KoanIcons.Close, "Close tab", open, onCloseTab)
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, interactive: Boolean, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    DockPopupRow(interactive, onClick) {
        Icon(icon, null, tint = palette.Icon, modifier = Modifier.size(TabMenuTokens.Icon))
        Spacer(Modifier.width(TabMenuTokens.IconGap))
        Text(title, color = palette.TextPrimary, fontSize = TabMenuTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}
