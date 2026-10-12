# Roadmap

Work strictly in order. Each milestone ends with: acceptance criteria met on the real phone, unit
tests green, a short summary to the owner, and a git tag `m<N>`.

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

## M4 — Motion polish (≈ 1–2 weeks, iterate with owner)
- Hero open/close morph per `UI_SPEC.md` (springs, thumbnail → live swap, pre-scroll on close).
- Stack compression/elastic trailing while scrolling.
- New-tab grow animation, card lift/drop, close collapse.
- Baseline profile; JankStats logging in debug.
**Done when:** owner signs off on the feel. Expect several rounds of tweaks.

## M5 — Browser essentials (≈ 1–2 weeks)
- Read-only passwords from Firefox Sync → login autofill. History + bookmarks → search suggestions.
- uBlock Origin install via add-ons; add-on manager in settings.
- Downloads, site permissions, context menu (open in new tab / in Glance later / copy link),
  find in page, share, default-browser intent filters.
**Done when:** owner can uninstall Opera.

## M6 — Write-back to Zen (≈ 1 week, careful)
- `writeBackEnabled` setting (off by default), undo journal, conditional writes per spec §7.2.
- Operations in the order listed in `SYNC.md`.
- Test against a throwaway Mozilla account / throwaway Space first.
**Done when:** a tab added/closed/moved on the phone appears correctly in Zen desktop, and a 412
conflict is handled without data loss.

## M7 — Delights (open-ended)
- Glance (long-press link → floating sheet; swipe up to promote to tab).
- Pull-down for new tab, `@engine` search shortcuts, space menu (rename/icon/theme) if write-back
  for `space` records is added, "send to Zen" from share sheet, home-screen widget for a space.
