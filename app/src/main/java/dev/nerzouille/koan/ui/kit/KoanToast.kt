package dev.nerzouille.koan.ui.kit

import android.os.Build
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

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
 * Confirms a copy to the clipboard. Android 13+ shows its own confirmation; below it nothing would,
 * so Koan says "[what] copied" itself.
 */
fun copiedToast(what: String): Toast? =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast("$what copied") else null

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

/**
 * Draws [state]'s toast [bottom] above the screen's bottom edge (above the dock), over everything.
 * A toast that replaces another one never just swaps its text: the old one sinks away (and fades)
 * while the new one rises in its place.
 */
@Composable
fun KoanToastHost(state: ToastState, bottom: Dp, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    // A swipe sideways or down sends the toast away early (no action).
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    // The toast being replaced, on its way out, with its own progress (1 = fully shown).
    var leaving by remember { mutableStateOf<Toast?>(null) }
    val leave = remember { Animatable(0f) }
    val leaveX = remember { mutableFloatStateOf(0f) }
    val leaveY = remember { mutableFloatStateOf(0f) }
    var drawn by remember { mutableStateOf<Toast?>(null) }
    val scope = rememberCoroutineScope()
    var drawnSerial by remember { mutableStateOf(-1) }
    LaunchedEffect(state.serial, state.visible) {
        if (drawnSerial != state.serial) {
            // A new toast: the one still showing (if any) leaves on its own while this one comes in.
            val old = drawn
            if (old != null && state.toast !== old && shown.value > 0f) {
                leaving = old
                leaveX.floatValue = dragX.value
                leaveY.floatValue = dragY.value
                leave.snapTo(shown.value)
                shown.snapTo(0f)
                scope.launch {
                    leave.animateTo(0f, Motion.Toast)
                    leaving = null
                }
            }
            drawn = state.toast
            drawnSerial = state.serial
            dragX.snapTo(0f)
            dragY.snapTo(0f)
        }
        if (state.visible) {
            shown.animateTo(1f, Motion.Toast)
            delay(ToastTokens.SHOW_MS)
            state.dismiss()
        } else {
            shown.animateTo(0f, Motion.Toast)
        }
    }
    val toast = drawn ?: state.toast
    val old = leaving
    if (old == null && (toast == null || (!state.visible && shown.value == 0f))) return
    Box(
        modifier
            .fillMaxWidth()
            // Above the keyboard too while it is up (History's search).
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = ToastTokens.Margin, end = ToastTokens.Margin, bottom = bottom + ToastTokens.Gap),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (old != null) {
            // Sinks below its spot (the way a toast leaves), under the one coming in.
            ToastCard(
                old,
                actionEnabled = false,
                onAction = {},
                modifier = Modifier.graphicsLayer {
                    alpha = leave.value * (1f - abs(leaveX.floatValue) / size.width).coerceIn(0f, 1f)
                    translationX = leaveX.floatValue
                    translationY = (1f - leave.value) * ToastTokens.Rise.toPx() * LEAVE_SINK + leaveY.floatValue
                    val s = 1f - (1f - leave.value) * LEAVE_SHRINK
                    scaleX = s
                    scaleY = s
                },
            )
        }
        if (toast != null && (state.visible || shown.value > 0f)) {
            ToastCard(
                toast,
                actionEnabled = state.visible,
                onAction = {
                    state.dismiss()
                    toast.onAction?.invoke()
                },
                modifier = Modifier
                    .graphicsLayer {
                        alpha = shown.value * (1f - abs(dragX.value) / size.width).coerceIn(0f, 1f)
                        translationX = dragX.value
                        translationY = (1f - shown.value) * ToastTokens.Rise.toPx() + dragY.value
                    }
                    .pointerInput(state) {
                        val tracker = VelocityTracker()
                        val fling = ToastTokens.SwipeFling.toPx()
                        detectDragGestures(
                            onDragStart = { tracker.resetTracking() },
                            onDragEnd = {
                                val v = tracker.calculateVelocity()
                                val w = size.width.toFloat()
                                scope.launch {
                                    when {
                                        abs(dragX.value) > w * ToastTokens.SWIPE_FRACTION || abs(v.x) > fling -> {
                                            val sign = if (dragX.value != 0f) sign(dragX.value) else sign(v.x)
                                            state.dismiss()
                                            dragX.animateTo(sign * w, Motion.Toast)
                                        }
                                        dragY.value > size.height * ToastTokens.SWIPE_FRACTION * 2 || v.y > fling -> state.dismiss()
                                        else -> {
                                            launch { dragX.animateTo(0f, Motion.Toast) }
                                            dragY.animateTo(0f, Motion.Toast)
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    launch { dragX.animateTo(0f, Motion.Toast) }
                                    dragY.animateTo(0f, Motion.Toast)
                                }
                            },
                        ) { change, delta ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch {
                                dragX.snapTo(dragX.value + delta.x)
                                dragY.snapTo((dragY.value + delta.y).coerceAtLeast(0f))
                            }
                        }
                    },
            )
        }
    }
}

/** How much further than [ToastTokens.Rise] a replaced toast sinks, and how much it shrinks. */
private const val LEAVE_SINK = 1.5f
private const val LEAVE_SHRINK = 0.06f

/** The toast itself: the message (and its quiet second line) and the action. */
@Composable
private fun ToastCard(toast: Toast, actionEnabled: Boolean, onAction: () -> Unit, modifier: Modifier) {
    val palette = KoanTheme.colors
    Row(
        modifier
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
                    .clickable(enabled = actionEnabled, onClick = onAction)
                    .padding(horizontal = ToastTokens.PaddingH),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, color = palette.OnPrimary, fontSize = ToastTokens.TextSize, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
