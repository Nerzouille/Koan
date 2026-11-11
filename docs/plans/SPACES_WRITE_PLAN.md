# Two-way sync for Zen Spaces: plan

Status: **plan only, nothing built. Postponed by the owner (2026-11-11): starts at step 0 after
the next release.** The owner's decisions are in §9.
Scope: the Zen `spaces` collection only. Passwords and history are in `docs/SYNC_WRITE.md`.

Sources: Z-Sync's contract (`reference/Z-Sync/shared/contract/SPEC.md` §2, §3, §6, §7, §7.2) and
its Android writer (`SpacesSyncService.kt`: `planAddTab`, `tombstoneBatch`), `docs/SYNC.md`,
`docs/SYNC_WRITE.md` §0 and §3. **Zen's own sync engine source was not read for this plan**: every
claim about how the desktop merges is marked *[unverified]* and is settled in step 0.

## 1. Principles

1. **Off by default, one gate.** Every write goes through `SyncClient`, so `SpacesWriteGate` and
   `Settings.writeBackEnabled` stay the single switch. Nothing in this plan adds a second path.
2. **Never re-serialize from Room.** Room is a projection (splits as one card, folders flattened).
   A write always starts from the **cleartext just downloaded** and changes named fields only.
   Unknown fields, `null`s and field types are kept as received.
3. **Intents, not state.** The phone records what the owner did ("closed tab X", "moved X after
   Y"), and replays each intent on a fresh read of the server. It never uploads "the phone's view
   of a space".
4. **The desktop wins.** On any doubt (conflict, unknown shape, failed check) the intent is
   dropped, the phone shows the server state again, and nothing is sent.
5. **One logical change = one atomic, conditional batch**, validated before it is sent.
6. **Any surprise turns `writeBackEnabled` off** and says why (same behaviour as the password and
   history switches).

## 2. What Koan writes, and what it never touches

| Record | v1 writes | Rule |
|---|---|---|
| `tab` | `folderId`, `workspaceUuid`, `pinned` on a move / pin; tombstone | Every other field kept as downloaded. `url` and `title` of an existing record are never changed. **No new tab record in v1** (§9.1). |
| `space` | **`data.children` only** | Add / remove / reorder ids. Name, icon, theme, `containerGuid`, `uuid`: never. No space is created or deleted. |
| `folder` | **`data.children` only** | Same. No folder is created, renamed or deleted. |
| `split` | never | A tab that is a split member can't be closed, moved or reordered to the server in v1: the action stays local. |
| `layout` | never | So essentials can't be closed, added or reordered to the server in v1; space order is never written. |
| `container`, unknown kinds, records that fail to decode | never | If an intent would need one, it is dropped. |
| `prefs`, `meta/global`, `crypto/keys`, `clients`, Firefox `tabs`, bookmarks | never | Read only. The `crypto/keys` bootstrap stays disabled. |

`docs/SYNC.md` says "never write `layout` or `space` records in v1". This plan narrows that to
"`space.children` only", because adding or removing a tab without updating its parent's `children`
leaves a ghost entry or an orphan (Z-Sync does the same).

## 3. Record format

All of it comes from the contract; the fixtures in `fixtures/wire/` are the test inputs.

- **Envelope:** cleartext `{id, kind, data}`, encrypted with the `spaces` collection key (else
  `default`) from `crypto/keys`, as reads do. `id` equals the BSO id. BSO ids are percent-encoded
  per §6 (`{uuid}` ids keep their braces).
- **Tombstone:** encrypted cleartext `{"id": <id>, "deleted": true}`, JSON boolean. Never a
  WBO-level `deleted` (Zen ignores it and re-uploads the item).
- **Rewritten record (space, folder, moved tab):** the downloaded cleartext with one field changed.
  A deep comparison before sending must show only whitelisted fields differ (§5).
- **New tab (not in v1, kept for the later "Add to Zen" action, §9.1):** the exact field set
  Z-Sync writes, which Zen reads back:

  | field | value |
  |---|---|
  | `id` (record) and `tabId` | same new id, in Zen's own id format (Z-Sync's `newTabRecordId`) |
  | `url` | non-empty, has a scheme, http(s) only |
  | `title` | string |
  | `icon` | `null` |
  | `essential` | `false` |
  | `pinned` | `true`; `false` only if `normalTabsCapability == enabled`, else written pinned |
  | `workspaceUuid` | an existing `space` record's `uuid` |
  | `folderId` | an existing folder of that space matched per §3.5 (D2), else `null`. Normal tabs: always `null` |
  | `hasStaticIcon` | `false` |
  | `staticLabel` | `null` |
  | `containerGuid` / `defaultContainer` | the space's `containerGuid` + `false`, or `null` + `true` |

- **Parent of a new tab:** its id appended to `children` (order-preserving union, never a
  duplicate).

## 4. Conflict rules

Each intent stores what the phone saw when the owner acted (the tab's url, the field's old value,
the parent id). At write time Koan reads the whole collection, replays the intent on it, and sends
the batch with `X-If-Unmodified-Since: <collection timestamp of that read>`.

| Phone intent | Server state at write time | Result |
|---|---|---|
| any | target record missing or tombstoned | Intent dropped. **Koan never resurrects** a record. |
| close tab | tab unchanged (same `url`) | Tombstone + parent `children` without it. |
| close tab | tab's `url` changed on the desktop | Dropped; the tab comes back on the phone (an edit beats a delete). |
| close tab | tab is in a split or in `layout.essentials` | Dropped (v1 never writes those); stays hidden locally only. |
| move / pin (one field) | that field still has the value the phone saw | Field changed; old and new parent `children` updated in the same batch. |
| move / pin | that field changed on the desktop | Dropped: the desktop's value wins. Other fields are untouched either way. |
| move | target space or folder gone, or folder in another space | Dropped. |
| reorder "X after Y in parent P" | X and Y still in P | Replayed on the **fresh** `children`: ids the desktop added stay where the desktop put them. |
| reorder | X no longer in P (moved or deleted on the desktop) | Dropped. |
| reorder | Y gone | X goes after the nearest earlier sibling the phone saw that still exists; none left: dropped. |
| add tab (after v1) | parent exists | New record + union into fresh `children`. |
| add tab (after v1) | space gone | Dropped (the tab stays a local phone tab). |

- A close is queued only once its **Undo has expired** (§9.2): Undo cancels it before anything is
  sent.
- A reorder never changes membership: the validator checks `children` is a permutation of the fresh
  list. Two devices reordering the same space: the second writer's moves are replayed on top of the
  first's, nothing is lost.
- **412** (the desktop wrote between the read and the POST): nothing was written. Re-read, replay,
  retry **once**; a second 412 leaves the intents queued for the next sync. Never an unconditional
  write.
- Intents expire after 7 days and at most 20 are sent per run; beyond that Koan drops them and
  shows the server state (a stale or huge replay is where damage comes from).
- How Zen merges an incoming record with a local change it hasn't uploaded yet is *[unverified]*
  (step 0). Koan's side is covered by the conditional write; the desktop's side is why step 0 and
  the test profile exist.

## 5. Safety checks before any write

**Guard (per run)**, extending `WriteGuard`: `meta/global` readable, `storageVersion == 5`,
`spaces` not declined, `engines.spaces` present with the **version and syncID pinned at the first
backup** (the expected version is fixed in step 0). The collection must decode cleanly: a kind
outside the contract's six, or more records than the 50-page read limit, refuses the run.

**Batch validator (pure function, per batch)** refuses the batch unless all hold:
- only `tab`, `space`, `folder` records and `tab` tombstones; at most 6 records; in v1 no record
  whose id is not already on the server;
- every rewritten record deep-equals its fresh cleartext except: `children` (space, folder),
  `folderId` / `workspaceUuid` / `pinned` (tab);
- `children` differs from the fresh list by exactly the intent's id (add / remove) or is a
  permutation (reorder); no duplicates;
- a tab's `workspaceUuid` is an existing space; its `folderId` is `null` or a folder of that space;
  it is listed in exactly one parent's `children` after the batch;
- `pinned: false` only with `normalTabsCapability == enabled`;
- no tombstoned id is referenced by any `children`, split `tabs` or `layout.essentials` left on the
  server;
- record `id` equals the BSO id; new ids don't exist on the server.

**Send:** one atomic `POST` (`SyncClient.postBatch`), `failed` must be empty and `success` complete,
then a read-back of the written ids compared with what was sent. Any mismatch: gate off.

**Logs:** counts, kinds and field names only. Never a url, title, space or folder name.

## 6. Backup and restore

**Backup** (before the first write, then before each run that writes; the last 10 kept):
- every BSO of `spaces` as downloaded (id, `modified`, encrypted payload), the collection
  timestamp, `engines.spaces` from `meta/global`, and the `modified` of `crypto/keys`;
- stored like the M6 backups: app-private `files/sync-backup/` plus a copy in the external files
  folder (reachable over USB). Still encrypted with the account's keys: useless without them;
- a run with no backup written and re-read from disk does not write.

**Restore** (Settings › Mozilla account, shown only while `writeBackEnabled` is on; confirm dialog):
1. Refuse if `engines.spaces` syncID or `crypto/keys` changed since the backup, or if any backup
   payload no longer decrypts with the current key.
2. Build one batch: every backup record re-uploaded with its exact payload; every record that
   exists now but not in the backup gets a cleartext tombstone.
3. Send it conditionally and atomically (the multi-POST `batch` / `commit` API if it exceeds one
   POST), read back, then turn `writeBackEnabled` off.

Limits, stated plainly: a restore puts the **server** back. Whether a desktop that already applied
Koan's writes fully returns to the old state on its next sync is *[unverified]* and is tested in
step 5. So the second safety net is on the desktop: **before the first write to the real account,
the owner copies the Zen profile folder** (Zen closed). That copy restores the desktop whatever the
server holds.

## 7. Tests before any real account

1. **Fake server (JVM, no network):** an in-memory `SyncTransport` with real encryption, per-BSO
   and collection timestamps, `X-If-Unmodified-Since` / 412, atomic POST with `success` / `failed`,
   pagination. Z-Sync's `fixtures/http/*` pin its answers.
2. **Round trip:** every `fixtures/wire/*` record goes download → planner with no intent → upload
   candidate, and must be JSON-equal (unknown fields such as a folder's `live` object included).
3. **One test per row of §4**, plus: the desktop writes between Koan's read and POST (412 then a
   clean replay; twice → queued); gate off → the fake server saw **zero** non-GET requests; guard
   refusals; validator refusals (one test per rule of §5).
4. **Random runs:** random intents interleaved with random "desktop" edits on the fake server;
   after each run the collection must pass a consistency check (every `children` id exists, every
   tab in exactly one parent, no tombstone referenced).
5. **Backup / restore:** backup → random writes → restore → the collection equals the backup.
6. **Test account:** a new Mozilla account, a separate Zen profile with fake spaces
   (`example.com` tabs), the `staging` build on the POCO signed in to it (`docs/SYNC_WRITE.md`
   "Test plan"). The owner's Koan keeps `writeBackEnabled` off the whole time.
7. **Real account** only after step 8 below, one operation type at a time.

## 8. Steps

Each step is its own branch from `dev`, ends with the targeted unit tests and a dev build on the
POCO. Until step 4 nothing can write: the POCO check is "sync still reads as before, log shows what
would be sent".

| # | Step | Check on the POCO |
|---|---|---|
| 0 | **Research, no code.** Read Zen's spaces sync source: field set of each kind, merge of incoming vs local changes, tombstone handling, what it does with an id in `children` it doesn't know, the engine version in `meta/global`. Read-only look at `engines.spaces` on the real account (version only). Fill the *[unverified]* points here; stop if something contradicts the plan. | — |
| 1 | Fake server, round-trip tests, batch validator, guard for `spaces`. | Dev build: pull unchanged. |
| 2 | Backup + restore code, a hidden switch for `writeBackEnabled` (dev / staging builds only) and a **dry-run** flag (on by default: plan, validate, log, send nothing). | Switch on, dry run on: a backup file appears, log says "0 intents". Fake-server restore tests green. |
| 3 | Intent queue (Room) + planner for **reorder**. | Reorder a synced tab: log "1 intent: 1 parent rewritten", no request sent. |
| 4 | **First real write, test account only** (staging build): reorder. Conflict tried by hand (reorder on both sides between syncs). | The order matches in the test Zen profile after a desktop sync. |
| 5 | Restore on the test account. | Server equals the backup; note what the test Zen profile does. |
| 6 | Move between spaces / folders, on the test account. | The tab is in the right space / folder on the desktop; a move made on the desktop first wins. |
| 7 | Pin and unpin. | Same checks, both directions. |
| 8 | **Close**, sent only after Undo has expired. Conflict tried by hand (edit on the desktop, close on the phone). Then a week of normal use on the test account with an R8 build. | Tab gone on the desktop; Undo sends nothing; the edited tab comes back on the phone; no gate-off in the log. |
| 9 | **Real account:** Zen profile copied, backup checked on disk, dry run read by the owner, then reorder → move → pin → close, each for a few days. | The owner's desktop Spaces unchanged except for the intended edits. |

A visible Settings switch, "Add to Zen" for new phone tabs, splits, essentials, and creating / renaming / deleting spaces or folders
come after step 9, as separate plans.

## 9. Decisions (owner, 2026-11-11)

1. **Which phone actions go up:** close, move, pin and reorder of **synced** tabs. A new phone tab
   goes up only through an explicit **"Add to Zen"** action, which is **not in v1**.
2. **Closing a synced tab on the phone closes it on the desktop**, but the upload waits until the
   close's **Undo has expired**; Undo before that sends nothing.
3. **Test account:** the owner creates a test Mozilla account and a Zen profile named
   "koan-test" (needed from step 4).
4. **Order of operations:** reorder, move, pin, and close last.
