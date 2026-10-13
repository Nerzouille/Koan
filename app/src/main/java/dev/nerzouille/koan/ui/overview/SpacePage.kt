package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.OverviewTokens
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlin.math.abs

@Immutable
data class SpaceModel(
    val uuid: String,
    val name: String,
    val icon: String,
    val gradient: List<Color>,
    val essentials: List<CardModel>,
    val pinned: List<CardModel>,
    val stack: List<CardModel>,
)

/**
 * One space in the overview: compact header and the depth stack. Essentials and pinned tabs live
 * in the bar's drawer. [pageOffset] is this page's distance from the settled one while switching
 * spaces (-1..1).
 */
@Composable
fun SpacePage(
    space: SpaceModel,
    stack: StackState,
    active: Boolean,
    pageOffset: () -> Float,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onOpenTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SpaceHeader(space, onBack = onDismiss, onNewTab = onNewTab)
        DepthStack(
            cards = space.stack,
            state = stack,
            enabled = active,
            onOpen = onOpenTab,
            onClose = onCloseTab,
            onReorder = onReorder,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    // The incoming stack trails the header slightly (0.85× speed) and grows 0.96 → 1.
                    val f = pageOffset()
                    translationX = -f * size.width * PagerTokens.PARALLAX
                    val s = 1f - PagerTokens.INCOMING_SCALE * abs(f).coerceAtMost(1f)
                    scaleX = s
                    scaleY = s
                },
        )
    }
}

/** Compact header: back, space icon + name, new tab. Swipe down on it = back to the current tab. */
@Composable
private fun SpaceHeader(space: SpaceModel, onBack: () -> Unit, onNewTab: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(OverviewTokens.HeaderHeight)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
                    total += dy
                    if (total > OverviewTokens.SwipeDownToDismiss.toPx()) {
                        total = Float.NEGATIVE_INFINITY
                        onBack()
                    }
                    change.consume()
                }
            }
            .padding(horizontal = OverviewTokens.HeaderPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIcon(KoanIcons.Back, onBack)
        Box(
            Modifier.size(OverviewTokens.SpaceIconBox).clip(RoundedCornerShape(OverviewTokens.SpaceIconRadius)).background(KoanColors.Glass),
            contentAlignment = Alignment.Center,
        ) { Text(space.icon, fontSize = OverviewTokens.SpaceIconSize) }
        Spacer(Modifier.width(OverviewTokens.TitleGap))
        Text(
            space.name,
            color = KoanColors.TextPrimary,
            fontSize = OverviewTokens.TitleSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .padding(end = OverviewTokens.HeaderPadding)
                .size(OverviewTokens.NewTabButton)
                .clip(CircleShape)
                .background(KoanColors.NewTabButton)
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center,
        ) {
            Icon(KoanIcons.Plus, "New tab", tint = KoanColors.NewTabIcon, modifier = Modifier.size(OverviewTokens.NewTabGlyph))
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(OverviewTokens.HeaderIcon).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = KoanColors.TextPrimary, modifier = Modifier.size(OverviewTokens.HeaderGlyph))
    }
}
