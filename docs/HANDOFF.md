# Handoff

State of Koan for a session that starts with no context. Last updated **2026-11-12**.
Read `CLAUDE.md` first (rules), then this file, then `docs/ROADMAP.md` (status of every item).

## Where things stand

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
  4. Refactoring, from **`docs/plans/BATCH4_BRIEF.md`** (branch `refactor` from `dev`): **next**,
     before the release (see "Next concrete steps").
  5. Tests, CI, baseline profile: after batch 4.
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
- Unit tests: 301 JVM tests on `dev`, green (`./gradlew testDebugUnitTest`). R8 staging build of
  `dev` + 160 launched on the POCO without a crash (2026-11-11).

## Open branches

| Branch | Base | What it contains | Status |
|---|---|---|---|
| `dev` | — | Working branch: night batch, video player, private mode, audit batches 1–3 merged. | Active |
| `main` | — | Released code, tags `vX.Y.Z`. Never commit here directly. | Release only |
| `fix/system-passkeys` | dev | System WebAuthn permissions, lifecycle-owned result routing, abandoned-result draining and Android Autofill. Credential UI uses the Activity context and a retained logical host; user dismissal is distinct from DOM abort. The pinned GeckoView 160 adapter preserves remote-only preparation results, with no generated callback class number. Design and limits: `docs/PASSKEYS.md`; source comparison/install identity: `docs/PASSKEYS_REVIEW.md`. | PR #2. 308 app + 11 adapter JVM tests (no skips), 15 Android integration tests. Debug and final ARM64 R8 flows passed creation/P-256 assertion, discoverable login, locked-supplier unlock handoff, nonmatching credential IDs with prompt failure/retry, pre-abort/invalid RP, user dismissal, provider errors/malformed responses, default links, bound Custom Tabs and system Autofill with GMS disabled. Preview .5 → .6 installed in place. Remote-only routing is checked through transformed bytecode and all candidate flag combinations; physical QR/security keys and real-device/provider interoperability remain unverified. |
| `private` | dev | Private mode, already merged into `dev` (`06e13d6`). | **Kept on purpose** by the owner; don't delete |
| `statusbar` | dev (old) | Prototype: hide the status bar only while scrolling. | **Dropped** by the owner; kept for reference. Don't merge |

The night-batch branches (`library`, `sync-two-way`, `video`, `nightly`…) and the audit branches
(`audit-fixes`, `audit-docs`, `geckoview-160`) are merged and deleted.

**Passkey preview hold (2026-11-12):** a user reports that invoking 1Password from Koan forces
account-password unlock for both Autofill and passkeys, although Edge and 1Password itself
still support biometrics. Root cause and first affected build are unknown. Do not issue a new
"fixed" preview or count synthetic provider results as real biometric acceptance. Investigate
this interoperability failure before further passkey polish; see `docs/PASSKEYS.md`.

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

1. **Batch 4**, refactoring, from `docs/plans/BATCH4_BRIEF.md` (branch `refactor` from `dev`).
2. **The owner's fixes list** (the owner brings it).
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
6. Tour clips (cards 2 and 5) show real data: re-record before a public release.

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

