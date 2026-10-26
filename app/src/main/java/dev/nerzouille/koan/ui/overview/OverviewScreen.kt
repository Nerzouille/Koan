package dev.nerzouille.koan.ui.overview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.layout.onGloballyPositioned
import dev.nerzouille.koan.ui.topSafePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.data.PinnedEntry
import dev.nerzouille.koan.data.SpaceWithTabs
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.OverviewTokens
import dev.nerzouille.koan.ui.theme.SpaceColors
import dev.nerzouille.koan.ui.theme.PagerTokens
import dev.nerzouille.koan.ui.theme.StackTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.abs

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
    /** The overview is open (false while its closing motion still shows it). */
    open: Boolean,
    /** The open motion has finished (work that would cost its frames waits for this). */
    settled: Boolean,
    /** Space uuid → that space's current tab (card id last opened there); owned by the caller. */
    currentTabs: Map<String, String>,
    onBadge: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
    /** (space uuid, card id) to bring into focus once that stack shows it; consumed here. */
    focusRequest: MutableState<Pair<String, String>?>,
) {
    BackHandler(onBack = onDismiss)
    if (spaces.isEmpty() || models.size != spaces.size) return

    // Composing a space's page costs ~40 ms (measured on the POCO). Doing it when a switch lands
    // was a visible hitch, so pages are composed ahead of time, one per idle moment, and kept
    // while the overview is open: switching spaces then composes nothing. The open's first frame
    // composes only the current space; its neighbours come first once the motion has settled.
    val precomposed = remember { mutableStateListOf<String>() }
    val latestSettled by rememberUpdatedState(settled)
    LaunchedEffect(models.map { it.uuid }) {
        snapshotFlow { latestSettled }.first { it }
        while (true) {
            snapshotFlow { pager.moving }.first { !it }
            val here = pager.settled
            val order = models.indices.sortedBy { abs(it - here) }.map { models[it].uuid }
            val next = order.firstOrNull { it !in precomposed } ?: break
            precomposed += next
            // Let that frame render before the next page; yield to gestures in between.
            repeat(PRECOMPOSE_GAP_FRAMES) { withFrameNanos { } }
        }
    }

    // A space once settled on stays composed while the overview is open.
    LaunchedEffect(pager.settled, models.map { it.uuid }) {
        val uuid = models.getOrNull(pager.settled)?.uuid ?: return@LaunchedEffect
        if (uuid !in precomposed) precomposed += uuid
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
    // The stack opens centred on the space's current tab (UI_SPEC): the live tab, else the card
    // last opened in that space, else the newest card.
    fun currentIndex(space: SpaceModel): Int {
        val active = space.stack.indexOfFirst { it.active }
        val last = currentTabs[space.uuid]?.let { id -> space.stack.indexOfFirst { it.id == id } } ?: -1
        val i = when {
            active >= 0 -> active
            last >= 0 -> last
            else -> space.stack.lastIndex
        }
        return i.coerceAtLeast(0)
    }
    fun stackFor(space: SpaceModel) = stacks.getOrPut(space.uuid) { StackState(currentIndex(space).toFloat(), scope) }

    // A scrolled stack never stays scrolled: reopening the overview, or coming back to a space,
    // finds it centred on its current tab again. Done without motion, while it is off screen.
    val latestModels by rememberUpdatedState(models)
    fun recentre(space: SpaceModel) {
        val stack = stacks[space.uuid] ?: return
        if (!stack.busy) stack.snapTo(currentIndex(space))
    }
    LaunchedEffect(open) {
        if (open) latestModels.forEach(::recentre)
    }
    LaunchedEffect(focusRequest.value) {
        val (uuid, id) = focusRequest.value ?: return@LaunchedEffect
        focusRequest.value = null
        latestModels.firstOrNull { it.uuid == uuid }?.let { stackFor(it).focusId = id }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settled }.collectLatest {
            // One space per idle frame (a far jump may compose a few cards), never one in view.
            for ((i, space) in latestModels.withIndex()) {
                snapshotFlow { pager.moving }.first { !it }
                if (i != pager.settled) recentre(space)
                withFrameNanos { }
            }
        }
    }

    val palette = KoanTheme.colors
    val hero = LocalCardHero.current
    Box(
        modifier
            .fillMaxSize()
            // Inside the open zoom: the stacks measure their cards from here (CardHero).
            .onGloballyPositioned { hero.overview = it }
            .drawBehind { drawRect(palette.Base) },
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .topSafePadding(),
            ) {
                val current = pager.settled
                for (i in models.indices) {
                    val space = models[i]
                    // A real page: composed ahead of time, or the settled one (a switch only lands
                    // once the motion is over: composing a page never costs a moving frame). Beside
                    // the settled space, a ghost stands in until then, laid out from the first frame.
                    val real = space.uuid in precomposed || i == current
                    val ghost = abs(i - current) <= 1
                    if (!real && !ghost) continue
                    key(space.uuid) {
                        val offset = { i - pager.pos }
                        // The whole space moves as one unit, a gap between spaces, and fades into
                        // the backdrop (drawn over it, never an offscreen layer) as it leaves.
                        val motion = Modifier.graphicsLayer {
                            val f = offset()
                            translationX = f * (size.width + PagerTokens.PageGap.toPx())
                            alpha = if (abs(f) >= 1f) 0f else 1f
                        }
                        val fade = Modifier.drawWithContent {
                            drawContent()
                            val e = abs(offset()).coerceIn(0f, 1f)
                            if (e > 0f) drawRect(palette.Base, alpha = e)
                        }
                        // Back to a ghost (a page dropped off): its next real page fades in over it again.
                        var ghostGone by remember(real) { mutableStateOf(false) }
                        if (real) {
                            // Composed while on screen (just landed): it fades in over its ghost.
                            val appear = remember { Animatable(if (!latestSettled || abs(offset()) >= 1f) 1f else 0f) }
                            LaunchedEffect(Unit) {
                                appear.animateTo(1f, tween(PagerTokens.PAGE_APPEAR_MS))
                                ghostGone = true
                            }
                            if (!ghostGone) GhostSpace(space, sync.badge, currentIndex(space), offset, motion.then(fade))
                            SpacePage(
                                space = space,
                                stack = stackFor(space),
                                active = i == current,
                                sync = sync,
                                pageOffset = offset,
                                onDismiss = onDismiss,
                                onNewTab = { onNewTab(i) },
                                onOpenTab = { id -> spaces[i].findTab(id)?.let { onOpenTab(i, it) } },
                                onCloseTab = onCloseTab,
                                onReorder = onReorder,
                                onBadge = onBadge,
                                onConnect = onConnect,
                                settled = settled,
                                modifier = motion
                                    .graphicsLayer {
                                        alpha = appear.value * reveal.value
                                        // Only the fade-in after a landing uses a layer at partial alpha.
                                        compositingStrategy = CompositingStrategy.Auto
                                    }
                                    .then(fade),
                            )
                        } else {
                            GhostSpace(space, sync.badge, currentIndex(space), offset, motion.then(fade))
                        }
                    }
                }
            }
            // The stack runs on under the dock's rounded top: the dock covers the cards, no gap.
            Spacer(Modifier.navigationBarsPadding().height(dock - StackTokens.UnderDock))
        }
        // The placeholders fade out while the real spaces fade in (one-off, after the first sync).
        val revealing by remember { derivedStateOf { reveal.value < 1f } }
        if (revealing) {
            Column(Modifier.fillMaxSize().topSafePadding().graphicsLayer { alpha = 1f - reveal.value }) {
                Spacer(Modifier.height(OverviewTokens.HeaderHeight))
                SkeletonStack(Modifier.weight(1f))
                Spacer(Modifier.navigationBarsPadding().height(dock - StackTokens.UnderDock))
            }
        }
    }
}

/** Frames between two pages composed ahead of time. */
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

/** The tab a card or row id opens: a split card opens its first member. */
fun SpaceWithTabs.findTab(id: String): TabEntity? =
    stack.firstOrNull { it.id == id }?.first
        ?: stack.firstNotNullOfOrNull { item -> item.tabs.firstOrNull { it.koanTabId == id } }
        ?: pinnedTabs.firstOrNull { it.koanTabId == id }
        ?: essentials.firstOrNull { it.koanTabId == id }

/**
 * UI model of a space. A pinned or essential tab opened on the phone has a card in the stack too,
 * marked pinned (docs/UI_SPEC.md "Live pinned tabs also get a card").
 */
fun SpaceWithTabs.toModel(activeTabId: String?, palette: KoanPalette): SpaceModel {
    fun TabEntity.card() =
        CardModel(koanTabId, title, url, koanTabId == activeTabId, pinned || essential, iconUrl, staticIcon, thumb = thumbnailKey)
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
    val gradient = space.gradientColors.map { Color(it.toInt()) }
    return SpaceModel(
        uuid = space.uuid,
        name = space.name,
        icon = space.icon,
        gradient = gradient,
        colors = SpaceColors.of(gradient, palette),
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
                    splitCount = item.tabs.size, thumb = first.thumbnailKey,
                )
            }
        },
        zen = space.zenOrigin,
    )
}
