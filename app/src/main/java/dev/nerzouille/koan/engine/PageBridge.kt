package dev.nerzouille.koan.engine

import java.util.WeakHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

/**
 * A small built-in extension that answers one question about a tab's page on request (Koan's
 * OpenSearch and full-image helpers): installed once at launch, a content-message port per tab, one
 * reply per request id. Main thread only (GeckoView's callbacks and the menu).
 */
abstract class PageBridge(
    private val engine: Engine,
    private val store: BrowserStore,
    private val scope: CoroutineScope,
    id: String,
    url: String,
    port: String,
) {
    private val controller = BuiltInWebExtensionController(id, url, port)

    private val registered = WeakHashMap<EngineSession, Unit>()
    private val ports = WeakHashMap<EngineSession, Port>()
    private val pending = HashMap<Int, CompletableDeferred<String?>>()
    private var nextId = 0

    /** The answer in a page's reply (null: none). */
    protected abstract fun answer(reply: JSONObject): String?

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
            pending.remove(json.optInt("id", -1))?.complete(answer(json))
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

    /** Sends [message] (with its request id) to the tab's page; its answer, or null after [timeoutMs]. */
    protected suspend fun ask(tabId: String, message: JSONObject, timeoutMs: Long): String? {
        val session = store.state.findTab(tabId)?.engineState?.engineSession ?: return null
        val port = ports[session] ?: return null
        val id = ++nextId
        val reply = CompletableDeferred<String?>()
        pending[id] = reply
        port.postMessage(message.put("id", id))
        val answer = withTimeoutOrNull(timeoutMs) { reply.await() }
        pending.remove(id)
        return answer
    }
}
