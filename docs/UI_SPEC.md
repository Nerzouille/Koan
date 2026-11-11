# UI Spec

Inspiration: Arc for iPhone's space view (stacked overlapping cards with a header row each, a
floating "Return to Homepage" pill at the bottom). Visual details come from design mockups kept
outside the repo; the interaction rules below always apply.

## Screens

1. **Browser (page view).** Full-screen web page. Bottom bar (see "Bottom bar / workspace bar"): back, big
   address field (domain only, tap to edit), tabs button (shows tab count), menu. Toolbar hides on scroll down, shows on
   scroll up (GeckoView dynamic toolbar); a page that can't scroll keeps it. The page ends above
   the navigation bar (a solid strip below it, as Firefox). A page's fixed bottom bar (a chat
   input, a cookie banner) sits on the dock and follows it down as it hides, to the page's real
   bottom. When a field in the page has the keyboard, the dock steps aside and the page ends at
   the keyboard, the field right on it; the dock comes back with the keyboard's going.
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
6. **Settings.** Plain list: the Mozilla account card on top, then General (search engine,
   search suggestions, start page, translation, default browser), Display (theme, hide status
   bar, picture-in-picture), Privacy (passwords, uBlock Origin), Private browsing, All tabs, Koan
   (downloads, download folder, about).
   Every full page (Settings and its pages, History, Downloads) uses the one page header (see
   "Page header").
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

- Background: see "Themes". Plain neutral in both themes (light `#F5F5F7`, dark `#121214`): no
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
  brand gradient on prompts. A card with a text field rises above the keyboard.
- **What a page asks** (`ui/menu/PagePrompts`) goes on that floating card, in Koan's style:
  `<select>` (a tap picks; multiple = tick boxes + Done; optgroups as small titles, a long list
  scrolls and opens on the selected option), date / time / date-and-time (calendar, then the
  clock) / month (year + 12 months) / week (a day of the week), colour (swatches or a hex value),
  alert / confirm / prompt ("example.com says", the prompt's field focused with its text
  selected), "Leave this page?", HTTP sign-in, "wants to open a pop-up" (Allow / Block), "Send
  the form again?". Closing the card any other way is Cancel. A dialog within 3 s of the last one
  offers "Don't let this page show more dialogs": ticked, that tab's alert/confirm/prompt are
  dismissed unseen until it loads another address. A share goes to Android's share sheet, a
  folder field to Android's folder picker, files to the file picker (`FilePrompt`). Anything
  else is dismissed at once, so a page never waits on a dialog nobody shows.
- **Toasts make way:** a toast goes away when a sheet, a card or a full page (History, Settings,
  Downloads) opens.
- **Find in page** takes the address row's place in the dock (query, "2/7", ↑ ↓ ✕); the dock
  stays shown while it is open, and the page ends above it (so a match on the last line shows
  above the find bar); Back or ✕ closes it.
- **Long press** on a link, an image or a video opens the link menu on a floating card. Link: Open in
  new tab, Open in background (next to the current card), Copy link, Share link; a link straight to
  a video file adds Play video on top. Image: Save image, Open image in new tab, Copy image link
  (on a small preview, Save image says "Waiting for the full image…" and saves the full one once
  it has loaded). Video: Play video, Download video, Open video in new tab, Copy video link.
- **Settings** (design 7n / 7o) is a full-screen page sliding up over everything, its own pages
  pushed from the right like a sheet's; opened from the menu's Settings row.

## Cards

- Width: screen width − 2×14dp (`CardSideMargin`). Corner radius 28dp (continuous/squircle-like if possible).
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
  of the viewport on the Xiaomi): it's clearly THE card on screen.
- **Pile (d > 0):** bunched tightly just above the focused card's top, converging fast:
  `offsetY(d) = −A·(1 − rᵈ)` with r = 0.45 and A = 28% of the focused card's height (first gap
  ≈ 15% of it); `scale(d) = sMin + (1 − sMin)·r_sᵈ`
  with sMin = 0.58, r_s = 0.52 → .80 · .69 · .64 · .61. Thumbnail darkness (brightness 1 → .84 →
  .72 → .62 …) and blur (σ 0, 0, .5, 1, 1.6, 2.2, 2.8 dp) increase with depth, keyframes joined
  linearly; cards fade out from d ≈ 5 and are gone by d = 8. In Light the "darkness" is a haze
  towards the neutral background (`#F5F5F7`) instead (1.25× the dim, at most 60%; dark dims towards black, at most 80%).
- **Card in front (d = −1):** closer to the viewer, so slightly **bigger** (scale 1.05) and drawn
  over the focused card: it overlaps the focused card's bottom by 40dp, no gap between them. Only
  its header strip and a hint of thumbnail show (60dp); the bottom fade is 16dp so it stays crisp. Cards past it
  slide straight down out of view, opaque (no fade), and rise the same way.
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
  elevation; light 12dp. On top, each card casts a **contact shadow** on the card
  behind it: a gradient just above its top edge (light: 16dp, 18% ink; dark: 18dp, 65% black), shorter
  where only a sliver of the card behind shows, fading as the card is swiped away. Never a big grey
  fog over the bottom of the focused card (light-mode rule from the owner).
- Rubber-band overscroll at both ends (the prototype clamps hard; Android users expect the stretch).
- **Only the thumbnail layer** gets darkened/blurred. The header strip (favicon + title) stays
  legible deep in the pile (dimmed to 80% at most), so you always know what's in the stack.
- All of this lives in `Modifier.graphicsLayer { }` reading the scroll state — zero recomposition
  per frame. If live blur ever drops frames on the Xiaomi, switch to the pre-blurred thumbnail
  fallback (`ARCHITECTURE.md`, "Blur strategy").

### Visual restraint (avoid the "AI-generated" look)

- No random saturated gradients on cards. A card shows the **real page thumbnail**. Placeholder for
  never-opened Zen tabs: the neutral card surface (light `#FFFFFF`, dark `#2C2C2E`) with a large
  centered favicon and the domain in secondary text, plus a very faint tint (8%) of the favicon
  colour. Nothing else.
- Card header: a 44dp strip over the thumbnail in the card colour (light white 94%, dark
  `#2C2C2E` 95%), favicon 24dp, two lines: the title 14.5sp SemiBold over the site's host 11.5sp
  in secondary text; ✕ only on the focused card. No colored header bars.
- One 1dp hairline all around each card (`CardHairline`: light `#1C1C30` at 10%, dark white at
  12%), plus in dark a 1dp light rim along the top edge, inside (`CardRim`, white 10%). Nothing
  else. Corner radius 28dp.
- The space colour is only a small accent (see "Themes"); cards stay neutral so the thumbnails are
  the color.

### The dock covers the bottom of the stack

- The stack's layout ends **at the dock's top edge**: the peeking card's header sits just above it.
  The viewport runs on 24dp under the dock (its rounded top corners), so cards show right up to
  the dock's edge and the opaque dock simply covers them. No fade band, no empty strip between
  the cards and the dock. (Still never a translucent bar with cards visible through it, v1.)
- The bar is **the dock**: its own opaque surface (light `#FFFFFF`, dark `#232326`) with a soft
  upward shadow (28dp; light `rgba(60,40,30,.07)`, dark black 45%) attached to its top edge, no
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
| | Long-press the address pill (only the pill: the buttons have their own long presses) | **Space mode** (haptic tick): the pill shows the current space's icon and name |
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
| Horizontal swipe | card area | swipe-to-close **the focused card**: follows the finger with a slight tilt and fade; released past 35% of the card's width, or flung sideways faster than 800dp/s, it flies off (180ms, 8°) while the gap closes at once: the card on the side last scrolled towards comes into focus (behind if not scrolled since the overview opened; the other side at an end). Nothing waits for the fly-off. "Tab closed · Undo" toast |
| Tap focused card | card | open tab (zoom morph) |
| Tap a pile or peek card | card | bring it into focus |
| Long-press card | card | lift (scale 1.03 + shadow), then drag to reorder within the space (dropping on the workspace bar to move the tab to another space: later) |
| Tap ✕ | focused card | close (same fly-off as swipe-close, to the right) |
| Mouse wheel / two-finger trackpad | card area | scrolls the stack like a finger drag, settles on a card when the scroll stops |
| Tap space name in header | header | space menu (rename, icon, theme, new folder): later, needs M6 |
| Swipe down on overview header | header | dismiss overview back to the current tab |

Rule: **horizontal swipes in the card area never switch spaces.** Switching spaces only happens on
the bottom bar. This avoids the close-vs-switch gesture conflict.

## Bottom bar / workspace bar

One bar serves both screens (Arc Search–sized, easy to hit with the thumb). Values in
`Tokens.kt` (`BarTokens`, `DrawerTokens`, `PagerTokens`).

- **Page view:** the dock, full width, square: back · big address field (50dp, 17sp, domain
  only) · tab count · menu. Swipe up on the bar (or tap the tab count) opens the overview;
  sideways switches tab; long-press the pill then sideways switches space (see the gesture model
  above). Long presses (each with a haptic), and only the pill's scrubs the spaces:
  - **back:** a small popup of the pages before this one in the tab (favicon, title, most recent
    first; scrolls past 6); tap = back to that page.
  - **tab count:** New tab · Close tab (Undo toast) · Reopen closed tab (the last tab closed in
    this space, with its history; greyed out when there is none).
  - **⋯:** Settings.
  - **History** (overview): Recently closed.
  **Dock popups** (every popup opened from a dock button follows this): just above the dock,
  anchored by the outer edge on the side of their button. A button on the right half (tab count,
  ⋯) → the popup's **right** edge on the dock's right margin (the ⋯ button's outer edge); a button
  on the left half (back) → its **left** edge on the dock's left margin. That edge never moves:
  the width hugs the content (tab count: at least 200dp; back: at most 300dp), so longer or extra
  items make it grow towards the middle, never shift it. They grow out of their button and close
  on a tap elsewhere or Back.
- **Loading line** (design 4d, 7g): a 2dp line in the **brand gradient** on the dock's top edge,
  from the left, with a soft glow. It eases towards the engine's progress (800ms, never backwards), a
  lighter streak sweeps along it while it waits, and on completion it runs to the end and fades
  out (350ms). Page view only.
- **Overview:** the same strip grows upward; the workspace switcher row (with the drawer handle)
  appears on top, the address row keeps its layout and only the buttons change: back crossfades
  to **History**, the field becomes "Search or type URL" (tap = new tab with search), the tab
  count stays (tap = back to the page), the menu crossfades to **Settings**. Long press on
  History opens it on **Recently closed**: the last 25 closed tabs (also the top section of
  History); one tap brings a tab back in its space and place, with its back/forward history,
  and opens it. From here it can grow further into the drawer (see "Essentials & pinned drawer").
  The morph only draws/transforms (no relayout per frame).
- The dock covers the bottom of the stack (see "The dock covers the bottom of the stack").
- The switcher is Zen's icon row (design 4a): one icon per space (initial if it has none), the
  active one in a pill tinted with its colour (accent at 14% light / 18% dark) showing its name in
  the space's ink colour. Inactive icons: 44dp touch targets, 70% opacity. The pill follows the
  drag continuously (positions, pill width and fades are layer/draw-only). Centred when it fits;
  with too many spaces the active pill stays centred.
- **Switching is a simple pager**, driven by horizontal drags anywhere on the bar:
  - The whole space follows the finger 1:1 (header and stack move as one unit; the stack trails
    slightly: 0.8× speed and scale 0.94 → 1).
  - The switcher pill interpolates between the two spaces by drag fraction.
  - Release: dragged past **25% of the width**, or flung in the drag direction, commits to the
    neighbour; otherwise it springs back.
  - **Exactly one light haptic tick per committed switch**, on release. Never while dragging.
- Tap a space icon = jump to it (one tick). Long-press bar = grid of all spaces (pick, reorder,
  create) — later.
- In the page view, space mode (long-press the address pill, then swipe) switches space: the host
  gives way to the space's icon and name, the target space's slide in; landing opens that space's
  current (else newest) tab, or its overview with an empty state (message + big "New tab" button)
  if it has no cards. A plain horizontal swipe there switches **tab** instead.

## Open / close transition (the hero animation)

- **Open:** tapped card becomes the page. Shared-element morph: card bounds → full screen,
  corner radius 28dp → 0 (or device corner radius), header row fades out during the first 40%,
  other cards drift down and fade slightly, the toolbar slides up from the bottom.
  Use a spring (no fixed duration): `Motion.Overview`, damping ratio 1, stiffness 600, no
  overshoot.
- **Close (back to overview):** reverse morph into the card's slot, same spring, no overshoot. If the card's slot is off-screen, the stack pre-scrolls so the slot is visible first.
- During the morph, show the cached thumbnail, then swap to the live GeckoView once its first
  frame is ready (never show a blank white page during the morph).
- **New tab** (M4, not built): a new card grows from the address field into the stack bottom,
  then immediately morphs open with the address sheet focused. Today a new tab uses the
  no-card fallback below.
- **Never a double image (0.4.0):** the overview is opaque throughout; only the flying card
  (`ui/overview/CardHero.kt`, the card's own header + the tab's screenshot) moves over it, and the
  real card hides while it flies. The header slides in from above the screen edge as the corners
  round. No card to fly to (a new tab, a link from another app): the page shrinks towards the
  middle instead. Same morph when a finger drives it on the dock.

## Themes

Source (local only, not in the repo): `docs/reference/design/Koan Essentials.html` section 7 ("M5 · Browser essentials — brand
gradient"). It supersedes the theme sections of `Koan Screens.dc.html` (the "soft wash" light theme
is gone). Setting: Light / Dark / Follow system (default). Web pages follow it too
(`prefers-color-scheme`).

- **Light, fully neutral:** base `#F5F5F7`, white surfaces (cards, dock, sheets), grey grouped
  rows, black text. No space-colour wash anywhere.
- **Dark:** base `#121214`, surfaces `#2C2C2E`, dock `#232326`.
- **Space colours stay only on tiny space indicators:** space icons and the active space pill in
  the switcher (accent from the Zen gradient's first colour: hue kept, saturation ≥ 55%, lightness
  45–62% light / 62–74% dark; pill tint 14% / 18%; ink text 38% / 80%; grey or missing theme uses
  `#5B6CFF` light / `#8EA2FF` dark).

### Brand gradient

Pink → violet → blue (`#FF5FA2` → `#B46BFF` → `#4F8DFF`, `BrandTokens`, helpers in
`ui/theme/Brand.kt`). A design-language element that means **Koan is working, active, or holds
your focus**, used only for:

- the page loading line; the download completion moment; download progress bars in the list;
- the syncing indicator (a small turning gradient arc: the drawer's sync row, Settings,
  pull-to-refresh; not in the overview header, which shows only the amber sync dot);
- toggles ON; the "@" search mode (ring + cursor);
- Settings' aurora glow (also behind About), the account card outline (**not** while it says
  "Couldn't sync": a failure is not an active moment), and the first-run "Connect your Zen
  Spaces" card (gradient outline);
- find in page: the find field gets the gradient ring while find is open (an active moment).
  The matches in the page are not the gradient (Gecko draws one colour per highlight); they
  follow Koan's theme, for every page: current match `FindCurrent` `#1554CB` with white text,
  other matches `FindMatch` `#D0DDF5` with black text (light) / `#1C396E` with white text (dark);
- the first-run tour / "What's new" (`ui/tour`, a 92% sheet): a faint glow behind each card's
  title, and the welcome card, where the whole sheet is the vivid gradient with a slowly turning
  vortex behind the 3D K, blending into the sheet's own colour as you swipe on.

**Never** as a passive "this is the current one" marker (the current tab is a state, not a moment), on login or password UI (one exception, the owner's choice: the faint glow behind the K on the sign-in choice page), permission prompts, destructive actions, body text, or behind
web content, or a static card that does nothing (the About "Built to pair with Zen" card is a
plain card). Primary buttons are never gradient.

### Page header

One component for every full page (`ui/kit/PageHeader`), iOS style:

- **The bar** (`PageBar`): 56dp, fixed at the top under the status bar, 20dp side padding: a 56dp
  back target with the 24dp ‹, the title in 22sp Bold (`KoanType.title`), one line, ellipsized,
  then an optional trailing slot (Downloads' folder chip).
- **Settings pages** (Settings, About, Passwords, a site's passwords and Add a password, Search
  engine, Add/Edit search engine, Mozilla account, Translation, Save downloads to; `SettingsPageFrame`) also show a **large
  title** under the bar: 34sp Bold (`KoanType.largeTitle`), up to 2 lines, on the page's aurora
  glow. It scrolls with the content and **collapses into the bar**, driven by the scroll offset
  (layer properties only, no recomposition): over the large title's height the bar's surface
  (the page's own base and glow, drawn at the same origin, so it is seamless) fades in; the large
  title stays fully opaque for the first 25% (a nudge doesn't grey it) and fades out by 60%,
  the compact title fades in over the last 40%, so the two
  never show together; once the content passes under the bar, a 1dp hairline fades in over 12dp.
  Collapsed, the bar stays fixed. A long title wraps on 2 lines when large and ellipsizes in the
  bar.
- **List pages** (History, Downloads; `ListPage`) use the bar directly, already collapsed: the
  list scrolls under a fixed bar, the search field and Clear sit at the bottom in thumb reach.
- Sheets' pushed pages (menu › Site settings, uBlock) keep the sheet header (‹ + 17sp title):
  they are part of a sheet, not full pages. The sign-in screen keeps its ✕ (a flow, not a page).

### Controls

- **Toggles** (`ui/kit/KoanToggle`, 50×30dp): ON = the gradient track at 90% (slightly softened)
  with a white knob and a soft shadow; OFF = `#848489` light / `#8E8E93` dark (≥ 3:1 against every
  row surface, so an OFF toggle still reads as a control). The unselected radio of the login
  sheet uses the same colour as a 2dp ring. On a tap the knob
  springs across and the gradient flows in behind it, from the left edge up to the knob. Light
  haptic tick. Draw-only animation.
- **Buttons** (`ui/kit/Buttons`): primary = black pill with white text in light, white with black
  text in dark, 50dp tall. **One destructive button** (`DestructiveButton`): red SemiBold text on
  a neutral pill, 50dp, never a red fill: History/Downloads' Clear, Clear site data, Delete
  engine, the confirm cards. A destructive *row* ("Sign out") is a row with a red title. The one
  red fill is the delete swipe's background on the library pages (History, Downloads,
  Passwords: it shows what the gesture does).
- **Fields** (`ui/kit/KoanField`): one style for every field outside the dock: 50dp (a button's
  height), pill, Glass fill, 16sp text and hint, an optional search glyph and a ✕ that empties a
  search. History, Downloads and Passwords search, the search-engine form, page prompts. The
  dock's address field and find-in-page are the dock's own (50dp pill, 17sp).
- **Rows, one style per role:** settings rows (full pages: 54dp, 62dp with a subtitle, bare
  20dp glyph); menu rows (sheet menus, link menu, site settings: 50dp / 58dp, the glyph on a
  30dp tile); dock popup rows (tab menu, back menu: 48dp, the same tile, or a favicon); item rows
  (history, downloads: a 40dp tile holding the favicon or file type; passwords: the bare 20dp
  site icon, see "Library pages").
- **Menu icons:** always on a tile (`MenuIconTile`, 30dp, 9dp corners, 18dp glyph): Dock fill on
  a grouped row, GroupedRow fill on a dock popup.
- **Favicons:** one letter tile everywhere (except the Passwords pages, see "Library pages"),
  the @ picker included: the site's first letter,
  white, on a muted tone picked by host. `m.`, `mobile.` and `www.` hosts share the site's icon
  and tile.
- **Search field:** docked right above the keyboard (the bar follows the IME), search glyph on the
  left, ✕ on the right. While typing: a neutral 1.5dp ring (`#B4B4BA` light / white 30% dark) and
  a plain cursor. Typing "@" first makes the brand gradient flow in from the left around both
  halves of the ring (2dp, crisp, gradient cursor) on a soft spring; deleting the "@" flows it
  back out to neutral. No glow bleeding around the field: at most a very faint halo (14%) kept
  1.5dp around the stroke. In "@" mode the ring is alive: the gradient (a sweep around the
  field's centre) turns slowly, ~48°/s, its speed drifting with two out-of-phase slow
  oscillations (±14° over 3.1s, ±10° over 4.7s), never mechanical. Only the shader moves, only
  while "@" mode is on, and not at all when the system removes animations.

## Library pages (History, Downloads, Passwords)

One set of rules for the three pages (`ui/kit/Selection.kt`, `ui/kit/SwipeRow.kt`,
`ui/kit/LibraryRow.kt`).

**Tap.** History: opens the page in the tab History was opened from; from the overview (no tab
behind it), in a new tab. A Recently closed row reopens that tab in its space and place.
Downloads: opens the file (a video in Koan's player). Passwords: the site's accounts, after the
fingerprint or PIN.

**Long press = selection mode.**
- A long press checks that row (haptic). In selection mode a tap checks or unchecks a row, it
  never opens it. Rows have no ⋯ button.
- Header: "N selected", **Select all** and **✕**. It replaces the page title cleanly: the large
  title hides while selecting, the header never sits on top of it.
- A bottom action bar (thumb zone, 64dp, radius 20) holds the actions for what is checked.
- Back, ✕ or unchecking the last row leaves selection mode.

| Page | One row | Several rows |
|---|---|---|
| History | New tab, Copy link, Share link, Remove (a group row alone: "Remove N visits" only) | Share (one link per line, every page of a checked group), Remove |
| History, Recently closed | Reopen, New tab, Copy link, Share link (one closed tab at a time, never with pages) | — |
| Downloads | Open (glyph and label by type, below), Copy link, Share (the file), Delete | Share, Delete |
| Passwords | Visit (the site in a new tab), Delete | Delete |

- "New tab" has the open-in-new-tab glyph (arrow out of a box).
- File-type glyph (Downloads' Open, the duplicate card's Open row): video or audio = play, image =
  picture, APK = install (label "Install"), anything else = document.
- Passwords: no copy action in the list. A username or password is copied only on the site's
  page, after biometrics.

**Swipes.**
- History and Passwords: right to left = delete. Left to right does nothing but give a little
  (36dp) over grey and spring back.
- Downloads: left to right = hide from the list (the file stays, grey background); right to left =
  delete the file (red background). Hide is a swipe only.
- Recently closed rows have no swipe.
- A swipe acts past 40% of the row's width or on a fling; otherwise the row always springs back,
  also when the gesture is cancelled. One row never blocks another.

**Confirmations and Undo.**
- History: Undo toast, no confirmation.
- Downloads: Undo toast; the file is deleted for real only once the toast is gone, and Undo
  restores it untouched. No confirmation for one file; several files from selection mode ask
  first: "Delete N files?", Cancel / Delete.
- Passwords: **always** a confirmation card before deleting (`KoanCard`): "Delete this password?"
  (or "Delete N passwords?"), site · username, what it affects ("Removed from this phone.", or
  "Removed from this phone and your Mozilla account." only when a synced login is among them and
  "Change and delete passwords" is on), Cancel / Delete (red). Then an Undo toast.

**Passwords page.**
- A flat list on the page background (no card container), hairline dividers, "N accounts" (or
  "This phone") and a chevron at the end.
- Site icons: only icons already on the phone (tabs, History, Essentials), **never fetched** for
  these pages (privacy rule; no favicon or logo service either). Each is cut to its drawing and
  fills the same 20dp box, greyscale at a little less contrast, bare (no tile); an opaque square
  icon keeps its square with rounded corners. No icon, or one that can't be seen in grey on the
  page: the key glyph (the globe on a site's page). Never a letter tile.
- Add: a round 56dp "+" floating at the bottom right (20dp margin, shadow, Primary fill) over the
  list, which ends with room for it. Hidden while selecting. The empty state has its own "Add a
  password" button.
- A site's page: the icon is grey while locked and crossfades (220ms) to full colour once
  unlocked, back to grey when it locks again. Locked, each account's site row shows above Unlock.
  Unlocked, a red "Delete password" row under each account opens the same confirmation card,
  then Undo; deleting the site's last account goes back to the list.
- **Locked:** nothing is deleted or selected before the fingerprint or PIN. A swipe shows the red
  delete as usual, then asks to unlock, then shows the delete card; a long press asks to unlock,
  then starts selection mode on that row.
- No screenshots, screen recordings or Recents preview of the Passwords pages (`SecureWindow`).

## Floating cards that answer an action

**Cancel, on every card: one rule, no text-only Cancel anywhere** (`ui/kit/Buttons.kt`).
- Two choices (delete confirmations, "Delete N files?", Sign out, Clear browsing data, the
  save-password prompt, "Open in [app]?"): `CardChoices`, Cancel and the action side by side, same
  size; Cancel neutral on the left, the action on the right (red text when destructive).
- A list of choices (the duplicate card, permissions…): `CardCancel`, a full-width neutral button
  under the list, same height and radius as the other buttons, normal text colour.

**Card to card = one card that morphs.** When a card leads to another (the image long-press card
→ the duplicate card; the download confirmation → the duplicate card), the card is not closed and
another opened: the same card stays, its height and corners spring to the new size (0.85 / 420),
the big image preview shrinks into the small thumbnail at the top (shared element), and the
content crossfades (220ms). The link card, the download confirmation and the duplicate card are
floating cards (they answer an action), never drawers.

**Duplicate download** (`ui/menu/DuplicateDownloadPrompt.kt`): every download passes the check
first, also the ones that skip the confirmation (Save image, a PDF viewer's Download). Shown when
the same address was downloaded and the file is still there, or a file of that name is in the
folder. Thumbnail (or type), file name, folder, then Open (type glyph), Replace, Keep both
("name (1).ext"), and Cancel under the list. Rename: a tap on the name or on the pencil (20dp
glyph in a 44dp target, next to the name) turns the name into a text field in place, extension
kept, only the base name selected, keyboard up; Done or a choice (Replace, Keep both) confirms it.

**App links** (`ui/menu/AppLinkPrompt.kt`): a link to an app (an "Open app" button, `intent://`, an
app's own scheme) asks first: "Open in [app]?" with the app's icon, the site, an "Always for this
site" switch, Cancel / Open. A page that tries on its own (timer, redirect, hidden frame) gets
the card at most once per page. Plain https links never get the card. No app installed: the
link's web fallback if it has one, else a toast. The ⋯ menu shows "Open in [app]" for a site an
installed app opens.

## Page translation

On the phone (Firefox Translations in Gecko). Code: `ui/browser/TranslateOfferHost.kt`,
`ui/menu/TranslatePage.kt`, `ui/settings/TranslationPage.kt`.
- **Offer card:** a small floating card for a page in another language than the phone's, once per
  page; it leaves on its own after 20 s, with ✕ or a tap outside. Quiet links: "Always for
  [language]", "Never for [language]", "Never for this site". A page with too little text gets
  no offer.
- **⋯ › Translate page:** From / To pickers, Translate, Show original once translated, and the
  switches "Always translate [language]", Never for this language, Never for this site. Once a
  translation or a restore has finished, the page always shows the finished state (Show original
  / Translate again), however many times it goes back and forth.
- **Language lists (From, To):** a ‹ back to the Translate page (as Back); a search field at the
  top (while searching the sections hide, only matches show); sections, each language once, in
  the first it fits: **Recent** (last 3 picked on that side, the current one checked), **On this
  phone** (downloaded, marked "On phone"), **All languages** (A–Z, with download size when
  known). A long press on a downloaded language: "Delete [language]?" card (frees N MB; downloads
  again next time), Cancel / Delete.
- **Always translate:** every page in that language is translated to the To language on its own
  (no offer card); a toast says "Translated from [language]" with Show original.
- **Settings › Translation:** Always translate (languages), Never translate (languages), On this
  phone (each downloaded language with its size and Delete, same card), Never translate these
  sites; each removable there.

## Back closes a tab

Back on a tab's first page closes it (`ui/browser/ClosingPage.kt`): the page's last pixels shrink
into a card that slides off to the left, like a card swiped off the overview, while the next tab
comes up under it (380ms, `FastOutSlowIn`, layer only). No animation when Back returns to the app
that opened the link, or when there is no other tab to land on.

## Toasts

One bottom toast (`ui/kit/KoanToast.kt`): a short message, at most one action ("Undo", "Switch",
"Open", "Show original"), an optional quiet second line; 3 s, then it sinks away; a swipe sends it
away. A download's completion gets the brand-gradient outline (a moment).
- **Hand-over:** a new toast while one shows never just swaps its text: the old one sinks further
  down (1.5× the rise), shrinks a little and fades, from where it was dragged, while the new one
  rises in its place.
- **Toasts make way** for a sheet, a card or a full page (see "One sheet everywhere").

## Private mode

Spec: `docs/design/OVERVIEW_PRIVATE.md` (design hand-over), as built in `docs/DESIGN_SYSTEM.md`
"Private mode". Code: `ui/privacy/`, `privacy/`.
- A mode of the overview, not a space: never in the space switcher. Private tabs are one set
  apart from the spaces; turning Private off keeps them.
- Toggle (`PrivateToggle`) at the end of the overview header: 56×48 touch, 56×32 track, mask in
  the knob; `CLOCK_TICK`; taps ignored during the flip. Its own colours (the other toggles keep
  `ToggleOff`).
- Palettes: **Dusk** (light, base `#DEDAF0`) and **Night** (dark, base `#110F24`), with stars and
  glows behind the stack (static at rest). Settings, History, Downloads and the ⋯ sheet keep the
  normal palette.
- Header: "Private" + "Not saved" chip + toggle, no space icon. Dock: one centred "Private" pill
  instead of the switcher; address pill "Search privately" (overview) or the host (page), with the
  mask. The tab count counts private tabs.
- Flip: Private is a page beside the current space; on / off is the space pager's own motion
  (into Private from the right, back from the left), stars appearing right to left. Reduced
  motion: a 150ms slide.
- Search sheet: a notice first, then Bookmarks and "Switch to private tab" only (no history, no
  normal tabs).
- Empty state: mask, "No private tabs", four facts, "New private tab".
- Locked (after 1 min away or the screen off, when private tabs exist): only **Unlock** and
  **Back to normal tabs**.
- No screenshots or Recents preview while Private shows (unless allowed in Settings). Private
  tabs' favicons are cache-only (never fetched) and their screenshots never written to disk.

## Video player

Spec: `docs/VIDEO_PLAYER.md` (design hand-over with an "As built" list; the code,
`ui/player/PlayerTokens.kt`, wins). Code: `ui/player/`.
- One player, two modes. **In a page:** a fullscreen video gets Koan's controls in place, in the
  same tab (YouTube keeps its own). **Standalone:** a video from Downloads, "Play video" in the
  link menu, or a video file opened from another app plays in an overlay above the current
  screen, never a new tab; closing returns to the same screen.
- Controls: a tap shows / hides them at once (on release); auto-hide after 3 s while playing,
  never while paused, ended, the menu open or the bar pressed. At the end, Koan's controls stay
  with Replay. Readable on bright video (scrims, 72% panels over a blur).
- Gestures, zones left 35% · centre 30% · right 35%: double tap a side = ∓10 s (each extra tap
  within 700ms adds 10 s); long press 400ms = 2× while held; vertical swipe left = brightness,
  right = volume (system volume, full range 0.35 × screen height); swipe down = leave
  fullscreen (page) or close (standalone), never the page's pull-to-refresh; a lock.
- ⋯ menu: speed (0.5–2×); in a page "Download video" (when the file can be downloaded) and "Use
  the site's controls"; standalone (file): Share, Open with another app, Delete (a download);
  standalone (link): Download video, Share link.
- Picture-in-picture on Home (Settings › Display › Picture-in-picture): 10 s back, play/pause,
  10 s ahead, and "Audio only" for a web video.
- Not built: the mini-player and minimise (M7).

## Visual tokens (starting point, tweak freely)

All values live in `ui/theme/Tokens.kt`: one `KoanPalette` per theme (colours), plus the shared
size/motion objects.

- Light: base `#F5F5F7`, cards/dock/sheets `#FFFFFF`, small surfaces `#EBEBEF`, grouped rows
  `#F2F2F5`, field `#F1F1F4`, text `#1C1C1E`, secondary and hints `#66666B`, icons `#6E6E73`,
  toggle OFF `#848489`, destructive `#C42A20`, attention (amber) `#8A5C00`.
- Dark: base `#121214`, cards `#2C2C2E`, dock `#232326`, small surfaces and field `white @ 8%`,
  grouped rows `white @ 7%`, text `#FFFFFF`, secondary `white @ 62%`, icons `#98989D`, hints
  `white @ 60%`, toggle OFF `#8E8E93`, destructive `#FF8077`, attention `#E8B45A`.
- Contrast: text ≥ 4.5:1 and controls ≥ 3:1 against every surface they sit on, in both themes
  (values in `docs/DESIGN_SYSTEM.md` "Contrast").
- Brand gradient: `#FF5FA2` → `#B46BFF` → `#4F8DFF` (see "Brand gradient").
- Typography: the system sans. Line height scales with the size (em), titles tighter
  (`ui/theme/Type.kt`): body 1.33em, tracking 0; headlines (17–20sp) 1.25em, −0.006em; titles
  (20–30sp) 1.18em, −0.012em; large title (34sp) 1.12em, −0.016em. A wrapped title never
  overlaps itself.
- Radii: cards 28dp, pills full, sheets 32dp top.
- Motion: springs everywhere; avoid linear/tween except for alpha. Respect the system
  "remove animations" setting (snap instead).
- Haptics: one light tick per committed space change, medium on card lift, confirm on close.
