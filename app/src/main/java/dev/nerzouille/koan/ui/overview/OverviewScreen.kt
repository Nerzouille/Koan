package dev.nerzouille.koan.ui.overview

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.theme.KoanColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/** Height of the dock zone below the stack: bar (56) + bottom margin (16) + gap above it (12). */
private val DockHeight = 84.dp

/** The Space overview: one page per space, switched only from the workspace bar. */
@Composable
fun OverviewScreen(
    spaces: List<SpaceWithTabs>,
    activeTabId: String?,
    initialSpace: Int,
    onSpaceSettled: (Int) -> Unit,
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
    val pager = remember { SpacePager(initialSpace.coerceIn(0, spaces.size - 1), scope) }
    pager.count = spaces.size
    // Per-space scroll state, kept while the overview is open even when a page is not composed.
    val stacks = remember { HashMap<String, StackState>() }
    fun stackFor(space: SpaceModel) = stacks.getOrPut(space.uuid) {
        val active = space.stack.indexOfFirst { it.active }
        StackState((if (active >= 0) active else space.stack.lastIndex).coerceAtLeast(0).toFloat(), scope)
    }

    val view = LocalView.current
    LaunchedEffect(pager) {
        snapshotFlow { pager.pos.roundToInt() }.distinctUntilChanged().drop(1).collect {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settled }.drop(1).collect(onSpaceSettled)
    }

    val dockPx = with(LocalDensity.current) { DockHeight.toPx() }
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
                                translationX = f * (size.width + 24.dp.toPx())
                                alpha = if (abs(f) >= 1f) 0f else 1f
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            WorkspaceBar(
                spaces = models,
                pager = pager,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/** Desaturated, darkened version of a Zen gradient stop, so cards stay the colour (UI_SPEC). */
private fun backdrop(gradient: List<Color>, stop: Int): Color {
    if (gradient.isEmpty()) return KoanColors.Base
    val c = gradient[stop.coerceAtMost(gradient.size - 1)]
    val grey = c.luminance().let { Color(it, it, it) }
    val muted = lerp(c, grey, 0.3f)
    return lerp(KoanColors.Base, muted, if (stop == 0) 0.75f else 0.45f)
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
