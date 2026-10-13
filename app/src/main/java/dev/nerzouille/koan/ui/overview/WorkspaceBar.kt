package dev.nerzouille.koan.ui.overview

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pager across spaces. [pos] is fractional while dragging; [settled] only changes when a switch
 * lands, so neighbour pages are composed before the drag starts and never mid-gesture.
 */
@Stable
class SpacePager(initial: Int, private val scope: CoroutineScope) {
    var pos by mutableFloatStateOf(initial.toFloat())
        private set
    var settled by mutableIntStateOf(initial)
        private set
    var count = 1
    var pageWidthPx = 1f
    private var job: Job? = null

    fun stop() {
        job?.cancel()
    }

    /** 1:1 with the finger; never more than one page away from the settled one. */
    fun dragBy(dxPx: Float) {
        val lo = (settled - 1).coerceAtLeast(0).toFloat()
        val hi = (settled + 1).coerceAtMost(count - 1).toFloat()
        val next = pos - dxPx / pageWidthPx
        // Rubber band at the first/last space.
        pos = when {
            next < lo -> lo - (lo - next) * 0.3f
            next > hi -> hi + (next - hi) * 0.3f
            else -> next
        }
    }

    fun fling(vxPx: Float) {
        val velocity = -vxPx / pageWidthPx
        val target = when {
            velocity > 0.6f -> kotlin.math.floor(pos) + 1
            velocity < -0.6f -> kotlin.math.ceil(pos) - 1
            else -> pos.roundToInt().toFloat()
        }.coerceIn((settled - 1).toFloat(), (settled + 1).toFloat()).coerceIn(0f, (count - 1).toFloat())
        animateTo(target.roundToInt(), velocity)
    }

    fun animateTo(index: Int, velocity: Float = 0f) {
        job?.cancel()
        job = scope.launch {
            animate(pos, index.toFloat(), velocity, spring(dampingRatio = 0.86f, stiffness = 300f)) { v, _ -> pos = v }
            settled = index
        }
    }
}

private val LabelSpacing = 116.dp

/** The dock: current space centred, neighbours faded at the sides, page dots. The only place that switches spaces. */
@Composable
fun WorkspaceBar(spaces: List<SpaceModel>, pager: SpacePager, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .pointerInput(pager) {
                val tracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = { pager.stop(); tracker.resetTracking() },
                    onDragEnd = { pager.fling(tracker.calculateVelocity().x) },
                    onDragCancel = { pager.fling(0f) },
                ) { change, dx ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                    pager.dragBy(dx)
                }
            }
            .pointerInput(pager) {
                detectTapGestures { p ->
                    val steps = ((p.x - size.width / 2f) / LabelSpacing.toPx()).roundToInt()
                    val target = (pager.settled + steps).coerceIn(0, pager.count - 1)
                    if (target != pager.settled) pager.animateTo(target)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            KoanIcons.Back, null, tint = KoanColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp).size(16.dp),
        )
        Icon(
            KoanIcons.Forward, null, tint = KoanColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(16.dp),
        )
        spaces.forEachIndexed { i, space ->
            Row(
                Modifier
                    .padding(bottom = 8.dp)
                    .graphicsLayer {
                        val f = i - pager.pos
                        translationX = f * LabelSpacing.toPx()
                        val far = abs(f)
                        alpha = if (far > 1.6f) 0f else (1f - far * 0.6f).coerceAtLeast(0f)
                        val s = 1f - 0.14f * far.coerceAtMost(1f)
                        scaleX = s
                        scaleY = s
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(space.icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(space.name, color = KoanColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        PageDots(count = spaces.size, pos = { pager.pos }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp))
    }
}

@Composable
private fun PageDots(count: Int, pos: () -> Float, modifier: Modifier = Modifier) {
    val dot = 5.dp
    val gap = 5.dp
    Canvas(modifier.size(width = (dot + gap) * count + 10.dp, height = dot)) {
        val d = dot.toPx()
        val g = gap.toPx()
        val p = pos()
        var x = 0f
        for (i in 0 until count) {
            val near = (1f - abs(i - p)).coerceIn(0f, 1f)
            val w = d + near * d * 1.6f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f + 0.6f * near),
                topLeft = Offset(x, 0f),
                size = Size(w, d),
                cornerRadius = CornerRadius(d / 2f),
            )
            x += w + g
        }
    }
}
