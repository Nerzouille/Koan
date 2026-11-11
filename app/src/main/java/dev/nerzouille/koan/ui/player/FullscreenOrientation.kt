package dev.nerzouille.koan.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/*
 * The screen's orientation in fullscreen (docs/plans/FIXES_PLAN.md F2). A video opens turned to its
 * own shape (a landscape video goes landscape). With the phone's auto-rotate on, once the phone is
 * actually held that way (the user turned it), the screen follows the phone from then on, portrait
 * included (YouTube-style). With auto-rotate off, it stays on the video's shape, as before.
 */

/** What to ask the activity for. [turned]: the phone has been held in the video's shape since it opened. */
fun fullscreenOrientation(fullscreen: Boolean, videoPortrait: Boolean, rotationLocked: Boolean, turned: Boolean): Int = when {
    !fullscreen -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    !rotationLocked && turned -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
    videoPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

/** Whether a sensor reading ([degrees], 0 = upright) is the phone held in portrait (true), landscape (false), or in between (null). */
fun heldPortrait(degrees: Int): Boolean? = when {
    degrees == OrientationEventListener.ORIENTATION_UNKNOWN -> null
    degrees <= HELD_MARGIN || degrees >= 360 - HELD_MARGIN || degrees in 180 - HELD_MARGIN..180 + HELD_MARGIN -> true
    degrees in 90 - HELD_MARGIN..90 + HELD_MARGIN || degrees in 270 - HELD_MARGIN..270 + HELD_MARGIN -> false
    else -> null
}

private const val HELD_MARGIN = 25

/** The phone's auto-rotate is off (Settings.System.ACCELEROMETER_ROTATION), kept up to date. */
@Composable
fun rememberRotationLocked(): Boolean {
    val context = LocalContext.current
    var locked by remember { mutableStateOf(rotationLocked(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                locked = rotationLocked(context)
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return locked
}

private fun rotationLocked(context: Context): Boolean =
    runCatching { Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0 }.getOrDefault(false)

/**
 * True once the phone has been held [portrait] (or landscape) since [active] last turned on: the
 * listener runs only while waiting, and stops at once after.
 */
@Composable
fun rememberTurned(active: Boolean, portrait: Boolean): Boolean {
    val context = LocalContext.current
    var turned by remember { mutableStateOf(false) }
    DisposableEffect(active, portrait) {
        turned = false
        if (!active) return@DisposableEffect onDispose { }
        var listener: OrientationEventListener? = null
        listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                if (heldPortrait(degrees) == portrait) {
                    turned = true
                    listener?.disable()
                }
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    return turned
}
