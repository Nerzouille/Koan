# Beta.2 bugs

The bugs for 1.0.0-beta.2, in one list: two from GitHub issues (triaged 2026-11-20) and the ones
noted in `docs/ROADMAP.md` › For 1.0.0-beta.2 and `docs/HANDOFF.md`, then the phone-run findings and
the beta.2 features (items 9 to 17; ROADMAP keeps the owner's wording of each feature).

## Status (2026-11-25)

A fresh session can work from this file alone. Read `CLAUDE.md` first (rules), then this file;
`docs/plans/PLAYER_TRACKS.md` holds the player research; `docs/ROADMAP.md` › For 1.0.0-beta.2 has
the owner's wording of each feature.

- **Branch:** `beta2-bugs`, rebased on `dev` (60a2199, passkeys PR #16 merged), local, not pushed.
- **The POCO's real Koan** (`dev.nerzouille.koan`) runs a `beta2-bugs` dev build (installed for
  item 1's kill test), not beta.1. Koan Test (`.staging`) has fake tabs on public pages.
- **The owner commits by default.** Give the exact `git add` / `git commit` commands.
- **Marked "needs the owner"** below: don't start those alone; prepare what they need and stop.

### Every item

| # | Item | State | Commits | Needs the owner |
|---|---|---|---|---|
| 1 | Private after a kill | **Done.** Fixed twice: 86002a7, then 28abd52 (the space pager also starts from the saved normal space's index, `PrivatePlaceStartupTest`). Phone OK 2026-11-25 on the real Koan: kill and cold start both reopen the second space. | 86002a7, 28abd52 | No |
| 2 | Translation state reset per page | **Done.** Fixed, `TranslationResetTest`. Phone OK 2026-11-25 (Koan Test): other-language page in the same tab, Back, same-page anchor, reload. | 821c633 | No |
| 3 | Find 0/0 | **Done.** Fixed, `FindCountTest`. Phone OK 2026-11-25 (Koan Test): counts, next / previous, 0/0, clear, new tab closes Find. A match only in a collapsed section not reproduced (French Wikipedia shows sections expanded). | 32048e6 | No |
| 4 | Download ring check | Fixed, `DownloadRingStateTest`. Phone check not done. | db0a692 | **Yes:** downloads on the phone, watching the ring |
| 5 | Missing download files | Fixed, `CheckedDownloadFilesTest`, `MissingFilesTest`; rows greyed in Downloads. Phone check not done. | c10b599, e0d9d18 | **Yes:** file manager, notification |
| 6 | Drawer height (#11) | **Built (2026-11-26), owner picks.** Both options behind `DrawerTokens.PAGE_HEIGHT` (`Tallest` = A, default; `Full` = B), `SheetStateTest`. Captures: `reference/beta2-choices/drawer-height-11.png` (one space only: Koan Test is signed out). | e150cae | **Yes:** A or B |
| 7 | Dock comes back (#9) | **Built (2026-11-26).** Dead zone `PageTokens.DockDeadZone` = 28 dp (0 = old behaviour), `DockDeadZoneTest`. | ee76f68 | **Yes:** feel check on a release build, tune the number |
| 8 | Save image (Google Images) | Not reproduced; more checks and a `KoanPageImage` log (extension 1.1). Fix or drop after the phone check. | a9f2841 | **Yes:** Google Images on the phone, fix-or-drop call |
| 9 | Translation card never auto-opens while Find is open | **Done (night 2026-11-25).** `translateCue(…, findOpen)`, `TranslateCueTest`. Phone check not done. | d5a9a4e | No (check in the list below) |
| 10 | An empty Find field clears all highlights | **Done (night).** Cause: the find display flags were set on every find, which makes Gecko re-highlight its *last* search asynchronously; now once per session, plus a second clear 350 ms after an empty field. `FindCountTest`. Phone check not done. | d58e642 | No (check in the list below) |
| 11 | Player checks (Commons stream or WebM, Commons `<track>`s, Vimeo tracks) | **Not run (night):** the POCO's network failed every page load in Koan Test (Gecko `OnLoadError`, the system's own push service timing out too). Probe patch kept in `reference/night-2026-11-25/player-probe.patch` (gitignored). | none | No |
| 12 | Player tracks (web Captions and Quality; standalone Audio / Subtitles; side files, option A) | **Done (night), untested on a phone.** The Commons `<track>` fallback was not built (waits for item 11). | 78735f2, 7731ee6, d07dbbf | No (checks below) |
| 13 | Translate icon stays active after translating; download ring shares its slot | **Done (night).** `TranslateCue.Active`, filled disc, tap = ⋯ › Translate page; ring slot was already there, now `pillEndSlot` + `PillEndTest`. | 166f4c5 | No (checks below) |
| 14 | Rotate row in the player's ⋯ menu | **Done.** Only with auto-rotate off, like the pill (owner, 2026-11-26). | b062a27, 83a76b2 | No |
| 15 | Passkey failure message | **Done as option C (owner):** a Known limitation in FEATURES, no code. | 35a2398 | No |
| 16 | Depth cue (A, B, C or A + B) | **Done: A (owner, 2026-11-26), stronger in dark towards #121214.** Back cards, header and thumbnail alike, recede into `DimColor`: light `#B9B9C2` (×1.1, max 0.62), dark `#121214` = Base (×1.4, max 0.85); Private the same on its own bases. `DepthDimTest`. Capture: `reference/beta2-choices/depth-A-dark-final.png`. | see Day run 2 | No (light check by hand) |
| 17 | End of phase: R8 release build, kill and restore | **Partly done (night):** full suite 493 green; R8 **staging** build (Koan Test) launched, `am kill` in the background, restored on the same tab, no crash. The R8 build of the real app id and the release from `main` wait for the release steps. | none | No |

**Also needs the owner:** Lock private tabs is **off** in the POCO's real Koan (turned off for the
kill tests, which are done): remind the owner to turn Settings › Lock private tabs back on.

**Needs the owner, in short:** items 4, 5, 8 (their hands on the phone), 6 (#11 drawer height:
picks an option), 7 (#9 dock feel, release build), Lock private tabs back on.
Everything else can run alone.

Last full JVM run: 460 green after item 8, then `MissingFilesTest` +3. Rerun the full suite after
item 1's second fix and at the end of the phase.

### Phone checks left (POCO, staging build)

Items 1 to 3 passed on 2026-11-25; their steps are in the item sections below.

`./gradlew assembleStaging`, `adb install -r app/build/outputs/apk/staging/app-staging.apk`
(own data: set up fake tabs on public pages first).

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
| 6 | Drawer height (#11) | **Built (2026-11-26), owner picks.** Both options behind `DrawerTokens.PAGE_HEIGHT` (`Tallest` = A, default; `Full` = B), `SheetStateTest`. Captures: `reference/beta2-choices/drawer-height-11.png` (one space only: Koan Test is signed out). | e150cae | **Yes:** A or B |
| 7 | Dock comes back (#9) | **Built (2026-11-26).** Dead zone `PageTokens.DockDeadZone` = 28 dp (0 = old behaviour), `DockDeadZoneTest`. | ee76f68 | **Yes:** feel check on a release build, tune the number |
| 8 | Save image never waits for the full image (Google Images) | ROADMAP, Xiaomi | `engine/PageImages.kt`, `pageimage` extension |
| 9 | Translation card never auto-opens while Find is open | **Done (night 2026-11-25).** `translateCue(…, findOpen)`, `TranslateCueTest`. Phone check not done. | d5a9a4e | No (check in the list below) |
| 10 | An empty Find field clears all highlights | **Done (night).** Cause: the find display flags were set on every find, which makes Gecko re-highlight its *last* search asynchronously; now once per session, plus a second clear 350 ms after an empty field. `FindCountTest`. Phone check not done. | d58e642 | No (check in the list below) |

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

- **Found** (phone run 2026-11-25): with Find open, a reload brought the translation offer card back
  on its own, over the Find bar, hiding the counter until the card was closed.
- **Want:** while Find is open, the card never opens on its own. The translate icon in the pill
  still offers it; tapping the icon still opens the card.
- **Look at:** the card's auto-open in `ui/browser/TranslateCue.kt` and the overlays in
  `ui/KoanOverlays.kt` (gate the auto-open on Find being closed).
- **Check:** Find open on a page in another language, reload: no card, counter visible, icon in the
  pill. Close Find, open another page in that language: the card opens as before.

## 10. An empty Find field clears all highlights

- **Found** (phone run 2026-11-25): deleting the search text letter by letter left a highlight from
  an earlier, shorter query ("zz" in "jazz") once the field was empty. The counter went away.
- **Want:** an empty field clears every highlight on the page (and the counter, as today).
- **Look at:** what Koan sends to Gecko on an empty query (`engine/PageTools.kt`, next to item 3's
  `GeckoFinder.find`): probably nothing, so the last highlight stays. Clear the matches instead.
- **Check:** type a word, delete it letter by letter: no highlight left. Select all and delete;
  type again; close Find.

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

## Night report 2026-11-25

Unattended run on `beta2-bugs` (plan: `docs/plans/NIGHT_PLAN.md`). Phone: the POCO, **Koan Test
(`.staging`) only**; the real Koan was not touched. The POCO's network failed every page load
during the run (see item 11), so no feature could be checked on a page.

### Items

- **9. No auto card while Find is open: done, d5a9a4e.** Tested: `TranslateCueTest` (Find open →
  icon, never the card; a phone language → nothing). Untested: on the phone.
- **10. Empty Find field: done, d58e642.** Cause found in GeckoView 160's own finder code
  (`GeckoViewContent._displayMatches`): `setDisplayFlags` on every find made Gecko highlight its
  *previous* search again, asynchronously, so a highlight could land after the empty field's
  clear. Now the flags are set once per session (`GeckoFinder`), and an empty field clears again
  350 ms later if nothing was typed since (`FindQueries`, `FindCountTest`). Untested: on the phone.
- **11. Player checks: not run.** The probe build (throwaway log patch, never committed) was
  installed on Koan Test, but every page failed to load (Gecko `OnLoadError` after ~30 s, even
  Koan Test's existing fake tabs; `ping` worked but at ~440 ms and the system's push service
  kept timing out). Likely the phone's own network setup, not Koan. Koan Test now has the night's R8 build, without the probe. To run it later:
  `git apply reference/night-2026-11-25/player-probe.patch`, staging build, `adb logcat -s
  KoanProbe`, play a public Commons video with subtitles and a public Vimeo video, then
  `git restore` the two files. Never commit the patch.
- **12. Player tracks: done in code, 78735f2, 7731ee6, d07dbbf; untested on a phone.**
  - ⋯ menu rows can open a choice list (back row, checked option, scrolls past six).
  - **Web Captions** (78735f2): the extension lists `video.textTracks` (subtitles / captions),
    sets the chosen one to `hidden`, sends its cue text; Koan draws it above the bottom pill
    (lifted with the controls, layer only). The track the page had on stays on; the language
    picked last comes back on the next video until Koan restarts; Off is remembered too. The
    page's own modes come back when Koan lets go. Extension 1.6.
  - **Web Quality + Download video choice** (7731ee6): one file list from the page (the video's
    plain `<source>`s, else the page's renditions in one folder, plus MediaWiki's original when
    linked). Quality shows only when the video plays a plain file that is in that list, with the
    files of the same family (WebM / MP4); switching keeps the time, speed and play state.
    Download video becomes a choice ("1080p · WebM", "Original · WebM") when there are two or
    more files. Extension 1.7. No new network call (no HEAD request for sizes).
  - **Standalone Audio / Subtitles** (d07dbbf): `MediaPlayer` audio and timed-text tracks
    (three-letter languages named), a same-name `.srt` / `.vtt` next to a downloaded video
    (looked up through the download's own folder; shown at once), and ⋯ › Subtitles › Choose a
    file… (system picker, no permission; the file is copied as SubRip into the cache, cleared at
    the next video). `.vtt` → SubRip converter unit tested.
  - **Skipped (depends on item 11):** rebuilding Commons' `<track>`s from its markup if its
    player removes them. Captions on Commons may not show until that's known.
  - Only in-band *timed text* (3GPP / SubRip) is listed in the standalone player, not
    `MEDIA_TRACK_TYPE_SUBTITLE` tracks (WebVTT / CEA in MKV / MP4): the platform only draws
    those through its hidden subtitle controller, and Koan can't get their text.
- **13. Translate icon stays active: done, 166f4c5.** Translated page → filled disc (Primary,
  no brand gradient); a tap opens the ⋯ menu straight on Translate page (Show original, the
  languages). The ring already took the icon's slot; now a tested `pillEndSlot`. Tests:
  `TranslateCueTest`, `PillEndTest`. Untested: on the phone.
- **14. Rotate row: done, b062a27.** Web and standalone, always shown (owner's answer). Found and
  fixed on the way: the standalone player ignored the rotate switch with auto-rotate on (it
  asked for SENSOR); now the other shape until the phone is held that way, as the web player
  does. Tests: `FullscreenOrientationTest`. Untested: on the phone.
- **15. Passkey failure message: stopped, no commit.** From GeckoView 160's classes: on Android
  14+ a passkey goes through Android's Credential Manager when it can (`WebAuthnCredentialManager`,
  errors mapped to `ABORT_ERR` for a user cancel, `NOT_SUPPORTED_ERR`, `UNKNOWN_ERR`), else through
  Play Services' *privileged* FIDO2 client (`WebAuthnTokenManager`, the route #16 wired through
  `startIntentSenderForResult`). In both, the error goes straight back to the page: Koan gets no
  callback, and a user cancel and a refusal can look the same. Telling them apart needs a phone
  run with a password manager (the owner's account: not allowed alone). Options:
  - **A.** A small content script that watches `navigator.credentials.create/get` rejections
    (the error name only) and shows the toast on `UnknownError` / `NotSupportedError`. Touches
    every site's passkeys: needs a careful check.
  - **B.** In `WebAuthnController`, read the FIDO2 error in the result intent (Play Services
    route only).
  - **C.** No toast; a line in FEATURES / the CHANGELOG's Known limitations.
- **17. End of phase: partly done.** Full JVM suite: **493 tests, 0 failures**. R8 staging build
  (`assembleStaging`, Koan Test) installed on the POCO, launched, sent to the background,
  `am kill`, reopened: same tab back, no crash in logcat. Not done: an R8 build of the real app
  id (that's the release step from `main`).

### Questions for the owner

1. Item 15: option A, B or C?
2. Item 11: run the checks yourself (probe patch above), or let the next session do it once the
   POCO's network works?
3. The Download video choice shows a list as soon as a page offers two files: fine, or only
   when the sizes differ a lot?

### Test by hand (one checklist)

Build: `./gradlew assembleStaging`, `adb install -r app/build/outputs/apk/staging/app-staging.apk`
(Koan Test; it already has the night's build). Public pages only.

- [ ] **Find + translation (9):** a page in another language, open Find, reload: no card, the
  counter stays visible, the translate icon is in the pill and its tap opens the card. Close Find,
  open another page in that language: the card opens as before.
- [ ] **Empty Find (10):** type "jazz" on a page that has it, delete letter by letter: no
  highlight left when the field is empty. Select all + delete, type again, close Find.
- [ ] **Translated icon (13):** translate a page: the icon stays, in a filled disc; tap it: the
  ⋯ menu opens on Translate page; Show original: the icon goes back to normal. Start a download
  on a translatable page: the ring takes the slot, the icon comes back after the check.
- [ ] **Web captions (12):** a fullscreen video with subtitle tracks (a plain `<video>` with
  `<track>`s, a public Commons file with subtitles, an hls.js test page): ⋯ › Captions lists Off
  + the languages; pick one: text above the bottom pill, lower when the controls hide; Off;
  leave fullscreen: the page's own captions as before; next video: the same language comes on.
- [ ] **Web quality (12):** a page whose video has several plain `<source>`s (Commons: does it
  play a WebM or the DASH stream?): ⋯ › Quality only when a choice exists; switching keeps the
  time and play / pause. ⋯ › Download video lists the files and saves the one picked.
- [ ] **Standalone (12):** a downloaded MP4 with two audio tracks: ⋯ › Audio switches the sound.
  Download a `.srt` (or `.vtt`) with the same name as a downloaded video: it shows at once,
  ⋯ › Subtitles lists it; Off; Choose a file… with a `.vtt` from the file manager.
- [ ] **Rotate row (14):** web and standalone, auto-rotate on and off: ⋯ › Rotate turns to the
  other shape; with auto-rotate on, turning the phone that way then follows the phone again.
- [ ] **Commons captions:** if they don't show on Commons, item 11 says why (its player may
  remove the `<track>`s): tell the next session.
- [ ] Still from before: items 4, 5, 8 (downloads, file manager, Google Images), 6 and 7 (feel),
  16 (depth), and **Lock private tabs back on** in the real Koan.

### Branch state

```
b062a27 feat(player): a Rotate row in the ⋯ menu, auto-rotate on or off
166f4c5 feat(translate): the translate icon stays, filled, on a translated page
d07dbbf feat(player): Audio and Subtitles in Koan's own player, with .srt / .vtt files
7731ee6 feat(player): Quality on a page's video, and a file choice for Download video
78735f2 feat(player): captions on a page's video, from the ⋯ menu
d58e642 fix(find): an empty Find field leaves no older highlight on the page
d5a9a4e fix(translate): no translation card on its own while Find in page is open
2af8410 docs(plans): night plan for the beta.2 items that don't need the owner's hands
```

Plus this report's commit. Nothing pushed, nothing merged.

To review: `git log --oneline f85e686..beta2-bugs`, `git diff f85e686..beta2-bugs -- app/`, one
commit at a time with `git show <hash>`. To merge once the checks pass (after the owner's items,
or now for what's done): `git switch dev && git merge --no-ff beta2-bugs`, then
`git push origin dev`.

### The phone

POCO: Koan Test has the night's R8 staging build (extension 1.7) with 2–3 extra test tabs on a
public Commons file page (the ⋯ menu has no "close tab"; close them in the overview). The real
Koan was not installed over or opened. DND was already on and is left on. Pages failed to load
on its network during the run: check its connection before item 11.

## Day run 2026-11-26 (owner away, remote instructions)

Same branch, `beta2-bugs`. Phone: the POCO, **Koan Test only**, local demo pages served from the
computer through `adb reverse` (the POCO's VPN was off: no public pages). Commit trailers: none.

### Items

1. **Rotate row only with auto-rotate off** (owner's choice): done, 83a76b2. The standalone
   player's rotation fix stays. Test: `FullscreenOrientationTest`.
2. **Passkey failure: option C**: done, 35a2398 (FEATURES › Known limitations; CHANGELOG too).
3. **Download video list only when files really differ** (resolution or format; copies of the
   same rendition don't count, the original counts as its own): done, 5524921, `VideoFilesTest`.
   Otherwise the plain row saves the best file directly, as before.
4. **Owner's choices, prepared:**
   - **Drawer height (#11):** both options built, e150cae; `DrawerTokens.PAGE_HEIGHT` picks
     (default `Tallest`). Captures: `reference/beta2-choices/drawer-height-11.png` (today, A, B).
     **Limit:** Koan Test is signed out, so it has a single space: the captures show A vs B on
     one space (A looks like today there; B opens at full height). Comparing two spaces of
     different sizes needs a signed-in copy: not done (no account touched).
   - **Depth cue:** `reference/beta2-choices/depth-cue-light.png` and `depth-cue-dark.png`, five
     side by side (as built, A recessed tint, B shadow + scale, C blur, A + B), on Koan's own
     Theme setting; single shots `depth-{light,dark}-{0..4}.png`. In dark the differences are
     small (as the roadmap said); in light A shows best. Prototype: local branch `capture-proto`
     (never merge), switched by a file in Koan Test's folder (`debug.*` properties are denied to
     apps at target SDK 37).
   - **Dock dead zone (#9):** ee76f68, `PageTokens.DockDeadZone` = 28 dp, the one number to tune
     on a release build (0 = the old behaviour). `DockDeadZoneTest`.
5. **Self-review of `dev...beta2-bugs`:**
   - Fixed: the item-10 change (find display flag once per session) could lose "highlight all"
     on a new page (a new page can get a new Gecko finder, which reads `findbar.highlightAll`):
     Koan now sets that pref too, e374da7. A doubled KDoc.
   - **CI would have failed:** detekt had 51 new findings and Android lint 6 errors on the branch
     (from the earlier commits and the night's): all fixed without touching the baselines,
     a2fe2c3 (wrapped lines, `NativeTracks` split out of `NativeVideo`, smaller functions, a
     composition read of a StateFlow, a missing `@OptIn`, two raw byte-order marks in Kotlin
     strings), 5b64eed. Both pass now.
   - Bigger, listed only: the video extension rebuilds the file list (`querySelectorAll`) every
     250 ms while a video is fullscreen (cheap, but could be cached per `<video>`); FEATURES.md
     doesn't describe the new player rows, the translated icon or the search morph yet (release
     step 1); the Downloads "Moved or deleted" card reuses the confirm card's Cancel slot for
     "Remove from list" (works, but a dismiss by drag does nothing, by design).
6. **CHANGELOG and release notes:** the beta.2 section is drafted in `CHANGELOG.md` (348b6cf,
   9575451); release notes draft in `reference/release-notes-1.0.0-beta.2.md` (gitignored),
   crediting zy6p for #16. Not published.
7. **From the owner's notes:**
   - **Search typing bug: fixed, 678cfe6.** Reproduced with fast `adb input text`: "localhost"
     came out "localhst" (and "…/sunset" lost "et") only when inline completion kicked in. Cause:
     the completion replaced the field's text while keys were still arriving, and the next key
     landed on the older text. Now the completion is drawn after the text (highlighted, then the
     page's title), never typed in: Go opens text + completion, Backspace first removes the
     completion, moving the cursor takes it in. `SearchFieldTest`. After the fix, 6 fast-typing
     runs kept every letter. Frames while typing (R8 Koan Test): median 9 ms, p99 14 ms, 3 of 52
     frames missed: the "lag" was the lost letters, not slow frames.
   - **Copy link text:** 6efbd56, shown only when the link has words, `LinkTextTest`.
   - **Search morph:** 7c3909c. The pill opens into the search field (its background clipped
     from the pill's bounds to the field's, the buttons beside it step aside, the field's contents
     come in over the second half) and closes back into it; one value read in layers only
     (`BarTokens.SearchMorph`). Recording: `reference/beta2-choices/search-morph.mp4` (Koan Test,
     a local demo page).
   - **DESIGN_SYSTEM rule** "prefer morphs, as long as they stay light" and the morph's spring in
     the motion table; **ROADMAP › After beta.2:** Private's gradient sweep (transition untouched).
     8e4286b.
   - **Payments research:** `docs/plans/PAYMENTS_RESEARCH.md` (a80dd27). In short: GeckoView has no
     Payment Request API (so many shops hide Google Pay, not fixable); Google Pay's fallback is a
     popup, which Koan opens as a full tab (could become a card; check that closing it returns
     to the checkout); Revolut / bank apps come back through the return URL as a *new* tab
     (fixable: reuse the tab that sent the link to the app, small).
8. **End checks:** full JVM suite **508 green**; detekt and lint pass; R8 Koan Test build
   (`assembleStaging`) installed, launched, `am kill` in the background, reopened on the same tab,
   no crash.

### Questions for the owner

1. Drawer (#11): A (tallest space) or B (always full)? Change `DrawerTokens.PAGE_HEIGHT` if B.
2. Depth cue: answered, A (Day run 2).
3. Dock dead zone: is 28 dp right on the release build?
4. Payments: start with "return to the same tab" after beta.2?

### Test by hand (one checklist, replaces the night's)

Koan Test already has the latest build (R8). Your real Koan has not been touched.

- [ ] **Search typing:** type fast in the search field on a site you've visited (inline
  completion shows): every letter stays. Backspace removes the grey completion first; Go opens
  the completed address; tapping in the text takes the completion in.
- [ ] **Search morph (rebuilt, Day run 2):** tap the pill: it rises with the keyboard and grows
  into the field in the same motion; Back: the reverse, down with the keyboard. Watch for a flash
  or a step back (release build, both phones; on the Xiaomi, HyperOS's keyboard slide).
- [ ] **Dock (#9):** on a long page, scroll down, then a tiny scroll up or a tap: the dock stays
  away; a real scroll up brings it. Tune `PageTokens.DockDeadZone` if needed.
- [ ] **Drawer (#11):** with your spaces (very different pinned counts), swipe between spaces in
  the drawer: no height jump (option A). Pick A or B.
- [ ] **Copy link text:** long-press a link with words: Copy link text copies the words.
- [ ] **Find + translation (9):** a page in another language, open Find, reload: no card, the
  counter stays, the icon opens the card.
- [ ] **Empty Find (10):** type a word, delete it letter by letter: no highlight left; then open
  another page and search: every match is highlighted (the pref fix).
- [ ] **Translated icon (13):** translate a page: filled icon; tap: Translate page; Show original.
  A download on that page: the ring takes the slot, the icon comes back after.
- [ ] **Web captions / Quality / Download choice (12):** needs public pages (VPN on): a video with
  subtitle tracks (⋯ › Captions), a page with several renditions (⋯ › Quality, the time kept),
  Download video's list only when the files differ.
- [ ] **Standalone (12):** a downloaded MP4 with two audio tracks (⋯ › Audio), a same-name
  `.srt` / `.vtt` next to a downloaded video, Choose a file….
- [ ] **Rotate row (14):** auto-rotate off: ⋯ › Rotate in both players; auto-rotate on: no row.
- [ ] **Item 11 player checks** once the VPN is on (probe patch in
  `reference/night-2026-11-25/player-probe.patch`).
- [ ] Still from before: items 4, 5, 8 (downloads, file manager, Google Images), the depth pick,
  and **Lock private tabs back on** in the real Koan.

### Branch state

```
9575451 docs(changelog): search typing, Copy link text and the search morph in beta.2
5b64eed refactor(bar): AddressRow under detekt's limits again after the search morph
a80dd27 docs(plans): why Google Pay and Revolut Pay checkouts struggle in Koan, and what could be fixed
8e4286b docs: prefer light morphs (design system), the search morph, Private's sweep after beta.2
7c3909c feat(search): the dock's pill opens into the search field and closes back into it
6efbd56 feat(links): Copy link text in the link long-press menu
678cfe6 fix(search): typing fast no longer loses letters (the completion is drawn, not typed in)
348b6cf docs(changelog): draft of the 1.0.0-beta.2 section
a2fe2c3 refactor: beta2-bugs passes detekt and lint again (wrapped lines, NativeTracks, smaller functions)
e374da7 fix(find): every page highlights all matches, with the display flag set once
e150cae fix(drawer): one height across spaces, the tallest one by default (#11)
ee76f68 fix(dock): a hidden dock waits for a real scroll up before it comes back (#9)
5524921 fix(player): Download video lists files only when they really differ
35a2398 docs(features): a failed passkey says nothing in Koan, as a known limitation
83a76b2 fix(player): the Rotate row shows only with auto-rotate off, like the pill
```

On top of the night's commits (`git log --oneline f85e686..beta2-bugs`). Local branch
`capture-proto` = beta2-bugs + the depth / drawer prototype, captures only, never merge. Nothing
pushed or merged.

To review: `git log --oneline f85e686..beta2-bugs`, `git show <hash>`. To merge when ready:
`git switch dev && git merge --no-ff beta2-bugs && git push origin dev`.

### The phone

POCO: Koan Test has the latest R8 build, its theme back on Dark (it was switched to Light for the
captures), ten local demo tabs ("Coral Bay"…"Night Owl", made-up pages, now unreachable since the
demo server is stopped) and the earlier Commons test tabs; one demo tab was closed by a test.
`adb reverse` removed, the prototype's switch file deleted, DND left as found (on). The real Koan
was not touched.

## Day run 2 (2026-11-26, owner's feedback on the morph and the depth pick)

1. **Search morph, rebuilt.** The first version was a relay: a spring of its own ran before the
   keyboard came, so the field widened, its ring showed, it shrank back to the pill, the address
   appeared and only then did it rise. Now:
   - **One shape.** The field draws the only background, from the pill's bounds to its own; the
     pill draws none while the field is there. The ring fades in with the shape (the "@" ring
     takes over only once it has landed).
   - **Driven by the keyboard.** `KeyboardSlide` reads the keyboard's own slide (the system's
     WindowInsetsAnimation, through Compose's insets) in layers: the morph's value is how far up
     the keyboard is. The dock rises with it as a layer translation (`keyboardRise`): it leaves on
     the slide's first frame and lands with the keyboard (no relayout per frame any more). No
     keyboard within 600 ms (a hardware one): the old spring.
   - **Cross-fade.** The host fades out while the address fades in, over the whole motion, riding
     the shape's left edge; back and tabs / ⋯ fade and step aside at the same time, ✕ comes in.
   - Back is the reverse, following the keyboard down.
   - `KeyboardSlideTest` (the dock never below the keyboard's top, leaves on the first frame; the
     shape only grows on the way up).
   - **Checked frame by frame** on Koan Test (R8, POCO, 120 Hz screenrecord): opening, the
     keyboard slides in ~6 frames there and each frame is a blend (shape between the pill and the
     field, both texts at partial alpha, buttons half faded); closing, ~24 frames, the same in
     reverse; no frame steps back to a smaller or larger size. Files:
     `reference/beta2-choices/search-morph.mp4` (full rate), `search-morph-slow4x.mp4`,
     `search-morph-open-frames.png`.
2. **Depth cue A** only (B dropped), stronger in dark: see item 16. Checked in dark on Koan Test;
   light is unchanged from the A capture.
3. End checks: full JVM suite **512 green**; detekt passes; lint has no new errors.
4. **"@" picker on close** (93d29ea, owner's report): closing the field with "@" typed (✕ or
   Back) cut the "Search with" panel off at once, and reopening flashed it for a frame (the
   sheet read the last editing's "@" before the field started again). Now the panel fades out
   with the dim (untouchable meanwhile), and the sheet shows nothing from the last editing until
   the field starts (`SearchField.current`, `SearchFieldTest`). Checked frame by frame on Koan
   Test: fades over ~8 frames on close, no frame of it on reopening.
