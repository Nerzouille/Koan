# CLAUDE.md — Koan

Koan is a personal Android web browser with an Arc-for-iPhone look, built around **Zen Browser's Spaces**.
The owner (Noa) uses Zen on a Linux desktop and wants the same Spaces, tabs, passwords and history on
an Android phone, with a much better tab/workspace UI than Firefox mobile.

This is a **single-user, sideloaded app**. No Play Store, no analytics, no other users to support.
Optimise for: feel (animation quality), daily-driver reliability, and simplicity of code. Skip anything
the owner won't personally use.

> "Koan" is a working codename. Never call the app "Zen", never use the Zen logo. Zen is an
> independent project and this app is not affiliated with it.

## Public repo

This repository is **public**. Everything committed (code, docs, tests, commit messages) is public.

- Never commit or mention the owner's personal browsing data: real tab titles/URLs, space names,
  folder names, sites they visit, account emails, Mozilla/Zen data, phone screenshots, logcat/adb
  dumps, trace files.
- Commit messages describe the change generically ("blank screenshot on a slow page"), never with
  the owner's real tabs or sites.
- Test data, seed data and fixtures must be fake (`example.com`, made-up titles).
- Measurements, screenshots and logs stay in gitignored folders (`app/build`, `reference/`,
  `docs/reference/`, `design-export/`) and are never committed.
- Before every commit, check the diff for anything personal; if in doubt, ask the owner.
- Personal context needed locally goes in a gitignored file (`CLAUDE.local.md`), not in the repo.

## Read these before doing anything

| File | What it is |
|---|---|
| `docs/VISION.md` | What we're building and why, feature list, what's out of scope |
| `docs/UI_SPEC.md` | Screens, card stack, workspace bar, gestures, motion specs, visual tokens |
| `docs/ARCHITECTURE.md` | Stack, modules, data flow, tab lifecycle, performance budgets |
| `docs/SYNC.md` | How Zen Spaces sync works (Firefox Sync `spaces` collection) and how we reuse Z-Sync |
| `docs/ROADMAP.md` | Milestones with acceptance criteria and a **Status** table (done / in progress / next step). |
| `docs/FEATURES.md` | What the app does today, screen by screen (user-facing) |
| `docs/PRIVACY.md` | Every network call Koan makes (public promise: keep it true) |
| `docs/DESIGN_SYSTEM.md` | Tokens, components and screens as they are in the code |
| `docs/SYNC_WRITE.md` | Two-way sync (passwords, history): as built, record formats, risks |
| `docs/VIDEO_PLAYER.md`, `docs/design/OVERVIEW_PRIVATE.md` | Design hand-overs for the video player and private mode, with deviations as built |
| `docs/HANDOFF.md` | **Start here in a new session:** current release, open branches, untested parts, pending owner decisions, build / test / perf / release how-to. Keep it up to date at the end of every session. |

The canonical Zen sync wire format lives in Z-Sync's repo: `reference/Z-Sync/shared/contract/SPEC.md`
(+ JSON fixtures). Clone it there if it's missing:
`git clone --depth 1 https://github.com/Kjell6/Z-Sync.git reference/Z-Sync` (gitignored, read-only).

## Stack (do not change without asking)

- Kotlin, Jetpack Compose (Material 3 only as a base — the look is custom), single-activity.
- **GeckoView** engine (Firefox's engine, same as Zen) via **Mozilla android-components**
  (`browser-engine-gecko`, `browser-state`, `feature-*`). Check maven.mozilla.org for the current
  release and keep android-components and GeckoView on matching versions.
- Room for local data, DataStore for settings, kotlinx.coroutines/Flow, `org.json` for sync records.
- Sync layer: ported from Z-Sync's Android `sync/` package (MIT). See `docs/SYNC.md`.
- minSdk 30 (Android 11+, since 2026-10-23; the owner's phones run Android 14 and 17). Below Android 12 there is no
  live blur, and phones with less RAM get smaller budgets (`DeviceProfile`, `docs/ARCHITECTURE.md`).
  Target the latest stable SDK.

## Working rules

1. **One piece of work at a time:** a roadmap item or batch, on its own branch from `dev`. Finish
   it, then stop and summarise what works, what doesn't, and what you'd do next. Don't start the
   next one unasked.
2. **UI before plumbing inside each milestone** when the milestone has UI: get something on screen
   the owner can feel on the phone early.
3. **Ask before adding a dependency** that isn't listed in `docs/ARCHITECTURE.md`.
4. **Never write to the Zen `spaces` collection** unless the `writeBackEnabled` setting is on
   (default off, not in the UI yet). `sync/SpacesWriteGate.kt` enforces it for every request.
   Reading is always safe; writing can scramble the owner's desktop Spaces. Tabs and bookmarks are
   never written either. Passwords and history are written only through their own Settings ›
   Mozilla account switches (M6, `docs/SYNC_WRITE.md`).
5. **Secrets:** never log, print, commit or put in crash output: `kB`, session tokens, Hawk keys,
   OAuth tokens, passwords. Store them only via Android Keystore-backed encryption.
   Never commit `local.properties`, keystores or signing configs.
6. **Ported code:** files ported from Z-Sync keep a header comment
   `// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends` and the
   MIT text goes in `THIRD_PARTY_NOTICES.md`.
7. **Contract tests:** the sync layer must pass Z-Sync's golden fixtures
   (`reference/Z-Sync/shared/contract/fixtures/**`) as JVM unit tests. Don't edit the fixtures.
8. **Performance is a feature.** Judge animations on a **release build** (R8 on, debuggable off) on
   the real phone, never on a debug build or emulator. Respect the budgets in `docs/ARCHITECTURE.md`.
9. Animations: animate only `graphicsLayer` properties (translation, scale, alpha, clip, corner
   radius) in hot paths; never trigger recomposition or relayout per frame.
10. Keep files small and feature-packaged (`ui/overview`, `ui/browser`, `sync`, `data`, `engine`…).
11. UI strings in English. Code comments in English.
12. Prefer simple, readable code over clever abstractions. This is a one-person app.

## Git branches (since 2026-10-27)

- `main` = what is released; it stays GitHub's default branch. **Never commit or push to `main`
  directly.**
- `dev` = where work happens. Every change goes to `dev`, or to a feature branch made from `dev`
  (e.g. `sync-back`) that is merged back into `dev` when done.
- **Release:** merge `dev` into `main`, tag on `main` (`vX.Y.Z`), build the release from `main`.
- **Urgent fix on a published version:** branch from `main` (e.g. `fix/…`), merge it into `main`
  (patch release, tagged) and into `dev`.
- Start every report to the owner with the branch you're on (`git branch --show-current`).
- **Commits:** by default the owner commits. Prepare the changes and give the exact `git add` /
  `git commit` commands. Commit, push, tag or release yourself only when the owner asks for it.
  Message style: Conventional Commits, `type(scope): summary`.

## Release checklist

Every release follows these steps, in this order. **→ Stop** marks a point where you wait for the
owner's approval before going on.

1. **Docs:** `CHANGELOG.md` (user-facing, sections New / Improved / Fixed / Known limitations),
   release notes, `docs/FEATURES.md`, `README.md` (APK size, requirements), `docs/PRIVACY.md` if
   the release adds any network call. **→ Show the owner the release notes draft.**
2. **Tour review:** go through every tour card (`ui/tour`) and check it still matches the app
   (gestures, wording, clips). List which cards are outdated, which new features deserve a card,
   and which cards could go. **→ Put it in the summary for the owner to choose.**
3. **"What's new" sheet:** decide whether the release needs one: only if it has user-visible
   highlights, never for a fix-only release. If yes, propose 3–5 highlights, each with one line on
   how to use it, and say which ones need a clip (only gestures or flows that are hard to explain
   in words). **→ Put it in the summary for the owner to choose.**
4. **Recordings:** propose the recordings you want to make (new What's new clips, outdated tour
   cards to re-record) **→ and ask the owner about them**: what may be shown, which demo pages or
   tabs to open. Then record only what was approved, with the same rules as the tour: on the
   owner's phone only, Show taps on, public demo pages, never the owner's email or personal tabs,
   check every frame, Show taps off afterwards.
5. **Technical checks:** full unit test suite; full R8 release build from `main`; update path tested
   on the phone (previous release → new one: data kept, tour / What's new shown once);
   `tools/perf` against the previous release on the phone.
6. **Release:** merge `dev` into `main`, tag on `main` (`vX.Y.Z`), draft the GitHub release (a
   normal release, not a pre-release) with the APK and its SHA-256. **→ Stop: the owner publishes.**
7. **After the owner publishes:** draft a short Discord update post for the owner's thread.

## Dev environment

- Owner's machine: Linux (EndeavourOS, Hyprland). Android Studio / Gradle CLI.
- Test devices (check `adb devices -l` for which one is connected):
  - **POCO X4 GT** (Dimensity 8100, Android 14): the **main test phone**. Every dev build and
    check goes there by default.
  - **Xiaomi 18 Pro Max** (HyperOS, Android 17, 120 Hz): the owner's daily phone and the
    **performance reference**; used for perf checks and releases. It has several displays:
    `adb shell screencap` needs `-d <main display id>`. HyperOS caps refresh rate for unknown apps
    and kills background apps aggressively: check the real refresh rate before blaming the code.
  - On both, always test state restore after process death.
- **Test builds (follow every time):**
  - Every test install on the phone is a **dev** build: `./gradlew assembleDev`, then
    `adb install -r app/build/outputs/apk/dev/app-dev.apk`. Release key, same applicationId (installs
    over the owner's data), not debuggable, but no R8 / no resource shrinking: an incremental
    change + install takes ~40 s instead of ~80 s. (A `staging` build type also exists, with its
    own app id and data: see `docs/HANDOFF.md` "Build".)
  - A full R8 **release** build (`./gradlew assembleRelease`) only for: performance checks
    (animations, frame stats), real releases, and once at the end of each phase before merging,
    installed and launched on the phone, to catch R8-only crashes (e.g. SnakeYAML in C1).
  - Never chain unit tests and a build in one command. Run targeted tests for the code you touched
    (`./gradlew testDebugUnitTest --tests '*SearchModelTest*'`), and the whole suite
    (`./gradlew testDebugUnitTest`) only at the end of a phase.
  - Gradle: configuration cache, build cache and parallel are on (`gradle.properties`); heap is
    5 GB on a 15 GB machine that often has ~2 GB free: don't raise it without checking `free -g`.
- **Driving a phone with adb (taps, swipes, typing, key events):** both phones hold the owner's
  real data and the Xiaomi is their daily phone, so it may be in use. Before ANY adb input, confirm both:
  1. Koan is the foreground app (`dumpsys activity activities | grep topResumedActivity` shows
     `dev.nerzouille.koan`; `mCurrentFocus=null` does not count), and
  2. a fresh screenshot shows exactly the screen you expect (right tab, right space, nothing else
     going on such as a call or the owner mid-task).
  If either fails, stop and ask the owner to hand over the phone. When in doubt, ask instead of
  tapping. Never type into a page you didn't open yourself. If a screenshot shows another app or
  private content, delete it and don't describe it. Read-only adb (installs when asked, logcat,
  dumpsys, screenshots) is fine.
