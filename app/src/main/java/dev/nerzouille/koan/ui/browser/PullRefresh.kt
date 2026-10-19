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
import androidx.compose.ui.input.pointer.pointerInput
import dev.nerzouille.koan.ui.theme.BrandSpinner
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PullTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pull to refresh: [pull] is how far the indicator came down (px, read in layers only); past
 * [triggerPx] a release reloads, and the indicator turns with the brand gradient until the page
 * has loaded ([refreshing]).
 */
@Stable
class PullRefresh(private val scope: CoroutineScope) {
    val pull = Animatable(0f)
    var refreshing by mutableStateOf(false)
        private set
    var triggerPx = 0f
    var maxPx = 0f

    /** The finger went past the trigger (for one tick). */
    internal var armed = false

    internal fun dragTo(px: Float) {
        scope.launch { pull.snapTo(px.coerceIn(0f, maxPx)) }
    }

    /** The finger lifted: reload (true) or go back up. */
    internal fun release(): Boolean {
        val go = pull.value >= triggerPx
        armed = false
        if (go) {
            refreshing = true
            scope.launch { pull.animateTo(triggerPx, Motion.DockSettle) }
        } else {
            scope.launch { pull.animateTo(0f, Motion.DockSettle) }
        }
        return go
    }

    /** The page finished loading (or another tab came up): the indicator goes. */
    fun done() {
        if (!refreshing && pull.value == 0f) return
        refreshing = false
        armed = false
        scope.launch { pull.animateTo(0f, Motion.DockSettle) }
    }
}

/**
 * Watches (never consumes) the fingers on the page: a pull down that starts while the page is at
 * its top ([atTop]) and that the page doesn't keep for itself ([pageKeepsTouch]: maps, canvases,
 * pages with their own pull) moves the indicator. A second finger, a sideways start or the page
 * scrolling cancels it.
 */
fun Modifier.pullToRefresh(
    state: PullRefresh,
    atTop: () -> Boolean,
    pageKeepsTouch: () -> Boolean,
    onRefresh: () -> Unit,
    onTick: () -> Unit,
): Modifier =
    pointerInput(state) {
        val slop = viewConfiguration.touchSlop
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed && !it.previousPressed } ?: continue
                if (state.refreshing || !atTop()) continue
                var pulling = false
                var decided = false
                var startY = down.position.y
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } > 1) {
                        if (pulling) state.done()
                        break
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (pulling && state.release()) onRefresh()
                        break
                    }
                    val dx = change.position.x - down.position.x
                    val dy = change.position.y - startY
                    if (!decided) {
                        if (abs(dx) < slop && abs(dy) < slop) continue
                        decided = true
                        pulling = dy > 0 && dy > abs(dx) && !pageKeepsTouch()
                        if (!pulling) break
                        startY = change.position.y
                        continue
                    }
                    // The page scrolled after all (it took the gesture): let it go.
                    if (pageKeepsTouch() || !atTop()) {
                        state.done()
                        pulling = false
                        break
                    }
                    state.dragTo(dy * PullTokens.RESISTANCE)
                    val past = state.pull.value >= state.triggerPx
                    if (past != state.armed) {
                        state.armed = past
                        if (past) onTick()
                    }
                }
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
