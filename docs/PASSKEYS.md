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
  Each launch has a unique registry key and a ViewModel keeps its callback through Activity
  recreation. Stale results cannot complete a retry, including after a new process starts.
  Registry callbacks are detached on Activity destruction. Success, cancellation,
  launch failure and Activity finish settle it once. A second overlapping launch is rejected
  without replacing the first callback. Stopped hosts reject new launches, while a provider's
  existing result survives the normal host stop. Unrelated results use their own launchers.
- The bridge observes the host lifecycle itself. Saved state records only opaque routing IDs;
  after callback loss/process death, their registrations drain and discard late results rather
  than letting AndroidX keep old credential Intents. A destroyed owner cannot restart the bridge.
- The selected normal/private engine view allows Android Autofill in addition to Koan's login
  delegate. No saved-password or sync behavior is replaced.
- Credential Manager UI receives an Activity-backed context. Its result executor waits for the
  browser to resume before forwarding the native callback, so a sequential website request
  cannot overtake the selector that is still closing. Host destruction still settles the
  callback. The host is referenced weakly; a retiring Activity cannot clear its replacement.
- Normal web links and external Custom Tabs clients reach the same MainActivity/WebAuthn bridge.

## Interaction and ownership

The credential provider owns its sheet, account selection and user verification. Koan adds no
second account picker, credential settings screen, automatic retry or cancellation toast.
The website receives the native result/error and controls its own sign-in UI. Android's selected
password Autofill service remains available alongside Koan's existing logins.

An Activity request owns an opaque ID and one callback. Only its matching result can finish it;
closing the host rejects the callback and permanently closes that ViewModel to new requests,
including reentrant callbacks. A stopped/destroyed host cannot launch another provider sheet.
Opening the provider normally stops Koan, so that stop deliberately retains the in-flight result.
The engine keeps the WebAuthn request attached to its originating document: Koan never sends a
credential to whichever tab happens to be selected when a result arrives.

Android permissions, the Activity bridge and Autofill are host-app responsibilities. Engine
compatibility changes are confined to `build-logic` and the small `engine/auth` helpers; tab,
workspace, gesture, password-storage and sync code do not depend on this adapter.

## GeckoView 160 compatibility adapter

The pinned GeckoView `160.0.20261104181552` can authenticate through Credential Manager but its
`isUserVerifyingPlatformAuthenticatorAvailable()` probe still calls Google FIDO. That can return
false on a working Credential Manager device. Its legacy FIDO fallback can also wait on an
absent/disabled Google service.

The `koan.passkeys` included Gradle plugin uses AGP's ASM instrumentation API to modify only
four static entry points and the legacy FIDO task callback edges in
`org.mozilla.geckoview.WebAuthnTokenManager`:

| Method | Added behavior |
|---|---|
| `webAuthnIsUserVerifyingPlatformAuthenticatorAvailable` | Return true when Android 14+, the system credential feature/service and a secure device lock are available; otherwise retain Google FIDO detection, or false if Google services are absent/disabled. |
| `hasCredentialInGMS` | Return false immediately if Google services are absent/disabled. |
| `makeCredential`, `getAssertion` (legacy FIDO methods only) | Reject with `NOT_SUPPORTED_ERR` if Google services are absent/disabled; settle canceled tasks, and attach the missing failure listener to the login task. |
| Legacy task success handlers' Activity failure-consumer factories | Create a shared abort consumer for canceled/failed Activity launches; successful native response parsing is unchanged. |

The native Credential Manager request construction, provider calls, parsing and security checks
are kept. A separate visitor replaces the application-context lookup with `CredentialUiHost.context`
only for the two methods that launch UI; background preparation stays unchanged. The Activity-backed
wrapper uses the host lifecycle for outcome delivery. This follows Android's requirement to use an
[Activity context for the correct task stack](https://developer.android.com/reference/android/credentials/CredentialManager).
There is no JavaScript shim, user-agent change or bypass of provider trust.
Platform capability does not guarantee a particular provider has a credential or trusts Koan.

The context wrapper changes the executor supplied for native response/error callbacks. Android's
[Credential Manager transport](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/core/java/android/credentials/CredentialManager.java)
launches PendingIntents directly through the context; those launches are not put in a host queue.
No fixed animation delay is used. Results are dispatched after resume observers finish, or on
destruction so the old request can still settle; routing IDs and origins remain unchanged.

The callback adapter first follows the task's actual method handles, then adapts the matching
failure-consumer factories. It uses no fixed generated method name/number, leaves the original
failure-handler bodies intact and does not depend on class-file method ordering. Signature, ownership and
call-site count checks reject a changed/ambiguous graph instead of patching unrelated callbacks.

The plugin checks the exact engine version, class, method descriptors and static access at
build time, including the task hook and callback contracts. An engine upgrade must
review/remove the adapter, rather than silently applying it to a different implementation.
The adapter's unit tests execute transformed bytecode and
check the platform route, legacy route, missing-service errors and signature mismatch guard.
Original source and MPL notice: [third-party notices](../THIRD_PARTY_NOTICES.md).

GeckoView 160's published `WebAuthnTokenManager.java` and `WebAuthnCredentialManager.java` were
compared with the former 157 sources on 2026-11-11 and are identical. This branch follows
upstream's matched GeckoView 160 / Android Components 160 upgrade, with the compatibility
guard re-pinned to the exact reviewed engine version. Upgrading alone does not fix these paths.

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
- This engine's native `AndroidWebAuthnService::Cancel` is not implemented. A website's abort
  or timeout cannot reliably dismiss an already open system credential sheet. Pre-aborted
  requests are tested, but closing an in-flight sheet from the website requires native engine
  cancellation support. The browser does not pretend that a JavaScript promise rejection
  proves the Android UI has closed.

## Verification

`./gradlew :build-logic:test :app:testDebugUnitTest` runs the adapter and callback unit tests
(run separately from APK assembly). Android instrumentation covers actual registry delivery
after Activity recreation, stale results after retry, launch failure/stopped hosts, legacy task
failure/cancellation, the shared Activity error mapping, destroyed-host protection, native success/cancel
delivery, Activity-context lifetime/resume delivery, and an unrelated AndroidX result.
See [the emulator fixture](../tools/passkey-test/README.md) for website
creation/signature checks, discoverable login, invalid RP/pre-abort rejection, cancellation,
unavailable-provider/malformed-response handling, an external Custom Tabs
client, and Android username/password Autofill. The provider is a separate APK/module, never a
dependency of the browser; it uses only synthetic localhost credentials.

## Remaining engine work

In-flight cancellation needs a native implementation, not another host UI workaround. The
native WebAuthn service must carry the transaction and browsing-context identity through JNI,
own the Android cancellation signal for that request, and route native cancellation/timeout to
that signal. Completion and cancellation must atomically remove the matching request; late
provider callbacks must be ignored. Canceling an old document must never cancel a new document's
request. This requires native engine tests and a rebuilt engine, separate from this host PR.

Conditional suggestions likewise need the engine's pending-request, candidate selection and
resume operations, which Android Gecko currently leaves unimplemented. They must remain
unadvertised until that path works. Provider trust belongs to the browser's stable official
package/certificate and the provider's policy; a fork's test certificate does not confer it.
