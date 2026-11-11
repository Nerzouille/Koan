package dev.nerzouille.koan.engine

import kotlinx.coroutines.CoroutineScope
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import org.json.JSONObject

/**
 * "Save image" on a picture that is still a small preview (Google Images, while the full image
 * loads): a small built-in extension (assets/extensions/pageimage) remembers where the page was
 * long-pressed and, when asked, waits for the full image at that spot and answers its address.
 * Bump its manifest version whenever content.js changes, or Gecko keeps the old one.
 */
class PageImages(engine: Engine, store: BrowserStore, scope: CoroutineScope) : PageBridge(engine, store, scope, ID, URL, PORT) {
    override fun answer(reply: JSONObject): String? = reply.optString("url").takeIf { it.startsWith("https://") || it.startsWith("http://") }

    /**
     * The full image at the spot last long-pressed in the tab's page, once it has loaded there
     * (waits up to [WAIT_MS]); null if none came (or the page doesn't answer).
     */
    suspend fun fullImage(tabId: String): String? =
        ask(tabId, JSONObject().put("action", "full").put("timeout", WAIT_MS), WAIT_MS + 1000)

    private companion object {
        const val ID = "pageimage@koan.nerzouille.dev"
        const val URL = "resource://android/assets/extensions/pageimage/"
        const val PORT = "koanPageImage"
        const val WAIT_MS = 10_000L
    }
}
