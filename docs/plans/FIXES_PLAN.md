# Fixes plan (docs/FIXES.md, 2026-11-12)

Branch `fixes` from `dev`. One commit per item (or per step for F10), Conventional Commits. Targeted
unit tests per item, dev build installed on the POCO per item. At the end of each phase: full suite,
R8 release build installed on the POCO (launch, kill, restore), then **stop**: give the owner that
phase's phone checks and wait for their OK. Never merge into `dev` without it. No new dependency,
no Room migration.

Paths are under `app/src/main/java/dev/nerzouille/koan/`.

## Owner's answers (2026-11-12)

- **F7:** normal mode never shows private downloads. Private mode shows everything, with the mask on
  private rows, plus a **Private** pill that is only visible in Private and shows only private
  downloads. The address bar ring and the menu badge follow the same rule.
- **F2:** with the rotation lock on, fullscreen follows the video's shape, as it does today.
- **F10:** private search suggests private bookmarks plus normal ones; normal search never shows
  private bookmarks. Private bookmarks are kept until they are deleted (unstar or Clear browsing data),
  through restarts and after the last private tab closes. The private lock also asks when only
  private bookmarks exist. They are stored encrypted with Keystore and never synced.

## Order

Phase 1 covers the independent items: F9, then F7, then F8 (F8 builds on F7's count), then F10.
Phase 2 covers the video player: F3, F4, F1, F2, F6, F5. The gesture items come first. F1 and F2
both touch `Fullscreen.kt`, so F1 lands before F2. F5 comes last because it needs a repro first.

---

## Phase 1: independent items

### F9: "Select all" per day (History, Downloads)
- **Files:** `ui/kit/Selection.kt`, `ui/history/HistoryScreen.kt`, `ui/downloads/DownloadsScreen.kt`,
  new test `ui/kit/SelectionTest.kt`.
- **Approach:**
  - Add `addAll(keys)`, `removeAll(keys)` and `hasAll(keys)` to `Selection` (today `set()` replaces
    the whole set).
  - Add a kit `SelectionDayHeader(day, allChecked, onToggle)`: the `SettingsSection` text with a
    trailing "Select all" / "Deselect" text link (`TextLink`). It is used in place of
    `SettingsSection(day)` only while selection is active, so the header doesn't move when selection
    starts or ends.
  - Downloads: the day keys are `items.map { it.id }` (`DownloadsScreen.kt` `byDay` loop).
  - History: the day keys follow the same rule as `checkable` (pages, groups, and a group's pages only
    while expanded, never closed tabs). The link is hidden in `closedMode`, like the top Select all.
- **Risk:** low. The only catch is that History's keys must match `checkable` exactly, or a day would
  never count as "all checked".
- **Tests:** `SelectionTest` covers addAll / removeAll / hasAll / keepOnly.
- **Phone check:**
  - History: long-press a row, tap a day's "Select all", and the whole day is checked and the count
    updates; tap "Deselect".
  - With a group expanded or collapsed, the day's link still reads correctly.
  - With a closed tab checked, there are no day links.
  - Downloads: the same, and the top "Select all" still works.

### F7: private downloads only in Private
- **Files:**
  - `ui/KoanOverlays.kt` (`private` is already a parameter: forward it)
  - `ui/overlays/LibraryOverlays.kt`
  - `ui/downloads/DownloadsScreen.kt`
  - `ui/overlays/MenuOverlays.kt:139` (badge)
  - `ui/KoanRoot.kt:922-930` (ring)
  - new pure `ui/downloads/DownloadFilter.kt` + test.
- **Approach:**
  - One pure function `visibleDownloads(list, privateOn, privateOnly)`:
    - normal mode: `filterNot { it.private }`;
    - Private: everything;
    - with the pill on: only private ones.
  - The page, the menu badge and the ring progress all go through it (the ring and badge without the
    pill, i.e. a count rule only).
  - The **Private pill** sits in `headerTrailing` before the folder chip. It is the same chip shape
    (`DownloadTokens.ChipHeight`, `palette.Card`), with a selected look when on. It is only composed
    when Private is on. Its state is a `rememberSaveable` that resets when Private goes off.
  - Selection: `keepOnly` already drops rows that disappear when the filter changes.
- **Risk:** low. Watch for one thing: a download started in Private that finishes after leaving
  Private must not show the completion toast in normal mode with the file name. The toast comes from
  `components.downloadDone` (`KoanRoot.kt:932-947`), so it needs the same rule. Check this in the plan
  step.
- **Tests:** `DownloadFilterTest` covers the three modes and the count rule.
- **Phone check:**
  - Download a file in Private and one in normal.
  - Normal Downloads shows only the normal one; no ring or badge for a private download in progress.
  - Private shows both, masked; the Private pill shows only the masked one.
  - Turn Private off and on again: the pill is reset.
  - Finish a private download while in normal mode: no toast, ring or badge.

### F8: "done" animation on the address bar ring, tap opens Downloads
- **Files:** `ui/bar/AddressRow.kt` (`DownloadRing` :342-372), `ui/KoanRoot.kt` (progress + `downloadDone`),
  maybe `ui/kit/MotionKit.kt` for one token.
- **Approach:**
  - `KoanRoot` keeps a short-lived `justDone` flag (about 1.2 s) set from `downloadDone` when the
    status is COMPLETED (only for a download the F7 rule counts).
  - The ring is composed while downloading **or** while `justDone` is set. It takes a `done: Boolean`.
  - When `done` flips, the arc fills to 360°, then a ✓ (the `KoanIcons.Check` path) draws in by path
    trim and scales up slightly, then the whole ring fades and shrinks out.
  - All of it is done in `drawWithCache` / `graphicsLayer` with an `Animatable`, with no
    recomposition per frame (rule 9). `animationsOff` skips straight to the end.
  - The tap keeps `onClick` → Downloads during the whole window. That already works today while
    downloading; the hit area stays mounted during the check.
  - A failed download gets no check: the ring goes as today, and the Retry toast says it.
- **Risk:** low to medium. Two things to watch: the layout shift when the ring leaves (text padding
  `:297`) must stay animated, not jump; and two downloads finishing close together.
- **Phone check:**
  - Download a small file: the ring fills, the check draws, then it goes.
  - Tap the ring during the download, and again during the check: Downloads opens.
  - Download two files at once.
  - Animations off (developer option): no animation, the ring just goes.
  - The completion toast still shows.

### F10: private bookmarks (encrypted, never synced)
- **Files:**
  - new `privacy/PrivateBookmarks.kt` + `privacy/PrivateBookmarksTest.kt`
  - `privacy/PrivateVault.kt` (pull out a small `SealedFile(file, box)` for atomic sealed read/write
    used by both)
  - `engine/Components.kt`
  - `privacy/PrivateMode.kt:54` (`needsUnlock`)
  - `ui/overlays/MenuOverlays.kt:89,130,176`
  - `ui/search/SearchState.kt:80-81`
  - `ui/history/ClearDataCard.kt` + `ui/overlays/LibraryOverlays.kt:117-131`
  - `ui/privacy/PrivateParts.kt` (`PRIVATE_FACTS` wording)
  - docs `docs/PRIVACY.md`, `docs/FEATURES.md`, `docs/design/OVERVIEW_PRIVATE.md`
  - the manifest / backup rules: check `private/` is excluded.
- **Approach (steps, one commit each):**
  1. **Store.**
     - `PrivateBookmarks` holds a `StateFlow<List<PrivateBookmark(url, title, createdAt)>>` in
       memory.
     - It is sealed to `files/private/bookmarks.sealed` with `KeystoreBox("koan_private")` (the same
       key as the tabs; it is never destroyed).
     - The format is versioned JSON `{"v":1,"bookmarks":[…]}`.
     - It loads once per process (with `PrivateTabs.start`) and is written right away on each change
       (tmp + rename).
     - It is **never** touched by `PrivateTabs.wipe()`, never read by `LibrarySync` / `SyncWrite`,
       and not in Room (so a DB export or backup can't carry it).
     - A file that can't be opened (for example a key lost after a restore) reads as empty, as the
       tabs vault does.
  2. **Star in the ⋯ menu.**
     - On a private tab, the star reads and toggles the private set only.
     - On a normal tab, nothing changes.
     - This also fixes today's leak: a private-tab star currently goes into the normal table.
     - Bookmarks starred in Private before this fix can't be told apart and stay normal (say so in
       the changelog).
  3. **Search.**
     - In Private, the bookmark source is private matches (an in-memory filter, same LIKE rule) plus
       the normal `searchBookmarks` / `recentBookmarks`, deduped by URL with the private one winning.
     - Normal search is unchanged. Ranking is `Source.Bookmark` as today.
  4. **Lock.** `needsUnlock` = locked && (private tabs > 0 || private bookmarks > 0) &&
     `settings.privateLock`.
  5. **Delete.** Clear browsing data gets a "Private bookmarks" row. It is only shown when some exist,
     **off by default**. When run, it empties the set and deletes the file.
  6. **Docs.** `PRIVACY.md` (the sealed file, never synced, how to delete), `FEATURES.md`,
     `OVERVIEW_PRIVATE.md` (deviation), and the `PRIVATE_FACTS` line if its wording no longer fits.
- **Risk:** medium. The main points:
  - private data must never land in plain storage or in normal search;
  - the lock change must not ask for the fingerprint when Private is empty;
  - the restore path after process death.
  - No sync code is touched: `SyncWrite` never writes bookmarks anyway, and no new call site is added.
- **Tests:** `PrivateBookmarksTest` (with `AesGcmBox` as in `PrivateVaultTest`): round trip, bad or
  other-key file reads as empty, toggle, dedupe. Plus a pure search-merge test.
- **Phone check:**
  - Star a page in a private tab, then switch to normal: it's not starred and not suggested in normal
    search.
  - Back in Private: starred, and suggested.
  - Close every private tab, kill Koan, reopen Private: the lock asks; the bookmark is still there.
  - Clear browsing data → Private bookmarks: gone, file deleted (`run-as` is not available on a
    non-debuggable build, so check through the UI only).
  - A normal bookmark is still suggested in Private.

---

## Phase 2: video player (together; spec `docs/VIDEO_PLAYER.md`, update its "as built" notes)

### F3: double-tap to seek doesn't show the controls
- **Files:** `ui/player/VideoPlayer.kt:435-470`, `ui/player/PlayerTokens.kt:124`.
- **Approach:**
  - A tap in the **centre** zone toggles the controls at once, as today. Double-tap seek only lives in
    the side zones.
  - A tap in a **side** zone waits `TAP_PAIR_MS` before toggling. A second tap inside that window
    cancels the pending toggle and seeks.
  - Further taps within `DOUBLE_TAP_WINDOW_MS` keep adding 10 s, as today.
  - Remove the "undo" line that forced `shown = false`.
  - Keep 250 ms, and try 300 ms on the phone if pairs are missed.
- **Risk:** low to medium. The feel matters most: a single side tap now shows the controls 250 ms
  later. Judge it on a release build.
- **Phone check:**
  - Double-tap left/right, with controls hidden and shown, playing and paused: the video seeks and
    the controls don't flash.
  - A triple tap adds 20 s.
  - A single side tap shows the controls after a short delay; a centre tap shows them at once.
  - A long press still gives 2×.

### F4: edge swipes ignored by the player
- **Files:** `ui/player/VideoPlayer.kt:324-415` (the drag start), the `ProgressBar` drag (:977-1040),
  `ui/player/PlayerTokens.kt`.
- **Approach:**
  - Read `WindowInsets.systemGestures` (top/bottom; fall back to a 32 dp `PlayerTokens.EdgeBand` when
    0, as in immersive mode).
  - A press that starts inside the top or bottom band never starts a level or swipe-away drag, and
    never a long press. A tap there still toggles the controls.
  - The progress bar only starts a scrub after a mostly **horizontal** slop. A vertical move that
    starts on it is left to the system.
- **Risk:** low. On HyperOS the system takes about 80 dp at the top in landscape. Check that the band
  is big enough on the Xiaomi, but don't widen it blindly.
- **Phone check, portrait and landscape:**
  - Pull the shade from the top edge: no brightness change.
  - Swipe up from the bottom to go home: no volume change, no seek.
  - Normal level swipes in the middle still work, and so does scrubbing the bar.
  - Check on both phones.

### F1: Private comes back locked in landscape
- **Files:** `ui/root/PrivateSwitch.kt` (`show()` :216-237, `leave()` :252-256), `ui/browser/Fullscreen.kt:35-41`,
  `ui/player/WebPlayer.kt:50-54` (`leave()`), `ui/player/StandalonePlayer.kt:83-97`.
- **Cause (from the code):**
  - Fullscreen is a per-tab engine state. Leaving Private (here through an incoming link) only
    switches the shown tab, so the private tab stays `fullScreen`.
  - Coming back to Private makes it the active tab again, so `Fullscreen.kt` sets `SENSOR_LANDSCAPE`
    again. This also happens under the lock cover.
- **Approach:**
  - Whenever the shown mode changes, exit fullscreen on the tab being left, through the same path as
    the player's own exit (`pageVideo.release` + `exitFullscreen`). This is done in `PrivateSwitch.show`.
  - Belt and braces: `rememberFullscreen` treats fullscreen as off while the private lock cover is
    shown.
  - The standalone player restores `PORTRAIT` instead of a saved `before` that may be a fullscreen
    value.
- **Risk:** low. Exiting fullscreen also releases the page's video port; the video stays on the page,
  paused or playing as Gecko leaves it.
- **Phone check:**
  - The owner's repro: Private, fullscreen video, Home, open a link from another app, then switch to
    Private: portrait, no player, no lock left over.
  - The same with the lock elapsed (screen off) and with PiP in between.
  - The normal-mode version of the repro.
  - Kill Koan while fullscreen, then reopen.

### F2: fullscreen follows the rotation, portrait included
- **Files:** `ui/browser/Fullscreen.kt`, `ui/player/StandalonePlayer.kt:85`, new
  `ui/player/FullscreenOrientation.kt` + test.
- **Approach:**
  - One pure decision `orientationFor(videoPortrait, rotationLocked, userTurned)` plus a small effect
    used by both players.
  - **On entry**, use the video's shape (`SENSOR_LANDSCAPE` / `SENSOR_PORTRAIT`), as today.
  - **Rotation lock off** (`Settings.System.ACCELEROMETER_ROTATION == 1`, watched by a
    `ContentObserver`): an `OrientationEventListener` waits until the phone is physically in the
    forced orientation (the user turned it), then switches to `SCREEN_ORIENTATION_SENSOR`. From then
    on, turning to portrait goes portrait (YouTube-style).
  - **Rotation lock on**: stay on the video's shape, as today (owner's answer).
  - **On exit**: `PORTRAIT`.
- **Risk:** medium. The points to watch:
  - an orientation flip-flop at entry if the switch happens too early;
  - HyperOS's own rotation handling;
  - the listener must be released when fullscreen ends and in PiP (no change while in PiP).
- **Tests:** `FullscreenOrientationTest` (the decision table).
- **Phone check:**
  - With auto-rotate on: a landscape video goes landscape; turn to portrait and it follows (portrait
    fullscreen); turn back and it follows.
  - A portrait video, the same.
  - With auto-rotate off: as today.
  - Standalone player from Downloads: the same rules.
  - Exit fullscreen in any orientation: back to portrait.
  - Check on both phones.

### F6: "Download video" always in the ⋯ menu, greyed out when it can't
- **Today:** the row exists (`WebPlayer.kt:56-58`) but is hidden unless the URL ends in a known
  video extension.
- **Files:**
  - `app/src/main/assets/extensions/video/content.js:186-203`
  - `engine/PageVideo.kt:31-51` (`WebVideo`)
  - `ui/player/PlayerModel.kt:95-117` (+ `isVideoFile` HEAD check)
  - `ui/player/WebPlayer.kt`
  - the `MenuRow` / `PlayerMenu` in `ui/player/VideoPlayer.kt:660-683`
  - `PlayerModelTest`
- **Approach:**
  - The extension also sends `drm` (`v.mediaKeys != null`), `blob` (`currentSrc` starts with
    `blob:`), `live` (non-finite duration) and the raw source type.
  - A pure `downloadability(video)` returns `File(url)` / `NotDownloadable(reason)` / `Check(url)`.
    - `File` covers the extensions.
    - `Check` covers an http URL with no extension; it is resolved by the existing HEAD check, run
      when the menu opens.
    - `NotDownloadable` covers DRM, `blob:`/MSE, `.m3u8`/`.mpd` and live.
  - `MenuRow` gains `enabled` (dimmed, no click) and an optional subtitle ("Protected video" /
    "Streamed video, can't be saved").
  - The row stays in the web player only; there is no Download in the standalone player (it's
    already a file).
- **Risk:** low to medium. Changing `content.js` needs the built-in extension's version bumped so
  Gecko reloads it. Plain http videos on some sites need the page's cookies: they keep the current
  `referrerUrl` path.
- **Tests:** `PlayerModelTest` gets the cases for each reason.
- **Phone check:**
  - A plain `.mp4` test page: enabled, downloads, and the toast shows.
  - A YouTube video (MSE) and a DRM page (Widevine): greyed out with the right line.
  - A live stream: greyed out.

### F5: picture-in-picture when leaving the app in fullscreen, with a ⋯ toggle
- **Today:** auto-enter already exists (`engine/PictureInPicture.kt:105-161`, `setAutoEnterEnabled` on
  Android 12+, `Settings.pictureInPicture` default **on**, Settings row). It needs the selected tab
  fullscreen, with a known video size, **and playing**.
- **Step 0 (no code):** reproduce on the POCO and the Xiaomi with a release build. The owner's case is
  a fullscreen video, then Home. Find which condition fails:
  - paused;
  - a private tab;
  - size unknown on that site;
  - HyperOS ignoring auto-enter;
  - the eligibility being computed on `selectedTabId` instead of the shown mode's tab.

  `PictureInPicture.kt:56-59` reads the store's selected tab, which can be the wrong mode's tab after
  F1-type switches.
- **Approach:**
  - Fix the cause found in step 0. The expected one is to use the active (shown) tab, and to allow a
    paused-but-fullscreen video (enter PiP paused).
  - Add a ⋯ row "Picture-in-picture on leave" with a check, in the web and standalone player menus. It
    writes the **same** `Settings.pictureInPicture` that the Settings row writes (one setting, two
    places).
- **Risk:** medium. PiP while Private is locked must not show private content after the lock time: it
  is only allowed if the lock isn't due. Check that the Audio-only action still `finish()`es
  correctly.
- **Phone check:**
  - Fullscreen video (playing, then paused), Home or the app switcher: PiP.
  - Turn the ⋯ toggle off: no PiP, and Settings shows it off.
  - Private video: PiP, and the lock applies after.
  - Check on both phones; on HyperOS, check the PiP permission for Koan first.

---

## Decisions for the owner before coding

1. **F7 pill:** Downloads has no filter pills today. The header only has the folder chip. The plan
   adds the Private pill next to it, styled like it. OK, or do you want a real filter row (All /
   Private, later file types)?
2. **F5:** PiP on leave already exists and is on by default, so something in your case stops it. I'll
   find which condition before changing anything. The ⋯ toggle would be the same setting as Settings ›
   Picture-in-picture. OK? Should PiP also start for a **paused** fullscreen video?
3. **F10:** in a private tab, the star shows only private bookmarks (a page you bookmarked normally
   looks unstarred, and starring adds a private copy). Clear browsing data gets a "Private bookmarks"
   row, off by default. OK?
4. **F3:** taps in the side zones show the controls about 250 ms late, so that a double-tap never
   flashes them. Centre taps stay instant. OK?
5. **F2:** after you turn a landscape video to portrait, it plays in portrait fullscreen (letterboxed).
   Confirm that's what you want.

## End of the batch
Update `docs/FIXES.md` (status per item), `CHANGELOG.md` "Unreleased", `docs/FEATURES.md`,
`docs/PRIVACY.md` (F10), `docs/VIDEO_PLAYER.md` (as built), and `docs/HANDOFF.md`. Then run the full
suite and install the R8 release build on the POCO. Give the owner the checks and wait for their OK.
Merge `fixes` into `dev` only after that OK.
