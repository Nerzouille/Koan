# Architecture

Last checked against the code: 2026-11-11 (`dev` at 06e13d6)

One Gradle module (`:app`), one activity, Kotlin + Jetpack Compose over GeckoView through Mozilla
android-components (AC). Source root: `app/src/main/java/dev/nerzouille/koan/` (paths below are
relative to it unless they start with `app/`, `docs/` or `tools/`).

## Layers

```
┌──────────────────────────── UI (Compose) ────────────────────────────┐
│ ui/KoanRoot.kt (all navigation state)   ui/KoanOverlays.kt (sheets)   │
│ overview/ browser/ bar/ search/ start/ menu/ sheet/ settings/         │
│ history/ downloads/ player/ privacy/ signin/ tour/ kit/ icons/ theme/ │
└───────▲──────────────────────────────▲────────────────────────────────┘
        │ Flows from Components         │ slices of BrowserStore
┌───────┴───────────────────────────────┴───────────────────────────────┐
│ engine/Components: process-wide service locator (KoanApp.components)  │
└───────▲───────────────────────────────────────────────────────────────┘
        │ builds and owns
  data/      Room, DataStore, Keystore boxes, account, sync runs
  sync/      FxA, Sync client, crypto, Zen decoder, upload plans
  engine/    GeckoEngine, BrowserStore, TabManager, page helpers, downloads
  privacy/   private tabs, sealed file, lock, engine privacy settings
  thumbs/ favicon/ search/ update/   screenshots, icons, search model, release feed
```

- There are no ViewModels, no `UiState` classes, no Navigation component and no fragments
  (`MainActivity` is a `FragmentActivity` only for `BiometricPrompt`).
- `engine/Components` builds every long-lived object lazily (engine, store, database, repositories,
  sync, settings, tab managers) and carries a few app-level event channels the UI collects
  (`incoming`, `pairLink`, `startPage`, `tour`, `downloadAsk`, `duplicateAsk`, `downloadDone`, `pip`).
  `KoanApp.components` is lazy so GeckoView's child processes never build a second runtime.
- UI state is Compose state: `remember` / `rememberSaveable` values and plain state-holder classes
  (`OverviewMotion`, `SpacePager`, `SheetState`, `Overlays`, `DockState`, `TabSwipe`, `BarGestures`,
  `SearchState`, `ToastState`, `PrivateFlip`) created in `ui/KoanRoot.kt`.
- Process-wide `object` stores initialised from `MainActivity.onCreate`: `DeviceProfile`,
  `ThumbnailStore`, `BakedBlur`, `IconStore` (which initialises `FaviconLoader`).

### Package map

| Package | What it holds |
|---|---|
| (root) | `KoanApp`, `MainActivity`, `SearchSelection` ("Search in Koan" trampoline), `DeviceProfile` (RAM tiers) |
| `data` | Room (`KoanDatabase`, `KoanDao`, entities), `SpacesRepository`, `Settings`, `LastPlace`, `AccountStore`, `SecretBox`/`KeystoreBox`, `SafeStores` (DataStores that survive a corrupted file), `LoginStore`, `Bookmarks`, sync runs (`SpacesSync`, `LibrarySync`, `SyncWrite`, `PhoneLogins`), `ZenDiff` |
| `engine` | `Components`, `KoanScopes` (guarded process scopes), `KoanHttp` (Koan's own small requests), `TabManager`, `HistoryRecorder`, `ExternalLinks`, `SitePermissions`, `PageTools` (reader, find), `PageEngines`, `PageImages`, `PageVideo`, `PictureInPicture`, `UBlock`, `LoginAutofill`, `GeckoPrefs`, `GeckoFinder.java`, Custom Tabs and media services |
| `engine/downloads` | `Downloads`, duplicate detection (`DuplicateDownloads`, `DuplicateMiddleware`), `KoanDownloadService` |
| `favicon` | `IconStore` (Koan's), and Z-Sync's favicon loader, discovery, decoder, SVG rasteriser |
| `privacy` | `PrivateTabs`, `PrivateVault`, `PrivateMode`, `PrivacyEngine`, `AmpLinks`, `LinkCleaner` |
| `search` | `SearchEngine`, `CustomEngines`, `OpenSearch`, shortcuts and ranking (`SearchModel.kt`), `UrlInput.kt`, `Suggestions`, `HistoryFilter` |
| `sync` | Ported from Z-Sync: `FxAClient`, `FxACrypto`, `HawkAuth`, `SyncClient`, `SyncCrypto`, `SyncTransport`, `SpacesSyncService`, `ZenSpaces`, `ZenRoomMapper`. Koan's own: `ScopedKeys`, `PairingFlow`, `SpacesWriteGate`, upload plans (`SyncBackPlan.kt`, `SyncWritePlan.kt`) |
| `thumbs` | `ThumbnailStore`, `BakedBlur`, `BlankCheck` |
| `update` | `UpdateCheck` |
| `ui` (root files) | `KoanRoot.kt`, `KoanOverlays.kt`, `OverviewMotion.kt`, `DisplayRefresh.kt` (`RefreshBooster`), `SystemBars.kt` |
| `ui/bar` | The dock: `KoanBar`, `AddressRow` (address field, find bar), `WorkspaceSwitcher`, `SpaceDrawer`, `BarGestures`, `SpaceScrub` |
| `ui/browser` | `BrowserScreen` (hosts the `GeckoEngineView`), `DockState`, `PageCover`, `PullRefresh`, `TabSwipe`, `Fullscreen`, `ClosingPage`, `TranslateOfferHost`, `observeSlice` |
| `ui/overview` | `OverviewScreen`, `SpacePage`, `SpacePager`, `DepthStack`, `StackState`, `DepthCurve`, `TabCard`, `CardHero`, `Favicon`, first-run card |
| `ui/search`, `ui/start` | Search sheet and its state; start page |
| `ui/menu` | The ⋯ menu pages and every prompt card (link, page prompts, file, logins, permissions, app links, downloads, translate, uBlock, site settings) |
| `ui/sheet`, `ui/kit` | Sheet and card primitives (`SheetState`, `KoanSheet`, `KoanCard`); shared controls (buttons, field, toggle, toast, list pages, selection, swipe rows, `SecureWindow`); motion helpers (`MotionKit`: `animationsOff`, `frameClock`) |
| `ui/settings`, `ui/history`, `ui/downloads` | Full-screen pages: settings (passwords, engines, privacy, translation, about), history, downloads |
| `ui/player` | `VideoPlayer`, `WebPlayerLayer`, `StandalonePlayer`, `NativeVideo`, `PlayerModel.kt` (`PlayerHost`, `PlayerState`) |
| `ui/privacy` | `PrivateFlip`, `PrivateAtmosphere`, private parts (toggle, lock cover), `unlockPrivate` |
| `ui/signin` | `SignInScreen`, Mozilla's pages in a system `WebView`, `QrScanner` (Camera2 + zxing) |
| `ui/tour` | First-run tour and "What's new" (`TourOverlay`, `TourContent.kt`), clips in `app/src/main/res/raw` |
| `ui/theme`, `ui/icons` | `KoanPalette` and all tokens (`Tokens.kt`), theme, type, brand; `KoanIcons`, `ZenIcon`, `ZiaIcons` (icon set in `app/src/main/assets/zia`: Phosphor names before Zia 2.42, loose Tabler files 2.42–2.58, the `resource://zia-tabler/…` bundle after; an unknown name takes the closest by spelling) |

Outside the source root: built-in WebExtensions (`app/src/main/assets/extensions`), Room schemas
(`app/schemas`), `app/src/staging` (the `staging` build's app name), unit tests (`app/src/test`),
perf scripts (`tools/perf`).

### Source of truth

- **Room** holds Koan's view of spaces, tabs, folders, splits, history, bookmarks, logins and
  recently closed tabs. `SpacesRepository.spaces` combines spaces, tabs and folders into
  `SpaceWithTabs` (essentials, pinned entries with folders, the card stack).
- **BrowserStore** (AC) holds live browsing state. Every Room tab can have a BrowserStore tab with
  the same id (`koanTabId`), created the first time the tab is opened on the phone. The UI reads the
  store directly for page info, loading, find, prompts, permissions, downloads, translations and
  extensions (`observeSlice`). Navigation (url, title) flows back into Room through `TabManager`.
- The one engine view shows the store's selected tab; `TabManager.current` is Koan's current tab.
- Sync reconciles Room with the Zen `spaces` collection, read-only (see "Sync").

## Storage

### Room (`data/KoanDatabase.kt`, `koan.db`, version 8)

- `spaces`: `SpaceEntity(uuid, name, icon, gradientColors (comma-separated longs), containerGuid?, order, zenOrigin)`
- `tabs`: `TabEntity(koanTabId, zenTabId?, spaceUuid, folderId?, splitId?, url, title, iconUrl?,
  staticIcon, pinned, essential, order, zenOrder, lastAccessed, thumbnailKey?, origin: ZEN|LOCAL,
  dirty, closed, cardOrder?, zenUrl?)`
- `folders`: `FolderEntity(folderId, spaceUuid, parentFolderId?, name, icon?, collapsed, order)`
- `splits`: `SplitEntity(splitId, spaceUuid, tabIds (JSON), gridType)`
- `sync_meta`: `SyncMetaEntity(collection, lastModified, lastSyncAt)`
- `bookmarks`: `BookmarkEntity(id, url (unique), title, createdAt)`; synced rows have id `sync:<id>`
- `visits`: `VisitEntity(url, title, lastVisit, visitCount, fromSync, hidden)`
- `logins`: `LoginEntity(guid, origin, formActionOrigin?, httpRealm?, usernameField, passwordField,
  secret (sealed username + password), timeCreated, timeLastUsed, timePasswordChanged, local)`
- `closed_tabs`: `ClosedTabEntity(koanTabId, tab (embedded TabEntity), closedAt, engineState?)`, 25 kept

`zenTabId` is set for tabs that came from Zen; phone-only tabs are `LOCAL`. Ids of Zen rows are
`zen:<tabId>`, essentials `zen:<spaceUuid>:<tabId>`. `order` uses a stride of 8. Migrations 2→8 are
written by hand (v1 falls back to a destructive migration); schemas are exported to `app/schemas`.
Private tabs use a second, in-memory `KoanDatabase` with the same schema.

### Other stores

- **DataStore (Preferences):** `settings` (`data/Settings.kt`: theme, hide status bar, sync-back
  switches, picture-in-picture, download location, suggestions, search engine and custom engines,
  start-page delay, tour seen, uBlock counters, privacy flags), `account` (the sealed account),
  `last_place` (`data/LastPlace.kt`: last space, last tab, when Koan was hidden),
  `site_permissions` (`engine/SitePermissions.kt`).
- **SharedPreferences:** `hidden_logins`, `history_deletions`, `sync_write` (last run, sync ids,
  backup names, status), `app_links` ("always open in this app"), `translate` (recent languages).
- **Keystore** (`data/SecretBox.kt`): `KeystoreBox(alias)` is an AES-256-GCM key in
  `AndroidKeyStore`. Three aliases: `koan_account` (account blob), `koan_logins` (each login's
  secret), `koan_private` (private tabs file). Showing passwords and unlocking Private go through
  `androidx.biometric` in the UI.
- **Files:** `files/thumbs` (screenshots), `files/thumbs-blur` (baked blur), `files/icons` (Zen
  inline icons as PNG), `cache/` favicon cache (40 MB cap), `files/private/tabs.sealed`,
  `files/history_ids.json`, `files/sync-backup/*.jsonl` (+ a copy in the app's external files),
  `cache/captures` (camera captures for file fields, shared through `FileProvider`). AC keeps the
  engine session snapshot (`SessionStorage`) and the download list (`DownloadStorage`).
- `SafeStores`: every DataStore replaces a corrupted file with an empty one (settings back to
  defaults) and reads give defaults on an I/O error, so a full disk can't crash-loop the start.
- **Downloads** go to the folder picked in Settings (a SAF tree) or the phone's Download folder;
  from a private tab always the Download folder.

## Engine

- AC modules in use: `browser-engine-gecko`, `browser-state`, `concept-engine`, `feature-session`,
  `feature-tabs`, `browser-session-storage`, `lib-state`, `feature-readerview`,
  `feature-downloads`, `feature-media`, `concept-storage`, `support-utils`,
  `support-webextensions`, plus `geckoview-omni`. None of AC's View widgets. Prompts, site
  permissions, the long-press menu, find in page, search and suggestions are Koan's own code over
  `BrowserStore` and `EngineSession`.
- One `GeckoRuntime` per process: `KoanApp.components` → `Components.runtime`, with
  `LoginAutofill` as its `Autocomplete.StorageDelegate`.
- `GeckoEngine` settings (`Components.engine`): tracking policy from `PrivacyEngine`, private-only
  HTTPS-only / fingerprinting protection / query stripping / email trackers, `HistoryRecorder` as
  history delegate, `ExternalLinks` as request interceptor.
- `BrowserStore` middleware: `EngineMiddleware`, `ReaderViewMiddleware`, `DuplicateMiddleware`,
  `DownloadMiddleware` (never deletes files on removal), `TranslationsMiddleware` (needs a
  `MainScope`). `DuplicateMiddleware` sees every download, including Save image and a PDF
  viewer's Download. For "is this page translated", Gecko's `requestedTranslationPair` is the
  truth: its processing flags can stay up.
- Gecko preferences without a GeckoView API go through `GeckoPreferenceController`
  (`engine/GeckoPrefs.kt` for find colours, `privacy/PrivacyEngine.kt`).
- uBlock Origin: installed from AMO at first start with `engine.installWebExtension`
  (`engine/UBlock.kt`), its prompts approved automatically, allowed in private browsing; its badge
  count feeds the "blocked this week" figure.

### Built-in extensions (`app/src/main/assets/extensions`)

Each is installed with `support-webextensions`' `BuiltInWebExtensionController` and talks over a
native port per engine session. Bump the manifest version when a script changes.

| Extension | Kotlin side | Messages |
|---|---|---|
| `opensearch` | `engine/PageEngines.kt` | Kotlin asks `find`; the page answers with its declared OpenSearch XML (only when the ⋯ menu asks) |
| `pageimage` | `engine/PageImages.kt` | The script remembers the long-press point; when Save is tapped Kotlin asks `full` and the page answers with the largest fully loaded image there (waits up to 10 s) |
| `video` | `engine/PageVideo.kt` | The page reports `state`, `media`, `gone`; Kotlin sends `play`, `pause`, `seek`, `rate`, `mute`, `release`. Not injected on YouTube |

### Tab lifecycle and memory (`engine/TabManager.kt`)

- Start: `TabManager.start` restores the engine tabs from `SessionStorage`, removes engine tabs
  whose Room row is gone, reopens the last tab (`LastPlace`), then watches the store and Room. The
  UI waits for `started`. Engine tabs are saved by `SessionStorage.autoSave` on change,
  periodically in the foreground and when going to the background.
- Open: selects the tab's engine tab, creating it on first open. Zen tabs never opened on the phone
  have no engine tab, only a URL. A new blank tab has no engine tab until its first load.
- Engine → Room: url/title changes are written after a 400 ms debounce (and recorded in history);
  a tab a page opened joins the stack next to its opener. Room → engine: a tab that lost its card
  (closed in Zen, hidden by a sync) drops its engine tab.
- Live sessions: the most recently used stay live, the others are suspended
  (`SuspendEngineSessionAction`) and restored on open. The count comes from `DeviceProfile`
  (read once at startup from reported RAM):

  | Tier | Live sessions | Screenshot width | Screenshot memory | Baked memory | Baked levels |
  |---|---|---|---|---|---|
  | High (≥ 6 GB phones) | 4 | 720 px | 64 MB | 24 MB | 3 |
  | Mid (4–6 GB) | 2 | 540 px | 32 MB | 12 MB | 2 |
  | Low (< 4 GB or low-RAM device) | 1 | 480 px | 16 MB | 6 MB | 2, no baked placeholders |

  Private tabs get half as many live sessions (at least 1), on top of the normal ones.
- `onTrimMemory`: at `BACKGROUND` and above, or `RUNNING_LOW/CRITICAL`, only the current session
  stays live (none for private tabs if none is on screen); `RUNNING_CRITICAL` also empties the
  decoded-screenshot caches. (Android 14+ only sends `UI_HIDDEN`/`BACKGROUND`.)
- A crashed content process is restored for the current tab. A tab that only held a download is
  closed.
- Close: normal tabs close (Zen rows are marked `closed`, local rows deleted); pinned and essential
  tabs are only unloaded. `closeForUndo` keeps the row and the engine state in `closed_tabs` for
  Undo and "Recently closed".

### Thumbnails (`thumbs/`)

- Captured with the engine view's capture API: when leaving a tab, when the overview opens, when
  Koan is paused, 800 ms after first paint, 1.5 s after a load ends, 1 s after scrolling stops; at
  most one background capture per tab every 4 s. A capture is dropped if the tab switched or
  navigated meanwhile, or if it is blank.
- `ThumbnailStore`: WebP quality 80, width per tier (table above), on-disk LRU capped at 150 MB,
  in-memory LRU of decoded hardware bitmaps sized per tier. Room's `thumbnailKey` is the file name,
  new per capture. Decoding is off the main thread; cards near the current one are prefetched.
- Zen tabs never opened on the phone have no thumbnail: favicon + domain on a placeholder.
- Private screenshots are kept in memory only.

## Private tabs (`privacy/`)

- `PrivateTabs`: rows live in an in-memory Room database (one space `koan-private`), never on disk,
  never synced. A second `TabManager(isPrivate = true)` runs their engine tabs (GeckoView private
  sessions) over the same `BrowserStore`; each manager only touches its own kind of engine tab. No
  history, no last place.
- `PrivateVault`: each private tab's url, title and engine state sealed with the Keystore key
  `koan_private` in `files/private/tabs.sealed`, written a second after a change and when Koan
  goes to the background; deleted with the last private tab.
- GeckoView keeps private cookies and site data in memory: a hidden private `GeckoSession`
  (`keeper`) keeps that context alive while private tabs exist. Closing the last private tab closes
  it, drops the in-memory screenshots and deletes the sealed file. After a process death the
  cookies are gone.
- `PrivacyEngine`: tracking policy (bounce tracking per setting), DNS over HTTPS (optional), and
  Gecko prefs (WebRTC address hiding, tracking content and GPC in private, cookie banners).
  `ExternalLinks` also rewrites Google AMP links and strips tracking parameters on private loads.
- `PrivateMode`: shown or not (per process, off on a cold start) and the lock (after 1 min away or
  the screen turning off; fingerprint or device credential). While Private is shown the window is
  `FLAG_SECURE` unless "Allow screenshots" is on.
- `ui/KoanRoot.kt` swaps the spaces, repository and tab manager it reads; `ui/privacy/PrivateFlip`
  runs the transition. A link from another app always leaves Private first.

## Sync

### Sign-in and account

- Password: Mozilla's accounts page in a system `WebView` (`ui/signin/MozillaSignIn.kt`) →
  `AccountStore.completeWebLogin` → session token + kB.
- From a computer: scan or paste a "Connect another device" link → `PairingFlow` →
  `AccountStore.completePairing` → OAuth refresh token + scoped Sync key (`ScopedKeys`). A pairing
  link from another app first shows a confirmation card (`Components.pairLink`): Sign in, or
  Replace account when one is signed in. `AccountStore` refuses another account's sign-in
  (`OtherAccount`) unless replacing was confirmed; screens show the account as `maskedEmail`.
- After a sign-in the sign-in `WebView`'s cookies and storage are wiped.
- The account is stored as one blob sealed with `koan_account` in the `account` DataStore. Sign-out
  destroys the token, the blob and the key, and drops all Zen and synced rows.

### Reading (`data/SpacesSync.kt`)

- A pull runs when Koan comes to the foreground if the last attempt is older than 2 min, every
  15 min while in the foreground, and on demand (sync badge, drawer status row, Settings, right
  after sign-in). There is no background (WorkManager) pull.
- Steps: `AccountStore.connect` (token-server credentials, cached) →
  `SpacesSyncService.loadSnapshot` (the `prefs` and `spaces` collections, decrypted and decoded by
  `ZenSpaces`) → `ZenRoomMapper.map` → `ZenDiff.between` → `KoanDao.applyZenDiff` in one
  transaction. A pull writes only changed rows; phone-side reorder, folder collapse, closes and
  phone-navigated pages survive. An empty snapshot keeps local data.
- Never blocks the UI. Errors show as a badge on the space header and in the drawer's status row.
- The same pull then runs `LibrarySync` (at most every 30 min unless asked): `passwords` (re-sealed
  into `logins`), `bookmarks`, the 5 000 most recent `history` records.
- Saved logins feed GeckoView's login autocomplete through `engine/LoginAutofill.kt`.

### Writing (`data/SyncWrite.kt`)

- **Nothing writes the Zen `spaces` collection.** Closing a Zen tab on the phone only hides it
  locally. `SyncClient` checks every request that is not a GET / HEAD with `SpacesWriteGate`: a
  write to `spaces` throws `SyncError.WriteBackDisabled` while `Settings.writeBackEnabled` is off
  (default; not in the UI). A read never writes `crypto/keys`.
- Passwords and history can be sent to the account, each behind its own switch in Settings
  (`Settings.syncBack`, both off by default); password edits and deletions need a second switch
  (`syncBackPasswordEdits`). Bookmarks are not uploaded.
- A run (after a pull, at most every 30 min unless asked) does, per collection:
  `WriteGuard.refusal` (storage version, engine version, collection not declined, sync id
  unchanged) → a one-time backup of the collection's encrypted records → an upload plan
  (`sync/SyncBackPlan.kt`, `sync/SyncWritePlan.kt`) → the first 3 records posted and read back,
  then batches of 50, each conditional on the collection not having changed.
- History tombstones (`HistoryWritePlan`) are only the pages removed on the phone while History
  sync was on (`data/HistoryDeletions.kt`), never the visits "Clear browsing data" hides.
- A refusal, a conflict or an HTTP error turns that collection's switch off and shows a status line
  in Settings. A cancelled run, a network error or a 5xx / 429 keeps it on and retries next run.

## UI

- `MainActivity.setContent` waits for the theme and "Hide status bar" settings, then draws
  `KoanTheme { KoanRoot(components) }`.
- `ui/KoanRoot.kt` owns navigation: overview open or not, editing (search), start page, sign-in,
  current space, hero card. Layers, bottom to top: `BrowserScreen` → web player → `OverviewScreen`
  → hero morph layer → `StartPage` → `SearchSheet` → `KoanBar` (dock) → `KoanOverlays` → translate
  offer → standalone player → toasts → sign-in → private lock cover.
- `ui/KoanOverlays.kt`: `Overlays` is the state of everything that opens over the page (⋯ menu,
  prompt cards, Settings, Downloads, History, tour); `KoanOverlays` draws them.
- Overview ↔ page: the hero morph animates the tab's screenshot between its card and the page
  rectangle; the live GeckoView is swapped in once it lands. A page cover shows the screenshot
  until the engine reports a first paint.
- The dock hides with GeckoView's dynamic toolbar (`DockState`, `BrowserScreen`); the page ends
  above the navigation bar and the keyboard, resized once per keyboard show or hide.
- Back: sheets, the drawer, search and the player handle it first; on a page it goes back in the
  tab, then closes the tab towards its opener tab, the app that opened it (`engine/ReturnToApp.kt`)
  or the next card; with no tab, the overview.

### Video player (`ui/player/`, `engine/PageVideo.kt`, `engine/PictureInPicture.kt`)

- Web: when a page's video goes fullscreen, `WebPlayerLayer` draws Koan's `VideoPlayer` over it and
  drives the video through the `video` extension. YouTube keeps its own player.
- Standalone: a downloaded video, a link to a video file or a video file from another app plays in
  `StandalonePlayer` with `NativeVideo` (platform `MediaPlayer` on a `TextureView`).
- Picture-in-picture for both, with remote actions; "Audio only" closes the window and keeps the
  media notification (`KoanMediaService`) playing.

### Links from and to other apps

- In: `MainActivity` handles `VIEW` http/https, `VIEW` video files, `WEB_SEARCH`, `SEND` text;
  `SearchSelection` forwards `PROCESS_TEXT`; `KoanCustomTabsService` makes Koan a Custom Tabs
  provider (links arrive as `VIEW`). `MainActivity.handleIntent` routes a pairing link to sign-in,
  a video to the player, anything else to `Components.incoming`, which `KoanRoot` opens as a new
  tab of the current space.
- Out: `engine/ExternalLinks.kt` intercepts app schemes and `intent://`: opens the app directly
  for system schemes and typed links, otherwise asks on a card ("always" is remembered per site).
  The manifest lists the schemes Koan may resolve instead of `QUERY_ALL_PACKAGES`.

### Update check and "What's new"

- `update/UpdateCheck.kt` reads the repository's GitHub releases feed once, only when About opens,
  and compares the newest version tag with the installed version. Nothing is downloaded.
- On a normal launch `MainActivity` shows the tour (first run) or a version's "What's new" once
  (`ui/tour/TourContent.kt`, `Settings.tourSeen`).

## Performance budgets (release build, owner's phone)

| Metric | Budget |
|---|---|
| Cold start → overview with cached spaces visible | < 800 ms |
| Overview scroll with 50 cards | no frame > 16 ms on 60 Hz panels, aim for display refresh (90/120 Hz) |
| Card open morph | starts within 1 frame of tap, no dropped frames |
| Space switch drag | 1:1 with finger, no dropped frames |
| Memory with 4 live sessions + overview | no OOM / process kill in normal use |

Keep card list items `@Stable`/immutable; use `key()` by `koanTabId`; read scroll offsets inside
`graphicsLayer {}` lambdas, never in composition. A/B frame timing and start times of two builds on
the phone: `tools/perf/` (see its README). Perfetto/System Trace for anything deeper.

## Blur strategy

- Cards in the pile use pre-blurred copies (`thumbs/BakedBlur.kt`): baked on one background thread
  when a screenshot is captured (never during the overview's open or close motion), stored next to
  it in `files/thumbs-blur`, uploaded once the overview has settled, crossfaded by depth. Until its
  copies are uploaded a card blurs live (`RenderEffect`, Android 12+).
- The drawer blurs the overview behind it with copies baked when it opens (`ui/bar/DrawerBackdrop.kt`).
- On Android 11 there is no live blur (`DeviceProfile.liveBlur`): cards use their baked copies
  only, and a stronger dim stands in where a live blur would show.

## Bumping android-components + GeckoView

Both versions move together (`gradle/libs.versions.toml`). After a bump, check by hand:

- AC sets the compileSdk floor (AAR metadata): AC 157 needed 37.1, AC 160 needs 37.2 (install
  the SDK platform, then `compileSdk { version = release(37) { minorApiLevel = N } }`).
- The media notification's timeline (position, seeking) is on by default since AC 160; before,
  Koan switched feature-media's Nimbus flag on by reflection (removed with the bump).
- Downloads are named by `DefaultSettings.downloadDelegate` (`DownloadFileUtils.guessFileName`
  with the server's Content-Disposition, which Gecko validates since 160). Without a delegate AC
  only has the URL's name.
- `engine/GeckoFinder.java` calls `GeckoEngineSession`'s Kotlin-internal GeckoSession getter (find
  in page "highlight all").
- `GeckoPreferenceController` (`engine/GeckoPrefs.kt`, `privacy/PrivacyEngine.kt`) is GeckoView's
  experimental prefs API; the pref names are Gecko's.
- `ui/browser/BrowserScreen.kt` removes GeckoView's keyboard insets listener by its name.
- `app/proguard-rules.pro` keeps SnakeYAML (R8 once broke GeckoView's config reading at start).
- `libmegazord.so` is excluded from the APK; Gecko's crash-helper libraries must stay.
- AC modules used without being declared (they arrive through the declared ones): `support-base`,
  `concept-base`. (`ErrorType` moved from `browser-errorpages` to `concept-engine` in AC 160.)
- Install and launch a full R8 release build on the phone.

## Dependencies (ask before adding others)

In use (`gradle/libs.versions.toml`, `app/build.gradle.kts`): AndroidX core-ktx, activity-compose,
fragment, lifecycle-runtime(-compose), biometric, browser; Compose BOM (ui, ui-graphics,
foundation, animation, material3); Room (KSP); DataStore Preferences;
kotlinx.coroutines; GeckoView omni and the AC modules listed under "Engine"; androidsvg (favicons);
zxing-core (QR sign-in, decoding only). Tests: JUnit, `org.json:json`, kotlinx-coroutines-test.

Allowed, not used yet: navigation-compose, WorkManager, kotlinx.serialization, androidx.security,
Coil 3, baselineprofile, Turbine, Robolectric.

## Decisions log

- 2026-10-12 — minSdk 34; live RenderEffect blur first, baked blur as fallback (owner).
  _(minSdk superseded on 2026-10-23.)_
- 2026-10-12 — **android-components (non-UI parts only) over raw GeckoView.** None of AC's View
  widgets (BrowserToolbar, tabstray); all UI is our own Compose that observes `BrowserStore`.
  Reason: AC gives us downloads, add-ons and session state save/restore for free; the "fights the
  UI" risk only applies to its widgets. Costs: bigger APK, API churn per release, AC pins some
  AndroidX versions we must follow. Versions then: AC 157.0.1 + GeckoView (omni)
  157.0.20261005135250; now 160 (see the 2026-11-11 entry). Bump both together.
- 2026-10-12 — **Hero morph animates the screenshot, never the live GeckoView.** GeckoView renders
  into a SurfaceView that can't be clipped/scaled by Compose; the live view is swapped in once the
  morph lands. No TextureView mode.
- 2026-10-12 — **arm64-v8a only** (`abiFilters`). Sideloaded; keeps the GeckoView-omni APK small.
  `-Pkoan.emulatorAbi` adds x86_64 for emulator test runs only.
- 2026-10-12 — Sync port keeps **org.json** (faithful to Z-Sync); `org.json:json` is a test-only
  dependency so JVM tests get a real implementation. Credentials: own Keystore AES-GCM wrapper
  over DataStore instead of deprecated `EncryptedSharedPreferences`.
- 2026-10-14 — **Spaces sync is read-only**: only the read half of Z-Sync's `SpacesSyncService` is
  ported. Ported files carry small `Koan:` edits (offline vs expired session via FxA errno
  110/102, redacted `TokenServerCreds.toString`).
- 2026-10-14 — **Favicons**: Zen's inline `data:` icons are normalised to ≤128 px PNGs on disk at
  sync time (Room holds only a key); other sites go through Z-Sync's `FaviconLoader`, whose last
  resort asks DuckDuckGo's icon service for the domain, never Google's (owner's choice). Decoded
  icons are prewarmed at start/sync.
- 2026-10-14 — **Stack scale**: the depth stack composes only cards near the focus and applies data
  updates only when the stack is idle, anchored on the focused card.
- 2026-10-14 — **Page cover**: a Compose layer over the engine view shows the tab's screenshot (or
  placeholder) until the engine reports a fresh first paint; no white flash on open, restore or
  cold start. Dock hides with GeckoView's dynamic toolbar (`setDynamicToolbarMaxHeight` +
  `setVerticalClipping`), driven by GeckoView's nested scrolling through Compose.
- 2026-10-16 — **Visual system**: light theme fully neutral (no space wash); the brand gradient
  (`BrandTokens`) marks only "Koan is working / active / your focus". Shared controls live in
  `ui/kit`.
- 2026-10-23 — **Page bottom**: the engine view is resized once per keyboard show or hide, never
  along its slide; GeckoView's own keyboard listener is removed after each attach. With the
  keyboard in a page field, or find open, the dynamic toolbar max is 0.
- 2026-10-23 — **minSdk 30 (Android 11+)** with RAM-based memory tiers (`DeviceProfile`); costs per
  API level in `docs/audits/MIN_SDK.md`. Phones with ≥ 6 GB on Android 12+ behave as before.

- 2026-10-26 — **Sign in from a computer** with Zen's "Connect another device" QR code (OAuth
  pairing, `PairingFlow`, zxing decoding only); password sign-in stays.
- 2026-11-10 — **Two-way sync for passwords and history** (M6), each behind its own switch, off by
  default; Spaces, tabs and bookmarks are never written. On-device page translation (Firefox
  Translations, models downloaded per language).
- 2026-11-11 — **Targeted `<queries>`** instead of `QUERY_ALL_PACKAGES` (which android-components
  adds; removed in the manifest): Koan only asks for the apps behind web links and app links.
- 2026-11-11 — **Video player**: Koan's own controls over the page's video (an extension hides the
  site's player in fullscreen); the standalone player uses Android's `MediaPlayer`; the media
  notification's timeline was switched on by reflection on feature-media's Nimbus flag (on by
  default since AC 160).
- 2026-11-11 — **Private mode** on GeckoView's real private browsing: a second `TabManager` over an
  in-memory Room, tabs sealed on disk with `koan_private`, wiped with the last private tab.
- 2026-11-11 — **Audit fixes**: `SpacesWriteGate` + `Settings.writeBackEnabled` make rule 4 a code
  check; guarded process scopes (`KoanScopes`) and `SafeStores`; Koan's own requests go through
  `KoanHttp` (the engine's User-Agent, capped reads); `res/raw/keep.xml` keeps the `zen_*` icons
  that `ZenIcon` loads by name, with a build check (`checkReleaseZenIcons` / `checkStagingZenIcons`).
- 2026-11-11 — **android-components 160.0 + GeckoView 160.0.20261104181552**, compileSdk 37.2.
  Downloads take the server's file name (`downloadDelegate`; it was never set before, so names
  came from the URL). GeckoView 160 implements Android's `ScrollCaptureCallback`: "Capture more"
  works on stock Android; HyperOS's long screenshot doesn't use that API.
