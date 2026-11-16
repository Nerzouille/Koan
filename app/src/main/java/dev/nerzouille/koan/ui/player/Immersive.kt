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
    // Never a system scrim behind the bars: not over a video, and not over Koan's own surfaces
    // either (it showed as a pale strip on Private's colours once a video had played).
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
    }
    val bars = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    if (on) {
        if (lightStatusBefore == null) lightStatusBefore = bars.isAppearanceLightStatusBars
        playerBars(window, window.context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
    } else {
        lightStatusBefore?.let { bars.isAppearanceLightStatusBars = it }
        lightStatusBefore = null
    }
    window.setBackgroundDrawable(ColorDrawable(if (on) Color.BLACK else ContextCompat.getColor(window.context, R.color.koan_base)))
}

/** The status bar's icon colour before the player turned it light-on-black (Koan's theme's). */
private var lightStatusBefore: Boolean? = null

/**
 * The system bars over the player: the navigation bar always away. In portrait the status bar
 * stays (white icons on the player's black) and the top row sits under it; in landscape both go
 * (a swipe from an edge shows them for a moment).
 */
fun playerBars(window: Window, portrait: Boolean) {
    val bars = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    bars.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    bars.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
    if (portrait) {
        bars.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        bars.isAppearanceLightStatusBars = false
    } else {
        bars.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
    }
}

/** Both system bars away (swipe from an edge shows them for a moment). */
fun hideBars(window: Window) {
    val bars = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    bars.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    bars.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
}
