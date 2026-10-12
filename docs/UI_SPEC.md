# UI Spec

Reference: `docs/reference/arc-iphone-space.png` (Arc for iPhone, "Work" space: stacked overlapping
cards with a header row each, a floating "Return to Homepage" pill at the bottom).
If a Claude Design mockup exists in `docs/reference/design/`, it overrides visual details here;
the interaction rules below still apply.

## Screens

1. **Browser (page view).** Full-screen web page. Bottom toolbar: address pill (domain only,
   tap to edit), back, tabs button (shows tab count), menu. Toolbar hides on scroll down, shows on
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
- Card height ≈ 62% of screen height. How much of each card is visible depends on its depth
  (see "Depth stack" below).
- Thumbnail fills the card, top-aligned, cropped (not letterboxed). Header and placeholder rules:
  see "Visual restraint".
- The currently active tab's card is marked by a small accent dot next to its title, not a stroke.

### Depth stack (the "premium" feel) — most important visual rule

The stack is a **3D deck receding into the distance**, not a flat list of overlapping rectangles.
Every card's transform is a pure function of its distance `d` (in card slots, fractional) from the
**focus line**, a horizontal line at ~62% of the stack viewport height. `d = 0` is the focused card,
`d > 0` are cards further back (above), `d < 0` cards in front (below, about to leave).

| d (slots behind focus) | scale | translateY spacing to next | rotationX (top edge away) | brightness | blur |
|---|---|---|---|---|---|
| 0 (focused) | 1.00 | full peek 220dp, header + big slice of thumbnail visible | 0° | 100% | 0 |
| 1 | 0.92 | 96dp | −6° | 88% | 0 |
| 2 | 0.85 | 52dp | −10° | 74% | 1dp* |
| 3 | 0.79 | 30dp | −13° | 60% | 2dp* |
| ≥ 4 | 0.74 → 0.70 | 16dp → 8dp (asymptotic) | −15° | 45% → fade to 0 | 3dp* |

\* Blur values are a floor, probably too low: the distance blur must **match v2**
(`docs/reference/design/v2-depth-stack.png`), where back cards are clearly soft. It's one of the main
things that makes the stack feel premium. Tune up visually, then measure on the POCO X4 GT (release
build). If live blur drops frames, switch to the pre-blurred thumbnail fallback
(`ARCHITECTURE.md`, "Blur strategy"). Never drop the blur without a side-by-side for the owner.

- Interpolate continuously between rows (use an easing curve on `d`, e.g. `1 - (1 - t)^2`), so
  scrolling makes cards smoothly travel *back into the distance* and shrink, and the card passing
  the focus line visibly "comes forward" to full size with a large view of its thumbnail.
- Camera distance for `rotationX`: ~ 12 × density so the perspective is subtle, not cheesy.
- Cards **in front** of the focus (d < 0) keep scale 1.0 and simply slide down out of the stack
  area (they never go behind the workspace bar, see below).
- The focused card gets a soft ambient shadow (large blur, 30% black, no hard edge); cards behind
  get progressively less shadow. Depth comes from scale, tilt, brightness and blur, never from
  borders.
- Scroll snaps softly so a card settles on the focus line after a fling (snap only on release,
  never fights the finger).
- Rubber-band overscroll at both ends; at the top, overscroll pulls the stack apart (spacing grows)
  before revealing the collapsed header block.
- **Only the thumbnail layer** gets darkened/blurred. The header strip (favicon + title) stays
  legible down to d = 3 (dimmed to 80% at most), so you always know what's in the stack.
- All of this lives in `Modifier.graphicsLayer { }` reading the scroll state — zero recomposition
  per frame.

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

### The stack never goes behind the workspace bar

- The stack viewport ends **12dp above the top of the workspace bar**. Cards are clipped to this
  viewport; the bar sits in its own reserved zone and nothing scrolls underneath it.
- The bottom 48dp of the viewport has a fade mask (content alpha 1 → 0 toward the bar) so cards
  leaving downward dissolve before the bar instead of being cut sharply.
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

## Workspace bar

- Pill, 56dp tall, 16dp from the bottom (above gesture nav inset), in its own reserved dock zone
  (see "The stack never goes behind the workspace bar"). Subtle glass (white 6%), no content
  passes underneath it.
- Shows the current space name centered in bold, neighbours' names/icons faded on both sides,
  plus page dots.
- Horizontal drag on the bar is mapped to a pager across spaces:
  - The whole card area translates horizontally with the drag (1:1), next space's stack slides in
    from the side with slight parallax (0.85× speed) and a scale of 0.96 → 1.
  - The whole space moves as one unit: header, essentials, pinned rows and the stack all slide
    together (each space has its own essentials/pinned). Space titles crossfade with a short
    slide, never overlap.
  - The background gradient interpolates between the two spaces' gradients by drag fraction.
  - Release: spring to the nearest space (fling velocity aware).
  - Light haptic tick when crossing into a new space.
- Tap a neighbour name = jump to it. Long-press bar = grid of all spaces (pick, reorder, create).
- In the page view, the address pill doubles as the bar: horizontal swipe on it switches space too
  (and opens that space's last-active tab).

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
- Haptics: light tick on space change, medium on card lift, confirm on close.
