# Roadmap

Work strictly in order. Each milestone ends with: acceptance criteria met on the real phone, unit
tests green, a short summary to the owner, and a git tag `m<N>`.

**Order (changed 2026-10-15):** M0 → M1 → M2 → M3 → **v0.1.0 pre-release** → **M5** → **M4** → M6 →
M7. Browser essentials come before the motion polish, so Koan can become the daily browser sooner.
Milestone numbers are kept as they are.

## M0 — Skeleton browser (≈ 2–4 days)
- Gradle project, package `dev.nerzouille.koan` (rename if owner prefers), Compose, single activity.
- GeckoView via android-components rendering one tab; bottom toolbar with address pill, back, menu.
- Type URL or search (DuckDuckGo default) → loads. Back/forward, reload, progress bar.
- Decide android-components vs raw GeckoView; log the decision in `ARCHITECTURE.md`.
**Done when:** can browse normally on the phone in a release build.

## M1 — Overview UI with fake data (≈ 1 week) ← the fun part, UI first
- Room schema from `ARCHITECTURE.md`; seed 3 fake spaces with gradients and 8–15 tabs each.
- Space overview: header, essentials grid, overlapping card stack, workspace bar, gradient bg.
- Workspace bar paging with parallax + gradient blend + haptics.
- Swipe-to-close, tap ✕, long-press reorder (reorder within space is enough here).
- Placeholder thumbnails (favicon-colored gradients).
**Done when:** the overview feels great on the phone at full refresh rate with 50 cards; no jank in
System Trace during scroll and space switching.

## M2 — Sign in + read Zen Spaces (≈ 1 week)
- Clone Z-Sync into `reference/`, port the sync layer (see `SYNC.md`), port contract tests.
- Mozilla sign-in screen (re-skinned), TOTP prompt, encrypted credential storage, sign-out.
- Pull `spaces` + `prefs` into Room; overview now shows real spaces, essentials, pinned/normal tabs,
  folders, splits. Pull-to-refresh, background refresh rules.
**Done when:** all contract fixtures pass, and the phone shows the same Spaces as Zen on the PC.

## M3 — Real tabs in spaces (≈ 1–2 weeks)
- Open a card → real engine tab (simple crossfade transition for now).
- New tab in current space; navigation updates title/url/favicon in Room.
- Tab lifecycle: max 4 live sessions, suspension/restore, trim-memory handling.
- Thumbnail capture + disk LRU + async decode.
- Swipe on address pill switches spaces in page view.
- State survives process death (restore last space/tab).
**Done when:** daily browsing across spaces works for a few days without lost tabs or crashes.

## v0.1.0 — First pre-release (after M3)
- Release hygiene: dev menu items and fake stress-fill data exist in debug builds only. A fresh
  install without a Mozilla account is a plain browser with one local space and an optional
  "Connect your Zen Spaces".
- versionName 0.1.0, `CHANGELOG.md` with what works and the known limitations.
- Signed arm64-v8a release APK, attached to a GitHub **pre-release** tagged `v0.1.0`.
**Done when:** first run checked on a clean install, the APK updates the owner's install without
losing data, and the owner has approved and published the release.

## M5 — Browser essentials (≈ 1–2 weeks) — done 2026-10-19 (tag `m5`, v0.2.0 beta)
- Read-only passwords from Firefox Sync → login autofill. History + bookmarks → search suggestions.
- uBlock Origin install via add-ons; add-on manager in settings.
- Downloads, site permissions, context menu (open in new tab / in Glance later / copy link),
  find in page, share, default-browser intent filters.
- About: one GitHub releases check when About opens ("Up to date" / "Update available"), never
  polled in the background.
- Logins saved on the phone ("Save in Koan, this phone only") stay local and encrypted until M6.
**Done when:** owner can uninstall Opera.

### Open items after v0.2.0 (not scheduled yet)
- **App icon / logo:** a real launcher icon and logo.
- **Search backdrop retry:** the old dim plus a subtle blur behind the suggestion cards only (also
  listed under M4).
- **Desktop site:** revisit the toggle.
- **Add to home screen:** a page as a launcher shortcut.
- **Picture-in-picture** for video when leaving Koan.
- **Private tab:** a tab that keeps no history, cookies or cache.
- **Pull to refresh in chats:** a site whose list scrolls inside the page (not the page itself) may
  reload on a pull at the top of the list. Gecko doesn't say whether an inner list can scroll up.
- **Long screenshots:** update to GeckoView/android-components 160 (after Nov 10), then test
  whether HyperOS's scroll screenshot works; if not, consider our own stitched full-page
  screenshot or Save as PDF.
- **Print:** `window.print()` does nothing yet: GeckoView needs an activity context
  (`GeckoEngineView.setActivityContext`, probably a one-liner).
- **Page painted only down to where the keyboard was** (white below, stays while scrolling):
  seen after submitting an address that switched to an older tab. Also on 142389d, so not from
  the dock work. Next to investigate.
- **Find in page with the keyboard up:** check that a match is never hidden under the find bar
  (tapping the page or scrolling already shows it).
- **Dock ignoring taps:** happened once on a Google AI mode tab (pill and tab count did
  nothing); not reproduced since. Watch for it.

### Android 11+ (minSdk 30) — merged 2026-10-23, ships with the next release
- Runs on Android 11–13 too: no live blur below 12 (stronger dim instead), "copied" toasts below
  13, smaller memory budgets on phones under 6 GB (`DeviceProfile`). Audit: `docs/audits/MIN_SDK.md`.
- Tested as `preview-android11` (0.2.2-preview.1, versionCode 7): no frame or start-time
  difference on the owner's phone (`tools/perf`). The next release takes versionCode 8 or higher.
- README and FEATURES still say Android 14+: update them with the next release's docs.

## M4 — Motion polish (≈ 1–2 weeks, iterate with owner)
- Hero open/close morph per `UI_SPEC.md` (springs, thumbnail → live swap, pre-scroll on close).
- Stack compression/elastic trailing while scrolling.
- New-tab grow animation, card lift/drop, close collapse.
- Baseline profile; JankStats logging in debug.
- Retry search backdrop: old dim + subtle blur behind the suggestion cards only.
**Done when:** owner signs off on the feel. Expect several rounds of tweaks.

## M6 — Write-back to Zen (≈ 1 week, careful)
- `writeBackEnabled` setting (off by default), undo journal, conditional writes per spec §7.2.
- Operations in the order listed in `SYNC.md`.
- Test against a throwaway Mozilla account / throwaway Space first.
- Offer to push the phone-only logins saved in M5 to the Mozilla account (`passwords` collection),
  one explicit confirmation, never automatically.
**Done when:** a tab added/closed/moved on the phone appears correctly in Zen desktop, and a 412
conflict is handled without data loss.

## M7 — Delights (open-ended)
- Pull-down for new tab, `@engine` search shortcuts, space menu (rename/icon/theme) if write-back
  for `space` records is added, "send to Zen" from share sheet, home-screen widget for a space.

### M7 ideas (to refine when we get there)

**Link gestures** (all start with a long-press on a link, so they never conflict with scrolling; a small link chip follows the finger, labelled with the action, haptic tick at the threshold; everything works one-handed from the bottom of the screen)
- **Glance**: long-press a link + drag UP → the page opens in a floating sheet over the current tab.
  - Controls live at the bottom of the sheet (thumb zone): a handle + two buttons, "Open as tab" and "✕". Android back also closes it.
  - Flick on the bottom handle: one direction promotes to a real tab, the other dismisses. Direction is a token, decide by testing (option A: up = open / down = dismiss, like the rest of Koan; option B: up = throw away / down = open, like the iOS app switcher).
- **Open in background**: long-press a link + drag DOWN → opens in a new background tab in the current space.
- **Open in another space**: while dragging a link down, the dock shows the space icons; dropping the link on one opens it in that space (in background).

**Tabs**
- **Undo close**: after closing a card (swipe or ✕), a small "Undo" toast for a few seconds.
  (Done for Back on a tab's first page in v0.2.0; not yet for a card closed in the overview.)
- **Recently closed**: long-press the tab button → list of recently closed tabs, tap to reopen.
- **Previous tab**: double-tap the tab button → switch back to the previous tab (Alt-Tab style).
- **Glance a card** (maybe): long-press a card in the overview to preview it larger without switching. Same sheet as Glance. To evaluate.
- **Auto-archive**: phone-only tabs untouched for N days (setting) move to an "Archived" list instead of piling up. Never touches tabs that come from Zen.

**Media**
- ~~**Background audio**: audio/video keeps playing with the screen off or Koan in the background, with media controls in the notification.~~ Done in v0.2.0.
- **Picture-in-picture** for video when leaving Koan.

**Phone → PC**
- **Send to PC now**: use Firefox Accounts "Send Tab to Device" (Koan registers as a device on the Mozilla account) so the page opens immediately on Zen desktop. Needs verification that Zen receives sent tabs (it's Firefox-based, so it should).
- **Add to a Zen space**: write the tab into a chosen Zen space via sync (requires M6 write-back); appears on desktop at the next sync.
