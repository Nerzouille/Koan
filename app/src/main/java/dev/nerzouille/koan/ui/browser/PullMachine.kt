package dev.nerzouille.koan.ui.browser

import dev.nerzouille.koan.ui.theme.PullTokens
import kotlin.math.abs

/**
 * Pull to refresh's phases: [Idle] (nothing shows), [Pulling] (a finger holds it), [Refreshing]
 * (let go past the trigger, the page reloads), [Settling] (on its way back to 0, after a short
 * pull, a cancel or the load's end). A new pull can start in any phase but [Pulling]: even while
 * the page reloads (a fast double pull), from where it rests.
 */
enum class PullPhase { Idle, Pulling, Refreshing, Settling }

/** What happens to it. */
enum class PullEvent { Grab, ReleasePast, ReleaseShort, Cancel, Loaded, Settled }

/** What the page's engine said about the touch so far: it lets it go, keeps it (maps, its own pull), or hasn't said yet. */
enum class PageTouch { Free, Kept, Unknown }

/** Whether a touch becomes a pull: not yet, yes, or no (and why, for the log). */
sealed interface PullStart {
    data object Wait : PullStart
    data object Start : PullStart
    data class Reject(val reason: String) : PullStart
}

object PullRules {
    /** The next phase; an event that doesn't apply leaves the phase as it is. */
    fun next(phase: PullPhase, event: PullEvent): PullPhase = when (event) {
        PullEvent.Grab -> if (canGrab(phase)) PullPhase.Pulling else phase
        PullEvent.ReleasePast -> if (phase == PullPhase.Pulling) PullPhase.Refreshing else phase
        PullEvent.ReleaseShort, PullEvent.Cancel -> if (phase == PullPhase.Pulling) PullPhase.Settling else phase
        // A load ending never takes the page from under the finger.
        PullEvent.Loaded -> if (phase == PullPhase.Refreshing) PullPhase.Settling else phase
        PullEvent.Settled -> if (phase == PullPhase.Settling) PullPhase.Idle else phase
    }

    fun canGrab(phase: PullPhase): Boolean = phase != PullPhase.Pulling

    /**
     * A touch that started with the page at its top ([atTop], read when the finger went down) and
     * has moved ([dx], [dy] px from where it went down): a pull once it moves down more than
     * sideways past [slop], unless the page keeps the touch. While the engine hasn't answered yet
     * ([PageTouch.Unknown]) it waits, up to [PullTokens.PAGE_ANSWER_WAIT_MS], then pulls.
     */
    fun start(phase: PullPhase, atTop: Boolean, dx: Float, dy: Float, slop: Float, touch: PageTouch, waitedMs: Long): PullStart = when {
        !canGrab(phase) -> PullStart.Reject("busy: $phase")
        !atTop -> PullStart.Reject("page not at its top")
        abs(dx) < slop && abs(dy) < slop -> PullStart.Wait
        dy <= 0f -> PullStart.Reject("moved up")
        abs(dx) >= dy -> PullStart.Reject("moved sideways")
        touch == PageTouch.Kept -> PullStart.Reject("the page keeps the touch")
        touch == PageTouch.Unknown && waitedMs < PullTokens.PAGE_ANSWER_WAIT_MS -> PullStart.Wait
        else -> PullStart.Start
    }
}
