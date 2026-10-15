package dev.nerzouille.koan.ui.bar

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A tap on the bar that is only ever a tap: the finger lifts before the long-press timeout, never
 * having moved past the touch slop, and no drag on the bar took the gesture. A swipe that starts on
 * a button or on the address pill, even one that springs back, or a long press, never clicks.
 */
@Composable
internal fun Modifier.cleanTap(enabled: Boolean = true, onTap: () -> Unit): Modifier {
    val tap by rememberUpdatedState(onTap)
    val source = remember { MutableInteractionSource() }
    if (!enabled) return this
    return indication(source, LocalIndication.current).pointerInput(source) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val press = PressInteraction.Press(down.position)
            source.tryEmit(press)
            val tapped = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { awaitCleanUp(down.id) } == true
            source.tryEmit(if (tapped) PressInteraction.Release(press) else PressInteraction.Cancel(press))
            if (tapped) tap()
        }
    }
}

/** True when pointer [id] lifts without moving past the slop or being taken by the bar's drags. */
private suspend fun AwaitPointerEventScope.awaitCleanUp(id: PointerId): Boolean {
    var total = Offset.Zero
    while (true) {
        // The Final pass: the bar's drag handler (a parent) has had its say.
        val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == id } ?: return false
        if (change.isConsumed) return false
        if (!change.pressed) return true
        total += change.position - change.previousPosition
        if (total.getDistance() > viewConfiguration.touchSlop) return false
    }
}
