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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The upright perspective stack (docs/UI_SPEC.md "Depth stack"). Cards are laid out once; every frame of
 * scrolling, closing or reordering only touches graphicsLayer/draw lambdas.
 *
 * [blockHeight] is the height (px) of the essentials/pinned block revealed above the stack.
 */
@Composable
fun DepthStack(
    cards: List<CardModel>,
    state: StackState,
    blockHeight: () -> Float,
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
        val geo = remember(width, height, density) {
            StackGeometry(state, width, height, density.density, blockHeight)
        }
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

        Box(
            Modifier
                .size(maxWidth, maxHeight)
                .drawWithContent {
                    // Cards never cover the revealed header block, and dissolve before the dock.
                    val top = (blockHeight() * state.expansion - StackTokens.BlockOverlap.toPx()).coerceAtLeast(0f)
                    clipRect(top = top) { this@drawWithContent.drawContent() }
                    drawRect(fade)
                }
                .pointerInput(enabled, geo) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        state.startDrag()
                        val hit = geo.hitTest(down.position, currentIds)
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)

                        // Decide: tap, drag (scroll or swipe) or long press.
                        var lifted = false
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
                        } ?: if (hit != null) Gesture.LongPress.also { lifted = true } else Gesture.Drag

                        when (result) {
                            Gesture.Cancel -> state.fling(0f)
                            Gesture.Tap -> when {
                                hit == null -> state.fling(0f)
                                hit.onClose -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    val a = state.anim(hit.id, 0)
                                    a.gone = true
                                    state.launch {
                                        state.animateExit(a)
                                        currentOnClose(hit.id)
                                    }
                                }
                                else -> currentOnOpen(hit.id)
                            }
                            Gesture.Drag -> if (hit != null && abs(slop.x) > abs(slop.y)) {
                                swipeCard(state, state.anim(hit.id, 0), width, tracker, down.id) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    currentOnClose(hit.id)
                                }
                            } else {
                                scrollStack(state, tracker, down.id)
                            }
                            Gesture.LongPress -> if (lifted && hit != null) {
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

private class Hit(val id: String, val onClose: Boolean)

/** Positions of everything in the stack, in px, as functions of the scroll state. */
private class StackGeometry(
    val state: StackState,
    val width: Float,
    val viewportHeight: Float,
    val density: Float,
    val blockHeight: () -> Float,
) {
    val cardWidth = width - 2 * StackTokens.CardSideMargin.value * density
    val cardHeight = viewportHeight * StackTokens.CARD_HEIGHT_FRACTION
    private val headerPx = CardTokens.HeaderHeight.value * density
    private val closePx = CardTokens.CloseTouch.value * density

    /** Top of the peeking card (d = -1): only its header and a bit show above the viewport bottom. */
    private val peekTop = viewportHeight - StackTokens.PeekVisible.value * density

    /** Top of the focused card with the header block collapsed: its bottom sits just above the peek. */
    private val focusTopCollapsed = peekTop - StackTokens.PeekGap.value * density - cardHeight

    /** A: the pile converges this far above the focused card, never into the header. */
    private val pileDepth = min(
        StackTokens.PileDepth.value * density,
        focusTopCollapsed - StackTokens.PileTopGap.value * density,
    ).coerceAtLeast(0f)

    fun depthOf(a: CardAnim): Float = state.stackPos - a.slot

    /** The focused card moves down as the header block is revealed, and further on overscroll. */
    fun focusTop(): Float {
        val e = state.expansion
        val expanded = blockHeight() + StackTokens.RevealGap.value * density
        val overscroll = (REVEAL_STOP - state.pos).coerceAtLeast(0f) * state.slotPx * StackTokens.OVERSCROLL_SPREAD
        return focusTopCollapsed + (expanded - focusTopCollapsed) * e + overscroll
    }

    /**
     * Visual top of a card at depth [d]. Behind the focus it climbs the perspective curve; in front
     * it slides straight down to the peek slot, then on out of the viewport.
     */
    fun topOf(d: Float): Float {
        val focus = focusTop()
        if (d >= 0f) return focus - pileDepth * DepthCurve.rise(d)
        val peek = max(peekTop, focus + cardHeight + StackTokens.PeekGap.value * density)
        if (d >= -1f) return focus + (peek - focus) * DepthCurve.drop(d)
        return peek + (-1f - d) * (viewportHeight - peek)
    }

    private fun visible(d: Float) = d <= DepthCurve.MAX_VISIBLE_D && d >= DepthCurve.MIN_VISIBLE_D

    fun applyTo(layer: GraphicsLayerScope, a: CardAnim) = with(layer) {
        val d = depthOf(a)
        if (!visible(d) || (a.gone && a.exit == 0f && a.swipeX == 0f)) {
            alpha = 0f
            return@with
        }
        val s = DepthCurve.scale(d) * (1f + StackTokens.LIFT_SCALE * a.lift) * (1f - StackTokens.EXIT_SCALE * a.exit)
        scaleX = s
        scaleY = s
        transformOrigin = TransformOrigin(0.5f, 0f)
        translationX = a.swipeX
        translationY = topOf(d) - a.exit * StackTokens.ExitSlide.value * density
        alpha = DepthCurve.alpha(d) * (1f - (abs(a.swipeX) / width) * 0.9f).coerceAtLeast(0f) * (1f - a.exit)
        shape = CardShape
        clip = true
        shadowElevation = (DepthCurve.shadow(d) * StackTokens.ShadowMax.value + a.lift * StackTokens.LiftShadow.value) * density
        ambientShadowColor = Color.Black
        spotShadowColor = Color.Black
    }

    /** Front-most card under [p]. Lifted card first, then by index (higher = drawn on top). */
    fun hitTest(p: Offset, ids: List<String>): Hit? {
        val order = buildList {
            state.liftedId?.let { add(it) }
            for (i in ids.indices.reversed()) if (ids[i] != state.liftedId) add(ids[i])
        }
        val clipTop = blockHeight() * state.expansion
        if (p.y < clipTop) return null
        for (id in order) {
            val a = state.cards[id] ?: continue
            val d = depthOf(a)
            if (a.gone || !visible(d)) continue
            val s = DepthCurve.scale(d)
            val top = topOf(d)
            val w = cardWidth * s
            val left = (width - w) / 2f + a.swipeX
            if (p.y in top..(top + cardHeight * s) && p.x in left..(left + w)) {
                val onClose = abs(d) < 0.5f && p.y < top + headerPx * s && p.x > left + w - closePx * s
                return Hit(id, onClose)
            }
        }
        return null
    }
}

private val CardShape = RoundedCornerShape(CardTokens.Radius)

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.trackUntilUp(
    pointer: androidx.compose.ui.input.pointer.PointerId,
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

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.scrollStack(
    state: StackState,
    tracker: VelocityTracker,
    pointer: androidx.compose.ui.input.pointer.PointerId,
) {
    trackUntilUp(pointer, tracker) { state.dragBy(it.positionChange().y) }
    state.fling(tracker.calculateVelocity().y)
}

/** Swipe-to-close: card follows the finger, flies off past 35% width or on a fling. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.swipeCard(
    state: StackState,
    a: CardAnim,
    width: Float,
    tracker: VelocityTracker,
    pointer: androidx.compose.ui.input.pointer.PointerId,
    onClosed: () -> Unit,
) {
    trackUntilUp(pointer, tracker) { a.swipeX += it.positionChange().x }
    val vx = tracker.calculateVelocity().x
    val flung = abs(vx) > StackTokens.SwipeCloseVelocity.toPx() && sign(vx) == sign(a.swipeX)
    if (abs(a.swipeX) > width * StackTokens.SWIPE_CLOSE_FRACTION || flung) {
        a.gone = true
        val target = sign(if (a.swipeX != 0f) a.swipeX else vx) * width * StackTokens.SWIPE_FLY_OFF
        state.launch {
            animate(a.swipeX, target, vx, Motion.SwipeFlyOff) { v, _ -> a.swipeX = v }
            onClosed()
        }
    } else {
        state.launch { animate(a.swipeX, 0f, vx, Motion.SwipeReturn) { v, _ -> a.swipeX = v } }
    }
}

/**
 * Long-press reorder within the stack. The lifted card follows the finger along the slots; the
 * others spring out of its way. Returns the new order, or null if nothing moved.
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.reorder(
    state: StackState,
    id: String,
    ids: List<String>,
    pointer: androidx.compose.ui.input.pointer.PointerId,
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
