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
   with its name, icon, colour, essentials, pinned tabs, folders and (if enabled in Zen) normal
   tabs. The theme stays neutral; the space's colour only tints its pill in the switcher.
2. **Tabs as vertical cards.** Inside a space, tabs are tall rounded cards stacked vertically with
   overlap (like Safari's old card view / Arc iPhone): each card shows
   its header (favicon, title, close button) and a live thumbnail. Opening and closing a card uses
   a fluid, spring-driven zoom morph like iOS's current tab animation.
3. **The dock.** One bar at the bottom, the same in a page and in the overview. Swiping left/right
   on it moves between spaces; the cards above slide with it.
4. **Real browser underneath.** GeckoView: same engine as Zen, full web compatibility, WebExtensions
   (uBlock Origin), downloads, permissions, passwords autofill from the Firefox account.

## "Better than Arc" list (in priority order)

(done) = in the app today, (planned) = not built yet.

1. Zen Spaces sync: reading (done, M2); writing back to Spaces (planned, M6).
2. Passwords + history + bookmarks from the Firefox account (done). Passwords and history also go
   back to the account, each behind its own switch (done); bookmarks stay read-only.
3. uBlock Origin (done).
4. Essentials grid per space (Zen essentials, synced), in the drawer that grows out of the dock (done).
5. **Glance**: long-press a link → page opens in a floating sheet over the current tab; swipe up to
   promote it to a real tab, swipe down to dismiss (Zen's Glance feature, mobile-ised) (planned).
6. Bottom address bar with a single-thumb flow (Arc-style), search suggestions, `@` to search a
   specific engine (done).
7. Swipe up on the dock in a page = the tab overview (done). Pull-down in a page is pull to refresh
   (done), not a new tab.
8. Zen folders as collapsible rows in the drawer, with the pinned tabs (done); Zen split views shown
   as one card (done).
9. "Open on desktop": send the current page to a Zen Space so it's there on the PC (planned, needs
   write-back to Spaces).
10. **Private browsing** (done; in scope since 2026-11-11, spec `docs/design/OVERVIEW_PRIVATE.md`): a mode
    of the overview with its own set of permanent private tabs (saved encrypted; sites sign you
    out on restart), a fingerprint lock, no screenshots, and the engine's private-only
    protections (HTTPS-only, fingerprinting protection, link cleaning, Global Privacy Control). It
    never claims to hide the IP address: that needs a VPN or Tor Browser.
11. **Page translation** on the phone with Firefox Translations, nothing sent anywhere (done).
12. **Video player**: Koan's own fullscreen controls, picture-in-picture with "Audio only", and a
    player for videos from Downloads or other apps (done; spec `docs/VIDEO_PLAYER.md`).

## Out of scope (for now, possibly forever)

- iOS, tablets, landscape-first layouts.
- Play Store release, onboarding for strangers, localisation.
- AI features (Arc's "Browse for me"). Maybe later as a toy.
- Hiding the IP address or location (Tor, a built-in VPN): out of scope, Private says so.
- Syncing Zen mods, themes beyond the space colour, containers (Zen doesn't sync containers yet).

## Success criteria

- The owner uses it as the only browser on the phone for two weeks without going back to Opera.
- Overview scroll and open/close animations never visibly stutter on the owner's phone.
- A tab pinned in a Zen Space on the PC shows up on the phone after a refresh, and (after M6) a tab
  added on the phone shows up in Zen on the PC.
