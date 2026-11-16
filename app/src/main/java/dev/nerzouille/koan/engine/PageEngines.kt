package dev.nerzouille.koan.engine

import dev.nerzouille.koan.search.EngineDraft
import dev.nerzouille.koan.search.OpenSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import org.json.JSONObject

/**
 * The search engine a page declares (OpenSearch), for the ⋯ menu's "Add this search engine". A
 * small built-in extension (assets/extensions/opensearch) reads the live page only when asked:
 * signed-in pages and pages behind a bot check work, and nothing is fetched on page load.
 * Bump its manifest version whenever content.js changes, or Gecko keeps the old one.
 */
class PageEngines(engine: Engine, store: BrowserStore, scope: CoroutineScope) : PageBridge(engine, store, scope, ID, URL, PORT) {
    override fun answer(reply: JSONObject): String? = reply.optString("xml").takeIf { reply.optBoolean("found") && it.isNotEmpty() }

    /** The engine the tab's page declares, null if none (or the page doesn't answer in time). */
    suspend fun find(tabId: String): EngineDraft? {
        val xml = ask(tabId, JSONObject().put("action", "find"), TIMEOUT_MS)
        return xml?.let { withContext(Dispatchers.Default) { runCatching { OpenSearch.parse(it) }.getOrNull() } }
    }

    private companion object {
        const val ID = "opensearch@koan.nerzouille.dev"
        const val URL = "resource://android/assets/extensions/opensearch/"
        const val PORT = "koanOpenSearch"
        const val TIMEOUT_MS = 4000L
    }
}
