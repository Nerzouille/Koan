package dev.nerzouille.koan.ui.kit

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext

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
