/*
 * Koan's video player over a page's fullscreen video, and the media notification's timeline
 * (engine/PageVideo.kt).
 *
 * - Any video that plays: its position, length, speed and poster go to Koan when they change
 *   (play, pause, seek, speed), so the media notification and the lock screen show a moving
 *   timeline even on pages that never tell the browser.
 * - When this frame puts an element holding a <video> fullscreen, Koan takes over: everything of
 *   the site's player but the video is hidden (its buttons, its end screen), the video's native
 *   controls are off, and the page can no longer leave fullscreen on its own (some players do at
 *   the end of the video): only Koan leaves it. The video's state goes to Koan a few times a
 *   second, and Koan's buttons come back as commands (play, pause, seek, speed, mute, captions,
 *   quality).
 * - Captions: the video's subtitle / caption tracks (video.textTracks). The chosen one is set to
 *   "hidden" (Gecko loads its cues but draws nothing) and its current text goes to Koan, which
 *   draws it above its own controls. The page's own track modes come back on release.
 *
 * Bump the manifest version whenever this file changes, or Gecko keeps the old one.
 */
"use strict";

const STYLE_ID = "koan-video-style";
const HIDE_CLASS = "koan-video-host";
const VIDEO_CLASS = "koan-video";

let port = null;
let video = null; // the fullscreen <video> Koan drives
let host = null; // the element the page put fullscreen (a container, or the video itself)
let hadControls = false;
let timer = 0;

function connect() {
  if (port) return port;
  port = browser.runtime.connectNative("koanVideo");
  port.onMessage.addListener(onCommand);
  port.onDisconnect.addListener(() => { port = null; release(); });
  return port;
}

function send(message) {
  try { connect().postMessage(message); } catch (e) { /* page going away */ }
}

// ---- the media notification's timeline ----

let lastMedia = 0;

function absolute(url) {
  try { return url ? new URL(url, document.baseURI).href : ""; } catch (e) { return ""; }
}

function reportMedia(v) {
  const d = v.duration;
  if (!isFinite(d) || d <= 0) return;
  lastMedia = Date.now();
  send({
    type: "media",
    time: v.currentTime || 0,
    duration: d,
    rate: v.playbackRate || 1,
    paused: v.paused || v.ended,
    poster: /^https?:/.test(absolute(v.poster)) ? absolute(v.poster) : "",
  });
}

const MEDIA_EVENTS = ["play", "playing", "pause", "seeked", "ratechange", "durationchange", "ended"];
for (const e of MEDIA_EVENTS) {
  document.addEventListener(e, ev => {
    if (ev.target instanceof HTMLVideoElement) reportMedia(ev.target);
  }, true);
}

// A playing video drifts a little from the timeline the system draws: a correction now and then.
setInterval(() => {
  if (Date.now() - lastMedia < 10000) return;
  for (const v of document.querySelectorAll("video")) {
    if (!v.paused && !v.ended) { reportMedia(v); break; }
  }
}, 10000);

// ---- the fullscreen player ----

/** The video inside [el] Koan should drive: the playing one, else the largest. */
function pickVideo(el) {
  if (el instanceof HTMLVideoElement) return el;
  const all = Array.from(el.querySelectorAll("video"));
  if (all.length === 0) return null;
  const playing = all.filter(v => !v.paused && !v.ended);
  const pool = playing.length ? playing : all;
  let best = null;
  let area = -1;
  for (const v of pool) {
    const r = v.getBoundingClientRect();
    const a = r.width * r.height;
    if (a > area) { area = a; best = v; }
  }
  return best;
}

function ensureStyle() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  // The host keeps its box (the video is laid out inside it) but nothing in it shows but the
  // video. visibility, not display: the site's layout and scripts don't notice.
  style.textContent = `
    .${HIDE_CLASS}, .${HIDE_CLASS} * { visibility: hidden !important; }
    .${HIDE_CLASS}.${VIDEO_CLASS}, .${HIDE_CLASS} video.${VIDEO_CLASS} {
      visibility: visible !important;
      object-fit: contain !important;
    }
    .${HIDE_CLASS}::backdrop { background: #000 !important; }
    .${HIDE_CLASS} { background: #000 !important; }
  `;
  (document.head || document.documentElement).appendChild(style);
}

// While Koan drives the video, the page's own exit calls do nothing (Koan leaves through Gecko).
const EXITS = ["exitFullscreen", "webkitExitFullscreen", "mozCancelFullScreen", "webkitCancelFullScreen"];
let exitsBlocked = false;

function blockExits(on) {
  if (on === exitsBlocked) return;
  exitsBlocked = on;
  const page = document.wrappedJSObject;
  for (const name of EXITS) {
    try {
      if (on) {
        exportFunction(function () { return new page.defaultView.Promise(r => r()); }, document, { defineAs: name });
      } else {
        delete page[name];
      }
    } catch (e) { /* not there */ }
  }
}

function take(el) {
  const v = pickVideo(el);
  if (!v) return;
  if (video && video !== v) release();
  video = v;
  host = el;
  ensureStyle();
  host.classList.add(HIDE_CLASS);
  video.classList.add(VIDEO_CLASS);
  hadControls = hadControls || video.hasAttribute("controls");
  video.removeAttribute("controls");
  blockExits(true);
  for (const e of EVENTS) video.addEventListener(e, report, true);
  takeCaptions(video);
  clearInterval(timer);
  timer = setInterval(report, 250);
  report();
}

/** Gives the page its player back. */
function release() {
  clearInterval(timer);
  timer = 0;
  releaseCaptions();
  blockExits(false);
  if (host) host.classList.remove(HIDE_CLASS);
  if (video) {
    video.classList.remove(VIDEO_CLASS);
    for (const e of EVENTS) video.removeEventListener(e, report, true);
    if (hadControls) video.setAttribute("controls", "");
  }
  hadControls = false;
  video = null;
  host = null;
}

const EVENTS = ["play", "pause", "playing", "waiting", "seeking", "seeked", "ended", "ratechange", "volumechange", "durationchange", "loadedmetadata", "emptied"];

function bufferedEnd(v) {
  const t = v.currentTime;
  const b = v.buffered;
  for (let i = 0; i < b.length; i++) {
    if (b.start(i) <= t + 0.5 && b.end(i) >= t) return b.end(i);
  }
  return t;
}

function report() {
  const v = video;
  if (!v) return;
  if (!v.isConnected) {
    send({ type: "gone" });
    release();
    return;
  }
  const d = v.duration;
  const src = v.currentSrc || v.src || "";
  send({
    type: "state",
    fullscreen: document.fullscreenElement != null,
    paused: v.paused,
    ended: v.ended,
    // Stalled: wants to play but has no data at the current time.
    waiting: !v.paused && !v.ended && (v.readyState < 3 || v.seeking),
    time: v.currentTime || 0,
    duration: isFinite(d) ? d : 0,
    buffered: bufferedEnd(v),
    rate: v.playbackRate || 1,
    muted: v.muted || v.volume === 0,
    width: v.videoWidth || 0,
    height: v.videoHeight || 0,
    title: titleOf(v),
    src: /^https?:/.test(src) ? src : "",
    // Why the file can't be saved, for the player's "Download video" (protected, streamed, live).
    drm: v.mediaKeys != null,
    kind: src.startsWith("blob:") || src.startsWith("mediasource:") ? "stream"
      : /\.(m3u8|mpd)([?#]|$)/i.test(src) ? "stream" : "",
    live: d === Infinity,
    // A streamed video's page often lists plain files too (<source>): the largest one, to download.
    file: fileSource(v),
    // Captions: the list only when it changed (it can grow while the video plays).
    tracks: tracksChanged(v),
    track: chosen,
    files: filesChanged(v),
  });
}

// ---- captions ----

let chosen = -1; // index in captionTracks(video), -1: off
let chosenTrack = null;
let savedModes = null; // the page's own modes, put back on release
let lastTracks = null;
let lastCue = null;

function captionTracks(v) {
  try {
    return Array.from(v.textTracks || []).filter(t => t.kind === "subtitles" || t.kind === "captions");
  } catch (e) { return []; }
}

/** The track list, when it differs from the last one sent; undefined otherwise (not sent). */
function tracksChanged(v) {
  const list = captionTracks(v).map(t => ({ kind: t.kind, label: t.label || "", lang: t.language || "" }));
  const key = JSON.stringify(list);
  if (key === lastTracks) return undefined;
  lastTracks = key;
  return list;
}

function cueText(t) {
  const out = [];
  const cues = t && t.activeCues;
  if (!cues) return "";
  for (let i = 0; i < cues.length; i++) {
    const c = cues[i];
    let text = "";
    try { text = c.getCueAsHTML ? c.getCueAsHTML().textContent : c.text; } catch (e) { text = c.text || ""; }
    if (text) out.push(text.trim());
  }
  return out.join("\n");
}

function sendCue() {
  const text = chosenTrack ? cueText(chosenTrack) : "";
  if (text === lastCue) return;
  lastCue = text;
  send({ type: "cue", text });
}

/** Shows track [i] of the caption list through Koan (-1: none); the others stop loading. */
function chooseCaption(i) {
  const list = captionTracks(video);
  if (chosenTrack) chosenTrack.removeEventListener("cuechange", sendCue);
  chosenTrack = i >= 0 && i < list.length ? list[i] : null;
  chosen = chosenTrack ? i : -1;
  list.forEach((t, j) => {
    try { t.mode = j === chosen ? "hidden" : "disabled"; } catch (e) {}
  });
  if (chosenTrack) chosenTrack.addEventListener("cuechange", sendCue);
  sendCue();
}

function takeCaptions(v) {
  lastTracks = null;
  lastFiles = null;
  lastCue = null;
  const list = captionTracks(v);
  savedModes = list.map(t => t.mode);
  // The track the page had on stays on, drawn by Koan.
  const on = list.findIndex(t => t.mode === "showing");
  chosen = -1;
  chosenTrack = null;
  if (on >= 0) chooseCaption(on);
}

function releaseCaptions() {
  if (chosenTrack) chosenTrack.removeEventListener("cuechange", sendCue);
  if (video && savedModes) {
    const list = captionTracks(video);
    if (list.length === savedModes.length) list.forEach((t, j) => { try { t.mode = savedModes[j]; } catch (e) {} });
  }
  savedModes = null;
  chosenTrack = null;
  chosen = -1;
  lastTracks = null;
  lastCue = null;
}

// A video file the page links (a "Download" or "Original file" link) whose name the video itself
// carries in its attributes or its player's (poster, data-*): the same video, as a plain file.
function linkedFile(v) {
  const words = [];
  for (let e = v, i = 0; e && i < 4; e = e.parentElement, i++) {
    for (const a of e.attributes || []) words.push(a.value);
  }
  const hay = words.join(" ");
  for (const a of document.querySelectorAll("a[href]")) {
    const u = a.href.split(/[?#]/)[0];
    if (!/^https?:/.test(u)) continue;
    const raw = u.substring(u.lastIndexOf("/") + 1);
    if (!/\.(mp4|webm|m4v|mov|mkv)$/i.test(raw)) continue;
    let name = raw;
    try { name = decodeURIComponent(raw); } catch (e) {}
    if (hay.includes(raw) || hay.includes(name)) return u;
  }
  return "";
}

function plainSource(s) {
  const u = s.src || "";
  const t = (s.type || "").toLowerCase();
  if (!/^https?:/.test(u)) return false;
  if (/dash|mpegurl|m3u8/.test(t) || /\.(m3u8|mpd)([?#]|$)/i.test(u)) return false;
  return !t || t.startsWith("video/");
}

function sourceHeight(s) {
  return parseInt(s.getAttribute("data-height") || s.getAttribute("height") || "0", 10) || 0;
}

/** The video's plain <source>s, else the page's copies of the same video (one folder). */
function plainSources(v) {
  let list = [...v.querySelectorAll("source")].filter(plainSource);
  if (!list.length) {
    // A streaming player that took its <source>s away: the page's other copies of the same video,
    // only when they are all renditions of one video (one folder).
    const all = [...document.querySelectorAll("video source")].filter(plainSource);
    const dirs = new Set(all.map((s) => s.src.substring(0, s.src.lastIndexOf("/"))));
    list = dirs.size === 1 ? all : [];
  }
  return list;
}

// MediaWiki (Commons, Wikipedia): renditions sit in ".../transcoded/<x>/<xy>/<File>/"; the
// original file, when the page links it.
function linkedOriginal(rendition) {
  const original = rendition.replace(/\/transcoded\/(.+)\/([^/]+)\/[^/]+$/, "/$1/$2");
  if (original === rendition) return "";
  const linked = [...document.querySelectorAll("a[href]")].some((a) => a.href.split(/[?#]/)[0] === original);
  return linked ? original : "";
}

function fileSource(v) {
  let best = "";
  let bestH = -1;
  for (const s of plainSources(v)) {
    const h = sourceHeight(s);
    if (h > bestH) { best = s.src; bestH = h; }
  }
  if (!best) return linkedFile(v);
  // The original file, when the page links it, is the one to save.
  return linkedOriginal(best) || best;
}

/**
 * Every plain file of this video (its renditions and the original): Download video's choice and,
 * when the video plays a plain file, ⋯ › Quality. One list for both.
 */
function videoFiles(v) {
  const out = [];
  const seen = new Set();
  for (const s of plainSources(v)) {
    if (seen.has(s.src)) continue;
    seen.add(s.src);
    out.push({ src: s.src, height: sourceHeight(s), type: (s.type || "").toLowerCase(), original: false });
  }
  const original = out.length ? linkedOriginal(out[0].src) : "";
  if (original && !seen.has(original)) out.push({ src: original, height: 0, type: "", original: true });
  return out;
}

let lastFiles = null;

/** The file list, when it differs from the last one sent; undefined otherwise (not sent). */
function filesChanged(v) {
  const list = videoFiles(v);
  const key = JSON.stringify(list);
  if (key === lastFiles) return undefined;
  lastFiles = key;
  return list;
}

/** ⋯ › Quality: plays [src] (one of the video's files) from the same time, speed and play state. */
function setSource(src) {
  const v = video;
  if (!v || !videoFiles(v).some((f) => f.src === src)) return;
  const t = v.currentTime;
  const rate = v.playbackRate;
  const playing = !v.paused && !v.ended;
  const restore = () => {
    v.removeEventListener("loadedmetadata", restore);
    try { v.currentTime = t; } catch (e) {}
    v.playbackRate = rate;
    if (playing) v.play().catch(() => {});
  };
  v.addEventListener("loadedmetadata", restore);
  v.src = src;
}

function titleOf(v) {
  const t = (v.getAttribute("title") || v.getAttribute("aria-label") || "").trim();
  if (t) return t;
  try { return (window.top === window ? document.title : "") || ""; } catch (e) { return ""; }
}

function onCommand(m) {
  const v = video;
  switch (m.action) {
    case "play": if (v) { if (v.ended) v.currentTime = 0; v.play().catch(() => {}); } break;
    case "pause": if (v) v.pause(); break;
    case "seek": if (v && isFinite(m.time)) v.currentTime = Math.max(0, m.time); break;
    case "rate": if (v && m.rate > 0) v.playbackRate = m.rate; break;
    case "mute": if (v) { v.muted = !!m.muted; if (!m.muted && v.volume === 0) v.volume = 1; } break;
    case "caption": if (v) chooseCaption(m.index); break;
    case "source": if (v && typeof m.src === "string") setSource(m.src); break;
    case "release": release(); break;
  }
  report();
}

document.addEventListener("fullscreenchange", () => {
  const el = document.fullscreenElement;
  if (el) {
    if (el instanceof HTMLIFrameElement) return; // the frame inside handles its own video
    take(el);
  } else if (video) {
    // Fullscreen ended (Koan left it): the page gets its player back.
    report();
    release();
  }
}, true);
