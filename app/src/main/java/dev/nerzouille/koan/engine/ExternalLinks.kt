package dev.nerzouille.koan.engine

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import dev.nerzouille.koan.search.siteHost
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.request.RequestInterceptor.InterceptionResponse
import java.net.URISyntaxException

/** An installed app that can open a link: its package, its name and icon. */
class AppTarget(val pkg: String, val label: String, val icon: Bitmap?)

/**
 * A page wants to open an app: the card asks first ("Open in YouTube?", Open / Cancel, "Always
 * for this site"). [site] is the page's host.
 */
class AppLinkAsk(val app: AppTarget, val site: String, private val launch: () -> Boolean, private val remember: (Boolean) -> Unit) {
    fun open(always: Boolean) {
        remember(always)
        launch()
    }
}

/**
 * Links the engine can't open itself (tel:, mailto:, intent://, an app's own scheme like weixin://
 * or alipays://) go to the app that handles them. Never on a page's own say-so: a tapped link (or a
 * typed address) opens the app, after a card the first time per site ([ask]; "Always for this
 * site" skips it from then on). A page that tries on its own (a timer, a redirect, a hidden frame:
 * what many "Open app" buttons do) gets the card at most once per page, so a tap still decides.
 * No app installed: the link's fallback address (intent://… browser_fallback_url) if it has one,
 * else the page stays and a toast says so ([messages]).
 */
class ExternalLinks(
    context: Context,
    private val pageUrl: () -> String?,
    /** A page load to send elsewhere first (Google AMP links → the article, privacy/AmpLinks); null = as is. */
    private val rewrite: (EngineSession, String) -> String? = { _, _ -> null },
) : RequestInterceptor {
    private val context = context.applicationContext
    private val pm = context.packageManager
    private val prefs = context.getSharedPreferences("app_links", Context.MODE_PRIVATE)

    /** The card's question, when a page wants an app (KoanOverlays shows it). */
    val ask = MutableStateFlow<AppLinkAsk?>(null)

    /** Short notes for the toast ("No app can open this link"). */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Pages that already got their one unasked-for card: page url + scheme. */
    private val askedOnce = LinkedHashSet<String>()

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
        if (!isSubframeRequest && (scheme == "https" || scheme == "http")) {
            rewrite(engineSession, uri)?.let { return InterceptionResponse.Url(it) }
        }
        if (scheme in ENGINE_SCHEMES) return null
        val page = lastUri?.takeIf { it.isNotBlank() } ?: pageUrl()
        val site = page?.let { Uri.parse(it).host }?.removePrefix("www.").orEmpty()
        log("scheme=$scheme site=$site gesture=$hasUserGesture redirect=$isRedirect direct=$isDirectNavigation frame=$isSubframeRequest")
        val intent = (if (scheme == "intent") parseIntent(uri) else Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            ?: return InterceptionResponse.Deny
        intent.addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tapped = hasUserGesture || isDirectNavigation
        val fallback = intent.getStringExtra(FALLBACK_URL)?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        // An app Koan can't see (a scheme outside the manifest's <queries>) may still be there:
        // the card asks for "another app", and Android says at the tap whether it exists.
        val app = resolve(intent) ?: if (fallback == null && mayBeHidden(intent)) HIDDEN else null
        if (app == null) {
            log("no app; fallback=${fallback != null}")
            if (fallback != null && !isSubframeRequest) return InterceptionResponse.Url(fallback)
            if (tapped) messages.tryEmit(if (scheme in SYSTEM_SCHEMES) "No app can open this link" else "The app for this link isn't installed")
            return InterceptionResponse.Deny
        }
        // tel:, mailto:, sms:… are what the tap asked for: no card.
        val trusted = scheme in SYSTEM_SCHEMES || isDirectNavigation || (hasUserGesture && always(site, app.pkg))
        if (tapped && trusted) {
            launch(intent)
            return InterceptionResponse.Deny
        }
        // Untapped: one card per page, never a launch.
        if (!tapped) {
            val key = "${page.orEmpty()}|$scheme"
            if (!askedOnce.add(key)) return InterceptionResponse.Deny
            if (askedOnce.size > ASKED_KEEP) askedOnce.remove(askedOnce.first())
        }
        ask.value = AppLinkAsk(app, site, launch = { launch(intent) }, remember = { on -> if (on) setAlways(site, app.pkg) })
        return InterceptionResponse.Deny
    }

    /**
     * Private tabs are HTTPS-only: a site without HTTPS fails, and GeckoView shows nothing (the
     * page stays). A toast says why.
     */
    override fun onErrorRequest(
        session: EngineSession,
        errorType: mozilla.components.concept.engine.request.ErrorType,
        uri: String?,
    ): RequestInterceptor.ErrorResponse? {
        if (errorType == mozilla.components.concept.engine.request.ErrorType.ERROR_HTTPS_ONLY) {
            messages.tryEmit("Not opened in Private: no HTTPS")
        }
        return null
    }

    /** Whether [url] is for an app, not the engine (weixin://…, intent://…, tel:…). */
    fun isAppLink(url: String) = url.substringBefore(':', "").lowercase() !in ENGINE_SCHEMES

    /**
     * An app's link typed in the address field (the engine doesn't take those): its app opens at
     * once, typing it is the tap. Returns the link's fallback address to load instead, if it has
     * one and no app is installed.
     */
    fun openTyped(uri: String): String? {
        val scheme = uri.substringBefore(':', "").lowercase()
        log("typed scheme=$scheme")
        val intent = (if (scheme == "intent") parseIntent(uri) else Intent(Intent.ACTION_VIEW, Uri.parse(uri))) ?: return null
        intent.addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = intent.getStringExtra(FALLBACK_URL)?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        if (resolve(intent) != null || (fallback == null && mayBeHidden(intent))) {
            launch(intent)
            return null
        }
        if (fallback == null) messages.tryEmit(if (scheme in SYSTEM_SCHEMES) "No app can open this link" else "The app for this link isn't installed")
        return fallback
    }

    /** The app (not a browser) that opens [url] in itself, e.g. YouTube for a youtube.com page; null if none. */
    fun appFor(url: String): AppTarget? {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return null
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        val browsers = handlers(Intent(Intent.ACTION_VIEW, Uri.parse(PROBE_URL)).addCategory(Intent.CATEGORY_BROWSABLE)).toSet()
        val apps = handlers(view).filter { it !in browsers && it != context.packageName }
        val preferred = pm.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        val pkg = preferred?.takeIf { it in apps } ?: apps.firstOrNull() ?: return null
        return target(pkg)
    }

    /** Opens [url] in [app] (the ⋯ menu's "Open in …"). */
    fun openIn(app: AppTarget, url: String): Boolean =
        launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(app.pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun handlers(intent: Intent): List<String> =
        runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }.distinct() }.getOrDefault(emptyList())

    /**
     * Who opens [intent], never Koan itself; [intent] is pinned to that app. A link pinned to an app
     * that can't take it (installed but disabled, or replaced: YouTube's "Open app" names the
     * official YouTube app) goes to another app that handles it, if any. Android doesn't always
     * say when a start fails (HyperOS just drops it), so nothing is started blind.
     */
    private fun resolve(intent: Intent): AppTarget? {
        val pinned = intent.`package`
        if (pinned != null && handlers(intent).isNotEmpty()) return target(pinned)
        val any = Intent(intent).setPackage(null)
        val pkg = (pm.resolveActivity(any, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.takeIf { it != "android" && it != context.packageName }
            ?: handlers(any).firstOrNull { it != context.packageName }) ?: return null
        log("pinned app can't open it: ${pinned != null}; handled by another app")
        intent.setPackage(pkg)
        return target(pkg)
    }

    /**
     * Whether an app Koan can't see might open [intent]: its scheme isn't one the manifest
     * declares (<queries>), so an installed app for it would be hidden from Koan. Web links and
     * declared schemes are fully visible: no app found there means none installed.
     */
    private fun mayBeHidden(intent: Intent): Boolean {
        val scheme = intent.data?.scheme?.lowercase() ?: return intent.`package` != null
        return scheme !in VISIBLE_SCHEMES
    }

    private fun target(pkg: String): AppTarget? = runCatching {
        val info = pm.getApplicationInfo(pkg, 0)
        val icon = runCatching { pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX) }.getOrNull()
        AppTarget(pkg, pm.getApplicationLabel(info).toString(), icon)
    }.getOrNull()

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallback = intent.getStringExtra(FALLBACK_URL)
        messages.tryEmit(if (fallback != null) "Couldn't open the app" else "The app for this link isn't installed")
        false
    } catch (_: SecurityException) {
        messages.tryEmit("Couldn't open the app")
        false
    }

    // Keyed by the site ("m." and "www." hosts are one site); choices saved under the plain host
    // before batch 4 are still read.
    private fun always(site: String, pkg: String) =
        site.isNotEmpty() && (prefs.getBoolean("${siteHost(site)}|$pkg", false) || prefs.getBoolean("$site|$pkg", false))

    private fun setAlways(site: String, pkg: String) {
        if (site.isNotEmpty()) prefs.edit().putBoolean("${siteHost(site)}|$pkg", true).apply()
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

    /** Which kind of link asked, never its address (it can carry tokens). */
    private fun log(event: String) = Log.i("KoanLinks", event)

    private companion object {
        /** An app that may exist but that Koan can't see or name (package visibility). */
        val HIDDEN = AppTarget("", "another app", null)

        /** Schemes whose apps Koan can see: keep in step with the manifest's <queries>. */
        val VISIBLE_SCHEMES = setOf(
            "http", "https", "weixin", "alipays", "alipay", "snssdk1128", "snssdk1233", "snssdk143", "aweme", "tiktok",
            "instagram", "linkedin", "fb", "fb-messenger", "twitter", "x", "whatsapp", "tg", "spotify", "zhihu", "taobao",
            "tbopen", "tmall", "openapp.jdmobile", "imeituan", "baiduboxapp", "bdapp", "amapuri", "androidamap", "iosamap",
            "baidumap", "bilibili", "vnd.youtube", "youtube", "discord", "reddit", "slack", "zoomus", "msteams", "market",
            "geo", "sms", "smsto", "mailto", "tel",
        )

        val ENGINE_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "javascript", "moz-extension", "resource", "chrome", "view-source", "")
        val SYSTEM_SCHEMES = setOf("tel", "mailto", "sms", "smsto", "mms", "geo", "market")
        const val FALLBACK_URL = "browser_fallback_url"
        const val PROBE_URL = "https://koan-probe.invalid/"
        const val ICON_PX = 96
        const val ASKED_KEEP = 64
    }
}
