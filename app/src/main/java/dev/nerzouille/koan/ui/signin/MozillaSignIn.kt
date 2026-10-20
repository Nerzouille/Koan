// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.ui.signin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/** What the Mozilla accounts page hands over on login. Secrets: never log or print it. */
class FxAWebLogin(
    val email: String,
    val uid: String,
    val sessionToken: String,
    val keyFetchToken: String,
    val unwrapBKey: String,
) {
    override fun toString() = "FxAWebLogin(email=$email, secrets=<redacted>)"
}

/**
 * Port of Z-Sync's `FxAWeb`: the official Mozilla accounts page plus the FxA WebChannel, answered
 * the way Firefox desktop answers it (docs/SYNC.md "Sign-in"). A direct POST /account/login is
 * blocked by Mozilla's CDN, so the page itself does the login (password, 2FA code, email check).
 */
internal object FxAWeb {
    const val LOGIN_URL = "https://accounts.firefox.com/?service=sync&context=fx_desktop_v3&entrypoint=koan&action=email"
    const val HOST = "accounts.firefox.com"
    // Z-Sync's tested desktop identity; the accounts page must see Firefox desktop.
    const val FIREFOX_DESKTOP_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:140.0) Gecko/20100101 Firefox/140.0"

    /**
     * Z-Sync's bridge script, verbatim except names: answers `fxaccounts:fxa_status` /
     * `can_link_account`, captures the login, and stamps every message with a per-WebView nonce
     * because the JavascriptInterface is reachable from every frame.
     */
    fun bridgeJS(ua: String, nonce: String): String = """
    (function() {
      if (window.__koanBridge) return;
      window.__koanBridge = true;
      window.__knonce = '$nonce';
      try {
        Object.defineProperty(navigator, 'userAgent', {
          get: function() { return '$ua'; }
        });
      } catch (e) {}

      function post(obj) {
        obj.knonce = window.__knonce;
        try { KoanBridge.fxa(JSON.stringify(obj)); } catch (e) {}
      }

      function reply(message) {
        var payload = { id: 'account_updates', message: message };
        try {
          window.dispatchEvent(new CustomEvent('WebChannelMessageToContent', { detail: payload }));
        } catch (e) {}
        try {
          window.dispatchEvent(new CustomEvent('WebChannelMessageToContent', {
            detail: JSON.stringify(payload)
          }));
        } catch (e) {}
      }

      function flattenLogin(data) {
        if (!data) return null;
        var email = data.email || '';
        var uid = data.uid || '';
        var sessionToken = data.sessionToken || data.session_token || '';
        var keyFetchToken = data.keyFetchToken || data.key_fetch_token || '';
        var unwrapBKey = data.unwrapBKey || data.unwrap_b_key || '';
        if (!email || !uid || !sessionToken || !keyFetchToken || !unwrapBKey) return null;
        return {
          command: 'koan:login',
          email: email,
          uid: uid,
          sessionToken: sessionToken,
          keyFetchToken: keyFetchToken,
          unwrapBKey: unwrapBKey
        };
      }

      function capture(data) {
        var flat = flattenLogin(data);
        if (!flat) return false;
        try { sessionStorage.setItem('koan.login', JSON.stringify(flat)); } catch (e) {}
        post(flat);
        return true;
      }

      function replay() {
        try {
          var raw = sessionStorage.getItem('koan.login');
          if (raw) post(JSON.parse(raw));
        } catch (e) {}
      }

      function handle(event) {
        var detail = event.detail;
        if (typeof detail === 'string') {
          try { detail = JSON.parse(detail); } catch (e) { return; }
        }
        if (!detail || detail.id !== 'account_updates' || !detail.message) return;
        var msg = detail.message;
        var command = msg.command || '';
        if (command === 'fxaccounts:fxa_status') {
          reply({
            command: command,
            messageId: msg.messageId,
            data: {
              signedInUser: null,
              clientId: (msg.data && msg.data.service) ? msg.data.service : 'sync',
              capabilities: {
                engines: ['addresses', 'creditcards'],
                choose_what_to_sync: false,
                pairing: false,
                multiService: false
              }
            }
          });
          replay();
          return;
        }
        if (command === 'fxaccounts:can_link_account') {
          reply({ command: command, messageId: msg.messageId, data: { ok: true } });
          return;
        }
        if (command === 'fxaccounts:login' || command === 'fxaccounts:verified' || command === 'fxaccounts:change_password' || command === 'fxaccount:change_password') {
          if (msg.data && msg.data.verified === false) {
            post({ command: 'koan:unverified' });
          }
          capture(msg.data);
        }
      }

      window.addEventListener('WebChannelMessageToChrome', handle);
      document.addEventListener('WebChannelMessageToChrome', handle);
      replay();

      // Passkeys can't work in an embedded WebView (no app association): hide them.
      function hidePasskeys() {
        try {
          var nodes = document.querySelectorAll('button, a');
          for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            var text = (el.innerText || el.textContent || '').toLowerCase();
            var html = el.innerHTML || '';
            var passkey = /passkey/.test(text) ||
              html.indexOf('M5.625 9.063') >= 0 ||
              html.indexOf('M4.627 4.774') >= 0;
            if (!passkey) continue;
            var container = el.parentElement;
            var root = container ? container.parentElement : null;
            try { el.remove(); } catch (err) {}
            var removedContainer = false;
            if (container && container.children.length === 0) {
              try { container.remove(); removedContainer = true; } catch (err2) {}
            }
            if (removedContainer && root && root.querySelectorAll('button, a, input').length === 0) {
              try { root.remove(); } catch (err3) {}
            }
          }
          var lines = document.querySelectorAll('div.flex-1.h-px');
          for (var j = 0; j < lines.length; j++) {
            var divider = lines[j].parentElement;
            var box = divider ? divider.parentElement : null;
            if (!box || box.querySelector('button, a')) continue;
            try { divider.remove(); } catch (err4) {}
            if (box.children.length === 0) {
              try { box.remove(); } catch (err5) {}
            }
          }
        } catch (err) {}
      }
      window.__koanHidePasskeys = hidePasskeys;
      hidePasskeys();
      var ticks = 0;
      var iv = setInterval(function() {
        hidePasskeys();
        if (++ticks > 80) clearInterval(iv);
      }, 250);
      try {
        new MutationObserver(hidePasskeys).observe(document.documentElement, {
          childList: true, subtree: true, attributes: true
        });
      } catch (err) {}
    })();
    """.trimIndent()

    fun replayJS(): String =
        "try { var r = sessionStorage.getItem('koan.login'); if (r) { var o = JSON.parse(r); o.knonce = window.__knonce; KoanBridge.fxa(JSON.stringify(o)); } } catch (e) {} " +
            "try { window.__koanHidePasskeys && window.__koanHidePasskeys(); } catch (e) {}"

    /** Sign-out: cookies and storage outlive the session, so the next sign-in would reuse them. */
    fun wipeWebSession() {
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            removeSessionCookies(null)
            flush()
        }
        WebStorage.getInstance().deleteOrigin("https://$HOST")
    }
}

/** Port of Z-Sync's `parseBridgeMessage`. */
internal fun parseBridgeMessage(json: String): FxAWebLogin? {
    val body = runCatching { JSONObject(json) }.getOrNull() ?: return null
    val command = body.optString("command")
    if (command != "koan:login" && command != "fxaccounts:login" && command != "fxaccounts:verified" &&
        command != "fxaccounts:change_password" && command != "fxaccount:change_password"
    ) {
        return null
    }
    val data = body.optJSONObject("data") ?: body
    fun str(key: String, alt: String? = null): String? =
        data.optString(key).takeIf { it.isNotEmpty() && !data.isNull(key) }
            ?: alt?.let { a -> data.optString(a).takeIf { it.isNotEmpty() && !data.isNull(a) } }
    return FxAWebLogin(
        email = str("email") ?: return null,
        uid = str("uid") ?: return null,
        sessionToken = str("sessionToken", "session_token") ?: return null,
        keyFetchToken = str("keyFetchToken", "key_fetch_token") ?: return null,
        unwrapBKey = str("unwrapBKey", "unwrap_b_key") ?: return null,
    )
}

/**
 * The sign-in WebView. Dark from the first frame: transparent until the page has painted, and the
 * page itself is algorithmically darkened. Calls back on the main thread.
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun signInWebView(
    context: android.content.Context,
    onLogin: (FxAWebLogin) -> Unit,
    onUnverified: () -> Unit,
    onPainted: () -> Unit,
    onProgress: (Int) -> Unit,
    onError: (String) -> Unit,
): WebView {
    val nonce = java.util.UUID.randomUUID().toString()
    val main = Handler(Looper.getMainLooper())
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = FxAWeb.FIREFOX_DESKTOP_UA
        // Android 13+; below it the sign-in page stays light.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) settings.isAlgorithmicDarkeningAllowed = true
        addJavascriptInterface(
            object {
                @JavascriptInterface
                fun fxa(json: String) {
                    main.post {
                        val body = runCatching { JSONObject(json) }.getOrNull() ?: return@post
                        // Anything not stamped by the injected bridge is hostile: drop it silently.
                        if (body.optString("knonce") != nonce) return@post
                        if (body.optString("command") == "koan:unverified") return@post onUnverified()
                        parseBridgeMessage(json)?.let(onLogin)
                    }
                }
            },
            "KoanBridge",
        )
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame || request.url.host == FxAWeb.HOST) return false
                // Never load other sites in the auth WebView; links the user taps open outside.
                if (request.hasGesture()) {
                    runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                if (isAuthHost(url)) view.evaluateJavascript(FxAWeb.bridgeJS(FxAWeb.FIREFOX_DESKTOP_UA, nonce), null)
            }

            override fun onPageCommitVisible(view: WebView, url: String?) = onPainted()

            override fun onPageFinished(view: WebView, url: String?) {
                if (isAuthHost(url)) view.evaluateJavascript(FxAWeb.replayJS(), null)
                onPainted()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) onError("Can't reach Mozilla accounts. Check your connection and try again.")
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress)
        }
        loadUrl(FxAWeb.LOGIN_URL)
    }
}

private fun isAuthHost(url: String?) = url != null && runCatching { Uri.parse(url).host }.getOrNull() == FxAWeb.HOST
