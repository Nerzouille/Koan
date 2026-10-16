package dev.nerzouille.koan.engine

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.state.extension.WebExtensionPromptRequest
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.EnableSource
import mozilla.components.concept.engine.webextension.InstallationMethod
import mozilla.components.concept.engine.webextension.PermissionPromptResponse
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.webextensions.WebExtensionSupport

/**
 * uBlock Origin, Koan's one add-on: installed from addons.mozilla.org on the first start (once:
 * if the owner removes or turns it off, it stays that way), its install permissions granted (only
 * for it), turned on and off from the menu and Settings. Its badge is the blocked count.
 */
class UBlock(
    private val engine: Engine,
    private val store: BrowserStore,
    private val settings: dev.nerzouille.koan.data.Settings,
    private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch(Dispatchers.Main) {
            launch { approvePrompts() }
            launch { countBlocked() }
            WebExtensionSupport.awaitInitialization()
            if (installed() == null && !settings.ublockOffered()) install()
        }
    }

    fun installed() = WebExtensionSupport.installedExtensions[ID]

    /** Installs it (first start, or the Settings row when it isn't there). */
    fun install() {
        scope.launch { settings.markUblockOffered() }
        engine.installWebExtension(
            AMO_URL,
            InstallationMethod.MANAGER,
            onSuccess = { Log.i(TAG, "installed") },
            onError = { e -> Log.w(TAG, "install failed: ${e.javaClass.simpleName}") },
        )
    }

    fun setEnabled(on: Boolean) {
        val ext = installed() ?: return
        if (on) {
            engine.enableWebExtension(ext, EnableSource.USER, onSuccess = {}, onError = {})
        } else {
            engine.disableWebExtension(ext, EnableSource.USER, onSuccess = {}, onError = {})
        }
    }

    /**
     * The week's count (Settings): every rise of a page's badge is added to the day at once, so
     * nothing is lost if Koan is killed, and a page is never counted twice.
     */
    private suspend fun countBlocked() {
        // tab id → (url, highest count already added for it)
        val pages = HashMap<String, Pair<String, Int>>()
        store.flow()
            .map { state -> state.tabs.map { Triple(it.id, it.content.url, count(it.extensionState[ID]?.browserAction?.badgeText)) } }
            .distinctUntilChanged()
            .collect { tabs ->
                val seen = HashSet<String>()
                for ((id, url, n) in tabs) {
                    seen += id
                    val before = pages[id]
                    val counted = if (before == null || before.first != url) 0 else before.second
                    if (n > counted) record(n - counted)
                    pages[id] = url to maxOf(n, counted)
                }
                pages.keys.retainAll(seen)
            }
    }

    private fun record(n: Int) {
        if (n <= 0) return
        val day = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ROOT).format(java.util.Date())
        scope.launch { settings.addBlocked(day, n) }
    }

    /** Its permission request is the owner's choice already (they asked for uBlock): granted. */
    private suspend fun approvePrompts() {
        store.flow().map { it.webExtensionPromptRequest }.distinctUntilChanged().collect { request ->
            when (request) {
                is WebExtensionPromptRequest.AfterInstallation.Permissions.Required -> {
                    if (request.extension.id != ID) return@collect
                    request.onConfirm(PermissionPromptResponse(isPermissionsGranted = true, isPrivateModeGranted = false))
                    store.dispatch(WebExtensionAction.ConsumePromptRequestWebExtensionAction)
                }
                is WebExtensionPromptRequest.AfterInstallation.PostInstallation -> {
                    if (request.extension.id == ID) store.dispatch(WebExtensionAction.ConsumePromptRequestWebExtensionAction)
                }
                else -> Unit
            }
        }
    }

    companion object {
        const val ID = "uBlock0@raymondhill.net"
        private const val AMO_URL = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi"
        private const val TAG = "KoanUBlock"

        /** The badge as a number: "12" → 12, "1.2k" → 1200, empty → 0. */
        fun count(badge: String?): Int {
            val b = badge?.trim()?.lowercase().orEmpty()
            if (b.isEmpty()) return 0
            val k = b.endsWith("k")
            val n = b.removeSuffix("k").toFloatOrNull() ?: return 0
            return (if (k) n * 1000 else n).toInt()
        }
    }
}
