package dev.nerzouille.koan.ui.kit

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

/** A header pulled down past [threshold] dismisses its page, once per drag. */
fun Modifier.swipeDownToDismiss(threshold: Dp, onDismiss: () -> Unit): Modifier = pointerInput(Unit) {
    var total = 0f
    detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
        total += dy
        if (total > threshold.toPx()) {
            total = Float.NEGATIVE_INFINITY
            onDismiss()
        }
        change.consume()
    }
}
