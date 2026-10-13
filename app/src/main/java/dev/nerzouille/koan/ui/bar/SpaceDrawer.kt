package dev.nerzouille.koan.ui.bar

import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.overview.CardModel
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.overview.SpaceModel
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The essentials/pinned drawer that grows out of the overview's bar. */
@Stable
class DrawerState(private val scope: CoroutineScope) {
    /** 0 closed … 1 open; follows the finger 1:1 while dragging. */
    var progress by mutableFloatStateOf(0f)
        private set

    /** Where it is going: flips on release or tap. Decides whether the sheet takes input. */
    var open by mutableStateOf(false)
        private set

    /** Height of the drawer content (px): the distance a full open/close travels. */
    var heightPx by mutableFloatStateOf(1f)
        internal set

    private var job: Job? = null

    fun grab() {
        job?.cancel()
    }

    /** Finger moved by [dyPx] (negative = up = open). */
    fun dragBy(dyPx: Float) {
        progress = (progress - dyPx / heightPx).coerceIn(0f, 1f)
    }

    /** Finger up with vertical velocity [vyPx] px/s: settle open or closed. */
    fun release(vyPx: Float, flingPx: Float) {
        val target = when {
            vyPx < -flingPx -> true
            vyPx > flingPx -> false
            else -> progress > 0.5f
        }
        animateTo(target, -vyPx / heightPx)
    }

    fun animateTo(open: Boolean, velocity: Float = 0f) {
        this.open = open
        job?.cancel()
        job = scope.launch {
            animate(progress, if (open) 1f else 0f, velocity, Motion.Drawer) { v, _ -> progress = v.coerceIn(0f, 1f) }
        }
    }

    fun snapClosed() {
        job?.cancel()
        progress = 0f
        open = false
    }
}

/**
 * Essentials grid on top, pinned rows below, one page per space sliding with the space pager. All
 * spaces are laid out so the sheet keeps the height of the tallest one while switching.
 */
@Composable
fun DrawerContent(
    spaces: List<SpaceModel>,
    pager: SpacePager,
    drawer: DrawerState,
    onOpenTab: (spaceIndex: Int, tabId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { drawer.heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                val p = drawer.progress
                alpha = p
                translationY = (1f - p) * DrawerTokens.Gap.toPx() * 3f
            },
    ) {
        spaces.forEachIndexed { i, space ->
            DrawerPage(
                space = space,
                interactive = drawer.open && i == pager.settled,
                onOpenTab = { onOpenTab(i, it) },
                modifier = Modifier.graphicsLayer {
                    val f = i - pager.pos
                    translationX = f * (size.width + PagerTokens.PageGap.toPx())
                    alpha = if (abs(f) >= 1f) 0f else 1f
                },
            )
        }
    }
}

@Composable
private fun DrawerPage(space: SpaceModel, interactive: Boolean, onOpenTab: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(DrawerTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(DrawerTokens.Gap),
    ) {
        if (space.essentials.isEmpty() && space.pinned.isEmpty()) {
            Text(
                "No essentials or pinned tabs in ${space.name}",
                color = KoanColors.TextSecondary,
                fontSize = DrawerTokens.PinnedTitleSize,
                modifier = Modifier.padding(vertical = DrawerTokens.Padding),
            )
        }
        space.essentials.take(8).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(DrawerTokens.EssentialGap)) {
                row.forEach { tab ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(DrawerTokens.EssentialHeight)
                            .clip(RoundedCornerShape(DrawerTokens.EssentialRadius))
                            .background(KoanColors.Glass)
                            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Favicon(tab.url, DrawerTokens.EssentialFavicon) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (space.pinned.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(DrawerTokens.PinnedRadius)).background(KoanColors.Glass)) {
                space.pinned.take(DrawerTokens.MAX_PINNED_ROWS).forEach { tab -> PinnedRow(tab, interactive, onOpenTab) }
                val more = space.pinned.size - DrawerTokens.MAX_PINNED_ROWS
                if (more > 0) {
                    Text(
                        "+$more more",
                        color = KoanColors.TextSecondary,
                        fontSize = DrawerTokens.MoreTextSize,
                        modifier = Modifier.padding(horizontal = DrawerTokens.MorePadding, vertical = DrawerTokens.Gap),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedRow(tab: CardModel, interactive: Boolean, onOpenTab: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.PinnedRowHeight)
            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier)
            .padding(horizontal = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(tab.url, DrawerTokens.PinnedFavicon)
        Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        Text(
            tab.title,
            color = KoanColors.TextPrimary,
            fontSize = DrawerTokens.PinnedTitleSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (tab.active) {
            Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(KoanColors.DefaultAccent))
            Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        }
        Icon(KoanIcons.Pin, null, tint = KoanColors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedGlyph))
    }
}
