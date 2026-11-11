package dev.nerzouille.koan.ui.player

import androidx.compose.foundation.layout.displayCutout
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Where the player's controls sit, from the screen edges (dp). */
data class PlayerInsets(
    val top: Dp,
    val bottom: Dp,
    val titleStart: Dp,
    val rightEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp,
    val levelStart: Dp,
    val levelEnd: Dp,
)

/**
 * The display cutouts in this window (px), read again whenever the window's cutout insets change:
 * just after a rotation the view could still report the old orientation's rects (a portrait
 * camera hole read in landscape pushed the top row down), so the rects follow the insets.
 */
@Composable
fun rememberCutouts(landscape: Boolean): List<Rect> {
    val view = LocalView.current
    val config = LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val cut = androidx.compose.foundation.layout.WindowInsets.displayCutout
    val key = listOf(cut.getLeft(density, dir), cut.getTop(density), cut.getRight(density, dir), cut.getBottom(density))
    return remember(landscape, config.screenWidthDp, config.screenHeightDp, key) {
        view.rootWindowInsets?.displayCutout?.boundingRects.orEmpty()
    }
}

/**
 * Every control [PlayerTokens.Inset] from the screen edges. A cutout pushes a control in only on
 * the side the cutout touches, and only when the control's band would overlap it: a camera hole
 * in the middle of the left edge leaves the title pill and the bottom pill where they are, but
 * moves the brightness level if it would sit on it.
 */
fun playerInsets(cutouts: List<Rect>, width: Dp, height: Dp, bottomPillH: Dp, density: Density, statusTop: Dp = 0.dp): PlayerInsets {
    // Portrait: the status bar shows over the player; the top row goes under it, the sides keep
    // the plain margin (a camera cutout only counts at the top, where the status bar covers it).
    if (height > width) return portraitInsets(cutouts, width, height, density, statusTop)
    val edge = PlayerTokens.Inset
    val gap = PlayerTokens.CutoutGap
    val rects = with(density) { cutouts.map { r -> DpRect(r.left.toDp(), r.top.toDp(), r.right.toDp(), r.bottom.toDp()) } }
    val left = rects.filter { it.left <= 1.dp && it.right < width / 2 }
    val right = rects.filter { it.right >= width - 1.dp && it.left > width / 2 }
    val top = rects.filter { it.top <= 1.dp && it.bottom < height / 2 }
    val bottom = rects.filter { it.bottom >= height - 1.dp && it.top > height / 2 }

    // The top row spans the whole width (the title pill grows toward the round pills).
    val topInset = maxOf(edge, top.maxOfOrNull { it.bottom + gap } ?: 0.dp)
    val bottomInset = maxOf(edge, bottom.maxOfOrNull { (height - it.top) + gap } ?: 0.dp)
    val topBand = topInset to topInset + PlayerTokens.PillHeight
    val bottomBand = height - bottomInset - bottomPillH to height - bottomInset
    val levelBand = topInset + PlayerTokens.RoundPill to height - bottomInset - bottomPillH

    fun fromLeft(band: Pair<Dp, Dp>) = maxOf(edge, left.filter { it.overlapsY(band) }.maxOfOrNull { it.right + gap } ?: 0.dp)
    fun fromRight(band: Pair<Dp, Dp>) = maxOf(edge, right.filter { it.overlapsY(band) }.maxOfOrNull { (width - it.left) + gap } ?: 0.dp)
    return PlayerInsets(
        top = topInset,
        bottom = bottomInset,
        titleStart = fromLeft(topBand),
        rightEnd = fromRight(topBand),
        bottomStart = fromLeft(bottomBand),
        bottomEnd = fromRight(bottomBand),
        levelStart = fromLeft(levelBand).let { if (it == edge) 0.dp else it },
        levelEnd = fromRight(levelBand).let { if (it == edge) 0.dp else it },
    )
}

private data class DpRect(val left: Dp, val top: Dp, val right: Dp, val bottom: Dp) {
    fun overlapsY(band: Pair<Dp, Dp>) = top < band.second && bottom > band.first
}

private fun portraitInsets(cutouts: List<Rect>, width: Dp, height: Dp, density: Density, statusTop: Dp): PlayerInsets {
    val edge = PlayerTokens.Inset
    val gap = PlayerTokens.CutoutGap
    val rects = with(density) { cutouts.map { r -> DpRect(r.left.toDp(), r.top.toDp(), r.right.toDp(), r.bottom.toDp()) } }
    val cutoutBottom = rects.filter { it.bottom < height / 2 }.maxOfOrNull { it.bottom } ?: 0.dp
    val top = maxOf(edge, maxOf(statusTop, cutoutBottom) + gap)
    val bottom = maxOf(edge, rects.filter { it.top > height / 2 }.maxOfOrNull { (height - it.top) + gap } ?: 0.dp)
    return PlayerInsets(
        top = top, bottom = bottom,
        titleStart = edge, rightEnd = edge, bottomStart = edge, bottomEnd = edge,
        levelStart = 0.dp, levelEnd = 0.dp,
    )
}
