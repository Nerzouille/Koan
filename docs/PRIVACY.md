# Privacy notice

Koan is a personal browser with no account of its own, no analytics, no telemetry, no ads and no
crash reporting. Nothing about you or your browsing is sent to the people who make Koan.

## What stays on the phone

Your tabs, spaces, history, bookmarks, downloads, site permissions and settings live on the phone
only. The passwords and Mozilla account keys Koan keeps are encrypted with a key held by Android's
Keystore. Passwords you save in Koan stay on this phone and are never uploaded to your account.

While a page plays audio or video, its title and icon show in Android's media notification,
including on the lock screen. Links like `tel:` or `mailto:` are handed to the app you choose,
only when you tap them.

## The servers Koan talks to

Apart from the sites you open, Koan only connects to these:

| Server | When | What it gets |
|---|---|---|
| Mozilla accounts and Firefox Sync (`accounts.firefox.com`, `token.services.mozilla.com`, Sync storage) | When you sign in, and when Koan syncs | Your sign-in, then requests for your Spaces, passwords, history and bookmarks. Sync data is end-to-end encrypted: Mozilla can't read it. Koan only reads it. |
| Your search engine (DuckDuckGo, Google, Brave Search or Startpage) | When you search, and while you type if search suggestions are on | What you type in the search field, never a web address. Turn suggestions off in Settings. |
| DuckDuckGo's icon service (`icons.duckduckgo.com`) | When a site's own icon can't be found | The site's domain name only. Never for local network addresses. |
| GitHub (`github.com`) | Each time you open Settings › About Koan | A request for Koan's public releases feed, to tell you whether an update exists. Never in the background. |
| Mozilla Add-ons (`addons.mozilla.org`) | On first start, to install uBlock Origin, and for its updates | A normal download request. |

uBlock Origin then downloads its filter lists from its own sources, as it does in any browser.

GeckoView, Firefox's engine that Koan runs on, also uses Firefox's standard protection services,
such as the lists that block trackers and dangerous sites. These lists are downloaded to the phone;
the pages you visit are not sent to anyone.

## Deleting your data

- **Settings › Mozilla account › Sign out** removes your account's passwords, bookmarks and history
  from the phone.
- **History:** swipe a page left to remove it. **Clear** › **Clear browsing data** deletes the
  phone's history and cached files, and if you choose, every site's cookies and site data. History
  that came from your account is only hidden on the phone: Koan never changes your account.
- **Site settings › Clear site data** clears one site's cookies, storage and permission answers.
- Uninstalling Koan removes everything it stored.

Koan is not affiliated with, endorsed by or connected to Zen Browser or Mozilla.
