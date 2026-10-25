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
 * Source: docs/UI_SPEC.md (v3 perspective stack).
 */

/**
 * One theme's colours (docs/UI_SPEC.md "Visual tokens"). Two instances, [Light] (design 7, fully
 * neutral) and [Dark]. Screens read the current one through `KoanTheme.colors`. The space's own
 * colour only marks tiny space indicators ([SpaceColors]); "Koan is working" uses [Brand].
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
    /** Primary buttons: black in light, white in dark. */
    val Primary: Color,
    val OnPrimary: Color,
    /** Destructive actions: red text only, never a red fill. */
    val Destructive: Color,
    /** Grouped rows on a sheet or a screen (menu, settings). */
    val GroupedRow: Color,
    val ToggleOff: Color,
    /** The address input's ring while typing (neutral; "@" turns it into the brand gradient). */
    val FocusRing: Color,
    /** The dock: the bottom bar's own surface (also the drawer sheet). */
    val Dock: Color,
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
    /** The same without blur behind the drawer (Android 11, DeviceProfile.liveBlur): stronger. */
    val DrawerScrimNoBlur: Color,
    /** Without blur, the drawer sheet also casts a taller shadow over the cards, at full open. */
    val DrawerShadowNoBlur: Color,
    /** Over the page while a sheet or a floating card is open (the menu, prompts). */
    val SheetScrim: Color,
    // Depth stack.
    /** Back cards fade towards Base (light: a haze) instead of black (dark: brightness). */
    val DimToBase: Boolean,
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
) {
    companion object {
        val Light = KoanPalette(
            isDark = false,
            Base = Color(0xFFF5F5F7),
            Glass = Color(0xFFEBEBEF),
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
            Primary = Color(0xFF1C1C1E),
            OnPrimary = Color.White,
            Destructive = Color(0xFFE5342B),
            GroupedRow = Color(0xFFF2F2F5),
            ToggleOff = Color(0xFFE5E5EA),
            FocusRing = Color(0xFFB4B4BA),
            Dock = Color(0xFFFFFFFF),
            DockShadow = Color(0xFF3C281E).copy(alpha = 0.07f),
            DrawerHandle = Color.Black.copy(alpha = 0.16f),
            BarField = Color(0xFFF1F1F4),
            FieldHint = Color(0xFF8E8E93),
            Skeleton = Color.Black.copy(alpha = 0.06f),
            IconBacking = Color.Unspecified,
            Notice = Color(0xF2F2F2F5),
            Attention = Color(0xFFC0841A),
            DrawerScrim = Color(0xFF1C1C28).copy(alpha = 0.16f),
            DrawerScrimNoBlur = Color(0xFF1C1C28).copy(alpha = 0.32f),
            DrawerShadowNoBlur = Color(0xFF1C1C28).copy(alpha = 0.16f),
            SheetScrim = Color(0xFF1C1C28).copy(alpha = 0.30f),
            DimToBase = true,
            DimGain = 1.25f,
            DimMax = 0.85f,
            CardElevation = 8.dp,
            CardShadowColor = Color(0xFF1E1E3C).copy(alpha = 0.5f),
            ContactShadow = Color(0xFF1C1C30).copy(alpha = 0.10f),
            ContactShadowLength = 14.dp,
            AccentLightness = 0.45f..0.62f,
            InkLightness = 0.38f,
            PillAlpha = 0.14f,
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
            Primary = Color.White,
            OnPrimary = Color(0xFF1C1C1E),
            Destructive = Color(0xFFFF5A50),
            GroupedRow = Color.White.copy(alpha = 0.07f),
            ToggleOff = Color(0xFF3A3A3C),
            FocusRing = Color.White.copy(alpha = 0.30f),
            Dock = Color(0xFF252527),
            DockShadow = Color.Black.copy(alpha = 0.35f),
            DrawerHandle = Color.White.copy(alpha = 0.20f),
            BarField = Color.White.copy(alpha = 0.08f),
            FieldHint = Color.White.copy(alpha = 0.60f),
            Skeleton = Color.White.copy(alpha = 0.07f),
            IconBacking = Color(0xFFE9E9EE),
            Notice = Color(0xF22C2C2E),
            Attention = Color(0xFFE8B45A),
            DrawerScrim = Color.Black.copy(alpha = 0.45f),
            DrawerScrimNoBlur = Color.Black.copy(alpha = 0.62f),
            DrawerShadowNoBlur = Color.Black.copy(alpha = 0.45f),
            SheetScrim = Color.Black.copy(alpha = 0.50f),
            DimToBase = false,
            DimGain = 1f,
            DimMax = 1f,
            CardElevation = 20.dp,
            CardShadowColor = Color.Black,
            ContactShadow = Color.Black.copy(alpha = 0.22f),
            ContactShadowLength = 14.dp,
            AccentLightness = 0.62f..0.74f,
            InkLightness = 0.80f,
            PillAlpha = 0.18f,
        )
    }
}

/** A Zen space colour → its small accent (the switcher pill and the space's icon). */
object SpaceColorTokens {
    /** Below this saturation a Zen colour counts as grey: the space uses the default accent. */
    const val MIN_SATURATION = 0.12f

    /** Accents get at least this saturation, so a muted Zen theme still reads as a colour. */
    const val ACCENT_SATURATION = 0.55f
}

/**
 * The brand gradient (pink → violet → blue): ONLY for "Koan is working / active / your focus"
 * (docs/UI_SPEC.md "Brand gradient"). Never on login or password UI, permission prompts,
 * destructive actions, body text, or behind web content. Helpers: ui/theme/Brand.kt.
 */
object BrandTokens {
    val Pink = Color(0xFFFF5FA2)
    val Violet = Color(0xFFB46BFF)
    val Blue = Color(0xFF4F8DFF)
    val Colors = listOf(Pink, Violet, Blue)

    /** Syncing indicator: a gradient arc that turns. */
    val SpinnerStroke = 2.dp
    const val SPINNER_SWEEP = 270f
    const val SPINNER_TURN_MS = 900

    /** Gradient outline of cards that hold your focus (first run, account). */
    val Outline = 1.5.dp
}

/** Switches: ON = brand gradient track (softened) + white knob; it flows in behind the knob. */
object ToggleTokens {
    val Width = 50.dp
    val Height = 30.dp
    val KnobInset = 2.dp
    val KnobShadow = 3.dp
    const val KNOB_SHADOW_ALPHA = 0.22f

    /** The gradient track is laid over the OFF track at this alpha ("slightly softened"). */
    const val TRACK_ALPHA = 0.9f
    val Spring: SpringSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 520f)
}

/** Primary (black / white) and destructive (red text) buttons. */
object ButtonTokens {
    val Height = 50.dp
    val PaddingH = 28.dp
    val TextSize = 16.sp
    val IconGap = 10.dp
}

/** The address input: neutral ring while typing, the brand gradient flows around it on "@". */
object SearchFieldTokens {
    /** The text scrolls so the cursor stays this far inside the field's edges. */
    val CursorEdge = 40.dp

    val Ring = 1.5.dp
    val AtRing = 2.dp

    /** A very faint halo kept tight to the stroke (never a glow bleeding around the field). */
    val Halo = 1.5.dp
    const val HALO_ALPHA = 0.14f

    /**
     * "@" mode is alive: the gradient turns slowly around the ring, its speed drifting with two
     * out-of-phase slow oscillations (degrees, seconds). Speed never drops below ~10°/s.
     */
    const val TURN_DEG_PER_S = 48f
    const val WOBBLE_1_DEG = 14f
    const val WOBBLE_1_S = 3.1f
    const val WOBBLE_2_DEG = 10f
    const val WOBBLE_2_S = 4.7f
    val Glyph = 18.dp
    val GlyphGap = 10.dp
    val Flow: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 140f)
}

/** The upright one-point-perspective card stack (docs/UI_SPEC.md "Depth stack"). Tuned by hand on the phone. */
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

    /** A wheel/trackpad scroll settles on a card once no scroll event came for this long. */
    const val WHEEL_SETTLE_MS = 120L

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

    /**
     * The stack's viewport runs this far under the dock (its rounded top corners), so cards show
     * right up to the dock's edge and the dock covers them. The layout ends at the edge itself.
     */
    val UnderDock = 24.dp

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
    /**
     * A card deep enough to blur but with nothing blurred to show yet (Android 11 before its baked
     * copies upload, or a placeholder the memory tier doesn't bake): extra dim at the deepest blur.
     */
    const val NO_BLUR_DIM = 0.22f
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

    /**
     * Only this top slice of the thumbnail is blurred: more than any blurred pile card ever shows
     * (~330 px on the POCO). In px, not dp: the GPU rounds a layer up to 512 / 1024 px, and 512
     * keeps each blurred card's buffer at 2 MB instead of 4.
     */
    const val BLUR_SLICE_PX = 512
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
    val NewTabGlyph = 20.dp
    val SwipeDownToDismiss = 48.dp

    /** A space with no cards: message + "New tab" button. */
    val EmptyPadding = 32.dp
    val EmptyTitleSize = 18.sp
    val EmptyBodySize = 14.sp
    val EmptyGap = 6.dp
    val EmptyButtonGap = 24.dp
}

/** The bottom bar: a plain strip with the address row; + workspace switcher in the overview. */
/** First-run card, placeholders and the quiet sync status. */
/** The first-run tour and "What's new" (ui/tour): a tall sheet over Koan's dimmed UI. */
object TourTokens {
    /** The sheet's share of the screen height (corners and scrim as the other sheets). */
    const val SHEET_FRACTION = 0.92f
    val Padding = 24.dp
    val SkipHeight = 44.dp
    val SkipTop = 6.dp
    val TitleSize = 22.sp
    val LineSize = 15.sp
    val TitleGap = 6.dp
    val Gap = 16.dp

    /** The clip's phone frame: this share of the sheet's width, all the height left. */
    const val FRAME_WIDTH = 0.9f

    /**
     * Whole-screen clips (the screen below the status bar), width / height. Other clips are cropped
     * to fill the slot (1208 x 1763 px on the phone).
     */
    const val SCREEN_CLIP_ASPECT = 720f / 1478f
    val FrameRadius = 30.dp
    val FrameBezel = 5.dp
    val FrameBorder = 1.dp
    val FrameShadow = 20.dp

    /** The soft brand glow behind the title. */
    val GlowHeight = 240.dp
    const val GLOW_ALPHA_LIGHT = 0.16f
    const val GLOW_ALPHA_DARK = 0.12f

    /** Page dots: the current one stretches into a pill. */
    val Dot = 7.dp
    val DotActiveWidth = 20.dp
    val DotGap = 7.dp

    // ---- the welcome card (Welcome.kt) ----

    /** The 3D K, and the room around it for its sparkles. */
    val K = 210.dp
    val WelcomeTitleSize = 30.sp
    val WelcomeLineSize = 17.sp
    val WelcomeTextGap = 28.dp

    /** In dark mode the gradient is a touch deeper. */
    const val WELCOME_DIM_DARK = 0.14f

    /** The vortex: soft rings around the K, each turning a little faster than the one inside it. */
    const val VORTEX_RINGS = 8
    const val VORTEX_RINGS_SIMPLE = 5
    val VortexInner = 120.dp
    val VortexStep = 46.dp
    val VortexStroke = 26.dp
    const val VORTEX_ALPHA = 0.20f
    const val VORTEX_TURN_MS = 24_000
    const val VORTEX_GLOW_ALPHA = 0.35f

    /** Entrance: the K springs up from below, turning into its tilt; a light sweep; then the text. */
    const val K_START_TURN = -38f
    const val K_START_SCALE = 0.72f
    const val K_RISE_DELAY_MS = 180
    const val K_RISE_DAMPING = 0.6f
    const val K_RISE_STIFFNESS = 120f
    const val SWEEP_AT_MS = 750
    const val SWEEP_MS = 650
    const val TEXT_AT_MS = 850
    const val TEXT_STAGGER_MS = 120
    const val TEXT_MS = 380
    val TextRise = 14.dp
    const val INTRO_MS = TEXT_AT_MS + TEXT_STAGGER_MS + TEXT_MS

    /** While shown: the K floats; sparkles twinkle around it. */
    val FloatAmplitude = 7.dp
    const val FLOAT_MS = 3_400
    const val FLOAT_TURN = 2.5f
    const val FLOAT_TURN_MS = 5_300
    const val SPARKLES_SIMPLE = 3

    /** Swiping on: the K flies off to the left ahead of the page, the vortex trails behind. */
    const val K_LEAVE_EXTRA = 0.6f
    const val K_LEAVE_TURN = -24f
    const val VORTEX_LEAVE_TRAIL = 0.5f
}

object FirstRunTokens {
    const val SKELETON_CARDS = 6
    val SideMargin = 14.dp
    val Padding = 24.dp
    val Gap = 10.dp
    val TitleSize = 20.sp
    val BodySize = 14.sp
    val ButtonSize = 15.sp
    val CompactPadding = 12.dp

    /** The app icon at the top of the Connect card. */
    val AppIcon = 64.dp
    val AppIconRadius = 16.dp

    /** Placeholder cards breathe between these alphas, slowly. */
    const val SKELETON_ALPHA_MIN = 0.55f
    const val SKELETON_BREATH_MS = 1400

    /** Crossfade from placeholders to the first real data. */
    const val REVEAL_MS = 420

    val BadgeTouch = 36.dp
    val BadgeDot = 7.dp
    val BadgeSpinner = 12.dp
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

    /** Oldest system WebView that can show Mozilla's sign-in (crypto.randomUUID). */
    const val MIN_WEBVIEW = 92
    val OutdatedPadding = 32.dp
    val OutdatedSize = 16.sp
    val OutdatedGap = 20.dp
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
    val TabsBadge = 26.dp
    val TabsBadgeRadius = 8.dp
    val TabsBadgeBorder = 2.dp
    val TabsCountSize = 12.sp

    /** The dock: top corners in the overview (design 4a: 24) and its upward shadow. */
    val DockRadius = 24.dp
    val DockShadowHeight = 28.dp

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

    /** Page ↔ overview follows the finger: this much vertical travel is the whole morph. */
    val OverviewDrag = 220.dp

    /** Release faster than this (vertical) goes page ↔ overview regardless of position. */
    val OverviewFling = 600.dp // per second

    /** Page view space scrub: finger travel from one space to the next (fitted to the screen, see SpaceScrub). */
    val ScrubStepMin = 24.dp
    val ScrubStepMax = 90.dp

    /** …and the first / last space is reached this far before the screen's edge. */
    val ScrubEdge = 48.dp

    /** Page view: tabs slide this far apart while a swipe on the dock switches tab. */
    val TabSwipeGap = 24.dp

    /**
     * Page view tab swipes: the tab the strip rests on goes live only after this long without a
     * new swipe, so a chain of swipes shows screenshots and loads nothing on the way.
     */
    const val TAB_SWIPE_REST_MS = 300L

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

    val FolderIndent = 18.dp
    val FolderCountSize = 13.sp
    val StatusRowHeight = 40.dp
    val StatusTextSize = 13.sp

    /** Cards behind the open drawer: blur radius at full open (the scrim is KoanPalette.DrawerScrim). */
    val Blur = 14.dp

    /** Without blur (Android 11): the sheet's shadow over the cards at full open. */
    val NoBlurShadowHeight = 56.dp

    // How many blurred copies are baked: DeviceProfile's MemoryTier.drawerBlurLevels.
}

/**
 * Every sheet's detents and gestures (ui/sheet: the pins drawer, the menu, floating cards). One
 * engine, so they all move and settle the same way.
 */
object SheetTokens {
    /** Compact detent: the whole sheet (with the bar, for the drawer) takes at most this share of the screen. */
    const val COMPACT_MAX_FRACTION = 0.55f

    /** Full detent: the sheet stops this far below the status bar. */
    val FullTopGap = 12.dp

    /** A full detent less than this above the compact one is not worth a step: one open detent. */
    val MinDetentStep = 48.dp

    /** Drag past this share of the gap to the next detent to commit to it (as the pager). */
    const val COMMIT_FRACTION = 0.25f

    /** Release faster than this (vertical) opens/closes regardless of position. */
    val FlingVelocity = 600.dp // per second

    /** Sheets over a page (the menu): top corners, handle, the page's dim at compact. */
    val TopRadius = 32.dp
    val HandleTop = 8.dp
    val HandleZone = 22.dp
    val Padding = 14.dp
    val Gap = 10.dp

    /** A pushed page slides in from the right over this spring; the page under it trails. */
    const val PUSH_PARALLAX = 0.3f

    /** Floating card: side and bottom margins, corners. */
    val CardMargin = 10.dp
    val CardRadius = 28.dp
    val CardPadding = 20.dp

    // Rows, groups and tiles on a sheet (design 7k / 7l).
    val GroupRadius = 18.dp
    val RowHeight = 50.dp
    val RowTallHeight = 58.dp
    val RowPadding = 12.dp
    val RowIconBox = 30.dp
    val RowIconRadius = 9.dp
    val RowGlyph = 18.dp
    val RowTitleSize = 16.sp
    val RowSubtitleSize = 12.sp
    val RowTrailingSize = 14.sp
    val Divider = 1.dp
    val TileHeight = 66.dp
    val TileRadius = 16.dp
    val TileGap = 8.dp
    val TileGlyph = 22.dp
    val TileLabelSize = 13.sp
    val TileLabelGap = 4.dp
    val NavRowHeight = 56.dp
    val NavFieldHeight = 44.dp
    val NavFieldTextSize = 16.sp
    val NavLockGlyph = 14.dp
    val HeaderHeight = 52.dp
    val HeaderTitleSize = 17.sp
    val CardTitleSize = 18.sp

    /** Long press on an image: its preview on top of the link menu. */
    val LinkImageHeight = 140.dp

    /** uBlock's own panel, pushed in the menu sheet. */
    val UBlockPanelHeight = 520.dp
    const val IMAGE_FADE_MS = 180
    val CardTextSize = 14.sp
}

/** What a page asks on the floating card (ui/menu/PagePrompts): <select>, pickers, dialogs. */
object PromptTokens {
    val TextGap = 6.dp
    val FieldRadius = 12.dp

    /** A long message scrolls past this; a long <select> list past ListMaxHeight. */
    val MessageMaxHeight = 220.dp
    val ListMaxHeight = 380.dp

    /** Options inside an <optgroup> sit this far in. */
    val GroupIndent = 28.dp

    /** A dialog this soon after the last one offers "Don't let this page show more dialogs". */
    const val MANY_DIALOGS_MS = 3000L

    // Tick boxes (multiple <select>, "Don't let this page show more dialogs").
    val Check = 22.dp
    val CheckRadius = 6.dp
    val CheckGlyph = 16.dp
    val CheckRowPadding = 8.dp

    /** A month tile of <input type=month>: width / height. */
    const val MONTH_ASPECT = 2f

    /** <input type=color>: the swatches offered before a typed hex value. */
    val Swatches = listOf(
        Color(0xFF000000), Color(0xFF8E8E93), Color(0xFFFFFFFF), Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
        Color(0xFF34C759), Color(0xFF00C7BE), Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFFAF52DE), Color(0xFFFF2D55),
    )
    const val SWATCHES_PER_ROW = 6
    val SwatchRing = 3.dp
}

/** Settings and its pages (design 7n / 7o): full screen, rows straight on the base. */
object SettingsTokens {
    val Padding = 20.dp
    val TopBar = 56.dp
    val TitleSize = 34.sp
    val TitleBottom = 12.dp
    val SectionTop = 22.dp
    val SectionTitleSize = 13.sp
    val RowHeight = 54.dp
    val RowTallHeight = 62.dp

    /** A row's pressed highlight reaches this far past its text on both sides, rounded. */
    val RowInset = 12.dp
    val RowRadius = 14.dp
    val RowGlyph = 20.dp
    val RowGlyphGap = 14.dp
    val RowTitleSize = 16.sp
    val RowSubtitleSize = 12.sp
    val RowValueSize = 14.sp
    val Divider = 1.dp
    val CardRadius = 22.dp
    val CardPadding = 16.dp
    val Avatar = 44.dp
    val AvatarTextSize = 18.sp
    val SegmentHeight = 40.dp
    val SegmentRadius = 12.dp
    val SegmentInset = 3.dp
    val SegmentTextSize = 14.sp

    /** The soft brand wash behind the top of the screen. */
    val WashHeight = 320.dp
    const val WASH_ALPHA_LIGHT = 0.20f
    const val WASH_ALPHA_DARK = 0.14f

    // About.
    val AppIcon = 104.dp
    val AppIconRadius = 26.dp
    val AppNameSize = 30.sp
    val VersionSize = 14.sp
    val FooterSize = 12.sp
    val ChipHeight = 30.dp
    val ChipPadding = 12.dp
    val ChipGap = 6.dp
    val ChipGlyph = 13.dp
    val ChipTextSize = 13.sp
    val ChipShadow = 1.dp
}

/** The bottom toast ("Opened in background · Switch", later "Closed · Undo"). */
object ToastTokens {
    val Margin = 12.dp
    val Gap = 10.dp
    val Height = 50.dp
    val Radius = 16.dp
    val PaddingH = 18.dp
    val TextSize = 15.sp
    val TallHeight = 60.dp
    val DetailSize = 12.sp
    const val DETAIL_ALPHA = 0.65f
    const val SHOW_MS = 3000L

    /** Swiped past this fraction of its width (or flung), a toast goes away. */
    const val SWIPE_FRACTION = 0.3f
    val SwipeFling = 800.dp // per second

    /** Slides up this far as it appears. */
    val Rise = 16.dp
}

/** The Downloads page (design 7i) and the in-progress ring in the address pill (7g). */
object DownloadTokens {
    val FileTile = 40.dp
    val FileTileRadius = 10.dp
    val FileGlyph = 18.dp
    val FileExtSize = 7.sp
    val RowGap = 12.dp
    val RowPaddingV = 12.dp
    val ProgressHeight = 4.dp
    val ProgressTop = 8.dp
    val RoundButton = 36.dp
    val ActionHeight = 40.dp
    val ActionGap = 8.dp
    val GroupRadius = 18.dp
    val ChipHeight = 32.dp
    val ChipTextSize = 13.sp
    val SearchHeight = 48.dp
    val BottomGap = 10.dp

    /** The ring in the address pill while something downloads. */
    val Ring = 18.dp
    val RingStroke = 2.dp
}

/** The search sheet above the address field (design 7a–7d). */
/** The dock's long-press popups (tab counter, back): small, just above the dock. */
object TabMenuTokens {
    val Radius = 16.dp
    val Shadow = 12.dp
    val Gap = 8.dp
    val RowHeight = 44.dp
    val RowPadding = 16.dp
    val Icon = 18.dp
    val IconGap = 12.dp
    val TextSize = 15.sp
    const val SCALE_FROM = 0.85f
    const val IN_MS = 160
    const val OUT_MS = 120

    /** The tab counter's menu is at least this wide (it widens to the right). */
    val MinWidth = 200.dp

    /** Back's history list: at most this wide, scrolls past [MAX_ROWS] (half a row peeks). */
    val MaxWidth = 300.dp
    const val MAX_ROWS = 6
}

/** The start page Koan comes back on after a while unused. */
object StartPageTokens {
    val SidePadding = 16.dp
    /** Between the essentials and the dock. */
    val BottomGap = 20.dp
    val TitleGap = 14.dp
    val SwipeDownToDismiss = 64.dp

    /** The "Continue" card: the last tab, between the essentials and the field. */
    val ContinueTop = 20.dp
    val ContinueLabelGap = 8.dp
    val ContinueRadius = 16.dp
    val ContinuePadding = 12.dp
    val ContinueFavicon = 24.dp
    val ContinueTile = 40.dp
    val ContinueTileRadius = 10.dp
    val ContinueGap = 12.dp
    val ContinueTitleSize = 15.sp
    val ContinueSubtitleSize = 13.sp
    val LabelSize = 13.sp
    const val FADE_OUT_MS = 180
}

object SearchTokens {
    val Margin = 10.dp
    val GroupGap = 8.dp
    val GroupRadius = 18.dp
    val LabelSize = 12.sp
    val LabelPadding = 12.dp
    val RowHeight = 54.dp
    val RowPadding = 12.dp
    val Favicon = 26.dp
    val TitleSize = 15.sp
    val UrlSize = 12.sp
    val ChipHeight = 34.dp
    val ChipTextSize = 14.sp
    val ChipGap = 8.dp
    val ChipIcon = 14.dp
    val SpaceIcon = 14.dp
    val TileHeight = 58.dp
    val PickerRadius = 28.dp
    val PickerPadding = 16.dp

    /** The results never go higher than the status bar plus this. */
    val StatusGap = 16.dp

}

object PullTokens {
    val Size = 40.dp
    val Arc = 20.dp
    val Stroke = 2.5.dp
    val Elevation = 4.dp

    /** Pulled this far (after resistance), a release reloads. */
    val Trigger = 72.dp
    val Max = 120.dp

    /** The indicator moves this much of the finger's travel. */
    const val RESISTANCE = 0.5f

    /** A reload that never starts loading lets the indicator go after this. */
    const val START_TIMEOUT_MS = 3000L

    /** The brand spinner turns at least this long, even for an instant reload. */
    const val MIN_SPIN_MS = 600L
}

object HistoryTokens {
    val Favicon = 24.dp
    val SyncGlyph = 16.dp
    val Chevron = 18.dp

    /** The pages of an open group sit this far in, under the group's text. */
    val GroupIndent = 20.dp
    const val CHEVRON_MS = 200

    /** Rows loaded at most (newest first). */
    const val LIMIT = 3000

    /** Recently closed tabs shown before "Show all". */
    const val CLOSED_PREVIEW = 3
}

object PagerTokens {
    /** Drag past this fraction of the page width to commit to the next space. */
    const val COMMIT_FRACTION = 0.25f

    /** …or fling faster than this in the drag direction. */
    val FlingVelocity = 500.dp // per second
    val PageGap = 40.dp

    /** The incoming stack trails the header (0.8× speed) and grows from 0.94. */
    const val PARALLAX = 0.2f
    const val INCOMING_SCALE = 0.06f

    /** A space's real page fading in over its ghost when it is composed on screen (after a landing). */
    const val PAGE_APPEAR_MS = 160
    const val RUBBER_BAND = 0.3f
}

/** Page screenshots (thumbs/ThumbnailStore). */
object ThumbTokens {
    // Width on disk and memory caches: DeviceProfile's MemoryTier (480 looked soft on a ~1000 px
    // wide focused card, so the top tier keeps 720).
    const val QUALITY = 80
    const val DISK_CAP_BYTES = 150L * 1024 * 1024

    /** Capture this long after a page finished loading (it settles: fonts, images, layout). */
    const val SETTLE_MS = 1500L

    /** Capture this long after a page's first paint (an early screenshot for the overview). */
    const val PAINT_SETTLE_MS = 800L

    /** Capture once the page has not scrolled for this long. */
    const val SCROLL_SETTLE_MS = 1000L

    /** Background captures of a tab are at least this far apart. */
    const val CAPTURE_MIN_INTERVAL_MS = 4000L

    /** Switching tabs waits at most this long for the outgoing page's screenshot. */
    const val LEAVE_CAPTURE_TIMEOUT_MS = 150L

    /** A card's real screenshot fades in over its placeholder. */
    const val FADE_MS = 140
}

/** Page view: the cover over the engine while a tab comes up, and the dock hiding on scroll. */
object PageTokens {
    /** The cover fades out once the live page has painted. */
    const val COVER_FADE_MS = 140

    /** Never cover a page longer than this, even if it never reports a paint. */
    const val COVER_MAX_MS = 2500L

    /** Opening a card: its screenshot grows from this scale to full screen. */
    const val OPEN_ZOOM_FROM = 0.92f

    /** The dock always shows when the page is scrolled less than this from the top. */
    val DockNearTop = 48.dp
}

object Motion {
    /**
     * Stack snap: exponential glide, no overshoot (prototype: 17% of the remaining distance per
     * 60 Hz frame = time constant ≈ 90 ms). Frame-rate independent here.
     */
    const val STACK_SNAP_TAU_SECONDS = 0.09f
    val CardSlot: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 420f)
    const val FLY_OFF_MS = 180
    val SwipeReturn: SpringSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 500f)
    val Lift: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 600f)
    val Drop: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 500f)
    val PagerSettle: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 300f)
    val Drawer: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 420f)

    /** Page view: the swiped page sliding out, the neighbour tab sliding in (or back). */
    val TabSwipe: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 400f)

    /** Page view space scrub: the highlight springing to the space under the finger. */
    val ScrubHighlight: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 900f)

    /** Page view: the address pill crossfading to the space's name in space mode. */
    const val SPACE_MODE_FADE_MS = 140
    val FolderChevron: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 600f)

    /** A page pushed into a sheet sliding in, or popped. */
    val SheetPush: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 520f)

    /** The bottom toast rising in and sinking away. */
    val Toast: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 700f)

    /** Full-screen sheets (sign-in) sliding up and down. */
    val Sheet: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 380f)

    /** The page dimming behind the search sheet. */
    val SearchScrim: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 700f)

    /** Overview open/close; also drives the bar morph. */
    val Overview: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 600f)
    const val OVERVIEW_ZOOM = 0.04f

    /** The dock settling shown/hidden after a page scroll lets go. */
    val DockSettle: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 800f)

    /** Keep the high refresh rate this long after the last touch/key: longer than the longest fling (2.2 s). */
    const val REFRESH_BOOST_IDLE_MS = 3000L
}
