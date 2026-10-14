package dev.nerzouille.koan.ui.overview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import dev.nerzouille.koan.data.PinnedEntry
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.OverviewTokens
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.floor

/**
 * The Space overview: one page per space, switched only from the workspace switcher in the bottom
 * bar (drawn by KoanRoot over the [dock] zone this screen leaves free).
 */
@Composable
fun OverviewScreen(
    spaces: List<SpaceWithTabs>,
    models: List<SpaceModel>,
    pager: SpacePager,
    dock: Dp,
    onOpenTab: (spaceIndex: Int, tab: TabEntity) -> Unit,
    onNewTab: (spaceIndex: Int) -> Unit,
    onCloseTab: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    sync: SyncUi,
    onBadge: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    if (spaces.isEmpty() || models.size != spaces.size) return

    // Composing a space's page costs ~40 ms (measured on the POCO). Doing it when a switch lands
    // was a visible hitch, so pages are composed ahead of time, one per idle moment, and kept
    // while the overview is open: switching spaces then composes nothing.
    val precomposed = remember { mutableStateListOf<String>() }
    LaunchedEffect(models.map { it.uuid }) {
        delay(PRECOMPOSE_START_MS)
        while (true) {
            snapshotFlow { pager.moving }.first { !it }
            val next = models.firstOrNull { it.uuid !in precomposed }?.uuid ?: break
            precomposed += next
            // Let that frame render before the next page; yield to gestures in between.
            repeat(PRECOMPOSE_GAP_FRAMES) { withFrameNanos { } }
        }
    }

    // The first real spaces fade in over the placeholders instead of replacing them abruptly.
    val zenReady = models.any { it.zen }
    val reveal = remember { Animatable(1f) }
    var wasWaiting by remember { mutableStateOf(!zenReady) }
    LaunchedEffect(zenReady) {
        if (!zenReady) {
            wasWaiting = true
        } else if (wasWaiting) {
            wasWaiting = false
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(FirstRunTokens.REVEAL_MS))
        }
    }

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
                for (i in models.indices) {
                    val space = models[i]
                    // The settled page and its neighbours, plus every page composed ahead of time.
                    if (abs(i - settled) > 1 && space.uuid !in precomposed) continue
                    key(space.uuid) {
                        val offset = { i - pager.pos }
                        SpacePage(
                            space = space,
                            stack = stackFor(space),
                            active = i == settled,
                            sync = sync,
                            pageOffset = offset,
                            onDismiss = onDismiss,
                            onNewTab = { onNewTab(i) },
                            onOpenTab = { id -> spaces[i].findTab(id)?.let { onOpenTab(i, it) } },
                            onCloseTab = onCloseTab,
                            onReorder = onReorder,
                            onBadge = onBadge,
                            onConnect = onConnect,
                            modifier = Modifier.graphicsLayer {
                                // The whole space moves as one unit, with a small gap between spaces.
                                val f = offset()
                                translationX = f * (size.width + PagerTokens.PageGap.toPx())
                                alpha = (if (abs(f) >= 1f) 0f else 1f) * reveal.value
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(dock))
        }
        // The placeholders fade out while the real spaces fade in (one-off, after the first sync).
        val revealing by remember { derivedStateOf { reveal.value < 1f } }
        if (revealing) {
            Column(Modifier.fillMaxSize().statusBarsPadding().graphicsLayer { alpha = 1f - reveal.value }) {
                Spacer(Modifier.height(OverviewTokens.HeaderHeight))
                SkeletonStack(Modifier.weight(1f))
                Spacer(Modifier.navigationBarsPadding().height(dock))
            }
        }
    }
}

/** Pages are composed ahead of time only after the overview's opening motion. */
private const val PRECOMPOSE_START_MS = 450L
private const val PRECOMPOSE_GAP_FRAMES = 3

/** What the overview shows about the account and sync. */
@Immutable
data class SyncUi(
    /** No account connected: offer the Connect card on the phone-only space. */
    val connect: Boolean = false,
    /** The first sync is running and nothing from Zen is here yet: show placeholder cards. */
    val firstSync: Boolean = false,
    val badge: SyncBadge = SyncBadge.None,
)

enum class SyncBadge { None, Syncing, Attention }

/** Desaturated, darkened version of a Zen gradient stop, so cards stay the colour (UI_SPEC). */
private fun backdrop(gradient: List<Color>, stop: Int): Color {
    if (gradient.isEmpty()) return KoanColors.Base
    val c = gradient[stop.coerceAtMost(gradient.size - 1)]
    val grey = c.luminance().let { Color(it, it, it) }
    val muted = lerp(c, grey, OverviewTokens.BACKDROP_DESATURATE)
    return lerp(KoanColors.Base, muted, if (stop == 0) OverviewTokens.BACKDROP_TOP else OverviewTokens.BACKDROP_MID)
}

/** The tab a card or row id opens: a split card opens its first member. */
fun SpaceWithTabs.findTab(id: String): TabEntity? =
    stack.firstOrNull { it.id == id }?.first
        ?: stack.firstNotNullOfOrNull { item -> item.tabs.firstOrNull { it.koanTabId == id } }
        ?: pinnedTabs.firstOrNull { it.koanTabId == id }
        ?: essentials.firstOrNull { it.koanTabId == id }

/**
 * UI model of a space. A pinned or essential tab that is live (the active tab) also gets a card at
 * the bottom of the stack, marked pinned (docs/UI_SPEC.md "Live pinned tabs also get a card").
 */
fun SpaceWithTabs.toModel(activeTabId: String?): SpaceModel {
    fun TabEntity.card(pinned: Boolean = false) =
        CardModel(koanTabId, title, url, koanTabId == activeTabId, pinned, iconUrl, staticIcon)
    val livePinned = (essentials + pinnedTabs).filter { it.koanTabId == activeTabId }.map { it.card(pinned = true) }
    val rows = buildList {
        for (entry in pinned) {
            when (entry) {
                is PinnedEntry.Tab -> add(DrawerRow.Tab(entry.tab.card(), depth = 0))
                is PinnedEntry.Folder -> {
                    if (entry.hidden) continue
                    val f = entry.folder
                    add(DrawerRow.Folder(f.folderId, f.name, f.icon, entry.depth, f.collapsed, entry.tabs.size))
                    if (!f.collapsed) entry.tabs.forEach { add(DrawerRow.Tab(it.card(), depth = entry.depth + 1, folderId = f.folderId)) }
                }
            }
        }
    }
    return SpaceModel(
        uuid = space.uuid,
        name = space.name,
        icon = space.icon,
        gradient = space.gradientColors.map { Color(it.toInt()) },
        essentials = essentials.map { it.card() },
        pinned = rows,
        stack = stack.map { item ->
            if (!item.isSplit) {
                item.first.card()
            } else {
                // One card for the whole split view; its title names every pane.
                val first = item.first
                CardModel(
                    item.id, item.tabs.joinToString("  |  ") { it.title }, first.url,
                    item.tabs.any { it.koanTabId == activeTabId }, icon = first.iconUrl, staticIcon = first.staticIcon,
                    splitCount = item.tabs.size,
                )
            }
        } + livePinned,
        zen = space.zenOrigin,
    )
}
