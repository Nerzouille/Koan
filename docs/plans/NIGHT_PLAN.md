# Night plan 2026-11-25 → 26: beta.2, the items that don't need the owner's hands

Unattended run on `beta2-bugs` (from `dev` 60a2199). The previous night plan (release prep for
beta.1) is in `NIGHT_PLAN_2026-11-12.md`.

## Context

`docs/plans/BETA2_BUGS.md` lists the beta.2 items. Items 1–3 are done and phone-checked; 4, 5, 6, 7,
8 and 16 need the owner's hands or choice and are not part of this night. This night does the rest,
in the owner's order, one commit per item, with JVM tests where the logic allows it.

## The owner's instructions (2026-11-25, before leaving)

- Order: the two Find / translation findings (items 9, 10), the player checks on public pages
  (item 11, Koan Test, staging log patch never committed), the player tracks (item 12, approved;
  if a check result contradicts the plan, skip that part and write it down), the translate icon
  and ring slot (13), the Rotate row (14), the passkey failure message (15), then the full suite
  and the R8 build with kill and restore (17).
- One commit per item, with tests.
- **Only Koan Test (`dev.nerzouille.koan.staging`) on the phone.** The real Koan is not touched.
- The report goes at the end of `BETA2_BUGS.md`, with **one checklist** of what the owner must test
  by hand. (This file keeps a short `## Night report` that points there.)

## Open questions, decided by default (owner away)

- **Item 14, hide Rotate when auto-rotate is on?** Default: always show it. It's a manual switch
  that works either way (it's the same switch as the rotate pill). Easy to change in the morning.
- **Item 12, side files:** option A (approved in BETA2_BUGS: "side files, option A").
- **Phone not connected at the start of the night.** Phone steps (item 11's checks, the R8 kill and
  restore) run only if the POCO shows up in `adb devices`; otherwise they're listed as not done.
  Without item 11's results, the Commons-specific part of item 12 (rebuilding `<track>`s from the
  page's markup) is skipped; the Quality row is guarded at run time (shown only when `currentSrc`
  is a plain http file), so both outcomes of the Commons check are safe.

## Items

9. **No auto card while Find is open.** Acceptance: `translateCue` gives the icon, never the card,
   with Find open; the icon still opens the card; JVM test.
10. **An empty Find field clears every highlight.** Acceptance: the cause found in GeckoView's
    finder code, a fix that doesn't leave an older query's highlight; JVM test of the logic.
11. **Player checks.** Acceptance: results in PLAYER_TRACKS.md, or "not run" with the reason.
12. **Player tracks** (web Captions, web Quality, standalone Audio / Subtitles, side files `.srt` /
    `.vtt`, "Choose a file…"). Acceptance: each row only with a real choice; `.vtt` → SubRip
    converter unit tested; the menu-row logic unit tested. Can be split into several commits.
13. **Translate icon stays active after translating; the download ring takes its slot.**
    Acceptance: cue logic gives an "active" icon once translated; the ring hides the icon; tests.
14. **Rotate row** in the web and standalone players' ⋯ menu. Acceptance: same switch as the rotate
    pill; test where the logic allows.
15. **Passkey failure message.** Acceptance: a short Koan toast when the credential request fails
    the way Google Password Manager fails today; string in `strings.xml`; test of the mapping.
17. **End of phase.** Full JVM suite; R8 build (staging) launched on the POCO, kill and restore, if
    the phone is connected.

## Night report

See `docs/plans/BETA2_BUGS.md` › Night report 2026-11-25 (the owner asked for it there).
