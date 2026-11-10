package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
    val offset = remember { Animatable(0f) }
    var raw by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    val nudge = with(LocalDensity.current) { SwipeTokens.Nudge.toPx() }
    val delete by rememberUpdatedState(onDelete)
    val hide by rememberUpdatedState(onHide)

    // A side without an action: the drag is damped and never goes past [nudge].
    fun shown(r: Float): Float {
        val free = if (r < 0) delete != null else hide != null
        if (free) return r
        val a = abs(r)
        return (nudge * a / (a + nudge)) * (if (r < 0) -1f else 1f)
    }

    val drag = rememberDraggableState { delta ->
        raw += delta
        val target = shown(raw)
        scope.launch { offset.snapTo(target) }
    }

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
                .drawWithContent { val o = offset.value; if (o < 0f) clipRect(left = size.width + o) { this@drawWithContent.drawContent() } }
                .background(if (onDelete != null) palette.Destructive else palette.Glass)
                .padding(horizontal = SettingsTokens.Padding),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (onDelete != null) Icon(KoanIcons.Trash, "Delete", tint = palette.OnPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent { val o = offset.value; if (o > 0f) clipRect(right = o) { this@drawWithContent.drawContent() } }
                .background(palette.Glass)
                .padding(horizontal = SettingsTokens.Padding),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (onHide != null) Icon(hideIcon, "Hide", tint = palette.TextPrimary, modifier = Modifier.size(SettingsTokens.RowGlyph))
        }
        Box(
            Modifier
                .graphicsLayer { translationX = offset.value }
                .draggable(
                    drag,
                    Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = { velocity ->
                        val r = raw
                        raw = 0f
                        val action = if (r < 0) delete else hide
                        val far = abs(r) > width * SwipeTokens.COMMIT
                        val flung = abs(velocity) > SwipeTokens.FLING && (velocity < 0) == (r < 0)
                        if (action != null && r != 0f && (far || flung)) {
                            offset.animateTo(if (r < 0) -width.toFloat() else width.toFloat(), tween(SwipeTokens.OUT_MS))
                            action()
                            if (springBack) offset.animateTo(0f, SwipeTokens.Back)
                        } else {
                            offset.animateTo(0f, SwipeTokens.Back)
                        }
                    },
                ),
        ) { content() }
    }
}
