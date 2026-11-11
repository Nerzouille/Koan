# Night batch, 2026-11-10

An overnight batch of 13 items, built one feature branch per item from `dev`, merged into a test
branch, tested on the spare test phone (POCO X4 GT, Android 14), then reviewed item by item with
the owner on 2026-11-11 (verdicts and the exact specs of every fix: `docs/REVIEW_NIGHT.md`). Every
item kept is merged into `dev`; the video player waits for a design.

## Items

| # | Item | Branch | Verdict |
|---|---|---|---|
| 1 | History opened from a page: a tap opens the entry in that tab | `library` | fixed, kept |
| 2 | ⋯ › History and the overview's History are the same page | `library` | kept |
| 3 | "Open in app" links: a card before an app opens, ⋯ "Open in [app]" | `app-links` | kept |
| 4 | Page translation on the phone (Firefox Translations) | `translate` | fixed, kept |
| 5 | Back closing a tab: the page shrinks into a card and slides away | `back-close` | kept |
| 6 | A toast replacing another: hand-over animation | `toast-swap` | kept |
| 7 | One set of rules for History, Downloads, Passwords | `library` | fixed, kept |
| 8 | Passwords look; add a password by hand | `library` | fixed, kept |
| 9 | Space icons picked with the Zia mod | `zen-icons` | kept |
| 10 | Re-check of the 0.4 features | — | checked |
| 11 | Two-way sync for passwords and history (M6) | `sync-two-way` | kept |
| 12 | Card for a download of a file already there | `download-dup` | fixed, kept |
| 13 | Koan's own video controls, picture-in-picture | `video` | waits for a design |

## Notes per item

- **3.** A site's "Open app" button often sends an `intent://` pinned to one app. When that app is
  disabled or replaced by another one, Android can drop the start without an error; Koan now finds
  the app that really handles the link and offers it. Plain https links never get the card.
  Package visibility: targeted `<queries>` (web links and the app schemes sites use) instead of
  `QUERY_ALL_PACKAGES`, which android-components adds and Koan now removes from its manifest.
- **4.** android-components' `TranslationsMiddleware` needs a main-thread scope (`MainScope`).
  Gecko's own page state (`requestedTranslationPair`) says whether a page is translated: its
  processing flags can stay up after Show original then Translate again. Pages with too little text
  get no offer (no language detected). Model downloads are a new network call
  (`docs/PRIVACY.md`).
- **9.** Zia ≥ 2.58 stores Tabler icons (`resource://zia-tabler/<outline|filled>/<name>.svg`),
  older versions Phosphor names (mapped with Zia's own table). Koan bundles Zia's Tabler pack and
  table (MIT, about 0.3 MB, `THIRD_PARTY_NOTICES.md`); an unknown name takes the closest one by
  spelling. Zia's own SVG icons exist only on the computer.
- **11.** See `docs/HANDOFF.md` "Two-way sync". An encrypted backup of each collection is kept on
  the phone (and a copy outside the repo) before the first write.
- **12.** A store middleware (`DuplicateMiddleware`) catches every download before it starts, even
  the ones that skip the confirmation (Save image, a PDF viewer's Download). The link card, the
  download confirmation and the duplicate card are one floating card that morphs from one to the
  next.
- **13.** Tested on a local mp4 page; a site with its own player controls (YouTube) keeps them
  (`KoanRoot.ownPlayer`). The owner's review asks for a rebuild from a full design.

## Tests

- 269 JVM unit tests green on `dev` after the merge; the R8 release of `dev` built and launched on
  the test phone without a crash.
- The batch's test pages were removed from the account's history afterwards (tombstones through
  two-way sync).
