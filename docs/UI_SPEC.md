# UI Spec

Reference: `docs/reference/arc-iphone-space.png` (Arc for iPhone, "Work" space: stacked overlapping
cards with a header row each, a floating "Return to Homepage" pill at the bottom).
If a Claude Design mockup exists in `docs/reference/design/`, it overrides visual details here;
the interaction rules below still apply.

## Screens

1. **Browser (page view).** Full-screen web page. Bottom bar (see "Bottom bar / workspace bar"): back, big
   address field (domain only, tap to edit), tabs button (shows tab count), menu. Toolbar hides on scroll down, shows on
   scroll up (GeckoView dynamic toolbar).
2. **Space overview** (the core screen). Shown when tapping the tabs button or swiping up on the
   toolbar.
3. **Address/search sheet.** Opens from the address pill: input at the bottom (above the keyboard),
   suggestions above it (history, bookmarks, open tabs in any space, search suggestions).
4. **Glance sheet** (M7). Floating rounded sheet over the current page.
5. **Settings.** Plain list: account, sync, search engine, write-back toggle, add-ons, about.
6. **Sign-in.** Mozilla accounts web login (see `docs/SYNC.md`).

## Space overview layout (top → bottom)

```
┌──────────────────────────────┐
│ ‹  💼 Work                +  │  compact header: back, space icon + name, new tab
│      ╭────────────────╮      │  pile: back cards converge upward
│    ╭──────────────────────╮  │
│ ╭──────────────────────────╮ │  focused card (big, upright)
│ │ ▣ Zenith OS           ✕  │ │
│ │      (thumbnail)         │ │
│ ╰──────────────────────────╯ │
│  ╭────────────────────────╮  │  peek: next card's header
╭──────────────────────────────╮  the dock (own surface, 24dp top corners)
│            ▬▬                │  drawer handle
│     🏡  ( 💼 Work )  🎓       │  workspace switcher  ┐ the bottom bar
│     [ Search or type URL ] ⧉ │  address row         ┘
└──────────────────────────────┘
```

- Background: see "Themes". Light = near-white with a soft wash of the space colour at the top;
  Dark = plain dark grey. The wash crossfades with the drag when switching spaces.
- Essentials and pinned tabs live in the **drawer** (see below), not in the overview itself.
- The card stack: folders (a labelled group that can collapse to a single "deck"
  card), then normal tabs, most recent last (bottom, closest to the thumb).
- **Live pinned tabs also get a card.** When a pinned tab (row) is opened on the phone, it gets a
  card in the stack while it's live, marked with a small pin glyph next to its title. Its row stays
  in the pinned list. Closing that card (✕ or swipe) only **unloads** it back to its row: the card
  disappears, the engine session is dropped, but the tab is never deleted or unpinned (and in M6
  never tombstoned or written as unpinned). Normal tabs are synced from Zen ("sync normal tabs" is
  on), so desktop normal tabs appear as regular cards.

### Essentials & pinned drawer

Replaces the old collapsing essentials/pinned block: reaching pinned tabs must never need a scroll
to the top of the stack.

- **Default state when opening the overview:** compact header + the stack centred on the current
  tab. Swiping down **on the compact header** closes the overview back to the current tab.
- **Open:** in the overview, swipe **up** on the bottom bar (anywhere on it) or tap the small handle
  at its top. The bar grows upward into a sheet (≈ half the screen, sized to the tallest space's
  content, 28dp top corners): essentials grid on top (favicon tiles, 4 per row, max 2 rows), pinned
  rows below (Arc-style compact rows, max 5 visible, then "+N more"), the switcher and address row
  stay at the bottom. Tap = open the tab (closes drawer and overview).
- The drawer follows the finger 1:1 and settles with a spring (position past half, or a fling,
  decides open/closed).
- Behind it the overview dims (light: `rgba(28,28,40,.16)`, dark: black 45%) and blurs (up to
  14dp), both driven by the drawer's progress.
- **Close:** swipe down on the sheet or the bar, tap outside it, tap the handle, or Back.
- On the bar, horizontal drags still switch spaces and vertical drags drive the drawer (axis picked
  after the touch slop). Switching spaces with the drawer open slides its content with the space.

## Cards

- Width: screen width − 2×12dp. Corner radius 28dp (continuous/squircle-like if possible).
- Card height: see "Depth stack" below (a fraction of the stack viewport). How much of each card
  is visible depends on its depth.
- Thumbnail fills the card, top-aligned, cropped (not letterboxed). Header and placeholder rules:
  see "Visual restraint".
- The currently active tab's card is marked by a small dot in the space's accent next to its title,
  not a stroke.

### Depth stack (the "premium" feel) — most important visual rule

Source: v3 "Upright depth stack" in `docs/reference/design/Koan Screens.dc.html`
(`v3-perspective-stack.png`), geometry retuned to the owner's `sketch-stack.png`. All numbers
below live in `ui/theme/Tokens.kt` (`StackTokens`).

The stack is a **one-point perspective deck**: cards behind the focused one converge toward a
vanishing point just above it. Cards are **always upright** (no tilt). Every card's transform is a
pure function of its distance `d` (in card slots, fractional) from the focus: `d = 0` focused,
`d > 0` behind (in the pile above), `d < 0` in front (the card below, then gone).

- **Vertical layout (top → bottom):** ~28dp of breathing room under the header (`PileTopGap`
  22dp + the pile's fade), the pile, the focused card, then the card in front peeking 60dp above
  the viewport bottom (just above the bar). The focused card gets all the remaining height (≈ 72%
  of the viewport on the POCO): it's clearly THE card on screen.
- **Pile (d > 0):** bunched tightly just above the focused card's top, converging fast:
  `offsetY(d) = −A·(1 − rᵈ)` with r = 0.45 and A = 28% of the focused card's height (first gap
  ≈ 15% of it); `scale(d) = sMin + (1 − sMin)·r_sᵈ`
  with sMin = 0.58, r_s = 0.52 → .80 · .69 · .64 · .61. Thumbnail darkness (brightness 1 → .84 →
  .72 → .62 …) and blur (σ 0, 0, .5, 1, 1.6, 2.2, 2.8 dp) increase with depth, keyframes joined
  linearly; cards fade out from d ≈ 5 and are gone by d = 8. In Light the "darkness" is a haze
  towards the space's wash colour instead (1.25× the dim, at most 85%), like the design.
- **Card in front (d = −1):** closer to the viewer, so slightly **bigger** (scale 1.05) and drawn
  over the focused card: it overlaps the focused card's bottom by 40dp, no gap between them. Only
  its header strip and a hint of thumbnail show (60dp); the bottom fade is 16dp so it stays crisp. Cards past it fade out
  within a third of a slot.
- **Scrolling is continuous along that curve:** a drag moves the stack 1:1 with the finger (170dp
  per card); the next card rises from the front and settles into focus, the current one shrinks and
  slides up into the pile.
- **Release:** a card always settles in focus.
  - Slow release (< 2 cards/s): prototype rule — moved more than 0.15 card → the next whole card
    in that direction, otherwise glide back (exponential glide, ≈ 90ms time constant).
  - Fling: **momentum scroll with friction** — it travels (velocity − 2.2) × 0.55 cards, velocity
    in cards/s capped at 36 (at least one card): gentle swipe ≈ 1 card, normal flick ≈ 2–4, strong
    fling 10+. It lands exactly on a card with a cubic ease-out that starts at the finger's speed
    (0.3–2.2s). Touching the stack during a fling stops it (that touch never opens a card).
- Every card keeps the same shadow (prototype): it is what layers the pile. Depth comes from
  position, scale, brightness and blur, never from borders or tilt. Per theme: dark 20dp black
  elevation; light a light 8dp one. On top, each card casts a **contact shadow** on the card
  behind it: a 14dp gradient just above its top edge (light: 10% ink, dark: 22% black), shorter
  where only a sliver of the card behind shows, fading as the card is swiped away. Never a big grey
  fog over the bottom of the focused card (light-mode rule from the owner).
- Rubber-band overscroll at both ends (the prototype clamps hard; Android users expect the stretch).
- **Only the thumbnail layer** gets darkened/blurred. The header strip (favicon + title) stays
  legible deep in the pile (dimmed to 80% at most), so you always know what's in the stack.
- All of this lives in `Modifier.graphicsLayer { }` reading the scroll state — zero recomposition
  per frame. If live blur ever drops frames on the POCO, switch to the pre-blurred thumbnail
  fallback (`ARCHITECTURE.md`, "Blur strategy").

### Visual restraint (avoid the "AI-generated" look)

- No random saturated gradients on cards. A card shows the **real page thumbnail**. Placeholder for
  never-opened Zen tabs: the neutral card surface (light `#FFFFFF`, dark `#2C2C2E`) with a large
  centered favicon and the domain in secondary text, plus a very faint tint (8%) of the favicon
  colour. Nothing else.
- Card header: a strip over the thumbnail in the card colour (light white 94%, dark `#2C2C2E`
  95%), favicon 22dp, title 15sp medium, ✕ only on the focused card. No colored header bars.
- One 1dp outline all around each card (light `#E4E4EA`, dark white 8%), nothing else. Corner
  radius 28dp.
- The space colour is only a small accent (see "Themes"); cards stay neutral so the thumbnails are
  the color.

### The stack never goes behind the bottom bar

- The stack viewport ends **12dp above the top of the workspace bar**. Cards are clipped to this
  viewport; the bar sits in its own reserved zone and nothing scrolls underneath it.
- The bottom 24dp of the viewport has a fade mask (content alpha 1 → 0 toward the bar) so cards
  leaving downward dissolve before the bar instead of being cut sharply. It is shorter than the
  peek so the peeking card's header stays crisp.
- The bar is **the dock**: its own surface (light `#FFFFFF`, dark `#252527`) with a 1dp hairline
  on top (light `#ECECF0`, dark white 8%) and a soft upward shadow (28dp; light `rgba(60,40,30,.07)`,
  dark black 35%). Square under a page, 24dp top corners in the overview, 28dp as the drawer. The
  stack fades out into the background just above it.

## Interactions and gesture ownership (important)

| Gesture | Where | Result |
|---|---|---|
| Vertical drag | card area | scroll the stack |
| Horizontal swipe | card area | swipe-to-close **the focused card** (prototype): follows the finger with a slight tilt and fade; past 100dp it flies off (280ms, 8°); the card in front rises into focus |
| Horizontal swipe | **bottom bar only** | switch space |
| Vertical swipe | bottom bar (overview) | open / close the essentials & pinned drawer |
| Tap focused card | card | open tab (zoom morph) |
| Tap a pile or peek card | card | bring it into focus |
| Long-press card | card | lift (scale 1.03 + shadow), then drag to reorder; drop on the workspace bar to move tab to another space |
| Tap ✕ | focused card | close (same fly-off as swipe-close, to the right) |
| Tap space name in header | header | space menu (rename, icon, theme, new folder) |
| Swipe down on overview header | header | dismiss overview back to the current tab |

Rule: **horizontal swipes in the card area never switch spaces.** Switching spaces only happens on
the bottom bar. This avoids the close-vs-switch gesture conflict.

## Bottom bar / workspace bar

One bar serves both screens (Arc Search–sized, easy to hit with the thumb). Values in
`Tokens.kt` (`BarTokens`, `DrawerTokens`, `PagerTokens`).

- **Page view:** the dock, full width, square: back · big address field (50dp, 17sp, domain
  only) · tab count · menu. Swipe up on the bar (or tap the tab count) opens the overview.
- **Loading line** (design 4d): a 2dp gradient line on the dock's top edge, from the left, in the
  space's gradient colours (pink → violet → blue when the space has fewer than two usable
  colours), with a soft glow. It eases towards the engine's progress (800ms, never backwards), a
  lighter streak sweeps along it while it waits, and on completion it runs to the end and fades
  out (350ms). Page view only.
- **Overview:** the same strip grows upward; the workspace switcher row (with the drawer handle)
  appears on top, the address field stays underneath (tap = new tab with search). Back and menu
  fade out. From here it can grow further into the drawer (see "Essentials & pinned drawer").
  The morph only draws/transforms (no relayout per frame).
- Nothing scrolls underneath the bar: the stack ends 12dp above it.
- The switcher is Zen's icon row (design 4a): one icon per space (initial if it has none), the
  active one in a pill tinted with its colour (accent at 14% light / 18% dark) showing its name in
  the space's ink colour. Inactive icons: 44dp touch targets, 70% opacity. The pill follows the
  drag continuously (positions, pill width and fades are layer/draw-only). Centred when it fits;
  with too many spaces the active pill stays centred.
- **Switching is a simple pager**, driven by horizontal drags anywhere on the bar:
  - The whole space follows the finger 1:1 (header and stack move as one unit; the stack trails
    slightly with parallax 0.85× and scale 0.96 → 1).
  - The background (light: the wash; the switcher pill) interpolates between the two spaces by drag fraction.
  - Release: dragged past **25% of the width**, or flung in the drag direction, commits to the
    neighbour; otherwise it springs back.
  - **Exactly one light haptic tick per committed switch**, on release. Never while dragging.
- Tap a space icon = jump to it (one tick). Long-press bar = grid of all spaces (pick, reorder,
  create) — later.
- In the page view, horizontal swipe on the address field switches space too — later (M3).

## Open / close transition (the hero animation)

- **Open:** tapped card becomes the page. Shared-element morph: card bounds → full screen,
  corner radius 28dp → 0 (or device corner radius), header row fades out during the first 40%,
  other cards drift down and fade slightly, the toolbar slides up from the bottom.
  Use a spring (no fixed duration): damping ratio ≈ 0.85, stiffness ≈ 380. Should feel ~350ms.
- **Close (back to overview):** reverse morph into the card's slot; the card lands with a tiny
  overshoot. If the card's slot is off-screen, the stack pre-scrolls so the slot is visible first.
- During the morph, show the cached thumbnail, then swap to the live GeckoView once its first
  frame is ready (never show a blank white page during the morph).
- **New tab (+):** a new card grows from the + button into the stack bottom, then immediately
  morphs open with the address sheet focused.

## Themes

Source: section 4 "Themes" of `docs/reference/design/Koan Screens.dc.html` (Light A "soft wash"
and Dark; Light B is not used). Setting: Light / Dark / Follow system (default), in the ⋯ menu
for now (Settings screen later). Web pages follow it too (`prefers-color-scheme`).

- **Light ("soft wash"):** background `#FFFFFF` with a wash of the space colour at the top:
  accent mixed into white at 16% at the top, 9% at 24% of the height, plain white from 50%.
- **Dark:** plain `#1C1C1E` background, cards `#2C2C2E`, dock `#252527`. No wash.
- **The space colour is only a small accent:** the loading line, the active space pill, the
  current-tab dot, the address field's focus ring and cursor, and the light wash. It is derived
  from the Zen gradient's first colour (hue kept; saturation ≥ 55%; lightness pulled into
  45–62% light, 62–74% dark; ink text 38% / 80%). A grey or missing theme uses the default accent
  (`#5B6CFF` light, `#8EA2FF` dark). It crossfades on space switch (with the drag in the overview,
  400ms elsewhere).

## Visual tokens (starting point, tweak freely)

All values live in `ui/theme/Tokens.kt`: one `KoanPalette` per theme (colours), plus the shared
size/motion objects.

- Light: base/cards/dock `#FFFFFF`, small surfaces `#F2F2F5`, field `#F1F1F4`, text `#1C1C1E`,
  secondary/icons `#6E6E73`, hints `#8E8E93`.
- Dark: base `#1C1C1E`, cards `#2C2C2E`, dock `#252527`, small surfaces and field `white @ 8%`,
  text `#FFFFFF`, secondary `white @ 62%`, icons `#98989D`, hints `white @ 60%`.
- Typography: one geometric sans (e.g. Inter or Google Sans Flex if bundled); titles 20sp bold,
  card titles 16sp semibold, body 14sp.
- Radii: cards 28dp, pills full, sheets 32dp top.
- Motion: springs everywhere; avoid linear/tween except for alpha. Respect the system
  "remove animations" setting (snap instead).
- Haptics: one light tick per committed space change, medium on card lift, confirm on close.
