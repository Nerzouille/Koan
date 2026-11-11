# Fixes list: phone checks (branch `fixes`)

The 10 items of `docs/FIXES.md`, built from `docs/plans/FIXES_PLAN.md` on 2026-11-12. The R8 release
build is installed on the POCO. The checks are ordered riskiest first. Use demo pages or throwaway
tabs only. The video checks need a plain `.mp4` test page, one big streaming site, one DRM page
and one live stream.

Start with the **Recheck** section: the owner's second-round feedback (POCO, HyperOS 2), fixed on
`fixes`, riskiest first. The tiers after it are the first round, still to check.

## Recheck (round 2, 2026-11-12), riskiest first

0. **Play / pause stuck pressed** (`ca15f9a`, the last bug; tests `PressTest`,
   `PlayerTapsTest`).
   - **Causes found:**
     - `pressScale` (and the ⋯ menu rows' `pressable`) restarted their touch handler whenever
       the caller's click lambda changed. The play button gets a new one with every player state
       update, so a press that spanned an update was cut off: no click, and the pressed flag never
       reset.
     - While the video buffered, the spinner took the centre group out, so a press there was torn
       down, and a tap in the middle hid the controls instead.
     - A press could also start just as the controls began hiding.
   - **Now:**
     - one handler for a button's whole life;
     - the pressed state resets however a press ends;
     - the centre group stays under the spinner (only faded) and still plays / pauses;
     - controls take taps whenever they are on screen, while fading in or out too, and using one
       brings them back;
     - a finger on the player holds the auto-hide;
     - quick play / pause taps go by the last tap's intent, so two quick taps never both read the
       same stale "playing".
   - **Checked on the POCO (Commons, 4K stream), all reached the button and alternated:**
     - 8 slow presses (400 ms);
     - 5 quick taps (150 ms apart);
     - a tap 120 ms into the fade-in;
     - a tap 100 ms into the fade-out.
   - **Your checks:** the same, by hand, playing and paused, and while it buffers (the spinner):
     a tap in the middle pauses, and the button never stays small.

Claude reproduced #5 on the POCO with logcat and checked it there after the fix. Claude also
checked the menu morph, the PiP row and the tap behaviour on the POCO. Everything else below needs
your hands, especially anything that needs the phone turned: Claude can't rotate the phone.

1. **Private lock and PiP** (`cd71fe8`, your #3). A private video in PiP:
   - keep the window over 1 min, then tap it back to full screen: **no** lock;
   - close the window, wait over 1 min, open Koan: locked; come back within 1 min: not locked;
   - Audio only, wait over 1 min, open Koan: locked;
   - turn the screen off during PiP: locked on return.
   PRIVACY.md and FEATURES.md say this now.
2. **Download video** (`cf60787`, `7f7eb84`, `061beaa`, your #5).
   - **Cause:** Commons now plays its videos as a DASH stream (the element's address is a `blob:`).
     Koan classed it "Streamed video, can't be saved", but the menu rows lost their greyed-out
     state on the way into the player (#1's bug), so the row looked normal and its tap ran an
     empty action.
   - **Now:** when a video is streamed, Koan saves a plain file the page offers for it. It looks,
     in order, at:
     1. the video's own `<source>`s;
     2. the page's renditions of that one video;
     3. MediaWiki's original file, when the page links it;
     4. a page link to a file whose name the video carries.
   - **Checked on the POCO:** on Commons, Big Buck Bunny 4K picked the original 2.76 GB `.webm`.
     The player said "Big_Buck_Bunny_4K.webm · Downloading · 2.8 GB", and the download ran (28 %
     in Downloads; Claude cancelled it there).
   - **Your checks:**
     - the same page, and a plain `.mp4` test page: the notice in the player, and the file in
       Downloads;
     - a big streaming site and a DRM page: greyed out with the reason;
     - airplane mode just after the tap: "Couldn't download the video" in the player;
     - a file already there: the player leaves fullscreen and the duplicate card shows.
3. **Rotation with auto-rotate off** (your #7, second version, `f6803d8`).
   - **Before building:** the system limit is confirmed in AOSP's `DisplayRotation.java`. Its rotate
     button is offered only for USER_* / FULL_USER requests:
     - USER_LANDSCAPE doesn't flip the landscape sides by itself under the lock;
     - FULL_USER goes back to the locked portrait;
     - SENSOR_LANDSCAPE flips the sides, but never gets the button.
   - **Built:**
     - fullscreen keeps SENSOR_LANDSCAPE / SENSOR_PORTRAIT;
     - turning the phone to the other shape shows Koan's round rotate pill for about 3 s, exactly
       where the fullscreen button is; while the controls show it sits above the bottom pill and
       moves with it (`7dade9c`);
     - a tap turns the player; doing nothing keeps it.
   - **Checks, auto-rotate off:**
     - a landscape video: turn the phone over (one landscape side to the other) and it flips with no
       button;
     - turn it towards portrait: the pill fades in. Tap it and the player turns to portrait. Turn
       the phone towards landscape: the pill again, and a tap turns it back;
     - leave it: it fades out after about 3 s and nothing turns;
     - turn back before it goes: it goes at once;
     - with the controls hidden it sits on the fullscreen button; tap the video to show them and
       it rises above the bottom pill, then goes back down as they hide;
     - leave fullscreen after rotating: back to portrait; the next fullscreen opens in the video's
       shape again;
     - the standalone player from Downloads: the same;
     - in PiP: no pill.
   - **Auto-rotate on:** no pill ever; the player follows the phone as before.
4. **Hidden controls and taps** (`0f601af`, your #8; test `PlayerTapsTest`).
   - With the controls hidden, tap where ⋯, lock, the play button or the progress bar would be:
     the controls show, and nothing else happens.
   - With them shown, every button works.
   - Double-tap a side: it seeks without showing them.
   - Locked: a tap anywhere shows Unlock.
   - **Checked on the POCO:** a tap on the hidden ⋯ showed the controls 250 ms later (a side tap),
     and ⋯ opened the menu once they were shown.
   - Note: the controls hide on their own 3 s after the last touch.
5. **White flash on rotation** (`722041c`, your #6). Portrait → landscape and back while
   fullscreen: the screen stays black (Gecko's clear colour is black while fullscreen). Check light
   and dark theme.
6. **Portrait fullscreen** (`4889b99`, `1d71103`, your #4). A portrait video, or a landscape one
   turned to portrait with auto-rotate on:
   - the status bar shows, white icons on black, and the top row sits just under it, clear of the
     camera;
   - turn to landscape: both bars go and the 20 dp margins are back;
   - leave fullscreen: Koan's status bar icons are back to the theme's colour.
   Found on the way: the cutout rects could be the previous orientation's after a rotation, which
   pushed the landscape top row about 25 dp down. They are read again now when the cutout changes.
7. **⋯ menu morph** (`3c348e7`, `7f7eb84`, your #2).
   - The panel grows out of the ⋯ pill, its top edge level with the top row, and shrinks back into
     the pill when it closes.
   - The PiP and lock pills fade out under it.
   - **Checked on the POCO:** the open morph and the panel's place. The feel needs a release build:
     this one is.
8. **PiP on leave row** (`7819781`, your #1).
   - The row reads "PiP on leave" with On / Off at the right, and its icon is crossed out when Off.
   - Toggle it and check Settings › Picture-in-picture follows.
   - **Checked on the POCO:** "PiP on leave · On" with the PiP icon.

## Tier 1: private data and things unit tests can't see

### F10 Private bookmarks (`066400d`)
1. In a private tab, ⋯ › Bookmark: the star fills.
2. Switch to normal mode: the same page's star is empty, and typing its site in normal search
   doesn't suggest it.
3. Back in Private: the star is filled, and private search suggests it first; a normal bookmark is
   still suggested after it.
4. A page bookmarked in normal mode, opened in a private tab, shows an empty star. Starring it there
   adds a private copy; the normal one stays.
5. Close every private tab, kill Koan (swipe it away), reopen, then flip Private on: the fingerprint
   is asked (there are only bookmarks now), and the bookmark is still there.
6. With Settings › Lock private tabs off: no fingerprint, and the bookmark is still there.
7. In Private, History › Clear: the card has a **Private bookmarks** row ("1 bookmark made in
   Private"), off. Clear with it on: the bookmark is gone from search and the star.
8. In normal mode the Clear card has no such row.
9. Unstar the last private bookmark, then close all private tabs, go away for over 1 min and come
   back: Private opens without the fingerprint (nothing private left).

### F5 Picture-in-picture on leave (`50f0825`)
The cause was **not reproduced** on the phone. Two changes were made:
- PiP now also counts Koan's own view of the video (its size and play state) when Gecko's media
  session has none;
- the ⋯ toggle was added.

Checks:
1. On the site where PiP didn't start for you: fullscreen video playing, then the home gesture →
   PiP. If it still doesn't start, tell me the site and the build, and I'll read the logcat with
   you.
2. A paused fullscreen video, then home: **no** PiP (your rule).
3. ⋯ › **Picture-in-picture on leave** → Off. Home: no PiP. Settings › Picture-in-picture shows Off;
   turn it back on there and the ⋯ row says On.
4. A private video in PiP (`cd71fe8`):
   - Keep the window on screen for over 1 min, then tap it back to full screen: **no** lock (the
     window counts as Private open).
   - Close the window (✕), wait over 1 min, open Koan: locked. Close it and come back within 1 min:
     not locked.
   - **Audio only**, wait over 1 min, open Koan: locked.
   - Turn the screen off during PiP: locked on return.
5. The standalone player (a video from Downloads): the row is there and works the same.
6. PiP's Audio only still works.

### F2 Rotation (`c8e0fc9`)
1. Auto-rotate on, phone in portrait, a landscape video → fullscreen turns landscape. Turn the
   phone to landscape and back to portrait: the video follows and plays in portrait fullscreen
   with bars above and below. Turn back: landscape.
2. The same with a portrait video.
3. Auto-rotate off: as before (the video's shape, both landscape sides only).
4. Switch auto-rotate on or off during fullscreen: it takes effect.
5. Leave fullscreen in any orientation: back to portrait.
6. Standalone player from Downloads, auto-rotate on: it follows the phone. Auto-rotate off: it
   takes the video's shape once loaded.
7. On the Xiaomi too (HyperOS has its own rotation handling).

### F4 Edge swipes (`fbf5938`)
1. Fullscreen video, portrait and landscape: pull the shade from the top edge. Brightness doesn't
   move and no 2× hold starts.
2. Swipe up from the bottom edge to go home: volume and the timeline don't move.
3. Level swipes in the middle of the screen still work, also near (but not on) the edges.
4. The progress bar: drag sideways scrubs (the bubble follows); a tap on it seeks; a vertical move
   starting on it does nothing.
5. On the Xiaomi in landscape, the top band may need to be wider (HyperOS takes about 80dp): tell
   me if a shade pull still moves brightness there.

## Tier 2: behaviour changes

### F1 Private back in landscape (`4a61764`)
1. Your repro: Private, a fullscreen video, home, open a link from another app (it opens in normal
   mode, in portrait), then switch to Private: portrait, no player over it.
2. The same, but come back after the screen was off (the lock cover): the cover is in portrait, not
   immersive.
3. A fullscreen video in normal mode, then flip Private on: the normal video leaves fullscreen.

### F3 Double-tap seek (`fbf5938`)
1. Double-tap left / right with the controls hidden: it seeks and the controls don't appear. With
   the controls shown: it seeks and they stay.
2. Playing and paused.
3. A triple tap adds 20 s.
4. A single tap on a side shows the controls about 250 ms later; a centre tap shows them at once.
   Judge the side delay: 300 ms is one token if pairs are missed (`PlayerTokens.TAP_PAIR_MS`).
5. A long press still gives 2×.

### F7 Private downloads (`ae4c9fe`)
1. Download a file in a private tab and one in a normal tab.
2. In normal mode: Downloads lists only the normal one. While the private one runs: no ring in the
   pill and no count in the ⋯ menu.
3. In Private: both are listed, the private one masked. The **Private** pill (next to the folder
   chip) narrows the list to the masked one; tap again to show all.
4. Turn Private off and on: the pill is off again.
5. Let a private download finish while normal mode shows: no toast there.
6. In normal mode, Downloads › Clear: the private row stays (visible in Private).

### F6 Download video (`cecf798`)
1. A plain `.mp4` page in fullscreen: ⋯ › Download video, enabled; it downloads and the toast says
   so.
2. A big streaming site (MSE): the row is greyed out with "Streamed video, can't be saved".
3. A DRM page: "Protected video". A live stream: "Live video, can't be saved".
4. A video whose address has no `.mp4`-type ending: the row is enabled. The tap either downloads it
   or says "This video can't be downloaded".
5. In a private tab, a video with no `.mp4`-type ending on a site you're signed in to only in
   Private: Download video works (the check uses the private session). The download is masked in
   Downloads and absent in normal mode. The same for an image saved from a long press in a private
   tab, and for "Download video" in the standalone player opened from a private page.
6. The changed video extension (version 1.2) loads after the update: Koan's player still takes
   over a fullscreen video.

## Tier 3: look and feel

### F8 Ring check (`e8ac502`)
1. Download a small file: the ring fills, a check draws in, then the ring fades and shrinks away
   (about 1.4 s). The "downloaded" toast still shows.
2. Tap the ring during the download and during the check: Downloads opens.
3. Two downloads at once: when the first ends, the ring keeps showing the second's progress.
4. Animations off (developer options): no animation.
5. Known: the address text re-centres in one step when the ring leaves (as before).

### F9 Select all per day (`858fa87`)
1. History: long-press a row. Each day header gets **Select all** at its right; tap it and the
   whole day is checked and the count goes up. It now says **Deselect**; tap it and the day is
   unchecked. If that was all, selection mode ends.
2. With a group expanded, its pages count in the day; with it collapsed, only the group does.
3. With a closed tab checked (Recently closed): no day links.
4. Downloads: the same per day; the top **Select all** still works.
5. The day headers don't move when selection starts or ends.

## Already checked by Claude
- Unit tests: 346 JVM tests, green (`./gradlew testDebugUnitTest`).
- The dev build was installed on the POCO after phase 1: it launches, no crash.
- The R8 release build of `ca15f9a` (round 2, the rotate pill, the stuck play button) is installed on the POCO (real app, release key). It
  launches, comes back cold after a background kill (new process), and logcat shows no crash. The
  restored screen itself was not looked at.

## Not as planned
- F3 and F4 are one commit: they change the same gesture loop.
- F10 is one commit, not six, and it uses its **own** Keystore key (`koan_private_bookmarks`), not
  the private tabs' key. Two stores creating one shared key at the same moment could have left one
  of them unreadable.
- The Clear browsing data row for private bookmarks only shows in Private, so normal mode never
  hints that they exist.
- F1: the standalone player keeps restoring the orientation it found. A web fullscreen under it
  still needs it.
- F5: there is no "lock is due, so no PiP" rule. Instead, time in PiP counts as time away, and the
  lock comes up as soon as the window closes or comes back to full screen, if it is due by then.
- Found while checking F6: Koan's own downloads (⋯ › Download video, a long-pressed image's Save
  image, the standalone player's Download) never marked a private tab's download as private. They
  now do, so they download in the private session and follow F7.
- The plan's per-item dev installs were folded into one install after phase 1 (owner: no stop
  between phases).
