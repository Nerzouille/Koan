# Changelog

## 0.1.0 — 2026-10-15 (pre-release)

The first build shared outside the owner's phone. Android 14+ (minSdk 34), arm64-v8a only.

### What works

- **Engine:** GeckoView 157 (Firefox's engine, the same as Zen) through Mozilla android-components,
  with a Jetpack Compose UI. Search or type a URL (DuckDuckGo by default), back/forward, reload,
  progress bar, pages that open new tabs.
- **Spaces from Zen, read-only:** sign in to your Mozilla account and Koan shows the Spaces Zen
  syncs: names, icons, colours, essentials, pinned tabs, folders, split views and open tabs. Sync
  runs quietly in the background; nothing is ever written back to Zen.
- **Works without an account:** a fresh install is a plain browser with one local space. "Connect
  your Zen Spaces" is offered, never forced.
- **Real tabs:** each card opens a real engine tab. New tabs go in the current space. Up to 4 tabs
  stay live, the others are suspended and restored on demand. Screenshots on the cards. The last
  space and tab come back after the app is killed.
- **Overview:** an Arc-style stack of cards per space, swipe or ✕ to close, long-press to reorder,
  a drawer with essentials, pinned tabs and folders.
- **Gestures:** one gesture model on the bottom bar: swipe the address pill to go through tabs
  (chained, Safari style), drag up for the overview, long-press to scrub through spaces, and a
  drawer with compact and full detents. The bar hides while a page scrolls.
- **Themes:** Light (a soft wash of the space colour) and Dark, following the system by default.

### Known limitations

- **No saved passwords yet:** Firefox Sync logins are not read and there is no autofill.
- **No ad blocking:** no uBlock Origin, no add-ons.
- **No downloads UI:** downloads, site permissions, find in page and share are not there yet.
- **Read-only sync:** tabs opened, closed or moved on the phone stay on the phone; Zen desktop
  never sees them. History and bookmarks are not synced.
- **Unofficial Mozilla sign-in:** to get into Firefox Sync, the sign-in identifies as Firefox
  desktop. This is unofficial, can break whenever Mozilla changes something, and may go against
  Mozilla's terms of service. Use it at your own risk, ideally not with an account you can't
  afford to have trouble with.
- Opening and closing a tab is a simple crossfade; the motion polish comes later.
- A large APK (~220 MB): GeckoView's native libraries are stored uncompressed so they load in place.
