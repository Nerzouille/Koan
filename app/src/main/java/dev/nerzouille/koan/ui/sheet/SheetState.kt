package dev.nerzouille.koan.ui.sheet

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.SheetTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Where a sheet rests: closed, compact (fits its content, no scrolling) or full screen. */
enum class Detent { Closed, Compact, Full }

/**
 * The detent engine every Koan sheet shares (the pins drawer, the menu, floating cards). Its
 * [height] follows the finger 1:1; only the full detent lets a list taller than the screen
 * scroll, and only once the sheet is all the way up (a drag down at the top of the list collapses
 * the sheet instead). [height] and [scroll] are read in layers and draw only.
 *
 * A sheet has pages (the drawer: one per space; the menu: the pages pushed into it); [refit] says
 * how tall each one is and which one is on screen.
 */
@Stable
class SheetState(private val scope: CoroutineScope, private val decay: DecayAnimationSpec<Float>) {
    /** Visible height of the sheet above the bar's rows (px), 0 = closed. */
    var height by mutableFloatStateOf(0f)
        private set

    /** How far the full sheet's list has scrolled (px). */
    var scroll by mutableFloatStateOf(0f)
        private set

    /** Where it rests or is going: flips on release or tap. Decides whether the sheet takes input. */
    var detent by mutableStateOf(Detent.Closed)
        private set
    val open: Boolean get() = detent != Detent.Closed

    /** Height the drawer is laid out at (the tallest space's content, capped at full). */
    var layoutPx by mutableFloatStateOf(0f)
        internal set

    /** Caps set from the window size: compact keeps the dock under ~55% of the screen. */
    var compactCapPx = 1f
    var fullCapPx = 1f
    var minStepPx = 0f

    /** Natural (unscrolled) content height of each space's page, and the page on screen. */
    private var pageHeights = IntArray(0)
    private var page = 0
    private val contentPx: Float get() = pageHeights.getOrElse(page) { 0 }.toFloat()
    private val compactPx: Float get() = minOf(contentPx, compactCapPx).coerceAtLeast(1f)
    private val fullPx: Float get() = minOf(contentPx, fullCapPx).coerceAtLeast(compactPx)
    private val maxScroll: Float get() = (contentPx - fullPx).coerceAtLeast(0f)

    /** 0 closed … 1 at the compact height (scrim, blur, corners); stays 1 above it. */
    val progress: Float get() = (height / compactPx).coerceIn(0f, 1f)

    private var job: Job? = null
    private var from = Detent.Closed
    private var dragging = false

    private fun stops(): List<Pair<Detent, Float>> = buildList {
        add(Detent.Closed to 0f)
        add(Detent.Compact to compactPx)
        if (fullPx - compactPx >= minStepPx) add(Detent.Full to fullPx)
    }

    private fun stopPx(d: Detent): Float = stops().lastOrNull { it.first <= d }?.second ?: 0f

    /** Top of the visible sheet inside the drawer's layout box. */
    fun sheetTop(): Float = layoutPx - height

    /** Finger down: freeze. A step that was still landing counts as landed. */
    fun grab() {
        job?.cancel()
        dragging = true
        from = detent
    }

    /**
     * Finger moved by [dyPx] (> 0 = down). Up grows the sheet to full, then scrolls its list; down
     * scrolls the list back to its top first, then shrinks the sheet.
     */
    fun dragBy(dyPx: Float) {
        var d = -dyPx
        if (d > 0f) {
            val grow = minOf(d, fullPx - height).coerceAtLeast(0f)
            height += grow
            d -= grow
            if (d > 0f) scroll = (scroll + d).coerceAtMost(maxScroll)
        } else {
            val back = minOf(-d, scroll)
            scroll -= back
            d += back
            if (d < 0f) height = (height + d).coerceAtLeast(0f)
        }
    }

    /**
     * Finger up with vertical velocity [vyPx] px/s. A fling goes to the next detent that way; a
     * slow release commits to the neighbouring detent past [SheetTokens.COMMIT_FRACTION] of the
     * gap. A fling of a scrolled full list keeps scrolling it. Returns true when the drawer lands
     * on another detent than the one it started from.
     */
    fun release(vyPx: Float, flingPx: Float): Boolean {
        dragging = false
        val atFull = maxScroll > 0f && height >= fullPx - 0.5f
        if (atFull && (scroll > 0f || vyPx < 0f)) {
            detent = Detent.Full
            flingList(-vyPx)
            return from != Detent.Full
        }
        val stops = stops()
        val target = when {
            vyPx < -flingPx -> stops.firstOrNull { it.second > height + 1f } ?: stops.last()
            vyPx > flingPx -> stops.lastOrNull { it.second < height - 1f } ?: stops.first()
            else -> {
                // Between stops j and j + 1: leaving the start stop commits early, coming back late.
                val j = stops.indexOfLast { it.second <= height }.coerceIn(0, stops.lastIndex)
                if (j == stops.lastIndex) {
                    stops[j]
                } else {
                    val f = (height - stops[j].second) / (stops[j + 1].second - stops[j].second)
                    val up = stops.indexOfFirst { it.first == from } <= j
                    val commit = if (up) SheetTokens.COMMIT_FRACTION else 1f - SheetTokens.COMMIT_FRACTION
                    if (f > commit) stops[j + 1] else stops[j]
                }
            }
        }.first
        animateTo(target, -vyPx)
        return target != from
    }

    /** Settles on [target] ([velocity] in px/s, > 0 = growing). */
    fun animateTo(target: Detent, velocity: Float = 0f) {
        detent = target
        job?.cancel()
        val to = stopPx(target)
        job = scope.launch {
            if (scroll > 0f && target != Detent.Full) launch { animate(scroll, 0f, 0f, Motion.Drawer) { v, _ -> scroll = v.coerceAtLeast(0f) } }
            animate(height, to, velocity, Motion.Drawer) { v, _ -> height = v.coerceAtLeast(0f) }
        }
    }

    fun toggle() = animateTo(if (open) Detent.Closed else Detent.Compact)

    fun snapClosed() {
        job?.cancel()
        dragging = false
        height = 0f
        scroll = 0f
        detent = Detent.Closed
    }

    private fun flingList(velocity: Float) {
        job?.cancel()
        job = scope.launch {
            AnimationState(scroll, velocity).animateDecay(decay) {
                scroll = value.coerceIn(0f, maxScroll)
                if (value <= 0f || value >= maxScroll) cancelAnimation()
            }
        }
    }

    /**
     * The pages were measured ([heights]) or the space on screen changed ([settled]): the sheet
     * keeps its detent, at the new content's height, and the list starts from its top.
     */
    internal fun refit(heights: IntArray, settled: Int) {
        val changed = !heights.contentEquals(pageHeights) || settled != page
        if (!changed) return
        if (settled != page) scroll = 0f
        pageHeights = heights
        page = settled
        if (dragging || detent == Detent.Closed) return
        scroll = scroll.coerceAtMost(maxScroll)
        val to = stopPx(detent)
        if (to != height) {
            job?.cancel()
            job = scope.launch { animate(height, to, 0f, Motion.Drawer) { v, _ -> height = v.coerceAtLeast(0f) } }
        }
    }
}
