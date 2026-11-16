---
description: Rules for an unattended (overnight) run while the owner is away
argument-hint: "<the batch to work on>"
---

The owner is away and won't answer until morning. Work through this batch on your own: $ARGUMENTS

## Before starting (while the owner is still there, if possible)
- Read `docs/HANDOFF.md` and the batch. Ask every open question **now**, in one message with
  concrete options. Note the owner's answers in the plan.
- Write the plan to `docs/plans/NIGHT_PLAN.md`: context, the owner's answers, numbered items with
  acceptance criteria. Commit it first, on the night's branch.

## Ground rules
- **Branches:** the night's branch is made from `dev`, or one feature branch per item made from
  `dev`, as the plan says. Never commit to `dev` or `main`.
- **Commits:** commit locally, one commit per item, Conventional Commits, no Claude trailers.
- **No push, no tag, no release, no merge into `dev` or `main`.** These need the owner. Only push
  if the plan says so in the owner's words.
- **Stop an item** at the first of these:
  - a failing test you can't explain;
  - a step that could lose data or touch the owner's accounts;
  - anything that writes to a sync server;
  - a new dependency that isn't approved;
  - any doubt about privacy.
  Note it in the report and move to the next item, if it doesn't depend on the stopped one.
- **Three tries, then revert:** if a fix still fails after 3 real attempts, revert the item's
  changes (`git restore` / `git reset` on its own branch only), write down what you tried and why
  it failed, and go on.
- **Tests and builds** follow CLAUDE.md:
  - targeted tests while working, the whole suite once at the end;
  - never tests and a build in one command;
  - dev builds for phone checks, one R8 release build at the end.
- **Phone:**
  - only the phone and the builds the plan allows;
  - Koan in front + a fresh screenshot before every input; if the owner is using the phone, stop
    all input;
  - only your own test tabs, closed afterwards; DND on during the run and put back at the end;
  - never uninstall or clear the real app (the safety hook blocks it). A signature mismatch on
    install means stop and note it.
- **Privacy (public repo):** no personal data in commits, the plan or the report. Write about
  "a slow page", not the owner's real sites or tabs. Screenshots and logs stay gitignored.
- **Secrets:** never log or print keys, tokens or passwords.

## Report
Add a `## Night report` section at the end of `docs/plans/NIGHT_PLAN.md` and commit it. For each
item give:
- its status (done / stopped / reverted) and the commit hash;
- what was tested, and how;
- what is untested;
- questions for the owner.

End with:
- the branch state (`git log --oneline` of the night);
- the exact commands the owner needs to review and merge;
- what the phone was left like.

Then update `docs/HANDOFF.md` "Where things stand".
