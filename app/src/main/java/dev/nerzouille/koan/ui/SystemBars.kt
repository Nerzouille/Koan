package dev.nerzouille.koan.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/*
 * "Hide status bar" (Settings > Display): the page and the overview take the status bar's row,
 * right up to the camera. Everything else (sheets, settings, sign-in) keeps clear of the camera
 * cutout instead, so it looks the same with or without the status bar.
 */

/** The top of the screen nothing may cover: the status bar when shown, else the camera cutout. */
val WindowInsets.Companion.topSafe: WindowInsets
    @Composable get() = statusBars.union(displayCutout).only(WindowInsetsSides.Top)

/** Padding for [WindowInsets.Companion.topSafe]. */
@Composable
fun Modifier.topSafePadding(): Modifier = windowInsetsPadding(WindowInsets.topSafe)

/**
 * The camera cutout in the top edge while the status bar is hidden (window pixels), so a header
 * that now sits beside it can keep clear of it; null when the status bar shows or there's none.
 */
@Composable
fun topCutoutBeside(): Rect? {
    // Read through Compose's insets so the header follows the status bar hiding and showing.
    if (WindowInsets.statusBars.getTop(LocalDensity.current) > 0) return null
    val insets = ViewCompat.getRootWindowInsets(LocalView.current) ?: return null
    if (insets.isVisible(WindowInsetsCompat.Type.statusBars())) return null
    val cutout = insets.displayCutout ?: return null
    val r = cutout.boundingRects.firstOrNull { it.top == 0 && it.height() > 0 && it.width() > 0 } ?: return null
    return Rect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
}
