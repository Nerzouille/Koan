package dev.nerzouille.koan.ui.sheet

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.SheetTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The pages of a sheet: a root and the pages pushed on it. A pushed page slides in from the right
 * at the sheet's height, the one under it trails; Back pops it. [pos] (the page on screen, between
 * two while sliding) is read in layers only.
 */
@Stable
class SheetNav<T : Any>(private val scope: CoroutineScope, private val root: T) {
    val pages = mutableStateListOf(root)

    /** The page that takes input: the last pushed one, even while it is still sliding. */
    var depth by mutableIntStateOf(0)
        private set
    private val slide = Animatable(0f)
    val pos: Float get() = slide.value
    private var job: Job? = null

    fun push(page: T) {
        // A page still sliding out is dropped first.
        while (pages.lastIndex > depth) pages.removeAt(pages.lastIndex)
        pages.add(page)
        depth = pages.lastIndex
        go()
    }

    /** False when already on the root. */
    fun pop(): Boolean {
        if (depth == 0) return false
        depth -= 1
        go()
        return true
    }

    /** Back to the root at once (the sheet closed). */
    fun reset() {
        job?.cancel()
        depth = 0
        pages.clear()
        pages.add(root)
        job = scope.launch { slide.snapTo(0f) }
    }

    private fun go() {
        job?.cancel()
        val to = depth
        job = scope.launch {
            slide.animateTo(to.toFloat(), Motion.SheetPush)
            while (pages.lastIndex > to) pages.removeAt(pages.lastIndex)
        }
    }
}

/** What a sheet page needs to know: whether it is the one taking input, and how to go back. */
class SheetPageScope(val interactive: Boolean, val onBack: () -> Unit)

/**
 * A sheet over the page (the ⋯ menu): it slides up from the bottom edge to its compact detent,
 * dims the page, and moves with the same detents and gestures as the pins drawer ([SheetState]).
 * Pages pushed through [nav] slide in at the same height. Back pops a page, then closes; a tap
 * above the sheet closes it. Composed only while it shows.
 */
@Composable
fun <T : Any> KoanSheet(
    state: SheetState,
    nav: SheetNav<T>,
    modifier: Modifier = Modifier,
    page: @Composable ColumnScope.(T, SheetPageScope) -> Unit,
) {
    val density = LocalDensity.current
    val windowPx = LocalWindowInfo.current.containerSize.height.toFloat()
    with(density) {
        state.compactCapPx = windowPx * SheetTokens.COMPACT_MAX_FRACTION
        state.fullCapPx = windowPx - WindowInsets.statusBars.getTop(density) - SheetTokens.FullTopGap.toPx()
        state.minStepPx = SheetTokens.MinDetentStep.toPx()
    }
    val shown by remember(state) { derivedStateOf { state.open || state.height > 0f } }
    // Closed: the next opening starts on the root page.
    LaunchedEffect(shown) { if (!shown) nav.reset() }
    if (!shown) return

    BackHandler { if (!nav.pop()) state.animateTo(Detent.Closed) }
    val view = LocalView.current
    val palette = KoanTheme.colors
    var heights by remember { mutableStateOf(IntArray(0)) }
    LaunchedEffect(state, nav) {
        // Only once the page on screen has been measured: refitting to an unmeasured (0 px) page
        // would start settling the sheet to nothing.
        snapshotFlow { heights to nav.depth }.collect { (h, depth) -> if (depth < h.size) state.refit(h, depth) }
    }
    Box(
        modifier
            .fillMaxSize()
            .drawBehind { drawRect(palette.SheetScrim, alpha = state.progress) }
            .sheetGestures(state, view) { boxHeight -> boxHeight - state.height },
    ) {
        Layout(
            content = {
                nav.pages.forEachIndexed { i, p ->
                    key(i, p) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(palette.Dock)
                                .padding(top = SheetTokens.HandleZone)
                                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                        ) {
                            val back = remember(nav) { { nav.pop(); Unit } }
                            page(p, SheetPageScope(interactive = state.open && i == nav.depth, onBack = back))
                        }
                    }
                }
                SheetHandle(Modifier)
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = size.height - state.height
                    clip = true
                    shape = RoundedCornerShape(topStart = SheetTokens.TopRadius, topEnd = SheetTokens.TopRadius)
                }
                .background(palette.Dock),
        ) { measurables, constraints ->
            val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
            // A pushed page is at least as tall as the root: the sheet keeps its height.
            val handle = measurables.last().measure(loose)
            val pageMeasurables = measurables.dropLast(1)
            val root = pageMeasurables.firstOrNull()?.measure(loose)
            val rest = pageMeasurables.drop(1).map { it.measure(loose.copy(minHeight = root?.height ?: 0)) }
            val placeables = listOfNotNull(root) + rest
            val measured = IntArray(placeables.size) { placeables[it].height }
            if (!measured.contentEquals(heights)) heights = measured
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { i, p ->
                    p.placeWithLayer(0, 0, zIndex = i.toFloat()) {
                        val f = i - nav.pos
                        // The page on top slides from the right edge; the one it covers trails behind.
                        translationX = size.width * if (f >= 0f) f else f * SheetTokens.PUSH_PARALLAX
                        alpha = if (abs(f) >= 1f) 0f else 1f
                        translationY = -if (i == nav.depth) state.scroll else 0f
                    }
                }
                handle.place((constraints.maxWidth - handle.width) / 2, 0, zIndex = placeables.size.toFloat())
            }
        }
    }
}

@Composable
private fun SheetHandle(modifier: Modifier) {
    Box(
        modifier
            .padding(top = SheetTokens.HandleTop)
            .size(BarTokens.HandleWidth, BarTokens.HandleHeight)
            .clip(CircleShape)
            .background(KoanTheme.colors.DrawerHandle),
    )
}

/**
 * A small floating card (confirmations, permission prompts, the link menu): sized to its content,
 * side margins, the page dimmed behind it. It rises from the bottom edge with the sheets' spring
 * and closes with a drag down, a tap outside or Back. Never the brand gradient.
 */
@Composable
fun KoanCard(state: SheetState, modifier: Modifier = Modifier, content: @Composable ColumnScope.(interactive: Boolean) -> Unit) {
    val density = LocalDensity.current
    val windowPx = LocalWindowInfo.current.containerSize.height.toFloat()
    // One open detent: the card's own height.
    state.compactCapPx = windowPx
    state.fullCapPx = windowPx
    state.minStepPx = windowPx
    val shown by remember(state) { derivedStateOf { state.open || state.height > 0f } }
    if (!shown) return

    BackHandler { state.animateTo(Detent.Closed) }
    val view = LocalView.current
    val palette = KoanTheme.colors
    var cardPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) { snapshotFlow { cardPx }.collect { state.refit(intArrayOf(it), 0) } }
    val marginPx = with(density) { SheetTokens.CardMargin.toPx() }
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars
    // Above the keyboard when a field on the card has it up. Read where it is used (layout,
    // gestures), so the keyboard's slide moves the card without recomposing it.
    fun bottomPx() = marginPx + maxOf(imeInsets.getBottom(density), navInsets.getBottom(density))
    Box(
        modifier
            .fillMaxSize()
            .drawBehind { drawRect(palette.SheetScrim, alpha = state.progress) }
            .sheetGestures(state, view) { boxHeight -> boxHeight - bottomPx() - state.height },
    ) {
        Layout(
            content = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SheetTokens.CardRadius))
                        .background(palette.Dock)
                        .padding(SheetTokens.CardPadding),
                ) { content(state.open) }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val side = marginPx.toInt()
            val p = measurables.first().measure(
                Constraints(maxWidth = constraints.maxWidth - 2 * side, maxHeight = constraints.maxHeight),
            )
            if (p.height != cardPx) cardPx = p.height
            val bottomPx = bottomPx()
            layout(constraints.maxWidth, constraints.maxHeight) {
                p.placeWithLayer(side, (constraints.maxHeight - bottomPx - p.height).toInt()) {
                    // Fully below the screen edge when closed, at rest at its height.
                    val shownFraction = if (p.height > 0) (state.height / p.height).coerceIn(0f, 1f) else 0f
                    translationY = (1f - shownFraction) * (p.height + bottomPx)
                }
            }
        }
    }
}

/**
 * A sheet's vertical drags (the over-page sheet and the floating card; the drawer has its own in
 * BarGestures, with the same engine): up = toward full, down = toward compact then closed, a list
 * scrolls only in full. Sideways drags are left to the content. A tap above [sheetTop] (given the
 * box's height) closes the sheet.
 */
private fun Modifier.sheetGestures(state: SheetState, view: View, sheetTop: (Float) -> Float): Modifier = pointerInput(state) {
    val fling = SheetTokens.FlingVelocity.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        val outside = down.position.y < sheetTop(size.height.toFloat())
        var total = Offset.Zero
        var start: androidx.compose.ui.input.pointer.PointerInputChange? = null
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed || change.isConsumed) break
            tracker.addPosition(change.uptimeMillis, change.position)
            total += change.positionChange()
            if (total.getDistance() > viewConfiguration.touchSlop) {
                start = change
                break
            }
        }
        if (start == null) {
            if (outside) state.animateTo(Detent.Closed)
            return@awaitEachGesture
        }
        if (abs(total.x) > abs(total.y)) return@awaitEachGesture

        state.grab()
        state.dragBy(start.positionChange().y)
        start.consume()
        var released = false
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    change?.consume()
                    released = true
                    val v = if (change == null) 0f else tracker.calculateVelocity().y
                    if (state.release(v, fling)) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    break
                }
                tracker.addPosition(change.uptimeMillis, change.position)
                state.dragBy(change.positionChange().y)
                change.consume()
            }
        } finally {
            if (!released) state.release(0f, fling)
        }
    }
}
