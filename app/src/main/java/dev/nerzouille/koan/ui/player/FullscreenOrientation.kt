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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/*
 * The screen's orientation in fullscreen (docs/plans/FIXES_PLAN.md F2). A video opens turned to its
 * own shape (a landscape video goes landscape). With the phone's auto-rotate on, once the phone is
 * actually held that way (the user turned it), the screen follows the phone from then on, portrait
 * included (YouTube-style). With auto-rotate off (owner, 2026-11-12), it stays on the video's
 * shape as SENSOR_LANDSCAPE / SENSOR_PORTRAIT, so the two landscape sides flip on their own, and
 * turning the phone to the other shape offers Koan's own rotate pill for a few seconds
 * ([rememberRotateOffer]); a tap turns the player. The system's rotate button can't do both:
 * AOSP's DisplayRotation offers it only for USER_* / FULL_USER requests, which don't flip the
 * landscape sides by themselves under the lock (FULL_USER even goes back to the locked portrait).
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

/**
 * The rotate pill's rule (auto-rotate off), for one reading of how the phone is held ([held]:
 * portrait, landscape, or null in between): true = offer it (the phone just turned to the other
 * shape than the screen's), false = take it away (back to the screen's shape), null = no change.
 */
fun rotateOffer(previous: Boolean?, held: Boolean?, screenPortrait: Boolean): Boolean? = when {
    held == null -> null
    held == screenPortrait -> false
    held != previous -> true
    else -> null
}

/**
 * Whether Koan's rotate pill shows: while [active] (fullscreen, auto-rotate off, not in PiP), from
 * the moment the phone turns to the other shape than the screen's ([screenPortrait]) for
 * [PlayerTokens.ROTATE_OFFER_MS], or until it turns back. It sits where the fullscreen button is. How the phone is held when this starts
 * never offers it (only a turn does).
 */
@Composable
fun rememberRotateOffer(active: Boolean, screenPortrait: Boolean): Boolean {
    val context = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    var serial by remember { mutableIntStateOf(0) }
    DisposableEffect(active, screenPortrait) {
        shown = false
        if (!active) return@DisposableEffect onDispose { }
        var first = true
        var previous: Boolean? = null
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                val held = heldPortrait(degrees) ?: return
                if (first) {
                    first = false
                    previous = held
                    return
                }
                when (rotateOffer(previous, held, screenPortrait)) {
                    true -> {
                        shown = true
                        serial++
                    }
                    false -> shown = false
                    null -> Unit
                }
                previous = held
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    LaunchedEffect(serial) {
        if (serial == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(PlayerTokens.ROTATE_OFFER_MS)
        shown = false
    }
    return shown
}
