package dev.nerzouille.koan.ui.bar

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The keyboard's own slide, frame by frame (the system's WindowInsetsAnimation, through Compose's
 * insets): what the dock rises with and what drives the search morph. Read in draw and layer
 * lambdas only: the insets change every frame of the slide.
 */
@Stable
class KeyboardSlide internal constructor(
    private val ime: WindowInsets,
    private val target: WindowInsets,
    private val density: Density,
) {
    /** The keyboard's full height, kept for its slide down (where the target is 0). */
    internal var lastFullPx = 0

    /** How far up the keyboard is now (px). */
    fun nowPx(): Int = ime.getBottom(density)

    /** The keyboard's full height (px): where it is heading, or where it was; 0 before any keyboard. */
    fun fullPx(): Int = target.getBottom(density).takeIf { it > 0 } ?: lastFullPx

    /** 0 the keyboard is down … 1 it is fully up. */
    fun fraction(): Float = keyboardFraction(nowPx(), fullPx())

    /** How far the dock sits above where it rests without a keyboard (px): see [keyboardRise]. */
    fun rise(navPx: Int): Float = keyboardRise(nowPx(), fullPx(), navPx)
}

/** 0 the keyboard is down … 1 it is fully up ([fullPx] 0: no keyboard known, 0). */
internal fun keyboardFraction(nowPx: Int, fullPx: Int): Float =
    if (fullPx <= 0) 0f else (nowPx.toFloat() / fullPx).coerceIn(0f, 1f)

/**
 * How far the dock rises with the keyboard: it leaves on the slide's first frame and arrives with
 * the keyboard, above it. Resting, it already sits above the navigation bar ([navPx]), so it
 * covers the rest of the way, (full − nav), in step with the keyboard: never below the keyboard's
 * top, and not waiting for the keyboard to pass the navigation bar before moving.
 */
internal fun keyboardRise(nowPx: Int, fullPx: Int, navPx: Int): Float =
    keyboardFraction(nowPx, fullPx) * (fullPx - navPx).coerceAtLeast(0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberKeyboardSlide(): KeyboardSlide {
    val ime = WindowInsets.ime
    val target = WindowInsets.imeAnimationTarget
    val density = LocalDensity.current
    val slide = remember(ime, target, density) { KeyboardSlide(ime, target, density) }
    LaunchedEffect(slide) {
        snapshotFlow { target.getBottom(density) }.collect { if (it > 0) slide.lastFullPx = it }
    }
    return slide
}
