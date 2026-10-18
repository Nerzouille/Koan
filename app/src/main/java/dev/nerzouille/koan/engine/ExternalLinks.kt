package dev.nerzouille.koan.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.request.RequestInterceptor.InterceptionResponse
import java.net.URISyntaxException

/**
 * Links the engine can't open itself (tel:, mailto:, intent://, an app's own scheme) go to the
 * app that handles them, like Chrome: only when tapped (or typed), never on a page's own say-so.
 * An intent:// link nobody handles falls back to its web address, if it has one.
 */
class ExternalLinks(context: Context) : RequestInterceptor {
    private val context = context.applicationContext

    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean,
    ): InterceptionResponse? {
        val scheme = uri.substringBefore(':', "").lowercase()
        if (scheme in ENGINE_SCHEMES || isSubframeRequest) return null
        if (!hasUserGesture && !isDirectNavigation && !isRedirect) return InterceptionResponse.Deny
        val intent = if (scheme == "intent") parseIntent(uri) ?: return InterceptionResponse.Deny else Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            InterceptionResponse.Deny
        } catch (_: ActivityNotFoundException) {
            intent.getStringExtra(FALLBACK_URL)?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                ?.let { InterceptionResponse.Url(it) } ?: InterceptionResponse.Deny
        } catch (_: SecurityException) {
            InterceptionResponse.Deny
        }
    }

    /** An intent:// link, made safe: browsable only, no explicit component or selector. */
    private fun parseIntent(uri: String): Intent? = try {
        Intent.parseUri(uri, Intent.URI_INTENT_SCHEME).apply {
            component = null
            selector = null
        }
    } catch (_: URISyntaxException) {
        null
    }

    private companion object {
        val ENGINE_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "javascript", "moz-extension", "resource", "chrome", "view-source", "")
        const val FALLBACK_URL = "browser_fallback_url"
    }
}
