# WebAuthn and system autofill

Koan delegates passkey creation and authentication to GeckoView. Android 14+ uses Android
Credential Manager and its selected provider; the earlier Google FIDO route remains available
on supported devices. Koan does not store passkeys, handle vault keys, or implement an
authenticator. Website origins, RP IDs, challenges and client-data hashes stay in the native
Gecko/Android/provider path.

## Browser integration

- The manifest declares `CREDENTIAL_MANAGER_SET_ORIGIN` and
  `CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS`. A provider must still trust the browser's
  package and signing certificate before accepting website origins.
- `WebAuthnController` registers an AndroidX IntentSender result launcher before Activity start.
  A ViewModel keeps the engine callback through Activity recreation. Success, cancellation,
  launch failure and Activity finish settle it once. A second overlapping launch is rejected
  without replacing the first callback. Unrelated Activity results use their own launchers.
- The selected normal/private engine view allows Android Autofill in addition to Koan's login
  delegate. No saved-password or sync behavior is replaced.
- Normal web links and external Custom Tabs clients reach the same MainActivity/WebAuthn bridge.

## GeckoView 157 compatibility adapter

The pinned GeckoView `157.0.20261005135250` can authenticate through Credential Manager but its
`isUserVerifyingPlatformAuthenticatorAvailable()` probe still calls Google FIDO. That can return
false on a working Credential Manager device. Its legacy FIDO fallback can also wait on an
absent/disabled Google service.

The `koan.passkeys` included Gradle plugin uses AGP's ASM instrumentation API to modify only
four static methods in `org.mozilla.geckoview.WebAuthnTokenManager`:

| Method | Added behavior |
|---|---|
| `webAuthnIsUserVerifyingPlatformAuthenticatorAvailable` | Return true when Android 14+, the system credential feature/service and a secure device lock are available; otherwise retain Google FIDO detection, or false if Google services are absent/disabled. |
| `hasCredentialInGMS` | Return false immediately if Google services are absent/disabled. |
| `makeCredential`, `getAssertion` (legacy FIDO methods only) | Reject with `NOT_SUPPORTED_ERR` if Google services are absent/disabled. |

The native Credential Manager create/get methods and existing origin/security checks are
unchanged. This is not a JavaScript shim, a user-agent change, or a bypass of provider trust.
Platform capability does not guarantee a particular provider has a credential or trusts Koan.

The plugin checks the exact engine version, class, method descriptors and static access at
build time. An engine upgrade must review/remove the adapter, rather than silently applying
it to a different implementation. The adapter's unit tests execute transformed bytecode and
check the platform route, legacy route, missing-service errors and signature mismatch guard.
Original source and MPL notice: [third-party notices](../THIRD_PARTY_NOTICES.md).

## Limits

- Gecko Android currently returns false for conditional WebAuthn mediation. Passkey suggestions
  inside username fields are therefore unavailable; explicit passkey buttons are separate.
  Advertising that feature without implementing it would leave websites waiting for a request
  the engine cannot service. This requires an upstream native-engine implementation.
- Some providers, including 1Password, maintain their own trusted-browser policy. Koan cannot
  grant itself trust by declaring a permission. Do not spoof another browser's package or origin.
- Device/provider support for security keys, cross-device QR flows and biometric verification
  varies. The synthetic tests below do not establish interoperability with physical hardware
  or a real provider's browser allowlist.
- Process death cannot preserve a live in-memory WebAuthn request. A restored page must initiate
  a new request. Activity recreation within the running process is supported.

## Verification

`./gradlew :build-logic:test :app:testDebugUnitTest` runs the adapter and callback unit tests
(run separately from APK assembly). The three Android instrumentation tests cover a retained
callback after Activity recreation, actual native success/cancellation delivery, and an unrelated
AndroidX result. See [the emulator fixture](../tools/passkey-test/README.md) for complete website
creation/signature checks, cancellation, unavailable-provider handling, an external Custom Tabs
client, and Android username/password Autofill. The provider is a separate APK/module, never a
dependency of the browser; it uses only synthetic localhost credentials.
