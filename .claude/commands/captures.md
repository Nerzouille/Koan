---
description: Rules for screenshots and screen recordings of Koan (README, tour, What's new, promo)
argument-hint: "[what to capture]"
---

The owner wants captures of: $ARGUMENTS

Before you take the first one, propose the shot list (screen, theme, which pages are open) and wait
for the owner's OK. These rules hold for every shot.

**Never on screen**
- The account email, anywhere. Settings and its account card show it, so crop or skip those.
  For a shot that needs the account card, use a temporary fake `you@example.com` in a build that
  is never committed.
- The Passwords, History and Mozilla account screens, and the sign-in flow.
- The owner's own tabs, spaces, folders or sites, unless the owner allowed it for this shot.
  Personal notifications, other apps' content, chats.

**What's on screen instead**
- Demo tabs only: public pages such as Wikipedia, Wikimedia Commons, the Koan GitHub repo or a
  project's public site, or local demo pages (`python3 -m http.server` + `adb reverse`). Vary the
  sites so it isn't all Wikipedia.
- Open the demo tabs yourself (`adb shell am start -a android.intent.action.VIEW -d <url> dev.nerzouille.koan`)
  and close them afterwards. Never close, move or change the owner's tabs.

**A clean phone**
- Status bar: turn Do Not Disturb on (`cmd notification set_dnd priority`) so no notification
  appears. Try demo mode (`settings put global sysui_demo_allowed 1` + the `com.android.systemui.demo`
  broadcasts) for a fixed clock, full battery and no extra icons. HyperOS ignores demo mode: then
  crop the status bar or cover it in post.
- Take screenshots from the main display (on the Xiaomi: `screencap -d <main display id>`, found
  with `dumpsys SurfaceFlinger --display-id`).
- Recordings: Show taps on before the take (the owner's rule for the tour); injected touches don't
  show, so draw the dots in post or with a temporary in-app overlay that is never committed.
- Afterwards, put everything back as it was: DND, demo mode, Show taps, theme, test tabs closed,
  downloads removed.

**Check every image and every frame**
- Before you keep a screenshot or clip, look at it yourself: no email, no private tab, no
  notification, no other app.
- A frame that breaks a rule is deleted, not described. If you saw private content, delete the
  file and don't mention what it showed.
- Driving the phone follows CLAUDE.md "Driving a phone with adb": Koan in front and a fresh
  screenshot before every tap. If the owner picks up the phone, stop.

**Where files go**
- Raw files stay in gitignored folders (`reference/captures-*/`, `design-export/`, `app/build/`)
  and are never committed.
- Only final images the owner approved go to `docs/media/`, and only after the owner has seen the
  full set.
- Show the owner a contact sheet of the set before anything is committed.
