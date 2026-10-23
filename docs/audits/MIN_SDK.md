# Audit: lowering minSdk below 34

Audit only, nothing changed. Method: lint (`NewApi`/`InlinedApi`) with `minSdk = 29` set temporarily,
plus a code read for what lint can't see (silent no-ops, behaviour changes). Lint at 29 found 10
`NewApi` errors and 4 `InlinedApi` warnings, all listed below.

## Libraries

| Library | minSdk |
|---|---|
| GeckoView 157 (omni) | 26 |
| All android-components 157 / app-services used | 26 |
| AndroidX (Compose BOM 2026.09, Room, DataStore, biometric, activity…) | ≤ 24 |
| androidsvg | 14 |

None of them blocks 29, 30 or 31. **But** `abiFilters` is arm64-only: phones on Android 10–11 are
sometimes 32-bit only (armeabi-v7a, especially "Go" phones). Supporting them means adding the
v7a ABI (GeckoView-omni ships it; roughly +40–60 MB APK, or one APK per ABI).

## API 31 (Android 12)

| API | Where | Level | Fix |
|---|---|---|---|
| `URLEncoder.encode(String, Charset)` | `search/SearchEngine.kt:18,23`, `search/SearchModel.kt:32,34` | 33 | `encode(s, "UTF-8")` (no new dependency). Without it: **crash** on every search. |
| `WebSettings.isAlgorithmicDarkeningAllowed` | `ui/signin/MozillaSignIn.kt:258` | 33 | Version guard; below 33 the sign-in page isn't darkened (degraded). `androidx.webkit` would fix it but adds a dependency. |
| `HapticFeedbackConstants.SEGMENT_TICK` | `ui/overview/DepthStack.kt:268` | 34 | Inlined int, no crash; below 34 the scroll ticks are **silent**. Guard + fall back to `CLOCK_TICK`. |
| `ClipDescription.EXTRA_IS_SENSITIVE` | `ui/settings/PasswordsPage.kt:221` | 33 | Inlined string, harmless. Below 33 there's no system "copied" confirmation and no clipboard preview to hide, so Koan should show its own "Copied" toast on 31–32 (it relies on the system one today). |

Everything else is fine at 31:
- **Blur:** `RenderEffect` exists, nothing changes.
- **Notification permission:** a runtime permission only on 33+; below that it's granted at install.
  The `POST_NOTIFICATIONS` request comes from android-components' downloads feature. Nothing to do.
- **Predictive back:** system animation only on 33/34+. Compose `BackHandler` (the app has no
  `PredictiveBackHandler`) works as plain back. Nothing to do.
- **Photo picker:** not used. The file field uses `ACTION_GET_CONTENT` plus camera intents, which work
  on every level.
- **Foreground service types:** `dataSync`/`mediaPlayback` exist since 29. The
  `FOREGROUND_SERVICE_*` permissions are 34-only and ignored below. Fine.
- **Themed icon:** `<monochrome>` is ignored below 33, so the plain adaptive icon shows. Fine.
- **Downloads/storage:** scoped storage, the same as today. Fine.

## API 30 (Android 11)

Everything in 31, plus:

| API | Where | Level | Fix |
|---|---|---|---|
| `BitmapRegionDecoder.newInstance(String)` | `thumbs/ThumbnailStore.kt:63` | 31 | Deprecated `newInstance(path, false)` under a version guard. |
| **`BlurEffect` / `RenderEffect`** | `ui/overview/DepthStack.kt:171`, `ui/bar/DrawerBackdrop.kt:64`, `ui/overview/TabCard.kt` | 31 | **No crash: Compose silently ignores `renderEffect` below 31.** See "Blur" below. |

### Blur below 31

Where blur comes from today:

1. **Card screenshots' baked levels**: already a **CPU** blur at capture time
   (`BakedBlur.bake`, three box blurs ≈ Gaussian, on one background thread, written to disk).
   This works unchanged on 29–30.
2. **Placeholder cards** (`BakedBlur.capture`): already CPU. Works unchanged.
3. **Live blur while baked copies aren't uploaded yet** (`TabCard` `renderEffect = blurFor(...)`):
   GPU. Below 31 it would draw **sharp**, and the fully sharp layer behind it is hidden, so the card
   shows sharp until its copies arrive. Fix: below 31, upload the baked copies as soon as the card is
   near, and until then show the sharp shot with the existing dim only (a short, acceptable
   degradation).
4. **Drawer backdrop** (`DrawerBackdrop.bake`): GPU blur drawn into a layer. Below 31 the "blurred"
   copies would be **sharp**. Fix: the same CPU blur as `BakedBlur`, on a quarter-resolution capture
   (the 14dp blur hides the lost detail), scaled up when drawn.

Measured cost of the existing CPU blur (JVM on a laptop i5-13420H, JIT-warm; multiply by ~2–4× for
a Dimensity 8100-class big core and ~6–10× for an older Cortex-A53/A55 phone):

| Bake | Size | Laptop | Older phone (est.) |
|---|---|---|---|
| Card slice, 3 levels (what every capture costs today) | 360×184 | 9 ms | 50–90 ms |
| Placeholder, 3 levels | 500×256 | 18 ms | 100–180 ms |
| Drawer, 2 levels, quarter-res | 270×600 | 15 ms | 90–150 ms |
| Drawer, 2 levels, half-res | 540×1200 | 66 ms | 0.4–0.7 s |
| Drawer, 2 levels, full-res | 1080×2400 | 337 ms | 2–3 s (not viable) |

So per capture, nothing new: a 3-level card bake already runs on every capture on all levels, off the
UI thread. Only the drawer needs new work, at quarter resolution; at ~100 ms the blur would fade in
slightly late. Doing the drawer bake when the overview settles, instead of at drag start, hides that.
The phone estimates are not measured: there's no test device below Android 14.

## API 29 (Android 10)

Everything in 30 and 31, plus:

| API | Where | Level | Fix |
|---|---|---|---|
| `Bitmap.CompressFormat.WEBP_LOSSY` | `thumbs/ThumbnailStore.kt:96`, `thumbs/BakedBlur.kt:173` | 30 | Guard + deprecated `WEBP` (lossy at quality < 100). Without it: **crash/no thumbnails** on every capture. |
| `Context.getDisplay()` (`activity.display`) | `ui/DisplayRefresh.kt:39` | 30 | `ContextCompat.getDisplayOrDefault(activity)` (androidx.core, already a dependency). |
| `HapticFeedbackConstants.CONFIRM` | `ui/overview/DepthStack.kt:202` | 30 | Inlined; silent below 30. Guard + `VIRTUAL_KEY`. |
| **Biometric `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`** | `ui/settings/PasswordsPage.kt:237` | 30 | Not flagged by lint: androidx.biometric **throws** on this combination on API 28–29, so the passwords page can't open. On 29, use `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` (the key in `SecretBox` doesn't need auth-bound strength, so the prompt is only a gate). |
| IME insets (`WindowInsets.ime`) | `ui/bar/KoanBar.kt`, `ui/browser/BrowserScreen.kt`, `ui/bar/AddressRow.kt`, … | 30 for animation | Works (adjustResize), but the bar **jumps** with the keyboard instead of riding it. Degraded feel, no fix. |
| Downloads | `engine/Components.kt:101`, android-components downloads | 29 = first scoped-storage level | android-components writes through `MediaStore.Downloads` on 29+, so it should work, but this is the least-tested path. Test downloading, opening, deleting and a picked folder. |

The predictive back, notification permission, photo picker, foreground service type, themed icon and
clipboard items are the same as for 31.

## Memory on older, weaker phones

Today's budget assumes the owner's phones: `MAX_LIVE = 4` sessions (`engine/TabManager.kt:603`),
720 px thumbnails, 64 MB of decoded thumbnails plus 24 MB of baked copies in memory, and 3 bake levels.
On a 3–4 GB Android 10–11 phone, several live Gecko pages plus the overview's GPU bitmaps can push the
process into a low-memory kill (lost tabs, and the cold-start screenshot gone).

One existing gap matters more on old phones: `TabManager.onTrimMemory` only reacts at
`>= TRIM_MEMORY_BACKGROUND`. The `RUNNING_LOW`/`RUNNING_CRITICAL` levels that
`docs/ARCHITECTURE.md` promises to handle are below that threshold, so they're ignored. They're
deprecated and no longer sent on 34+, which is why this never showed up on the owner's phones, but
29–33 still send them.

What I'd lower automatically, from `ActivityManager.MemoryInfo.totalMem` / `isLowRamDevice`, read once
at startup:

| Tier | Live sessions | Thumbnail width | Thumb memory LRU | Baked LRU / levels | Other |
|---|---|---|---|---|---|
| ≥ 6 GB (today) | 4 | 720 | 64 MB | 24 MB / 3 | as now |
| 4–6 GB | 2 | 540 | 32 MB | 12 MB / 2 | |
| < 4 GB or `isLowRamDevice` | 1 | 480 | 16 MB | 6 MB / 2 | no placeholder bakes, drawer blur 1 level |

Plus: handle `RUNNING_LOW` → 1 live session and `RUNNING_CRITICAL` → also clear the in-memory LRUs.
The numbers are a starting point: they need a real memory profile (`dumpsys meminfo`) on a weak phone.

## Effort estimate

| Target | Work | Estimate |
|---|---|---|
| **31** | 4 small guards/replacements, own "Copied" toast on 31–32 | ~½ day + testing |
| **30** | 31, plus decoder guard, live-blur fallback, CPU drawer blur | ~2 days + testing |
| **29** | 30, plus WEBP/display/haptic guards, biometric fix, downloads pass | ~3 days + testing |
| Memory tiers + trim fix | Independent of the level, most useful for 29–30 | ~1 day + profiling |
| armeabi-v7a | Only if the target phone is 32-bit | ~½ day (+ APK size) |

"Testing" is the real cost at every level: there's no device below Android 14, and the CLAUDE.md
rule is not to judge feel on an emulator. An emulator can catch crashes and broken flows (search,
thumbnails, passwords, downloads, sign-in), but smoothness and memory need a real old phone.
