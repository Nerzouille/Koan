package dev.nerzouille.koan.engine

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
import java.util.WeakHashMap

/**
 * "Save image" on a picture that is still a small preview (Google Images, while the full image
 * loads): a small built-in extension (assets/extensions/pageimage) remembers where the page was
 * long-pressed and, when asked, waits for the full image at that spot and answers its address.
 * Bump its manifest version whenever content.js changes, or Gecko keeps the old one.
 */
class PageImages(private val engine: Engine, private val store: BrowserStore, private val scope: CoroutineScope) {
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
            val url = json.optString("url").takeIf { it.startsWith("https://") || it.startsWith("http://") }
            pending.remove(json.optInt("id", -1))?.complete(url)
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

    /**
     * The full image at the spot last long-pressed in the tab's page, once it has loaded there
     * (waits up to [WAIT_MS]); null if none came (or the page doesn't answer).
     */
    suspend fun fullImage(tabId: String): String? {
        val session = store.state.findTab(tabId)?.engineState?.engineSession ?: return null
        val port = ports[session] ?: return null
        val id = ++nextId
        val reply = CompletableDeferred<String?>()
        pending[id] = reply
        port.postMessage(JSONObject().put("action", "full").put("id", id).put("timeout", WAIT_MS))
        val url = withTimeoutOrNull(WAIT_MS + 1000) { reply.await() }
        pending.remove(id)
        return url
    }

    private companion object {
        const val ID = "pageimage@koan.nerzouille.dev"
        const val URL = "resource://android/assets/extensions/pageimage/"
        const val PORT = "koanPageImage"
        const val WAIT_MS = 10_000L
    }
}
