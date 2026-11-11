# Fixes list: phone checks (branch `fixes`)

The 10 items of `docs/FIXES.md`, built from `docs/plans/FIXES_PLAN.md` on 2026-11-12. The R8 release
build is installed on the POCO. The checks are ordered riskiest first. Use demo pages or throwaway
tabs only. The video checks need a plain `.mp4` test page, one big streaming site, one DRM page
and one live stream.

None of this was driven on the phone by Claude. Only the launch / kill / restore of the R8 build
was checked (see the end).

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
4. A private video in PiP for over 1 min (`5f6b4ab`):
   - Tap the window back to full screen: the lock cover comes up at once, in portrait, with no
     video over it, and the fingerprint is asked.
   - Under 1 min: no lock.
   - Close the window (✕) after over 1 min, then open Koan: locked.
   - Close it after 30 s, wait 40 s, then open Koan: locked (the time counts from when PiP
     started).
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
- Unit tests: 324 JVM tests, green (`./gradlew testDebugUnitTest`).
- The dev build was installed on the POCO after phase 1: it launches, no crash.
- The R8 release build of `50f0825` is installed on the POCO (real app, release key). It
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
