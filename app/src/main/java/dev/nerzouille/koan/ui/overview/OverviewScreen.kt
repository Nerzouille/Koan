package dev.nerzouille.koan.ui.overview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.OverviewTokens
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlin.math.abs
import kotlin.math.floor

/**
 * The Space overview: one page per space, switched only from the workspace switcher in the bottom
 * bar (drawn by KoanRoot over the [dock] zone this screen leaves free).
 */
@Composable
fun OverviewScreen(
    spaces: List<SpaceWithTabs>,
    activeTabId: String?,
    pager: SpacePager,
    dock: Dp,
    onOpenTab: (spaceIndex: Int, tab: TabEntity) -> Unit,
    onNewTab: (spaceIndex: Int) -> Unit,
    onCloseTab: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    if (spaces.isEmpty()) return

    val models = remember(spaces, activeTabId) { spaces.map { it.toModel(activeTabId) } }
    val scope = rememberCoroutineScope()
    // Per-space scroll state, kept while the overview is open even when a page is not composed.
    val stacks = remember { HashMap<String, StackState>() }
    fun stackFor(space: SpaceModel) = stacks.getOrPut(space.uuid) {
        val active = space.stack.indexOfFirst { it.active }
        StackState((if (active >= 0) active else space.stack.lastIndex).coerceAtLeast(0).toFloat(), scope)
    }

    val density = LocalDensity.current
    val dockPx = with(density) { dock.toPx() } + WindowInsets.navigationBars.getBottom(density)
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(KoanColors.Base)
                // Background = the space gradients blended by drag fraction; ends above the dock.
                val p = pager.pos.coerceIn(0f, (models.size - 1).toFloat())
                val i = floor(p).toInt()
                val f = p - i
                val a = models[i].gradient
                val b = models[(i + 1).coerceAtMost(models.size - 1)].gradient
                val top = lerp(backdrop(a, 0), backdrop(b, 0), f)
                val mid = lerp(backdrop(a, 1), backdrop(b, 1), f)
                drawRect(
                    Brush.verticalGradient(
                        0f to top, 0.45f to mid, 1f to KoanColors.Base,
                        endY = size.height - dockPx,
                    ),
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding(),
            ) {
                val settled = pager.settled
                for (i in (settled - 1)..(settled + 1)) {
                    val space = models.getOrNull(i) ?: continue
                    key(space.uuid) {
                        val offset = { i - pager.pos }
                        SpacePage(
                            space = space,
                            stack = stackFor(space),
                            active = i == settled,
                            pageOffset = offset,
                            onDismiss = onDismiss,
                            onNewTab = { onNewTab(i) },
                            onOpenTab = { id -> spaces[i].findTab(id)?.let { onOpenTab(i, it) } },
                            onCloseTab = onCloseTab,
                            onReorder = onReorder,
                            modifier = Modifier.graphicsLayer {
                                // The whole space moves as one unit, with a small gap between spaces.
                                val f = offset()
                                translationX = f * (size.width + PagerTokens.PageGap.toPx())
                                alpha = if (abs(f) >= 1f) 0f else 1f
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(dock))
        }
    }
}

/** Desaturated, darkened version of a Zen gradient stop, so cards stay the colour (UI_SPEC). */
private fun backdrop(gradient: List<Color>, stop: Int): Color {
    if (gradient.isEmpty()) return KoanColors.Base
    val c = gradient[stop.coerceAtMost(gradient.size - 1)]
    val grey = c.luminance().let { Color(it, it, it) }
    val muted = lerp(c, grey, OverviewTokens.BACKDROP_DESATURATE)
    return lerp(KoanColors.Base, muted, if (stop == 0) OverviewTokens.BACKDROP_TOP else OverviewTokens.BACKDROP_MID)
}

private fun SpaceWithTabs.findTab(id: String): TabEntity? =
    stack.firstOrNull { it.koanTabId == id } ?: pinned.firstOrNull { it.koanTabId == id }
        ?: essentials.firstOrNull { it.koanTabId == id }

private fun SpaceWithTabs.toModel(activeTabId: String?): SpaceModel {
    fun TabEntity.card() = CardModel(koanTabId, title, url, koanTabId == activeTabId)
    return SpaceModel(
        uuid = space.uuid,
        name = space.name,
        icon = space.icon,
        gradient = space.gradientColors.map { Color(it.toInt()) },
        essentials = essentials.map { it.card() },
        pinned = pinned.map { it.card() },
        stack = stack.map { it.card() },
    )
}
