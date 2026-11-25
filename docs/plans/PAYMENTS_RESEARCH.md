# Google Pay / Revolut Pay checkouts in Koan (research, 2026-11-26)

Owner's note: Google Pay and Revolut Pay checkouts "don't work well" in Koan (popups, coming back
from the app, Payment Request API). Research only: what happens, why, and what Koan could fix.
Nothing built yet. Checked in GeckoView 160's own files (omni.ja prefs, classes) and Koan's code;
the parts marked **verify** need a phone run on a real checkout (the owner's hands: a real
payment account).

## 1. Payment Request API: missing, and Koan can't add it

- `window.PaymentRequest` doesn't exist in GeckoView 160: `dom.payments.request.enabled` is off
  by default (it's a static pref; omni.ja only ships `dom.payments.request.supportedRegions`), and
  GeckoView has no payment delegate for an app to show the sheet or talk to a wallet. Firefox for
  Android is in the same place.
- Effect: checkouts that offer Google Pay through the Payment Request API (Stripe's and Adyen's
  "Payment Request Button", many shop themes) check `PaymentRequest` / `canMakePayment()` first
  and simply **hide the Google Pay button** in Koan. That's "Google Pay isn't there".
- Turning the pref on would expose an API with nothing behind it (no UI, no wallet): pages would
  find it and then fail. **Not fixable in Koan** short of building a payment handler on Android's
  side, which GeckoView doesn't support.

## 2. Google Pay without Payment Request: a popup

- Google's own `pay.js` falls back, in browsers without Payment Request, to a **popup window** on
  pay.google.com that talks back to the checkout through `window.opener` / `postMessage`, then
  closes itself.
- In Koan, `window.open` from a tap opens a new tab next to its opener (`TabManager`,
  `adoptWindowTab`), with the opener link kept (Android Components' `WindowFeature` in
  `MainActivity`), and `window.close()` closes it. A popup opened **after an `await`** (no longer
  in the tap) is blocked by Gecko; Koan then asks "… wants to open a pop-up" (`PagePrompts`), and
  Allow opens it.
- Likely troubles (**verify**):
  - the popup is a full tab, not a small window over the checkout: it looks like the shop went
    away, and after it closes, Koan may show another tab than the checkout (which tab is selected
    after `window.close()` depends on Koan's tab ordering);
  - the user isn't signed in to Google in Koan, so the popup first asks for a Google sign-in
    (Chrome has the phone's account; Koan doesn't);
  - Koan's tracking protection (recommended categories, with Total Cookie Protection) partitions
    third-party storage: an embedded payment iframe (js.stripe.com…) talking to pay.google.com
    may see its storage split. Usually fine for the popup itself (first party).
- **Could fix:**
  - after a `window.close()`, always go back to the opener tab (check `WindowFeature` + Koan's
    selection; small);
  - show a page's popup as a sheet over its opener instead of a full tab (a "popup card": the
    child session in a `KoanCard`, closed with the popup). Medium, and the same card would help
    OAuth "Sign in with…" popups;
  - a per-site tracking-protection exception from the ⋯ menu for a checkout that breaks (Gecko
    has the API; medium).

## 3. Revolut Pay (and bank apps): leaving for the app and coming back

- On a phone, Revolut Pay (and most bank 3-D Secure checks) send you to the Revolut / bank app
  through an app link or an `intent://` URL. Koan hands these to the app (`ExternalLinks`, with
  the `browser_fallback_url` when the app isn't there): that part works.
- Coming back is the problem: when the payment is done, the app opens the shop's **return URL**
  with a plain `ACTION_VIEW`. Koan treats it as a link from another app: a **new tab** in the
  current space (`MainActivity.handleIntent`), and Back on its first page closes it and returns to
  that app (`ReturnToApp`). Meanwhile the checkout tab is still there, waiting.
  - A shop that keeps the order in `sessionStorage` (per tab) or polls from the checkout page sees
    nothing in the new tab: "payment pending", or a second order page.
  - The user sees two tabs of the same shop, and Back takes them out of Koan.
- **Could fix (recommended first):** remember the tab that sent a link to an app
  (`ExternalLinks.openIn` / the intent launch, with the time), and when a link comes back from an
  app within a few minutes **for the same site** (registrable domain) as that tab, open it **in
  that tab** instead of a new one, and don't arm `ReturnToApp` for it. Small, unit-testable
  (`returnTab(sent, incoming, now)`), and it's what Chrome effectively does for Custom-Tab-less
  returns. Needs a real checkout to confirm (**verify**: which URL the app opens, and whether the
  shop expects the same tab).

## 4. Summary

| Problem | Cause | Fixable in Koan? |
|---|---|---|
| No Google Pay button | No Payment Request API in GeckoView | No |
| Google Pay popup feels broken | Popup is a full tab; Google sign-in; maybe the wrong tab after close | Yes, partly: back to opener (small), popup card (medium) |
| Revolut / bank app returns into a new tab | Return URL arrives as a link from another app | Yes: reuse the tab that sent the link (small) |
| A payment iframe can't keep its session | Total Cookie Protection | Per-site exception (medium) |

Suggested order after beta.2: the return-to-the-same-tab fix, then back-to-opener after
`window.close()`, then the popup card. Each needs the owner's phone run on a real checkout.
