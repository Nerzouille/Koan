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
 *   second, and Koan's buttons come back as commands (play, pause, seek, speed, mute).
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
  clearInterval(timer);
  timer = setInterval(report, 250);
  report();
}

/** Gives the page its player back. */
function release() {
  clearInterval(timer);
  timer = 0;
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
  });
}

function fileSource(v) {
  let best = "";
  let bestH = -1;
  for (const s of v.querySelectorAll("source")) {
    const u = s.src || "";
    const t = (s.type || "").toLowerCase();
    if (!/^https?:/.test(u)) continue;
    if (/dash|mpegurl|m3u8/.test(t) || /\.(m3u8|mpd)([?#]|$)/i.test(u)) continue;
    if (t && !t.startsWith("video/")) continue;
    const h = parseInt(s.getAttribute("data-height") || s.getAttribute("height") || "0", 10) || 0;
    if (h > bestH) { best = u; bestH = h; }
  }
  return best;
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
