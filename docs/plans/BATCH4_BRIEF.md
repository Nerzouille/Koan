# Batch 4 — refactoring brief

Brief for the session that plans and does audit batch 4. Read `CLAUDE.md` and `docs/HANDOFF.md`
first, then this file. Written 2026-11-11 on `dev` at `272eaee` (audit batches 1–3 merged).

## Goal

Make the code smaller and more consistent **without changing what the app does**:

1. Merge near-duplicate UI components, animations, gestures and logic helpers into shared ones.
2. Split the two oversized composables: `KoanRootContent` (`ui/KoanRoot.kt`, ~1 090 lines in one
   function) and `KoanOverlays` (`ui/KoanOverlays.kt`, ~1 030 lines, 20 unrelated surfaces).
3. Fix the per-frame recompositions (CLAUDE.md working rule 9).
4. Remove dead code.

## Sources

The audit of 2026-11-11 is **local only** (gitignored, not in the public repo; it stays that way
until the owner publishes it):

- `docs/AUDIT.md` sections **1.3** (layering), **3** (duplicates and simplification), **4**
  (performance), **5** (design system consistency), and "Top 15" items 9, 15.
- Detail reports in `docs/audits/2026-11-11/`: `01-architecture.md` (split plans, L8–L24),
  `03-ui-duplicates.md` (19 component families with proposed signatures), `04-motion-gestures.md`
  (part A motion, part B gestures), `05-logic-deadcode.md` (part A helpers, part B dead code),
  `06-performance.md` (how to measure each finding), `07-design-tokens.md`.

Line numbers in the audit are from `06e13d6`; batches 1–3 moved some of them. Re-check each one
in the code before acting on it.

## Findings to cover

Numbers are `docs/AUDIT.md`'s.

**Per-frame recompositions (do first, measurable):**
- 4.1 keyboard inset read in the root's composition (`ui/KoanRoot.kt`, `searchBottom`).
- 4.2 page load progress read in the root and `KoanOverlays`.
- 3.14 / 3.15 / 4.9 animated values read in composition (player open morph and seek scrub, toast,
  dock popup, search sheet, start page, address row, selection bar): one
  `rememberPresence(open, spec)` helper covers most; the seek scrub needs its own rework.
- 4.5 load line re-records the dock every frame; 4.7 depth dim through draw lambdas; 4.8 page
  lambdas rebuilt on each overview recomposition.

**Split the root and the overlays:**
- 1.1 `KoanRootContent` → a plain `RootState` class plus files for private switching, intent
  effects and player actions.
- 1.2 `KoanOverlays` → one file per group (menu, permissions, logins, settings, library, tour).
- Optional if it falls out naturally: 1.5 (UI tokens imported by `thumbs`, `TabManager`,
  `GeckoPrefs`), 1.7 (one `TabSet` for normal / private), 1.9 (`TabManager` and `VideoPlayer()`
  too big).

**Shared UI components:** 3.1–3.13 (buttons, rows, icon buttons, toggle rows, headers, tiles,
chips and links, slide-up pages, confirm cards, empty states, exact copies, dividers / labels /
groups / progress / scrims / fields, tour sheet). 3.11 (exact copies) first: no risk.

**Motion:** 3.16 (duplicate springs), 3.17 (animation literals → three fade tokens), 3.18 (mist
naming), 3.19 (`animationsOff` and frame-clock loop written several times), 3.20 (press-scale,
slide-up, pop-in helpers).

**Gestures:** 3.21 (drag loop written 5 times), 3.22 (swipe-to-dismiss 5 times), 3.23
(`SwipeTokens.FLING` in px/s), 3.24 (`SpacePager` / `TabSwipe` pager logic), 3.25 (player long
press, tap-outside), 3.26 (two haptics APIs).

**Logic helpers:** 3.28 (host / site / origin derived 12 ways → one `search/Urls.kt`), 3.29
(Downloads' Undo window apart from the toast's), 3.30 (undo toasts built in 3 places), 3.31
(`PageEngines` / `PageImages` / `PageVideo` → `PageBridge`), 3.32 (size formatters, clipboard,
biometric prompt), 3.33 (favicon decode and two memory caches).

**Dead code:** 3.34 (`SyncBack` and its chain), 3.35 (`SyncClient.putRecord` ×2, `postRecords`,
`putTombstone`: no production caller), 3.36 (`FaviconLoader.prefetch` chain), 3.37 (test-only
ported helpers, **owner decision first**), 3.38 (`SeedData`: delete, or rewrite with
`example.com` data), 3.39 (small unused items, minus the corrections below), 3.40 (unused tooling
dependencies, launcher bitmaps).

**Tokens, where they come with the merges:** 5.1 (player token pass), 5.7 (unused and duplicated
tokens). 5.2, 5.3 and 5.8 (spacing scale, kit-level tokens, splitting `Tokens.kt`) only if time
is left, as the last steps.

## Corrections found since the audit

- **3.27 is done:** batch 1 added `engine/KoanHttp.kt` (engine User-Agent, capped reads). Left on
  their own client: `update/UpdateCheck.kt`, `favicon/FaviconLoader.kt`, `sync/SyncTransport.kt`.
- **1.3 is done:** `sync/SpacesWriteGate.kt` + `Settings.writeBackEnabled`. The dead write methods
  of 3.35 are still there: delete them (keep `postBatch`, `SyncWrite` uses it).
- **`QuietButton` is used** (`ui/signin/SignInScreen.kt`): not dead, unlike 3.39 says. Fold it
  into the shared button (3.1) instead.
- **`R.color.zen_icon_fill` is used** by `res/drawable/zen_fish.xml` (the `zen_*` icons are
  loaded by name): keep it, unlike 3.40 says.
- **`SyncBack`** (`data/SyncBack.kt`) is called only by `SyncBackTest`. Remove it with its chain
  (3.34) and fix the stale "dry run" comments that go with it: `data/SyncBack.kt`,
  `data/Settings.kt` (`SyncBackChoice` KDoc, the `syncBack` comment; `SyncBackChoice.bookmarks`),
  `ui/settings/SettingsScreen.kt` ("Two-way sync, dry run only"), `data/KoanDatabase.kt`
  ("Two-way sync (dry run)").
- Other stale KDocs: `ui/settings/PasswordsPage.kt` ("the round + next to the search"; it is a
  floating +), `ui/downloads/DownloadsScreen.kt` (selection actions are Open / Copy link / Share /
  Delete, not "Open, Share, Hide, Delete"), the `Destructive` colour's comment in `Tokens.kt`
  ("History's swipe-to-remove"; it is the delete swipe on every library page).
- The media-timeline reflection (L24 in `01-architecture.md`) is gone with GeckoView 160.

## Rules

- **Branch `refactor` from `dev`.** Merge into `dev` only after the owner's OK.
- **No behaviour change** unless it is in this list (each one says so in its commit message):
  - 3.1 one "Cancel" weight and one button gap (**owner picks**: Medium or SemiBold, 8 or 10 dp);
  - 3.3 touch sizes aligned; 3.5 sub-line size aligned;
  - 3.20 player pills gain the press scale; 3.26 haptics added to the two actions without one;
  - 3.22 swipe thresholds unified (a few percent); 3.23 the row fling in dp/s;
  - 3.17 fades moved to three tokens (±20 ms);
  - 3.28 the three host bugs fixed; 3.29 a replaced toast no longer deletes the file later;
    3.30 the player's Delete gains Undo; 3.32 Passwords unlock falls back on phones without a
    screen lock;
  - 3.9 the player's confirm dialog becomes the shared card.
- **Small, reviewable steps:** one finding or one family per commit, Conventional Commits
  (`refactor(ui): …`, `perf(root): …`). The owner commits by default: give the exact `git add`
  (files listed, never `git add .`) and `git commit` commands, unless the owner says otherwise.
- **Each step testable on the phone:** a dev build (`./gradlew assembleDev`) on the POCO, the main
  test phone, with what to look at. Targeted unit tests per step; the full suite
  (`./gradlew testDebugUnitTest`) and an R8 release build at the end of each group.
- **Perf steps are measured:** `tools/perf` A/B on the POCO before / after (staging copies,
  `KOAN_PKG=dev.nerzouille.koan.staging`, coordinates in `docs/HANDOFF.md` "Test"). Force
  `speed-profile` on both builds before measuring.
- Animate only `graphicsLayer` properties in hot paths; no recomposition or relayout per frame.
- Ported Z-Sync files keep their header; ask before trimming them (3.37).
- No new dependency without asking.
- Update the docs the change touches (`docs/ARCHITECTURE.md` package map, `docs/DESIGN_SYSTEM.md`
  for shared components and tokens) in the same step.
- Public repo: nothing personal in code, docs, tests or commit messages.

## Owner decisions to ask before starting

1. 3.1: button text weight (Medium or SemiBold) and gap (8 or 10 dp).
2. 3.37: ported files mirror upstream, or hold only what Koan runs?
3. Order: the suggested one below, or another?

## Suggested order

1. Dead code (3.34, 3.35, 3.36, 3.38–3.40) and the stale comments: smallest risk, smaller files
   for everything after.
2. Per-frame recompositions (4.1, 4.2, `rememberPresence` for 3.14 / 3.15 / 4.9, then 4.5, 4.7,
   4.8), each with a perf A/B.
3. Split `KoanRootContent` and `KoanOverlays` (1.1, 1.2), no logic change.
4. Shared UI components (3.11 first, then 3.1–3.13).
5. Motion and gestures (3.16–3.26).
6. Logic helpers (3.28–3.33).
7. Token pass (5.1, 5.7), then 5.2 / 5.3 / 5.8 if wanted.

## Out of scope (batch 5 or later)

- Tests: Room migration and DAO tests, a first `TabManager` restore test, `SyncWrite` /
  `postBatch` tests (Robolectric or instrumented tests: a new dependency, ask first).
- CI, lint as a check.
- Baseline profile (4.4) and new perf scenarios (4.10: `reportFullyDrawn`, keyboard, page load,
  player, memory).
- Design changes: Dusk contrast (5.4), window background (5.5), private buttons (5.6), the design
  audit follow-ups in `docs/HANDOFF.md`.
- Larger restructuring: `Components` as an app-level locator (1.4), the `sync` / `data` /
  `favicon` cycle (1.6), global object stores (1.8), drawer composed at first frame (4.3),
  screenshot memory per space (4.6).
