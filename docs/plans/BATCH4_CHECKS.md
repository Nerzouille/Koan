# Batch 4 — phone checks

What to try on the phone before `refactor` is merged into `dev`. Ordered by risk: the checks at
the top cover changes that touch data (files, logins), security or the root's state; the ones at
the bottom cover moves that should change nothing. Group letters match `docs/plans/BATCH4_PLAN.md`
and the "Batch 4 on `refactor`" entries in `docs/HANDOFF.md`.

Use the release build (R8) for anything about feel; try light, dark and Private where a screen
has them.

## 1. Data and security (behaviour changed on purpose)

1. **Downloads' Undo follows its toast (F3).** Delete a file, then before the toast goes do
   something that shows another toast (copy a link): the file is gone at once and stays gone.
   Delete, then Undo: the file is back and opens. Hide from the list, then Undo. Delete and wait:
   gone once the toast leaves. Kill Koan right after a delete: the file stays. A file Koan may not
   delete (one another app saved or replaced): the row comes back with "Couldn't delete …", and
   the file is still on the phone (before the fix the row vanished and the file stayed).
2. **The player's Delete gains Undo (F4).** Open a downloaded video, ⋯ › Delete: "File deleted ·
   Undo" brings it back in Downloads; without Undo it is deleted once the toast goes.
3. **Logins per site (F1).** A login saved on one `*.github.io` site is no longer offered on
   another `*.github.io` site; a login on `accounts.example.com` is still offered on
   `example.com`.
4. **Passwords unlock (F2).** Passwords still ask for the fingerprint or PIN and open after it;
   cancel leaves them locked. Copy a username and a password (Android hides the password in its
   preview). The private tabs' lock works as before.
5. **App links (F1).** "Open in app" with "Always" ticked on `m.youtube.com`, then a YouTube link
   on `youtube.com`: no card. An "Always" chosen before this build still holds.
6. **Root state after process death (C2).** With the overview open, put Koan in the background,
   kill it (or let HyperOS kill it), come back: the overview is open on the same space. Same with
   a page open, and after a link shared from another app.
7. **Private (C3).** Private on and off from the overview toggle, the lock after a minute away,
   "Back to normal tabs" on the lock cover. A link from another app while Private is on opens as a
   normal tab.

## 2. Gestures, motion and per-frame work (feel)

8. **Keyboard (B3).** Tap the address pill on a page and on the start page: the results sit just
   above the keyboard and move with it; the "@" picker too. Close with Back.
9. **Load line (B4, B5).** Load a slow page: the line runs, shimmers and fades as before, on the
   dock's top edge, and the dock does not flicker.
10. **The player (B2, B6, E2, E3).** Open from Downloads (the morph), tap to show / hide the
    controls (soft blur), drag the seek bar (the bubble follows the finger, the time updates by
    the second), double-tap seek, brightness / volume swipe, 2× long press, ⋯ menu, spinner. The
    speed pill, Unlock and Replay now shrink when pressed; some fades are up to 20 ms longer or
    shorter.
11. **Drawer (C).** Open and close the space drawer several times: perf runs showed a small
    drift there (janky 4.3 → 4.7–4.9 %, p99 36 → 40–42 ms, inside the noise threshold). Say if it
    feels heavier.
12. **Overview (B7).** Scroll the stack (the depth dims look the same), fling, switch spaces,
    open a card.
13. **Library rows (E4, E5).** Delete a History / Downloads / Passwords row with a swipe: now a
    short haptic. A quick short swipe still deletes or hides. Long-press a downloaded language in
    ⋯ › Translate: now a haptic. Other haptics (toggles, dock scrub, stack, long presses) as
    before.
14. **Toasts and popups (B2).** A toast in and out, one replacing another, swiping one away; the
    tab-counter and Back long-press popups; Select all on History and Passwords (wash and check
    marks fade).

## 3. Moved surfaces (should look and act exactly as before)

15. **Overlays (C1).** Open and close each: ⋯ menu and its pages (uBlock, Translate, Site
    settings), clear site data, a site permission card, "Open in app", the logins chip, list and
    save card, a page's alert / confirm / <select> / date picker, long press on a link and on an
    image, Save image (Google Images waits for the full image), a download card and a duplicate,
    Settings and its pages, the sign-out card, Downloads, History, Clear browsing data, the tour
    from Settings › About.
16. **Confirm cards (D11).** ⋯ › Site settings › Clear site data, Settings › Translation › delete
    a downloaded language, Downloads › Clear: each asks on the floating card and does the same
    thing as before.
17. **Buttons (D2, decided change).** Permission card (Block / Always allow / Allow once),
    download card, "Open in app" card, a page's alert / confirm: the secondary buttons are
    SemiBold and two buttons in a row are 10 dp apart; nothing else moves.
18. **Toggles (D3).** Settings: search suggestions, hide status bar, picture-in-picture, uBlock,
    the three sync switches, Private browsing's switches; the menu's Desktop site; Translate's
    three switches; Site settings' permission switches.
19. **Page builder pieces (D1, D4–D10).** The overview header and the start page title, and
    pulling each down to dismiss; sheet and drawer handles; back buttons in pages and sheets;
    empty Downloads / History; the private empty state and the lock cover; Settings' and the
    tour's top glow.
20. **Engines (F5).** ⋯ on a page that declares a search engine: "Add this search engine".
21. **Tokens (G1, G2).** Nothing should look different: a sheet, Settings, the search sheet,
    Downloads, the player's pills, bubble, badge and level indicator.

## 4. Dead code removed (A)

22. The launcher and Settings › Apps still show Koan's icon (the themed icon too).
23. Favicons in the overview, search and History after a cold start.
24. Settings › Mozilla account › Send to your Mozilla account: the three switches; Sync now runs
    a password and a history sync (status line under the switches).
