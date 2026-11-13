<p align="center"><img src="branding/koan-icon.png" width="128" height="128" alt="Koan icon"></p>

<h1 align="center">Koan</h1>

<p align="center">
  <a href="https://github.com/Nerzouille/Koan/releases">Download</a> ·
  <a href="docs/FEATURES.md">User guide</a> ·
  <a href="docs/PRIVACY.md">Privacy</a> ·
  <a href="CHANGELOG.md">Changelog</a>
  <br><br>
  <a href="https://github.com/Nerzouille/Koan/actions/workflows/ci.yml"><img src="https://github.com/Nerzouille/Koan/actions/workflows/ci.yml/badge.svg?branch=dev" alt="CI"></a>
</p>

A personal Android web browser built on **GeckoView** (Firefox's engine): a card stack of tabs
with depth, one-handed gestures, and workspaces that are your **Zen Browser Spaces**, read from
your Mozilla account: the same spaces, icons, colours, essentials, pinned tabs and folders you have
on the desktop.

> Not affiliated with, endorsed by or connected to Zen Browser or Mozilla.

**Beta: daily-driver ready. Spaces are read-only; passwords and history can sync both ways if you
turn it on.**

## Features

- **Your Zen Spaces** as workspaces, with their essentials, pinned tabs and folders in a drawer.
  No account? It works with one local space.
- **A card stack of tabs** with depth; swipe the dock up for it, sideways for the next tab,
  long-press the address pill to switch space.
- **Dock long presses:** back lists the pages before this one, the tab count opens New tab /
  Close tab / Reopen closed tab, ⋯ opens Settings, History (in the overview) opens Recently closed.
- **Recently closed:** the last 25 closed tabs come back in their space and place, with their
  history.
- **What pages ask, in Koan's style:** drop-down lists, date/time/colour pickers, alerts and
  prompts, site sign-in, pop-ups, printing.
- **Pages fit around the dock and the keyboard:** a page's bottom bar (chat field, cookie banner)
  sits on the dock and follows it as it hides; a field you type in shows right above the keyboard.
- **Search** with suggestions, inline completion, `@` shortcuts (`@yt`, `@wiki`…) and your own
  search engines; open tabs, history and bookmarks in the results.
- **Private browsing:** a separate set of private tabs with its own look, locked behind your
  fingerprint, no screenshots, nothing kept in history, HTTPS only, tracking and fingerprinting
  protection. **Private bookmarks** are kept apart and encrypted on the phone.
- **Koan's own video player** for full-screen video: double-tap to skip, hold for 2×, swipe for
  brightness and volume, speed, lock; picture-in-picture with "Audio only".
- **Translation on the phone** (Firefox Translations): pages are translated without their text
  leaving the phone.
- **Passwords and autofill** from your account and saved on the phone, behind your fingerprint.
- **Two-way sync, if you want it:** passwords saved on the phone and history can go back to your
  Mozilla account, each behind its own switch (off by default).
- uBlock Origin, downloads, find in page, reader view, site settings, and "Open in app" for links
  that have an app.

Everything, screen by screen: [`docs/FEATURES.md`](docs/FEATURES.md).

## Screenshots

<!-- Taken on a phone; pages from Wikipedia, Wikimedia Commons and this repository. -->

| Tab stack | A page | Search |
|:---:|:---:|:---:|
| <img src="docs/media/overview.png" width="240" alt="The tab stack of a space"> | <img src="docs/media/page.png" width="240" alt="A page with the dock"> | <img src="docs/media/search.png" width="240" alt="The search sheet with history, open tabs and suggestions"> |
| **Private browsing** | **Translation** | **⋯ menu** |
| <img src="docs/media/private.png" width="240" alt="The private overview, dark"> | <img src="docs/media/translate.png" width="240" alt="A Chinese page with the translate icon in the address bar"> | <img src="docs/media/menu.png" width="240" alt="The ⋯ menu sheet"> |

### Koan's own video player

<p align="center"><img src="docs/media/player.png" width="780" alt="Koan's video player in landscape on Big Buck Bunny"></p>

### Koan's colours

| About | Settings | What's new |
|:---:|:---:|:---:|
| <img src="docs/media/about.png" width="240" alt="The About page"> | <img src="docs/media/settings.png" width="240" alt="Settings, not signed in"> | <img src="docs/media/whatsnew.png" width="240" alt="The What's new sheet for 1.0"> |

## Status

A one-person hobby project, built for my own phone and shared as-is.

- Sideloaded only: no Play Store, no support. Issues and PRs may go unanswered.
- Android 11+ (minSdk 30): tested on Android 14 and 17; 11–13 are supported but less tested.
  64-bit ARM (arm64-v8a) only. The APK is about 223 MB.
- See [`docs/ROADMAP.md`](docs/ROADMAP.md) for what is done and what is planned.

## Install

1. Download the APK from the [Releases page](https://github.com/Nerzouille/Koan/releases)
   (`koan-vX.Y.Z.apk`, about 223 MB) and open it. Android asks you to allow installs from your
   browser or file manager the first time.
2. Optional: check the file against the SHA-256 in the release notes:
   `sha256sum koan-vX.Y.Z.apk`.
3. Open Koan and sign in with your Mozilla account (on Mozilla's own page, or by scanning the code
   Zen shows on your computer), or skip it and just browse.

**Requirements:** Android 11 or newer (tested on Android 14 and 17), a 64-bit ARM phone
(arm64-v8a).

**Updates:** Settings › About Koan checks GitHub when you open it. To get updates automatically,
add `https://github.com/Nerzouille/Koan` to [Obtainium](https://github.com/ImranR98/Obtainium);
turn on its **Include prereleases** option while Koan's releases are betas. Installing over a
previous version keeps your tabs, spaces, logins and settings.

## Privacy

Koan has no account of its own, no server, no analytics, no telemetry, no ads and no crash
reporting.

- Your tabs, history, bookmarks, downloads and settings stay on the phone. Passwords and the
  Mozilla account keys are encrypted with a key held by Android's Keystore.
- Sign-in and sync talk only to **Mozilla's servers**. Sync data is end-to-end encrypted.
- Koan writes to your account only what you turn on (passwords saved on the phone, history).
  **Spaces, tabs and bookmarks are never written.** Writing Spaces back to Zen is planned behind a
  setting that will be **off by default**, because a bad write could scramble your desktop Spaces.
- The other requests are the ones any browser makes: your search engine, site icons, uBlock
  Origin's filter lists, Firefox's own lists, and GitHub's releases feed when you open About.

Every network request Koan makes, and why: [`docs/PRIVACY.md`](docs/PRIVACY.md).

### Sign-in caveat

To be allowed into Firefox Sync, Koan's sign-in **identifies as Firefox** (desktop, or Android
when you sign in from your computer). This is unofficial: it can break whenever Mozilla changes
something, and it may go against Mozilla's terms of service. **Use it at your own risk**, ideally
not with an account you can't afford to have trouble with.

## Build

Requirements: JDK 17 and the Android SDK (platform 37.2; Android Studio or the command-line tools).

```sh
git clone https://github.com/Nerzouille/Koan.git
cd Koan
./gradlew assembleDebug              # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest          # JVM unit tests (Robolectric for Room, DataStore and sync)
```

The sync layer is checked against [Z-Sync](https://github.com/Kjell6/Z-Sync)'s golden contract
fixtures, read in place from a local checkout (gitignored, never modified). Without it, those tests
are skipped:

```sh
git clone --depth 1 https://github.com/Kjell6/Z-Sync.git reference/Z-Sync
```

**Release build** (R8 on; judge animations on this one): `./gradlew assembleRelease`. It signs with
the key named in a `keystore.properties` file at the repo root (gitignored, keystore kept outside
the repo):

```properties
storeFile=/path/to/your-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without that file, the release build is signed with the debug key so that it still installs.

**Baseline profile:** `app/src/main/generated/baselineProfiles/` is generated on a connected phone
(Android 13+) with `./gradlew :app:generateBaselineProfile`. It drives its own copy of the
app (`dev.nerzouille.koan.bp`), never an installed Koan.

CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs the unit tests and a debug build on
every push and pull request to `dev` and `main`.

## Feedback

Found a bug or have an idea? Open an issue:
[bug report](https://github.com/Nerzouille/Koan/issues/new?template=bug_report.yml) (phone model,
Android version, Koan version from Settings › About Koan, what happened) or
[feature request](https://github.com/Nerzouille/Koan/issues/new?template=feature_request.yml).
Please leave private things out of issues: account emails, real tabs, passwords.

## Credits

- [Z-Sync](https://github.com/Kjell6/Z-Sync) by Kjell Behrends (MIT): the sync, crypto, Mozilla
  sign-in and favicon code is ported from it, and its contract is what makes reading Zen Spaces
  possible.
- [Zen Browser](https://github.com/zen-browser/desktop): the Spaces concept, and the space and
  folder icons (MPL 2.0, most of them [Ionicons](https://ionic.io/ionicons) glyphs).
- [Tabler Icons](https://tabler.io/icons) (MIT) and the [Zia](https://github.com/z1n-k/zia) mod
  (MIT): the icons, and Zia's Phosphor-to-Tabler table for space icons picked with Zia.
- [Big Buck Bunny](https://peach.blender.org/) (© 2008 Blender Foundation, www.bigbuckbunny.org,
  [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)): in the What's new video player card
  and the video player screenshot above, played from Wikimedia Commons.
- [GeckoView and Mozilla Android Components](https://github.com/mozilla-firefox/firefox) (MPL 2.0).

Firefox and Mozilla are trademarks of the Mozilla Foundation. Zen Browser is an independent project.

## Licence

Koan's own code is under the [MIT licence](LICENSE). Third-party parts keep their own licences:
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
