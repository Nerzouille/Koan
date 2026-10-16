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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.bar.PageInfo
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.sheet.RowChevron
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetPageScope
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.sheet.SheetTile
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens

/** The pages of the ⋯ menu sheet. */
sealed interface MenuPage {
    data object Root : MenuPage
    data object Settings : MenuPage
}

/** What the menu's root page does. Rows without an action yet show as "Soon". */
@Immutable
class MenuActions(
    val back: () -> Unit,
    val forward: () -> Unit,
    val reload: () -> Unit,
    val stop: () -> Unit,
    val editAddress: () -> Unit,
    val copyLink: () -> Unit,
    val newTab: () -> Unit,
    val desktopSite: (Boolean) -> Unit,
    val settings: () -> Unit,
)

/**
 * The menu's root page (design 7k compact / 7l full): the page's navigation row, quick actions,
 * the page rows, and the content blocker's card at the bottom (peeking in compact).
 */
@Composable
fun MenuRoot(page: PageInfo, actions: MenuActions, scope: SheetPageScope) {
    val on = scope.interactive
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SheetTokens.Padding)
            .padding(bottom = SheetTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap),
    ) {
        NavRow(page, actions, on)
        Row(horizontalArrangement = Arrangement.spacedBy(SheetTokens.TileGap)) {
            SheetTile(KoanIcons.Plus, "New tab", on, onClick = actions.newTab)
            SheetTile(KoanIcons.Star, "Bookmark", on, enabled = false) {}
            SheetTile(KoanIcons.Share, "Share", on, enabled = false) {}
            SheetTile(KoanIcons.Search, "Find", on, enabled = false) {}
        }
        SheetGroup {
            SheetRow(KoanIcons.Lock, "Site settings", on, enabled = false)
            SheetDivider()
            SheetRow(KoanIcons.Desktop, "Desktop site", on, onClick = { actions.desktopSite(!page.desktopSite) }) {
                KoanToggle(checked = page.desktopSite, onCheckedChange = if (on) actions.desktopSite else null)
            }
            SheetDivider()
            SheetRow(KoanIcons.Reader, "Reader view", on, enabled = false)
            SheetDivider()
            SheetRow(KoanIcons.Essentials, "Add to Essentials", on, enabled = false)
            SheetDivider()
            SheetRow(KoanIcons.Download, "Downloads", on, enabled = false)
            SheetDivider()
            SheetRow(KoanIcons.Settings, "Settings", on, onClick = actions.settings) { RowChevron() }
        }
        SheetGroup {
            SheetRow(KoanIcons.Shield, "uBlock Origin", on, subtitle = "Blocks ads and trackers", enabled = false)
        }
    }
}

/** back · forward · the page's address · copy link · reload/stop. Tap the address to edit it. */
@Composable
private fun NavRow(page: PageInfo, actions: MenuActions, on: Boolean) {
    val palette = KoanTheme.colors
    val host = displayHost(page.url)
    Row(
        Modifier
            .fillMaxWidth()
            .height(SheetTokens.NavRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavIcon(KoanIcons.Back, page.canGoBack, on, actions.back)
        NavIcon(KoanIcons.Forward, page.canGoForward, on, actions.forward)
        Row(
            Modifier
                .weight(1f)
                .height(SheetTokens.NavFieldHeight)
                .clip(CircleShape)
                .background(palette.BarField)
                .then(if (on) Modifier.clickable(onClick = actions.editAddress) else Modifier)
                .padding(horizontal = SheetTokens.RowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (page.url.startsWith("https://")) {
                Icon(KoanIcons.Lock, null, tint = palette.TextSecondary, modifier = Modifier.size(SheetTokens.NavLockGlyph))
                Spacer(Modifier.width(SheetTokens.TileLabelGap * 2))
            }
            Text(
                host.ifEmpty { "New tab" },
                color = if (host.isEmpty()) palette.FieldHint else palette.TextPrimary,
                fontSize = SheetTokens.NavFieldTextSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        NavIcon(KoanIcons.Link, page.url.isNotEmpty(), on, actions.copyLink)
        if (page.loading) {
            NavIcon(KoanIcons.Close, true, on, actions.stop)
        } else {
            NavIcon(KoanIcons.Reload, page.url.isNotEmpty(), on, actions.reload)
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, enabled: Boolean, interactive: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(BarTokens.IconTouch)
            .clip(CircleShape)
            .then(if (enabled && interactive) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) KoanTheme.colors.Icon else KoanTheme.colors.TextDisabled, modifier = Modifier.size(BarTokens.Icon))
    }
}
