package dev.nerzouille.koan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import dev.nerzouille.koan.ui.kit.SELECTION_FADE_MS
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import dev.nerzouille.koan.ui.kit.CollapsingBar
import dev.nerzouille.koan.ui.theme.KoanType
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
 * One settings page (docs/UI_SPEC.md "Page header"): the page bar on top, a large [title] under it
 * that collapses into the bar as [content] scrolls (iOS style), then the content.
 * [interactive]: only the page on top takes taps.
 */
@Composable
fun SettingsPageFrame(
    title: String,
    interactive: Boolean,
    onBack: () -> Unit,
    /** Selection mode's header is up: the large title fades out under it. */
    titleHidden: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val hide by animateFloatAsState(if (titleHidden) 1f else 0f, tween(SELECTION_FADE_MS), label = "title")
    var largeTitle by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                // A form's fields stay above the keyboard.
                .imePadding()
                .verticalScroll(scroll)
                .topSafePadding()
                .navigationBarsPadding()
                .padding(horizontal = SettingsTokens.Padding),
        ) {
            Spacer(Modifier.height(SettingsTokens.TopBar))
            Text(
                title,
                color = KoanTheme.colors.TextPrimary,
                style = KoanType.largeTitle(SettingsTokens.TitleSize),
                maxLines = SettingsTokens.TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .onSizeChanged { largeTitle = it.height }
                    // Gone before the compact title appears: the two never show at once.
                    .graphicsLayer {
                        // Fully opaque through the first bit of scroll (a nudge doesn't grey it).
                        val h = largeTitle.coerceAtLeast(1)
                        val from = h * SettingsTokens.TITLE_FADE_FROM
                        val to = h * SettingsTokens.BAR_TITLE_FROM
                        alpha = (1f - ((scroll.value - from) / (to - from)).coerceIn(0f, 1f)) * (1f - hide)
                    }
                    .padding(bottom = SettingsTokens.TitleBottom),
            )
            content()
            Spacer(Modifier.height(SettingsTokens.SectionTop))
        }
        CollapsingBar(title, scroll, { largeTitle }, interactive, onBack)
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
    /** In place of [icon]: any glyph-sized content (a site's favicon). */
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .rowArea(if (interactive && enabled) onClick else null)
            .height(if (subtitle != null) SettingsTokens.RowTallHeight else SettingsTokens.RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(SettingsTokens.RowGlyphGap))
        } else if (icon != null) {
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun Modifier.rowArea(onClick: (() -> Unit)?, onLongClick: (() -> Unit)? = null, wash: Color = Color.Transparent): Modifier =
    layout { measurable, constraints ->
        val inset = SettingsTokens.RowInset.roundToPx()
        val width = constraints.maxWidth + 2 * inset
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-inset, 0) }
    }
        .clip(RoundedCornerShape(SettingsTokens.RowRadius))
        .background(wash)
        .then(
            when {
                onClick != null && onLongClick != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                onClick != null -> Modifier.clickable(onClick = onClick)
                else -> Modifier
            },
        )
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
