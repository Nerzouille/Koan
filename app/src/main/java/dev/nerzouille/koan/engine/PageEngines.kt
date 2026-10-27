package dev.nerzouille.koan.engine

import dev.nerzouille.koan.search.EngineDraft
import dev.nerzouille.koan.search.OpenSearch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.webextensions.BuiltInWebExtensionController
import org.json.JSONObject
import java.util.WeakHashMap

/**
 * The search engine a page declares (OpenSearch), for the ⋯ menu's "Add this search engine". A
 * small built-in extension (assets/extensions/opensearch) reads the live page only when asked:
 * signed-in pages and pages behind a bot check work, and nothing is fetched on page load.
 * Bump its manifest version whenever content.js changes, or Gecko keeps the old one.
 */
class PageEngines(private val engine: Engine, private val store: BrowserStore, private val scope: CoroutineScope) {
    private val controller = BuiltInWebExtensionController(ID, URL, PORT)

    // Main thread only (GeckoView's callbacks and the menu).
    private val registered = WeakHashMap<EngineSession, Unit>()
    private val ports = WeakHashMap<EngineSession, Port>()
    private val pending = HashMap<Int, CompletableDeferred<String?>>()
    private var nextId = 0

    private val handler = object : MessageHandler {
        override fun onPortConnected(port: Port) {
            port.engineSession?.let { ports[it] = port }
        }

        override fun onPortDisconnected(port: Port) {
            val session = port.engineSession ?: return
            if (ports[session] === port) ports.remove(session)
        }

        override fun onPortMessage(message: Any, port: Port) {
            val json = message as? JSONObject ?: return
            val xml = json.optString("xml").takeIf { json.optBoolean("found") && it.isNotEmpty() }
            pending.remove(json.optInt("id", -1))?.complete(xml)
        }
    }

    /** Installs the extension, then listens on every tab's page. Once, at launch. */
    fun start() {
        controller.install(engine, onSuccess = { scope.launch(Dispatchers.Main) { watch() } }, onError = {})
    }

    private suspend fun watch() {
        store.flow()
            .map { s -> s.tabs.mapNotNull { it.engineState.engineSession } }
            .distinctUntilChanged()
            .collect { sessions ->
                sessions.forEach { session ->
                    if (registered.put(session, Unit) == null) controller.registerContentMessageHandler(session, handler)
                }
            }
    }

    /** The engine the tab's page declares, null if none (or the page doesn't answer in time). */
    suspend fun find(tabId: String): EngineDraft? {
        val session = store.state.findTab(tabId)?.engineState?.engineSession ?: return null
        val port = ports[session] ?: return null
        val id = ++nextId
        val reply = CompletableDeferred<String?>()
        pending[id] = reply
        port.postMessage(JSONObject().put("action", "find").put("id", id))
        val xml = withTimeoutOrNull(TIMEOUT_MS) { reply.await() }
        pending.remove(id)
        return xml?.let { withContext(Dispatchers.Default) { runCatching { OpenSearch.parse(it) }.getOrNull() } }
    }

    private companion object {
        const val ID = "opensearch@koan.nerzouille.dev"
        const val URL = "resource://android/assets/extensions/opensearch/"
        const val PORT = "koanOpenSearch"
        const val TIMEOUT_MS = 4000L
    }
}
