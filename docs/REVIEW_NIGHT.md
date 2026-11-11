# Review of the night batch (2026-11-10)

The owner's verdict on each item of `docs/NIGHT_REPORT.md`, tested on the test phone (nightly build). Fixes
are made in batches when the owner asks; nothing is merged into `dev` before the final OK.

Verdicts: **keep** / **fix** (exact change below) / **drop** (with the reason).

| # | Item | Verdict |
|---|---|---|
| 1 | History from a page | fix |
| 2 | One History page | keep |
| 7 | Library rules (History, Downloads, Passwords) | fix |
| 8 | Passwords page look, add a password | fix |
| 5 | Back close animation | keep |
| 6 | Toast replacing a toast | keep |
| 12 | Duplicate download card | fix |
| 4 | Page translation | fix |
| 3 | "Open in app" links | keep |
| 13 | Video player | fix (waits for the owner's design) |
| 9 | Zen mod icons (Zia) | keep |
| 11 | Two-way sync (real account) | keep |
| 8 | (again) Passwords page | fix, second pass |
| 10 | 0.4 re-checks | checked |

## Item 1: History from a page (fix)

- **Keep:** tapping an entry opens it in the tab History was opened from. From the overview (no tab
  behind it) a tap still opens a new tab. A Recently closed row: a tap reopens that tab.
- **Change:** the long-press bottom sheet is removed. A long press enters selection mode (item 7).

## Item 2: One History page (keep)

As built: ⋯ › History and the overview's History are the same page in the same state.

## Item 7: Library rules (fix)

Applies to History, Downloads and Passwords.

### Long press = selection mode

- Long-pressing a row enters selection mode with that row checked.
- In selection mode, a tap on a row checks or unchecks it (it doesn't open it).
- Header: "N selected", a **Select all** action and a **✕** that leaves selection mode.
- A bottom action bar (thumb zone) shows the actions. With one row checked it shows that row's
  actions; with several it shows only the actions listed under "Several" below.
- Back or ✕ leaves selection mode. Leaving with nothing checked also leaves it.
- Rows have no ⋯ button.

| Page | One row | Several rows |
|---|---|---|
| History | Open in new tab, Copy link, Share link, Remove (grouped row: "Remove N visits") | Share, Remove |
| History, Recently closed row | Reopen, Open in new tab, Copy link, Share link | — |
| Downloads | Open, Share (a small choice: "Share file" / "Copy download link"), Hide, Delete | Share, Hide, Delete |
| Passwords | Visit (opens the site in a new tab), Delete | Delete |

- Hide (Downloads) = removed from the list, the file stays.
- Passwords: no copy action in the list. Copying a username or password happens only on the
  site's page, after biometrics.

### Swipes

- **History and Passwords:** right to left = delete. Left to right does nothing beyond visual
  feedback: the row moves a little over a grey background, then springs back.
- **Downloads:** left to right = hide from the list (the file stays); right to left = delete the file.

### Confirmations and Undo

- **History:** Undo toast, no confirmation.
- **Downloads:** Undo toast. The file is only deleted for real once the toast is gone; Undo
  restores it untouched. No confirmation for one file. Deleting several files from selection mode
  asks first: "Delete N files?", Delete / Cancel.
- **Passwords:** always a confirmation card before deleting, in Koan's card style (`KoanCard`):
  - site + username;
  - what it affects: "Removed from this phone" for a synced login while password sync-back
    deletes ("Change and delete passwords") are off; otherwise "Removed from this phone and your
    Mozilla account";
  - Delete (red) / Cancel.
  - After Delete: an Undo toast.

## Item 8: Passwords page look, add a password (fix)

- **Look:** back to the previous design: a flat list on the page background (no card container),
  hairline dividers, chevron and "N accounts" on the right.
- **Icons on the list:** the site's favicon, bare at 20dp in place of the key icon (no tile, no
  background), always greyscale and slightly lower contrast so it sits with the key icons. A site
  with no favicon keeps the key icon. No letter tiles on this page.
- **On a site's page:** the favicon is greyscale while locked, crossfades (short) to full colour
  once unlocked with biometrics, and back to greyscale when it locks again.
- **Add a password:** the floating + button is removed. A round "+" button sits next to the search
  field, same height and style as the Clear button next to Downloads' search. The add form and
  the empty state stay as built.

## Done on `library` (first batch: items 1, 2, 7, 8)

The owner asked to implement this batch before going on with the review. Choices made where the
spec was silent:

- Unchecking the last row leaves selection mode.
- The bar's labels are short: "New tab" for Open in new tab, "Share link", "Copy link".
- Recently closed: one closed tab is checked at a time (the spec has no "several" actions for
  them), and closed tabs are never checked together with pages. Their rows have no swipe, as before.
- A group row checked alone: only "Remove N visits". Several rows: Share shares every page of a
  checked group, one link per line.
- Downloads › Share with one file: the bar turns into Back / Share file / Copy download link.
- Passwords' confirmation says "…and your Mozilla account" only when a synced login is among them
  and "Change and delete passwords" is on; a login saved only on this phone says "Removed from this
  phone".
- A site's page, locked, now shows each account's site row (grey favicon) above Unlock, so the
  favicon is there to turn to colour.
- Favicons on the Passwords list are loaded from each site (as tabs' are); Koan 0.4 showed only
  the key. To note in `docs/PRIVACY.md` at merge time.

## Library batch, second round (owner's test feedback)

- History's bar: "New tab" keeps its label, with an open-in-new-tab glyph (arrow out of a box).
- Swipes: a row always springs back when the gesture ends or is cancelled; one row never blocks
  another (one owner for each row's offset). History, Downloads and Passwords.
- Downloads' bar, one file: Open, Copy link, Share (the file), Delete. Several: Share, Delete. Hide
  is a swipe only. Open's glyph by type: video or audio = play, image = picture, APK = install
  (label "Install"), anything else = document.
- Passwords' icons: only icons already on the phone (tabs, History, Essentials), never fetched
  when the list opens. Each is cut to its drawing and fills the same 20dp box, in greyscale; an
  opaque square icon keeps its square with rounded corners; an icon that can't be seen in grey on
  the page (too light or too dark) shows the key.
- The build tested before this round fetched missing icons for the Passwords list, falling back to
  DuckDuckGo's icon service for public hosts. That test build was never released; removed.
- Large titles of Settings pages stay fully opaque for the first quarter of their height scrolled
  (a small nudge greyed them), then fade as before.

## Item 5: Back close animation (keep)

As built: Back on a tab's first page shrinks the page into a card that slides off to the left over
the tab underneath; no animation when Back returns to the app that opened the link.

## Item 6: Toast replacing a toast (keep)

As built: a new toast while one shows: the old one sinks away and fades (from where it was dragged)
while the new one rises in its place.

## Item 12: Duplicate download card (fix)

- **Keep:** the card and its choices (Open, Replace, Keep both, Cancel), the rename pencil, the
  checks (same address with the file still there, or same name in the folder). Both this card and
  the download confirmation stay floating cards (they answer an action), not drawers.
- **Rename:** tapping the file name edits it too, not only the pencil. The pencil is bigger (20dp
  glyph in a 44dp tap target) and stays next to the name. Editing: the name becomes a text field in
  place, the extension kept and only the base name selected, keyboard up; Done or tapping a choice
  (Replace, Keep both) confirms it.
- **Open row:** the type-based icon, same as the Downloads bar: play for video or audio, picture
  for an image, install for an APK, document otherwise.
- **Cancel, on every card (one rule: no text-only Cancel anywhere):**
  - A card with two choices (delete confirmations for passwords and "Delete N files?", Sign out,
    Clear browsing data, the save-password prompt): Cancel and the action side by side, same size;
    Cancel neutral on the left, the action on the right (red text when destructive).
  - A card with a list of choices (the duplicate card, permissions…): Cancel is a full-width
    neutral button under the list (same height and radius as the other buttons, normal text colour).
- **Card to card = one card that morphs:** when a card leads to another (the image long-press card
  → the duplicate card; the download confirmation → the duplicate card; any other such chain),
  the card isn't closed and another opened. The same card stays: its height and corners animate to
  the new size (spring), the big image preview shrinks into the small thumbnail at the top, and the
  content crossfades.

## Item 4: Page translation (fix)

- **Keep:** on-phone translation (Firefox Translations in Gecko), the offer card (20 s), ⋯ ›
  Translate page with its pickers, Show original, Never for language / site.
- **Bug:** Translate, then Show original, then Translate again: the page is translated correctly
  but the page stays in its "Translating…" state (button locked). Once a translation (or a
  restore) has finished, the page must show the finished state (Show original / Translate again),
  every time, however many times it goes back and forth.
- **Language list (From and To):**
  - A ‹ back button in its header returns to the Translate page (as Back does).
  - A search field at the top; while searching, the sections hide and only matches show.
  - Sections, each language once, in the first one it fits:
    1. **Recent:** the last 3 languages picked on that side (From or To), the current one checked.
    2. **On this phone:** languages already downloaded, marked "On phone".
    3. **All languages:** the rest A–Z, each with its download size when known.
- **Always translate a language:**
  - ⋯ › Translate page: an "Always translate [language]" switch next to the Never switches.
  - The offer card: a quiet "Always for [language]" link next to the Never links.
  - With it on, every page in that language is translated automatically to the To language (no
    offer card), and a toast says "Translated from [language]" with Show original.
  - Settings › Translation: lists the Always languages, the Never languages and the Never sites,
    each removable there.

## Item 3: "Open in app" links (keep)

As built (tested by the owner with a YouTube client: the card, the app opens, ⋯ "Open in [app]"). Its card's
Cancel follows the card rule of item 12 (Cancel and Open side by side). Not tried yet: a custom
scheme for an app that isn't installed (`alipays://x` → toast).

## Item 13: Video player (fix, waits for a design)

The owner is getting a full design from Claude Design (layout, states, gestures: speed, long-press
2×, brightness / volume swipes, PiP button, lock). The player is rebuilt from it once it arrives.

- **Keep:** picture-in-picture on Home; YouTube keeps its own controls (fallback list).
- **Problems to fix (in the rebuild):**
  - A tap shows / hides the controls at once, never waiting for the double-tap timeout.
  - At the end of the video Koan's controls stay, with a Replay button (the site's own player never
    comes back).
  - Volume is a mute toggle, not the system volume panel.
  - No rotate button.
  - "Use the site's controls" moves into a ⋯ menu (no unclear finger button).
  - Controls stay readable on bright videos.
  - Swipe down only leaves fullscreen (back to the same spot in the page); it never triggers the
    page's pull-to-refresh.
- **One player component, two modes:**
  - **In a page:** it takes over the site's player in place, in the same tab.
  - **Without a page** (Downloads, "Play video" on a long-pressed video link, a direct .mp4 / .webm
    opened from another app): an overlay above the current screen, never a new tab; closing it
    returns to the previous screen.
- **New entries:**
  - The link long-press menu gets "Play video" for video links.
  - Tapping a video in Downloads opens this player instead of an external app; "Open with…" stays
    available.
- **Open point:** where "Open with…" sits for a video in Downloads (selection bar, or the player's
  ⋯ menu): to settle with the design.

## Item 9: Zen mod icons (Zia) (keep)

As built; tested by the owner with a Zia icon on a space: the phone shows the same icon.

## Item 11: Two-way sync on the real account (keep)

As built; tested by the owner. Switches stay: Passwords on, History on, Change and delete
passwords off.

## Item 8, second pass: Passwords page (fix)

Replaces the "+" placement written above for item 8.

- **Add button:** the round "+" next to the search field goes. It is a round "+" at the bottom
  right, floating above the page (over the list, which scrolls under it) with a shadow; the list
  ends with room for it so the last row isn't hidden. Same add form as now.
- **Delete on a site's page:** under each account's username and password, a red "Delete password"
  row, shown only once the page is unlocked. It opens the same confirmation card as the list (site ·
  username, "Removed from this phone" / "…and your Mozilla account", Cancel / Delete), then an Undo
  toast; when it was the site's last account, the page goes back to the list.

## Item 10: 0.4 re-checks (checked)

No code. Search in Koan, the download confirmation card, slow swipe-to-close, Reopen closed tab and
the status bar in the search sheet work on the nightly build; the space chip on "Switch to tab"
checked in code only.

## Final list approved (2026-11-11), with two additions

- **Selection mode header (library pages):** "N selected" replaces the page title cleanly: the
  large title collapses or fades out while selecting; the header never sits on top of it.
- **QUERY_ALL_PACKAGES:** before documenting it, try targeted `<queries>` (VIEW intents for http /
  https and the custom schemes Koan handles, specific packages if needed) so Koan can still name
  the app and show its icon. Keep the permission only if that really doesn't work, and say why.

## Test pages removed from the account history (2026-11-11)

The batch's test pages and one test search were removed from History on the phone, by exact
address and only for the night's visits; two-way sync sent their tombstones to the account.

## Build batch after the final OK (2026-11-11)

Built on `library`, `app-links`, `download-dup` (now contains `library`, for the file-type
glyphs) and `translate`, merged into `nightly`; 269 unit tests, 0 failures; dev build on the test phone.
The video player waits for the owner's design.

- **QUERY_ALL_PACKAGES:** replaced by targeted `<queries>` (VIEW for http / https, 43 app schemes
  sites use). Koan 0.4 already had the permission without asking for it: android-components'
  `support-utils` adds it; it is now removed from the merged manifest (`tools:node="remove"`).
  Checked on the test phone: the permission is gone and Koan still sees the YouTube app. An app behind a
  scheme outside the list is offered as "another app" (Android says at the tap whether it exists).
  So no permission to document in PRIVACY; the change goes in the CHANGELOG.

## Retest of the build batch (owner, 2026-11-11)

All approved, with these follow-ups (done on the branches, in `nightly`):

- **Passwords, locked:** nothing is deleted or selected before the fingerprint / PIN: a swipe shows
  the red delete as usual, then asks to unlock, then shows the delete card; a long press asks to
  unlock, then starts selection mode on that row.
- **Google Images:** a press before the full image has loaded returns Google's small preview
  (encrypted-tbn…gstatic.com). Save image then waits for the full image (like Opera): a small
  built-in extension (assets/extensions/pageimage) remembers the long-press point and answers the
  largest fully loaded real image there (up to 10 s); the card says "Waiting for the full image…",
  then saves it (or turns into the duplicate card). Nothing is read before Save is tapped.
- **Downloaded languages:** a long press on a language that is on the phone (From / To lists)
  opens a card "Delete French?" (frees N MB; downloads again next time), Cancel / Delete.
- **Settings › Translation:** an "On this phone" section: each downloaded language with its size
  and Delete (same card).
- App links on another phone (Alipay etc.): not testable here; trusted.

## Open later (owner, 2026-11-11)

- **Full image on Google Images:** kept as it is for now ("not perfect, but better"). To improve
  later: it waits up to 10 s for the largest loaded image under the long-press point; revisit what
  it picks and how the wait feels (owner to say what's off when we come back to it). Goes in
  `docs/HANDOFF.md` as an open point at merge time.
