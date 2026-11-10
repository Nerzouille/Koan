package dev.nerzouille.koan.ui.player

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import dev.nerzouille.koan.R

/**
 * A video edge to edge (the player, web and standalone): the window draws into the display
 * cutout on every side and is black behind everything, so no band of Koan's background shows
 * where the system bars were. [on] = false: back to Koan's own window.
 */
fun immersive(window: Window, on: Boolean) {
    window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = if (on) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    // No grey scrim behind the (transient) bars over a video.
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = !on
        window.isStatusBarContrastEnforced = !on
    }
    if (on) hideBars(window)
    window.setBackgroundDrawable(ColorDrawable(if (on) Color.BLACK else ContextCompat.getColor(window.context, R.color.koan_base)))
}

/** Both system bars away (swipe from an edge shows them for a moment). */
fun hideBars(window: Window) {
    val bars = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    bars.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    bars.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
}
