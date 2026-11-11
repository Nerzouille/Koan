# Privacy notice

Koan is a personal browser with no account of its own, no analytics, no telemetry, no ads and no
crash reporting. Nothing about you or your browsing is sent to the people who make Koan.

## What stays on the phone

Your tabs, spaces, history, bookmarks, downloads, site permissions and settings live on the phone
only. The passwords and Mozilla account keys Koan keeps are encrypted with a key held by Android's
Keystore. Passwords you save in Koan stay on this phone unless you turn on two-way sync for
passwords (Settings › Mozilla account): then they are added to your account, end-to-end encrypted
like the rest of Firefox Sync. History goes back to your account only if you turn that on too.

Translation runs on the phone (Firefox Translations): the text of a page is never sent anywhere.

When you tap **Save image** on a picture that is still a small preview, a small part of Koan
built into the engine looks at the page where you long-pressed to find the full image. It only
remembers the spot of your last long-press, in that page, and reads nothing before you tap Save.

The Passwords list shows a site's icon only if Koan already has it from your tabs or history; it
never downloads an icon for that list, so no site and no icon service learns which sites you have
saved logins for. (A test build on 2026-11-10 did ask DuckDuckGo's icon service once for those
icons; that build was never released.)

While a page plays audio or video, its title and icon show in Android's media notification,
including on the lock screen. Links like `tel:` or `mailto:` are handed to the app you choose,
only when you tap them.

When you open the ⋯ menu, Koan asks the page on screen whether it offers a search engine (the
"Add this search engine" row). Only then, and only if the page declares one, its description is
downloaded from that same site, as the page itself would. Nothing is sent anywhere else, and
nothing happens on pages you only browse.

## The servers Koan talks to

Apart from the sites you open, Koan only connects to these:

| Server | When | What it gets |
|---|---|---|
| Mozilla accounts and Firefox Sync (`accounts.firefox.com`, `token.services.mozilla.com`, Sync storage) | When you sign in, and when Koan syncs | Your sign-in, then requests for your Spaces, passwords, history and bookmarks, and (only with two-way sync on) the passwords and history Koan sends back. Sync data is end-to-end encrypted: Mozilla can't read it. |
| Your search engine (DuckDuckGo, Google, Brave Search, Startpage, Baidu, or an engine you added yourself) | When you search, and while you type if search suggestions are on | What you type in the search field, never a web address. Turn suggestions off in Settings. An engine you add gets your searches (and, if you gave it a suggestions address, what you type) exactly like the built-in ones: only add engines you trust. |
| DuckDuckGo's icon service (`icons.duckduckgo.com`) | When a site's own icon can't be found | The site's domain name only. Never for local network addresses. |
| Mozilla's translation models (Firefox's Remote Settings servers) | The first time you translate from or to a language | A normal download of that language's model, about 20–30 MB. The page's text is not sent. |
| GitHub (`github.com`) | Each time you open Settings › About Koan | A request for Koan's public releases feed, to tell you whether an update exists. Never in the background. |
| Mozilla Add-ons (`addons.mozilla.org`) | On first start, to install uBlock Origin, and for its updates | A normal download request. |
| Mozilla's DNS over HTTPS resolver (`mozilla.cloudflare-dns.com`, run by Cloudflare) | Only if you turn on Settings › DNS over HTTPS (off by default) | The names of the sites Koan looks up (instead of your network's DNS server). |

A link to a video file (`…/clip.mp4`) that another app opens with Koan first gets a short `HEAD`
request to that same address, to ask whether it really is a video (Koan's player) or a page (a
tab). It goes to the site you opened, nowhere else.

For the media notification, the poster image a page gives its video (when the page sets no
artwork of its own) is downloaded once from the address the page names, like the page would.

uBlock Origin then downloads its filter lists from its own sources, as it does in any browser.

GeckoView, Firefox's engine that Koan runs on, also uses Firefox's standard protection services,
such as the lists that block trackers and dangerous sites. These lists are downloaded to the phone;
the pages you visit are not sent to anyone.

## Deleting your data

- **Settings › Mozilla account › Sign out** removes your account's passwords, bookmarks and history
  from the phone.
- **History:** swipe a page right to left to remove it. **Clear** › **Clear browsing data** deletes the
  phone's history and cached files, and if you choose, every site's cookies and site data. Clear
  browsing data never deletes your account's history. Removing single pages from History removes
  them from your account too, but only while two-way history sync is on.
- **Translation languages:** Settings › Translation › On this phone › Delete.
- **Site settings › Clear site data** clears one site's cookies, storage and permission answers.
- Uninstalling Koan removes everything it stored.

Koan is not affiliated with, endorsed by or connected to Zen Browser or Mozilla.
