package dev.nerzouille.koan.ui.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.DrawerTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.SpaceColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

private const val OFFSCREEN_PX = 100_000

/** Space the bar takes at the bottom (above the navigation bar), so screens can end above it. */
fun barZone(overview: Boolean): Dp =
    if (overview) {
        BarTokens.GapAbove + BarTokens.SwitcherRowHeight + BarTokens.AddressRowHeight + BarTokens.BottomMargin
    } else {
        BarTokens.AddressRowHeight + BarTokens.BottomMargin
    }

/**
 * The one bottom bar shared by the page view and the overview: the dock, its own surface (hairline
 * on top, soft upward shadow) with the address row and the page load line on its top edge. As the overview opens ([overview] 0 → 1, read only in draw/layer lambdas) it grows
 * upward and the workspace switcher appears on top; in the overview it grows further into the
 * essentials/pinned drawer. The morphs never relayout per frame: rows are laid out, the surface is drawn.
 *
 * Input: see [barGestures] (up = one level deeper, down = one level back, sideways = tabs or spaces).
 */
@Composable
fun KoanBar(
    overviewOpen: Boolean,
    overview: () -> Float,
    bar: BarGestures,
    loading: Boolean,
    loadProgress: Float,
    /** The current space's colours: the load line uses its gradient. */
    colors: SpaceColors,
    /** Page view: how far the dock has slid down as the page scrolls (px); read in a layer. */
    dockHide: () -> Float,
    drawerContent: @Composable (Modifier) -> Unit,
    switcher: @Composable (Modifier) -> Unit,
    address: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val drawer = bar.drawer
    val palette = KoanTheme.colors
    val line = rememberLoadLine(loading, loadProgress)
    // The space colours crossfade when the current space changes; read only while drawing.
    val fade = tween<Color>(Motion.ACCENT_FADE_MS)
    val c0 = animateColorAsState(colors.progress[0], fade, label = "progress0")
    val c1 = animateColorAsState(colors.progress[1], fade, label = "progress1")
    val c2 = animateColorAsState(colors.progress[2], fade, label = "progress2")

    // The drawer's caps from the window: compact keeps the whole dock under ~55% of the screen,
    // full stops just below the status bar.
    val density = LocalDensity.current
    val windowPx = LocalWindowInfo.current.containerSize.height.toFloat()
    with(density) {
        val rows = (BarTokens.SwitcherRowHeight + BarTokens.AddressRowHeight).toPx()
        val below = WindowInsets.navigationBars.getBottom(density) + BarTokens.BottomMargin.toPx()
        val top = WindowInsets.statusBars.getTop(density) + DrawerTokens.FullTopGap.toPx()
        drawer.compactCapPx = windowPx * DrawerTokens.COMPACT_MAX_FRACTION - rows - below
        drawer.fullCapPx = windowPx - top - rows - below
        drawer.minStepPx = DrawerTokens.MinDetentStep.toPx()
        bar.scrub.stepPx = BarTokens.ScrubStep.toPx()
    }
    val gestures = Modifier.barGestures(bar, view)

    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = if (overviewOpen) 0f else dockHide() }
            .drawWithCache {
                val shadowPx = BarTokens.DockShadowHeight.toPx()
                val hairline = BarTokens.Hairline.toPx()
                val shimmerPx = BarTokens.ShimmerWidth.toPx()
                onDrawBehind {
                    val o = overview()
                    val d = drawer.progress
                    val sheetTop = if (overviewOpen) drawer.sheetTop() else 0f
                    // The switcher row shows in the overview, and over the page while scrubbing spaces.
                    val row = maxOf(o, bar.scrub.shown)
                    val top = sheetTop + BarTokens.SwitcherRowHeight.toPx() * (1f - row)
                    // Square under a page, rounded in the overview, a little more as the drawer.
                    val r = BarTokens.DockRadius.toPx() * row * (1f - d) + DrawerTokens.TopRadius.toPx() * d
                    // Soft shadow cast upward over the content; it wraps the rounded corners.
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 1f to palette.DockShadow, startY = top - shadowPx, endY = top + r),
                        topLeft = Offset(0f, top - shadowPx),
                        size = Size(size.width, shadowPx + r),
                    )
                    // The hairline is the surface itself, peeking out 1px above its fill.
                    drawPath(dockPath(top, r), palette.DockHairline)
                    drawPath(dockPath(top + hairline, r), palette.Dock)
                    // Page load line on the top edge, page view only.
                    val a = line.alpha.value * (1f - row)
                    if (a > 0.01f) drawLoadLine(top, line, a, shimmerPx, c0.value, c1.value, c2.value)
                }
            }
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(bottom = BarTokens.BottomMargin),
    ) {
        if (overviewOpen) {
            drawerContent(
                (if (drawer.open) Modifier.barGestures(bar, view, drawerArea = true) else Modifier).layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) {
                        // Fully closed, the (invisible) sheet still takes its height so drags know
                        // their distance, but sits off-screen: its pinned list must never catch the
                        // touches meant for the stack behind it. Placement only, no recomposition.
                        val shown = drawer.open || drawer.height > 0f
                        p.place(if (shown) 0 else -OFFSCREEN_PX, 0)
                    }
                },
            )
        }
        Column {
            Box(
                (if (overviewOpen) gestures else Modifier)
                    .height(BarTokens.SwitcherRowHeight)
                    .graphicsLayer {
                        val p = maxOf(overview(), bar.scrub.shown)
                        alpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        translationY = (1f - p) * BarTokens.MorphSlide.toPx()
                    },
            ) {
                switcher(Modifier.fillMaxSize())
                if (overviewOpen) {
                    DrawerHandle(onClick = drawer::toggle, modifier = Modifier.align(Alignment.TopCenter))
                }
            }
            // The same handler instance in both screens: a drag that opens the overview keeps going.
            address(Modifier.height(BarTokens.AddressRowHeight).then(gestures))
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
                .background(KoanTheme.colors.DrawerHandle),
        )
    }
}

private fun DrawScope.dockPath(top: Float, r: Float) = Path().apply {
    val radius = CornerRadius(r)
    addRoundRect(
        RoundRect(
            left = 0f, top = top, right = size.width, bottom = size.height,
            topLeftCornerRadius = radius, topRightCornerRadius = radius,
        ),
    )
}

/** Page load line: progress, fade and the shimmer that runs along it while it waits. */
@Stable
private class LoadLine {
    val progress = Animatable(0f)
    val alpha = Animatable(0f)
    val shimmer = Animatable(0f)
}

private val ProgressEasing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)

/**
 * Smooth, never backwards: a new load starts from the left, the line eases towards the engine's
 * progress, shimmers while waiting, and on completion runs to the end and fades out.
 */
@Composable
private fun rememberLoadLine(loading: Boolean, progress: Float): LoadLine {
    val line = remember { LoadLine() }
    val target by rememberUpdatedState(progress)
    LaunchedEffect(loading) {
        if (loading) {
            if (line.alpha.value < 0.05f) line.progress.snapTo(0f)
            launch { line.alpha.animateTo(1f, tween(BarTokens.PROGRESS_FADE_MS / 2)) }
            launch {
                while (true) {
                    line.shimmer.snapTo(0f)
                    line.shimmer.animateTo(1f, tween(BarTokens.SHIMMER_MS, easing = LinearEasing))
                }
            }
            snapshotFlow { target }.collectLatest { p ->
                val to = max(p, BarTokens.PROGRESS_START).coerceAtLeast(line.progress.value)
                line.progress.animateTo(to, tween(BarTokens.PROGRESS_MS, easing = ProgressEasing))
            }
        } else {
            line.progress.animateTo(1f, tween(BarTokens.PROGRESS_FINISH_MS))
            line.alpha.animateTo(0f, tween(BarTokens.PROGRESS_FADE_MS))
        }
    }
    return line
}

private fun DrawScope.drawLoadLine(top: Float, line: LoadLine, alpha: Float, shimmerPx: Float, c0: Color, c1: Color, c2: Color) {
    val w = size.width * line.progress.value
    if (w < 1f) return
    val h = BarTokens.ProgressHeight.toPx()
    val glow = BarTokens.ProgressGlow.toPx()
    val brush = Brush.horizontalGradient(listOf(c0, c1, c2), startX = 0f, endX = w)
    // A soft glow: two wider, fainter passes of the same gradient under the line.
    drawRect(brush, topLeft = Offset(0f, top + h / 2f - glow / 2f), size = Size(w, glow), alpha = alpha * BarTokens.PROGRESS_GLOW_ALPHA * 0.35f)
    drawRect(brush, topLeft = Offset(0f, top + h / 2f - glow / 4f), size = Size(w, glow / 2f), alpha = alpha * BarTokens.PROGRESS_GLOW_ALPHA)
    drawRect(brush, topLeft = Offset(0f, top), size = Size(w, h), alpha = alpha)
    // Shimmer: a lighter streak sweeping the filled part.
    val x = line.shimmer.value * (w + shimmerPx) - shimmerPx
    clipRect(right = w) {
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = BarTokens.SHIMMER_ALPHA), Color.Transparent),
                startX = x, endX = x + shimmerPx,
            ),
            topLeft = Offset(x, top),
            size = Size(shimmerPx, h),
            alpha = alpha,
        )
    }
}
