# Night plan 2026-11-12 → 13: release prep for v1.0.0-beta.1 (unattended)

## Context

Batch 4 and the fixes list are merged into `dev`. The next release becomes **v1.0.0-beta.1**
(instead of 0.5). It is prepared tonight without the owner and **not published**. Before the
release, batch 5 has to be done: tests for the risky paths that have none, CI, and a baseline
profile. Then the README rewrite and the captures.
The night's work goes on the **`release-prep`** branch made from `dev`. The plan is committed first
as `docs/plans/NIGHT_PLAN.md` (this file's content), and the night report is added at the end of
that same file.

What I found while reading:
- `UpdateCheck.parse` drops everything after `-`, so `v1.0.0-beta.1` is read as `1.0.0`. A beta
  user would then be told they are up to date when 1.0.0 comes out, and the update label would lose
  "-beta.1". The releases Atom feed doesn't say which releases are pre-releases. The only sign is
  the tag suffix.
- `LATEST_RELEASE_URL = …/releases/latest` returns 404 while every release is a pre-release (HANDOFF
  pending decision 1). The beta would have the same bug.
- `featureVersion("1.0.0-beta.1")` gives `"1.0"`, so the What's new key is `"1.0"` and works for
  every beta and the final 1.0.
- Robolectric and baselineprofile are already listed as "allowed, not used yet" in
  `docs/ARCHITECTURE.md`.
- Room is at v8 with schemas 1–8 exported in `app/schemas/` and `MIGRATION_2_3` … `MIGRATION_7_8`
  defined. Migrations have never been run in a test (audit 9.2).
- **There are no DataStore migrations** (no `SharedPreferencesMigration` or `DataMigration`). Only
  `SafeStores.resetOnCorruption` and the defaults can be tested.
- `PrivateMode` reads `SystemClock.elapsedRealtime()` directly, with no clock seam. PiP is handled
  only implicitly: the timer starts in `MainActivity.onStop` (the comment is at
  `MainActivity.kt:302-306`).
- The sync write tests that exist cover only the pure plans (`SyncWritePlanTest`,
  `SpacesWriteGateTest`, `SyncBackPlanTest`). Nothing covers `data/SyncWrite.kt`,
  `SyncClient.postBatch` or `data/HistoryDeletions.kt`. `FakeHttpTransport`
  (`sync/SyncTransportTests.kt:422`) and `SyncClient`'s internal transport constructor make
  postBatch testable.
- There is no `.github/workflows`. The Z-Sync contract tests skip silently without
  `reference/Z-Sync` (audit 9.4).

## Owner's answers (2026-11-12, before the night)

1. **Spaces:** every space except one personal space (named in the gitignored `CLAUDE.local.md`) may be shown, including folder and essential names and
   the drawer. Public sites allowed on pages: Wikipedia, Wikimedia Commons and the Koan GitHub repo.
   Everything else comes from the local demo pages. A space switch or swipe must never land on or
   show that space's tabs. If its name or icon appears in a strip, check the frame and say so in the
   report; skip the shot if its tabs show.
2. **Git:** commit locally on `release-prep`, one commit per step, no trailers. Push
   `release-prep` **once**, at the end of the code work, so CI runs, and record the CI result.
   Nothing else is pushed: no tags, no releases.
3. **Dependencies:** room-testing approved. The baselineprofile set (plugin, benchmark-macro-junit4,
   uiautomator, profileinstaller) is approved.
4. **POCO:** staging and `.bp` copies may be installed, never over the real app. The owner sets the
   phone up before sleeping.
5. **Xiaomi:** the release-prep dev build may go over the real app for the captures. DND is on
   during the run and put back afterwards.
6. **What's new:** I pick the 4 highlights, stills only, no clips. **README recordings:** card
   stack and space switch, kept raw only (`reference/captures-1.0.0-beta.1/`); the owner approves
   them in the morning, so they are not added to `docs/media/` tonight.
7. **Update rule** approved.
8. **1.0.0-beta.1 replaces 0.5.** The CHANGELOG date stays unset.

**Unattended:** I don't wait for the owner. I stop the night only on a failing test that a fix
within its own step doesn't resolve, or on anything risky (an unexpected screen on a phone, an
install that would go over the real app on the POCO, a diff with personal data). The other timebox
cases are reverted and written down, and the night goes on.

## Rules for the night (on top of CLAUDE.md)

- **Branch:** only `release-prep`. Never touch `main`, push only `release-prep`, once, never
  tag, never create a GitHub release.
- **Commits:** one per step, Conventional Commits, no Claude trailers. Before each commit, check the
  diff for anything personal (allowed, owner's answer 2).
- **Memory:** tests are targeted per step (`--tests '*X*'`). The full suite runs once at the end of
  the code work. Never chain tests and a build in one command. Check `free -g` before heavy steps.
- **POCO:** only the **staging** id (`dev.nerzouille.koan.staging`) and the baseline profile's own
  id are installed on it. Nothing goes over the real app. Before every install,
  `aapt2 dump badging` must show that the applicationId is not `dev.nerzouille.koan`; otherwise
  abort.
- **Any adb input:** the `guard` from `tools/perf/env.sh` (Koan copy in front) and a fresh
  screenshot first. If something is unexpected (another app, a call, a dialog), stop that step and
  note it.
- **Timebox:** if a step fails 3 times in a row, revert it to its last green state, write it down in
  the report and move on. A half-working change is never left in the branch.
- **No network calls from tests or demo pages to third parties:** demo pages and test downloads are
  served from this machine through `adb reverse tcp:8765 tcp:8765` (`python3 -m http.server`, files
  in the scratchpad).

## Order

0 → 1a–1e → 4-code → 2 → checks → 3 → 4-docs → 5 → report. The baseline profile is the last piece
of code work, so it profiles the final 1.0 code.

### 0. Setup
- `git switch -c release-prep dev`. Write `docs/plans/NIGHT_PLAN.md` and commit it
  (`docs(plans): night plan for release prep`).
- Check `java -version`, `free -g` and `adb devices -l`. Clone Z-Sync if it is missing.

### 1a. Sync write tests (JVM)
- `sync/SyncClientPostBatchTest` (`FakeHttpTransport`):
  - `X-If-Unmodified-Since` is on every POST, and records are split into batches.
  - A 412 stops the run and nothing more is posted.
  - The `spaces` collection is refused while `writeBackEnabled` is off, and the transport is never
    called.
  - `bookmarks`, `tabs`, `meta`, `crypto` and `prefs` are never posted on any path.
- `data/SyncWriteTest`:
  - The encrypted backup is written before the first POST.
  - A `WriteGuard` refusal (engine version or sync id changed) means no POST and the switch turned
    off.
  - The first batch is 3 records and is read back, then batches of 50.
  - A server error stops that collection and turns its switch off. Cancel or a network error keeps
    the switch on.
  - Passwords are add-only unless "Change and delete" is on.
  - History tombstones are sent only for pages removed while the switch was on.
- **Seams:** if `SyncWrite` or `HistoryDeletions` need a `Context`, the files folder or the store
  are added as constructor parameters with today's values as defaults. There is no behaviour change;
  the refactor is its own commit, before the tests. `HistoryDeletions` gets Robolectric if
  extracting a seam isn't clean.

### 1b. Room migrations (Robolectric + `androidx.room:room-testing`, see Q3)
- `data/KoanDatabaseMigrationTest` uses `MigrationTestHelper` with the exported schemas
  (`app/schemas` added as test assets). It covers:
  - each step 2→3 … 7→8 with fake rows (example.com, made-up titles) that must survive;
  - the full chain 2→8;
  - 1→8 using the destructive fallback.
- DAO smoke tests on an in-memory database for the queries that run the history, tabs and
  bookmarks screens.
- Robolectric is set to `@Config(sdk = [34])` (it may not support 37 yet).
- `unitTests.isIncludeAndroidResources = true` only if needed.

### 1c. DataStore (no migrations exist; I will say so in the report)
- `SafeStores`: a corrupted file is reset, and the next read gives the defaults.
- `Settings` defaults that matter for safety:
  - `writeBackEnabled = false`;
  - the three sync-write switches off;
  - `privateLock` on.
- The value written is the value read back (Robolectric or a temp-file DataStore).
- `tourSeen`: an update from "0.4" to "1.0.0-beta.1" shows What's new "1.0" once.

### 1d. Private mode
- **Seam:** `PrivateMode(now: () -> Long = SystemClock::elapsedRealtime)`. The screen-off receiver
  calls a plain `onScreenOff()`. This is its own commit, with no behaviour change.
- `privacy/PrivateModeLockTest` covers:
  - hidden for less than 60 s: no lock;
  - hidden for 60 s or more: locked;
  - screen off: locked at once;
  - **PiP:** the PiP window visible (no `onStop`) never starts the timer; the PiP window closed or
    Audio only (`onStop`) starts it; screen off during PiP locks;
  - the lock switch off: never locked.
- If the PiP logic lives in `MainActivity`, I extract the decision into a pure function next to
  `PrivateMode` and test that.
- **Encrypted storage** (`SealedFile`, `PrivateVault`):
  - the file on disk does not contain any plaintext URL or title;
  - a wrong key or a corrupted file reads as empty, without a crash, and the next save works;
  - a round trip keeps the Back history.
- **Kept separate:**
  - The star in a private tab goes to `PrivateBookmarks` and never to the normal bookmark store (a
    test on the routing function, extracted if it is inline).
  - Private bookmarks are not in normal search.
  - Private downloads: `DownloadFilter` both ways, and the duplicate middleware skips private
    downloads.

### 1e. GitHub Actions: `.github/workflows/ci.yml`
- It runs on `push` and `pull_request` to `dev` and `main`, with `permissions: contents: read`, no
  secrets, no signing and a concurrency group.
- Steps:
  1. checkout;
  2. `actions/setup-java` with Temurin 17 (or whatever AGP 9.4 needs);
  3. `gradle/actions/setup-gradle`;
  4. install SDK platform 37 ext 2 with `sdkmanager` if the runner doesn't have it;
  5. clone Z-Sync at a pinned commit (public, MIT) into `reference/Z-Sync`;
  6. `./gradlew testDebugUnitTest`;
  7. `./gradlew assembleDebug` as a separate step (debug-signed, no keystore);
  8. upload the test reports if the run fails.
- Actions are pinned by tag (or SHA). The heap is lowered for the 16 GB runner with a `-D` override
  in CI only.
- Contract tests **fail** in CI instead of skipping (`-Pkoan.requireContract=true`; audit 9.4). They
  still skip locally.
- Checked with `actionlint` if it is installed. **It can't run on GitHub until it is pushed**, and
  the report says so.

### 4-code. Release code for v1.0.0-beta.1
- `app/build.gradle.kts`: `versionCode = 10`, `versionName = "1.0.0-beta.1"`.
- `UpdateCheck` gets pre-release-aware semver:
  - order: `1.0.0-beta.1 < 1.0.0-beta.2 < 1.0.0-rc.1 < 1.0.0`;
  - the label keeps the suffix;
  - **rule (Q7):** a user on a stable version ≥ 1.0.0 is not offered pre-releases; a user on a
    pre-release or on 0.x is offered the newest release of either kind.
  - Tests go in `UpdateCheckTest` (extended or created).
- `LATEST_RELEASE_URL` changes to the current version's tag page
  (`…/releases/tag/v${BuildConfig.VERSION_NAME}`), which works for pre-releases too.
- What's new `"1.0"` in `ui/tour/TourContent.kt`:
  - 4 highlights proposed with drawn or still scenes first: Private browsing, Koan's video player +
    PiP, two-way sync for passwords and history, page translation (or private bookmarks).
  - Clips are added only if approved (Q6).
  - Tests: `dueSheet("0.4", "1.0")` gives WhatsNew("1.0"); a "1.0.0-beta.2" install after beta.1
    shows nothing.

### 2. Baseline profile (last of the code work)
- **Setup:**
  - New module `:baselineprofile` (`com.android.test` + the `androidx.baselineprofile` plugin,
    benchmark-macro-junit4, uiautomator).
  - In the app: the `androidx.baselineprofile` consumer plugin and `profileinstaller`.
  - The generated build types (`nonMinifiedRelease` / `benchmarkRelease`) get their **own
    applicationId suffix** (`.bp`) and the debug key, so they can never go over the real app. This
    is checked with `aapt2` before the first install.
- **Journeys:**
  - cold start, with the tour dismissed;
  - open the overview and scroll the stack;
  - open a tab;
  - the search sheet;
  - a space switch (with a local space only);
  - the ⋯ menu and Settings.
  - Pages come from the local demo server (no network).
- **Generation:** generate on the POCO, then check that `baseline-prof.txt` is in
  `app/src/release/generated/…` and is a sensible size.
- **Measuring:** `tools/perf` on **staging copies** with the POCO coordinates (HANDOFF "Test"):
  - A = release-prep without the profile, B = with the profile, both R8.
  - Interleaved runs A/B/A/B, thermal status 0, 5 runs each of `frames.sh` and `start.sh`.
  - Two states:
    - **Just after install** (launch once, kill): what a sideload update feels like.
    - **After `cmd package compile -f -m speed-profile`**.
  - `compare.py` results go to `app/build/perf/` (never committed). APK size before and after.
- If B is not better beyond noise, the profile stays in but the report says so plainly.

### Checks (end of the code work)
- Full `./gradlew testDebugUnitTest`.
- Then, separately, `./gradlew assembleRelease` (R8) from `release-prep`, with the cert checked
  against v0.4.0 (`apksigner`).
- **Staging R8 copy on the POCO:** launch, kill in the background, restore, no crash.
- **Update path** with two staging copies:
  - v0.4.0, built in a worktree from the tag, is installed first;
  - 1.0.0-beta.1 goes over it;
  - data is kept, What's new "1.0" is shown once and not again after a restart, and About's update
    check shows sensible text.
- The APK and its SHA-256 go to `reference/release-1.0.0-beta.1/` (gitignored).

### 3. README.md rewrite (neutral, nothing personal)
- Sections:
  - **What Koan is**, with the non-affiliation line.
  - **Features**, taken from `docs/FEATURES.md`.
  - **Screenshots**, as a light/dark grid.
  - **Install:**
    - the APK from the Releases page (`/releases`, not `/latest`) and how to check its SHA-256;
    - Obtainium, with the "include pre-releases" note;
    - Android 11+, arm64 only, the APK size from the checks.
  - **Privacy summary**, a short version of `docs/PRIVACY.md` with a link to it.
  - **Sync**: what is read and what is written, and that it is opt-in.
  - **Build:**
    - JDK, SDK, the Z-Sync clone and the Gradle tasks;
    - `keystore.properties` is optional;
    - tests and the CI badge.
  - **Licence:** MIT, plus `THIRD_PARTY_NOTICES.md` (GeckoView MPL-2.0, Z-Sync MIT).
- No first person and no "my phone".
- Images are placeholders such as `<!-- IMG docs/media/overview-light.png -->` and are filled in
  after step 5.
- `docs/screenshots/` is removed in the same commit as the new images arrive, and only then.

### 4-docs. Release documents
- **`CHANGELOG.md`:** "Unreleased" becomes "1.0.0-beta.1 (date to be set)", with sections New /
  Improved / Fixed / Known limitations, plus batch 5 under Improved.
- **Also updated:** `docs/FEATURES.md` (only for user-visible changes tonight), `docs/PRIVACY.md`
  (no new network call is expected; I check it), and the README size.
- **Release notes:** `reference/release-notes-1.0.0-beta.1.md`, short and hand-written, with the
  SHA-256. A Discord version of about 5 lines goes in `reference/discord-1.0.0-beta.1.md`. Both are
  gitignored.
- **CLAUDE.md release checklist, new pre-release case:**
  - the tag is `vX.Y.Z-beta.N` / `-rc.N`;
  - `gh release create --prerelease`;
  - `/releases/latest` doesn't point to it;
  - `versionCode` still goes up;
  - What's new is keyed by feature version, so a later beta shows nothing unless the highlights
    change;
  - Obtainium users need "include pre-releases";
  - the final release after betas reuses the CHANGELOG section.
  - HANDOFF's Release section gets the same.
- **Tour review (release step 2):** each card in `ui/tour` checked against the app. The list of
  outdated, missing and droppable cards goes in the report, and nothing is changed without the
  owner. Design follow-up 6 (cards 2 and 5 show real data) is flagged for re-recording.
- `docs/HANDOFF.md` and `docs/ROADMAP.md` status are updated.

### 5. Captures on the Xiaomi (last)
- **Wait for the phone:** a Monitor until-loop on `adb devices -l` that waits for the Xiaomi's
  model (POCO ignored). Then the display id comes from `dumpsys SurfaceFlinger --display-id`.
- **Before anything else:**
  - Koan is in front and a fresh screenshot is checked. If the phone is locked or shows anything
    else, stop and note it.
  - Note the current `cmd uimode night`, `show_touches` and Koan's theme setting, so they can be
    put back exactly.
  - DND on during the run, if Q5 allows it.
- **Build:** the release-prep dev build is installed over the real app (Q5).
- **Spaces:** only the ones named in Q1, and only in those spaces. Test tabs are opened with
  `am start -a VIEW -d <demo url>` and closed afterwards. The owner's tabs are never closed,
  reordered or opened.
- **Never shown:**
  - Passwords, History, the Mozilla account page;
  - any screen with the email;
  - the drawer if the folder names aren't approved.
  - Downloads only with test files served locally (`koan-test.pdf`, `sample.jpg`, `clip.mp4`),
    deleted from Downloads afterwards.
- **Shot list (each in light and dark,** through `cmd uimode night yes|no` with Koan on Follow
  system):
  - overview stack;
  - page + dock;
  - search sheet;
  - space switcher;
  - ⋯ menu;
  - Private mode (empty and with a demo tab);
  - video player on a public test video (served locally);
  - Downloads with test files;
  - Settings top level.
- **Recordings:** only those approved in Q6, with Show taps on and turned off right after.
- **Every image is opened and checked:**
  - anything personal (other tab titles in the stack, notifications, the status bar, the account
    avatar) means delete it, don't describe it, and retake or skip it;
  - status bar: try SystemUI demo mode (a clean clock with no icons); if HyperOS ignores it, crop
    the status bar.
- **Where files go:** raw files go to `reference/captures-1.0.0-beta.1/` (gitignored). The ones
  chosen for the README are resized to 1080 wide and put in `docs/media/` with stable names
  (`overview-light.png` …).
- **Put back:** night mode, `show_touches`, DND and Koan's theme as they were. Close the test tabs
  and delete the test downloads.
- Then fill in the README placeholders and `git rm docs/screenshots/`.

### Report
- Added to the end of `docs/plans/NIGHT_PLAN.md`, one section per item:
  - what works and what was tested;
  - what failed or was skipped, and why;
  - perf numbers in brief;
  - commits made (or the commands to make them);
  - the owner's decisions left: What's new pick, tour cards, recordings, publish.
- `HANDOFF.md` updated.
- Owner checks for the morning, riskiest first.

## Critical files
- `data/SyncWrite.kt`, `sync/SyncClient.kt`, `sync/SpacesWriteGate.kt`, `data/HistoryDeletions.kt`
- `data/KoanDatabase.kt`, `app/schemas/**`, `data/SafeStores.kt`, `data/Settings.kt`
- `privacy/PrivateMode.kt`, `privacy/SealedFile.kt`, `privacy/PrivateVault.kt`,
  `privacy/PrivateBookmarks.kt`, `ui/downloads/DownloadFilter.kt`, `MainActivity.kt` (onStop / PiP)
- `update/UpdateCheck.kt`, `ui/tour/TourContent.kt`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `settings.gradle.kts`, new `baselineprofile/`
- `.github/workflows/ci.yml`, `README.md`, `CHANGELOG.md`, `CLAUDE.md`, `docs/HANDOFF.md`

## Verification
- Targeted tests per step, then the full suite (count reported: 346 → N).
- R8 release build. Staging launch / kill / restore on the POCO.
- Update path 0.4.0 → 1.0.0-beta.1 on staging copies.
- Perf A/B before and after the baseline profile.
- `actionlint` on the workflow.
- A personal-data check of every diff and every image.

## Everything still open (HANDOFF, ROADMAP, FIXES, docs/plans)

`docs/FIXES.md` itself is closed: all 10 items are done, rechecked and merged. Its remaining
follow-ups are listed below. `docs/AUDIT.md` was read only for the items the docs point to; its
"Decisions for the owner" list is summarised as one line.

### Before beta.1
- **Tonight's work:** batch 5 (tests, CI, baseline profile), the README, release prep and the
  captures.
- **Push `dev`:** the fixes merge `0477033` and the two handoff commits are not on origin.
- **Your audit batch 1 phone checks** (HANDOFF says before the release):
  - Passwords can't be screenshotted;
  - removing history with History sync on;
  - space icons in the R8 build;
  - private favicons;
  - file upload;
  - signing in again after the WebView wipe.
  Only the pairing card is confirmed so far.
- **Batch 4 phone checks** in `BATCH4_CHECKS.md` beyond tier 1: root state after process death,
  overlays, the player, the drawer, buttons in light, dark and Private.
- **Pending decision 1:** v0.4.0 users' "See the full changelog" goes to `/releases/latest`, which
  is a 404. Tonight's code fixes it for beta.1, but the installed 0.4.0 keeps the dead link unless
  v0.4.0 is marked as Latest on GitHub.
- **Pending decision 2:** was the 0.4.0 Discord post done?
- **Pending decision 4:** when to commit `docs/AUDIT.md` + `docs/audits/2026-11-11/`. The security
  fixes are merged, so it could go with the beta.
- **Release checklist steps for you:**
  - approve the release notes;
  - pick the tour cards and What's new;
  - approve the recordings;
  - merge `dev` → `main`, tag `v1.0.0-beta.1`, draft as a pre-release, publish;
  - the Discord post.
- **Design follow-up 6:** tour cards 2 and 5 show real data; re-record them before a public
  release.
- **Small audit items worth doing at release time:**
  - 10.4: keep `mapping.txt` for each release in `reference/`;
  - 10.7: make the release build fail instead of signing with the debug key without saying so.

### During the beta (check, watch, small fixes)
- **Change and delete passwords:** unit-tested only, never run on the account.
- **Video player:** not tried yet on YouTube, Reddit or X, in dark theme, on Android 11 or on the
  Xiaomi (grey bands in landscape, the HyperOS top-edge gesture). From the fixes list: F2 / F4 on
  the Xiaomi, the play button during buffering, F10's sealed bookmarks file.
- **Private mode:**
  - not tried yet: the real 1-min / screen-off lock with a fingerprint, Android 11, downloads;
  - **killed while Private is on screen, Koan comes back on the first space** (audit 8.3, not
    fixed): a real bug, fix it during the beta;
  - the cookie-banner pref is unverified; the HTTPS-only toast stays the fallback;
  - minor: no "Private" chip in the search sheet, and the star placement.
- **"Open in app"** with apps other than YouTube, and a scheme outside `<queries>`.
- **Vimeo:** check ⋯ › Site settings there (DRM was once blocked).
- **Google Images full image:** ask what's off.
- **Watch on GeckoView 160:** the page painted only down to where the keyboard was, the dock
  ignoring taps, the space switch p90 (12 → 17 ms), the drawer's perf drift since group C.
- **Design audit follow-ups 1–5** (for you to pick): snap the collapsing header, `FindMatch` light
  contrast, find colours per page scheme, the light toggle OFF colour, About's double title.

### After the beta
- **Two-way Spaces sync:** step 0 of `docs/plans/SPACES_WRITE_PLAN.md` (read Zen's sync source, no
  code), then the plan. Then M6 bookmarks, and decide whether the backups are enough without an
  undo journal.
- **Batch 4 leftovers:** E6–E8 (gesture merges, your feel check per caller), F6 (one favicon cache,
  meminfo A/B), G3 (tokens).
- **Video player:** a quality choice for Download video.
- **Tests left out of tonight's scope** (audit 9.3 / 9.5): `TabManager`, `RootStateTest` (needs
  interfaces), and extracting and testing intent parsing.
- **M4, not started:** stack compression / elastic trailing, new-tab grow, close collapse,
  JankStats, search backdrop retry.
- **M7 ideas:** Glance, open in background, open in another space, Send tab to PC.
- **Open items after v0.2.0:** Desktop site, Add to home screen, pull to refresh in chats, long
  screenshots (own capture or Save as PDF).
- **Audit "Decisions for the owner":** whether ported Z-Sync files mirror upstream, the lint
  baseline (10.3).
- **Branches** `private` and `statusbar`: kept as they are, as you decided.

## Night report (2026-11-12, 06:00–07:30 local)

Branch `release-prep` (from `dev`), 15 commits, pushed once to origin. Nothing on `main`, no tag,
no release.

### Done
- **1a Sync write tests:** `FakeStorage` (an in-memory Sync storage server) +
  `SyncClientPostBatchTest` (7) + `SyncWriteTest` (16, Robolectric). No production change needed.
- **1b Room:** Robolectric 4.17, room-testing 2.8.5, androidx.test (test-only).
  `KoanDatabaseMigrationTest` (7: each step 2→8, the chain through the real DAO, v1 rebuilt) and
  `KoanDaoVisitsTest` (4). The schemas are added to the **debug** source set's assets (the only
  assets unit tests see; never in dev, staging or release APKs).
- **1c DataStore:** there are no DataStore migrations to test. An internal `Settings(store)`
  constructor + `SettingsTest` (3): the safety defaults (write-back off, sync switches off, lock
  on), also after a corrupted file, and restarts.
- **1d Private:** a clock seam in `PrivateMode` + `onScreenOff()`; `localHits()` pulled out of the
  search composable (no behaviour change). Tests: `PrivateModeLockTest` (11, PiP included),
  `SealedFileTest` (5), `PrivateSearchTest` (3), `DuplicateDownloadsPrivateTest` (2).
  **One fix found:** a normal download's "same file" card could point at a private download
  (`fix(downloads)`).
- **1e CI:** `.github/workflows/ci.yml` (actions pinned by SHA, read-only token, Z-Sync at a pinned
  commit, `-Pkoan.requireContract=true` so missing fixtures fail). **Not run yet:** it triggers on
  `dev` / `main` and on PRs to them, not on a push of `release-prep`. Open a draft PR
  `release-prep → dev` to run it.
- **4 code:** `versionCode 10` / `1.0.0-beta.1`; semver `UpdateCheck` with the approved rule
  (13 tests); the What's new link goes to the version's tag page; What's new "1.0" with 4
  text + icon cards (Private, video player, translation, two-way sync).
- **2 Baseline profile:** the `:baselineprofile` module, profileinstaller, the generated build
  types under their own id `dev.nerzouille.koan.bp` (checked with aapt2), journeys guarded to run
  only with that copy in front, and local demo pages through `adb reverse`. **No profile
  generated:** see Blocked.
- **Checks:** full suite **411 tests, 0 failures, 0 skipped** (346 before). R8 release build OK,
  same certificate as v0.4.0, 87/87 Zen icons, 219 124 488 bytes. APK, SHA-256 and `mapping.txt`
  are in `reference/release-1.0.0-beta.1/` (built from `release-prep`: rebuild from `main` at
  release).
- **3 README** rewritten; the image cells are placeholder comments.
- **4 docs:** CHANGELOG (section `1.0.0-beta.1`, date unset), FEATURES, the CLAUDE.md
  pre-release case, release notes and a Discord post in `reference/` (gitignored).

### Blocked (needs the owner)
- **POCO installs:** MIUI asks for an on-phone confirmation for every new package over USB and
  cancels it after a timeout (`INSTALL_FAILED_USER_RESTRICTED`), even with Install via USB on.
  Tapping that system dialog through adb is outside the rules, so these are not done:
  - baseline profile generation;
  - the perf A/B (APK A without a profile is saved in `reference/perf-bp/`);
  - the staging R8 launch / kill / restore;
  - the 0.4.0 → beta update-path test.
  Next time: watch the POCO and tap Install for `dev.nerzouille.koan.bp`, the test APK and the
  staging copies.
- **Xiaomi captures (step 5):** the phone was connected but another app was in front (in use), so
  nothing was done on it. No screenshots or recordings yet; the README images and the What's new
  stills are still to do.

### Tour review (release step 2)
- **Outdated:** cards 2 (Spaces) and 5 (hold and slide) show real data in their clips (design
  follow-up 6): re-record them before a public release. Their text is still right.
- **Still right:** Welcome, swipe up, swipe sideways, search shortcuts, long presses.
- **Worth a card:** Private browsing (the header switch); maybe the video player's gestures.
- **Could go:** none.

### What's new "1.0" (release step 3)
Four highlights, stills only: Private browsing, Koan's video player (+ PiP), Translate a page,
Two-way sync. Planned stills from the captures: the private overview, the player on a Wikimedia
Commons video, a translated Wikipedia page. The sync card stays text-only (its screen shows the
email).

### State left on the phones
- POCO: "Stay awake while plugged in" turned on through adb (`svc power stayon usb`, was 0); put it
  back with `adb -s <poco> shell settings put global stay_on_while_plugged_in 0`. `adb reverse
  tcp:8765` is set, and a local demo server is running on this machine.
- Xiaomi: untouched (night mode auto, show taps off, DND off).

## Morning session (2026-11-12, 10:55–12:50, owner present)

### POCO (owner tapping Install)
- **Why the night's installs failed:** Gradle's installer is cancelled at once by MIUI; a plain
  `adb install` waits for the owner's tap. Updates of a package already on the phone go through
  without a prompt. The generator was then run by hand (`am instrument`, `targetAppId=…​.bp`).
- **Baseline profile:** the first run gave R8-renamed classes: AGP 9's `optimization {}` block
  stays on in the plugin's `nonMinified…` types (the plugin only turns off `isMinifyEnabled`).
  Fixed with `androidComponents.finalizeDsl` in `app/build.gradle.kts`. Second run: 28,469
  rules, 5,303 on Koan's own classes, nothing personal (class and method rules only). Committed
  in `app/src/main/generated/baselineProfiles/` (5a6815b). APK +0.4 MB.
- **Perf A/B** (staging copies, interleaved A/B/A/B, thermal 0 throughout; A = no app profile,
  B = with it):
  - cold start just after install: A 292 / 319 ms, B 273 / 300 ms (about −6 %);
  - after `compile -m speed-profile`: cold start A 259 / 274, B 251 / 258 ms, with a tighter
    max (263 / 301 against 310 / 321); warm start the same;
  - page ↔ overview ("open"): janky A 5.6 / 7.0 %, B 3.2 / 5.0 %, p90 15 / 19 → 13 / 15 ms;
  - dock, drawer, stack scroll, space switch: within noise (one B round had a space p90 of 19 ms,
    the other 12 ms).
  Small but consistent; the profile stays.
- **Staging R8 copy with the fixes** installed over staging 0.4.0: What's new "1.0" shown once
  (4 cards checked), "See the full changelog" opens the tag page (404 until published), launch,
  kill in the background and restore on the same tab without a crash, What's new not shown again.
  Release step 5 done on the POCO.

### Xiaomi (dev build over the real app, ~15 min)
- The owner allowed the space excluded at night in captures this morning (real tab titles still stay out of README
  images).
- **Bug found and fixed: stuck on the start page after What's new.** The start page closed only
  through `LaunchedEffect(editing)`; on the first launch after the update the start page opened
  and What's new closed the field in the same frame, so the effect never fired and the start page
  stayed until a restart. Closing the field now clears it in `RootState`'s setter (cb65dd4,
  `RootStateEditingTest`).
- **Bug found and fixed: "Translated · show original" on every page, Translate did nothing.**
  Android Components 160 always builds a `TranslationPair`, with null languages on a page that
  isn't translated; Koan counted any pair (c4c6b11, `TranslateStateTest`). Checked on the POCO
  staging copy: a French demo page translates.
- **Not reproduced:** a page video's full-screen button didn't open the player on the Xiaomi; the
  same page and button work on the POCO. Owner to try a real video on the Xiaomi.
- **Captured** (raw, `reference/captures-1.0.0-beta.1/`, all checked): the tab stack, a page and
  the ⋯ menu in light and dark (demo tabs only), the translation offer card, and (POCO) a
  translated page. **Not done:** Private (FLAG_SECURE: needs the dev switch in Settings ›
  Privacy), the player, search (HyperOS blocks `adb input text`), Settings, the two recordings.
- Minor: the stack keeps a stale card preview after a theme change until the tab is opened.
- The owner unplugged the Xiaomi before its settings were put back: night mode (left on light,
  was auto), DND (left on), SystemUI demo mode (left on). The owner restores them by hand.

### Owner request: the translation offer on every French page
The offer shows for any page whose language isn't the phone's first language. Proposed (waiting
for the owner): never offer a language from the phone's language list, and an "Offer
translations" switch in Settings › Translation. Meanwhile "Never for French" on the card.

## Afternoon (2026-11-12): translation offer, CI, captures, rotation, Private switch

- **CI:** draft PR #3 (`release-prep` → `dev`, the pushed head 8b29879): `build` passed in 6 min 34 s.
  The later local commits aren't pushed (owner: no push).
- **Translation offer** (eb1c7d6): no card for any language in the phone's list (`fr-CA` is French),
  and Settings › Translation › Offer to translate pages.
- **Captures (Xiaomi):** search, Private empty and with a Wikipedia tab, the player, plus the two raw
  recordings (the owner allows their real tabs). No Settings image (the account card shows the email).
- **Rotation blocker** (4555612): not reproduced through adb (21 enter/leave rounds 0–2.5 s apart,
  40 back-to-back changes 50–800 ms apart, on the Xiaomi; 15 more on the POCO's R8 copy): every layer
  always ended at the display's shape. It needs the phone to turn. Safety net: after each fullscreen
  change `LayoutWatch` compares the display with the window, the root view, Compose and Gecko's
  surface, logs a stale layer (`adb logcat -s KoanSize`) and repairs (relayout → attributes and
  orientation again → a new activity, after leaving only); leaving always re-applies portrait and
  relayouts once the configuration is portrait. Silent when the sizes are right.
- **Private switch** (b8ed0ec), recorded before and after on the Xiaomi with the same 10 taps:
  before, 6 taps dead (three guards, and `running` held by the 1.32 s star clock), the slide jumped
  to its end after a slow frame (the cut and the left strip), and coming back replayed the first-sync
  reveal (blank cards, because it read the private models). After: every tap acts and the screen
  ends on the last one; one position on the pager's spring, retargeted from where it is, at most
  20 ms per frame; a fixed tap target at the toggle's place while the headers slide; Private's page
  composed off screen once the overview settles; the reveal reads the normal models. POCO R8: 9
  fast taps end in Private, the 10th in normal, no crash.
- 428 JVM tests green. R8 release: same certificate as before, 219,583,298 bytes.

### Recheck on the phones (owner, riskiest first)
1. **Rotation, auto-rotate on:** a landscape video fullscreen; turn the phone to landscape and back,
   leave fullscreen while held each way, and right after a turn; enter again right after leaving
   (the player fills the screen). If Koan ever stays at the wrong size, wait 2 s (it repairs), and
   send `adb logcat -d -s KoanSize`.
2. **Rotation, auto-rotate off:** turn the phone so the rotate pill shows, tap it, leave fullscreen
   while the pill shows; leave during the black resize.
3. **Private switch:** spam it on the overview, both directions: it always reacts, never a dead
   tap, ends on the last state; no blank cards coming back; no strip, no cut.
4. **Locked Private** (1 min away): the knob moves at once, the fingerprint is asked; cancel puts
   the knob back.
5. **Remove animations** on: a 150 ms slide both ways.
6. **Overview open** in normal mode: it now composes Private's page once settled; check it feels the
   same (`tools/perf` "open" if in doubt).
7. **Translation (reworked in the evening, below):** see the evening list.

## Evening (2026-11-12): a quieter translation offer, APK size

- **Translation offer** (76950fb, 53a507c), owner's rules: by default only a translate icon that
  pops into the end of the address pill (spring scale + fade, layer only) on a page in a language
  that isn't on the phone; a tap opens the card. The card opens by itself only when
  `TranslateCue` says so: Firefox's reader-view check passes (article-length text, not mostly
  links, forms or media), the page language is sure (Gecko gives `documentLangTag` only from the
  page's declaration or a confident detection), the page isn't a results page of any engine in the
  owner's list (`isSearchResults`: the engine's site in any country domain, its path and query
  parameter), and the card wasn't dismissed on that site since Koan started (✕, a tap outside or a
  drag; the 20 s timeout doesn't count). "Offer to translate pages" off hides both.
  Found on the phone: Gecko's own `isOfferTranslate` fires once per site and session, so it can't
  be the confidence signal (an article after a short page of the same site never got its card).
  Checked on the POCO R8 staging copy: links page → icon only; article → card; ✕ or a tap outside
  → the next article on that site gets the icon only; a tap on the icon → the card; after a restart
  the site gets its card again. Search pages are covered by unit tests only (no real queries sent
  from the phone). 434 JVM tests green.
- The staging copy on the POCO has German set to "Never" (a stray test tap): Settings ›
  Translation › Remove, if it matters for a later test.
- **APK size:** 219.6 MB against 215.9 MB for v0.4.0 (+3.7 MB, +1.7 %): GeckoView 157 → 160
  (+2.46 MB, libxul +2.2 MB), dex +0.98 MB (new code ~0.53 MB; the startup profile splits the
  dex, ~0.45 MB), the Zia icon set `assets/zia/*.json` +0.34 MB, omni.ja +0.14 MB. One ABI
  (arm64-v8a), libs stored uncompressed and 16 KB aligned in both, no debug sections, no Room
  schemas, fixtures, raw profiles or mapping files. Only ~60 KB of library licence/version files,
  already in 0.4.0, left as they are (the Apache licences are notices).

### Recheck: translation (owner)
1. A news article or docs page in a language not on the phone: the icon pops in and the card opens
   by itself; ✕ it, open another article of that site: icon only.
2. Google / DuckDuckGo results in another language: icon only, never the card.
3. A short page or a page of links in another language: icon only; tapping it opens the card.
4. A French page (French on the phone): no icon, no card.
5. Settings › Translation › Offer to translate pages off: no icon, no card anywhere; ⋯ › Translate
   page still works.
