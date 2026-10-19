# UI Spec

Inspiration: Arc for iPhone's space view (stacked overlapping cards with a header row each, a
floating "Return to Homepage" pill at the bottom). Visual details come from design mockups kept
outside the repo; the interaction rules below always apply.

## Screens

1. **Browser (page view).** Full-screen web page. Bottom bar (see "Bottom bar / workspace bar"): back, big
   address field (domain only, tap to edit), tabs button (shows tab count), menu. Toolbar hides on scroll down, shows on
   scroll up (GeckoView dynamic toolbar).
2. **Space overview** (the core screen). Shown when tapping the tabs button or swiping up on the
   toolbar.
3. **Address/search sheet.** Opens from the address pill: input at the bottom (above the keyboard),
   suggestions above it (history, bookmarks, open tabs in any space, search suggestions).
4. **Glance sheet** (M7). Floating rounded sheet over the current page.
5. **Start page.** When Koan comes back after being unused a while (Settings: 30 min / 1 h / 4 h /
   Never, default 1 h), it opens on the current space's essentials grid, a "Continue" card for the
   last tab (favicon, title, domain; tap = back to it; hidden if that tab is gone) and the dock's
   search field focused, instead of the last tab. Not a tab: one only exists once a search or an essential
   opens something. Back, swipe down or the field's ✕ return to the last tab, leaving nothing
   behind. Never shown when a link or a share from another app brought Koan up.
6. **Settings.** Plain list: account, sync, search engine, write-back toggle, add-ons, about.
7. **Sign-in.** Mozilla accounts web login (see `docs/SYNC.md`).

## Space overview layout (top → bottom)

```
┌──────────────────────────────┐
│ ‹  💼 Work                   │  compact header: back, space icon + name
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
│ 🕘 [ Search or type URL ] ⧉ ⚙│  address row         ┘
└──────────────────────────────┘
```

- Background: see "Themes". Plain neutral in both themes (light `#F5F5F7`, dark `#1C1C1E`): no
  space-colour wash anywhere.
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
  at its top. The bar grows upward into a sheet (28dp top corners): essentials grid on top (favicon
  tiles, 4 per row, as many rows as Zen sends: no Koan-side cap), pinned rows and Zen folders below (Arc-style compact rows), the
  sync status row last; the switcher and address row stay at the bottom. Tap = open the tab (closes
  drawer and overview).
- **A sheet with detents** (iOS / Google Maps style), sized to the current space's content:
  - **Compact:** fits its content, capped so the whole dock (sheet + bar) takes at most ~55% of the
    screen. **Never scrolls** in this state, so a swipe down always moves the sheet. Taller content
    is cut off at the bar.
  - **Full:** only when the content is taller than compact: swipe up again and the sheet grows to
    nearly full screen (12dp below the status bar). If the content is still longer than that, the
    list scrolls, and only here.
  - **Nested scroll:** in full, a drag up first grows the sheet, then scrolls the list; a drag down
    first scrolls the list back to its top, then collapses the sheet, all in one gesture. A fling of
    a scrolled list keeps scrolling it (decay); at the top, a fling down collapses the sheet.
  - Switching spaces with the drawer open slides its content with the space; once the switch lands
    the sheet springs to the new space's height for the same detent.
- The sheet follows the finger 1:1 and settles on a detent with a spring: a fling goes to the next
  detent that way; a slow release commits to the neighbouring detent past 25% of the gap.
- Behind it the overview dims (light: `rgba(28,28,40,.16)`, dark: black 45%) and blurs (up to
  14dp), both driven by the sheet's height up to the compact detent.
- **Close:** swipe down (full → compact → closed), tap outside it, tap the handle, or Back.

### One sheet everywhere (`ui/sheet`)
Every sheet shares the drawer's detent engine (`SheetState`): same detents, 1:1 drag, 25% commit,
fling, nested scroll only in full, same spring. Up = expand, down = compact then close.
- **Sheet over the page** (the ⋯ menu, design 7k / 7l): rises from the bottom edge, 32dp top
  corners, compact = 55% of the screen (the drawer's compact height), the page dimmed
  (light `rgba(28,28,40,.30)`, dark black 50%), no blur (the page is a live surface).
- **Pushed pages** (Settings, later Site settings): slide in from the right at the sheet's height,
  the page under trails behind (30%); ‹ in the header or Back pops, then Back closes.
- **Floating card** (confirmations, permission prompts, the link menu): sized to its content, 10dp
  side and bottom margins, the page dimmed; drag down, tap outside or Back closes it. Never the
  brand gradient on prompts.
- **Find in page** takes the address row's place in the dock (query, "2/7", ↑ ↓ ✕); the dock
  stays shown while it is open; Back or ✕ closes it.
- **Long press** on a link or an image opens the link menu on a floating card: open in new tab,
  open in background (next to the current card), copy link, share link.
- **Settings** (design 7n / 7o) is a full-screen page sliding up over everything, its own pages
  pushed from the right like a sheet's; opened from the menu's Settings row.

## Cards

- Width: screen width − 2×12dp. Corner radius 28dp (continuous/squircle-like if possible).
- Card height: see "Depth stack" below (a fraction of the stack viewport). How much of each card
  is visible depends on its depth.
- Thumbnail fills the card, top-aligned, cropped (not letterboxed). Header and placeholder rules:
  see "Visual restraint".
- The currently active tab's card is marked by a small **neutral dot** (6dp, secondary text
  colour) next to its title, not a stroke. Same marker on the live pinned row in the drawer. The
  current tab is a state, not a moment: never the brand gradient.

### Depth stack (the "premium" feel) — most important visual rule

Source: the v3 "Upright depth stack" mockup, geometry retuned by hand on the phone. All numbers
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
  towards the neutral background (`#F5F5F7`) instead (1.25× the dim, at most 85%).
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

### The dock covers the bottom of the stack

- The stack's layout ends **at the dock's top edge**: the peeking card's header sits just above it.
  The viewport runs on 24dp under the dock (its rounded top corners), so cards show right up to
  the dock's edge and the opaque dock simply covers them. No fade band, no empty strip between
  the cards and the dock. (Still never a translucent bar with cards visible through it, v1.)
- The bar is **the dock**: its own opaque surface (light `#FFFFFF`, dark `#252527`) with a soft
  upward shadow (28dp; light `rgba(60,40,30,.07)`, dark black 35%) attached to its top edge, no
  hairline between them. The same shadow falls on the page (page view) and on the cards
  (overview). Square under a page, 24dp top corners in the overview, 28dp as the drawer.

## Interactions and gesture ownership (important)

### The bottom bar: one gesture model everywhere

**Swipe UP = one level deeper, swipe DOWN = one level back.**

```
Page  ⇄  Overview  ⇄  Drawer (compact)  ⇄  Drawer (full)
     up →          up →                 up →
     ← down        ← down               ← down
```

- One swipe = one step. Everything follows the finger 1:1 and settles with a spring; a step
  commits past ~25% of its travel or on a fling, otherwise it springs back.
- **One light haptic tick per committed step**, on release (never while dragging).
- The axis is picked once the finger passes the touch slop: vertical = levels, horizontal = tabs
  (page view) or spaces (overview, or page view in space mode).

| Where | Gesture | Result |
|---|---|---|
| **Page view** (the dock) | Swipe up | Overview opens under the finger (morph follows the drag) |
| | Swipe down | Nothing (already the top level) |
| | Horizontal swipe on the address row | Previous / next **tab** in the current space (Safari-style): the page and the pill follow the finger, the neighbour tab's screenshot slides in beside it. Commit past 25% of the width or a fling. First/last tab: rubber band, no wrap |
| | Long-press the address row | **Space mode** (haptic tick): the pill shows the current space's icon and name |
| | …then swipe horizontally | Switch **space**, the neighbour space's icon and name slide into the pill; landing opens that space's current tab (or its overview if it has none). Lifting without a swipe leaves space mode. Steps fit the screen: the way to each edge (minus 48 dp) is shared by the spaces on that side, 24–90 dp each |
| | Tap the pill | Edit the address (as before) |
| **Overview**, drawer closed | Swipe up on the dock | Drawer opens (compact) |
| | Swipe down on the dock | Back to the page of the current tab (morph follows the drag) |
| | Horizontal swipe on the dock | Switch space (pager, as before) |
| | Tap the handle | Toggle the drawer |
| **Drawer** (sheet or dock) | Swipe up | Compact → full (only when the content is taller than compact); in full, scrolls a list longer than the screen |
| | Swipe down | Full → compact → closed (in full, a scrolled list first scrolls back to its top) |
| | Horizontal swipe | Switch space |
| | Tap outside / Back / handle | Close |

### Card area (overview)

| Gesture | Where | Result |
|---|---|---|
| Vertical drag | card area | scroll the stack |
| Horizontal swipe | card area | swipe-to-close **the focused card**: follows the finger with a slight tilt and fade; past 100dp it flies off (280ms, 8°); the card in front rises into focus |
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
  only) · tab count · menu. Swipe up on the bar (or tap the tab count) opens the overview;
  sideways switches tab; long-press then sideways switches space (see the gesture model above).
  Long-press the tab count (haptic) opens a small floating card: New tab · Close tab (Undo toast).
- **Loading line** (design 4d, 7g): a 2dp line in the **brand gradient** on the dock's top edge,
  from the left, with a soft glow. It eases towards the engine's progress (800ms, never backwards), a
  lighter streak sweeps along it while it waits, and on completion it runs to the end and fades
  out (350ms). Page view only.
- **Overview:** the same strip grows upward; the workspace switcher row (with the drawer handle)
  appears on top, the address row keeps its layout and only the buttons change: back crossfades
  to **History**, the field becomes "Search or type URL" (tap = new tab with search), the tab
  count stays (tap = back to the page), the menu crossfades to **Settings**. From here it can grow further into the drawer (see "Essentials & pinned drawer").
  The morph only draws/transforms (no relayout per frame).
- The dock covers the bottom of the stack (see "The dock covers the bottom of the stack").
- The switcher is Zen's icon row (design 4a): one icon per space (initial if it has none), the
  active one in a pill tinted with its colour (accent at 14% light / 18% dark) showing its name in
  the space's ink colour. Inactive icons: 44dp touch targets, 70% opacity. The pill follows the
  drag continuously (positions, pill width and fades are layer/draw-only). Centred when it fits;
  with too many spaces the active pill stays centred.
- **Switching is a simple pager**, driven by horizontal drags anywhere on the bar:
  - The whole space follows the finger 1:1 (header and stack move as one unit; the stack trails
    slightly with parallax 0.85× and scale 0.96 → 1).
  - The switcher pill interpolates between the two spaces by drag fraction.
  - Release: dragged past **25% of the width**, or flung in the drag direction, commits to the
    neighbour; otherwise it springs back.
  - **Exactly one light haptic tick per committed switch**, on release. Never while dragging.
- Tap a space icon = jump to it (one tick). Long-press bar = grid of all spaces (pick, reorder,
  create) — later.
- In the page view, space mode (long-press the address row, then swipe) switches space: the host
  gives way to the space's icon and name, the target space's slide in; landing opens that space's
  current (else newest) tab, or its overview with an empty state (message + big "New tab" button)
  if it has no cards. A plain horizontal swipe there switches **tab** instead.

## Open / close transition (the hero animation)

- **Open:** tapped card becomes the page. Shared-element morph: card bounds → full screen,
  corner radius 28dp → 0 (or device corner radius), header row fades out during the first 40%,
  other cards drift down and fade slightly, the toolbar slides up from the bottom.
  Use a spring (no fixed duration): damping ratio ≈ 0.85, stiffness ≈ 380. Should feel ~350ms.
- **Close (back to overview):** reverse morph into the card's slot; the card lands with a tiny
  overshoot. If the card's slot is off-screen, the stack pre-scrolls so the slot is visible first.
- During the morph, show the cached thumbnail, then swap to the live GeckoView once its first
  frame is ready (never show a blank white page during the morph).
- **New tab:** a new card grows from the address field into the stack bottom, then immediately
  morphs open with the address sheet focused.

## Themes

Source: `docs/reference/design/Koan Essentials.html` section 7 ("M5 · Browser essentials — brand
gradient"). It supersedes the theme sections of `Koan Screens.dc.html` (the "soft wash" light theme
is gone). Setting: Light / Dark / Follow system (default). Web pages follow it too
(`prefers-color-scheme`).

- **Light, fully neutral:** base `#F5F5F7`, white surfaces (cards, dock, sheets), grey grouped
  rows, black text. No space-colour wash anywhere.
- **Dark:** base `#1C1C1E`, surfaces `#2C2C2E`, dock `#252527`.
- **Space colours stay only on tiny space indicators:** space icons and the active space pill in
  the switcher (accent from the Zen gradient's first colour: hue kept, saturation ≥ 55%, lightness
  45–62% light / 62–74% dark; pill tint 14% / 18%; ink text 38% / 80%; grey or missing theme uses
  `#5B6CFF` light / `#8EA2FF` dark).

### Brand gradient

Pink → violet → blue (`#FF5FA2` → `#B46BFF` → `#4F8DFF`, `BrandTokens`, helpers in
`ui/theme/Brand.kt`). A design-language element that means **Koan is working, active, or holds
your focus**, used only for:

- the page loading line; the download completion moment; download progress bars in the list;
- the syncing indicator (a small turning gradient arc);
- toggles ON; the "@" search mode (ring + cursor);
- Settings' aurora glow, the account card outline, About, and the first-run "Connect your Zen
  Spaces" card (gradient outline).

**Never** as a passive "this is the current one" marker (the current tab is a state, not a moment), on login or password UI, permission prompts, destructive actions, body text, or behind
web content. Primary buttons are never gradient.

### Controls

- **Toggles** (`ui/kit/KoanToggle`, 50×30dp): ON = the gradient track at 90% (slightly softened)
  with a white knob and a soft shadow; OFF = `#E5E5EA` light / `#3A3A3C` dark. On a tap the knob
  springs across and the gradient flows in behind it, from the left edge up to the knob. Light
  haptic tick. Draw-only animation.
- **Buttons** (`ui/kit/Buttons`): primary = black pill with white text in light, white with black
  text in dark, 50dp tall. Destructive = red text only on a neutral row, never a red fill.
- **Search field:** docked right above the keyboard (the bar follows the IME), search glyph on the
  left, ✕ on the right. While typing: a neutral 1.5dp ring (`#B4B4BA` light / white 30% dark) and
  a plain cursor. Typing "@" first makes the brand gradient flow in from the left around both
  halves of the ring (2dp, crisp, gradient cursor) on a soft spring; deleting the "@" flows it
  back out to neutral. No glow bleeding around the field: at most a very faint halo (14%) kept
  1.5dp around the stroke. In "@" mode the ring is alive: the gradient (a sweep around the
  field's centre) turns slowly, ~48°/s, its speed drifting with two out-of-phase slow
  oscillations (±14° over 3.1s, ±10° over 4.7s), never mechanical. Only the shader moves, only
  while "@" mode is on, and not at all when the system removes animations.

## Visual tokens (starting point, tweak freely)

All values live in `ui/theme/Tokens.kt`: one `KoanPalette` per theme (colours), plus the shared
size/motion objects.

- Light: base `#F5F5F7`, cards/dock/sheets `#FFFFFF`, small surfaces `#EBEBEF`, grouped rows
  `#F2F2F5`, field `#F1F1F4`, text `#1C1C1E`, secondary/icons `#6E6E73`, hints `#8E8E93`, toggle
  OFF `#E5E5EA`, destructive `#E5342B`.
- Dark: base `#1C1C1E`, cards `#2C2C2E`, dock `#252527`, small surfaces and field `white @ 8%`,
  grouped rows `white @ 7%`, text `#FFFFFF`, secondary `white @ 62%`, icons `#98989D`, hints
  `white @ 60%`, toggle OFF `#3A3A3C`, destructive `#FF5A50`.
- Brand gradient: `#FF5FA2` → `#B46BFF` → `#4F8DFF` (see "Brand gradient").
- Typography: one geometric sans (e.g. Inter or Google Sans Flex if bundled); titles 20sp bold,
  card titles 16sp semibold, body 14sp.
- Radii: cards 28dp, pills full, sheets 32dp top.
- Motion: springs everywhere; avoid linear/tween except for alpha. Respect the system
  "remove animations" setting (snap instead).
- Haptics: one light tick per committed space change, medium on card lift, confirm on close.
