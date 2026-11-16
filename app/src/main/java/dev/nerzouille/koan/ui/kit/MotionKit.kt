package dev.nerzouille.koan.ui.kit

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import dev.nerzouille.koan.ui.theme.Motion

/** The system's "remove animations" (animator duration scale 0): no motion, everything just shows. */
fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** [animationsOff], read once for the composable's life. */
@Composable
fun rememberAnimationsOff(): Boolean {
    val context = LocalContext.current
    return remember { animationsOff(context) }
}

/**
 * Calls [onFrame] on every frame with the milliseconds since the previous one, until cancelled.
 * For idle clocks read in layers and draw only (write a float state, never recompose).
 */
suspend fun frameClock(onFrame: (deltaMs: Float) -> Unit): Nothing {
    var last = withFrameNanos { it }
    while (true) {
        withFrameNanos { now ->
            onFrame((now - last) / 1_000_000f)
            last = now
        }
    }
}

/**
 * Shrinks to [to] while pressed and springs back; acts on release inside. Consumes the touch. Put
 * it before the surface's own modifiers so the whole button scales.
 *
 * One touch handler for the button's life: a caller's new [onClick] (a new lambda on each
 * recomposition, e.g. the player's play button as the video plays) no longer restarts it in the
 * middle of a press, which lost the click and left the button pressed. The pressed state is reset
 * however the press ends ([pressing]).
 */
@Composable
fun Modifier.pressScale(enabled: Boolean = true, to: Float = Motion.PRESS_SCALE, onClick: () -> Unit): Modifier {
    var down by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val s by animateFloatAsState(if (down) to else 1f, Motion.Press, label = "press")
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .then(
            if (!enabled) Modifier else Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    val up = pressing({ down = it }) { waitForUpOrCancellation() }
                    if (up != null) {
                        up.consume()
                        click()
                    }
                }
            },
        )
}

/**
 * A press from finger down to its end: [pressed] true while [waitUp] runs, false again however it
 * ends (a lift, a cancel, the gesture torn down by a recomposition or the button going away).
 * Inline, so [waitUp] can wait for pointer events.
 */
inline fun <T> pressing(pressed: (Boolean) -> Unit, waitUp: () -> T?): T? {
    pressed(true)
    try {
        return waitUp()
    } finally {
        pressed(false)
    }
}

/** A full-screen page sliding up from the bottom edge: [progress] 0 (below the screen) … 1 (in place), read in the layer. */
fun Modifier.slideUpPage(progress: () -> Float): Modifier = graphicsLayer { translationY = (1f - progress()) * size.height }
