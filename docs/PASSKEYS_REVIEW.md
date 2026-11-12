# Passkey experience review

Reviewed 2026-11-12. The goal is a quiet, reliable native sign-in flow inside Koan's existing
interface. This compares source and contracts; it is not a claim of feature parity with Chrome,
Firefox desktop or every credential provider.

## Reference implementations

- Chromium, pinned at [`ccae90050393f05866c2e9eff21c33a1ad1a9aa7`](https://github.com/chromium/chromium/tree/ccae90050393f05866c2e9eff21c33a1ad1a9aa7/components/webauthn/android).
  `AuthenticationContextProvider` and `AuthenticatorImpl` associate the operation with its
  context, frame and WebContents; the callback is cleared on completion/connection closure.
  `cred_man/CredManHelper` distinguishes enumeration, selection and cancellation states.
  The Android README describes routing through the selected OS provider in third-party
  password-manager mode, rather than requiring a duplicate browser account picker.
- GeckoView `160.0.20261104181552`, the engine used here. Native request construction, origin/RP
  checks, response parsing and signing are preserved. Its Android integration still has gaps
  that an Activity-only change cannot fill; see [implementation and limits](PASSKEYS.md).
- [WebAuthn Level 3, request exceptions](https://www.w3.org/TR/2026/REC-webauthn-3-20260825/#sctn-getAssertion-exceptions):
  user dismissal is `NotAllowedError`; an `AbortController` cancellation is `AbortError`.
- [Android Credential Manager](https://developer.android.com/reference/android/credentials/CredentialManager)
  requires an Activity context for the proper task stack. Its cancellation API is separate from
  successful/error outcome delivery.

## Interaction contract

| Situation | Expected experience | Koan work / boundary |
|---|---|---|
| Explicit sign-in or create | One provider-owned selection/verification flow for the requesting website | Keep native origin/RP/challenge handling; no extra Koan picker |
| User dismisses the provider | Return control to the site without an automatic retry or browser error toast | `NotAllowedError`, including the native Credential Manager outcome and legacy Activity failure consumer |
| Website pre-aborts | No provider prompt; reject as website cancellation | Native DOM `AbortError`; separate from user dismissal |
| Error then retry | Old results cannot complete the next request; next prompt waits for the closing UI | Per-request registry IDs, native completion guards, resume-aware outcome executor |
| Activity recreation | Follow the same logical browser host | ViewModel identity survives recreation; no Activity/credential stored in it |
| Host destroyed, a new host opens | Settle the old native result without waiting for the new host's lifecycle | Captured host identity; never follow an unrelated Activity |
| No matching credential / provider failure | Give the website its native error; keep its password/other-device alternative usable | No silent account creation, no forced provider switch; synthetic tests cover completion and retry |
| Username field suggestions | Suggestions must come from a functioning conditional request | Gecko Android conditional mediation remains unimplemented; never advertise it as available |
| Abort after platform UI opens | Invalidate the request and reject/ignore late outcomes; UI dismissal must be evaluated separately | Native engine work remains; do not simulate success by closing Koan or sending global Back |
| Another device / security key | Offer native provider-supported choices without claiming universal hardware support | Real USB/NFC, QR and biometric interoperability still requires device testing |

The host identity only scopes **outcome delivery timing**. The engine still owns the request's
document and origin. It does not add Chromium's frame-aware cancellation to Gecko, change the
destination of a credential, or prove that navigation/tab-switch cases are fully implemented.

## Cancellation is not one feature flag

Chromium's reviewed `AuthenticatorImpl.cancel()` explicitly limits cancellation after GMS/Core
or Credential Manager UI has opened. `CredManHelper.cancelGetAssertion()` handles pending
enumeration/selection states; a platform UI already in progress is a distinct case. Therefore,
implementing Gecko's native `Cancel` and passing a cancellation signal is necessary work, but is
not sufficient evidence that every Android/provider sheet can be forcibly dismissed.

The next engine change must carry transaction and browsing-context identity through JNI, own
the matching cancellation handle, clear callbacks exactly once, and ignore late outcomes.
Test preparation cancellation, UI-open cancellation, navigation and a new request separately.
Do not guess request identity from the selected tab or use a timer to close arbitrary UI.

## Install and upgrade identity

The unofficial preview uses `dev.nerzouille.koan.staging` (**Koan Test**). The upstream release
uses `dev.nerzouille.koan` (**Koan**). They coexist with separate app data; neither is an in-place
update of the other. They also have different signing certificates.

An update within either channel must retain its application ID and compatible signing identity,
and use an appropriate non-decreasing versionCode. Keep the preview's existing signing key;
generating another local debug key breaks updates even when the APK has the same name.
Changing a suffix, release tag or file name cannot bridge this boundary. See
[Android app signing](https://developer.android.com/studio/publish/app-signing#considerations).

Installing upstream alongside a preview does not transfer browser sessions, local tabs or local
passwords. Provider-held passkeys stay with that provider, but trusting the official package
does not automatically mean trusting a preview package/certificate. Do not uninstall the
preview as an upgrade step before arranging any required data migration.

The current About page checks upstream releases. It is not a preview-channel updater and must
not be treated as evidence that an upstream APK can replace Koan Test. This review does not
change the updater or introduce a new credential/settings screen.
