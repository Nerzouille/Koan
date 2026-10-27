/*
 * Koan asks, when its menu opens on this page, whether the page declares a search engine
 * (<link rel="search" type="application/opensearchdescription+xml">). Only then the description
 * is fetched, as the page itself would (same cookies), and handed to Koan. Nothing else is read.
 */
"use strict";

const TYPE = "application/opensearchdescription+xml";

async function find() {
  const link = [...document.querySelectorAll("link[rel][href]")].find(l =>
    l.relList.contains("search") && (l.type || "").toLowerCase() === TYPE);
  if (!link) return { found: false };
  try {
    const fetcher = typeof content !== "undefined" && content.fetch ? content.fetch : fetch;
    const response = await fetcher(link.href, { credentials: "include" });
    if (!response.ok) return { found: false };
    const text = await response.text();
    return { found: true, xml: text.slice(0, 256 * 1024) };
  } catch (e) {
    return { found: false };
  }
}

function connect() {
  const port = browser.runtime.connectNative("koanOpenSearch");
  port.onMessage.addListener(async message => {
    if (message.action !== "find") return;
    const result = await find();
    port.postMessage({ id: message.id, url: location.href, ...result });
  });
  return port;
}

let port = connect();

// A page restored from the back/forward cache doesn't run this again: reconnect.
window.addEventListener("pageshow", () => { port = port || connect(); });
window.addEventListener("pagehide", () => { port.disconnect(); port = null; });
