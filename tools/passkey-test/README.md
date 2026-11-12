# Disposable-emulator credential tests

These tests contain only synthetic accounts for `http://localhost:8786`. Never install the
fixture credential provider on a daily phone or select it for a real vault. It is a P-256
transport/signature fixture, not a secure authenticator: its private key lives in fixture
preferences and it reports UV without performing biometric verification.

Use a separate Android 14+ ARM64 emulator named **Koan_Passkey_Test**, with no accounts. Set a
synthetic device PIN on that AVD so Android reports a secure lock; keep the screen unlocked.
The scripts require the exact AVD name and `ro.kernel.qemu=1` before changing anything. They
never boot, wipe, or select a physical phone.

With JDK 21, the Android SDK, Python 3 and Node installed:

```sh
./gradlew :build-logic:test :app:testDebugUnitTest
# Assembly is a separate invocation from unit tests.
./gradlew :app:assembleDebug :credential-fixture:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5564 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5564 install -r credential-fixture/build/outputs/apk/debug/credential-fixture-debug.apk
adb -s emulator-5564 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5564 shell am instrument -w -r \
  -e class dev.nerzouille.koan.auth.testing.PasskeyIntegrationTest \
  dev.nerzouille.koan.debug.test/androidx.test.runner.AndroidJUnitRunner
# Do not run this concurrently with instrumentation or another UI driver.
python3 tools/passkey-test/run.py --serial emulator-5564
```

The runner temporarily selects the fixture services and default browser, disables Google
Play Services, and sets up the localhost reverse tunnel. A `finally` block restores those
settings and stops the local server. Use a disposable AVD regardless; interrupted processes
or a disconnected emulator can prevent cleanup. Stop only this AVD when done.
The runner force-stops the selected test browser once before the run. All requests, retries
and entry modes then share that process. Fixture PendingIntents have unique identities so
overlapping candidate queries cannot invalidate a displayed entry; the cancellation driver
waits for the specifically named cancellation entry before pressing Back.

The web test checks real browser-returned client data, RP hash, origin, challenge, credential
ID and a real P-256 signature on the local server. It covers platform capability detection
without GMS, registration, authentication, prompt cancellation followed by retry, and a
no-options request that must reject promptly. It also checks pre-aborted calls and an invalid RP
ID, discoverable login without an allowlist, provider exceptions/malformed responses and a
successful retry. It repeats through a bound external Custom Tabs
client. Normal links resolve through the system browser role. Finally, a separate fixture
AutofillService fills an HTML username/password form and the server checks both values.
For coexistence with Koan's built-in login store, add a synthetic login for the fixture origin
in Settings > Passwords before running the Autofill check.
The website requires `NotAllowedError` for user dismissal and `AbortError` for a pre-aborted
signal; accepting either error would hide a browser classification regression.

`credential-fixture` is a separate app, with no dependency from `:app`. Neither its exported
services nor its synthetic key code is present in a browser APK. The only browser debug
manifest override allows localhost HTTP for instrumentation's test client.

For the minified user build, assemble/install `:app:assembleStaging` separately (optionally
`-Pkoan.testVersionSuffix=-passkeys.test` to identify it in About) and run with
`--browser dev.nerzouille.koan.staging`. The browser and fixture must use the same debug signing
key: the fixture validates the caller's package/certificate and accepts only the localhost
origin. This deliberately tests origin delegation instead of bypassing it.

These tests do not establish compatibility with 1Password's trusted-browser policy, real
biometrics, physical USB/NFC keys or cross-device QR authentication.
The Android integration suite also checks Activity-backed credential contexts, outcome delivery
after resume or destruction, and draining queued/late results after callback-owner loss. The
latter simulates loss of the callback owner; it is not an end-to-end process-kill test.
