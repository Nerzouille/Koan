package dev.nerzouille.koan.ui.player

/*
 * What a tap on the video does (VIDEO_PLAYER.md §4, fixes F3 / F8 recheck). Whether the controls
 * are shown never changes it: with them hidden, a tap anywhere (where a button or the bar would
 * be too) shows them; only a double-tap on a side seeks instead.
 */

enum class TapAction {
    /** Locked: Unlock shows for 3 s. */
    ShowUnlock,

    /** Another tap on the side a seek is showing on: 10 s more. */
    SeekMore,

    /** The second tap of a pair on a side: a seek; the first tap's show / hide never happens. */
    SeekPair,

    /** In the middle: the controls show or hide at once. */
    ToggleNow,

    /** On a side: the controls show or hide after [PlayerTokens.TAP_PAIR_MS], unless a second tap comes. */
    ToggleLater,
}

/** [zone]: -1 left side, 1 right side, 0 middle. [seekSide]: the side a seek is showing on (0 none). */
fun tapAction(locked: Boolean, zone: Int, seekSide: Int, sinceSeekMs: Long, lastTapZone: Int, sinceTapMs: Long): TapAction = when {
    locked -> TapAction.ShowUnlock
    zone != 0 && zone == seekSide && sinceSeekMs < PlayerTokens.DOUBLE_TAP_WINDOW_MS -> TapAction.SeekMore
    zone != 0 && zone == lastTapZone && sinceTapMs < PlayerTokens.TAP_PAIR_MS -> TapAction.SeekPair
    zone == 0 -> TapAction.ToggleNow
    else -> TapAction.ToggleLater
}

/** Whether the controls' buttons, bar and pills take touches: only while they show. Hidden, every touch reaches the video. */
fun controlsTakeTouches(shown: Boolean, hidden: Boolean): Boolean = shown && !hidden
