package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import dev.nerzouille.koan.ui.theme.BrandSpinner
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PullTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Pull to refresh: [pull] is how far the page (or the circle) came down (px, read in layers only);
 * past [triggerPx] a release reloads, and it rests at [restPx] until the page has loaded
 * ([refreshing]). Its phases follow [PullRules]: a pull can always start again once the load is
 * over, even while it is still settling back, and nothing but the finger ends a pull.
 */
@Stable
class PullRefresh(private val scope: CoroutineScope) {
    val pull = Animatable(0f)
    var phase by mutableStateOf(PullPhase.Idle)
        private set
    val refreshing: Boolean get() = phase == PullPhase.Refreshing
    var triggerPx = 0f
    var maxPx = 0f

    /** Where it rests while the page reloads (the trigger, for the circle). */
    var restPx = 0f

    /** The page itself slides (the prototypes): a smooth way back, no bounce. */
    var slides = false

    /** The finger went past the trigger (for one tick). */
    internal var armed = false

    /** "Remove animations": the page or the circle jumps back instead of sliding. */
    var still = false

    /** Where the pull was when the finger took it (it may be settling back): the drag adds to it. */
    internal var grabbedAt = 0f
        private set
    private var settleJob: Job? = null

    private fun on(event: PullEvent) {
        val was = phase
        phase = PullRules.next(phase, event)
        if (phase != was) pullLog("$was → $phase ($event)")
    }

    /** A finger took it: from where it is, even on its way back. */
    internal fun grab() {
        settleJob?.cancel()
        on(PullEvent.Grab)
        grabbedAt = pull.value
        armed = grabbedAt >= triggerPx
    }

    internal fun dragTo(px: Float) {
        val to = px.coerceIn(0f, maxPx)
        scope.launch { pull.snapTo(to) }
    }

    /** The finger lifted: reload (true) or go back up. */
    internal fun release(): Boolean {
        val go = pull.value >= triggerPx
        armed = false
        on(if (go) PullEvent.ReleasePast else PullEvent.ReleaseShort)
        settle(if (go) restPx else 0f)
        return go
    }

    /** A second finger: back up, no reload. */
    internal fun cancel() {
        armed = false
        on(PullEvent.Cancel)
        settle(0f)
    }

    /** The page finished loading (or another tab came up): it goes back up. Never under a finger. */
    fun done() {
        if (phase == PullPhase.Pulling) return
        armed = false
        if (phase == PullPhase.Refreshing) on(PullEvent.Loaded)
        if (phase == PullPhase.Idle && pull.value == 0f) return
        if (phase == PullPhase.Idle) phase = PullPhase.Settling
        settle(0f)
    }

    private fun settle(to: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            when {
                still -> pull.snapTo(to)
                slides -> pull.animateTo(to, PullTokens.SlideBack)
                else -> pull.animateTo(to, Motion.DockSettle)
            }
            if (to == 0f) on(PullEvent.Settled)
        }
    }
}

/** Dev, debug and staging builds: each pull attempt and phase, in logcat (tag KoanPull). */
internal fun pullLog(message: String) {
    if (dev.nerzouille.koan.ui.overview.StackLab.available) android.util.Log.i("KoanPull", message)
}

/**
 * The prototypes' pull: [dy] px of finger travel → page travel, 1 : 1 up to [PullTokens.SLIDE_KNEE]
 * of [maxPx], then a rubber band that never passes [maxPx] (same speed at the knee, no jolt).
 */
fun rubberBand(dy: Float, maxPx: Float): Float {
    val knee = maxPx * PullTokens.SLIDE_KNEE
    val room = maxPx - knee
    return when {
        dy <= 0f || maxPx <= 0f -> 0f
        dy <= knee -> dy
        else -> knee + room * (1f - kotlin.math.exp(-(dy - knee) / room))
    }
}

/**
 * Pull to refresh on the page. A touch that starts with the page at its top ([atTop], read when the
 * finger goes down) becomes a pull once it moves down past the slop, unless the page keeps it
 * ([pageTouch]: maps, canvases, pages with their own pull); while the page's engine hasn't answered
 * yet it waits a moment for it ([PullRules.start]). From then on the pull owns the gesture until the
 * finger lifts: its moves are consumed (the page gets a cancel), and no scroll, load or touch
 * report from the page can end it; only a second finger does. Each attempt is logged ([pullLog]).
 */
fun Modifier.pullToRefresh(
    state: PullRefresh,
    atTop: () -> Boolean,
    pageTouch: () -> PageTouch,
    onRefresh: () -> Unit,
    onTick: () -> Unit,
): Modifier =
    pointerInput(state) {
        val slop = viewConfiguration.touchSlop
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed && !it.previousPressed } ?: continue
                val top = atTop()
                if (!PullRules.canGrab(state.phase) || !top) {
                    pullLog("down: no pull (phase ${state.phase}, at top $top)")
                    continue
                }
                if (awaitPull(state, down, slop, top, pageTouch)) followPull(state, down, onRefresh, onTick)
            }
        }
    }

/** Until the touch is a pull (true) or clearly isn't (false, logged). */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPull(
    state: PullRefresh,
    down: PointerInputChange,
    slop: Float,
    atTop: Boolean,
    pageTouch: () -> PageTouch,
): Boolean {
    var decision: PullStart = PullStart.Wait
    while (decision == PullStart.Wait) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed || event.changes.count { it.pressed } > 1) {
            decision = PullStart.Reject("lifted or a second finger before it moved")
            pullLog("pull: ${decision.reason}")
            break
        }
        val touch = pageTouch()
        val waited = change.uptimeMillis - down.uptimeMillis
        val dx = change.position.x - down.position.x
        val dy = change.position.y - down.position.y
        decision = PullRules.start(state.phase, atTop, dx, dy, slop, touch, waited)
        val taken = if (change.isConsumed) ", its moves already taken by the page" else ""
        when (decision) {
            PullStart.Start -> pullLog("pull: start (page $touch after ${waited}ms, at top $atTop)")
            is PullStart.Reject -> pullLog("pull: rejected, ${decision.reason} (page $touch after ${waited}ms$taken)")
            PullStart.Wait -> Unit
        }
    }
    return decision == PullStart.Start
}

/** The pull follows the finger, 1 : 1 from where it went down, until it lifts. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.followPull(
    state: PullRefresh,
    down: PointerInputChange,
    onRefresh: () -> Unit,
    onTick: () -> Unit,
) {
    state.grab()
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.count { it.pressed } > 1) {
            pullLog("pull: second finger, cancelled")
            state.cancel()
            return
        }
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change == null || !change.pressed) {
            val go = state.release()
            pullLog("pull: released at ${state.pull.value.toInt()}px, ${if (go) "reload" else "back up"}")
            if (go) onRefresh()
            return
        }
        // Koan's from here: the page gets a cancel and none of these moves.
        event.changes.forEach { it.consume() }
        val dy = (change.position.y - down.position.y).coerceAtLeast(0f)
        val px = if (state.slides) rubberBand(state.grabbedAt + dy, state.maxPx) else state.grabbedAt + dy * PullTokens.RESISTANCE
        state.dragTo(px)
        val past = px >= state.triggerPx
        if (past != state.armed) {
            state.armed = past
            if (past) onTick()
        }
    }
}

/** The small circle under the top edge: a quiet arc while pulling, the brand gradient turning while it reloads. */
@Composable
fun PullIndicator(state: PullRefresh, modifier: Modifier = Modifier) {
    val palette = KoanTheme.colors
    Box(
        modifier
            .graphicsLayer {
                val p = state.pull.value
                translationY = p - size.height
                alpha = (p / (state.triggerPx * 0.6f)).coerceIn(0f, 1f)
                rotationZ = if (state.refreshing) 0f else p / state.triggerPx * 180f
            }
            .shadow(PullTokens.Elevation, CircleShape)
            .clip(CircleShape)
            .background(palette.Dock)
            .size(PullTokens.Size),
        contentAlignment = Alignment.Center,
    ) {
        if (state.refreshing) {
            BrandSpinner(PullTokens.Arc)
        } else {
            Box(
                Modifier.size(PullTokens.Arc).drawBehind {
                    val stroke = PullTokens.Stroke.toPx()
                    val f = (state.pull.value / state.triggerPx).coerceIn(0f, 1f)
                    drawArc(
                        color = if (f >= 1f) palette.TextPrimary else palette.TextSecondary,
                        startAngle = -90f,
                        sweepAngle = 300f * f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                },
            )
        }
    }
}
