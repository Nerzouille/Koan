# Vision

## One sentence

An Android browser that feels like Arc on iPhone, but is a *real* full browser, and whose
workspaces **are** your Zen desktop Spaces.

## Why

- Zen (desktop) is the owner's main browser. Zen has no mobile app and isn't planning one.
- Arc for iPhone looks and moves beautifully, but lacks features (no extensions, weak sync, iOS only).
- Firefox for Android has the right engine and account, but the owner dislikes its UI.
- Opera works but can't link to Zen's Spaces or the Firefox account.
- Z-Sync (github.com/Kjell6/Z-Sync) proved that Zen Spaces can be read/written from a phone via
  Firefox Sync, but it's a companion app with a mini browser, not a real browser.

## Core idea

1. **Spaces from Zen.** Sign in with the Mozilla account → every Zen Space appears on the phone,
   with its name, icon, gradient theme, essentials, pinned tabs, folders and (if enabled in Zen)
   normal tabs.
2. **Tabs as vertical cards.** Inside a space, tabs are tall rounded cards stacked vertically with
   overlap (like Safari's old card view / Arc iPhone): each card shows
   its header (favicon, title, close button) and a live thumbnail. Opening and closing a card uses
   a fluid, spring-driven zoom morph like iOS's current tab animation.
3. **Workspace bar.** A horizontal bar under the cards. Swiping left/right on it moves between
   spaces; the cards above slide/parallax with it and the background gradient blends to the next
   space's Zen theme.
4. **Real browser underneath.** GeckoView: same engine as Zen, full web compatibility, WebExtensions
   (uBlock Origin), downloads, permissions, passwords autofill from the Firefox account.

## "Better than Arc" list (in priority order)

1. Zen Spaces sync (read in M2, write-back in M6).
2. Passwords + history + bookmarks from the Firefox account.
3. uBlock Origin.
4. Essentials grid per space (Zen essentials, synced), in the drawer that grows out of the bottom bar.
5. **Glance**: long-press a link → page opens in a floating sheet over the current tab; swipe up to
   promote it to a real tab, swipe down to dismiss (Zen's Glance feature, mobile-ised).
6. Bottom address bar with a single-thumb flow (Arc-style), search suggestions, `@` to search a
   specific engine.
7. Pull-down in a page = new tab in this space; pull-up on the bar = space overview.
8. Folders shown as collapsible groups of cards; Zen split views shown as one card with a split
   indicator.
9. "Open on desktop": send the current page to a Zen Space so it's there on the PC (needs write-back).

## Out of scope (for now, possibly forever)

- iOS, tablets, landscape-first layouts.
- Play Store release, onboarding for strangers, localisation.
- AI features (Arc's "Browse for me"). Maybe later as a toy.
- Private browsing beyond a basic private tab.
- Syncing Zen mods, themes beyond the space gradient, containers (Zen doesn't sync containers yet).

## Success criteria

- The owner uses it as the only browser on the phone for two weeks without going back to Opera.
- Overview scroll and open/close animations never visibly stutter on the owner's phone.
- A tab pinned in a Zen Space on the PC shows up on the phone after a refresh, and (after M6) a tab
  added on the phone shows up in Zen on the PC.
