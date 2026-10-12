# Kickoff prompt for Claude Code

Paste this as your first message in Claude Code, from inside this folder (the one with CLAUDE.md).
For later milestones, reuse the last block and change the milestone number.

---

You're starting a new Android project called Koan: a personal Arc-for-iPhone-style browser for
Android whose workspaces are my Zen Browser Spaces (synced through my Mozilla account).

1. Read `CLAUDE.md` and everything in `docs/` (including the screenshot in `docs/reference/`).
2. Clone Z-Sync into `reference/Z-Sync` (gitignored) and read `shared/contract/SPEC.md` and the
   Android `sync/` package, so you understand what we'll port in M2.
3. Before writing code, reply with:
   - anything in the docs that's unclear, contradictory or that you think is a bad idea;
   - the current versions of GeckoView and android-components you plan to use (check
     maven.mozilla.org), and whether you recommend android-components or raw GeckoView for this UI;
   - your plan for M0 and M1 as a short checklist.
4. Wait for my OK, then implement **M0 only**, following `docs/ROADMAP.md`. Initialise git, add a
   `.gitignore` (including `reference/`, `local.properties`, keystores), commit as you go.
5. When M0's acceptance criteria are met, give me the exact commands to build a release APK and
   install it on my phone over adb, then stop.

---

Next milestones:

> Continue with **M1** from `docs/ROADMAP.md`. Re-read `docs/UI_SPEC.md` first. If
> `docs/reference/design/` contains a mockup, match it. Stop when M1's acceptance criteria are met and
> tell me exactly what to test on the phone.
