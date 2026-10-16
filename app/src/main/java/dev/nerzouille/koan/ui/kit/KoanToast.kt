package dev.nerzouille.koan.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import dev.nerzouille.koan.ui.theme.brandOutline
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.ToastTokens
import kotlinx.coroutines.delay

/**
 * One toast: a short message and at most one action ("Switch", "Undo", "Open"). [detail]: a quiet
 * second line. [moment]: Koan just finished something for you (a download): a brand-gradient
 * outline for that moment.
 */
@Immutable
class Toast(
    val message: String,
    val action: String? = null,
    val detail: String? = null,
    val moment: Boolean = false,
    val onAction: (() -> Unit)? = null,
)

/**
 * The one bottom toast, shared by everything that confirms quietly. A new toast replaces the one
 * showing; each stays [ToastTokens.SHOW_MS], then sinks away.
 */
@Stable
class ToastState {
    var toast by mutableStateOf<Toast?>(null)
        private set

    /** Bumped on every [show], so the same message twice still restarts the timer. */
    internal var serial by mutableStateOf(0)
        private set
    internal var visible by mutableStateOf(false)
        private set

    fun show(toast: Toast) {
        this.toast = toast
        serial++
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

/** Draws [state]'s toast [bottom] above the screen's bottom edge (above the dock), over everything. */
@Composable
fun KoanToastHost(state: ToastState, bottom: Dp, modifier: Modifier = Modifier) {
    val shown = androidx.compose.runtime.remember { Animatable(0f) }
    LaunchedEffect(state.serial, state.visible) {
        if (state.visible) {
            shown.animateTo(1f, Motion.Toast)
            delay(ToastTokens.SHOW_MS)
            state.dismiss()
        } else {
            shown.animateTo(0f, Motion.Toast)
        }
    }
    val toast = state.toast ?: return
    if (!state.visible && shown.value == 0f) return
    val palette = KoanTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = ToastTokens.Margin, end = ToastTokens.Margin, bottom = bottom + ToastTokens.Gap),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .graphicsLayer {
                    alpha = shown.value
                    translationY = (1f - shown.value) * ToastTokens.Rise.toPx()
                }
                .fillMaxWidth()
                .height(if (toast.detail != null) ToastTokens.TallHeight else ToastTokens.Height)
                .clip(RoundedCornerShape(ToastTokens.Radius))
                .background(palette.Primary)
                .then(if (toast.moment) Modifier.brandOutline(RoundedCornerShape(ToastTokens.Radius)) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(start = ToastTokens.PaddingH)) {
                Text(
                    toast.message,
                    color = palette.OnPrimary,
                    fontSize = ToastTokens.TextSize,
                    fontWeight = if (toast.detail != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                toast.detail?.let {
                    Text(it, color = palette.OnPrimary.copy(alpha = ToastTokens.DETAIL_ALPHA), fontSize = ToastTokens.DetailSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val action = toast.action
            if (action != null) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .clickable(enabled = state.visible) {
                            state.dismiss()
                            toast.onAction?.invoke()
                        }
                        .padding(horizontal = ToastTokens.PaddingH),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(action, color = palette.OnPrimary, fontSize = ToastTokens.TextSize, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
