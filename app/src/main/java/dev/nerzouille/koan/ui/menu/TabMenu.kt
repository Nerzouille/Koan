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
 * The tab counter's long-press menu (New tab, Close tab, Reopen closed tab: greyed out when
 * [onReopenClosed] is null), just above the dock: its right edge on the dock's right margin,
 * growing to the left with its labels. It grows out of the counter.
 */
@Composable
fun TabMenu(open: Boolean, onDismiss: () -> Unit, onNewTab: () -> Unit, onCloseTab: () -> Unit, onReopenClosed: (() -> Unit)?) {
    DockPopup(
        open = open,
        onDismiss = onDismiss,
        end = true,
        // The counter's centre from the screen's right edge: margin, ⋯, gap, half the counter.
        anchor = BarTokens.RowPadding + BarTokens.IconTouch + BarTokens.RowGap + BarTokens.IconTouch / 2,
        minWidth = TabMenuTokens.MinWidth,
    ) {
        MenuItem(KoanIcons.Plus, "New tab", open, onNewTab)
        MenuItem(KoanIcons.Close, "Close tab", open, onCloseTab)
        MenuItem(KoanIcons.History, "Reopen closed tab", open && onReopenClosed != null, onReopenClosed ?: {}, enabled = onReopenClosed != null)
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, interactive: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val palette = KoanTheme.colors
    DockPopupRow(interactive, onClick) {
        Icon(icon, null, tint = if (enabled) palette.Icon else palette.TextDisabled, modifier = Modifier.size(TabMenuTokens.Icon))
        Spacer(Modifier.width(TabMenuTokens.IconGap))
        Text(title, color = if (enabled) palette.TextPrimary else palette.TextDisabled, fontSize = TabMenuTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}
