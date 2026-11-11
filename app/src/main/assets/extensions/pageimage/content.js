/*
 * "Save image" on a picture that is still a small preview (Google Images shows its thumbnail
 * while the full image loads): Koan asks which image is really at the spot that was
 * long-pressed, and this waits until the full one has loaded there, then answers its address.
 * Only the last long-press point is kept, in this page; nothing is read before Koan asks.
 */
"use strict";

let pressed = null; // { x, y } in the viewport, at the last long press (contextmenu)

window.addEventListener("contextmenu", e => { pressed = { x: e.clientX, y: e.clientY }; }, true);

function isPreview(src) {
  try {
    const u = new URL(src);
    return u.hostname.endsWith(".gstatic.com") && u.hostname.includes("tbn") && u.search.includes("tbn");
  } catch (e) {
    return true;
  }
}

/** The largest fully loaded, real (not preview, not data:) image under the pressed point. */
function best() {
  if (!pressed) return null;
  let found = null;
  let area = 0;
  for (const el of document.elementsFromPoint(pressed.x, pressed.y)) {
    if (!(el instanceof HTMLImageElement)) continue;
    const src = el.currentSrc || el.src;
    if (!src || !/^https?:/.test(src) || isPreview(src)) continue;
    if (!el.complete || el.naturalWidth === 0) continue;
    const a = el.naturalWidth * el.naturalHeight;
    if (a > area) { area = a; found = src; }
  }
  return found;
}

function resolve(timeoutMs) {
  return new Promise(done => {
    const start = Date.now();
    const tick = () => {
      const src = best();
      if (src) return done(src);
      if (Date.now() - start > timeoutMs) return done(null);
      setTimeout(tick, 200);
    };
    tick();
  });
}

function connect() {
  const port = browser.runtime.connectNative("koanPageImage");
  port.onMessage.addListener(async message => {
    if (message.action !== "full") return;
    const url = await resolve(message.timeout || 10000);
    port.postMessage({ id: message.id, url });
  });
  return port;
}

let port = connect();

// A page restored from the back/forward cache doesn't run this again: reconnect.
window.addEventListener("pageshow", () => { port = port || connect(); });
window.addEventListener("pagehide", () => { port.disconnect(); port = null; });
