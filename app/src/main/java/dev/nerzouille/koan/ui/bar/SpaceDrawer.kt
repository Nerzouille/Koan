package dev.nerzouille.koan.ui.bar

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.icons.ZenIcon
import dev.nerzouille.koan.ui.overview.DrawerRow
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.overview.SpaceModel
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.overview.CurrentTabDot
import dev.nerzouille.koan.ui.theme.BrandSpinner
import dev.nerzouille.koan.ui.theme.FirstRunTokens
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Where the drawer rests: closed, compact (fits its content, no scrolling) or full screen. */
enum class Detent { Closed, Compact, Full }

/**
 * The essentials/pinned drawer that grows out of the overview's bar, a sheet with detents. Its
 * [height] follows the finger 1:1; only the full detent lets a list taller than the screen
 * scroll, and only once the sheet is all the way up (a drag down at the top of the list collapses
 * the sheet instead). [height] and [scroll] are read in layers and draw only.
 */
@Stable
class DrawerState(private val scope: CoroutineScope, private val decay: DecayAnimationSpec<Float>) {
    /** Visible height of the sheet above the bar's rows (px), 0 = closed. */
    var height by mutableFloatStateOf(0f)
        private set

    /** How far the full sheet's list has scrolled (px). */
    var scroll by mutableFloatStateOf(0f)
        private set

    /** Where it rests or is going: flips on release or tap. Decides whether the sheet takes input. */
    var detent by mutableStateOf(Detent.Closed)
        private set
    val open: Boolean get() = detent != Detent.Closed

    /** Height the drawer is laid out at (the tallest space's content, capped at full). */
    var layoutPx by mutableFloatStateOf(0f)
        internal set

    /** Caps set from the window size: compact keeps the dock under ~55% of the screen. */
    var compactCapPx = 1f
    var fullCapPx = 1f
    var minStepPx = 0f

    /** Natural (unscrolled) content height of each space's page, and the page on screen. */
    private var pageHeights = IntArray(0)
    private var page = 0
    private val contentPx: Float get() = pageHeights.getOrElse(page) { 0 }.toFloat()
    private val compactPx: Float get() = minOf(contentPx, compactCapPx).coerceAtLeast(1f)
    private val fullPx: Float get() = minOf(contentPx, fullCapPx).coerceAtLeast(compactPx)
    private val maxScroll: Float get() = (contentPx - fullPx).coerceAtLeast(0f)

    /** 0 closed … 1 at the compact height (scrim, blur, corners); stays 1 above it. */
    val progress: Float get() = (height / compactPx).coerceIn(0f, 1f)

    private var job: Job? = null
    private var from = Detent.Closed
    private var dragging = false

    private fun stops(): List<Pair<Detent, Float>> = buildList {
        add(Detent.Closed to 0f)
        add(Detent.Compact to compactPx)
        if (fullPx - compactPx >= minStepPx) add(Detent.Full to fullPx)
    }

    private fun stopPx(d: Detent): Float = stops().lastOrNull { it.first <= d }?.second ?: 0f

    /** Top of the visible sheet inside the drawer's layout box. */
    fun sheetTop(): Float = layoutPx - height

    /** Finger down: freeze. A step that was still landing counts as landed. */
    fun grab() {
        job?.cancel()
        dragging = true
        from = detent
    }

    /**
     * Finger moved by [dyPx] (> 0 = down). Up grows the sheet to full, then scrolls its list; down
     * scrolls the list back to its top first, then shrinks the sheet.
     */
    fun dragBy(dyPx: Float) {
        var d = -dyPx
        if (d > 0f) {
            val grow = minOf(d, fullPx - height).coerceAtLeast(0f)
            height += grow
            d -= grow
            if (d > 0f) scroll = (scroll + d).coerceAtMost(maxScroll)
        } else {
            val back = minOf(-d, scroll)
            scroll -= back
            d += back
            if (d < 0f) height = (height + d).coerceAtLeast(0f)
        }
    }

    /**
     * Finger up with vertical velocity [vyPx] px/s. A fling goes to the next detent that way; a
     * slow release commits to the neighbouring detent past [DrawerTokens.COMMIT_FRACTION] of the
     * gap. A fling of a scrolled full list keeps scrolling it. Returns true when the drawer lands
     * on another detent than the one it started from.
     */
    fun release(vyPx: Float, flingPx: Float): Boolean {
        dragging = false
        val atFull = maxScroll > 0f && height >= fullPx - 0.5f
        if (atFull && (scroll > 0f || vyPx < 0f)) {
            detent = Detent.Full
            flingList(-vyPx)
            return from != Detent.Full
        }
        val stops = stops()
        val target = when {
            vyPx < -flingPx -> stops.firstOrNull { it.second > height + 1f } ?: stops.last()
            vyPx > flingPx -> stops.lastOrNull { it.second < height - 1f } ?: stops.first()
            else -> {
                // Between stops j and j + 1: leaving the start stop commits early, coming back late.
                val j = stops.indexOfLast { it.second <= height }.coerceIn(0, stops.lastIndex)
                if (j == stops.lastIndex) {
                    stops[j]
                } else {
                    val f = (height - stops[j].second) / (stops[j + 1].second - stops[j].second)
                    val up = stops.indexOfFirst { it.first == from } <= j
                    val commit = if (up) DrawerTokens.COMMIT_FRACTION else 1f - DrawerTokens.COMMIT_FRACTION
                    if (f > commit) stops[j + 1] else stops[j]
                }
            }
        }.first
        animateTo(target, -vyPx)
        return target != from
    }

    /** Settles on [target] ([velocity] in px/s, > 0 = growing). */
    fun animateTo(target: Detent, velocity: Float = 0f) {
        detent = target
        job?.cancel()
        val to = stopPx(target)
        job = scope.launch {
            if (scroll > 0f && target != Detent.Full) launch { animate(scroll, 0f, 0f, Motion.Drawer) { v, _ -> scroll = v.coerceAtLeast(0f) } }
            animate(height, to, velocity, Motion.Drawer) { v, _ -> height = v.coerceAtLeast(0f) }
        }
    }

    fun toggle() = animateTo(if (open) Detent.Closed else Detent.Compact)

    fun snapClosed() {
        job?.cancel()
        dragging = false
        height = 0f
        scroll = 0f
        detent = Detent.Closed
    }

    private fun flingList(velocity: Float) {
        job?.cancel()
        job = scope.launch {
            AnimationState(scroll, velocity).animateDecay(decay) {
                scroll = value.coerceIn(0f, maxScroll)
                if (value <= 0f || value >= maxScroll) cancelAnimation()
            }
        }
    }

    /**
     * The pages were measured ([heights]) or the space on screen changed ([settled]): the sheet
     * keeps its detent, at the new content's height, and the list starts from its top.
     */
    internal fun refit(heights: IntArray, settled: Int) {
        val changed = !heights.contentEquals(pageHeights) || settled != page
        if (!changed) return
        if (settled != page) scroll = 0f
        pageHeights = heights
        page = settled
        if (dragging || detent == Detent.Closed) return
        scroll = scroll.coerceAtMost(maxScroll)
        val to = stopPx(detent)
        if (to != height) {
            job?.cancel()
            job = scope.launch { animate(height, to, 0f, Motion.Drawer) { v, _ -> height = v.coerceAtLeast(0f) } }
        }
    }
}

/** The drawer's footer: account and sync, quietly. */
@Immutable
sealed interface DrawerStatus {
    data object None : DrawerStatus
    data object Connect : DrawerStatus
    data object Syncing : DrawerStatus
    data class Synced(val at: Long, val failed: Boolean = false, val offline: Boolean = false) : DrawerStatus
    data object SignInAgain : DrawerStatus
}

/**
 * Essentials grid on top, pinned rows (with Zen folders) below, the sync row at the bottom. One
 * page per space sliding with the space pager. All spaces are laid out so the sheet keeps the
 * height of the tallest one while switching.
 */
@Composable
fun DrawerContent(
    spaces: List<SpaceModel>,
    pager: SpacePager,
    drawer: DrawerState,
    status: DrawerStatus,
    onOpenTab: (spaceIndex: Int, tabId: String) -> Unit,
    onToggleFolder: (folderId: String, collapsed: Boolean) -> Unit,
    onStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pages are measured at their natural height; the box is as tall as the tallest one (capped at
    // the full detent) and each page hangs from the sheet's top edge, clipped at the bar.
    var heights by remember { mutableStateOf(IntArray(0)) }
    LaunchedEffect(drawer, pager) {
        snapshotFlow { heights to pager.settled }.collect { (h, settled) -> drawer.refit(h, settled) }
    }
    Layout(
        content = {
            spaces.forEachIndexed { i, space ->
                DrawerPage(
                    space = space,
                    status = status,
                    interactive = drawer.open && i == pager.settled,
                    onOpenTab = { onOpenTab(i, it) },
                    onToggleFolder = onToggleFolder,
                    onStatus = onStatus,
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .graphicsLayer {
                val p = drawer.progress
                alpha = p
                // No offscreen buffer for the fade (the GPU would keep it cached; see DepthStack).
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = measurables.map { it.measure(loose) }
        val measured = IntArray(placeables.size) { placeables[it].height }
        if (!measured.contentEquals(heights)) heights = measured
        val tallest = placeables.maxOfOrNull { it.height } ?: 0
        val h = minOf(tallest, drawer.fullCapPx.toInt()).coerceAtLeast(0)
        if (drawer.layoutPx != h.toFloat()) drawer.layoutPx = h.toFloat()
        layout(constraints.maxWidth, h) {
            placeables.forEachIndexed { i, p ->
                p.placeWithLayer(0, 0) {
                    val f = i - pager.pos
                    translationX = f * (size.width + PagerTokens.PageGap.toPx())
                    alpha = if (abs(f) >= 1f) 0f else 1f
                    val slide = (1f - drawer.progress) * DrawerTokens.Gap.toPx() * 3f
                    translationY = drawer.sheetTop() + slide - if (i == pager.settled) drawer.scroll else 0f
                }
            }
        }
    }
}

@Composable
private fun DrawerPage(
    space: SpaceModel,
    status: DrawerStatus,
    interactive: Boolean,
    onOpenTab: (String) -> Unit,
    onToggleFolder: (String, Boolean) -> Unit,
    onStatus: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(DrawerTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(DrawerTokens.Gap),
    ) {
        if (space.essentials.isEmpty() && space.pinned.isEmpty()) {
            Text(
                "No essentials or pinned tabs in ${space.name}",
                color = KoanTheme.colors.TextSecondary,
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
                            .background(KoanTheme.colors.Glass)
                            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Favicon(tab.url, DrawerTokens.EssentialFavicon, icon = tab.icon, staticIcon = tab.staticIcon) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (space.pinned.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DrawerTokens.PinnedRadius))
                    .background(KoanTheme.colors.Glass),
            ) {
                space.pinned.forEach { row ->
                    key(row.key) {
                        when (row) {
                            is DrawerRow.Tab -> PinnedRow(row, interactive, onOpenTab)
                            is DrawerRow.Folder -> FolderRow(row, interactive) { onToggleFolder(row.id, !row.collapsed) }
                        }
                    }
                }
            }
        }
        StatusRow(status, interactive, onStatus)
    }
}

@Composable
private fun PinnedRow(row: DrawerRow.Tab, interactive: Boolean, onOpenTab: (String) -> Unit) {
    val tab = row.card
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.PinnedRowHeight)
            .then(if (interactive) Modifier.clickable { onOpenTab(tab.id) } else Modifier)
            .padding(start = DrawerTokens.PinnedPadding + DrawerTokens.FolderIndent * row.depth, end = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(tab.url, DrawerTokens.PinnedFavicon, icon = tab.icon, staticIcon = tab.staticIcon)
        Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        Text(
            tab.title,
            color = KoanTheme.colors.TextPrimary,
            fontSize = DrawerTokens.PinnedTitleSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (tab.active) {
            CurrentTabDot()
            Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        }
        if (row.depth == 0) Icon(KoanIcons.Pin, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedGlyph))
    }
}

/** A Zen folder: tap to fold/unfold its rows (remembered on the phone, never synced). */
@Composable
private fun FolderRow(row: DrawerRow.Folder, interactive: Boolean, onToggle: () -> Unit) {
    val turn = animateFloatAsState(if (row.collapsed) -90f else 0f, Motion.FolderChevron, label = "chevron")
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.PinnedRowHeight)
            .then(if (interactive) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(start = DrawerTokens.PinnedPadding + DrawerTokens.FolderIndent * row.depth, end = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(DrawerTokens.PinnedFavicon), contentAlignment = Alignment.Center) {
            if (row.icon.isNullOrBlank()) {
                Icon(KoanIcons.Folder, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(DrawerTokens.PinnedFavicon))
            } else {
                ZenIcon(row.icon, DrawerTokens.PinnedFavicon, tint = KoanTheme.colors.TextSecondary)
            }
        }
        Spacer(Modifier.width(DrawerTokens.PinnedPadding))
        // Name and count take the free width, so the chevron always sits at the right edge.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.name.ifBlank { "Folder" },
                color = KoanTheme.colors.TextPrimary,
                fontSize = DrawerTokens.PinnedTitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(DrawerTokens.Gap))
            Text("${row.count}", color = KoanTheme.colors.TextSecondary, fontSize = DrawerTokens.FolderCountSize)
        }
        Icon(
            KoanIcons.ChevronDown, null, tint = KoanTheme.colors.TextSecondary,
            modifier = Modifier.size(DrawerTokens.PinnedGlyph).graphicsLayer { rotationZ = turn.value },
        )
    }
}

/** "Synced 4 min ago", "Syncing…", or a quiet amber "Sign in again". Tap = act on it. */
@Composable
private fun StatusRow(status: DrawerStatus, interactive: Boolean, onClick: () -> Unit) {
    if (status == DrawerStatus.None) return
    val (text, attention) = when (status) {
        DrawerStatus.Connect -> "Connect your Zen Spaces" to false
        DrawerStatus.Syncing -> "Syncing with Zen…" to false
        DrawerStatus.SignInAgain -> "Sign in again to keep your Zen Spaces in sync" to true
        is DrawerStatus.Synced -> when {
            status.offline -> "Offline · showing your spaces from ${ago(status.at)}" to true
            status.failed -> "Couldn't sync · tap to retry" to true
            else -> "Synced ${ago(status.at)} · Sync now" to false
        }
        DrawerStatus.None -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(DrawerTokens.StatusRowHeight)
            .clip(CircleShape)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = DrawerTokens.PinnedPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (attention) {
            Box(Modifier.size(CardTokens.ActiveDot).clip(CircleShape).background(KoanTheme.colors.Attention))
            Spacer(Modifier.width(DrawerTokens.Gap))
        } else if (status == DrawerStatus.Syncing) {
            BrandSpinner(FirstRunTokens.BadgeSpinner)
            Spacer(Modifier.width(DrawerTokens.Gap))
        }
        Text(text, color = KoanTheme.colors.TextSecondary, fontSize = DrawerTokens.StatusTextSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun ago(at: Long): String {
    if (at <= 0L) return "never"
    val minutes = (System.currentTimeMillis() - at) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
}
