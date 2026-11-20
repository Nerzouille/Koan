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

/** Every image covering the pressed point, even one that ignores touches (pointer-events: none). */
function imagesAt(x, y) {
  const found = new Set(document.elementsFromPoint(x, y).filter(el => el instanceof HTMLImageElement));
  for (const el of document.images) {
    const r = el.getBoundingClientRect();
    if (r.width > 0 && r.height > 0 && x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) found.add(el);
  }
  return [...found];
}

/**
 * The largest fully loaded, real (not preview, not data:) image under the pressed point, or why
 * there is none (a short reason for Koan's log; never an address).
 */
function best() {
  if (!pressed) return { reason: "no-press" };
  const images = imagesAt(pressed.x, pressed.y);
  if (images.length === 0) return { reason: "no-image" };
  let found = null;
  let area = 0;
  let real = 0;
  for (const el of images) {
    const src = el.currentSrc || el.src;
    if (!src || !/^https?:/.test(src) || isPreview(src)) continue;
    real++;
    if (!el.complete || el.naturalWidth === 0) continue;
    const a = el.naturalWidth * el.naturalHeight;
    if (a > area) { area = a; found = src; }
  }
  if (found) return { url: found };
  return { reason: real === 0 ? "preview-only" : "not-loaded", images: images.length };
}

function resolve(timeoutMs) {
  return new Promise(done => {
    const start = Date.now();
    const tick = () => {
      const result = best();
      if (result.url || Date.now() - start > timeoutMs) return done(result);
      setTimeout(tick, 200);
    };
    tick();
  });
}

function connect() {
  const port = browser.runtime.connectNative("koanPageImage");
  port.onMessage.addListener(async message => {
    if (message.action !== "full") return;
    const result = await resolve(message.timeout || 10000);
    port.postMessage({ id: message.id, ...result });
  });
  return port;
}

let port = connect();

// A page restored from the back/forward cache doesn't run this again: reconnect.
window.addEventListener("pageshow", () => { port = port || connect(); });
window.addEventListener("pagehide", () => { port.disconnect(); port = null; });
