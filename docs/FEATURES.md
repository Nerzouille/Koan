# Koan — user guide (v0.2.0)

Everything that works in this build, how to use it, and what isn't there yet. Koan is a browser
built on GeckoView (Firefox's engine). Its workspaces are your Zen Browser Spaces, read from your
Mozilla account. Koan is not affiliated with, endorsed by or connected to Zen Browser or Mozilla.

The bottom bar is called **the dock** in this guide. In a page it holds: back, the **address pill**
(the site's name), the **tab count** and **⋯** (the menu).

---

## 1. Setup

### Install

- Requirements: Android 14 or newer, a 64-bit ARM phone (arm64-v8a). Other CPUs are not built.
- Download `koan-v0.2.0.apk` from the GitHub release and open it. Android asks you to allow
  installing apps from that source (your browser or file manager) the first time.
- Installing over a previous Koan version keeps your tabs, spaces, logins and settings.
- The APK is about 210 MB. See [Known limitations](#13-known-limitations).

### First run

- Without an account, Koan is a plain browser with one space called **Phone** (📱). The overview
  opens on a **Connect your Zen Spaces** card; you can ignore it and just browse.
- Tap the address pill, type an address or a search, and go.

### Signing in with a Mozilla account

- Where: the **Connect your Zen Spaces** card in the overview, the status row at the bottom of the
  drawer, or **⋯ › Settings › Connect your Zen Spaces** (the card at the top of Settings).
- You sign in on Mozilla's own accounts page, shown inside Koan. Two-step verification codes are
  asked there if your account uses them.
- The session and the sync key are stored on the phone, encrypted with an Android Keystore key.
  There is no Koan server, no analytics, no telemetry.
- The sign-in identifies as Firefox desktop to be let into Firefox Sync. This is unofficial: it can
  break when Mozilla changes something and may go against Mozilla's terms. Use it at your own risk.

### What is synced (read-only)

Koan only reads. Nothing you do on the phone is written to your account or to Zen.

| From your account | What Koan does with it |
|---|---|
| Zen Spaces | Spaces, their icons and colours, essentials, pinned tabs, folders, split views, open tabs |
| Passwords | Autofill in pages, and the list in Settings › Passwords |
| Bookmarks | Search results |
| History | Search results (the 5000 most recently changed pages) |

- Spaces sync when Koan starts, when it comes back after more than 2 minutes, every 15 minutes
  while it's open, and on **Sync now**. Passwords, bookmarks and history follow a Spaces sync, at
  most every 30 minutes, and always on **Sync now**.
- **Sync now:** the status row at the bottom of the drawer, or Settings › your account card ›
  **Sync now**.
- Errors stay quiet: an amber dot on the drawer's status row (and on the overview's badge) when a
  sync failed, or "Sign in again" when the session expired.

### Signing out

- **⋯ › Settings ›** your account card › **Sign out**, then confirm.
- Your Zen Spaces, and the account's passwords, bookmarks and history, leave the phone. Tabs you
  opened on the phone move to the **Phone** space. Logins saved on the phone stay.

---

## 2. Spaces

### How Zen maps to Koan

- Each Zen Space is a Koan space, with its name and icon. Its colour only tints the space's pill
  in the switcher and its small icons; cards and pages stay neutral.
- A Space's **open tabs** are the cards in its stack (Zen's "sync normal tabs" must be on for
  them to come over).
- Its **essentials** and **pinned tabs** (with Zen folders) live in the **drawer**.
- A Zen **split view** is one card in the stack.
- Tabs you open on the phone go in the current space and stay on the phone.

### Switching spaces

- **In the overview:** swipe sideways on the dock, or tap a space icon in the switcher row. One
  light tick per switch.
- **In a page:** long-press the address pill (a tick: "space mode", the pill shows the current
  space), then slide sideways without lifting: you scrub through your spaces, a tick per space.
  Lift on a space to open its current tab (or its overview if it has none). Lift without sliding to
  cancel.

### Essentials, pinned tabs and folders (the drawer)

- Open the overview, then swipe up on the dock (or tap the small handle at its top).
- The drawer has two heights: **compact** (about half the screen) and **full** (when there's more
  to show). Swipe up for full, down for compact, down again to close. A tap above it or Back closes
  it too.
- Top: the essentials grid (4 per row, all of them). Below: pinned tabs and Zen folders. Tap a
  folder to fold or unfold it (remembered on the phone only). Tap a tab to open it.
- At the bottom: the sync status row (tap it to sync now, or to connect / sign in again).
- An essential or pinned tab you open also gets a card in the stack while it's open. Closing that
  card only unloads it; its row in the drawer stays.

---

## 3. The tab overview

- Open it: swipe up on the dock in a page, or tap the tab count.
- **The depth stack:** your tabs as cards, newest at the bottom (closest to your thumb), older ones
  shrinking into a pile above. The overview opens centred on your current tab.
- **Scroll:** drag up and down in the card area; flings coast.
- **Open a tab:** tap the card in front. Tap a card in the pile or the one peeking below to bring it
  to the front.
- **Close a tab:** swipe the front card sideways, or tap its ✕. A pinned tab or essential's card is
  only unloaded (see above).
- **Reorder:** long-press a card, drag it, drop it. Within the same space only.
- **New tab:** the **+** in the header, or tap the dock's search field ("Search or type URL"). The
  search sheet opens right away. An empty space shows a button for it too.
- **Back to the page:** swipe down on the dock, swipe down on the header, tap **‹** in the header,
  or Back.
- Up to 4 tabs stay live; the others are saved and reload where they were when you open them.
  Cards show a screenshot of the page; tabs that were never opened on the phone show their site's
  icon instead.
- Koan reopens on your last space and tab after being closed or killed by the system.

---

## 4. Gestures

One rule on the dock: **swipe up = one level deeper, swipe down = one level back.**

```
Page  ⇄  Overview  ⇄  Drawer (compact)  ⇄  Drawer (full)
     up →          up →                 up →
     ← down        ← down               ← down
```

- Everything follows your finger and settles with a spring. A step happens past about a quarter of
  the way, or with a quick flick; otherwise it springs back. One light tick per step.
- **Previous / next tab (in a page):** swipe the address pill left or right. The page and the
  neighbour tab's screenshot follow your finger. Keep swiping to go through several tabs; the tab
  you stop on loads. At the first and last tab it rubber-bands, no wrap-around.
- **Space mode:** long-press the pill, then slide (see [Switching spaces](#switching-spaces)).
- **The dock hides** while you scroll a page down and comes back when you scroll up or a new page
  loads.

### Back

- In a page: back through the page's history, then to the overview.
- In the overview: back to the current tab.
- Any sheet, card or panel (menu, drawer, a prompt, Settings, Downloads): Back closes it, or goes
  back one page inside it.
- The search field and find in page: one Back (the gesture or the key) closes both the keyboard
  and the field.

---

## 5. Search

### The search sheet

- Tap the address pill: the address is selected, ready to type over.
- As you type, results appear above the field, and the page behind dims. From the field upward:
  1. **Suggestions** from your search engine (one row of chips).
  2. **Switch to tab:** up to 2 open tabs, in any space. A tab in another space shows that space's
     name.
  3. **History & bookmarks:** up to 2 pages, ranked by how often and how recently you visited them.
     A page that's already open shows only as its tab.
- What matches: under 3 characters, only the start of a site's address (`sa` → samsung.com). From 3
  characters, also the start of a word in the page's title.
- **Inline completion:** the field completes the best matching site as you type (`git` →
  `github.com — GitHub`). Keep typing to replace it, or Backspace to remove it.
- **Go** on the keyboard: opens the completed page (switching to its tab if it's open). Otherwise
  it opens what you typed as an address, or searches it.
- Tap a result to open it; tap a suggestion to search it. Tap the dimmed page to close.

### Search engines

- **⋯ › Settings › Search engine:** DuckDuckGo (default), Google, Brave Search or Startpage.
- **Settings › Search suggestions** turns suggestions off entirely. They come from the chosen
  engine; if it's slow or unreachable you simply get none, and local results never wait for it.

### @ shortcuts

- Type **@** in the field: a picker shows **Search with** (DuckDuckGo `@ddg`, Google `@g`,
  Wikipedia `@wiki`, YouTube `@yt`) and **Search in** (Tabs, History, Bookmarks).
- Tap one, or type it: `@wiki koan` searches Wikipedia, with that engine's own suggestions.
  `@history docs` searches only your history (up to 8 results).
- Wikipedia searches in your phone's language.

---

## 6. Page tools

### The ⋯ menu

Tap **⋯** in a page. It opens as a sheet at half height; swipe up for all of it, down to close.

- **Top row:** back, forward, the address (tap to edit it), copy link, reload / stop.
- **New tab**, **Bookmark** (saved on the phone; the star fills), **Share**, **Find**.
- **Site settings** (see below), **Desktop site** (toggle), **Reader view**, **Add to Essentials**,
  **Downloads**, **Settings**, and the **uBlock Origin** card.

### Find in page

- ⋯ › **Find**. The field replaces the address row; matches are highlighted in blue (the current
  one stronger) with a count like `3/12`.
- Enter or ↓ goes to the next match, ↑ to the previous one. You can tap the page to hide the
  keyboard and keep using ↑ / ↓.
- ✕ or Back closes it.

### Reader view

- ⋯ › **Reader view** shows the article as plain text, in Koan's theme. The row says "Available"
  when the page can be shown that way, "Not available" otherwise.
- ⋯ › **Leave reader view** goes back. The address stays the article's.

### Add to Essentials

- ⋯ › **Add to Essentials** puts the page in the current space's essentials (in the drawer).
  **Remove from Essentials** takes it out again.
- For a tab that came from Zen, Koan adds a phone-only copy (Zen's own essentials aren't changed).
  An essential that came from Zen shows "In Essentials · From Zen" and can't be removed here.

### Site settings

- ⋯ › **Site settings** opens inside the menu: the site, whether the connection is secure, and
  what it may do.
- **Permissions:** everything you allowed or blocked for this site, each with a toggle (on =
  allowed). Something it never asked for isn't listed.
- **Reader view** toggle.
- **Clear site data:** asks first, then removes the site's cookies, storage, caches and sign-in
  sessions (you'll be signed out of it), and forgets its permissions so it asks again. Saved
  passwords are not touched. The page reloads.

### When a site asks for something

- Location, camera, microphone, protected content (DRM), persistent storage, cross-site cookies
  and local network access show a card: **Block**, **Always allow**, **Allow once**.
- Block and Always allow are remembered for the site (change them in Site settings). Allow once
  holds for this time only; dismissing the card blocks it this time only.
- For location, camera and microphone, Android also asks for its own permission the first time.
  For camera and microphone this happens before Koan's card.
- Web notifications are refused without asking: Koan can't show them yet.
- Video and audio with sound don't autoplay; silent autoplay is allowed.

### Long-press on a link or an image

A light tick and a card showing what you pressed (the image, or the link's site, title and
address):

- **Link:** Open in new tab, **Open in background** (a toast "Opened in background · Switch"; tap
  Switch within 3 seconds to go to it), Copy link, Share link.
- **Image:** Save image, Open image in new tab, Copy image link.

### Desktop site

⋯ › **Desktop site** asks the site for its desktop version, for this tab.

---

## 7. Downloads

- A download starts as soon as a page offers a file, without asking. A small ring in the address
  pill shows its progress. Tap the ring for Downloads.
- When it's done: a toast with the file's name, size and site, and **Open**. A failed download says
  so, with **Retry**.
- A link that only downloads a file (from another app, or opening a new tab) doesn't leave an empty
  tab behind.
- Downloads keep going with Koan in the background. Android shows their progress as a notification
  (Koan asks for the notification permission the first time).
- **Downloads page** (⋯ › Downloads, the ring, or Settings › Downloads):
  - **In progress:** progress, pause / resume, cancel (or retry when it failed).
  - Finished downloads by day. Tap one for **Open**, **Share**, **Delete** (deletes the file).
  - **Search downloads** at the bottom, and **Clear** to empty the list (asks first; the files stay).
  - The chip at the top shows the folder; tap it for Android's own downloads view.
- **Where files go:** the phone's **Download** folder. To use another folder: Settings › **Save
  downloads to** › **Choose a folder…** (Android's folder picker).

---

## 8. Passwords and autofill

- Passwords from your Mozilla account are copied to the phone, read-only. Usernames and passwords
  are encrypted with an Android Keystore key; only the site is stored in the clear.
- **Filling a login:** tap a login field on a site you have logins for. Your accounts for that site
  appear as chips above the keyboard (usernames partly hidden). Tap one to fill. The key at the
  start of the strip opens the full list: pick one, then **Use** (or **Not now**).
- **Saving a new login:** after you sign in somewhere new, a card offers **Save in Koan**. It's
  kept encrypted on the phone only and isn't added to your Mozilla account. The next visit offers
  it like the others. Saving the same site and username again updates it.
- Android's own autofill service (for example Google Password Manager) is kept out of pages, so a
  login isn't offered to be saved twice.
- **Settings › Passwords:**
  - The list shows the sites only, until you **Unlock** with your fingerprint or the device PIN.
    Tapping a site while locked asks for it too, then opens that login.
  - After unlocking: usernames, a search field, and each login's page with **Reveal** for the
    password, and **Copy** for the username or password. A copied password is marked sensitive so
    Android hides it in clipboard previews.
  - Leaving the Passwords pages locks them again.
- Logins from your account can't be edited or deleted in Koan, and phone-only logins can't be
  deleted yet either.

---

## 9. History and bookmarks

- Koan records the pages you visit, on the phone only. Your account's history (5000 most recent
  pages) and bookmarks are added after each sync.
- Bookmark the current page with ⋯ › **Bookmark** (on the phone only).
- They're used by the search sheet, and `@history` / `@bookmarks` search only them.
- There's no history or bookmarks screen yet: use search.
- Signing out removes your account's history and bookmarks; what you visited and bookmarked on the
  phone stays.

---

## 10. uBlock Origin

- Koan installs uBlock Origin from addons.mozilla.org on its first start (this needs internet
  once). If you later turn it off or it's removed, Koan doesn't reinstall it.
- **The menu card** shows how many requests it blocked on the current page, and turns it on or off
  for every site.
- **Tap the card** for uBlock's own panel, inside the menu: the big power button turns it off for
  this site only, plus its counts and tools (element zapper, picker, dashboard).
- **Settings › uBlock Origin:** on/off, and how many requests it blocked over the last 7 days. If
  it isn't installed, the row offers **Install**.

---

## 11. Settings

⋯ › **Settings** (it slides up over everything; ‹ or Back closes it).

| Setting | What it does |
|---|---|
| Account card | Connect (signed out), or open the account page: **Sync now**, **Sign out** |
| Search engine | DuckDuckGo, Google, Brave Search, Startpage |
| Search suggestions | On by default; off = no suggestions at all |
| Theme | Light, Dark, Follow system (pages follow it too) |
| Default browser | Opens Android's choice to make Koan the default browser |
| Passwords | See [Passwords and autofill](#8-passwords-and-autofill) |
| uBlock Origin | On/off, blocked this week, or Install |
| Downloads | The Downloads page |
| Save downloads to | The phone's Download folder, or a folder you pick |
| About Koan | Version, update check, links to the changelog, the privacy notice and the open-source licences |

**As the default browser:** links you open in other apps, web searches, and text or links shared to
Koan open as a new tab in your current space.

**Update check:** opening About looks once at Koan's GitHub releases and says **Up to date** or
**Update available** (tap it to open the release page and download the APK). Koan never checks in
the background. If the check fails (no network), tap the chip to try again.

**Privacy:** what Koan sends and to whom is in the [privacy notice](PRIVACY.md).

---

## 12. Themes

- **Light:** neutral greys and white. **Dark:** dark greys. **Follow system** by default.
- The space's colour only appears on its pill in the switcher and on small space icons.
- Pages that support it follow the theme (light / dark color scheme), from the next load.

---

## 13. Known limitations

- **Read-only sync:** nothing is written back to Zen or your Mozilla account. Tabs opened, closed or
  moved on the phone stay on the phone; Zen desktop never sees them. Logins saved on the phone stay
  on the phone.
- **No editing of Spaces:** no creating, renaming or reordering spaces, no moving a tab to another
  space, no editing folders. Card reordering works within a space only.
- **Unofficial sign-in:** see [Signing in](#signing-in-with-a-mozilla-account).
- **Android 14+ and arm64 only.**
- **APK size (~210 MB):** almost all of it is GeckoView, the same engine as Firefox. Its native
  libraries are stored uncompressed so Android can load them straight from the APK instead of
  keeping a second, unpacked copy.
- **Not there yet:** web notifications, a history / bookmarks screen, deleting saved logins,
  other add-ons than uBlock Origin. Opening and closing a tab is a simple
  crossfade (the motion polish comes later).
- **Not affiliated** with Zen Browser or Mozilla. Zen is an independent project; "Firefox" and
  "Mozilla" are trademarks of the Mozilla Foundation.
