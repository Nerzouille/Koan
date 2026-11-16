package dev.nerzouille.koan.ui.kit

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Koan's haptic vocabulary, over View.performHapticFeedback: one place to tune or mute it. */
object KoanHaptics {
    /** A committed step, a toggle, a level end. */
    fun tick(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    /** A long press landed. */
    fun hold(view: View) = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

    /** Something was closed or deleted. */
    fun confirm(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    /** Crossing a slot while dragging (a segment tick on Android 14+). */
    fun notch(view: View) = view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )
}
