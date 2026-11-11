# Fixes list

The owner's list of fixes and small features, written 2026-11-11. It comes after batch 4
(`docs/plans/BATCH4_PLAN.md`) and before release 0.5 (`docs/HANDOFF.md`, "Next concrete steps").

**Status: nothing implemented yet.**

## Video player

1. **Private mode stays locked in landscape.** Quit the app while a video is fullscreen in private
   mode, then come back through an external link: normal browsing is fine, but switching back to
   private stays locked in landscape.
   - Expected: private mode comes back in the phone's orientation, with no lock left over from
     the fullscreen video.
2. **Rotation only flips between the two landscape sides.** Turning the phone to portrait does
   nothing.
   - Expected: fullscreen follows the phone's rotation, portrait included, unless rotation is
     locked on the phone.
3. **Double-tap to seek (±10 s) shows the controls.**
   - Expected: double-tap seeks without showing the controls.
4. **Edge swipes move the sliders or the progress bar.** Swiping down from the top edge (to pull
   the notification shade) or up from the bottom edge (to leave the app, for example) is taken by
   the player.
   - Expected: edge swipes are ignored by the player's gestures.
5. **Picture-in-picture when leaving the app in fullscreen.**
   - Expected: quitting the app while fullscreen goes to PiP by default, with an on/off option in
     the player's ⋯ menu.
6. **"Download video" in the player's ⋯ menu.**
   - Expected: the button downloads the video when it can be downloaded; it is greyed out
     otherwise (DRM, streams).

## Downloads

7. **Private downloads appear in the normal Downloads list.**
   - Expected: private downloads only show in private mode.
8. **The progress bar in the address bar just disappears when a download finishes.**
   - Expected: a short "done" animation (a check) before it goes, and tapping the progress icon,
     during the download or right after, opens the Downloads page.

## History and library

9. **"Select all" per day in selection mode (History and Downloads).**
   - Expected: each day header has a "Select all" that selects that day's entries.

## Private mode

10. **Private bookmarks.**
    - Expected: bookmarks made in private mode are kept separate from normal bookmarks and only
      shown in private mode.
