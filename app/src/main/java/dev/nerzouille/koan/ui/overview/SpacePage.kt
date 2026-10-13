package dev.nerzouille.koan.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanColors
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

private const val MAX_PINNED_ROWS = 5

/**
 * One space in the overview: header, the collapsing essentials/pinned block, the depth stack.
 * [pageOffset] is this page's distance from the settled one while switching spaces (-1..1).
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
        SpaceHeader(space, expansion = { stack.expansion }, onBack = onDismiss, onNewTab = onNewTab)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            var blockHeight by remember { mutableFloatStateOf(0f) }
            DepthStack(
                cards = space.stack,
                state = stack,
                blockHeight = { blockHeight },
                enabled = active,
                onOpen = onOpenTab,
                onClose = onCloseTab,
                onReorder = onReorder,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // The incoming stack trails the header slightly (0.85× speed) and grows 0.96 → 1.
                        val f = pageOffset()
                        translationX = -f * size.width * 0.15f
                        val s = 1f - 0.04f * abs(f).coerceAtMost(1f)
                        scaleX = s
                        scaleY = s
                    },
            )
            val interactive by remember { derivedStateOf { stack.expansion > 0.5f } }
            HeaderBlock(
                space = space,
                interactive = interactive && active,
                onOpenTab = onOpenTab,
                modifier = Modifier
                    .onSizeChanged { blockHeight = it.height.toFloat() }
                    .graphicsLayer {
                        val e = stack.expansion
                        alpha = e
                        translationY = -24.dp.toPx() * (1f - e)
                        val s = 0.96f + 0.04f * e
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .then(
                        if (interactive && active) {
                            Modifier.pointerInput(stack) {
                                detectVerticalDragGestures(
                                    onDragStart = { stack.stopScroll() },
                                    onDragEnd = { stack.fling(0f) },
                                    onDragCancel = { stack.fling(0f) },
                                ) { change, dy ->
                                    change.consume()
                                    stack.dragBy(dy)
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

/** 52dp header: back, space icon + name (larger while the block is revealed), new tab. */
@Composable
private fun SpaceHeader(space: SpaceModel, expansion: () -> Float, onBack: () -> Unit, onNewTab: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .pointerInput(Unit) {
                // Swipe down on the header = back to the current tab.
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
                    total += dy
                    if (total > 48.dp.toPx()) {
                        total = Float.NEGATIVE_INFINITY
                        onBack()
                    }
                    change.consume()
                }
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIcon(KoanIcons.Back, onBack)
        Row(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    val s = 0.88f + 0.12f * expansion()
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(KoanColors.Glass),
                contentAlignment = Alignment.Center,
            ) { Text(space.icon, fontSize = 17.sp) }
            Spacer(Modifier.width(10.dp))
            Text(
                space.name,
                color = KoanColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .padding(end = 4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center,
        ) {
            Icon(KoanIcons.Plus, "New tab", tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = KoanColors.TextPrimary, modifier = Modifier.size(22.dp))
    }
}

/** Essentials grid (4 per row, max 2 rows) and pinned rows. Not cards (docs/UI_SPEC.md). */
@Composable
private fun HeaderBlock(
    space: SpaceModel,
    interactive: Boolean,
    onOpenTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        space.essentials.take(8).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tab ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KoanColors.Glass)
                            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Favicon(tab.url, 24.dp) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (space.pinned.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KoanColors.Glass)) {
                space.pinned.take(MAX_PINNED_ROWS).forEach { tab ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Favicon(tab.url, 22.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            tab.title,
                            color = KoanColors.TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(KoanIcons.Pin, null, tint = KoanColors.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                val more = space.pinned.size - MAX_PINNED_ROWS
                if (more > 0) {
                    Text(
                        "+$more more",
                        color = KoanColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
