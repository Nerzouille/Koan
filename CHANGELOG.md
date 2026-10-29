# Changelog

## 0.4.0 — unreleased

Installs over 0.3.x and keeps your data. Full guide: [`docs/FEATURES.md`](docs/FEATURES.md). For
automatic updates, install with [Obtainium](https://github.com/ImranR98/Obtainium) from this repo's
GitHub releases.

### New

- **Sign in from your computer:** on the sign-in screen, scan the code Zen shows under Settings ›
  Sync › Connect another device (or paste its link). No password to type on the phone. Signing out
  revokes that sign-in. Email and password still work as before. A first page asks which of the
  two ways, with the K floating gently among a few sparkles; Mozilla's pages now follow Koan's
  light or dark theme.
- **Your own search engines** (Settings › Search engine › Add a search engine): a name, the search
  address with `%s` for the words, an optional `@shortcut` and an optional suggestions address.
  They work like the built-in ones (default search, `@` picker with the site's icon, suggestions)
  and can be edited or deleted (Undo in the toast). On a page that offers its search engine (a
  SearXNG instance, many sites), ⋯ › **Add this search engine** fills the form in for you.
- **Baidu** is one of the built-in search engines, with its own suggestions and the `@bd` shortcut.
- **Hide status bar** (Settings › Display, off by default): pages use the whole screen, right up
  to the camera. Swipe down from the top edge to see the status bar for a moment. The search
  sheet, the overview, menus, Settings and the other pages bring it back (over the page, on a
  strip of the page's colour). Theme moved into the new Display section.
- **Search in Koan** from any app: select text, then pick **Search in Koan** in the selection menu
  (sometimes under ⋯). A link or an address opens as it is, anything else is searched with your
  default engine, in a new tab of the current space; Back returns to the app.
- **Links opened "in the app"** (Custom Tabs, as in Discord or GitHub) can open in Koan: a new tab
  in the current space, and Back closes it and returns to the app.
- **Reopen closed tab:** long-press the tab counter. It brings back the last tab closed in this
  space, in its place and with its back/forward history (greyed out when there is none).
- **Downloads a page starts on its own** (a countdown, a script, a background tab) now ask first,
  on a card with the file's name, size and site: **Download** or **Cancel**. A download you tap
  still starts at once.

### Improved

- **Page ↔ overview, no more double image:** the overview no longer fades over the page. The tab's
  card flies between the page and its place in the stack, fully opaque, both ways (also when you
  drag it on the dock). It lands exactly on its card; the cards behind rise in, the card in front
  slides up from the bottom and the space name slides down from the top, nothing pops in at the
  end.
- **Stack:** cards in front of the peek now slide down out of view, opaque, instead of fading.
- **Dock popups** (tab counter, back's history) stay on their side of the dock and grow towards
  the middle as their items get longer.
- **Smaller app:** the download is about 201 MB (was 228) and it takes about 27 MB less space once
  installed: an unused library and the libraries' translations are gone (Koan is English only),
  and the tour's clips are lighter.

### Fixed

- **Links from other apps** always open on their page, even when Koan comes back after a long
  time (it could stay on the overview, with the new card at the bottom). Back on that page returns
  to the app that sent the link, when Android says which one.
- **Swipe to close in the overview** works every time: a slow swipe past about a third of the card,
  a short quick flick, or a diagonal one all close it, and the card follows your finger exactly
  (a slow start used to pick the card up for reordering instead).
- After sign-in, once the first sync is done, the overview opens on Zen's top tab (the front-most
  Zen card; tabs opened on the phone sit just in front of it).
- **Tab order on sync:** tabs opened on the phone keep their place in the stack when Zen's tabs
  sync in, at first sign-in and on every later sync.
- Settings selectors (Start page, Theme) in dark mode: the chosen option now sits on a clearly
  lighter pill, in bold.
- Dragging the card of an open pinned tab in the stack now moves the card (it used to snap back
  and move the tab in the drawer instead).
- Closing the current tab in the overview, then going back to the page (tab counter, Back, swipe
  down) opened a blank page. It now opens the card that came forward in its place (the start page
  if the space is empty), and Undo makes the closed tab current again.
- **Issue templates:** bug reports and feature requests on GitHub ask for what's needed (phone,
  Android and Koan versions, steps). See Feedback in the README.

### Known limitations

- **Search in Koan** comes near the end of the selection menu: Android sorts these entries by app,
  and an app can't move itself up.
- **Long screenshots:** HyperOS's scrolling screenshot still doesn't scroll Koan's pages; waiting
  on the GeckoView update due after 10 November.
- **Old WebView warning:** written but not tested on a phone with an old WebView.
- **arm64 only** (64-bit ARM phones); APK about 201 MB.

## 0.3.0 — 2026-10-25 (beta)

Installs over 0.2.x (and the Android 11 preview) and keeps your data. Full guide:
[`docs/FEATURES.md`](docs/FEATURES.md). For automatic updates, install with
[Obtainium](https://github.com/ImranR98/Obtainium) from this repo's GitHub releases.

### New

- **A tour on first launch:** a welcome card, then six short looping clips: your Zen Spaces,
  swiping up for your tabs, swiping sideways to switch tabs, holding the bar to change space,
  `@` search shortcuts, and the long presses. It shows once (also when you update to 0.3), Skip
  or a swipe down closes it, and **Settings › About Koan › Take the tour** replays it.
- **New icon:** the K in a colour vortex, on the launcher, About and the first-run card; Android
  13+ themed icons use a flat K.
- **Android 11 and up:** Koan now installs on Android 11, 12 and 13 too (tested on Android 14;
  11–13 are supported but less tested). Below Android 12 the drawer dims what's behind it
  instead of blurring it, and phones with less memory keep fewer tabs live and smaller
  screenshots.
- **What pages ask, in Koan's style:** drop-down lists (`<select>`, single and multiple choice)
  now work, on a card with groups and the current choice ticked; a long list opens on it. Date,
  time, date and time, month, week and colour pickers; alerts, confirmations and text prompts
  ("example.com says"), with **Don't let this page show more dialogs** when a page keeps opening
  them; **Leave this page?**; site sign-in (HTTP authentication); **wants to open a pop-up**
  (Allow / Block); **Send the form again?**. A page's share button opens Android's share sheet, a
  folder field Android's folder picker. Printing a page opens Android's print dialog (save as
  PDF or print).
- **Recently closed:** the last 25 tabs you closed, at the top of History, or long-press History
  on the overview's dock. One tap brings a tab back in its space and place, with its back and
  forward history, and opens it.
- **Back's history:** long-press back in a page for the pages before this one in the tab (icon
  and title, most recent first); tap one to go straight back to it.
- **⋯ long press:** opens Settings.
- **Close with Undo in the overview:** closing a card shows a **Tab closed · Undo** toast.
- **Old WebView warning on sign-in:** if the phone's Android System WebView is too old for
  Mozilla's sign-in page (older than 92), sign-in says so, with a button to update it on the
  Play Store.

### Improved

- **Pages' bottom bars and the dock:** a page's own bar at the bottom (a chat's message field,
  Google's AI mode, a cookie banner) now sits right on the dock instead of behind it, follows it
  down as it hides, and stops at the page's real bottom. A page that can't scroll keeps its dock.
- **Typing in a page:** when a field in the page has the keyboard, the dock steps aside and the
  page ends at the keyboard, so the whole field shows right above it. The dock comes back when
  the keyboard goes.
- **Long presses on the dock:** only the address pill starts space mode. The tab count's menu is
  wider. Every long press gives a light vibration.
- **Closing in the overview** is instant: the stack moves on while the card flies off, bringing
  forward the card on the side you last scrolled from.
- **Undo keeps a tab's back history.**
- **Mouse wheel and trackpad** scroll the overview's stack.
- **Toasts** step aside when a sheet, a card or a full page (History, Settings, Downloads) opens.
- **Find in page:** a match on a page's last line shows above the find bar.

### Fixed

- Drop-down lists (`<select>`) did nothing.
- A page's fixed bottom bar was hidden behind the dock.
- A page's field at the bottom was half hidden by the keyboard.
- A card brought back with Undo could stay invisible.
- `window.print()` did nothing.

### Known limitations

- **Long screenshots:** HyperOS's scrolling screenshot doesn't scroll Koan's pages yet. A
  GeckoView update after 10 November should help; if not, Koan will get its own full-page
  screenshot.
- **Old WebView warning:** written but not tested on a phone with an old WebView.
- **arm64 only** (64-bit ARM phones); APK about 228 MB.

## 0.2.1 — 2026-10-19 (beta)

Installs over 0.2.0 and keeps your data. Full guide: [`docs/FEATURES.md`](docs/FEATURES.md).

### New

- **Start page:** when Koan comes back after being unused for a while, it opens on the current
  space's essentials, a **Continue** card for the tab you left (tap it to go back) and the search
  field, keyboard up. Nothing becomes a tab until you search or open something; Back or a swipe
  down returns to your tab. Never when a link from another app opens Koan. Settings › **Start
  page**: 30 min, 1 h (default), 4 h or Never.
- **Overview dock:** same layout as in a page, only the buttons change: **History** on the left,
  the search field, the tab count (tap = back to the page) and **Settings** on the right. The **+**
  left the overview's header: tap the search field for a new tab.
- **Tab count menu:** in a page, long-press the tab count for a small menu with **New tab** and
  **Close tab** (with Undo).
- **History, grouped:** back-to-back visits to the same site on a day make one row ("N pages ·
  time range") that opens in place; a single visit stays a normal row, and the same page visited
  twice in a row shows once. Swipe a group to remove all its pages (with Undo). Searching shows
  every page, ungrouped.

### Fixed

- The Settings icon is a gear (it looked like a sun).

## 0.2.0 — 2026-10-19 (beta)

The first beta: Koan is ready to be your everyday browser. Sync stays read-only. Android 14+
(minSdk 34), arm64-v8a only. Installs over 0.1.0 and keeps your data. Full guide:
[`docs/FEATURES.md`](docs/FEATURES.md).

### New

- **⋯ menu as a sheet:** the page's navigation row (back, forward, address, copy link,
  reload/stop), New tab, Bookmark, Share, Find, Site settings, Desktop site, Reader view, Add to
  Essentials, History, Downloads, Settings and uBlock Origin. Same heights and gestures as the
  drawer.
- **Settings:** Mozilla account (Sync now, Sign out with confirmation), search engine, search
  suggestions, theme, default browser, passwords, uBlock Origin, downloads and where they go, About.
- **About:** version, an update check that looks once at Koan's GitHub releases when you open it
  ("Up to date" or "Update available", never in the background), what's new, the privacy notice
  and the open-source licences.
- **Privacy notice** ([`docs/PRIVACY.md`](docs/PRIVACY.md)): every server Koan talks to and what
  it sends.
- **Search sheet:** results above the field while you type (engine suggestions right above the
  field, then open tabs in any space, then history and bookmarks), inline completion of the best
  site, and `@` shortcuts for DuckDuckGo, Google, Wikipedia, YouTube and for searching only tabs,
  history or bookmarks.
- **Search engines:** DuckDuckGo, Google, Brave Search, Startpage, each with its own suggestions
  (shown with its icon); suggestions can be turned off.
- **History page** (⋯ › History): the pages you visited, on the phone and synced from your
  desktop (marked with a small computer icon), one card per day. Search it, tap a page to open it
  in a new tab, swipe it left to remove it (with Undo).
- **Clear browsing data** (History › Clear): history, cached files and, if you ask, cookies and
  site data.
- **Back closes a tab when there's nothing left to go back to:** a tab another app opened closes
  and takes you back to that app; a tab opened from a link goes back to the tab it came from; any
  other tab closes onto the next one, with Undo. Pinned tabs and essentials never close.
- **Pull to refresh:** pull down at the top of a page to reload it.
- **Media controls:** audio and video keep playing with Koan in the background, with a
  notification and lock screen controls (play/pause, and next/previous when the site offers them).
- **Full screen video:** hides the bars and the dock, turns to the video's orientation; Back
  leaves full screen first.
- **File upload:** a site's "choose a file" button opens Android's picker, with the camera for
  photos and videos.
- **Phone, email and app links** (`tel:`, `mailto:`, `intent://`) open in their app.
- **Default browser:** links from other apps, web searches and shared text open as a new tab in
  the current space.
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
  only); Settings › Passwords behind your fingerprint or PIN, one row per site.
- **Bookmarks:** your account's (read-only) and the phone's, used by search; bookmark a page from
  the menu.
- **uBlock Origin:** installed on first start; its blocked count on the menu card, its own panel in
  the menu, on/off and the week's count in Settings.
- **Add to Essentials** from the menu (phone-only copy for tabs that came from Zen).

### Improved

- **Passwords:** a site with several accounts is one row ("3 accounts"); after your fingerprint,
  all its accounts show on one page. Tap a username or password to copy it, Reveal to see the
  password; a single account also has a big Copy password button.
- **Space scrub:** long-press the address pill and slide: the steps now fit your screen and number
  of spaces, so the first and last spaces are always reached just before the edges.
- **Toasts** can be swiped away, sideways or down, and sit above the keyboard.
- **Downloads** shows one card per day (Today, Yesterday, then dates), like History.
- **Address field:** a long address scrolls to keep the cursor in view, also while you drag the
  cursor handle.
- **Search:** the page dims behind the search sheet as soon as it opens; suggestions start from the
  first letter; inline completion only goes to sites you actually visit, never to sign-in or
  redirect pages.
- **Light theme:** neutral greys and white instead of a wash of the space colour; the space colour
  only marks its pill and icons.
- The drawer shows all essentials (no limit of 8).
- One Back closes the search field or find in page together with the keyboard.
- Sign-out asks first, and the account's passwords, bookmarks and history leave the phone with it.

### Fixed

- Redirect pages (like a sign-in's "Redirection" step) no longer end up in history; the ones
  recorded before are cleaned up.
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
