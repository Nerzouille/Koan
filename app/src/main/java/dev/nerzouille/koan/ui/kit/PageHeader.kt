package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.settings.settingsWash
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanType
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.topSafePadding

/*
 * The one page header (docs/UI_SPEC.md "Page header"): ‹ back and a compact title on a fixed 56dp
 * bar, an optional trailing slot. List pages (History, Downloads) show it as is; settings pages add
 * a large title under it that collapses into the bar as the page scrolls ([CollapsingBar]).
 */

/** The bar itself: back, then [title] on one line (ellipsized), then [trailing]. */
@Composable
fun PageBar(
    title: String?,
    interactive: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleAlpha: () -> Float = { 1f },
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = KoanTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(SettingsTokens.TopBar)
            .padding(horizontal = SettingsTokens.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KoanIconButton(
            KoanIcons.Back, onBack, SettingsTokens.TopBar, SettingsTokens.BackGlyph, palette.TextPrimary,
            interactive = interactive, alignment = Alignment.CenterStart, contentDescription = "Back",
        )
        Box(Modifier.weight(1f).graphicsLayer { alpha = titleAlpha() }) {
            if (title != null) {
                Text(
                    title,
                    color = palette.TextPrimary,
                    style = KoanType.title(SettingsTokens.BarTitleSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

/**
 * The bar over a scrolling settings page. While the large title (its height [largeTitlePx]) scrolls
 * under the bar, the bar's surface fades in; the compact title fades in over the end of it; once
 * the content passes under the bar, a hairline. All of it is read from [scroll] in layer / draw
 * lambdas: scrolling never recomposes.
 */
@Composable
fun BoxScope.CollapsingBar(
    title: String?,
    scroll: ScrollState,
    largeTitlePx: () -> Int,
    interactive: Boolean,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = KoanTheme.colors
    fun collapse(): Float {
        val h = largeTitlePx()
        return if (h <= 0) 1f else (scroll.value / h.toFloat()).coerceIn(0f, 1f)
    }
    Box(Modifier.align(Alignment.TopStart).fillMaxWidth()) {
        // The surface repeats the page's base and wash at the same origin, so it is seamless.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = collapse() }
                .background(palette.Base)
                .settingsWash(palette.isDark)
                .drawBehindHairline(scroll, largeTitlePx),
        )
        PageBar(
            title,
            interactive,
            onBack,
            modifier = Modifier.topSafePadding(),
            titleAlpha = {
                val from = SettingsTokens.BAR_TITLE_FROM
                ((collapse() - from) / (1f - from)).coerceIn(0f, 1f)
            },
            trailing = trailing,
        )
    }
}

@Composable
private fun Modifier.drawBehindHairline(scroll: ScrollState, largeTitlePx: () -> Int): Modifier {
    val color = KoanTheme.colors.CardBorder
    return drawBehind {
        val past = scroll.value - largeTitlePx().coerceAtLeast(0)
        val a = (past / SettingsTokens.BarHairlineFade.toPx()).coerceIn(0f, 1f)
        if (a <= 0f) return@drawBehind
        val y = size.height - SettingsTokens.Divider.toPx() / 2
        drawLine(color.copy(alpha = color.alpha * a), Offset(0f, y), Offset(size.width, y), SettingsTokens.Divider.toPx())
    }
}
