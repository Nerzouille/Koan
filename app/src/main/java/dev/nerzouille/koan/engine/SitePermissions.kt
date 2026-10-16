package dev.nerzouille.koan.engine

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.PermissionRequest
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission
import org.mozilla.geckoview.StorageController
import kotlin.coroutines.resume

private val Context.siteDecisions by preferencesDataStore(name = "site_permissions")

/** What a page asks for, as Koan shows it. */
enum class SiteAsk(val label: String, val wants: String) {
    Location("Location", "use your location"),
    Camera("Camera", "use your camera"),
    Microphone("Microphone", "use your microphone"),
    CameraAndMicrophone("Camera and microphone", "use your camera and microphone"),
    Storage("Storage", "store data on this phone"),
    ProtectedMedia("Protected content", "play protected content"),
    CrossSiteCookies("Cross-site cookies", "use its cookies on this site"),
    LocalNetwork("Local network", "reach devices on your network"),
    VirtualReality("Virtual reality", "use virtual reality"),
}

/** A decision a site has: allowed or blocked ([raw] = Gecko's record, null for camera/mic). */
class SiteDecision(val ask: SiteAsk, val allowed: Boolean, internal val raw: ContentPermission?)

/**
 * Site permissions. Gecko asks Koan every time (it doesn't keep the answers here), so Koan
 * remembers "Always allow" and "Block" per site and answers for itself next time. Site settings
 * shows those, and any record Gecko does hold for the site.
 */
class SitePermissions(private val context: Context, private val runtime: GeckoRuntime) {

    /** Koan doesn't show web notifications (yet), and autoplay follows Gecko's default. */
    fun autoAnswer(request: PermissionRequest): Boolean? {
        val p = request.permissions
        return when {
            p.any { it is Permission.ContentNotification } -> false
            p.all { it is Permission.ContentAutoPlayInaudible } -> true
            p.any { it is Permission.ContentAutoPlayAudible } -> false
            else -> null
        }
    }

    /** What a request is for, for the card; null when Koan has nothing to ask (see [autoAnswer]). */
    fun askOf(request: PermissionRequest): SiteAsk? {
        val p = request.permissions
        val camera = p.any { it is Permission.ContentVideoCamera || it is Permission.ContentVideoCapture }
        val mic = p.any { it is Permission.ContentAudioMicrophone || it is Permission.ContentAudioCapture }
        return when {
            camera && mic -> SiteAsk.CameraAndMicrophone
            camera -> SiteAsk.Camera
            mic -> SiteAsk.Microphone
            p.any { it is Permission.ContentGeoLocation } -> SiteAsk.Location
            p.any { it is Permission.ContentPersistentStorage } -> SiteAsk.Storage
            p.any { it is Permission.ContentMediaKeySystemAccess || it is Permission.ContentProtectedMediaId } -> SiteAsk.ProtectedMedia
            p.any { it is Permission.ContentCrossOriginStorageAccess } -> SiteAsk.CrossSiteCookies
            p.any { it is Permission.ContentLocalNetworkAccess || it is Permission.ContentLocalDeviceAccess } -> SiteAsk.LocalNetwork
            else -> null
        }
    }

    /** A decision remembered for the site of [url]: true allow, false block, null ask. */
    suspend fun remembered(url: String, ask: SiteAsk): Boolean? {
        val prefs = context.siteDecisions.data.first()
        val values = kinds(ask).map { prefs[key(originOf(url), it)] }
        return when {
            values.all { it == ALLOW } -> true
            values.any { it == BLOCK } -> false
            else -> null
        }
    }

    /**
     * The owner answered the card. [always]: remember it for the site. Otherwise ("Allow once",
     * or the card dismissed = blocked this time) it holds for this request only.
     */
    suspend fun answer(request: PermissionRequest, ask: SiteAsk, allow: Boolean, always: Boolean) {
        if (allow) request.grant() else request.reject()
        if (!always) return
        val origin = originOf(request.uri ?: return)
        context.siteDecisions.edit { prefs -> kinds(ask).forEach { prefs[key(origin, it)] = if (allow) ALLOW else BLOCK } }
    }

    /** Everything decided for the site of [url]. */
    suspend fun decisions(url: String): List<SiteDecision> {
        val origin = originOf(url)
        val prefs = context.siteDecisions.data.first()
        val own = SiteAsk.entries.filter { it != SiteAsk.CameraAndMicrophone }.mapNotNull { ask ->
            when (prefs[key(origin, ask.name)]) {
                ALLOW -> SiteDecision(ask, true, null)
                BLOCK -> SiteDecision(ask, false, null)
                else -> null
            }
        }
        val gecko = geckoRecords(url).mapNotNull { p ->
            val ask = geckoAsk(p.permission)?.takeIf { a -> own.none { it.ask == a } } ?: return@mapNotNull null
            when (p.value) {
                ContentPermission.VALUE_ALLOW -> SiteDecision(ask, true, p)
                ContentPermission.VALUE_DENY -> SiteDecision(ask, false, p)
                else -> null
            }
        }
        return own + gecko
    }

    /** Flips a decision from Site settings. */
    suspend fun set(url: String, decision: SiteDecision, allow: Boolean) {
        val raw = decision.raw
        if (raw != null) {
            runtime.storageController.setPermission(raw, if (allow) ContentPermission.VALUE_ALLOW else ContentPermission.VALUE_DENY)
        } else {
            context.siteDecisions.edit { it[key(originOf(url), decision.ask.name)] = if (allow) ALLOW else BLOCK }
        }
    }

    /**
     * Everything the site left on the phone: cookies, storage, caches, logins, and what it was
     * allowed or blocked (Koan's answers and Gecko's records). False when Gecko failed.
     */
    suspend fun clearSiteData(url: String): Boolean {
        val host = android.net.Uri.parse(url).host ?: return false
        val origin = originOf(url)
        context.siteDecisions.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith("$origin|") }.forEach { prefs.remove(it) }
        }
        geckoRecords(url).filter { it.value != ContentPermission.VALUE_PROMPT }
            .forEach { runtime.storageController.setPermission(it, ContentPermission.VALUE_PROMPT) }
        val flags = StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.DOM_STORAGES or
            StorageController.ClearFlags.AUTH_SESSIONS or StorageController.ClearFlags.NETWORK_CACHE or
            StorageController.ClearFlags.IMAGE_CACHE
        return runtime.storageController.clearDataFromBaseDomain(host, flags).awaitDone()
    }

    private suspend fun geckoRecords(url: String): List<ContentPermission> =
        runtime.storageController.getPermissions(url).await().orEmpty()

    private fun geckoAsk(type: Int): SiteAsk? = when (type) {
        PermissionDelegate.PERMISSION_GEOLOCATION -> SiteAsk.Location
        PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> SiteAsk.Storage
        PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> SiteAsk.ProtectedMedia
        PermissionDelegate.PERMISSION_STORAGE_ACCESS -> SiteAsk.CrossSiteCookies
        PermissionDelegate.PERMISSION_LOCAL_NETWORK_ACCESS, PermissionDelegate.PERMISSION_LOCAL_DEVICE_ACCESS -> SiteAsk.LocalNetwork
        PermissionDelegate.PERMISSION_XR -> SiteAsk.VirtualReality
        else -> null
    }

    private fun kinds(ask: SiteAsk): List<String> = when (ask) {
        SiteAsk.CameraAndMicrophone -> listOf(SiteAsk.Camera.name, SiteAsk.Microphone.name)
        else -> listOf(ask.name)
    }

    private fun key(origin: String, kind: String) = stringPreferencesKey("$origin|$kind")

    private companion object {
        const val ALLOW = "allow"
        const val BLOCK = "block"

        fun originOf(url: String): String {
            val uri = android.net.Uri.parse(url)
            return "${uri.scheme}://${uri.host}" + if (uri.port > 0) ":${uri.port}" else ""
        }
    }
}

/** Waits for a GeckoResult that has no value: true when it succeeded. */
private suspend fun GeckoResult<Void>.awaitDone(): Boolean = suspendCancellableCoroutine { cont ->
    accept({ if (cont.isActive) cont.resume(true) }, { if (cont.isActive) cont.resume(false) })
}

/** Waits for a GeckoResult; null when it fails. */
private suspend fun <T> GeckoResult<T>.await(): T? = suspendCancellableCoroutine { cont ->
    accept({ if (cont.isActive) cont.resume(it) }, { if (cont.isActive) cont.resume(null) })
}
