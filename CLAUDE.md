# CLAUDE.md — Koan

Koan is a personal Android web browser with an Arc-for-iPhone look, built around **Zen Browser's Spaces**.
The owner (Noa) uses Zen on a Linux desktop and wants the same Spaces, tabs, passwords and history on
an Android phone, with a much better tab/workspace UI than Firefox mobile.

This is a **single-user, sideloaded app**. No Play Store, no analytics, no other users to support.
Optimise for: feel (animation quality), daily-driver reliability, and simplicity of code. Skip anything
the owner won't personally use.

> "Koan" is a working codename. Never call the app "Zen", never use the Zen logo. Zen is an
> independent project and this app is not affiliated with it.

## Read these before doing anything

| File | What it is |
|---|---|
| `docs/VISION.md` | What we're building and why, feature list, what's out of scope |
| `docs/UI_SPEC.md` | Screens, card stack, workspace bar, gestures, motion specs, visual tokens |
| `docs/ARCHITECTURE.md` | Stack, modules, data flow, tab lifecycle, performance budgets |
| `docs/SYNC.md` | How Zen Spaces sync works (Firefox Sync `spaces` collection) and how we reuse Z-Sync |
| `docs/ROADMAP.md` | Milestones with acceptance criteria. **Work one milestone at a time.** |

The canonical Zen sync wire format lives in Z-Sync's repo: `reference/Z-Sync/shared/contract/SPEC.md`
(+ JSON fixtures). Clone it there if it's missing:
`git clone --depth 1 https://github.com/Kjell6/Z-Sync.git reference/Z-Sync` (gitignored, read-only).

## Stack (do not change without asking)

- Kotlin, Jetpack Compose (Material 3 only as a base — the look is custom), single-activity.
- **GeckoView** engine (Firefox's engine, same as Zen) via **Mozilla android-components**
  (`browser-engine-gecko`, `browser-state`, `feature-*`). Check maven.mozilla.org for the current
  release and keep android-components and GeckoView on matching versions.
- Room for local data, DataStore for settings, kotlinx.coroutines/Flow, kotlinx.serialization.
- Sync layer: ported from Z-Sync's Android `sync/` package (MIT). See `docs/SYNC.md`.
- minSdk 34 (owner's phones run Android 14+). Target the latest stable SDK.

## Working rules

1. **One milestone at a time** from `docs/ROADMAP.md`. Finish its acceptance criteria, then stop and
   summarise what works, what doesn't, and what you'd do next. Don't start the next one unasked.
2. **UI before plumbing inside each milestone** when the milestone has UI: get something on screen
   the owner can feel on the phone early.
3. **Ask before adding a dependency** that isn't listed in `docs/ARCHITECTURE.md`.
4. **Never write to the Zen `spaces` collection** until milestone M6, and then only behind the
   `writeBackEnabled` setting (default off). Reading is always safe; writing can scramble the
   owner's desktop Spaces.
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

## Dev environment

- Owner's machine: Linux (EndeavourOS, Hyprland). Android Studio / Gradle CLI.
- Test devices: POCO X4 GT (Dimensity 8100, 144 Hz, HyperOS, Android 14) is the reference for
  performance: if it's smooth there, it's smooth everywhere. Soon also a Xiaomi 18 Pro Max.
  HyperOS caps refresh rate for unknown apps and kills background apps aggressively: check the real
  refresh rate before blaming the code, and always test state restore after process death.
- Build: `./gradlew assembleDebug` for iteration, `./gradlew assembleRelease` (signed with a local
  debug-style keystore) for anything performance-related.
- Run unit tests: `./gradlew test`. Run them at the end of a milestone, not after every edit.
- **Driving the phone with adb (taps, swipes, typing, key events):** the phone is the owner's daily
  phone and may be in use. Before ANY adb input, confirm both:
  1. Koan is the foreground app (`dumpsys activity activities | grep topResumedActivity` shows
     `dev.nerzouille.koan`; `mCurrentFocus=null` does not count), and
  2. a fresh screenshot shows exactly the screen you expect (right tab, right space, nothing else
     going on such as a call or the owner mid-task).
  If either fails, stop and ask the owner to hand over the phone. When in doubt, ask instead of
  tapping. Never type into a page you didn't open yourself. If a screenshot shows another app or
  private content, delete it and don't describe it. Read-only adb (installs when asked, logcat,
  dumpsys, screenshots) is fine.
