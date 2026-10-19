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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.BarTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.TabMenuTokens

/**
 * The tab counter's long-press menu: a small popup just above the counter (New tab, Close tab).
 * It grows out of the counter and shrinks back into it; a tap anywhere else or Back closes it.
 */
@Composable
fun TabMenu(open: Boolean, onDismiss: () -> Unit, onNewTab: () -> Unit, onCloseTab: () -> Unit) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(open) { shown.animateTo(if (open) 1f else 0f, tween(if (open) TabMenuTokens.IN_MS else TabMenuTokens.OUT_MS)) }
    if (!open && shown.value == 0f) return
    if (open) BackHandler(onBack = onDismiss)
    val palette = KoanTheme.colors
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
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                // Its right edge on the counter's, just above the dock.
                .padding(
                    end = BarTokens.RowPadding + BarTokens.IconTouch + BarTokens.RowGap,
                    bottom = BarTokens.AddressRowHeight + BarTokens.BottomMargin + TabMenuTokens.Gap,
                )
                .graphicsLayer {
                    val p = shown.value
                    alpha = p
                    val s = TabMenuTokens.SCALE_FROM + (1f - TabMenuTokens.SCALE_FROM) * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.85f, 1f)
                }
                .shadow(TabMenuTokens.Shadow, shape)
                .clip(shape)
                .background(palette.Dock)
                .width(IntrinsicSize.Max),
        ) {
            MenuItem(KoanIcons.Plus, "New tab", open, onNewTab)
            MenuItem(KoanIcons.Close, "Close tab", open, onCloseTab)
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, interactive: Boolean, onClick: () -> Unit) {
    val palette = KoanTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(TabMenuTokens.RowHeight)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = TabMenuTokens.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = palette.Icon, modifier = Modifier.size(TabMenuTokens.Icon))
        Spacer(Modifier.width(TabMenuTokens.IconGap))
        Text(title, color = palette.TextPrimary, fontSize = TabMenuTokens.TextSize, fontWeight = FontWeight.Medium)
    }
}
