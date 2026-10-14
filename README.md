# Koan

A personal Android web browser built on **GeckoView** (Firefox's engine). Tabs live in an
Arc-for-iPhone-style stack of cards, and the workspaces are your **Zen Browser Spaces**, synced
read-only from your Mozilla account: the same spaces, icons, colours, essentials, pinned tabs and
folders you have on the desktop.

> Not affiliated with, endorsed by or connected to Zen Browser or Mozilla.

## Status

A one-person hobby project, built for my own phone and shared as-is.

- Sideloaded only: no Play Store, no releases, no support. Issues and PRs may go unanswered.
- Android 14+ (minSdk 34).
- Work in progress. See [`docs/ROADMAP.md`](docs/ROADMAP.md) for what is done and what is planned.

## Sign-in and sync

- You sign in on **Mozilla's own accounts page**, shown in a WebView. Koan has no login form of
  its own.
- The resulting session and sync key are stored on the device, encrypted with a key held in the
  **Android Keystore**. They are never logged or sent anywhere else.
- Sign-in and sync only talk to **Mozilla's servers** (Mozilla accounts, the Sync token server and
  the Sync storage server it points to). There is no Koan server, no analytics and no telemetry.
  Pages and their favicons load from the websites themselves, as in any browser.
- Spaces are read from the `spaces` collection that Zen writes to Firefox Sync, using the protocol
  documented by [Z-Sync](https://github.com/Kjell6/Z-Sync).

### Caveat

To be allowed into Firefox Sync, the sign-in **identifies as Firefox desktop**. This is unofficial:
it can break whenever Mozilla changes something, and it may go against Mozilla's terms of service.
**Use it at your own risk**, ideally not with an account you can't afford to have trouble with.

Koan only reads your Spaces. Writing back to Zen is planned behind a setting that will be **off by
default**, because a bad write could scramble your desktop Spaces.

## Building

Requirements: JDK 17+ and the Android SDK (Android Studio or the command-line tools).

```sh
git clone https://github.com/Nerzouille/Koan.git
cd Koan
./gradlew assembleDebug        # app/build/outputs/apk/debug/
```

### Tests

```sh
./gradlew test
```

The sync layer is checked against Z-Sync's golden contract fixtures, which are read in place from a
local Z-Sync checkout (gitignored, never modified). Without it, those tests are skipped. To enable
them:

```sh
git clone --depth 1 https://github.com/Kjell6/Z-Sync.git reference/Z-Sync
./gradlew test
```

### Release build

Animations should be judged on a release build (R8 on, not debuggable). Create a
`keystore.properties` at the repo root (gitignored) pointing at your own keystore, kept outside the
repo:

```properties
storeFile=/home/you/.android/koan-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Then run `./gradlew assembleRelease`. Without `keystore.properties`, the release build is signed
with the debug key so that it still installs.

## Credits

- [Z-Sync](https://github.com/Kjell6/Z-Sync) by Kjell Behrends (MIT): the sync, crypto, Mozilla
  sign-in and favicon code is ported from it, and its contract is what makes reading Zen Spaces
  possible.
- [Zen Browser](https://github.com/zen-browser/desktop): the Spaces concept, and the space/folder
  icons (MPL 2.0, most of them [Ionicons](https://ionic.io/ionicons) glyphs).
- [GeckoView and Mozilla Android Components](https://github.com/mozilla-firefox/firefox) (MPL 2.0).
- Arc for iPhone, for the look this tries to live up to.

Full notices: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

Firefox and Mozilla are trademarks of the Mozilla Foundation. Zen Browser is an independent project.

## License

[MIT](LICENSE) for Koan's own code. Third-party parts keep their own licenses, see
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
