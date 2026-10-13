package dev.nerzouille.koan.ui.overview

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.StackTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * The upright perspective stack (docs/UI_SPEC.md "Depth stack"). Cards are laid out once; every
 * frame of scrolling, closing or reordering only touches graphicsLayer/draw lambdas.
 */
@Composable
fun DepthStack(
    cards: List<CardModel>,
    state: StackState,
    enabled: Boolean,
    onOpen: (String) -> Unit,
    onClose: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val view = LocalView.current

    // Order committed by a drop, shown until Room emits it back.
    var pendingOrder by remember { mutableStateOf<List<String>?>(null) }
    val roomIds = cards.map { it.id }
    val ids = pendingOrder?.takeIf { it.size == roomIds.size && it.toSet() == roomIds.toSet() } ?: roomIds
    if (pendingOrder != null && pendingOrder == roomIds) pendingOrder = null
    val byId = remember(cards) { cards.associateBy { it.id } }

    state.last = (ids.size - 1).coerceAtLeast(0)
    state.slotPx = with(density) { StackTokens.SlotDrag.toPx() }

    LaunchedEffect(ids) {
        state.cards.keys.retainAll(ids.toSet())
        ids.forEachIndexed { i, id ->
            val a = state.anim(id, i)
            if (a.slot != i.toFloat() && state.liftedId != id) state.animateSlot(a, i.toFloat())
        }
        if (state.pos > state.last) state.animateTo(state.last.toFloat())
    }

    val blurs = remember(density) {
        // 0.5dp steps; RenderEffects are immutable so the layer can reuse them every frame.
        val steps = (StackTokens.BlurDp.max() * 2f).roundToInt() + 1
        Array(steps) { step -> if (step == 0) null else BlurEffect(step * 0.5f * density.density, step * 0.5f * density.density, TileMode.Clamp) }
    }
    val blurFor: (Float) -> RenderEffect? = remember(blurs) {
        { d -> blurs[(DepthCurve.blurDp(d) * 2f).roundToInt().coerceIn(0, blurs.size - 1)] }
    }

    BoxWithConstraints(modifier.clipToBounds()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val geo = remember(width, height, density) { StackGeometry(state, width, height, density.density) }
        // The dock zone is solid Base, so fading *to Base* looks like the spec's alpha mask without
        // an offscreen layer for the whole stack every frame.
        val fade = remember(height, density) {
            Brush.verticalGradient(
                0f to Color.Transparent, 1f to KoanColors.Base,
                startY = height - with(density) { StackTokens.BottomFade.toPx() }, endY = height,
            )
        }
        val cardWidthDp = with(density) { geo.cardWidth.toDp() }
        val cardHeightDp = with(density) { geo.cardHeight.toDp() }

        val currentIds by rememberUpdatedState(ids)
        val currentOnOpen by rememberUpdatedState(onOpen)
        val currentOnClose by rememberUpdatedState(onClose)
        val currentOnReorder by rememberUpdatedState(onReorder)

        /** Fly the card off sideways (prototype: 280 ms ease-in, 480dp, 8°), then close it. */
        fun flyOff(id: String, dir: Float) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val a = state.anim(id, 0)
            a.gone = true
            a.flyFromX = a.swipeX
            a.flyDir = dir
            state.launch {
                state.animateFlyOff(a)
                currentOnClose(id)
            }
        }

        Box(
            Modifier
                .size(maxWidth, maxHeight)
                .drawWithContent {
                    drawContent()
                    drawRect(fade)
                }
                .pointerInput(enabled, geo) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // A touch that catches a moving stack only stops it; it never opens or focuses a card.
                        val caught = state.startDrag()
                        val hit = geo.hitTest(down.position, currentIds)
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)

                        // Decide: tap, drag (scroll or swipe) or long press.
                        var slop = Offset.Zero
                        val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            var total = Offset.Zero
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    ?: return@withTimeoutOrNull Gesture.Cancel
                                tracker.addPosition(change.uptimeMillis, change.position)
                                if (!change.pressed) return@withTimeoutOrNull Gesture.Tap
                                total += change.positionChange()
                                if (total.getDistance() > viewConfiguration.touchSlop) {
                                    slop = total
                                    change.consume()
                                    return@withTimeoutOrNull Gesture.Drag
                                }
                            }
                            @Suppress("UNREACHABLE_CODE") Gesture.Cancel
                        } ?: if (hit != null) Gesture.LongPress else Gesture.Drag

                        when (result) {
                            Gesture.Cancel -> state.release()
                            Gesture.Tap -> when {
                                hit == null || caught -> state.release()
                                hit.onClose -> flyOff(hit.id, 1f)
                                // Tap the focused card to open it; tap any other card to bring it into focus.
                                hit.focused -> currentOnOpen(hit.id)
                                else -> state.animateTo(currentIds.indexOf(hit.id).toFloat())
                            }
                            Gesture.Drag -> {
                                val focusedId = currentIds.getOrNull(state.pos.roundToInt())
                                if (abs(slop.x) > abs(slop.y) && focusedId != null) {
                                    // Sideways always swipes the focused card, wherever the finger is (prototype).
                                    state.release()
                                    swipeCard(state, state.anim(focusedId, 0), down.id) { dir -> flyOff(focusedId, dir) }
                                } else {
                                    trackUntilUp(down.id, tracker) { state.dragBy(it.positionChange().y) }
                                    state.release(tracker.calculateVelocity().y)
                                }
                            }
                            Gesture.LongPress -> if (hit != null) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                reorder(state, hit.id, currentIds, down.id) {
                                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                                }?.let { order ->
                                    pendingOrder = order
                                    currentOnReorder(order)
                                }
                            }
                        }
                    }
                },
        ) {
            ids.forEachIndexed { index, id ->
                val card = byId[id] ?: return@forEachIndexed
                key(id) {
                    val a = state.anim(id, index)
                    TabCard(
                        card = card,
                        depth = { geo.depthOf(a) },
                        blurFor = blurFor,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(if (state.liftedId == id) 1f else 0f)
                            .size(cardWidthDp, cardHeightDp)
                            .graphicsLayer { geo.applyTo(this, a) },
                    )
                }
            }
        }
    }
}

private enum class Gesture { Tap, Drag, LongPress, Cancel }

private class Hit(val id: String, val focused: Boolean, val onClose: Boolean)

private val CardShape = RoundedCornerShape(CardTokens.Radius)

/** Positions of everything in the stack, in px, as functions of the scroll state. */
private class StackGeometry(
    val state: StackState,
    val width: Float,
    val viewportHeight: Float,
    val density: Float,
) {
    val cardWidth = width - 2 * StackTokens.CardSideMargin.value * density

    /** The focused card gets all the height left by the top gap, the pile (A = fraction × height) and the peek. */
    val cardHeight = (
        viewportHeight - (StackTokens.PileTopGap + StackTokens.PeekVisible - StackTokens.PeekOverlap).value * density
        ) / (1f + StackTokens.PILE_DEPTH_FRACTION)
    private val headerPx = CardTokens.HeaderHeight.value * density
    private val closePx = CardTokens.CloseTouch.value * density

    /** Top of the card in front (d = -1): only its header and a hint of thumbnail show. */
    private val peekTop = viewportHeight - StackTokens.PeekVisible.value * density

    /** Top of the focused card: the card in front overlaps its bottom part. */
    private val focusTop = peekTop + StackTokens.PeekOverlap.value * density - cardHeight

    /** A: the pile converges this far above the focused card, never into the header. */
    private val pileDepth = min(
        StackTokens.PILE_DEPTH_FRACTION * cardHeight,
        focusTop - StackTokens.PileTopGap.value * density,
    ).coerceAtLeast(0f)

    fun depthOf(a: CardAnim): Float = state.pos - a.slot

    /**
     * Visual top of a card at depth [d]. Behind the focus it climbs the perspective curve; in front
     * it slides straight down to the peek slot, then on out of the viewport.
     */
    fun topOf(d: Float): Float = when {
        d >= 0f -> focusTop - pileDepth * DepthCurve.rise(d)
        d >= -1f -> focusTop + (peekTop - focusTop) * DepthCurve.drop(d)
        else -> peekTop + (-1f - d) * (viewportHeight - peekTop)
    }

    private fun visible(d: Float) = d <= DepthCurve.MAX_VISIBLE_D && d >= DepthCurve.MIN_VISIBLE_D

    fun applyTo(layer: GraphicsLayerScope, a: CardAnim) = with(layer) {
        val d = depthOf(a)
        if (!visible(d) || (a.gone && a.fly == 0f && a.swipeX == 0f)) {
            alpha = 0f
            return@with
        }
        val s = DepthCurve.scale(d) * (1f + StackTokens.LIFT_SCALE * a.lift)
        scaleX = s
        scaleY = s
        transformOrigin = TransformOrigin(0.5f, 0f)

        // Swipe / fly-off: offset, tilt and fade, interpolated towards the fly-off target.
        val x: Float
        val tilt: Float
        val swipeAlpha: Float
        if (a.fly > 0f) {
            val e = a.fly
            val toX = a.flyDir * StackTokens.FlyOffDistance.value * density
            x = a.flyFromX + (toX - a.flyFromX) * e
            tilt = tiltFor(a.flyFromX) + (a.flyDir * StackTokens.FLY_OFF_TILT - tiltFor(a.flyFromX)) * e
            swipeAlpha = alphaFor(a.flyFromX) * (1f - e)
        } else {
            x = a.swipeX
            tilt = tiltFor(x)
            swipeAlpha = alphaFor(x)
        }
        // The tilt pivots on the card's bottom centre: rotate about the top, then shift back.
        val h = cardHeight * s
        val rad = tilt * PI.toFloat() / 180f
        rotationZ = tilt
        translationX = x + h * sin(rad)
        translationY = topOf(d) + h * (1f - cos(rad))
        alpha = DepthCurve.alpha(d) * swipeAlpha
        shape = CardShape
        clip = true
        shadowElevation = (StackTokens.Shadow.value + a.lift * StackTokens.LiftShadow.value) * density
        ambientShadowColor = Color.Black
        spotShadowColor = Color.Black
    }

    private fun tiltFor(xPx: Float) = xPx / density * StackTokens.SWIPE_TILT_PER_DP

    private fun alphaFor(xPx: Float) =
        (1f - abs(xPx) / (StackTokens.SwipeFadeDistance.value * density)).coerceAtLeast(StackTokens.SWIPE_MIN_ALPHA)

    /** Front-most card under [p]. Lifted card first, then by index (higher = drawn on top). */
    fun hitTest(p: Offset, ids: List<String>): Hit? {
        val order = buildList {
            state.liftedId?.let { add(it) }
            for (i in ids.indices.reversed()) if (ids[i] != state.liftedId) add(ids[i])
        }
        for (id in order) {
            val a = state.cards[id] ?: continue
            val d = depthOf(a)
            if (a.gone || !visible(d)) continue
            val s = DepthCurve.scale(d)
            val top = topOf(d)
            val w = cardWidth * s
            val left = (width - w) / 2f
            if (p.y in top..(top + cardHeight * s) && p.x in left..(left + w)) {
                val focused = abs(d) < 0.5f
                val onClose = focused && p.y < top + headerPx * s && p.x > left + w - closePx * s
                return Hit(id, focused, onClose)
            }
        }
        return null
    }
}

private suspend fun AwaitPointerEventScope.trackUntilUp(
    pointer: PointerId,
    tracker: VelocityTracker,
    onMove: (PointerInputChange) -> Unit,
) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer } ?: return
        if (!change.pressed) return
        tracker.addPosition(change.uptimeMillis, change.position)
        onMove(change)
        change.consume()
    }
}

/** Swipe-to-close: the card follows the finger; released past 100dp it flies off, else springs back. */
private suspend fun AwaitPointerEventScope.swipeCard(
    state: StackState,
    a: CardAnim,
    pointer: PointerId,
    onFlyOff: (dir: Float) -> Unit,
) {
    trackUntilUp(pointer, VelocityTracker()) { a.swipeX += it.positionChange().x }
    if (abs(a.swipeX) > StackTokens.SwipeCloseDistance.toPx()) {
        onFlyOff(sign(a.swipeX))
    } else {
        state.launch { animate(a.swipeX, 0f, 0f, Motion.SwipeReturn) { v, _ -> a.swipeX = v } }
    }
}

/**
 * Long-press reorder within the stack. The lifted card follows the finger along the slots; the
 * others spring out of its way. Returns the new order, or null if nothing moved.
 */
private suspend fun AwaitPointerEventScope.reorder(
    state: StackState,
    id: String,
    ids: List<String>,
    pointer: PointerId,
    onSlotChange: () -> Unit,
): List<String>? {
    val a = state.cards[id] ?: return null
    state.liftedId = id
    a.slotJob?.cancel()
    state.launch { animate(a.lift, 1f, 0f, Motion.Lift) { v, _ -> a.lift = v } }
    val start = ids.indexOf(id)
    var dy = 0f
    var target = start
    val others = ids - id
    trackUntilUp(pointer, VelocityTracker()) { change ->
        dy += change.positionChange().y
        // Down = towards the front of the stack = higher index.
        a.slot = (start + dy / state.slotPx).coerceIn(-0.4f, state.last + 0.4f)
        val t = a.slot.roundToInt().coerceIn(0, state.last)
        if (t != target) {
            target = t
            onSlotChange()
            others.forEachIndexed { i, other ->
                state.cards[other]?.let { state.animateSlot(it, (if (i >= target) i + 1 else i).toFloat()) }
            }
        }
    }
    state.animateSlot(a, target.toFloat())
    state.launch { animate(a.lift, 0f, 0f, Motion.Drop) { v, _ -> a.lift = v } }
    state.liftedId = null
    if (target == start) return null
    return others.toMutableList().apply { add(target, id) }
}
