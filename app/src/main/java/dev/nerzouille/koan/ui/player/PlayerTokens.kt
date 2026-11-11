package dev.nerzouille.koan.ui.player

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.theme.Motion

/** Koan's video player (docs/VIDEO_PLAYER.md): always dark, whatever Koan's theme. */
object PlayerTokens {
    // Corner radii (half the height of their pill or disc).
    val PillRadius = 22.dp
    val ReplayRadius = 28.dp
    val BubbleRadius = 18.dp
    val BadgeRadius = 16.dp
    val ChipRadius = 16.dp
    val LevelRadius = 30.dp
    val SeekLabelRadius = 36.dp

    // §1 colour
    val Background = Color(0xFF000000)
    val PanelNoBlur = Color(0xE61C1C1E) // 90 %
    val PanelMenu = Color(0xF51C1C1E) // 96 %
    val Stroke = Color(0x14FFFFFF) // 8 %
    val Text = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xCCFFFFFF)
    val Track = Color(0x3DFFFFFF)
    val TrackBuffered = Color(0x66FFFFFF)
    val TrackPlayed = Color(0xFFFFFFFF)
    val Chip = Color(0x1AFFFFFF)
    val Destructive = Color(0xFFFF5A50)
    val ScrimTop = 120.dp
    val ScrimBottom = 200.dp
    const val SCRIM_TOP_ALPHA = 0.45f
    const val SCRIM_BOTTOM_ALPHA = 0.60f
    val MenuShadow = 24.dp

    // §2 layout
    /** Every pill's distance from the screen edges (owner's feedback: 20dp, the cutout only where it is). */
    val Inset = 20.dp
    /** Space kept beside a display cutout when a control has to move for it. */
    val CutoutGap = 8.dp
    val PillHeight = 44.dp
    val RoundPill = 44.dp
    val RoundGap = 8.dp
    val Glyph = 22.dp
    val TitleMaxLandscape = 0.45f
    val SideDisc = 56.dp
    val SideGlyph = 26.dp
    val PlayDisc = 72.dp
    val PlayGlyph = 32.dp
    /** The play triangle sits this far right of centre so it looks centred (its mass is on the left). */
    val PlayOptical = 1.75.dp
    val PlayOpticalSmall = 1.25.dp
    val CentreGapLandscape = 40.dp
    val CentreGapPortrait = 28.dp
    val ReplayHeight = 56.dp
    val BottomPillHeightLandscape = 56.dp
    val BottomPillHeightPortrait = 92.dp
    val BottomPillRadius = 28.dp
    val TimeMinWidth = 36.dp
    val TrackThickness = 8.dp
    val TrackThicknessPressed = 12.dp
    val BubbleSize = DpSize(68.dp, 36.dp)
    val BubbleGap = 12.dp
    val MenuWidth = 264.dp
    val MenuRadius = 18.dp
    val MenuRow = 44.dp
    val ChipHeight = 32.dp
    val SeekLabelHeight = 72.dp
    val SeekLabelText = 28.sp
    val BadgeHeight = 32.dp
    val LevelSize = DpSize(60.dp, 240.dp)
    val LevelTrack = DpSize(12.dp, 140.dp)
    val LevelEdgeLandscape = 32.dp
    val LevelEdgePortrait = 20.dp
    /** Inside the level pill: paddings, the number's line, the glyph, the gaps around the track. */
    val LevelPadTop = 16.dp
    val LevelPadBottom = 16.dp
    val LevelNumberLine = 20.dp
    val LevelGlyph = 26.dp
    val LevelInnerGap = 10.dp
    /** Space kept between the level indicator and the top / bottom pills. */
    val LevelGap = 12.dp
    /** A vertical move of this share of the screen height sweeps a level 0 → 100. */
    const val LEVEL_RANGE = 0.35f
    val LevelMin = 120.dp
    val Spinner = 32.dp
    val SpinnerStroke = 2.5.dp

    // §5 motion
    val ControlsIn: TweenSpec<Float> = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f))
    val ControlsOut: TweenSpec<Float> = tween(160, easing = FastOutLinearInEasing)
    val ControlsSpring: SpringSpec<Float> = Motion.Toast
    val MistBlurIn = 12.dp
    val MistBlurOut = 8.dp
    val MistShift = 8.dp
    const val AUTO_HIDE_MS = 3000L
    val PlayPop: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 600f)
    val Press: SpringSpec<Float> = Motion.Press
    val SwipeBack: SpringSpec<Float> = Motion.Overview
    val OpenSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 420f)
    val CloseSpring: SpringSpec<Float> = Motion.SheetPush
    val LevelScale: SpringSpec<Float> = Motion.Lift
    val LevelFill: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 900f)
    val GlyphTurn: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 600f)
    val GlowPulse: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 500f)
    const val SPINNER_DELAY_MS = 400L
    const val SPINNER_TURN_MS = 900
    const val SEEK_THROTTLE_MS = 80L
    const val GLOW_HOLD_MS = 700L
    const val LEVEL_HOLD_MS = 600L
    const val UNLOCK_MS = 3000L

    // §4 gestures
    const val SIDE_ZONE = 0.35f
    const val TAP_PAIR_MS = 250L
    const val DOUBLE_TAP_WINDOW_MS = 700L
    const val LONG_PRESS_MS = 400L
    val SwipeSlop = 12.dp
    /** The least of a system gesture edge (top and bottom) the player's drags leave alone. */
    val EdgeBand = 32.dp

    /** A ⋯ menu row that can't be used (Download video on a protected or streamed video). */
    const val DISABLED_ALPHA = 0.5f
    const val SWIPE_RATIO = 1.5f
    const val SWIPE_DOWN_SCALE_AT = 0.4f
    const val SWIPE_DOWN_COMMIT = 0.2f
    val FlingCommit = 800.dp // per second
    const val SKIP_S = 10.0

    val Speeds = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0)
}
