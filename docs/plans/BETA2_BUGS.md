# Beta.2 bugs

The bugs for 1.0.0-beta.2, in one list: two from GitHub issues (triaged 2026-11-20) and the ones
noted in `docs/ROADMAP.md` › For 1.0.0-beta.2 and `docs/HANDOFF.md`. The beta.2 features (depth
cue, Rotate row, video quality, translate icon and ring slot, passkeys) stay in ROADMAP.

## Status (2026-11-20, later)

- **Branch:** `beta2-bugs`, from `dev` (6975314), local, not pushed.
- **Done and committed, phone checks still to do** (no phone was connected), one commit each:
  1. Private after a kill (6ffea26, `RootStatePrivateRestoreTest`).
  2. Translation state reset on each page load (c84d836): `engine/TranslationReset.kt`, a
     middleware that clears the tab's translation state when a load starts (not on a same-page
     link). `TranslationResetTest`.
  3. Find in page (3c75827). Cause, from GeckoView's `GeckoViewContent.sys.mjs`: Gecko answers
     `found = true` with a count of 0 when its match counter skips text the find reached (likely
     Wikipedia's collapsed sections); Android Components passes only the count. Find now reads
     GeckoView's own result (`GeckoFinder.find`), keyed by query and tab; a found but uncounted
     match shows no counter. `FindCountTest`.
  4. Download ring check (92c95c1). Cause: the store marks the download completed before the
     "stopped" broadcast; the running ring left, came back already done and was drawn at its
     faded-out end (`Animatable(1f)`). The finish is now read from the store update itself
     (`ui/downloads/DownloadRingState.kt`); the animation starts at 0. `DownloadRingStateTest`.
  5. Missing download files (2419d4e): `engine/downloads/CheckedDownloadFiles.kt` wraps
     `DownloadFileUtils.openFile`, used by Downloads, the toast's Open and the notification:
     "File moved or missing" (Koan's toast on screen, the system's from the notification with
     Koan in the background). `CheckedDownloadFilesTest`. Then (c47a2a2, owner's choice): when
     Downloads opens, finished files are checked once; a missing one is greyed with "Moved or
     deleted"; a tap offers Download again (same address and mode, replaces the row) or Remove
     from list (Undo). `MissingFilesTest`.
  8. Save image (e1c40b7): not reproduced. The page script now checks every image covering the
     pressed point (it missed images with `pointer-events: none`), and when it gives up, logs why
     (`adb logcat -s KoanPageImage`: `no-press`, `no-image`, `preview-only`, `not-loaded`, never an
     address). Fix or drop after the phone check. Extension 1.1.
- 460 JVM tests green.
- **Later: items 6 and 7.** 6 needs the owner to choose between two options, shown on the POCO;
  7 needs the owner's feel check on a release build.
- The owner commits by default. Give the exact `git add` / `git commit` commands.

### Phone checks (POCO, staging build, all in one go)

`./gradlew assembleStaging`, `adb install -r app/build/outputs/apk/staging/app-staging.apk`
(own data: set up fake tabs on public pages first).

1. **Private after a kill:** on a normal space other than the first, Private on, Home, `adb shell
   am kill dev.nerzouille.koan.staging`, reopen: normal mode on that space. Then swipe Koan away
   and reopen: the same space.
2. **Translation:** translate a page; open a page in another language in the same tab (sheet's
   "From" is right, icon shows); Back to the translated page; a same-page anchor link (stays
   translated); reload.
3. **Find:** a word on a Wikipedia article (also one only in a collapsed section), next and
   previous, a word with no match (0/0), clear the field, switch tab while finding.
4. **Ring:** a small and a large download with Koan in front: the ring fills, the check draws,
   it goes, once. Two downloads at once: the check for the first, the ring keeps going.
5. **Missing file:** download a file, delete it with a file manager, open it from Downloads, the
   toast's Open, the notification (Koan in front and in the background), a video from Downloads.
   In Downloads: the row is greyed, "Moved or deleted"; tap → Download again (a private one stays
   private, the old row goes) and Remove from list (Undo); swipe left and right still work; a
   file deleted while Downloads is open is only marked on the next opening.
8. **Save image:** Google Images, long-press the large preview at once, Save image; watch
   `adb logcat -s KoanPageImage` for the reason when it fails.
9. **End of phase:** R8 release build launched on the POCO, process death and restore.

Branch: `beta2-bugs`, from `dev`. Order: the items below, one commit each.
For each item, follow these steps:
1. Reproduce on the POCO (staging build, its own data).
2. Fix, with a JVM test where the logic allows it.
3. Check on a dev build.
4. Once all items are done: a full R8 release build, launched on the phone, with process death and
   restore checked.

| # | Bug | Source | Starts in |
|---|---|---|---|
| 1 | Private mode comes back on the first space after a kill | HANDOFF (audit) | `ui/KoanRoot.kt` |
| 2 | Translation state carried over to the next page | ROADMAP, Xiaomi | `ui/browser/TranslateCue.kt`, `ui/menu/TranslatePage.kt` |
| 3 | Find in page says 0/0 while it highlights a match | ROADMAP | `engine/PageTools.kt`, `ui/KoanOverlays.kt` |
| 4 | The download ring's check never shows | ROADMAP | `ui/bar/AddressRow.kt`, `ui/KoanRoot.kt` (`DONE_MS`) |
| 5 | A moved or deleted download fails silently | ROADMAP | `DownloadsScreen.open` |
| 6 | The drawer's height jumps between spaces | GitHub #11 | `ui/bar/SpaceDrawer.kt` |
| 7 | The dock comes back after the smallest scroll up | GitHub #9 | `ui/browser/DockState.kt` |
| 8 | Save image never waits for the full image (Google Images) | ROADMAP, Xiaomi | `engine/PageImages.kt`, `pageimage` extension |

Items 6 and 7 need the owner's feel check (6: a choice between two options). Item 8 is the
least sure: it can end with the wait dropped.

## 1. Private mode comes back on the first space after a kill

- **What:** Koan is killed (by the system in the background) while Private is on screen. When it
  comes back, it shows the first space instead of Private, and the saved place (the last normal
  space) is overwritten with the first space.
- **Likely cause:** `currentSpaceUuid` (saved instance state) comes back as
  `PrivateTabs.SPACE.uuid`. The `LaunchedEffect(spaceIds, placeRestored)` in `KoanRoot.kt` doesn't
  find that id among the normal spaces (`anchored = -1`), falls back to `currentSpace` (0), and
  snaps there. Then `currentSpaceUuid` becomes the first space, and the effect above saves it to
  `lastPlace`.
- **Fix:** Private is per process by design (`PrivateMode`: a new process opens normal mode,
  locked; FEATURES: "Koan opens in normal mode after a restart"). So a restored Private id goes
  back to the last normal space (`normalSpaceUuid`, saved when Private was switched on):
  `RootState.leavePrivatePlace`, called from the restore effect in `KoanRoot.kt`. A configuration
  change with Private still on keeps Private.
- **Check:** on a normal space other than the first, switch Private on, put Koan in the background,
  `adb shell am kill`, reopen: normal mode on that space. Then a cold start (swiped away): the same
  space. JVM test: `RootStatePrivateRestoreTest`.

## 2. Translation state carried over to the next page

- **What** (seen 2026-11-13 on the Xiaomi): after translating a page, a Chinese page opened in the
  same tab got a translate sheet saying "From English", and no translate icon showed in the pill. A
  fresh tab on the same page was right.
- **Fix:** reset the tab's translation state on each new page (each navigation, not each tab):
  `translated`, the detected source language and the offer.
- **Check:** translate a page, then open a page in another language in the same tab. Also test
  Back to the translated page and a same-page anchor link (which must not reset it).

## 3. Find in page says 0/0 while it highlights a match

- **What** (seen 2026-11-13): searching a word in a Wikipedia article, the bar said "0/0" while a
  match was highlighted.
- **Look at:** how the find result reaches the bar (`PageTools.kt` → `KoanOverlays.kt`). Is the
  count read before the result arrives? Or is it reset by a later empty result (from another frame
  or an older query)?
- **Check:** the same word on a Wikipedia article, next and previous, a word with no match, and
  clearing the field.

## 4. The download ring's check never shows

- **What:** the address bar's ring should fill and draw a check before it goes (`AddressRow.kt`,
  the `done` state; `KoanRoot.kt` keeps it for `DONE_MS`). Nothing shows at the end of a download.
- **Look at:** whether `done` is ever set. The finished download may drop out of the running list
  before the ring sees it. Also check on which pages it happens.
- **Check:** a small and a large download, with Koan in the foreground; the check shows once, then
  the ring goes.

## 5. A moved or deleted download fails silently

- **What:** opening a download whose file was moved or deleted must say so ("File moved or
  missing"), wherever it's opened from: Downloads, the ring, the toast, the notification.
  `DownloadsScreen.open` already has a "Couldn't open this file" toast for when `actions.open`
  fails, but it doesn't show. The open probably "succeeds" with a stale URI, and the other app
  fails instead.
- **Fix:** check that the file exists before opening it, and show the toast if not. Maybe mark such
  rows in the list too (ask the owner).
- **Check:** download a file, delete it with a file manager, then open it from each of the four
  places.

## 6. The drawer's height jumps between spaces (GitHub #11)

Reported on a Samsung Galaxy S24 FE, Android 17 (One UI 9), 1.0.0-beta.1. The reply to #11 promised
a fix in the next beta.

- **What:** swiping between spaces in the drawer, the sheet grows or shrinks with each space's
  number of essentials and pinned tabs, so the UI jumps.
- **Cause (from the code):** `ui/bar/SpaceDrawer.kt` measures each page at its natural height.
  While switching, the box is as tall as the tallest page; then `drawer.refit(heights, settled)`
  fits it to the settled page. The jump is that refit, once the swipe settles.
- **Fix, a design choice for the owner:** keep one height across spaces. Either the tallest space's
  height (capped at the full detent) for as long as the drawer is open, or always open at the full
  detent. Show both on the POCO before choosing.
- **Not part of this bug:** "add each space's gradient to the drawer" is a separate feature request.
- **Check:** two spaces with very different pinned counts, swipe back and forth, compact and full.
  No relayout per frame (rule 9).

## 7. The dock comes back after the smallest scroll up (GitHub #9)

Reported on a Samsung Galaxy S24 FE, Android 17 (One UI 9), 1.0.0-beta.1. The reply to #9 promised
only a closer look, no fix.

- **What:** after scrolling down a long page (dock hidden), the smallest scroll up, or even a tap on
  the page, brings the dock back.
- **From the code:** `ui/browser/DockState.kt` moves the dock 1:1 with the page's scroll
  (`scrollBy`), so any scroll up starts showing it. On lift, `settle` finishes in the scroll's
  direction once the move passed the touch slop, or else goes to the nearest end. A tap shouldn't
  move it: if it does, something else calls `show()` (the near-top check in `BrowserScreen.kt`, or a
  page that scrolls on tap). Reproduce first.
- **Fix to try:** a dead zone. The dock starts following an upward scroll only after about 24–32 dp
  of it (reset on finger down), and a tap never shows it. Keep "near the top of the page, it always
  shows".
- **Check:** a long article and a page with its own scrolling list: a tiny scroll up, a tap, a fling
  up. Feel check on a release build (rule 8), and drop the change if it feels worse.

## 8. Save image never waits for the full image (Google Images)

- **What:** on Google Images, saving before the full picture has loaded should wait for it and
  save the full image (1.0.0-beta.1 CHANGELOG, Improved). On the phone it never does.
- **Look at:** `engine/PageImages.kt` and the `pageimage` extension
  (`assets/extensions/pageimage/content.js`). Reproduce on the Xiaomi, and log what the extension
  reports.
- **Fix or drop:** if the wait can't be made reliable, drop it, save what's there, and remove the
  CHANGELOG claim in the beta.2 notes.
