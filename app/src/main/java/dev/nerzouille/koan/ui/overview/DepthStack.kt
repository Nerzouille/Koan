package dev.nerzouille.koan.ui.overview

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.StackTokens
import kotlin.math.PI
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
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
    /**
     * This stack's page is on screen (read in layers). Pages composed ahead of time but not shown
     * get no blur: the renderer would still render every blurred card into a ~4 MB buffer and cache
     * those buffers until Koan goes to the background.
     */
    onScreen: () -> Boolean = { true },
    /**
     * How visible this stack's page is while spaces switch (1 = settled here, 0 = a whole page
     * away), read in layers. Cards fade through their own layer alpha: they have elevation, so the
     * renderer draws them above anything the page paints over them (an overlay can't hide them).
     */
    pageFade: () -> Float = { 1f },
    /**
     * The overview's open motion has finished. Until then only the visible cards are composed (the
     * open's first frame is the costly one); the margin and the cards ahead come after.
     */
    settled: Boolean = true,
) {
    val density = LocalDensity.current
    val view = LocalView.current

    // What the stack shows. New data waits until no finger is down and nothing moves, then lands in
    // one snapshot with the focused card anchored: a sync never moves what the user is looking at.
    var shown by remember { mutableStateOf(cards) }

    // Order committed by a drop, shown until Room emits it back.
    var pendingOrder by remember { mutableStateOf<List<String>?>(null) }
    // Cards closed here: out of the stack at once (the gap closes while they fly off), whatever
    // Room still says, until Room drops them too.
    var removed by remember { mutableStateOf(emptySet<String>()) }
    val flying = remember { mutableStateMapOf<String, Flying>() }
    val roomIds = shown.map { it.id }
    val ordered = pendingOrder?.takeIf { it.size == roomIds.size && it.toSet() == roomIds.toSet() } ?: roomIds
    if (pendingOrder != null && pendingOrder == roomIds) pendingOrder = null
    val ids = if (removed.isEmpty()) ordered else ordered.filter { it !in removed }
    val byId = remember(shown) { shown.associateBy { it.id } }
    val displayed by rememberUpdatedState(ids)
    LaunchedEffect(cards) {
        if (cards == shown) return@LaunchedEffect
        snapshotFlow { state.busy }.first { !it }
        Snapshot.withMutableSnapshot {
            val stillThere = removed.filterTo(HashSet()) { id -> cards.any { it.id == id } }
            val next = cards.filter { it.id !in stillThere }
            val from = state.pos.roundToInt()
            val focused = displayed.getOrNull(from)
            val to = focused?.let { id -> next.indexOfFirst { it.id == id } } ?: -1
            if (to >= 0) state.shiftBy(to - from)
            shown = cards
            removed = stillThere
        }
    }
    // A card put back (Undo) comes into focus once the stack shows it.
    LaunchedEffect(ids, state.focusId) {
        val id = state.focusId ?: return@LaunchedEffect
        // Back before Room dropped it here: it stops being hidden (this runs again with it shown).
        if (id in removed) {
            removed = removed - id
            return@LaunchedEffect
        }
        val i = ids.indexOf(id)
        if (i < 0) return@LaunchedEffect
        state.focusId = null
        state.animateTo(i.toFloat())
    }

    state.last = (ids.size - 1).coerceAtLeast(0)
    state.slotPx = with(density) { StackTokens.SlotDrag.toPx() }

    // Only cards near the focus are composed (a space can hold hundreds). Composing a card and
    // recording its first frame costs ~4–7 ms, enough to miss a 120 Hz frame, so it happens while
    // the stack is idle: [ahead] grows a few cards per frame around the focus and does not move
    // during a gesture. The window only extends mid-gesture when a long fling outruns it.
    var ahead by remember { mutableStateOf(IntRange.EMPTY) }
    val latestSettled = rememberUpdatedState(settled)
    val window by remember {
        derivedStateOf {
            val margin = if (latestSettled.value) WINDOW_MARGIN else 0
            val lo = floor(state.pos - DepthCurve.MAX_VISIBLE_D).toInt() - margin
            val hi = ceil(state.pos - DepthCurve.MIN_VISIBLE_D).toInt() + margin
            val a = ahead
            if (a.isEmpty()) lo..hi else minOf(lo, a.first)..maxOf(hi, a.last)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { latestSettled.value }.first { it }
        // Also when the focus jumps while idle (the overview re-centres a stack on its current tab).
        snapshotFlow { if (state.busy) null else state.pos.roundToInt() }.collect { focus ->
            if (focus == null) return@collect
            val target = (focus - AHEAD_BEHIND)..(focus + AHEAD_FRONT)
            // Drop what is far away at once (cheap), then compose towards the target a few cards
            // per frame, so a finger landing meanwhile still gets a prompt frame.
            var cur = window.first.coerceAtLeast(target.first)..window.last.coerceAtMost(target.last)
            ahead = cur
            while (cur != target && !state.busy) {
                withFrameNanos { }
                cur = (cur.first - AHEAD_STEP).coerceAtLeast(target.first)..(cur.last + AHEAD_STEP).coerceAtMost(target.last)
                ahead = cur
            }
        }
    }

    LaunchedEffect(ids) {
        state.cards.keys.retainAll(ids.toSet() + flying.keys)
        val near = window.first - WINDOW_MARGIN..window.last + WINDOW_MARGIN
        ids.forEachIndexed { i, id ->
            val a = state.anim(id, i)
            if (a.slot != i.toFloat() && state.liftedId != id) {
                // Off-screen cards jump straight to their slot; only what can be seen animates.
                if (i in near || a.slot.roundToInt() in near) state.animateSlot(a, i.toFloat()) else a.slot = i.toFloat()
            }
        }
        if (state.pos > state.last) state.animateTo(state.last.toFloat())
    }

    val blurs = remember(density) {
        // 0.5dp steps; RenderEffects are immutable so the layer can reuse them every frame.
        val steps = (StackTokens.BlurDp.max() * 2f).roundToInt() + 1
        Array(steps) { step -> if (step == 0) null else BlurEffect(step * 0.5f * density.density, step * 0.5f * density.density, TileMode.Clamp) }
    }
    val currentOnScreen by rememberUpdatedState(onScreen)
    val currentPageFade by rememberUpdatedState(pageFade)
    val blurFor: (Float) -> RenderEffect? = remember(blurs) {
        { d ->
            // A card that can't be seen gets no blur: every blurred layer is an offscreen buffer
            // (~3 MB) that the renderer allocates even off screen and caches long after.
            if (d > DepthCurve.MAX_VISIBLE_D || d < DepthCurve.MIN_VISIBLE_D || !currentOnScreen()) null
            else blurs[(DepthCurve.blurDp(d) * 2f).roundToInt().coerceIn(0, blurs.size - 1)]
        }
    }

    val palette = KoanTheme.colors
    // Light: back cards fade into the background (a haze); dark: they darken.
    val dimColor = if (palette.DimToBase) palette.Base else Color.Black

    val hero = LocalCardHero.current
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    BoxWithConstraints(modifier.clipToBounds().onGloballyPositioned { coordinates[0] = it }) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val geo = remember(width, height, density, palette) { StackGeometry(state, width, height, density.density, palette) }
        val cardWidthDp = with(density) { geo.cardWidth.toDp() }
        val cardHeightDp = with(density) { geo.cardHeight.toDp() }

        val currentIds by rememberUpdatedState(ids)
        // The card flying to or from the page (CardHero) lands where this stack shows it, while
        // this stack's page is the one settled on screen.
        SideEffect { hero.cardSize = Size(geo.cardWidth, geo.cardHeight) }
        DisposableEffect(hero, state, geo) {
            hero.register(state) { id ->
                val i = currentIds.indexOf(id)
                val stack = coordinates[0]
                val root = hero.overview
                if (i < 0 || stack == null || root == null || !stack.isAttached || !root.isAttached) return@register null
                if (!currentOnScreen() || currentPageFade() < 1f) return@register null
                val d = geo.depthOf(state.anim(id, i))
                val scale = DepthCurve.scale(d)
                val origin = root.localPositionOf(stack, Offset.Zero)
                val left = origin.x + (width - geo.cardWidth * scale) / 2f
                val top = origin.y + geo.topOf(d)
                Rect(left, top, left + geo.cardWidth * scale, top + geo.cardHeight * scale)
            }
            onDispose { hero.unregister(state) }
        }
        val currentById by rememberUpdatedState(byId)
        val currentOnOpen by rememberUpdatedState(onOpen)
        val currentOnClose by rememberUpdatedState(onClose)
        val currentOnReorder by rememberUpdatedState(onReorder)

        /**
         * Closes the card: it leaves the stack now and flies off sideways (ease-in, 480dp, 8°) while
         * the gap closes. Nothing waits for it: the stack scrolls, closes and opens meanwhile.
         * Closing the focused card brings forward the card on the side the user last scrolled
         * towards (behind if they haven't), or the other one at that end of the stack.
         */
        fun flyOff(id: String, dir: Float) {
            val ids = currentIds
            val i = ids.indexOf(id)
            val card = currentById[id]
            if (i < 0 || card == null) return
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val a = state.anim(id, i)
            a.flyDepth = geo.depthOf(a)
            a.gone = true
            a.flyFromX = a.swipeX
            a.flyDir = dir
            flying[id] = Flying(card, a, inFrontOf = ids.getOrNull(i + 1))
            removed = removed + id
            val behindForward = closeBringsBehind(i, state.pos.roundToInt(), ids.lastIndex, state.lastDirection)
            // The cards behind move one slot down the pile with the focus; the ones in front stay.
            if (behindForward) state.shiftBy(-1)
            currentOnClose(id)
            state.launch {
                try {
                    state.animateFlyOff(a)
                } finally {
                    flying.remove(id)
                }
            }
        }

        Box(
            Modifier
                .size(maxWidth, maxHeight)
                // Mouse wheel / trackpad (a mouse, scrcpy): the same scroll as a finger drag.
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val factor = android.view.ViewConfiguration.get(view.context).scaledVerticalScrollFactor
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            val dy = change.scrollDelta.y
                            if (dy == 0f || state.liftedId != null) continue
                            state.wheel(-dy * factor)
                            change.consume()
                        }
                    }
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
                                    view.performHapticFeedback(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK)
                                }?.let { order ->
                                    pendingOrder = order
                                    currentOnReorder(order)
                                }
                            }
                        }
                        state.endTouch()
                    }
                },
        ) {
            val visible = window
            // Cards flying off draw where they were in the pile: over the cards behind, under the ones in front.
            val flyingByAnchor = flying.values.groupBy { f -> f.inFrontOf?.takeIf { it in ids && ids.indexOf(it) in visible } }
            ids.forEachIndexed { index, id ->
                flyingByAnchor[id]?.forEach { FlyingCard(it, geo, blurFor, dimColor, cardWidthDp, cardHeightDp, currentPageFade) }
                if (index !in visible && state.liftedId != id) return@forEachIndexed
                val card = byId[id] ?: return@forEachIndexed
                key(id) {
                    val a = state.anim(id, index)
                    // Off-screen cards (composed ahead of time) must not redraw every frame: their
                    // inner effects read the live depth only while the card is near the visible range.
                    val near = remember(a, geo) {
                        derivedStateOf { geo.depthOf(a).let { it >= DepthCurve.MIN_VISIBLE_D - 1f && it <= DepthCurve.MAX_VISIBLE_D + 1f } }
                    }
                    // This card casts a short contact shadow on the one behind it, from a layer just under it.
                    val behind = ids.getOrNull(index - 1)?.let { state.anim(it, index - 1) }
                    val layout = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(if (state.liftedId == id) 1f else 0f)
                        .size(cardWidthDp, cardHeightDp)
                    if (behind != null) {
                        ContactShadow(
                            length = { if (near.value) geo.shadowLength(a, behind) else 0f },
                            modifier = layout.graphicsLayer {
                                geo.applyTo(this, a)
                                alpha *= if (hero.hides(id)) 0f else currentPageFade()
                                clip = false
                                shadowElevation = 0f
                            },
                        )
                    }
                    TabCard(
                        card = card,
                        depth = { if (near.value) geo.depthOf(a) else OFFSCREEN_DEPTH },
                        blurFor = blurFor,
                        dimColor = dimColor,
                        modifier = layout.graphicsLayer {
                            geo.applyTo(this, a)
                            // In flight over the overview (CardHero): the real card waits hidden.
                            alpha *= if (hero.hides(id)) 0f else currentPageFade()
                        },
                    )
                }
            }
            flyingByAnchor[null]?.forEach { FlyingCard(it, geo, blurFor, dimColor, cardWidthDp, cardHeightDp, currentPageFade) }
        }
    }
}

/** A closed card on its way out; [inFrontOf]: the card that was in front of it (drawn over it). */
private class Flying(val card: CardModel, val anim: CardAnim, val inFrontOf: String?)

@Composable
private fun BoxScope.FlyingCard(
    f: Flying,
    geo: StackGeometry,
    blurFor: (Float) -> RenderEffect?,
    dimColor: Color,
    width: Dp,
    height: Dp,
    pageFade: () -> Float,
) = key("flying:${f.card.id}") {
    TabCard(
        card = f.card,
        depth = { geo.depthOf(f.anim) },
        blurFor = blurFor,
        dimColor = dimColor,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .size(width, height)
            .graphicsLayer {
                geo.applyTo(this, f.anim)
                alpha *= pageFade()
            },
    )
}

private enum class Gesture { Tap, Drag, LongPress, Cancel }

/** Extra cards composed beyond the visible range on each side, so none pops in at the edges. */
private const val WINDOW_MARGIN = 2

/** Cards kept composed around the focus while idle: deeper into the pile, and in front. */
private const val AHEAD_BEHIND = 16
private const val AHEAD_FRONT = 10

/** What an off-screen card's inner effects see: a constant, so they never redraw. */
private const val OFFSCREEN_DEPTH = 99f

/** Cards added to each side per idle frame. */
private const val AHEAD_STEP = 1

private class Hit(val id: String, val focused: Boolean, val onClose: Boolean)

private val CardShape = RoundedCornerShape(CardTokens.Radius)

/**
 * The stack's frame in px, independent of any scroll state: card size and where a card at depth d
 * sits. Shared by the real stack and the overview's ghost of a space not composed yet.
 */
internal class StackFrame(width: Float, private val viewportHeight: Float, density: Float) {
    val cardWidth = width - 2 * StackTokens.CardSideMargin.value * density

    /** The viewport runs on under the dock's rounded top; the layout ends at the dock's edge. */
    private val visibleBottom = viewportHeight - StackTokens.UnderDock.value * density

    /** The focused card gets all the height left by the top gap, the pile (A = fraction × height) and the peek. */
    val cardHeight = (
        visibleBottom - (StackTokens.PileTopGap + StackTokens.PeekVisible - StackTokens.PeekOverlap).value * density
        ) / (1f + StackTokens.PILE_DEPTH_FRACTION)

    /** Top of the card in front (d = -1): only its header and a hint of thumbnail show. */
    private val peekTop = visibleBottom - StackTokens.PeekVisible.value * density

    /** Top of the focused card: the card in front overlaps its bottom part. */
    private val focusTop = peekTop + StackTokens.PeekOverlap.value * density - cardHeight

    /** A: the pile converges this far above the focused card, never into the header. */
    private val pileDepth = min(
        StackTokens.PILE_DEPTH_FRACTION * cardHeight,
        focusTop - StackTokens.PileTopGap.value * density,
    ).coerceAtLeast(0f)

    /**
     * Visual top of a card at depth [d]. Behind the focus it climbs the perspective curve; in front
     * it slides straight down to the peek slot, then on out of the viewport.
     */
    fun topOf(d: Float): Float = when {
        d >= 0f -> focusTop - pileDepth * DepthCurve.rise(d)
        d >= -1f -> focusTop + (peekTop - focusTop) * DepthCurve.drop(d)
        else -> peekTop + (-1f - d) * (viewportHeight - peekTop)
    }
}

/** Positions of everything in the stack, in px, as functions of the scroll state. */
private class StackGeometry(
    val state: StackState,
    val width: Float,
    val viewportHeight: Float,
    val density: Float,
    val palette: KoanPalette,
) {
    private val frame = StackFrame(width, viewportHeight, density)
    val cardWidth = frame.cardWidth
    val cardHeight = frame.cardHeight
    private val headerPx = CardTokens.HeaderHeight.value * density
    private val closePx = CardTokens.CloseTouch.value * density

    fun topOf(d: Float): Float = frame.topOf(d)

    fun depthOf(a: CardAnim): Float = if (a.flyDepth.isNaN()) state.pos - a.slot else a.flyDepth

    private fun visible(d: Float) = d <= DepthCurve.MAX_VISIBLE_D && d >= DepthCurve.MIN_VISIBLE_D

    /**
     * How far [a]'s contact shadow rises above its top edge onto [behind], in [a]'s own px: the
     * full length, shorter where the pile leaves only a sliver of [behind] showing. 0 = none.
     */
    fun shadowLength(a: CardAnim, behind: CardAnim): Float {
        val d = depthOf(a)
        val db = depthOf(behind)
        if (!visible(d) || !visible(db) || behind.gone) return 0f
        val s = DepthCurve.scale(d)
        val sliver = (topOf(d) - topOf(db)) / s
        return min(palette.ContactShadowLength.value * density, sliver * CardTokens.CONTACT_SHADOW_MAX_FRACTION).coerceAtLeast(0f)
    }

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
        // Fade by modulating each draw, never through an offscreen buffer: a layer at partial
        // alpha is otherwise promoted to its own ~5 MB GPU buffer (every fading card and contact
        // shadow, on every page composed ahead), and the renderer keeps those buffers cached
        // until Koan goes to the background.
        compositingStrategy = CompositingStrategy.ModulateAlpha
        shape = CardShape
        clip = true
        shadowElevation = (palette.CardElevation.value + a.lift * StackTokens.LiftShadow.value) * density
        ambientShadowColor = palette.CardShadowColor
        spotShadowColor = palette.CardShadowColor
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
