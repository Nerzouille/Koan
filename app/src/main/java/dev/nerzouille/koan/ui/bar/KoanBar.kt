package dev.nerzouille.koan.ui.bar

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import dev.nerzouille.koan.ui.overview.SpacePager
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.PagerTokens
import kotlin.math.abs

/** Space the bar takes at the bottom (above the navigation bar), so screens can end above it. */
fun barZone(overview: Boolean): Dp =
    if (overview) {
        BarTokens.GapAbove + BarTokens.SwitcherRowHeight + BarTokens.AddressRowHeight + BarTokens.BottomMargin
    } else {
        BarTokens.AddressRowHeight + BarTokens.BottomMargin
    }

/**
 * The one bottom bar shared by the page view and the overview: a plain page-dark strip with the
 * address row. As the overview opens ([overview] 0 → 1, read only in draw/layer lambdas) it grows
 * upward and the workspace switcher appears on top; in the overview it grows further into the
 * essentials/pinned drawer. The morphs never relayout per frame: rows are laid out, the surface is drawn.
 *
 * Input in the overview: horizontal drags anywhere on the bar switch spaces, vertical drags open and
 * close the drawer.
 */
@Composable
fun KoanBar(
    overviewOpen: Boolean,
    overview: () -> Float,
    pager: SpacePager,
    drawer: DrawerState,
    loading: Boolean,
    loadProgress: Float,
    onSwipeUp: () -> Unit,
    drawerContent: @Composable (Modifier) -> Unit,
    switcher: @Composable (Modifier) -> Unit,
    address: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val progress by animateFloatAsState(if (loading) loadProgress.coerceAtLeast(0.05f) else 1f, spring(), label = "progress")
    val progressAlpha by animateFloatAsState(if (loading) 1f else 0f, label = "progressAlpha")

    val overviewGestures = Modifier.pointerInput(pager, drawer) {
        val pagerFling = PagerTokens.FlingVelocity.toPx()
        val drawerFling = DrawerTokens.FlingVelocity.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var total = Offset.Zero
            var horizontal: Boolean? = null
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    val v = if (change == null) Velocity.Zero else tracker.calculateVelocity()
                    when (horizontal) {
                        // Exactly one light tick per committed space switch, on release.
                        true -> if (pager.release(v.x, pagerFling)) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        false -> drawer.release(v.y, drawerFling)
                        null -> Unit
                    }
                    break
                }
                // The pinned list scrolled with this drag: leave it to the list (and settle the sheet
                // back if it had started to follow).
                if (horizontal != true && change.isConsumed) {
                    if (horizontal == false) drawer.release(0f, drawerFling)
                    break
                }
                tracker.addPosition(change.uptimeMillis, change.position)
                val delta = change.positionChange()
                if (horizontal == null) {
                    total += delta
                    if (total.getDistance() <= viewConfiguration.touchSlop) continue
                    horizontal = abs(total.x) > abs(total.y)
                    if (horizontal) pager.grab() else drawer.grab()
                }
                if (horizontal) pager.dragBy(delta.x) else drawer.dragBy(delta.y)
                change.consume()
            }
        }
    }
    val pageGestures = Modifier.pointerInput(Unit) {
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

    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val o = overview()
                val d = drawer.progress
                val drawerPx = if (overviewOpen) drawer.heightPx else 0f
                val top = drawerPx * (1f - d) + BarTokens.SwitcherRowHeight.toPx() * (1f - o)
                val radius = CornerRadius(DrawerTokens.TopRadius.toPx() * d)
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f, top = top, right = size.width, bottom = size.height,
                            topLeftCornerRadius = radius, topRightCornerRadius = radius,
                        ),
                    )
                }
                drawPath(path, KoanColors.BarSurface)
                drawRect(KoanColors.BarHairline, topLeft = Offset(0f, top), size = Size(size.width, BarTokens.Hairline.toPx()), alpha = 1f - d)
                // Page load progress along the top edge, page view only.
                val a = progressAlpha * (1f - o)
                if (a > 0.01f) {
                    drawRect(
                        KoanColors.DefaultAccent,
                        topLeft = Offset(0f, top),
                        size = Size(size.width * progress, BarTokens.ProgressHeight.toPx()),
                        alpha = a,
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(bottom = BarTokens.BottomMargin),
    ) {
        if (overviewOpen) {
            drawerContent(if (drawer.open) overviewGestures else Modifier)
        }
        Column(if (overviewOpen) overviewGestures else Modifier) {
            Box(
                Modifier
                    .height(BarTokens.SwitcherRowHeight)
                    .graphicsLayer {
                        val p = overview()
                        alpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationY = (1f - p) * BarTokens.MorphSlide.toPx()
                    },
            ) {
                switcher(Modifier.fillMaxSize())
                if (overviewOpen) {
                    DrawerHandle(onClick = { drawer.animateTo(!drawer.open) }, modifier = Modifier.align(Alignment.TopCenter))
                }
            }
            address(Modifier.height(BarTokens.AddressRowHeight).then(if (overviewOpen) Modifier else pageGestures))
        }
    }
}

@Composable
private fun DrawerHandle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(BarTokens.HandleWidth * 2)
            .height(BarTokens.HandleTouch)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = BarTokens.HandleTop)
                .size(BarTokens.HandleWidth, BarTokens.HandleHeight)
                .clip(CircleShape)
                .background(KoanColors.DrawerHandle),
        )
    }
}
