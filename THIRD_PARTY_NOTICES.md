# Third-party notices

## Z-Sync

Koan's sync layer (`app/src/main/java/dev/nerzouille/koan/sync/`), favicon loader
(`app/src/main/java/dev/nerzouille/koan/favicon/`), Mozilla sign-in bridge, Zen icon decoding, and
the matching contract tests are ported from or partly adapted from Z-Sync (https://github.com/Kjell6/Z-Sync). Ported files carry a
header comment saying so.

```
MIT License

Copyright (c) 2026 Kjell Behrends

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Zen Browser icons (MPL 2.0)

The space/folder icon drawables `app/src/main/res/drawable/zen_*.xml` come, via Z-Sync, from Zen
Browser (https://github.com/zen-browser/desktop),
`src/browser/themes/shared/zen-icons/common/selectable/*.svg`, converted from SVG to Android vector
drawables. Only these drawables are under the MPL; the Kotlin code that picks them
(`ui/icons/ZenIcon.kt`) is ported from Z-Sync (MIT, above).

Those files are subject to the Mozilla Public License, v. 2.0. If a copy of the MPL was not
distributed with this file, you can obtain one at https://mozilla.org/MPL/2.0/. The Source Code
Form of the original icons is available at https://github.com/zen-browser/desktop.

Most of these icons are glyphs from **Ionicons** (https://github.com/ionic-team/ionicons), some
under Zen's own names (`sun` = `sunny`, `cutlery` = `restaurant`, `lightning` = `flash`…). Ionicons
is under the MIT License:

```
The MIT License (MIT)

Copyright (c) 2015-present Ionic (http://ionic.io/)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

## Tabler Icons (MIT)

`app/src/main/assets/zia/tabler.json` holds Tabler Icons 3.48.0 (https://tabler.io/icons), as
packed by the Zia mod (icons/tabler-bundle.js), to draw space icons picked with Zia.

```
MIT License

Copyright (c) 2020-2026 Paweł Kuna

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Zia mod: Phosphor-to-Tabler table (MIT)

`app/src/main/assets/zia/phosphor-to-tabler.json` is Zia's icons/phosphor-to-tabler.js
(https://github.com/z1n-k/zia), which maps the Phosphor icon names of Zia before 2.42.0 to Tabler.

```
MIT License

Copyright (c) 2026 z1nk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## GitHub mark (Octicons, MIT)

The GitHub icon in `ui/icons/KoanIcons.kt` (`GitHub`) is the path of `mark-github-16` from
Octicons (https://github.com/primer/octicons), Copyright (c) GitHub Inc., under the MIT License
(same text as in the Z-Sync section above). It is used only on the link to Koan's own release
page.

## Dependencies

Pulled in by Gradle, not copied into this repository:

- **GeckoView** (`org.mozilla.geckoview`) and **Mozilla Android Components**
  (`org.mozilla.components`): Mozilla Public License 2.0, https://www.mozilla.org/MPL/2.0/.
  Source: https://github.com/mozilla-firefox/firefox. GeckoView bundles further third-party code
  under its own licenses (listed in `about:license` of any Firefox build).
  Koan modifies four Java entry points and legacy FIDO Activity failure-consumer factories in
  `WebAuthnTokenManager` at build time for Credential Manager capability detection, Google-service
  availability, and task/cancellation error handling. The two UI-launching methods in
  `WebAuthnCredentialManager` receive an Activity-backed context and lifecycle-aware result
  executor. Original source:
  https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-omni/160.0.20261104181552/geckoview-omni-160.0.20261104181552-sources.jar.
  The complete modification and pinned build recipe are in `build-logic/`, with runtime checks
  in `app/src/main/java/dev/nerzouille/koan/engine/auth/PasskeyCapabilities.kt`; see
  `docs/PASSKEYS.md`. These modifications to the MPL-covered classes remain available under MPL 2.0.
- **AndroidX**, **Jetpack Compose**, **Room**, **DataStore**, **Kotlin** standard library and
  **kotlinx.coroutines**: Apache License 2.0, https://www.apache.org/licenses/LICENSE-2.0.
- **AndroidSVG** (`com.caverock:androidsvg-aar`): Apache License 2.0.
- **ZXing core** (`com.google.zxing:core`, QR codes for sign-in from a computer): Apache License
  2.0. Source: https://github.com/zxing/zxing.
- Test vectors only (in unit tests, not shipped): Mozilla's scoped-key test vectors from
  application-services and Firefox, Mozilla Public License 2.0.
- Tests only: **JUnit 4** (Eclipse Public License 1.0) and **org.json** (public domain).
- Build tooling only: **ASM** (`org.ow2.asm:asm`), BSD 3-Clause License,
  https://gitlab.ow2.org/asm/asm/-/blob/ASM_9_9_1/LICENSE.txt. Not packaged in the browser APK.

## Trademarks

Firefox and Mozilla are trademarks of the Mozilla Foundation in the U.S. and other countries.
GitHub and the GitHub logo are trademarks of GitHub, Inc.
Zen Browser is an independent project. Koan is not affiliated with, endorsed by or connected to
Mozilla or Zen Browser; these names are used only to describe compatibility.
