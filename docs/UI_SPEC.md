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
│ ‹   Work            ⌕   +    │  header: space icon + name, search-in-space, new tab
│ ┌──┐┌──┐┌──┐┌──┐             │  essentials grid (favicons, 4 per row, max 2 rows)
│ └──┘└──┘└──┘└──┘             │
│ 📌 Notion · Planning          │  pinned tabs: compact rows (collapse on scroll, see below)
│ 📌 Linear · Sprint board      │
│ ╭──────────────────────────╮ │
│ │ ▣ Zenith OS           ✕  │ │  card (header row always visible)
│ │                          │ │
│ ╭──────────────────────────╮ │  next card overlaps the previous one
│ │ ▣ Tailwind CSS        ✕  │ │
│ │      (thumbnail)         │ │
│ │                          │ │
│ ╰──────────────────────────╯ │
│  ●  Personal · WORK · Uni  ● │  workspace bar (swipe left/right)
└──────────────────────────────┘
```

- Background = the space's Zen theme gradient (`theme.gradientColors`), darkened for legibility.
  Default if no theme: near-black.
- Essentials grid: favicon tiles. Tap = open, long-press = context menu.
- **Pinned tabs are not cards.** They are compact Arc-style rows (favicon + title, 44dp each,
  max 5 visible, then "+N more") directly under the essentials.
- Below them, the card stack: folders (a labelled group that can collapse to a single "deck"
  card), then normal tabs, most recent last (bottom, closest to the thumb).
- **Live pinned tabs also get a card.** When a pinned tab (row) is opened on the phone, it gets a
  card in the stack while it's live, marked with a small pin glyph next to its title. Its row stays
  in the pinned list. Closing that card (✕ or swipe) only **unloads** it back to its row: the card
  disappears, the engine session is dropped, but the tab is never deleted or unpinned (and in M6
  never tombstoned or written as unpinned). Normal tabs are synced from Zen ("sync normal tabs" is
  on), so desktop normal tabs appear as regular cards.

### Collapsing top area

- **Default state when opening the overview: half-collapsed and centred on the current tab**
  (compact header bar, essentials and pinned hidden, current tab on the focus line). Pulling down
  **inside the card stack** reveals essentials and pinned; swiping down **on the compact header**
  closes the overview back to the current tab.

- Header + essentials + pinned rows form one collapsing block (iOS large-title behaviour).
- Scrolling the stack up first collapses this block: essentials and pinned rows fade and slide up
  (alpha 1 → 0, translateY −24dp, scale 0.96), then the header shrinks to a compact 52dp bar with
  the space name, search and +. Only after that do the cards themselves scroll.
- Scrolling back down to the top, or pulling down past the top, reveals the block again with a
  spring. Collapse progress is driven by the scroll offset (1:1), not a timed animation.

## Cards

- Width: screen width − 2×12dp. Corner radius 28dp (continuous/squircle-like if possible).
- Card height: see "Depth stack" below (a fraction of the stack viewport). How much of each card
  is visible depends on its depth.
- Thumbnail fills the card, top-aligned, cropped (not letterboxed). Header and placeholder rules:
  see "Visual restraint".
- The currently active tab's card is marked by a small accent dot next to its title, not a stroke.

### Depth stack (the "premium" feel) — most important visual rule

Source: v3 "Upright depth stack" in `docs/reference/design/Koan Screens.dc.html`
(`v3-perspective-stack.png`). All numbers below live in `ui/theme/Tokens.kt` (`StackTokens`).

The stack is a **one-point perspective deck**: cards behind the focused one converge toward a
vanishing point above it. Cards are **always upright** (no tilt). Every card's transform is a pure
function of its distance `d` (in card slots, fractional) from the focus: `d = 0` focused, `d > 0`
behind (in the pile above), `d < 0` in front (the peek below, then gone).

- **Focus:** the focused card is big and dominant, upright, scale 1.0, around the middle of the stack
  area. Height = 58% of the stack viewport (design 64%, made ~9% shorter so the pile has room).
  Its bottom edge sits `PeekGap` (14dp) above the peeking card.
- **Pile (d > 0):** with ρ = rᵈ, r = 0.55:
  `offsetY(d) = −A·(1 − ρ)` (top edge rises; A ≈ 176dp, clamped so the pile never reaches the
  header) and `scale(d) = sMin + (1 − sMin)·ρ`, sMin = 0.62 → .83 · .73 · .68 · .65.
  Spacing and size shrink geometrically. Thumbnail darkness (brightness 1 → .84 → .72 → .62 …) and
  blur (σ 0, 0, .5, 1, 1.6, 2.2, 2.8 dp) increase with depth; cards fade out from d ≈ 5 and are gone
  by d = 8.
- **Peek (d = −1):** only the next card, upright at scale 0.94, its header strip visible at the
  bottom of the viewport (`PeekVisible` 80dp shows). Cards past the peek fade out within a third of
  a slot.
- **Scrolling is continuous along that curve:** the stack moves 1:1 with the finger (230dp per
  slot); the next card rises from the peek and grows into focus, the current one shrinks and slides
  up into the pile. On release the stack **snaps so a card always settles in focus**: a release that
  moved more than 0.15 slot (fling carry included) commits to the next card in that direction,
  otherwise it springs back.
- The focused card gets a soft ambient shadow; cards behind get progressively less. Depth comes from
  position, scale, brightness and blur, never from borders or tilt.
- Rubber-band overscroll at both ends; at the top, overscroll pulls the stack down before revealing
  the collapsed header block.
- **Only the thumbnail layer** gets darkened/blurred. The header strip (favicon + title) stays
  legible deep in the pile (dimmed to 80% at most), so you always know what's in the stack.
- All of this lives in `Modifier.graphicsLayer { }` reading the scroll state — zero recomposition
  per frame. If live blur ever drops frames on the POCO, switch to the pre-blurred thumbnail
  fallback (`ARCHITECTURE.md`, "Blur strategy").

### Visual restraint (avoid the "AI-generated" look)

- No random saturated gradients on cards. A card shows the **real page thumbnail**. Placeholder for
  never-opened Zen tabs: dark neutral surface `#18181B` with a large centered favicon and the
  domain in secondary text, plus a very faint tint (8%) of the favicon colour. Nothing else.
- Card header: a frosted strip over the thumbnail (black 35% + blur), favicon 22dp, title 15sp
  medium, ✕ only on the focused card (others show it on touch). No colored header bars.
- No outlines/strokes on cards except the 1dp `white @ 10%` hairline at the top edge for light
  catch. Corner radius 28dp, continuous.
- The space gradient is only the page background, desaturated and dark; cards stay neutral so the
  thumbnails are the color.

### The stack never goes behind the bottom bar

- The stack viewport ends **12dp above the top of the workspace bar**. Cards are clipped to this
  viewport; the bar sits in its own reserved zone and nothing scrolls underneath it.
- The bottom 24dp of the viewport has a fade mask (content alpha 1 → 0 toward the bar) so cards
  leaving downward dissolve before the bar instead of being cut sharply. It is shorter than the
  peek so the peeking card's header stays crisp.
- The bar's zone background is the solid dark base (gradient bg ends above it), so the bar reads
  as a dock, not as glass floating over content.

## Interactions and gesture ownership (important)

| Gesture | Where | Result |
|---|---|---|
| Vertical drag | card area | scroll the stack |
| Horizontal swipe on a card | card | swipe-to-close (card follows finger, fades, flies off; others close the gap with a spring) |
| Horizontal swipe | **workspace bar only** | switch space |
| Tap card | card | open tab (zoom morph) |
| Long-press card | card | lift (scale 1.03 + shadow), then drag to reorder; drop on the workspace bar to move tab to another space |
| Tap ✕ | card | close (same animation as swipe-close, upward collapse) |
| Tap space name in header | header | space menu (rename, icon, theme, new folder) |
| Swipe down on overview header | header | dismiss overview back to the current tab |

Rule: **horizontal swipes in the card area never switch spaces.** Switching spaces only happens on
the workspace bar. This avoids the close-vs-switch gesture conflict.

## Bottom bar / workspace bar

One bar serves both screens (Arc Search–sized, easy to hit with the thumb). Values in
`Tokens.kt` (`BarTokens`, `PagerTokens`).

- **Page view:** floating rounded bar, 68dp tall, 10dp from the sides and bottom (above the gesture
  nav inset): back · big address field (50dp, 17sp, domain only, load progress inside) · tabs ·
  menu. Swipe up on it (or tap tabs) opens the overview.
- **Overview:** the same bar morphs into the workspace bar. Two styles exist while the owner
  compares them (dev menu toggle), one will be kept:
  - **Grow** (default): the bar grows upward; the workspace switcher row appears on top, the
    address field stays underneath (tap = new tab with search). Back and menu fade out.
  - **Replace:** same size; the address row crossfades into the workspace switcher.
  The morph only draws/transforms (no relayout per frame).
- Nothing scrolls underneath the bar: each screen ends 12dp above it.
- The switcher shows the current space name centered in bold, neighbours faded on both sides, plus
  page dots.
- **Switching is a simple pager**, driven only by horizontal drags on the switcher:
  - The whole space follows the finger 1:1 (header, essentials, pinned and stack move as one unit;
    the stack trails slightly with parallax 0.85× and scale 0.96 → 1).
  - The background gradient interpolates between the two spaces' gradients by drag fraction.
  - Release: dragged past **25% of the width**, or flung in the drag direction, commits to the
    neighbour; otherwise it springs back.
  - **Exactly one light haptic tick per committed switch**, on release. Never while dragging.
- Tap a neighbour name = jump to it (one tick). Long-press bar = grid of all spaces (pick, reorder,
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

## Visual tokens (starting point, tweak freely)

- Dark-first UI. Surfaces: `#0E0E10` base, glass surfaces `white @ 8%` with 24dp blur where
  supported (Android 12+ RenderEffect), fallback to `#1A1A1E @ 92%`.
- Text: primary `#FFFFFF`, secondary `white @ 62%`.
- Accent = primary color of the current space's gradient.
- Typography: one geometric sans (e.g. Inter or Google Sans Flex if bundled); titles 20sp bold,
  card titles 16sp semibold, body 14sp.
- Radii: cards 28dp, pills full, sheets 32dp top.
- Motion: springs everywhere; avoid linear/tween except for alpha. Respect the system
  "remove animations" setting (snap instead).
- Haptics: one light tick per committed space change, medium on card lift, confirm on close.
