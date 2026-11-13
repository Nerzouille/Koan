# Roadmap

M0–M3 and M5 were done strictly in order, each ending with its acceptance criteria met on the
real phone, unit tests green, a summary to the owner and a git tag `m<N>`. Since v0.3.0, M4, M6
and M7 move forward in parallel, one item or batch at a time on its own branch from `dev`
(CLAUDE.md rule 1); there is no `m<N>` tag for them until they are finished.

**Order (changed 2026-10-15):** M0 → M1 → M2 → M3 → **v0.1.0 pre-release** → **M5** → **M4** → M6 →
M7. Browser essentials come before the motion polish, so Koan can become the daily browser sooner.
Milestone numbers are kept as they are.

## Status (2026-11-12)

Latest release **v0.4.0** (pre-release, 2026-10-30). Branches and how to build/test/release:
`docs/HANDOFF.md`.

| Item | Status | Where | Next concrete step |
|---|---|---|---|
| M0 Skeleton browser | Done | tag `m0` | — |
| M1 Overview UI | Done | tag `m1` | — |
| M2 Sign in + read Spaces | Done | tag `m2` | — |
| M3 Real tabs | Done | tag `m3` | — |
| v0.1.0 pre-release | Done | tag `v0.1.0` | — |
| M5 Browser essentials | Done | tag `m5`, v0.2.0 / v0.2.1 | — |
| Android 11+ (minSdk 30) | Done | v0.3.0 | — |
| Releases v0.3.0, v0.4.0 | Done | tags on `main` | 0.4.0: owner decides whether to mark it a normal release (`/releases/latest` 404s, see HANDOFF) |
| M4 Motion polish | **Partly done**, no branch | `dev` | Owner picks the next M4 item. Done: card flies page ↔ stack both ways (`ui/overview/CardHero.kt`, v0.4.0), long-press lift, swipe-to-close by distance or fling, Undo toast after a close, Back on a tab's first page shrinks the page into its card (`ui/browser/ClosingPage.kt`). Not started: stack compression / elastic trailing, new-tab grow, close collapse in the stack, baseline profile, JankStats, search backdrop retry. |
| M6 Write-back | **In progress** | `dev` | Passwords (add; edits and deletions behind their own switch, off) and history (both ways) done and running on the owner's account since 2026-11-10 (`docs/HANDOFF.md` "Two-way sync"). Next: the Spaces write plan (`docs/plans/SPACES_WRITE_PLAN.md`, to be written; plan only). Bookmarks and Zen Spaces not written yet; Spaces writes come last, behind `writeBackEnabled` (the setting and its gate `sync/SpacesWriteGate.kt` exist since the audit fixes, off and not in the UI). |
| M7 Delights | **Partly done** | `dev` | Done: `@engine` shortcuts, custom engines, Undo close, Recently closed (last 25, since v0.3.0) and Reopen closed tab, background audio, Koan's video player and picture-in-picture (`ui/player`, `engine/PictureInPicture.kt`). Dropped: status bar hidden while scrolling (branch `statusbar`, not merged). Set aside: the video mini-player. Not started: everything else below. |
| Private mode + overview polish | **Done** on `dev` (tested on the phone) | `06e13d6` | Open points in `docs/HANDOFF.md` "Private mode". Card separation, two-line headers, amber sync dot, `#121214` dark base, encrypted private tabs kept across restarts, lock, FLAG_SECURE, private-only engine protections. |
| Page translation, "Open in app" cards, library selection mode, Zia icons | Done on `dev` | night batch | Ship in 0.5 (`CHANGELOG.md` "Unreleased"). |
| Audit 2026-11-11 | **In progress** | `dev` | Batches 1 (critical fixes), 2 (docs) and 3 (GeckoView 160) merged; the owner's batch 1 phone checks pending. Batch 4, refactoring (`docs/plans/BATCH4_BRIEF.md`), and batch 5 (tests, CI, baseline profile) come after the 0.5 release. |
| Open items after v0.2.0 | Not started | — | GeckoView 160 is in, but HyperOS's long screenshot still doesn't work (it doesn't use Android's scroll-capture API): **Koan's own full-page screenshot or Save as PDF** is the option left. The other items wait for the owner's choice. |

## M0 — Skeleton browser (≈ 2–4 days)
- Gradle project, package `dev.nerzouille.koan` (rename if owner prefers), Compose, single activity.
- GeckoView via android-components rendering one tab; bottom toolbar with address pill, back, menu.
- Type URL or search (DuckDuckGo default) → loads. Back/forward, reload, progress bar.
- Decide android-components vs raw GeckoView; log the decision in `ARCHITECTURE.md`.
**Done when:** can browse normally on the phone in a release build.

## M1 — Overview UI with fake data (≈ 1 week) ← the fun part, UI first
- Room schema from `ARCHITECTURE.md`; seed 3 fake spaces with gradients and 8–15 tabs each.
- Space overview: header, essentials grid, overlapping card stack, workspace bar, gradient bg.
- Workspace bar paging with parallax + gradient blend + haptics.
- Swipe-to-close, tap ✕, long-press reorder (reorder within space is enough here).
- Placeholder thumbnails (favicon-colored gradients).
**Done when:** the overview feels great on the phone at full refresh rate with 50 cards; no jank in
System Trace during scroll and space switching.

## M2 — Sign in + read Zen Spaces (≈ 1 week)
- Clone Z-Sync into `reference/`, port the sync layer (see `SYNC.md`), port contract tests.
- Mozilla sign-in screen (re-skinned), TOTP prompt, encrypted credential storage, sign-out.
- Pull `spaces` + `prefs` into Room; overview now shows real spaces, essentials, pinned/normal tabs,
  folders, splits. Pull-to-refresh, background refresh rules.
**Done when:** all contract fixtures pass, and the phone shows the same Spaces as Zen on the PC.

## M3 — Real tabs in spaces (≈ 1–2 weeks)
- Open a card → real engine tab (simple crossfade transition for now).
- New tab in current space; navigation updates title/url/favicon in Room.
- Tab lifecycle: max 4 live sessions, suspension/restore, trim-memory handling.
- Thumbnail capture + disk LRU + async decode.
- Swipe on address pill switches spaces in page view.
- State survives process death (restore last space/tab).
**Done when:** daily browsing across spaces works for a few days without lost tabs or crashes.

## v0.1.0 — First pre-release (after M3)
- Release hygiene: dev menu items and fake stress-fill data exist in debug builds only. A fresh
  install without a Mozilla account is a plain browser with one local space and an optional
  "Connect your Zen Spaces".
- versionName 0.1.0, `CHANGELOG.md` with what works and the known limitations.
- Signed arm64-v8a release APK, attached to a GitHub **pre-release** tagged `v0.1.0`.
**Done when:** first run checked on a clean install, the APK updates the owner's install without
losing data, and the owner has approved and published the release.

## M5 — Browser essentials (≈ 1–2 weeks) — done 2026-10-19 (tag `m5`, v0.2.0 beta)
- Read-only passwords from Firefox Sync → login autofill. History + bookmarks → search suggestions.
- uBlock Origin install via add-ons; add-on manager in settings.
- Downloads, site permissions, context menu (open in new tab / in Glance later / copy link),
  find in page, share, default-browser intent filters.
- About: one GitHub releases check when About opens ("Up to date" / "Update available"), never
  polled in the background.
- Logins saved on the phone ("Save in Koan, this phone only") stay local and encrypted until M6.
**Done when:** owner can uninstall Opera.

### For 1.0.0-beta.2

- **Real blur on the deeper visible cards** (HyperOS recents style), asked 2026-11-12: a
  `RenderEffect` blur on the back cards' visible slice on Android 12+, growing with depth; Android
  11 keeps the depth dim only (no live blur there). The v0.4.0 depth dim stays underneath (pinned by
  `DepthDimTest`). Before keeping it: `tools/perf/frames.sh` on the stack scroll ("scroll" and
  "open"), release build on the POCO and the Xiaomi, against the build without it; drop it if p90
  or janky % get worse beyond noise. Keep batch 4 group B's rule: no recomposition or re-recording
  per frame (the radius read in a layer lambda, a few cached `RenderEffect` steps like `blurFor`).
- **A visible depth cue in light and dark** (owner, 2026-11-12, design change): white headers barely
  dim (254 → 244 in light), so the dim alone can't show depth. Options to show the owner as
  screenshots on the POCO, light and dark, before building one:
  - **A, recessed tint:** headers and thumbnails tint with depth, at full strength (not halved like
    today's header dim), towards a colour darker than the background in light (`#B9B9C2`) and
    towards black in dark; capped around 0.6.
  - **B, shadow and scale:** the shadow grows with depth (+7 dp per step up to depth 4, the key
    shadow not softened) and the scale steps are smaller (0.46 + 0.54 × 0.62ᵈ instead of
    0.58 + 0.42 × 0.52ᵈ).
  - **C, blur (the item above):** the deeper cards' thumbnails and headers blur with depth
    (`RenderEffect`, Android 12+; dim only on Android 11).
  Prototype of all three on the local branch `depth-cue-proto` (31e6f5d, not for release), switched
  with `adb shell setprop debug.koan.depthcue 0|1|2|3` and a restart of the staging copy. Light
  screenshots were taken; the dark ones failed (MIUI stopped applying `cmd uimode night` to the
  screen) and the front card was a test tab, so redo them with Koan's own Settings › Theme and the
  colour demo pages in front. Pick one (or a mix: A + B) before building it for real, then measure
  with `tools/perf` like the blur.

- **Rotate row in the player's ⋯ menu** (on top of the rotate pill): a "Rotate" row that turns
  full screen to the other shape (portrait ↔ landscape) at any time, not only during the pill's
  3 s after the phone turns. Same switch as the pill (`PlayerHost.fullscreenRotated`); the
  standalone player needs the same. With auto-rotate on it isn't needed (the screen follows the
  phone): hide it there or keep it, the owner's call.
- **Quality choice for Download video:** when the page offers several files of the same video
  (Wikimedia Commons: 240p … 1080p renditions and the 2.8 GB original), let the owner pick one
  (a short list in the ⋯ menu, with sizes when the page or a HEAD request gives them) instead of
  always taking the original or the largest rendition. Starts in the video extension's
  `fileSource` (`assets/extensions/video/content.js`), which then reports every candidate, not
  just the best one.
- **Translate icon after translating** (owner, 2026-11-13): the icon stays in the address pill once
  the page is translated, in an active state (filled icon, no brand gradient); a tap opens the card
  with **Show original** and the language choice. Today `translateCue` returns `None` once
  `translated` is true, so the icon goes away (`ui/browser/TranslateCue.kt`).
- **Download ring and translate icon share the slot** (owner, 2026-11-13): while a download runs,
  its ring takes the translate icon's place in the address pill; the icon comes back when the ring
  is gone.
- **Bug: translation state carried over to the next page** (seen 2026-11-13 on the Xiaomi): after a
  translation in a tab, a Chinese page opened in the same tab got a translate sheet saying
  "From English" (and no translate icon in the pill). The translation state must reset on each new
  page (each navigation, not each tab). A fresh tab on the same page was right.
- **Bug: Find in page counter** (seen 2026-11-13): the bar said "0/0" while it highlighted a match
  (searching a word on a Wikipedia article). The count must follow the matches.
- **Downloads: a file that is no longer there** (owner, 2026-11-13): opening a download whose file
  was moved or deleted must say so ("File moved or missing"), wherever it's opened from (Downloads,
  the ring, the toast, the notification). `DownloadsScreen.open` already has a "Couldn't open this
  file" toast for when `actions.open` fails, so check first why it doesn't show: the open probably
  "succeeds" with a stale URI and the other app fails instead. Check the file exists before opening;
  maybe mark such rows in the list too.
- **Bug: Save image waits for the full image, never works** (owner, 2026-11-13): on Google Images,
  saving before the full picture has loaded should wait for it and save the full image (1.0.0-beta.1
  CHANGELOG, Improved), but on the phone it never does. Starts in `engine/PageImages.kt` and the
  `pageimage` extension (`assets/extensions/pageimage/content.js`): reproduce on the Xiaomi, log what
  the extension reports, then fix or drop the wait.
- **Bug: no check when a download finishes** (owner, 2026-11-13): the address bar's ring should fill
  and draw a check before it goes (`ui/bar/AddressRow.kt`, the `done` state; `KoanRoot.kt` keeps it
  for `DONE_MS`), but at the end of a download nothing shows. Check whether `done` is ever set (the
  finished download may drop out of the running list before the ring sees it), and on which pages.

### Batch 4 leftovers (after the release)

From the batch 4 refactoring (`docs/plans/BATCH4_PLAN.md`, branch `refactor`), left for after the
0.5 release:

- **E6–E8, gesture merges:** one `NeighbourDrag` for the space pager and the tab swipe (E6), one
  drag loop (`awaitSlop` / `dragUntilUp`, E7) and one `Modifier.swipeToDismiss` with unified
  thresholds (E8), each with its JVM test. The plan needs the owner's feel check on a release
  build after each caller, the stack last (dropped if it feels different at all).
- **F6, favicons:** one decode path and one memory cache instead of `FaviconLoader`'s and
  `IconStore`'s; measure `dumpsys meminfo` before / after.
- **G3, tokens (only if wanted):** a spacing scale, kit-level tokens, then `Tokens.kt` split by
  feature (a pure move, last).

### Open items after v0.2.0 (not scheduled yet)
- **Search backdrop retry:** the old dim plus a subtle blur behind the suggestion cards only (also
  listed under M4).
- **Desktop site:** revisit the toggle.
- **Add to home screen:** a page as a launcher shortcut.
- ~~**Picture-in-picture** for video when leaving Koan.~~ Done with the video player.
- ~~**Private browsing**~~ Done (merged 2026-11-11, `docs/design/OVERVIEW_PRIVATE.md`).
- **Pull to refresh in chats:** a site whose list scrolls inside the page (not the page itself) may
  reload on a pull at the top of the list. Gecko doesn't say whether an inner list can scroll up.
- **Long screenshots:** GeckoView 160 implements Android's scroll capture ("Capture more" works
  on stock Android), but HyperOS's own scroll screenshot doesn't use it and stays greyed out in
  Koan. Option: Koan's own stitched full-page screenshot, or Save as PDF.
- **Page painted only down to where the keyboard was** (white below, stays while scrolling):
  seen twice on 2026-10-23 during adb tests, then never again (four sequences tried, old and new
  builds). Likely cause: GeckoView kept a keyboard height per tab, and a tab left with the
  keyboard up never heard it close; its keyboard listener is gone since b8cf703. Watch for it:
  note what was done just before.
- **Dock ignoring taps:** happened once on a Google AI mode tab (pill and tab count did
  nothing) after the phone sat ~10 min untouched (the screen may simply have been off); not
  reproduced since. Watch for it.

### Android 11+ (minSdk 30) — merged 2026-10-23, shipped in v0.3.0
- Runs on Android 11–13 too: no live blur below 12 (stronger dim instead), "copied" toasts below
  13, smaller memory budgets on phones under 6 GB (`DeviceProfile`). Audit: `docs/audits/MIN_SDK.md`.
- Tested as `preview-android11` (0.2.2-preview.1, versionCode 7): no frame or start-time
  difference on the owner's phone (`tools/perf`). v0.3.0 is versionCode 8.

## M4 — Motion polish (≈ 1–2 weeks, iterate with owner) — partly done (see Status)
- Hero open/close morph per `UI_SPEC.md` (springs, thumbnail → live swap, pre-scroll on close).
- Stack compression/elastic trailing while scrolling.
- New-tab grow animation, card lift/drop, close collapse.
- Baseline profile; JankStats logging in debug.
- Retry search backdrop: old dim + subtle blur behind the suggestion cards only.
**Done when:** owner signs off on the feel. Expect several rounds of tweaks.

## M6 — Write-back to Zen (≈ 1 week, careful) — in progress: passwords and history on `dev`; bookmarks and Spaces next
Done (details: `docs/SYNC_WRITE.md` "As built"):
- Passwords and history, each behind its own switch in Settings › Mozilla account (off by
  default). Phone-only logins go up only when "Passwords saved on this phone" is turned on.
- Conditional writes (X-If-Unmodified-Since), meta/global checks, one encrypted backup per
  collection before the first write, a first batch of 3 read back. Running on the owner's real
  account since 2026-11-10 (no throwaway account was used in the end).
- `writeBackEnabled` setting (off, not in the UI) and `sync/SpacesWriteGate.kt`: no request can
  write `spaces` while it is off.

Left:
- Bookmarks, then Zen Spaces (operations in the order listed in `SYNC.md`), Spaces against a
  throwaway Space first.
- No undo journal (the original plan had one): decide whether the backups are enough.
**Done when:** a tab added/closed/moved on the phone appears correctly in Zen desktop, and a 412
conflict is handled without data loss.

## M7 — Delights (open-ended)
- Pull-down for new tab, `@engine` search shortcuts, space menu (rename/icon/theme) if write-back
  for `space` records is added, "send to Zen" from share sheet, home-screen widget for a space.

### M7 ideas (to refine when we get there)

**Link gestures** (all start with a long-press on a link, so they never conflict with scrolling; a small link chip follows the finger, labelled with the action, haptic tick at the threshold; everything works one-handed from the bottom of the screen)
- **Glance**: long-press a link + drag UP → the page opens in a floating sheet over the current tab.
  - Controls live at the bottom of the sheet (thumb zone): a handle + two buttons, "Open as tab" and "✕". Android back also closes it.
  - Flick on the bottom handle: one direction promotes to a real tab, the other dismisses. Direction is a token, decide by testing (option A: up = open / down = dismiss, like the rest of Koan; option B: up = throw away / down = open, like the iOS app switcher).
- **Open in background**: long-press a link + drag DOWN → opens in a new background tab in the current space.
- **Open in another space**: while dragging a link down, the dock shows the space icons; dropping the link on one opens it in that space (in background).

**Tabs**
- **Undo close**: after closing a card (swipe or ✕), a small "Undo" toast for a few seconds.
  (Done: Back on a tab's first page since v0.2.0, and a card closed in the overview.)
- ~~**Recently closed**~~: done. A list of the last 25 closed tabs since v0.3.0, and **Reopen
  closed tab** in the tab counter's long-press menu since v0.4.0.
- **Previous tab**: double-tap the tab button → switch back to the previous tab (Alt-Tab style).
- **Glance a card** (maybe): long-press a card in the overview to preview it larger without switching. Same sheet as Glance. To evaluate.
- **Auto-archive**: phone-only tabs untouched for N days (setting) move to an "Archived" list instead of piling up. Never touches tabs that come from Zen.

**Display**
- **Status bar hidden only while scrolling**: prototype on branch `statusbar`, works but a fixed
  bottom bar can still be off by one frame because Gecko's redraw timing varies; revisit if
  GeckoView adds a draw callback or top toolbar support. Earlier attempts below.
- (Tried and reverted 2026-10-27, before v0.4.0): the
  status bar would show at the top of a page and hide once it is scrolled down. Two attempts:
  first with the dock (1:1), then on its own past a scroll distance, back only at the top. Both
  slid the page down under the status bar (a layer translation) and counted that slide as extra
  dynamic toolbar so fixed bottom bars stayed above the dock. In real use it broke the bottom of
  the page: the page showed through the gesture-bar area again under fixed bottom bars, a black
  band plus the dock made two bars at the bottom, and it needed too much scroll to hide. Back to
  plain "Hide status bar" (hidden in the page view and its search sheet). A retry should leave the
  dock / fixed-bottom / keyboard layout untouched, e.g. resize the page once at the threshold
  instead of translating it, and be judged on chat sites and pages with fixed bottom bars first.

**Media**
- ~~**Background audio**: audio/video keeps playing with the screen off or Koan in the background, with media controls in the notification.~~ Done in v0.2.0.
- ~~**Picture-in-picture** for video when leaving Koan.~~ Done with Koan's video player (2026-11-11), with an "Audio only" action and a Settings › Display switch.
- **Video mini-player** (docs/VIDEO_PLAYER.md §6, built once on 2026-11-11 and set aside; a copy of that version is in `reference/mini-player-2026-11-11/`, local only): minimise a fullscreen web video to a card above the dock, visible all over Koan. Dropped for now because the page's own player kept playing in place under the card (Commons' modal player): it needs the page's player hidden or paused while minimised, and a live thumbnail (pages from another origin never give frames).
  Possible again with GeckoView 160's experimental picture-in-picture API
  (`PictureInPictureController.SessionPictureInPicture`): Gecko clones the playing video into a
  second session Koan hosts (a small GeckoView in the card), `setKeepActive` keeps the page
  painting while hidden, and Gecko shows its "playing in picture-in-picture" overlay on the
  original video. Kept for later: the API is experimental and Fenix doesn't use it yet.

**Phone → PC**
- **Send to PC now**: use Firefox Accounts "Send Tab to Device" (Koan registers as a device on the Mozilla account) so the page opens immediately on Zen desktop. Needs verification that Zen receives sent tabs (it's Firefox-based, so it should).
- **Add to a Zen space**: write the tab into a chosen Zen space via sync (requires M6 write-back); appears on desktop at the next sync.
