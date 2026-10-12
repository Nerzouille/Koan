package dev.nerzouille.koan.ui

import android.app.Activity

/**
 * Ask for the display's highest refresh rate at the current resolution. HyperOS otherwise tends to
 * cap unknown apps at 60 Hz.
 */
fun Activity.requestMaxRefreshRate() {
    val display = display ?: return
    val current = display.mode
    val best = display.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate } ?: return
    window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
}
