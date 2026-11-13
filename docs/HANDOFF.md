# Handoff

State of Koan for a session that starts with no context. Last updated **2026-11-12**.
Read `CLAUDE.md` first (rules), then this file, then `docs/ROADMAP.md` (status of every item).

## Where things stand

- **Release prep for v1.0.0-beta.1 (2026-11-12):** branch **`release-prep`** (from `dev`, pushed),
  plan and night report in `docs/plans/NIGHT_PLAN.md`. Batch 5 tests (411 JVM tests, Robolectric
  added), CI workflow (not run yet: open a draft PR to `dev`), semver update check, What's new
  "1.0", the baseline profile (generated 2026-11-12, small gain: see the plan's "Morning
  session"), README and release docs. Fixed in the morning: stuck start page after What's new,
  "Translated" on every page. Staging R8 check and the update path done on the POCO. Still to do:
  the owner's phone recheck (plan "Afternoon": rotation with the phone turning, the Private
  switch, the lock, translation), then the release steps. Afternoon: translation offer change,
  rotation safety net (`ui/browser/LayoutWatch.kt`, logs `KoanSize`), Private switch rework
  (interruptible flip), captures done; CI green on PR #3 (draft, pushed head only). Evening:
  the translation offer is an icon in the pill, the card on its own only on readable pages
  (`ui/browser/TranslateCue.kt`); APK size explained (nothing by mistake).
  Late: depth dim caps back to v0.4.0 (`DepthDimTest`); a depth-cue proposal for beta.2 is
  prototyped on the local branch `depth-cue-proto` only (ROADMAP › For 1.0.0-beta.2).
  Final prep (2026-11-12, night): README images in `docs/media/` (1080 wide, from the Xiaomi;
  `docs/screenshots/` removed; retaken 2026-11-13: one image each (light, Private dark),
  Wikipedia / Commons / this repo, 256-colour PNGs; grid of 6, the player full width, then About,
  Settings signed out (staging copy) and What's new), tour cards 2 and 5 keep their clips
  (owner: approved, fine to ship), CHANGELOG / FEATURES brought up
  to date (translation icon, Private switch, rotation safety net, depth caps). Release notes and
  the Discord post in `reference/` wait for the owner's approval. Local commits not pushed.
  ⋯ menu shots retaken with the dev build (no translation state). APK size everywhere in decimal
  MB (10⁶ bytes) from the R8 release build: 222,701,496 bytes = about 223 MB (2026-11-13, with the final What's new clips) (GitHub's asset list
  shows binary units, ~212). Depth stays out of the release notes (a beta.2 item).
  Next: owner's approvals, the phone recheck list, push, then release steps 6–7.
  beta.2 list: ROADMAP › For 1.0.0-beta.2 (depth cue A/B/C and the blur, the player's Rotate row,
  the Download video quality choice).
  What's new "1.0" reworked (owner, 2026-11-12 night): every media card in the same Screen frame
  (Private still, a translate clip, a new "Open in app" clip with the Wikipedia app), the player in
  a sideways `Framing.Landscape` frame (a clip on Big Buck Bunny: controls, hold 2×, double-tap
  +10 s, one clip for both themes), Two-way sync text + icon; the title glow fades on that centred
  card. Clips 2026-11-13 recorded in the owner's Koan, scripts in
  `reference/capture-tools/` (`take_openapp.sh`, `take_bbb.sh` + `bbb.txt` for the injector).
  1.0.0-beta.1 replaces the planned 0.5; versionCode 10.

- **Latest release: v0.4.0** ("Koan 0.4 (beta)"), tagged on `main` at `d4f3a2e`, published on
  2026-10-30 as a GitHub **pre-release**. versionName `0.4.0`, versionCode `9`: the next release
  needs versionCode **≥ 10**.
- **`dev` is ahead of `main`** (`CHANGELOG.md` "Unreleased") with:
  - the night batch of 2026-11-10, reviewed with the owner and merged on 2026-11-11: library pages
    (selection mode, swipes, Passwords look, add a password), two-way sync for passwords and
    history (**M6**, see below), page translation, "Open in app" cards, the duplicate download card
    and the morphing action card, Back close animation, toast hand-over, Zia icons, targeted
    package queries instead of `QUERY_ALL_PACKAGES`, full image on Google Images;
  - the **video player** (`000d4a1`, spec `docs/VIDEO_PLAYER.md`): own controls, picture-in-picture
    with Audio only, standalone player for videos from Downloads and other apps;
  - **private mode** and the overview polish (`06e13d6`, spec `docs/design/OVERVIEW_PRIVATE.md`),
    see "Private mode" below.
- **Audit of 2026-11-11:** report in `docs/AUDIT.md` + `docs/audits/2026-11-11/` (both
  **gitignored** until the owner publishes them, after the security fixes are merged). Fixes come
  in batches, each on its own branch:
  1. `audit-fixes` (`79de50d`, merged into `dev`): critical fixes (history
     deletions only for pages removed on the phone, `zen_*` icons kept in R8 builds, crash and
     DataStore corruption handlers, private favicons cache-only, pairing-link confirmation card,
     FLAG_SECURE on Passwords, upload URI filter, sign-in WebView wipe, generic User-Agent, PiP
     leak, `writeBackEnabled` + `SpacesWriteGate` for the Zen `spaces` collection).
  2. `audit-docs` (merged into `dev`): every doc brought in line with the code;
     `docs/ARCHITECTURE.md` rewritten; `NIGHT_REPORT.md` and `REVIEW_NIGHT.md` removed (their
     rules are in `docs/UI_SPEC.md`).
  3. `geckoview-160` (merged into `dev`): GeckoView / android-components 160, see "Engine".
  4. Refactoring (`docs/plans/BATCH4_PLAN.md`): merged into `dev` on 2026-11-12, see "Batch 4";
     leftovers in `docs/ROADMAP.md` "Batch 4 leftovers".
  5. Tests, CI, baseline profile: after batch 4.
- **The owner's fixes list** (`docs/FIXES.md`, 10 items, plus two rounds of the owner's phone
  feedback and the stuck play button): built on branch `fixes`, the owner's recheck passed,
  **merged into `dev` on 2026-11-12** (`0477033`, commits kept); `fixes` deleted, not pushed.
  Plan `docs/plans/FIXES_PLAN.md`; checks and what was verified in `docs/plans/FIXES_CHECKS.md`.
  346 JVM tests green on `dev`. Still worth watching: F2 / F4 on the Xiaomi (only the POCO was
  used), the buffering-spinner case of the play button (fixed in code, not seen on the phone),
  F10's sealed file (`files/private/bookmarks.sealed`, Keystore alias `koan_private_bookmarks`).
  For beta.2: the Rotate row and the download quality choice (`docs/ROADMAP.md` "For 1.0.0-beta.2").
- Engine: android-components **160.0** / GeckoView **160.0.20261104181552**
  (`gradle/libs.versions.toml`, the two versions move together), compileSdk **37.2** (AC 160
  requires it). Bump notes: ARCHITECTURE "Bumping android-components + GeckoView". Tested on the
  POCO (staging copies): R8 launch, Widevine, downloads + duplicate card, media notification; perf
  A/B 157 → 160 on the POCO: no regression, starts faster, space switch p90 12 → 17 ms to watch.
  Android's own scroll screenshot ("Capture more") works on stock Android 14 (emulator); HyperOS's
  does not (see "Untested / unverified").
- Milestones done and tagged: `m0`, `m1`, `m2`, `m3`, `m5`. M4 partly done (no tag), M6 passwords
  + history done on `dev` (Spaces, tabs, bookmarks never written), M7 partly done. Details per
  item in `docs/ROADMAP.md`.
- Unit tests: 301 JVM tests on `dev`, green (after batch 4: dead-code tests removed, batch 4 tests added)  (`./gradlew testDebugUnitTest`). R8 staging build of
  `dev` + 160 launched on the POCO without a crash (2026-11-11).

## Batch 4 (branch `refactor`, merged into `dev` 2026-11-12)

Plan: `docs/plans/BATCH4_PLAN.md` (defaults: SemiBold + 10 dp, 3.37 skipped, SeedData deleted). One
commit per step; full suite + dev build per step; R8 release build installed on the POCO at the end of
each group (launch + kill + restore checked).

- **Group A, dead code (done, 6 commits):** SyncBack chain + bookmark planner, SyncClient's
  single-record writes (postBatch is the only write left; its tests replace the postRecords ones),
  favicon prefetch / synced-icon paths, small unused items + compose tooling + the 10 legacy
  launcher bitmaps, SeedData + the debug fake-tab menu, three stale KDocs. 301 → 290 tests (all
  deleted ones covered deleted code). R8 build: launches, restores after a kill, no crash.
  **Check on the phone:** launcher and Settings › Apps still show the Koan icon (themed icon too);
  favicons in the overview, search and History after a cold start; Settings › Mozilla account ›
  Send to your Mozilla account still shows the three switches; a password sync and a history sync
  still run (Sync now, status line under the switches).
- **Group B, per-frame recompositions (done, 15 commits):** `ui/kit/MotionKit.kt` (one
  `animationsOff`, `frameClock`), `ui/kit/Presence.kt` (`rememberPresence`) for the toast, dock
  popup, search sheet, start page, player overlays and play glyph; the pill's space-mode fade and
  the selection wash / check read in layers or draw; the standalone player's open morph; the
  keyboard inset applied in layout (search sheet, start page); page load progress read only by the
  load line, which now has its own layer and cached brushes; the seek scrub read in draw / layout
  and the mist's blurs cached; card dims through layer alpha; overview page lambdas kept.
  Measured on the POCO (staging copies, speed-profile, `tools/perf/frames.sh` + hand scenarios
  keyboard and pull-to-refresh, local scripts): no scenario got worse beyond noise; reload p90
  10 → 8–9 ms, space p90 19 → 13 ms at B2. The phone drops to half rate when warm after ~1 h of
  runs: compare interleaved runs only (A/B/A/B). The player (B6) was not measured on the phone
  (no player scenario). R8 build: launches, restores after a kill, no crash.
  **Check on the phone:** toast in and out (and a toast replaced by another); dock popups (tab
  counter long press, Back long press); typing in the address field on a page and on the start
  page: results sit just above the keyboard and move with it, the "@" picker too; a page load: the
  line runs, shimmers and fades as before, above the dock's top edge; Select all on History /
  Passwords (wash and check marks fade); the player: open from Downloads (morph), tap to show /
  hide the controls (soft blur), drag the seek bar (bubble follows the finger, time updates),
  double-tap seek glow, brightness / volume swipe, 2× long press, ⋯ menu; overview scroll (dims
  unchanged), space switch.
- **Group C, root and overlays split (done, 6 commits; C4 skipped, optional):** `KoanOverlays`
  1 370 → 313 lines, one composable per group in `ui/overlays/` (one commit: the groups were cut
  from one function sharing state; a line-by-line check found every original line, verbatim).
  `ui/root/`: `RootState` (the navigation values, each still its own `rememberSaveable`; the
  root reads them through property delegates), `PrivateSwitch`, `IncomingLinks`, `PlayerActions`.
  **Not done as planned:** the root's transitions stay in `KoanRoot.kt` and there is no
  `RootStateTest`: the transitions call `TabManager`, `SpacePager`, `OverviewMotion` and the store,
  so a JVM test needs interfaces over them (a design change, not a move): left for batch 5.
  Checked on the staging copy: ⋯ menu, Site settings, Back, tab-counter menu, overview, Settings,
  History, Private on / off, a link from another app while the overview is up, overview restored
  after a kill. Perf, interleaved end of B / end of C: same or better except the drawer, janky
  4.3 → 4.7–4.9 % and p99 36 → 40–42 ms in both pairs (inside the noise threshold, but consistent:
  watch it). R8 build: launches, restores after a kill, no crash.
  **Check on the phone:** every overlay opens and closes as before (⋯ menu and its pages, clear
  site data, a site permission, "Open in app", logins chip / list / save, a page's <select> /
  alert, long press on a link and an image, Save image, a download and a duplicate, tab counter
  and Back long press, Settings and its pages, sign out card, Downloads, History, Clear data, the
  tour from Settings › About); Private on / off and the lock; a link shared from another app;
  kill Koan in the background with the overview open and come back. Open and close the drawer a
  few times: it should feel the same.
- **Group D, shared UI components (done, 14 commits; D12 dropped, as the plan allowed):**
  `DragHandle`, `brandWash`, `SpaceTitle`, `swipeDownToDismiss`; `KoanButton(kind)` under the
  four buttons (prompt cards' private secondaries gone); `SettingsToggleRow` / `SheetToggleRow`;
  `KoanIconButton`; `CardHeader` + `CardIconTile`; `FileGlyph`, `PrivateIconTile`; `TextLink`;
  `EmptyLine`; `KoanDivider`; `RowTexts` in both kit rows; clear site data, delete a language and
  clear downloads now ask on `Overlays.ask`. **Visible change (decided):** prompt cards' secondary
  buttons SemiBold (were Medium), 10dp between two buttons (were 8). **Left out:** touch-size
  alignment (3.3, every icon button keeps its size), the chips (3.7: no two alike), the 4
  hand-built rows (3.2), the scrim / spinner / field / label merges (3.12), the player's delete
  dialog (draws over the overlays, so `ask` would sit under it). R8 build: launches, restores
  after a kill, no crash.
  **Check on the phone, light, dark and Private:** permission card (Block / Always allow / Allow
  once), download card, "Open in app" card, a page's alert / confirm / <select> / date: buttons
  SemiBold and 10dp apart, nothing else moved; sign-out and Clear browsing data cards; ⋯ › Site
  settings › Clear site data, Settings › Translation › delete a downloaded language, Downloads ›
  Clear: each asks on the floating card and does the same thing; Settings toggles (search
  suggestions, hide status bar, picture-in-picture, uBlock, the three sync switches, Private
  browsing's toggles); the menu's Desktop site, Translate's three switches, Site settings'
  permission switches; the overview header and start page title, pull down to dismiss both;
  sheet and drawer handles; empty Downloads / History; the private empty state and lock cover.
- **Group E, motion and gestures (partly done, 6 commits):** duplicate springs merged (E1); three
  fade tokens `Motion.FadeQuick / Fade / FadeSlow` for the player, `Modifier.mist` renamed
  `flipTextDip` (E2); `Modifier.pressScale` and `slideUpPage` in `ui/kit/MotionKit.kt` (E3);
  `KoanHaptics` everywhere (E4); `SwipeTokens.FLING_DP_PER_S` (E5, `SwipeTokensTest`);
  `Modifier.blockTouches` (part of E9). **Not done, waiting for you:** E6 (`NeighbourDrag`), E7
  (one drag loop) and E8 (one `swipeToDismiss`): the plan needs your feel check on a release build
  after each caller, so they were left for a session with you. E9's system long-press timeout and
  `tapOutside` would change behaviour (not in the allowed list): skipped. R8 build: launches,
  restores after a kill, no crash.
  **Check on the phone:** the player: tap the speed pill, Unlock, Replay (they now shrink on press
  like the round buttons), the level indicator, spinner and 2× badge fades (20 ms longer or
  shorter), the standalone player opening from Downloads; delete a History / Downloads /
  Passwords row with a swipe (now a short haptic), long-press a downloaded language in ⋯ ›
  Translate (now a haptic); a quick short swipe on a library row still deletes / hides; every
  other haptic (toggles, dock scrub, stack, long presses) feels the same; Private's flip still
  dips the dock's text.
- **Group F, logic helpers (done except F6, 10 commits):** `search/Urls.kt` (`isWebUrl`,
  `siteHost`, `baseDomain`; `UrlsTest` first) and its three fixes: an IPv6 address shows whole in
  the pill, a `*.github.io` / `pages.dev`… login stays on its own site, an app-link "Always" holds
  for `m.` / `www.` hosts (old choices still read); `copyText`; `ui/kit/DeviceAuth.kt` for
  Private and Passwords (Passwords now open on a phone without a screen lock); Downloads' Undo tied
  to its toast (`Toast.onGone`, `PendingDeletes`, `UndoWindowTest`): a replaced toast no longer
  deletes the file seconds later; the player's Delete gains Undo (`undoToast`); `PageBridge` under
  `PageEngines` / `PageImages`. **Left out:** the other host call sites (player `siteOf`,
  `ownPlayer`, origins: same results, low value), the size formatters (merging would change shown
  sizes), F6 (two favicon caches: memory only, needs a fair meminfo A/B). R8 build: launches,
  restores after a kill, no crash.
  **Check on the phone:** Downloads: delete a file, then before the toast goes do something that
  shows another toast (copy a link): the file is gone at once and stays gone; delete then Undo:
  it's back; hide then Undo; delete and wait: gone after the toast. The player: open a downloaded
  video, ⋯ › Delete: "File deleted · Undo" brings it back in Downloads. Passwords: unlock with the
  fingerprint as before; copy a username / password. A login saved on one `*.github.io` site is not
  offered on another; one on `accounts.example.com` still is on `example.com`. "Open in app" with
  "Always" on `m.youtube.com`, then a link on `youtube.com`: no card. ⋯ on a page that declares a
  search engine: "Add this search engine"; long-press an image on Google Images: Save image waits
  for the full one.
- **Group G, tokens (G1, G2 done; G3 not started, it waits for your yes):** 13 unused tokens
  deleted; `SharedTokens.Hairline` and `SharedTokens.GroupRadius` behind the five 1dp line tokens
  and the three 18dp group radii; the player's corner radii as `PlayerTokens`. Values unchanged.
  **Check on the phone:** nothing should look different: a sheet, Settings, the search sheet,
  Downloads, the player's pills, bubble, badge and level indicator.
- **Batch 4 overall:** the phone checks, riskiest first: `docs/plans/BATCH4_CHECKS.md`. E6–E8,
  F6 and G3 are in `docs/ROADMAP.md` "Batch 4 leftovers" (after the release). 299 JVM tests, green. Final R8 release build installed on the POCO (real
  app). Merged into `dev` on 2026-11-12 after the owner's tier 1 checks (`a4d39a3`); `refactor` deleted. Open for you: E6–E8 (gesture merges, need your feel check per
  caller), G3, F6, the drawer's small perf drift (group C).

## Open branches

| Branch | Base | What it contains | Status |
|---|---|---|---|
| `release-prep` | dev | v1.0.0-beta.1 prep: batch 5 tests, CI, baseline profile module, update check, README, release docs, the 2026-11-12 fixes. | Pushed up to 8b29879 (draft PR #3); later commits local only; not merged |
| `depth-cue-proto` | release-prep | Depth-cue prototype A/B/C (31e6f5d), switched by `debug.koan.depthcue`. | Local only, **not for release** |
| `dev` | — | Working branch: night batch, video player, private mode, audit batches 1–3, batch 4 and the fixes list merged. | Active |
| `main` | — | Released code, tags `vX.Y.Z`. Never commit here directly. | Release only |
| `private` | dev | Private mode, already merged into `dev` (`06e13d6`). | **Kept on purpose** by the owner; don't delete |
| `statusbar` | dev (old) | Prototype: hide the status bar only while scrolling. | **Dropped** by the owner; kept for reference. Don't merge |

The night-batch branches (`library`, `sync-two-way`, `video`, `nightly`…) and the audit branches
(`audit-fixes`, `audit-docs`, `geckoview-160`) are merged and deleted.

## Two-way sync (M6), as it runs

- Switches in Settings › Mozilla account › Send to your Mozilla account: **Passwords saved on this
  phone** (add only), **Change and delete passwords** (off by default; edits by site + username,
  tombstones for account logins deleted on the phone), **History, both ways** (visits up; pages
  removed on the phone while the switch is on go up as tombstones).
- `data/SyncWrite.kt`, `sync/SyncWritePlan.kt`, `data/HistoryDeletions.kt`: one encrypted backup
  per collection before the first write, `WriteGuard` checks meta/global (engine versions
  passwords 1, history 1; sync ids unchanged), a first batch of 3 read back, then batches of 50
  with X-If-Unmodified-Since. Any error stops that collection and turns its switch off (a cancel
  or a network error keeps it on).
- Never written: bookmarks, tabs, Spaces, prefs, keys, meta/global. The `spaces` collection is
  also blocked in `SyncClient` by `sync/SpacesWriteGate.kt` while `Settings.writeBackEnabled` is
  off (default, not in the UI; CLAUDE.md rule 4). Details: `docs/SYNC_WRITE.md` "As built".
- Run on the owner's real account since 2026-11-10. Phone-made logins use plain UUID ids (no
  braces); Firefox read them back, desktop editing of one not tried.

## Untested / unverified

- **Audit batch 1 on the phone (the owner's checks):** only the pairing card is confirmed. Still to check: Passwords
  screenshots blocked, history removal with History sync on, space icons in the R8 build, private
  favicons, file upload, sign-in again after the WebView wipe.
- **Change and delete passwords:** unit-tested only, never run on the account.
- **Video player:** not yet tried on YouTube, Reddit, X, in dark theme, on Android 11 or on the
  Xiaomi (grey bands seen there in landscape; on HyperOS a drag starting within ~80dp of the top
  edge in landscape is taken by the system bar gesture).
- **Private mode:** not tried: the real 1-min / screen-off lock with a fingerprint, Android 11,
  downloads.
- **"Open in app"** for apps other than YouTube (Instagram, LinkedIn, maps, WeChat…), and the
  "another app" path for a scheme outside the manifest's `<queries>`: not tested on a phone that
  has those apps.
- **Full image on Google Images** (`engine/PageImages.kt`, `assets/extensions/pageimage`): works
  but the owner calls it "not perfect, but better": revisit what it picks and how the wait feels
  (ask the owner what's off).
- **Long screenshots on HyperOS:** still greyed out on GeckoView 160. HyperOS doesn't use
  Android's scroll-capture API (`frameworkSupportLongScreenshot:false`); its own helper gives up
  ("can not run invoke canScrollVertically on background thread"). Option left: Koan's own
  full-page screenshot or Save as PDF (ROADMAP).
- **Vimeo** says "unsupported browser" on a copy where Vimeo's protected-content (DRM) request
  was once blocked: Gecko then refuses without a card. Widevine plays after "Allow once" on a new
  site. Check ⋯ › Site settings on Vimeo in the real app.
- Two rare bugs seen once and never reproduced (ROADMAP "Open items after v0.2.0"): page painted
  only down to where the keyboard was; dock ignoring taps after the phone sat idle.
- The "black screen" when a link goes Discord → GitHub app → Koan is the GitHub app's own App
  Lock, not Koan. Not fixable on our side.

## Pending decisions (the owner's)

1. **v0.4.0 is a pre-release, so `/releases/latest` returns 404.** The What's new sheet's last
   card ("See the full changelog", `LATEST_RELEASE_URL` in `ui/tour/TourContent.kt`) opens that
   URL, so it is broken in the installed 0.4.0. Options: mark v0.4.0 as a normal (latest) release
   on GitHub (fixes it with no new build), or point the link at `/releases` in the next release.
2. **Release step 7 for 0.4.0** (short Discord update post): not known to be done. Ask.
3. **Next release (0.5):** when to cut it from `dev`, and its What's new / tour review
   (CLAUDE.md release checklist).
4. **Audit report:** when to commit `docs/AUDIT.md` and `docs/audits/2026-11-11/` (now gitignored).
5. The audit's other open questions (`docs/AUDIT.md` "Decisions for the owner"): ported Z-Sync
   files mirror upstream or not, button weight and gap, Robolectric for migration / `TabManager`
   tests.
6. The design audit follow-ups below.

## Next concrete steps

In this order (owner, 2026-11-11, changed the same day):

1. ~~**Batch 4**, refactoring~~: merged into `dev` 2026-11-12.
2. ~~**The owner's fixes list**~~: merged into `dev` 2026-11-12 (not pushed yet).
3. **Release 0.5** (CLAUDE.md release checklist; versionCode ≥ 10). It was planned for Friday
   2026-11-13, before batch 4 moved ahead of it: confirm the date with the owner. The owner's
   batch 1 phone checks (list under "Untested / unverified") come before it.
4. After the release: **two-way Spaces sync**, starting at step 0 of
   `docs/plans/SPACES_WRITE_PLAN.md` (reading Zen's own sync source, no code). The plan is written
   and the owner's decisions are in its §9; no code before that step 0.

**Batch 5** (tests, CI, baseline profile) still comes after batch 4; the owner hasn't placed it
relative to the release and the Spaces sync: ask.

Watch on GeckoView 160 meanwhile: the rare bugs above, the space switch p90.

## Build

All from the repo root. Gradle has configuration cache, build cache and parallel on; heap 5 GB
(check `free -g` before raising it).

| Build | Command | Output | Use |
|---|---|---|---|
| dev | `./gradlew assembleDev` | `app/build/outputs/apk/dev/app-dev.apk` | **Every test install on the phone.** Release key, same id (installs over the owner's data), not debuggable, no R8 (~40 s incremental). |
| release | `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release.apk` | Perf checks, real releases, and once at the end of each phase (R8-only crashes). |
| staging | `./gradlew assembleStaging` | `app/build/outputs/apk/staging/app-staging.apk` | Release build as `dev.nerzouille.koan.staging` ("Koan Test"), debug-signed, own data: first-run checks, update-path tests, perf A/B of two versions, the M6 test account. HyperOS asks for on-phone approval of each staging install over USB (`INSTALL_FAILED_USER_RESTRICTED` otherwise). |
| debug | `./gradlew assembleDebug` | `.debug` id | Rarely used; dev menu items only exist here. |

- Install: `adb install -r <apk>`. A downgrade needs an uninstall (wipes data): never on the real
  app.
- arm64-v8a only. `-Pkoan.emulatorAbi` adds x86_64 for emulator runs only. **Never boot an
  emulator without asking the owner** (AVDs `koan30`, `koan31`, `koan34` exist locally; the old
  WebViews of `koan30`/`koan31` can't load Mozilla sign-in).

## Test

- Targeted while working: `./gradlew testDebugUnitTest --tests '*SearchModelTest*'`.
- Whole suite only at the end of a phase: `./gradlew testDebugUnitTest`.
- Never chain tests and a build in one command.
- Sync contract tests run Z-Sync's fixtures from `reference/Z-Sync/shared/contract/fixtures/**`.
  Clone if missing: `git clone --depth 1 https://github.com/Kjell6/Z-Sync.git reference/Z-Sync`.
  Never edit the fixtures.
- **Phones:** the **POCO X4 GT** (Android 14, 1080×2460, one display, its VPN can drop) is the
  **main test phone**: every dev build, check and perf A/B goes there by default (`tools/perf`
  with `CX=540 DOCK_Y=2306 DOCK_DOWN_Y=2455 STACK_TOP=850 STACK_BOTTOM=1780 EDGE_L=180 EDGE_R=900`).
  The **Xiaomi 18 Pro Max** (HyperOS, Android 17, 120 Hz, 1208×2624) is the owner's daily phone:
  mostly the release check and screenshots / recordings. Check `adb devices -l` before installing. The owner allows testing any time, in
  their real spaces, but follow CLAUDE.md "Driving the phone with adb" (Koan in front + fresh
  screenshot before any input). The Xiaomi has several displays: `adb shell screencap` needs
  `-d <main display id>` (`adb shell dumpsys SurfaceFlinger --display-id`). Open throwaway test
  tabs with `adb shell am start -a android.intent.action.VIEW -d <url> dev.nerzouille.koan` and
  close them afterwards. Never close or change the owner's tabs. Never describe private content.
- Always test state restore after process death (HyperOS kills background apps).

## Perf scripts (`tools/perf`)

A/B of two builds on the phone: Koan's own UI frame timing (gfxinfo) and start times. Full guide
in `tools/perf/README.md`. In short:

1. Install build A (R8 release or published APK), open an ordinary page, check `./shot.sh`.
2. `./frames.sh a 5` then `./start.sh a 5`.
3. Install B over A, one warm-up round (`./frames.sh warmup 1`), then `./frames.sh b 5`,
   `./start.sh b 5`.
4. `./compare.py a b`.

Coordinates live in `env.sh` (set for the Xiaomi; override with env vars). `KOAN_PKG=dev.nerzouille.koan.staging`
measures the staging copy. Results go to `app/build/perf/` (gitignored, never commit). Fair
numbers: same build type, same ART compile status, thermal status 0, no scrcpy (or scrcpy for
both). Under ~1 % janky / a few ms p90 is noise. Last A/B (0.3.0 vs 0.4.0, staging copies): no
regression.

## Release

Follow **CLAUDE.md "Release checklist"** step by step and stop at every **→ Stop**. Practical
notes from past releases:

- **Commits:** by default the owner commits; prepare the changes and give the exact `git add` /
  `git commit` commands (Conventional Commits, `type(scope): summary`). Commit, push, tag or
  release yourself only when the owner asks for that step.
- **Signing:** release key outside the repo, PKCS12, alias `koan`; paths and passwords in the
  gitignored `keystore.properties` at the repo root. Never print, copy or commit it. Without that
  file, release builds fall back to the debug key (they then can't update the owner's install).
  Check the cert of a new APK matches the previous one (`apksigner verify --print-certs`).
- **Version:** bump `versionCode` (only goes up; next ≥ 10) and `versionName` in
  `app/build.gradle.kts`.
- **Docs:** `CHANGELOG.md` is detailed; the GitHub release notes are short and hand-written (1–2
  lines per bullet, no implementation details), drafted in `reference/release-notes-X.Y.Z.md`
  (gitignored).
- **What's new sheet:** `ui/tour` (`TourSheet.WhatsNew`, highlights per version, shown once after
  an update via `tour_seen`; Settings › About replays it). Recording rules in CLAUDE.md step 4.
- **Update path:** previous release → new one with data kept and the tour / What's new shown once.
  When a downgrade is in the way, test with two staging copies.
- **Build from `main`:** merge `dev` into `main` (fast-forward), annotated tag `vX.Y.Z` on `main`,
  push both, `./gradlew assembleRelease`, copy the APK to `reference/release-X.Y.Z/koan-vX.Y.Z.apk`,
  `sha256sum` it, then
  `gh release create vX.Y.Z --draft --title "Koan X.Y" --notes-file reference/release-notes-X.Y.Z.md reference/release-X.Y.Z/koan-vX.Y.Z.apk`
  (a normal release: no `--prerelease`; put the SHA-256 in the notes). **Stop: the owner
  publishes.**
- The About screen's update check reads `releases.atom` (pre-releases included), only when About
  opens.

## Local-only material (gitignored, not in the repo)

`reference/` holds Z-Sync, release APKs and notes, the sync-back patch, screenshots and probes.
`app/build/` holds perf results. Recordings for the tour / promo live outside the repo. None of it
is ever committed (CLAUDE.md "Public repo").

## Design audit follow-ups (proposed 2026-11-10, waiting for the owner's pick)

1. Snap the collapsing header to open / collapsed when the finger lifts mid-way (`SettingsPageFrame`).
2. Light `FindMatch` `#D0DDF5` nearly vanishes on grey backgrounds (code chips): stronger value or outline.
3. Per-theme find colours make light pages in dark Koan heavy (navy blocks); per page scheme is a one-line change in `GeckoPrefs.findPrefs`.
4. Light toggle OFF `#848489` looks heavy: lighter fill + thin darker outline would also pass 3:1.
5. About shows "About" then "Koan": start that page collapsed, or drop its large title.
6. ~~Tour clips (cards 2 and 5) show real data~~: the owner approved them for release (2026-11-12).

## Private mode (merged into `dev`, 2026-11-11)

Decisions (owner, 2026-11-11): real GeckoView private mode; private tabs come back after a restart
(URL + Back history, sealed), signed out. utm_* and click IDs stripped in Private; cookie-banner
refusal optional (off); Google AMP links rewritten in Private (option for normal tabs); DoH
optional, off, all tabs; WebRTC local addresses hidden (all tabs, always); bounce-tracking
protection on (all tabs: GeckoView can't scope it, a setting); fingerprinting protection on (not
RFP); HTTPS-only + tracking protection in Private with a per-site Protections switch (⋯); GPC in
Private; lock after 1 min away / screen off; FLAG_SECURE with "Allow screenshots"; a hidden
FLAG_SECURE switch in dev builds only; uBlock allowed in private; Koan's copies marked sensitive in
Private; private downloads listed with a mask (until restart).

Not done / open:
- **HTTPS-only fallback:** GeckoView has no API for an HTTP exception and shows nothing when a
  site has no HTTPS (the page just stays): Koan shows a toast, "Not opened in Private: no HTTPS".
  The Protections switch does not lift HTTPS-only.
- Gecko strips tracking parameters only on link clicks: Koan's request hook (`LinkCleaner`) also
  cleans typed and pasted links in private tabs.
- Fixed on the way: the video player turned the system bars' contrast scrim back on after
  fullscreen (`ui/player/Immersive.kt`), a pale strip over Private's colours; now always off.
- **Cookie banners** use Gecko's `cookiebanners.service.mode.privateBrowsing` pref (GeckoView 157
  has no API left): unverified whether its rules still ship.
- **ToggleOff** stays `#848489` / `#8E8E93` for all toggles (the spec's `#636366` fails 3:1 on dark
  grouped rows); the Private toggle uses the spec's colours.
- Private downloads leave the list on restart (AC never stores private downloads).
- The "Private" chip on private-tab rows in the search sheet (§4.6) isn't drawn (every private tab
  is in the one set).
- Star placement avoids the card pile's full width, not each back card's narrower shape.
- No Android 11 emulator image on the dev machine (only `koan34`).
- Killed while Private is on screen: Koan comes back on the first space and overwrites the saved
  place (audit 8.3, not fixed yet). Baked blurs surviving the wipe (8.7) are fixed in batch 1.

