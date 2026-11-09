# Handoff

State of Koan for a session that starts with no context. Last updated **2026-11-09**.
Read `CLAUDE.md` first (rules), then this file, then `docs/ROADMAP.md` (status of every item).

## Where things stand

- **Latest release: v0.4.0** ("Koan 0.4 (beta)"), tagged on `main` at `d4f3a2e`, published on
  2026-10-30 as a GitHub **pre-release**. versionName `0.4.0`, versionCode `9`: the next release
  needs versionCode **≥ 10**.
- `main` and `dev` point at the same commit (plus this handoff on `dev`). Nothing unreleased on
  `dev` except docs.
- Engine: android-components / GeckoView **157** (`gradle/libs.versions.toml`, the two versions
  move together). GeckoView 160 is due on 2026-11-10 (see "Next steps").
- Milestones done and tagged: `m0`, `m1`, `m2`, `m3`, `m5`. M4 partly done (no tag), M6 started on
  a branch, M7 partly done. Details per item in `docs/ROADMAP.md`.
- Unit tests: ~230 JVM tests, green at v0.4.0 (`./gradlew testDebugUnitTest`).

## Open branches

| Branch | Base | What it contains | Tested | Status |
|---|---|---|---|---|
| `dev` | — | Working branch. Same code as `main` (v0.4.0) + docs. | — | Active |
| `main` | — | Released code, tags `vX.Y.Z`. Never commit here directly. | — | Release only |
| `sync-back` | dev (behind it by ~20 commits) | **M6 step 1:** sync-back **dry run** for passwords, history and bookmarks. `sync/SyncBackPlan.kt` (what would be uploaded: `LoginUpload`, `HistoryUpload`, `BookmarkUpload`, `WriteGuard`), `data/SyncBack.kt` (dry run only: logs counts and field names, never values, **has no send path**), opt-in setting `Settings.syncBack` (off), DAO queries for phone-only visits and bookmarks, `SyncClient.metaGlobal`, switches on the Account page. Tests `SyncBackPlanTest`, `SyncBackTest` (fake data). | JVM tests green when made (2026-10-27). **Never run against a real account**, never on the phone. Needs a rebase on `dev` before more work. | Waiting for the owner's test Mozilla account + Zen test profile |
| `statusbar` | dev (behind it by ~16 commits) | Prototype: **hide the status bar only while scrolling** (`DockState.statusHidden` / `statusShift`, `PageTokens.STATUS_SETTLE_FRAMES`). | Checked frame by frame on a release build (2026-10-28): works, but a fixed bottom bar can still be off by one frame (Gecko's redraw timing varies; `CompositorController.addDrawCallback` never fires in GV 157 release). | **Dropped** by the owner; kept for reference. Don't merge. Revisit only if GeckoView adds a draw callback or top-toolbar support (ROADMAP M7 "Display"). |

## Untested / unverified

- **sync-back:** nothing has been sent anywhere, by design. The upload formats in
  `SyncBackPlan.kt` follow `docs/SYNC_WRITE.md` but have never been checked by a real Firefox/Zen
  client.
- **Writes to the Zen `spaces` collection:** not written at all yet (CLAUDE.md rule 4).
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
   The release checklist says releases are normal, not pre-releases.
2. **Release step 7 for 0.4.0** (short Discord update post for the owner's thread): not known to
   be done. Ask.
3. **M6 start:** the owner creates a throwaway Mozilla account and a separate Zen profile and says
   when the phone is free. Decisions already made: writes use the QR sign-in token; history =
   phone visits only; bookmarks go to "Mobile Bookmarks"; defaults after testing: passwords +
   history on, bookmarks off at first, tabs/Spaces off. Order: passwords → history + bookmarks →
   Zen Spaces (`docs/SYNC_WRITE.md`).
4. **What comes next** after GeckoView 160: M4 (motion polish), M6 (write-back) or M7 items. The
   owner picks; don't start a milestone unasked.
5. `.gitignore` has an uncommitted local line (`design-export/`, a local design-system export).
   The owner commits it or not.

## Next concrete steps

1. Ask the owner about pending decisions 1–2.
2. **GeckoView / android-components 160** (released 2026-11-10): bump both in
   `gradle/libs.versions.toml` on a branch from `dev` (`gv-160`), check maven.mozilla.org for the
   exact matching GeckoView build, fix API breaks, full test suite, R8 release on the phone, then
   retest: HyperOS long screenshot (GV 160 adds `ScrollCaptureCallback`), the status-bar draw
   callback question, and the rare bugs above. Size of the APK (`README.md`) may change.
3. Then whichever milestone the owner chooses (ROADMAP lists the next step for each).

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
