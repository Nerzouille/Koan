# Handoff

State of Koan for a session that starts with no context. Last updated **2026-11-11**.
Read `CLAUDE.md` first (rules), then this file, then `docs/ROADMAP.md` (status of every item).

## Where things stand

- **Latest release: v0.4.0** ("Koan 0.4 (beta)"), tagged on `main` at `d4f3a2e`, published on
  2026-10-30 as a GitHub **pre-release**. versionName `0.4.0`, versionCode `9`: the next release
  needs versionCode **≥ 10**.
- **`dev` is ahead of `main`** with the night batch of 2026-11-10, reviewed item by item with the
  owner and merged on 2026-11-11 (`CHANGELOG.md` "Unreleased"; specs and verdicts in
  `docs/REVIEW_NIGHT.md`, what was built and how it was tested in `docs/NIGHT_REPORT.md`):
  library pages (selection mode, swipes, Passwords look, add a password), two-way sync for
  passwords and history (**M6**, see below), page translation, "Open in app" cards, the duplicate
  download card and the morphing action card, Back close animation, toast hand-over, Zia icons,
  targeted package queries instead of `QUERY_ALL_PACKAGES`, full image on Google Images.
- Engine: android-components / GeckoView **157** (`gradle/libs.versions.toml`, the two versions
  move together). 160 was still not released on 2026-11-11 (betas only on maven.mozilla.org).
- Milestones done and tagged: `m0`, `m1`, `m2`, `m3`, `m5`. M4 partly done (no tag), M6 passwords
  + history done on `dev` (Spaces, tabs, bookmarks never written), M7 partly done. Details per
  item in `docs/ROADMAP.md`.
- Unit tests: 269 JVM tests, green on `dev` (`./gradlew testDebugUnitTest`). R8 release of `dev`
  built and launched on the POCO without a crash (2026-11-11).

## Open branches

| Branch | Base | What it contains | Tested | Status |
|---|---|---|---|---|
| `dev` | — | Working branch, everything below merged (`video` too, 2026-11-11). | See above. | Active |
| `main` | — | Released code, tags `vX.Y.Z`. Never commit here directly. | — | Release only |
| `video` | dev (rebased 2026-11-11) | **Video player** from `docs/VIDEO_PLAYER.md` plus the owner's feedback of 2026-11-11: `ui/player/*` (one `VideoPlayer`, web and standalone; standalone on MediaPlayer), `engine/PageVideo.kt` + `assets/extensions/video` (hides the site's player in fullscreen, blocks its exit, state + commands, and the media notification's position / poster for every playing video), edge-to-edge black window (`ui/player/Immersive.kt`), symmetric safe-area layout, filled primary icons, level slider = its own track, PiP with Audio only. Notification timeline: feature-media's Nimbus flag switched on by reflection (`Components.enableMediaTimeline`, keep rules in `proguard-rules.pro`). **No mini-player** (ROADMAP M7; old version in `reference/mini-player-2026-11-11/`). | POCO (dev + R8 release): Commons, test-videos.co.uk; glyph centring measured on screenshots. Not yet: YouTube, Reddit, X, dark theme, Android 11 emulator, the Xiaomi (grey bands seen there in landscape; on HyperOS a drag starting within ~80dp of the top edge in landscape is taken by the system bar gesture). | Approved by the owner, merged into `dev` 2026-11-11 (`000d4a1`, pushed). `video` can be deleted. |
| `library`, `sync-two-way`, `app-links`, `download-dup`, `translate`, `toast-swap`, `back-close`, `zen-icons` | dev | The night batch items, merged into `dev`. `download-dup` and `sync-two-way` contain `library`. | Retested by the owner on the POCO. | Merged; can be deleted. |
| `nightly` | dev | The batch's test branch (all items + video). Local only, never pushed (its doc history has personal phone details). | — | Delete once `video` is redone. |
| `statusbar` | dev (old) | Prototype: hide the status bar only while scrolling. | Frame by frame, 2026-10-28. | **Dropped** by the owner; kept for reference. Don't merge. |

## Two-way sync (M6), as it runs

- Switches in Settings › Mozilla account: **Passwords saved on this phone** (add only), **Change
  and delete passwords** (off by default; edits by site + username, tombstones for account logins
  deleted on the phone), **History, both ways** (visits up, phone deletions as tombstones).
- `data/SyncWrite.kt`, `sync/SyncWritePlan.kt`, `data/HistoryDeletions.kt`: one encrypted backup
  per collection before the first write, `WriteGuard` checks meta/global (engine versions
  passwords 1, history 1; sync ids unchanged), a first batch of 3 read back, then batches of 50
  with X-If-Unmodified-Since. Any error stops that collection and turns its switch off.
- Never written: bookmarks, tabs, Spaces, prefs, keys, meta/global (CLAUDE.md rule 4 still holds
  for `spaces`).
- Run on the owner's real account since 2026-11-10. Phone-made logins use plain UUID ids (no
  braces); Firefox read them back, desktop editing of one not tried.

## Untested / unverified

- **Change and delete passwords:** unit-tested only, never run on the account.
- **"Open in app"** for apps other than YouTube (Instagram, LinkedIn, maps, WeChat…), and the
  "another app" path for a scheme outside the manifest's `<queries>`: not tested on a phone that
  has those apps.
- **Full image on Google Images** (`engine/PageImages.kt`, `assets/extensions/pageimage`): works
  but the owner calls it "not perfect, but better": revisit what it picks and how the wait feels
  (ask the owner what's off).
- **Long screenshots** (HyperOS scroll screenshot) on GeckoView 160: not tried yet.
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
3. **The video player design** (Claude Design): when it arrives, rebuild on `video`.
4. **Next release (0.5):** when to cut it from `dev`, and its What's new / tour review
   (CLAUDE.md release checklist).
5. `.gitignore` has an uncommitted local line (`design-export/`). The owner commits it or not.

## Next concrete steps

1. Ask the owner about pending decisions 1, 2 and 4.
2. **GeckoView / android-components 160** when it's released: bump both in
   `gradle/libs.versions.toml` on a branch from `dev` (`gv-160`), fix API breaks, full test suite,
   R8 release on the phone, then retest the HyperOS long screenshot and the rare bugs above.
3. The video player from the owner's design.

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
- On the phone: the owner's only phone is now the **Xiaomi 18 Pro Max** (Android 17, 120 Hz,
  1208×2624); the POCO X4 GT is retired. The owner allows testing on it any time, in their real
  spaces, but follow CLAUDE.md "Driving the phone with adb" (Koan in front + fresh screenshot
  before any input). It has several displays: `adb shell screencap` needs
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

The old POCO X4 GT (1080×2460, one display, its VPN can drop) is available for unattended tests; its Koan runs the R8 release of `dev` (2026-11-11).
