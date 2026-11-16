# Batch 4 — refactoring plan

Plan for `docs/plans/BATCH4_BRIEF.md`. Written 2026-11-11 on `dev` at `bcf2114`. No code changed yet. Finding
numbers are the audit's (`docs/AUDIT.md`, local only). Paths are under `app/src/main/java/dev/nerzouille/koan/`.

## Decisions (defaults marked, owner confirms before group D / step A5)

| # | Question | **Default** | Why |
|---|---|---|---|
| 1 | 3.1 button text weight and gap | **SemiBold, 10 dp** | Already the kit's values (`SecondaryButton`, `ButtonTokens.ChoiceGap`); only the prompt cards change. |
| 2 | 3.37 ported files: mirror upstream or only what Koan runs | **Mirror upstream: skip 3.37** | `ZenSpaces.encodeSnapshot` / `decodeSnapshot` are covered by the contract fixtures, and the Spaces write work (after the release) will diff against Z-Sync again. 3.35 is still deleted: it is a safety fix, not a trim. |
| 3 | 3.38 `SeedData` | **Delete**, with `fillWithFakeTabs`, `removeFakeTabs`, `deleteTabsWithPrefix` and the debug menu entries | Only reachable in a build nobody installs; its pool is real sites. |

## Ground rules for every step

- Branch `refactor` from `dev`. One finding or family per commit, reverted alone with `git revert`.
- Re-check every audit line number: `ui/KoanRoot.kt` and `ui/KoanOverlays.kt` are now ~1 370 lines each.
- Per step: targeted unit tests, then a dev build on the POCO with the listed check. End of each group: full
  suite, R8 release build installed and launched, restore after process death, docs updated (`ARCHITECTURE.md`,
  `DESIGN_SYSTEM.md`). The owner can stop and merge after any group.
- Never changes: sync wire format and `SpacesWriteGate`, Room schema, DataStore keys that are read, the
  `docs/PRIVACY.md` network list, ported-file headers, gesture feel outside the brief's list.

## Group A — dead code and stale comments (no phone-visible change)

| Step | What | Files / symbols |
|---|---|---|
| A1 | 3.34 `SyncBack` chain | delete `data/SyncBack.kt`, `SyncBackTest`; in `sync/SyncBackPlan.kt` remove `PhoneBookmark`, `BookmarkUpload` (+ their cases in `SyncBackPlanTest`); `data/Settings.kt` `send_bookmarks` key and `SyncBackChoice.bookmarks`; `SyncBackRow` in `ui/settings/SettingsScreen.kt`; the DAO's `phoneBookmarks()`; "dry run" comments in `Settings.kt`, `SettingsScreen.kt`, `data/KoanDatabase.kt` |
| A2 | 3.35 single-record writes | `sync/SyncClient.kt`: `putRecord` ×2, `postRecords`, `putTombstone`, then `PutOutcome` / `getCollectionWithMetadata` / `CollectionRead` if nothing else uses them. Keep `postBatch`. Remove the three matching tests in `SyncTransportTests` |
| A3 | 3.36 favicon prefetch | `favicon/FaviconLoader.kt`: `prefetch`, `prefetchScope`, `pruneIndex`; `FaviconResolver.requests`, `FaviconRequest`; the `directURL` / `dataURL` parameters and branches |
| A4 | 3.39, 3.40 small items | 2 DAO methods, 3 icons, `topCutoutBeside`, 4 never-read parameters, `backup_count_*` key; `compose-ui-tooling(-preview)` in `app/build.gradle.kts` and `gradle/libs.versions.toml`; density launcher bitmaps only if `mipmap-anydpi-v26` covers them. **Keep** `QuietButton`, `PressScale` (until D1 / E3), `R.color.zen_icon_fill`. Unused tokens wait for G1 |
| A5 | 3.38 `SeedData` (decision 3) | `data/SeedData.kt`, `data/SpacesRepository.kt` (`fillWithFakeTabs`, `removeFakeTabs`), DAO `deleteTabsWithPrefix`, debug menu entries in `ui/KoanRoot.kt` |
| A6 | Stale KDocs from the brief | `ui/settings/PasswordsPage.kt`, `ui/downloads/DownloadsScreen.kt`, `Destructive` in `ui/theme/Tokens.kt` |
**Risks and catches:** a symbol reached by name, reflection or only under R8 → grep `main`, `test`,
`res`, `proguard-rules.pro` before deleting; R8 build at the end. A1 leaves an orphan DataStore
key: harmless, no migration. A2: `SpacesWriteGateTest`, `SyncWritePlanTest` and the contract
tests stay green untouched. A3: favicons still load everywhere after a cold start.

## Group B — per-frame recompositions (each step measured)

Baseline first: an R8 staging build of `dev` at the branch point, `speed-profile` forced, kept as build A for
the whole group.

| Step | What | Files / symbols |
|---|---|---|
| B1 | 3.19 one `animationsOff(context)` and one frame-clock loop | new `ui/kit/MotionKit.kt`; callers in `ui/bar/AddressRow.kt`, `ui/privacy/PrivateFlip.kt`, `ui/tour/SignInScene.kt`, `ui/overview/DepthStack.kt`, `thumbs/BakedBlur.kt` |
| B2 | `rememberPresence(open, spec)` (3.14, 3.15, 3.8), one caller per commit | new `ui/kit/Presence.kt`; `kit/KoanToast.kt`, `menu/DockPopup.kt`, `search/SearchSheet.kt`, `start/StartPage.kt`, `bar/AddressRow.kt`, `kit/Selection.kt`, `player/StandalonePlayer.kt` (open morph), player overlays |
| B3 | 4.1 keyboard inset | `ui/KoanRoot.kt` `searchBottom` (~l. 1023): pass `() -> Dp` / read in a layout or offset lambda; `search/SearchSheet.kt`, start page callers |
| B4 | 4.2 load progress | `ui/KoanRoot.kt` (`currentPage.progress`, `loadProgress =`, ~l. 980–1110), `ui/KoanOverlays.kt`, `bar/AddressRow.kt`, `bar/KoanBar.kt`: `() -> Float`, read in draw |
| B5 | 4.5 load line | `bar/KoanBar.kt`: cache brushes and `Path`, own layer |
| B6 | 4.9 seek scrub and blur | `player/VideoPlayer.kt` (scrub position read in draw / layout; `BlurEffect` cached by radius step) |
| B7 | 4.7 depth dim, 4.8 page lambdas | `overview/TabCard.kt` (dim through layer alpha, placeholder only without a screenshot), `overview/OverviewScreen.kt` (`remember` lambdas, one derived slice) |

**Must not change:** springs and durations, exits finishing before removal, keyboard and sheet moving together, the
load line's look.
**Risks and catches:**
- Presence removed a frame early or late (flicker) → each caller at animator scale 5× and with animations off.
- A value read in a lambda no longer invalidates (frozen progress, sheet under the keyboard) → slow page;
  keyboard on the start page and on a page.
- `tools/perf` has no keyboard, page-load or player scenario (4.10 is batch 5): B3, B4, B6 are measured by hand
  (`dumpsys gfxinfo <pkg> reset`, the action 10×, `framestats`, A vs B) plus a temporary, uncommitted
  recomposition counter. B5, B7: `tools/perf` A/B.

## Group C — split the root and the overlays (move only, no logic change)

| Step | What | Files / symbols |
|---|---|---|
| C1 | 1.2 `KoanOverlays` → one file per group, one group per commit | `ui/KoanOverlays.kt` keeps `Overlays`, `KoanOverlays` (calls the groups) and `rememberFindUi`; new `ui/overlays/{Menu,Permission,Login,Settings,Library,Tour}Overlays.kt`; helpers (`fileThumbnail`, `folderLabel`, `androidPermissions`, `ActionPage`) follow their only user |
| C2 | 1.1 state out of `KoanRootContent` | new `ui/root/RootState.kt`: plain class holding the navigation state and its transitions, moved verbatim; `rememberRootState(...)` |
| C3 | 1.1 the rest | new `ui/root/PrivateSwitch.kt`, `ui/root/IntentEffects.kt`, `ui/root/PlayerActions.kt`; `ui/KoanRoot.kt` keeps layout and layers |
| C4 | Optional, only if it falls out: 1.7 `TabSet`, 1.5 capture constants into `thumbs` | `engine/Components.kt`, `thumbs/ThumbnailStore.kt`, `engine/TabManager.kt`. 1.9 is left for later |

**Must not change:** `LaunchedEffect` order and keys, `rememberSaveable` keys, back-press priority, z-order of the
layers, which overlay closes which.
**Risks and catches:** a key or effect order changes when a block moves → move whole blocks, review with `git diff
--color-moved`. Phone script per step: open, back out of and dismiss each moved surface, private switch, link
from another app, share-to-Koan, player open / PiP / close, kill and restore. After C: `tools/perf` A/B (a split
changes recomposition scopes).

## Group D — shared UI components (decision 1 before D2)

Order: D1 3.11 exact copies (`DragHandle`, `brandWash` for `settingsWash` / `titleGlow`, `SpaceTitle`,
`swipeDownToDismiss`) → D2 3.1 `KoanButton(kind)` + `CardButtons` (folds `QuietButton`, the private `CardButton`
/ `SecondaryButton` in `menu/PermissionPrompt.kt`, `menu/PagePrompts.kt`, `player/StandalonePlayer.kt`) → D3 3.4
toggle rows → D4 3.3 `KoanIconButton` → D5 3.5 `PageBar`, `CardHeader` → D6 3.6 tiles and avatars → D7 3.7 chips
and links → D8 3.10 empty states → D9 3.12 dividers, labels, groups, spinner, scrim, field → D10 3.2
`RowContent` (`sheet/SheetKit.kt` `SheetRow`, `settings/SettingsKit.kt` `SettingsRow`, the 4 hand-built rows) →
D11 3.9 confirm cards through `Overlays.ask` → D12 3.13 tour sheet on `SheetState`, `PagePlaceholder`.

New pieces go in `ui/kit`, which stops importing feature packages. Signatures: the audit's
`03-ui-duplicates.md`. Must not change: anything visual outside the brief's list; each commit message names its
visible change or says "no visual change".
**Risks and catches:** pixel drift → before / after screenshots of each touched screen in light, dark and private
(in `reference/`, never committed). D10 touches every settings page and sheet: one commit per row type. D11:
each confirm runs its action once and closes on back. D12 is on the baked-blur hot path: perf A/B, first-run
tour on staging; first to drop if time is short.

## Group E — motion and gestures

E1 3.16 exact duplicate springs (`Motion.Toast` / `Motion.SearchScrim` in `ui/theme/Tokens.kt`, `ControlsSpring`
and four copies in `ui/player/PlayerTokens.kt`) → E2 3.17 three fade tokens, 3.18 mist naming
(`player/VideoPlayer.kt`, `PlayerParts.kt`) → E3 3.20 `pressScale`, `slideUpPage`, `popIn` in
`ui/kit/MotionKit.kt` (`PressScale` goes) → E4 3.26 new `ui/kit/KoanHaptics.kt`, 21 call sites, 2 added → E5
3.23 `SwipeTokens.FLING` in dp/s (`ui/kit/SwipeRow.kt`) → E6 3.24 `NeighbourDrag` (`overview/SpacePager.kt`,
`browser/TabSwipe.kt`) → E7 3.21 `awaitSlop` / `dragUntilUp` in new `ui/kit/Drag.kt`, one caller per commit
(`bar/BarGestures.kt`, `sheet/KoanSheet.kt`, `tour/TourOverlay.kt`, `player/VideoPlayer.kt`,
`overview/DepthStack.kt` last) → E8 3.22 `Modifier.swipeToDismiss` + one `Gesture` token object, one caller per
commit → E9 3.25 system long-press timeout, `tapOutside`, `blockTouches`.

**Must not change:** the stack's fling curve (`DepthCurveTest`), bar and sheet detents, pull to refresh, double-tap
seek, the space scrub.
**Risks and catches:** feel can't be unit-tested. E7 and E8 are judged on a release build, each caller tried by the
owner before the next; the stack is dropped if it feels different at all. Nested gestures (sheet over stack, row
swipe in the pager) checked after each caller.

## Group F — logic helpers

F1 3.28 `search/Urls.kt` (`WebUrl`: host, site, origin; replaces the 12 derivations incl. `Logins.baseDomain`,
`displayHost`, app-link keys), one call-site family per commit → F2 3.32 size formatter, clipboard, biometric
prompt (Passwords fallback without a screen lock) → F3 3.29 Downloads' Undo tied to the toast's lifetime
(`ui/downloads/DownloadsScreen.kt`, `kit/KoanToast.kt`) → F4 3.30 `undoToast` helper (player Delete gains Undo)
→ F5 3.31 `engine/PageBridge.kt` under `PageEngines`, `PageImages`, `PageVideo` → F6 3.33 one favicon decode
path and one memory cache (`favicon/FaviconLoader.kt`, `favicon/IconStore.kt`).

**Must not change:** which login is offered where, except the `*.github.io` fix; stored app-link "Always" choices
(read old keys too if the format changes); Passwords pages never fetch icons.
**Risks and catches:** F1 changes security-relevant matching → characterisation tests first, `LoginOriginTest` and
`IconHostTest` stay green. F5 has no JVM coverage → on the phone: add a search engine from a page, long-press an
image, open a video. F6 → `dumpsys meminfo` before / after.

## Group G — tokens

G1 5.7 delete the 13 unused tokens, one hairline, one group radius → G2 5.1 player token pass (`ui/player/*`,
values unchanged) → G3 only if the owner wants: 5.2, 5.3, then 5.8 split of `Tokens.kt` last (pure move,
conflicts with everything before it).

## Tests to add first (JVM only, no new dependency; Compose, Gecko and gesture behaviour: phone checks, UI tests are batch 5)

| Before | Test | Pins |
|---|---|---|
| C2 | none possible before (state lives in a composable). Right after the verbatim move: `RootStateTest` for back-press order, overview / search / private transitions | later edits to the root |
| E5 | `SwipeTokensTest`: fling threshold in dp/s equals today's 1800 px/s at the POCO's density | density fix only |
| E6 | `NeighbourDragTest`, written against the logic as extracted from `SpacePager` | both pagers agree |
| E8 | `SwipeDecisionTest`: pure `(offset, width, velocity) → dismiss?`, first with each of the 5 current threshold sets, then updated in the unifying commit | the "few percent" is visible in the diff |
| F1 | `UrlsTest` characterising today's host / site / origin results (plain, `www.`, `m.`, port, IPv6, IP, `*.github.io`, `co.uk`, `about:`, `file:`), the three bugs asserted as wrong-today then flipped | every unintended change |
| F2 | `SizeFormatTest` over the three current formatters' outputs | one format, chosen on purpose |
| F3 | `UndoWindowTest` on the extracted pure timer / slot logic: a replaced toast cancels the pending delete | the 3.29 fix |
