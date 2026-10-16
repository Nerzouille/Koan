package dev.nerzouille.koan.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.engine.SiteDecision
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetPageScope
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.sheet.SheetSectionTitle
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/** What Site settings shows and does for the page. [decisions] null = still loading. */
class SiteSettingsUi(
    val url: String,
    val secure: Boolean,
    val decisions: List<SiteDecision>?,
    val onSet: (SiteDecision, Boolean) -> Unit,
    val readerAvailable: Boolean,
    val readerActive: Boolean,
    val onReader: () -> Unit,
    val onClearData: () -> Unit,
)

/**
 * Site settings (design 7m), pushed inside the menu sheet: the site and its connection, what it
 * may do (toggles flip allowed ↔ blocked), reader view, and clearing its cookies and data.
 */
@Composable
fun SiteSettingsPage(ui: SiteSettingsUi, scope: SheetPageScope) {
    val on = scope.interactive
    val palette = KoanTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SheetTokens.Padding)
            .padding(bottom = SheetTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap),
    ) {
        Row(Modifier.fillMaxWidth().height(SheetTokens.HeaderHeight + SheetTokens.Gap), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(SheetTokens.HeaderHeight)
                    .clip(CircleShape)
                    .then(if (on) Modifier.clickable(onClick = scope.onBack) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Back, null, tint = palette.TextPrimary, modifier = Modifier.size(SheetTokens.TileGlyph))
            }
            Favicon(ui.url, SheetTokens.RowIconBox + SheetTokens.Gap)
            Spacer(Modifier.width(SheetTokens.RowPadding))
            Column(Modifier.weight(1f)) {
                Text(displayHost(ui.url), color = palette.TextPrimary, fontSize = SheetTokens.HeaderTitleSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(KoanIcons.Lock, null, tint = if (ui.secure) palette.TextSecondary else palette.Attention, modifier = Modifier.size(SheetTokens.NavLockGlyph))
                    Spacer(Modifier.width(SheetTokens.Divider * 4))
                    Text(
                        if (ui.secure) "Connection is secure" else "Connection is not secure",
                        color = if (ui.secure) palette.TextSecondary else palette.Attention,
                        fontSize = SheetTokens.RowSubtitleSize,
                    )
                }
            }
        }

        SheetSectionTitle("Permissions")
        SheetGroup {
            val decisions = ui.decisions
            when {
                decisions == null -> SheetRow(null, "…", on)
                decisions.isEmpty() -> SheetRow(null, "Nothing allowed or blocked yet", on, subtitle = "Koan asks the first time a site wants something")
                else -> decisions.forEachIndexed { i, d ->
                    if (i > 0) SheetDivider()
                    SheetRow(d.ask.icon(), d.ask.label, on, subtitle = if (d.allowed) "Allowed" else "Blocked", onClick = { ui.onSet(d, !d.allowed) }) {
                        KoanToggle(checked = d.allowed, onCheckedChange = if (on) { allow -> ui.onSet(d, allow) } else null)
                    }
                }
            }
        }

        SheetGroup {
            SheetRow(
                KoanIcons.Reader, "Reader view", on,
                subtitle = if (ui.readerAvailable || ui.readerActive) "Simplified, distraction-free view" else "Not available on this page",
                onClick = if (ui.readerAvailable || ui.readerActive) ui.onReader else null,
            ) {
                if (ui.readerAvailable || ui.readerActive) KoanToggle(checked = ui.readerActive, onCheckedChange = if (on) { _ -> ui.onReader() } else null)
            }
        }

        Text(
            "Cookies and site data",
            color = palette.TextSecondary,
            fontSize = SheetTokens.RowSubtitleSize,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = SheetTokens.Gap),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(SheetTokens.RowHeight)
                .clip(CircleShape)
                .background(palette.GroupedRow)
                .then(if (on) Modifier.clickable(onClick = ui.onClearData) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("Clear site data", color = palette.Destructive, fontSize = SheetTokens.RowTitleSize, fontWeight = FontWeight.Medium)
        }
    }
}
