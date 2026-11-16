package dev.nerzouille.koan.ui.browser

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.delay

/*
 * After a fullscreen video turns the screen (or turns it back), every layer must end at the
 * display's shape: the window, the root view, Compose and Gecko's surface. The owner saw Koan
 * left laid out at landscape size on a portrait screen (the page and the bar in the top ~40 %,
 * cut off on the right, the rest black, nothing responding until a kill), and a player kept at
 * the size from before the rotation. Not reproduced through adb (it needs the phone to turn), so
 * this watches the sizes after each change, logs them (tag "KoanSize") and repairs, step by step.
 */

/** A width and height in px. */
data class Px(val w: Int, val h: Int) {
    val landscape get() = w > h
    override fun toString() = "${w}x$h"
}

/** What to do about the sizes after a fullscreen change. */
enum class LayoutFix { None, Relayout, Reapply, Recreate }

/**
 * Whether a layer ([layers]: its size) kept another shape than the display ([display]): wider than
 * tall on a portrait display, or the reverse. Sizes not known yet (0) don't count, and neither do
 * square ones.
 */
fun staleLayers(display: Px, layers: Map<String, Px>): List<String> {
    if (display.w <= 0 || display.h <= 0 || display.w == display.h) return emptyList()
    return layers.filter { (_, s) -> s.w > 0 && s.h > 0 && s.w != s.h && s.landscape != display.landscape }.keys.toList()
}

/**
 * The repair for check number [attempt] (0 = the first one, once the change should have landed):
 * a relayout first, then the window's attributes and the orientation applied again, then (still
 * stale, and only after leaving fullscreen: [leaving]) the activity made again, which keeps the
 * tabs and the place like a kill would. Entering, a new activity would drop the video.
 */
fun layoutFix(stale: Boolean, attempt: Int, leaving: Boolean): LayoutFix = when {
    !stale -> LayoutFix.None
    attempt <= 0 -> LayoutFix.Relayout
    attempt == 1 || !leaving -> LayoutFix.Reapply
    else -> LayoutFix.Recreate
}

/** The display's size, the window's and the layers Koan draws in. */
private fun sizes(activity: Activity): Pair<Px, Map<String, Px>> {
    val wm = activity.windowManager
    val max = wm.maximumWindowMetrics.bounds
    val now = wm.currentWindowMetrics.bounds
    val decor = activity.window.decorView
    val layers = linkedMapOf("window" to Px(now.width(), now.height()), "decor" to Px(decor.width, decor.height))
    val content = activity.findViewById<ViewGroup>(android.R.id.content)
    content?.getChildAt(0)?.let { compose ->
        val inner = (compose as? ViewGroup)?.getChildAt(0) ?: compose
        layers["compose"] = Px(inner.width, inner.height)
    }
    firstSurface(decor)?.let { layers["gecko"] = Px(it.width, it.height) }
    return Px(max.width(), max.height()) to layers
}

private fun firstSurface(view: View): View? {
    if (view is SurfaceView && view.isShown) return view
    if (view !is ViewGroup) return null
    for (i in 0 until view.childCount) firstSurface(view.getChildAt(i))?.let { return it }
    return null
}

/**
 * After a fullscreen change ([fullScreen]: its new state): waits for the configuration to match
 * what Koan asked for (portrait after leaving), applies the orientation again and forces a relayout
 * once (leaving), then checks the sizes a few times and repairs what stayed behind.
 */
suspend fun watchLayout(activity: Activity, fullScreen: Boolean, orientation: () -> Int) {
    if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) return
    if (!fullScreen) {
        // Leaving: once the configuration is portrait again, the orientation once more and a relayout.
        repeat(40) {
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) return@repeat
            delay(50)
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity.window.decorView.requestLayout()
    }
    var attempt = 0
    for (wait in CHECKS_MS) {
        delay(wait)
        if (activity.isFinishing || activity.isDestroyed || activity.isInPictureInPictureMode) return
        val (display, layers) = sizes(activity)
        val stale = staleLayers(display, layers)
        val fix = layoutFix(stale.isNotEmpty(), attempt, leaving = !fullScreen)
        if (fix == LayoutFix.None) {
            if (attempt > 0) Log.i(TAG, "fixed after $attempt step(s): display=$display $layers")
            return
        }
        Log.w(TAG, "stale ${stale.joinToString()} (fullscreen=$fullScreen, step=$fix): display=$display $layers")
        when (fix) {
            LayoutFix.Relayout -> activity.window.decorView.let { it.requestLayout(); it.invalidate() }
            LayoutFix.Reapply -> {
                activity.window.attributes = activity.window.attributes
                activity.requestedOrientation = orientation()
                activity.window.decorView.requestLayout()
            }
            LayoutFix.Recreate -> {
                activity.recreate()
                return
            }
            LayoutFix.None -> Unit
        }
        attempt++
    }
}

/** Between the checks (ms): the first once a rotation has usually landed, then slower. */
private val CHECKS_MS = listOf(400L, 400L, 700L, 1200L)

private const val TAG = "KoanSize"
