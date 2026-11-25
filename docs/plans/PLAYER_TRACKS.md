# Player: captions, audio tracks, quality (research, 2026-11-20)

What Koan's video player can really offer for captions, audio track choice and playback quality,
for the two players it has:

- **Web:** Koan's controls over a page's fullscreen `<video>`. The video extension
  (`assets/extensions/video/content.js`) hides the site's player except the video and drives the
  element (play, pause, seek, rate, mute). YouTube is left out entirely (manifest
  `exclude_matches`): its own controls stay, so none of the three applies there.
- **Standalone:** files from Downloads and video links, played by Android's `MediaPlayer` on a
  `TextureView` (`ui/player/NativeVideo.kt`).

The extension sees the page's DOM and the `<video>` element, not the site's player objects. A
content script can reach page globals through `window.wrappedJSObject` (Gecko's Xray waiver), but
hls.js / dash.js / video.js instances are rarely globals. Hooking them (wrapping
`Hls.prototype.attachMedia` from a page script) would be fragile and could break sites: not
proposed.

Checked: a public Commons file page (mobile user agent, 2026-11-20). Not checked yet on a phone:
everything marked **verify**.

## 1. Captions / subtitles

**API:** `video.textTracks`, the standard track list. Gecko supports it fully, with no pref needed.
For each track, `kind` (subtitles / captions), `label`, `language` and `mode`. With
`mode = "hidden"`, Gecko loads the cues and fires `cuechange` with `activeCues`, but draws nothing.
Koan then sends the cue text to the app and draws it itself, above the bottom pill when the
controls show. With `"showing"`, Gecko draws cues itself, inside the video's box, under Koan's
controls. A track in `"disabled"` mode never loads its cues.

| Site | Works? | How |
|---|---|---|
| Plain file with `<track>` | Yes | `textTracks` lists them. |
| Wikimedia Commons | Likely, with a fallback | The page markup has one `<track kind="subtitles" type="text/vtt">` per language (31 on a well-known demo file), from `/w/api.php?action=timedtext&title=File:…&lang=xx&trackformat=vtt`, same origin. The page's player (video.js, `ext.tmh.player`) draws captions in its own overlay, which Koan hides, so **today captions vanish in Koan's fullscreen there**. **Verify** whether the player leaves the `<track>` elements on the `<video>`; video.js removes them when it emulates text tracks. If they're gone: copy them back from the page's markup (another `<video>` with the same `data-mwtitle`), or build them from that `timedtext` address and the video's `data-mwtitle`. |
| hls.js sites | Yes, usually | hls.js draws subtitles natively by default (`renderTextTracksNatively`): it adds tracks with `addTextTrack` and fills their cues, CEA-608 captions included. A site that turns that off and draws its own captions exposes nothing. |
| dash.js sites | Yes, usually | dash.js puts WebVTT / TTML into native text tracks too. Same exception for custom rendering. |
| Vimeo | **Verify** | Draws captions in its own overlay; whether its `<video>` also carries `<track>`s is unknown. If not, no captions in Koan's player (the site's controls still have them). |
| YouTube | No | Not Koan's player. YouTube draws captions in its own DOM; there's no `textTracks`. |

**What we can't do:** captions burned into the video; captions a player draws from its own data
without native tracks; a track whose file is on another site without CORS (the page's own problem:
it fails for the site too).

**Standalone:** `MediaPlayer.getTrackInfo()` lists in-band timed text (3GPP text in MP4); select it
with `selectTrack` and draw it from `OnTimedTextListener`. A sidecar `.srt` can be added with
`addTimedTextSource(…, MEDIA_MIMETYPE_TEXT_SUBRIP)`. Subtitle streams in MKV (SRT / ASS) are poorly
supported by the platform player. Downloaded web videos rarely have either. Later, not beta.2.

## 2. Audio track choice

**API:** `video.audioTracks` exists in Gecko only behind the pref `media.track.enabled`, which is off
in Firefox release (`audioTracks` / `videoTracks` are undefined). Koan could turn it on for every
page (`GeckoPreferenceController`, like the find prefs in `engine/GeckoPrefs.kt`). Even then:

- It only lists the tracks inside one plain file (an MP4 / WebM with several audio tracks), which
  is rare on the web. **Verify** that switching `enabled` really changes the sound in Gecko:
  Firefox never shipped it, so the switch may be incomplete.
- It exposes an API Firefox doesn't, on every page: sites that sniff it could behave differently.

| Site | Works? | Why |
|---|---|---|
| Plain files | Rarely useful | Needs the pref; multi-audio files are rare; switching unverified. |
| Commons | No | One audio track per rendition. |
| hls.js / dash.js | No | The player picks the audio rendition (`hls.audioTracks`, dash.js `setCurrentTrack`) and feeds it through Media Source Extensions (MSE); `video.audioTracks` never sees it. Reaching the player object isn't reliable (see above). |
| Vimeo, YouTube | No | Their own menu: ⋯ › Use the site's controls (Vimeo); YouTube keeps its controls. |

**Standalone:** `MediaPlayer.getTrackInfo()` with `MEDIA_TRACK_TYPE_AUDIO`, then `selectTrack(index)`.
This works for local MP4 / MKV files with several audio tracks, which are the files where a choice
matters. Later, not beta.2.

## 3. Playback quality

**API:** the page's own plain files. When the `<video>` plays a plain file (`currentSrc` is an http
address, not `blob:`) and the page offers other renditions of the same video, Koan sets `v.src` to
the chosen file, then puts back the time, the speed and the play / pause state. Expect a short
rebuffer. The candidates are the ones `fileSource` already finds for Download video: the video's
`<source>`s, the page's renditions of one video (one folder), MediaWiki's original. They come with
`data-height` / `data-width` (Commons) and sizes from a HEAD request.

| Site | Works? | Why |
|---|---|---|
| Plain files with several `<source>`s | Yes | Switch `src`. |
| Commons | **Verify** | The markup offers 240p, 480p and 1080p WebM, a 360p MP4, the original and an adaptive DASH `.mpd` (listed first). If Gecko plays the DASH stream (`currentSrc` is `blob:`), quality is the player's job and switching `src` would pull the video out from under video.js. Then Commons only gets the Download video list. If it plays a WebM, switching works. |
| hls.js / dash.js | No | Adaptive streams over MSE: the renditions live in the player (`hls.levels` / `currentLevel`, dash.js ABR settings), not in the page. ⋯ › Use the site's controls. |
| Vimeo | No | Its own adaptive player; its own quality menu. |
| YouTube | No | Not Koan's player. |

**Shared with "Download video" quality:** the same candidate list. Download video can always offer
every file, even when playback is a stream (Commons with DASH): saving a file doesn't touch the
player.

## Approved scope for 1.0.0-beta.2 (owner, 2026-11-20)

Every row shows only when there is a real choice (two or more options, or Off plus at least one
track).

- **Web player:**
  - **⋯ › Captions** from `video.textTracks`; Koan draws the cue.
  - **⋯ › Quality** from the page's plain files, shared with the Download video quality list.
  - **No audio tracks on the web.**
- **Standalone player (Downloads and phone files), in beta.2, not later:**
  - **⋯ › Audio** and **⋯ › Subtitles** for embedded tracks, through `MediaPlayer.getTrackInfo()` /
    `selectTrack()` (audio) and timed text with `OnTimedTextListener` (subtitles).
  - **A subtitle file next to the video**, same name, `.srt` or `.vtt`, added with
    `addTimedTextSource`. MediaPlayer reads SubRip only, so a `.vtt` is converted to SubRip first
    (a small converter, unit tested).
  - **Open point for the owner (found 2026-11-20, scoped storage):** Koan has no storage
    permission, so on Android 11+ it can read a file next to the video only when:
    - Koan downloaded that subtitle file itself (it owns it in the Download folder);
    - or the video sits in the download folder picked in Settings (Koan holds that folder's grant).

    A subtitle file another app put in the Download folder can't be read. A video opened from
    another app (a `content://` address) gives no access to its folder at all. To cover those, the
    options are:
    - **A, ⋯ › Subtitles › "Choose a file…"** with the system file picker (`ACTION_OPEN_DOCUMENT`,
      no permission). Recommended.
    - **B, all-files access** (`MANAGE_EXTERNAL_STORAGE`). Not recommended: a broad permission for
      a small feature.
    - **C, only the cases above.**

    Decide before coding.

**Checks before coding (POCO, staging build, with the beta.2 bug checks):** the three below. Results
go here, then wait for the owner's OK.

## Proposal for 1.0.0-beta.2 (first version, 2026-11-20)

1. **Captions (web): ⋯ › Captions,** Off plus each subtitle / caption track (label, else language
   name), shown only when the video has tracks. The chosen track is set to `hidden`; the extension
   sends the active cue text (markup stripped) on `cuechange`. Koan draws it centred near the
   bottom, white on a dark scrim, lifted above the bottom pill while the controls show. The track
   the page had on stays on; the last language picked is remembered until Koan restarts. Commons
   fallback as above if its player removed the `<track>`s.
2. **Quality: one candidate list from `fileSource`,** with height, type and size. Used by:
   - **Download video** (the existing beta.2 item): a short list, one row per file, the original
     marked.
   - **⋯ › Quality** in the web player, shown only when the video plays a plain file and there are
     at least two playable renditions (same codec family as the one playing, so the phone can
     decode it).
3. **Not in beta.2:** audio tracks on the web (needs a pref Firefox doesn't ship, rare files, the
   switch unverified); the standalone player's tracks (in-band captions and audio via `MediaPlayer`):
   a small later item; anything on hls.js / dash.js / Vimeo beyond captions (⋯ › Use the site's
   controls covers it). *(Superseded: the owner put the standalone player in beta.2, see above.)*

**Checks before building (POCO, staging build), results to fill in:**
- Does Commons play the DASH `.mpd` or a WebM in GeckoView (`currentSrc`)? *Result: pending
  (night 2026-11-25: not run, the phone's network failed). The Quality row is guarded at run time
  (shown only for a plain file), so either answer is safe.*
- Does its player leave the `<track>`s on the `<video>`? *Result: pending (not run). If not, the
  fallback (rebuild them from the page's markup) is still to build.*
- Do Vimeo videos carry `<track>`s? *Result: pending (not run).*

Built on 2026-11-25 (night, `beta2-bugs`): web Captions, web Quality + the Download video file
choice, standalone Audio / Subtitles with side files and Choose a file…. See BETA2_BUGS.md ›
Night report 2026-11-25.
