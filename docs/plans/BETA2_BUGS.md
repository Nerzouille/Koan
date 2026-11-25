# Beta.2 bugs

The bugs for 1.0.0-beta.2, in one list: two from GitHub issues (triaged 2026-11-20) and the ones
noted in `docs/ROADMAP.md` › For 1.0.0-beta.2 and `docs/HANDOFF.md`. The beta.2 features (depth
cue, Rotate row, video quality, translate icon and ring slot, passkeys) stay in ROADMAP.

## Status (2026-11-25)

A fresh session can work from this file alone. Read `CLAUDE.md` first (rules), then this file;
`docs/plans/PLAYER_TRACKS.md` holds the player research; `docs/ROADMAP.md` › For 1.0.0-beta.2 has
the owner's wording of each feature.

- **Branch:** `beta2-bugs`, rebased on `dev` (60a2199, passkeys PR #16 merged), local, not pushed.
- **The owner commits by default.** Give the exact `git add` / `git commit` commands.
- **Marked "needs the owner"** below: don't start those alone; prepare what they need and stop.

### Every item

| # | Item | State | Commits | Needs the owner |
|---|---|---|---|---|
| 1 | Private after a kill | Fixed twice: 86002a7, then 28abd52 (the space pager also starts from the saved normal space's index, `PrivatePlaceStartupTest`). Recheck the kill test on the POCO. | 86002a7, 28abd52 | No (staging kill test) |
| 2 | Translation state reset per page | Fixed, `TranslationResetTest`. Phone check to finish; see new finding A. | 821c633 | No |
| 3 | Find 0/0 | Fixed, `FindCountTest`. Phone check to finish; see new finding B. | 32048e6 | No |
| 4 | Download ring check | Fixed, `DownloadRingStateTest`. Phone check not done. | db0a692 | **Yes:** downloads on the phone, watching the ring |
| 5 | Missing download files | Fixed, `CheckedDownloadFilesTest`, `MissingFilesTest`; rows greyed in Downloads. Phone check not done. | c10b599, e0d9d18 | **Yes:** file manager, notification |
| 6 | Drawer height (#11) | Not started. Two options to show on the POCO. | none | **Yes:** picks the option |
| 7 | Dock comes back (#9) | Not started. Dead zone to try. | none | **Yes:** feel check on a release build |
| 8 | Save image (Google Images) | Not reproduced; more checks and a `KoanPageImage` log (extension 1.1). Fix or drop after the phone check. | a9f2841 | **Yes:** Google Images on the phone, fix-or-drop call |
| 9 | Find: translation card never auto-opens while Find is open | New finding, not started. | none | No |
| 10 | Find: an empty field clears all highlights | New finding, not started. | none | No |
| 11 | Player checks (Commons stream or WebM, Commons `<track>`s, Vimeo tracks) | Not run. | none | No (staging, public pages) |
| 12 | Player tracks (web Captions and Quality; standalone Audio / Subtitles; side files, option A) | Scope approved; coding after item 11. | none | No (results go into PLAYER_TRACKS.md first) |
| 13 | Translate icon stays active after translating; download ring shares its slot | Not started. | none | No |
| 14 | Rotate row in the player's ⋯ menu | Not started. | none | Hide with auto-rotate on, or keep: ask |
| 15 | Passkey failure message | Unblocked (#16 merged). Not started. | none | No |
| 16 | Depth cue (A, B, C or A + B) | Prototype on `depth-cue-proto`. | none | **Yes:** picks from screenshots |
| 17 | End of phase: R8 release build, kill and restore | After everything else. | none | No |

**Also needs the owner:** turn Settings › Lock private tabs back on, on the phone, once the kill
tests are done.

Last full JVM run: 460 green after item 8, then `MissingFilesTest` +3. Rerun the full suite after
item 1's second fix and at the end of the phase.

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
11. **Player research checks** (`docs/plans/PLAYER_TRACKS.md`), staging, public pages only: a
   Commons video in fullscreen, the extension's view of `currentSrc` (stream `blob:` or a WebM)
   and whether `<track>`s are still on the `<video>`; a public Vimeo video, its `<video>`'s
   `textTracks`. Results into PLAYER_TRACKS.md, then wait for the owner's OK before coding.
17. **End of phase:** R8 release build launched on the POCO, process death and restore.

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
| 9 | Translation card never auto-opens while Find is open | Phone run (owner) | `ui/browser/TranslateCue.kt`, `ui/KoanOverlays.kt` |
| 10 | An empty Find field clears all highlights | Phone run (owner) | `engine/PageTools.kt`, `ui/KoanOverlays.kt` |

Items 6 and 7 need the owner's feel check (6: a choice between two options). Item 8 is the
least sure: it can end with the wait dropped. Items 9 to 17 are below item 8.

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

## 9. The translation card never auto-opens while Find is open

- **What** (new finding): with Find in page open, a page that would offer translation on its own
  never shows the card.
- **Look at:** how the card's auto-open in `ui/browser/TranslateCue.kt` is gated by other overlays
  (`ui/KoanOverlays.kt`). Decide with the owner only if the right behaviour is unclear (wait until
  Find closes, or open over it).
- **Check:** Find open, then open a page in another language; then close Find.

## 10. An empty Find field clears all highlights

- **What** (new finding): clearing the Find field removes every highlight. Expected behaviour to
  confirm against Firefox for Android before changing it.
- **Look at:** what Koan sends to Gecko on an empty query (`engine/PageTools.kt`), next to item 3's
  `GeckoFinder.find`.
- **Check:** find a word, clear the field, type again; close Find.

## 11. Player checks

Staging build, public pages only, with a throwaway staging-only log patch in the video extension
(no URLs, never committed):

- Commons in fullscreen: is `currentSrc` the DASH stream (`blob:`) or a WebM?
- Does Commons' player leave the `<track>`s on the `<video>`?
- A public Vimeo video: does its `<video>` carry `textTracks`?

Write the results into `docs/plans/PLAYER_TRACKS.md` ("Checks before building").

## 12. Player tracks

Approved scope (owner, 2026-11-20 and 2026-11-25), details in PLAYER_TRACKS.md › Approved scope.
Every row shows only when there's a real choice.

- **Web:** ⋯ › Captions (`video.textTracks`, mode `hidden`, Koan draws the cue above the bottom
  pill) and ⋯ › Quality (the page's plain files, one candidate list shared with Download video's
  quality choice). No audio tracks on the web.
- **Standalone player:** ⋯ › Audio and ⋯ › Subtitles for embedded tracks (`MediaPlayer`
  `getTrackInfo` / `selectTrack`, timed text). A `.srt` / `.vtt` next to the video, same name
  (`.vtt` converted to SubRip, unit tested).
- **Side files: option A**, ⋯ › Subtitles › "Choose a file…" with the system picker
  (`ACTION_OPEN_DOCUMENT`), for the files Koan can't read on its own. No all-files access.
- Commons' path (stream or WebM, tracks kept or rebuilt) depends on item 11.

## 13. Translate icon stays active; the download ring shares its slot

From ROADMAP (owner, 2026-11-13):

- Once the page is translated, the icon stays in the address pill in an active state (filled
  icon, no brand gradient); a tap opens the card with **Show original** and the language choice.
  Today `translateCue` returns `None` once `translated` is true (`ui/browser/TranslateCue.kt`).
- While a download runs, its ring takes the translate icon's place; the icon comes back when the
  ring is gone (`ui/bar/AddressRow.kt`).
- **Check:** translate, tap the icon, Show original; a download on a translatable page, then the
  icon back after the ring's check.

## 14. Rotate row in the player's ⋯ menu

A "Rotate" row that turns full screen to the other shape (portrait ↔ landscape) at any time, same
switch as the rotate pill (`PlayerHost.fullscreenRotated`), in the web and the standalone player.
Ask the owner whether to hide it when auto-rotate is on.

## 15. A message when a passkey request fails

#16 is merged, so this is unblocked. Today the page gets an abort error and Koan shows nothing.
Show a short Koan message, e.g. "Your password manager doesn't support Koan for passkeys yet"
(string in `values/strings.xml`). Start from #16's credential activity result handling
(b447e59). Check on the POCO with Google Password Manager, which rejects Koan today.

## 16. Depth cue

ROADMAP › For 1.0.0-beta.2: options A (recessed tint), B (shadow and scale), C (blur), prototype
on `depth-cue-proto`. Redo the dark screenshots with Koan's own Settings › Theme. The owner picks;
then build it and measure with `tools/perf`.

## 17. End of phase

Full JVM suite, then an R8 release build launched on the POCO, with process death and restore.
