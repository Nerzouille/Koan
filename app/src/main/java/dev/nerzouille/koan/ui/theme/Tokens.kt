package dev.nerzouille.koan.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Every visual value of Koan in one place: colours, sizes, the depth-stack geometry, motion.
 * Tweak here, not in the screens. A second theme = a second set of these objects.
 * Sources: docs/UI_SPEC.md and docs/reference/design/Koan Screens.dc.html (v3 perspective stack).
 */

object KoanColors {
    val Base = Color(0xFF0E0E10)
    val Glass = Color.White.copy(alpha = 0.08f)
    val GlassFallback = Color(0xEB1A1A1E) // #1A1A1E @ 92%
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.62f)
    val TextDisabled = Color.White.copy(alpha = 0.19f)
    val CardPlaceholder = Color(0xFF18181B)
    val CardHeaderScrim = Color.Black.copy(alpha = 0.35f)
    val CardHairline = Color.White.copy(alpha = 0.10f)
    val CloseButton = Color.White.copy(alpha = 0.14f)
    val DefaultAccent = Color(0xFF8AB4FF)
    val NewTabButton = Color.White
    val NewTabIcon = Color.Black

    /** The bottom bar is a plain page-dark strip; the drawer sheet shares it. */
    val BarSurface = Color(0xFF0E0E10)
    val BarHairline = Color.White.copy(alpha = 0.06f)
    val DrawerHandle = Color.White.copy(alpha = 0.28f)
    val BarField = Color.White.copy(alpha = 0.10f)
    val PageDot = Color.White
}

/** The upright one-point-perspective card stack (docs/UI_SPEC.md "Depth stack"). */
object StackTokens {
    /** Geometric ratio per slot of depth: spacing and scale shrink by this factor each card back. */
    const val R = 0.55f

    /** Scale the pile converges to far back (the vanishing point). */
    const val S_MIN = 0.62f

    /** A: how far above the focused card's top the pile converges. Clamped to the room available. */
    val PileDepth = 176.dp

    /** Focused card height as a fraction of the stack viewport (design: 0.64; −9% per owner). */
    const val CARD_HEIGHT_FRACTION = 0.58f
    val CardSideMargin = 14.dp

    /** How much of the card below the focus shows at the bottom of the viewport (its header + a bit). */
    val PeekVisible = 80.dp

    /** Gap between the focused card's bottom edge and the peeking card's top edge. */
    val PeekGap = 14.dp
    const val PEEK_SCALE = 0.94f
    const val PEEK_DIM = 0.12f

    /** Cards further in front than the peek fade out over this fraction of a slot. */
    const val FRONT_FADE_SLOTS = 0.33f

    /** Keep the top of the pile this far below the header. */
    val PileTopGap = 10.dp

    /** Keyframes at d = 0, 1, 2 …, joined linearly like the prototype. Thumbnail dim = 1 − brightness. */
    val Dim = floatArrayOf(0f, 0.16f, 0.28f, 0.38f, 0.48f, 0.58f, 0.66f)

    /**
     * Thumbnail blur radius in dp. Design's CSS blur σ [0, 0, .5, 1, 1.6, 2.2, 2.8] converted to
     * Android's radius (σ = 0.577·r + 0.5).
     */
    val BlurDp = floatArrayOf(0f, 0f, 0f, 0.9f, 1.9f, 2.9f, 4f)
    val Alpha = floatArrayOf(1f, 1f, 1f, 1f, 1f, 0.85f, 0.6f, 0.3f, 0f)

    /** Every visible card keeps the same soft shadow (prototype), it's what layers the pile. */
    val Shadow = 20.dp

    /** Header strip is dimmed at most this much, so titles stay legible deep in the pile. */
    const val HEADER_DIM_MAX = 0.2f

    /** Finger travel that moves the stack by one slot. */
    val SlotDrag = 230.dp

    /** A release that moved the stack more than this (in slots) commits to the next card. */
    const val SNAP_COMMIT = 0.15f

    /** Overscroll resistance past both ends. */
    const val RUBBER_BAND = 0.45f

    /** Fade to the dock colour at the bottom of the viewport. */
    val BottomFade = 48.dp

    // Long-press reorder.
    const val LIFT_SCALE = 0.03f
    val LiftShadow = 16.dp

    // Swipe-to-close (always moves the focused card), from the prototype.
    /** Tilt while dragging, degrees per dp of travel; pivot at the card's bottom centre. */
    const val SWIPE_TILT_PER_DP = 0.03f

    /** Alpha falls by 1 per this much travel, never below SWIPE_MIN_ALPHA. */
    val SwipeFadeDistance = 420.dp
    const val SWIPE_MIN_ALPHA = 0.3f

    /** Released past this: the card flies off. */
    val SwipeCloseDistance = 100.dp
    val FlyOffDistance = 480.dp
    const val FLY_OFF_TILT = 8f
}

object CardTokens {
    val Radius = 28.dp
    val HeaderHeight = 44.dp
    val HeaderPaddingStart = 14.dp
    val HeaderPaddingEnd = 6.dp
    val HeaderGap = 10.dp
    val Favicon = 22.dp
    val TitleSize = 15.sp
    val CloseTouch = 44.dp
    val CloseButton = 30.dp
    val CloseIcon = 14.dp
    val ActiveDot = 6.dp
    val PinGlyph = 14.dp
    val Hairline = 1.dp
    const val PLACEHOLDER_TINT = 0.08f
    val PlaceholderFavicon = 64.dp
    val PlaceholderTop = 44.dp // below the header
    val PlaceholderGap = 14.dp
    val PlaceholderDomainSize = 14.sp

    /** Only this top slice of the thumbnail is blurred: more than any pile card ever shows. */
    val BlurSlice = 300.dp
}

object OverviewTokens {
    val HeaderHeight = 52.dp
    val HeaderPadding = 6.dp
    val HeaderIcon = 44.dp
    val HeaderGlyph = 22.dp
    val TitleGap = 10.dp
    val SpaceIconBox = 32.dp
    val SpaceIconRadius = 10.dp
    val SpaceIconSize = 16.sp
    val TitleSize = 20.sp
    val NewTabButton = 40.dp
    val NewTabGlyph = 20.dp
    val SwipeDownToDismiss = 48.dp

    /** Space gradient backdrop: how much of each stop survives over the base colour. */
    const val BACKDROP_TOP = 0.75f
    const val BACKDROP_MID = 0.45f
    const val BACKDROP_DESATURATE = 0.3f
}

/** The bottom bar: a plain strip with the address row; + workspace switcher in the overview. */
object BarTokens {
    val BottomMargin = 6.dp
    val AddressRowHeight = 68.dp
    val SwitcherRowHeight = 60.dp

    /** Drawer handle at the top of the switcher row. */
    val HandleWidth = 36.dp
    val HandleHeight = 4.dp
    val HandleTop = 8.dp
    val HandleTouch = 24.dp
    val FieldHeight = 50.dp
    val FieldTextSize = 17.sp
    val FieldPadding = 18.dp
    val RowPadding = 6.dp
    val RowGap = 2.dp
    val IconTouch = 48.dp
    val Icon = 24.dp
    val MenuGlyph = 20.dp
    val TabsBadge = 26.dp
    val TabsBadgeRadius = 8.dp
    val TabsBadgeBorder = 2.dp
    val TabsCountSize = 12.sp
    /** Page load progress along the bar's top edge. */
    val ProgressHeight = 2.dp
    val Hairline = 1.dp

    /** Rows slide this far while they crossfade during the page ↔ overview morph. */
    val MorphSlide = 12.dp

    /** Space between the bar and the stack above it in the overview. */
    val GapAbove = 12.dp
    val SwipeUpToOpen = 40.dp

    // Switcher row.
    val LabelSpacing = 116.dp
    val LabelSize = 15.sp
    val LabelGap = 6.dp
    val LabelLift = 8.dp
    const val LABEL_FAR_SCALE = 0.14f
    const val LABEL_FAR_FADE = 0.6f
    val Chevron = 16.dp
    val ChevronInset = 14.dp
    val Dot = 5.dp
    val DotGap = 5.dp
    val DotsBottom = 8.dp
}

/** Essentials + pinned drawer that grows out of the overview's bar. */
object DrawerTokens {
    val TopRadius = 28.dp
    val Padding = 14.dp
    val Gap = 10.dp
    val EssentialGap = 8.dp
    val EssentialHeight = 52.dp
    val EssentialRadius = 14.dp
    val EssentialFavicon = 26.dp
    val PinnedRowHeight = 48.dp
    val PinnedRadius = 18.dp
    val PinnedPadding = 12.dp
    val PinnedFavicon = 22.dp
    val PinnedTitleSize = 15.sp
    val PinnedGlyph = 16.dp
    val MoreTextSize = 13.sp
    val MorePadding = 16.dp
    const val MAX_PINNED_ROWS = 5

    /** Cards behind the open drawer: black overlay alpha and blur radius at full open. */
    const val SCRIM = 0.45f
    val Blur = 14.dp

    /** Release faster than this (vertical) opens/closes regardless of position. */
    val FlingVelocity = 600.dp // per second
}

object PagerTokens {
    /** Drag past this fraction of the page width to commit to the next space. */
    const val COMMIT_FRACTION = 0.25f

    /** …or fling faster than this in the drag direction. */
    val FlingVelocity = 500.dp // per second
    val PageGap = 24.dp

    /** The incoming stack trails the header (0.85× speed) and grows from 0.96. */
    const val PARALLAX = 0.15f
    const val INCOMING_SCALE = 0.04f
    const val RUBBER_BAND = 0.3f
}

object Motion {
    /**
     * Stack snap: exponential glide, no overshoot (prototype: 17% of the remaining distance per
     * 60 Hz frame = time constant ≈ 90 ms). Frame-rate independent here.
     */
    const val STACK_SNAP_TAU_SECONDS = 0.09f
    val CardSlot: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 420f)
    const val FLY_OFF_MS = 280
    val SwipeReturn: SpringSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 500f)
    val Lift: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 600f)
    val Drop: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 500f)
    val PagerSettle: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 300f)
    val Drawer: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 420f)

    /** Overview open/close; also drives the bar morph. */
    val Overview: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 600f)
    const val OVERVIEW_ZOOM = 0.04f
}
