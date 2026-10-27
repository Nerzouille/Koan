package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.topSafePadding

/*
 * Settings' building blocks (design 7n / 7o): rows straight on the base with hairlines, small
 * grey section titles, a segmented control, and the soft brand wash at the top of the screen.
 */

/** The soft pink → violet → blue glow behind the top of a settings page. */
fun Modifier.settingsWash(dark: Boolean): Modifier = drawBehind {
    val h = SettingsTokens.WashHeight.toPx()
    val a = if (dark) SettingsTokens.WASH_ALPHA_DARK else SettingsTokens.WASH_ALPHA_LIGHT
    val spots = listOf(
        BrandTokens.Pink to Offset(size.width * 0.1f, h * 0.15f),
        BrandTokens.Violet to Offset(size.width * 0.55f, h * 0.05f),
        BrandTokens.Blue to Offset(size.width * 0.95f, h * 0.3f),
    )
    for ((color, center) in spots) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), center, h * 0.75f), h * 0.75f, center)
    }
}

/**
 * One settings page: ‹ on top, an optional big [title], then [content], scrolling as a whole.
 * [interactive]: only the page on top takes taps.
 */
@Composable
fun SettingsPageFrame(title: String?, interactive: Boolean, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            // A form's fields stay above the keyboard.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .topSafePadding()
            .navigationBarsPadding()
            .padding(horizontal = SettingsTokens.Padding),
    ) {
        Box(
            Modifier
                .size(SettingsTokens.TopBar)
                .clip(CircleShape)
                .then(if (interactive) Modifier.clickable(onClick = onBack) else Modifier),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(KoanIcons.Back, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph + SettingsTokens.Divider * 4))
        }
        if (title != null) {
            Text(
                title,
                color = KoanTheme.colors.TextPrimary,
                fontSize = SettingsTokens.TitleSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = SettingsTokens.TitleBottom),
            )
        }
        content()
        Spacer(Modifier.height(SettingsTokens.SectionTop))
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        title,
        color = KoanTheme.colors.TextSecondary,
        fontSize = SettingsTokens.SectionTitleSize,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = SettingsTokens.SectionTop, bottom = SettingsTokens.Divider * 4),
    )
}

@Composable
fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SettingsTokens.Divider)
            .background(KoanTheme.colors.CardBorder),
    )
}

/**
 * Glyph, title (and [subtitle]), then a quiet [value] and a chevron when it opens something.
 * [enabled] false = a feature still to come ("Soon").
 */
@Composable
fun SettingsRow(
    icon: ImageVector?,
    title: String,
    interactive: Boolean,
    subtitle: String? = null,
    value: String? = null,
    chevron: Boolean = false,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .rowArea(if (interactive && enabled) onClick else null)
            .height(if (subtitle != null) SettingsTokens.RowTallHeight else SettingsTokens.RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) palette.TextPrimary else palette.TextDisabled, modifier = Modifier.size(SettingsTokens.RowGlyph))
            Spacer(Modifier.width(SettingsTokens.RowGlyphGap))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = when {
                    !enabled -> palette.TextDisabled
                    titleColor != Color.Unspecified -> titleColor
                    else -> palette.TextPrimary
                },
                fontSize = SettingsTokens.RowTitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, color = palette.TextSecondary, fontSize = SettingsTokens.RowSubtitleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val shown = if (!enabled) "Soon" else value
        if (shown != null) {
            Text(shown, color = if (enabled) palette.TextSecondary else palette.TextDisabled, fontSize = SettingsTokens.RowValueSize, maxLines = 1)
        }
        if (enabled) trailing()
        if (chevron && enabled) {
            Spacer(Modifier.width(SettingsTokens.Divider * 6))
            Icon(KoanIcons.ChevronRight, null, tint = palette.TextSecondary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
    }
}

/**
 * Every settings row's touch area: the full width of the row plus [SettingsTokens.RowInset] on
 * each side, rounded, so the pressed highlight always covers the whole row the same way. The
 * content stays aligned with the page's text and dividers.
 */
private fun Modifier.rowArea(onClick: (() -> Unit)?): Modifier =
    layout { measurable, constraints ->
        val inset = SettingsTokens.RowInset.roundToPx()
        val width = constraints.maxWidth + 2 * inset
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-inset, 0) }
    }
        .clip(RoundedCornerShape(SettingsTokens.RowRadius))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = SettingsTokens.RowInset)

/** A few mutually exclusive options side by side; the chosen one sits on a lighter pill, in bold. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, interactive: Boolean, onSelect: (T) -> Unit) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(SettingsTokens.SegmentHeight)
            .clip(RoundedCornerShape(SettingsTokens.SegmentRadius))
            .background(palette.Glass)
            .padding(SettingsTokens.SegmentInset),
        horizontalArrangement = Arrangement.spacedBy(SettingsTokens.SegmentInset),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val pill = RoundedCornerShape(SettingsTokens.SegmentRadius - SettingsTokens.SegmentInset)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .then(
                        if (on) {
                            Modifier
                                .shadow(SettingsTokens.SegmentShadow, pill)
                                .background(palette.SegmentPill, pill)
                        } else {
                            Modifier
                        },
                    )
                    .clip(pill)
                    .then(if (interactive) Modifier.clickable { onSelect(value) } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) palette.TextPrimary else palette.TextSecondary,
                    fontSize = SettingsTokens.SegmentTextSize,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}
