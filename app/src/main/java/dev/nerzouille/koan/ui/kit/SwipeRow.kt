package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.core.animate
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Job
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SettingsTokens
import dev.nerzouille.koan.ui.theme.SwipeTokens
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A library row's swipes (ui/kit/LibraryRow): right to left [onDelete] over red with the bin;
 * left to right [onHide] over grey (Downloads: off the list, the file stays). A side with no
 * action still answers: the row gives a little over grey and springs back. Past
 * [SwipeTokens.COMMIT] of the width (or flung), the row leaves and the action runs; [springBack]
 * brings it back after (the action asks first, like a password's delete). The row's offset is a
 * layer property: dragging never recomposes.
 */
@Composable
fun SwipeRow(
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onHide: (() -> Unit)? = null,
    hideIcon: ImageVector = KoanIcons.Close,
    enabled: Boolean = true,
    springBack: Boolean = false,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit,
) {
    val palette = KoanTheme.colors
    val scope = rememberCoroutineScope()
    // One owner for the offset: the drag writes it, the settle animation writes it, nothing else.
    // Read only in layer / draw lambdas, so dragging never recomposes.
    var offset by remember { mutableFloatStateOf(0f) }
    val gesture = remember { Gesture() }
    var width by remember { mutableIntStateOf(0) }
    val nudge = with(LocalDensity.current) { SwipeTokens.Nudge.toPx() }
    val delete by rememberUpdatedState(onDelete)
    val hide by rememberUpdatedState(onHide)

    fun free(r: Float) = if (r < 0) delete != null else hide != null

    // A side without an action: the drag is damped and never goes past [nudge].
    fun shown(r: Float): Float {
        if (free(r)) return r
        val a = abs(r)
        return nudge * a / (a + nudge) * (if (r < 0) -1f else 1f)
    }

    // The drag that gives [d] on screen (a new drag picks up where the row is).
    fun rawFor(d: Float): Float {
        if (free(d)) return d
        val a = abs(d).coerceAtMost(nudge * 0.99f)
        return nudge * a / (nudge - a) * (if (d < 0) -1f else 1f)
    }

    fun settle(to: Float, then: (suspend () -> Unit)? = null) {
        gesture.settle?.cancel()
        gesture.settle = scope.launch {
            animate(offset, to, animationSpec = if (to == 0f) SwipeTokens.Back else tween(SwipeTokens.OUT_MS)) { v, _ -> offset = v }
            then?.invoke()
        }
    }

    val drag = rememberDraggableState { delta ->
        gesture.raw += delta
        offset = shown(gesture.raw)
    }

    // Selection mode began (or the row was turned off) mid-swipe: back in place.
    LaunchedEffect(enabled) { if (!enabled && offset != 0f) settle(0f) }

    Box(
        modifier
            // A flat row isn't clipped: its pressed highlight reaches past its sides.
            .then(if (shape == RectangleShape) Modifier else Modifier.clip(shape))
            .onSizeChanged { width = it.width },
    ) {
        // Under the row: red + bin at the end (delete), grey at the start (hide or nothing).
        Box(
            Modifier
                .matchParentSize()
                // Only the part the row uncovers: rows on the page background are see-through.
                .drawWithContent { val o = offset; if (o < 0f) clipRect(left = size.width + o) { this@drawWithContent.drawContent() } }
                .background(if (onDelete != null) palette.Destructive else palette.Glass)
                .padding(horizontal = SettingsTokens.Padding),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (onDelete != null) Icon(KoanIcons.Trash, "Delete", tint = palette.OnPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent { val o = offset; if (o > 0f) clipRect(right = o) { this@drawWithContent.drawContent() } }
                .background(palette.Glass)
                .padding(horizontal = SettingsTokens.Padding),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (onHide != null) Icon(hideIcon, "Hide", tint = palette.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
        Box(
            Modifier
                .graphicsLayer { translationX = offset }
                .draggable(
                    drag,
                    Orientation.Horizontal,
                    enabled = enabled,
                    onDragStarted = {
                        gesture.settle?.cancel()
                        gesture.raw = rawFor(offset)
                    },
                    // Also called when the gesture is cancelled: the row always settles.
                    onDragStopped = { velocity ->
                        val r = gesture.raw
                        gesture.raw = 0f
                        val action = if (r < 0) delete else hide
                        val far = abs(r) > width * SwipeTokens.COMMIT
                        val flung = abs(velocity) > SwipeTokens.FLING && (velocity < 0) == (r < 0)
                        if (action != null && r != 0f && width > 0 && (far || flung)) {
                            settle(if (r < 0) -width.toFloat() else width.toFloat()) {
                                action()
                                if (springBack) settle(0f)
                            }
                        } else {
                            settle(0f)
                        }
                    },
                ),
        ) { content() }
    }
}

/** A row's gesture bookkeeping (not state: nothing draws from it). */
private class Gesture {
    var raw = 0f
    var settle: Job? = null
}
