package dev.nerzouille.koan.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.TabMenuTokens

/**
 * A small popup attached to the page view's dock, opened by a long press on one of its buttons,
 * just above the dock (UI_SPEC, "Dock popups"). Its outer edge sits on the dock's margin, on the
 * side of its button ([end]: the right one, else the left), and never moves: its width hugs its
 * content (at least [minWidth], only for content that has intrinsic sizes) and it grows towards
 * the middle. It grows out of the button ([anchor]: the button's centre, from that side) and
 * shrinks back into it; a tap anywhere else or Back closes it.
 */
@Composable
fun DockPopup(
    open: Boolean,
    onDismiss: () -> Unit,
    end: Boolean,
    anchor: Dp,
    modifier: Modifier = Modifier,
    minWidth: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Where the popup landed (px from the left) and the screen's width, for the scale's origin.
    val placed = remember { IntArray(2) }
    val shown = remember { Animatable(0f) }
    LaunchedEffect(open) { shown.animateTo(if (open) 1f else 0f, tween(if (open) TabMenuTokens.IN_MS else TabMenuTokens.OUT_MS)) }
    if (!open && shown.value == 0f) return
    if (open) BackHandler(onBack = onDismiss)
    val shape = RoundedCornerShape(TabMenuTokens.Radius)
    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (open) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                            onDismiss()
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = BarTokens.AddressRowHeight + BarTokens.BottomMargin + TabMenuTokens.Gap)
                .layout { measurable, constraints ->
                    val full = constraints.maxWidth
                    val margin = BarTokens.RowPadding.roundToPx()
                    val loose = constraints.copy(minWidth = 0, maxWidth = full - 2 * margin)
                    val p: Placeable = if (minWidth != null) {
                        // Rows fill the popup's width: measured at the width its content wants.
                        val natural = measurable.maxIntrinsicWidth(constraints.maxHeight)
                        val w = maxOf(natural, minWidth.roundToPx()).coerceAtMost(loose.maxWidth)
                        measurable.measure(loose.copy(minWidth = w, maxWidth = w))
                    } else {
                        measurable.measure(loose)
                    }
                    val x = if (end) full - margin - p.width else margin
                    placed[0] = x
                    placed[1] = full
                    layout(full, p.height) { p.place(x, 0) }
                }
                .then(modifier)
                .graphicsLayer {
                    val p = shown.value
                    alpha = p
                    val s = TabMenuTokens.SCALE_FROM + (1f - TabMenuTokens.SCALE_FROM) * p
                    scaleX = s
                    scaleY = s
                    // Out of the button: its centre on screen, from the popup's own left edge.
                    val screenX = if (end) placed[1] - anchor.toPx() else anchor.toPx()
                    transformOrigin = TransformOrigin(((screenX - placed[0]) / size.width).coerceIn(0f, 1f), 1f)
                }
                .shadow(TabMenuTokens.Shadow, shape)
                .clip(shape)
                .background(KoanTheme.colors.Dock),
            content = content,
        )
    }
}

/** One row of a dock popup: [leading] (an icon or a favicon), then the text. */
@Composable
fun DockPopupRow(interactive: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(TabMenuTokens.RowHeight)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = TabMenuTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
