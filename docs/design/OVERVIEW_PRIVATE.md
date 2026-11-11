# Koan · Tab overview, deeper dark and Private mode — implementation spec

Hand-over for Claude Code (the design canvas is kept outside the repo).

Approved: card separation, two-line card headers, amber sync dot, deeper dark base `#121214` everywhere, Private mode with **Dusk** (light theme) and **Night** (dark theme), the toggle at the top right of the overview header, the sequenced transition both ways, the private empty state.

## Deviations as built

The code wins over this hand-over; current values are in `ui/theme/Tokens.kt` and `DESIGN_SYSTEM.md`.

- **Toggle off colour (§1, §4.2):** only the Private toggle uses `#86868B` (light) / `#636366`
  (dark) (`PrivateTokens.TrackOffLight` / `TrackOffDark`). Every other toggle keeps `ToggleOff`
  `#848489` (light) / `#8E8E93` (dark).
- **Sync dot (§3.1):** drawn with its own `SyncDot` token (`#C0841A` light / `#E8B45A` dark), not
  `Attention` (which is `#8A5C00` in light).
- **Private tabs after a restart (§4.1):** they come back, saved sealed on the phone
  (`privacy/PrivateVault.kt`, Keystore key); cookies and site data are not saved, so sites sign you
  out when Koan restarts.
- **Private search (§4.6):** the "mask 14 + Private" row chip is not drawn; the group is titled
  "Switch to private tab".
- **Empty state (§4.7):** four facts, not three; the second is "Private tabs stay open, but sites
  sign you out when Koan restarts" (`ui/privacy/PrivateParts.kt`, `PRIVATE_FACTS`).
- **Stars (§5):** they avoid the card pile's whole area (card side margins, header to dock), not
  each card's bounds.
- **Transition (§6):** replaced, see the note at the top of §6.
- **Not in this spec, built:** the private-tab lock (fingerprint or PIN after 1 min away or the
  screen off), no screenshots (FLAG_SECURE, with an "Allow screenshots" switch) and the engine
  protections: see `docs/FEATURES.md` §14 and Settings › Private browsing / All tabs.

Conventions
- Units: dp / sp. The reference phone is 403 × 875dp.
- Colours `#RRGGBB` or `#RRGGBBAA` (alpha last, as in the Koan design system). Compose: `Color(0xAARRGGBB)`.
- Springs: `spring(dampingRatio, stiffness)`.
- `e(x)` = cubic ease-out `1 − (1 − x)³`, clamped to 0…1. `seg(t, a, b)` = `e((t − a) / (b − a))`.
- Everything not listed here stays as it is in the Koan design system (`DESIGN_SYSTEM.md`, `Tokens.kt`).

---

## 1. Dark theme greys after the `#121214` change

Applies to every dark surface: overview, page view, search sheet, Settings and its pages, History, Downloads, drawer, ⋯ menu and over-page sheets, prompts, toasts. Light theme is unchanged.

Back to front: **Base < Dock < Card < GroupedRow / SegmentPill**.

| Token | Dark before | Dark now | Notes |
|---|---|---|---|
| `Base` | `#1C1C1EFF` | **`#121214FF`** | Every screen background; the stack fades to it above the dock |
| `Dock` | `#252527FF` | **`#232326FF`** | Dock, drawer, ⋯ menu, over-page sheets · 1.19:1 on Base (was 1.11) |
| `DockShadow` | `#00000059` | **`#00000073`** | Upward dock shadow, 28dp |
| `Card` | `#2C2C2EFF` | `#2C2C2EFF` | Unchanged · 1.34:1 on Base (was 1.22) |
| `CardStrip` | `#2C2C2EF2` | `#2C2C2EF2` | Unchanged |
| `Notice` | `#2C2C2EF2` | `#2C2C2EF2` | Unchanged |
| `GroupedRow` | `#FFFFFF12` | `#FFFFFF12` | Unchanged (≈ `#303033` on Dock, 1.23:1) |
| `Glass` | `#FFFFFF14` | `#FFFFFF14` | Unchanged |
| `BarField` | `#FFFFFF14` | `#FFFFFF14` | Unchanged · FieldHint on it 5.1:1 |
| `SegmentPill` | `#4A4A4DFF` | `#4A4A4DFF` | Unchanged |
| `TextPrimary` | `#FFFFFFFF` | `#FFFFFFFF` | |
| `TextSecondary` | `#FFFFFF9E` | `#FFFFFF9E` | 7.6:1 on Base, 6.3:1 on Card |
| `Icon` | `#98989DFF` | `#98989DFF` | 6.5:1 on Base |
| `DrawerScrim` / `SheetScrim` | `#00000073` / `#00000080` | unchanged | They sit over pages and cards |
| Settings / History / Downloads brand wash alpha | 0.14 | **0.16** | Keeps its strength on the darker base |
| Stack dim | towards black | unchanged | |
| `ToggleOff` (all toggles) | `#3A3A3CFF` | **`#636366FF`** | 3.1:1 on Base (see §4.2) |

Light `ToggleOff` becomes **`#86868BFF`** (3.3:1 on `#F5F5F7`); today's `#E5E5EA` is 1.12:1.

---

## 2. Depth stack: card separation

Each card reads against the base and against the card behind it, at every depth. All values live in the card's graphics layer, so they scale with `scale(d)`.

| Token | Light | Dark | Use |
|---|---|---|---|
| `stack.card.hairline` | `#1C1C301A` (10%), 1dp | `#FFFFFF1F` (12%), 1dp | All around the 28dp card shape. Drawn **above** the depth-dim overlay so back cards keep their edge. Replaces the `CardBorder` outline on stack cards |
| `stack.card.rim` | — | `#FFFFFF1A` (10%), 1dp, top edge, inside | Separates stacked dark header strips |
| `stack.contact.color` | `#1C1C302E` (18%, was 10%) | `#000000A6` (65%, was 22%) | `ContactShadow` layer above each card's top edge |
| `stack.contact.length` | 16dp (was 14) | 18dp (was 14) | Still ≤ 35% of the visible sliver of the card behind |
| `stack.key.elevation` | 12dp (was 8) | 20dp | `shadowElevation` |
| `stack.key.color` | ambient + spot `#1C1C3024` (14%) (was `#1E1E3C80` spot) | black `#00000099` (60%) | |
| `stack.dim.max` | 0.60 (was 0.85), gain 1.25, haze to Base | 0.80 (was 1.0), gain 1.0, to black | |

Per depth (`d` = 0 focused, > 0 behind, < 0 in front):
- Hairline and rim: alpha 1 at every depth (never dimmed).
- Contact shadow: × card alpha (fades with the card, swipe fade included).
- Key shadow alpha: × (1 − 0.1 d), min 0.6. The peek card (d = −1) keeps full shadow.

Private palettes use their own hairline (§5.1).

---

## 3. Overview header and card header

### 3.1 Space header (normal)
- Height 52dp, top = status bar inset, padding start 6dp / end 10dp, row centred.
- Back button 44dp, glyph 22 `TextPrimary`.
- Space icon box 32dp, radius 10, `Glass`, Zen icon 20.
- 10dp gap → space name 20sp Bold `TextPrimary`, one line, ellipsis.
- **No meta line.**
- **Sync badge**: only when sync fails, a 7dp dot in `Attention` (`#C0841A` light / `#E8B45A` dark, 9.9:1 on dark Base), 8dp after the name, in a 36dp touch target. Tap = retry, or sign in when signed out. Syncing and synced show nothing (the old spinner is removed). contentDescription "Sync failed, retry".
- **Private toggle** right-aligned at the end of the row (§4.2), state off.
- Drag down on the header > 48dp dismisses the overview (unchanged).

### 3.2 Stack card header (two lines)
- Strip 44dp, `CardStrip`, no blur. Padding start 12 / end 6.
- Favicon 24dp (corner 28% of size) → 10dp gap.
- Column, flex:
  - Title 14.5sp / 18 SemiBold `TextPrimary`, one line, ellipsis. If the title ends with " - <site name>" that repeats the host (e.g. " - Wikipedia"), drop the suffix.
  - Host 11.5sp / 14 Regular `TextSecondary`, one line (registrable host, e.g. `en.wikipedia.org`).
- Pinned glyph, current-tab 6dp dot and ✕ (44dp touch, 30dp circle) as today.
- Header dim as today.

---

## 4. Private mode

### 4.1 Model
- Private is a **mode of the overview**, not a space. It is **not** in the space switcher.
- Private tabs are one separate set. Turning Private off does **not** close them; they stay until closed. When the last private tab closes, history, cookies and site data of the private session are deleted (GeckoView private session).
- While Private is on: the space pager is disabled, the dock shows the private state, the tab count badge counts private tabs, opening a card goes to the page view with the private dock.
- Back in the private overview = leave the overview (to the current private tab), not leave Private.
- The mode is remembered per app process; a cold start opens normal mode.

### 4.2 Toggle (`PrivateToggle`)
- Position: end of the header row, both modes.
- Touch target **56 × 48dp**. Track 56 × 32dp, radius 16. Knob 26dp, `#FFFFFF`, shadow 0 / 1 / 3dp black 25%, knob x = 3dp (off) → 27dp (on).
- Mask glyph `icon-private` 17dp inside the knob.

| Part | Light | Dark |
|---|---|---|
| Track off | `#86868B` (3.3:1 on Base) | `#636366` (3.1:1 on Base) |
| Track on | `#4B3FD6` (5.2:1 on Dusk Base) | `#7B6CF0` (4.7:1 on Night Base) |
| Mask off | `#6E6E73` | `#636366` |
| Mask on | `#2E2399` | `#3A2FB8` |

- Semantics: `Role.Switch`, contentDescription "Private browsing", stateDescription on/off.
- Tap: `CLOCK_TICK` haptic, run the transition (§6). Taps are ignored while a transition runs (760ms).

### 4.3 Private header
- Back 44 · "Private" 20sp Bold `TextPrimary` · 10dp · **"Not saved" chip** · toggle (on), right.
- **No icon tile** (the toggle carries the mask).
- "Not saved" chip: height 24dp, radius 12, padding 0 / 10, `private.pill` fill, 12sp SemiBold `private.ink`.

### 4.4 Private dock (overview)
- Same dock (156dp incl. nav, top radius 24, handle).
- Row 1 (66dp): **no space switcher**. One centred pill: height 36, radius 18, padding 0 / 16 / 0 / 12, `private.pill`, mask 20 + 7dp + "Private" 14sp SemiBold `private.ink`.
- Address row: History · pill with mask 18 (`private.ink`) + 8dp + "Search privately" (`FieldHint`) · tab count (private tabs) · Settings.

### 4.5 Private page view
- Dock surface, field and icons in the private palette.
- Address pill: mask 18 in `private.ink`, 8dp gap, then the host (17sp Medium `TextPrimary`).
- The page keeps its own colours.

### 4.6 Private search sheet
- `SheetScrim` of the private palette.
- First item, a notice: radius 18, fill `Card` + `private.pill` over it, padding 10 / 14, mask 20 + 10dp + "Private search: only your bookmarks are suggested, and nothing you type is kept." 13sp / 18 Medium `private.ink`.
- Groups (Card, radius 18, as today):
  - **"Bookmarks"**: bookmarks only. No history is read.
  - **"Switch to private tab"**: private tabs only. Row chip: mask 14 + "Private".
  - Engine suggestions (chips).
- Never offers normal tabs from Private, nor private tabs outside Private.
- Field while editing: ring 1.5dp `private.accent`; text selection `#4B3FD638` (light) / `#9C8CFF59` (dark).

### 4.7 Private empty state (no private tabs)
- Centred column, side padding 32dp, top at 230dp from the screen top.
- Mask 36 in a 64dp box, radius 20, `private.pill`, `private.ink` → 18dp.
- "No private tabs" 18sp / 24 SemiBold `TextPrimary` → 6dp.
- "Browse without leaving a trace on this phone." 14sp / 20 `TextSecondary` → 20dp.
- Three facts, 8dp apart: 6dp dot `private.accent` (8dp from the top), 10dp gap, 14sp / 21 `TextSecondary`, left-aligned:
  1. History, cookies and site data are deleted when you close the last private tab
  2. Downloads and bookmarks you save are kept
  3. Sites, your network and your employer can still see what you visit
- → 24dp → Primary button 48dp, radius 24, padding 0 / 22, plus glyph 20 + 8dp, "New private tab" 16sp SemiBold. Dusk: `#17142E` fill / `#FFFFFF` text. Night: `#FFFFFF` / `#17142E`.
- Star field uses the empty-state placement (§5.3).

---

## 5. Private palettes and atmosphere

### 5.1 Palettes

| Token | Dusk (light theme) | Night (dark theme) |
|---|---|---|
| `Base` | `#DEDAF0` | `#110F24` |
| `Card` | `#FFFFFF` | `#1E1C38` |
| `CardStrip` | `#FFFFFFF0` | `#1E1C38F2` |
| `CardBorder` | `#CFCAE8` | `#B9B4FF24` |
| `stack.card.hairline` | `#17142E1A` | `#B9B4FF29` |
| `stack.card.rim` | — | `#FFFFFF1A` |
| `CloseButton` / `CloseIcon` | `#17142E0F` / `#524D70` | `#FFFFFF1A` / `#FFFFFFBF` |
| `Dock` | `#F6F4FD` | `#18162F` |
| `DockShadow` | `#281E6E1A` | `#00000073` |
| `BarField` | `#E9E6F6` | `#FFFFFF14` |
| `Glass` | `#D2CDEA` | `#FFFFFF14` |
| `GroupedRow` | `#EEECF8` | `#FFFFFF12` |
| `DrawerHandle` | `#17142E2E` | `#FFFFFF33` |
| `TextPrimary` | `#17142E` (13.1:1 on Base) | `#FFFFFF` (18.8:1) |
| `TextSecondary` | `#524D70` (5.8:1 on Base, 7.9:1 on Card) | `#FFFFFFA3` (8.0:1) |
| `Icon` | `#524D70` | `#A5A1C4` |
| `FieldHint` | `#6E6990` (4.2:1 on BarField) | `#FFFFFF94` |
| `private.accent` | `#4B3FD6` | `#9C8CFF` |
| `private.pill` | `#4B3FD629` (16%) | `#9C8CFF33` (20%) |
| `private.ink` | `#2E2399` (6.8:1 on the pill) | `#CFC6FF` (8.6:1 on the pill) |
| `SheetScrim` | `#17142E57` | `#05041299` |
| Stack dim | haze to Base (`#DEDAF0`), gain 1.25, max 0.60 | to `#050412`, gain 1.0, max 0.80 |

Cards stay white in Dusk so they lift off the lavender base.

### 5.2 Atmosphere layers (behind the stack, under the header and status bar)

Drawn in this order over `Base`:

1. **Gradient**: vertical linear, from the top of the screen.
   - Dusk: `#AFA4E4` → transparent over 480dp.
   - Night: `#1F1A48` → transparent over 420dp.
2. **Glow**: three radial ellipses (centre colour at alpha → same colour at 0), in a 380dp band from the top, transform origin top centre.

   | # | Shape (radius x × radius y at centre) | Dusk | Night |
   |---|---|---|---|
   | 1 | 280 × 220dp at (16% width, 20dp) | `#4B3BE0` α 0.34 | `#7864FF` α 0.26 |
   | 2 | 260 × 200dp at (86% width, 0) | `#8050E8` α 0.26 | `#9A6CFF` α 0.20 |
   | 3 | 320 × 170dp at (50% width, 200dp) | `#4B3BE0` α 0.14 | `#7864FF` α 0.10 |

3. **Star field**: §5.3.

### 5.3 Star field

| Parameter | Dusk | Night |
|---|---|---|
| Stars | 56 | 56 |
| Star radius | random from {0.6, 0.7, 0.8, 1.0, 1.2, 1.4}dp | same |
| Star colours | 60% `#20166F`, 40% `#4A22B0` | 60% `#FFFFFF`, 40% `#D6CEFF` |
| Star alpha | `0.72 × (1 − y / 380dp)^1.1 × w` | `0.58 × (1 − y / 380dp)^1.1 × w` |
| Sparkles (four-point star) | 7, sizes from {8, 10, 12, 14}dp, colour `#4A22B0` | 7, colour `#D6CEFF` |
| Sparkle alpha | `0.82 × (1 − y / 380dp)^0.6` | `0.66 × (1 − y / 380dp)^0.6` |

- `w` = per-star random weight 0.5…1.0.
- Area: the top 360dp of the screen.
- Seed: per install, so the sky is stable for a user.
- Static at rest. The twinkle plays only when stars appear and disappear (§6).
- Sparkle path (24 grid, filled): `M12 1 L13.6 10.4 L23 12 L13.6 13.6 L12 23 L10.4 13.6 L1 12 L10.4 10.4 Z`.

**Placement: never over UI.** Reject a candidate point (and, for sparkles, its whole box) if it falls inside any of these rects. Draw until 56 stars and 7 sparkles are placed.
- The header row: full width, status bar inset − 6dp to header bottom + 6dp. This includes the toggle.
- Every visible card's on-screen bounds after `scale(d)`, expanded by 6dp (d = 4 … −1).
- The dock zone: dock top − 10dp to the screen bottom.
- In the empty state, the empty-state column bounds (20 … 383dp × 214 … 660dp) replace the card rects.

Recompute on layout changes (rotation, window size, empty ↔ non-empty). In practice the stars land in the status-bar strip, the gap under the header and the sides of the card pile.

---

## 6. Transition: toggle on / off

> **Replaced (owner, 2026-11-11):** Private is a page beside the current space and the flip is the space pager's own motion, instead of the mist below; the stars appear from the right. See `docs/DESIGN_SYSTEM.md` › Private, Transition.

One time driver `t` from 0 to **760ms**, linear (`Animatable` with `tween(760, easing = LinearEasing)`). Every part reads its own segment.

Rules:
- Nothing is ever shown twice. Old content fully leaves before new content arrives.
- **Mist out** = alpha 1 → 0, blur 0 → 8dp, translateY 0 → −6dp.
- **Mist in** = alpha 0 → 1, blur 8 → 0dp, translateY +6 → 0dp.
- Blur on Android 12+ (`RenderEffect`); without it, alpha and translate only.

### 6.1 On (normal → Private)

| ms | Part | What happens |
|---|---|---|
| 0 | Haptic | `CLOCK_TICK` |
| 0 → 200 | Knob | x 3 → 27dp, `seg(t, 0, 200)`; track and mask colours lerp with it |
| 0 → 450 | Colours | Base, Dock, BarField, text and icon colours lerp normal → private (sRGB), `seg(t, 0, 450)` |
| 0 → 450 | Gradient | alpha 0 → 1, same curve as the colours |
| 150 → 270 | Title | The space name + its icon box **mist out** |
| 150 → 270 | Dock texts | space switcher, "Search or type URL" and the tab count **mist out** |
| 150 → 300 | Cards out | normal stack: translateY 0 → +28dp, scale 1 → 0.96, alpha 1 → 0 (origin 50% / 30% of the stack). Fully gone at 300 |
| 270 → 420 | Title | "Private" + "Not saved" chip **mist in** |
| 270 → 420 | Dock texts | "Private" pill, mask + "Search privately", private tab count **mist in** |
| 300 → 450 | Cards in | private stack: translateY +28 → 0dp, scale 0.98 → 1, alpha 0 → 1 |
| 300 → 760 | Glow | alpha 0 → 1, scale 0.6 → 1 from the top centre, `seg(t, 300, 760)` |
| 320 → 740 | Stars | star k (0 … 55, random order) starts at `320 + 4k` ms, lasts 200ms; f = linear progress: alpha 0 → its value × f, scale `0.4 + 0.6f + 0.45 sin(πf)` (peaks ≈ 1.25, settles at 1) |
| 340 → 860* | Sparkles | sparkle k (0 … 6) starts at `340 + 50k` ms, lasts 220ms: rotate 45° → 0, scale `0.3 + 0.7f + 0.5 sin(πf)`, alpha × f |

\*The last sparkle finishes about 100ms after the driver ends. Run sparkles on their own short animations, triggered by the driver.

Board frames: 0, 190, 380, 570, 760ms (0 / 25 / 50 / 75 / 100%).

### 6.2 Off (Private → normal)

The same chain with the roles swapped, except that the atmosphere leaves first.

| ms | Part | What happens |
|---|---|---|
| 0 | Haptic | `CLOCK_TICK` |
| 0 → 200 | Knob | x 27 → 3dp |
| 0 → 450 | Colours | private → normal |
| 0 → 450 | Gradient | alpha 1 → 0 |
| 0 → 360 | Glow | alpha 1 → 0, scale 1 → 0.6 |
| 0 → 315 | Stars | star k leaves at `3 × (55 − k)` ms, 150ms each: reverse order, alpha → 0, scale 1 → ~1.25 → 0.4 |
| 0 → 330 | Sparkles | sparkle k leaves at `30 × (6 − k)` ms, 150ms each |
| 150 → 270 | Title, dock texts | "Private", "Not saved", the "Private" pill and "Search privately" **mist out** |
| 150 → 300 | Cards out | private stack sinks +28dp, scale → 0.96, alpha → 0 |
| 270 → 420 | Title, dock texts | The space name + icon box, the switcher and "Search or type URL" **mist in** |
| 300 → 450 | Cards in | normal stack rises +28 → 0dp, scale 0.98 → 1, alpha 0 → 1 |

### 6.3 Other motion
- **Reduced motion** (`ANIMATOR_DURATION_SCALE` = 0, or the system "remove animations" setting): colours, title, dock texts and stacks crossfade in 150ms, sequenced (out 0–75, in 75–150). Knob jumps. The atmosphere appears and disappears with no twinkle.
- **Closing the last private tab:** the card flies off as usual, then the empty state fades in over 160ms. The atmosphere stays.
- **Opening a private card:** the usual card → page transition (`Motion.Overview` 1.0 / 600); the dock is already private.

---

## 7. Icons

New KoanIcons entry **`icon-private`** (24 grid, 2dp stroke, round caps and joins), a domino mask with two eye slits:

```
M2.5 9.2 C2.5 7.3 4.3 6 6.8 6 C9 6 10.4 7.2 12 7.2 C13.6 7.2 15 6 17.2 6 C19.7 6 21.5 7.3 21.5 9.2 C21.5 13.4 19.4 17 16.4 17 C14.4 17 13.4 14.6 12 14.6 C10.6 14.6 9.6 17 7.6 17 C4.6 17 2.5 13.4 2.5 9.2 Z
M6.2 11 C6.9 10 8.5 10 9.3 11 C8.5 11.9 6.9 11.9 6.2 11 Z
M14.7 11 C15.5 10 17.1 10 17.8 11 C17.1 11.9 15.5 11.9 14.7 11 Z
```

Used at 17 (toggle knob), 18 (address pill), 20 (dock pill, notice) and 36 (empty state).

---

## 8. States checklist

| Screen | Normal light | Normal dark | Private Dusk | Private Night |
|---|---|---|---|---|
| Overview with tabs | §2, §3 | §1, §2, §3 | §4.3, §4.4, §5 | same |
| Overview, sync failed | amber dot | amber dot | — (Private never syncs) | — |
| Overview, empty | as today | as today | §4.7 | §4.7 |
| Page view | as today | §1 | §4.5 | §4.5 |
| Search sheet | as today | §1 | §4.6 | §4.6 |
| Settings, History, Downloads, ⋯ sheet | as today | §1 (wash 0.16) | normal palette (not private) | normal palette |
| Toggle | off | off | on | on |
| Transition | — | — | §6 | §6 |

Removed and not to be built: space-colour glow, space chip outline, focused-card edge colour, tinted icon box, pill gradient, address-pill ring, the "Space colours" setting, the overview meta line, and a Private entry in the space switcher.
