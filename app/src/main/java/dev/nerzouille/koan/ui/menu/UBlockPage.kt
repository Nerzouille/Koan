package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetHeader
import dev.nerzouille.koan.ui.sheet.SheetPageScope
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.concept.engine.EngineSession

/** uBlock Origin on the current page, for the menu's card. Null = not installed. */
class UBlockUi(val enabled: Boolean, val blocked: Int)

/**
 * The menu's uBlock card (design 7k / 7l): how many requests it blocked on this page, on/off for
 * every site; tap = its own panel, pushed in the sheet.
 */
@Composable
fun UBlockCard(ui: UBlockUi?, interactive: Boolean, onToggle: (Boolean) -> Unit, onOpen: () -> Unit, onInstall: () -> Unit) {
    SheetGroup {
        when {
            ui == null -> SheetRow(KoanIcons.Shield, "uBlock Origin", interactive, subtitle = "Blocks ads and trackers · Install", onClick = onInstall)
            else -> SheetRow(
                KoanIcons.Shield, "uBlock Origin", interactive,
                subtitle = when {
                    !ui.enabled -> "Off"
                    ui.blocked == 1 -> "1 request blocked on this page"
                    else -> "${ui.blocked} requests blocked on this page"
                },
                onClick = if (ui.enabled) onOpen else null,
            ) { KoanToggle(checked = ui.enabled, onCheckedChange = if (interactive) onToggle else null) }
        }
    }
}

/**
 * uBlock's own panel (its per-site switch and what it blocked), pushed inside the menu sheet. The
 * panel is a page of the add-on, shown in its own engine view; it closes with the page.
 */
@Composable
fun UBlockPage(popup: EngineSession?, scope: SheetPageScope, onClosed: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SheetTokens.Padding)
            .padding(bottom = SheetTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap),
    ) {
        SheetHeader("uBlock Origin", scope)
        Box(
            Modifier
                .fillMaxWidth()
                .height(SheetTokens.UBlockPanelHeight)
                .clip(RoundedCornerShape(SheetTokens.GroupRadius))
                .background(KoanTheme.colors.GroupedRow),
            contentAlignment = Alignment.Center,
        ) {
            if (popup == null) {
                Text("…", color = KoanTheme.colors.TextSecondary)
            } else {
                val view = remember { GeckoEngineView(context) }
                AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().height(SheetTokens.UBlockPanelHeight)) { it.render(popup) }
                DisposableEffect(popup) { onDispose { view.release() } }
            }
        }
    }
    DisposableEffect(Unit) { onDispose(onClosed) }
}
