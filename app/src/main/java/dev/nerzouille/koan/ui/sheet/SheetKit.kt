package dev.nerzouille.koan.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.KoanDivider
import dev.nerzouille.koan.ui.kit.KoanIconButton
import dev.nerzouille.koan.ui.kit.KoanToggle
import dev.nerzouille.koan.ui.kit.RowTexts
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SheetTokens

/*
 * The building blocks of a sheet's pages (design 7k / 7l): grouped rows, quick-action tiles and
 * the header of a pushed page. Rows only take taps while their page is the [SheetPageScope]'s
 * interactive one.
 */

/** Rows on one rounded grey surface, with hairlines between them. */
@Composable
fun SheetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SheetTokens.GroupRadius))
            .background(KoanTheme.colors.GroupedRow),
        content = content,
    )
}

/** Hairline between two rows of a [SheetGroup], aligned with the titles. */
@Composable
fun SheetDivider() = KoanDivider(inset = SheetTokens.RowPadding * 2 + SheetTokens.RowIconBox, thickness = SheetTokens.Divider)

/**
 * Icon box, title (and a quiet [subtitle]), then [trailing]. [onClick] null = not tappable; a row
 * that is not [enabled] yet (a feature still to come) is greyed and says so.
 */
@Composable
fun SheetRow(
    icon: ImageVector?,
    title: String,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(if (subtitle != null) SheetTokens.RowTallHeight else SheetTokens.RowHeight)
            .then(if (interactive && enabled && onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = SheetTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            MenuIconTile(icon, enabled, palette.Dock)
            Spacer(Modifier.width(SheetTokens.RowPadding))
        }
        RowTexts(title, subtitle, enabled, titleColor, SheetTokens.RowTitleSize, SheetTokens.RowSubtitleSize)
        if (!enabled) SoonLabel() else trailing()
    }
}

/**
 * The icon of every menu row (sheet menus and dock popups): the glyph on a small rounded tile.
 * [fill] is the neutral that differs from the surface under it: Dock on a grouped row, GroupedRow
 * on a dock popup.
 */
@Composable
fun MenuIconTile(icon: ImageVector, enabled: Boolean, fill: Color) {
    Box(
        Modifier
            .size(SheetTokens.RowIconBox)
            .clip(RoundedCornerShape(SheetTokens.RowIconRadius))
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) KoanTheme.colors.TextPrimary else KoanTheme.colors.TextDisabled, modifier = Modifier.size(SheetTokens.RowGlyph))
    }
}

/** A row's quiet trailing value ("Available", "1 active"). */
@Composable
fun RowValue(text: String) {
    Text(text, color = KoanTheme.colors.TextSecondary, fontSize = SheetTokens.RowTrailingSize, maxLines = 1)
}

/** Trailing chevron of a row that pushes a page. */
@Composable
fun RowChevron() {
    Icon(KoanIcons.ChevronRight, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(SheetTokens.RowGlyph))
}

/** A feature that arrives in a later update. */
@Composable
private fun SoonLabel() {
    Text("Soon", color = KoanTheme.colors.TextDisabled, fontSize = SheetTokens.RowTrailingSize)
}

/** One square-ish quick action: glyph over a short label. */
@Composable
fun RowScope.SheetTile(icon: ImageVector, label: String, interactive: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    val tint = if (enabled) palette.TextPrimary else palette.TextDisabled
    Column(
        Modifier
            .weight(1f)
            .height(SheetTokens.TileHeight)
            .clip(RoundedCornerShape(SheetTokens.TileRadius))
            .background(palette.GroupedRow)
            .then(if (interactive && enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(SheetTokens.TileGlyph))
        Spacer(Modifier.height(SheetTokens.TileLabelGap))
        Text(label, color = tint, fontSize = SheetTokens.TileLabelSize, maxLines = 1)
    }
}

/** Top of a pushed page: ‹ back and its title. */
@Composable
fun SheetHeader(title: String, page: SheetPageScope) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(SheetTokens.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KoanIconButton(KoanIcons.Back, page.onBack, SheetTokens.HeaderHeight, SheetTokens.TileGlyph, KoanTheme.colors.TextPrimary, interactive = page.interactive)
        Text(title, color = KoanTheme.colors.TextPrimary, style = KoanType.headline(SheetTokens.HeaderTitleSize))
    }
}

/** A small grey section title above a group. */
@Composable
fun SheetSectionTitle(text: String) {
    Text(
        text,
        color = KoanTheme.colors.TextSecondary,
        fontSize = SheetTokens.RowSubtitleSize,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = SheetTokens.RowPadding, top = SheetTokens.Gap / 2),
    )
}

/** A sheet row with a switch: the whole row toggles; inert while not [interactive]. */
@Composable
fun SheetToggleRow(
    icon: ImageVector?,
    title: String,
    interactive: Boolean,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    SheetRow(icon, title, interactive, subtitle = subtitle, onClick = { onChange(!checked) }) {
        KoanToggle(checked = checked, onCheckedChange = if (interactive) onChange else null)
    }
}
