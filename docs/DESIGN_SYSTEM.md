# Koan design system

The design language of **Koan**, an Android browser built around Zen Browser's Spaces, as it is in
the code on `dev`: v0.4.0 plus what was merged since (library selection mode and swipes, page
translation, "Open in app" and duplicate-download cards, Back closing a tab, toast hand-over, Zia
icons, the video player, private mode). Every value is read from the code; references are file
paths from the repository root, and token names (`Object.Member`) in
`app/src/main/java/dev/nerzouille/koan/ui/theme/Tokens.kt`. Where the code and `docs/UI_SPEC.md`
disagree, the code is what ships and the difference is noted. The video player has its own spec:
`docs/VIDEO_PLAYER.md`.

The files this document points to live in `design-export/`, which is local only (gitignored:
screenshots come from the owner's phone). On GitHub the folder does not exist, so the images and
section 4 are empty there. The export is older than this document (v0.4.0 + the design audit).

| Folder | Contents |
|---|---|
| `tokens/tokens.json` | All foundations: colours (light, dark, usage), brand, typography, spacing, radii, sizes, motion, and every component token with its line |
| `brand/` | App icon, 3D K, monochrome K (SVG + PNG), 24-grid K |
| `icons/` | 40 of Koan's icons as SVG (`currentColor`), `_overview.png` (older export: the code now has 62) |
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
- Dark: base `#121214` (since 2026-11-11, was `#1C1C1E`), surfaces `#2C2C2E`, dock `#232326`. Back to front: Base < Dock < Card < GroupedRow / SegmentPill.
- Cards stay neutral: the page thumbnails are the colour.
- Destructive actions are red **text** only, never a red fill.
- Quiet sync notices use amber (`Attention`), never red.

| Token | Light | Dark | Where it is used |
|---|---|---|---|
| `Base` | `#F5F5F7FF` | `#121214FF` | Page background behind everything, and what the stack fades to above the dock. |
| `Glass` | `#EBEBEFFF` | `#FFFFFF14` | Small surfaces on Base: the header's space icon box, essentials tiles, pinned list. |
| `TextPrimary` | `#1C1C1EFF` | `#FFFFFFFF` |  |
| `TextSecondary` | `#66666BFF` | `#FFFFFF9E` |  |
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
| `Destructive` | `#C42A20FF` | `#FF8077FF` | Destructive actions: red text only, never a red fill (one exception: History's swipe-to-remove background). ≥ 4.5:1 on every surface. |
| `GroupedRow` | `#F2F2F5FF` | `#FFFFFF12` | Grouped rows on a sheet or a screen (menu, settings). |
| `ToggleOff` | `#848489FF` | `#8E8E93FF` | Off state of a control: toggle track, unselected radio ring. ≥ 3:1 against every row surface. |
| `FocusRing` | `#B4B4BAFF` | `#FFFFFF4C` | The address input's ring while typing (neutral; "@" turns it into the brand gradient). |
| `Dock` | `#FFFFFFFF` | `#232326FF` | The dock: the bottom bar's own surface (also the drawer sheet). |
| `DockShadow` | `#3C281E12` | `#00000073` | Soft shadow the dock casts upward over the content. |
| `DrawerHandle` | `#00000029` | `#FFFFFF33` |  |
| `BarField` | `#F1F1F4FF` | `#FFFFFF14` |  |
| `SegmentPill` | `#FFFFFFFF` | `#4A4A4DFF` | The chosen option's pill in a segmented selector (on a [Glass] track). |
| `FieldHint` | `#66666BFF` | `#FFFFFF99` |  |
| `Skeleton` | `#0000000F` | `#FFFFFF12` |  |
| `IconBacking` | `—` | `#E9E9EEFF` | Behind favicons too dark to read on dark surfaces; Unspecified = never needed. |
| `Notice` | `#F2F2F5F2` | `#2C2C2EF2` |  |
| `Attention` | `#8A5C00FF` | `#E8B45AFF` | Quiet sync notices and "not secure": amber, never red. ≥ 4.5:1 as text. |
| `DrawerScrim` | `#1C1C2829` | `#00000073` | Over the overview while the drawer is open, at full open. |
| `DrawerScrimNoBlur` | `#1C1C2852` | `#0000009E` | The same without blur behind the drawer (Android 11, DeviceProfile.liveBlur): stronger. |
| `DrawerShadowNoBlur` | `#1C1C2829` | `#00000073` | Without blur, the drawer sheet also casts a taller shadow over the cards, at full open. |
| `SheetScrim` | `#1C1C284C` | `#00000080` | Over the page while a sheet or a floating card is open (the menu, prompts). |
| `FindCurrent` | `#1554CBFF` | `#1554CBFF` | Find in page, the current match (Gecko `ui.textSelectAttentionBackground`). |
| `FindCurrentText` | `#FFFFFFFF` | `#FFFFFFFF` | Its text (`ui.textSelectAttentionForeground`). |
| `FindMatch` | `#D0DDF5FF` | `#1C396EFF` | Every other match (`ui.textHighlightBackground`). |
| `FindMatchText` | `#000000FF` | `#FFFFFFFF` | Their text (`ui.textHighlightForeground`), so Gecko never forces its own white. |
| `CardShadowColor` | `#1C1C3024` | `#00000099` | Key shadow of stack cards (ambient and spot); alpha × (1 − 0.1 d), min 0.6, behind the focus. |
| `ContactShadow` | `#1C1C302E` | `#000000A6` | Short, soft shadow a card casts on the card behind it, just above its top edge. |
| `ContactShadowLength` (dp) | 16 | 18 | How far above a card's top edge its contact shadow starts. |
| `CardHairline` | `#1C1C301A` | `#FFFFFF1F` | 1dp line all around a stack card, drawn over its depth dim so back cards keep their edge. |
| `CardRim` | — (transparent) | `#FFFFFF1A` | Dark only: 1dp light line inside a stack card's top edge. |
| `SyncDot` | `#C0841AFF` | `#E8B45AFF` | The overview header's "sync failed" dot. |
| `DimColor` | `#F5F5F7FF` | `#000000FF` | What back cards fade towards (light: Base, a haze; dark: black). |
| `PrivateAccent`, `PrivatePill`, `PrivateInk` | — | — | Set only in the private palettes (see below). |

Private mode has two more palettes, `KoanPalette.Dusk` (light theme) and `KoanPalette.Night` (dark
theme), with the same fields; their main values are in **3.B › Private mode**. For example
`PrivateAccent` `#4B3FD6` / `#9C8CFF`, `PrivatePill` `#4B3FD629` / `#9C8CFF33`, `PrivateInk`
`#2E2399` / `#CFC6FF`, `DimColor` `#DEDAF0` / `#050412`.

#### Contrast

Text ≥ **4.5:1**, controls ≥ **3:1** (WCAG 2.x), measured against the worst surface each token is
drawn on: light = White, `Base`, `GroupedRow`, `BarField`, `Glass`; dark = `Base`, `Card`, `Dock`,
and `GroupedRow` / `Glass` composited over them. Changed in the 2026-11-10 design audit:

| Token | Light before → after | Light ratio before → after | Dark before → after | Dark ratio before → after |
|---|---|---|---|---|
| `FieldHint` ("Search or type URL", search glyph) | `#8E8E93` → `#66666B` | 2.89 on BarField (2.74 on Glass) → **4.80** worst | white 60% (unchanged) | 5.21 |
| `Attention` ("not secure", amber dots) | `#C0841A` → `#8A5C00` | 2.94 (2.87 on GroupedRow) → **4.89** | `#E8B45A` (unchanged) | 5.93 |
| `Destructive` (red text) | `#E5342B` → `#C42A20` | 3.87 on GroupedRow → **4.77** | `#FF5A50` → `#FF8077` | 3.65 on GroupedRow over Card → **4.60** |
| `TextSecondary` | `#6E6E73` → `#66666B` | 4.27 on Glass → **4.80** | white 62% (unchanged) | 5.44 |
| `ToggleOff` (toggle OFF track) | `#E5E5EA` → `#848489` | ~1.1 → **3.13** | `#3A3A3C` → `#8E8E93` | ~1.1 → **3.44** |
| Login sheet radio, unselected | `GroupedRow` fill on a `GroupedRow` row → 2dp `ToggleOff` ring | 1.0 → **3.13** | same | 1.0 → **3.44** |

The white toggle knob stays readable on the darker OFF track (3.6:1 light, 3.3:1 dark).

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
| Settings' wash behind the top of the screen (320dp, 20% light / 16% dark, also on About); the account card outline (1.5dp), **not while it says "Couldn't sync"** | Behind web content |
| First-run "Connect your Zen Spaces" card outline (1.5dp) | Primary buttons (always black / white) |
| Find in page: the find field's ring (1.5dp) while find is open; the matches in the page in Koan's find blues (`FindCurrent`, `FindMatch`) | Static, non-tappable cards (About "Built to pair with Zen" is a plain card) |
| "Moment" toasts (outline) | A failure state (the account card while sync fails) |
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
| Stack dim | haze towards `DimColor` (= `Base`), ×1.25, max 0.85 | towards black, ×1, max 1.0 (as in v0.4.0) | Back cards in the depth stack (see 3.B Depth stack) |

### 1.2 Typography

- **Font:** the system default sans-serif (Compose `FontFamily.Default`). Koan bundles no font; on
  the owner's phone that is the HyperOS system font.
- **Base style** (`ui/theme/Type.kt`, `KoanTypography`, given to `MaterialTheme`): every Material 3
  `Text` inherits it. **Line height 1.33em** (relative to the font size, so it scales with any
  size a call sets), **letter spacing 0**, line-height style centred, no trim. Before the audit the
  base was Material's `bodyLarge`: a fixed 24sp line height and 0.5sp tracking for every size, so
  a 30–34sp title that wrapped overlapped itself and 12sp helper text was spaced like 16sp.
- **Title styles** (`KoanType`, used with each feature's size token):

| Style | Use | Weight | Line height | Letter spacing |
|---|---|---|---|---|
| body (default) | everything ≤ 16sp | as set | 1.33em | 0 |
| `KoanType.headline(size)` | 17–20sp: card titles (`SheetTokens.CardTitleSize` 18), sheet headers (`HeaderTitleSize` 17), empty-state titles | SemiBold | 1.25em | −0.006em |
| `KoanType.title(size)` | 20–30sp: page bar title (`SettingsTokens.BarTitleSize` 22), tour (22), welcome (30), sign-in (17), overview space name (20), first run (20), About app name (30) | Bold | 1.18em | −0.012em |
| `KoanType.largeTitle(size)` | settings large title (`SettingsTokens.TitleSize` 34), up to 2 lines | Bold | 1.12em | −0.016em |

- **Weights in use:** Normal 400, Medium 500, SemiBold 600, Bold 700. The weight per component is
  given in each component's section. Buttons and destructive actions are SemiBold.

Type scale (every text size token, sp):

| Size (sp) | Tokens (where it is used) |
|---|---|
| **34** | `SettingsTokens.TitleSize` |
| **30** | `TourTokens.WelcomeTitleSize`, `SettingsTokens.AppNameSize` |
| **26** | `SignInTokens.HeroSize` |
| **22** | `TourTokens.TitleSize`, `SettingsTokens.BarTitleSize` |
| **20** | `OverviewTokens.TitleSize`, `FirstRunTokens.TitleSize` |
| **18** | `OverviewTokens.EmptyTitleSize`, `SheetTokens.CardTitleSize`, `SettingsTokens.AvatarTextSize` |
| **17** | `TourTokens.WelcomeLineSize`, `SignInTokens.TitleSize`, `BarTokens.FieldTextSize`, `SheetTokens.HeaderTitleSize` |
| **16** | `ButtonTokens.TextSize`, `SignInTokens.OutdatedSize`, `SignInTokens.ModeSize`, `SheetTokens.RowTitleSize`, `SheetTokens.NavFieldTextSize`, `SettingsTokens.RowTitleSize` |
| **15** | `TourTokens.LineSize`, `FirstRunTokens.ButtonSize`, `SignInTokens.BodySize`, `DrawerTokens.PinnedTitleSize`, `ToastTokens.TextSize`, `TabMenuTokens.TextSize`, `StartPageTokens.ContinueTitleSize`, `SearchTokens.TitleSize` |
| **14.5** | `CardTokens.TitleSize` (tab card title, line 18sp) |
| **14** | `CardTokens.PlaceholderDomainSize`, `OverviewTokens.EmptyBodySize`, `FirstRunTokens.BodySize`, `SignInTokens.NoticeSize`, `SignInTokens.LinkSize`, `BarTokens.SwitcherNameSize`, `SheetTokens.RowTrailingSize`, `SheetTokens.CardTextSize`, `SettingsTokens.RowValueSize`, `SettingsTokens.SegmentTextSize`, `SettingsTokens.VersionSize`, `SearchTokens.ChipTextSize` |
| **13** | `SignInTokens.SubtitleSize`, `SignInTokens.StepNumberSize`, `DrawerTokens.FolderCountSize`, `DrawerTokens.StatusTextSize`, `SheetTokens.TileLabelSize`, `SettingsTokens.SectionTitleSize`, `SettingsTokens.ChipTextSize`, `DownloadTokens.ChipTextSize`, `StartPageTokens.ContinueSubtitleSize`, `StartPageTokens.LabelSize` |
| **12** | `BarTokens.TabsCountSize`, `SheetTokens.RowSubtitleSize`, `SettingsTokens.RowSubtitleSize`, `SettingsTokens.FooterSize`, `ToastTokens.DetailSize`, `SearchTokens.LabelSize`, `SearchTokens.UrlSize` |
| **11.5** | `CardTokens.HostSize` (tab card host line, line 14sp) |
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
| **14** | `DrawerTokens.EssentialRadius`, `SettingsTokens.RowRadius` |
| **pill** | `KoanField` (all fields outside the dock), buttons, the dock's address field |
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
| Stack card shadow (`CardElevation`, `CardShadowColor`) | 12dp, `#1C1C30` at 14% | 20dp, black at 60% |
| Contact shadow (a card on the card behind it, just above its top edge) | `#1C1C30` at 18%, 16dp long, at most 35% of the visible sliver | black at 65%, 18dp |
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

All 1dp lines share `SharedTokens.Hairline` (`SheetTokens.Divider`, `SettingsTokens.Divider`, `CardTokens.Hairline`, `SignInTokens.CardBorder`, `TourTokens.FrameBorder` point at it); grouped blocks share `SharedTokens.GroupRadius` 18dp (sheet, search and Downloads groups).

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

Motion helpers in `ui/kit` (use them instead of writing the pattern again):

- `rememberPresence(open, enter, exit, snapIn, startShown)` (`kit/Presence.kt`): anything that shows
  and hides with a 0…1 animation. `progress` is read in layer / draw / layout lambdas only;
  `composed` decides whether it is in the composition and changes only at the two ends (rule 9).
  A second overload wraps an `Animatable` the caller drives itself (the toast). `startShown`
  starts at the first target, like `animateFloatAsState`.
- `animationsOff(context)` / `rememberAnimationsOff()` and `frameClock { deltaMs -> }`
  (`kit/MotionKit.kt`): the system's "Remove animations", and an idle clock for illustrations.

### 1.9 Icons

Koan's own stroked icon set (`ui/icons/KoanIcons.kt`): **24×24 grid, 2-unit stroke, round caps and
joins, single colour** (tinted by the caller). No Material icons library. Filled exceptions:
`star-filled`, `contrast` (half filled), `more` (three dots), and GitHub's mark (Octicons, MIT, only
on What's new's changelog card; not exported), and the video player's filled glyphs (`PlayFilled`,
`PauseFilled`, `SkipBackFilled`, `SkipAheadFilled`, `ReplayFilled`). The code has **62** icons
(`ImageVector`s, `Retry` an alias of `Reload`); the export in `icons/` is older and has 40 of them
as SVG (`currentColor`), overview sheet `icons/_overview.png`.

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

All values read from the code on branch `dev`. Paths are relative to the repo root.
Abbreviations used below:

- `bar/` = `app/src/main/java/dev/nerzouille/koan/ui/bar/`
- `menu/` = `app/src/main/java/dev/nerzouille/koan/ui/menu/`
- `browser/` = `app/src/main/java/dev/nerzouille/koan/ui/browser/`
- `ui/` = `app/src/main/java/dev/nerzouille/koan/ui/`

### Colour tokens used in this part (KoanPalette)

Values from `KoanPalette.Light` and `KoanPalette.Dark` (`ui/theme/Tokens.kt`). "@ n%" = that colour at alpha n.

| Token | Light | Dark | Used for |
|---|---|---|---|
| `Dock` | `#FFFFFF` | `#232326` | dock surface, drawer sheet, dock popups |
| `DockShadow` | `#3C281E @ 7%` | `#000000 @ 35%` | upward shadow on the dock's top edge |
| `BarField` | `#F1F1F4` | `#FFFFFF @ 8%` | address pill fill |
| `FieldHint` | `#66666B` | `#FFFFFF @ 60%` | "Search or type URL", search glyph, completion title |
| `FocusRing` | `#B4B4BA` | `#FFFFFF @ 30%` | address input ring while typing |
| `TextPrimary` | `#1C1C1E` | `#FFFFFF` | host text, row titles, space icons |
| `TextSecondary` | `#66666B` | `#FFFFFF @ 62%` | find count, status row, folder glyphs, pin glyph, active dot, download progress arc |
| `TextDisabled` | `#000000 @ 22%` | `#FFFFFF @ 19%` | disabled back icon, disabled popup row |
| `Icon` | `#6E6E73` | `#98989D` | bar icons, tab-count badge border and number, popup icons |
| `Glass` | `#EBEBEF` | `#FFFFFF @ 8%` | essentials tiles, pinned group, download ring track |
| `DrawerHandle` | `#000000 @ 16%` | `#FFFFFF @ 20%` | drawer handle bar |
| `DrawerScrim` | `#1C1C28 @ 16%` | `#000000 @ 45%` | overview dim behind open drawer (live blur devices) |
| `DrawerScrimNoBlur` | `#1C1C28 @ 32%` | `#000000 @ 62%` | same on Android 11 (no blur) |
| `DrawerShadowNoBlur` | `#1C1C28 @ 16%` | `#000000 @ 45%` | taller sheet shadow on Android 11 |
| `Attention` | `#8A5C00` | `#E8B45A` | amber dot on a sync-problem status row |
| `DefaultAccent` | `#5B6CFF` | `#8EA2FF` | space accent when its Zen colour is grey |
| `PillAlpha` (float) | 0.14 | 0.18 | alpha of the active space pill tint |
| `InkLightness` (float) | 0.38 | 0.80 | HSL lightness of the space name colour |
| `AccentLightness` (range) | 0.45–0.62 | 0.62–0.74 | HSL lightness clamp of the space accent |

Brand gradient (`BrandTokens`, theme-independent): Pink `#FF5FA2` → Violet `#B46BFF`
(`BrandTokens.Violet`) → Blue `#4F8DFF`, list `Colors`; `BrandHorizontal` =
horizontal gradient of those (`ui/theme/Brand.kt`).

Haptics used: `HapticFeedbackConstants.CLOCK_TICK` (light tick) and `LONG_PRESS`.
"Touch slop" and "long-press timeout" are Android's `ViewConfiguration` values (not set in Koan).

---

### Dock (KoanBar) + address pill (AddressRow, SearchRing, CleanTap)

**Purpose:** the one bottom bar shared by the page view and the overview: an opaque dock with the
address row; in the overview it grows a space-switcher row and further into the drawer.

**Anatomy (page view, left → right inside the address row, `bar/AddressRow.kt`)**
1. Dock surface (drawn, not a layout box): rounded-top rect in `Dock`, upward shadow, page load line on its top edge (`bar/KoanBar.kt`).
2. Back button (overview: crossfades to History) (`bar/AddressRow.kt`).
3. Address pill (host label; overview: "Search or type URL") (`bar/AddressRow.kt`).
4. Tab counter button (see next component) (`bar/AddressRow.kt`).
5. ⋯ More button (overview: crossfades to Settings) (`bar/AddressRow.kt`).
6. Optional download ring at the end of the pill (`bar/AddressRow.kt`).
7. Overview only, above the address row: switcher row (60dp) with the drawer handle centred at its top (`bar/KoanBar.kt`).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Address row height | 68dp | `BarTokens.AddressRowHeight`, used `bar/KoanBar.kt` |
| Switcher row height (overview / scrub) | 60dp | `BarTokens.SwitcherRowHeight`, `bar/KoanBar.kt` |
| Bottom margin under the rows (above nav bar / IME inset) | 6dp | `BarTokens.BottomMargin`, `bar/KoanBar.kt` |
| Bar zone, page view | 68 + 6 = 74dp (+ nav-bar inset) | `bar/KoanBar.kt` |
| Bar zone, overview | 60 + 68 + 6 = 134dp (+ inset) | `bar/KoanBar.kt` |
| Row horizontal padding (dock margin) | 6dp | `BarTokens.RowPadding`, `bar/AddressRow.kt` |
| Gap between row items | 2dp | `BarTokens.RowGap`, `bar/AddressRow.kt` |
| Button touch target | 48dp circle | `BarTokens.IconTouch`, `bar/AddressRow.kt` |
| Button glyph | 24dp | `BarTokens.Icon`, `bar/AddressRow.kt` |
| Pill height | 50dp, fully rounded (`CircleShape`) | `BarTokens.FieldHeight`, `bar/AddressRow.kt` |
| Pill width | fills the rest: screen − 2×6 − 3×48 − 3×2 = screen − 162dp (derived) | `bar/AddressRow.kt` (`weight(1f)`) |
| Pill horizontal text padding | 18dp | `BarTokens.FieldPadding`, `bar/AddressRow.kt` |
| Pill text | 17sp, `FontWeight.Medium`, 1 line, ellipsis, centred | `BarTokens.FieldTextSize`, `bar/AddressRow.kt` |
| Dock top corner radius in overview | 24dp | `BarTokens.DockRadius` |
| Dock top corner radius as drawer | 28dp | `DrawerTokens.TopRadius` |
| Dock corner radius in page view | 0 (square) | `bar/KoanBar.kt` (`r` = 24·row·(1−d) + 28·d; row = 0 in page view) |
| Upward shadow height | 28dp, vertical gradient transparent → `DockShadow`, also spans the corner radius | `BarTokens.DockShadowHeight`, `bar/KoanBar.kt` |
| Drawer handle | bar 36 × 4dp, pill-shaped, 8dp from top, `DrawerHandle` colour; touch area 72 × 24dp | `BarTokens.HandleWidth`, `BarTokens.HandleHeight`, `SheetTokens.HandleTop`, `BarTokens.HandleTouch`, `kit/DragHandle.kt` |

**Page load line** (page view only; fades out as the switcher row shows, `bar/KoanBar.kt`)

| Property | Value | Ref |
|---|---|---|
| Height | 2dp, on the dock's top edge, from the left | `BarTokens.ProgressHeight`, `bar/KoanBar.kt` |
| Fill | `BrandTokens.Colors` horizontal gradient spanning the filled width (blue end leads) | `bar/KoanBar.kt` |
| Glow | 8dp band at alpha 0.35 × 0.35, plus a 4dp band at alpha 0.35 | `BarTokens.ProgressGlow`, `BarTokens.PROGRESS_GLOW_ALPHA`, `bar/KoanBar.kt` |
| Start value | 6% as soon as a load begins | `BarTokens.PROGRESS_START`, `bar/KoanBar.kt` |
| Ease to progress | tween 800ms, `CubicBezierEasing(0.2, 0.8, 0.3, 1)`, never backwards | `BarTokens.PROGRESS_MS`, `bar/KoanBar.kt` |
| Fade in | 175ms (350 / 2) | `bar/KoanBar.kt` |
| Finish | runs to 100% in 250ms, then fades out over 350ms | `BarTokens.PROGRESS_FINISH_MS`, `BarTokens.PROGRESS_FADE_MS`, `bar/KoanBar.kt` |
| Shimmer | 72dp streak, white @ 55% in the middle, linear 1300ms loop, clipped to the filled part | `BarTokens.ShimmerWidth`, `BarTokens.SHIMMER_MS`, `BarTokens.SHIMMER_ALPHA`, `bar/KoanBar.kt` |

**States**

| State | What changes | Ref |
|---|---|---|
| Default (page view) | back · host pill · counter · ⋯ ; host in `TextPrimary`; empty url shows "Search or type URL" in `FieldHint` | `bar/AddressRow.kt` |
| Pressed (buttons, pill) | `LocalIndication` (Material ripple) on press, clipped to the 48dp circle / pill; a tap only fires if the finger lifts before the long-press timeout without passing the touch slop and no bar drag took it (`cleanTap`) | `bar/CleanTap.kt` |
| Long press (back, ⋯, counter, History) | after the long-press timeout held still: `LONG_PRESS` haptic, opens its action; the rest of the gesture is consumed | `bar/CleanTap.kt`, `bar/AddressRow.kt` |
| Disabled (back with no history) | glyph `TextDisabled`, no tap, no long press | `bar/AddressRow.kt` |
| Overview (morph 0 → 1) | back → History, ⋯ → Settings, host → "Search or type URL" (all alpha crossfades); counter stays. Tap pill = new tab with search. Long press History = Recently closed; ⋯'s long press (page view) = Settings | `bar/AddressRow.kt`, `ui/KoanRoot.kt` |
| Editing (address input) | see below | `bar/AddressRow.kt` |
| Find in page | find bar replaces the row | `bar/AddressRow.kt` |
| Downloading | progress ring at the pill's end | `bar/AddressRow.kt` |
| Tab swipe | host slides out, neighbour's favicon + host slides in | `bar/AddressRow.kt` |
| Space mode (long press on pill) | host gives way to the current space's icon + name, 140ms crossfade | `bar/AddressRow.kt`, `Motion.SPACE_MODE_FADE_MS` |
| Hidden by scroll | slides down by up to its height | `browser/DockState.kt` |
| Keyboard for a page field | dock alpha 0 (not drawn) | `bar/KoanBar.kt`, `browser/DockState.kt` |
| Fullscreen video | the bar is not composed at all | `ui/KoanRoot.kt` |

**Address input (editing)** — `bar/AddressRow.kt`
- Pill 50dp, `BarField`, start padding 18 − 10/2 = 13dp, end 18dp (`bar/AddressRow.kt`).
- Search glyph 18dp in `FieldHint` (`SearchFieldTokens.Glyph`, `bar/AddressRow.kt`), then 10dp gap (`SearchFieldTokens.GlyphGap`, `bar/AddressRow.kt`).
- Text 17sp `TextPrimary`, regular weight (not Medium); hint "Search or type URL" in `FieldHint` (`bar/AddressRow.kt`). Keyboard: URI, IME action Go, no autocorrect (`bar/AddressRow.kt`).
- Inline completion: selected completed tail; " — Title" after it in `FieldHint` (`bar/AddressRow.kt`).
- Text scrolls inside the field, cursor kept 40dp from the edges (`SearchFieldTokens.CursorEdge`, `bar/AddressRow.kt`).
- Trailing Close button (48dp / 24dp glyph) (`bar/AddressRow.kt`). Back, keyboard dismiss or focus loss cancels (`bar/AddressRow.kt`).
- Focus ring (`searchRing`, `bar/SearchRing.kt`): neutral 1.5dp stroke in `FocusRing` while typing (`SearchFieldTokens.Ring`, `bar/AddressRow.kt`).
- "@" mode (text starts with "@"): brand sweep gradient flows in from the left around both halves of the pill, stroke grows 1.5 → 2dp (`SearchFieldTokens.AtRing`, `bar/SearchRing.kt`), halo 1.5dp each side at alpha 0.14 (`SearchFieldTokens.Halo`, `SearchFieldTokens.HALO_ALPHA`, `bar/SearchRing.kt`); neutral ring fades as `1 − f²` (`bar/SearchRing.kt`). Flow spring dampingRatio 1, stiffness 140 (`bar/AddressRow.kt`). Gradient turns 48°/s with wobbles 14° / 3.1s and 10° / 4.7s (`SearchFieldTokens`, `bar/SearchRing.kt`); turning off when animator scale is 0 (`bar/AddressRow.kt`). Cursor becomes `BrandHorizontal` (`bar/AddressRow.kt`).

**Find bar** — `bar/AddressRow.kt`: same 50dp `BarField` pill and padding as the input; search glyph 18dp + 10dp gap; hint "Find in page" (`FieldHint`, 17sp); match count ("3/12", "No matches") in `TextSecondary` at 12sp × 1.15 = 13.8sp (`bar/AddressRow.kt`); then three 48dp buttons: ArrowUp (previous), ChevronDown (next), Close. IME action Search = next.

**Download ring** — `bar/AddressRow.kt`: own 48dp circular hit target at the pill's end (text area loses 48 − 18 = 30dp of end padding, `bar/AddressRow.kt`); ring 18dp, stroke 2dp (`DownloadTokens.Ring`, `DownloadTokens.RingStroke`); track `Glass` full circle, progress arc `TextSecondary` from 12 o'clock, round cap; unknown size = fixed 90° arc. Tap: `CLOCK_TICK` + opens Downloads. Hidden in the overview (`bar/AddressRow.kt`).

**Swipe labels in the pill** — `bar/AddressRow.kt`: neighbour = favicon (or space icon) 20dp (`BarTokens.SwitcherIcon`) + 7dp gap (`BarTokens.SwitcherNameGap`) + name 17sp Medium `TextPrimary`. Labels travel 0.6 × text width (`bar/AddressRow.kt`); outgoing alpha `1 − 4|f|`, incoming `4(f − 0.18)` clamped (`bar/AddressRow.kt`).

**Light vs dark:** only the palette tokens above change (`Dock`, `DockShadow`, `BarField`, `FieldHint`, `FocusRing`, `Icon`, `TextDisabled`…). Dark shadow is much stronger (black 35% vs warm 7%). Geometry is identical. On Android 11 (no live blur) the drawer adds a 56dp `DrawerShadowNoBlur` shadow (`bar/KoanBar.kt`, `DrawerTokens.NoBlurShadowHeight`).

**Motion and gesture thresholds** (`bar/BarGestures.kt`)
- Axis chosen once the finger passes touch slop; horizontal if |dx| > |dy| (`bar/BarGestures.kt`).
- Page view: swipe up = overview opens under the finger; 220dp of travel is the whole morph (`BarTokens.OverviewDrag`, `ui/KoanRoot.kt`); release faster than 600dp/s goes regardless of position (`BarTokens.OverviewFling`, `bar/BarGestures.kt`). Swipe down on the page's dock = nothing (`bar/BarGestures.kt`). Overview spring dampingRatio 1, stiffness 600 (`Motion.Overview`).
- Page view horizontal swipe = tab swipe (see TabSwipe below); long press on the pill only = space scrub (`bar/BarGestures.kt`).
- One `CLOCK_TICK` per committed step, on release (`bar/BarGestures.kt`).
- Morph: switcher row alpha `(p − 0.35) / 0.65`, slides 12dp (`BarTokens.MorphSlide`, `bar/KoanBar.kt`); buttons and pill labels pure alpha crossfade; no relayout per frame.
- **Hide on scroll** (`browser/DockState.kt`): offset follows page scroll 1:1, 0 … dock height (bar zone + nav inset, `browser/BrowserScreen.kt`) (`browser/DockState.kt`). On finger up: if the page moved more than touch slop since down, finish in that direction; else snap to the nearer end (`browser/DockState.kt`). Settle spring dampingRatio 1, stiffness 800 (`Motion.DockSettle`). Stays shown on pages that cannot scroll (`browser/DockState.kt`, `browser/BrowserScreen.kt`). Locked shown while overview / editing / find is open (`ui/KoanRoot.kt`, `browser/DockState.kt`); snaps shown on tab change (`ui/KoanRoot.kt`); slides back when a new page load starts (`ui/KoanRoot.kt`).
- **TabSwipe** (`browser/TabSwipe.kt`): page width + 24dp gap per step (`BarTokens.TabSwipeGap`, `ui/KoanRoot.kt`); rubber band 0.3 past first/last neighbour (`PagerTokens.RUBBER_BAND`, `browser/TabSwipe.kt`); commits past 25% of a page or a fling faster than 500dp/s (`PagerTokens.COMMIT_FRACTION`, `PagerTokens.FlingVelocity`; `browser/TabSwipe.kt`, `bar/BarGestures.kt`); spring dampingRatio 0.9, stiffness 400 (`Motion.TabSwipe`, `browser/TabSwipe.kt`); the landed tab goes live after 300ms without a new swipe (`BarTokens.TAB_SWIPE_REST_MS`, `browser/TabSwipe.kt`).

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

**Anatomy** (`bar/AddressRow.kt`)
1. 48dp circular touch target, clipped circle for the ripple.
2. Rounded-square outline badge.
3. Count number centred (or "∞" above 99).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Touch target | 48dp | `BarTokens.IconTouch`, `bar/AddressRow.kt` |
| Badge | 26 × 26dp, corner radius 8dp | `BarTokens.TabsBadge`, `BarTokens.TabsBadgeRadius`, `bar/AddressRow.kt` |
| Badge border | 2dp, `Icon` colour | `BarTokens.TabsBadgeBorder`, `bar/AddressRow.kt` |
| Number | 12sp, `FontWeight.SemiBold`, `Icon` colour | `BarTokens.TabsCountSize`, `bar/AddressRow.kt` |
| Count source | size of the current space's card stack | `ui/KoanRoot.kt` |
| Overflow | count > 99 shows "∞" | `bar/AddressRow.kt` |

**States**
- Default: outline badge + number (`Icon`: light `#6E6E73`, dark `#98989D`).
- Pressed: ripple in the 48dp circle (`cleanTap`, `bar/CleanTap.kt`).
- Long press (page view only): `LONG_PRESS` haptic, opens the Tab menu (`bar/AddressRow.kt`, `bar/AddressRow.kt`). In the overview there is no long press (`onHold = null`).
- Tap: page view → opens overview; overview → back to the page (`ui/KoanRoot.kt`).
- No disabled or selected state.

**Tab menu** (long press, `menu/TabMenu.kt`, built on DockPopup, see below)
- Rows: Plus "New tab", Close "Close tab", History "Reopen closed tab" (`menu/TabMenu.kt`).
- "Reopen closed tab" disabled when nothing to reopen: icon and text `TextDisabled`, not tappable (`menu/TabMenu.kt`).
- Anchored right: right edge on the dock's right margin; min width 200dp, grows left (`TabMenuTokens.MinWidth`, `menu/TabMenu.kt`).
- Grows out of the counter: anchor = 6 + 48 + 2 + 24 = 80dp from the screen's right edge (`menu/TabMenu.kt`).
- Row text 15sp `FontWeight.Medium` `TextPrimary`; icon 18dp `Icon` (`menu/TabMenu.kt`).

**Light vs dark:** palette only (`Icon`, `TextPrimary`, `TextDisabled`, `Dock`).

**Screenshots needed**
1. Page view dock, counter showing "3" (light and dark).
2. Counter at "∞" (a space with more than 99 tabs).
3. Tab menu open above the dock, all three rows enabled.
4. Tab menu with "Reopen closed tab" greyed out.

---

### Space drawer (DrawerContent, DrawerBackdrop, SheetState)

**Purpose:** a sheet that grows out of the overview's dock with the current space's essentials, pinned tabs and Zen folders, and the sync status row.

**Anatomy** (`bar/SpaceDrawer.kt`)
1. Sheet surface = the dock itself (`Dock`), top corners 28dp at full open (`bar/KoanBar.kt`).
2. Drawer handle at the top of the switcher row (see Dock).
3. One page per space, sliding with the space pager (`bar/SpaceDrawer.kt`).
4. Per page, top → bottom, spaced 10dp: empty-state text (if nothing), essentials grid, pinned group (tab rows and folder rows), status row (`bar/SpaceDrawer.kt`).
5. Switcher row and address row stay at the bottom, below the sheet.
6. Behind: overview scrim + baked blur (`ui/KoanRoot.kt`, `bar/DrawerBackdrop.kt`).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Page padding | 14dp all sides | `DrawerTokens.Padding`, `bar/SpaceDrawer.kt` |
| Section gap | 10dp | `DrawerTokens.Gap`, `bar/SpaceDrawer.kt` |
| Essentials grid | 4 per row (empty cells as spacers), 8dp horizontal gap, 10dp row gap | `DrawerTokens.Gap`, `DrawerTokens.EssentialGap`, `bar/SpaceDrawer.kt` |
| Essential tile | height 52dp, width = 1/4 of row, radius 14dp, `Glass` fill, favicon 26dp centred | `DrawerTokens.EssentialHeight`, `DrawerTokens.EssentialRadius`, `DrawerTokens.EssentialFavicon`, `bar/SpaceDrawer.kt` |
| Pinned group | radius 18dp, `Glass` fill, rows stacked with no divider | `DrawerTokens.PinnedRadius`, `bar/SpaceDrawer.kt` |
| Pinned tab row | height 48dp; padding 12dp + 18dp × depth start, 12dp end; favicon 22dp; 12dp gap; title 15sp `TextPrimary`, 1 line ellipsis; active tab: 6dp dot in `TextSecondary` + 12dp; pin glyph 16dp `TextSecondary` only at depth 0 | `DrawerTokens`, `bar/SpaceDrawer.kt`, dot `ui/overview/TabCard.kt`, `CardTokens.ActiveDot` |
| Folder row | height 48dp, same indent; folder glyph (or the folder's Zen icon) 22dp `TextSecondary`; 12dp gap; name 15sp `FontWeight.Medium` `TextPrimary` (blank → "Folder"); 10dp gap; count 13sp `TextSecondary`; chevron-down 16dp `TextSecondary` at the right edge | `DrawerTokens`, `bar/SpaceDrawer.kt` |
| Status row | height 40dp, pill-clipped, 12dp horizontal padding, centred text 13sp `TextSecondary`; attention: 6dp `Attention` dot + 10dp; syncing: `BrandSpinner` 12dp + 10dp | `DrawerTokens.StatusRowHeight`, `DrawerTokens.StatusTextSize`, `FirstRunTokens.BadgeSpinner`, `bar/SpaceDrawer.kt` |
| Empty state | "No essentials or pinned tabs in {space}" 15sp `TextSecondary`, 14dp vertical padding | `bar/SpaceDrawer.kt` |
| Compact detent cap | whole dock ≤ 55% of the window: 0.55 × window − (60 + 68)dp − (nav inset + 6dp) | `SheetTokens.COMPACT_MAX_FRACTION`, `bar/KoanBar.kt` |
| Full detent cap | window − (top safe inset + 12dp) − 128dp − (nav inset + 6dp) | `SheetTokens.FullTopGap`, `bar/KoanBar.kt` |
| Full detent exists only if ≥ 48dp above compact | | `SheetTokens.MinDetentStep`, `ui/sheet/SheetState.kt` |
| Compact / full height | min(content, cap) | `ui/sheet/SheetState.kt` |

Status texts (`bar/SpaceDrawer.kt`): "Connect your Zen Spaces", "Syncing with Zen…", "Synced {n min ago} · Sync now", amber: "Couldn't sync · tap to retry", "Offline · showing your spaces from {ago}", "Sign in again to keep your Zen Spaces in sync".

**States**
- Closed / Compact / Full detents (`ui/sheet/SheetState.kt`). Compact never scrolls; full scrolls a list taller than the screen (nested: up grows then scrolls, down scrolls back then shrinks) (`ui/sheet/SheetState.kt`).
- Pressed rows/tiles: default `clickable` ripple (`bar/SpaceDrawer.kt`). Only the settled space's page is interactive while open (`bar/SpaceDrawer.kt`).
- Folder collapsed: chevron rotated −90°; expanded 0° (`bar/SpaceDrawer.kt`).
- Active (current) tab: 6dp dot before the pin glyph.
- Status row attention (amber dot) / syncing (spinner) / normal.

**Backdrop**
- Scrim over the overview, alpha = drawer progress: `DrawerScrim` (light `#1C1C28 @ 16%`, dark black @ 45%); without live blur (Android 11) `DrawerScrimNoBlur` (32% / 62%) (`ui/KoanRoot.kt`).
- Blur (Android 12+ only): the overview is captured once when the drawer starts opening and baked into N blurred copies, crossfaded with the drag; never blurred per frame (`bar/DrawerBackdrop.kt`). Max radius 14dp (`DrawerTokens.Blur`); radii = 14dp × (i + 1) / N (`ui/KoanRoot.kt`); N = 2 on High and Mid RAM tiers, 1 on Low (`app/src/main/java/dev/nerzouille/koan/DeviceProfile.kt`). Edges clamp (`bar/DrawerBackdrop.kt`).
- Android 11: extra 56dp sheet shadow in `DrawerShadowNoBlur` × progress (`DrawerTokens.NoBlurShadowHeight`, `bar/KoanBar.kt`).

**Motion and gestures**
- Height follows the finger 1:1 (`ui/sheet/SheetState.kt`). `progress` = height / compact height, 0…1, drives scrim, blur, corners and content alpha (`ui/sheet/SheetState.kt`).
- Release: fling faster than 600dp/s goes to the next detent that way (`SheetTokens.FlingVelocity`, `ui/sheet/SheetState.kt`); slow release commits to the neighbouring detent once the sheet has moved 25% of the gap away from the detent it started on, in either direction (`SheetTokens.COMMIT_FRACTION`, `ui/sheet/SheetState.kt`).
- Spring `Motion.Drawer`: dampingRatio 0.9, stiffness 420 (`ui/sheet/SheetState.kt`). A scrolled full list fling decays (`ui/sheet/SheetState.kt`).
- Content fades with progress and rises 30dp (10dp × 3 × (1 − progress)) (`bar/SpaceDrawer.kt`).
- Space switching slides pages by width + 40dp (`PagerTokens.PageGap`, `bar/SpaceDrawer.kt`); the sheet then springs to the new space's height (`ui/sheet/SheetState.kt`).
- Open: swipe up on the overview's dock, or tap the handle (toggles closed ↔ compact) (`bar/BarGestures.kt`, `ui/sheet/SheetState.kt`). Close: swipe down, tap above the sheet (`bar/BarGestures.kt`), handle, Back. `CLOCK_TICK` when it lands on another detent (`bar/BarGestures.kt`).
- Folder chevron spring dampingRatio 0.85, stiffness 600 (`Motion.FolderChevron`).

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
1. Row 60dp high (the dock's switcher row), items bottom-aligned with 2dp bottom padding (`BarTokens.SwitcherBottom`, `bar/WorkspaceSwitcher.kt`).
2. Per space: item (icon at a fixed inset) laid out at its full pill width (`bar/WorkspaceSwitcher.kt`).
3. Active pill tint drawn behind (`bar/WorkspaceSwitcher.kt`).
4. Space icon (Zen icon, or the name's initial) (`bar/WorkspaceSwitcher.kt`).
5. Space name (only inside the pill) (`bar/WorkspaceSwitcher.kt`).
6. Drawer handle centred on the row's top (overview, see Dock).

**Sizes**

| Part | Value | Ref |
|---|---|---|
| Inactive item (touch target, no gap between items) | 44dp wide, 44dp tall | `BarTokens.SwitcherItem`, `bar/WorkspaceSwitcher.kt` |
| Icon | 20dp, `TextPrimary` tint | `BarTokens.SwitcherIcon`, `bar/WorkspaceSwitcher.kt` |
| Icon inset from item left | 12dp (same collapsed and in the pill) | `BarTokens.SwitcherIconInset`, `bar/WorkspaceSwitcher.kt` |
| Icon → name gap | 7dp | `BarTokens.SwitcherNameGap` |
| Pill end padding | 14dp | `BarTokens.SwitcherPillEnd` |
| Pill height | 36dp, radius 18dp (fully rounded), vertically centred in the 44dp item | `BarTokens.SwitcherPillHeight`, `bar/WorkspaceSwitcher.kt` |
| Pill width | 44dp + max(0, 12 + 20 + 7 + 14 + nameWidth − 44) | `bar/WorkspaceSwitcher.kt` |
| Name | 14sp `FontWeight.SemiBold`, 1 line, ellipsis, max 140dp | `BarTokens.SwitcherNameSize`, `BarTokens.SwitcherNameMax`, `bar/WorkspaceSwitcher.kt` |
| Initial (no icon) | first letter uppercased, same 14sp SemiBold, `TextPrimary` | `bar/WorkspaceSwitcher.kt` |

**Colours** (`ui/theme/Theme.kt`)
- Accent = the space's first Zen gradient colour, saturation raised to ≥ 0.55, lightness clamped to `AccentLightness` (light 0.45–0.62, dark 0.62–0.74); saturation < 0.12 → `DefaultAccent` (light `#5B6CFF`, dark `#8EA2FF`) (`SpaceColorTokens.MIN_SATURATION`, `SpaceColorTokens.ACCENT_SATURATION`, `ui/theme/Theme.kt`).
- Pill tint = accent @ `PillAlpha` (light 14%, dark 18%) (`ui/theme/Theme.kt`).
- Name ink = accent at HSL lightness `InkLightness` (light 0.38, dark 0.80) (`ui/theme/Theme.kt`).

**States**
- Active (selected): pill drawn at full alpha, icon alpha 1, name visible (`bar/WorkspaceSwitcher.kt`).
- Inactive: no pill, icon alpha 0.7 (`BarTokens.SWITCHER_INACTIVE_ALPHA`, `bar/WorkspaceSwitcher.kt`).
- In between (dragging): the open amount is shared between the two neighbours under the finger (`1 − |i − pos|`); during an animated jump only the from/to pills open, an overshoot opens nothing (`bar/WorkspaceSwitcher.kt`). Name fades in only past 45% open (`BarTokens.SWITCHER_NAME_FROM`, `bar/WorkspaceSwitcher.kt`); it is clipped to the pill width (`bar/WorkspaceSwitcher.kt`).
- Pressed: no ripple; taps go through `detectTapGestures` on the whole row (`bar/WorkspaceSwitcher.kt`).
- Disabled: taps off in the page view (`enabled = overviewOpen`, `ui/KoanRoot.kt`).
- Layout: centred when all items fit; otherwise the open pill stays centred, ends clamped; rubber band past an end keeps that end in view (`bar/WorkspaceSwitcher.kt`).

**Motion and gestures**
- Overview: horizontal drag anywhere on the dock moves the space pager (`bar/BarGestures.kt`); commit past 25% of the width or a fling > 500dp/s (`PagerTokens.COMMIT_FRACTION`, `PagerTokens.FlingVelocity`, `ui/overview/SpacePager.kt`, `bar/BarGestures.kt`); rubber band 0.3 (`ui/overview/SpacePager.kt`); settle spring dampingRatio 0.86, stiffness 300 (`Motion.PagerSettle`, `ui/overview/SpacePager.kt`). One `CLOCK_TICK` per committed switch on release (`bar/BarGestures.kt`).
- Tap an icon = jump to that space, one `CLOCK_TICK` (`bar/WorkspaceSwitcher.kt`).
- Page-view **scrub** (`bar/SpaceScrub.kt`): long press (system timeout) on the address pill only, held still → space mode, `CLOCK_TICK` (`bar/BarGestures.kt`). The switcher row grows out of the dock (spring `Motion.Overview` dampingRatio 1, stiffness 600: `bar/SpaceScrub.kt`) and the pill label shows the space (see Dock). Sideways moves step the highlight: step = (distance from finger to the screen edge − 48dp) / spaces on that side, clamped to 24…90dp (smaller only if that many spaces would not fit) (`BarTokens.ScrubStepMin`, `BarTokens.ScrubStepMax`, `BarTokens.ScrubEdge`, `bar/SpaceScrub.kt`). Travel limited to half a step past the ends (`bar/SpaceScrub.kt`). Each step: `CLOCK_TICK` (`bar/BarGestures.kt`); highlight spring dampingRatio 0.85, stiffness 900 (`Motion.ScrubHighlight`, `bar/SpaceScrub.kt`). Release on another space switches to it; on the start space cancels (`bar/SpaceScrub.kt`, `bar/BarGestures.kt`).

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
1. Full-screen transparent touch catcher (no scrim): first touch anywhere is consumed and dismisses (`menu/DockPopup.kt`).
2. Popup card: `Dock` fill, 16dp corners, 12dp shadow elevation (`menu/DockPopup.kt`, `TabMenuTokens.Radius`, `TabMenuTokens.Shadow`).
3. Rows (`DockPopupRow`): leading icon or favicon, gap, text (`menu/DockPopup.kt`).

**Sizes and anchoring**

| Part | Value | Ref |
|---|---|---|
| Bottom offset | above nav inset + 68dp (address row) + 6dp (bottom margin) + 8dp gap = 82dp | `TabMenuTokens.Gap`, `menu/DockPopup.kt` |
| Side margin | 6dp (the dock's row padding) on the button's side; max width = screen − 12dp | `menu/DockPopup.kt` |
| Anchoring | `end = true`: right edge pinned at the right margin, grows left; else left edge pinned, grows right. The pinned edge never moves | `menu/DockPopup.kt` |
| Width | hugs content; with `minWidth`, measured at max intrinsic width ≥ minWidth | `menu/DockPopup.kt` |
| Corner radius | 16dp | `TabMenuTokens.Radius` |
| Shadow | 12dp elevation (`Modifier.shadow`) | `TabMenuTokens.Shadow` |
| Row height | 48dp, 16dp horizontal padding | `TabMenuTokens.RowHeight`, `menu/DockPopup.kt` |
| Leading icon | `MenuIconTile`: 30dp tile, 9dp corners, `GroupedRow` fill, 18dp glyph `TextPrimary`, then 12dp gap (same tile as sheet menu rows) | `ui/sheet/SheetKit.kt` `MenuIconTile` |
| Leading favicon (Back menu) | 18dp, then 12dp gap | `TabMenuTokens.Icon/IconGap` |
| Row text | 15sp | `TabMenuTokens.TextSize` |
| Tab menu | min width 200dp, Medium weight text, anchor 80dp from the right edge | `TabMenuTokens.MinWidth`, `menu/TabMenu.kt` |
| Back menu | max width 300dp; max height 6 rows + half a row = 286dp (scrolls, half a row peeks); anchor 6 + 24 = 30dp from the left edge | `TabMenuTokens.MaxWidth`, `TabMenuTokens.MAX_ROWS`, `menu/BackMenu.kt` |

**Rows**
- Tab menu: icon on its tile (`TextPrimary` glyph) + text `TextPrimary` Medium; disabled row: glyph and text `TextDisabled`, not clickable (`menu/TabMenu.kt`).
- Back menu: favicon 18dp + page title (or host, or the raw url) in `TextPrimary`, regular weight, 1 line ellipsis; the pages before the current one, most recent first (`menu/BackMenu.kt`). Tap = go back to that entry.

**States**
- Closed / opening / open / closing (`shown` 0 → 1) (`menu/DockPopup.kt`).
- Row pressed: default `clickable` ripple (`menu/DockPopup.kt`); rows are not clickable while closing (`interactive = open`).
- Row disabled: Tab menu's "Reopen closed tab" only.
- Back menu stays showing the old entries while it closes (`menu/BackMenu.kt`); it does not open when there is no earlier entry (`menu/BackMenu.kt`).

**Motion**
- In: tween 160ms; out: tween 120ms (default easing) on alpha and scale (`TabMenuTokens.IN_MS`, `TabMenuTokens.OUT_MS`, `menu/DockPopup.kt`).
- Scale 0.85 → 1 (`TabMenuTokens.SCALE_FROM`, `menu/DockPopup.kt`), transform origin = the button's centre x, bottom edge of the popup (`menu/DockPopup.kt`): it grows out of the button and shrinks back into it.
- Dismiss: tap anywhere else, or Back (`menu/DockPopup.kt`).
- Opened with a `LONG_PRESS` haptic from the button (`bar/AddressRow.kt`).

**Light vs dark:** card `Dock` (`#FFFFFF` / `#232326`); text and icon tokens per palette; the shadow is the platform elevation shadow (no palette colour).

**Screenshots needed**
1. Page view, light: Tab menu open above the dock at the right (New tab, Close tab, Reopen closed tab).
2. Same, dark, with "Reopen closed tab" greyed out.
3. Back menu open at the left with 3 fake history entries (example.com, example.org pages with titles).
4. Back menu with 8+ entries, showing the half-row peek at the bottom.

---

### Code vs docs/UI_SPEC.md (code wins)

- Space pager parallax: UI_SPEC "Bottom bar" says the stack trails at 0.85× and scales 0.96 → 1; code has `PagerTokens.PARALLAX = 0.2` (0.8×) and `INCOMING_SCALE = 0.06` (0.94 → 1). Not part of this export's components, noted for whoever documents the overview.
- UI_SPEC "One sheet everywhere" lists 32dp top corners for sheets over a page; that is `SheetTokens.TopRadius` for the ⋯ menu only. The drawer uses 28dp (`DrawerTokens.TopRadius`), as the drawer section of UI_SPEC also says.
- Otherwise the dock, popups, drawer and switcher values in UI_SPEC match the code.


## 3.B Overview, depth stack, transitions, start page, error pages, pull to refresh

All paths are relative to the repo root. Short forms used below:
`OV/` = `app/src/main/java/dev/nerzouille/koan/ui/overview/`,
`UI/` = `app/src/main/java/dev/nerzouille/koan/ui/`.

Colours are given as `KoanPalette` token → Light / Dark (`KoanPalette.Light`, `KoanPalette.Dark`).
Colour notation: `#RRGGBB`, or `#RRGGBB @ a` for an alpha copy; `#AARRGGBB` where the code writes an 8-digit literal.

Palette tokens used in this file (resolved once):

| Token | Light | Dark |
|---|---|---|
| Base | #F5F5F7 | #121214 |
| Glass | #EBEBEF | white @ 0.08 |
| TextPrimary | #1C1C1E | #FFFFFF |
| TextSecondary | #66666B | white @ 0.62 |
| Card | #FFFFFF | #2C2C2E |
| CardStrip | white @ 0.94 | #F22C2C2E (≈ #2C2C2E @ 0.95) |
| CardBorder | #E4E4EA | white @ 0.08 |
| CloseButton | black @ 0.06 | white @ 0.10 |
| CloseIcon | #6E6E73 | white @ 0.75 |
| Primary / OnPrimary | #1C1C1E / #FFFFFF | #FFFFFF / #1C1C1E |
| Dock | #FFFFFF | #232326 |
| Skeleton | black @ 0.06 | white @ 0.07 |
| IconBacking | Unspecified (never used) | #E9E9EE |
| Attention | #8A5C00 | #E8B45A |
| DimToBase | true (back cards haze towards Base) | false (they darken towards black) |
| DimGain | 1.25 | 1.0 |
| DimMax | 0.60 | 0.80 |
| CardElevation | 12dp | 20dp |
| CardShadowColor | #1C1C30 @ 0.14 | black @ 0.60 |
| ContactShadow | #1C1C30 @ 0.18 | black @ 0.65 |
| ContactShadowLength | 16dp | 18dp |

Brand gradient (`BrandTokens`): Pink #FF5FA2 → Violet #B46BFF → Blue #4F8DFF, same in both themes.
`BrandSpinner` (`UI/theme/Brand.kt`): sweep-gradient arc of those colours, stroke 2dp (`BrandTokens.SpinnerStroke`), sweep 270° (`BrandTokens.SPINNER_SWEEP`), one turn every 900ms, linear, repeating (`BrandTokens.SPINNER_TURN_MS`, Brand.kt), round caps.
`brandOutline` (`Brand.kt`): 1.5dp border (`BrandTokens.Outline`) with the horizontal brand gradient.

---

### Depth stack (DepthStack + DepthCurve + StackState)

**Purpose:** the space's open tabs as an upright one-point-perspective deck: one big focused card, a pile converging upward behind it, and the next card peeking from below.

**Anatomy** (`OV/DepthStack.kt`)
- Viewport: a `BoxWithConstraints`, clipped except while the hero card flies out of it (`DepthStack.kt`).
- Per card, back to front by index: an optional `ContactShadow` layer (if there is a card behind it), the `TabCard`, and, for the card the page transition is flying, the `FlyingHero` drawn in that card's place (`DepthStack.kt`).
- Cards closed and flying off are drawn where they were in the pile: over the cards behind, under the ones in front (`DepthStack.kt`).
- Only cards near the focus are composed: visible range plus `WINDOW_MARGIN` = 2 each side once settled, growing while idle to 16 behind / 10 in front, 1 more card per side per frame (`DepthStack.kt`).

**Layout frame** (`StackFrame`, `DepthStack.kt`). W, H = viewport width/height.
- `cardWidth = W − 2 × CardSideMargin` with CardSideMargin = 14dp. (UI_SPEC says 12dp; code is 14dp.)
- `visibleBottom = H − UnderDock`, UnderDock = 24dp: the viewport runs 24dp under the dock's rounded top.
- `cardHeight = (visibleBottom − (PileTopGap + PeekVisible − PeekOverlap)) / (1 + PILE_DEPTH_FRACTION)`
  = `(visibleBottom − (22 + 60 − 40)dp) / 1.28` = `(visibleBottom − 42dp) / 1.28` (`StackTokens.PileTopGap`, `StackTokens.PeekVisible`, `StackTokens.PeekOverlap`, `StackTokens.PILE_DEPTH_FRACTION`; DepthStack.kt).
- `peekTop = visibleBottom − PeekVisible(60dp)` (DepthStack.kt).
- `focusTop = peekTop + PeekOverlap(40dp) − cardHeight` (DepthStack.kt).
- `A (pileDepth) = max(0, min(0.28 × cardHeight, focusTop − 22dp))` (DepthStack.kt). With the formulas above both terms are equal, so A = 0.28 × cardHeight on any screen.
- Cards are horizontally centred; `transformOrigin = (0.5, 0)` (top centre) (DepthStack.kt).

**The depth formula.** `d = state.pos − card.slot` (in card slots, fractional); a closed card keeps the `d` it had when closed (`DepthStack.kt`). d = 0 focused, d > 0 behind (pile), d < 0 in front.
Parameters: r = `R` = 0.45, r_s = `SCALE_R` = 0.52, sMin = `S_MIN` = 0.58, `PEEK_SCALE` = 1.05, `PEEK_DIM` = 0.12.

| Property | d ≥ 0 (focus and pile) | −1 ≤ d < 0 (to the peek) | d < −1 (past the peek) | Ref |
|---|---|---|---|---|
| rise(d) | 1 − 0.45^d | 0 | 0 | DepthCurve.kt |
| drop(d) | 0 | −d | 1 | DepthCurve.kt |
| top (translationY) | focusTop − A·(1 − 0.45^d) | focusTop + (peekTop − focusTop)·(−d) | peekTop + (−1 − d)·(H − peekTop) | DepthStack.kt |
| scale | 0.58 + 0.42·0.52^d | 1 + 0.05·(−d) (1.05 at d = −1) | 1.05 | DepthCurve.kt |
| dim (overlay alpha on thumbnail) | keyframes `Dim`, linear between integer d | 0.12·(−d) | 0.12 | DepthCurve.kt |
| blur radius (dp) | keyframes `BlurDp` (0 at d ≤ 0) | 0 | 0 | DepthCurve.kt |
| card alpha | keyframes `Alpha` | 1 | 1 | DepthCurve.kt |
| header dim | min(dim × 0.5, 0.2) | same | same | DepthCurve.kt, `StackTokens.HEADER_DIM_MAX` |
| ✕ alpha | clamp(1 − 2·abs(d), 0, 1) | same | same | TabCard.kt |

Keyframes at d = 0, 1, 2, … joined linearly and held at the last value (`DepthCurve.kt`):
- `Dim` = [0, 0.16, 0.28, 0.38, 0.48, 0.58, 0.66]. Effective dim = min(dim × DimGain, DimMax) → Light ×1.25 capped 0.60, Dark ×1.0 capped 0.80 (`TabCard.kt`). Overlay colour: Light = `Base` (haze), Dark = black (`DepthStack.kt`).
- `BlurDp` = [0, 0, 0, 0.9, 1.9, 2.9, 4.0] dp; quantised to 0.5dp steps (`DepthStack.kt`). Only a top slice of 512px of the thumbnail is blurred (`CardTokens.BLUR_SLICE_PX`, `TabCard.kt`). Without live blur (Android 11) or before baked copies exist, an extra dim of `NO_BLUR_DIM` 0.22 × (blurDp/4dp) is added instead (`TabCard.kt`).
- `Alpha` = [1, 1, 1, 1, 1, 0.85, 0.6, 0.3, 0] → cards fade from d = 5 and are invisible at d = 8 (`MAX_VISIBLE_D` = 8, DepthCurve.kt). In front, cards are invisible below d = −2 (`MIN_VISIBLE_D`, DepthCurve.kt). Invisible cards get alpha 0 and are not hit-tested.
- Header dim: the result is then also × DimGain capped DimMax (`TabCard.kt`), so the header strip is dimmed at most 0.20 in Dark and 0.25 in Light.

Worked values (computed from the formulas): scale d=1 0.798, d=2 0.694, d=3 0.639, d=4 0.611; rise d=1 0.55, d=2 0.798, d=3 0.909, d=4 0.959.

**Corner radius vs depth:** the card's shape is a constant `RoundedCornerShape(28dp)` (`CardTokens.Radius`, `DepthStack.kt`) applied before the layer scale, so on screen the corner is 28dp × scale(d) (e.g. ≈ 22.3dp at d = 1, 29.4dp at the peek). Not animated separately.

**Shadow:** every visible card has `shadowElevation = CardElevation + lift × 16dp` (Light 12dp, Dark 20dp; LiftShadow), ambient and spot colour `CardShadowColor`, its alpha × (1 − 0.1 d) for back cards (min 0.6; the peek card keeps it all) (`DepthStack.kt`).

**Swipe tilt / fade** (applied to the same layer, `DepthStack.kt`): tilt = swipeX(dp) × 0.03°/dp (`StackTokens.SWIPE_TILT_PER_DP`), pivot at the card's bottom centre; alpha × max(1 − abs(swipeX)/420dp, 0.3) (`StackTokens.SwipeFadeDistance`, `StackTokens.SWIPE_MIN_ALPHA`).

**States**
- Focused (abs(d) < 0.5 is "focused" for taps, `DepthStack.kt`): full size, no dim, ✕ visible.
- Pile (d > 0): smaller, higher, dimmed/hazed, blurred from d = 3, fading from d = 5.
- Peek (d = −1): 1.05×, 12% dim, overlaps the focused card's bottom by 40dp; 60dp visible above the dock.
- Lifted (long press): see TabCard / Reorder below.
- Swiping / flying off: see Swipe-to-close below.
- Disabled (`enabled = false`): no pointer input at all; same look (used for the non-current space page and the skeleton stack; `DepthStack.kt`, `SpacePage.kt`, `SpacePage.kt`).
- Pressed: no pressed visual (no ripple); the gesture handler is custom.
- Light vs dark: only the dim colour/gain/cap, elevation and shadow colours differ (table above).

**Scroll gesture** (`StackState`, `OV/StackState.kt`; gesture code `DepthStack.kt`)
- Axis decision: the first 6dp of travel (`GestureSlop`) picks the axis: more sideways than vertical = swipe-to-close of the focused card (wherever the finger is); else scroll. No movement until Android's long-press timeout = long press if on a card (`viewConfiguration.longPressTimeoutMillis`, system value, `DepthStack.kt`).
- Drag: 170dp of finger travel = 1 slot (`SlotDrag`; `DepthStack.kt`). Finger down moves earlier cards forward.
- Rubber band past both ends: delta × 0.45 / (1 + 2·overshoot) (`StackTokens.RUBBER_BAND`, `StackState.kt`).
- Release, v in slots/s (`StackState.kt`):
  - abs(v) < 2 (`FLING_MIN_VELOCITY`): snap. Moved more than 0.15 slot since the drag started (`SNAP_COMMIT`) → next whole card in that direction, else back to the start card (`DepthCurve.kt`).
  - Fling: travels max(1, (min(abs(v), 36) − 2.2) × 0.55) cards, rounded to a card, at least the next card (`StackTokens.FLING_DEADZONE`, `StackTokens.FLING_GAIN`, `StackTokens.FLING_MAX_VELOCITY`, `DepthCurve.kt`). Motion: cubic ease-out x(t) = x0 + D·(1 − (1 − t/T)³), T = 3·D/v clamped to 0.3–2.2s (`StackTokens.FLING_EASE_POWER`, `StackTokens.FLING_MIN_SECONDS`, `StackTokens.FLING_MAX_SECONDS`).
- Snap glide (`animateTo`): exponential, pos += (target − pos)·(1 − e^(−dt/0.09s)), stops within 0.002 slot; no overshoot (`Motion.STACK_SNAP_TAU_SECONDS` = 0.09; `StackState.kt`).
- A touch during a fling only stops it; that touch never opens or focuses a card (`DepthStack.kt`).
- Mouse wheel / trackpad: same as a drag (× system scaled vertical scroll factor), settles like a slow release 120ms after the last event (`WHEEL_SETTLE_MS`; `StackState.kt`).
- Taps: tap the focused card = open the tab; tap the ✕ zone = close (fly off to the right); tap any other visible card = glide it into focus (`DepthStack.kt`). ✕ zone = the card's top `HeaderHeight` (44dp) × the rightmost `CloseTouch` (44dp), both × scale (`DepthStack.kt`).
- Card slot changes (gap closing after a close, reorder): spring `Motion.CardSlot` dampingRatio 0.8, stiffness 420 (`StackState.kt`). Off-screen cards jump without motion (`DepthStack.kt`).

**Screenshot needed:** overview of a space with ≥ 8 fake tabs (example.com pages), focused on a middle card, so the pile shows 4–5 receding cards and the peek card shows at the bottom above the dock. One in Light, one in Dark.

---

### TabCard (card anatomy)

**Purpose:** one tab in the stack: header strip with favicon, title and close button over the page's screenshot (or a placeholder).

**Anatomy** (`OV/TabCard.kt`), bottom to top:
1. Card surface `Card`, plus a tint of the favicon's colour at 8% (`PLACEHOLDER_TINT` 0.08; `TabCard.kt`).
2. Placeholder thumbnail (only while there is no screenshot; see Placeholder thumbnail).
3. Screenshot: full width, top-aligned under the header (starts at y = 44dp), scaled to the card width, cropped at the bottom (`TabCard.kt`). Fades in over the placeholder in 140ms (`ThumbTokens.FADE_MS`); a fresher screenshot crossfades over the old one in the same 140ms.
4. Dim overlay: rect of the dim colour at the depth dim (`TabCard.kt`).
5. Header strip (`CardHeader`, `TabCard.kt`): height 44dp, background `CardStrip`, no blur, padding start 12dp / end 6dp, vertically centred row (two lines since 2026-11-11, `docs/design/OVERVIEW_PRIVATE.md` §3.2):
   - Favicon 24dp (corner 28% of its size).
   - 10dp gap (`HeaderGap`).
   - Title 14.5sp / 18 SemiBold `TextPrimary`, 1 line, ellipsis; a trailing " - <site name>" that repeats one of the host's labels is dropped (`cardTitle`, `OV/CardTitle.kt`). Split view cards join pane titles with "  |  ".
   - Under it, the host 11.5sp / 14 Regular `TextSecondary`, 1 line (`displayHost`: no scheme, no "www.").
   - Pinned marker (live pinned/essential tab): 10dp gap + pin glyph 14dp (`CardTokens.PinGlyph`) tinted `TextSecondary`.
   - Current-tab marker: 10dp gap + 6dp circle (`CardTokens.ActiveDot`) in `TextSecondary` (`CurrentTabDot`, `TabCard.kt`). Never the brand gradient.
   - ✕: 44dp touch box (`CardTokens.CloseTouch`) holding a 30dp circle of `CloseButton` with a 14dp close icon tinted `CloseIcon`. Alpha = clamp(1 − 2·abs(d)), so it shows only on the focused card.
   - Header dim overlay (see depth table).
6. Edge (`CardEdge`), over the depth dim so back cards keep it at every depth: a 1dp hairline all around in `CardHairline` (light `#1C1C301A`, dark `#FFFFFF1F`; replaces `CardBorder` on stack cards), and in dark a 1dp rim inside the top edge in `CardRim` (`#FFFFFF1A`), which separates stacked dark header strips.

**Sizes:** width/height from the stack frame (Depth stack). Corner radius 28dp (`CardTokens.Radius`). Clipped to the shape.

**States**
- Default / focused, pile, peek: per the depth table.
- Active (current tab): neutral 6dp dot after the title.
- Pinned: pin glyph after the title.
- Split view (`splitCount > 1`): one card; title joins every pane; placeholder domain line reads "host  ·  split view of N" (`TabCard.kt`).
- Never opened: placeholder thumbnail.
- Skeleton (first sync running): see FirstRun / Skeleton card.
- Lifted (long press): scale × (1 + 0.03·lift) (`StackTokens.LIFT_SCALE`), elevation + 16dp·lift (`StackTokens.LiftShadow`), drawn above all others (zIndex 1) (`DepthStack.kt`).
- Pressed / disabled: no distinct visual.
- Light vs dark: only through the palette tokens above.

**Long-press lift and reorder** (`DepthStack.kt`)
- After the system long-press timeout with no movement on a card: haptic `LONG_PRESS`, lift springs to 1 with `Motion.Lift` dampingRatio 0.7, stiffness 600.
- The lifted card follows the finger: slot = start + dy / 170dp, clamped to [−0.4, last + 0.4]; when its rounded slot changes, a tick haptic (`SEGMENT_TICK` on Android 14+, else `CLOCK_TICK`) and the other cards spring out of the way (`CardSlot` 0.8 / 420).
- Drop: slot springs to its target (CardSlot), lift back to 0 with `Motion.Drop` dampingRatio 0.8, stiffness 500.

**Swipe-to-close** (`DepthStack.kt`)
- Always moves the focused card, 1:1 with the finger (the 6dp slop included).
- While dragging: tilt 0.03°/dp, pivot bottom centre; alpha 1 − abs(x)/420dp, min 0.3.
- Release closes if abs(vx) > 800dp/s (`SwipeCloseFling`, flies the way it was flung) or abs(x) > 0.35 × card width (`SWIPE_CLOSE_FRACTION`). (UI_SPEC says "past 100dp"; code uses 35% of the card width.) Otherwise springs back with `Motion.SwipeReturn` dampingRatio 0.75, stiffness 500.
- Fly-off: tween 180ms (`FLY_OFF_MS`), `FastOutLinearInEasing`, from the release x to ±480dp (`StackTokens.FlyOffDistance`), tilt to ±8° (`StackTokens.FLY_OFF_TILT`), alpha to 0 (`StackState.kt`, `DepthStack.kt`). Haptic `CONFIRM`. The gap closes at once (it does not wait): closing the focused card brings forward the card on the side last scrolled towards (behind if not scrolled), the other side at an end (`DepthCurve.kt`).

**Screenshot needed:** (1) focused card close-up with a real screenshot (example.com), current-tab dot visible; (2) a pinned live tab card showing the pin glyph; (3) a card mid-swipe (≈ 30% across, tilted and faded); (4) a lifted card during reorder. Light and Dark.

---

### Placeholder thumbnail (never-opened tab)

**Purpose:** what a card shows before its page has ever been captured: neutral surface, big favicon, domain.

**Anatomy** (`OV/TabCard.kt`): column centred horizontally, top padding = header 44dp + `PlaceholderTop` 44dp = 88dp; favicon 64dp (`CardTokens.PlaceholderFavicon`); 14dp gap (`CardTokens.PlaceholderGap`); domain text 14sp (`CardTokens.PlaceholderDomainSize`) `TextSecondary`, 1 line. Background is the card's `Card` + 8% favicon-colour tint.
- Deep in the pile it is captured once and crossfaded through pre-blurred copies; it blurs live until they are ready (`TabCard.kt`). Visual result: same blur as a screenshot at that depth.
- Same placeholder, full screen, is used as the page cover (`UI/browser/PageCover.kt`): top spacer 44dp × 4 = 176dp, favicon 64dp, gap 14dp, domain 14sp `TextSecondary` on `Card`. A Zen tab that only the desktop can open adds "This tab opens only on your computer." (14sp, `TextSecondary`, centred, horizontal padding 44dp).

**Screenshot needed:** a card for a never-opened tab of a fake host (e.g. "example.org") in focus, Light and Dark; and the full-screen cover variant.

---

### Favicon

**Purpose:** a tab's icon: Zen static icon, else the real favicon, else **the one letter tile**
(the same everywhere: cards, drawer, history, search results, the @ picker, prompts).

**Anatomy** (`ui/overview/Favicon.kt`)
- Zen static icon: the glyph tinted `TextPrimary`.
- Letter tile: rounded square, corner = size × 0.28; background = one of 8 muted tones picked by
  host hash: #4F6D8F, #7A5C8E, #5E8C6A, #8E6A4F, #4F8A8B, #8B4F5E, #6B6F8F, #7F7A4A; one letter,
  Bold, white, font size (and line height) = size × 0.55. Letter = first letter of the site name
  (the label before the suffix), uppercased; "•" if none. Before the audit a table of 22 known
  hosts had their own tiles (two letters, light tiles with coloured letters), which read as a
  different style, e.g. a bare black "W" in the @ picker; that table is gone.
- **One site, one icon:** the host loses `www.`, `m.` and `mobile.` labels (anywhere before the
  site name: `en.m.example.org` → `en.example.org`) before it picks the tile colour and the icon
  cache key (`FaviconDiscovery.iconHost`, `siteKey`), so `m.example.com` and `example.com` show
  the same icon and tile. The page that is fetched to find the icon is unchanged.
- Real favicon: fitted in the same rounded square; fades in over the letter tile in 140ms (`Favicon.kt`). An icon already in memory draws in the first frame without a fade.
- Dark: an icon detected as dark is placed on an `IconBacking` tile (#E9E9EE) at 72% of the size (`Favicon.kt`). Light: no backing (`IconBacking` = Unspecified).

**Sizes used:** card header 24dp (`CardTokens.Favicon`), placeholder 64dp (`CardTokens.PlaceholderFavicon`), Continue card 24dp (`StartPageTokens.ContinueFavicon`), essentials tile 26dp (`DrawerTokens.EssentialFavicon`).

**States:** letter tile → fading → real icon; backed (dark icon in Dark theme). No pressed/disabled.

**Screenshot needed:** a row of cards/essentials showing a letter tile (fake host), a real favicon, and in Dark a dark favicon on its light backing.

---

### ContactShadow

**Purpose:** the short soft shadow a card casts upward onto the card behind it, which layers the pile.

**Anatomy** (`OV/ContactShadow.kt`): a separate layer drawn just under the casting card, with the card's size and transform.
- Vertical gradient from `ContactShadowLength` (16dp light, 18dp dark) above the card's top edge down to the edge; alpha ∝ t² at stops 0, 0.25, 0.5, 0.75, 1 (`ContactShadow.kt`).
- Horizontal mask fading the ends before the corners: transparent at 0.5 × radius, opaque from 1.5 × radius, symmetric at the right (radius = 28dp) (`ContactShadow.kt`).
- A full-strength band of height 28dp under the card's top edge, so the shadow meets the rounded edge without a cut (`ContactShadow.kt`).
- Length = min(`ContactShadowLength`, 0.35 × the visible sliver of the card behind) (`CardTokens.CONTACT_SHADOW_MAX_FRACTION`; `DepthStack.kt`); 0 if either card is invisible or the card behind is closed. Not drawn under 0.5px.
- Fades with the card (same alpha incl. swipe fade).

**Light vs dark:** colour `ContactShadow`: Light #1C1C30 @ 0.18, Dark black @ 0.65; length 16dp light, 18dp dark (still ≤ 35% of the sliver behind).

**Screenshot needed:** close crop of the focused card's top edge over the pile card behind it (Light, where it is subtle, and Dark).

---

### Overview screen (OverviewScreen + SpacePage + SpaceHeader)

**Purpose:** the space overview: one page per space with a compact header and the depth stack; the dock/bar is drawn over the bottom zone by KoanRoot.

**Anatomy** (`OV/OverviewScreen.kt`, `OV/SpacePage.kt`)
- Full-screen background: flat `Base` (`OverviewScreen.kt`). **No gradient background per space**: the code paints only `Base` in both themes; the space gradient is used only to derive the small accent (`Theme.kt`). UI_SPEC agrees ("no space-colour wash anywhere").
- Column: the space pages area (top safe-area padding, `topSafePadding`) then a bottom spacer of navigation-bar inset + (dock height − 24dp) so the stack runs 24dp under the dock (`OverviewScreen.kt`).
- Per space page: `SpaceHeader`, optionally a compact Connect card (phone-only space, signed out, with tabs), then a box with the stack, the skeleton stack, the full Connect card or the empty state (`SpacePage.kt`).
- **Essentials grid and pinned row are not on the overview**: they live in the bar's drawer (`SpacePage.kt`; drawer in `UI/bar/SpaceDrawer.kt`). The start page reuses the essentials grid (see Start page).

**SpaceHeader** (`SpacePage.kt`)
- Height 52dp (`OverviewTokens.HeaderHeight`), padding start 6dp / end 10dp, row centred vertically. No meta line.
- Back button: 44dp circular touch (`OverviewTokens.HeaderIcon`), back glyph 22dp (`OverviewTokens.HeaderGlyph`) `TextPrimary`, ripple via `clickable`.
- Space icon box: 32dp (`OverviewTokens.SpaceIconBox`), corner 10dp (`OverviewTokens.SpaceIconRadius`), `Glass` fill, Zen icon 20dp (`OverviewTokens.SpaceIconGlyph`) tinted `TextPrimary`.
- 10dp gap (`OverviewTokens.TitleGap`).
- Space name: 20sp (`OverviewTokens.TitleSize`) Bold `TextPrimary`, 1 line, ellipsis.
- Sync badge (`SyncBadgeView`): only when sync failed or the account needs signing in again, a 7dp dot in `SyncDot` (light `#C0841A`, dark `#E8B45A`) 8dp after the name, in a 36dp touch target; contentDescription "Sync failed, retry". Tap = retry / sign in. Syncing and synced show nothing (the spinner is gone).
- Private toggle (`PrivateToggle`) at the end of the row: see "Private mode" below.
- Gesture: vertical drag down on the header > 48dp (`OverviewTokens.SwipeDownToDismiss`) dismisses the overview back to the page.
- (UI_SPEC lists "tap space name → space menu"; the current header has no tap action on the name.)

**Empty space** (`SpacePage.kt`): centred column, horizontal padding 32dp (`OverviewTokens.EmptyPadding`); "No open tabs in {space}" 18sp (`OverviewTokens.EmptyTitleSize`) SemiBold `TextPrimary`, 1 line; 6dp (`OverviewTokens.EmptyGap`); "Its pinned tabs are in the drawer below." 14sp (`OverviewTokens.EmptyBodySize`) `TextSecondary`; 24dp (`OverviewTokens.EmptyButtonGap`); `PrimaryButton` "New tab" with a 20dp plus glyph (`OverviewTokens.NewTabGlyph`) in `OnPrimary` (button component from `UI/kit`, documented elsewhere).

**States:** current space (stack enabled), neighbour spaces (composed but disabled), ghost (not composed yet, see GhostSpace), first sync (skeleton stack), signed-out phone-only space (Connect card), empty space.

**Light vs dark:** palette tokens only.

**Screenshot needed:** (1) overview at rest on a space with fake tabs, header with icon + name; (2) the empty-space state; (3) header with the amber sync-failed dot; (4) the Private toggle off and on. Light and Dark for (1).

---

### SpacePager (space switching + parallax)

**Purpose:** horizontal pager between spaces, driven only by the workspace switcher in the bottom bar (never by swipes in the card area).

**Motion** (`OV/SpacePager.kt`, `OV/OverviewScreen.kt`, `SpacePage.kt`)
- One page of travel = window width + `PageGap` 40dp (`KoanRoot.kt`); the finger drives it 1:1.
- Each page: translationX = (i − pos) × (width + 40dp); alpha 0 once a full page away; a `Base` overlay at alpha abs(i − pos) fades it into the background as it leaves (`OverviewScreen.kt`).
- Parallax of the stack inside the page: translationX = −f × width × 0.2 (`PARALLAX`) and scale = 1 − 0.06 × abs(f) (`INCOMING_SCALE`), f = page offset; the header moves with the page, so the stack trails it (0.8× speed) and grows from 0.94 to 1.
- The cards' own alpha also × (1 − abs(f)) (`SpacePage.kt`).
- Rubber band at the first/last space: × 0.3 (`PagerTokens.RUBBER_BAND`; `SpacePager.kt`); never more than one page from the settled one.
- Release: commits to the neighbour past 0.25 of a page (`COMMIT_FRACTION`) or on a fling > 500dp/s in the drag direction (`FlingVelocity`; `BarGestures.kt`); settle spring `Motion.PagerSettle` dampingRatio 0.86, stiffness 300, carrying the release velocity. A committed change gives a haptic tick (`BarGestures.kt`).
- A page composed on landing fades in over its ghost in 160ms (`PAGE_APPEAR_MS`).

**Screenshot needed:** mid-switch frame (about 40% between two spaces) showing the outgoing page fading and the incoming stack slightly smaller and trailing its header.

---

### GhostSpace

**Purpose:** a cheap stand-in for a neighbour space's page before it is composed for real, so a swipe never pops a page in.

**Anatomy** (`OV/GhostSpace.kt`): the real `SpaceHeader`, then one canvas drawing the stack at rest with the same frame: for each integer depth from min(focus, 8) down to −1 (−1 only if a card exists in front): card rect in `Card` × the depth alpha, scaled per `DepthCurve.scale`, corner 28dp × scale; the focused card's screenshot only if already in memory; header band `CardStrip` (44dp × scale); dim overlay (`Base` in Light, black in Dark, depth dim × DimGain capped DimMax); 1dp `CardBorder` outline. No favicons, titles, blur or contact shadows. Hidden while the page ↔ overview hero is flying (`GhostSpace.kt`). Same parallax as the real page.

**States:** shown, then faded out under the real page (160ms).

**Screenshot needed:** none separately (it is meant to be indistinguishable); optional: first frame of a space switch on a slow device.

---

### FirstRun (skeleton stack + Connect card)

**Purpose:** never a blank overview: breathing placeholder cards while the first sync runs, and one calm card inviting the Mozilla sign-in.

**Skeleton stack** (`OV/SpacePage.kt`, `OV/FirstRun.kt`, `OV/TabCard.kt`)
- 6 skeleton cards (`FirstRunTokens.SKELETON_CARDS`), a real `DepthStack` (disabled) focused on index 4.
- Whole stack alpha breathes 1 ↔ 0.55 (`FirstRunTokens.SKELETON_ALPHA_MIN`), 1400ms tween each way, reversing (`FirstRunTokens.SKELETON_BREATH_MS`).
- Skeleton card: `Card` surface with depth dim; header strip 44dp `CardStrip`, padding 14/6dp; a 22dp rounded square (corner 22 × 0.28 dp) and a 140 × 10dp pill (`CardTokens.SkeletonTitle`, `CardTokens.SkeletonBar`), both `Skeleton` (Light black @ 0.06, Dark white @ 0.07); 1dp `CardBorder` outline.
- When the first real spaces arrive, they crossfade in over the skeletons in 420ms (`REVEAL_MS`; `OverviewScreen.kt`).

**Connect card, full** (`OV/FirstRun.kt`): side margin 14dp (`FirstRunTokens.SideMargin`), corner 28dp, `Card` fill, 1.5dp brand-gradient outline, padding 24dp (`FirstRunTokens.Padding`), items centred, spaced 10dp (`FirstRunTokens.Gap`): app icon 64dp (`FirstRunTokens.AppIcon`) with 16dp corners (`FirstRunTokens.AppIconRadius`); "Connect your Zen Spaces" 20sp Bold `TextPrimary` centred (`FirstRunTokens.TitleSize`); body 14sp `TextSecondary` centred (`FirstRunTokens.BodySize`); an extra 10dp spacer; `PrimaryButton` "Sign in".
**Connect card, compact** (phone-only space already has tabs): full-width pill (CircleShape), `Card` fill, brand outline, vertical padding 12dp (`FirstRunTokens.CompactPadding`), "Connect your Zen Spaces" 15sp Medium `TextPrimary` (`FirstRunTokens.ButtonSize`). Tap = sign in (ripple).

**Screenshot needed:** (1) first-run overview signed out with the full Connect card on an empty phone-only space; (2) the skeleton stack during first sync; (3) compact Connect pill above a stack of fake tabs.

---

### Page ↔ overview transition (OverviewMotion + CardHero)

**Purpose:** opening a card into the page and back, without a double image: the overview stays opaque and only the tab's card flies between the page rect and its slot.

**Progress driver** (`UI/OverviewMotion.kt`): progress 0 = page, 1 = overview.
- Spring on its own: `Motion.Overview` dampingRatio 1.0 (no overshoot), stiffness 600, carrying a drag's release velocity (`OverviewMotion.kt`). (UI_SPEC says ≈ 0.85 / 380 and "a tiny overshoot" on landing; code is critically damped 1.0 / 600.)
- Finger-driven on the dock: 220dp of vertical travel = the whole morph (`BarTokens.OverviewDrag`; `KoanRoot.kt`). Release commits past 0.25 of the way (`PagerTokens.COMMIT_FRACTION`) or on a vertical fling > 600dp/s (`BarTokens.OverviewFling`; `OverviewMotion.kt`, `BarGestures.kt`).

**What moves** (p = progress)
- Overview zoom: scale = 1 + 0.04 × (1 − p) around the window centre (`OVERVIEW_ZOOM`; `KoanRoot.kt`): the overview settles from 1.04 to 1.0 as it opens. It is always opaque.
- Flying card (`FlyingHero` / `HeroBody`, `OV/CardHero.kt`): the card's own header + the tab's screenshot (or placeholder) on `Card`/`Base`. Layout height = max(header + window height × cardWidth / windowWidth, cardHeight).
  - Scale lerps from pageWidth/cardWidth to slotWidth/cardWidth; position lerps from the page's top-left to the slot's top-left (`CardHero.kt`).
  - Visible band: top lerps from 44dp (header hidden just above the screen) to 0 (header shown); bottom lerps from the page bottom to the card bottom.
  - Corner radius lerps 0 → 28dp on screen (`CardHero.kt`).
  - Hairline `CardBorder` 1dp drawn at alpha p (`CardHero.kt`).
  - Elevation = `CardElevation` (12dp Light / 20dp Dark), shadow colour `CardShadowColor`.
  - The real card is hidden while it flies (`CardHero.kt`).
- Other cards (`DepthStack.kt`, e = p while flying): cards behind the focus rise out from under the focused slot (translationY from topOf(0) to their place) and fade in (alpha × e); cards in front slide up from below the screen's bottom edge, opaque.
- Space header: alpha = clamp(1.5e − 0.5, 0, 1), translationY = −(1 − e) × header height (slides in from above; gone a third of the way out) (`SpacePage.kt`).
- Fallback (no stack shows the card: new tab, link from another app): the page itself shrinks to 0.85 around the centre (× the zoom), opaque, no header (`HERO_FALLBACK_SCALE`; `CardHero.kt`).
- After the overview settles open, deferred work starts (neighbour spaces composed, blur baking) (`KoanRoot.kt`).

**Page cover** (`UI/browser/PageCover.kt`): on the page side, the tab's screenshot (or placeholder) covers the engine view at once and fades out in 140ms (`COVER_FADE_MS`) once the live page has painted (+32ms), never later than 2500ms (`COVER_MAX_MS`). Background `Base`. A blank new tab stays covered (plain `Base`).

**Screenshot / clip needed:** a short screen recording of tap-to-open and swipe-down-to-overview on a fake tab (example.com), plus one still at ~50% progress showing the card mid-flight with partly rounded corners and the header sliding in.

---

### Back closes a tab (`ClosingPage`) (`ui/browser/ClosingPage.kt`)

Back on a tab's first page closes it. When another tab takes its place (its opener tab, else the
next card), the page's last pixels (a grab that may wait at most 150ms, `PageTokens.CLOSE_GRAB_MS`;
else the tab's thumbnail) are drawn over the next tab coming up and animate away, like a card swiped
off the overview. A tab opened by another app goes back to that app instead (no animation).

- Progress 0 → 1 in 380ms (`PageTokens.CLOSE_MS`), `FastOutSlowInEasing`, read in the layer only.
- Shrink over the first 45%: scale 1 → 0.85 (`Motion.HERO_FALLBACK_SCALE`), origin 50% / 45%, corners 0 → 28dp (`CardTokens.Radius`), shadow up to 24px.
- Fly from 30%: `translationX = −1.15 × width × fly²` (to the left, speeding up), alpha 1 → 0.75.
- Drawn below the status bar (from the very top when the status bar is hidden).

### Start page

**Purpose:** what Koan shows when it comes back after a while unused: the current space's essentials and a "Continue" card for the last tab, above the dock whose search field is focused. Not a tab.

**Anatomy** (`UI/start/StartPage.kt`)
- Full-screen `Base` background.
- Bottom-aligned column, horizontal padding 16dp (`StartPageTokens.SidePadding`), bottom padding = dock height (above keyboard) + 20dp (`StartPageTokens.BottomGap`).
- Space row: icon box 32dp, 10dp corners, `Glass`, Zen icon 20dp `TextPrimary`; 10dp gap; space name 20sp Bold `TextPrimary`, 1 line, ellipsis (same tokens as the overview header).
- 14dp gap (`StartPageTokens.TitleGap`).
- Essentials grid (`UI/bar/SpaceDrawer.kt`): 4 per row, 8dp between tiles (`DrawerTokens.EssentialGap`), 10dp between rows (`DrawerTokens.Gap`); tile height 52dp (`DrawerTokens.EssentialHeight`), corner 14dp (`DrawerTokens.EssentialRadius`), `Glass` fill, favicon 26dp (`DrawerTokens.EssentialFavicon`) centred; empty slots in the last row are blank. Tap opens the tab (ripple). Empty: "No essentials in {space}" 15sp (`DrawerTokens.PinnedTitleSize`) `TextSecondary`.
- Continue card (only if the last tab still exists): 20dp above (`StartPageTokens.ContinueTop`); label "Continue" 13sp (`StartPageTokens.LabelSize`) Medium `TextSecondary`; 8dp (`StartPageTokens.ContinueLabelGap`); row: corner 16dp (`StartPageTokens.ContinueRadius`), `Glass` fill, padding 12dp (`StartPageTokens.ContinuePadding`); a 40dp tile (`StartPageTokens.ContinueTile`), 10dp corners (`StartPageTokens.ContinueTileRadius`), `Base` fill, holding the favicon 24dp (`StartPageTokens.ContinueFavicon`); 12dp gap (`StartPageTokens.ContinueGap`); title 15sp (`StartPageTokens.ContinueTitleSize`) Medium `TextPrimary` (falls back to the host), host 13sp (`StartPageTokens.ContinueSubtitleSize`) `TextSecondary`, both 1 line ellipsis. Tap = back to that tab (same as Back).

**States / motion**
- Open: appears at once (alpha snaps to 1); closing fades out in 180ms (`StartPageTokens.FADE_OUT_MS`) and is not interactive while fading.
- Swipe down > 64dp anywhere on it (`StartPageTokens.SwipeDownToDismiss`), Back, or the field's ✕ dismisses it to the last tab.
- No essentials / no last tab variants.

**Light vs dark:** palette tokens only (Base, Glass, TextPrimary, TextSecondary).

**Screenshot needed:** start page over a fake space with 4–8 fake essentials and a Continue card for an example.com tab, keyboard up with the dock's field focused. Light and Dark. Also the "No essentials" variant.

---

### Error pages

**Purpose:** what the user sees when a page fails to load.

**Finding:** Koan does **not** have its own error page. The engine's only `RequestInterceptor` is `ExternalLinks` (`app/src/main/java/dev/nerzouille/koan/engine/ExternalLinks.kt`, registered at `app/src/main/java/dev/nerzouille/koan/engine/Components.kt`), and it overrides only `onLoadRequest` (`ExternalLinks.kt`); there is no `onErrorRequest`, no `ErrorPages`/`ErrorType` handling and no error-page asset anywhere in `app/src/main` (grep for onLoadError, onErrorRequest, ErrorType, ErrorPage, neterror: no hits). So load errors use **GeckoView's built-in default error pages, unchanged**. They follow the system light/dark preference (`preferredColorScheme = PreferredColorScheme.System`, `Components.kt`).
- The page cover (`PageCover.kt`) lifts after at most 2500ms even if the page never reports a paint, so an error page is never hidden behind a cover.
- Docs: UI_SPEC has no error-page section.

**Screenshot needed:** an unreachable host (e.g. `http://nonexistent.example/`) loaded in Koan in Light and Dark, to show the GeckoView default page inside Koan's page view with the dock.

---

### Pull to refresh

**Purpose:** pull down on a page at its top to reload; a small circle under the top edge shows the pull and then a brand-gradient spinner.

**Anatomy** (`UI/browser/PullRefresh.kt`): a 40dp circle (`PullTokens.Size`) with a 4dp shadow (`PullTokens.Elevation`), `Dock` fill (Light #FFFFFF, Dark #232326), aligned top centre of the page (`BrowserScreen.kt`).
- Pulling: an arc in a 20dp box (`PullTokens.Arc`), stroke 2.5dp (`PullTokens.Stroke`), round caps, starting at 12 o'clock, sweep = 300° × f, f = pull / trigger clamped 0..1; colour `TextSecondary` until f reaches 1, then `TextPrimary`.
- Refreshing: `BrandSpinner` 20dp (stroke 2dp, 270° sweep, 900ms/turn).

**Motion and thresholds**
- Starts only when the page is at its top, not fullscreen, the page does not keep the touch (maps, canvases, own pull), after the system touch slop with a downward, more-vertical-than-horizontal start (`PullRefresh.kt`, `BrowserScreen.kt`). A second finger or the page scrolling cancels it.
- The indicator moves 0.5 × the finger's travel (`RESISTANCE`), max 120dp (`PullTokens.Max`).
- Layer: translationY = pull − 40dp (comes down from above the edge), alpha = pull / (0.6 × trigger) clamped, rotation = pull / trigger × 180° while pulling, 0 while refreshing (`PullRefresh.kt`).
- Trigger: 72dp of indicator travel. Crossing it gives a `CLOCK_TICK` haptic (`BrowserScreen.kt`).
- Release past the trigger: reload; the indicator springs back to the trigger position and spins. Released short: springs to 0. Both springs: `Motion.DockSettle` dampingRatio 1.0, stiffness 800.
- Ends when the reload has loaded, spinning at least 600ms (`PullTokens.MIN_SPIN_MS`); if loading never starts, it lets go after 3000ms (`PullTokens.START_TIMEOUT_MS`) (`BrowserScreen.kt`). Also ends when another tab comes up.

**States:** idle (hidden), pulling (grey arc growing), armed (arc in TextPrimary), refreshing (brand spinner), settling back.

**Light vs dark:** circle `Dock`, arc `TextSecondary`/`TextPrimary` per theme; spinner colours identical.

**Screenshot needed:** (1) mid-pull below the trigger (grey partial arc) on example.com; (2) armed (full-colour arc); (3) refreshing with the brand spinner. Light and Dark.

---

### Video player (`ui/player/`)

Koan's own fullscreen player for page videos (`WebPlayer`), and a standalone one for videos from
Downloads, from other apps, and "Play video" in the link menu (`StandalonePlayer`). Picture in
picture, with an "Audio only" choice. Always dark, whatever Koan's theme. The full spec and its
differences from the build are in `docs/VIDEO_PLAYER.md`; all values are `PlayerTokens`
(`ui/player/PlayerTokens.kt`). Main ones:

| Group | Tokens |
|---|---|
| Colour | `Background` black; `Panel` `#1C1C1E` @ 72% over a blur (`PanelNoBlur` 90%, `PanelMenu` 96%); `Stroke` white @ 8%; `Text` white, `TextSecondary` white @ 80%; track white @ 24% / buffered 40% / played 100%; `Destructive` `#FF5A50`; scrims 120dp top @ 0.45, 200dp bottom @ 0.60 |
| Layout | `Inset` 20dp from every edge; pills 44dp; play disc 72dp (glyph 32dp), side discs 56dp; bottom pill 56dp landscape / 92dp portrait, radius 28dp; track 8dp, 12dp pressed; ⋯ menu 264dp wide, radius 18dp, rows 44dp |
| Motion | controls in `tween(200)`, out `tween(160)`; auto-hide after 3000ms; open spring(1, 420), close spring(1, 520); spinner after 400ms, one turn per 900ms |
| Gestures | side zones 35% of the width; double tap within 700ms skips 10s; long press 400ms = 2×; swipe down past 20% (or 800dp/s) closes; speeds 0.5–2× |

### Private mode

Spec: `docs/design/OVERVIEW_PRIVATE.md` (§4 model, §5 palettes and atmosphere, §6 transition).
Code: `ui/privacy/` (toggle, header title and chip, dock pill, empty state, search notice, lock
cover, atmosphere, the flip driver), `privacy/` (private tabs, vault, engine settings).

**Palettes** (`KoanPalette.Dusk` in the light theme, `KoanPalette.Night` in the dark theme): the
full table is in the spec §5.1. Main values: Base `#DEDAF0` / `#110F24`, Card `#FFFFFF` /
`#1E1C38`, Dock `#F6F4FD` / `#18162F`, text `#17142E` / white, `PrivateAccent` `#4B3FD6` /
`#9C8CFF`, `PrivatePill` accent at 16% / 20%, `PrivateInk` `#2E2399` / `#CFC6FF`. Stack dim to
`DimColor` (`#DEDAF0` haze, gain 1.25, max 0.85 / `#050412`, gain 1.0, max 1.0: the normal stack's v0.4.0 values, owner 2026-11-12). Settings, History,
Downloads and the ⋯ sheet keep the normal palette (`KoanNormalTheme`).

**Toggle** (`PrivateTokens`): 56×48 touch, 56×32 track (radius 16), 26dp white knob at x 3 → 27dp,
mask 17dp inside. Track off `#86868B` / `#636366`, on `#4B3FD6` / `#7B6CF0`; mask off `#6E6E73` /
`#636366`, on `#2E2399` / `#3A2FB8`. `Role.Switch`, "Private browsing". `CLOCK_TICK` on tap; every
tap acts, mid-flip too (its place stays a tap target while the headers slide). (The other toggles keep `ToggleOff` `#848489` / `#8E8E93`: the spec's
`#636366` would be 2.2:1 on a dark grouped row, under the 3:1 rule.)

**Header:** back · "Private" 20sp Bold · 10dp · "Not saved" chip (24dp, radius 12, padding 10,
`PrivatePill`, 12sp SemiBold `PrivateInk`) · toggle. **Dock:** one centred "Private" pill (36dp,
radius 18, mask 20 + 7dp + 14sp SemiBold `PrivateInk`), a label with no action; address pill with
the mask 18 + 8dp + "Search privately" (overview) or the host (page). **Search sheet:** the notice
first (radius 18, Card + PrivatePill, 13sp / 18 Medium `PrivateInk`), "Bookmarks", "Switch to
private tab"; field ring `PrivateAccent`. **Empty state:** §4.7, with a fourth fact: "Private tabs
stay open, but sites sign you out when Koan restarts".

**Atmosphere** (`PrivateAtmosphere`): gradient `#AFA4E4` → transparent over 480dp / `#1F1A48` over
420dp; three radial glows (§5.2); 56 stars and 7 sparkles in the top 360dp, never over the header,
the card pile (its full width under the header) or the empty-state column, nor the dock zone; seed
per install. Static at rest.

**Transition** (`PrivateFlip`, owner's change 2026-11-11, replaces the spec's §6 mist): Private
is one more page beside the space you were on, and a flip is the space pager's own motion between
the two (`Motion.PagerSettle`, page + 40dp gap, same parallax and fade): into Private from the
right, back to normal from the left. The overview gets both modes' models; during a flip it draws
only those two pages, each in its own palette. Both stay ready: the normal one stays composed
while Private shows, and Private's page is composed off screen once the overview has settled, so
nothing composes when a flip starts. The screen is one position (0 normal … 1 Private) on the
pager's spring (damping 0.86, stiffness 300), advanced at most 20 ms per frame (a slow frame
slows it, never skips it). A toggle mid-flip turns it around from where it is, speed kept; the
screen always ends on the latest state. The tabs switch at the tap. Locked: the knob moves at
once, the fingerprint is asked, the screen follows (the knob goes back if it's refused). Private's base colour (72dp soft
edge) and its sky travel with its page; entering, stars and sparkles appear right to left (from
140ms, spread 700ms, 480ms each, small twinkle). Knob 200ms. Dock: colour crossfades with the slide;
its texts dip out and back in around 35% of it, where they switch. Reduced motion: a 150ms slide,
no twinkle.

### Code vs docs/UI_SPEC.md disagreements (code wins)

- Card side margin: spec 12dp, code 14dp (`StackTokens.CardSideMargin`).
- Swipe-close threshold: spec "past 100dp", code 35% of card width or > 800dp/s fling (`StackTokens.SWIPE_CLOSE_FRACTION`, `StackTokens.SwipeCloseFling`).
- Open/close spring: spec damping ≈ 0.85, stiffness ≈ 380 with a tiny overshoot; code `Motion.Overview` damping 1.0, stiffness 600, no overshoot.
- Card title: spec "Visual tokens" 16sp semibold; code 14.5sp SemiBold (`CardTokens.TitleSize`) over an 11.5sp host line (`CardTokens.HostSize`).
- Header: spec "tap space name → space menu"; no such action in `SpaceHeader`.
- Header strip dim: spec "dimmed to 80% at most"; code caps it at 0.2 before the theme gain, so Light can reach 0.25 (DepthCurve.kt, TabCard.kt).


## 3.C sheets, search, menus, toasts, prompts, kit

Every value below comes from the code; the reference after each value is a file path relative to
the repo root, or a token name in `ui/theme/Tokens.kt`. Where `docs/UI_SPEC.md` and the code
disagree, the code is documented and the difference is noted.

### Shared conventions

- **Typography:** no custom font. `KoanTheme` sets only a colour scheme, no `Typography`
  (app/src/main/java/dev/nerzouille/koan/ui/theme/Theme.kt), and there is no `res/font`. Text uses the platform default sans
  (Roboto on stock Android, the OEM font on HyperOS). Weights used: Normal, Medium, SemiBold, Bold.
  (UI_SPEC "Visual tokens" suggests Inter / Google Sans Flex: not implemented.)
- **Pressed state:** every tappable item uses plain `Modifier.clickable` under `MaterialTheme`, so
  the pressed feedback is the default Material 3 ripple, clipped to the item's shape. There is no
  custom pressed colour or scale anywhere in these components. The toggle is the exception: it has
  `indication = null` (no ripple) (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToggle.kt).
- **"interactive" flag:** sheet pages and cards pass `interactive` (true only for the page on top
  of an open sheet/card). When false, rows simply stop being clickable; they look the same.
- **Shapes:** "pill" = `CircleShape` (fully rounded ends).

### Colour tokens used in this file (KoanPalette)

| Token | Light | Dark |
|---|---|---|
| Base | `#F5F5F7` | `#121214` |
| Glass | `#EBEBEF` | `#FFFFFF` @ 8% |
| TextPrimary | `#1C1C1E` | `#FFFFFF` |
| TextSecondary | `#66666B` | `#FFFFFF` @ 62% |
| TextDisabled | `#000000` @ 22% | `#FFFFFF` @ 19% |
| Icon | `#6E6E73` | `#98989D` |
| Card | `#FFFFFF` | `#2C2C2E` |
| CardBorder | `#E4E4EA` | `#FFFFFF` @ 8% |
| Primary | `#1C1C1E` | `#FFFFFF` |
| OnPrimary | `#FFFFFF` | `#1C1C1E` |
| Destructive | `#C42A20` | `#FF8077` |
| GroupedRow | `#F2F2F5` | `#FFFFFF` @ 7% |
| ToggleOff | `#848489` | `#8E8E93` |
| FocusRing | `#B4B4BA` | `#FFFFFF` @ 30% |
| Dock (sheet and card surface) | `#FFFFFF` | `#232326` |
| DrawerHandle | `#000000` @ 16% | `#FFFFFF` @ 20% |
| BarField | `#F1F1F4` | `#FFFFFF` @ 8% |
| SegmentPill | `#FFFFFF` | `#4A4A4D` |
| FieldHint | `#66666B` | `#FFFFFF` @ 60% |
| IconBacking (behind dark favicons) | unspecified (never) | `#E9E9EE` |
| Attention (amber, "not secure") | `#8A5C00` | `#E8B45A` |
| SheetScrim | `#1C1C28` @ 30% | `#000000` @ 50% |

Brand gradient (`BrandTokens.Colors`): Pink `#FF5FA2` → Violet `#B46BFF` → Blue `#4F8DFF`
(`BrandTokens.Pink`, `BrandTokens.Violet`, `BrandTokens.Blue`). `BrandHorizontal` = left-to-right horizontal gradient of those three
(app/src/main/java/dev/nerzouille/koan/ui/theme/Brand.kt). `brandOutline` = a 1.5dp border with that gradient (`BrandTokens.Outline`,
app/src/main/java/dev/nerzouille/koan/ui/theme/Brand.kt).

### Springs used in this file (Motion / tokens)

| Name | dampingRatio | stiffness | Used for |
|---|---|---|---|
| Motion.Drawer | 0.9 | 420 | every sheet/card height (open, close, detent snap) |
| Motion.SheetPush | 1.0 | 520 | a page pushed into / popped from a sheet |
| Motion.Toast | 1.0 | 700 | toast rise / sink / swipe |
| Motion.Sheet | 1.0 | 380 | full-screen pages (ListPage: History, Downloads) sliding up |
| Motion.SearchScrim | 1.0 | 700 | the search sheet's dim |
| ToggleTokens.Spring | 0.72 | 520 | toggle knob |
| SearchFieldTokens.Flow | 1.0 | 140 | "@" gradient flowing into the field ring |

---

### Sheet container (KoanSheet, KoanCard, SheetState, SheetKit)

**Purpose:** one detent engine for every sheet: the over-page sheet (the ⋯ menu) and the small
floating card (prompts, confirmations, link menu); the pins drawer uses the same engine.

There are two containers built on `SheetState` (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt):

#### A. Over-page sheet: `KoanSheet` (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt)

**Anatomy**
1. Scrim: full-screen rect, colour `SheetScrim`, alpha = `state.progress` (0 closed → 1 at the
   compact height, stays 1 above) (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt, app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt).
2. Sheet surface: full width, colour `Dock`, top corners 32dp (`SheetTokens.TopRadius`,
   `SheetTokens.TopRadius`; clip at app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt), no bottom corners (it runs to the screen edge),
   no shadow, no blur (UI_SPEC agrees: "no blur, the page is a live surface").
3. Handle: 36 × 4dp pill (`BarTokens.HandleWidth/HandleHeight`), colour
   `DrawerHandle`, 8dp from the top (`SheetTokens.HandleTop`), centred
   (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt, placed at). Decorative: no tap action on the
   over-page sheet's handle.
4. Pages: each page is a column on `Dock`, with 22dp top padding for the handle zone
   (`SheetTokens.HandleZone`; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt) and the navigation-bar inset
   at the bottom. Page content padding is the page's own (menu: 14dp sides and bottom,
   10dp gaps; `SheetTokens.Padding/Gap`).

**Sizes / detents**
- Compact: the content's height, capped at 55% of the window height
  (`COMPACT_MAX_FRACTION = 0.55`; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt).
- Full: the content's height, capped at window − status-bar inset − 12dp
  (`FullTopGap`; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt). Only exists if it is at least 48dp
  taller than compact (`MinDetentStep`; app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt).
- A pushed page is measured at least as tall as the root page, so the sheet never shrinks when a
  page is pushed (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt).

**States:** Closed (not composed at all), Compact, Full (`Detent`, app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt);
plus "dragging" (follows the finger 1:1) and "pushed page" (depth > 0).

**Motion and gestures** (`sheetGestures`, app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt; engine
app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt)
- Opening: height springs from 0 to the compact height with `Motion.Drawer` (0.9 / 420)
  (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt). The sheet is a `translationY` of the whole layer
  (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt): no relayout per frame.
- A drag starts after the system touch slop (`viewConfiguration.touchSlop`,
  app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt). A drag that is more horizontal than vertical is left to the
  content.
- Drag up grows the sheet to full, then scrolls the list; drag down scrolls the list back to its
  top, then shrinks the sheet (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt). The list only scrolls at full.
- Release: velocity beyond 600dp/s (`SheetTokens.FlingVelocity`) goes to the next detent
  in that direction (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt). Otherwise, between two detents, it commits
  to the next one past 25% of the gap when leaving the detent it started from, and needs 75% when
  coming back (`COMMIT_FRACTION = 0.25`; app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetState.kt). A fling on a
  scrolled full list keeps scrolling with spline decay.
- Landing on a different detent than the one the drag started from: haptic `CLOCK_TICK`
  (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt).
- Tap above the sheet (no drag): closes it (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt). Back pops a pushed
  page, then closes.
- Pushed pages (`SheetNav`, app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt): the new page slides in from the right
  edge (`translationX = width × f`), the covered page trails at 30% (`PUSH_PARALLAX = 0.3`,
  `SheetTokens.PUSH_PARALLAX`; app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt); pages more than one step away are alpha 0. Spring
  `Motion.SheetPush` (1.0 / 520). Closing resets to the root page.

**Light vs dark:** surface `#FFFFFF` vs `#232326`; scrim `#1C1C28` @ 30% vs black @ 50%; handle
black @ 16% vs white @ 20%.

**Screenshot needed:** the ⋯ menu open at the compact detent over a page of `example.com`,
light and dark; a second shot dragged to full (if the content is taller than compact).

#### B. Floating card: `KoanCard` (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt)

**Anatomy**
1. Scrim: `SheetScrim` × progress, as above (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt).
2. Card: `Dock` surface, all corners 28dp (`SheetTokens.CardRadius`), 20dp inner
   padding on all sides (`CardPadding`), 10dp side margins and 10dp bottom margin
   (`CardMargin`). No handle, no shadow, never the brand gradient.
3. Content: the caller's column (no automatic spacing; each prompt sets its own).

**Sizes:** width = screen − 2 × 10dp; height = its content (one open detent: the caps are set to
the window so there is no full detent, app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt). Bottom edge sits 10dp
above the larger of the navigation bar and the keyboard, so a card with a text field rises above
the keyboard without recomposing.

**States:** closed (not composed) / open (Compact). `interactive` = open.

**Motion and gestures:** rises from below the screen edge: `translationY = (1 − shown) ×
(card height + bottom gap)` where shown = height / card height (app/src/main/java/dev/nerzouille/koan/ui/sheet/KoanSheet.kt),
driven by `Motion.Drawer` (0.9 / 420). Same `sheetGestures` as the sheet: drag down to close
(25% commit, 600dp/s fling), tap outside closes, Back closes. Closing any of these ways is
treated as "Cancel" / "Block this time" by the prompts (e.g. app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt,
app/src/main/java/dev/nerzouille/koan/ui/menu/PagePrompts.kt).

**Light vs dark:** card `#FFFFFF` vs `#232326` over the scrim.

**Screenshot needed:** a permission card ("example.com wants to use your location") over a page,
light and dark; one card with a text field and the keyboard up (the JS prompt) to show it riding
above the keyboard.

#### C. Sheet building blocks (app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt)

| Part | Spec | Ref |
|---|---|---|
| `SheetGroup` | full-width column, `GroupedRow` fill, 18dp corners | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt, `SheetTokens.GroupRadius` |
| `SheetDivider` | 1dp `CardBorder` hairline, inset 54dp from the left (12 + 30 + 12, aligned with titles) | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt, `SheetTokens` |
| `MenuIconTile` | the icon of every menu row (sheet menus and dock popups): 30dp tile, 9dp corners, 18dp glyph `TextPrimary` (`TextDisabled` disabled); fill = the neutral that differs from the surface under it (`Dock` on a grouped row, `GroupedRow` on a dock popup) | `ui/sheet/SheetKit.kt` |
| `SheetRow` | height 50dp, 58dp with a subtitle; 12dp side padding; icon = `MenuIconTile` with `Dock` fill (white tile on the grey group in light; `#232326` on white 7% in dark), glyph 18dp `TextPrimary`; 12dp gap; title 16sp Medium `TextPrimary` (or a caller colour), 1 line ellipsis; subtitle 12sp `TextSecondary`; trailing slot | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt, `SheetTokens` |
| `SheetRow` disabled (`enabled = false`) | icon and title `TextDisabled`, trailing replaced by "Soon" (14sp `TextDisabled`), not clickable | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt |
| `SheetRow` without `onClick` | not tappable, no ripple | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt |
| `RowValue` | trailing text 14sp `TextSecondary` | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt |
| `RowChevron` | ChevronRight 18dp `TextSecondary` | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt |
| `SheetTile` | weight 1 in a row (8dp gaps), height 66dp, 16dp corners, `GroupedRow`; glyph 22dp over a 13sp label, 4dp apart; disabled = `TextDisabled` tint and not clickable | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt, `SheetTokens` |
| `SheetHeader` (pushed page) | row 52dp tall; back button 52dp circle with Back glyph 22dp `TextPrimary`; title 17sp SemiBold `TextPrimary` | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt, `SheetTokens.HeaderHeight`, `SheetTokens.HeaderTitleSize` |
| `SheetSectionTitle` | 12sp Medium `TextSecondary`, 12dp start padding, 5dp top padding | app/src/main/java/dev/nerzouille/koan/ui/sheet/SheetKit.kt |

---

### Search sheet (SearchSheet, SearchField, SearchState)

**Purpose:** while the address field is being edited, show results above the field (best match
nearest the thumb), or the "@" engine/scope picker; the page dims behind.

**Anatomy** (app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt)
1. Dim: full-screen `SheetScrim`, alpha animated 0 ↔ 1 with `Motion.SearchScrim` (1.0 / 700)
   (app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt). Tap on the dim = cancel editing.
2. Results column, anchored to the bottom, above the dock: bottom padding = dock zone + 8dp
   (`GroupGap`), 10dp side margins (`SearchTokens.Margin`); max height = screen −
   status bar − 16dp (`StatusGap`) − dock zone − 8dp; scrolls in reverse (starts on
   the bottom); groups 8dp apart, bottom-aligned. The dock zone = 74dp address-row
   zone (68 + 6, app/src/main/java/dev/nerzouille/koan/ui/bar/KoanBar.kt, `BarTokens.BottomMargin`, `BarTokens.AddressRowHeight`) + navigation bar / keyboard inset
   (app/src/main/java/dev/nerzouille/koan/ui/KoanRoot.kt).
3. Groups, top → bottom (furthest → closest to the thumb) (app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt):
   - "History" / "Bookmarks" / "History & bookmarks" (label picked by the sources),
     Reload glyph;
   - "Switch to tab" (open tabs of every space except the current tab), Essentials glyph;
   - the engine's suggestions: label = engine name with its favicon, then a horizontally scrolling
     row of chips;
   - for an "@engine query": one row "Search Wikipedia for “query”" with the engine's favicon.
4. Group card (`Group`): `Card` fill, 18dp corners (`SearchTokens.GroupRadius`);
   label row padding 12dp start / 12dp top / 4dp bottom; optional glyph 14dp `TextSecondary` or a
   14dp favicon, 4dp gap; label 12sp Medium `TextSecondary` (`SearchTokens.LabelSize`, `SearchTokens.LabelPadding`).
5. Divider between rows: 1dp (`ChipGap / 8` = 8 / 8) `CardBorder`, inset 50dp (12 × 2 + 26).
6. Hit row (`HitRow`): height 54dp (`SearchTokens.RowHeight`), 12dp side padding, favicon 26dp
   (`SearchTokens.Favicon`), 12dp gap; title 15sp `TextPrimary` (`SearchTokens.TitleSize`) (falls back to the host), address 12sp
   `TextSecondary` (`SearchTokens.UrlSize`), each 1 line with ellipsis. An open tab in another space adds a space
   chip at the end: pill, `GroupedRow`, padding 8dp × 4dp, space icon 14dp (`SpaceIcon`),
   4dp gap, space name 12sp `TextPrimary`.
7. Suggestion chip (`Chip`): height 34dp (`SearchTokens.ChipHeight`), pill, `GroupedRow`, 12dp side
   padding; Search glyph 14dp `TextSecondary`, 4dp gap; text 14sp Medium `TextPrimary`
   (`SearchTokens.ChipTextSize`). Chip row: 12dp start/end/bottom padding, 8dp between chips (`SearchTokens.ChipGap`).
8. Favicons (`Favicon`, app/src/main/java/dev/nerzouille/koan/ui/overview/Favicon.kt): rounded square, corners 28% of the
   size; a letter tile until the icon loads, then a fade; in dark, icons too dark to read get a
   `#E9E9EE` tile behind them (`IconBacking`).

**"@" picker** (`Picker`, app/src/main/java/dev/nerzouille/koan/ui/search/SearchSheet.kt), shown while the text is "@" or
"@" + a partial key with no space yet, and something matches:
- Panel: full width (no side margins), top corners 28dp (`PickerRadius`), `Dock` fill,
  16dp padding (`PickerPadding`), children 8dp apart; sits directly on the dock (bottom
  padding = dock zone, no gap) with the same max height. It has no scroll modifier.
- "Search with" label, 12sp Medium `TextSecondary`; engine tiles two per row (an odd last tile
  keeps half width), 8dp gap: height 58dp (`TileHeight`), 18dp corners, `GroupedRow`,
  12dp side padding, favicon 26dp, 12dp gap, name 15sp Medium `TextPrimary`, then "@key" 12sp
  `TextSecondary`, with " · default" on the default engine.
- "Search in" label (8dp extra top padding), then scope chips (same `Chip`): Tabs (Essentials
  glyph), History (Reload glyph), Bookmarks (Star glyph).
- Matching: what follows "@" must start the key or the name, case-insensitive; empty matches all.
  Tapping a tile or chip puts "@key " in the field.
- Built-in engine shortcuts: `@ddg` DuckDuckGo, `@g` Google, `@wiki` Wikipedia (device language),
  `@yt` YouTube, `@bd` Baidu, then each custom engine that has a shortcut
  (app/src/main/java/dev/nerzouille/koan/search/SearchModel.kt).
  Scopes: `@tabs`, `@history`, `@bookmarks` (app/src/main/java/dev/nerzouille/koan/search/SearchModel.kt).

**The field itself** (state in `SearchField`, drawn by the dock's address row
app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt and the ring app/src/main/java/dev/nerzouille/koan/ui/bar/SearchRing.kt):
- Pill 50dp tall (`BarTokens.FieldHeight`), `BarField` fill, search glyph 18dp `FieldHint`
  with a 10dp gap (`SearchFieldTokens.Glyph`, `SearchFieldTokens.GlyphGap`), text 17sp `TextPrimary` (`BarTokens.FieldTextSize`), hint "Search or type URL" in
  `FieldHint` (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt).
- Editing starts with the page's address fully selected (app/src/main/java/dev/nerzouille/koan/ui/search/SearchField.kt).
- Inline completion: what was typed stays, the rest of the top hit's address follows selected;
  the completed page's title follows in `FieldHint` (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt).
- Text scrolls to keep the cursor 40dp inside the edges (`CursorEdge`).
- Ring, focused: neutral 1.5dp `FocusRing` stroke. "@" first: the brand gradient
  (a sweep gradient around the field centre) flows in from the left along both halves of the pill
  with spring `Flow` (1.0 / 140) (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt), stroke grows to 2dp (`SearchFieldTokens.AtRing`), a
  faint 1.5dp halo at 14% (`SearchFieldTokens.Halo`, `SearchFieldTokens.HALO_ALPHA`), the neutral ring fades out as `1 − f²`
  (app/src/main/java/dev/nerzouille/koan/ui/bar/SearchRing.kt). In "@" mode the gradient turns at 48°/s plus two wobbles
  (±14° over 3.1 s, ±10° over 4.7 s) (`SearchFieldTokens`, app/src/main/java/dev/nerzouille/koan/ui/bar/SearchRing.kt); not at all when
  the system animator scale is 0 (app/src/main/java/dev/nerzouille/koan/ui/bar/AddressRow.kt). The cursor turns into the brand
  gradient in "@" mode.

**Timing (SearchState):** local results 40ms after typing pauses, suggestions 180ms, at most 40
rows per source (app/src/main/java/dev/nerzouille/koan/ui/search/SearchState.kt). Suggestions come from the default engine,
or from the shortcut's engine for "@yt query"; none for a scope. (The class comment at
 still says "DuckDuckGo's suggestions": outdated, the code uses the chosen engine.)

**States:** closed (not composed once the dim reached 0); open with nothing to show yet (dim only);
results; "@" picker; "@engine query" (suggestions from that engine + the "Search … for" row).
Rows: default / pressed (ripple). No selected or disabled row states.

**Light vs dark:** groups `#FFFFFF` vs `#2C2C2E` (`Card`); chips and space chips `#F2F2F5` vs white
@ 7%; picker panel `#FFFFFF` vs `#232326` (`Dock`); dim `#1C1C28` @ 30% vs black @ 50%; neutral
ring `#B4B4BA` vs white @ 30%.

**Screenshots needed:** (1) typing "exa" on a page: history group + switch-to-tab group (fake
tabs, `example.com`, `example.org`) + suggestion chips, inline completion visible in the field;
(2) "@" typed: the picker with the gradient ring; (3) "@wiki koan": suggestion chips from
Wikipedia + "Search Wikipedia for “koan”". Each in light and dark.

---

### Browser menu (BrowserMenu: MenuRoot)

**Purpose:** the ⋯ menu: page navigation, quick actions, page options, links to History /
Downloads / Settings, and the content blocker. Lives in a `KoanSheet` (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt).

**Anatomy** (app/src/main/java/dev/nerzouille/koan/ui/menu/BrowserMenu.kt), column with 14dp side and bottom padding, 10dp
between blocks:
1. Nav row (`NavRow`), 56dp tall (`NavRowHeight`): Back, Forward icon buttons;
   the address pill; Copy link; Reload (or Close = stop while loading).
   - Icon button (`NavIcon`): 48dp circle touch (`BarTokens.IconTouch`), glyph
     24dp, tint `Icon`, `TextDisabled` when unavailable (and not clickable).
   - Address pill: weight 1, 44dp tall (`NavFieldHeight`), pill, `BarField`, 12dp side
     padding, centred; lock glyph 14dp `TextSecondary` for https (`SheetTokens.NavLockGlyph`) + 8dp gap; host 16sp
     Medium `TextPrimary` (`SheetTokens.NavFieldTextSize`), or "New tab" in `FieldHint` when there is no page. Tap = edit
     the address.
2. Quick tiles row, 8dp gaps (`TileGap`): New tab (Plus), Bookmark / Bookmarked (Star /
   StarFilled), Share, Find. Tiles = `SheetTile`; all but New tab disabled without a page.
3. Group of rows: Site settings (Lock; subtitle "Secure" / "Not secure"; chevron) ·
   Desktop site (toggle) · Reader view / Leave reader view (value "On" / "Available" /
   "Not available") · Essentials row (Add to / Remove from Essentials; "In Essentials" + "From
   Zen"; "Add to Essentials" + "No page") · optional "Add this search engine" (subtitle = engine
   name, chevron) · History · Downloads (value "N active" while downloading, else chevron) ·
   Settings.
4. uBlock card (`UBlockCard`, app/src/main/java/dev/nerzouille/koan/ui/menu/UBlockPage.kt), its own `SheetGroup` with one row:
   Shield glyph, "uBlock Origin", subtitle "N requests blocked on this page" / "1 request blocked
   on this page" / "Off", trailing toggle; tap (when on) pushes the uBlock page. Not installed:
   subtitle "Blocks ads and trackers · Install", tap installs.

**States:** no page (address "New tab" in hint colour, tiles disabled, Site settings without
subtitle or chevron); loading (stop ✕ instead of reload); bookmarked; reader available / active;
active downloads. Rows: default / pressed ripple / disabled.

**Motion:** the sheet's (see Sheet container). Most rows close the menu, then act
(app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt).

**Light vs dark:** sheet `#FFFFFF` vs `#232326`; groups and tiles `#F2F2F5` vs white @ 7%; row icon
boxes `#FFFFFF` vs `#232326`; address pill `#F1F1F4` vs white @ 8%.

**Screenshot needed:** the menu at compact on `https://example.com` (lock shown), uBlock card
peeking at the bottom; then full; then the same over a new blank tab (disabled tiles). Light and dark.

#### Site settings page (app/src/main/java/dev/nerzouille/koan/ui/menu/SiteSettings.kt)

Pushed in the menu sheet. Column 14dp side/bottom padding, 10dp gaps.
- Header row 62dp tall (52 + 10): back button 52dp circle, Back glyph 22dp; favicon 40dp
  (30 + 10); 12dp gap; host 17sp SemiBold `TextPrimary`; under it a 14dp lock + 4dp gap +
  "Connection is secure" 12sp `TextSecondary`, or "Connection is not secure" with lock and text in
  `Attention` amber (`#8A5C00` light / `#E8B45A` dark).
- "Permissions" section title; group with one row per decided permission: glyph, label
  (Location, Camera, Microphone, Camera and microphone, Storage, Protected content, Cross-site
  cookies, Local network, Virtual reality; `app/src/main/java/dev/nerzouille/koan/engine/SitePermissions.kt`),
  subtitle "Allowed" / "Blocked", trailing toggle. Loading: a "…" row; empty: "Nothing
  allowed or blocked yet" / "Koan asks the first time a site wants something".
- Group with "Reader view" (subtitle "Simplified, distraction-free view" or "Not available on this
  page"; toggle only when available).
- "Cookies and site data" caption 12sp `TextSecondary`, centred, 10dp top padding; then a
  full-width 50dp pill on `GroupedRow` with "Clear site data" 16sp Medium `Destructive`.
  It opens a `Confirm` card "Clear data for example.com?" (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt).

**Screenshot needed:** Site settings for `example.com` with two fake decisions (Location allowed,
Camera blocked), light and dark; plus the not-secure header (amber) on an `http://` page.

#### uBlock page (app/src/main/java/dev/nerzouille/koan/ui/menu/UBlockPage.kt)

Pushed in the menu sheet: `SheetHeader` "uBlock Origin", then a 520dp tall box
(`UBlockPanelHeight`), 18dp corners, `GroupedRow`, holding uBlock's own popup in an engine
view; "…" in `TextSecondary` until it arrives. The panel's own look is uBlock's, not Koan's.

**Screenshot needed:** the uBlock page pushed over the menu on `example.com` (shows uBlock's panel).

#### Link context menu (LinkMenu) (app/src/main/java/dev/nerzouille/koan/ui/menu/LinkMenu.kt)

**Purpose:** long press on a link or image: what to do with it. On a `KoanCard`, with a haptic
`CLOCK_TICK` as it opens (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt).

**Anatomy** (column, 10dp gaps):
1. Image preview (images only): full width, 140dp tall (`LinkImageHeight`), 18dp corners,
   `GroupedRow` placeholder from the first frame; the image (crop) fades in over 180ms tween
   (`IMAGE_FADE_MS`). Downloads at most 4 MB, decoded to ≤ 800px.
2. Header: 6dp side padding; link favicon 30dp + 12dp gap (links only); title 16sp SemiBold
   `TextPrimary` (link text, else the host), address 12sp `TextSecondary` up to 2 lines.
3. Link group: Open in new tab (Plus) · Open in background (Essentials) · Copy link (Link) ·
   Share link (Share).
4. Image group: Save image (Download) · Open image in new tab (Plus) · Copy image link (Link).
   An image that is also a link shows both groups.

**States:** link / image / image-link; rows default / pressed.

**Screenshot needed:** long press on a link on `example.com` (link only), and on an image (preview +
both groups for an image link). Light and dark.

---

### Translate page and offer (app/src/main/java/dev/nerzouille/koan/ui/menu/TranslatePage.kt, app/src/main/java/dev/nerzouille/koan/ui/browser/TranslateOfferHost.kt)

Fully on the phone (Firefox Translations in Gecko).

**Translate page** (⋯ › Translate page, a pushed sheet page; 14dp side and bottom padding, 10dp
gaps): "Translate page" 18sp SemiBold; a `SheetGroup` with From / To rows (value + chevron, each opens
a language list); a status line 14sp `TextSecondary` ("Pages are translated on this phone, never
sent anywhere.", with "Downloads N MB once." first when models are needed; errors in `Destructive`);
full-width `PrimaryButton` "Translate to {language}" ("Translating…" while it works), or "Show
original" `SecondaryButton` + "Translate again" side by side once translated; a `SheetGroup` of three
toggle rows: Always translate {language}, Never translate {language}, Never translate this site.
On a phone Gecko can't translate on: "Translation doesn't work on this phone." only.
- **Working**: a 4dp track (`TranslateTokens.Track`) in `Glass`, a brand-gradient segment of 35% of the width sweeping across every 1100ms (`SWEEP_MS`), then "Downloading the language (N MB) and translating…" or "Translating…".
- **Language list**: ‹ back, a search field, then sections in the first that fits: Recent (the last 3 picked on that side, `TranslateTokens.RECENT`), On this phone (downloaded models; a long press deletes one), All languages (A–Z, with the download size when known). Searching shows only the matches, without sections.

**Offer** (`TranslateOffer`, in a `KoanCard`): a page in another language than the phone's, once per
page, not while the overview, editing, a sheet or fullscreen is up. `Translate` 18dp + 12dp + "This
page is in {language}" 16sp Medium, ✕ (14.4dp glyph in a 32dp circle, `TranslateTokens.Close`);
full-width "Translate to {language}"; three quiet links 14sp `TextSecondary` (Always for / Never for
{language}, Never for this site). It leaves on its own after 20s (`OFFER_MS`); ✕, a tap outside,
Back or a drag down dismiss it for that page. A language set to Always translate gets no offer:
Koan starts the translation itself if Gecko hasn't after 1.5s (`AUTO_WAIT_MS`). When it is done, a
toast "Page translated" (or "Translated from {language}") with "Show original".

| Token (`TranslateTokens`) | Value |
|---|---|
| `SWEEP_MS` | 1100 |
| `Track` | 4dp |
| `Close` | 32dp |
| `OFFER_MS` | 20000 |
| `AUTO_WAIT_MS` | 1500 |
| `RECENT` | 3 |

### Toast (KoanToast)

**Purpose:** the one quiet bottom confirmation ("Tab closed · Undo", "Removed from history · Undo",
"example.pdf downloaded · Open"), at most one action.

**Anatomy** (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToast.kt)
1. Container: full width minus 12dp margins each side (`ToastTokens.Margin`); height 50dp,
   60dp with a detail line (`ToastTokens.Height`, `ToastTokens.TallHeight`); 16dp corners (`ToastTokens.Radius`); fill `Primary`
   (`#1C1C1E` in light, `#FFFFFF` in dark: an inverted pill); no shadow.
2. Text column, 18dp start padding (`ToastTokens.PaddingH`): message 15sp (`ToastTokens.TextSize`) `OnPrimary`, Normal weight,
   SemiBold when there is a detail line; detail 12sp (`ToastTokens.DetailSize`) `OnPrimary` @ 65% (`ToastTokens.DETAIL_ALPHA`); 1 line
   each, ellipsis.
3. Action (optional): full-height tap area with 18dp side padding, label 15sp SemiBold `OnPrimary`.
   Tap = the toast goes away, then the action runs.
4. "Moment" variant (`moment = true`, e.g. a finished download): a 1.5dp brand-gradient outline
   around the pill (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToast.kt, `BrandTokens.Outline`).

**Position:** bottom-centre, its bottom edge 10dp (`Gap`) above the dock zone: 74dp on a
page, 134dp in the overview (60 + 68 + 6) (app/src/main/java/dev/nerzouille/koan/ui/bar/KoanBar.kt, app/src/main/java/dev/nerzouille/koan/ui/KoanRoot.kt),
plus the navigation bar or keyboard inset (above the keyboard while it is up).

**Timing:** shown for 3000ms (`SHOW_MS`), then sinks away. A new toast replaces
the current one and restarts the timer. Any sheet, card, full page, the drawer or sign-in
opening dismisses it at once (app/src/main/java/dev/nerzouille/koan/ui/KoanRoot.kt, app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt). The
"copied" toast only exists below Android 13 (the system confirms on 13+).

**Motion and gestures**
- In / out: `shown` 0 ↔ 1 with `Motion.Toast` (1.0 / 700): alpha = shown, rises 16dp
  (`Rise`).
- Hand-over: a new toast never just swaps the text. The one showing leaves on its own
  (`Motion.Toast`): it sinks 1.5 × `Rise` below its spot, shrinks by 6% and fades, under the new
  one rising in its place. Its action is off while it leaves.
- Swipe sideways: follows the finger, alpha falls with |dx| / width; released past 30% of its width
  (`SWIPE_FRACTION`) or faster than 800dp/s (`SwipeFling`) it flies off to ±width
  with `Motion.Toast`.
- Swipe down (up is clamped to 0): past 60% of its height (2 × SWIPE_FRACTION) or faster than
  800dp/s down = dismissed. Otherwise it springs back.

**States:** message only / with action / with detail (tall) / moment (gradient outline). Action:
default / pressed ripple; not clickable while it is leaving.

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
  20dp (18 + 2) `TextPrimary` (app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt).
- **Card title** 18sp SemiBold `TextPrimary` (`CardTitleSize`); **card text** 14sp
  `TextSecondary` (`CardTextSize`).
- **Secondary buttons on cards**: the kit's `SecondaryButton` (50dp pill, `GroupedRow`, 16sp
  SemiBold `TextPrimary`), `enabled = interactive`, no side padding when two share a row by weight.

#### Permission prompt (app/src/main/java/dev/nerzouille/koan/ui/menu/PermissionPrompt.kt)

**Anatomy:** column with 10dp gaps: [icon tile + 12dp gap + title "example.com wants to use your
location" + reason ("It can see where this phone is.")]; 5dp spacer; row of "Block" and
"Always allow" SecondaryButtons (weight 1 each, 10dp gap, `ButtonTokens.ChoiceGap`); full-width PrimaryButton "Allow once".
Glyph per permission: Location, Camera (camera and camera+mic), Mic, Database (storage), Lock
(protected media), Link (cross-site cookies), Globe (local network), Desktop (VR).
**Behaviour:** Block = block and remember; Always allow = allow and remember; Allow once = allow now;
closing the card (tap outside, drag, Back) = blocked this time, nothing remembered
(app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt).
**Screenshot:** location request from `example.com`, light and dark.

#### Download confirmation (app/src/main/java/dev/nerzouille/koan/ui/menu/DownloadPrompt.kt)

Shown when a page starts a download with no tap just before. Icon tile with the Download glyph;
file name 18sp SemiBold, middle ellipsis; detail "1.2 MB · example.com" 14sp `TextSecondary`
(size omitted when unknown) (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt); 5dp spacer; `CardChoices`: "Cancel" +
"Download", weight 1 each, 10dp gap. Closing the card = Cancel.
**Screenshot:** "example.pdf", "1.2 MB · example.com", light and dark.

#### Duplicate download (app/src/main/java/dev/nerzouille/koan/ui/menu/DuplicateDownloadPrompt.kt)

A download of a file that is already there, in the same card as the download confirmation (the
action card morphs between them; the thumbnail has shared bounds with the image card). Column with
10dp gaps:
- Thumbnail 50dp (30 + 2 × 10), 9dp corners, `GroupedRow` (the file's picture, else `File` 18dp over its extension), 12dp, then "Already downloaded" 14sp `TextSecondary`, the name 18sp SemiBold (`KoanType.headline`, middle ellipsis) with a pencil (`Edit` 20dp `TextSecondary` in a 44dp circle, `SheetTokens.EditGlyph`, `EditTarget`), and "In {folder}" 14sp `TextSecondary`.
- A tap on the name or the pencil edits it in place: only the base name is selected (the extension stays), a 1dp `TextSecondary` underline, the pencil turns into ✓; Done or a choice confirms.
- `SheetGroup` of three rows: Open (glyph by type, "The file already there"), Replace (`Download`, "Downloads it again over the old one · size"), Keep both (`Plus`, "Saves as name (1).ext · size").
- `CardCancel` under the group.

#### Open in app (app/src/main/java/dev/nerzouille/koan/ui/menu/AppLinkPrompt.kt)

A page wants to open an app. Floating card, column with 10dp gaps: the app's own icon 40dp (30 +
10; else a `GroupedRow` tile with `Open` 18dp), 12dp, "Open in {app}?" 18sp SemiBold (1 line) over
"{site} wants to open the app" 14sp `TextSecondary`; a row "Always for this site" (50dp,
`GroupedRow`, 18dp corners, 16sp `TextPrimary`, trailing `KoanToggle`, the whole row toggles); 5dp;
`CardChoices`: "Cancel" + "Open", weight 1 each, 10dp gap. Closing the card = Cancel.
**Screenshot:** a demo page's app link, light and dark.

#### File prompt (app/src/main/java/dev/nerzouille/koan/ui/menu/FilePrompt.kt)

No Koan UI: a page's `<input type=file>` opens Android's chooser (files/photos), with the camera
offered when the field takes images or video, straight to the camera for a capture field; asks for
the camera permission first if needed. Nothing picked = the field stays empty.
**Screenshot:** none (system UI only).

#### Login prompts (app/src/main/java/dev/nerzouille/koan/ui/menu/LoginPrompts.kt)

- **Login chips** (`LoginChips`), not on a card: a horizontally scrolling row just above
  the keyboard, at the page dock zone (74dp) (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt); padding 10dp × 8dp,
  8dp gaps. First a 34dp circle on `Card` with a Key glyph 18dp (opens the full list); then one
  chip per account: 34dp tall pill on `Card`, 12dp side padding, initial avatar + 8dp gap + masked
  username 14sp `TextPrimary`. Masking keeps half the name (1 to 6 chars) then "••••" and the
  domain: "alex.r••••@example.com".
- **Initial avatar**: 26dp circle `GroupedRow`, first letter uppercased 12sp SemiBold.
- **"Sign in to example.com with" card** (`LoginSheet`): column 10dp gaps; favicon 40dp +
  12dp gap + title 18sp SemiBold + "Koan passwords" 12sp `TextSecondary`; a `SheetGroup` of 58dp
  rows (avatar, 12dp gap, masked name 16sp, radio); PrimaryButton "Use", QuietButton "Not now".
  The most recently used account is preselected.
  - Radio: 22dp circle (26 − 4); selected = `Primary` fill with a 14dp Check in
    `OnPrimary`; unselected = a 2dp ring in `ToggleOff` (`LoginTokens.RadioRing`), ≥ 3:1 on the
    group (was a `GroupedRow` fill on the `GroupedRow` group: invisible).
- **Save / update password card** (`SaveLoginCard`): favicon 40dp + title "Save the
  password for example.com?" (or "Update the password for example.com?") 18sp SemiBold + masked
  username 12sp; body "Kept encrypted on this phone only; it isn't added to your Mozilla account."
  14sp `TextSecondary`; PrimaryButton "Save in Koan"; QuietButton "Not now".
**Screenshots:** chips over the keyboard on a fake login form (`example.com`, two fake accounts);
the "Sign in with" card; the save card. Light and dark. Never real accounts.

#### Page prompts: alert / confirm / text and the rest (app/src/main/java/dev/nerzouille/koan/ui/menu/PagePrompts.kt)

Building blocks (the card's column has no automatic spacing; each block adds its own):
- `PromptTitle` 18sp SemiBold `TextPrimary`, up to 3 lines. Dialog titles say
  "example.com says" (the host, not the engine's long title).
- `PromptMessage`: 6dp spacer (`TextGap`), then the text 16sp `TextSecondary`, scrolling
  past 220dp (`MessageMaxHeight`).
- `PromptField`: 10dp spacer; a `KoanFieldFrame` (50dp pill, `Glass`, 16sp text and hint);
  password fields masked; the JS prompt's field is focused with its text selected.
- `NoMoreDialogs`: 10dp spacer; row with 12dp corners and 8dp vertical padding: tick box + 12dp
  gap + "Don't let this page show more dialogs" 14sp. Offered when a dialog comes within 3000ms of
  the previous answer (`MANY_DIALOGS_MS`).
- `CheckMark` tick box: 22dp, 6dp corners, 2dp border `TextSecondary` unticked; ticked = `Primary`
  fill + border with a 16dp Check in `OnPrimary` (`PromptTokens.CheckRadius`, `PromptTokens.CheckGlyph`).
- `Buttons`: 20dp spacer; secondary (and third) buttons side by side (weight 1, 8dp gap), 10dp
  spacer, then the full-width PrimaryButton. The secondary here is a private
  `SecondaryButton`: 50dp, pill, `GroupedRow`, 16sp **Medium**, 1 line (the kit's
  `SecondaryButton` uses SemiBold; see Buttons).

Variants: Alert ("OK"); Confirm (page's own labels, default "OK" / "Cancel", optional
third); Text prompt (label, field, "OK" / "Cancel"); "Leave this page?" / "Changes you made may not
be saved." ("Leave" / "Stay"); HTTP sign-in "Sign in to example.com" with Username and Password
fields, "That didn't work. Check the username and password." after a failure ("Sign in" /
"Cancel"); "example.com wants to open a pop-up" + target host ("Allow" / "Block"); "Send the form
again?" ("Resend" / "Cancel"). Closing the card any other way = cancel.

`<select>` list (`ChoiceList`): scrolls past 380dp (`ListMaxHeight`), opens two
rows above the selected option. Option row ≥ 50dp, 12dp corners, 12dp start padding (28dp inside an
optgroup, `PromptTokens.GroupIndent`), 16sp; selected (single) = `GroupedRow` fill, SemiBold, Check 18dp at the end;
disabled option `TextDisabled`; optgroup title 12sp SemiBold `TextSecondary` (10dp top, 6dp bottom);
separator 1dp `GroupedRow` with 6dp vertical padding. A tap picks at once; multiple = tick boxes +
"Done" PrimaryButton.

Date / time (`TimeSelection`): Material 3 `DatePicker` (no title, no headline, no mode
toggle) and `TimePicker`, recoloured to Koan tokens: container `Dock`, selected day/year and
selector `Primary` with `OnPrimary` content, today border `TextSecondary`, disabled `TextDisabled`,
clock dial `GroupedRow`. Date-and-time shows the date first, "Next", then the time.
Buttons "Set" (or "Next") + "Clear". Month (`MonthGrid`): "‹ 2026 ›" row (44dp circle
steppers, 18sp; title 18sp SemiBold), 12 month tiles 3 per row, aspect 2:1 (`PromptTokens.MONTH_ASPECT`), 12dp corners,
8dp gaps, selected `Primary` / `OnPrimary`, others `GroupedRow`, out of range `TextDisabled`.

Colour (`ColorPicker`): "Choose a colour"; 12 swatches (`PromptTokens.Swatches`:
`#000000 #8E8E93 #FFFFFF #FF3B30 #FF9500 #FFCC00 #34C759 #00C7BE #007AFF #5856D6 #AF52DE #FF2D55`),
6 per row, circles, 8dp gaps, 1dp `GroupedRow` ring, the chosen one a 3dp `TextPrimary` ring
(`PromptTokens.SwatchRing`); then a 44dp preview circle + hex field (hint "Hex, like 1a2b3c"); "Set".

**Screenshots needed:** alert "example.com says" with a short message (light, dark); confirm with
OK / Cancel; text prompt with the keyboard up; the many-dialogs tick box; a `<select>` with an
optgroup; date picker; month grid; colour picker. All on a local test page with fake text.

#### Sign-out confirm and other confirmations (app/src/main/java/dev/nerzouille/koan/ui/menu/SignOutConfirm.kt)

`Confirm`, column 10dp gaps: title 18sp SemiBold; text 14sp `TextSecondary`; 5dp spacer;
full-width `DestructiveButton` (the action); full-width `QuietButton` "Cancel". Sign-out: "Sign
out?" / "Your Zen Spaces leave this phone until you sign in again. Tabs opened only on this phone
stay." / "Sign out". The same `Confirm` is used for "Clear data for example.com?"
(app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt) and "Clear the download list?".
**Screenshot:** the sign-out card from Settings, light and dark.

---

### Buttons (app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt)

**Purpose:** one `KoanButton(kind = Primary | Secondary | Destructive | Quiet)`; the four named
buttons below are one-line wrappers. All 50dp tall (`ButtonTokens.Height`), pill
shape, 28dp side padding (`ButtonTokens.PaddingH`), label 16sp (`ButtonTokens.TextSize`), content centred.

| Variant | Fill | Label | Ref |
|---|---|---|---|
| `PrimaryButton` | `Primary` (`#1C1C1E` light / `#FFFFFF` dark) | `OnPrimary`, SemiBold; optional leading slot, 10dp gap (`ButtonTokens.IconGap`) | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt |
| `QuietButton` | none | `TextPrimary`, Medium | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt |
| `SecondaryButton` | `GroupedRow` (`#F2F2F5` / white @ 7%), overridable `surface` | `TextPrimary`, SemiBold | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt |
| `DestructiveButton` | `GroupedRow`, overridable `surface` (`Card` when it sits on `Base`, e.g. History/Downloads' Clear) | `Destructive` (`#C42A20` / `#FF8077`), **SemiBold**; never a red fill | app/src/main/java/dev/nerzouille/koan/ui/kit/Buttons.kt |

Cards: two choices side by side are `CardChoices` (Cancel on the left, the action on the right,
10dp gap, no side padding); the permission card and the page prompts put their secondaries side
by side the same way above a full-width primary. Labels are one line. **One destructive button:**
History/Downloads' "Clear", Site settings' "Clear site data", Delete (search engine) and the
confirm cards all use `DestructiveButton` (their own pills are gone). A destructive *row* ("Sign
out", "Delete password") is a `SettingsRow` with a red title; a destructive selection-bar action
has a red glyph and label. The one red fill in the app is the library rows' delete swipe
background (`ui/kit/SwipeRow.kt`: History, Downloads, Passwords).

**States:** default; pressed = Material 3 ripple clipped to the pill. **No disabled style**:
`enabled = false` (a card closing) only removes the click and the ripple. Never the brand gradient (UI_SPEC agrees).

**Light vs dark:** Primary flips black ↔ white; the others only change their fill/label tokens.

**Screenshot needed:** a permission card (Primary + two secondaries) and the sign-out card
(Destructive + Quiet), light and dark; Primary with a leading icon if one is used on the sign-in
screen.

---

### Field (`KoanField`, `KoanFieldFrame`) (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanField.kt)

**Purpose:** the one text field outside the dock.

**Anatomy:** height **50dp** (`FieldTokens.Height`, the same as a button), full pill, `Glass` fill
(`#EBEBEF` / white 8%), 18dp side padding; optional leading `Search` glyph 20dp `FieldHint` + 12dp
gap; hint and input **16sp** (hint `FieldHint`, input `TextPrimary`, solid `TextPrimary` cursor);
a search field shows a ✕ (32dp circle, 16dp `Close` glyph `FieldHint`, end padding 9dp) once it
has text. `KoanFieldFrame` is the same frame for callers that drive their own `BasicTextField`
(page prompts: `TextFieldValue`, focus, password mask).

**Where:** History and Downloads search (bottom of `ListPage`), Passwords search, the four
search-engine form fields (label 13sp SemiBold `TextSecondary` above, help/error 12sp under),
page prompts (`prompt()` / login dialogs). Replaced: ListPage's 48dp `BarField` pill,
Passwords' 46dp `SearchBox`, the engine form's 48dp / 14dp-corner box with 14sp text, and page
prompts' 44dp / 12dp-corner `BarField` box.

**Not this:** the dock's address field and find-in-page (50dp pill on `BarField`, 17sp) are the
dock's own control; the ⋯ menu's address chip is a button.

---

### Page header (`PageBar`, `CollapsingBar`) (app/src/main/java/dev/nerzouille/koan/ui/kit/PageHeader.kt)

**Purpose:** one header for every full page (UI_SPEC "Page header").

**Bar** (`PageBar`): **56dp** tall (`SettingsTokens.TopBar`), under the status bar, 20dp side
padding; back target 56dp circle with `KoanIcons.Back` **24dp** (`BackGlyph`) `TextPrimary`,
start-aligned (content description "Back"); title **22sp** (`BarTitleSize`, `KoanType.title`:
Bold, 1.18em, −0.012em) `TextPrimary`, 1 line, ellipsis, weight 1; optional trailing slot
(Downloads' folder chip).

**Collapsing large title** (`SettingsPageFrame` + `CollapsingBar`; every settings page: Settings,
About, Passwords, a site's passwords, Search engine, Add/Edit search engine, Mozilla account,
Save downloads to): the scrolling column starts with a 56dp spacer, then the large title
**34sp** (`KoanType.largeTitle`: Bold, 1.12em, −0.016em), up to **2 lines** (ellipsis after),
12dp under it, then the content. The bar sits on top, fixed. With `h` = the large title's height
and `y` = the scroll offset, all read in layer / draw lambdas (scrolling never recomposes):

| Part | Value |
|---|---|
| Bar surface (`Base` + settings wash, drawn at the page's origin so it is seamless) | alpha = clamp(y / h) |
| Large title | alpha = 1 − clamp(y / (0.6·h)) |
| Compact title in the bar | alpha = clamp((y/h − 0.6) / 0.4) (`BAR_TITLE_FROM` = 0.6) |
| Bottom hairline (1dp `CardBorder`) | alpha = clamp((y − h) / 12dp) (`BarHairlineFade`) |

The two titles never show at once. Collapsed, the bar stays put. No snap: it follows the finger.

**List pages** (History, Downloads, `ListPage`) show `PageBar` directly (always "collapsed", no
large title, no surface: the list scrolls in its own area under it).

**Not this:** a sheet's pushed pages keep `SheetHeader` (52dp, 17sp SemiBold); the sign-in
screen keeps its ✕.

**Before the audit:** settings pages had the ‹ on its own 56dp row and a 34sp title that
scrolled away (no bar once scrolled); History/Downloads had ‹ + a 23.8sp title on one row;
About had no title.

---

### Toggle (KoanToggle) (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanToggle.kt)

**Purpose:** Koan's switch.

**Anatomy:** track 50 × 30dp (`ToggleTokens.Width`, `ToggleTokens.Height`), fully rounded; knob = white circle, radius 13dp
(height / 2 − 2dp inset, `ToggleTokens.KnobInset`); soft knob shadow: radial gradient black @ 22% from 75%
of the radius to transparent, radius knob + 3dp, offset 1dp down (shadow / 3) (`ToggleTokens.KnobShadow`, `ToggleTokens.KNOB_SHADOW_ALPHA`). ON = the brand gradient across the full track width, revealed from the left edge up to
the knob's right side + inset, at 90% alpha (`TRACK_ALPHA`) over the OFF track.

**States:** off (track `ToggleOff`: `#848489` light / `#8E8E93` dark); on (gradient); moving;
read-only (`onCheckedChange = null`: same look, not toggleable). No pressed visual (no ripple), no
disabled look.

**Motion:** knob position springs with `ToggleTokens.Spring` (0.72 / 520); the
knob is kept inside the track despite the overshoot; the gradient reveal is
`clamp(position × 3)` so it arrives in the first third of the travel. Haptic `CLOCK_TICK`
on every change. Draw-only (no recomposition per frame). Accessibility role Switch.

**Screenshot needed:** Settings › Search suggestions on and Hide status bar off, side by side;
light and dark. A mid-animation frame if possible.

---

### Segmented selector (Segmented) (app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsKit.kt)

Defined in Settings (`app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsKit.kt`), not in
`ui/kit`. Used for "Start page" (30 min / 1 h / 4 h / Never) and "Theme" (Light / Dark / Follow
system) (app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsScreen.kt).

**Anatomy:** full-width track 40dp tall (`SegmentHeight`), 12dp corners (`SettingsTokens.SegmentRadius`), `Glass`
fill (`#EBEBEF` light / white @ 8% dark), 3dp inner padding and 3dp gaps (`SegmentInset`);
equal-width segments; the chosen one sits on a pill with 9dp corners (12 − 3), `SegmentPill` fill
(`#FFFFFF` light / `#4A4A4D` dark) and a 2dp shadow (`SettingsTokens.SegmentShadow`); label 14sp (`SettingsTokens.SegmentTextSize`), 1 line.

**States:** selected (Bold, `TextPrimary`, pill); unselected (Medium, `TextSecondary`, no pill);
pressed ripple clipped to the segment; not interactive = not clickable.

**Motion:** none. The pill jumps to the new segment (no animation in the code).

**Screenshot needed:** Settings › Display › Theme selector on "Follow system", light and dark.

---

### ListPage scaffolding (app/src/main/java/dev/nerzouille/koan/ui/kit/ListPage.kt)

**Purpose:** the full-screen list page opened from the ⋯ menu (History, Downloads:
app/src/main/java/dev/nerzouille/koan/ui/history/HistoryScreen.kt, app/src/main/java/dev/nerzouille/koan/ui/downloads/DownloadsScreen.kt).

**Anatomy**
1. Background `Base` + the settings wash: three radial brand spots (pink at 10% / 15%, violet at
   55% / 5%, blue at 95% / 30% of width / wash height), radius 75% of 320dp, alpha 20% light /
   16% dark (app/src/main/java/dev/nerzouille/koan/ui/settings/SettingsKit.kt, `SettingsTokens.WashHeight`, `SettingsTokens.WASH_ALPHA_LIGHT`, `SettingsTokens.WASH_ALPHA_DARK`). Status bar, navigation bar and
   keyboard insets are padded.
2. `PageBar` (see **Page header**): 56dp, ‹ 24dp, title 22sp Bold, optional trailing slot.
3. Content: fills the rest; scrolls itself.
4. Bottom row, in thumb reach and above the keyboard: 20dp side padding, 10dp vertical
   (`DownloadTokens.BottomGap`): a search `KoanField` (50dp, weight 1); then (if `onClear`) an 8dp
   gap and `DestructiveButton` "Clear" on `Card` (50dp, SemiBold). In selection mode the page
   bar and this row crossfade (180ms) to the `SelectionHeader` and the `SelectionBar` (see
   **Selection mode**).
5. `DayCard`: full width, 18dp corners (`DownloadTokens.GroupRadius`), `Card`
   fill, 12dp side padding; `dayRowShape` rounds only the first and last rows of a lazily drawn
   day. Day labels: "Today", "Yesterday", "Monday 12 October", "12 October 2025" for
   another year.

**Motion:** the page slides up from the bottom (`translationY = (1 − slide) × height`) with
`Motion.Sheet` (1.0 / 380) (app/src/main/java/dev/nerzouille/koan/ui/KoanOverlays.kt); Back closes.

**Light vs dark:** base `#F5F5F7` vs `#121214`; day cards `#FFFFFF` vs `#2C2C2E`; search field
`#EBEBEF` vs white @ 8%; wash 20% vs 16%.

**Screenshot needed:** History with fake visits (`example.com`, `example.org`) grouped under Today
/ Yesterday, search empty, light and dark; Downloads with one fake file; keyboard up with a query.

### Library rows (`LibraryRowFrame`, `FaviconTile`, `LibraryRowTitle`, `LibraryRowSubtitle`) (app/src/main/java/dev/nerzouille/koan/ui/kit/LibraryRow.kt)

**Rules** (one set for History, Downloads and Passwords): a tap does the thing (opens the page,
the file, the password); a swipe right to left deletes (Downloads: left to right also hides from
the list), with Undo or a confirmation card; a long press enters selection mode. Same row, same
motion on every page.

**Anatomy of `LibraryRowFrame`** (a row inside a `DayCard`): `Card` fill, then the selection wash;
12dp side padding (`SettingsTokens.RowInset`); a `SettingsDivider` above every row but the first;
`combinedClickable` (tap, long press with the `LongPress` haptic); optional 20dp start indent
(`HistoryTokens.GroupIndent`); 12dp vertical padding (`DownloadTokens.RowPaddingV`). In selection
mode, 8dp (`DownloadTokens.ActionGap`) then the `SelectionCheck` at the end.
- `FaviconTile`: 40dp (`DownloadTokens.FileTile`), radius 10dp (`FileTileRadius`), `GroupedRow`, favicon 24dp (`HistoryTokens.Favicon`).
- `LibraryRowTitle`: 16sp Medium `TextPrimary` (`SettingsTokens.RowTitleSize`); `LibraryRowSubtitle`: 12sp `TextSecondary` (`RowSubtitleSize`); 1 line, ellipsis.

Passwords' site rows follow the same rules but sit flat on the page (`SiteRow` in `PasswordsPage.kt`).

### Selection mode (`Selection`, `SelectionHeader`, `SelectionBar`, `SelectionCheck`, `selectionWash`) (app/src/main/java/dev/nerzouille/koan/ui/kit/Selection.kt)

A long press checks a row; then taps check or uncheck rows. The header says "N selected" (✕ and
Select all), a bar at the bottom, in thumb reach, holds the actions. Back, ✕ or unchecking the last
row leaves it; rows that leave the list are unchecked. In `ListPage` the header and the bar
crossfade in place of the page bar and the search row (`tween(180)`, `SELECTION_FADE_MS`); rows stop
swiping while selecting.

- **Header**: 56dp tall (`SettingsTokens.TopBar`), 20dp side padding; ✕ 24dp `TextPrimary` in a 56dp circle ("Leave selection"); "N selected" `KoanType.title` 22sp; "Select all" 14sp SemiBold `TextPrimary`, pill tap area padded 10 × 8dp.
- **Bar**: 64dp tall, radius 20dp, `Card` fill, 4dp inset and gaps; each action takes an equal share: glyph 22dp over a 12sp Medium label 3dp below, `TextPrimary`, or `Destructive` for a destructive action; tap area radius 16dp (20 − 4).
- **Check**: 22dp circle; unchecked = 1.5dp `TextSecondary` ring; checked = `Primary` disc with a 14dp `Check` in `OnPrimary`; the fill animates (`animateColorAsState`).
- **Wash**: a checked row gets a `Glass` wash over its surface (animated colour).

| Token (`SelectionTokens`) | Value |
|---|---|
| `BarHeight` | 64dp |
| `BarRadius` | 20dp |
| `BarInset` | 4dp |
| `Glyph` | 22dp |
| `LabelSize` | 12sp |
| `LabelGap` | 3dp |
| `Check` | 22dp |
| `CheckGlyph` | 14dp |
| `CheckRing` | 1.5dp |
| `SelectAllPadH` / `SelectAllPadV` | 10dp / 8dp |

### SwipeRow (app/src/main/java/dev/nerzouille/koan/ui/kit/SwipeRow.kt)

A library row's swipes. Right to left = `onDelete`: the uncovered part shows `Destructive` with the
bin (20dp, `OnPrimary`, at the end, 20dp padding). Left to right = `onHide` (Downloads): `Glass`
with the hide glyph (default `Close`, 20dp `TextPrimary`, at the start). A side with no action
still answers: the row gives at most 36dp (damped) over `Glass` and springs back. Past 40% of the
row's width, or a fling faster than 1800 px/s the same way, the row slides off (160ms tween) and
the action runs; with `springBack` (Passwords: the action asks first) it comes back after. The
offset is a layer translation: dragging never recomposes. A row whose shape is not a rectangle is
clipped to it (`dayRowShape`); turning selection mode on mid-swipe sends the row back.

| Token (`SwipeTokens`) | Value | Use |
|---|---|---|
| `Nudge` | 36dp | how far a side without an action gives |
| `COMMIT` | 0.4 | share of the width past which the swipe acts |
| `FLING` | 1800 px/s | a fling this fast acts even when short |
| `OUT_MS` | 160 | the row sliding off |
| `Back` | spring(0.62, 520) | the row springing back |

### SiteIcon (app/src/main/java/dev/nerzouille/koan/ui/kit/SiteIcon.kt)

A site's favicon as the Passwords pages show it: only one already on the phone (never fetched),
cut to its drawing so every icon fills the same box, in greyscale at 0.8 contrast
(`PasswordTokens.GREY_CONTRAST`) so it sits with the plain glyphs. An opaque square icon keeps its
square with corners at 24% of its size (`TILE_RADIUS`). `colour = true` crossfades it to its own
colours in 220ms (`COLOUR_MS`, two stacked images, layer alpha only). No usable icon (none cached,
or one that can't be seen on this theme): the fallback glyph in `TextPrimary`, never a letter tile.

### Small shared pieces (`ui/kit`, batch 4)

Use these instead of rebuilding them: `DragHandle` (sheet and drawer grab handle),
`Modifier.brandWash(height, alpha)` (the three-spot brand glow; `settingsWash`, the tour's title
glow), `Modifier.swipeDownToDismiss(threshold, onDismiss)`, `KoanIconButton` (glyph on a round
target, touch / glyph sizes passed), `CardHeader` + `CardIconTile` (floating cards' header),
`EmptyLine` (one-line "nothing here"), `FileGlyph` (file glyph over its extension), `TextLink`
(secondary text with a round ripple), `KoanDivider(inset, thickness)`, `RowTexts` (a row's title
over subtitle, shared by `SheetRow` and `SettingsRow`), `SettingsToggleRow` / `SheetToggleRow`. In
`ui/overview`: `SpaceTitle`. Confirmations go through `Overlays.ask` (one card) rather than their
own `SheetState`.

### KoanClipboard (app/src/main/java/dev/nerzouille/koan/ui/kit/KoanClipboard.kt)

Koan's own copies ("Copy link"). In private mode the clip is flagged sensitive, so Android 13+
hides the text in its clipboard preview. Copied passwords are always flagged sensitive
(`PasswordsPage.kt`). Below Android 13 Koan confirms a copy with its own toast ("… copied").

---

### Values that could not be resolved from the code

- The platform `touchSlop` (drag start for sheets and cards) is the system's
  `ViewConfiguration` value, not a Koan token.
- Ripple colour / alpha: Material 3 defaults, not set by Koan.
- Spline fling decay for a full sheet's list (`splineBasedDecay`): platform physics, no Koan value.


## 3.D Pages, sign-in, tour and icons

All paths are relative to `app/src/main/java/dev/nerzouille/koan/` unless they start with `app/` or `docs/`.
Token values are resolved from `ui/theme/Tokens.kt` (cited by token name), `ui/theme/Brand.kt`, `ui/theme/Theme.kt`.

### Shared colour reference (`KoanPalette.Light` / `.Dark` in `ui/theme/Tokens.kt`)

| Token | Light | Dark |
|---|---|---|
| Base | `#F5F5F7` | `#121214` |
| Glass | `#EBEBEF` | White @ 0.08 |
| TextPrimary | `#1C1C1E` | `#FFFFFF` |
| TextSecondary | `#66666B` | White @ 0.62 |
| TextDisabled | Black @ 0.22 | White @ 0.19 |
| Card | `#FFFFFF` | `#2C2C2E` |
| CardBorder (hairlines) | `#E4E4EA` | White @ 0.08 |
| Primary | `#1C1C1E` | `#FFFFFF` |
| OnPrimary | `#FFFFFF` | `#1C1C1E` |
| Destructive | `#C42A20` | `#FF8077` |
| GroupedRow | `#F2F2F5` | White @ 0.07 |
| ToggleOff | `#848489` | `#8E8E93` |
| Dock | `#FFFFFF` | `#232326` |
| BarField | `#F1F1F4` | White @ 0.08 |
| SegmentPill | `#FFFFFF` | `#4A4A4D` |
| FieldHint | `#66666B` | White @ 0.60 |
| Attention | `#8A5C00` | `#E8B45A` |
| SheetScrim | `#1C1C28` @ 0.30 | Black @ 0.50 |

Brand colours (`BrandTokens`): Pink `#FF5FA2`, Violet `#B46BFF`, Blue `#4F8DFF`; `Colors = [Pink, Violet, Blue]`. Same in light and dark.

Typography: no `FontFamily` is set anywhere in `app/src/main/java` and there is no `res/font` folder, so every text uses the platform default sans (Roboto on stock Android; the OEM font on HyperOS). `docs/UI_SPEC.md` ("Visual tokens") suggests "Inter or Google Sans Flex if bundled": not done in code.

---

### Settings page frame (`SettingsPageFrame`)

**Purpose.** The shell of every Settings page: the page bar, a large title that collapses into it,
scrolling content, on the base colour with the brand wash at the top.

**Anatomy** (`ui/settings/SettingsKit.kt`): see **Page header** (section 3.C) for the bar and the
collapse. The title is required (About is titled "About"). Horizontal page padding 20dp; insets
`imePadding`, `topSafePadding`, `navigationBarsPadding`; the content column scrolls
(`verticalScroll`) and ends with a 22dp spacer (`SectionTop`).

**Settings wash** (`settingsWash`, `SettingsKit.kt`): three radial gradients, each `color @ a → Transparent`, radius `h*0.75`, where `h = WashHeight` **320dp**:
| Spot | Colour | Centre (x, y) |
|---|---|---|
| 1 | Brand Pink `#FF5FA2` | (0.10·width, 0.15·h) |
| 2 | Brand Violet `#B46BFF` | (0.55·width, 0.05·h) |
| 3 | Brand Blue `#4F8DFF` | (0.95·width, 0.30·h) |
Alpha `a`: light **0.20** (`WASH_ALPHA_LIGHT`), dark **0.16** (`WASH_ALPHA_DARK`; 0.14 before the `#121214` base).

**States.** `interactive` false (a page under the top one, or sliding): the back button and every row lose their click handler (`SettingsKit.kt`).

**Motion.**
- Settings opens/closes as a full-screen slide driven by `Motion.Sheet` = spring(dampingRatio 1.0, stiffness 380) (`ui/KoanOverlays.kt`).
- A pushed page slides in from the right: `translationX = width·f` for the incoming page; the page under it trails at `f·PUSH_PARALLAX` (`PUSH_PARALLAX` = **0.3**); pages more than one step away get alpha 0 (`SettingsScreen.kt`). Driven by `SheetNav` with `Motion.SheetPush` = spring(dampingRatio 1.0, stiffness 520) (`ui/sheet/KoanSheet.kt`). System Back pops a page, then closes (`SettingsScreen.kt`).

**Light vs dark.** Only through palette tokens and the wash alpha (0.20 vs 0.14).

**Screenshot.** Settings root, scrolled to the top, light and dark, signed in with a fake account (shows the wash, back chevron, 34sp "Settings" title and the account card).

### Settings section header (`SettingsSection`)

**Purpose.** Small grey title above a group of settings rows (also used as day headers in History and Downloads).

**Anatomy / sizes** (`SettingsKit.kt`): text `SectionTitleSize` **13sp** SemiBold, colour `TextSecondary`; padding top `SectionTop` **22dp**, bottom `Divider*4` = **4dp**. No icon, no uppercase transform (text as given: "General", "Display", "Privacy", "Koan"; `SettingsScreen.kt`).

**States.** Static.

**Screenshot.** Settings root showing the "General" header and its first rows.

### Settings divider (`SettingsDivider`)

Full-width **1dp** hairline (`SettingsTokens.Divider`), colour `CardBorder` (light `#E4E4EA`, dark White @ 0.08) (`SettingsKit.kt`). Spans the full content width (no indent), unlike the sheet divider.

### Settings row (`SettingsRow`) — all variants

**Purpose.** The one row type of Settings: glyph, title (+ subtitle), then an optional value, trailing slot (toggle, check, "Edit"/"Copy" action) and/or chevron.

**Anatomy** (`SettingsKit.kt`)
1. Optional leading glyph: **20dp** (`RowGlyph`), tint `TextPrimary` (disabled: `TextDisabled`), then a **14dp** gap (`RowGlyphGap`). No tile/box behind the glyph (unlike sheet rows).
2. Text column (weight 1): title **16sp** Medium (`RowTitleSize`), 1 line, ellipsis; optional subtitle **12sp** Regular `TextSecondary` (`RowSubtitleSize`), 1 line, ellipsis.
3. Optional value text **14sp** (`RowValueSize`) in `TextSecondary`, 1 line.
4. Trailing slot (only when enabled).
5. Optional chevron: 6dp spacer (`Divider*6`), `KoanIcons.ChevronRight` **20dp**, tint `TextSecondary`.

**Sizes.** Height **54dp** (`RowHeight`); with a subtitle **62dp** (`RowTallHeight`) (`SettingsKit.kt`). Touch/highlight area: the row is widened by `RowInset` **12dp** on each side past the page padding and clipped to a **14dp** rounded rectangle (`RowRadius`), content padded back by 12dp so text stays aligned with the dividers (`rowArea`, `SettingsKit.kt`).

**Variants** (all are `SettingsRow` with different arguments):
| Variant | How it is built | Example |
|---|---|---|
| Navigation | `chevron = true`, `onClick` pushes a page; often a `value` before the chevron | "Search engine" + engine name (`SettingsScreen.kt`), "About Koan" + version, "Save downloads to" |
| Toggle | trailing `KoanToggle`; tapping the whole row flips it too | "Search suggestions", "Hide status bar", "uBlock Origin" installed |
| Value / action | `value` text, no chevron | "uBlock Origin" not installed → value "Install"; "Default browser" → "Koan" (not tappable) or "Set" + chevron |
| Segmented host | row with no click, followed by a `Segmented` control and a 10dp spacer (`Divider*10`) | "Start page", "Theme" |
| Selection (check) | trailing `KoanIcons.Check` 20dp `TextPrimary` on the chosen item | Search engine list (`EnginesPage.kt`), Save downloads to (`SettingsScreen.kt`) |
| Row action word | trailing `RowAction` text 14sp `TextSecondary`, own circle-clipped tap target padded 6dp | "Edit" on custom engines (`EnginesPage.kt`), "Copy", "Reveal"/"Hide" (`PasswordsPage.kt`, `RowAction`) |
| Destructive | `titleColor = Destructive` (light `#C42A20`, dark `#FF8077`); red text only, no fill | "Sign out" (`SettingsScreen.kt`) |
| Info (no click) | `onClick` null | Account page email row (`SettingsScreen.kt`) |
| Soon (disabled) | `enabled = false`: glyph and title `TextDisabled`, value replaced by "Soon" in `TextDisabled`, trailing and chevron hidden, not tappable (`SettingsKit.kt`) | not used on any current Settings page (API only) |

**States.**
- Default: as above.
- Pressed: the Material `clickable` default indication (ripple) inside the 14dp rounded, inset area (`SettingsKit.kt`). No custom pressed colour.
- Disabled ("Soon"): see table.
- Non-interactive (page underneath): no click.
- Toggle ON/OFF: see `KoanToggle` below.

**Light vs dark.** Palette only.

**Screenshot.** Settings root top to bottom (two screens): one each of navigation + value, toggle ON and OFF, segmented, "Install" value row; plus the Mozilla account page showing the red "Sign out" row; plus a pressed row (ripple) if capturable.

### Toggle (`KoanToggle`, used in settings rows and Clear data card)

`ui/kit/KoanToggle.kt`. Size **50×30dp** (`ToggleTokens.Width/Height`). Track: rounded (radius h/2) `ToggleOff` (light `#848489`, dark `#8E8E93`). Knob: white circle, radius `h/2 − KnobInset` (KnobInset **2dp**), with a radial shadow (`Black @ 0.22` at 75% → transparent, radius r + **3dp**, offset down by shadow/3; `ToggleTokens.KnobShadow`, `ToggleTokens.KNOB_SHADOW_ALPHA`; `KoanToggle.kt`). ON: brand horizontal gradient revealed from the left edge up to the knob's right side, alpha `reveal·0.9` (`TRACK_ALPHA` **0.9**), where `reveal = clamp(p·3, 0, 1)` (`KoanToggle.kt`). Motion: `ToggleTokens.Spring` = spring(dampingRatio **0.72**, stiffness **520**); knob clamped inside the track during overshoot. Haptic `CLOCK_TICK` on change. Disabled/non-interactive: `onCheckedChange` null → no toggleable modifier. No pressed indication (`indication = null`).

### Segmented control (`Segmented`)

**Purpose.** Mutually exclusive options (Start page delay; Theme Light / Dark / Follow system).

**Anatomy / sizes** (`SettingsKit.kt`): full-width track, height **40dp** (`SegmentHeight`), radius **12dp** (`SegmentRadius`), background `Glass` (light `#EBEBEF`, dark White @ 0.08), inner padding and gap **3dp** (`SegmentInset`). Each option weight 1. Selected pill: radius 12 − 3 = **9dp**, background `SegmentPill` (light `#FFFFFF`, dark `#4A4A4D`), shadow elevation **2dp** (`SegmentShadow`). Label **14sp** (`SettingsTokens.SegmentTextSize`), 1 line: selected `TextPrimary` Bold, unselected `TextSecondary` Medium.

**States.** Selected / unselected; pressed = default ripple clipped to the pill shape; non-interactive = no click. No animation between options (the pill jumps; no animate call in the code).

**Screenshot.** The Theme row with "Follow system" selected, light and dark.

### Account card (Settings root)

**Purpose.** The Mozilla account entry point; a card that "holds your focus".

**Anatomy** (`SettingsScreen.kt`): full-width, radius **22dp** (`SettingsTokens.CardRadius`), background `Card`, **brand gradient outline 1.5dp** (`brandOutline`) except while the status needs attention ("Couldn't sync": a failure is not an active moment, the card is then a plain `Card`), padding **16dp** (`CardPadding`).
- Avatar: **44dp** circle (`Avatar`) on `Glass`, the email's first letter uppercased (or "Z" signed out), **18sp** SemiBold (`AvatarTextSize`).
- 14dp gap; title **16sp** SemiBold ("Connect your Zen Spaces" signed out / "Mozilla account" signed in) and subtitle **12sp** `TextSecondary` (email or "Sign in with your Mozilla account"), then `ChevronRight` 20dp `TextSecondary`.
- Signed in with a status: 12dp gap (`Divider*12`), then a status line **14sp** `TextPrimary`, led by either a `BrandSpinner` **12dp** (`FirstRunTokens.BadgeSpinner`) + 8dp while syncing, or a **6dp** `Attention` dot (light `#8A5C00`, dark `#E8B45A`) + 8dp when the status needs attention.

**States.** Signed out / signed in idle / syncing (outline) / attention (no outline, amber dot). Pressed: default ripple in the 22dp shape.

**Brand spinner** (`Brand.kt`): a 270° arc (`SPINNER_SWEEP`), stroke **2dp** round cap (`BrandTokens.SpinnerStroke`), sweep gradient Pink→Violet→Blue→Pink, one turn per **900ms** linear (`BrandTokens.SPINNER_TURN_MS`).

**Screenshot.** Settings top with the card signed out (fake), and signed in with a fake email (`someone@example.com`) while syncing.

### About page (`AboutPage`)

**Purpose.** App identity, version, update check, links (tour, What's new, privacy, licences).

**Anatomy** (`ui/settings/AboutPage.kt`), page title "About" (collapsing large title):
1. Top spacer 22dp (`SectionTop`).
2. App icon (`R.drawable.koan_icon`) **104dp**, radius **26dp** (`SettingsTokens.AppIcon`, `SettingsTokens.AppIconRadius`).
3. 22dp; "Koan" **30sp** Bold `TextPrimary` (`SettingsTokens.AppNameSize`); 6dp; "Version x.y.z" **14sp** `TextSecondary` (`SettingsTokens.VersionSize`); 13.2dp (`SectionTop*0.6`).
4. Update chip (`UpdateChip`): height **30dp** (`SettingsTokens.ChipHeight`), fully rounded (radius 15dp), shadow **1dp** (`SettingsTokens.ChipShadow`), background `Card`, horizontal padding **12dp** (`SettingsTokens.ChipPadding`); optional **13dp** glyph (`SettingsTokens.ChipGlyph`) + **6dp** gap (`SettingsTokens.ChipGap`); label **13sp** SemiBold (`SettingsTokens.ChipTextSize`).
   - Checking: no glyph, "Checking for updates…", text `TextSecondary`, not tappable.
   - Up to date: `KoanIcons.Check`, "Up to date", `TextPrimary`, not tappable.
   - Available: `KoanIcons.Download`, "Update available: vX", tappable (opens the release).
   - Failed: `KoanIcons.Retry`, "Couldn't check for updates", tappable (retries).
5. 66dp (`SectionTop*3`); "Built to pair with Zen" card: radius 22dp, `Card` (no outline: it is not tappable, so no gradient), padding 16dp, `KoanIcons.Essentials` 20dp, 14dp gap, title 16sp SemiBold + subtitle 12sp `TextSecondary`. Not tappable.
6. 8dp; navigation rows (chevron): "Take the tour", "What's new in X.Y", "Privacy notice", "Open-source licences", separated and followed by dividers.
7. 44dp; footer "Built on GeckoView · Made for one hand" **12sp** `TextSecondary`, centred (`SettingsTokens.FooterSize`).

**Screenshot.** About in light and dark, chip in "Up to date" state; a second capture of the chip in "Update available".

### Search engine pages (`EnginesPage`, `EngineEditPage`)

**Purpose.** Pick the default engine; add/edit custom engines.

**EnginesPage** (`ui/settings/EnginesPage.kt`): title "Search engine"; one `SettingsRow` per engine (no glyph), custom ones with subtitle "@shortcut · host", the chosen one with a trailing `Check` 20dp, custom ones with an "Edit" `RowAction`; dividers between; last row "Add a search engine" with `KoanIcons.Plus`.

**EngineEditPage**: title "Add a search engine" / "Edit search engine"; four `Field`s (Name, Search address, Shortcut, Suggestions address), then 16dp, `PrimaryButton` "Add"/"Save" full width, and when editing 8dp + `DestructiveButton` "Delete".
- **Field**: top padding **16dp** (`FieldGap`); label **13sp** SemiBold `TextSecondary`; **6dp** (`FieldLabelGap`); then a `KoanField` (50dp pill, `Glass`, 16sp hint and input, solid `TextPrimary` cursor, never gradient). Under it, error text in `Destructive` or help text in `TextSecondary`, **12sp**, 6dp top. Error shows once the field has text or after a Save attempt.
- **PrimaryButton** (`ui/kit/Buttons.kt`): height **50dp** (`ButtonTokens.Height`), pill, background `Primary`, horizontal padding **28dp** (`ButtonTokens.PaddingH`), text **16sp** SemiBold `OnPrimary` (`ButtonTokens.TextSize`), icon gap 10dp (`ButtonTokens.IconGap`).
- **DestructiveButton** (`Buttons.kt`): 50dp pill, background `GroupedRow`, text 16sp SemiBold `Destructive`.

**States.** Field: empty (hint) / filled / error / help. Buttons: default + ripple pressed; no disabled look (Save validates on tap).

**Screenshot.** Engines list with the default checked and one fake custom engine ("Example Search", `@ex · example.com`); the edit page with one field showing an error.

### Passwords pages (`PasswordsPage`, `PasswordSitePage`, `AddPasswordPage`)

**Purpose.** Browse saved logins per site, unlock with fingerprint/PIN, copy/reveal, add one by
hand, delete. Follows the library rules (see **Library rows**). Every Passwords page sets
`FLAG_SECURE` (`SecureWindow()`): no screenshot, recording or Recents preview.

**PasswordsPage** (`ui/settings/PasswordsPage.kt`), title "Passwords", in a `SettingsPageFrame`:
- Empty: "No saved passwords yet" 16sp SemiBold `TextPrimary`, 6dp, "Passwords from your Mozilla account show up here after the next sync. You can also add one yourself." 14sp `TextSecondary`, 22dp, full-width `PrimaryButton` "Add a password".
- Search `KoanField` "Search passwords" (matches the site, and the username once unlocked), then 12dp.
- Locked: "N saved. Unlock to see usernames and passwords." 14sp `TextSecondary`, full-width `PrimaryButton` "Unlock", 12dp gaps.
- No match: "No password matches “q”." 14sp `TextSecondary`.
- **Site rows**, flat on the page (no card), a `SettingsDivider` between them, each in a `SwipeRow` (delete only, `springBack = true`): `SiteIcon` 20dp (`PasswordTokens.Favicon`, fallback `KoanIcons.Key`), 14dp, site 16sp Medium `TextPrimary`, subtitle 12sp `TextSecondary` = the username once unlocked ("No username", "…" while opening; one account only); value 14sp `TextSecondary` "N accounts" or "This phone"; 6dp; `ChevronRight` 20dp `TextSecondary`, or the `SelectionCheck` in selection mode. Height 54dp, 62dp with a subtitle (`SettingsTokens.RowHeight`, `RowTallHeight`).
- **Floating +** (not while selecting): 56dp circle (`PasswordTokens.AddButton`), `Primary` fill, 6dp shadow (`AddShadow`), 20dp from the bottom-right corner above the navigation bar (`AddMargin`), `Plus` 20dp `OnPrimary`. Opens **Add a password**.
- **Gestures.** Tap: the site's page (locked: fingerprint first, then straight there). Swipe right to left: the delete card. Long press: selection mode. Locked, a swipe or long press asks for the fingerprint first. Selection bar: one site = Visit (`Globe`) + Delete; several = Delete. Select all in the header.
- **Delete card** (`confirmDelete`, a `Confirm` floating card): "Delete this password?" / "Delete N passwords?", then who (site · username, or the sites, at most 3 named + "and N more") and what it affects: "Removed from this phone." or "Removed from this phone and your Mozilla account." (when two-way sync's "Change and delete passwords" is on and an account login is in the pick). Then an Undo toast.

**PasswordSitePage**: title = the site. Per account, 22dp apart: a `SettingsRow` with `SiteIcon` (fallback `Globe`; grey while locked, crossfades to colour in 220ms once unlocked, `PasswordTokens.COLOUR_MS`), the origin, subtitle "Saved on this phone" / "From your Mozilla account". Unlocked, below it: username row (subtitle "Username", trailing `RowAction` "Copy"; tap copies), password row ("••••••••••" or the password, subtitle "Password", trailing "Reveal" / "Hide"; a tap on the row copies it, flagged sensitive), and a "Delete password" row in `Destructive` text, each after a `SettingsDivider`. Locked: full-width "Unlock". Unlocked with one account: full-width `PrimaryButton` "Copy password". Deleting a site's last account goes back to the list.

**AddPasswordPage**: title "Add a password"; fields Site ("example.com"), Username ("name@example.com", help "Leave empty if the site only asks for a password."), Password (hidden; `RowAction` "Show password" / "Hide password"); "Saved encrypted on this phone." 12sp `TextSecondary`; full-width "Save". Errors show after the first Save: "Type the site, like example.com", "That isn't a web address", "Type the password". Android autofill is turned off on this page.

**States.** Empty / locked / unlocking ("…") / unlocked / no match / selection / password hidden vs revealed. Unlock uses the system BiometricPrompt (BIOMETRIC_STRONG or DEVICE_CREDENTIAL).

**Tokens** (`PasswordTokens`)

| Token | Value | Use |
|---|---|---|
| `AddButton` | 56dp | the floating + |
| `AddMargin` | 20dp | its distance from the corner |
| `AddShadow` | 6dp | its shadow |
| `Favicon` | 20dp | `SiteIcon` on the rows (same box as `SettingsTokens.RowGlyph`) |
| `GREY_CONTRAST` | 0.8 | greyscale icons: contrast scaled around mid-grey |
| `COLOUR_MS` | 220 | grey → colour once unlocked, and back |
| `TILE_RADIUS` | 0.24 | an opaque square icon keeps its square, corners 24% of its size |

**Screenshot.** Not possible: the pages are `FLAG_SECURE`. Use fake data only (seeded `example.com` logins) for any recording made another way.

---

### List page shell (`ListPage`) — History and Downloads

**Purpose.** Full-screen list page opened from the ⋯ menu: back + title on top, list in the middle, search field and Clear at the bottom in thumb reach.

**Anatomy** (`ui/kit/ListPage.kt`)
1. Background `Base` + `settingsWash` (same wash as Settings), insets top-safe, navigation bars, IME.
2. Header: `PageBar` (see **Page header**): 56dp, ‹ 24dp, title **22sp** Bold, trailing slot.
3. Content (weight 1).
4. Bottom bar row: padding horizontal 20dp, vertical **10dp** (`DownloadTokens.BottomGap`): a search `KoanField` (50dp pill, `Glass`, 16sp, ✕ once typed), then, when `onClear` != null, 8dp (`ActionGap`) and `DestructiveButton` "Clear" with `surface = Card` (50dp, SemiBold `Destructive`).
5. Selection mode: the header and the bottom row crossfade (`tween(180)`) to `SelectionHeader` and `SelectionBar` (3.C **Selection mode**).

**Open/close motion.** Slides up from the bottom: `translationY = (1 − slide)·height`, slide animated with `Motion.Sheet` spring(1.0, 380) (`ui/KoanOverlays.kt` Downloads History). Back closes.

### Day card and day header (`DayCard`, `dayRowShape`, `byDay`)

- **Day header:** a `SettingsSection` (13sp SemiBold `TextSecondary`, 22dp top / 4dp bottom) with the label "Today", "Yesterday", "EEEE d MMMM" this year, "d MMMM yyyy" older (`ListPage.kt`).
- **DayCard** (`ListPage.kt`): full width, radius **18dp** (`DownloadTokens.GroupRadius`), background `Card`, horizontal padding 12dp; rows inside separated by `SettingsDivider` (1dp `CardBorder`, full width of the card's inner content).
- **dayRowShape**: in the lazy History list each row is clipped separately: 18dp corners only on the first row's top and the last row's bottom.

### History page (`HistoryScreen`)

**Purpose.** Pages visited (phone + synced desktop), one card per day, groups of same-site runs,
Recently closed at the top. Library rules (see **Library rows**): a tap opens the page (in the
current tab when History was opened from a page, else a new tab), a swipe right to left removes it
(or a whole group) with Undo, a long press enters selection mode.

**Anatomy** (`ui/history/HistoryScreen.kt`), inside `ListPage` titled "History", hint "Search history", Clear always shown:
- `LazyColumn`, content padding start/end 20dp, bottom 22dp.
- **Recently closed** section (hidden while searching): header "Recently closed", the first **3** closed tabs (`HistoryTokens.CLOSED_PREVIEW`; all of them when opened from the long press on the dock's History), then a row "Show all N more" (16sp Medium `TextPrimary`) that expands the list. A tap puts the tab back in its space and place.
- **Day sections:** header + rows; rows are `HistoryItem.Page` or `HistoryItem.Group` (grouping: consecutive visits to the same host become a group; the same page twice in a row, ignoring a trailing "/", shows once; `ui/history/HistoryGroups.kt`). While searching, rows are flat pages. Loads at most **3000** rows (`HistoryTokens.LIMIT`).

**Rows** are `LibraryRowFrame`s (see **Library rows**) with a `FaviconTile` (40dp, radius 10dp, `GroupedRow`, favicon 24dp: `DownloadTokens.FileTile`, `FileTileRadius`, `HistoryTokens.Favicon`), 12dp gap (`DownloadTokens.RowGap`), title 16sp Medium `TextPrimary` and subtitle 12sp `TextSecondary` (`LibraryRowTitle`, `LibraryRowSubtitle`). Pages of an open group are indented 20dp (`HistoryTokens.GroupIndent`).

| Row | Title | Subtitle | Trailing |
|---|---|---|---|
| Page (`PageRow`) | page title, or host if blank | "host · HH:mm" | if from sync: 8dp + `KoanIcons.Desktop` **16dp** `TextSecondary` (`HistoryTokens.SyncGlyph`) |
| Closed tab (`ClosedRow`) | title or host | "host · HH:mm" today, else the day label | — |
| Group (`GroupRow`) | host | "N pages · HH:mm–HH:mm" (single time when equal) | 8dp + `ChevronRight` **18dp** `TextSecondary` (`HistoryTokens.Chevron`), turns 0° → 90° when open, `tween(200ms)` (`HistoryTokens.CHEVRON_MS`), a layer rotation |
| Show all | "Show all N more" | — | — |

**Swipe** (`SwipeRow`, delete side only; see **SwipeRow**): right to left over `Destructive` with the bin, clipped to `dayRowShape`. Pages and groups only; closed tabs have no swipe. A removal shows "Removed from history" / "Removed N pages from history" with Undo.

**Selection mode** (`HistoryActions.forChecked`). Closed tabs and pages never mix: one closed tab is checked at a time (the check moves); Select all checks pages and groups only.

| Checked | Bar actions |
|---|---|
| one page | New tab (`Open`), Copy link (`Link`), Share link (`Share`), Remove (`Trash`, destructive) |
| one group | "Remove N visits" (destructive) |
| one closed tab | Reopen (`Retry`), New tab, Copy link, Share link |
| several | Share (links one per line), Remove (destructive) |

**Item motion.** Every item uses `Modifier.animateItem()` (default LazyColumn placement/fade specs).

**Empty states** (14sp `TextSecondary`, 22dp top): "Pages you visit show up here." / "Nothing in History matches “q”.".

**Light vs dark.** Palette only; the bin on red is `OnPrimary`: white in light, near-black in dark.

**Screenshot.** With fake history only (example.com pages): Recently closed with 3 rows + "Show all", a Today card with one open group (chevron turned, indented pages) and a synced page with the desktop mark; a row mid-swipe showing the red bin; selection mode with two pages checked; the empty state.

### Clear browsing data card (`ClearDataCard`)

**Purpose.** Floating card behind History's Clear: choose history / cookies / cache.

**Anatomy** (`ui/history/ClearDataCard.kt`), shown inside `KoanCard` (the floating card, `ui/sheet/KoanSheet.kt`: margin 10dp, radius **28dp**, padding 20dp, `SheetTokens.CardMargin/CardRadius/CardPadding`; used at `ui/KoanOverlays.kt`):
- Column with **10dp** gaps (`SheetTokens.Gap`).
- Title "Clear browsing data?" **18sp** SemiBold `TextPrimary` (`SheetTokens.CardTitleSize`).
- `SheetGroup` (`ui/sheet/SheetKit.kt`): radius **18dp** (`SheetTokens.GroupRadius`), background `GroupedRow`; three `SheetRow`s without icon (`SheetKit.kt`): height 50dp / **58dp** with subtitle (`SheetTokens.RowHeight`, `SheetTokens.RowTallHeight`), horizontal padding 12dp, title 16sp Medium, subtitle 12sp `TextSecondary`; each with a trailing `KoanToggle`. Rows: "History" (subtitle "Desktop history is only hidden here", default ON), "Cookies and site data" ("Signs you out of most sites", default OFF), "Cached images and files" (default ON). `SheetDivider` 1dp `CardBorder`, indented start 12·2 + 30 = **54dp** (`SheetKit.kt`).
- 5dp spacer; `DestructiveButton` "Clear" full width (does nothing if all three are off); `QuietButton` "Cancel" (50dp, no surface, 16sp Medium `TextPrimary`, `Buttons.kt`).

**States.** Each toggle on/off; all off = Clear inert (no visual disabled state).

**Screenshot.** History with the Clear card open over it (scrim visible), default toggles.

### Downloads page (`DownloadsScreen`)

**Purpose.** Running downloads with gradient progress, then finished files by day. Library rules
(see **Library rows**): a tap opens the file, a swipe left to right hides it from the list (the
file stays), right to left deletes the file (both with Undo; the file is only deleted once the
toast is gone), a long press enters selection mode.

**Anatomy** (`ui/downloads/DownloadsScreen.kt`), inside `ListPage` titled "Downloads", hint "Search downloads", Clear shown only if there is any download:
- Header trailing **folder chip**: height **32dp** (`DownloadTokens.ChipHeight`), full pill, `Card`, horizontal padding 12dp, `KoanIcons.Folder` **16dp** (20·0.8) `TextSecondary`, 6dp, folder name **13sp** `TextSecondary` (`DownloadTokens.ChipTextSize`). Tap opens the folder.
- Content column scrolls, horizontal padding 20dp.
- "In progress" section + one `DayCard` of running rows; then a section + `DayCard` per day of finished rows, newest first.

**File tile** (`FileTile`): **40dp**, radius 10dp, `GroupedRow`; `KoanIcons.File` **18dp** (`DownloadTokens.FileGlyph`) `TextPrimary` over the extension (max 4 chars, uppercased) **7sp** SemiBold `TextSecondary` (`DownloadTokens.FileExtSize`).

**Private mark**: a download from a private tab has `KoanIcons.Private` **14dp** `TextSecondary` + 6dp before its name (`DownloadTokens.PrivateGlyph`, `PrivateGap`); it stays in the list until Koan restarts.

**Running row** (no swipe, no selection): vertical padding 12dp; tile; 12dp; column: one line with the filename 16sp Medium (ellipsis) and the state 12sp `TextSecondary` at its end ("Paused", "Couldn't finish", "Starting…", or "NN%"); **progress bar** height **4dp** (`DownloadTokens.ProgressHeight`), top padding **8dp** (`DownloadTokens.ProgressTop`) / bottom 4dp, fully rounded, track `Glass`, fill **brand horizontal gradient** (`BrandHorizontal`) over the fraction; then "x MB of y MB · host" 12sp `TextSecondary`. Then 8dp + round button (Pause / Play when paused / Retry when failed) + 8dp + round Close (cancel).
- **Round button**: **36dp** circle (`DownloadTokens.RoundButton`), `GroupedRow`, glyph 16dp `TextPrimary`.

**Done row**: a `LibraryRowFrame` in a `SwipeRow` (delete and hide sides, clipped to `dayRowShape`): tile, 12dp, filename 16sp Medium + "size · host" 12sp `TextSecondary`. Tap opens the file by its type: a video plays in Koan's player when it can, else Android opens it ("Couldn't open this file" toast if it is gone).

**Swipes and toasts.** Left to right (hide, `Close` glyph over `Glass`): "Hidden from the list", detail "The file stays in {folder}", Undo. Right to left (delete, bin over `Destructive`): "File deleted", Undo.

**Selection mode** (finished rows only; Select all; hide stays a swipe only):

| Checked | Bar actions |
|---|---|
| one file | Open by type (`Play` "Open" for video/audio, `Image` "Open", `Install` "Install" for an APK, else `File` "Open"), Copy link, Share, Delete (destructive) |
| several | Share, Delete (destructive; asks first: "Delete N files?", "They're deleted from {folder}.") |

**Empty states** (14sp `TextSecondary`, 22dp top): "Files you download show up here." / "No download matches “q”.".

**Screenshot.** With fake files only (e.g. `example.pdf`, `photo.jpg` from example.com): one running download at ~40%, one paused, a Today card with one row mid-swipe (hide side) and selection mode with one file checked; the empty state.

---

### Sign-in screen shell (`SignInScreen`)

**Purpose.** Full-screen Mozilla sign-in dressed as Koan: header with ✕, thin progress line, then the Choice page, the From-your-computer page, or Mozilla's own web page (email/password or pairing).

**Anatomy** (`ui/signin/SignInScreen.kt`)
1. Background `Base`, top-safe padding.
2. **Header** row: height **64dp** (`SignInTokens.HeaderHeight`), horizontal padding **8dp** (`SignInTokens.HeaderPadding`).
   - Close: **44dp** circle touch (`CloseTouch`), `KoanIcons.Close` **20dp** (`CloseGlyph`), `TextPrimary`; disabled while finishing.
   - On every page except Choice: 8dp, then "Sync your Zen Spaces" **17sp** Bold `TextPrimary` (`SignInTokens.TitleSize`) over "Mozilla account" **13sp** `TextSecondary` (`SignInTokens.SubtitleSize`).
3. **Progress line**: full width, **2dp** (`ProgressHeight`), drawn in `TextSecondary` at alpha **0.8**, width = progress; shown only between 1% and 99% or while finishing (then 100%). Deliberately neutral: "Login UI stays neutral: never the brand gradient or a space colour".
4. Page area (weight 1, IME padding).
5. **Notice** (top centre, over the page): margin **12dp** (`SignInTokens.NoticeMargin`), radius **16dp** (`SignInTokens.NoticeRadius`), background `Notice` (light `#F2F2F5` @ alpha 0xF2 ≈ 0.95; dark `#2C2C2E` @ 0xF2 ≈ 0.95), padding **14dp** (`SignInTokens.NoticePadding`), text **14sp** `TextPrimary` (`SignInTokens.NoticeSize`); tap dismisses. Texts include "No connection. Check it and try again.", camera and clipboard messages.
6. **Finishing pill** (bottom centre, nav-bar padded): same margin/padding/colour, fully round, "Connecting to your Zen Spaces…" 14sp.
7. **Outdated WebView** state (system WebView major < **92**, `SignInTokens.MIN_WEBVIEW`): centred column, horizontal padding **32dp** (`SignInTokens.OutdatedPadding`), message **16sp** centred (`SignInTokens.OutdatedSize`), **20dp** gap (`SignInTokens.OutdatedGap`), `PrimaryButton` "Open Play Store".

**Motion.** The screen slides up over everything and back down with `Motion.Sheet` spring(1.0, 380) (`ui/KoanRoot.kt`). A web page fades in once it has painted: alpha 0 → 1 `tween(220ms)` (`SignInTokens.FADE_MS`; `SignInScreen.kt`), reset to 0 on every mode change. WebViews have a transparent background until painted (`ui/signin/MozillaSignIn.kt`, `ui/signin/PairingWeb.kt`).

**Back behaviour**: web history back → Pairing returns to From-your-computer → any page returns to Choice → Choice closes.

**Light vs dark.** Palette only; Mozilla's pages are themed to Koan's theme through a light/dark `ContextThemeWrapper` (`MozillaSignIn.kt`) and algorithmic darkening on Android 13+ (`MozillaSignIn.kt`); below Android 13 the sign-in page stays light.

### Sign-in Choice page (`Choice`, floating K, cards, Recommended badge)

**Purpose.** First page of sign-in: the K, the title, and two ways in as cards in the thumb zone.

**Anatomy** (`ui/signin/SignInPages.kt`), column centred, horizontal padding **20dp** (`PagePadding`), nav-bar padded:
1. Flexible spacer (weight 1).
2. **Floating K** (`FloatingK`), box **96dp** (`KSize`): brand glow layer, sparkles layer, K image (`R.drawable.koan_k`, 768×762 WebP with alpha, `app/src/main/res/drawable-nodpi/koan_k.webp`).
3. **24dp** (`KGap`); title "Sync your Zen Spaces" **26sp** Bold centred `TextPrimary` (`HeroSize`).
4. **8dp** (`HeroLineGap`); "Sign in with the Mozilla account you use in Zen." **15sp** `TextSecondary` centred (`BodySize`).
5. Flexible spacer (weight 0.4).
6. Choice card "From your computer" / "Scan a code from Zen. No password." (`KoanIcons.Desktop`, Recommended); **16dp** (`ChoiceGap`); Choice card "With email and password" / "On Mozilla's sign-in page." (`KoanIcons.Key`).
7. **20dp** bottom (`BottomGap`).

**Floating K motion** (a frame clock in ms, starts at `STILL_CLOCK` = 1300):
- Float: `translationY = sin(2π·clock/4200)·4dp` (`K_FLOAT_MS` **4200**, `KFloat` **4dp**), applied to the K and the sparkles.
- Tilt: `rotationZ = sin(2π·clock/6400)·1.8°` (`K_TURN_MS` **6400**, `K_TURN` **1.8**).
- "Remove animations" (animator duration scale 0, `ui/tour/Welcome.kt`): clock frozen at 1300ms, nothing moves.

**Brand glow** (`brandGlow`): three radial gradients `color @ a → Transparent` of radius `r`, where
- `breath = sin(2π·clock/5600)` (`GLOW_BREATH_MS` **5600**);
- `a = base·(1 + breath·0.12)` with base light **0.30** / dark **0.38** (`GLOW_ALPHA_LIGHT/DARK`, `SignInTokens.GLOW_ALPHA_DARK`; `GLOW_BREATH` **0.12**);
- `r = min(w,h)·0.95·(1 + breath·0.12·0.5)` (`GLOW_RADIUS` **0.95**), i.e. radius ≈ 91dp ± 6%.
- Spot centres (c = box centre): Pink at (c.x − 0.35r, c.y + 0.10r); Violet at (c.x, c.y − 0.20r); Blue at (c.x + 0.35r, c.y + 0.15r).
This is the single allowed brand-gradient use on sign-in UI (the brand-gradient rule comment in `Tokens.kt`; `docs/UI_SPEC.md` "Brand gradient").

**Sparkles** (shared `sparkles` modifier, `ui/tour/Welcome.kt`), unit **5dp** (`SparkleUnit`), colour **white in dark, Brand Violet `#B46BFF` in light** (`SignInPages.kt`). Four sparkles (`KSparkles`), positions as fractions of the 96dp box:
| # | x | y | size | period ms | phase rad |
|---|---|---|---|---|---|
| 1 | −0.14 | 0.18 | 0.8 | 3400 | 0.0 |
| 2 | 1.10 | 0.08 | 0.6 | 4100 | 2.1 |
| 3 | 1.04 | 0.92 | 0.7 | 3700 | 4.0 |
| 4 | 0.12 | 1.08 | 0.5 | 4600 | 5.3 |
Without live blur (Android 11, `DeviceProfile.liveBlur` = SDK ≥ 31, `DeviceProfile.kt`) only the first **2** (`SPARKLES_SIMPLE`). Star shape and twinkle formula: see "Welcome card · sparkles" below.

**Choice card** (`ChoiceCard`)
- Row: full width, height **84dp** fixed (`ChoiceHeight`), radius **22dp** (`ChoiceRadius`), background `Card`, border **1dp** `CardBorder`, horizontal padding **16dp** (`ChoiceInset`); clickable (ripple).
- Icon box **44dp** circle (`ChoiceIconBox`) on `Base`, glyph **24dp** (`ChoiceGlyph`) `TextPrimary`.
- 16dp; title **16sp** SemiBold `TextPrimary` (`ModeSize`), 1 line; **2dp** (`ChoiceLineGap`); subtitle **13sp** `TextSecondary` (`SubtitleSize`), 1 line.
- 2dp; `ChevronRight` **18dp** (`ModeGlyph`) `TextSecondary`.
- **Recommended badge**: text "Recommended" **11sp** SemiBold (`BadgeSize`) `OnPrimary` on a full pill of `Primary` (light: white on `#1C1C1E`; dark: `#1C1C1E` on white), padding **9dp × 3dp** (`SignInTokens.BadgePaddingH`, `SignInTokens.BadgePaddingV`), aligned top-end of the card, offset x = −16dp, y = −**9dp** (`BadgeLift`) so it straddles the top edge. Not gradient.

**States.** Card default / pressed (ripple). No disabled state. The camera permission is requested only after tapping "From your computer" (`SignInScreen.kt`).

**Light vs dark.** Glow alpha 0.30 vs 0.38; sparkles violet vs white; badge inverts with `Primary`.

**Screenshot.** Signed-out, Settings › account card → sign-in: Choice page in light and in dark (glow, K, both cards, badge). No Mozilla page content needed.

### Sign-in From-your-computer page (`FromComputer`: viewfinder, brackets, steps)

**Purpose.** Scan the "Connect another device" QR code from Zen (or paste its link).

**Anatomy** (`ui/signin/SignInPages.kt`), column centred, horizontal padding 20dp:
1. Spacer weight 1.
2. **Viewfinder**: width **75%** of the page (`VIEWFINDER_WIDTH`), square (`aspectRatio(1f)`), radius **28dp** (`ScanRadius`). Background **black** with camera, `Card` without.
   - With camera: `QrScanner` (Camera2 back camera, TextureView preview filling the width, cropped top/bottom; preview stream near 1280×720, max 1920×1080; zxing QR decode; `ui/signin/QrScanner.kt`).
   - Without camera: `KoanIcons.Camera` **24dp** `TextSecondary`, 8dp, `SecondaryButton` "Allow the camera" on `Base` (50dp pill, 28dp padding, 16sp SemiBold `TextPrimary`, `ui/kit/Buttons.kt`).
   - **Aim brackets** on top (`aimBrackets`): four corners, inset **18dp** (`SignInTokens.BracketInset`), each leg **30dp** (`SignInTokens.BracketLength`), rounded corner radius **10dp** (quadratic curve, `SignInTokens.BracketRadius`), stroke **3dp** round cap (`SignInTokens.BracketStroke`); colour **White** with camera, `TextSecondary` without.
3. **28dp** (`StepsGap`).
4. **Steps** column (75% width, **12dp** apart, `SignInTokens.StepGap`): "In Zen, open Settings › Sync.", "Click \"Connect another device\".", "Scan the code.". Each step (`Step`): number dot **26dp** circle (`SignInTokens.StepDot`) on `Card`, number **13sp** SemiBold `TextPrimary` (`SignInTokens.StepNumberSize`); **12dp** (`SignInTokens.StepTextGap`); text **15sp** `TextPrimary`.
5. Spacer weight 1.
6. `SecondaryButton` "Paste the link instead": full width, surface `Card`, **1dp** `CardBorder` circle border.
7. **Switch link** "Use email and password instead" (`SwitchLink`): **14sp** Medium `TextSecondary` (`LinkSize`), pill-clipped tap area padded **16dp × 12dp** (`SignInTokens.LinkPaddingH`, `SignInTokens.LinkPaddingV`), centred; then **4dp** (`LinkBottom`).

**States.** Camera allowed (live preview, white brackets) / not allowed (Card + "Allow the camera", grey brackets) / camera blocked for good (button opens app settings, `SignInScreen.kt`) / finishing (inputs ignored, `enabled = false`). Errors appear as the top Notice.

**Screenshot.** Two captures: without camera permission (Card viewfinder with "Allow the camera"), and with the camera pointing at a blank surface (no real code, no account). Light and dark.

### Sign-in email/password and pairing pages (`WebPage`, Mozilla's page)

**Purpose.** Host Mozilla's own accounts page (email/password) or its pairing page, with a switch to the other way in.

**Anatomy** (`ui/signin/SignInPages.kt`): the WebView fills the area (weight 1); under it, when not finishing and the keyboard is hidden, the `SwitchLink` ("Sign in from your computer instead" on the password page, "Use email and password instead" on the pairing page, `SignInScreen.kt`) and 4dp. The page itself is Mozilla's (not Koan UI): Koan adds only the header, progress line, notices and switch link. Brand gradient is never used here (`SignInScreen.kt`).

**States.** Loading (progress line, page invisible until painted, then 220ms fade), painted, keyboard up (switch link hidden), finishing (bottom pill), error notice, unverified ("Confirm the sign-in from the email Mozilla just sent…").

**Screenshot.** Header + progress line + the switch link only; crop or blur Mozilla's page, and never show a real email.

---

### Tour / What's new sheet (`TourOverlay`)

**Purpose.** The first-run tour and each version's "What's new": a tall sheet of swipeable cards over Koan's dimmed UI.

**Anatomy** (`ui/tour/TourOverlay.kt`)
1. **Scrim**: full screen, `SheetScrim` (light `#1C1C28` @ 0.30, dark Black @ 0.50) × open progress × (1 − drag/height); swallows all touches.
2. **Sheet**: bottom-aligned, full width, height **92%** of the screen (`TourTokens.SHEET_FRACTION` 0.92); top corners **32dp** (`SheetTokens.TopRadius`); background `Dock` (light `#FFFFFF`, dark `#232326`); then the **title glow**, then the **welcome backdrop**; nav-bar padded.
3. **Skip** row: top padding **6dp** (`SkipTop`), height **44dp** (`SkipHeight`), horizontal padding 12dp (Padding/2); `TourButton` "Skip" unfilled, aligned end.
4. What's new only: "What's new in X.Y" **15sp** Medium `TextSecondary` centred, 6dp below; alpha = 1 − welcome (fades in as the intro card leaves).
5. **HorizontalPager** (weight 1, `beyondViewportPageCount = 0`) of cards.
6. **16dp** (`Gap`) · **page dots** · 16dp.
7. **Primary button**: "Next", last card "Get started" (tour) or "Got it" (What's new); full width minus 24dp each side (`Padding`); then 12dp.

**Title glow** (`titleGlow`): same 3-spot layout as the Settings wash, `h = GlowHeight` **240dp**, radius h·0.75 = 180dp, alpha light **0.16** / dark **0.12** (`TourTokens.GLOW_ALPHA_LIGHT`, `TourTokens.GLOW_ALPHA_DARK`). Spots: Pink (0.10·w, 0.15·h), Violet (0.55·w, 0.05·h), Blue (0.95·w, 0.30·h). Drawn on every card whose title sits at the top (under the welcome backdrop); it fades out on a centred card (no media) and crossfades with the pager's position (`TourCard.centred`, read at draw time).

**TourButton**: height **50dp** (`ButtonTokens.Height`), full pill, horizontal padding 28dp, text **16sp** (SemiBold filled / Medium unfilled). Filled = fill `lerp(Primary → White, FastOutSlowIn(welcome))`; label `OnPrimary` until welcome > 0.5, then **Black** (flips at mid-grey). Unfilled (Skip) = no fill, label `lerp(TextPrimary → White, welcome)`. Pressed = ripple. Note: Skip sits in the 44dp `SkipHeight` box, so its 50dp height is constrained to 44dp by the parent (Compose `height` respects incoming max constraints).

**Page dots** (`PageDots`), Canvas: dot **7dp** (`TourTokens.Dot`), current one stretches to **20dp** (`TourTokens.DotActiveWidth`), gap **7dp** (`TourTokens.DotGap`), all fully rounded; width follows the finger (`near = 1 − |pos − i|`). Colours: on = `lerp(TextPrimary → White, welcome)`, off = `lerp(TextDisabled → White @ 0.45, welcome)`, interpolated by `near`.

**Gestures and thresholds**
- Swipe between cards: `HorizontalPager` defaults (fling/snap not overridden). Next / Back animate with `Motion.PagerSettle` = spring(dampingRatio **0.86**, stiffness **300**). System Back steps to the previous card; on the first card Back does nothing.
- **Swipe down to close** (`swipeDownToClose`): vertical drag moves the sheet 1:1 (never above 0); closes if dragged past **25%** of the sheet height (`SheetTokens.COMMIT_FRACTION` 0.25) or released faster than **600dp/s** downward (`SheetTokens.FlingVelocity`); else springs back with `Motion.Sheet`. A sideways drag belongs to the pager.
- Open/close: `Motion.Sheet` spring(1.0, 380); `translationY = drag + (1 − shown)·(height − drag)`.

**Light vs dark.** Sheet `Dock` white vs `#232326`; glow 0.16 vs 0.12; scrim strength.

**Screenshot.** About › Take the tour, card 2 ("Your Zen Spaces, on your phone") at rest, light and dark; a mid-swipe from card 1 to 2 showing the gradient fading into the sheet; the last card showing "Get started".

### Tour card (`TourPage`, `TourCard`) and clip frame (`PhoneFrame`, `TourVideo`)

**Purpose.** One tour/What's new card: title, one line, then a looping clip, still or drawn scene in a phone frame.

**Card data** (`ui/tour/TourContent.kt`): title, line, dark/light MP4 (`res/raw`), framing (`Screen` = aspect **720/1478** (`SCREEN_CLIP_ASPECT`), `Landscape` = **1280/590** (`LANDSCAPE_CLIP_ASPECT`, the phone held sideways) or `Focus` = fill the slot), `welcome`, optional `icon`, dark/light still (`res/drawable-nodpi`), optional `scene` (`TourScene.SignIn`).

**Anatomy** (`ui/tour/TourOverlay.kt`), centred column:
1. (Text-only card with an `icon`: icon tile **96dp** (`TourTokens.IconTile`), radius **28dp** (`TourTokens.IconTileRadius`), `GroupedRow`, glyph **44dp** (`TourTokens.IconGlyph`) `TextPrimary`, then 24dp (Gap·1.5); content vertically centred. Used only by What's new "1.0"'s Two-way sync card (`KoanIcons.Key`), `TourContent.kt`; no title glow on it.)
2. Title **22sp** Bold centred `TextPrimary` (`TitleSize`), horizontal padding 24dp.
3. **6dp** (`TitleGap`); line **15sp** `TextSecondary` centred (`LineSize`), `minLines = 2` so frames line up across cards.
4. 16dp; frame slot: **90%** of the width (`FRAME_WIDTH`), all the height left; frame sized to the clip's aspect (plus bezels) or the whole slot for Focus; then **10dp** (FrameShadow/2).
5. Optional footer (last What's new card: changelog button).

**PhoneFrame**: outer radius **30dp** (`FrameRadius`); shadow elevation **20dp** (`FrameShadow`) with ambient and spot colour Black @ **0.18**; background `Card`; border **1dp** `CardBorder` (`TourTokens.FrameBorder`); bezel padding **5dp** (`FrameBezel`); inner clip radius 30 − 5 = **25dp**, inner background `GroupedRow` (shows until the clip's first frame).

**TourVideo** (`ui/tour/TourVideo.kt`): muted, looping MP4 via MediaPlayer on a TextureView, centre-crop to cover; view alpha 0 until the first rendered frame, then fades to 1 in **150ms** (`FADE_MS`); decoded only while composed. Stills: `Image` with `ContentScale.Crop`, top-centre aligned (`TourOverlay.kt`).

**Clips in the app** (`TourContent.kt`, files `app/src/main/res/raw/*_{dark,light}.mp4`): tour_spaces (Focus), tour_stack (Screen), tour_tabs (Focus), tour_scrub (Focus), tour_search (Screen), tour_holds (Focus); What's new 0.4: SignIn scene (Focus), whatsnew_search (Screen), whatsnew_reopen (Screen), whatsnew_engines still (Screen); What's new 1.0, every media card but the player in the same Screen frame, title and line at the top: whatsnew_private still (Screen, from the README captures), whatsnew_player clip (Landscape, the only sideways frame: Koan's player full screen on Big Buck Bunny from Wikimedia Commons, a tap for the controls, a hold for 2×, a double-tap for +10 s; one clip for both themes, the player is always dark), whatsnew_translate clip (Screen: the address bar's translate icon, the card, Translate, the page turns English), whatsnew_openapp clip (Screen: ⋯ › Open in Wikipedia, the article opens in the Wikipedia app), Two-way sync text + icon. Recorded on the Xiaomi with demo pages and public sites, status bar cropped off, taps drawn in post as rings. The dark/light variant follows Koan's theme (`TourOverlay.kt`).

**Tour card list** (`TourContent.kt`): 1 Welcome to Koan / Your Zen Spaces, in your pocket. (welcome) · 2 Your Zen Spaces, on your phone · 3 Swipe up for your tabs · 4 Swipe sideways to switch tabs · 5 Hold and slide to change space · 6 Search shortcuts · 7 Long presses.

**What's new logic** (`TourContent.kt`): opens on the intro card (`whatsNewIntro`), then the highlights of the newest feature version after the last seen one; a version without highlights shows nothing.

**States.** Clip loading (GroupedRow inner) / playing; animations off: clips still play (MediaPlayer is not gated by `animationsOff`; only the Welcome card and SignInScene check it).

**Screenshot.** One Focus-framed card (e.g. "Swipe sideways to switch tabs") and one Screen-framed card ("Swipe up for your tabs"), light and dark, mid-clip.

### Changelog card (last What's new highlight, `WhatsNewEnd`)

`ui/tour/TourOverlay.kt`: under the last highlight's frame, 16dp, then a pill button: height **50dp**, padding 28dp, background `GroupedRow`; `KoanIcons.GitHub` **20dp** (`ChangelogIcon`) `TextPrimary`; **9dp** (TitleGap·1.5); "See the full changelog" **16sp** Medium `TextPrimary`. Opens `https://github.com/Nerzouille/Koan/releases/latest` (`TourContent.kt`, `ui/settings/AboutPage.kt`). The primary button below reads "Got it". Not gradient.

**Screenshot.** What's new 0.4, last card ("Your own search engines") with the changelog button and "Got it".

### Welcome card (tour card 1) and What's new intro card (`WelcomePage`, `welcomeBackdrop`, `WelcomeMotion`)

**Purpose.** The tour's first card: the 3D K on the vivid brand gradient, a turning vortex, twinkling sparkles. The What's new intro card reuses it ("What's new in X.Y" / "A few new things since last time ✨", `TourContent.kt`) with a lighter entrance.

**Anatomy** (`ui/tour/Welcome.kt`), centred column:
1. Spacer weight 1.
2. K box **210dp** (`TourTokens.K`): sparkles layer + K image (`R.drawable.koan_k`); its centre is reported to the sheet for the vortex.
3. **28dp** (`WelcomeTextGap`); title **30sp** Bold **White** centred (`WelcomeTitleSize`), horizontal padding 24dp.
4. **6dp**; line **17sp** Medium **White @ 0.88** centred (`WelcomeLineSize`).
5. Spacer weight 0.75.
The Skip label, dots and Next button recolour to white / white-with-black-text via `welcome` (see TourOverlay).

**Brand gradient backdrop** (`welcomeBackdrop`), drawn by the whole sheet (no seam):
- `Brush.linearGradient([Pink #FF5FA2, Violet #B46BFF, Blue #4F8DFF])`, evenly spaced stops (0, 0.5, 1), from the sheet's **top-left (0,0) to bottom-right (w,h)**, alpha = `a`.
- Dark mode only: an extra Black rect at alpha `a·0.14` (`WELCOME_DIM_DARK`) — "a touch deeper".
- `a` = 1 on the card, falling linearly to 0 as the next card slides in (`1 − pagerPos`, `TourOverlay.kt`): the gradient blends into the sheet's `Dock` colour while swiping.

**Vortex** (`Vortex`), centred on the K's centre:
- Rings: **8** (`VORTEX_RINGS`), **5** without live blur (`VORTEX_RINGS_SIMPLE`).
- Radius of ring i: `120dp + i·46dp` (`VortexInner` 120, `VortexStep` 46) → 120, 166, 212, 258, 304, 350, 396, 442dp.
- Each ring: a sweep gradient of White with 5 evenly spaced stops `[0, a_i, 0, 0.55·a_i, 0]` (two bright arcs, one fainter), `a_i = 0.20·(1 − 0.6·i/rings)` (`VORTEX_ALPHA` **0.20**).
- Soft edges: every ring drawn three times with strokes **26dp**, 15.6dp (×0.6) and 6.5dp (×0.25) (`VortexStroke`).
- Centre glow: radial White @ **0.35** → 0 (`VORTEX_GLOW_ALPHA`), radius `120dp·1.4` = **168dp**.
- Rotation of ring i: `turn·(1.6 − 0.12·i) + i·47°`, where `turn = clock/24000·360°` (`VORTEX_TURN_MS` **24000**) → the base turn is 15°/s; ring 0 turns at 24°/s, ring 7 at ≈11.4°/s (inner rings faster).
- Vortex alpha = `vortex()·a`; `vortex()` ramps 0 → 1 over the first **750ms** of the intro with LinearOutSlowIn.
- Leaving: the vortex trails the page: x offset `−(1 − a)·w·(1 − 0.5)` (`VORTEX_LEAVE_TRAIL` **0.5**).

**Sparkles** (`Sparkles`): colour White, unit **9dp** (hard-coded, not a token). Six sparkles, positions as fractions of the 210dp K box:
| # | x | y | size | period ms | phase rad |
|---|---|---|---|---|---|
| 1 | −0.12 | 0.12 | 1.00 | 2300 | 0.0 |
| 2 | 1.06 | 0.02 | 0.75 | 2900 | 1.7 |
| 3 | 1.14 | 0.62 | 0.55 | 2100 | 3.1 |
| 4 | 0.02 | 1.02 | 0.60 | 2600 | 4.4 |
| 5 | 0.62 | −0.14 | 0.50 | 1900 | 2.4 |
| 6 | 0.86 | 1.10 | 0.85 | 3200 | 5.2 |
Without live blur only the first **3** (`SPARKLES_SIMPLE`).
- Shape: four-point star of radius 1 with curved-in sides (quadratic control points at ±0.12), plus a soft halo dot: radial `color @ 0.45 → 0` at **1.8×** the star radius.
- Twinkle: `wave = 0.5 + 0.5·sin(2π·t/period + phase)`, `level = wave²`; skipped below 0.02; star radius `unit·size·(0.45 + 0.55·level)`; star and halo alpha = `level`.
- Layer alpha = `sparkles()·clamp(1 − 2·leave, 0, 1)`: sparkles fade in 750→1400ms of the intro (linear) and vanish in the first half of the swipe away.

**K motion**
- **Tour entrance** (`WelcomeMotion.play`): after **180ms** (`K_RISE_DELAY_MS`) the K rises with spring(dampingRatio **0.6**, stiffness **120**) (`TourTokens.K_RISE_DAMPING`, `TourTokens.K_RISE_STIFFNESS`): `translationY = (1 − rise)·pageHeight·0.6`; rotation from **−38°** to 0 (`K_START_TURN`); scale from **0.72** to 1 (`K_START_SCALE`); alpha = clamp(rise·2.5).
- **Light sweep** across the K at **750ms** for **650ms**, FastOutSlowIn (`SWEEP_AT_MS`, `SWEEP_MS`): a diagonal band 0.35·width wide, gradient White `[0, 0.7, 0]`, from (x − band, 0.2h) to (x + band, 0.8h − 0.6·band), `BlendMode.SrcAtop` (on the K's pixels only; offscreen layer while it runs).
- **Text**: title from **850ms** for **380ms** (LinearOutSlowIn), line **120ms** later; each fades 0→1 and rises **14dp** → 0 (`TEXT_AT_MS`, `TEXT_STAGGER_MS`, `TEXT_MS`, `TextRise`). Intro total `INTRO_MS` = 850 + 120 + 380 = **1350ms**, driven linearly.
- **What's new entrance** ("pop"): the K is already risen; it pops from scale **0.86** to 1 with spring(dampingRatio **0.5**, stiffness **380**) (`TourTokens.POP_START_SCALE`, `TourTokens.POP_DAMPING`, `TourTokens.POP_STIFFNESS`), alpha = clamp(pop·3); the timeline is entered at 750 − 180 = 570ms so the sweep starts **180ms** after the pop (`POP_SWEEP_AT_MS`).
- **Idle** (only while the welcome card is at least partly on screen and animations are on, `TourOverlay.kt`): float `sin(2π·clock/3400)·7dp` (`FLOAT_MS` **3400**, `FloatAmplitude` **7dp**); tilt `sin(2π·clock/5300)·2.5°` (`FLOAT_TURN_MS` **5300**, `FLOAT_TURN` **2.5**).
- **Leaving** (swipe to card 2; `leave` 0→1 follows the pager both ways): K `translationX = −leave·pageWidth·0.6` (`K_LEAVE_EXTRA`), extra rotation `leave·−24°` (`K_LEAVE_TURN`), scale ×(1 − 0.1·leave).
- **Animations off**: everything snaps to the end of the intro, clock frozen at **1300ms** (sparkles frozen mid-twinkle).

**Light vs dark.** Same gradient; dark adds the 14% black dim. Text is always white.

**Screenshot.** About › Take the tour, card 1 at rest (after the intro, ~2s in), light and dark; and About › What's new in 0.4 intro card. Optionally a frame mid-sweep.

### Sign-in scene (What's new 0.4 highlight, `SignInScene`)

**Purpose.** "Sign in from your computer" drawn in code instead of recorded (no real account or code): a laptop with a made-up code, a phone slides in, a gradient line scans, the code turns into a check, the K shows on the phone; loops.

**Timeline** (`SignInSceneTokens`; ms within a **4400ms** loop): phone in at **250** for **750** (LinearOutSlowIn); scan at **1050** for **900** (FastOutSlowIn); check at **1950** for **450**; K at **2150** for **400**; reset at **3750** for **550** (FastOutSlowIn). Animations off: frozen at **3000ms** (`STILL_MS`) (`ui/tour/SignInScene.kt`).

**Colours** (light / dark, `SignInSceneTokens`): lid `#E4E4EA` / `#3A3A41`; display `#FFFFFF` / `#1F1F24`; base `#D3D3DB` / `#2D2D33`; code modules `#1C1C1E` / `#EDEDF2`; phone body `#1C1C1F`; phone screen vertical gradient `#2B2B33` → `#141418` (both themes). Brand mist: three radial spots Pink (0.12w, 0.18h, r 0.75·unit), Violet (0.5w, 0.45h, r 0.8·unit), Blue (0.9w, 0.85h, r 0.75·unit) at alpha **0.13** light / **0.16** dark (`SignInScene.kt`). Laptop shadow: radial Black @ 0.12 light / 0.35 dark.

**Geometry**: sizes in a scene `unit = min(w·(1 − 2·0.07)/span, 0.78·h)`; lid width 0.8u, height 0.64·lid; base 1.16·lid wide, 0.032u tall; bezel 0.022u; code 0.6·lid height, **21×21** modules (QR finder squares + fixed scatter), module gap 12%, radius 25%; phone 0.3u wide × 2.02 tall, at 0.45·lid right of centre, tilted **6°** (rises from 0.55h below, tilt 12°→6°); phone corner radius 0.17·w, screen inset 0.045·w, screen radius 0.13·w; camera corners box 0.62·screen width, arms 22%, stroke 0.025·phoneW, White @ 0.7.
- **Scan line**: horizontal gradient Transparent → Pink → Violet → Blue → Transparent, 0.4 module tall, trailing glow Violet @ 0.22 over 0.28·code height.
- **Check**: brand linear-gradient disc, r = 0.36·code size, ease-out-back scale; violet halo @ 0.3 at 2.2r; white check stroke 0.16r round; 4 violet (@ 0.9) sparkles (2 without live blur), periods 1500/1800/1300/1700ms.
- **K on phone**: violet glow @ 0.45 (r 0.6·screen width), K at 0.62·screen width scaling 0.8→1, placed by its optical centre (0.46, 0.511).

**Screenshot.** What's new 0.4, card 2 ("Sign in from your computer"), captured at the check + K moment (~2.6s into the loop), light and dark.

---

### Icons (`KoanIcons`, `ZenIcon`)

**Icon sets in the app**
1. **KoanIcons** (`ui/icons/KoanIcons.kt`): Koan's own small set drawn as Compose `ImageVector`s on a **24×24 viewport, 24dp default size**, "so we don't need material-icons-extended". No Material icon library is used (no `material-icons` dependency; only `androidx.compose.material3`, `app/build.gradle.kts`). Stroked style (`stroked()`): stroke **2** viewport units (= 2dp at 24dp; scales with the rendered size, e.g. ≈1.67dp at 20dp, 1.5dp at 18dp, ≈1.17dp at 14dp), **round caps, round joins**, no fill. Painted white and tinted by `Icon(tint = …)`.
   - Exceptions: `StarFilled` (filled + 2-unit stroke, round join); `Contrast` (2-unit stroked circle r 8.5 + filled half); `More` (three filled dots r 1.6 at x 5/12/19); `GitHub` (Octicons `mark-github-16`, MIT, filled, 16-unit viewport rendered at 24dp; notice in `THIRD_PARTY_NOTICES.md`). `Retry` is an alias of `Reload`.
2. **Zen space icons** (`ui/icons/ZenIcon.kt`): Zen's selectable icons shipped as **87** `zen_*.xml` vector drawables (512×512 viewport, filled with `@color/zen_icon_fill`; MPL 2.0, `THIRD_PARTY_NOTICES.md`), or an emoji. Vector drawn at **82%** of the slot, tinted; emoji at `0.78 × size` sp. Used only for space/folder icons, never as UI chrome.
3. **Bitmaps**: app icon `koan_icon.webp` (432×432) and the 3D K `koan_k.webp` (768×762, alpha) in `res/drawable-nodpi`; What's new stills `whatsnew_engines_{dark,light}.webp`.

**Every KoanIcons glyph and where/at what size it is used**
| Icon | Used in (path:line) → rendered size |
|---|---|
| Back | page bar `bar/AddressRow.kt` (BarIcon 24dp, `BarTokens.Icon`, touch 48dp `BarTokens.IconTouch`); overview header `overview/SpacePage.kt` (HeaderIcon 22dp in 44dp, `OverviewTokens.HeaderGlyph`); menu nav row `menu/BrowserMenu.kt` (24dp in 48dp); sheet header `sheet/SheetKit.kt` and `menu/SiteSettings.kt` (22dp, `SheetTokens.TileGlyph`); Settings/ListPage back `settings/SettingsKit.kt`, `kit/ListPage.kt` (24dp) |
| Forward | menu nav row `menu/BrowserMenu.kt` (24dp) |
| Close | bar edit/find `bar/AddressRow.kt` (24dp); menu stop `menu/BrowserMenu.kt` (24dp); tab menu `menu/TabMenu.kt` (18dp, `TabMenuTokens.Icon`); tab card close `overview/TabCard.kt` (14dp, `CardTokens.CloseIcon`); search clear `settings/PasswordsPage.kt` (16dp); download cancel `downloads/DownloadsScreen.kt` (16dp); sign-in close `signin/SignInScreen.kt` (20dp) |
| Reload / Retry | menu nav row `menu/BrowserMenu.kt` (24dp); search group/chip `search/SearchSheet.kt` (14dp, `SearchTokens.ChipIcon`); download retry `downloads/DownloadsScreen.kt` (16dp); About chip `settings/AboutPage.kt` (13dp) |
| History | overview bar `bar/AddressRow.kt` (24dp); menu row `menu/BrowserMenu.kt` (18dp, SheetRow `SheetTokens.RowGlyph`); tab menu `menu/TabMenu.kt` (18dp) |
| Plus | menu tile `menu/BrowserMenu.kt` (22dp); link menu rows `menu/LinkMenu.kt` (18dp); tab menu `menu/TabMenu.kt` (18dp); overview new tab `overview/SpacePage.kt` (20dp, `OverviewTokens.NewTabGlyph`); "Add a search engine" `settings/EnginesPage.kt` (20dp) |
| Search | bar field `bar/AddressRow.kt` (18dp, `SearchFieldTokens.Glyph`); menu tile/row `menu/BrowserMenu.kt` (22dp) (18dp); suggestion chip `search/SearchSheet.kt` (14dp); list page field `kit/ListPage.kt` (20dp); passwords search `settings/PasswordsPage.kt` (20dp); Settings row `settings/SettingsScreen.kt` (20dp) |
| Pin | tab card `overview/TabCard.kt` (14dp, `CardTokens.PinGlyph`); drawer `bar/SpaceDrawer.kt` (16dp, `DrawerTokens.PinnedGlyph`) |
| Folder | drawer folder `bar/SpaceDrawer.kt` (22dp, `DrawerTokens.PinnedFavicon`); downloads chip `downloads/DownloadsScreen.kt` (16dp); Settings row `settings/SettingsScreen.kt` (20dp) |
| ChevronDown | find-in-page next `bar/AddressRow.kt` (24dp); drawer folder chevron `bar/SpaceDrawer.kt` (16dp, rotates) |
| ChevronRight | Settings rows `settings/SettingsKit.kt` and account card `settings/SettingsScreen.kt` (20dp); sheet `RowChevron` `sheet/SheetKit.kt` (18dp); History group `history/HistoryScreen.kt` (18dp, rotates 0→90°); sign-in choice card `signin/SignInPages.kt` (18dp) |
| Check | Settings selection `settings/EnginesPage.kt`, `settings/SettingsScreen.kt` (20dp); About chip `settings/AboutPage.kt` (13dp); page prompts `menu/PagePrompts.kt` (16dp, `PromptTokens.CheckGlyph`) (18dp); login prompt `menu/LoginPrompts.kt` (14dp) |
| Link | menu nav row `menu/BrowserMenu.kt` (24dp); link menu `menu/LinkMenu.kt` (18dp); permission (cross-site cookies) `menu/PermissionPrompt.kt` (20dp in prompt, 18dp in Site settings rows) |
| Star / StarFilled | menu Bookmark tile `menu/BrowserMenu.kt` (22dp); Star in search scope chip `search/SearchSheet.kt` (14dp) |
| ArrowUp | find-in-page previous `bar/AddressRow.kt` (24dp) |
| Share | menu tile `menu/BrowserMenu.kt` (22dp); link menu `menu/LinkMenu.kt` (18dp); selection bars (History, Downloads) `kit/Selection.kt` (22dp, `SelectionTokens.Glyph`) |
| Lock | menu Site settings row `menu/BrowserMenu.kt` (18dp); nav field padlock `menu/BrowserMenu.kt`, `menu/SiteSettings.kt` (14dp, `SheetTokens.NavLockGlyph`); permission (protected media) `menu/PermissionPrompt.kt` |
| Desktop | menu Desktop site row `menu/BrowserMenu.kt` (18dp); History synced mark `history/HistoryScreen.kt` (16dp); permission (VR) `menu/PermissionPrompt.kt`; sign-in choice card `signin/SignInPages.kt` (24dp) |
| StatusBar | Settings "Hide status bar" `settings/SettingsScreen.kt` (20dp) |
| Reader | menu `menu/BrowserMenu.kt`, `menu/SiteSettings.kt` (18dp); Settings "Search suggestions" `settings/SettingsScreen.kt` (20dp) |
| Essentials | menu rows `menu/BrowserMenu.kt` (18dp); link menu `menu/LinkMenu.kt` (18dp); search group/scope `search/SearchSheet.kt` (14dp); Settings "Start page" `settings/SettingsScreen.kt` (20dp); About card `settings/AboutPage.kt` (20dp) |
| Download | menu row `menu/BrowserMenu.kt` (18dp); download prompt `menu/DownloadPrompt.kt` (20dp); link menu `menu/LinkMenu.kt` (18dp); About chip `settings/AboutPage.kt` (13dp); Settings row `settings/SettingsScreen.kt` (20dp) |
| Settings (gear) | overview bar `bar/AddressRow.kt` (24dp); menu row `menu/BrowserMenu.kt` (18dp) |
| Location, Camera, Mic, Database, Globe | permission glyphs `menu/PermissionPrompt.kt` (20dp on the prompt card; 18dp in Site settings rows `menu/SiteSettings.kt`); Camera also on the viewfinder `signin/SignInPages.kt` (24dp); Globe also Settings "Default browser" `settings/SettingsScreen.kt` (20dp) and password account row `settings/PasswordsPage.kt` (20dp) |
| Pause, Play | download round button `downloads/DownloadsScreen.kt` (16dp); Play also link menu "Play video" `menu/LinkMenu.kt`, Downloads' Open for media, Settings `settings/SettingsScreen.kt` |
| File | download file tile `downloads/DownloadsScreen.kt` (18dp, `DownloadTokens.FileGlyph`) |
| Trash | library delete swipe `kit/SwipeRow.kt` (20dp, `OnPrimary` on `Destructive`); selection bars' Delete / Remove `kit/Selection.kt` (22dp); player `player/StandalonePlayer.kt` |
| Open | selection bars' New tab (History) `kit/Selection.kt` (22dp); app-link card fallback tile `menu/AppLinkPrompt.kt` (18dp) |
| Info | Settings "About Koan" `settings/SettingsScreen.kt` (20dp) |
| Key | Settings "Passwords" and site rows `settings/SettingsScreen.kt`, `settings/PasswordsPage.kt` (20dp); login prompt `menu/LoginPrompts.kt` (18dp = 14 + 8/2); sign-in choice card `signin/SignInPages.kt` (24dp) |
| Contrast | Settings "Theme" `settings/SettingsScreen.kt` (20dp) |
| Shield | Settings uBlock `settings/SettingsScreen.kt` (20dp); menu uBlock card `menu/UBlockPage.kt` (18dp) |
| More (⋯) | page bar `bar/AddressRow.kt` (24dp) |
| GitHub | What's new changelog button `tour/TourOverlay.kt` (20dp) |
| Edit | duplicate-download card's rename pencil `menu/DuplicateDownloadPrompt.kt` (20dp, `SheetTokens.EditGlyph`) |
| Translate | ⋯ menu row `menu/BrowserMenu.kt` (18dp); translate offer `menu/TranslatePage.kt` (18dp); Settings `settings/SettingsScreen.kt` (20dp) |
| Image, Install | Downloads' Open by type: picture, APK ("Install") `downloads/DownloadsScreen.kt` (22dp in the selection bar) |
| Private | dock and address pill in private mode `bar/AddressRow.kt`; private parts `privacy/PrivateParts.kt`; Settings `settings/PrivacySettings.kt`; private download mark `downloads/DownloadsScreen.kt` (14dp, `DownloadTokens.PrivateGlyph`) |
| Video player: Volume, VolumeOff, Fullscreen, ExitFullscreen, Pip, Unlock, Speed, SiteControls, PlayFilled, PauseFilled, SkipBackFilled, SkipAheadFilled, ReplayFilled | `player/VideoPlayer.kt`, `player/PlayerParts.kt`, `player/WebPlayer.kt` (sizes in `PlayerTokens`, see `docs/VIDEO_PLAYER.md`) |

Rendered sizes come from: `BarTokens.Icon` 24dp, `SheetTokens.RowGlyph` 18dp, `SheetTokens.TileGlyph` 22dp, `SettingsTokens.RowGlyph` 20dp, `SearchTokens.ChipIcon` 14dp, `TabMenuTokens.Icon` 18dp, and the multipliers noted per row (e.g. `RowGlyph·0.8` = 16dp, `RowGlyph·0.85` = 17dp).

**Icon tints** are always neutral palette tokens (`TextPrimary`, `TextSecondary`, `Icon` (light `#6E6E73`, dark `#98989D`), `FieldHint`, `TextDisabled` when disabled, `OnPrimary` on fills, `Attention` for an insecure padlock `menu/SiteSettings.kt`). No icon is ever painted with the brand gradient.

**Screenshot.** A contact sheet is better rendered from code than captured; if captured: the page bar (Back, More), the ⋯ menu (tiles + rows), Settings root (row glyphs), Downloads in selection mode (bar glyphs).

### Brand gradient: where it is and is not used in code

Definition: `BrandTokens.Colors` = Pink `#FF5FA2` → Violet `#B46BFF` → Blue `#4F8DFF`; helpers `BrandHorizontal`, `brandOutline` (1.5dp), `BrandSpinner` (`ui/theme/Brand.kt`). Rule comment: the brand-gradient rule comment in `Tokens.kt`; spec `docs/UI_SPEC.md` "Brand gradient".

**Used (complete list from a grep of `BrandTokens|BrandHorizontal|brandOutline|BrandSpinner`):**
| Where | How | Code |
|---|---|---|
| Page load line | horizontal gradient over the filled part, two glow passes + shimmer | `ui/bar/KoanBar.kt` |
| "@" search ring + cursor | sweep gradient (closed loop) turning; gradient cursor | `ui/bar/SearchRing.kt`, `ui/bar/AddressRow.kt` |
| Toggles ON | horizontal gradient track @ 0.9 | `ui/kit/KoanToggle.kt` |
| Syncing spinner | 270° sweep arc | `ui/theme/Brand.kt`; used `ui/settings/SettingsScreen.kt`, `ui/bar/SpaceDrawer.kt`, `ui/browser/PullRefresh.kt` |
| Download progress bars | `BrandHorizontal` fill | `ui/downloads/DownloadsScreen.kt` |
| Download-done toast ("moment") | `brandOutline` | `ui/kit/KoanToast.kt` |
| Account card (not while sync fails) | `brandOutline` 1.5dp | `ui/settings/SettingsScreen.kt` `AccountCard` |
| Find-in-page field | `brandOutline(CircleShape)` 1.5dp while find is open | `ui/bar/AddressRow.kt` `FindBar` |
| First-run Connect card / pill | `brandOutline` | `ui/overview/FirstRun.kt` |
| Settings / History / Downloads wash | 3 radial spots @ 0.20 light / 0.16 dark | `ui/settings/SettingsKit.kt`, `ui/kit/ListPage.kt` |
| Tour title glow | 3 radial spots @ 0.16 / 0.12 | `ui/tour/TourOverlay.kt` |
| Welcome / What's new intro backdrop | full-sheet linear gradient + vortex | `ui/tour/Welcome.kt` |
| Sign-in scene (tour) | mist, scan line, check disc, violet glows/sparkles | `ui/tour/SignInScene.kt` |
| Sign-in Choice page | faint breathing glow behind the K; violet sparkles in light mode | `ui/signin/SignInPages.kt` |
| Find in page highlights (Gecko prefs, not the gradient) | per Koan's theme, set live through GeckoView's `GeckoPreferenceController` when the theme changes, under both `ui.<colour>` and `ui.<colour>.dark` so every page gets the theme's colours whatever its own colour scheme: current `FindCurrent` `#1554CB` / white text; others `FindMatch` `#D0DDF5` / black (light) or `#1C396E` / white (dark) | `engine/GeckoPrefs.kt`, `MainActivity.kt` |

**Not used (by code and stated rule):** primary buttons (`ui/kit/Buttons.kt`), the Recommended badge, Mozilla's pages, the sign-in header/progress line (neutral `TextSecondary`, `ui/signin/SignInScreen.kt`), password fields and Passwords pages, destructive actions, the current tab/space marker, body text, text cursors in Settings fields (solid `TextPrimary`), icons, and web content.

---

### Spec vs code disagreements (brief)
- `docs/UI_SPEC.md` "Screens" lists Settings as "account, sync, search engine, write-back toggle, add-ons, about". Code: sections General / Display / Privacy / Koan; **no write-back toggle** on the Settings page (`ui/settings/SettingsScreen.kt`).
- Typography: UI_SPEC now describes the code (system sans, `KoanType` styles, em line heights); resolved in the 2026-11-10 audit.
- `docs/UI_SPEC.md` "Visual tokens" "cards 28dp": settings cards are **22dp**, choice cards 22dp, day cards 18dp, tour frame 30dp; the floating card and tour sheet match (28dp / 32dp top).
- `docs/UI_SPEC.md` "Controls" "Destructive = red text only … never a red fill": the library rows' delete swipe background is a full `Destructive` fill (`ui/kit/SwipeRow.kt`).
- `docs/UI_SPEC.md` "Brand gradient" allows only "the faint glow behind the K" on sign-in UI; code also draws **Brand Violet sparkles** around that K in light mode (`ui/signin/SignInPages.kt`).

### Values not resolvable from repo code
- Tour `HorizontalPager` swipe fling/snap thresholds: Compose Foundation defaults (`ui/tour/TourOverlay.kt`).
- Pressed states: Material `clickable` default ripple (colour/alpha from the Material 3 theme, not Koan tokens).
- `LazyColumn` `animateItem()` specs in History: Compose defaults.


## 4. Screens

Every main screen, light and dark, in `design-export/screens/light/` and `screens/dark/` (local
only: not in the public repository, so this section is empty on GitHub). Phone: 1208×2624 px
(403×875dp).
