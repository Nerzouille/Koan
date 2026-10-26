package dev.nerzouille.koan.ui.signin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.nerzouille.koan.sync.PairingFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mozilla's pairing page for [flow] (QR sign-in), answering its WebChannel the way Firefox for
 * Android does: status, the OAuth request (pairing v2 asks for it over the channel), and the
 * authorization code once the computer approved. [onCode] runs once, on the main thread. Nothing
 * here is logged: the page's messages carry the code.
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun pairingWebView(
    context: Context,
    flow: PairingFlow,
    onCode: (String) -> Unit,
    onPainted: () -> Unit,
    onProgress: (Int) -> Unit,
    onError: (String) -> Unit,
): WebView {
    val nonce = java.util.UUID.randomUUID().toString()
    val main = Handler(Looper.getMainLooper())
    var delivered = false
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = PairingFlow.USER_AGENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) settings.isAlgorithmicDarkeningAllowed = true
        val web = this
        fun reply(command: String, messageId: String, data: JSONObject) {
            val payload = JSONObject().put("id", "account_updates")
                .put("message", JSONObject().put("command", command).put("messageId", messageId).put("data", data))
            web.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('WebChannelMessageToContent', { detail: ${JSONObject.quote(payload.toString())} }));" +
                    "window.dispatchEvent(new CustomEvent('WebChannelMessageToContent', { detail: $payload }));",
                null,
            )
        }
        addJavascriptInterface(
            object {
                @JavascriptInterface
                fun message(json: String) {
                    main.post {
                        val body = runCatching { JSONObject(json) }.getOrNull() ?: return@post
                        // Anything not stamped by the injected bridge is hostile: drop it silently.
                        if (body.optString("knonce") != nonce) return@post
                        val command = body.optString("command")
                        val id = body.optString("messageId")
                        val data = body.optJSONObject("data")
                        when (command) {
                            "fxaccounts:fxa_status" -> reply(command, id, JSONObject()
                                .put("signedInUser", JSONObject.NULL)
                                .put("clientId", PairingFlow.CLIENT_ID)
                                .put("capabilities", JSONObject().put("engines", JSONArray()).put("choose_what_to_sync", false).put("pairing", true)))
                            // Pairing v2: the page asks for the OAuth request instead of reading the URL.
                            "fxaccounts:pair_oauth_start" -> reply(command, id, JSONObject()
                                .put("client_id", PairingFlow.CLIENT_ID)
                                .put("state", flow.state)
                                .put("scope", PairingFlow.SCOPE)
                                .put("code_challenge", flow.codeChallenge)
                                .put("code_challenge_method", "S256")
                                .put("keys_jwk", flow.keysJwk))
                            "fxaccounts:oauth_login" -> {
                                val code = data?.optString("code").orEmpty()
                                if (delivered || code.isEmpty()) return@post
                                if (data?.optString("state") != flow.state) return@post onError("The pairing didn't match this phone. Scan the code again.")
                                delivered = true
                                onCode(code)
                            }
                        }
                    }
                }
            },
            "KoanPair",
        )
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.isForMainFrame && request.url.host != PairingFlow.HOST

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                view.evaluateJavascript(bridgeJs(nonce), null)
            }

            override fun onPageCommitVisible(view: WebView, url: String?) = onPainted()

            override fun onPageFinished(view: WebView, url: String?) {
                view.evaluateJavascript(bridgeJs(nonce), null)
                onPainted()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) onError("Can't reach Mozilla accounts. Check your connection and try again.")
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress)
        }
        loadUrl(flow.supplicantUrl())
    }
}

/** Passes the page's WebChannel messages to Koan, stamped; presents the page as Firefox for Android. */
private fun bridgeJs(nonce: String) = """
    (function() {
      if (window.__koanPair) return;
      window.__koanPair = true;
      try { Object.defineProperty(navigator, 'userAgent', { get: function() { return '${PairingFlow.USER_AGENT}'; } }); } catch (e) {}
      function handle(event) {
        var d = event.detail;
        if (typeof d === 'string') { try { d = JSON.parse(d); } catch (e) { return; } }
        if (!d || !d.message) return;
        var m = d.message; m.knonce = '$nonce';
        try { KoanPair.message(JSON.stringify(m)); } catch (e) {}
      }
      window.addEventListener('WebChannelMessageToChrome', handle);
      document.addEventListener('WebChannelMessageToChrome', handle);
    })();
""".trimIndent()
