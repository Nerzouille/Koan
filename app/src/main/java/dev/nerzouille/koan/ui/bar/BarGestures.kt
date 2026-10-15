package dev.nerzouille.koan.ui.bar

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.ui.OverviewMotion
import dev.nerzouille.koan.ui.browser.TabSwipe
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlin.math.abs

/**
 * Everything a drag on the bar can move. One rule everywhere: up = one level deeper, down = one
 * level back (page → overview → drawer compact → drawer full), sideways = tabs in the page view,
 * spaces in the overview; in the page view a long press scrubs across the spaces. The callbacks are set by the
 * caller on every composition.
 */
@Stable
class BarGestures(
    val pager: SpacePager,
    val drawer: DrawerState,
    val overview: OverviewMotion,
    val tabSwipe: TabSwipe,
    /** Page view: a long press on the dock scrubs across the spaces until the finger lifts. */
    val scrub: SpaceScrub,
) {
    var overviewOpen: () -> Boolean = { false }

    /** Page → overview: open it under the finger (its progress follows the drag). */
    var onOverviewDragStart: () -> Unit = {}

    /** A page ↔ overview drag let go: true = the overview stays, false = back to the page. */
    var onOverviewSettle: (Boolean) -> Unit = {}

    /** Page view: the current tab's stack (front tab of each card, oldest first) and its index in it. */
    var tabStrip: () -> Pair<List<TabEntity>, Int>? = { null }
    var onTabSwipe: (TabEntity) -> Unit = {}

    /** Page view: a scrub let go on another space (its index). */
    var onSpaceSwipe: (Int) -> Unit = {}
}

private enum class Drag { Scrub, Space, Tab, OverviewOpen, OverviewClose, Drawer }

/** The finger passed the touch slop: the change that did, and the total travel so far. */
private class Slop(val change: PointerInputChange, val total: Offset) {
    val horizontal: Boolean get() = abs(total.x) > abs(total.y)
}

/** Wraps [Slop] so a timeout (null) is told apart from a lift ([slop] null). */
private class SlopWait(val slop: Slop?)

/**
 * The bar's drags, for one region of it: the address row (always), the switcher row and the
 * drawer ([drawerArea]: a tap above the visible sheet closes it) in the overview.
 */
fun Modifier.barGestures(g: BarGestures, view: View, drawerArea: Boolean = false): Modifier = pointerInput(g, drawerArea) {
    val pagerFling = PagerTokens.FlingVelocity.toPx()
    val drawerFling = DrawerTokens.FlingVelocity.toPx()
    val overviewFling = BarTokens.OverviewFling.toPx()
    // One light tick per committed step, on release (and when space mode starts).
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        val inOverview = g.overviewOpen()
        val outside = drawerArea && down.position.y < g.drawer.sheetTop()

        var scrubbing = false
        val start: Slop? = if (inOverview) {
            awaitSlop(down.id, tracker, PointerEventPass.Main)
        } else {
            val first = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                SlopWait(awaitSlop(down.id, tracker, PointerEventPass.Main))
            }
            if (first != null) {
                first.slop
            } else {
                // Held still: space mode, a scrubber across all spaces, from the first move on.
                scrubbing = true
                g.scrub.begin(g.pager.settled, g.pager.count)
                tick()
                null
            }
        }
        if (start == null && !scrubbing) {
            if (outside) g.drawer.animateTo(Detent.Closed)
            return@awaitEachGesture
        }

        val drawerShown = g.drawer.open || g.drawer.height > 0f
        val kind = when {
            scrubbing || start == null -> Drag.Scrub
            start.horizontal -> if (inOverview) Drag.Space else Drag.Tab
            inOverview && drawerShown -> Drag.Drawer
            inOverview -> if (start.total.y > 0f) Drag.OverviewClose else Drag.Drawer
            start.total.y < 0f -> Drag.OverviewOpen
            // Down on the page's dock: nothing to go back to.
            else -> return@awaitEachGesture
        }
        when (kind) {
            Drag.Scrub -> Unit
            Drag.Space -> g.pager.grab()
            Drag.Tab -> if (!g.tabSwipe.grab(g.tabStrip)) return@awaitEachGesture
            Drag.OverviewOpen -> {
                g.overview.grab(open = false)
                g.onOverviewDragStart()
            }
            Drag.OverviewClose -> g.overview.grab(open = true)
            Drag.Drawer -> g.drawer.grab()
        }

        fun drag(delta: Offset) {
            when (kind) {
                Drag.Scrub -> if (g.scrub.dragBy(delta.x)) tick()
                Drag.Space -> g.pager.dragBy(delta.x)
                Drag.Tab -> g.tabSwipe.dragBy(delta.x)
                Drag.OverviewOpen, Drag.OverviewClose -> g.overview.dragBy(delta.y)
                Drag.Drawer -> g.drawer.dragBy(delta.y)
            }
        }

        var released = false
        fun release(v: Velocity) {
            released = true
            when (kind) {
                Drag.Scrub -> g.scrub.end()?.let(g.onSpaceSwipe)
                Drag.Space -> if (g.pager.release(v.x, pagerFling)) tick()
                Drag.Tab -> if (g.tabSwipe.release(v.x, pagerFling, g.onTabSwipe)) tick()
                Drag.OverviewOpen, Drag.OverviewClose -> {
                    val open = g.overview.release(v.y, overviewFling)
                    if (open != (kind == Drag.OverviewClose)) tick()
                    g.onOverviewSettle(open)
                }
                Drag.Drawer -> if (g.drawer.release(v.y, drawerFling)) tick()
            }
        }

        // While scrubbing, events are read (and consumed) before the pill's own tap sees them.
        val pass = if (kind == Drag.Scrub) PointerEventPass.Initial else PointerEventPass.Main
        try {
            if (start != null) {
                drag(start.change.positionChange())
                start.change.consume()
            }
            while (true) {
                val change = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    change?.consume()
                    release(if (change == null) Velocity.Zero else tracker.calculateVelocity())
                    break
                }
                tracker.addPosition(change.uptimeMillis, change.position)
                drag(change.positionChange())
                change.consume()
            }
        } finally {
            // The gesture was cancelled (the bar changed under it): never leave a state mid-drag.
            if (!released) release(Velocity.Zero)
        }
    }
}

/** Waits for pointer [id] to pass the touch slop. Null when it lifts (or is taken) first. */
private suspend fun AwaitPointerEventScope.awaitSlop(id: PointerId, tracker: VelocityTracker, pass: PointerEventPass): Slop? {
    var total = Offset.Zero
    while (true) {
        val change = awaitPointerEvent(pass).changes.firstOrNull { it.id == id } ?: return null
        if (!change.pressed || change.isConsumed) return null
        tracker.addPosition(change.uptimeMillis, change.position)
        total += change.positionChange()
        if (total.getDistance() > viewConfiguration.touchSlop) return Slop(change, total)
    }
}
