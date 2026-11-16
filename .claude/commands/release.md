---
description: The full release checklist (normal release or beta / release candidate), stopping at every owner check
argument-hint: "<version, e.g. 1.0.0 or 1.0.0-beta.2>"
---

Release **$ARGUMENTS** of Koan. `CLAUDE.md` "Release checklist" is the source of truth; this is the
procedure with the practical notes from `docs/HANDOFF.md` "Release". Start the report with
`git branch --show-current`. At every **→ Stop**, wait for the owner's answer before going on.

**Pre-release?** The version has `-beta.N` or `-rc.N`. Then:
- the tag is `vX.Y.Z-beta.N`;
- `gh release create` gets `--prerelease`;
- the release notes say that Obtainium users need **Include prereleases**;
- links point to `/releases` or the version's own tag page, never `/releases/latest` (GitHub skips
  pre-releases there; see `releaseUrl()` in `ui/tour`).

A final release after betas reuses the betas' CHANGELOG section: rename its heading, add what
changed since the last beta, and publish it as a normal release.

## 0. Version
- Bump `versionCode` (always +1, even for a beta) and `versionName` in `app/build.gradle.kts`.
  This is done on `dev`.

## 1. Docs → Stop
- `CHANGELOG.md`: user-facing, with the sections New / Improved / Fixed / Known limitations.
- The release notes draft in `reference/release-notes-X.Y.Z.md` (gitignored): short and
  hand-written, 1–2 lines per bullet, no implementation details.
- Also update:
  - `docs/FEATURES.md`;
  - `README.md` (APK size, requirements);
  - `docs/PRIVACY.md`, if the release adds a network call.
- **→ Show the owner the release notes draft.**

## 2. Tour review → Stop
- Go through every tour card (`ui/tour`): do its gestures, wording and clip still match the app?
- List which cards are outdated, which new features deserve a card, and which cards could go.
- **→ Put the list in the summary for the owner to choose.**

## 3. "What's new" sheet → Stop
- Show it only if the release has user-visible highlights, never for a fix-only release.
- If it does, propose 3–5 highlights, each with one line on how to use it, and say which ones need
  a clip (only gestures or flows that are hard to explain in words).
- What's new is keyed by feature version (`"1.0"`). A later beta of the same version shows
  nothing, unless you bump the key for a real new feature version.
- **→ Put the proposal in the summary for the owner to choose.**

## 4. Recordings → Stop
- Propose the recordings: new What's new clips and outdated tour cards to re-record.
- **→ Ask the owner** what may be shown and which demo pages or tabs to open.
- Record only what was approved, following `/captures`:
  - on the owner's phone only;
  - Show taps on (off again afterwards);
  - public demo pages, never the email or personal tabs;
  - check every frame.

## 5. Technical checks
- The full unit test suite: `./gradlew testDebugUnitTest` (on its own, not chained with a build).
- A full R8 release build from `main` (step 6 order), installed and launched on the phone.
- The update path on the phone: previous release → new one, with data kept and the tour / What's
  new shown once. If a downgrade is in the way, use two staging copies.
- `tools/perf` against the previous release, on the POCO (see `tools/perf/README.md`), with the
  same build type, thermal status 0, no scrcpy.
- Report each result. A failing check means stop and tell the owner.

## 6. Release → Stop
- The owner commits by default: give the exact `git add <files>` / `git commit` commands, with no
  Claude trailers. Merge, tag and push only when the owner asks for that step.
- Merge `dev` into `main` (fast-forward), then put an annotated tag `vX.Y.Z[-beta.N]` on `main`.
- `./gradlew assembleRelease` from `main`. The release key comes from the gitignored
  `keystore.properties`; never print it.
- Check the cert matches the previous release: `apksigner verify --print-certs`.
- Copy the APK to `reference/release-X.Y.Z/koan-vX.Y.Z.apk` and run `sha256sum` on it. The SHA-256
  goes into the notes.
- Create the draft:
  `gh release create vX.Y.Z --draft --title "Koan X.Y" --notes-file reference/release-notes-X.Y.Z.md reference/release-X.Y.Z/koan-vX.Y.Z.apk`
  (add `--prerelease` for a beta or rc).
- **→ Stop: the owner publishes.**

## 7. After the owner publishes
- Draft a short Discord update post for the owner's thread: what's new in a few lines, plus the
  link to the version's tag page.
- Update `docs/HANDOFF.md` (current release, next step).
