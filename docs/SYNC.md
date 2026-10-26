# Zen Spaces sync

Summary for orientation. The **authoritative spec is Z-Sync's**
`reference/Z-Sync/shared/contract/SPEC.md` + fixtures. When this file and the spec disagree, the
spec wins.

## How Zen syncs Spaces

- Zen ≥ 1.22 syncs Spaces through the user's **Mozilla account / Firefox Sync** (enabled on desktop
  in Settings → Sync → "Sync your Spaces across devices").
- Data lives in the Sync storage collection **`spaces`**. Each record decrypts to
  `{id, kind, data}` with `kind` ∈ `space | tab | folder | split | layout | container`.
  - `space`: `uuid`, `name`, `icon`, `theme { type, gradientColors, opacity, texture, lightness }`,
    `containerGuid`, `children` (sidebar order).
  - `tab`: `tabId`, `url` (required), `title`, `icon`, `essential`, `pinned`
    (**absent/null/true = pinned, false = normal tab**), `workspaceUuid`, `folderId`,
    `staticLabel`, …
  - `folder`: `folderId`, `name`, `icon`, `workspaceUuid`, `parentFolderId`, `children`.
  - `split`: `splitId`, `gridType`, `pinned`, `tabs` (member tab ids), `workspaceUuid`, `folderId`.
  - `layout`: `spaces` (display order), `essentials` (container bucket → tab ids).
  - `container`: ignore.
  - Tombstones: decrypted `{id, deleted: true}` (JSON boolean only).
- The **`prefs`** record carries `zen.spaces-sync.normal-tabs` (are normal/unpinned tabs synced?) and
  optionally `zen.workspaces.separate-essentials`.
- Records are encrypted with the standard Firefox Sync scheme: `kB` → oldsync key bundle →
  `crypto/keys` collection keys → AES-256-CBC + HMAC-SHA256 envelopes `{ciphertext, IV, hmac}`.
  Requests to Sync storage use **Hawk** auth with credentials from the token server.

Read the spec sections: §2 dropping rules (be strict, never guess), §3 per-kind schemas, §4 crypto,
§5 Hawk (use the *verbatim* percent-encoded resource!), §6 BSO id encoding, §7 API usage incl.
pagination, conditional writes (`X-If-Unmodified-Since`, 412 handling) and the
**normalTabsCapability** write-gating rule, §7.4 essentials grouping.

## Sign-in

Z-Sync signs in exactly like Firefox desktop does:

1. A WebView loads `https://accounts.firefox.com/?service=sync&context=fx_desktop_v3&action=email`.
2. The page talks to the "browser" through the FxA **WebChannel** protocol (`fxaccounts:fxa_status`,
   `fxaccounts:can_link_account`, `fxaccounts:login`, …). Z-Sync injects a JS bridge that answers
   these and captures `email, uid, sessionToken, keyFetchToken, unwrapBKey` on login.
3. `kB` is fetched with `keyFetchToken` + `unwrapBKey` (onepw protocol).
4. Token server credentials: OAuth `fxa-credentials` grant with the Firefox desktop client id and
   the `oldsync` scope (BrowserID assertion as fallback) → `token.services.mozilla.com` → Hawk
   `id/key` + `api_endpoint`.
5. TOTP (errno 103) must be handled by prompting for a code.

See Z-Sync `android/.../ui/screens/MozillaSignInScreen.kt` and `sync/FxAClient.kt`.

Caveat: this piggybacks on Firefox desktop's client identity; Mozilla could change it. Acceptable for
a personal app. If it breaks, check what Z-Sync did to fix it.

### Sign-in from a computer (QR pairing, Koan only)

Mozilla's pairing ("Connect another device"): the desktop shows
`https://accounts.firefox.com/pair#channel_id=…&channel_key=…` as a QR code. Koan (`sync/Pairing.kt`,
`ui/signin/PairingWeb.kt`) plays the phone side as **Firefox for Android** (client id
`a2270f727f45f648`, Fenix UA; Mozilla only pairs its own apps):

1. Opens `/pair/supp?entrypoint=pairing&service=sync&client_id=…&state=…&code_challenge_method=S256&code_challenge=…&access_type=offline&keys_jwk=…&scope=profile+oldsync&context=oauth_webchannel_v1#<channel>`.
   **No `redirect_uri`**: FxA rejects the WebChannel redirect as a parameter (`IsFxaRedirectUri`);
   `context` replaces it, as in application-services' `oauth_flow`.
2. Answers the page's WebChannel: `fxaccounts:fxa_status` (pairing capability), and
   `fxaccounts:pair_oauth_start` (pairing v2) with the same OAuth request; the page then sends
   `fxaccounts:oauth_login {code, state}` once the desktop approved.
3. `POST /v1/oauth/token` (authorization_code + PKCE verifier) → `access_token`, `refresh_token`,
   `keys_jwe`. `keys_jwe` (ECDH-ES + A256GCM to our ephemeral P-256 key) holds the **oldsync scoped
   key**: 64 bytes = exactly `HKDF(kB, "identity.mozilla.com/picl/v1/oldsync", 64)`, and its
   `kid` = `keysChangedAt-b64url(sha256(kB)[0:16])` (`sync/ScopedKeys.kt`, tested on Mozilla's
   vectors). The profile endpoint gives email and uid.
4. Stored: email, uid, refresh token, scoped key, kid (sealed). Sync: refresh token →
   `grant_type=refresh_token` (oldsync) → Bearer to the token server with `X-KeyID: kid` → Hawk
   as before. Sign-out: `POST /v1/oauth/destroy`.

Verified live on 2026-10-26 (code exchange, scope `profile oldsync`, keys_jwe present, tokens
revoked). The probe used for it is kept out of the repo in `reference/pairing-probe/`.

## Reuse plan

Port (copy + adapt, keep MIT header) these Z-Sync Android files into `sync/`:

| Z-Sync file | Purpose |
|---|---|
| `sync/FxACrypto.kt` | HKDF, unbundle, token material |
| `sync/SyncCrypto.kt` | BSO envelope encrypt/decrypt |
| `sync/HawkAuth.kt` | Hawk headers |
| `sync/SyncTransport.kt` | HTTP transport abstraction |
| `sync/FxAClient.kt` | account login completion, kB, token server |
| `sync/SyncClient.kt` | storage GET/PUT, crypto/keys, pagination |
| `sync/ZenSpaces.kt` | wire decoding of spaces/tabs/folders/splits/layout/prefs |
| `sync/SpacesSyncService.kt` | orchestration, write paths (study carefully before M6) |
| `sync/SyncSafety.kt` | safe-sync switch |
| `ui/screens/MozillaSignInScreen.kt` | WebView sign-in + WebChannel bridge (re-skin it) |
| `data/AccountStore.kt` | encrypted credential storage |

Don't port Z-Sync's UI, DemoCatalog, Play review, snapshot cache (we use Room).

Then: copy the JSON fixtures into `app/src/test/resources/contract/` (or read them from
`reference/`) and port Z-Sync's contract tests so `./gradlew test` proves the port is faithful.

## Mapping into Koan

- Spaces ordered by `layout.spaces`, falling back to record order.
- Within a space: essentials (from `layout.essentials`, grouped per §7.4), then pinned tabs/folders
  in `space.children` order, then normal tabs (only if `normal-tabs` pref allows).
- Splits: one card, `tabs` member ids; member tabs never appear as separate cards.
- Folders: grouped cards, nested folders flattened one level for v1 (indent the label).

## Write-back (M6 only, behind `writeBackEnabled`)

The full design (passwords, history, bookmarks, then Spaces; safety rules; test plan) is in
[`SYNC_WRITE.md`](SYNC_WRITE.md). Summary of the Spaces part:

Allowed operations, in this order of implementation:
1. Add a pinned tab to a space (or a folder in that space, per §3.5 target-folder rules).
2. Add a normal tab — only if `normalTabsCapability == enabled`; otherwise write it pinned.
3. Close/remove a tab → tombstone.
4. Move a tab between spaces / reorder → update tab record + parent `children`.

Every write: conditional `X-If-Unmodified-Since`, on 412 re-read, merge, retry once, otherwise
surface a conflict. Keep a local **undo journal** of the last 50 writes (previous cleartext) so a bad
write can be reverted from Settings. Never write `layout` or `space` records in v1.
