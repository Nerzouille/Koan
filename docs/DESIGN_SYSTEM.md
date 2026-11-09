# Koan design system

The design language of **Koan**, an Android browser built around Zen Browser's Spaces, as it is in
the code today (v0.4.0). Every value is read from the code; references are `path:line` from the
repository root (`T:` = `app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt`). Where the code
and `docs/UI_SPEC.md` disagree, the code is what ships and the difference is noted.

The files this document points to live in `design-export/` (gitignored: screenshots come from the owner's phone and stay local). Images below only show in a local checkout that has that folder.

| Folder | Contents |
|---|---|
| `tokens/tokens.json` | All foundations: colours (light, dark, usage), brand, typography, spacing, radii, sizes, motion, and every component token with its line |
| `brand/` | App icon, 3D K, monochrome K (SVG + PNG), 24-grid K |
| `icons/` | Koan's 40 icons as SVG (`currentColor`), `_overview.png` |
| `screens/light/`, `screens/dark/` | Screenshots of every main screen and component (taken in the real app on demo pages, email shown as `you@example.com`; some show real space names and pinned tabs) |

Contents: 1. Foundations · 2. Brand · 3. Components (A dock and bar, B overview and stack, C sheets,
prompts and kit, D pages, sign-in, tour and icons) · 4. Screens

## 1. Foundations

Every value below comes from `app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt` (machine
readable: `tokens/tokens.json`). Units: **dp** (density-independent px) and **sp** (text px; 1sp =
1dp at the default font scale). The reference phone is 1208×2624 px at 480 dpi, so 1dp = 3px and
the screen is about 403×875dp. Colours are `#RRGGBBAA` where they have an alpha.

### 1.1 Colour

Two themes, **Light** (fully neutral) and **Dark**, chosen in Settings › Display › Theme
(Light / Dark / Follow system, default Follow system). Web pages follow the same theme
(`prefers-color-scheme`). Screens read the palette through `KoanTheme.colors`.

Rules (docs/UI_SPEC.md "Themes", "Visual restraint"):
- Light: base `#F5F5F7`, white surfaces (cards, dock, sheets), grey grouped rows, near-black text.
  No space-colour wash anywhere.
- Dark: base `#1C1C1E`, surfaces `#2C2C2E`, dock `#252527`.
- Cards stay neutral: the page thumbnails are the colour.
- Destructive actions are red **text** only, never a red fill.
- Quiet sync notices use amber (`Attention`), never red.

| Token | Light | Dark | Where it is used |
|---|---|---|---|
| `Base` | `#F5F5F7FF` | `#1C1C1EFF` | Page background behind everything, and what the stack fades to above the dock. |
| `Glass` | `#EBEBEFFF` | `#FFFFFF14` | Small surfaces on Base: the header's space icon box, essentials tiles, pinned list. |
| `TextPrimary` | `#1C1C1EFF` | `#FFFFFFFF` |  |
| `TextSecondary` | `#6E6E73FF` | `#FFFFFF9E` |  |
| `TextDisabled` | `#00000038` | `#FFFFFF30` |  |
| `Icon` | `#6E6E73FF` | `#98989DFF` | Bar icons and the tab-count badge. |
| `Card` | `#FFFFFFFF` | `#2C2C2EFF` |  |
| `CardStrip` | `#FFFFFFF0` | `#2C2C2EF2` |  |
| `CardBorder` | `#E4E4EAFF` | `#FFFFFF14` | 1dp outline all around a card. |
| `CloseButton` | `#0000000F` | `#FFFFFF1A` |  |
| `CloseIcon` | `#6E6E73FF` | `#FFFFFFBF` |  |
| `DefaultAccent` | `#5B6CFFFF` | `#8EA2FFFF` | Fallback accent for spaces without a usable Zen colour. |
| `Primary` | `#1C1C1EFF` | `#FFFFFFFF` | Primary buttons: black in light, white in dark. |
| `OnPrimary` | `#FFFFFFFF` | `#1C1C1EFF` |  |
| `Destructive` | `#E5342BFF` | `#FF5A50FF` | Destructive actions: red text only, never a red fill. |
| `GroupedRow` | `#F2F2F5FF` | `#FFFFFF12` | Grouped rows on a sheet or a screen (menu, settings). |
| `ToggleOff` | `#E5E5EAFF` | `#3A3A3CFF` |  |
| `FocusRing` | `#B4B4BAFF` | `#FFFFFF4C` | The address input's ring while typing (neutral; "@" turns it into the brand gradient). |
| `Dock` | `#FFFFFFFF` | `#252527FF` | The dock: the bottom bar's own surface (also the drawer sheet). |
| `DockShadow` | `#3C281E12` | `#00000059` | Soft shadow the dock casts upward over the content. |
| `DrawerHandle` | `#00000029` | `#FFFFFF33` |  |
| `BarField` | `#F1F1F4FF` | `#FFFFFF14` |  |
| `SegmentPill` | `#FFFFFFFF` | `#4A4A4DFF` | The chosen option's pill in a segmented selector (on a [Glass] track). |
| `FieldHint` | `#8E8E93FF` | `#FFFFFF99` |  |
| `Skeleton` | `#0000000F` | `#FFFFFF12` |  |
| `IconBacking` | `—` | `#E9E9EEFF` | Behind favicons too dark to read on dark surfaces; Unspecified = never needed. |
| `Notice` | `#F2F2F5F2` | `#2C2C2EF2` |  |
| `Attention` | `#C0841AFF` | `#E8B45AFF` | Quiet sync notices: amber, never red. |
| `DrawerScrim` | `#1C1C2829` | `#00000073` | Over the overview while the drawer is open, at full open. |
| `DrawerScrimNoBlur` | `#1C1C2852` | `#0000009E` | The same without blur behind the drawer (Android 11, DeviceProfile.liveBlur): stronger. |
| `DrawerShadowNoBlur` | `#1C1C2829` | `#00000073` | Without blur, the drawer sheet also casts a taller shadow over the cards, at full open. |
| `SheetScrim` | `#1C1C284C` | `#00000080` | Over the page while a sheet or a floating card is open (the menu, prompts). |
| `CardShadowColor` | `#1E1E3C80` | `#000000FF` |  |
| `ContactShadow` | `#1C1C301A` | `#00000038` | Short, soft shadow a card casts on the card behind it, just above its top edge. |

#### Brand gradient

Pink → violet → blue: `#FF5FA2` → `#B46BFF` → `#4F8DFF` (`BrandTokens`, `ui/theme/Brand.kt`),
left to right (`BrandHorizontal`), or as a sweep (spinner, "@" ring). It means **Koan is working,
active, or holds your focus**.

| Allowed (the complete list) | Never |
|---|---|
| Page loading line on the dock's top edge (2dp, 8dp glow at 35%) | A passive "this is the current one" marker (the current tab is a state, not a moment) |
| Download completion moment; download progress bars in the list | Login or password UI, Mozilla's pages |
| Syncing indicator: a turning gradient arc (2dp stroke, 270° sweep, one turn per 900 ms) | Permission prompts |
| Toggles ON (gradient track at 90%) | Destructive actions |
| The "@" search mode: ring (2dp) and cursor | Body text |
| Settings' wash behind the top of the screen (320dp, 20% light / 14% dark), the account card outline (1.5dp), About | Behind web content |
| First-run "Connect your Zen Spaces" card outline (1.5dp) | Primary buttons (always black / white) |
| Tour / What's new: faint glow behind each title (240dp, 16% light / 12% dark), the welcome card (whole sheet) | Random gradients on cards |
| The faint glow behind the K on the sign-in choice page (owner's one exception) | |

#### Space colours

A Zen space's theme colour only marks **tiny space indicators**: the active space pill in the
switcher (with the space name) and the space's icon. Derivation (`ui/theme/Theme.kt` `SpaceColors`):
take the first colour of the Zen gradient; below 12% saturation it counts as grey and the space uses
`DefaultAccent` (`#5B6CFF` light / `#8EA2FF` dark); otherwise keep the hue, raise saturation to at
least 55%, clamp lightness to 45–62% (light) / 62–74% (dark). Pill = accent at 14% / 18% alpha;
text on the pill ("ink") = the accent at 38% / 80% lightness.

#### Scrims and dims

| Token | Light | Dark | Use |
|---|---|---|---|
| `SheetScrim` | `#1C1C284D` | `#00000080` | Over the page while a sheet or floating card is open |
| `DrawerScrim` | `#1C1C2829` | `#00000073` | Over the overview at full drawer open (with 14dp blur) |
| `DrawerScrimNoBlur` | `#1C1C2852` | `#0000009E` | Same on Android 11 (no live blur) |
| Stack dim | haze towards `Base`, ×1.25, max 0.85 | towards black, ×1, max 1 | Back cards in the depth stack (see 3.x Depth stack) |

### 1.2 Typography

- **Font:** the system default sans-serif (Compose `FontFamily.Default`). Koan bundles no font and
  sets no `Typography`; on the owner's phone that is the HyperOS system font.
- **Base style:** every text is a Material 3 `Text`, so it inherits Material 3's default
  `bodyLarge`: **line height 24sp, letter spacing 0.5sp**. Koan overrides only `fontSize` and
  `fontWeight`, so these two stay the same for every size below.
- **Weights in use:** Normal 400 (2 uses), Medium 500 (33), SemiBold 600 (32), Bold 700 (12).
  The weight per component is given in each component's section.

Type scale (every text size token, sp):

| Size (sp) | Tokens (where it is used) |
|---|---|
| **34** | `SettingsTokens.TitleSize` |
| **30** | `TourTokens.WelcomeTitleSize`, `SettingsTokens.AppNameSize` |
| **26** | `SignInTokens.HeroSize` |
| **22** | `TourTokens.TitleSize` |
| **20** | `OverviewTokens.TitleSize`, `FirstRunTokens.TitleSize` |
| **18** | `OverviewTokens.EmptyTitleSize`, `SheetTokens.CardTitleSize`, `SettingsTokens.AvatarTextSize` |
| **17** | `TourTokens.WelcomeLineSize`, `SignInTokens.TitleSize`, `BarTokens.FieldTextSize`, `SheetTokens.HeaderTitleSize` |
| **16** | `ButtonTokens.TextSize`, `SignInTokens.OutdatedSize`, `SignInTokens.ModeSize`, `SheetTokens.RowTitleSize`, `SheetTokens.NavFieldTextSize`, `SettingsTokens.RowTitleSize` |
| **15** | `CardTokens.TitleSize`, `TourTokens.LineSize`, `FirstRunTokens.ButtonSize`, `SignInTokens.BodySize`, `DrawerTokens.PinnedTitleSize`, `ToastTokens.TextSize`, `TabMenuTokens.TextSize`, `StartPageTokens.ContinueTitleSize`, `SearchTokens.TitleSize` |
| **14** | `CardTokens.PlaceholderDomainSize`, `OverviewTokens.EmptyBodySize`, `FirstRunTokens.BodySize`, `SignInTokens.NoticeSize`, `SignInTokens.LinkSize`, `BarTokens.SwitcherNameSize`, `SheetTokens.RowTrailingSize`, `SheetTokens.CardTextSize`, `SettingsTokens.RowValueSize`, `SettingsTokens.SegmentTextSize`, `SettingsTokens.VersionSize`, `SearchTokens.ChipTextSize` |
| **13** | `SignInTokens.SubtitleSize`, `SignInTokens.StepNumberSize`, `DrawerTokens.MoreTextSize`, `DrawerTokens.FolderCountSize`, `DrawerTokens.StatusTextSize`, `SheetTokens.TileLabelSize`, `SettingsTokens.SectionTitleSize`, `SettingsTokens.ChipTextSize`, `DownloadTokens.ChipTextSize`, `StartPageTokens.ContinueSubtitleSize`, `StartPageTokens.LabelSize` |
| **12** | `BarTokens.TabsCountSize`, `SheetTokens.RowSubtitleSize`, `SettingsTokens.RowSubtitleSize`, `SettingsTokens.FooterSize`, `ToastTokens.DetailSize`, `SearchTokens.LabelSize`, `SearchTokens.UrlSize` |
| **11** | `SignInTokens.BadgeSize` |
| **7** | `DownloadTokens.FileExtSize` |

### 1.3 Spacing

There is no named spacing scale: each component has its own tokens. The values actually used
(paddings, gaps, margins, insets), with how many tokens use each:

**2**dp ×4, **3**dp ×2, **4**dp ×2, **6**dp ×10, **7**dp ×2, **8**dp ×13, **9**dp ×1, **10**dp ×11, **12**dp ×20, **14**dp ×9, **16**dp ×11, **18**dp ×4, **20**dp ×8, **22**dp ×2, **24**dp ×5, **28**dp ×4, **32**dp ×2, **40**dp ×1, **44**dp ×1, **48**dp ×1, **56**dp ×1

Common rhythm: **6 / 8 / 10 / 12 / 14 / 16 / 20 / 24**. Screen side margin: 14dp (stack cards,
drawer, sheets, first run), 16dp (start page), 20dp (settings, sign-in pages), 24dp (tour).

### 1.4 Corner radii

| Radius (dp) | Used by |
|---|---|
| **6** | `PromptTokens.CheckRadius` |
| **8** | `BarTokens.TabsBadgeRadius` |
| **9** | `SheetTokens.RowIconRadius` |
| **10** | `OverviewTokens.SpaceIconRadius`, `SignInTokens.BracketRadius`, `DownloadTokens.FileTileRadius`, `StartPageTokens.ContinueTileRadius` |
| **12** | `PromptTokens.FieldRadius`, `SettingsTokens.SegmentRadius` |
| **14** | `DrawerTokens.EssentialRadius`, `SettingsTokens.RowRadius`, `SettingsTokens.FieldRadius` |
| **16** | `FirstRunTokens.AppIconRadius`, `SignInTokens.NoticeRadius`, `SheetTokens.TileRadius`, `ToastTokens.Radius`, `TabMenuTokens.Radius`, `StartPageTokens.ContinueRadius` |
| **18** | `DrawerTokens.PinnedRadius`, `SheetTokens.GroupRadius`, `DownloadTokens.GroupRadius`, `SearchTokens.GroupRadius` |
| **22** | `SignInTokens.ChoiceRadius`, `SettingsTokens.CardRadius` |
| **24** | `BarTokens.DockRadius` |
| **26** | `SettingsTokens.AppIconRadius` |
| **28** | `CardTokens.Radius`, `TourTokens.IconTileRadius`, `SignInTokens.ScanRadius`, `DrawerTokens.TopRadius`, `SheetTokens.CardRadius`, `SearchTokens.PickerRadius` |
| **30** | `TourTokens.FrameRadius` |
| **32** | `SheetTokens.TopRadius` |

### 1.5 Elevation and shadows

| What | Light | Dark |
|---|---|---|
| Stack card shadow (`CardElevation`, `CardShadowColor`) | 8dp, `#1E1E3C` at 50% | 20dp, black |
| Contact shadow (a card on the card behind it, just above its top edge) | `#1C1C30` at 10%, 14dp long, at most 35% of the visible sliver | black at 22%, 14dp |
| Dock's upward shadow (`DockShadow`, 28dp tall, no hairline) | `#3C281E` at 7% | black at 35% |
| Drawer sheet shadow over the cards without blur (Android 11, 56dp) | `#1C1C28` at 16% | black at 45% |
| Long-press lifted card | +16dp shadow, scale +3% | same |
| Dock popups (tab counter, back) | 12dp | same |
| Toggle knob | 3dp at 22% | same |
| Segmented pill | 2dp | same |
| About chips | 1dp | same |
| Pull-to-refresh disc | 4dp | same |
| Tour clip frame | 20dp | same |

### 1.6 Blur

| What | Value |
|---|---|
| Cards behind the open drawer | 14dp radius at full open, under `DrawerScrim`; baked into 2 blurred copies that cross-fade (no live blur per frame); none below Android 12 (stronger scrim + shadow instead) |
| Depth-stack thumbnails | radius by depth `[0, 0, 0, 0.9, 1.9, 2.9, 4]` dp (CSS σ `[0, 0, .5, 1, 1.6, 2.2, 2.8]`); only the top 512 px slice is blurred |

### 1.7 Hairlines

1dp everywhere: card outline (`CardBorder`, light `#E4E4EA`, dark white 8%), sheet and settings
dividers, sign-in choice cards' border, tour clip frame border. Focus rings are thicker: search
field 1.5dp (neutral) / 2dp ("@" gradient), brand outlines 1.5dp.

### 1.8 Motion

Springs are Compose `spring(dampingRatio, stiffness)`; damping 1.0 = no overshoot. Only
`graphicsLayer` properties animate in hot paths (translation, scale, alpha, clip, corner radius).
"Remove animations" (system setting) freezes the decorative loops.

| Spring | Damping | Stiffness | What moves |
|---|---|---|---|
| `ToggleTokens.Spring` | 0.72 | 520 |  |
| `SearchFieldTokens.Flow` | 1 | 140 |  |
| `Motion.CardSlot` | 0.8 | 420 |  |
| `Motion.SwipeReturn` | 0.75 | 500 |  |
| `Motion.Lift` | 0.7 | 600 |  |
| `Motion.Drop` | 0.8 | 500 |  |
| `Motion.PagerSettle` | 0.86 | 300 |  |
| `Motion.Drawer` | 0.9 | 420 |  |
| `Motion.TabSwipe` | 0.9 | 400 | Page view: the swiped page sliding out, the neighbour tab sliding in (or back). |
| `Motion.ScrubHighlight` | 0.85 | 900 | Page view space scrub: the highlight springing to the space under the finger. |
| `Motion.FolderChevron` | 0.85 | 600 |  |
| `Motion.SheetPush` | 1 | 520 | A page pushed into a sheet sliding in, or popped. |
| `Motion.Toast` | 1 | 700 | The bottom toast rising in and sinking away. |
| `Motion.Sheet` | 1 | 380 | Full-screen sheets (sign-in) sliding up and down. |
| `Motion.SearchScrim` | 1 | 700 | The page dimming behind the search sheet. |
| `Motion.Overview` | 1 | 600 | Overview open/close; also drives the bar morph. |
| `Motion.DockSettle` | 1 | 800 | The dock settling shown/hidden after a page scroll lets go. |

Durations (ms):

| Token | Value | What |
|---|---|---|
| `SignInSceneTokens.LOOP_MS` | 4400 |  |
| `SignInSceneTokens.PHONE_IN_MS` | 750 |  |
| `SignInSceneTokens.SCAN_MS` | 900 |  |
| `SignInSceneTokens.CHECK_MS` | 450 |  |
| `SignInSceneTokens.K_MS` | 400 |  |
| `SignInSceneTokens.RESET_MS` | 550 |  |
| `SignInSceneTokens.STILL_MS` | 3000 | Animations off: this moment, everything in place. |
| `BrandTokens.SPINNER_TURN_MS` | 900 |  |
| `StackTokens.WHEEL_SETTLE_MS` | 120 | A wheel/trackpad scroll settles on a card once no scroll event came for this long. |
| `StackTokens.FLING_MIN_SECONDS` | 0.3 |  |
| `StackTokens.FLING_MAX_SECONDS` | 2.2 |  |
| `TourTokens.VORTEX_TURN_MS` | 24000 |  |
| `TourTokens.K_RISE_DELAY_MS` | 180 |  |
| `TourTokens.SWEEP_AT_MS` | 750 |  |
| `TourTokens.SWEEP_MS` | 650 |  |
| `TourTokens.TEXT_AT_MS` | 850 |  |
| `TourTokens.TEXT_STAGGER_MS` | 120 |  |
| `TourTokens.TEXT_MS` | 380 |  |
| `TourTokens.INTRO_MS` | 1350 |  |
| `TourTokens.POP_SWEEP_AT_MS` | 180 |  |
| `TourTokens.FLOAT_MS` | 3400 |  |
| `TourTokens.FLOAT_TURN_MS` | 5300 |  |
| `FirstRunTokens.SKELETON_BREATH_MS` | 1400 |  |
| `FirstRunTokens.REVEAL_MS` | 420 | Crossfade from placeholders to the first real data. |
| `SignInTokens.FADE_MS` | 220 | The page fades in once it has painted, so its first (white) frame is never shown. |
| `SignInTokens.K_FLOAT_MS` | 4200 |  |
| `SignInTokens.K_TURN_MS` | 6400 |  |
| `SignInTokens.GLOW_BREATH_MS` | 5600 |  |
| `BarTokens.PROGRESS_MS` | 800 |  |
| `BarTokens.PROGRESS_FINISH_MS` | 250 |  |
| `BarTokens.PROGRESS_FADE_MS` | 350 |  |
| `BarTokens.SHIMMER_MS` | 1300 |  |
| `BarTokens.TAB_SWIPE_REST_MS` | 300 | Page view tab swipes: the tab the strip rests on goes live only after this long without a new swipe, so a chain of swipes shows screenshots and loads nothing on the way. |
| `SheetTokens.IMAGE_FADE_MS` | 180 |  |
| `PromptTokens.MANY_DIALOGS_MS` | 3000 | A dialog this soon after the last one offers "Don't let this page show more dialogs". |
| `ToastTokens.SHOW_MS` | 3000 |  |
| `TabMenuTokens.IN_MS` | 160 |  |
| `TabMenuTokens.OUT_MS` | 120 |  |
| `StartPageTokens.FADE_OUT_MS` | 180 |  |
| `PullTokens.START_TIMEOUT_MS` | 3000 | A reload that never starts loading lets the indicator go after this. |
| `PullTokens.MIN_SPIN_MS` | 600 | The brand spinner turns at least this long, even for an instant reload. |
| `HistoryTokens.CHEVRON_MS` | 200 |  |
| `PagerTokens.PAGE_APPEAR_MS` | 160 | A space's real page fading in over its ghost when it is composed on screen (after a landing). |
| `ThumbTokens.SETTLE_MS` | 1500 | Capture this long after a page finished loading (it settles: fonts, images, layout). |
| `ThumbTokens.PAINT_SETTLE_MS` | 800 | Capture this long after a page's first paint (an early screenshot for the overview). |
| `ThumbTokens.SCROLL_SETTLE_MS` | 1000 | Capture once the page has not scrolled for this long. |
| `ThumbTokens.CAPTURE_MIN_INTERVAL_MS` | 4000 | Background captures of a tab are at least this far apart. |
| `ThumbTokens.LEAVE_CAPTURE_TIMEOUT_MS` | 150 | Switching tabs waits at most this long for the outgoing page's screenshot. |
| `ThumbTokens.FADE_MS` | 140 | A card's real screenshot fades in over its placeholder. |
| `PageTokens.COVER_FADE_MS` | 140 | The cover fades out once the live page has painted. |
| `PageTokens.COVER_MAX_MS` | 2500 | Never cover a page longer than this, even if it never reports a paint. |
| `Motion.STACK_SNAP_TAU_SECONDS` | 0.09 | Stack snap: exponential glide, no overshoot (prototype: 17% of the remaining distance per 60 Hz frame = time constant ≈ 90 ms). Frame-rate independent here. |
| `Motion.FLY_OFF_MS` | 180 |  |
| `Motion.SPACE_MODE_FADE_MS` | 140 | Page view: the address pill crossfading to the space's name in space mode. |
| `Motion.REFRESH_BOOST_IDLE_MS` | 3000 | Keep the high refresh rate this long after the last touch/key: longer than the longest fling (2.2 s). |

Gesture thresholds:

| Gesture | Threshold |
|---|---|
| Stack scroll | 170dp of finger travel = one card slot; release past 0.15 slot commits; ≥ 2 slots/s is a fling: travels (v − 2.2) × 0.55 cards (≥ 1), v capped at 36 slots/s, cubic ease-out over 0.3–2.2 s; rubber band 0.45 at both ends |
| Swipe a card to close | axis picked after 6dp; closes past 35% of the card width or a sideways fling ≥ 800dp/s; tilt 0.03°/dp around the bottom centre; alpha −1 per 420dp (min 0.3); flies 480dp, tilt 8°, in 180 ms |
| Page ↔ overview on the dock | 220dp of vertical travel = whole morph; fling ≥ 600dp/s decides |
| Space pager (overview) | commit past 25% of the width or a fling ≥ 500dp/s; pages 40dp apart; incoming stack at 0.8× speed growing from 0.94; rubber band 0.3 |
| Space scrub (page view) | 24–90dp per space, fitted to the screen; first/last space reached 48dp before the edge |
| Tab swipe on the dock | tabs 24dp apart; the resting tab goes live after 300 ms without a new swipe |
| Sheets | commit past 25% of the gap to the next detent, or a fling ≥ 600dp/s; compact detent ≤ 55% of the screen; full detent 12dp below the status bar |
| Toast | swipe past 30% of its width or fling ≥ 800dp/s; shown 3 s |
| Pull to refresh | indicator moves 0.5× the finger; reloads past 72dp (max 120dp) |
| Dismiss overview / start page by pulling down | 48dp / 64dp |
| Dock on page scroll | always shown within 48dp of the top |

### 1.9 Icons

Koan's own stroked icon set (`ui/icons/KoanIcons.kt`): **24×24 grid, 2-unit stroke, round caps and
joins, single colour** (tinted by the caller). No Material icons library. Filled exceptions:
`star-filled`, `contrast` (half filled), `more` (three dots), and GitHub's mark (Octicons, MIT, only
on What's new's changelog card; not exported). All 40 icons are exported as SVG in `icons/`
(`currentColor`), overview sheet `icons/_overview.png`.

Rendered sizes (dp): bar icons 24 (48 touch); sheet rows 18 (30dp box, 9dp radius); sheet tiles
22; settings rows 20; dock popups 18; search glyph 18; card ✕ 14 (in a 30dp circle, 44 touch); pin
glyph 14; drawer pinned glyph 16; space icons 20 (switcher, header box 32dp), 14 (search chips);
sign-in choice 24 (44dp box); tour icon tiles 44 (96dp tile, 28dp radius); history chevron 18,
sync glyph 16; download file glyph 18 (40dp tile).

Space icons are Zen's own (emoji or Zen's SVG icon names), drawn by `ui/icons/ZenIcon.kt`.


## 2. Brand

"Koan" is a working codename. The app is not affiliated with Zen Browser: never call it Zen, never
use Zen's logo or name in Koan's own branding.

### 2.1 Logo files (`brand/`)

| File | What it is | Use |
|---|---|---|
| `koan-icon.png` | The full app icon: the 3D K on its tile | Launcher icon, About (104dp, 26dp radius), first-run Connect card (64dp, 16dp radius) |
| `koan-k.png` | The 3D K alone, transparent | Welcome card (210dp box), sign-in choice page (96dp), What's new sign-in scene |
| `koan-k-monochrome.svg` / `.png` | One-colour K | Source of Android's themed (monochrome) launcher icon (`res/drawable/ic_launcher_monochrome`) |
| `koan-k-24.svg` / `.png` | One-colour K on a 24-unit grid, `currentColor`, matching the icon set | Small glyph sizes; not used in the app yet |

| koan-icon.png | koan-k.png | koan-k-monochrome.png | koan-k-24.png |
|---|---|---|---|
| <img src="../design-export/brand/koan-icon.png" width="120" alt="koan-icon.png"> | <img src="../design-export/brand/koan-k.png" width="120" alt="koan-k.png"> | <img src="../design-export/brand/koan-k-monochrome.png" width="120" alt="koan-k-monochrome.png"> | <img src="../design-export/brand/koan-k-24.png" width="120" alt="koan-k-24.png"> |

### 2.2 The welcome look

The tour's first card and the What's new intro card: the **whole 92% sheet is the brand gradient**
(linear, top-left → bottom-right, `#FF5FA2` → `#B46BFF` → `#4F8DFF`; dark mode adds black at 14%).
Behind the 3D K, a slowly turning **vortex**: 8 soft white rings (120dp + 46dp per ring, each a
sweep of two bright arcs, alpha 0.20 fading outwards, 26dp strokes drawn three times for soft
edges, inner rings turning faster: 24°/s down to ~11°/s) and a white centre glow (35%, 168dp).
Around the K, six white **sparkles** (four-point stars with curved sides and a soft halo) twinkle
at their own periods (1.9–3.2 s). Text is white: title 30sp Bold, line 17sp Medium at 88%.

Motion: the K rises from below with a soft spring (damping 0.6, stiffness 120), untwisting from
−38° and growing from 0.72; a light sweep runs across it at 750 ms; the title then the line fade
up 14dp (380 ms, 120 ms apart). While shown, the K floats ±7dp (3.4 s) and tilts ±2.5° (5.3 s).
Swiping on, the K flies off to the left ahead of the page, the vortex trails at half speed, and
the gradient blends into the sheet's own colour. What's new starts with the K already in place: a
small pop (0.86 → 1, damping 0.5, stiffness 380). Full parameters: component "Welcome card" below.

The sign-in choice page uses a quieter version: the K (96dp) floating ±4dp with a brand glow
behind it (radius 0.95× the K, 30% light / 38% dark, breathing ±12% over 5.6 s) and two sparkles.

### 2.3 Do's and don'ts

**Do**
- Keep the UI neutral (greys, white, near-black) and let page thumbnails and favicons bring the
  colour.
- Use the brand gradient only for the moments in 1.1 "Brand gradient": working, active, focus.
- Keep the gradient's three colours in order (pink → violet → blue), left to right or as a sweep.
- Use the 3D K on the gradient or on the plain base; give it room (its sparkles sit outside it).
- Keep motion physical: springs, 1:1 finger tracking, no overshoot where things settle (damping 1).
- Primary buttons: black in light, white in dark, 50dp pill.

**Don't**
- Don't put the gradient on primary buttons, password or login UI, permission prompts,
  destructive actions, body text, or behind web content.
- Don't use the gradient to mark the current tab or space (that's a state, not a moment).
- Don't tint cards or use random saturated gradients on cards; no coloured header bars.
- Don't use a space's colour for anything beyond its tiny indicators (icon, active pill).
- Don't use red fills: destructive is red text only; sync problems are amber, never red.
- Don't use translucent bars with cards showing through: the dock is opaque.
- Don't recolour, stretch or outline the K, and don't pair it with Zen's logo.


## 3. Components

Each component: purpose, anatomy, sizes, states, light/dark, motion and gestures. Pressed states are Material 3's default ripple unless a section says otherwise; touch slop and long-press delay are Android's defaults.

## 3.A dock, address pill, tab counter, drawer, space switcher, dock popups

All values read from the code on branch `dev` (2026-11-09). Paths are relative to the repo root.
Abbreviations used below:

- `T` = `app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt`
- `bar/` = `app/src/main/java/dev/nerzouille/koan/ui/bar/`
- `menu/` = `app/src/main/java/dev/nerzouille/koan/ui/menu/`
- `browser/` = `app/src/main/java/dev/nerzouille/koan/ui/browser/`
- `ui/` = `app/src/main/java/dev/nerzouille/koan/ui/`

### Colour tokens used in this part (KoanPalette, `T`)

Light palette starts at `T:93`, dark at `T:139`. "@ n%" = that colour at alpha n.

| Token | Light | Dark | Ref (L / D) | Used for |
|---|---|---|---|---|
| `Dock` | `#FFFFFF` | `#252527` | T:113 / T:159 | dock surface, drawer sheet, dock popups |
| `DockShadow` | `#3C281E @ 7%` | `#000000 @ 35%` | T:114 / T:160 | upward shadow on the dock's top edge |
| `BarField` | `#F1F1F4` | `#FFFFFF @ 8%` | T:116 / T:162 | address pill fill |
| `FieldHint` | `#8E8E93` | `#FFFFFF @ 60%` | T:118 / T:164 | "Search or type URL", search glyph, completion title |
| `FocusRing` | `#B4B4BA` | `#FFFFFF @ 30%` | T:112 / T:158 | address input ring while typing |
| `TextPrimary` | `#1C1C1E` | `#FFFFFF` | T:97 / T:143 | host text, row titles, space icons |
| `TextSecondary` | `#6E6E73` | `#FFFFFF @ 62%` | T:98 / T:144 | find count, status row, folder glyphs, pin glyph, active dot, download progress arc |
| `TextDisabled` | `#000000 @ 22%` | `#FFFFFF @ 19%` | T:99 / T:145 | disabled back icon, disabled popup row |
| `Icon` | `#6E6E73` | `#98989D` | T:100 / T:146 | bar icons, tab-count badge border and number, popup icons |
| `Glass` | `#EBEBEF` | `#FFFFFF @ 8%` | T:96 / T:142 | essentials tiles, pinned group, download ring track |
| `DrawerHandle` | `#000000 @ 16%` | `#FFFFFF @ 20%` | T:115 / T:161 | drawer handle bar |
| `DrawerScrim` | `#1C1C28 @ 16%` | `#000000 @ 45%` | T:123 / T:169 | overview dim behind open drawer (live blur devices) |
| `DrawerScrimNoBlur` | `#1C1C28 @ 32%` | `#000000 @ 62%` | T:124 / T:170 | same on Android 11 (no blur) |
| `DrawerShadowNoBlur` | `#1C1C28 @ 16%` | `#000000 @ 45%` | T:125 / T:171 | taller sheet shadow on Android 11 |
| `Attention` | `#C0841A` | `#E8B45A` | T:122 / T:168 | amber dot on a sync-problem status row |
| `DefaultAccent` | `#5B6CFF` | `#8EA2FF` | T:106 / T:152 | space accent when its Zen colour is grey |
| `PillAlpha` (float) | 0.14 | 0.18 | T:136 / T:182 | alpha of the active space pill tint |
| `InkLightness` (float) | 0.38 | 0.80 | T:135 / T:181 | HSL lightness of the space name colour |
| `AccentLightness` (range) | 0.45–0.62 | 0.62–0.74 | T:134 / T:180 | HSL lightness clamp of the space accent |

Brand gradient (`BrandTokens`, theme-independent): Pink `#FF5FA2` (T:242) → Violet `#B46BFF`
(T:243) → Blue `#4F8DFF` (T:244), list `Colors` (T:245); `BrandHorizontal` =
horizontal gradient of those (`ui/theme/Brand.kt:30`).

Haptics used: `HapticFeedbackConstants.CLOCK_TICK` (light tick) and `LONG_PRESS`.
"Touch slop" and "long-press timeout" are Android's `ViewConfiguration` values (not set in Koan).

---

### Dock (KoanBar) + address pill (AddressRow, SearchRing, CleanTap)

**Purpose:** the one bottom bar shared by the page view and the overview: an opaque dock with the
address row; in the overview it grows a space-switcher row and further into the drawer.

**Anatomy (page view, left → right inside the address row, `bar/AddressRow.kt:197-241`)**
1. Dock surface (drawn, not a layout box): rounded-top rect in `Dock`, upward shadow, page load line on its top edge (`bar/KoanBar.kt:126-158`).
2. Back button (overview: crossfades to History) (`bar/AddressRow.kt:217-220`).
3. Address pill (host label; overview: "Search or type URL") (`bar/AddressRow.kt:221-234`).
4. Tab counter button (see next component) (`bar/AddressRow.kt:235`).
5. ⋯ More button (overview: crossfades to Settings) (`bar/AddressRow.kt:236-239`).
6. Optional download ring at the end of the pill (`bar/AddressRow.kt:293`).
7. Overview only, above the address row: switcher row (60dp) with the drawer handle centred at its top (`bar/KoanBar.kt:177-192`).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Address row height | 68dp | T:687, used `bar/KoanBar.kt:194` |
| Switcher row height (overview / scrub) | 60dp | T:688, `bar/KoanBar.kt:180` |
| Bottom margin under the rows (above nav bar / IME inset) | 6dp | T:686, `bar/KoanBar.kt:160-161` |
| Bar zone, page view | 68 + 6 = 74dp (+ nav-bar inset) | `bar/KoanBar.kt:65-70` |
| Bar zone, overview | 60 + 68 + 6 = 134dp (+ inset) | `bar/KoanBar.kt:65-70` |
| Row horizontal padding (dock margin) | 6dp | T:698, `bar/AddressRow.kt:200` |
| Gap between row items | 2dp | T:699, `bar/AddressRow.kt:202` |
| Button touch target | 48dp circle | T:700, `bar/AddressRow.kt:704-705` |
| Button glyph | 24dp | T:701, `bar/AddressRow.kt:720` |
| Pill height | 50dp, fully rounded (`CircleShape`) | T:695, `bar/AddressRow.kt:275-276` |
| Pill width | fills the rest: screen − 2×6 − 3×48 − 3×2 = screen − 162dp (derived) | `bar/AddressRow.kt:233` (`weight(1f)`) |
| Pill horizontal text padding | 18dp | T:697, `bar/AddressRow.kt:290` |
| Pill text | 17sp, `FontWeight.Medium`, 1 line, ellipsis, centred | T:696, `bar/AddressRow.kt:396-406` |
| Dock top corner radius in overview | 24dp | T:708 |
| Dock top corner radius as drawer | 28dp (`DrawerTokens.TopRadius`) | T:775 |
| Dock corner radius in page view | 0 (square) | `bar/KoanBar.kt:138` (`r` = 24·row·(1−d) + 28·d; row = 0 in page view) |
| Upward shadow height | 28dp, vertical gradient transparent → `DockShadow`, also spans the corner radius | T:709, `bar/KoanBar.kt:141-145` |
| Drawer handle | bar 36 × 4dp, pill-shaped, 8dp from top, `DrawerHandle` colour; touch area 72 × 24dp | T:691-694, `bar/KoanBar.kt:199-216` |

**Page load line** (page view only; fades out as the switcher row shows, `bar/KoanBar.kt:155-157`)

| Property | Value | Ref |
|---|---|---|
| Height | 2dp, on the dock's top edge, from the left | T:712, `bar/KoanBar.kt:272-278` |
| Fill | `BrandTokens.Colors` horizontal gradient spanning the filled width (blue end leads) | `bar/KoanBar.kt:274` |
| Glow | 8dp band at alpha 0.35 × 0.35, plus a 4dp band at alpha 0.35 | T:713-714, `bar/KoanBar.kt:276-277` |
| Start value | 6% as soon as a load begins | T:717, `bar/KoanBar.kt:257` |
| Ease to progress | tween 800ms, `CubicBezierEasing(0.2, 0.8, 0.3, 1)`, never backwards | T:718, `bar/KoanBar.kt:236,257-258` |
| Fade in | 175ms (350 / 2) | `bar/KoanBar.kt:249` |
| Finish | runs to 100% in 250ms, then fades out over 350ms | T:719-720, `bar/KoanBar.kt:261-262` |
| Shimmer | 72dp streak, white @ 55% in the middle, linear 1300ms loop, clipped to the filled part | T:723-725, `bar/KoanBar.kt:250-254,280-290` |

**States**

| State | What changes | Ref |
|---|---|---|
| Default (page view) | back · host pill · counter · ⋯ ; host in `TextPrimary`; empty url shows "Search or type URL" in `FieldHint` | `bar/AddressRow.kt:309-317,399` |
| Pressed (buttons, pill) | `LocalIndication` (Material ripple) on press, clipped to the 48dp circle / pill; a tap only fires if the finger lifts before the long-press timeout without passing the touch slop and no bar drag took it (`cleanTap`) | `bar/CleanTap.kt:26-53` |
| Long press (back, ⋯, counter, History) | after the long-press timeout held still: `LONG_PRESS` haptic, opens its action; the rest of the gesture is consumed | `bar/CleanTap.kt:60-77`, `bar/AddressRow.kt:707-713` |
| Disabled (back with no history) | glyph `TextDisabled`, no tap, no long press | `bar/AddressRow.kt:706-708,719` |
| Overview (morph 0 → 1) | back → History, ⋯ → Settings, host → "Search or type URL" (all alpha crossfades); counter stays. Tap pill = new tab with search. Long press History = Recently closed; ⋯'s long press (page view) = Settings | `bar/AddressRow.kt:195-196,218-238,321`, `ui/KoanRoot.kt:944-958` |
| Editing (address input) | see below | `bar/AddressRow.kt:410-551` |
| Find in page | find bar replaces the row | `bar/AddressRow.kt:603-655` |
| Downloading | progress ring at the pill's end | `bar/AddressRow.kt:331-359` |
| Tab swipe | host slides out, neighbour's favicon + host slides in | `bar/AddressRow.kt:298-393` |
| Space mode (long press on pill) | host gives way to the current space's icon + name, 140ms crossfade | `bar/AddressRow.kt:268-270,318-320`, `Motion.SPACE_MODE_FADE_MS` T:1191 |
| Hidden by scroll | slides down by up to its height | `browser/DockState.kt` |
| Keyboard for a page field | dock alpha 0 (not drawn) | `bar/KoanBar.kt:124`, `browser/DockState.kt:87-92` |
| Fullscreen video | the bar is not composed at all | `ui/KoanRoot.kt:900` |

**Address input (editing)** — `bar/AddressRow.kt:410-551`
- Pill 50dp, `BarField`, start padding 18 − 10/2 = 13dp, end 18dp (`bar/AddressRow.kt:452-456`).
- Search glyph 18dp in `FieldHint` (T:298, `bar/AddressRow.kt:459`), then 10dp gap (T:299, `bar/AddressRow.kt:460`).
- Text 17sp `TextPrimary`, regular weight (not Medium); hint "Search or type URL" in `FieldHint` (`bar/AddressRow.kt:462,502`). Keyboard: URI, IME action Go, no autocorrect (`bar/AddressRow.kt:504-508`).
- Inline completion: selected completed tail; " — Title" after it in `FieldHint` (`bar/AddressRow.kt:511-514,554-572`).
- Text scrolls inside the field, cursor kept 40dp from the edges (T:280, `bar/AddressRow.kt:468-497`).
- Trailing Close button (48dp / 24dp glyph) (`bar/AddressRow.kt:548`). Back, keyboard dismiss or focus loss cancels (`bar/AddressRow.kt:428,540-545,550`).
- Focus ring (`searchRing`, `bar/SearchRing.kt:29-78`): neutral 1.5dp stroke in `FocusRing` while typing (T:282, `bar/AddressRow.kt:453`).
- "@" mode (text starts with "@"): brand sweep gradient flows in from the left around both halves of the pill, stroke grows 1.5 → 2dp (T:283, `bar/SearchRing.kt:68`), halo 1.5dp each side at alpha 0.14 (T:286-287, `bar/SearchRing.kt:74`); neutral ring fades as `1 − f²` (`bar/SearchRing.kt:63`). Flow spring dampingRatio 1, stiffness 140 (T:300, `bar/AddressRow.kt:432`). Gradient turns 48°/s with wobbles 14° / 3.1s and 10° / 4.7s (T:293-297, `bar/SearchRing.kt:81-86`); turning off when animator scale is 0 (`bar/AddressRow.kt:439`). Cursor becomes `BrandHorizontal` (`bar/AddressRow.kt:503`).

**Find bar** — `bar/AddressRow.kt:603-655`: same 50dp `BarField` pill and padding as the input; search glyph 18dp + 10dp gap; hint "Find in page" (`FieldHint`, 17sp); match count ("3/12", "No matches") in `TextSecondary` at 12sp × 1.15 = 13.8sp (`bar/AddressRow.kt:647`); then three 48dp buttons: ArrowUp (previous), ChevronDown (next), Close. IME action Search = next.

**Download ring** — `bar/AddressRow.kt:331-359`: own 48dp circular hit target at the pill's end (text area loses 48 − 18 = 30dp of end padding, `bar/AddressRow.kt:288`); ring 18dp, stroke 2dp (T:1004-1005); track `Glass` full circle, progress arc `TextSecondary` from 12 o'clock, round cap; unknown size = fixed 90° arc. Tap: `CLOCK_TICK` + opens Downloads. Hidden in the overview (`bar/AddressRow.kt:229`).

**Swipe labels in the pill** — `bar/AddressRow.kt:361-393`: neighbour = favicon (or space icon) 20dp (`BarTokens.SwitcherIcon`, T:756) + 7dp gap (T:760) + name 17sp Medium `TextPrimary`. Labels travel 0.6 × text width (`bar/AddressRow.kt:386`); outgoing alpha `1 − 4|f|`, incoming `4(f − 0.18)` clamped (`bar/AddressRow.kt:392-393`).

**Light vs dark:** only the palette tokens above change (`Dock`, `DockShadow`, `BarField`, `FieldHint`, `FocusRing`, `Icon`, `TextDisabled`…). Dark shadow is much stronger (black 35% vs warm 7%). Geometry is identical. On Android 11 (no live blur) the drawer adds a 56dp `DrawerShadowNoBlur` shadow (`bar/KoanBar.kt:147-153`, T:800).

**Motion and gesture thresholds** (`bar/BarGestures.kt`)
- Axis chosen once the finger passes touch slop; horizontal if |dx| > |dy| (`bar/BarGestures.kt:69,193-201`).
- Page view: swipe up = overview opens under the finger; 220dp of travel is the whole morph (T:731, `ui/KoanRoot.kt:215`); release faster than 600dp/s goes regardless of position (T:734, `bar/BarGestures.kt:82,159`). Swipe down on the page's dock = nothing (`bar/BarGestures.kt:126-127`). Overview spring dampingRatio 1, stiffness 600 (`Motion.Overview` T:1207).
- Page view horizontal swipe = tab swipe (see TabSwipe below); long press on the pill only = space scrub (`bar/BarGestures.kt:97-113`).
- One `CLOCK_TICK` per committed step, on release (`bar/BarGestures.kt:84,152-164`).
- Morph: switcher row alpha `(p − 0.35) / 0.65`, slides 12dp (T:728, `bar/KoanBar.kt:181-186`); buttons and pill labels pure alpha crossfade; no relayout per frame.
- **Hide on scroll** (`browser/DockState.kt`): offset follows page scroll 1:1, 0 … dock height (bar zone + nav inset, `browser/BrowserScreen.kt:149-151`) (`browser/DockState.kt:60-67`). On finger up: if the page moved more than touch slop since down, finish in that direction; else snap to the nearer end (`browser/DockState.kt:75-80`). Settle spring dampingRatio 1, stiffness 800 (`Motion.DockSettle` T:1214). Stays shown on pages that cannot scroll (`browser/DockState.kt:63`, `browser/BrowserScreen.kt:172`). Locked shown while overview / editing / find is open (`ui/KoanRoot.kt:272`, `browser/DockState.kt:33-38`); snaps shown on tab change (`ui/KoanRoot.kt:273`); slides back when a new page load starts (`ui/KoanRoot.kt:857`).
- **TabSwipe** (`browser/TabSwipe.kt`): page width + 24dp gap per step (T:744, `ui/KoanRoot.kt:271`); rubber band 0.3 past first/last neighbour (T:1129, `browser/TabSwipe.kt:93-104`); commits past 25% of a page or a fling faster than 500dp/s (T:1117,1120; `browser/TabSwipe.kt:112-121`, `bar/BarGestures.kt:80`); spring dampingRatio 0.9, stiffness 400 (`Motion.TabSwipe` T:1185, `browser/TabSwipe.kt:126`); the landed tab goes live after 300ms without a new swipe (T:750, `browser/TabSwipe.kt:128`).

**Screenshots needed**
1. Page view, light theme, dock visible, pill showing `example.com`, back enabled, counter "3", no load line.
2. Same, dark theme.
3. Page view mid-load: load line about 40% across the top edge with its glow.
4. Page view, back disabled (a fresh tab with one page) to show the `TextDisabled` back glyph.
5. Editing: address input focused with "example.com" typed, neutral ring, Close button, keyboard up.
6. "@" mode: input showing "@" with the gradient ring.
7. Find in page open with "2/7".
8. Download ring in the pill at ~60%.
9. Overview (drawer closed): dock with 24dp top corners, handle, switcher row, History / "Search or type URL" / counter / Settings.
10. Page view scrolled so the dock is half hidden (optional, shows the slide).

---

### Tab counter button (TabsButton) and its long-press menu

**Purpose:** shows how many tabs the current space has; tap toggles page ↔ overview; long press (page view) opens a small menu.

**Anatomy** (`bar/AddressRow.kt:657-689`)
1. 48dp circular touch target, clipped circle for the ripple.
2. Rounded-square outline badge.
3. Count number centred (or "∞" above 99).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Touch target | 48dp | T:700, `bar/AddressRow.kt:663` |
| Badge | 26 × 26dp, corner radius 8dp | T:702-703, `bar/AddressRow.kt:677-678` |
| Badge border | 2dp, `Icon` colour | T:704, `bar/AddressRow.kt:678` |
| Number | 12sp, `FontWeight.SemiBold`, `Icon` colour | T:705, `bar/AddressRow.kt:681-686` |
| Count source | size of the current space's card stack | `ui/KoanRoot.kt:934` |
| Overflow | count > 99 shows "∞" | `bar/AddressRow.kt:682` |

**States**
- Default: outline badge + number (`Icon`: light `#6E6E73`, dark `#98989D`).
- Pressed: ripple in the 48dp circle (`cleanTap`, `bar/CleanTap.kt:26-40`).
- Long press (page view only): `LONG_PRESS` haptic, opens the Tab menu (`bar/AddressRow.kt:666-672`, `bar/AddressRow.kt:235`). In the overview there is no long press (`onHold = null`).
- Tap: page view → opens overview; overview → back to the page (`ui/KoanRoot.kt:946`).
- No disabled or selected state.

**Tab menu** (long press, `menu/TabMenu.kt`, built on DockPopup, see below)
- Rows: Plus "New tab", Close "Close tab", History "Reopen closed tab" (`menu/TabMenu.kt:32-34`).
- "Reopen closed tab" disabled when nothing to reopen: icon and text `TextDisabled`, not tappable (`menu/TabMenu.kt:34,42-44`).
- Anchored right: right edge on the dock's right margin; min width 200dp, grows left (T:1024, `menu/TabMenu.kt:27-30`).
- Grows out of the counter: anchor = 6 + 48 + 2 + 24 = 80dp from the screen's right edge (`menu/TabMenu.kt:29`).
- Row text 15sp `FontWeight.Medium` `TextPrimary`; icon 18dp `Icon` (`menu/TabMenu.kt:42-44`).

**Light vs dark:** palette only (`Icon`, `TextPrimary`, `TextDisabled`, `Dock`).

**Screenshots needed**
1. Page view dock, counter showing "3" (light and dark).
2. Counter at "∞" (a space with more than 99 tabs; use fake tabs from the debug fill).
3. Tab menu open above the dock, all three rows enabled.
4. Tab menu with "Reopen closed tab" greyed out.

---

### Space drawer (DrawerContent, DrawerBackdrop, SheetState)

**Purpose:** a sheet that grows out of the overview's dock with the current space's essentials, pinned tabs and Zen folders, and the sync status row.

**Anatomy** (`bar/SpaceDrawer.kt:72-200`)
1. Sheet surface = the dock itself (`Dock`), top corners 28dp at full open (`bar/KoanBar.kt:138`).
2. Drawer handle at the top of the switcher row (see Dock).
3. One page per space, sliding with the space pager (`bar/SpaceDrawer.kt:119-128`).
4. Per page, top → bottom, spaced 10dp: empty-state text (if nothing), essentials grid, pinned group (tab rows and folder rows), status row (`bar/SpaceDrawer.kt:165-199`).
5. Switcher row and address row stay at the bottom, below the sheet.
6. Behind: overview scrim + baked blur (`ui/KoanRoot.kt:772-780`, `bar/DrawerBackdrop.kt`).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Page padding | 14dp all sides | T:776, `bar/SpaceDrawer.kt:168` |
| Section gap | 10dp | T:777, `bar/SpaceDrawer.kt:169` |
| Essentials grid | 4 per row (empty cells as spacers), 8dp horizontal gap, 10dp row gap | T:777-778, `bar/SpaceDrawer.kt:134-154` |
| Essential tile | height 52dp, width = 1/4 of row, radius 14dp, `Glass` fill, favicon 26dp centred | T:779-781, `bar/SpaceDrawer.kt:140-148` |
| Pinned group | radius 18dp, `Glass` fill, rows stacked with no divider | T:783, `bar/SpaceDrawer.kt:182-196` |
| Pinned tab row | height 48dp; padding 12dp + 18dp × depth start, 12dp end; favicon 22dp; 12dp gap; title 15sp `TextPrimary`, 1 line ellipsis; active tab: 6dp dot in `TextSecondary` + 12dp; pin glyph 16dp `TextSecondary` only at depth 0 | T:782-787,791, `bar/SpaceDrawer.kt:202-229`, dot `ui/overview/TabCard.kt:453-455`, T:431 |
| Folder row | height 48dp, same indent; folder glyph (or the folder's Zen icon) 22dp `TextSecondary`; 12dp gap; name 15sp `FontWeight.Medium` `TextPrimary` (blank → "Folder"); 10dp gap; count 13sp `TextSecondary`; chevron-down 16dp `TextSecondary` at the right edge | T:782-792, `bar/SpaceDrawer.kt:232-270` |
| Status row | height 40dp, pill-clipped, 12dp horizontal padding, centred text 13sp `TextSecondary`; attention: 6dp `Attention` dot + 10dp; syncing: `BrandSpinner` 12dp + 10dp | T:793-794, T:594, `bar/SpaceDrawer.kt:272-295` |
| Empty state | "No essentials or pinned tabs in {space}" 15sp `TextSecondary`, 14dp vertical padding | `bar/SpaceDrawer.kt:171-178` |
| Compact detent cap | whole dock ≤ 55% of the window: 0.55 × window − (60 + 68)dp − (nav inset + 6dp) | T:811, `bar/KoanBar.kt:106-109` |
| Full detent cap | window − (top safe inset + 12dp) − 128dp − (nav inset + 6dp) | T:814, `bar/KoanBar.kt:108-110` |
| Full detent exists only if ≥ 48dp above compact | | T:817, `ui/sheet/SheetState.kt:69-73` |
| Compact / full height | min(content, cap) | `ui/sheet/SheetState.kt:57-60` |

Status texts (`bar/SpaceDrawer.kt:298-308`): "Connect your Zen Spaces", "Syncing with Zen…", "Synced {n min ago} · Sync now", amber: "Couldn't sync · tap to retry", "Offline · showing your spaces from {ago}", "Sign in again to keep your Zen Spaces in sync".

**States**
- Closed / Compact / Full detents (`ui/sheet/SheetState.kt:19`). Compact never scrolls; full scrolls a list taller than the screen (nested: up grows then scrolls, down scrolls back then shrinks) (`ui/sheet/SheetState.kt:91-104`).
- Pressed rows/tiles: default `clickable` ripple (`bar/SpaceDrawer.kt:146,209,239,281`). Only the settled space's page is interactive while open (`bar/SpaceDrawer.kt:95`).
- Folder collapsed: chevron rotated −90°; expanded 0° (`bar/SpaceDrawer.kt:234,267`).
- Active (current) tab: 6dp dot before the pin glyph.
- Status row attention (amber dot) / syncing (spinner) / normal.

**Backdrop**
- Scrim over the overview, alpha = drawer progress: `DrawerScrim` (light `#1C1C28 @ 16%`, dark black @ 45%); without live blur (Android 11) `DrawerScrimNoBlur` (32% / 62%) (`ui/KoanRoot.kt:772-777`).
- Blur (Android 12+ only): the overview is captured once when the drawer starts opening and baked into N blurred copies, crossfaded with the drag; never blurred per frame (`bar/DrawerBackdrop.kt:24-58`). Max radius 14dp (T:797); radii = 14dp × (i + 1) / N (`ui/KoanRoot.kt:252-256`); N = 2 on High and Mid RAM tiers, 1 on Low (`app/src/main/java/dev/nerzouille/koan/DeviceProfile.kt:63-67`). Edges clamp (`bar/DrawerBackdrop.kt:62-64`).
- Android 11: extra 56dp sheet shadow in `DrawerShadowNoBlur` × progress (T:800, `bar/KoanBar.kt:147-153`).

**Motion and gestures**
- Height follows the finger 1:1 (`ui/sheet/SheetState.kt:91-104`). `progress` = height / compact height, 0…1, drives scrim, blur, corners and content alpha (`ui/sheet/SheetState.kt:63`).
- Release: fling faster than 600dp/s goes to the next detent that way (T:823, `ui/sheet/SheetState.kt:122-123`); slow release commits to the neighbouring detent once the sheet has moved 25% of the gap away from the detent it started on, in either direction (T:820, `ui/sheet/SheetState.kt:125-134`).
- Spring `Motion.Drawer`: dampingRatio 0.9, stiffness 420 (T:1182, `ui/sheet/SheetState.kt:148`). A scrolled full list fling decays (`ui/sheet/SheetState.kt:162-170`).
- Content fades with progress and rises 30dp (10dp × 3 × (1 − progress)) (`bar/SpaceDrawer.kt:105-110,125`).
- Space switching slides pages by width + 40dp (`PagerTokens.PageGap` T:1121, `bar/SpaceDrawer.kt:123`); the sheet then springs to the new space's height (`ui/sheet/SheetState.kt:176-189`).
- Open: swipe up on the overview's dock, or tap the handle (toggles closed ↔ compact) (`bar/BarGestures.kt:123-124`, `ui/sheet/SheetState.kt:152`). Close: swipe down, tap above the sheet (`bar/BarGestures.kt:94,115`), handle, Back. `CLOCK_TICK` when it lands on another detent (`bar/BarGestures.kt:163`).
- Folder chevron spring dampingRatio 0.85, stiffness 600 (T:1192).

**Light vs dark:** surface `Dock`, tiles/group `Glass`, scrim and no-blur shadow differ per palette (see table). Geometry identical.

**Screenshots needed**
1. Overview with drawer at compact, light: 4–8 essentials tiles (example.com-style fake favicons), a pinned group with 2 tabs (one active with the dot), one expanded folder with 2 indented rows, status "Synced 2 min ago · Sync now"; blurred, dimmed cards behind.
2. Same, dark.
3. Drawer at full detent (a fake space with many pinned rows).
4. Collapsed folder (chevron rotated).
5. Status row in amber attention ("Couldn't sync · tap to retry").
6. Empty space: "No essentials or pinned tabs in Demo".

---

### Space switcher (WorkspaceSwitcher) and page-view scrub (SpaceScrub)

**Purpose:** Zen-style row of space icons; the active space sits in a pill tinted with its colour and shows its name. In the overview it follows the space pager; in the page view it appears during a long-press scrub.

**Anatomy** (`bar/WorkspaceSwitcher.kt`)
1. Row 60dp high (the dock's switcher row), items bottom-aligned with 2dp bottom padding (T:755, `bar/WorkspaceSwitcher.kt:109-110`).
2. Per space: item (icon at a fixed inset) laid out at its full pill width (`bar/WorkspaceSwitcher.kt:106-113`).
3. Active pill tint drawn behind (`bar/WorkspaceSwitcher.kt:118-126`).
4. Space icon (Zen icon, or the name's initial) (`bar/WorkspaceSwitcher.kt:144-149`).
5. Space name (only inside the pill) (`bar/WorkspaceSwitcher.kt:152-164`).
6. Drawer handle centred on the row's top (overview, see Dock).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Inactive item (touch target, no gap between items) | 44dp wide, 44dp tall | T:754, `bar/WorkspaceSwitcher.kt:112` |
| Icon | 20dp, `TextPrimary` tint | T:756, `bar/WorkspaceSwitcher.kt:145` |
| Icon inset from item left | 12dp (same collapsed and in the pill) | T:759, `bar/WorkspaceSwitcher.kt:133` |
| Icon → name gap | 7dp | T:760 |
| Pill end padding | 14dp | T:761 |
| Pill height | 36dp, radius 18dp (fully rounded), vertically centred in the 44dp item | T:762, `bar/WorkspaceSwitcher.kt:118-126` |
| Pill width | 44dp + max(0, 12 + 20 + 7 + 14 + nameWidth − 44) | `bar/WorkspaceSwitcher.kt:76-82` |
| Name | 14sp `FontWeight.SemiBold`, 1 line, ellipsis, max 140dp | T:763-764, `bar/WorkspaceSwitcher.kt:68,152-160` |
| Initial (no icon) | first letter uppercased, same 14sp SemiBold, `TextPrimary` | `bar/WorkspaceSwitcher.kt:147-148` |

**Colours** (`ui/theme/Theme.kt:55-87`)
- Accent = the space's first Zen gradient colour, saturation raised to ≥ 0.55, lightness clamped to `AccentLightness` (light 0.45–0.62, dark 0.62–0.74); saturation < 0.12 → `DefaultAccent` (light `#5B6CFF`, dark `#8EA2FF`) (T:190-193, `ui/theme/Theme.kt:62-79`).
- Pill tint = accent @ `PillAlpha` (light 14%, dark 18%) (`ui/theme/Theme.kt:67`).
- Name ink = accent at HSL lightness `InkLightness` (light 0.38, dark 0.80) (`ui/theme/Theme.kt:66`).

**States**
- Active (selected): pill drawn at full alpha, icon alpha 1, name visible (`bar/WorkspaceSwitcher.kt:118-126,139,162`).
- Inactive: no pill, icon alpha 0.7 (T:767, `bar/WorkspaceSwitcher.kt:139`).
- In between (dragging): the open amount is shared between the two neighbours under the finger (`1 − |i − pos|`); during an animated jump only the from/to pills open, an overshoot opens nothing (`bar/WorkspaceSwitcher.kt:177-187`). Name fades in only past 45% open (T:770, `bar/WorkspaceSwitcher.kt:162`); it is clipped to the pill width (`bar/WorkspaceSwitcher.kt:129`).
- Pressed: no ripple; taps go through `detectTapGestures` on the whole row (`bar/WorkspaceSwitcher.kt:90-97`).
- Disabled: taps off in the page view (`enabled = overviewOpen`, `ui/KoanRoot.kt:923`).
- Layout: centred when all items fit; otherwise the open pill stays centred, ends clamped; rubber band past an end keeps that end in view (`bar/WorkspaceSwitcher.kt:198-213`).

**Motion and gestures**
- Overview: horizontal drag anywhere on the dock moves the space pager (`bar/BarGestures.kt:122,144`); commit past 25% of the width or a fling > 500dp/s (T:1117,1120, `ui/overview/SpacePager.kt:90-91`, `bar/BarGestures.kt:80`); rubber band 0.3 (`ui/overview/SpacePager.kt:68-69`); settle spring dampingRatio 0.86, stiffness 300 (`Motion.PagerSettle` T:1181, `ui/overview/SpacePager.kt:104`). One `CLOCK_TICK` per committed switch on release (`bar/BarGestures.kt:156`).
- Tap an icon = jump to that space, one `CLOCK_TICK` (`bar/WorkspaceSwitcher.kt:94-96`).
- Page-view **scrub** (`bar/SpaceScrub.kt`): long press (system timeout) on the address pill only, held still → space mode, `CLOCK_TICK` (`bar/BarGestures.kt:97-113`). The switcher row grows out of the dock (spring `Motion.Overview` dampingRatio 1, stiffness 600: `bar/SpaceScrub.kt:98-100`, T:1207) and the pill label shows the space (see Dock). Sideways moves step the highlight: step = (distance from finger to the screen edge − 48dp) / spaces on that side, clamped to 24…90dp (smaller only if that many spaces would not fit) (T:737-741, `bar/SpaceScrub.kt:56-60,90-96`). Travel limited to half a step past the ends (`bar/SpaceScrub.kt:72`). Each step: `CLOCK_TICK` (`bar/BarGestures.kt:143`); highlight spring dampingRatio 0.85, stiffness 900 (`Motion.ScrubHighlight` T:1188, `bar/SpaceScrub.kt:78`). Release on another space switches to it; on the start space cancels (`bar/SpaceScrub.kt:83-87`, `bar/BarGestures.kt:155`).

**Light vs dark:** pill tint alpha 14% vs 18%; ink lightness 0.38 vs 0.80; accent lightness ranges differ; icon tint `TextPrimary` (`#1C1C1E` vs `#FFFFFF`).

**Screenshots needed**
1. Overview, light, 4 fake spaces (e.g. "Work", "Home", "Study", "Demo"), active one in the middle with its tinted pill and name.
2. Same, dark.
3. Mid-drag between two spaces (both pills half open).
4. Page view during a scrub: switcher row above the pill, pill showing the highlighted space's icon and name.
5. A space without an icon (initial letter).

---

### Dock popups (DockPopup, DockPopupRow, TabMenu, BackMenu)

**Purpose:** small popups opened by a long press on a dock button, sitting just above the dock on the side of their button.

**Anatomy** (`menu/DockPopup.kt`)
1. Full-screen transparent touch catcher (no scrim): first touch anywhere is consumed and dismisses (`menu/DockPopup.kt:65-80`).
2. Popup card: `Dock` fill, 16dp corners, 12dp shadow elevation (`menu/DockPopup.kt:114-116`, T:1011-1012).
3. Rows (`DockPopupRow`): leading icon or favicon, gap, text (`menu/DockPopup.kt:122-134`).

**Sizes and anchoring**

| Part | Value | Ref |
|---|---|---|
| Bottom offset | above nav inset + 68dp (address row) + 6dp (bottom margin) + 8dp gap = 82dp | T:1013, `menu/DockPopup.kt:84-85` |
| Side margin | 6dp (the dock's row padding) on the button's side; max width = screen − 12dp | `menu/DockPopup.kt:88-89,98` |
| Anchoring | `end = true`: right edge pinned at the right margin, grows left; else left edge pinned, grows right. The pinned edge never moves | `menu/DockPopup.kt:40-47,98` |
| Width | hugs content; with `minWidth`, measured at max intrinsic width ≥ minWidth | `menu/DockPopup.kt:90-97` |
| Corner radius | 16dp | T:1011 |
| Shadow | 12dp elevation (`Modifier.shadow`) | T:1012 |
| Row height | 44dp, 16dp horizontal padding | T:1014-1015, `menu/DockPopup.kt:128-130` |
| Leading icon / favicon | 18dp, then 12dp gap | T:1016-1017 |
| Row text | 15sp | T:1018 |
| Tab menu | min width 200dp, Medium weight text, anchor 80dp from the right edge | T:1024, `menu/TabMenu.kt:27-44` |
| Back menu | max width 300dp; max height 6 rows + half a row = 286dp (scrolls, half a row peeks); anchor 6 + 24 = 30dp from the left edge | T:1027-1028, `menu/BackMenu.kt:39-42` |

**Rows**
- Tab menu: icon in `Icon` + text `TextPrimary` Medium; disabled row both in `TextDisabled` and not clickable (`menu/TabMenu.kt:38-45`).
- Back menu: favicon 18dp + page title (or host, or the raw url) in `TextPrimary`, regular weight, 1 line ellipsis; the pages before the current one, most recent first (`menu/BackMenu.kt:34,44-53`). Tap = go back to that entry.

**States**
- Closed / opening / open / closing (`shown` 0 → 1) (`menu/DockPopup.kt:60-62`).
- Row pressed: default `clickable` ripple (`menu/DockPopup.kt:129`); rows are not clickable while closing (`interactive = open`).
- Row disabled: Tab menu's "Reopen closed tab" only.
- Back menu stays showing the old entries while it closes (`menu/BackMenu.kt:31-33`); it does not open when there is no earlier entry (`menu/BackMenu.kt:36`).

**Motion**
- In: tween 160ms; out: tween 120ms (default easing) on alpha and scale (T:1020-1021, `menu/DockPopup.kt:61`).
- Scale 0.85 → 1 (T:1019, `menu/DockPopup.kt:107-109`), transform origin = the button's centre x, bottom edge of the popup (`menu/DockPopup.kt:110-112`): it grows out of the button and shrinks back into it.
- Dismiss: tap anywhere else, or Back (`menu/DockPopup.kt:63,69-75`).
- Opened with a `LONG_PRESS` haptic from the button (`bar/AddressRow.kt:669,710`).

**Light vs dark:** card `Dock` (`#FFFFFF` / `#252527`); text and icon tokens per palette; the shadow is the platform elevation shadow (no palette colour).

**Screenshots needed**
1. Page view, light: Tab menu open above the dock at the right (New tab, Close tab, Reopen closed tab).
2. Same, dark, with "Reopen closed tab" greyed out.
3. Back menu open at the left with 3 fake history entries (example.com, example.org pages with titles).
4. Back menu with 8+ entries, showing the half-row peek at the bottom.

---

### Code vs docs/UI_SPEC.md (code wins)

- Space pager parallax: UI_SPEC "Bottom bar" says the stack trails at 0.85× and scales 0.96 → 1; code has `PagerTokens.PARALLAX = 0.2` (0.8×) and `INCOMING_SCALE = 0.06` (0.94 → 1) (T:1124-1125). Not part of this export's components, noted for whoever documents the overview.
- UI_SPEC "One sheet everywhere" lists 32dp top corners for sheets over a page; that is `SheetTokens.TopRadius` (T:826) for the ⋯ menu only. The drawer uses 28dp (`DrawerTokens.TopRadius`, T:775), as the drawer section of UI_SPEC also says.
- Otherwise the dock, popups, drawer and switcher values in UI_SPEC match the code.


## 3.B Overview, depth stack, transitions, start page, error pages, pull to refresh

All paths are relative to the repo root. Short forms used below:
`T` = `app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt`,
`OV/` = `app/src/main/java/dev/nerzouille/koan/ui/overview/`,
`UI/` = `app/src/main/java/dev/nerzouille/koan/ui/`.

Colours are given as `KoanPalette` token → Light / Dark (`T:93-137` Light, `T:139-183` Dark).
Colour notation: `#RRGGBB`, or `#RRGGBB @ a` for an alpha copy; `#AARRGGBB` where the code writes an 8-digit literal.

Palette tokens used in this file (resolved once):

| Token | Light | Dark | Ref |
|---|---|---|---|
| Base | #F5F5F7 | #1C1C1E | T:95 / T:141 |
| Glass | #EBEBEF | white @ 0.08 | T:96 / T:142 |
| TextPrimary | #1C1C1E | #FFFFFF | T:97 / T:143 |
| TextSecondary | #6E6E73 | white @ 0.62 | T:98 / T:144 |
| Card | #FFFFFF | #2C2C2E | T:101 / T:147 |
| CardStrip | white @ 0.94 | #F22C2C2E (≈ #2C2C2E @ 0.95) | T:102 / T:148 |
| CardBorder | #E4E4EA | white @ 0.08 | T:103 / T:149 |
| CloseButton | black @ 0.06 | white @ 0.10 | T:104 / T:150 |
| CloseIcon | #6E6E73 | white @ 0.75 | T:105 / T:151 |
| Primary / OnPrimary | #1C1C1E / #FFFFFF | #FFFFFF / #1C1C1E | T:107-108 / T:153-154 |
| Dock | #FFFFFF | #252527 | T:113 / T:159 |
| Skeleton | black @ 0.06 | white @ 0.07 | T:119 / T:165 |
| IconBacking | Unspecified (never used) | #E9E9EE | T:120 / T:166 |
| Attention | #C0841A | #E8B45A | T:122 / T:168 |
| DimToBase | true (back cards haze towards Base) | false (they darken towards black) | T:127 / T:173 |
| DimGain | 1.25 | 1.0 | T:128 / T:174 |
| DimMax | 0.85 | 1.0 | T:129 / T:175 |
| CardElevation | 8dp | 20dp | T:130 / T:176 |
| CardShadowColor | #1E1E3C @ 0.5 | #000000 | T:131 / T:177 |
| ContactShadow | #1C1C30 @ 0.10 | black @ 0.22 | T:132 / T:178 |
| ContactShadowLength | 14dp | 14dp | T:133 / T:179 |

Brand gradient (`BrandTokens`, T:241-254): Pink #FF5FA2 → Violet #B46BFF → Blue #4F8DFF, same in both themes.
`BrandSpinner` (`UI/theme/Brand.kt:37-62`): sweep-gradient arc of those colours, stroke 2dp (T:248), sweep 270° (T:249), one turn every 900ms, linear, repeating (T:250, Brand.kt:39-43), round caps.
`brandOutline` (`Brand.kt:33-34`): 1.5dp border (T:253) with the horizontal brand gradient.

---

### Depth stack (DepthStack + DepthCurve + StackState)

**Purpose:** the space's open tabs as an upright one-point-perspective deck: one big focused card, a pile converging upward behind it, and the next card peeking from below.

**Anatomy** (`OV/DepthStack.kt:76-443`)
- Viewport: a `BoxWithConstraints`, clipped except while the hero card flies out of it (`DepthStack.kt:224-228`).
- Per card, back to front by index: an optional `ContactShadow` layer (if there is a card behind it), the `TabCard`, and, for the card the page transition is flying, the `FlyingHero` drawn in that card's place (`DepthStack.kt:383-439`).
- Cards closed and flying off are drawn where they were in the pile: over the cards behind, under the ones in front (`DepthStack.kt:381-384, 440`).
- Only cards near the focus are composed: visible range plus `WINDOW_MARGIN` = 2 each side once settled, growing while idle to 16 behind / 10 in front, 1 more card per side per frame (`DepthStack.kt:158-185, 476-486`).

**Layout frame** (`StackFrame`, `DepthStack.kt:496-528`). W, H = viewport width/height.
- `cardWidth = W − 2 × CardSideMargin` with CardSideMargin = 14dp (T:324). (UI_SPEC says 12dp; code is 14dp.)
- `visibleBottom = H − UnderDock`, UnderDock = 24dp (T:387): the viewport runs 24dp under the dock's rounded top.
- `cardHeight = (visibleBottom − (PileTopGap + PeekVisible − PeekOverlap)) / (1 + PILE_DEPTH_FRACTION)`
  = `(visibleBottom − (22 + 60 − 40)dp) / 1.28` = `(visibleBottom − 42dp) / 1.28` (T:337, T:327, T:330, T:318; DepthStack.kt:503-505).
- `peekTop = visibleBottom − PeekVisible(60dp)` (DepthStack.kt:508).
- `focusTop = peekTop + PeekOverlap(40dp) − cardHeight` (DepthStack.kt:511).
- `A (pileDepth) = max(0, min(0.28 × cardHeight, focusTop − 22dp))` (DepthStack.kt:514-517). With the formulas above both terms are equal, so A = 0.28 × cardHeight on any screen.
- Cards are horizontally centred; `transformOrigin = (0.5, 0)` (top centre) (DepthStack.kt:572).

**The depth formula.** `d = state.pos − card.slot` (in card slots, fractional); a closed card keeps the `d` it had when closed (`DepthStack.kt:546`). d = 0 focused, d > 0 behind (pile), d < 0 in front.
Parameters: r = `R` = 0.45 (T:306), r_s = `SCALE_R` = 0.52 (T:309), sMin = `S_MIN` = 0.58 (T:312), `PEEK_SCALE` = 1.05 (T:333), `PEEK_DIM` = 0.12 (T:334).

| Property | d ≥ 0 (focus and pile) | −1 ≤ d < 0 (to the peek) | d < −1 (past the peek) | Ref |
|---|---|---|---|---|
| rise(d) | 1 − 0.45^d | 0 | 0 | DepthCurve.kt:26 |
| drop(d) | 0 | −d | 1 | DepthCurve.kt:29 |
| top (translationY) | focusTop − A·(1 − 0.45^d) | focusTop + (peekTop − focusTop)·(−d) | peekTop + (−1 − d)·(H − peekTop) | DepthStack.kt:523-527 |
| scale | 0.58 + 0.42·0.52^d | 1 + 0.05·(−d) (1.05 at d = −1) | 1.05 | DepthCurve.kt:31-32 |
| dim (overlay alpha on thumbnail) | keyframes `Dim`, linear between integer d | 0.12·(−d) | 0.12 | DepthCurve.kt:35, T:340 |
| blur radius (dp) | keyframes `BlurDp` (0 at d ≤ 0) | 0 | 0 | DepthCurve.kt:37, T:346 |
| card alpha | keyframes `Alpha` | 1 | 1 | DepthCurve.kt:40-41, T:347 |
| header dim | min(dim × 0.5, 0.2) | same | same | DepthCurve.kt:44, T:352 |
| ✕ alpha | clamp(1 − 2·abs(d), 0, 1) | same | same | TabCard.kt:245 |

Keyframes at d = 0, 1, 2, … joined linearly and held at the last value (`DepthCurve.kt:56-61`):
- `Dim` = [0, 0.16, 0.28, 0.38, 0.48, 0.58, 0.66] (T:340). Effective dim = min(dim × DimGain, DimMax) → Light ×1.25 capped 0.85, Dark ×1.0 capped 1.0 (`TabCard.kt:105`). Overlay colour: Light = `Base` (haze), Dark = black (`DepthStack.kt:218`).
- `BlurDp` = [0, 0, 0, 0.9, 1.9, 2.9, 4.0] dp (T:346); quantised to 0.5dp steps (`DepthStack.kt:200-213`). Only a top slice of 512px of the thumbnail is blurred (T:448, `TabCard.kt:419`). Without live blur (Android 11) or before baked copies exist, an extra dim of `NO_BLUR_DIM` 0.22 × (blurDp/4dp) is added instead (T:418, `TabCard.kt:181-193`).
- `Alpha` = [1, 1, 1, 1, 1, 0.85, 0.6, 0.3, 0] (T:347) → cards fade from d = 5 and are invisible at d = 8 (`MAX_VISIBLE_D` = 8, DepthCurve.kt:47). In front, cards are invisible below d = −2 (`MIN_VISIBLE_D`, DepthCurve.kt:53). Invisible cards get alpha 0 and are not hit-tested.
- Header dim: the result is then also × DimGain capped DimMax (`TabCard.kt:212`), so the header strip is dimmed at most 0.20 in Dark and 0.25 in Light.

Worked values (computed from the formulas): scale d=1 0.798, d=2 0.694, d=3 0.639, d=4 0.611; rise d=1 0.55, d=2 0.798, d=3 0.909, d=4 0.959.

**Corner radius vs depth:** the card's shape is a constant `RoundedCornerShape(28dp)` (T:421, `DepthStack.kt:490, 601`) applied before the layer scale, so on screen the corner is 28dp × scale(d) (e.g. ≈ 22.3dp at d = 1, 29.4dp at the peek). Not animated separately.

**Shadow:** every visible card has `shadowElevation = CardElevation + lift × 16dp` (Light 8dp, Dark 20dp; LiftShadow T:391), ambient and spot colour `CardShadowColor` (`DepthStack.kt:603-605`).

**Swipe tilt / fade** (applied to the same layer, `DepthStack.kt:574-595, 622-625`): tilt = swipeX(dp) × 0.03°/dp (T:395), pivot at the card's bottom centre; alpha × max(1 − abs(swipeX)/420dp, 0.3) (T:398-399).

**States**
- Focused (abs(d) < 0.5 is "focused" for taps, `DepthStack.kt:642`): full size, no dim, ✕ visible.
- Pile (d > 0): smaller, higher, dimmed/hazed, blurred from d = 3, fading from d = 5.
- Peek (d = −1): 1.05×, 12% dim, overlaps the focused card's bottom by 40dp; 60dp visible above the dock.
- Lifted (long press): see TabCard / Reorder below.
- Swiping / flying off: see Swipe-to-close below.
- Disabled (`enabled = false`): no pointer input at all; same look (used for the non-current space page and the skeleton stack; `DepthStack.kt:302-303, 317-318`, `SpacePage.kt:119`, `SpacePage.kt:181`).
- Pressed: no pressed visual (no ripple); the gesture handler is custom.
- Light vs dark: only the dim colour/gain/cap, elevation and shadow colours differ (table above).

**Scroll gesture** (`StackState`, `OV/StackState.kt`; gesture code `DepthStack.kt:317-378`)
- Axis decision: the first 6dp of travel (`GestureSlop`, T:408) picks the axis: more sideways than vertical = swipe-to-close of the focused card (wherever the finger is); else scroll. No movement until Android's long-press timeout = long press if on a card (`viewConfiguration.longPressTimeoutMillis`, system value, `DepthStack.kt:329-344`).
- Drag: 170dp of finger travel = 1 slot (`SlotDrag`, T:355; `DepthStack.kt:152`). Finger down moves earlier cards forward.
- Rubber band past both ends: delta × 0.45 / (1 + 2·overshoot) (T:381, `StackState.kt:127-132`).
- Release, v in slots/s (`StackState.kt:138-161`):
  - abs(v) < 2 (`FLING_MIN_VELOCITY`, T:364): snap. Moved more than 0.15 slot since the drag started (`SNAP_COMMIT`, T:361) → next whole card in that direction, else back to the start card (`DepthCurve.kt:69-78`).
  - Fling: travels max(1, (min(abs(v), 36) − 2.2) × 0.55) cards, rounded to a card, at least the next card (T:371-373, `DepthCurve.kt:84-91`). Motion: cubic ease-out x(t) = x0 + D·(1 − (1 − t/T)³), T = 3·D/v clamped to 0.3–2.2s (T:376-378).
- Snap glide (`animateTo`): exponential, pos += (target − pos)·(1 − e^(−dt/0.09s)), stops within 0.002 slot; no overshoot (`Motion.STACK_SNAP_TAU_SECONDS` = 0.09, T:1175; `StackState.kt:191-203`).
- A touch during a fling only stops it; that touch never opens or focuses a card (`DepthStack.kt:321-322, 349`).
- Mouse wheel / trackpad: same as a drag (× system scaled vertical scroll factor), settles like a slow release 120ms after the last event (`WHEEL_SETTLE_MS`, T:358; `StackState.kt:167-180`).
- Taps: tap the focused card = open the tab; tap the ✕ zone = close (fly off to the right); tap any other visible card = glide it into focus (`DepthStack.kt:348-354`). ✕ zone = the card's top `HeaderHeight` (44dp) × the rightmost `CloseTouch` (44dp), both × scale (`DepthStack.kt:641-643`).
- Card slot changes (gap closing after a close, reorder): spring `Motion.CardSlot` dampingRatio 0.8, stiffness 420 (T:1176; `StackState.kt:213-218`). Off-screen cards jump without motion (`DepthStack.kt:187-198`).

**Screenshot needed:** overview of a space with ≥ 8 fake tabs (example.com pages), focused on a middle card, so the pile shows 4–5 receding cards and the peek card shows at the bottom above the dock. One in Light, one in Dark.

---

### TabCard (card anatomy)

**Purpose:** one tab in the stack: header strip with favicon, title and close button over the page's screenshot (or a placeholder).

**Anatomy** (`OV/TabCard.kt:96-198`), bottom to top:
1. Card surface `Card`, plus a tint of the favicon's colour at 8% (`PLACEHOLDER_TINT` 0.08, T:434; `TabCard.kt:108`).
2. Placeholder thumbnail (only while there is no screenshot; see Placeholder thumbnail).
3. Screenshot: full width, top-aligned under the header (starts at y = 44dp), scaled to the card width, cropped at the bottom (`TabCard.kt:342-402`). Fades in over the placeholder in 140ms (`ThumbTokens.FADE_MS`, T:1155); a fresher screenshot crossfades over the old one in the same 140ms.
4. Dim overlay: rect of the dim colour at the depth dim (`TabCard.kt:178-194`).
5. Header strip (`CardHeader`, `TabCard.kt:202-258`): height 44dp (T:422), background `CardStrip`, padding start 14dp / end 6dp (T:423-424), vertically centred row:
   - Favicon 22dp (T:426).
   - 10dp gap (`HeaderGap`, T:425).
   - Title: 15sp (T:427), FontWeight.Medium, `TextPrimary`, 1 line, ellipsis. (UI_SPEC "Visual tokens" says card titles 16sp semibold; code is 15sp Medium, matching UI_SPEC "Visual restraint".) Split view cards join pane titles with "  |  " (`OverviewScreen.kt:316`).
   - Pinned marker (live pinned/essential tab): 10dp gap + pin glyph 14dp (T:432) tinted `TextSecondary`.
   - Current-tab marker: 10dp gap + 6dp circle (T:431) in `TextSecondary` (`CurrentTabDot`, `TabCard.kt:452-455`). Never the brand gradient.
   - ✕: 44dp touch box (T:428) holding a 30dp circle (T:429) of `CloseButton` with a 14dp close icon (T:430) tinted `CloseIcon`. Alpha = clamp(1 − 2·abs(d)), so it shows only on the focused card.
   - Header dim overlay (see depth table).
6. Hairline: 1dp border (T:433) in `CardBorder` with the 28dp card shape, all around (`TabCard.kt:196`).

**Sizes:** width/height from the stack frame (Depth stack). Corner radius 28dp (T:421). Clipped to the shape.

**States**
- Default / focused, pile, peek: per the depth table.
- Active (current tab): neutral 6dp dot after the title.
- Pinned: pin glyph after the title.
- Split view (`splitCount > 1`): one card; title joins every pane; placeholder domain line reads "host  ·  split view of N" (`TabCard.kt:274`).
- Never opened: placeholder thumbnail.
- Skeleton (first sync running): see FirstRun / Skeleton card.
- Lifted (long press): scale × (1 + 0.03·lift) (T:390), elevation + 16dp·lift (T:391), drawn above all others (zIndex 1) (`DepthStack.kt:398, 569, 603`).
- Pressed / disabled: no distinct visual.
- Light vs dark: only through the palette tokens above.

**Long-press lift and reorder** (`DepthStack.kt:366-374, 693-726`)
- After the system long-press timeout with no movement on a card: haptic `LONG_PRESS`, lift springs to 1 with `Motion.Lift` dampingRatio 0.7, stiffness 600 (T:1179).
- The lifted card follows the finger: slot = start + dy / 170dp, clamped to [−0.4, last + 0.4]; when its rounded slot changes, a tick haptic (`SEGMENT_TICK` on Android 14+, else `CLOCK_TICK`) and the other cards spring out of the way (`CardSlot` 0.8 / 420).
- Drop: slot springs to its target (CardSlot), lift back to 0 with `Motion.Drop` dampingRatio 0.8, stiffness 500 (T:1180).

**Swipe-to-close** (`DepthStack.kt:355-361, 665-687, 266-296`)
- Always moves the focused card, 1:1 with the finger (the 6dp slop included).
- While dragging: tilt 0.03°/dp, pivot bottom centre; alpha 1 − abs(x)/420dp, min 0.3.
- Release closes if abs(vx) > 800dp/s (`SwipeCloseFling`, T:405, flies the way it was flung) or abs(x) > 0.35 × card width (`SWIPE_CLOSE_FRACTION`, T:402). (UI_SPEC says "past 100dp"; code uses 35% of the card width.) Otherwise springs back with `Motion.SwipeReturn` dampingRatio 0.75, stiffness 500 (T:1178).
- Fly-off: tween 180ms (`FLY_OFF_MS`, T:1177), `FastOutLinearInEasing`, from the release x to ±480dp (T:409), tilt to ±8° (T:410), alpha to 0 (`StackState.kt:222-223`, `DepthStack.kt:578-583`). Haptic `CONFIRM`. The gap closes at once (it does not wait): closing the focused card brings forward the card on the side last scrolled towards (behind if not scrolled), the other side at an end (`DepthCurve.kt:100-104`).

**Screenshot needed:** (1) focused card close-up with a real screenshot (example.com), current-tab dot visible; (2) a pinned live tab card showing the pin glyph; (3) a card mid-swipe (≈ 30% across, tilted and faded); (4) a lifted card during reorder. Light and Dark.

---

### Placeholder thumbnail (never-opened tab)

**Purpose:** what a card shows before its page has ever been captured: neutral surface, big favicon, domain.

**Anatomy** (`OV/TabCard.kt:264-280`): column centred horizontally, top padding = header 44dp + `PlaceholderTop` 44dp = 88dp (T:436); favicon 64dp (T:435); 14dp gap (T:437); domain text 14sp (T:438) `TextSecondary`, 1 line. Background is the card's `Card` + 8% favicon-colour tint.
- Deep in the pile it is captured once and crossfaded through pre-blurred copies; it blurs live until they are ready (`TabCard.kt:112-176`). Visual result: same blur as a screenshot at that depth.
- Same placeholder, full screen, is used as the page cover (`UI/browser/PageCover.kt:113-135`): top spacer 44dp × 4 = 176dp, favicon 64dp, gap 14dp, domain 14sp `TextSecondary` on `Card`. A Zen tab that only the desktop can open adds "This tab opens only on your computer." (14sp, `TextSecondary`, centred, horizontal padding 44dp).

**Screenshot needed:** a card for a never-opened tab of a fake host (e.g. "example.org") in focus, Light and Dark; and the full-screen cover variant.

---

### Favicon

**Purpose:** a tab's icon: Zen static icon, else the real favicon, else a coloured letter tile.

**Anatomy** (`OV/Favicon.kt:83-154`)
- Zen static icon: the glyph tinted `TextPrimary` (`Favicon.kt:85-90`).
- Letter tile: rounded square, corner = size × 0.28 (`Favicon.kt:104`); background = the host's colour; letters Bold, font size = size × 0.42 for 2+ letters, × 0.55 for one (`Favicon.kt:146-152`), colour white unless the host entry says otherwise.
  - Colour: a fixed table of 22 well-known public hosts (`Favicon.kt:40-63`), else one of 8 muted tones picked by host hash: #4F6D8F, #7A5C8E, #5E8C6A, #8E6A4F, #4F8A8B, #8B4F5E, #6B6F8F, #7F7A4A (`Favicon.kt:66-68`). Letter = first letter of the second-level domain, uppercased; "•" if none (`Favicon.kt:70-76`).
- Real favicon: fitted in the same rounded square; fades in over the letter tile in 140ms (`Favicon.kt:100, 134`). An icon already in memory draws in the first frame without a fade.
- Dark: an icon detected as dark is placed on an `IconBacking` tile (#E9E9EE) at 72% of the size (`Favicon.kt:108-128`). Light: no backing (`IconBacking` = Unspecified).

**Sizes used:** card header 22dp (T:426), placeholder 64dp (T:435), Continue card 24dp (T:1044), essentials tile 26dp (T:781).

**States:** letter tile → fading → real icon; backed (dark icon in Dark theme). No pressed/disabled.

**Screenshot needed:** a row of cards/essentials showing a letter tile (fake host), a real favicon, and in Dark a dark favicon on its light backing.

---

### ContactShadow

**Purpose:** the short soft shadow a card casts upward onto the card behind it, which layers the pile.

**Anatomy** (`OV/ContactShadow.kt:17-64`): a separate layer drawn just under the casting card, with the card's size and transform.
- Vertical gradient from `ContactShadowLength` (14dp) above the card's top edge down to the edge; alpha ∝ t² at stops 0, 0.25, 0.5, 0.75, 1 (`ContactShadow.kt:32-39, 64`).
- Horizontal mask fading the ends before the corners: transparent at 0.5 × radius, opaque from 1.5 × radius, symmetric at the right (radius = 28dp) (`ContactShadow.kt:40-47`).
- A full-strength band of height 28dp under the card's top edge, so the shadow meets the rounded edge without a cut (`ContactShadow.kt:56-58`).
- Length = min(14dp, 0.35 × the visible sliver of the card behind) (`CONTACT_SHADOW_MAX_FRACTION`, T:441; `DepthStack.kt:554-561`); 0 if either card is invisible or the card behind is closed. Not drawn under 0.5px.
- Fades with the card (same alpha incl. swipe fade).

**Light vs dark:** colour `ContactShadow`: Light #1C1C30 @ 0.10, Dark black @ 0.22; length 14dp in both.

**Screenshot needed:** close crop of the focused card's top edge over the pile card behind it (Light, where it is subtle, and Dark).

---

### Overview screen (OverviewScreen + SpacePage + SpaceHeader)

**Purpose:** the space overview: one page per space with a compact header and the depth stack; the dock/bar is drawn over the bottom zone by KoanRoot.

**Anatomy** (`OV/OverviewScreen.kt:166-255`, `OV/SpacePage.kt:84-131`)
- Full-screen background: flat `Base` (`OverviewScreen.kt:173`). **No gradient background per space**: the code paints only `Base` in both themes; the space gradient is used only to derive the small accent (`Theme.kt:55-69`). UI_SPEC agrees ("no space-colour wash anywhere").
- Column: the space pages area (top safe-area padding, `topSafePadding`) then a bottom spacer of navigation-bar inset + (dock height − 24dp) so the stack runs 24dp under the dock (`OverviewScreen.kt:175-245`).
- Per space page: `SpaceHeader`, optionally a compact Connect card (phone-only space, signed out, with tabs), then a box with the stack, the skeleton stack, the full Connect card or the empty state (`SpacePage.kt:102-130`).
- **Essentials grid and pinned row are not on the overview**: they live in the bar's drawer (`SpacePage.kt:79-83`; drawer in `UI/bar/SpaceDrawer.kt`). The start page reuses the essentials grid (see Start page).

**SpaceHeader** (`SpacePage.kt:189-244`)
- Height 52dp (T:452), horizontal padding 6dp (T:453), row centred vertically.
- Back button: 44dp circular touch (T:454), back glyph 22dp (T:455) `TextPrimary`, ripple via `clickable`.
- Space icon box: 32dp (T:457), corner 10dp (T:458), `Glass` fill, Zen icon 20dp (T:459) tinted `TextPrimary`.
- 10dp gap (T:456).
- Space name: 20sp (T:460) Bold `TextPrimary`, 1 line, ellipsis.
- Sync badge (`SyncBadgeView`, `SpacePage.kt:251-267`): 36dp circular touch (T:592); None = nothing; Syncing = `BrandSpinner` 12dp (T:594); Attention = 7dp dot (T:593) in `Attention` (Light #C0841A, Dark #E8B45A). Tap = sync now / sign in.
- Gesture: vertical drag down on the header > 48dp (T:462) dismisses the overview back to the page.
- (UI_SPEC lists "tap space name → space menu"; the current header has no tap action on the name.)

**Empty space** (`SpacePage.kt:146-164`): centred column, horizontal padding 32dp (T:465); "No open tabs in {space}" 18sp (T:466) SemiBold `TextPrimary`, 1 line; 6dp (T:468); "Its pinned tabs are in the drawer below." 14sp (T:467) `TextSecondary`; 24dp (T:469); `PrimaryButton` "New tab" with a 20dp plus glyph (T:461) in `OnPrimary` (button component from `UI/kit`, documented elsewhere).

**States:** current space (stack enabled), neighbour spaces (composed but disabled), ghost (not composed yet, see GhostSpace), first sync (skeleton stack), signed-out phone-only space (Connect card), empty space.

**Light vs dark:** palette tokens only.

**Screenshot needed:** (1) overview at rest on a space with fake tabs, header with icon + name; (2) the empty-space state; (3) header with the syncing spinner; (4) header with the amber attention dot. Light and Dark for (1).

---

### SpacePager (space switching + parallax)

**Purpose:** horizontal pager between spaces, driven only by the workspace switcher in the bottom bar (never by swipes in the card area).

**Motion** (`OV/SpacePager.kt`, `OV/OverviewScreen.kt:191-240`, `SpacePage.kt:133-140`)
- One page of travel = window width + `PageGap` 40dp (T:1121; `KoanRoot.kt:184`); the finger drives it 1:1.
- Each page: translationX = (i − pos) × (width + 40dp); alpha 0 once a full page away; a `Base` overlay at alpha abs(i − pos) fades it into the background as it leaves (`OverviewScreen.kt:195-204`).
- Parallax of the stack inside the page: translationX = −f × width × 0.2 (`PARALLAX`, T:1124) and scale = 1 − 0.06 × abs(f) (`INCOMING_SCALE`, T:1125), f = page offset; the header moves with the page, so the stack trails it (0.8× speed) and grows from 0.94 to 1.
- The cards' own alpha also × (1 − abs(f)) (`SpacePage.kt:125`).
- Rubber band at the first/last space: × 0.3 (T:1129; `SpacePager.kt:62-72`); never more than one page from the settled one.
- Release: commits to the neighbour past 0.25 of a page (`COMMIT_FRACTION`, T:1117) or on a fling > 500dp/s in the drag direction (`FlingVelocity`, T:1120; `BarGestures.kt:80`); settle spring `Motion.PagerSettle` dampingRatio 0.86, stiffness 300 (T:1181), carrying the release velocity. A committed change gives a haptic tick (`BarGestures.kt:156`).
- A page composed on landing fades in over its ghost in 160ms (`PAGE_APPEAR_MS`, T:1128).

**Screenshot needed:** mid-switch frame (about 40% between two spaces) showing the outgoing page fading and the incoming stack slightly smaller and trailing its header.

---

### GhostSpace

**Purpose:** a cheap stand-in for a neighbour space's page before it is composed for real, so a swipe never pops a page in.

**Anatomy** (`OV/GhostSpace.kt:33-97`): the real `SpaceHeader`, then one canvas drawing the stack at rest with the same frame: for each integer depth from min(focus, 8) down to −1 (−1 only if a card exists in front): card rect in `Card` × the depth alpha, scaled per `DepthCurve.scale`, corner 28dp × scale; the focused card's screenshot only if already in memory; header band `CardStrip` (44dp × scale); dim overlay (`Base` in Light, black in Dark, depth dim × DimGain capped DimMax); 1dp `CardBorder` outline. No favicons, titles, blur or contact shadows. Hidden while the page ↔ overview hero is flying (`GhostSpace.kt:45-49`). Same parallax as the real page.

**States:** shown, then faded out under the real page (160ms).

**Screenshot needed:** none separately (it is meant to be indistinguishable); optional: first frame of a space switch on a slow device.

---

### FirstRun (skeleton stack + Connect card)

**Purpose:** never a blank overview: breathing placeholder cards while the first sync runs, and one calm card inviting the Mozilla sign-in.

**Skeleton stack** (`OV/SpacePage.kt:166-187`, `OV/FirstRun.kt:32-34`, `OV/TabCard.kt:424-449`)
- 6 skeleton cards (T:572), a real `DepthStack` (disabled) focused on index 4.
- Whole stack alpha breathes 1 ↔ 0.55 (T:586), 1400ms tween each way, reversing (T:587).
- Skeleton card: `Card` surface with depth dim; header strip 44dp `CardStrip`, padding 14/6dp; a 22dp rounded square (corner 22 × 0.28 dp) and a 140 × 10dp pill (T:419-420), both `Skeleton` (Light black @ 0.06, Dark white @ 0.07); 1dp `CardBorder` outline.
- When the first real spaces arrive, they crossfade in over the skeletons in 420ms (`REVEAL_MS`, T:590; `OverviewScreen.kt:109-121, 246-254`).

**Connect card, full** (`OV/FirstRun.kt:40-91`): side margin 14dp (T:573), corner 28dp, `Card` fill, 1.5dp brand-gradient outline, padding 24dp (T:574), items centred, spaced 10dp (T:575): app icon 64dp (T:582) with 16dp corners (T:583); "Connect your Zen Spaces" 20sp Bold `TextPrimary` centred (T:576); body 14sp `TextSecondary` centred (T:577); an extra 10dp spacer; `PrimaryButton` "Sign in".
**Connect card, compact** (phone-only space already has tabs): full-width pill (CircleShape), `Card` fill, brand outline, vertical padding 12dp (T:579), "Connect your Zen Spaces" 15sp Medium `TextPrimary` (T:578). Tap = sign in (ripple).

**Screenshot needed:** (1) first-run overview signed out with the full Connect card on an empty phone-only space; (2) the skeleton stack during first sync; (3) compact Connect pill above a stack of fake tabs.

---

### Page ↔ overview transition (OverviewMotion + CardHero)

**Purpose:** opening a card into the page and back, without a double image: the overview stays opaque and only the tab's card flies between the page rect and its slot.

**Progress driver** (`UI/OverviewMotion.kt`): progress 0 = page, 1 = overview.
- Spring on its own: `Motion.Overview` dampingRatio 1.0 (no overshoot), stiffness 600 (T:1207), carrying a drag's release velocity (`OverviewMotion.kt:57-61`). (UI_SPEC says ≈ 0.85 / 380 and "a tiny overshoot" on landing; code is critically damped 1.0 / 600.)
- Finger-driven on the dock: 220dp of vertical travel = the whole morph (`BarTokens.OverviewDrag`, T:731; `KoanRoot.kt:215`). Release commits past 0.25 of the way (`PagerTokens.COMMIT_FRACTION`, T:1117) or on a vertical fling > 600dp/s (`BarTokens.OverviewFling`, T:734; `OverviewMotion.kt:45-54`, `BarGestures.kt:82, 159`).

**What moves** (p = progress)
- Overview zoom: scale = 1 + 0.04 × (1 − p) around the window centre (`OVERVIEW_ZOOM`, T:1208; `KoanRoot.kt:764-771`): the overview settles from 1.04 to 1.0 as it opens. It is always opaque.
- Flying card (`FlyingHero` / `HeroBody`, `OV/CardHero.kt:129-279`): the card's own header + the tab's screenshot (or placeholder) on `Card`/`Base`. Layout height = max(header + window height × cardWidth / windowWidth, cardHeight).
  - Scale lerps from pageWidth/cardWidth to slotWidth/cardWidth; position lerps from the page's top-left to the slot's top-left (`CardHero.kt:195-204`).
  - Visible band: top lerps from 44dp (header hidden just above the screen) to 0 (header shown); bottom lerps from the page bottom to the card bottom.
  - Corner radius lerps 0 → 28dp on screen (`CardHero.kt:241`).
  - Hairline `CardBorder` 1dp drawn at alpha p (`CardHero.kt:257-269`).
  - Elevation = `CardElevation` (8dp Light / 20dp Dark), shadow colour `CardShadowColor`.
  - The real card is hidden while it flies (`CardHero.kt:96`).
- Other cards (`DepthStack.kt:614-620`, e = p while flying): cards behind the focus rise out from under the focused slot (translationY from topOf(0) to their place) and fade in (alpha × e); cards in front slide up from below the screen's bottom edge, opaque.
- Space header: alpha = clamp(1.5e − 0.5, 0, 1), translationY = −(1 − e) × header height (slides in from above; gone a third of the way out) (`SpacePage.kt:192-201`).
- Fallback (no stack shows the card: new tab, link from another app): the page itself shrinks to 0.85 around the centre (× the zoom), opaque, no header (`HERO_FALLBACK_SCALE`, T:1211; `CardHero.kt:164-186`).
- After the overview settles open, deferred work starts (neighbour spaces composed, blur baking) (`KoanRoot.kt:167-174`).

**Page cover** (`UI/browser/PageCover.kt:62-91`): on the page side, the tab's screenshot (or placeholder) covers the engine view at once and fades out in 140ms (`COVER_FADE_MS`, T:1161) once the live page has painted (+32ms), never later than 2500ms (`COVER_MAX_MS`, T:1164). Background `Base`. A blank new tab stays covered (plain `Base`).

**Screenshot / clip needed:** a short screen recording of tap-to-open and swipe-down-to-overview on a fake tab (example.com), plus one still at ~50% progress showing the card mid-flight with partly rounded corners and the header sliding in.

---

### Start page

**Purpose:** what Koan shows when it comes back after a while unused: the current space's essentials and a "Continue" card for the last tab, above the dock whose search field is focused. Not a tab.

**Anatomy** (`UI/start/StartPage.kt:52-179`)
- Full-screen `Base` background.
- Bottom-aligned column, horizontal padding 16dp (T:1033), bottom padding = dock height (above keyboard) + 20dp (T:1035).
- Space row: icon box 32dp, 10dp corners, `Glass`, Zen icon 20dp `TextPrimary`; 10dp gap; space name 20sp Bold `TextPrimary`, 1 line, ellipsis (same tokens as the overview header).
- 14dp gap (T:1036).
- Essentials grid (`UI/bar/SpaceDrawer.kt:133-154`): 4 per row, 8dp between tiles (T:778), 10dp between rows (T:777); tile height 52dp (T:779), corner 14dp (T:780), `Glass` fill, favicon 26dp (T:781) centred; empty slots in the last row are blank. Tap opens the tab (ripple). Empty: "No essentials in {space}" 15sp (T:786) `TextSecondary`.
- Continue card (only if the last tab still exists): 20dp above (T:1040); label "Continue" 13sp (T:1050) Medium `TextSecondary`; 8dp (T:1041); row: corner 16dp (T:1042), `Glass` fill, padding 12dp (T:1043); a 40dp tile (T:1045), 10dp corners (T:1046), `Base` fill, holding the favicon 24dp (T:1044); 12dp gap (T:1047); title 15sp (T:1048) Medium `TextPrimary` (falls back to the host), host 13sp (T:1049) `TextSecondary`, both 1 line ellipsis. Tap = back to that tab (same as Back).

**States / motion**
- Open: appears at once (alpha snaps to 1); closing fades out in 180ms (T:1051) and is not interactive while fading.
- Swipe down > 64dp anywhere on it (T:1037), Back, or the field's ✕ dismisses it to the last tab.
- No essentials / no last tab variants.

**Light vs dark:** palette tokens only (Base, Glass, TextPrimary, TextSecondary).

**Screenshot needed:** start page over a fake space with 4–8 fake essentials and a Continue card for an example.com tab, keyboard up with the dock's field focused. Light and Dark. Also the "No essentials" variant.

---

### Error pages

**Purpose:** what the user sees when a page fails to load.

**Finding:** Koan does **not** have its own error page. The engine's only `RequestInterceptor` is `ExternalLinks` (`app/src/main/java/dev/nerzouille/koan/engine/ExternalLinks.kt:17`, registered at `app/src/main/java/dev/nerzouille/koan/engine/Components.kt:76`), and it overrides only `onLoadRequest` (`ExternalLinks.kt:20`); there is no `onErrorRequest`, no `ErrorPages`/`ErrorType` handling and no error-page asset anywhere in `app/src/main` (grep for onLoadError, onErrorRequest, ErrorType, ErrorPage, neterror: no hits). So load errors use **GeckoView's built-in default error pages, unchanged**. They follow the system light/dark preference (`preferredColorScheme = PreferredColorScheme.System`, `Components.kt:72`).
- The page cover (`PageCover.kt`) lifts after at most 2500ms even if the page never reports a paint, so an error page is never hidden behind a cover.
- Docs: UI_SPEC has no error-page section.

**Screenshot needed:** an unreachable host (e.g. `http://nonexistent.example/`) loaded in Koan in Light and Dark, to show the GeckoView default page inside Koan's page view with the dock.

---

### Pull to refresh

**Purpose:** pull down on a page at its top to reload; a small circle under the top edge shows the pull and then a brand-gradient spinner.

**Anatomy** (`UI/browser/PullRefresh.kt:136-173`): a 40dp circle (T:1080) with a 4dp shadow (T:1083), `Dock` fill (Light #FFFFFF, Dark #252527), aligned top centre of the page (`BrowserScreen.kt:248`).
- Pulling: an arc in a 20dp box (T:1081), stroke 2.5dp (T:1082), round caps, starting at 12 o'clock, sweep = 300° × f, f = pull / trigger clamped 0..1; colour `TextSecondary` until f reaches 1, then `TextPrimary`.
- Refreshing: `BrandSpinner` 20dp (stroke 2dp, 270° sweep, 900ms/turn).

**Motion and thresholds**
- Starts only when the page is at its top, not fullscreen, the page does not keep the touch (maps, canvases, own pull), after the system touch slop with a downward, more-vertical-than-horizontal start (`PullRefresh.kt:81-133`, `BrowserScreen.kt:213-221`). A second finger or the page scrolling cancels it.
- The indicator moves 0.5 × the finger's travel (`RESISTANCE`, T:1090), max 120dp (T:1087).
- Layer: translationY = pull − 40dp (comes down from above the edge), alpha = pull / (0.6 × trigger) clamped, rotation = pull / trigger × 180° while pulling, 0 while refreshing (`PullRefresh.kt:141-146`).
- Trigger: 72dp of indicator travel (T:1086). Crossing it gives a `CLOCK_TICK` haptic (`BrowserScreen.kt:220`).
- Release past the trigger: reload; the indicator springs back to the trigger position and spins. Released short: springs to 0. Both springs: `Motion.DockSettle` dampingRatio 1.0, stiffness 800 (T:1214).
- Ends when the reload has loaded, spinning at least 600ms (T:1096); if loading never starts, it lets go after 3000ms (T:1093) (`BrowserScreen.kt:192-200`). Also ends when another tab comes up.

**States:** idle (hidden), pulling (grey arc growing), armed (arc in TextPrimary), refreshing (brand spinner), settling back.

**Light vs dark:** circle `Dock`, arc `TextSecondary`/`TextPrimary` per theme; spinner colours identical.

**Screenshot needed:** (1) mid-pull below the trigger (grey partial arc) on example.com; (2) armed (full-colour arc); (3) refreshing with the brand spinner. Light and Dark.

---

### Code vs docs/UI_SPEC.md disagreements (code wins)

- Card side margin: spec 12dp, code 14dp (T:324).
- Swipe-close threshold: spec "past 100dp", code 35% of card width or > 800dp/s fling (T:402, T:405).
- Open/close spring: spec damping ≈ 0.85, stiffness ≈ 380 with a tiny overshoot; code `Motion.Overview` damping 1.0, stiffness 600, no overshoot (T:1207).
- Card title: spec "Visual tokens" 16sp semibold; code 15sp Medium (T:427), which matches the spec's "Visual restraint" section.
- Header: spec "tap space name → space menu"; no such action in `SpaceHeader`.
- Header strip dim: spec "dimmed to 80% at most"; code caps it at 0.2 before the theme gain, so Light can reach 0.25 (DepthCurve.kt:44, TabCard.kt:212).


## 3.C sheets, search, menus, toasts, prompts, kit

Source: the Koan code on branch `dev` (read 2026-11-09). Every value below comes from the code; the
reference after each value is `path:line` relative to the repo root. Where `docs/UI_SPEC.md` and the
code disagree, the code is documented and the difference is noted. A bare `:line` (for example `(:129)`) refers to the last full path named before it in the same section.

### Shared conventions

- **Typography:** no custom font. `KoanTheme` sets only a colour scheme, no `Typography`
  (app/src/main/java/dev/nerzouille/koan/ui/theme/Theme.kt:17-40), and there is no `res/font`. Text uses the platform default sans
  (Roboto on stock Android, the OEM font on HyperOS). Weights used: Normal, Medium, SemiBold, Bold.
  (UI_SPEC "Visual tokens" suggests Inter / Google Sans Flex: not implemented.)
- **Pressed state:** every tappable item uses plain `Modifier.clickable` under `MaterialTheme`, so
  the pressed feedback is the default Material 3 ripple, clipped to the item's shape. There is no
  custom pressed colour or scale anywhere in these components. The toggle is the exception: it has
  `indication = null` (no ripple) (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToggle.kt:38-47).
- **"interactive" flag:** sheet pages and cards pass `interactive` (true only for the page on top
  of an open sheet/card). When false, rows simply stop being clickable; they look the same.
- **Shapes:** "pill" = `CircleShape` (fully rounded ends).

### Colour tokens used in this file (KoanPalette)

| Token | Light | Dark | Ref (light / dark) |
|---|---|---|---|
| Base | `#F5F5F7` | `#1C1C1E` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:95 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:141 |
| Glass | `#EBEBEF` | `#FFFFFF` @ 8% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:96 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:142 |
| TextPrimary | `#1C1C1E` | `#FFFFFF` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:97 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:143 |
| TextSecondary | `#6E6E73` | `#FFFFFF` @ 62% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:98 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:144 |
| TextDisabled | `#000000` @ 22% | `#FFFFFF` @ 19% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:99 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:145 |
| Icon | `#6E6E73` | `#98989D` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:100 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:146 |
| Card | `#FFFFFF` | `#2C2C2E` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:101 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:147 |
| CardBorder | `#E4E4EA` | `#FFFFFF` @ 8% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:103 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:149 |
| Primary | `#1C1C1E` | `#FFFFFF` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:107 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:153 |
| OnPrimary | `#FFFFFF` | `#1C1C1E` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:108 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:154 |
| Destructive | `#E5342B` | `#FF5A50` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:109 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:155 |
| GroupedRow | `#F2F2F5` | `#FFFFFF` @ 7% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:110 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:156 |
| ToggleOff | `#E5E5EA` | `#3A3A3C` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:111 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:157 |
| FocusRing | `#B4B4BA` | `#FFFFFF` @ 30% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:112 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:158 |
| Dock (sheet and card surface) | `#FFFFFF` | `#252527` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:113 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:159 |
| DrawerHandle | `#000000` @ 16% | `#FFFFFF` @ 20% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:115 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:161 |
| BarField | `#F1F1F4` | `#FFFFFF` @ 8% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:116 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:162 |
| SegmentPill | `#FFFFFF` | `#4A4A4D` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:117 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:163 |
| FieldHint | `#8E8E93` | `#FFFFFF` @ 60% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:118 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:164 |
| IconBacking (behind dark favicons) | unspecified (never) | `#E9E9EE` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:120 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:166 |
| Attention (amber, "not secure") | `#C0841A` | `#E8B45A` | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:122 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:168 |
| SheetScrim | `#1C1C28` @ 30% | `#000000` @ 50% | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:126 / app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:172 |

Brand gradient (`BrandTokens.Colors`): Pink `#FF5FA2` → Violet `#B46BFF` → Blue `#4F8DFF`
(app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:242-245). `BrandHorizontal` = left-to-right horizontal gradient of those three
(app/src/main/java/dev/nerzouille/koan/ui/theme/Brand.kt:30). `brandOutline` = a 1.5dp border with that gradient (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:253,
app/src/main/java/dev/nerzouille/koan/ui/theme/Brand.kt:33-34).

### Springs used in this file (Motion / tokens)

| Name | dampingRatio | stiffness | Used for | Ref |
|---|---|---|---|---|
| Motion.Drawer | 0.9 | 420 | every sheet/card height (open, close, detent snap) | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1182 |
| Motion.SheetPush | 1.0 | 520 | a page pushed into / popped from a sheet | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1195 |
| Motion.Toast | 1.0 | 700 | toast rise / sink / swipe | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1198 |
| Motion.Sheet | 1.0 | 380 | full-screen pages (ListPage: History, Downloads) sliding up | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1201 |
| Motion.SearchScrim | 1.0 | 700 | the search sheet's dim | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1204 |
| ToggleTokens.Spring | 0.72 | 520 | toggle knob | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:266 |
| SearchFieldTokens.Flow | 1.0 | 140 | "@" gradient flowing into the field ring | app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:300 |

---

### Sheet container (KoanSheet, KoanCard, SheetState, SheetKit)

**Purpose:** one detent engine for every sheet: the over-page sheet (the ⋯ menu) and the small
floating card (prompts, confirmations, link menu); the pins drawer uses the same engine.

There are two containers built on `SheetState` (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:30-190):

#### A. Over-page sheet: `KoanSheet` (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:120-204)

**Anatomy**
1. Scrim: full-screen rect, colour `SheetScrim`, alpha = `state.progress` (0 closed → 1 at the
   compact height, stays 1 above) (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:151, app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:63).
2. Sheet surface: full width, colour `Dock`, top corners 32dp (`SheetTokens.TopRadius`,
   app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:826; clip at app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:177), no bottom corners (it runs to the screen edge),
   no shadow, no blur (UI_SPEC agrees: "no blur, the page is a live surface").
3. Handle: 36 × 4dp pill (`BarTokens.HandleWidth/HandleHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:691-692), colour
   `DrawerHandle`, 8dp from the top (`SheetTokens.HandleTop`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:827), centred
   (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:206-215, placed at :200). Decorative: no tap action on the
   over-page sheet's handle.
4. Pages: each page is a column on `Dock`, with 22dp top padding for the handle zone
   (`SheetTokens.HandleZone`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:828; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:162) and the navigation-bar inset
   at the bottom (:163). Page content padding is the page's own (menu: 14dp sides and bottom,
   10dp gaps; `SheetTokens.Padding/Gap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:829-830).

**Sizes / detents**
- Compact: the content's height, capped at 55% of the window height
  (`COMPACT_MAX_FRACTION = 0.55`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:811; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:130).
- Full: the content's height, capped at window − status-bar inset − 12dp
  (`FullTopGap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:814; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:131). Only exists if it is at least 48dp
  taller than compact (`MinDetentStep`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:817; app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:72).
- A pushed page is measured at least as tall as the root page, so the sheet never shrinks when a
  page is pushed (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:182-186).

**States:** Closed (not composed at all), Compact, Full (`Detent`, app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:19);
plus "dragging" (follows the finger 1:1) and "pushed page" (depth > 0).

**Motion and gestures** (`sheetGestures`, app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:285-332; engine
app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt)
- Opening: height springs from 0 to the compact height with `Motion.Drawer` (0.9 / 420)
  (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:142-150). The sheet is a `translationY` of the whole layer
  (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:175): no relayout per frame.
- A drag starts after the system touch slop (`viewConfiguration.touchSlop`,
  app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:299). A drag that is more horizontal than vertical is left to the
  content (:308).
- Drag up grows the sheet to full, then scrolls the list; drag down scrolls the list back to its
  top, then shrinks the sheet (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:91-104). The list only scrolls at full.
- Release: velocity beyond 600dp/s (`SheetTokens.FlingVelocity`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:823) goes to the next detent
  in that direction (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:122-123). Otherwise, between two detents, it commits
  to the next one past 25% of the gap when leaving the detent it started from, and needs 75% when
  coming back (`COMMIT_FRACTION = 0.25`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:820; app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt:124-134). A fling on a
  scrolled full list keeps scrolling with spline decay (:114-119, :162-170).
- Landing on a different detent than the one the drag started from: haptic `CLOCK_TICK`
  (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:321).
- Tap above the sheet (no drag): closes it (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:304-305). Back pops a pushed
  page, then closes (:139).
- Pushed pages (`SheetNav`, app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:65-108): the new page slides in from the right
  edge (`translationX = width × f`), the covered page trails at 30% (`PUSH_PARALLAX = 0.3`,
  app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:833; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:195); pages more than one step away are alpha 0 (:196). Spring
  `Motion.SheetPush` (1.0 / 520) (:105). Closing resets to the root page (:136).

**Light vs dark:** surface `#FFFFFF` vs `#252527`; scrim `#1C1C28` @ 30% vs black @ 50%; handle
black @ 16% vs white @ 20%.

**Screenshot needed:** the ⋯ menu open at the compact detent over a page of `example.com`,
light and dark; a second shot dragged to full (if the content is taller than compact).

#### B. Floating card: `KoanCard` (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:222-277)

**Anatomy**
1. Scrim: `SheetScrim` × progress, as above (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:247).
2. Card: `Dock` surface, all corners 28dp (`SheetTokens.CardRadius`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:837; :255), 20dp inner
   padding on all sides (`CardPadding`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:838; :257), 10dp side margins and 10dp bottom margin
   (`CardMargin`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:836; :238, :262). No handle, no shadow, never the brand gradient.
3. Content: the caller's column (no automatic spacing; each prompt sets its own).

**Sizes:** width = screen − 2 × 10dp; height = its content (one open detent: the caps are set to
the window so there is no full detent, app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:227-229). Bottom edge sits 10dp
above the larger of the navigation bar and the keyboard, so a card with a text field rises above
the keyboard without recomposing (:243).

**States:** closed (not composed) / open (Compact). `interactive` = open.

**Motion and gestures:** rises from below the screen edge: `translationY = (1 − shown) ×
(card height + bottom gap)` where shown = height / card height (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt:270-273),
driven by `Motion.Drawer` (0.9 / 420). Same `sheetGestures` as the sheet: drag down to close
(25% commit, 600dp/s fling), tap outside closes, Back closes (:233). Closing any of these ways is
treated as "Cancel" / "Block this time" by the prompts (e.g. app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:453, :476,
app/src/main/java/dev/nerzouille/koan/ui/menu/PagePrompts.kt:172-182).

**Light vs dark:** card `#FFFFFF` vs `#252527` over the scrim.

**Screenshot needed:** a permission card ("example.com wants to use your location") over a page,
light and dark; one card with a text field and the keyboard up (the JS prompt) to show it riding
above the keyboard.

#### C. Sheet building blocks (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt)

| Part | Spec | Ref |
|---|---|---|
| `SheetGroup` | full-width column, `GroupedRow` fill, 18dp corners | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:40-49, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:841 |
| `SheetDivider` | 1dp `CardBorder` hairline, inset 54dp from the left (12 + 30 + 12, aligned with titles) | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:52-61, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:844-851 |
| `SheetRow` | height 50dp, 58dp with a subtitle; 12dp side padding; icon box 30dp, 9dp corners, `Dock` fill (white tile on the grey group in light; `#252527` on white 7% in dark), glyph 18dp `TextPrimary`; 12dp gap; title 16sp Medium `TextPrimary` (or a caller colour), 1 line ellipsis; subtitle 12sp `TextSecondary`; trailing slot | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:67-119, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:842-850 |
| `SheetRow` disabled (`enabled = false`) | icon and title `TextDisabled`, trailing replaced by "Soon" (14sp `TextDisabled`), not clickable | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:96, :104, :117, :134-137 |
| `SheetRow` without `onClick` | not tappable, no ripple | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:84 |
| `RowValue` | trailing text 14sp `TextSecondary` | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:122-125 |
| `RowChevron` | ChevronRight 18dp `TextSecondary` | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:128-131 |
| `SheetTile` | weight 1 in a row (8dp gaps), height 66dp, 16dp corners, `GroupedRow`; glyph 22dp over a 13sp label, 4dp apart; disabled = `TextDisabled` tint and not clickable | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:140-158, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:852-857 |
| `SheetHeader` (pushed page) | row 52dp tall; back button 52dp circle with Back glyph 22dp `TextPrimary`; title 17sp SemiBold `TextPrimary` | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:161-180, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:862-863 |
| `SheetSectionTitle` | 12sp Medium `TextSecondary`, 12dp start padding, 5dp top padding | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt:183-192 |

---

### Search sheet (SearchSheet, SearchField, SearchState)

**Purpose:** while the address field is being edited, show results above the field (best match
nearest the thumb), or the "@" engine/scope picker; the page dims behind.

**Anatomy** (app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt)
1. Dim: full-screen `SheetScrim`, alpha animated 0 ↔ 1 with `Motion.SearchScrim` (1.0 / 700)
   (app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt:96-103). Tap on the dim = cancel editing (:104).
2. Results column, anchored to the bottom, above the dock: bottom padding = dock zone + 8dp
   (`GroupGap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1056), 10dp side margins (`SearchTokens.Margin`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1055); max height = screen −
   status bar − 16dp (`StatusGap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1075) − dock zone − 8dp (:129); scrolls in reverse (starts on
   the bottom) (:146); groups 8dp apart, bottom-aligned (:149). The dock zone = 74dp address-row
   zone (68 + 6, app/src/main/java/dev/nerzouille/koan/ui/bar/KoanBar.kt:65-70, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:686-687) + navigation bar / keyboard inset
   (app/src/main/java/dev/nerzouille/koan/ui/KoanRoot.kt:859).
3. Groups, top → bottom (furthest → closest to the thumb) (app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt:151-193):
   - "History" / "Bookmarks" / "History & bookmarks" (label picked by the sources, :154-158),
     Reload glyph;
   - "Switch to tab" (open tabs of every space except the current tab), Essentials glyph (:161);
   - the engine's suggestions: label = engine name with its favicon, then a horizontally scrolling
     row of chips (:162-177);
   - for an "@engine query": one row "Search Wikipedia for “query”" with the engine's favicon
     (:178-193).
4. Group card (`Group`, :197-221): `Card` fill, 18dp corners (`SearchTokens.GroupRadius`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1057);
   label row padding 12dp start / 12dp top / 4dp bottom; optional glyph 14dp `TextSecondary` or a
   14dp favicon, 4dp gap; label 12sp Medium `TextSecondary` (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1058-1059, :1068).
5. Divider between rows: 1dp (`ChipGap / 8` = 8 / 8) `CardBorder`, inset 50dp (12 × 2 + 26)
   (:223-232).
6. Hit row (`HitRow`, :235-270): height 54dp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1060), 12dp side padding, favicon 26dp
   (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1062), 12dp gap; title 15sp `TextPrimary` (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1063) (falls back to the host), address 12sp
   `TextSecondary` (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1064), each 1 line with ellipsis. An open tab in another space adds a space
   chip at the end: pill, `GroupedRow`, padding 8dp × 4dp, space icon 14dp (`SpaceIcon`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1069),
   4dp gap, space name 12sp `TextPrimary`.
7. Suggestion chip (`Chip`, :272-290): height 34dp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1065), pill, `GroupedRow`, 12dp side
   padding; Search glyph 14dp `TextSecondary`, 4dp gap; text 14sp Medium `TextPrimary`
   (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1066). Chip row: 12dp start/end/bottom padding, 8dp between chips (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1067).
8. Favicons (`Favicon`, app/src/main/java/dev/nerzouille/koan/ui/overview/Favicon.kt:84-130): rounded square, corners 28% of the
   size; a letter tile until the icon loads, then a fade; in dark, icons too dark to read get a
   `#E9E9EE` tile behind them (`IconBacking`).

**"@" picker** (`Picker`, app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt:292-349), shown while the text is "@" or
"@" + a partial key with no space yet, and something matches (:130-136):
- Panel: full width (no side margins), top corners 28dp (`PickerRadius`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1071), `Dock` fill,
  16dp padding (`PickerPadding`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1072), children 8dp apart; sits directly on the dock (bottom
  padding = dock zone, no gap) with the same max height. It has no scroll modifier.
- "Search with" label, 12sp Medium `TextSecondary`; engine tiles two per row (an odd last tile
  keeps half width), 8dp gap: height 58dp (`TileHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1070), 18dp corners, `GroupedRow`,
  12dp side padding, favicon 26dp, 12dp gap, name 15sp Medium `TextPrimary`, then "@key" 12sp
  `TextSecondary`, with " · default" on the default engine (:308-337).
- "Search in" label (8dp extra top padding), then scope chips (same `Chip`): Tabs (Essentials
  glyph), History (Reload glyph), Bookmarks (Star glyph) (:338-347).
- Matching: what follows "@" must start the key or the name, case-insensitive; empty matches all
  (:352-353). Tapping a tile or chip puts "@key " in the field.
- Built-in engine shortcuts: `@ddg` DuckDuckGo, `@g` Google, `@wiki` Wikipedia (device language),
  `@yt` YouTube, `@bd` Baidu, then each custom engine that has a shortcut
  (app/src/main/java/dev/nerzouille/koan/search/SearchModel.kt:27-49).
  Scopes: `@tabs`, `@history`, `@bookmarks` (app/src/main/java/dev/nerzouille/koan/search/SearchModel.kt:57-61).

**The field itself** (state in `SearchField`, drawn by the dock's address row
app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt:448-520 and the ring app/src/main/java/dev/nerzouille/koan/ui/bar/SearchRing.kt):
- Pill 50dp tall (`BarTokens.FieldHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:695), `BarField` fill, search glyph 18dp `FieldHint`
  with a 10dp gap (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:298-299), text 17sp `TextPrimary` (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:696), hint "Search or type URL" in
  `FieldHint` (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt:118, :461-463).
- Editing starts with the page's address fully selected (app/src/main/java/dev/nerzouille/koan/ui/search/SearchField.kt:32-37).
- Inline completion: what was typed stays, the rest of the top hit's address follows selected
  (:67-72); the completed page's title follows in `FieldHint` (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt:510-513).
- Text scrolls to keep the cursor 40dp inside the edges (`CursorEdge`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:280).
- Ring, focused: neutral 1.5dp `FocusRing` stroke (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:282). "@" first: the brand gradient
  (a sweep gradient around the field centre) flows in from the left along both halves of the pill
  with spring `Flow` (1.0 / 140) (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt:432), stroke grows to 2dp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:283), a
  faint 1.5dp halo at 14% (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:286-287), the neutral ring fades out as `1 − f²`
  (app/src/main/java/dev/nerzouille/koan/ui/bar/SearchRing.kt:63). In "@" mode the gradient turns at 48°/s plus two wobbles
  (±14° over 3.1 s, ±10° over 4.7 s) (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:293-297, app/src/main/java/dev/nerzouille/koan/ui/bar/SearchRing.kt:81-86); not at all when
  the system animator scale is 0 (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt:439). The cursor turns into the brand
  gradient in "@" mode (:503).

**Timing (SearchState):** local results 40ms after typing pauses, suggestions 180ms, at most 40
rows per source (app/src/main/java/dev/nerzouille/koan/ui/search/SearchState.kt:117-119). Suggestions come from the default engine,
or from the shortcut's engine for "@yt query"; none for a scope (:99-104). (The class comment at
:47 still says "DuckDuckGo's suggestions": outdated, the code uses the chosen engine.)

**States:** closed (not composed once the dim reached 0); open with nothing to show yet (dim only);
results; "@" picker; "@engine query" (suggestions from that engine + the "Search … for" row).
Rows: default / pressed (ripple). No selected or disabled row states.

**Light vs dark:** groups `#FFFFFF` vs `#2C2C2E` (`Card`); chips and space chips `#F2F2F5` vs white
@ 7%; picker panel `#FFFFFF` vs `#252527` (`Dock`); dim `#1C1C28` @ 30% vs black @ 50%; neutral
ring `#B4B4BA` vs white @ 30%.

**Screenshots needed:** (1) typing "exa" on a page: history group + switch-to-tab group (fake
tabs, `example.com`, `example.org`) + suggestion chips, inline completion visible in the field;
(2) "@" typed: the picker with the gradient ring; (3) "@wiki koan": suggestion chips from
Wikipedia + "Search Wikipedia for “koan”". Each in light and dark.

---

### Browser menu (BrowserMenu: MenuRoot)

**Purpose:** the ⋯ menu: page navigation, quick actions, page options, links to History /
Downloads / Settings, and the content blocker. Lives in a `KoanSheet` (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:303).

**Anatomy** (app/src/main/java/dev/nerzouille/koan/ui/menu/BrowserMenu.kt:96-155), column with 14dp side and bottom padding, 10dp
between blocks:
1. Nav row (`NavRow`, :158-201), 56dp tall (`NavRowHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:858): Back, Forward icon buttons;
   the address pill; Copy link; Reload (or Close = stop while loading).
   - Icon button (`NavIcon`, :203-214): 48dp circle touch (`BarTokens.IconTouch`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:700), glyph
     24dp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:701), tint `Icon`, `TextDisabled` when unavailable (and not clickable).
   - Address pill: weight 1, 44dp tall (`NavFieldHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:859), pill, `BarField`, 12dp side
     padding, centred; lock glyph 14dp `TextSecondary` for https (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:861) + 8dp gap; host 16sp
     Medium `TextPrimary` (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:860), or "New tab" in `FieldHint` when there is no page. Tap = edit
     the address.
2. Quick tiles row, 8dp gaps (`TileGap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:854): New tab (Plus), Bookmark / Bookmarked (Star /
   StarFilled), Share, Find (:108-117). Tiles = `SheetTile`; all but New tab disabled without a page.
3. Group of rows (:118-152): Site settings (Lock; subtitle "Secure" / "Not secure"; chevron) ·
   Desktop site (toggle) · Reader view / Leave reader view (value "On" / "Available" /
   "Not available") · Essentials row (Add to / Remove from Essentials; "In Essentials" + "From
   Zen"; "Add to Essentials" + "No page") · optional "Add this search engine" (subtitle = engine
   name, chevron) · History · Downloads (value "N active" while downloading, else chevron) ·
   Settings.
4. uBlock card (`UBlockCard`, app/src/main/java/dev/nerzouille/koan/ui/menu/UBlockPage.kt:38-54), its own `SheetGroup` with one row:
   Shield glyph, "uBlock Origin", subtitle "N requests blocked on this page" / "1 request blocked
   on this page" / "Off", trailing toggle; tap (when on) pushes the uBlock page. Not installed:
   subtitle "Blocks ads and trackers · Install", tap installs.

**States:** no page (address "New tab" in hint colour, tiles disabled, Site settings without
subtitle or chevron); loading (stop ✕ instead of reload); bookmarked; reader available / active;
active downloads. Rows: default / pressed ripple / disabled.

**Motion:** the sheet's (see Sheet container). Most rows close the menu, then act
(app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:314-367).

**Light vs dark:** sheet `#FFFFFF` vs `#252527`; groups and tiles `#F2F2F5` vs white @ 7%; row icon
boxes `#FFFFFF` vs `#252527`; address pill `#F1F1F4` vs white @ 8%.

**Screenshot needed:** the menu at compact on `https://example.com` (lock shown), uBlock card
peeking at the bottom; then full; then the same over a new blank tab (disabled tiles). Light and dark.

#### Site settings page (app/src/main/java/dev/nerzouille/koan/ui/menu/SiteSettings.kt:53-133)

Pushed in the menu sheet. Column 14dp side/bottom padding, 10dp gaps.
- Header row 62dp tall (52 + 10): back button 52dp circle, Back glyph 22dp; favicon 40dp
  (30 + 10); 12dp gap; host 17sp SemiBold `TextPrimary`; under it a 14dp lock + 4dp gap +
  "Connection is secure" 12sp `TextSecondary`, or "Connection is not secure" with lock and text in
  `Attention` amber (`#C0841A` light / `#E8B45A` dark) (:64-88).
- "Permissions" section title; group with one row per decided permission: glyph, label
  (Location, Camera, Microphone, Camera and microphone, Storage, Protected content, Cross-site
  cookies, Local network, Virtual reality; `app/src/main/java/dev/nerzouille/koan/engine/SitePermissions.kt:21-31`),
  subtitle "Allowed" / "Blocked", trailing toggle (:90-103). Loading: a "…" row; empty: "Nothing
  allowed or blocked yet" / "Koan asks the first time a site wants something".
- Group with "Reader view" (subtitle "Simplified, distraction-free view" or "Not available on this
  page"; toggle only when available) (:105-113).
- "Cookies and site data" caption 12sp `TextSecondary`, centred, 10dp top padding; then a
  full-width 50dp pill on `GroupedRow` with "Clear site data" 16sp Medium `Destructive`
  (:115-131). It opens a `Confirm` card "Clear data for example.com?" (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:396-415).

**Screenshot needed:** Site settings for `example.com` with two fake decisions (Location allowed,
Camera blocked), light and dark; plus the not-secure header (amber) on an `http://` page.

#### uBlock page (app/src/main/java/dev/nerzouille/koan/ui/menu/UBlockPage.kt:60-89)

Pushed in the menu sheet: `SheetHeader` "uBlock Origin", then a 520dp tall box
(`UBlockPanelHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:870), 18dp corners, `GroupedRow`, holding uBlock's own popup in an engine
view; "…" in `TextSecondary` until it arrives. The panel's own look is uBlock's, not Koan's.

**Screenshot needed:** the uBlock page pushed over the menu on `example.com` (shows uBlock's panel).

#### Link context menu (LinkMenu) (app/src/main/java/dev/nerzouille/koan/ui/menu/LinkMenu.kt:76-148)

**Purpose:** long press on a link or image: what to do with it. On a `KoanCard`, with a haptic
`CLOCK_TICK` as it opens (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:587).

**Anatomy** (column, 10dp gaps):
1. Image preview (images only): full width, 140dp tall (`LinkImageHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:867), 18dp corners,
   `GroupedRow` placeholder from the first frame; the image (crop) fades in over 180ms tween
   (`IMAGE_FADE_MS`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:871) (:128-148). Downloads at most 4 MB, decoded to ≤ 800px (:177-179).
2. Header: 6dp side padding; link favicon 30dp + 12dp gap (links only); title 16sp SemiBold
   `TextPrimary` (link text, else the host), address 12sp `TextSecondary` up to 2 lines (:84-100).
3. Link group: Open in new tab (Plus) · Open in background (Essentials) · Copy link (Link) ·
   Share link (Share) (:101-111).
4. Image group: Save image (Download) · Open image in new tab (Plus) · Copy image link (Link)
   (:112-120). An image that is also a link shows both groups.

**States:** link / image / image-link; rows default / pressed.

**Screenshot needed:** long press on a link on `example.com` (link only), and on an image (preview +
both groups for an image link). Light and dark.

---

### Toast (KoanToast)

**Purpose:** the one quiet bottom confirmation ("Tab closed · Undo", "Removed from history · Undo",
"example.pdf downloaded · Open"), at most one action.

**Anatomy** (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToast.kt:96-209)
1. Container: full width minus 12dp margins each side (`ToastTokens.Margin`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:965); height 50dp,
   60dp with a detail line (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:967, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:971); 16dp corners (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:968); fill `Primary`
   (`#1C1C1E` in light, `#FFFFFF` in dark: an inverted pill); no shadow.
2. Text column, 18dp start padding (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:969): message 15sp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:970) `OnPrimary`, Normal weight,
   SemiBold when there is a detail line; detail 12sp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:972) `OnPrimary` @ 65% (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:973); 1 line
   each, ellipsis (:179-191).
3. Action (optional): full-height tap area with 18dp side padding, label 15sp SemiBold `OnPrimary`
   (:192-206). Tap = the toast goes away, then the action runs.
4. "Moment" variant (`moment = true`, e.g. a finished download): a 1.5dp brand-gradient outline
   around the pill (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToast.kt:176, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:253).

**Position:** bottom-centre, its bottom edge 10dp (`Gap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:966) above the dock zone: 74dp on a
page, 134dp in the overview (60 + 68 + 6) (app/src/main/java/dev/nerzouille/koan/ui/bar/KoanBar.kt:65-70, app/src/main/java/dev/nerzouille/koan/ui/KoanRoot.kt:1004),
plus the navigation bar or keyboard inset (above the keyboard while it is up) (:123-124).

**Timing:** shown for 3000ms (`SHOW_MS`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:974), then sinks away (:107-115). A new toast replaces
the current one and restarts the timer (:84-88). Any sheet, card, full page, the drawer or sign-in
opening dismisses it at once (app/src/main/java/dev/nerzouille/koan/ui/KoanRoot.kt:248-249, app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:174-177). The
"copied" toast only exists below Android 13 (the system confirms on 13+) (:66-67).

**Motion and gestures**
- In / out: `shown` 0 ↔ 1 with `Motion.Toast` (1.0 / 700): alpha = shown, rises 16dp
  (`Rise`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:981) (:129-133).
- Swipe sideways: follows the finger, alpha falls with |dx| / width; released past 30% of its width
  (`SWIPE_FRACTION`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:977) or faster than 800dp/s (`SwipeFling`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:978) it flies off to ±width
  with `Motion.Toast` (:144-148).
- Swipe down (up is clamped to 0): past 60% of its height (2 × SWIPE_FRACTION) or faster than
  800dp/s down = dismissed (:149, :168). Otherwise it springs back (:150-153).

**States:** message only / with action / with detail (tall) / moment (gradient outline). Action:
default / pressed ripple; not clickable while it is leaving (:197).

**Light vs dark:** light = near-black `#1C1C1E` pill with white text; dark = white pill with
`#1C1C1E` text.

**Screenshot needed:** on a page: "Tab closed · Undo" (light and dark); a tall toast with detail +
"Open" and the gradient outline after a fake download (`example.pdf`, "1.2 MB · example.com");
one in the overview to show the higher position.

---

### Prompt cards

All of these sit on the floating card (`KoanCard`, see Sheet container B: `Dock` surface, 28dp
corners, 20dp padding, 10dp margins, scrim). Never the brand gradient. Shared parts:
- **Icon tile** (permission, download): 40dp square (30 + 10), 9dp corners, `GroupedRow`, glyph
  20dp (18 + 2) `TextPrimary` (app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt:64-72).
- **Card title** 18sp SemiBold `TextPrimary` (`CardTitleSize`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:864); **card text** 14sp
  `TextSecondary` (`CardTextSize`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:872).
- **CardButton** (secondary on permission and download cards): height 50dp, pill, `GroupedRow`,
  16sp Medium `TextPrimary`, no side padding (fills its weight) (app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt:88-101).

#### Permission prompt (app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt:55-86)

**Anatomy:** column with 10dp gaps: [icon tile + 12dp gap + title "example.com wants to use your
location" + reason ("It can see where this phone is.") (:43-53)]; 5dp spacer; row of "Block" and
"Always allow" CardButtons (weight 1 each, 8dp gap); full-width PrimaryButton "Allow once".
Glyph per permission: Location, Camera (camera and camera+mic), Mic, Database (storage), Lock
(protected media), Link (cross-site cookies), Globe (local network), Desktop (VR) (:32-41).
**Behaviour:** Block = block and remember; Always allow = allow and remember; Allow once = allow now;
closing the card (tap outside, drag, Back) = blocked this time, nothing remembered
(app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:446-463).
**Screenshot:** location request from `example.com`, light and dark.

#### Download confirmation (app/src/main/java/dev/nerzouille/koan/ui/menu/DownloadPrompt.kt:26-56)

Shown when a page starts a download with no tap just before. Icon tile with the Download glyph;
file name 18sp SemiBold, middle ellipsis; detail "1.2 MB · example.com" 14sp `TextSecondary`
(size omitted when unknown) (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:481-487); 5dp spacer; row: "Cancel" CardButton +
"Download" PrimaryButton, weight 1 each, 8dp gap. Closing the card = Cancel (:476).
**Screenshot:** "example.pdf", "1.2 MB · example.com", light and dark.

#### File prompt (app/src/main/java/dev/nerzouille/koan/ui/menu/FilePrompt.kt:27-89)

No Koan UI: a page's `<input type=file>` opens Android's chooser (files/photos), with the camera
offered when the field takes images or video, straight to the camera for a capture field; asks for
the camera permission first if needed. Nothing picked = the field stays empty.
**Screenshot:** none (system UI only).

#### Login prompts (app/src/main/java/dev/nerzouille/koan/ui/menu/LoginPrompts.kt)

- **Login chips** (`LoginChips`, :52-91), not on a card: a horizontally scrolling row just above
  the keyboard, at the page dock zone (74dp) (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:528-535); padding 10dp × 8dp,
  8dp gaps. First a 34dp circle on `Card` with a Key glyph 18dp (opens the full list); then one
  chip per account: 34dp tall pill on `Card`, 12dp side padding, initial avatar + 8dp gap + masked
  username 14sp `TextPrimary`. Masking keeps half the name (1 to 6 chars) then "••••" and the
  domain: "alex.r••••@example.com" (:42-50).
- **Initial avatar** (:155-167): 26dp circle `GroupedRow`, first letter uppercased 12sp SemiBold.
- **"Sign in to example.com with" card** (`LoginSheet`, :93-128): column 10dp gaps; favicon 40dp +
  12dp gap + title 18sp SemiBold + "Koan passwords" 12sp `TextSecondary`; a `SheetGroup` of 58dp
  rows (avatar, 12dp gap, masked name 16sp, radio); PrimaryButton "Use", QuietButton "Not now".
  The most recently used account is preselected.
  - Radio (:169-181): 22dp circle (26 − 4); selected = `Primary` fill with a 14dp Check in
    `OnPrimary`; unselected = `GroupedRow` fill, which is the same colour as the group behind it
    (nearly invisible: an observation, not a documented intent).
- **Save / update password card** (`SaveLoginCard`, :130-153): favicon 40dp + title "Save the
  password for example.com?" (or "Update the password for example.com?") 18sp SemiBold + masked
  username 12sp; body "Kept encrypted on this phone only; it isn't added to your Mozilla account."
  14sp `TextSecondary`; PrimaryButton "Save in Koan"; QuietButton "Not now".
**Screenshots:** chips over the keyboard on a fake login form (`example.com`, two fake accounts);
the "Sign in with" card; the save card. Light and dark. Never real accounts.

#### Page prompts: alert / confirm / text and the rest (app/src/main/java/dev/nerzouille/koan/ui/menu/PagePrompts.kt)

Building blocks (the card's column has no automatic spacing; each block adds its own):
- `PromptTitle` 18sp SemiBold `TextPrimary`, up to 3 lines (:274-277). Dialog titles say
  "example.com says" (the host, not the engine's long title) (:186-188).
- `PromptMessage`: 6dp spacer (`TextGap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:877), then the text 16sp `TextSecondary`, scrolling
  past 220dp (`MessageMaxHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:881) (:279-287).
- `PromptField`: 10dp spacer; 44dp tall, 12dp corners (`FieldRadius`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:878), `BarField`, 12dp
  side padding; text 16sp `TextPrimary`, hint 16sp `FieldHint`; password fields masked; the JS
  prompt's field is focused with its text selected (:289-324).
- `NoMoreDialogs`: 10dp spacer; row with 12dp corners and 8dp vertical padding: tick box + 12dp
  gap + "Don't let this page show more dialogs" 14sp. Offered when a dialog comes within 3000ms of
  the previous answer (`MANY_DIALOGS_MS`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:888) (:326-342, :157).
- `CheckMark` tick box: 22dp, 6dp corners, 2dp border `TextSecondary` unticked; ticked = `Primary`
  fill + border with a 16dp Check in `OnPrimary` (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:891-893) (:344-357).
- `Buttons`: 20dp spacer; secondary (and third) buttons side by side (weight 1, 8dp gap), 10dp
  spacer, then the full-width PrimaryButton (:359-376). The secondary here is a private
  `SecondaryButton`: 50dp, pill, `GroupedRow`, 16sp **Medium**, 1 line (:378-390) (the kit's
  `SecondaryButton` uses SemiBold; see Buttons).

Variants (:190-249): Alert ("OK"); Confirm (page's own labels, default "OK" / "Cancel", optional
third); Text prompt (label, field, "OK" / "Cancel"); "Leave this page?" / "Changes you made may not
be saved." ("Leave" / "Stay"); HTTP sign-in "Sign in to example.com" with Username and Password
fields, "That didn't work. Check the username and password." after a failure ("Sign in" /
"Cancel"); "example.com wants to open a pop-up" + target host ("Allow" / "Block"); "Send the form
again?" ("Resend" / "Cancel"). Closing the card any other way = cancel (:172-182).

`<select>` list (`ChoiceList`, :408-469): scrolls past 380dp (`ListMaxHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:882), opens two
rows above the selected option. Option row ≥ 50dp, 12dp corners, 12dp start padding (28dp inside an
optgroup, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:885), 16sp; selected (single) = `GroupedRow` fill, SemiBold, Check 18dp at the end;
disabled option `TextDisabled`; optgroup title 12sp SemiBold `TextSecondary` (10dp top, 6dp bottom);
separator 1dp `GroupedRow` with 6dp vertical padding. A tap picks at once; multiple = tick boxes +
"Done" PrimaryButton.

Date / time (`TimeSelection`, :473-572): Material 3 `DatePicker` (no title, no headline, no mode
toggle) and `TimePicker`, recoloured to Koan tokens: container `Dock`, selected day/year and
selector `Primary` with `OnPrimary` content, today border `TextSecondary`, disabled `TextDisabled`,
clock dial `GroupedRow` (:510-559). Date-and-time shows the date first, "Next", then the time.
Buttons "Set" (or "Next") + "Clear". Month (`MonthGrid`, :574-624): "‹ 2026 ›" row (44dp circle
steppers, 18sp; title 18sp SemiBold), 12 month tiles 3 per row, aspect 2:1 (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:897), 12dp corners,
8dp gaps, selected `Primary` / `OnPrimary`, others `GroupedRow`, out of range `TextDisabled`.

Colour (`ColorPicker`, :628-659): "Choose a colour"; 12 swatches (`PromptTokens.Swatches`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:900-903:
`#000000 #8E8E93 #FFFFFF #FF3B30 #FF9500 #FFCC00 #34C759 #00C7BE #007AFF #5856D6 #AF52DE #FF2D55`),
6 per row, circles, 8dp gaps, 1dp `GroupedRow` ring, the chosen one a 3dp `TextPrimary` ring
(app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:905); then a 44dp preview circle + hex field (hint "Hex, like 1a2b3c"); "Set".

**Screenshots needed:** alert "example.com says" with a short message (light, dark); confirm with
OK / Cancel; text prompt with the keyboard up; the many-dialogs tick box; a `<select>` with an
optgroup; date picker; month grid; colour picker. All on a local test page with fake text.

#### Sign-out confirm and other confirmations (app/src/main/java/dev/nerzouille/koan/ui/menu/SignOutConfirm.kt)

`Confirm` (:30-40), column 10dp gaps: title 18sp SemiBold; text 14sp `TextSecondary`; 5dp spacer;
full-width `DestructiveButton` (the action); full-width `QuietButton` "Cancel". Sign-out: "Sign
out?" / "Your Zen Spaces leave this phone until you sign in again. Tabs opened only on this phone
stay." / "Sign out" (:17-28). The same `Confirm` is used for "Clear data for example.com?"
(app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:396-415) and "Clear the download list?" (:766-779).
**Screenshot:** the sign-out card from Settings, light and dark.

---

### Buttons (app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt)

**Purpose:** the four kit button styles. All 50dp tall (`ButtonTokens.Height`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:271), pill
shape, 28dp side padding (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:272), label 16sp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:273), content centred.

| Variant | Fill | Label | Ref |
|---|---|---|---|
| `PrimaryButton` | `Primary` (`#1C1C1E` light / `#FFFFFF` dark) | `OnPrimary`, SemiBold; optional leading slot, 10dp gap (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:274) | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt:21-37 |
| `QuietButton` | none | `TextPrimary`, Medium | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt:39-53 |
| `SecondaryButton` | `GroupedRow` (`#F2F2F5` / white @ 7%), overridable `surface` | `TextPrimary`, SemiBold | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt:55-70 |
| `DestructiveButton` | `GroupedRow` | `Destructive` (`#E5342B` / `#FF5A50`), Medium; never a red fill | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt:72-87 |

Related non-kit buttons: `CardButton` (`GroupedRow`, Medium, no padding;
app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt:88-101), PagePrompts' private secondary (same, 1 line;
app/src/main/java/dev/nerzouille/koan/ui/menu/PagePrompts.kt:378-390), Site settings' "Clear site data" pill (app/src/main/java/dev/nerzouille/koan/ui/menu/SiteSettings.kt:121-131),
ListPage's "Clear" (see ListPage).

**States:** default; pressed = Material 3 ripple clipped to the pill. **No disabled style**: the
kit buttons have no `enabled` parameter; callers ignore taps when not interactive
(e.g. app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt:84). Never the brand gradient (UI_SPEC agrees).

**Light vs dark:** Primary flips black ↔ white; the others only change their fill/label tokens.

**Screenshot needed:** a permission card (Primary + two CardButtons) and the sign-out card
(Destructive + Quiet), light and dark; Primary with a leading icon if one is used on the sign-in
screen.

---

### Toggle (KoanToggle) (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToggle.kt:27-90)

**Purpose:** Koan's switch.

**Anatomy:** track 50 × 30dp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:258-259), fully rounded; knob = white circle, radius 13dp
(height / 2 − 2dp inset, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:260) (:57-58); soft knob shadow: radial gradient black @ 22% from 75%
of the radius to transparent, radius knob + 3dp, offset 1dp down (shadow / 3) (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:261-262,
:74-86). ON = the brand gradient across the full track width, revealed from the left edge up to
the knob's right side + inset, at 90% alpha (`TRACK_ALPHA`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:265) over the OFF track (:62-73).

**States:** off (track `ToggleOff`: `#E5E5EA` light / `#3A3A3C` dark); on (gradient); moving;
read-only (`onCheckedChange = null`: same look, not toggleable). No pressed visual (no ripple), no
disabled look.

**Motion:** knob position springs with `ToggleTokens.Spring` (0.72 / 520) (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:266, :35); the
knob is kept inside the track despite the overshoot (:60-61); the gradient reveal is
`clamp(position × 3)` so it arrives in the first third of the travel (:63). Haptic `CLOCK_TICK`
on every change (:44). Draw-only (no recomposition per frame). Accessibility role Switch.

**Screenshot needed:** Settings › Search suggestions on and Hide status bar off, side by side;
light and dark. A mid-animation frame if possible.

---

### Segmented selector (Segmented) (app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsKit.kt:197-240)

Defined in Settings (`app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsKit.kt`), not in
`ui/kit`. Used for "Start page" (30 min / 1 h / 4 h / Never) and "Theme" (Light / Dark / Follow
system) (app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsScreen.kt:177, :190-193).

**Anatomy:** full-width track 40dp tall (`SegmentHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:932), 12dp corners (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:933), `Glass`
fill (`#EBEBEF` light / white @ 8% dark), 3dp inner padding and 3dp gaps (`SegmentInset`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:934);
equal-width segments; the chosen one sits on a pill with 9dp corners (12 − 3), `SegmentPill` fill
(`#FFFFFF` light / `#4A4A4D` dark) and a 2dp shadow (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:936); label 14sp (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:935), 1 line.

**States:** selected (Bold, `TextPrimary`, pill); unselected (Medium, `TextSecondary`, no pill);
pressed ripple clipped to the segment; not interactive = not clickable.

**Motion:** none. The pill jumps to the new segment (no animation in the code).

**Screenshot needed:** Settings › Display › Theme selector on "Follow system", light and dark.

---

### ListPage scaffolding (app/src/main/java/dev/nerzouille/koan/ui/kit/ListPage.kt)

**Purpose:** the full-screen list page opened from the ⋯ menu (History, Downloads:
app/src/main/java/dev/nerzouille/koan/ui/history/HistoryScreen.kt:96, app/src/main/java/dev/nerzouille/koan/ui/downloads/DownloadsScreen.kt:74).

**Anatomy** (:52-134)
1. Background `Base` + the settings wash: three radial brand spots (pink at 10% / 15%, violet at
   55% / 5%, blue at 95% / 30% of width / wash height), radius 75% of 320dp, alpha 20% light /
   14% dark (app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsKit.kt:51-62, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:945-947). Status bar, navigation bar and
   keyboard insets are padded.
2. Top bar 56dp tall (`SettingsTokens.TopBar`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:911), 20dp side padding (app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:910): back target
   56dp circle with the Back glyph 24dp (20 + 4) `TextPrimary`, aligned to the start; title
   23.8sp (34 × 0.7, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:912) Bold `TextPrimary`; optional trailing slot (:75-87).
3. Content: fills the rest; scrolls itself (:89).
4. Bottom row, in thumb reach and above the keyboard: 20dp side padding, 10dp vertical
   (`DownloadTokens.BottomGap`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1001): search pill 48dp (`SearchHeight`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:1000) on `BarField`,
   20dp side padding, Search glyph 20dp `FieldHint`, 12dp gap, hint/text 16sp; then (if `onClear`)
   an 8dp gap and a 48dp pill on `Card` with "Clear" 16sp Medium `Destructive` (:91-132).
5. `DayCard` (:136-147): full width, 18dp corners (`DownloadTokens.GroupRadius`, app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt:997), `Card`
   fill, 12dp side padding; `dayRowShape` rounds only the first and last rows of a lazily drawn
   day (:149-160). Day labels: "Today", "Yesterday", "Monday 12 October", "12 October 2025" for
   another year (:162-180).

**Motion:** the page slides up from the bottom (`translationY = (1 − slide) × height`) with
`Motion.Sheet` (1.0 / 380) (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt:753, :782); Back closes (:64).

**Light vs dark:** base `#F5F5F7` vs `#1C1C1E`; day cards `#FFFFFF` vs `#2C2C2E`; search pill
`#F1F1F4` vs white @ 8%; wash 20% vs 14%.

**Screenshot needed:** History with fake visits (`example.com`, `example.org`) grouped under Today
/ Yesterday, search empty, light and dark; Downloads with one fake file; keyboard up with a query.

---

### Values that could not be resolved from the code

- The platform `touchSlop` (drag start for sheets and cards) is the system's
  `ViewConfiguration` value, not a Koan token.
- Ripple colour / alpha: Material 3 defaults, not set by Koan.
- Spline fling decay for a full sheet's list (`splineBasedDecay`): platform physics, no Koan value.


## 3.D Pages, sign-in, tour and icons

All paths are relative to `app/src/main/java/dev/nerzouille/koan/` unless they start with `app/` or `docs/`.
Token values are resolved from `ui/theme/Tokens.kt` (abbreviated **T**), `ui/theme/Brand.kt`, `ui/theme/Theme.kt`.

### Shared colour reference (KoanPalette, `ui/theme/Tokens.kt:93-183`)

| Token | Light | Dark |
|---|---|---|
| Base | `#F5F5F7` (T:95) | `#1C1C1E` (T:141) |
| Glass | `#EBEBEF` (T:96) | White @ 0.08 (T:142) |
| TextPrimary | `#1C1C1E` (T:97) | `#FFFFFF` (T:143) |
| TextSecondary | `#6E6E73` (T:98) | White @ 0.62 (T:144) |
| TextDisabled | Black @ 0.22 (T:99) | White @ 0.19 (T:145) |
| Card | `#FFFFFF` (T:101) | `#2C2C2E` (T:147) |
| CardBorder (hairlines) | `#E4E4EA` (T:103) | White @ 0.08 (T:149) |
| Primary | `#1C1C1E` (T:107) | `#FFFFFF` (T:153) |
| OnPrimary | `#FFFFFF` (T:108) | `#1C1C1E` (T:154) |
| Destructive | `#E5342B` (T:109) | `#FF5A50` (T:155) |
| GroupedRow | `#F2F2F5` (T:110) | White @ 0.07 (T:156) |
| ToggleOff | `#E5E5EA` (T:111) | `#3A3A3C` (T:157) |
| Dock | `#FFFFFF` (T:113) | `#252527` (T:159) |
| BarField | `#F1F1F4` (T:116) | White @ 0.08 (T:162) |
| SegmentPill | `#FFFFFF` (T:117) | `#4A4A4D` (T:163) |
| FieldHint | `#8E8E93` (T:118) | White @ 0.60 (T:164) |
| Attention | `#C0841A` (T:122) | `#E8B45A` (T:168) |
| SheetScrim | `#1C1C28` @ 0.30 (T:126) | Black @ 0.50 (T:172) |

Brand colours (`BrandTokens`, T:241-254): Pink `#FF5FA2` (T:242), Violet `#B46BFF` (T:243), Blue `#4F8DFF` (T:244); `Colors = [Pink, Violet, Blue]` (T:245). Same in light and dark.

Typography: no `FontFamily` is set anywhere in `app/src/main/java` and there is no `res/font` folder, so every text uses the platform default sans (Roboto on stock Android; the OEM font on HyperOS). `docs/UI_SPEC.md:392` suggests "Inter or Google Sans Flex if bundled": not done in code.

---

### Settings page frame (`SettingsPageFrame`)

**Purpose.** The shell of every Settings page: back chevron, optional large title, scrolling content, on the base colour with the brand wash at the top.

**Anatomy** (`ui/settings/SettingsKit.kt:68-101`)
1. Page background: `Base` + `settingsWash` (`ui/settings/SettingsScreen.kt:126-127`; pushed pages repeat it, `:141`).
2. Back button: a 56dp circle-clipped box (`SettingsTokens.TopBar` = 56.dp, T:911), glyph `KoanIcons.Back` aligned centre-start, tint `TextPrimary`, glyph size `RowGlyph + Divider*4` = 20 + 4 = **24dp** (`SettingsKit.kt:80-88`).
3. Title (optional; About has none): `TitleSize` **34sp** Bold, `TextPrimary`, bottom padding `TitleBottom` **12dp** (T:912-913, `SettingsKit.kt:89-97`).
4. Content column, then a trailing spacer `SectionTop` **22dp** (`SettingsKit.kt:99`).
- Horizontal page padding `SettingsTokens.Padding` **20dp** (T:910, `SettingsKit.kt:78`). Insets: `imePadding`, `topSafePadding`, `navigationBarsPadding` (`:74-77`). The whole page scrolls (`verticalScroll`, `:75`).

**Settings wash** (`settingsWash`, `SettingsKit.kt:51-62`): three radial gradients, each `color @ a → Transparent`, radius `h*0.75`, where `h = WashHeight` **320dp** (T:945):
| Spot | Colour | Centre (x, y) |
|---|---|---|
| 1 | Brand Pink `#FF5FA2` | (0.10·width, 0.15·h) |
| 2 | Brand Violet `#B46BFF` | (0.55·width, 0.05·h) |
| 3 | Brand Blue `#4F8DFF` | (0.95·width, 0.30·h) |
Alpha `a`: light **0.20** (`WASH_ALPHA_LIGHT`, T:946), dark **0.14** (`WASH_ALPHA_DARK`, T:947).

**States.** `interactive` false (a page under the top one, or sliding): the back button and every row lose their click handler (`SettingsKit.kt:84`, `:144`).

**Motion.**
- Settings opens/closes as a full-screen slide driven by `Motion.Sheet` = spring(dampingRatio 1.0, stiffness 380) (`ui/KoanOverlays.kt:656`, T:1201).
- A pushed page slides in from the right: `translationX = width·f` for the incoming page; the page under it trails at `f·PUSH_PARALLAX` (`PUSH_PARALLAX` = **0.3**, T:833); pages more than one step away get alpha 0 (`SettingsScreen.kt:136-140`). Driven by `SheetNav` with `Motion.SheetPush` = spring(dampingRatio 1.0, stiffness 520) (`ui/sheet/KoanSheet.kt:105`, T:1195). System Back pops a page, then closes (`SettingsScreen.kt:122`).

**Light vs dark.** Only through palette tokens and the wash alpha (0.20 vs 0.14).

**Screenshot.** Settings root, scrolled to the top, light and dark, signed in with a fake account (shows the wash, back chevron, 34sp "Settings" title and the account card).

### Settings section header (`SettingsSection`)

**Purpose.** Small grey title above a group of settings rows (also used as day headers in History and Downloads).

**Anatomy / sizes** (`SettingsKit.kt:103-112`): text `SectionTitleSize` **13sp** SemiBold, colour `TextSecondary`; padding top `SectionTop` **22dp** (T:914), bottom `Divider*4` = **4dp**. No icon, no uppercase transform (text as given: "General", "Display", "Privacy", "Koan", "Developer"; `SettingsScreen.kt:167,188,202,221,229`).

**States.** Static.

**Screenshot.** Settings root showing the "General" header and its first rows.

### Settings divider (`SettingsDivider`)

Full-width **1dp** hairline (`SettingsTokens.Divider`, T:927), colour `CardBorder` (light `#E4E4EA`, dark White @ 0.08) (`SettingsKit.kt:114-122`). Spans the full content width (no indent), unlike the sheet divider.

### Settings row (`SettingsRow`) — all variants

**Purpose.** The one row type of Settings: glyph, title (+ subtitle), then an optional value, trailing slot (toggle, check, "Edit"/"Copy" action) and/or chevron.

**Anatomy** (`SettingsKit.kt:128-179`)
1. Optional leading glyph: **20dp** (`RowGlyph`, T:922), tint `TextPrimary` (disabled: `TextDisabled`), then a **14dp** gap (`RowGlyphGap`, T:923). No tile/box behind the glyph (unlike sheet rows).
2. Text column (weight 1): title **16sp** Medium (`RowTitleSize`, T:924), 1 line, ellipsis; optional subtitle **12sp** Regular `TextSecondary` (`RowSubtitleSize`, T:925), 1 line, ellipsis.
3. Optional value text **14sp** (`RowValueSize`, T:926) in `TextSecondary`, 1 line.
4. Trailing slot (only when enabled, `:173`).
5. Optional chevron: 6dp spacer (`Divider*6`), `KoanIcons.ChevronRight` **20dp**, tint `TextSecondary` (`:174-177`).

**Sizes.** Height **54dp** (`RowHeight`, T:916); with a subtitle **62dp** (`RowTallHeight`, T:917) (`SettingsKit.kt:145`). Touch/highlight area: the row is widened by `RowInset` **12dp** on each side past the page padding and clipped to a **14dp** rounded rectangle (`RowRadius`, T:921), content padded back by 12dp so text stays aligned with the dividers (`rowArea`, `SettingsKit.kt:186-195`).

**Variants** (all are `SettingsRow` with different arguments):
| Variant | How it is built | Example |
|---|---|---|
| Navigation | `chevron = true`, `onClick` pushes a page; often a `value` before the chevron | "Search engine" + engine name (`SettingsScreen.kt:168`), "About Koan" + version (`:226`), "Save downloads to" (`:224`) |
| Toggle | trailing `KoanToggle`; tapping the whole row flips it too | "Search suggestions" (`:170-174`), "Hide status bar" (`:196-200`), "uBlock Origin" installed (`:214-218`) |
| Value / action | `value` text, no chevron | "uBlock Origin" not installed → value "Install" (`:212`); "Default browser" → "Koan" (not tappable) or "Set" + chevron (`:180-186`) |
| Segmented host | row with no click, followed by a `Segmented` control and a 10dp spacer (`Divider*10`) | "Start page" (`:176-178`), "Theme" (`:189-194`) |
| Selection (check) | trailing `KoanIcons.Check` 20dp `TextPrimary` on the chosen item | Search engine list (`EnginesPage.kt:52-54`), Save downloads to (`SettingsScreen.kt:304-310`) |
| Row action word | trailing `RowAction` text 14sp `TextSecondary`, own circle-clipped tap target padded 6dp | "Edit" on custom engines (`EnginesPage.kt:55`), "Copy", "Reveal"/"Hide" (`PasswordsPage.kt:198-202`, `RowAction` `:208-216`) |
| Destructive | `titleColor = Destructive` (light `#E5342B`, dark `#FF5A50`); red text only, no fill | "Sign out" (`SettingsScreen.kt:321`) |
| Info (no click) | `onClick` null | Account page email row (`SettingsScreen.kt:317`) |
| Soon (disabled) | `enabled = false`: glyph and title `TextDisabled`, value replaced by "Soon" in `TextDisabled`, trailing and chevron hidden, not tappable (`SettingsKit.kt:149,156,169-176`) | not used on any current Settings page (API only) |

**States.**
- Default: as above.
- Pressed: the Material `clickable` default indication (ripple) inside the 14dp rounded, inset area (`SettingsKit.kt:193-194`). No custom pressed colour.
- Disabled ("Soon"): see table.
- Non-interactive (page underneath): no click.
- Toggle ON/OFF: see `KoanToggle` below.

**Light vs dark.** Palette only.

**Screenshot.** Settings root top to bottom (two screens): one each of navigation + value, toggle ON and OFF, segmented, "Install" value row; plus the Mozilla account page showing the red "Sign out" row; plus a pressed row (ripple) if capturable.

### Toggle (`KoanToggle`, used in settings rows and Clear data card)

`ui/kit/KoanToggle.kt:32-89`. Size **50×30dp** (`ToggleTokens.Width/Height`, T:258-259). Track: rounded (radius h/2) `ToggleOff` (light `#E5E5EA`, dark `#3A3A3C`). Knob: white circle, radius `h/2 − KnobInset` (KnobInset **2dp**, T:260), with a radial shadow (`Black @ 0.22` at 75% → transparent, radius r + **3dp**, offset down by shadow/3; T:261-262; `KoanToggle.kt:74-86`). ON: brand horizontal gradient revealed from the left edge up to the knob's right side, alpha `reveal·0.9` (`TRACK_ALPHA` **0.9**, T:265), where `reveal = clamp(p·3, 0, 1)` (`KoanToggle.kt:63-72`). Motion: `ToggleTokens.Spring` = spring(dampingRatio **0.72**, stiffness **520**) (T:266); knob clamped inside the track during overshoot (`:60-61`). Haptic `CLOCK_TICK` on change (`:44`). Disabled/non-interactive: `onCheckedChange` null → no toggleable modifier (`:38`). No pressed indication (`indication = null`, `:41`).

### Segmented control (`Segmented`)

**Purpose.** Mutually exclusive options (Start page delay; Theme Light / Dark / Follow system).

**Anatomy / sizes** (`SettingsKit.kt:197-240`): full-width track, height **40dp** (`SegmentHeight`, T:932), radius **12dp** (`SegmentRadius`, T:933), background `Glass` (light `#EBEBEF`, dark White @ 0.08), inner padding and gap **3dp** (`SegmentInset`, T:934). Each option weight 1. Selected pill: radius 12 − 3 = **9dp**, background `SegmentPill` (light `#FFFFFF`, dark `#4A4A4D`), shadow elevation **2dp** (`SegmentShadow`, T:936). Label **14sp** (T:935), 1 line: selected `TextPrimary` Bold, unselected `TextSecondary` Medium.

**States.** Selected / unselected; pressed = default ripple clipped to the pill shape (`:226-227`); non-interactive = no click. No animation between options (the pill jumps; no animate call in the code).

**Screenshot.** The Theme row with "Follow system" selected, light and dark.

### Account card (Settings root)

**Purpose.** The Mozilla account entry point; a card that "holds your focus".

**Anatomy** (`SettingsScreen.kt:238-298`): full-width, radius **22dp** (`SettingsTokens.CardRadius`, T:928), background `Card`, **brand gradient outline 1.5dp** (`brandOutline`, `BrandTokens.Outline` T:253, `ui/theme/Brand.kt:33-34`), padding **16dp** (`CardPadding`, T:929).
- Avatar: **44dp** circle (`Avatar`, T:930) on `Glass`, the email's first letter uppercased (or "Z" signed out), **18sp** SemiBold (`AvatarTextSize`, T:931).
- 14dp gap; title **16sp** SemiBold ("Connect your Zen Spaces" signed out / "Mozilla account" signed in) and subtitle **12sp** `TextSecondary` (email or "Sign in with your Mozilla account"), then `ChevronRight` 20dp `TextSecondary`.
- Signed in with a status: 12dp gap (`Divider*12`), then a status line **14sp** `TextPrimary`, led by either a `BrandSpinner` **12dp** (`FirstRunTokens.BadgeSpinner`, T:594) + 8dp while syncing, or a **6dp** `Attention` dot (light `#C0841A`, dark `#E8B45A`) + 8dp when the status needs attention (`:285-296`).

**States.** Signed out / signed in idle / syncing / attention. Pressed: default ripple in the 22dp shape.

**Brand spinner** (`Brand.kt:37-62`): a 270° arc (`SPINNER_SWEEP`, T:249), stroke **2dp** round cap (T:248), sweep gradient Pink→Violet→Blue→Pink, one turn per **900ms** linear (T:250).

**Screenshot.** Settings top with the card signed out (fake), and signed in with a fake email (`someone@example.com`) while syncing.

### About page (`AboutPage`)

**Purpose.** App identity, version, update check, links (tour, What's new, privacy, licences).

**Anatomy** (`ui/settings/AboutPage.kt:44-108`), no page title:
1. Top spacer 44dp (`SectionTop*2`, `:54`).
2. App icon (`R.drawable.koan_icon`) **104dp**, radius **26dp** (T:950-951, `:55-59`).
3. 22dp; "Koan" **30sp** Bold `TextPrimary` (T:952); 6dp; "Version x.y.z" **14sp** `TextSecondary` (T:953); 13.2dp (`SectionTop*0.6`).
4. Update chip (`UpdateChip`, `:110-143`): height **30dp** (T:955), fully rounded (radius 15dp), shadow **1dp** (T:960), background `Card`, horizontal padding **12dp** (T:956); optional **13dp** glyph (T:958) + **6dp** gap (T:957); label **13sp** SemiBold (T:959).
   - Checking: no glyph, "Checking for updates…", text `TextSecondary`, not tappable.
   - Up to date: `KoanIcons.Check`, "Up to date", `TextPrimary`, not tappable.
   - Available: `KoanIcons.Download`, "Update available: vX", tappable (opens the release).
   - Failed: `KoanIcons.Retry`, "Couldn't check for updates", tappable (retries).
5. 66dp (`SectionTop*3`); "Built to pair with Zen" card: radius 22dp, `Card`, **brand outline 1.5dp**, padding 16dp, `KoanIcons.Essentials` 20dp, 14dp gap, title 16sp SemiBold + subtitle 12sp `TextSecondary` (`:74-90`). Not tappable.
6. 8dp; navigation rows (chevron): "Take the tour", "What's new in X.Y", "Privacy notice", "Open-source licences", separated and followed by dividers (`:92-99`).
7. 44dp; footer "Built on GeckoView · Made for one hand" **12sp** `TextSecondary`, centred (T:954, `:101-106`).

**Screenshot.** About in light and dark, chip in "Up to date" state; a second capture of the chip in "Update available".

### Search engine pages (`EnginesPage`, `EngineEditPage`)

**Purpose.** Pick the default engine; add/edit custom engines.

**EnginesPage** (`ui/settings/EnginesPage.kt:42-61`): title "Search engine"; one `SettingsRow` per engine (no glyph), custom ones with subtitle "@shortcut · host", the chosen one with a trailing `Check` 20dp, custom ones with an "Edit" `RowAction`; dividers between; last row "Add a search engine" with `KoanIcons.Plus`.

**EngineEditPage** (`:67-122`): title "Add a search engine" / "Edit search engine"; four `Field`s (Name, Search address, Shortcut, Suggestions address), then 16dp, `PrimaryButton` "Add"/"Save" full width, and when editing 8dp + `DestructiveButton` "Delete".
- **Field** (`:124-169`): top padding **16dp** (`FieldGap`, T:941); label **13sp** SemiBold `TextSecondary`; **6dp** (`FieldLabelGap`, T:942); box height **48dp** (`FieldHeight`, T:939), radius **14dp** (`FieldRadius`, T:940), background `Glass`, horizontal padding 12dp; hint 14sp `FieldHint`; input 14sp `TextPrimary`, cursor `TextPrimary` (solid, never gradient). Under it, error text in `Destructive` or help text in `TextSecondary`, **12sp**, 6dp top. Error shows once the field has text or after a Save attempt (`:77`).
- **PrimaryButton** (`ui/kit/Buttons.kt:22-37`): height **50dp** (T:271), pill, background `Primary`, horizontal padding **28dp** (T:272), text **16sp** SemiBold `OnPrimary` (T:273), icon gap 10dp (T:274).
- **DestructiveButton** (`Buttons.kt:73-87`): 50dp pill, background `GroupedRow`, text 16sp Medium `Destructive`.

**States.** Field: empty (hint) / filled / error / help. Buttons: default + ripple pressed; no disabled look (Save validates on tap).

**Screenshot.** Engines list with the default checked and one fake custom engine ("Example Search", `@ex · example.com`); the edit page with one field showing an error.

### Passwords pages (`PasswordsPage`, `PasswordSitePage`)

**Purpose.** Browse saved logins per site, unlock with fingerprint/PIN, copy/reveal.

**PasswordsPage** (`ui/settings/PasswordsPage.kt:64-118`), title "Passwords":
- Empty state: one paragraph, 14sp `TextSecondary`, 22dp top: "Passwords saved in your Mozilla account show up here after the next sync." (`:69-77`).
- `SearchBox` (`:120-152`): height 40 + 6 = **46dp** (`SegmentHeight + RowInset/2`), full pill, `Glass`, horizontal padding 12 + 4 = **16dp**; `Search` glyph 20dp `FieldHint`; 12dp; hint/input **16sp**; when not empty a clear button **32dp** circle (20 + 12) with `Close` glyph **16dp** (20·0.8) `FieldHint`.
- Locked: "N saved. Unlock to see usernames and passwords." 14sp `TextSecondary`, then full-width `PrimaryButton` "Unlock", 12dp gaps (`:81-90`).
- No match: "No password matches “q”." 14sp `TextSecondary` (`:96-98`).
- Site rows: `SettingsRow` with `KoanIcons.Key`, site host, subtitle = username once unlocked ("No username" / "…" while opening), value "N accounts" or "This phone", chevron (`:99-116`).

**PasswordSitePage** (`:159-205`): title = the site; locked = "Unlock" button; unlocked = per account: row `KoanIcons.Globe` origin + "Saved on this phone"/"From your Mozilla account"; divider; username row (subtitle "Username", trailing "Copy"); divider; password row ("••••••••••" or the password, subtitle "Password", trailing "Reveal"/"Hide"); 22dp between accounts; with a single account a full-width `PrimaryButton` "Copy password".

**States.** Empty / locked / unlocking (rows show "…") / unlocked / search no-match / password hidden vs revealed. Unlock uses the system BiometricPrompt (BIOMETRIC_STRONG or DEVICE_CREDENTIAL, `:233-248`).

**Screenshot.** Only with fake data (seeded `example.com` logins, never real ones): the list locked, the list unlocked, a site page with the password hidden. Note: the system biometric sheet is FLAG_SECURE-like and may not screenshot.

---

### List page shell (`ListPage`) — History and Downloads

**Purpose.** Full-screen list page opened from the ⋯ menu: back + title on top, list in the middle, search field and Clear at the bottom in thumb reach.

**Anatomy** (`ui/kit/ListPage.kt:52-134`)
1. Background `Base` + `settingsWash` (same wash as Settings), insets top-safe, navigation bars, IME (`:67-73`).
2. Header row: height **56dp** (`TopBar`), horizontal padding 20dp; back button 56dp circle with `KoanIcons.Back` **24dp** `TextPrimary`; title `TitleSize·0.7` = **23.8sp** Bold `TextPrimary` (weight 1); optional trailing slot (`:75-87`).
3. Content (weight 1) (`:89`).
4. Bottom bar row: padding horizontal 20dp, vertical **10dp** (`DownloadTokens.BottomGap`, T:1001) (`:91-94`).
   - Search field: height **48dp** (`DownloadTokens.SearchHeight`, T:1000), full pill, background `BarField` (light `#F1F1F4`, dark White @ 0.08), horizontal padding 20dp, `Search` glyph 20dp `FieldHint`, 12dp gap, hint/input **16sp**, cursor `TextPrimary` (`:95-117`).
   - Clear button (only when `onClear` != null): 8dp gap (`ActionGap`, T:996), height 48dp, full pill, background `Card`, horizontal padding 20dp, "Clear" **16sp** Medium `Destructive` (`:118-131`).

**Open/close motion.** Slides up from the bottom: `translationY = (1 − slide)·height`, slide animated with `Motion.Sheet` spring(1.0, 380) (`ui/KoanOverlays.kt:752-762` Downloads, `:781-804` History). Back closes.

### Day card and day header (`DayCard`, `dayRowShape`, `byDay`)

- **Day header:** a `SettingsSection` (13sp SemiBold `TextSecondary`, 22dp top / 4dp bottom) with the label "Today", "Yesterday", "EEEE d MMMM" this year, "d MMMM yyyy" older (`ListPage.kt:162-180`).
- **DayCard** (`ListPage.kt:136-147`): full width, radius **18dp** (`DownloadTokens.GroupRadius`, T:997), background `Card`, horizontal padding 12dp; rows inside separated by `SettingsDivider` (1dp `CardBorder`, full width of the card's inner content).
- **dayRowShape** (`:149-160`): in the lazy History list each row is clipped separately: 18dp corners only on the first row's top and the last row's bottom.

### History page (`HistoryScreen`)

**Purpose.** Pages visited (phone + synced desktop), one card per day, groups of same-site runs, Recently closed at the top; tap opens in a new tab, swipe left removes.

**Anatomy** (`ui/history/HistoryScreen.kt:75-181`), inside `ListPage` titled "History", hint "Search history", Clear always shown:
- `LazyColumn`, content padding start/end 20dp, bottom 22dp (`:106-109`).
- **Recently closed** section (hidden while searching): header "Recently closed", the first **3** closed tabs (`HistoryTokens.CLOSED_PREVIEW`, T:1112), then a row "Show all N more" (16sp Medium `TextPrimary`) that expands the list (`:110-130`).
- **Day sections:** header + rows; rows are `HistoryItem.Page` or `HistoryItem.Group` (grouping: consecutive visits to the same host become a group; the same page twice in a row, ignoring a trailing "/", shows once; `ui/history/HistoryGroups.kt:30-51`). While searching, rows are flat pages (`HistoryScreen.kt:143`). Loads at most **3000** rows (`LIMIT`, T:1109).

**Row frame** (`RowFrame`, `:267-285`): background `Card`, horizontal padding 12dp; a 1dp `SettingsDivider` above every row except the first; the row is clickable (ripple), vertical padding **12dp** (`DownloadTokens.RowPaddingV`, T:991); indented rows (pages of an open group) get start padding **20dp** (`HistoryTokens.GroupIndent`, T:1105).

**Favicon tile** (`:287-296`): **40dp** (`DownloadTokens.FileTile`, T:986), radius **10dp** (T:987), background `GroupedRow`, favicon **24dp** inside (`HistoryTokens.Favicon`, T:1100). Then **12dp** gap (`RowGap`, T:990).

**Row types**
| Row | Title (16sp Medium `TextPrimary`, 1 line, ellipsis) | Subtitle (12sp `TextSecondary`, 1 line) | Trailing |
|---|---|---|---|
| Page (`PageRow`, `:211-227`) | page title, or host if blank | "host · HH:mm" | if from sync: 8dp + `KoanIcons.Desktop` **16dp** `TextSecondary` (`HistoryTokens.SyncGlyph`, T:1101) |
| Closed tab (`ClosedRow`, `:229-242`) | title or host | "host · HH:mm" today, else the day label | — |
| Group (`GroupRow`, `:244-265`) | host | "N pages · HH:mm–HH:mm" (single time when equal) | 8dp + `ChevronRight` **18dp** `TextSecondary` (`HistoryTokens.Chevron`, T:1102), rotates 0° → 90° when open, `tween(200ms)` (`CHEVRON_MS`, T:1106) |
| Show all | "Show all N more" | — | — |

**Swipe to remove** (`SwipeToRemove`, `:183-209`): Material 3 `SwipeToDismissBox`, end-to-start only (`enableDismissFromStartToEnd = false`); background under the row: `Destructive` fill (light `#E5342B`, dark `#FF5A50`) with `KoanIcons.Trash` **20dp** tinted `OnPrimary` (light white, dark `#1C1C1E`), aligned centre-end with 20dp end padding; clipped to `dayRowShape`. The dismiss threshold is **not set in code**: it is Material 3's `SwipeToDismissBoxDefaults` positional threshold (unresolved from repo code). A removal shows a toast with Undo (`ui/KoanOverlays.kt:795`). A group's swipe removes the whole group. Note: this red fill under a swiped row is the one place a red *fill* appears, despite `KoanPalette.Destructive`'s comment "red text only, never a red fill" (T:45).

**Item motion.** Every item uses `Modifier.animateItem()` (default LazyColumn placement/fade specs) (`:113-167`).

**Empty states** (14sp `TextSecondary`, 22dp top): "Pages you visit show up here." / "Nothing in History matches “q”." (`:131-140`).

**Light vs dark.** Palette only; note the trash glyph on red is white in light and near-black in dark (`OnPrimary`).

**Screenshot.** With fake history only (example.com pages): Recently closed with 3 rows + "Show all", a Today card with one open group (chevron turned, indented pages) and a synced page with the desktop mark; a row mid-swipe showing the red bin; the empty state.

### Clear browsing data card (`ClearDataCard`)

**Purpose.** Floating card behind History's Clear: choose history / cookies / cache.

**Anatomy** (`ui/history/ClearDataCard.kt:31-60`), shown inside `KoanCard` (the floating card, `ui/sheet/KoanSheet.kt:223-257`: margin 10dp, radius **28dp**, padding 20dp, `SheetTokens.CardMargin/CardRadius/CardPadding` T:836-838; used at `ui/KoanOverlays.kt:808`):
- Column with **10dp** gaps (`SheetTokens.Gap`, T:830).
- Title "Clear browsing data?" **18sp** SemiBold `TextPrimary` (`SheetTokens.CardTitleSize`, T:864).
- `SheetGroup` (`ui/sheet/SheetKit.kt:41-49`): radius **18dp** (T:841), background `GroupedRow`; three `SheetRow`s without icon (`SheetKit.kt:68-119`): height 50dp / **58dp** with subtitle (T:842-843), horizontal padding 12dp, title 16sp Medium, subtitle 12sp `TextSecondary`; each with a trailing `KoanToggle`. Rows: "History" (subtitle "Desktop history is only hidden here", default ON), "Cookies and site data" ("Signs you out of most sites", default OFF), "Cached images and files" (default ON). `SheetDivider` 1dp `CardBorder`, indented start 12·2 + 30 = **54dp** (`SheetKit.kt:53-61`).
- 5dp spacer; `DestructiveButton` "Clear" full width (does nothing if all three are off); `QuietButton` "Cancel" (50dp, no surface, 16sp Medium `TextPrimary`, `Buttons.kt:40-53`).

**States.** Each toggle on/off; all off = Clear inert (no visual disabled state).

**Screenshot.** History with the Clear card open over it (scrim visible), default toggles.

### Downloads page (`DownloadsScreen`)

**Purpose.** Running downloads with gradient progress, then finished files by day; a finished row expands to Open / Share / Delete.

**Anatomy** (`ui/downloads/DownloadsScreen.kt:56-129`), inside `ListPage` titled "Downloads", hint "Search downloads", Clear shown only if there is any download:
- Header trailing **folder chip**: height **32dp** (`DownloadTokens.ChipHeight`, T:998), full pill, `Card`, horizontal padding 12dp, `KoanIcons.Folder` **16dp** (20·0.8) `TextSecondary`, 6dp, folder name **13sp** `TextSecondary` (T:999). Tap opens the folder (`:82-96`).
- Content column scrolls, horizontal padding 20dp.
- "In progress" section + one `DayCard` of running rows; then a section + `DayCard` per day of finished rows.

**File tile** (`FileTile`, `:257-272`): **40dp**, radius 10dp, `GroupedRow`; `KoanIcons.File` **18dp** (T:988) `TextPrimary` over the extension (max 4 chars, uppercased) **7sp** SemiBold `TextSecondary` (T:989).

**Running row** (`:133-182`): vertical padding 12dp; tile; 12dp; column: filename 16sp Medium (ellipsis) + state text 12sp `TextSecondary` ("Paused", "Couldn't finish", "Starting…", or "NN%"); **progress bar** height **4dp** (T:992), top padding **8dp** (T:993) / bottom 4dp, fully rounded, track `Glass`, fill **brand horizontal gradient** (`BrandHorizontal`) over the fraction (`:152-165`); then "x MB of y MB · host" 12sp `TextSecondary`. Then 8dp + round button (Pause / Play when paused / Retry when failed) + 8dp + round Close (cancel).
- **Round button** (`:243-255`): **36dp** circle (T:994), `GroupedRow`, glyph 16dp `TextPrimary`.

**Done row** (`:184-218`): tap toggles expansion (only one open at a time); vertical padding 12dp; tile, 12dp, filename 16sp Medium + "size · host" 12sp `TextSecondary`. Expanded: a row of three equal **actions** with 8dp gaps, bottom padding 12dp.
- **Action** (`:220-241`): height **40dp** (T:995), pill; Open = primary (`Primary` fill, `OnPrimary` content); Share = `GroupedRow` fill, `TextPrimary`; Delete = `GroupedRow` fill, `Destructive` content; glyph **17dp** (20·0.85) + 6dp + label 14sp Medium.

**States.** Running: downloading (with %) / starting / paused / failed. Done: collapsed / expanded. No swipe-to-remove on Downloads. Expansion has no animation (plain `if (open)`).

**Empty states** (14sp `TextSecondary`, 22dp top): "Files you download show up here." / "No download matches “q”." (`:104-111`).

**Screenshot.** With fake files only (e.g. `example.pdf`, `photo.jpg` from example.com): one running download at ~40%, one paused, a Today card with one row expanded showing Open/Share/Delete; the empty state.

---

### Sign-in screen shell (`SignInScreen`)

**Purpose.** Full-screen Mozilla sign-in dressed as Koan: header with ✕, thin progress line, then the Choice page, the From-your-computer page, or Mozilla's own web page (email/password or pairing).

**Anatomy** (`ui/signin/SignInScreen.kt:88-354`)
1. Background `Base`, top-safe padding (`:210-215`).
2. **Header** row: height **64dp** (`SignInTokens.HeaderHeight`, T:599), horizontal padding **8dp** (T:600) (`:216-240`).
   - Close: **44dp** circle touch (`CloseTouch`, T:601), `KoanIcons.Close` **20dp** (`CloseGlyph`, T:602), `TextPrimary`; disabled while finishing (`:223-231`).
   - On every page except Choice: 8dp, then "Sync your Zen Spaces" **17sp** Bold `TextPrimary` (T:603) over "Mozilla account" **13sp** `TextSecondary` (T:604) (`:233-239`).
3. **Progress line**: full width, **2dp** (`ProgressHeight`, T:605), drawn in `TextSecondary` at alpha **0.8**, width = progress; shown only between 1% and 99% or while finishing (then 100%) (`:241-251`). Deliberately neutral: "Login UI stays neutral: never the brand gradient or a space colour" (`:206-207`).
4. Page area (weight 1, IME padding) (`:265`).
5. **Notice** (top centre, over the page): margin **12dp** (T:606), radius **16dp** (T:608), background `Notice` (light `#F2F2F5` @ alpha 0xF2 ≈ 0.95, T:121; dark `#2C2C2E` @ 0xF2 ≈ 0.95, T:167), padding **14dp** (T:607), text **14sp** `TextPrimary` (T:609); tap dismisses (`:324-336`). Texts include "No connection. Check it and try again.", camera and clipboard messages (`:132-136`, `:171`, `:202`, `:295`).
6. **Finishing pill** (bottom centre, nav-bar padded): same margin/padding/colour, fully round, "Connecting to your Zen Spaces…" 14sp (`:337-351`).
7. **Outdated WebView** state (system WebView major < **92**, T:615): centred column, horizontal padding **32dp** (T:616), message **16sp** centred (T:617), **20dp** gap (T:618), `PrimaryButton` "Open Play Store" (`:363-385`).

**Motion.** The screen slides up over everything and back down with `Motion.Sheet` spring(1.0, 380) (`ui/KoanRoot.kt:1006-1008`). A web page fades in once it has painted: alpha 0 → 1 `tween(220ms)` (`SignInTokens.FADE_MS`, T:612; `SignInScreen.kt:266`), reset to 0 on every mode change (`:125`). WebViews have a transparent background until painted (`ui/signin/MozillaSignIn.kt:256`, `ui/signin/PairingWeb.kt:39`).

**Back behaviour** (`:114-123`): web history back → Pairing returns to From-your-computer → any page returns to Choice → Choice closes.

**Light vs dark.** Palette only; Mozilla's pages are themed to Koan's theme through a light/dark `ContextThemeWrapper` (`MozillaSignIn.kt:313-314`) and algorithmic darkening on Android 13+ (`MozillaSignIn.kt:259-260`); below Android 13 the sign-in page stays light.

### Sign-in Choice page (`Choice`, floating K, cards, Recommended badge)

**Purpose.** First page of sign-in: the K, the title, and two ways in as cards in the thumb zone.

**Anatomy** (`ui/signin/SignInPages.kt:68-101`), column centred, horizontal padding **20dp** (`PagePadding`, T:625), nav-bar padded:
1. Flexible spacer (weight 1).
2. **Floating K** (`FloatingK`, `:108-144`), box **96dp** (`KSize`, T:627): brand glow layer, sparkles layer, K image (`R.drawable.koan_k`, 768×762 WebP with alpha, `app/src/main/res/drawable-nodpi/koan_k.webp`).
3. **24dp** (`KGap`, T:628); title "Sync your Zen Spaces" **26sp** Bold centred `TextPrimary` (`HeroSize`, T:629).
4. **8dp** (`HeroLineGap`, T:630); "Sign in with the Mozilla account you use in Zen." **15sp** `TextSecondary` centred (`BodySize`, T:631).
5. Flexible spacer (weight 0.4).
6. Choice card "From your computer" / "Scan a code from Zen. No password." (`KoanIcons.Desktop`, Recommended); **16dp** (`ChoiceGap`, T:654); Choice card "With email and password" / "On Mozilla's sign-in page." (`KoanIcons.Key`) (`:96-98`).
7. **20dp** bottom (`BottomGap`, T:626).

**Floating K motion** (a frame clock in ms, starts at `STILL_CLOCK` = 1300 (T:649); `:112-122`):
- Float: `translationY = sin(2π·clock/4200)·4dp` (`K_FLOAT_MS` **4200**, `KFloat` **4dp**, T:639-640; `:156`), applied to the K and the sparkles.
- Tilt: `rotationZ = sin(2π·clock/6400)·1.8°` (`K_TURN_MS` **6400**, `K_TURN` **1.8**, T:641-642; `:140`).
- "Remove animations" (animator duration scale 0, `ui/tour/Welcome.kt:74-75`): clock frozen at 1300ms, nothing moves (`:113-114`).

**Brand glow** (`brandGlow`, `:158-172`): three radial gradients `color @ a → Transparent` of radius `r`, where
- `breath = sin(2π·clock/5600)` (`GLOW_BREATH_MS` **5600**, T:645);
- `a = base·(1 + breath·0.12)` with base light **0.30** / dark **0.38** (`GLOW_ALPHA_LIGHT/DARK`, T:635-636; `GLOW_BREATH` **0.12**, T:644);
- `r = min(w,h)·0.95·(1 + breath·0.12·0.5)` (`GLOW_RADIUS` **0.95**, T:634), i.e. radius ≈ 91dp ± 6%.
- Spot centres (c = box centre): Pink at (c.x − 0.35r, c.y + 0.10r); Violet at (c.x, c.y − 0.20r); Blue at (c.x + 0.35r, c.y + 0.15r).
This is the single allowed brand-gradient use on sign-in UI (`T:197-201`; `docs/UI_SPEC.md:355`).

**Sparkles** (shared `sparkles` modifier, `ui/tour/Welcome.kt:332-358`), unit **5dp** (`SparkleUnit`, T:646), colour **white in dark, Brand Violet `#B46BFF` in light** (`SignInPages.kt:124`). Four sparkles (`KSparkles`, `:147-152`), positions as fractions of the 96dp box:
| # | x | y | size | period ms | phase rad |
|---|---|---|---|---|---|
| 1 | −0.14 | 0.18 | 0.8 | 3400 | 0.0 |
| 2 | 1.10 | 0.08 | 0.6 | 4100 | 2.1 |
| 3 | 1.04 | 0.92 | 0.7 | 3700 | 4.0 |
| 4 | 0.12 | 1.08 | 0.5 | 4600 | 5.3 |
Without live blur (Android 11, `DeviceProfile.liveBlur` = SDK ≥ 31, `DeviceProfile.kt:13`) only the first **2** (`SPARKLES_SIMPLE`, T:647). Star shape and twinkle formula: see "Welcome card · sparkles" below.

**Choice card** (`ChoiceCard`, `:174-219`)
- Row: full width, height **84dp** fixed (`ChoiceHeight`, T:652), radius **22dp** (`ChoiceRadius`, T:655), background `Card`, border **1dp** `CardBorder` (T:656), horizontal padding **16dp** (`ChoiceInset`, T:653); clickable (ripple).
- Icon box **44dp** circle (`ChoiceIconBox`, T:657) on `Base`, glyph **24dp** (`ChoiceGlyph`, T:658) `TextPrimary`.
- 16dp; title **16sp** SemiBold `TextPrimary` (`ModeSize`, T:621), 1 line; **2dp** (`ChoiceLineGap`, T:659); subtitle **13sp** `TextSecondary` (`SubtitleSize`), 1 line.
- 2dp; `ChevronRight` **18dp** (`ModeGlyph`, T:622) `TextSecondary`.
- **Recommended badge** (`:204-217`): text "Recommended" **11sp** SemiBold (`BadgeSize`, T:660) `OnPrimary` on a full pill of `Primary` (light: white on `#1C1C1E`; dark: `#1C1C1E` on white), padding **9dp × 3dp** (T:662-663), aligned top-end of the card, offset x = −16dp, y = −**9dp** (`BadgeLift`, T:661) so it straddles the top edge. Not gradient.

**States.** Card default / pressed (ripple). No disabled state. The camera permission is requested only after tapping "From your computer" (`SignInScreen.kt:185-191`).

**Light vs dark.** Glow alpha 0.30 vs 0.38; sparkles violet vs white; badge inverts with `Primary`.

**Screenshot.** Signed-out, Settings › account card → sign-in: Choice page in light and in dark (glow, K, both cards, badge). No Mozilla page content needed.

### Sign-in From-your-computer page (`FromComputer`: viewfinder, brackets, steps)

**Purpose.** Scan the "Connect another device" QR code from Zen (or paste its link).

**Anatomy** (`ui/signin/SignInPages.kt:221-283`), column centred, horizontal padding 20dp:
1. Spacer weight 1.
2. **Viewfinder**: width **75%** of the page (`VIEWFINDER_WIDTH`, T:666), square (`aspectRatio(1f)`), radius **28dp** (`ScanRadius`, T:667). Background **black** with camera, `Card` without (`:243-250`).
   - With camera: `QrScanner` (Camera2 back camera, TextureView preview filling the width, cropped top/bottom; preview stream near 1280×720, max 1920×1080; zxing QR decode; `ui/signin/QrScanner.kt:47-86`, `:194-196`).
   - Without camera: `KoanIcons.Camera` **24dp** `TextSecondary`, 8dp, `SecondaryButton` "Allow the camera" on `Base` (50dp pill, 28dp padding, 16sp SemiBold `TextPrimary`, `ui/kit/Buttons.kt:55-70`) (`:258-264`).
   - **Aim brackets** on top (`aimBrackets`, `:285-310`): four corners, inset **18dp** (T:668), each leg **30dp** (T:669), rounded corner radius **10dp** (quadratic curve, T:670), stroke **3dp** round cap (T:671); colour **White** with camera, `TextSecondary` without.
3. **28dp** (`StepsGap`, T:672).
4. **Steps** column (75% width, **12dp** apart, T:673): "In Zen, open Settings › Sync.", "Click \"Connect another device\".", "Scan the code." (`:268-272`). Each step (`Step`, `:312-324`): number dot **26dp** circle (T:674) on `Card`, number **13sp** SemiBold `TextPrimary` (T:675); **12dp** (T:676); text **15sp** `TextPrimary`.
5. Spacer weight 1.
6. `SecondaryButton` "Paste the link instead": full width, surface `Card`, **1dp** `CardBorder` circle border (`:274-279`).
7. **Switch link** "Use email and password instead" (`SwitchLink`, `:341-355`): **14sp** Medium `TextSecondary` (`LinkSize`, T:679), pill-clipped tap area padded **16dp × 12dp** (T:680-681), centred; then **4dp** (`LinkBottom`, T:682).

**States.** Camera allowed (live preview, white brackets) / not allowed (Card + "Allow the camera", grey brackets) / camera blocked for good (button opens app settings, `SignInScreen.kt:174-179`) / finishing (inputs ignored, `enabled = false`). Errors appear as the top Notice.

**Screenshot.** Two captures: without camera permission (Card viewfinder with "Allow the camera"), and with the camera pointing at a blank surface (no real code, no account). Light and dark.

### Sign-in email/password and pairing pages (`WebPage`, Mozilla's page)

**Purpose.** Host Mozilla's own accounts page (email/password) or its pairing page, with a switch to the other way in.

**Anatomy** (`ui/signin/SignInPages.kt:326-339`): the WebView fills the area (weight 1); under it, when not finishing and the keyboard is hidden, the `SwitchLink` ("Sign in from your computer instead" on the password page, "Use email and password instead" on the pairing page, `SignInScreen.kt:269-305`) and 4dp. The page itself is Mozilla's (not Koan UI): Koan adds only the header, progress line, notices and switch link. Brand gradient is never used here (`SignInScreen.kt:206`).

**States.** Loading (progress line, page invisible until painted, then 220ms fade), painted, keyboard up (switch link hidden), finishing (bottom pill), error notice, unverified ("Confirm the sign-in from the email Mozilla just sent…", `:279`).

**Screenshot.** Header + progress line + the switch link only; crop or blur Mozilla's page, and never show a real email.

---

### Tour / What's new sheet (`TourOverlay`)

**Purpose.** The first-run tour and each version's "What's new": a tall sheet of swipeable cards over Koan's dimmed UI.

**Anatomy** (`ui/tour/TourOverlay.kt:85-177`)
1. **Scrim**: full screen, `SheetScrim` (light `#1C1C28` @ 0.30, dark Black @ 0.50) × open progress × (1 − drag/height); swallows all touches (`:117-123`).
2. **Sheet**: bottom-aligned, full width, height **92%** of the screen (`TourTokens.SHEET_FRACTION` 0.92, T:477); top corners **32dp** (`SheetTokens.TopRadius`, T:826); background `Dock` (light `#FFFFFF`, dark `#252527`); then the **title glow**, then the **welcome backdrop**; nav-bar padded (`:124-141`).
3. **Skip** row: top padding **6dp** (`SkipTop`, T:480), height **44dp** (`SkipHeight`, T:479), horizontal padding 12dp (Padding/2); `TourButton` "Skip" unfilled, aligned end (`:142-144`).
4. What's new only: "What's new in X.Y" **15sp** Medium `TextSecondary` centred, 6dp below; alpha = 1 − welcome (fades in as the intro card leaves) (`:145-155`).
5. **HorizontalPager** (weight 1, `beyondViewportPageCount = 0`) of cards (`:156-165`).
6. **16dp** (`Gap`, T:484) · **page dots** · 16dp (`:166-168`).
7. **Primary button**: "Next", last card "Get started" (tour) or "Got it" (What's new); full width minus 24dp each side (`Padding`, T:478) (`:169-173`); then 12dp (`:174`).

**Title glow** (`titleGlow`, `:375-387`): same 3-spot layout as the Settings wash, `h = GlowHeight` **240dp** (T:508), radius h·0.75 = 180dp, alpha light **0.16** / dark **0.12** (T:509-510). Spots: Pink (0.10·w, 0.15·h), Violet (0.55·w, 0.05·h), Blue (0.95·w, 0.30·h). Drawn on every card (under the welcome backdrop).

**TourButton** (`:348-373`): height **50dp** (`ButtonTokens.Height`), full pill, horizontal padding 28dp, text **16sp** (SemiBold filled / Medium unfilled). Filled = fill `lerp(Primary → White, FastOutSlowIn(welcome))`; label `OnPrimary` until welcome > 0.5, then **Black** (flips at mid-grey). Unfilled (Skip) = no fill, label `lerp(TextPrimary → White, welcome)`. Pressed = ripple. Note: Skip sits in the 44dp `SkipHeight` box, so its 50dp height is constrained to 44dp by the parent (Compose `height` respects incoming max constraints).

**Page dots** (`PageDots`, `:318-346`), Canvas: dot **7dp** (T:513), current one stretches to **20dp** (T:514), gap **7dp** (T:515), all fully rounded; width follows the finger (`near = 1 − |pos − i|`). Colours: on = `lerp(TextPrimary → White, welcome)`, off = `lerp(TextDisabled → White @ 0.45, welcome)`, interpolated by `near`.

**Gestures and thresholds**
- Swipe between cards: `HorizontalPager` defaults (fling/snap not overridden). Next / Back animate with `Motion.PagerSettle` = spring(dampingRatio **0.86**, stiffness **300**) (T:1181; `:113`, `:172`). System Back steps to the previous card; on the first card Back does nothing (`:112-115`).
- **Swipe down to close** (`swipeDownToClose`, `:179-211`): vertical drag moves the sheet 1:1 (never above 0); closes if dragged past **25%** of the sheet height (`SheetTokens.COMMIT_FRACTION` 0.25, T:820) or released faster than **600dp/s** downward (`SheetTokens.FlingVelocity`, T:823); else springs back with `Motion.Sheet`. A sideways drag belongs to the pager.
- Open/close: `Motion.Sheet` spring(1.0, 380) (`:87-88`); `translationY = drag + (1 − shown)·(height − drag)` (`:130-135`).

**Light vs dark.** Sheet `Dock` white vs `#252527`; glow 0.16 vs 0.12; scrim strength.

**Screenshot.** About › Take the tour, card 2 ("Your Zen Spaces, on your phone") at rest, light and dark; a mid-swipe from card 1 to 2 showing the gradient fading into the sheet; the last card showing "Get started".

### Tour card (`TourPage`, `TourCard`) and clip frame (`PhoneFrame`, `TourVideo`)

**Purpose.** One tour/What's new card: title, one line, then a looping clip, still or drawn scene in a phone frame.

**Card data** (`ui/tour/TourContent.kt:17-28`): title, line, dark/light MP4 (`res/raw`), framing (`Screen` = aspect **720/1478** (`SCREEN_CLIP_ASPECT`, T:501) or `Focus` = fill the slot), `welcome`, optional `icon`, dark/light still (`res/drawable-nodpi`), optional `scene` (`TourScene.SignIn`).

**Anatomy** (`ui/tour/TourOverlay.kt:213-275`), centred column:
1. (Text-only card with an `icon`: icon tile **96dp** (T:487), radius **28dp** (T:488), `GroupedRow`, glyph **44dp** (T:489) `TextPrimary`, then 24dp (Gap·1.5); content vertically centred. No current card uses this path: every card in `TourCards` and `Highlights` has a clip, still or scene, `TourContent.kt:58-122`.)
2. Title **22sp** Bold centred `TextPrimary` (`TitleSize`, T:481), horizontal padding 24dp.
3. **6dp** (`TitleGap`, T:483); line **15sp** `TextSecondary` centred (`LineSize`, T:482), `minLines = 2` so frames line up across cards.
4. 16dp; frame slot: **90%** of the width (`FRAME_WIDTH`, T:495), all the height left; frame sized to the clip's aspect (plus bezels) or the whole slot for Focus (`:253-261`); then **10dp** (FrameShadow/2) (`:271`).
5. Optional footer (last What's new card: changelog button).

**PhoneFrame** (`:297-316`): outer radius **30dp** (`FrameRadius`, T:502); shadow elevation **20dp** (`FrameShadow`, T:505) with ambient and spot colour Black @ **0.18**; background `Card`; border **1dp** `CardBorder` (T:504); bezel padding **5dp** (`FrameBezel`, T:503); inner clip radius 30 − 5 = **25dp**, inner background `GroupedRow` (shows until the clip's first frame).

**TourVideo** (`ui/tour/TourVideo.kt:20-94`): muted, looping MP4 via MediaPlayer on a TextureView, centre-crop to cover; view alpha 0 until the first rendered frame, then fades to 1 in **150ms** (`FADE_MS`, `:58`, `:92`); decoded only while composed. Stills: `Image` with `ContentScale.Crop`, top-centre aligned (`TourOverlay.kt:266-268`).

**Clips in the app** (`TourContent.kt:58-122`, files `app/src/main/res/raw/*_{dark,light}.mp4`): tour_spaces (Focus), tour_stack (Screen), tour_tabs (Focus), tour_scrub (Focus), tour_search (Screen), tour_holds (Focus); What's new 0.4: SignIn scene (Focus), whatsnew_search (Screen), whatsnew_reopen (Screen), whatsnew_engines still (Screen). The dark/light variant follows Koan's theme (`TourOverlay.kt:217-218`).

**Tour card list** (`TourContent.kt:58-92`): 1 Welcome to Koan / Your Zen Spaces, in your pocket. (welcome) · 2 Your Zen Spaces, on your phone · 3 Swipe up for your tabs · 4 Swipe sideways to switch tabs · 5 Hold and slide to change space · 6 Search shortcuts · 7 Long presses.

**What's new logic** (`TourContent.kt:43-55`, `:127-146`): opens on the intro card (`whatsNewIntro`), then the highlights of the newest feature version after the last seen one; a version without highlights shows nothing.

**States.** Clip loading (GroupedRow inner) / playing; animations off: clips still play (MediaPlayer is not gated by `animationsOff`; only the Welcome card and SignInScene check it).

**Screenshot.** One Focus-framed card (e.g. "Swipe sideways to switch tabs") and one Screen-framed card ("Swipe up for your tabs"), light and dark, mid-clip.

### Changelog card (last What's new highlight, `WhatsNewEnd`)

`ui/tour/TourOverlay.kt:277-295`: under the last highlight's frame, 16dp, then a pill button: height **50dp**, padding 28dp, background `GroupedRow`; `KoanIcons.GitHub` **20dp** (`ChangelogIcon`, T:492) `TextPrimary`; **9dp** (TitleGap·1.5); "See the full changelog" **16sp** Medium `TextPrimary`. Opens `https://github.com/Nerzouille/Koan/releases/latest` (`TourContent.kt:125`, `ui/settings/AboutPage.kt:38`). The primary button below reads "Got it". Not gradient.

**Screenshot.** What's new 0.4, last card ("Your own search engines") with the changelog button and "Got it".

### Welcome card (tour card 1) and What's new intro card (`WelcomePage`, `welcomeBackdrop`, `WelcomeMotion`)

**Purpose.** The tour's first card: the 3D K on the vivid brand gradient, a turning vortex, twinkling sparkles. The What's new intro card reuses it ("What's new in X.Y" / "A few new things since last time ✨", `TourContent.kt:127-129`) with a lighter entrance.

**Anatomy** (`ui/tour/Welcome.kt:161-252`), centred column:
1. Spacer weight 1.
2. K box **210dp** (`TourTokens.K`, T:520): sparkles layer + K image (`R.drawable.koan_k`); its centre is reported to the sheet for the vortex (`:173-178`).
3. **28dp** (`WelcomeTextGap`, T:523); title **30sp** Bold **White** centred (`WelcomeTitleSize`, T:521), horizontal padding 24dp.
4. **6dp**; line **17sp** Medium **White @ 0.88** centred (`WelcomeLineSize`, T:522; `:236`).
5. Spacer weight 0.75.
The Skip label, dots and Next button recolour to white / white-with-black-text via `welcome` (see TourOverlay).

**Brand gradient backdrop** (`welcomeBackdrop`, `:262-281`), drawn by the whole sheet (no seam):
- `Brush.linearGradient([Pink #FF5FA2, Violet #B46BFF, Blue #4F8DFF])`, evenly spaced stops (0, 0.5, 1), from the sheet's **top-left (0,0) to bottom-right (w,h)** (`:268`), alpha = `a`.
- Dark mode only: an extra Black rect at alpha `a·0.14` (`WELCOME_DIM_DARK`, T:526) — "a touch deeper" (`:274`).
- `a` = 1 on the card, falling linearly to 0 as the next card slides in (`1 − pagerPos`, `TourOverlay.kt:111`): the gradient blends into the sheet's `Dock` colour while swiping.

**Vortex** (`Vortex`, `:283-314`), centred on the K's centre:
- Rings: **8** (`VORTEX_RINGS`, T:529), **5** without live blur (`VORTEX_RINGS_SIMPLE`, T:530).
- Radius of ring i: `120dp + i·46dp` (`VortexInner` 120, `VortexStep` 46, T:531-532) → 120, 166, 212, 258, 304, 350, 396, 442dp.
- Each ring: a sweep gradient of White with 5 evenly spaced stops `[0, a_i, 0, 0.55·a_i, 0]` (two bright arcs, one fainter), `a_i = 0.20·(1 − 0.6·i/rings)` (`VORTEX_ALPHA` **0.20**, T:534; `:291-295`).
- Soft edges: every ring drawn three times with strokes **26dp**, 15.6dp (×0.6) and 6.5dp (×0.25) (`VortexStroke`, T:533; `:302`).
- Centre glow: radial White @ **0.35** → 0 (`VORTEX_GLOW_ALPHA`, T:536), radius `120dp·1.4` = **168dp** (`:296-301`).
- Rotation of ring i: `turn·(1.6 − 0.12·i) + i·47°`, where `turn = clock/24000·360°` (`VORTEX_TURN_MS` **24000**, T:535) → the base turn is 15°/s; ring 0 turns at 24°/s, ring 7 at ≈11.4°/s (inner rings faster) (`:304-313`).
- Vortex alpha = `vortex()·a`; `vortex()` ramps 0 → 1 over the first **750ms** of the intro with LinearOutSlowIn (`:99`).
- Leaving: the vortex trails the page: x offset `−(1 − a)·w·(1 − 0.5)` (`VORTEX_LEAVE_TRAIL` **0.5**, T:568; `:277`).

**Sparkles** (`Sparkles`, `:316-330`; drawing `:332-358`): colour White, unit **9dp** (hard-coded at `:330`, not a token). Six sparkles, positions as fractions of the 210dp K box:
| # | x | y | size | period ms | phase rad |
|---|---|---|---|---|---|
| 1 | −0.12 | 0.12 | 1.00 | 2300 | 0.0 |
| 2 | 1.06 | 0.02 | 0.75 | 2900 | 1.7 |
| 3 | 1.14 | 0.62 | 0.55 | 2100 | 3.1 |
| 4 | 0.02 | 1.02 | 0.60 | 2600 | 4.4 |
| 5 | 0.62 | −0.14 | 0.50 | 1900 | 2.4 |
| 6 | 0.86 | 1.10 | 0.85 | 3200 | 5.2 |
Without live blur only the first **3** (`SPARKLES_SIMPLE`, T:563).
- Shape: four-point star of radius 1 with curved-in sides (quadratic control points at ±0.12) (`:336-343`), plus a soft halo dot: radial `color @ 0.45 → 0` at **1.8×** the star radius (`:344`, `:353`).
- Twinkle: `wave = 0.5 + 0.5·sin(2π·t/period + phase)`, `level = wave²`; skipped below 0.02; star radius `unit·size·(0.45 + 0.55·level)`; star and halo alpha = `level` (`:348-354`).
- Layer alpha = `sparkles()·clamp(1 − 2·leave, 0, 1)`: sparkles fade in 750→1400ms of the intro (linear) and vanish in the first half of the swipe away (`:186`, `:98`).

**K motion**
- **Tour entrance** (`WelcomeMotion.play`, `:110-137`): after **180ms** (`K_RISE_DELAY_MS`, T:541) the K rises with spring(dampingRatio **0.6**, stiffness **120**) (T:542-543): `translationY = (1 − rise)·pageHeight·0.6` (`:255`); rotation from **−38°** to 0 (`K_START_TURN`, T:539); scale from **0.72** to 1 (`K_START_SCALE`, T:540); alpha = clamp(rise·2.5) (`:197-208`).
- **Light sweep** across the K at **750ms** for **650ms**, FastOutSlowIn (`SWEEP_AT_MS`, `SWEEP_MS`, T:544-545): a diagonal band 0.35·width wide, gradient White `[0, 0.7, 0]`, from (x − band, 0.2h) to (x + band, 0.8h − 0.6·band), `BlendMode.SrcAtop` (on the K's pixels only; offscreen layer while it runs) (`:360-376`, `:209-213`).
- **Text**: title from **850ms** for **380ms** (LinearOutSlowIn), line **120ms** later; each fades 0→1 and rises **14dp** → 0 (`TEXT_AT_MS`, `TEXT_STAGGER_MS`, `TEXT_MS`, `TextRise`, T:546-549; `:100-101`, `:227-247`). Intro total `INTRO_MS` = 850 + 120 + 380 = **1350ms** (T:550), driven linearly.
- **What's new entrance** ("pop"): the K is already risen; it pops from scale **0.86** to 1 with spring(dampingRatio **0.5**, stiffness **380**) (T:553-555), alpha = clamp(pop·3); the timeline is entered at 750 − 180 = 570ms so the sweep starts **180ms** after the pop (`POP_SWEEP_AT_MS`, T:556; `:119-127`).
- **Idle** (only while the welcome card is at least partly on screen and animations are on, `TourOverlay.kt:107-109`): float `sin(2π·clock/3400)·7dp` (`FLOAT_MS` **3400**, `FloatAmplitude` **7dp**, T:559-560; `:259-260`); tilt `sin(2π·clock/5300)·2.5°` (`FLOAT_TURN_MS` **5300**, `FLOAT_TURN` **2.5**, T:561-562; `:201-202`).
- **Leaving** (swipe to card 2; `leave` 0→1 follows the pager both ways): K `translationX = −leave·pageWidth·0.6` (`K_LEAVE_EXTRA`, T:566; `:257`), extra rotation `leave·−24°` (`K_LEAVE_TURN`, T:567), scale ×(1 − 0.1·leave) (`:204`).
- **Animations off**: everything snaps to the end of the intro, clock frozen at **1300ms** (sparkles frozen mid-twinkle) (`:111-116`, `:150-153`).

**Light vs dark.** Same gradient; dark adds the 14% black dim. Text is always white.

**Screenshot.** About › Take the tour, card 1 at rest (after the intro, ~2s in), light and dark; and About › What's new in 0.4 intro card. Optionally a frame mid-sweep.

### Sign-in scene (What's new 0.4 highlight, `SignInScene`)

**Purpose.** "Sign in from your computer" drawn in code instead of recorded (no real account or code): a laptop with a made-up code, a phone slides in, a gradient line scans, the code turns into a check, the K shows on the phone; loops.

**Timeline** (`SignInSceneTokens`, T:207-221; ms within a **4400ms** loop): phone in at **250** for **750** (LinearOutSlowIn); scan at **1050** for **900** (FastOutSlowIn); check at **1950** for **450**; K at **2150** for **400**; reset at **3750** for **550** (FastOutSlowIn). Animations off: frozen at **3000ms** (`STILL_MS`) (`ui/tour/SignInScene.kt:48-74`, `:138-152`).

**Colours** (light / dark, T:224-238): lid `#E4E4EA` / `#3A3A41`; display `#FFFFFF` / `#1F1F24`; base `#D3D3DB` / `#2D2D33`; code modules `#1C1C1E` / `#EDEDF2`; phone body `#1C1C1F`; phone screen vertical gradient `#2B2B33` → `#141418` (both themes). Brand mist: three radial spots Pink (0.12w, 0.18h, r 0.75·unit), Violet (0.5w, 0.45h, r 0.8·unit), Blue (0.9w, 0.85h, r 0.75·unit) at alpha **0.13** light / **0.16** dark (`SignInScene.kt:113-117`). Laptop shadow: radial Black @ 0.12 light / 0.35 dark (`:118-122`).

**Geometry** (`:77-136`, `:303-324`): sizes in a scene `unit = min(w·(1 − 2·0.07)/span, 0.78·h)`; lid width 0.8u, height 0.64·lid; base 1.16·lid wide, 0.032u tall; bezel 0.022u; code 0.6·lid height, **21×21** modules (QR finder squares + fixed scatter, `:278-293`), module gap 12%, radius 25%; phone 0.3u wide × 2.02 tall, at 0.45·lid right of centre, tilted **6°** (rises from 0.55h below, tilt 12°→6°); phone corner radius 0.17·w, screen inset 0.045·w, screen radius 0.13·w; camera corners box 0.62·screen width, arms 22%, stroke 0.025·phoneW, White @ 0.7.
- **Scan line**: horizontal gradient Transparent → Pink → Violet → Blue → Transparent, 0.4 module tall, trailing glow Violet @ 0.22 over 0.28·code height (`:183-197`).
- **Check**: brand linear-gradient disc, r = 0.36·code size, ease-out-back scale; violet halo @ 0.3 at 2.2r; white check stroke 0.16r round; 4 violet (@ 0.9) sparkles (2 without live blur), periods 1500/1800/1300/1700ms (`:199-226`, `:318-323`).
- **K on phone**: violet glow @ 0.45 (r 0.6·screen width), K at 0.62·screen width scaling 0.8→1, placed by its optical centre (0.46, 0.511) (`:264-271`, `:315-317`).

**Screenshot.** What's new 0.4, card 2 ("Sign in from your computer"), captured at the check + K moment (~2.6s into the loop), light and dark.

---

### Icons (`KoanIcons`, `ZenIcon`)

**Icon sets in the app**
1. **KoanIcons** (`ui/icons/KoanIcons.kt:13-294`): Koan's own small set drawn as Compose `ImageVector`s on a **24×24 viewport, 24dp default size**, "so we don't need material-icons-extended" (`:13`). No Material icon library is used (no `material-icons` dependency; only `androidx.compose.material3`, `app/build.gradle.kts:148`). Stroked style (`stroked()`, `:285-294`): stroke **2** viewport units (= 2dp at 24dp; scales with the rendered size, e.g. ≈1.67dp at 20dp, 1.5dp at 18dp, ≈1.17dp at 14dp), **round caps, round joins**, no fill. Painted white and tinted by `Icon(tint = …)`.
   - Exceptions: `StarFilled` (filled + 2-unit stroke, round join, `:79-91`); `Contrast` (2-unit stroked circle r 8.5 + filled half, `:233-242`); `More` (three filled dots r 1.6 at x 5/12/19, `:257-268`); `GitHub` (Octicons `mark-github-16`, MIT, filled, 16-unit viewport rendered at 24dp, `:270-283`; notice in `THIRD_PARTY_NOTICES.md:74-77`). `Retry` is an alias of `Reload` (`:210`).
2. **Zen space icons** (`ui/icons/ZenIcon.kt:23-47`): Zen's selectable icons shipped as **87** `zen_*.xml` vector drawables (512×512 viewport, filled with `@color/zen_icon_fill`; MPL 2.0, `THIRD_PARTY_NOTICES.md:34-43`), or an emoji. Vector drawn at **82%** of the slot, tinted; emoji at `0.78 × size` sp. Used only for space/folder icons, never as UI chrome.
3. **Bitmaps**: app icon `koan_icon.webp` (432×432) and the 3D K `koan_k.webp` (768×762, alpha) in `res/drawable-nodpi`; What's new stills `whatsnew_engines_{dark,light}.webp`.

**Every KoanIcons glyph and where/at what size it is used**
| Icon | Used in (path:line) → rendered size |
|---|---|
| Back | page bar `bar/AddressRow.kt:218` (BarIcon 24dp, T:701, touch 48dp T:700); overview header `overview/SpacePage.kt:218` (HeaderIcon 22dp in 44dp, T:454-455); menu nav row `menu/BrowserMenu.kt:168` (24dp in 48dp); sheet header `sheet/SheetKit.kt:176` and `menu/SiteSettings.kt:72` (22dp, `SheetTokens.TileGlyph` T:855); Settings/ListPage back `settings/SettingsKit.kt:87`, `kit/ListPage.kt:83` (24dp) |
| Forward | menu nav row `menu/BrowserMenu.kt:169` (24dp) |
| Close | bar edit/find `bar/AddressRow.kt:548,652` (24dp); menu stop `menu/BrowserMenu.kt:196` (24dp); tab menu `menu/TabMenu.kt:33` (18dp, `TabMenuTokens.Icon` T:1016); tab card close `overview/TabCard.kt:254` (14dp, `CardTokens.CloseIcon` T:430); search clear `settings/PasswordsPage.kt:148` (16dp); download cancel `downloads/DownloadsScreen.kt:180` (16dp); sign-in close `signin/SignInScreen.kt:230` (20dp) |
| Reload / Retry | menu nav row `menu/BrowserMenu.kt:198` (24dp); search group/chip `search/SearchSheet.kt:159,342` (14dp, `SearchTokens.ChipIcon` T:1068); download retry `downloads/DownloadsScreen.kt:176` (16dp); About chip `settings/AboutPage.kt:118` (13dp) |
| History | overview bar `bar/AddressRow.kt:219` (24dp); menu row `menu/BrowserMenu.kt:145` (18dp, SheetRow `SheetTokens.RowGlyph` T:847); tab menu `menu/TabMenu.kt:34` (18dp) |
| Plus | menu tile `menu/BrowserMenu.kt:109` (22dp); link menu rows `menu/LinkMenu.kt:103,116` (18dp); tab menu `menu/TabMenu.kt:32` (18dp); overview new tab `overview/SpacePage.kt:161` (20dp, `OverviewTokens.NewTabGlyph` T:461); "Add a search engine" `settings/EnginesPage.kt:59` (20dp) |
| Search | bar field `bar/AddressRow.kt:459,623` (18dp, `SearchFieldTokens.Glyph` T:298); menu tile/row `menu/BrowserMenu.kt:116` (22dp), `:142` (18dp); suggestion chip `search/SearchSheet.kt:171` (14dp); list page field `kit/ListPage.kt:104` (20dp); passwords search `settings/PasswordsPage.kt:133` (20dp); Settings row `settings/SettingsScreen.kt:168` (20dp) |
| Pin | tab card `overview/TabCard.kt:233` (14dp, T:432); drawer `bar/SpaceDrawer.kt:227` (16dp, `DrawerTokens.PinnedGlyph` T:787) |
| Folder | drawer folder `bar/SpaceDrawer.kt:245` (22dp, T:785); downloads chip `downloads/DownloadsScreen.kt:92` (16dp); Settings row `settings/SettingsScreen.kt:224` (20dp) |
| ChevronDown | find-in-page next `bar/AddressRow.kt:651` (24dp); drawer folder chevron `bar/SpaceDrawer.kt:266` (16dp, rotates) |
| ChevronRight | Settings rows `settings/SettingsKit.kt:176` and account card `settings/SettingsScreen.kt:283` (20dp); sheet `RowChevron` `sheet/SheetKit.kt:130` (18dp); History group `history/HistoryScreen.kt:260` (18dp, rotates 0→90°); sign-in choice card `signin/SignInPages.kt:202` (18dp) |
| Check | Settings selection `settings/EnginesPage.kt:53`, `settings/SettingsScreen.kt:305,309` (20dp); About chip `settings/AboutPage.kt:116` (13dp); page prompts `menu/PagePrompts.kt:355` (16dp, `PromptTokens.CheckGlyph` T:893), `:458` (18dp); login prompt `menu/LoginPrompts.kt:179` (14dp) |
| Link | menu nav row `menu/BrowserMenu.kt:194` (24dp); link menu `menu/LinkMenu.kt:107,118` (18dp); permission (cross-site cookies) `menu/PermissionPrompt.kt:38` (20dp in prompt `:71`, 18dp in Site settings rows) |
| Star / StarFilled | menu Bookmark tile `menu/BrowserMenu.kt:111` (22dp); Star in search scope chip `search/SearchSheet.kt:342` (14dp) |
| ArrowUp | find-in-page previous `bar/AddressRow.kt:650` (24dp) |
| Share | menu tile `menu/BrowserMenu.kt:115` (22dp); link menu `menu/LinkMenu.kt:109` (18dp); download action `downloads/DownloadsScreen.kt:213` (17dp) |
| Lock | menu Site settings row `menu/BrowserMenu.kt:120` (18dp); nav field padlock `menu/BrowserMenu.kt:182`, `menu/SiteSettings.kt:79` (14dp, `SheetTokens.NavLockGlyph` T:861); permission (protected media) `menu/PermissionPrompt.kt:37` |
| Desktop | menu Desktop site row `menu/BrowserMenu.kt:125` (18dp); History synced mark `history/HistoryScreen.kt:224` (16dp); permission (VR) `menu/PermissionPrompt.kt:40`; sign-in choice card `signin/SignInPages.kt:96` (24dp) |
| StatusBar | Settings "Hide status bar" `settings/SettingsScreen.kt:197` (20dp) |
| Reader | menu `menu/BrowserMenu.kt:130`, `menu/SiteSettings.kt:107` (18dp); Settings "Search suggestions" `settings/SettingsScreen.kt:171` (20dp) |
| Essentials | menu rows `menu/BrowserMenu.kt:135-138` (18dp); link menu `menu/LinkMenu.kt:105` (18dp); search group/scope `search/SearchSheet.kt:161,342` (14dp); Settings "Start page" `settings/SettingsScreen.kt:176` (20dp); About card `settings/AboutPage.kt:84` (20dp) |
| Download | menu row `menu/BrowserMenu.kt:147` (18dp); download prompt `menu/DownloadPrompt.kt:42` (20dp); link menu `menu/LinkMenu.kt:114` (18dp); About chip `settings/AboutPage.kt:117` (13dp); Settings row `settings/SettingsScreen.kt:222` (20dp) |
| Settings (gear) | overview bar `bar/AddressRow.kt:238` (24dp); menu row `menu/BrowserMenu.kt:151` (18dp) |
| Location, Camera, Mic, Database, Globe | permission glyphs `menu/PermissionPrompt.kt:32-41` (20dp on the prompt card `:71`; 18dp in Site settings rows `menu/SiteSettings.kt:98`); Camera also on the viewfinder `signin/SignInPages.kt:260` (24dp); Globe also Settings "Default browser" `settings/SettingsScreen.kt:181` (20dp) and password account row `settings/PasswordsPage.kt:190` (20dp) |
| Pause, Play | download round button `downloads/DownloadsScreen.kt:175,177` (16dp) |
| File | download file tile `downloads/DownloadsScreen.kt:269` (18dp, T:988) |
| Trash | download Delete action `downloads/DownloadsScreen.kt:214` (17dp); History swipe background `history/HistoryScreen.kt:205` (20dp) |
| Open | download Open action `downloads/DownloadsScreen.kt:212` (17dp) |
| Info | Settings "About Koan" `settings/SettingsScreen.kt:226` (20dp) |
| Key | Settings "Passwords" and site rows `settings/SettingsScreen.kt:204`, `settings/PasswordsPage.kt:103` (20dp); login prompt `menu/LoginPrompts.kt:74` (18dp = 14 + 8/2); sign-in choice card `signin/SignInPages.kt:98` (24dp) |
| Contrast | Settings "Theme" `settings/SettingsScreen.kt:189` (20dp) |
| Shield | Settings uBlock `settings/SettingsScreen.kt:212,215` (20dp); menu uBlock card `menu/UBlockPage.kt:42,44` (18dp) |
| More (⋯) | page bar `bar/AddressRow.kt:237` (24dp) |
| GitHub | What's new changelog button `tour/TourOverlay.kt:291` (20dp) |
| Book | defined (`icons/KoanIcons.kt:243`) but **not used** anywhere |

Rendered sizes come from: `BarTokens.Icon` 24dp (T:701), `SheetTokens.RowGlyph` 18dp (T:847), `SheetTokens.TileGlyph` 22dp (T:855), `SettingsTokens.RowGlyph` 20dp (T:922), `SearchTokens.ChipIcon` 14dp (T:1068), `TabMenuTokens.Icon` 18dp (T:1016), and the multipliers noted per row (e.g. `RowGlyph·0.8` = 16dp, `RowGlyph·0.85` = 17dp).

**Icon tints** are always neutral palette tokens (`TextPrimary`, `TextSecondary`, `Icon` (light `#6E6E73`, dark `#98989D`, T:100/146), `FieldHint`, `TextDisabled` when disabled, `OnPrimary` on fills, `Attention` for an insecure padlock `menu/SiteSettings.kt:79`). No icon is ever painted with the brand gradient.

**Screenshot.** A contact sheet is better rendered from code than captured; if captured: the page bar (Back, More), the ⋯ menu (tiles + rows), Settings root (row glyphs), Downloads expanded row (Open/Share/Trash).

### Brand gradient: where it is and is not used in code

Definition: `BrandTokens.Colors` = Pink `#FF5FA2` → Violet `#B46BFF` → Blue `#4F8DFF` (T:241-254); helpers `BrandHorizontal`, `brandOutline` (1.5dp), `BrandSpinner` (`ui/theme/Brand.kt:30-62`). Rule comment: T:196-201; spec `docs/UI_SPEC.md:345-361`.

**Used (complete list from a grep of `BrandTokens|BrandHorizontal|brandOutline|BrandSpinner`):**
| Where | How | Code |
|---|---|---|
| Page load line | horizontal gradient over the filled part, two glow passes + shimmer | `ui/bar/KoanBar.kt:268-280` |
| "@" search ring + cursor | sweep gradient (closed loop) turning; gradient cursor | `ui/bar/SearchRing.kt:55`, `ui/bar/AddressRow.kt:503` |
| Toggles ON | horizontal gradient track @ 0.9 | `ui/kit/KoanToggle.kt:68` |
| Syncing spinner | 270° sweep arc | `ui/theme/Brand.kt:37-62`; used `ui/settings/SettingsScreen.kt:289`, `ui/bar/SpaceDrawer.kt:290`, `ui/overview/SpacePage.kt:262`, `ui/browser/PullRefresh.kt:154` |
| Download progress bars | `BrandHorizontal` fill | `ui/downloads/DownloadsScreen.kt:163` |
| Download-done toast ("moment") | `brandOutline` | `ui/kit/KoanToast.kt:176` |
| Account card, About "Built to pair with Zen" card | `brandOutline` 1.5dp | `ui/settings/SettingsScreen.kt:248`, `ui/settings/AboutPage.kt:80` |
| First-run Connect card / pill | `brandOutline` | `ui/overview/FirstRun.kt:49,64` |
| Settings / History / Downloads wash | 3 radial spots @ 0.20 light / 0.14 dark | `ui/settings/SettingsKit.kt:51-62`, `ui/kit/ListPage.kt:70` |
| Tour title glow | 3 radial spots @ 0.16 / 0.12 | `ui/tour/TourOverlay.kt:375-387` |
| Welcome / What's new intro backdrop | full-sheet linear gradient + vortex | `ui/tour/Welcome.kt:267-281` |
| Sign-in scene (tour) | mist, scan line, check disc, violet glows/sparkles | `ui/tour/SignInScene.kt:107-117,191,205,223,266` |
| Sign-in Choice page | faint breathing glow behind the K; violet sparkles in light mode | `ui/signin/SignInPages.kt:124-126,158-172` |
| Find in page highlight (Gecko pref) | Brand Blue only, rgba @ 0.32 for matches, solid for the current | `engine/GeckoPrefs.kt:12-24` |

**Not used (by code and stated rule):** primary buttons (`ui/kit/Buttons.kt:21`), the Recommended badge, Mozilla's pages, the sign-in header/progress line (neutral `TextSecondary`, `ui/signin/SignInScreen.kt:206-207,248`), password fields and Passwords pages, destructive actions, the current tab/space marker, body text, text cursors in Settings fields (solid `TextPrimary`), icons, and web content.

---

### Spec vs code disagreements (brief)
- `docs/UI_SPEC.md:26` lists Settings as "account, sync, search engine, write-back toggle, add-ons, about". Code: sections General / Display / Privacy / Koan (+ Developer in debug); **no write-back toggle** on the Settings page (`ui/settings/SettingsScreen.kt:162-236`).
- `docs/UI_SPEC.md:391-393` typography "titles 20sp bold, card titles 16sp semibold, body 14sp", "Inter or Google Sans Flex if bundled": Settings titles are **34sp**, list-page titles **23.8sp**, tour titles 22sp / welcome 30sp; no font is bundled.
- `docs/UI_SPEC.md:394` "cards 28dp": settings cards are **22dp**, choice cards 22dp, day cards 18dp, tour frame 30dp; the floating card and tour sheet match (28dp / 32dp top).
- `docs/UI_SPEC.md:371-372` "Destructive = red text only … never a red fill": the History swipe-to-remove background is a full `Destructive` fill (`ui/history/HistoryScreen.kt:201`).
- `docs/UI_SPEC.md:355` allows only "the faint glow behind the K" on sign-in UI; code also draws **Brand Violet sparkles** around that K in light mode (`ui/signin/SignInPages.kt:124`).

### Values not resolvable from repo code
- History swipe-to-remove dismiss threshold and velocity: Material 3 `SwipeToDismissBox` defaults (not overridden; `ui/history/HistoryScreen.kt:187`).
- Tour `HorizontalPager` swipe fling/snap thresholds: Compose Foundation defaults (`ui/tour/TourOverlay.kt:156`).
- Pressed states: Material `clickable` default ripple (colour/alpha from the Material 3 theme, not Koan tokens).
- `LazyColumn` `animateItem()` specs in History: Compose defaults.


## 4. Screens

Every main screen, light and dark. Phone: 1208×2624 px (403×875dp).


