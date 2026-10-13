package dev.nerzouille.koan.ui.bar

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanColors

/**
 * How the page-view bar becomes the overview's workspace bar (owner compares both, then we keep one).
 * [Grow]: the bar grows upward, switcher on top, address field still underneath.
 * [Replace]: same size, the address row crossfades into the switcher.
 */
enum class BarStyle { Grow, Replace }

/** Space the bar takes at the bottom (above the navigation bar), so screens can end above it. */
fun barZone(style: BarStyle, overview: Boolean): Dp {
    val rows = if (overview && style == BarStyle.Grow) BarTokens.SwitcherRowHeight + BarTokens.AddressRowHeight else BarTokens.AddressRowHeight
    return rows + BarTokens.BottomMargin + BarTokens.GapAbove
}

/**
 * The one bottom bar shared by the page view and the overview. [overview] is the overview's open
 * progress (0 page … 1 overview, read only in draw/layer lambdas); [overviewOpen] flips which row
 * takes input. The morph never relayouts: both rows are always laid out, the surface is drawn.
 */
@Composable
fun KoanBar(
    style: BarStyle,
    overviewOpen: Boolean,
    overview: () -> Float,
    onSwipeUp: () -> Unit,
    switcher: @Composable (Modifier) -> Unit,
    address: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addressModifier = if (overviewOpen) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            // Swipe up on the bar opens the overview.
            var total = 0f
            detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
                total += dy
                if (total < -BarTokens.SwipeUpToOpen.toPx()) {
                    total = Float.POSITIVE_INFINITY
                    onSwipeUp()
                }
                change.consume()
            }
        }
    }
    val outer = modifier
        .fillMaxWidth()
        .padding(start = BarTokens.SideMargin, end = BarTokens.SideMargin, bottom = BarTokens.BottomMargin)

    when (style) {
        BarStyle.Grow -> Column(
            outer.drawBehind {
                // The surface grows upward over the switcher row as the overview opens.
                drawSurface(top = (1f - overview()) * BarTokens.SwitcherRowHeight.toPx())
            },
        ) {
            switcher(
                Modifier
                    .height(BarTokens.SwitcherRowHeight)
                    .graphicsLayer {
                        val p = overview()
                        alpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationY = (1f - p) * BarTokens.MorphSlide.toPx()
                    },
            )
            address(Modifier.height(BarTokens.AddressRowHeight).then(addressModifier))
        }
        BarStyle.Replace -> Box(
            outer
                .height(BarTokens.AddressRowHeight)
                .drawBehind { drawSurface(top = 0f) },
        ) {
            address(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = overview()
                        alpha = (1f - p * 2f).coerceIn(0f, 1f)
                        translationY = p * BarTokens.MorphSlide.toPx()
                    }
                    .then(addressModifier),
            )
            switcher(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = overview()
                        alpha = (p * 2f - 1f).coerceIn(0f, 1f)
                        translationY = (1f - p) * -BarTokens.MorphSlide.toPx()
                    },
            )
        }
    }
}

private fun DrawScope.drawSurface(top: Float) {
    drawRoundRect(
        KoanColors.BarSurface,
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top),
        cornerRadius = CornerRadius(BarTokens.Radius.toPx()),
    )
}
