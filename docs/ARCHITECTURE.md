# Architecture

## Layers

```
┌────────────── UI (Compose) ───────────────┐
│ overview/  browser/  search/  settings/   │  screens + ViewModels, motion helpers
└───────────────▲───────────────────────────┘
                │ StateFlow<UiState>
┌───────────────┴───────────── domain ──────┐
│ SpacesRepository   TabsRepository         │  single source of truth = Room
│ SearchRepository   AccountRepository      │
└──────▲──────────────────────▲─────────────┘
       │                      │
┌──────┴──────┐        ┌──────┴────────────────────────┐
│ data/ (Room)│◀──────▶│ sync/  (ported from Z-Sync)    │
│ spaces,tabs,│        │ FxA sign-in, token server,     │
│ folders,    │        │ Hawk, crypto, spaces/prefs/    │
│ essentials, │        │ passwords/history collections  │
│ thumbnails  │        └───────────────────────────────┘
└──────▲──────┘
       │ tab ids ↔ engine sessions
┌──────┴───────────────────── engine/ ──────┐
│ android-components BrowserStore +         │
│ GeckoEngine (GeckoView)                   │
│ session lifecycle, thumbnails, prompts,   │
│ downloads, permissions, add-ons           │
└───────────────────────────────────────────┘
```

### Source of truth

- **Room** holds Koan's view of spaces/tabs/folders/essentials. The UI only observes Room.
- The **sync engine** reconciles Room ↔ Zen `spaces` collection (read-only until M6).
- **BrowserStore** (android-components) holds *live* browsing state for tabs that have an engine
  session. Mapping: every Koan tab row has a stable `koanTabId`; when a tab is opened we create /
  restore a BrowserStore tab with the same id. Navigation updates (url, title, favicon) flow back
  into Room.

### Data model (Room)

- `Space(uuid, name, icon, gradientColors JSON, containerGuid?, order, zenOrigin: Boolean)`
- `Tab(koanTabId, zenTabId?, spaceUuid, folderId?, url, title, iconUrl, pinned, essential,
   order, lastAccessed, thumbnailKey?, origin: ZEN|LOCAL, dirty: Boolean)`
- `Folder(folderId, spaceUuid, parentFolderId?, name, icon, collapsed, order)`
- `Split(splitId, spaceUuid, tabIds JSON, gridType)`
- `SyncMeta(collection, lastModified, lastSyncAt)`

`zenTabId` is set for tabs that came from / were written to Zen. Phone-only tabs are `LOCAL`.

## Engine

- Use android-components: `browser-engine-gecko`, `browser-state`, `browser-thumbnails`,
  `browser-icons`, `browser-storage-sync` (only if it fits; see below), `feature-session`,
  `feature-tabs`, `feature-prompts`, `feature-downloads`, `feature-sitepermissions`,
  `feature-contextmenu`, `feature-findinpage`, `feature-media`, `feature-addons`, `feature-search`,
  `feature-awesomebar` (suggestions), `concept-engine`. Verify artifact names/versions against
  the current android-components release before adding them.
- Fallback if android-components is too heavy or fights the custom UI: raw GeckoView
  (`GeckoRuntime`, `GeckoSession`, `GeckoView`) with our own small session manager. Decide in M0
  and record the decision at the bottom of this file.
- One `GeckoRuntime` per process (Application singleton).
- uBlock Origin: install via `feature-addons` / `WebExtensionController` from AMO.

### Tab lifecycle and memory

- At most **4 live engine sessions** (current + 3 most recently used). Others are suspended:
  session state saved (android-components does this via `EngineSessionState`), engine session
  closed. Opening a suspended tab restores it.
- On `onTrimMemory(RUNNING_LOW/CRITICAL)` drop to 1 live session.
- Tabs from Zen that were never opened on the phone have **no** engine session at all, only a URL.
  First open = fresh load.

### Thumbnails

- Captured with the engine's capture API when leaving a tab, when the overview opens (current tab
  only), and after page load settles (debounced 1.5s).
- Store as WebP, 480px wide (enough for cards at 1080p width × 0.95 scale… check visual quality on
  the phone and adjust), in an on-disk LRU (~150 MB cap) + small in-memory LRU of decoded bitmaps
  for visible cards only.
- Decode off the main thread; cards show a gradient placeholder until ready.
- Zen tabs never opened on the phone have no thumbnail: show favicon + domain on the gradient
  placeholder. (Optional later: low-priority background "pre-render" of pinned tabs on Wi-Fi.)

## Sync scheduling

- Pull on: app start (after showing the cached Room state immediately), returning to foreground
  after >2 min, pull-to-refresh in overview, every 15 min while foregrounded.
- WorkManager periodic pull (1h, unmetered) is optional.
- Never block UI on sync. Errors appear as a small badge on the space header, not dialogs.

## Accounts data beyond Spaces (M5)

The same Sync credentials give access to other collections (`passwords`, `history`, `bookmarks`,
`forms`, `tabs`, `clients`). Two options; pick in M5:

1. **Own read-only fetch** with the ported sync client: decrypt `passwords` into an encrypted local
   store (SQLCipher or Keystore-encrypted Room fields), feed GeckoView's login autocomplete
   (`Autocomplete.StorageDelegate` / android-components `LoginsStorage` interface). Same for
   history (for suggestions) and bookmarks. Simple, fully under our control, read-only.
2. **Mozilla application-services** sync engines (`sync-logins`, `places`) via android-components
   `FxaAccountManager`. Full two-way sync but needs an FxA OAuth client setup and is heavy.

Default plan: option 1 (read-only) first.

## Performance budgets (release build, owner's phone)

| Metric | Budget |
|---|---|
| Cold start → overview with cached spaces visible | < 800 ms |
| Overview scroll with 50 cards | no frame > 16 ms on 60 Hz panels, aim for display refresh (90/120 Hz) |
| Card open morph | starts within 1 frame of tap, no dropped frames |
| Space switch drag | 1:1 with finger, no dropped frames |
| Memory with 4 live sessions + overview | no OOM / process kill in normal use |

Tools: Baseline Profiles (`androidx.baselineprofile`), `JankStats`, Perfetto/System Trace, Compose
compiler reports for unstable classes. Keep card list items `@Stable`/immutable; use `key()` by
`koanTabId`; read scroll offsets inside `graphicsLayer {}` lambdas, never in composition.

## Blur strategy

Start with live blur (`RenderEffect`, available on all supported devices since minSdk 34) on the
thumbnail layer of cards behind the focus line. Measure on the POCO X4 GT in a release build at
its max refresh rate. Only if it drops frames, switch to pre-blurred thumbnails (2-3 baked blur
levels per thumbnail, crossfaded by depth) without changing the visual result.

Request the display's highest refresh rate for the overview and page view
(`Surface.setFrameRate` / preferred display mode), since HyperOS may otherwise cap the app.

## Allowed dependencies (ask before adding others)

AndroidX core/activity/lifecycle/navigation-compose, Compose BOM (+ foundation, animation,
material3), Room, DataStore, WorkManager, kotlinx.coroutines, kotlinx.serialization,
androidx.security / Keystore, Coil 3 (images), Mozilla android-components + GeckoView (incl.
`browser-session-storage`, approved for M3),
androidsvg (favicons, as in Z-Sync), baselineprofile, JUnit, Turbine, Robolectric (if needed).
Approved for M5 (2026-10-16): AC `feature-readerview`, AC `concept-storage`, `androidx.biometric`.

## Decisions log

- 2026-10-12 — minSdk 34; live RenderEffect blur first, baked blur as fallback (owner).
- 2026-10-12 — **android-components (non-UI parts only) over raw GeckoView.** We use
  `browser-engine-gecko`, `browser-state`, `concept-engine`, `feature-session`, `feature-tabs` and,
  later, prompts/downloads/sitepermissions/addons/etc. None of AC's View widgets (BrowserToolbar,
  tabstray); all UI is our own Compose that observes `BrowserStore`. Reason: AC gives us prompts,
  downloads, permissions, add-ons, session state save/restore for free; the "fights the UI" risk
  only applies to its widgets. Costs: bigger APK, API churn per release, AC pins some AndroidX
  versions we must follow. Versions: **AC 157.0.1 + GeckoView (omni) 157.0.20261005135250**
  (the GeckoView build AC 157.0.1 depends on). Bump both together.
- 2026-10-12 — **Hero morph animates the screenshot, never the live GeckoView.** GeckoView renders
  into a SurfaceView that can't be clipped/scaled by Compose; the live view is swapped in once the
  morph lands. No TextureView mode.
- 2026-10-12 — **arm64-v8a only** (`abiFilters`). Sideloaded, both phones are arm64; keeps the
  GeckoView-omni APK small. Emulators must be arm64 images.
- 2026-10-12 — Sync port keeps **org.json** (faithful to Z-Sync); `org.json:json` is a test-only
  dependency so JVM tests get a real implementation. Credentials (M2): own Keystore AES-GCM wrapper
  over DataStore instead of deprecated `EncryptedSharedPreferences`.
- 2026-10-12 — Live pinned tabs get a card while loaded; closing it unloads, never deletes/unpins
  (see `UI_SPEC.md`).

- 2026-10-14 — **M2 sync is read-only by construction**: only the read half of Z-Sync's
  `SpacesSyncService` is ported; write paths arrive with M6. Ported files carry small `Koan:` edits
  (offline vs expired session via FxA errno 110/102, redacted `TokenServerCreds.toString`).
- 2026-10-14 — **Room ids for Zen rows**: `zen:<tabId>`, essentials `zen:<spaceUuid>:<tabId>` (they
  repeat across spaces). `order` uses a stride of 8 so split members sort with their split. A pull
  writes only changed rows (`ZenDiff`); phone-side reorder, folder collapse and closes survive.
  Closing a Zen tab on the phone hides it (`closed`) until Zen closes it too (no write-back yet).
- 2026-10-14 — **Favicons**: Zen's inline `data:` icons are normalised to ≤128 px PNGs on disk at
  sync time (Room holds only a key); other sites go through Z-Sync's `FaviconLoader`, whose last
  resort asks DuckDuckGo's icon service (`icons.duckduckgo.com/ip3/<host>.ico`) for the domain,
  never Google's (owner's choice). Decoded icons are prewarmed at start/sync.
- 2026-10-14 — **Stack scale**: the depth stack composes only cards near the focus and applies data
  updates only when the stack is idle, anchored on the focused card.

- 2026-10-14 — **M3 tabs**: `engine/TabManager` maps Room tabs to BrowserStore tabs with the same
  id, created on first open (never-opened Zen tabs have none). Max 4 live sessions (MRU); others
  suspended via `SuspendEngineSessionAction`. Android 14+ only delivers `UI_HIDDEN`/`BACKGROUND` to
  `onTrimMemory`: BACKGROUND keeps only the current session live; the screenshot for the next start
  is taken in `onPause`. Engine tabs persist with AC `browser-session-storage` (AutoSave on change,
  periodically in the foreground, on stop). Room v3 adds `cardOrder` (live pinned/essential tabs'
  stack slot) and `zenUrl`: a pull only overrides a phone-navigated page when Zen navigated that tab
  itself, never while it is open on the phone.
- 2026-10-14 — **Thumbnails**: own `ThumbnailStore` (not AC `browser-thumbnails`): WebP lossy q80,
  480px wide (`ThumbTokens`), 150 MB disk LRU, ~48 MB memory LRU of hardware bitmaps. Room
  `thumbnailKey` = file name, new per capture, so cards update via the normal diff.
- 2026-10-14 — **Page cover**: a Compose layer over the engine view shows the tab's screenshot (or
  placeholder) until the engine reports a fresh first paint; no white flash on open, restore or
  cold start. Dock hides with GeckoView's dynamic toolbar (`setDynamicToolbarMaxHeight` +
  `setVerticalClipping`), driven by GeckoView's nested scrolling through Compose.

- 2026-10-16 — **M5 visual system**: light theme fully neutral (no space wash); the brand gradient
  (`BrandTokens`) marks only "Koan is working / active / your focus" (UI_SPEC "Brand gradient").
  `SpaceColors` shrank to the switcher pill's accent and ink. Shared controls live in `ui/kit`.

_(Claude Code: append dated decisions here, e.g. "2026-10-20 — chose android-components over raw
GeckoView because …")_
