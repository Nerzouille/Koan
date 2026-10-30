# Koan — user guide (v0.4.0)

Everything that works in this build, how to use it, and what isn't there yet. Koan is a browser
built on GeckoView (Firefox's engine). Its workspaces are your Zen Browser Spaces, read from your
Mozilla account. Koan is not affiliated with, endorsed by or connected to Zen Browser or Mozilla.

The bottom bar is called **the dock** in this guide. In a page it holds: back, the **address pill**
(the site's name), the **tab count** and **⋯** (the menu). In the overview the same row holds
**History** (clock), the search field, the tab count and **Settings** (gear).

---

## 1. Setup

### Install

- Requirements: Android 11 or newer (tested on Android 14 and 17; 11–13 are supported but less tested),
  a 64-bit ARM phone (arm64-v8a). Other CPUs are not built. Below Android 12 the drawer dims
  what's behind it instead of blurring it; phones with less memory keep fewer tabs live.
- Download `koan-v0.4.0.apk` (about 206 MB) from the GitHub release and open it. Android asks you to allow
  installing apps from that source (your browser or file manager) the first time.
- **Updates:** install with [Obtainium](https://github.com/ImranR98/Obtainium) from the GitHub
  releases (add `https://github.com/Nerzouille/Koan`) and it fetches new versions for you.
  Otherwise Settings › About checks once when you open it.
- Installing over a previous Koan version keeps your tabs, spaces, logins and settings.
- The APK is about 206 MB. See [Known limitations](#13-known-limitations).

### First run

- **The tour:** the first time Koan opens, a short tour slides up: a welcome card, then six
  cards with a looping clip each (your Zen Spaces, swiping up for your tabs, swiping sideways to
  switch tabs, holding the bar to change space, `@` search shortcuts, long presses). Swipe or tap
  **Next**; **Skip** or a swipe down on the sheet closes it, Back goes to the previous card. It
  shows once (also after updating from 0.2.x), and not when a link from another app opens Koan.
  Replay it from **Settings › About Koan › Take the tour**.
- **What's new:** after an update that brings new features, a short sheet of the same kind shows
  the highlights once: it opens on the tour's welcome look ("What's new in 0.4"), then one card
  per highlight (a clip, a screenshot or a small drawn animation), ending with
  **See the full changelog** (the latest GitHub release). **Settings › About Koan › What's new in
  0.4** shows it again.
- Without an account, Koan is a plain browser with one space called **Phone** (📱). The overview
  opens on a **Connect your Zen Spaces** card; you can ignore it and just browse.
- Tap the address pill, type an address or a search, and go.

### Signing in with a Mozilla account

- Where: the **Connect your Zen Spaces** card in the overview, the status row at the bottom of the
  drawer, or **⋯ › Settings › Connect your Zen Spaces** (the card at the top of Settings).
- Two ways in, chosen on the first page (each page has a small link at the bottom to switch to the
  other).
  Mozilla's pages follow Koan's theme (Android 13+):
  - **Email and password** on Mozilla's own accounts page, shown inside Koan. Two-step
    verification codes are asked there if your account uses them.
  - **Sign in from your computer:** on the computer, open Zen › Settings › Sync › **Connect
    another device**; on the phone, tap **From your computer** (Koan asks for the camera only
    then; if it was refused for good, "Allow the camera" opens Koan's app settings) and point it
    at the code, then confirm on the computer. **Paste the link instead**
    works too if you copied the code's link (Google Lens…), and when Koan is the default
    browser, scanning the code with the phone's own camera opens it in Koan directly.
- The session (email and password) or the long-lived token (from a computer) and the sync key are
  stored on the phone, encrypted with an Android Keystore key. Signing out revokes the token.
  There is no Koan server, no analytics, no telemetry.
- The sign-in identifies as Firefox desktop (email and password) or Firefox for Android (from a
  computer) to be let into Firefox Sync. This is unofficial: it can break when Mozilla changes
  something and may go against Mozilla's terms. Use it at your own risk.

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
  space), then slide sideways without lifting: you scrub through your spaces, a tick per space,
  the space under your finger highlighted. The steps fit your screen: sliding towards an edge
  always reaches the first or last space a little before the edge, however many spaces you have.
  Lift on a space to open its current tab (or its overview if it has none). Lift where you started
  to cancel.

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
- **Close a tab:** swipe the front card sideways (past about a third of its width, or a quick
  flick, at any speed or angle), or tap its ✕: it goes at once, the card on the
  side you last scrolled from comes forward, and a **Tab closed · Undo** toast follows (Undo puts
  it back in its place, with its page history). A pinned tab or essential's card is only unloaded
  (see above).
- **Mouse wheel or trackpad:** scrolls the stack.
- **Reorder:** long-press a card, drag it, drop it. Within the same space only.
- **New tab:** tap the dock's search field ("Search or type URL"). The search sheet opens right
  away. An empty space shows a button for it too.
- **History and Settings:** the clock (left) and the gear (right) on the dock. Long-press the clock
  for **Recently closed**.
- **Back to the page:** swipe down on the dock, swipe down on the header, tap **‹** in the header,
  or Back.
- Up to 4 tabs stay live; the others are saved and reload where they were when you open them.
  Cards show a screenshot of the page; tabs that were never opened on the phone show their site's
  icon instead.
- Koan reopens on your last space and tab after being closed or killed by the system.

### The start page

When you come back to Koan after not using it for a while (1 hour by default; Settings › **Start
page**: 30 min, 1 h, 4 h or Never), it opens on a start page instead of your last tab:

- the current space's **essentials** (tap one to open it);
- a **Continue** card with the tab you left (its icon, title and site): tap it to go back to it.
  It isn't there if that tab was closed meanwhile (in Zen, for example);
- the dock's search field, ready to type, keyboard up.

The start page isn't a tab: a tab only exists once you search or open something from it. Back, a
swipe down or the field's ✕ take you back to your tab, leaving nothing behind. Koan never shows it
when a link or a share from another app opened it.

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
- **Long presses on the dock** (each with a light vibration):
  - **the address pill:** space mode, then slide (see [Switching spaces](#switching-spaces)).
    Only the pill does this;
  - **back:** a small list of the pages before this one in the tab (icon and title, most recent
    first); tap one to go straight back to it;
  - **the tab count:** a small menu with **New tab**, **Close tab** (the next tab shows, with a
    **Tab closed · Undo** toast; a pinned tab or an essential goes back to its row) and **Reopen
    closed tab** (the last tab closed in this space, in its place and with its history; greyed out
    when there is none);
  - **⋯:** Settings;
  - **History** (the clock, in the overview): Recently closed.
- **The dock hides** while you scroll a page down and comes back when you scroll up or a new page
  loads. A page that can't scroll keeps it.
- **A page's own bottom bar** (a chat's message field, a search box, a cookie banner) sits right on
  the dock and follows it down as it hides, to the bottom of the page. Below the page there's a
  solid strip where Android's navigation bar is.
- **Typing in a page:** when you tap a field in the page, the dock steps aside and the page ends at
  the keyboard, so the field shows right above it. The dock comes back when the keyboard goes.
- **Pull to refresh:** at the top of a page, pull down: a small circle follows your finger; let go
  past the tick to reload, and it turns with Koan's gradient until the page has loaded. Pages that
  handle the touch themselves (maps, drawing apps, sites with their own pull) keep their own
  gesture.
- **Toasts** (the small bar at the bottom: "Tab closed · Undo", "Opened in background · Switch",
  a finished download): swipe one sideways or down to dismiss it early, without its action. A toast
  steps aside when a sheet, a card or a full page (History, Settings, Downloads) opens.

### Back

- In a page: back through the page's history first. When there's nothing left to go back to:
  - a tab another app opened (a link from a chat, an email…) closes, and you're back in that app;
  - a tab opened from a link in another tab closes, and you're back on that tab;
  - any other tab closes and the next one in the stack shows, with a **Tab closed · Undo** toast
    (Undo brings it back with its page history). After the space's last tab, the overview opens,
    with the same toast;
  - a pinned tab or an essential never closes: the overview opens, as before.
- A full screen video: Back leaves full screen first.
- In the overview: back to the current tab.
- On the start page: back to your tab (the keyboard goes with it).
- Any sheet, card or panel (menu, drawer, a prompt, Settings, History, Downloads): Back closes it,
  or goes back one page inside it.
- The search field and find in page: one Back (the gesture or the key) closes both the keyboard
  and the field.

---

## 5. Search

### The search sheet

- Tap the address pill: the address is selected, ready to type over. The page behind dims at once.
- A long address scrolls to keep the cursor in view, also while you drag the cursor handle to an
  edge; you can drag the text itself to scroll it.
- As you type, results appear above the field. From the field upward:
  1. **Suggestions** from your search engine (one row of chips, from the first letter, with the
     engine's icon next to its name).
  2. **Switch to tab:** up to 2 open tabs, in any space. A tab in another space shows that space's
     name.
  3. **History & bookmarks:** up to 2 pages, ranked by how often and how recently you visited them.
     A page that's already open shows only as its tab.
- What matches: under 3 characters, only the start of a site's address (`sa` → samsung.com). From 3
  characters, also the start of a word in the page's title.
- **Inline completion:** the field completes the best matching site as you type (`git` →
  `github.com — GitHub`). Keep typing to replace it, or Backspace to remove it. It only completes
  to open tabs, bookmarks and sites you visit often or recently, never to sign-in or redirect pages.
- **Go** on the keyboard: opens the completed page (switching to its tab if it's open). Otherwise
  it opens what you typed as an address, or searches it.
- Tap a result to open it; tap a suggestion to search it. Tap the dimmed page to close.

### Search engines

- **⋯ › Settings › Search engine:** DuckDuckGo (default), Google, Brave Search, Startpage or Baidu,
  and your own engines. Tap one to make it the default.
- **Add a search engine** (at the end of the list): a name, the search address with `%s` where the
  words go (`https://example.com/search?q=%s`, https only), an optional `@shortcut` (not one Koan
  already uses), and an optional suggestions address (`%s` too; empty = no suggestions). Each
  field says what's wrong as you type.
- Your engines work like the built-in ones: default search, `@shortcut` in the picker (with the
  site's icon), suggestions if they have an address for them. **Edit** on their row changes or
  deletes them (a toast confirms; Undo brings a deleted one back). Built-in engines can't be
  deleted. Deleting the default one makes DuckDuckGo the default again.
- **⋯ › Add this search engine:** on a page that declares a search engine (most self-hosted
  SearXNG instances, many sites), the menu offers it, with the form already filled in. Hidden when
  Koan already has an engine for that site.
- **Settings › Search suggestions** turns suggestions off entirely. They come from the chosen
  engine; if it's slow or unreachable you simply get none, and local results never wait for it.

### @ shortcuts

- Type **@** in the field: a picker shows **Search with** (DuckDuckGo `@ddg`, Google `@g`,
  Wikipedia `@wiki`, YouTube `@yt`, Baidu `@bd`) and **Search in** (Tabs, History, Bookmarks).
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
  **Add this search engine** (only on a page that offers one, see Search engines), **History**,
  **Downloads**, **Settings**, and the **uBlock Origin** card.

### Find in page

- ⋯ › **Find**. The field replaces the address row; matches are highlighted in blue (the current
  one stronger) with a count like `3/12`.
- Enter or ↓ goes to the next match, ↑ to the previous one. You can tap the page to hide the
  keyboard and keep using ↑ / ↓.
- The page ends above the find bar while it's open, so a match on the last line still shows.
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

### When a page asks you something

A page's questions open on Koan's floating card (swipe it down, tap outside or Back to cancel):

- **Drop-down lists:** tap an option to pick it (the current one is ticked, groups have small
  titles, a long list opens on the current option). For lists that allow several, tick them and
  tap **Done**.
- **Dates and times:** a calendar for dates (days outside what the page allows are greyed), the
  clock for times, the calendar then the clock for both, a year and its twelve months for a month,
  a calendar for a week. **Clear** empties the field.
- **Colours:** a set of swatches, or type a hex value.
- **Alerts, confirmations and questions** ("example.com says"); a text question opens with the
  keyboard up and the page's suggestion selected. When a page keeps opening them, the card offers
  **Don't let this page show more dialogs**: ticked, that page's dialogs stay quiet until the tab
  goes to another address.
- **Leave this page?** (unsaved changes), **site sign-in** (a username and password the site asks
  for itself), **wants to open a pop-up** (Allow / Block), **Send the form again?** (reloading a
  page you sent a form from).
- A page's **share** button opens Android's share sheet; a **folder** field Android's folder
  picker.
- **Printing** a page (its print button, or `window.print()`) opens Android's print dialog: print
  or save as PDF.

### Long-press on a link or an image

A light tick and a card showing what you pressed (the image, or the link's site, title and
address):

- **Link:** Open in new tab, **Open in background** (a toast "Opened in background · Switch"; tap
  Switch within 3 seconds to go to it), Copy link, Share link.
- **Image:** Save image, Open image in new tab, Copy image link.

### Desktop site

⋯ › **Desktop site** asks the site for its desktop version, for this tab.

### Links to other apps

Phone numbers (`tel:`), email addresses (`mailto:`) and links meant for an app (`intent://`, or
an app's own link) open in that app when you tap them. A page can't open an app by itself, only
when you tap. An app link with a web fallback opens the web page when the app isn't installed.

### Uploading a file

A site's "choose a file" button opens Android's picker: your files, photos and the camera (for
fields that take pictures or videos). A field that asks for a capture opens the camera directly.
Koan asks for the camera permission the first time. Closing the picker leaves the field empty.

### Full screen video

When a video goes full screen, the status bar, navigation bar and dock hide, and the screen turns
to the video's orientation (landscape, or portrait for a portrait video). Swipe from an edge to
see the system bars for a moment. Back leaves full screen.

### Audio and video in the background

A page playing sound shows a media notification, also on the lock screen: the page's title and
icon, play/pause, and next/previous when the site offers them (YouTube does). Playback keeps going
with Koan in the background or the screen off.

---

## 7. Downloads

- A download you start (tapping a file link or a download button) begins at once, without asking.
  One a page starts on its own (a countdown, a script, a background tab) asks first, on a card
  with the file's name, size and site: **Download** or **Cancel** (a tap outside or Back cancels).
  A small ring in the address pill shows the progress. Tap the ring for Downloads.
- When it's done: a toast with the file's name, size and site, and **Open**. A failed download says
  so, with **Retry**.
- A link that only downloads a file (from another app, or opening a new tab) doesn't leave an empty
  tab behind.
- Downloads keep going with Koan in the background. Android shows their progress as a notification
  (Koan asks for the notification permission the first time).
- **Downloads page** (⋯ › Downloads, the ring, or Settings › Downloads):
  - **In progress:** progress, pause / resume, cancel (or retry when it failed).
  - Finished downloads, one card per day (Today, Yesterday, then dates). Tap one for **Open**,
    **Share**, **Delete** (deletes the file).
  - **Search downloads** at the bottom, and **Clear** next to it to empty the list (asks first; the
    files stay).
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
  - One row per site, with a search field. A site with several accounts says how many ("3
    accounts"); once unlocked, a site with one account shows its username.
  - Tap a site: Koan asks for your fingerprint or the device PIN (once per visit), then shows all
    the site's accounts on one page: where each comes from, its username and its password (hidden).
  - Tap a username or a password to copy it; **Reveal** shows the password, **Hide** hides it again.
    A site with one account also has a big **Copy password** button. A copied password is marked
    sensitive so Android hides it in clipboard previews.
  - Leaving the Passwords pages locks them again.
- Logins from your account can't be edited or deleted in Koan, and phone-only logins can't be
  deleted yet either.

---

## 9. History and bookmarks

- Koan records the pages you visit, on the phone only. Redirect pages (the in-between steps of a
  sign-in, for example) are left out. Your account's history (5000 most recent pages) and bookmarks
  are added after each sync.
- **History page** (⋯ › **History**, or the clock on the overview's dock): the pages you visited,
  newest first, one card per day (Today, Yesterday, then dates). Pages from your desktop's history
  carry a small computer icon.
  - **Groups:** several pages of the same site visited one after another make one row: the site,
    "N pages · time range" and a chevron. Tap it to open the pages in place, again to fold them. A
    single visit stays a normal row; the same page twice in a row shows once.
  - Searching shows every matching page, without groups.
  - **Search history** at the bottom.
  - Tap a page to open it in a new tab.
  - Swipe a page left to remove it: a **Removed from history · Undo** toast follows. Swipe a group
    to remove all its pages at once. A page from your desktop is only hidden on the phone; your
    account isn't changed.
  - **Clear** (next to the search field) opens **Clear browsing data**: History (on), Cached images
    and files (on), Cookies and site data (off: it signs you out of most sites). Desktop history is
    only hidden on the phone.
- **Recently closed:** the last 25 tabs you closed (from the overview, the tab count's menu or
  Back) are at the top of the History page, or long-press the clock on the overview's dock for the
  whole list. Tap one: it comes back in its space and place, with its back and forward history,
  and opens.
- Bookmark the current page with ⋯ › **Bookmark** (on the phone only). There's no bookmarks screen
  yet: use search.
- History and bookmarks are used by the search sheet, and `@history` / `@bookmarks` search only
  them.
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

⋯ › **Settings**, or the gear on the overview's dock (it slides up over everything; ‹ or Back
closes it).

| Setting | What it does |
|---|---|
| Account card | Connect (signed out), or open the account page: **Sync now**, **Sign out** |
| Search engine | DuckDuckGo, Google, Brave Search, Startpage, Baidu |
| Search suggestions | On by default; off = no suggestions at all |
| Start page | After how long unused Koan opens on the [start page](#the-start-page): 30 min, 1 h (default), 4 h, Never |
| Default browser | Opens Android's choice to make Koan the default browser |
| Theme (Display) | Light, Dark, Follow system (pages follow it too) |
| Hide status bar (Display) | Off by default. On: pages use the whole screen up to the camera; swipe down from the top edge to see the status bar for a moment. The search sheet, the overview, menus, Settings and the other pages still show it |
| Passwords | See [Passwords and autofill](#8-passwords-and-autofill) |
| uBlock Origin | On/off, blocked this week, or Install |
| Downloads | The Downloads page |
| Save downloads to | The phone's Download folder, or a folder you pick |
| About Koan | Version, update check, **Take the tour**, links to the changelog, the privacy notice and the open-source licences |

**As the default browser:** links you open in other apps, web searches, and text or links shared to
Koan open as a new tab in your current space. So do links apps open "in the app" (Custom Tabs, as
in Discord or GitHub), and Back returns to the app.

**Search in Koan:** select text in any app and pick **Search in Koan** in the selection menu (it
may sit under ⋯, near the end). A link or an address opens as it is; anything else is searched
with your default engine, in a new tab of the current space. Back returns to the app.

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
- **Android 11+ and arm64 only:** tested on Android 14 and 17; 11–13 are supported but less tested.
- **Long screenshots:** HyperOS's scrolling screenshot doesn't scroll Koan's pages yet (a GeckoView
  update after 10 November should help).
- **Old WebView warning on sign-in:** shown when Android System WebView is older than 92 (it can't
  load Mozilla's sign-in page), with a Play Store button; not yet tested on such a phone.
- **APK size (~206 MB):** almost all of it is GeckoView, the same engine as Firefox. Its native
  libraries are stored uncompressed so Android can load them straight from the APK instead of
  keeping a second, unpacked copy.
- **Pull to refresh in some chats:** a site whose message list scrolls inside the page (instead of
  the page itself) may reload when you pull down at the top of the list.
- **Not there yet:** web notifications, a bookmarks screen, deleting saved logins, other add-ons
  than uBlock Origin, picture-in-picture, private tabs, adding a page to the home screen. Opening
  and closing a tab is a simple crossfade (the motion polish comes later).
- **Not affiliated** with Zen Browser or Mozilla. Zen is an independent project; "Firefox" and
  "Mozilla" are trademarks of the Mozilla Foundation.
