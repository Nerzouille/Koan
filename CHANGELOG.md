# Changelog

## Unreleased

### New

- **Update check in About:** opening About looks once at Koan's GitHub releases and says "Up to
  date" or "Update available" (tap to open the release). Never in the background.
- **Privacy notice** ([`docs/PRIVACY.md`](docs/PRIVACY.md)), linked from About: every server Koan
  talks to and what it sends.

## 0.2.0 — 2026-10-16 (pre-release)

Browser essentials. Android 14+ (minSdk 34), arm64-v8a only. Installs over 0.1.0 and keeps your
data. Full guide: [`docs/FEATURES.md`](docs/FEATURES.md).

### New

- **⋯ menu as a sheet:** the page's navigation row (back, forward, address, copy link,
  reload/stop), New tab, Bookmark, Share, Find, Site settings, Desktop site, Reader view, Add to
  Essentials, Downloads, Settings and uBlock Origin. Same detents and gestures as the drawer.
- **Settings:** Mozilla account (Sync now, Sign out with confirmation), search engine, search
  suggestions, theme, default browser, passwords, uBlock Origin, downloads and where they go, About.
- **Search sheet:** results above the field while you type (engine suggestions, open tabs in any
  space, history and bookmarks), inline completion of the best site, and `@` shortcuts for
  DuckDuckGo, Google, Wikipedia, YouTube and for searching only tabs, history or bookmarks.
- **Search engines:** DuckDuckGo, Google, Brave Search, Startpage, each with its own suggestions;
  suggestions can be turned off.
- **Default browser:** links from other apps, web searches and shared text open as a new tab in the
  current space.
- **Find in page** (blue highlights, match count, previous/next) and **reader view**.
- **Long-press menus:** for links (open in new tab, open in background with a Switch toast, copy,
  share) and images (save, open, copy link), with a preview of what you pressed.
- **Site settings and permission prompts:** Block / Always allow / Allow once for location, camera,
  microphone and the rest, remembered per site; clear a site's data.
- **Downloads:** start without a prompt, progress ring in the address pill, a toast with Open when
  done, a Downloads page (pause, resume, cancel, open, share, delete, search), the phone's Download
  folder by default or a folder you pick.
- **Passwords:** your Mozilla account's passwords, read-only and encrypted on the phone; autofill
  chips above the keyboard and a full "Sign in with" list; "Save in Koan" for new logins (phone
  only); Settings › Passwords behind your fingerprint or PIN, with search, reveal and copy.
- **History and bookmarks:** your account's (read-only) and the phone's, used by search; bookmark a
  page from the menu.
- **uBlock Origin:** installed on first start; its blocked count on the menu card, its own panel in
  the menu, on/off and the week's count in Settings.
- **Add to Essentials** from the menu (phone-only copy for tabs that came from Zen).

### Improved

- Light theme: neutral greys and white instead of a wash of the space colour; the space colour only
  marks its pill and icons.
- The drawer shows all essentials (no limit of 8).
- One Back closes the search field or find in page together with the keyboard.
- Sign-out asks first, and the account's passwords, bookmarks and history leave the phone with it.

### Fixed

- A tab opened straight onto a file no longer stays empty and downloads the file again later.
- The address pill's download ring has its own tap target (it used to start editing the address).
- The long-press card no longer grows when the image preview arrives.

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
- **Overview:** a stack of cards per space, swipe or ✕ to close, long-press to reorder,
  a drawer with essentials, pinned tabs and folders.
- **Gestures:** one gesture model on the bottom bar: swipe the address pill to go through tabs
  (chained), drag up for the overview, long-press to scrub through spaces, and a
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
