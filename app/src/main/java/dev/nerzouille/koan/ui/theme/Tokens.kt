package dev.nerzouille.koan.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Every visual value of Koan in one place: colours, sizes, the depth-stack geometry, motion.
 * Tweak here, not in the screens. Colours come in one set per theme ([KoanPalette]).
 * Sources: docs/UI_SPEC.md and docs/reference/design/Koan Screens.dc.html (v3 perspective stack).
 */

/**
 * One theme's colours (docs/UI_SPEC.md "Visual tokens"). Two instances, [Light] (design 4a, "soft
 * wash") and [Dark] (4i, grey). Screens read the current one through `KoanTheme.colors`; the
 * space's own colour is only a small accent on top (see [SpaceColors]).
 */
@Immutable
class KoanPalette(
    val isDark: Boolean,
    /** Page background behind everything, and what the stack fades to above the dock. */
    val Base: Color,
    /** Small surfaces on Base: the header's space icon box, essentials tiles, pinned list. */
    val Glass: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextDisabled: Color,
    /** Bar icons and the tab-count badge. */
    val Icon: Color,
    val Card: Color,
    val CardStrip: Color,
    /** 1dp outline all around a card. */
    val CardBorder: Color,
    val CloseButton: Color,
    val CloseIcon: Color,
    /** Fallback accent for spaces without a usable Zen colour. */
    val DefaultAccent: Color,
    val NewTabButton: Color,
    val NewTabIcon: Color,
    /** The dock: the bottom bar's own surface (also the drawer sheet). */
    val Dock: Color,
    val DockHairline: Color,
    /** Soft shadow the dock casts upward over the content. */
    val DockShadow: Color,
    val DrawerHandle: Color,
    val BarField: Color,
    val FieldHint: Color,
    val Skeleton: Color,
    /** Behind favicons too dark to read on dark surfaces; Unspecified = never needed. */
    val IconBacking: Color,
    val Notice: Color,
    /** Quiet sync notices: amber, never red. */
    val Attention: Color,
    /** Over the overview while the drawer is open, at full open. */
    val DrawerScrim: Color,
    // Depth stack.
    /** Back cards fade towards this (dark: black = brightness). Light uses the space's haze instead. */
    val DimFromHaze: Boolean,
    /** Multiplies StackTokens.Dim, capped at DimMax (light haze is stronger than a dim). */
    val DimGain: Float,
    val DimMax: Float,
    val CardElevation: Dp,
    val CardShadowColor: Color,
    /** Short, soft shadow a card casts on the card behind it, just above its top edge. */
    val ContactShadow: Color,
    val ContactShadowLength: Dp,
    // Space colour → accent (hue kept, saturation and lightness pulled into a range that reads on Base).
    val AccentLightness: ClosedFloatingPointRange<Float>,
    val InkLightness: Float,
    /** Alpha of the active space pill's tint. */
    val PillAlpha: Float,
    /** How much of the accent tints the top of the background, and at 24% of the height (light only). */
    val WashTop: Float,
    val WashMid: Float,
) {
    companion object {
        val Light = KoanPalette(
            isDark = false,
            Base = Color(0xFFFFFFFF),
            Glass = Color(0xFFF2F2F5),
            TextPrimary = Color(0xFF1C1C1E),
            TextSecondary = Color(0xFF6E6E73),
            TextDisabled = Color.Black.copy(alpha = 0.22f),
            Icon = Color(0xFF6E6E73),
            Card = Color(0xFFFFFFFF),
            CardStrip = Color.White.copy(alpha = 0.94f),
            CardBorder = Color(0xFFE4E4EA),
            CloseButton = Color.Black.copy(alpha = 0.06f),
            CloseIcon = Color(0xFF6E6E73),
            DefaultAccent = Color(0xFF5B6CFF),
            NewTabButton = Color(0xFF1C1C1E),
            NewTabIcon = Color.White,
            Dock = Color(0xFFFFFFFF),
            DockHairline = Color(0xFFECECF0),
            DockShadow = Color(0xFF3C281E).copy(alpha = 0.07f),
            DrawerHandle = Color.Black.copy(alpha = 0.16f),
            BarField = Color(0xFFF1F1F4),
            FieldHint = Color(0xFF8E8E93),
            Skeleton = Color.Black.copy(alpha = 0.06f),
            IconBacking = Color.Unspecified,
            Notice = Color(0xF2F2F2F5),
            Attention = Color(0xFFC0841A),
            DrawerScrim = Color(0xFF1C1C28).copy(alpha = 0.16f),
            DimFromHaze = true,
            DimGain = 1.25f,
            DimMax = 0.85f,
            CardElevation = 8.dp,
            CardShadowColor = Color(0xFF1E1E3C).copy(alpha = 0.5f),
            ContactShadow = Color(0xFF1C1C30).copy(alpha = 0.10f),
            ContactShadowLength = 14.dp,
            AccentLightness = 0.45f..0.62f,
            InkLightness = 0.38f,
            PillAlpha = 0.14f,
            WashTop = 0.16f,
            WashMid = 0.09f,
        )

        val Dark = KoanPalette(
            isDark = true,
            Base = Color(0xFF1C1C1E),
            Glass = Color.White.copy(alpha = 0.08f),
            TextPrimary = Color.White,
            TextSecondary = Color.White.copy(alpha = 0.62f),
            TextDisabled = Color.White.copy(alpha = 0.19f),
            Icon = Color(0xFF98989D),
            Card = Color(0xFF2C2C2E),
            CardStrip = Color(0xF22C2C2E),
            CardBorder = Color.White.copy(alpha = 0.08f),
            CloseButton = Color.White.copy(alpha = 0.10f),
            CloseIcon = Color.White.copy(alpha = 0.75f),
            DefaultAccent = Color(0xFF8EA2FF),
            NewTabButton = Color.White,
            NewTabIcon = Color(0xFF1C1C1E),
            Dock = Color(0xFF252527),
            DockHairline = Color.White.copy(alpha = 0.08f),
            DockShadow = Color.Black.copy(alpha = 0.35f),
            DrawerHandle = Color.White.copy(alpha = 0.20f),
            BarField = Color.White.copy(alpha = 0.08f),
            FieldHint = Color.White.copy(alpha = 0.60f),
            Skeleton = Color.White.copy(alpha = 0.07f),
            IconBacking = Color(0xFFE9E9EE),
            Notice = Color(0xF22C2C2E),
            Attention = Color(0xFFE8B45A),
            DrawerScrim = Color.Black.copy(alpha = 0.45f),
            DimFromHaze = false,
            DimGain = 1f,
            DimMax = 1f,
            CardElevation = 20.dp,
            CardShadowColor = Color.Black,
            ContactShadow = Color.Black.copy(alpha = 0.22f),
            ContactShadowLength = 14.dp,
            AccentLightness = 0.62f..0.74f,
            InkLightness = 0.80f,
            PillAlpha = 0.18f,
            WashTop = 0f,
            WashMid = 0f,
        )
    }
}

/** Where the background wash ends, as fractions of the screen height (design 4a). */
object WashTokens {
    const val MID_STOP = 0.24f
    const val END_STOP = 0.5f

    /** Below this saturation a Zen colour counts as grey: the space uses the default accent. */
    const val MIN_SATURATION = 0.12f

    /** Accents get at least this saturation, so a muted Zen theme still reads as a colour. */
    const val ACCENT_SATURATION = 0.55f

    /** Loading bar when the space has fewer than two colours: pink → violet → blue. */
    val DefaultProgress = listOf(Color(0xFFFF5FA2), Color(0xFFB46BFF), Color(0xFF4F8DFF))
}

/** The upright one-point-perspective card stack (docs/UI_SPEC.md "Depth stack"). Tuned to sketch-stack.png. */
object StackTokens {
    /** Geometric ratio per slot of depth for the pile's spacing: lower = bunches tighter, faster. */
    const val R = 0.45f

    /** Same, for the pile's scale (the sketch shrinks widths a bit slower than it closes gaps). */
    const val SCALE_R = 0.52f

    /** Scale the pile converges to far back (the vanishing point). */
    const val S_MIN = 0.58f

    /**
     * A: how far above the focused card's top the pile converges, as a fraction of the focused
     * card's height (first gap = A·(1 − r)). Clamped to the room available.
     */
    const val PILE_DEPTH_FRACTION = 0.28f

    /*
     * Vertical layout, top → bottom: PileTopGap, the pile (A), the focused card, the card in front
     * peeking PeekVisible above the viewport bottom. The focused card gets all the height left.
     */
    val CardSideMargin = 14.dp

    /** How much of the card in front shows above the viewport bottom: its header + a hint of thumbnail. */
    val PeekVisible = 60.dp

    /** The card in front overlaps the focused card's bottom by this much (no gap between them). */
    val PeekOverlap = 40.dp

    /** The card in front is closer to the viewer: slightly bigger than the focused one. */
    const val PEEK_SCALE = 1.05f
    const val PEEK_DIM = 0.12f

    /** Cards further in front than the peek fade out over this fraction of a slot. */
    const val FRONT_FADE_SLOTS = 0.33f

    /** Breathing room between the header and the top of the pile. */
    val PileTopGap = 22.dp

    /** Keyframes at d = 0, 1, 2 …, joined linearly like the prototype. Thumbnail dim = 1 − brightness. */
    val Dim = floatArrayOf(0f, 0.16f, 0.28f, 0.38f, 0.48f, 0.58f, 0.66f)

    /**
     * Thumbnail blur radius in dp. Design's CSS blur σ [0, 0, .5, 1, 1.6, 2.2, 2.8] converted to
     * Android's radius (σ = 0.577·r + 0.5).
     */
    val BlurDp = floatArrayOf(0f, 0f, 0f, 0.9f, 1.9f, 2.9f, 4f)
    val Alpha = floatArrayOf(1f, 1f, 1f, 1f, 1f, 0.85f, 0.6f, 0.3f, 0f)

    // Every visible card keeps the same shadow (it layers the pile): KoanPalette.CardElevation.

    /** Header strip is dimmed at most this much, so titles stay legible deep in the pile. */
    const val HEADER_DIM_MAX = 0.2f

    /** Finger travel that moves the stack by one slot (prototype: 230dp; shorter covers more cards). */
    val SlotDrag = 170.dp

    /** A slow release that moved the stack more than this (in slots) commits to the next card. */
    const val SNAP_COMMIT = 0.15f

    /** Releases faster than this (slots/s) are flings: momentum scroll through several cards. */
    const val FLING_MIN_VELOCITY = 2f

    /**
     * Fling friction: a fling travels (velocity − FLING_DEADZONE) × FLING_GAIN cards (at least one),
     * velocity capped at FLING_MAX_VELOCITY, all in slots/s. Gentle swipe ≈ 1 card, normal flick
     * ≈ 2–4, strong fling 10+.
     */
    const val FLING_DEADZONE = 2.2f
    const val FLING_GAIN = 0.55f
    const val FLING_MAX_VELOCITY = 36f

    /** Fling deceleration: cubic ease-out (starts at the finger's speed, stops dead on a card). */
    const val FLING_EASE_POWER = 3f
    const val FLING_MIN_SECONDS = 0.3f
    const val FLING_MAX_SECONDS = 2.2f

    /** Overscroll resistance past both ends. */
    const val RUBBER_BAND = 0.45f

    /** Fade to the background at the bottom of the viewport; short, so the peek's header stays crisp. */
    val BottomFade = 16.dp

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
    val SkeletonTitle = 140.dp
    val SkeletonBar = 10.dp
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

    /** The contact shadow never covers more than this share of a card's visible sliver (deep pile). */
    const val CONTACT_SHADOW_MAX_FRACTION = 0.35f

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
    val SpaceIconGlyph = 20.dp
    val TitleSize = 20.sp
    val NewTabButton = 40.dp
    val NewTabGlyph = 20.dp
    val SwipeDownToDismiss = 48.dp
}

/** The bottom bar: a plain strip with the address row; + workspace switcher in the overview. */
/** First-run card, placeholders and the quiet sync status. */
object FirstRunTokens {
    const val SKELETON_CARDS = 6
    val SideMargin = 14.dp
    val Padding = 24.dp
    val Gap = 10.dp
    val TitleSize = 20.sp
    val BodySize = 14.sp
    val ButtonSize = 15.sp
    val ButtonPaddingH = 28.dp
    val ButtonPaddingV = 12.dp
    val CompactPadding = 12.dp

    /** Placeholder cards breathe between these alphas, slowly. */
    const val SKELETON_ALPHA_MIN = 0.55f
    const val SKELETON_BREATH_MS = 1400

    /** Crossfade from placeholders to the first real data. */
    const val REVEAL_MS = 420

    val BadgeTouch = 36.dp
    val BadgeDot = 7.dp
}

/** The Mozilla sign-in screen. */
object SignInTokens {
    val HeaderHeight = 64.dp
    val HeaderPadding = 8.dp
    val CloseTouch = 44.dp
    val CloseGlyph = 20.dp
    val TitleSize = 17.sp
    val SubtitleSize = 13.sp
    val ProgressHeight = 2.dp
    val NoticeMargin = 12.dp
    val NoticePadding = 14.dp
    val NoticeRadius = 16.dp
    val NoticeSize = 14.sp

    /** The page fades in once it has painted, so its first (white) frame is never shown. */
    const val FADE_MS = 220
}

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
    val Hairline = 1.dp

    /** The dock: top corners in the overview (design 4a: 24) and its upward shadow. */
    val DockRadius = 24.dp
    val DockShadowHeight = 28.dp

    /** Ring around the address input while it has focus. */
    val FocusRing = 1.5.dp

    /** Page load progress: a gradient line on the dock's top edge, with a soft glow. */
    val ProgressHeight = 2.dp
    val ProgressGlow = 8.dp
    const val PROGRESS_GLOW_ALPHA = 0.35f

    /** Where the line starts as soon as a load begins, so it never looks stuck at 0. */
    const val PROGRESS_START = 0.06f
    const val PROGRESS_MS = 800
    const val PROGRESS_FINISH_MS = 250
    const val PROGRESS_FADE_MS = 350

    /** A lighter streak running along the line while it waits. */
    val ShimmerWidth = 72.dp
    const val SHIMMER_MS = 1300
    const val SHIMMER_ALPHA = 0.55f

    /** Rows slide this far while they crossfade during the page ↔ overview morph. */
    val MorphSlide = 12.dp

    /** Space between the bar and the stack above it in the overview. */
    val GapAbove = 12.dp
    val SwipeUpToOpen = 40.dp

    // Switcher row: Zen-style space icons, the active one in a tinted pill with its name (design 4a).
    /** Touch target of an inactive icon; icons sit side by side, no gap. */
    val SwitcherItem = 44.dp
    val SwitcherBottom = 2.dp
    val SwitcherIcon = 20.dp

    /** Icon inset from the item's left edge, the same collapsed and in the pill, so it never jumps. */
    val SwitcherIconInset = 12.dp
    val SwitcherNameGap = 7.dp
    val SwitcherPillEnd = 14.dp
    val SwitcherPillHeight = 36.dp
    val SwitcherNameSize = 14.sp
    val SwitcherNameMax = 140.dp

    /** Inactive icons (design: .55; a bit more visible on the phone). */
    const val SWITCHER_INACTIVE_ALPHA = 0.7f

    /** The name only shows once the pill is this far open, so it never runs into the next icon. */
    const val SWITCHER_NAME_FROM = 0.45f
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

    /** Pinned rows visible before the list scrolls; the half row hints that there is more. */
    const val VISIBLE_PINNED_ROWS = 5.5f
    val FolderIndent = 18.dp
    val FolderCountSize = 13.sp
    val StatusRowHeight = 40.dp
    val StatusTextSize = 13.sp

    /** Cards behind the open drawer: blur radius at full open (the scrim is KoanPalette.DrawerScrim). */
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
    val FolderChevron: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 600f)

    /** Full-screen sheets (sign-in) sliding up and down. */
    val Sheet: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 380f)

    /** The space accent (loading bar, cursor) crossfades this long when the current space changes. */
    const val ACCENT_FADE_MS = 400

    /** Overview open/close; also drives the bar morph. */
    val Overview: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 600f)
    const val OVERVIEW_ZOOM = 0.04f

    /** Keep the high refresh rate this long after the last touch/key: longer than the longest fling (2.2 s). */
    const val REFRESH_BOOST_IDLE_MS = 3000L
}
