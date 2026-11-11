# Koan · Fullscreen video player — design hand-over

Hand-over for Claude Code, from the owner's design canvas (boards: Landscape, Portrait, Standalone, Transition, Mini-player, Minimise, Final spec).

> Status: built and merged into `dev` (`000d4a1`) without the mini-player (§6) and the minimise transition, which wait (docs/ROADMAP.md, M7). This file is the design hand-over; where it and the code disagree, the code (`ui/player/PlayerTokens.kt`) wins.

### As built (differences from this spec)

- **No minimise, no mini-player.** No ⌄ in the title pill; on a web video a swipe down (or the fullscreen button, or Back) leaves fullscreen and the video keeps playing in the page. §6 and every "Minimised" / "minimise" row below are not built.
- Symmetric safe-area insets: `PlayerTokens.Inset = 20.dp` from the screen edges (not 16dp), the cutout only on the side where it is.
- Play glyph optical offset: `PlayOptical = 1.75.dp` (72dp disc), `PlayOpticalSmall = 1.25.dp` (44dp button), not +3dp.
- Brightness / volume swipe: full range = `LEVEL_RANGE = 0.35` × screen height (not 0.6); the level indicator's track = that full range.
- Filled primary icons (play, pause, …).
- Picture-in-picture with 10 s back, play/pause, 10 s ahead and, for a web video, **Audio only** (the PiP window closes and the sound keeps playing; `engine/PictureInPicture.kt`). Switch: Settings › Display › **Picture-in-picture**.
- The media notification shows the video's position, length and poster (`engine/PageVideo.kt`; the timeline is on by default since AC 160).
- YouTube keeps its own controls: Koan's player is not used there.
- Fixes of 2026-11-12 (`docs/plans/FIXES_PLAN.md`):
  - Double tap: a side tap's show / hide waits `TAP_PAIR_MS` (250 ms) so a pair never flashes the controls; a centre tap toggles at once.
  - Gestures starting in the system's top or bottom gesture edge (`WindowInsets.systemGestures`, at least `PlayerTokens.EdgeBand` 32dp) never start a level, swipe-away or 2× hold; the progress bar scrubs only after a sideways move (or a tap), a vertical move on it is left to the system.
  - Orientation (`ui/player/FullscreenOrientation.kt`): the video's shape first; with auto-rotate on, once the phone is held that way, `SENSOR` (portrait included). Auto-rotate off (owner, 2026-11-12): the video's shape as `SENSOR_LANDSCAPE` / `SENSOR_PORTRAIT` (the landscape sides flip by themselves), and Koan's rotate pill (`rememberRotateOffer`, `rotateOffer`): when the phone turns to the other shape, a round pill shows for `ROTATE_OFFER_MS` (3 s) exactly where the fullscreen button is (`BottomPillInner*`), rising above the bottom pill with the controls' mist while they show (owner, 2026-11-12); a tap swaps the shape (`PlayerHost.fullscreenRotated`, reset when fullscreen ends; the standalone player keeps its own). The system's button can't do both: AOSP's `DisplayRotation.isValidRotationChoice` offers it only for USER_* / FULL_USER, which don't flip the landscape sides under the lock. The standalone player: `SENSOR`, or the same user orientation when auto-rotate is off. Gecko's clear colour is black while fullscreen (no white flash when it turns).
  - Portrait fullscreen keeps the status bar (light icons; `playerBars`) and puts the top row under it (`portraitInsets`: status bar + gap, plain side margins); landscape stays immersive with the 20 dp margins. The cutout rects are read again when the cutout insets change.
  - Hidden controls take no touches (`controlsTakeTouches`): a tap anywhere reaches the video and shows them; the tap rule is `tapAction` (`ui/player/PlayerTaps.kt`).
  - The ⋯ menu grows out of the ⋯ pill (a clip from the pill's rounded square to the panel, `PlayerTokens.OpenSpring` / `CloseSpring`); its top edge is level with the top row; the round pills fade out under it.
  - A mode switch (Private on / off) and the private lock take every (private) page out of fullscreen (`leaveFullscreen`).
  - ⋯ › **Download video** always shown, greyed out with a reason (protected, streamed, live, not a file); an http address with no video ending is checked (HEAD, through Gecko and the tab's session) only on the tap. A streamed video saves a plain file its page offers (`fileSource` in the video extension: its own `<source>`s, the page's renditions of one video, MediaWiki's original, or a page link to a file the video names). The download is followed in the player (`PlayerNotice`: "Downloading · size", or the failure). ⋯ › **PiP on leave** (On / Off at the end, icon crossed out when off) writes Settings' switch.
  - PiP on leave: only a playing fullscreen video; Koan's own view of the video (the extension's size and play state) counts when Gecko's media session has none.

Conventions
- Units: dp for sizes, sp for text. On the canvas 1dp = 1px (reference phone 403 × 875dp, landscape 875 × 403dp).
- Springs are written `spring(dampingRatio, stiffness)` = Compose `spring(dampingRatio = …, stiffness = …f)`.
- Easing curves are `CubicBezier(x1, y1, x2, y2)`. `FastOutLinearIn` = (0.4, 0, 1, 1), `LinearOutSlowIn` = (0, 0, 0.2, 1).
- Font: the app font (Roboto / system). Every number in the player uses tabular figures (`fontFeatureSettings = "tnum"`).
- The player is **always dark**, whatever the app theme. The mini-player follows the app theme.
- Same layout everywhere: web fullscreen, standalone (file or link), portrait and landscape. Only the differences listed in §9 change.

---

## 1. Colour and material tokens

| Token | Value | Use |
|---|---|---|
| `player.background` | `#000000` | Behind the video |
| `player.scrim.top` | linear `#000000` 45% → 0 %, 120dp tall, from the top | Shown with the controls |
| `player.scrim.bottom` | linear `#000000` 60% → 0 %, 200dp tall, from the bottom | Shown with the controls |
| `player.panel` | `#1C1C1E` @ 72% (`#B81C1C1E` ARGB) + background blur **24dp** | Every pill and disc: title pill, round pills, bottom pill, centre discs, seek label, time bubble, level indicator, speed badge, Unlock |
| `player.panel.noBlur` | `#1C1C1E` @ 90% (`#E61C1C1E`), no blur | Android 11 and lower, blur disabled, battery saver |
| `player.panel.menu` | `#1C1C1E` @ 96% (`#F51C1C1E`) + blur 24dp · shadow 0 / 12 / 24dp black 35% | ⋯ menu (near-opaque so red Delete reads on any frame) |
| `player.panel.stroke` | `#FFFFFF` @ 8% (`#14FFFFFF`), 1dp, inside | Hairline on every pill (= Koan dark `CardBorder`) |
| `player.text` | `#FFFFFF` | Title, elapsed, glyphs, numbers |
| `player.text.secondary` | `#FFFFFF` @ 80% (`#CCFFFFFF`) | Site / file line, total time, menu values |
| `player.track` | `#FFFFFF` @ 24% (`#3DFFFFFF`) | Progress and level tracks |
| `player.track.buffered` | `#FFFFFF` @ 40% (`#66FFFFFF`) | Buffered range |
| `player.track.played` | `#FFFFFF` | Played range, level fill. **No thumb dot.** |
| `player.chip` | `#FFFFFF` @ 10% · selected `#FFFFFF` bg / `#000000` text | Speed chips in the ⋯ menu |
| `player.glow` | radial ellipse 70% × 62% at the tapped edge: `#FFFFFF` 22% → 10% at 42% → 0 at 100%, width 55% of the screen | Double-tap bloom |
| `player.seekLabel.halo` | shadow 0 / 0 / blur 56dp / spread 20dp, `#FFFFFF` @ 16% | Soft glow behind the ±10 s pill |
| `player.destructive` | `#FF5A50` | Delete in the standalone file menu (Koan dark `Destructive`) |
| `player.spinner` | `#FFFFFF`, 2.5dp stroke, round caps, 75% arc, 32dp. **No gradient.** | Buffering |

Contrast (checked): text on panel 6.72:1 · secondary on panel 5.01:1 · played vs track 3.8:1 · destructive on menu panel 4.97:1.

Blur: `RenderEffect.createBlurEffect(24dp, 24dp, CLAMP)` on a snapshot layer of the video behind each pill (or Haze-style backdrop blur) on Android 12+. Below 12 → `player.panel.noBlur`.

---

## 2. Layout

### Insets
- `player.inset.side` = 16dp from the screen edges + display cutout. *(As built: 20dp, see above.)*
- Landscape: top 16dp, bottom 16dp.
- Portrait: top = status bar + 12dp (44dp on the reference), bottom = nav bar + 12dp (28dp on the reference).
- Video: fit (letterbox) centred. Landscape reference 716 × 403, portrait 403 × 227.

### Title pill (top left)
- 44dp tall, radius 22, padding 0 / 16 / 0 / 0 (left padding comes from the button).
- Leading button 44dp: **⌄ Minimise** (web, glyph `chevron-down` 24; *not built: no button on the web*) or **✕ Close** (standalone, glyph `close` 22).
- Two lines: title 15sp SemiBold / 18 line, site or file line 12sp Regular / 15 line `player.text.secondary`. One line each, ellipsis (standalone file name: ellipsis in the middle).
- Max width: landscape 45% of the screen width (394dp). Portrait: all the space up to 8dp before the right-hand pills = screen width − 16 − 16 − 8 − width of the right group (3 × 44 + 2 × 8 = 148dp, plus the "1.5×" pill and its 8dp gap when shown) → 215dp on the reference, 149dp with the speed pill. Only then ellipsis. Measure the right group at layout time, not with constants.

### Round pills (top right)
- PiP · Lock · ⋯ : 44dp discs, glyph 22, 8dp apart, right inset 16.
- When speed ≠ 1×: a 44dp pill "1.5×" (14sp SemiBold tabular, padding 0 / 14) sits before them.

### Centre group
- Centred on the video rect.
- ⏪10 / 10⏩: 56dp discs, glyph 26 (`replay-10`, `forward-10`).
- Play / Pause: 72dp disc, glyph 32 (play glyph offset +3dp x for optical centring; *as built 1.75dp*).
- Gap 40dp landscape / 28dp portrait.
- Replay (ended): 56dp pill, padding 0 / 24 / 0 / 18, glyph `reload` 24 + 10dp gap, "Replay" 16sp SemiBold.
- Buffering: the spinner (32dp) replaces the whole group.

### Bottom pill — one row in landscape, two rows in portrait
Same pill, same controls; only the arrangement follows the screen shape (standalone follows the same rule).
- Left / right inset 16, bottom inset per §Insets, radius 28, `player.panel` + stroke.
- Fullscreen toggle 44dp, glyph 22: web = `fullscreen-exit` (leave fullscreen, keeps playing in the page); standalone = `fullscreen` ↔ fit / fill (crop).
- Mute 44dp, glyph `volume` / `volume-off` 22.
- Times: 14sp Medium tabular, tracking 0.2sp. Elapsed `player.text`, total `player.text.secondary`. ≥ 1 h: `1:02:07` / `1:45:10`.
- Speed is not in the pill: it lives in the ⋯ menu, plus the "1.5×" pill at the top right when ≠ 1×.

**Landscape · one row** — no play button in the pill (the centre group is enough at this size) · height **56dp**, padding 0 / 6dp, items centred, gap 10dp:
Mute · elapsed "5:58" (min width 36dp) · progress bar (flex 1, 44dp touch) · total "9:56" (min width 36dp, right-aligned) · fullscreen.

**Portrait · two rows** — padding 14 / 12 / 6dp, total height **92dp**:
- Row 1: progress bar, full width with 6dp side margins, 24dp touch height.
- 4dp gap.
- Row 2 (44dp, gap 4dp), left → right: **Play / Pause** 44dp, glyph 24 (`pause` while playing, buffering or dragging; `play` when paused; `reload` 22 when ended; mirrors the centre button, within thumb reach) · "5:58 / 9:56" (6dp in, " / 9:56" in secondary), flex · Mute · fullscreen.
- No previous / next buttons.

### Progress bar (YouTube-Music style)
- Track 8dp, radius 4 → pressed / dragging **12dp**, radius 6.
- Played `#FFFFFF`, buffered 40%, track 24%. **No thumb.**
- Landscape: the 44dp row accepts the press. Portrait: the bar row and the row under it accept the drag. A tap jumps there.

### Time bubble (while dragging)
- 68 × 36dp, radius 18, `player.panel`, 12dp above the bottom pill.
- "1:58" 15sp SemiBold tabular, centred.
- x centre = finger x. Resting bar positions: landscape `x = 106 + (pillWidth − 216) × fraction`; portrait `x = 18 + (pillWidth − 36) × fraction`. Clamped 16dp inside the screen.

### ⋯ Menu
- 264dp wide, radius 18, `player.panel.menu`, 8dp below the round pills, right inset 16, padding 4 / 0.
- Rows 44dp, padding 0 / 16, glyph 18 + 12dp, 15sp Medium. Divider 1dp `player.panel.stroke`.
- Speed: row "Playback speed" + value, then chips 0.5 · 0.75 · 1 · 1.25 · 1.5 · 2: 32dp tall, radius 16, gap 6, 13sp (Bold when selected), padding 0 / 12 / 10.
- Web: Speed · Download video · Use the site's controls.
- Standalone file: Speed · Share · Open with another app · Delete (`player.destructive`, then a dark confirm card).
- Standalone link: Speed · Download video · Share link.

### Seek feedback (double tap)
- Glow: `player.glow`, full height, from the tapped edge.
- Label pill: **72dp** tall, radius 36, padding 0 / 26 / 0 / 20, glyph 36 (`replay-10` / `forward-10`) + 12dp gap, "−10 s" / "+20 s" **28sp Bold** tabular `#FFFFFF`, halo `player.seekLabel.halo`.
- Position: vertically centred; horizontal centre at 20% of the width from the tapped edge (landscape), 29% (portrait).

### Speed badge (long press 2×)
- 32dp tall, radius 16, padding 0 / 12 / 0 / 14, "2×" 15sp Bold + double chevron 18, centred horizontally, 6dp below the top inset.

### Level indicator (brightness left / volume right)
- **60 × 240dp**, radius 30, `player.panel`, padding 16 / 0 / 14, vertically centred.
- 32dp from its edge (landscape), 20dp (portrait).
- Top: value "70" **16sp Bold** tabular.
- Middle: track **12 × 140dp**, radius 6, `player.track`; fill `#FFFFFF` from the bottom, radius 6.
- Bottom: glyph 26.
  - Sun (brightness): core circle r = 3 + 0.6 × f; 8 rays from r 5.6 to r 7 + 4.5 × f; the whole sun rotates 45° × f (f = level 0…1). Stroke 2, round caps.
  - Speaker (volume): 0 → speaker + ✕ (muted) · 1–33% → 1 wave · 34–66% → 2 waves · 67–100% → 3 waves.
- Scales to **1.06** while the finger is down (transform origin = its screen edge), back to 1 on release.

### Unlock (locked state)
- Takes Lock's place top right: 44dp pill, padding 0 / 16 / 0 / 12, glyph `unlock` 22 + 8dp, "Unlock" 15sp SemiBold.

---

## 3. States

| State | Top pills | Centre | Bottom pill | Lifetime |
|---|---|---|---|---|
| Playing | ● | Pause + ±10 | ● | auto-hide 3 s |
| Paused | ● | Play + ±10 | ● | stays |
| Buffering | ● | spinner alone | ● | while stalled |
| Ended | ● | Replay | ● (100%) | stays |
| ⋯ menu open | ● + menu (+ speed pill if ≠ 1×) | ● (keeps state) | ● | until closed |
| Seeking (drag) | ● | hidden | ● track 12dp + bubble | while pressed |
| Double tap ± | — | — | — | glow + label |
| Long press 2× | — | — | — | badge while held |
| Brightness / volume | — | — | — | level indicator |
| Locked | Unlock only | — | — | Unlock visible 3 s after a tap |
| Hidden | — | — | — | tap shows |
| Minimised *(not built)* | — | — | — | mini-player card in the app |

---

## 4. Gestures

Zones across the width: **left 35% · centre 30% · right 35%**.

| Gesture | Behaviour |
|---|---|
| Tap | Toggle controls on ACTION_UP. A 2nd tap within 250ms in a side zone turns the pair into a seek and reverts the toggle (controls out 120ms). |
| Double tap side | Left −10 s, right +10 s. Each extra tap within 700ms adds 10 s ("+20 s"). Haptic `CLOCK_TICK` per step. Works with controls hidden. Centre zone: nothing. |
| Long press | 400ms on the video, playing only → 2× while held, previous speed on release. Haptic `LONG_PRESS`. Speed badge. |
| Vertical swipe | Starts after 12dp with \|dy\| > 1.5 \|dx\|. Left zone = window brightness, right zone = media volume. Full range = 0.6 × screen height (*as built 0.35*). `CLOCK_TICK` at 0 and 100%. |
| Swipe down (centre) | Video follows 1:1, scale 1 → 0.9 over 0.4 × height. Release past 0.2 × height or ≥ 800dp/s → **minimise** (web; *not built: leaves fullscreen*) / **close** (standalone); otherwise spring back `spring(1.0, 600)`. |
| Progress drag | Press: track grows, bubble shows, centre group hides. Seeks live, throttled to 80ms. Tap on the track = jump. |
| Locked | Everything off; a tap shows Unlock for 3000ms. |
| Back | Menu open → close it · web → exit fullscreen (keeps playing in the page) · standalone → close. |

---

## 5. Motion

| Token | Spec |
|---|---|
| `controls.in` (mist) | alpha 0 → 1 and blur 12dp → 0 over 200ms `CubicBezier(0.2, 0.8, 0.3, 1)` · scale 0.96 → 1 + translate 8dp → 0 toward its own edge with `spring(1.0, 700)` · origins: title pill top-left, round pills top-right, bottom pill bottom-centre, centre group centre · blur part on Android 12+ only |
| `controls.out` | 160ms `FastOutLinearIn`: alpha 1 → 0, blur 0 → 8dp, scale 1 → 0.98 |
| `controls.autoHide` | 3000ms after the last touch while playing. Never while paused, ended, menu open, or pressing the bar. Buffering keeps the spinner alone. |
| `playPause` | glyph crossfade 120ms + scale 0.8 → 1 `spring(0.6, 600)` · disc press scale 0.92 `spring(0.7, 700)` |
| `progress.press` | track 8 → 12dp, radius 4 → 6, `spring(0.8, 600)` · bubble mists in 120ms, follows the finger 1:1, out 160ms on release · centre group out 160ms while pressed |
| `seek.glow` | alpha 0 → 1 in 120ms, holds while taps continue, fades 1 → 0 over 400ms ease-out starting 700ms after the last tap · each tap pulses scale 0.96 → 1 `spring(0.6, 500)` · label in 120ms, out 160ms with the glow · glyph turns ∓30° per tap `spring(0.5, 600)` |
| `speed.badge` | in 200ms (mist), out 160ms |
| `level` | in **150ms** (alpha + scale 0.94 → 1) at swipe start · finger down: scale 1 → 1.06 `spring(0.7, 600)` · fill and number follow with `spring(1.0, 900)` (never jumpy; the number shows the animated value, rounded) · sun rays / speaker waves morph 120ms · release: scale back to 1 `spring(0.7, 600)` · out **200ms**, 600ms after release |
| `buffering` | spinner appears after 400ms of stall, fades in 200ms, 900ms per turn, linear |
| `menu` | in 160ms: alpha + scale 0.92 → 1, origin = ⋯ button · out 120ms |
| `standalone.open` | grows from the source rect (Downloads row thumbnail or tapped link) to fullscreen, `spring(1.0, 420)`; black backdrop 0 → 1 in 180ms; controls mist in after |
| `standalone.close` | back into the source rect, `spring(1.0, 520)`; if the source is gone, scale 1 → 0.9 + fade 200ms |

---

## 6. Mini-player (not built)

> Not built: waits for ROADMAP M7. Nothing in this section is in the app.

Appears when the user minimises a web video (⌄ or swipe down). Keeps playing.

### Where it lives
- Global overlay above Koan screens while a video is minimised: page view, Downloads, History, Settings. Navigating never removes it.
- **Not shown in the overview**: it fades out 120ms when the overview opens and back in 160ms when it closes. The video keeps playing.
- Under sheets and the drawer while they are open; back when they close.
- Left / right 10dp (= Koan floating card).
- Bottom:
  - Page view: 8dp above the dock (74dp + nav bar).
  - No dock: 8dp above the screen's own bottom row (Downloads search row), else 12dp above the nav bar.
  - Lifts with the keyboard.
- Toasts rise to 10dp above it. Opening another video replaces it.

### Card
- 64dp tall, radius 18, padding 0 / 6dp, gap 12dp, items centred.
- Surface Koan `Card` (`#FFFFFF` light / `#2C2C2E` dark), 1dp inside `CardBorder` (`#E4E4EA` / `#FFFFFF` 8%), shadow = stack card (light 0 / 4 / 8dp `#1E1E3C` 25%, dark 0 / 10 / 20dp black 60%).
- Content, and nothing else:
  1. **Thumbnail** 92 × 52dp (16:9), radius 12, the live video surface, crop.
  2. **Title** 15sp Medium `TextPrimary`, one line, ellipsis, flex 1.
  3. **Play / Pause** 44dp, glyph 24 `TextPrimary`. Buffering: plain spinner 20dp, 2dp stroke `TextPrimary`, no gradient. Ended: `reload`.
  4. **✕** 44dp, glyph 20 `Icon` colour (`#6E6E73` / `#98989D`).
- No site line, no progress line.

### Gestures
| Gesture | Result |
|---|---|
| Tap card / swipe up | Expand back to fullscreen |
| Play / Pause | Toggle |
| ✕ | Close and stop |
| Swipe down | Card follows the finger, alpha 1 → 0.4 · past 50% of its height or ≥ 800dp/s → close and stop · else spring back `spring(1.0, 600)` |
| Back, page changes, tab switches | Never affect it |

Only ✕ and swipe down stop the video.

### Transitions
| Token | Spec |
|---|---|
| `minimise` | controls out 120ms → the video rect flies to the thumbnail rect `spring(1.0, 420)`, corner radius 0 → 12dp · black backdrop 1 → 0, 220ms `LinearOutSlowIn`, revealing the screen · card surface, title and buttons fade in 160ms starting at t = 0.6 · if fullscreen was landscape, rotate back to the user's orientation first |
| `expand` | the same path backwards: card content out 100ms → rect `spring(1.0, 420)` to the video rect, corner 12 → 0 · backdrop 0 → 1 in 180ms · controls mist in 160ms after |
| `mini.close.swipe` | translateY to +80dp + alpha → 0, 180ms, playback stops |
| `mini.close.x` | alpha 1 → 0 + translateY 16dp, `spring(1.0, 700)`, playback stops |

Risk: minimising a **web** video needs the GeckoView media session / fullscreen element to survive leaving fullscreen while its page is not visible. Validate early; fallback = keep the page's `GeckoSession` active and render its video surface in the card.

---

## 7. Standalone mode

| | |
|---|---|
| Entry | Downloads row (or Open) · long-press a link › Play video · a video URL opened from another app |
| Container | Overlay above the current screen, not a tab: nothing underneath changes; closing returns to the same scroll, focus and sheet |
| Title pill | ✕ Close instead of ⌄ · file name with a middle ellipsis · "Downloads · 64.2 MB" or the host |
| ⋯ menu | file: Speed · Share · Open with another app · Delete / link: Speed · Download video · Share link |
| Fullscreen toggle | fit ↔ fill (crop to the screen) |
| Minimise | none: swipe down closes |

---

## 8. Icons (KoanIcons, 24 grid, 2dp stroke, round caps and joins)

New: `chevron-down` (reused if present), `fullscreen-exit`, `fullscreen`, `replay-10`, `forward-10`, `volume`, `volume-off`, `pip`, `unlock`, `brightness`, `speed`, `site-controls`.
Reused: `play`, `pause`, `more`, `close`, `lock`, `reload`, `download`, `share`, `open`, `trash`.
The "10" in `replay-10` / `forward-10` is 7.5 units, Bold, centred at y 15.6. `brightness` and `volume` are drawn live by the level indicator (see §2).

---

## 9. Web vs standalone differences (everything else identical)

| | Web fullscreen | Standalone |
|---|---|---|
| Leading button | ⌄ Minimise *(not built: none)* | ✕ Close |
| Swipe down | minimise to the mini-player *(not built: exit fullscreen)* | close |
| Fullscreen toggle | exit fullscreen | fit ↔ fill |
| ⋯ menu | Speed · Download video · Use the site's controls | see §7 |
| Back | exit fullscreen | close |

---

## 10. Compose token sketch

```kotlin
object PlayerTokens {
    // colour
    val Background = Color(0xFF000000)
    val Panel = Color(0xB81C1C1E)          // 72 %
    val PanelNoBlur = Color(0xE61C1C1E)    // 90 %
    val PanelMenu = Color(0xF51C1C1E)      // 96 %
    val Stroke = Color(0x14FFFFFF)         // 8 %
    val Text = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xCCFFFFFF)  // 80 %
    val Track = Color(0x3DFFFFFF)          // 24 %
    val TrackBuffered = Color(0x66FFFFFF)  // 40 %
    val TrackPlayed = Color(0xFFFFFFFF)
    val Chip = Color(0x1AFFFFFF)           // 10 %
    val Destructive = Color(0xFFFF5A50)
    val Blur = 24.dp

    // layout
    val Inset = 16.dp                       // as built: 20.dp
    val PillHeight = 44.dp
    val RoundPill = 44.dp
    val BottomPillHeightLandscape = 56.dp; val BottomPillHeightPortrait = 92.dp; val BottomPillRadius = 28.dp
    val TrackThickness = 8.dp; val TrackThicknessPressed = 12.dp
    val SideDisc = 56.dp; val PlayDisc = 72.dp
    val SeekLabelHeight = 72.dp; val SeekLabelText = 28.sp
    val LevelSize = DpSize(60.dp, 240.dp); val LevelTrack = DpSize(12.dp, 140.dp)
    val Spinner = 32.dp; val SpinnerStroke = 2.5.dp
    val MenuWidth = 264.dp

    // motion
    val ControlsIn = tween<Float>(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f))
    val ControlsOut = tween<Float>(160, easing = FastOutLinearInEasing)
    val ControlsSpring = spring<Float>(dampingRatio = 1f, stiffness = 700f)
    const val AutoHideMs = 3000L
    val MinimiseSpring = spring<Float>(dampingRatio = 1f, stiffness = 420f)
    val LevelScale = spring<Float>(dampingRatio = 0.7f, stiffness = 600f)
    val LevelFill = spring<Float>(dampingRatio = 1f, stiffness = 900f)

    // gestures
    const val SideZone = 0.35f
    const val DoubleTapWindowMs = 700L
    const val LongPressMs = 400L
    val SwipeSlop = 12.dp
    const val SwipeDownCommit = 0.2f       // of height
    val FlingCommit = 800.dp               // per second
}
```
