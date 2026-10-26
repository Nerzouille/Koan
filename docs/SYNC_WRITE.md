# Two-way sync (M6): study

Status: **study only** (2026-10-27). Nothing in Koan writes to a Mozilla account today. This file
decides how it will, in the owner's order: **(1) passwords → (2) history and bookmarks → (3) Zen
Spaces**. Sources are listed at the end; claims marked *[unverified]* were not confirmed in source.

## 0. Ground rules for every write

These hold for all three steps; each step adds its own below.

1. **Opt-in, off by default, per collection.** Settings › Sync › "Send … to your Mozilla account",
   one switch per collection (passwords, history, bookmarks, Spaces), all off. The Spaces switch is
   the planned `writeBackEnabled` (CLAUDE.md rule 4).
2. **Dry run first.** Each collection has a dry-run mode (on by default when its switch is first
   turned on): the whole upload is built, validated and *logged as a summary* (record count, which
   fields, which ids are new/changed/deleted, never a password, URL list or title), and nothing is
   sent. Real writes need the dry run switched off explicitly.
3. **Never write `meta/global` or `crypto/keys`.** A missing/lower storage version or syncID makes
   desktop **wipe the server**; a lower engine version makes clients wipe that collection.
   (`SyncClient` still has Z-Sync's `crypto/keys` bootstrap for an *empty* node: it must be disabled
   for any account that already has data, i.e. always for the owner.)
4. **Check before writing.** Read `meta/global`: `storageVersion == 5`; the collection is not in
   `declined`; its `engines[coll].version` is what we expect (passwords 1, history 1, bookmarks 2)
   and its `syncID` is the one Koan saw on its last read. Anything else: drop the local mirror,
   re-read, and only then plan the write. Never write over an unseen reset.
5. **Snapshot before any write.** Before the first write of a session, the collection's current
   encrypted BSOs (as downloaded, still encrypted) are saved to app-private storage (one file per
   collection, the last 5 kept). Restore = re-upload those exact payloads (§ Undo).
6. **Conditional, atomic writes.** `POST /storage/<coll>?batch=true … &commit=true` with
   `X-If-Unmodified-Since: <collection last-modified>` (from the read we planned from). On **412**:
   nothing was written; re-read `newer=<last>`, re-merge, re-plan, retry at the next sync (as desktop
   does), never unconditionally. Respect `info/configuration` limits (production defaults are around
   100 records / 2.5 MB per POST), 409/503 + `Retry-After`, and `X-Weave-Backoff`.
7. **Encrypt with the collection's key** from `crypto/keys` (`collections[coll]`, else `default`),
   as reads already do.
8. **Keep what we don't understand.** When re-uploading a record that was downloaded, start from the
   full downloaded cleartext and change only our fields: both Firefox engines round-trip unknown
   fields and expect clients to keep them.
9. **Undo journal.** Every real write appends (collection, id, previous cleartext or "didn't exist",
   new cleartext, time) to a local journal (sealed, last 200 entries). Settings › Sync › "Undo last
   sync writes" re-uploads the previous versions (or tombstones for records we created) through the
   same conditional path.
10. **Secrets.** Logs and the journal's UI never show passwords, usernames, URLs or titles; counts
    and field names only (rule 5 of CLAUDE.md).
11. **No `clients` record needed.** Writing records doesn't require registering in the `clients`
    collection. Not registering only means Koan won't receive other devices' commands (wipe/reset),
    which is one more reason for rule 4. Registering is a later, separate decision.

### Which credential writes: QR token or password session?

Both end in the same thing: an OAuth access token with the `oldsync` scope, exchanged at the token
server for one Hawk id/key per user. **There is no separate write scope**, and nothing suggests the
token server or storage treat them differently *[unverified: whether the token server checks
`client_id`]*. So writes don't *need* the QR sign-in.

Recommendation: **prefer the QR token for writes, but don't require it.**
- A refresh token is revocable on its own from the account's "Connected services" (shown as
  "Firefox for Android"), without changing the password: the owner's kill switch if a write goes
  wrong.
- The password session is the Firefox **desktop** identity; writes made with it look like the
  desktop wrote them, which makes any later debugging on Mozilla's side confusing.
- Neither identity is officially allowed for a third-party app; writing doesn't change that risk
  class, it raises the stakes (a bad write is worse than a bad read).

---

## 1. Passwords (phone → Mozilla)

**Scope:** the logins saved on the phone ("Save in Koan", `LoginEntity.local == true`). Koan
doesn't edit or delete the account's logins, so step 1 is upload-only of local ones.

### Record (cleartext of a `passwords` BSO)

| field | rule |
|---|---|
| `id` | BSO id: 1–64 printable ASCII, no comma. Koan's local ids are UUIDs: valid as is. |
| `hostname` | origin only: `scheme://host[:port]`, no path, no trailing `/`. |
| `formSubmitURL` | form action origin; **`""` = any**. Required for a form login. |
| `httpRealm` | only for HTTP-auth logins. **Exactly one** of `formSubmitURL`/`httpRealm`: desktop *skips* a record with both, Fenix *rejects* one with neither. Omit the other key (don't send `""`). |
| `username` | string, may be `""`. |
| `password` | string, **non-empty**. |
| `usernameField`, `passwordField` | strings; `""` for HTTP-auth logins; `usernameField != "."`. |
| `timeCreated`, `timePasswordChanged`, `timeLastUsed` | integer **milliseconds**; `timesUsed` a count. |
| no NUL / newline | in origins, realm and field names; no NUL in username/password. |

Never upload a login for the Mozilla accounts origin (both engines exclude it).

### How the others merge it

- **Zen/Firefox desktop (release)** still uses the legacy JS passwords engine
  (`signon.storage.rust.enabled` is false on release; Zen *[unverified, inferred]*):
  whole-record last-writer-wins by age. An unknown id is first matched against local logins by
  origin + form action + realm + username (password ignored): if one matches, **the local login
  takes the incoming id**.
- **Fenix (Rust)**: per-field three-way merge when it has a mirror; otherwise `timePasswordChanged`
  wins; dupes found by origin + realm + form action host + username.
- So: a phone login for a site and username the desktop already has merges into that login (the
  newer `timePasswordChanged` wins), which is the right outcome. A new one simply appears.

### Risks

- A login with neither `formSubmitURL` nor `httpRealm` (Koan's `formActionOrigin` can be null):
  rejected by Fenix. **Fix at upload: null → `formSubmitURL: ""`.**
- Wrong timestamp units (s or µs): clamped/zeroed, and the wrong side wins conflicts.
- After the upload, the next read brings the same id back as an account login: the local row must
  become the synced one (same guid, `local = false`) or Settings shows it twice.
- Desktop can't be hurt structurally by a password record; the worst case is a wrong password
  winning over the desktop's. Snapshot + undo journal cover it.

### Plan (branch `sync-back`, step 1, dry-run only for now)

- `sync/LoginUpload.kt`: pure function `plan(localLogins, serverIds) → UploadPlan` (records,
  per-record validation result, summary) + `toRecord()` (the table above).
- Setting "Send passwords saved on this phone to your Mozilla account" (off) + "Dry run" (on).
- `LibrarySync` after a pull: if the switch is on, build the plan and log the summary
  (`KoanSyncBack: passwords dry run: 3 to upload (new 2, changed 1), fields […], 0 invalid`).
  No network call.

---

## 2. History and bookmarks

### History record

| field | rule |
|---|---|
| `id` | **exactly 12 base64url characters** (`^[-a-z0-9_]{12}$` i): anything else is dropped. Reuse the server id for a URL Koan read from the account. |
| `histUri` | the URL (≤ 65 536 chars, a scheme Places stores). |
| `title` | string. |
| `visits` | `[{date, type}]`: `date` integer **MICROseconds**, `type` transition 1–9 (1 = link, 2 = typed). **≤ 20, newest first.** |
| BSO `ttl` | 5 184 000 s (60 days), as Firefox sends. `sortindex`: frecency (optional). |

**Merge:** visits are a union, de-duplicated on (date, type), never removed; a record with no new
visit is a no-op (a title alone never changes). Older than the client's 20th visit: dropped.

**Upload recipe:** start from the server record Koan already read for that URL (all its fields),
union in the phone's visits, keep the newest 20, keep the id. Uploading only the phone's visits would
replace the server copy and hide the desktop's visits from any *new* device.

**Risks:** dates in ms instead of µs land in 1970: Fenix drops them, desktop clamps them to 1993 and
keeps them forever (permanent junk). A history **tombstone deletes the page and all its visits on
every device**: Koan must never send one in step 2 (clearing history on the phone stays local).
Koan reads only the 5000 most recent records: a URL older than that gets a new id from Koan, and
desktop's behaviour for "known URL, new id" is *[unverified]* (Fenix re-keys to the incoming id).

### Bookmark records (engine version 2)

| type | fields |
|---|---|
| all | `id` (12 base64url), `type`, `parentid`, `parentName` (legacy), `dateAdded` (ms), `hasDupe: true` (desktop always sets it; do the same) |
| `bookmark` | `title`, `bmkUri` (valid URL, mandatory), `keyword`, `tags` |
| `folder` | `title`, **`children`** (ordered ids: the source of truth for structure and order) |
| `separator` | `pos` |

Roots in payloads use short ids: `menu`, `toolbar`, `unfiled`, `mobile` (never the local
`mobile______` form). **Never upload `places`** (desktop deletes it from the server).

**Merge:** desktop and Fenix use *dogear*, a two-way tree merge of the whole local and remote trees.
Values: the newer side wins (by server `modified`). Structure: if both sides moved an item, newer
wins. Inconsistent trees are repaired: an item in no `children` list goes to the end of its
`parentid` folder; an item with a missing/invalid parent goes to **unfiled**; a bookmark without a
valid URL is replaced or deleted. New items are also de-duplicated by content (title + URL in the
same parent).

**Upload recipe (add):** one atomic batch with the new bookmark (`parentid: "mobile"`) **and** the
`mobile` folder record with its complete, updated `children` (other fields kept). Move: the item +
old parent + new parent. Delete: tombstone + parent without the child (+ tombstones for a folder's
descendants).

**Risks:** a child without its parent's updated `children` (order lost, appended), a wrong
`parentid` (moved to unfiled on every device), a partial upload outside a batch (torn tree for
whoever syncs in between), dropped titles/unknown fields on the parent folder.

**Koan's scope:** bookmarks added on the phone ("Bookmark" in the menu) go to `mobile` ("Mobile
Bookmarks" on desktop). Koan doesn't edit or move the account's bookmarks in step 2. Koan's local
bookmark ids aren't 12-char base64url today: new ids are generated at upload and stored.

### Plan (branch `sync-back`, step 2, dry-run only)

`sync/HistoryUpload.kt` and `sync/BookmarkUpload.kt`, pure planners like passwords: history = the
phone's own visits since the last upload, merged into the server records (or new records with fresh
12-char ids); bookmarks = new phone bookmarks + the `mobile` folder record. Same switches, same
dry-run summary, no network call.

---

## 3. Zen Spaces (`spaces` collection)

### What Z-Sync's spec says about writing

- Write = `PUT /storage/spaces/<id>` with the encrypted cleartext `{id, kind, data}`; deletes are
  **cleartext tombstones** `{id, deleted: true}` (a WBO-level `deleted` is ignored by Zen, which
  then re-uploads the item).
- Conditional writes (`X-If-Unmodified-Since`), 412 → re-read, merge, retry; atomic multi-record
  POST conditions the whole batch on the collection timestamp (§7.2).
- **normalTabsCapability:** never write a `pinned: false` tab unless the `prefs` record says
  `zen.spaces-sync.normal-tabs` is enabled; otherwise write it pinned (§7).
- Folder targeting rules (§3.5): a tab lands in a folder only if that folder record exists, has the
  exact `folderId`, and belongs to the same space; else the space root.
- Z-Sync's own writer (`SpacesSyncService.tombstoneBatch`) shows what a removal really touches: the
  tab tombstone, plus every `space`/`folder` whose `children` lists it, every `split` containing it
  (a split left with < 2 tabs is tombstoned and its members spliced into the parent), and the
  `layout.essentials` buckets. One logical change = one batch of records.

### Is Zen's format stable enough?

- The contract is versioned (Contract-Version 1) and has changed **additively** only (normal tabs,
  essentials grouping, conditional writes, 2026-09 → 2026-10). Zen's spaces sync itself is young
  (Zen ≥ 1.22) and owned by Zen, not Mozilla: fields can appear without notice (`container` kind is
  already "recognized, never applied").
- Reading tolerates that (unknown kinds dropped). **Writing does not**: a record re-uploaded without
  a field Zen added later would erase it on the desktop. Rule 8 (start from the full cleartext) is
  mandatory here, and Koan should refuse to write a record whose kind it doesn't know.
- Verdict: stable enough for **tab-level writes** (add/close/pin/move a tab, keeping everything
  else); **not** for writing `space` or `layout` records yet (themes, ordering, essentials are the
  part most likely to change, and the one that scrambles the desktop if wrong). DOCS/SYNC.md already
  says "never write layout or space records in v1"; keep it.

### What could break the desktop Spaces

- A tab removed without updating its parent's `children` (ghost entry, or Zen re-adds it).
- A wrong `workspaceUuid`/`folderId` (tab in the wrong space/folder on the desktop).
- `pinned: false` written while normal-tab sync is off (tab invisible everywhere).
- A partial batch (desktop syncs between two PUTs and sees half a move).
- Racing the desktop (both edit the same space's `children`): 412 handling is what prevents a lost
  update.
- Koan's Room model is a *projection* (splits as one card, nested folders flattened): writes must be
  planned against the **server records**, never re-serialized from Room.

### Plan (step 3, later)

Operations, in order (as in SYNC.md): add a pinned tab → add a normal tab (capability-gated) →
close a tab (tombstone batch à la Z-Sync) → move/reorder. Each = a planner over the freshly read
records producing one batch; same dry run, snapshot, conditional batch, journal. Never `space`,
`layout`, `container` records.

---

## Test plan (no write ever touches the owner's real account)

1. **A separate test Mozilla account** (new email), with Sync on.
2. **A separate Zen profile** on the PC (`zen --ProfileManager` → new profile "koan-test"), signed
   in to the test account only, Spaces sync on, a few fake spaces/tabs/folders, a few fake logins
   (example.com…), bookmarks and history. Never the owner's profile.
3. On the phone, a **separate Koan install**: the `staging` build type (`dev.nerzouille.koan.staging`,
   its own data and account, "Koan Test" in the launcher; `./gradlew assembleStaging`), signed in
   to the test account (QR sign-in from the test Zen profile).
   The owner's Koan keeps the real account and never has the write switches on during testing.
4. Order, per collection: dry run (compare the logged summary with what's expected) → snapshot
   checked on disk → one real write → check on the test Zen profile (and on a second Firefox profile
   with Rust engines, e.g. Fenix or Nightly, for passwords/bookmarks) → undo from the journal → check
   it's gone → conflict test (edit the same item on desktop and phone between syncs, expect a 412
   and a clean re-merge).
5. Spaces only after passwords, history and bookmarks have run cleanly for a while, and only with
   the test profile, starting with "add a pinned tab".
6. Only then the owner's account, collection by collection, dry run first.

## Sources

- Z-Sync contract: `reference/Z-Sync/shared/contract/SPEC.md` (§2, §3, §7, §7.2) and
  `reference/Z-Sync/android/.../sync/SpacesSyncService.kt`.
- Firefox Sync storage API 1.5: https://mozilla-services.readthedocs.io/en/latest/storage/apis-1.5.html
- Object formats: https://mozilla-services.readthedocs.io/en/latest/sync/objectformats.html
- application-services: `components/logins/src/sync/{payload,engine,merge,update_plan}.rs`,
  `components/places/src/history_sync/{record,plan,mod}.rs`, `components/places/src/bookmark_sync/record.rs`,
  `components/sync15/src/client/*` (https://github.com/mozilla/application-services)
- Firefox desktop: `services/sync/modules/engines/{passwords,history,bookmarks}.sys.mjs`,
  `services/sync/modules/{engines,service,util,constants}.sys.mjs`,
  `toolkit/components/places/SyncedBookmarksMirror.sys.mjs`, `modules/libpref/init/all.js`
  (https://github.com/mozilla-firefox/firefox)
- dogear: https://github.com/mozilla/dogear (`src/tree.rs`, `src/merge.rs`)
- syncstorage-rs limits: https://github.com/mozilla-services/syncstorage-rs (`syncstorage-settings`)
