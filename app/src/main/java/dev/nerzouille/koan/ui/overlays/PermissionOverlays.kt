package dev.nerzouille.koan.ui.overlays

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.browser.observeSlice
import dev.nerzouille.koan.ui.downloads.host
import dev.nerzouille.koan.ui.kit.Toast
import dev.nerzouille.koan.ui.menu.AppLinkPrompt
import dev.nerzouille.koan.ui.menu.PermissionPrompt
import dev.nerzouille.koan.ui.sheet.Detent
import dev.nerzouille.koan.ui.sheet.KoanCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.PermissionRequest

/**
 * A site asks for something (location, camera…) on its card, a page wants an app ("Open in
 * YouTube?"), and Android's own permission behind Gecko's (Gecko asks for it itself).
 */
@Composable
internal fun PermissionOverlays(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    activeTabId: String?,
    onToast: (Toast) -> Unit,
) {
    val context = LocalContext.current
    // ---- a site asks for something (location, camera…) ----
    val request by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.permissionRequestsList?.firstOrNull() }
    val appRequest by components.store.observeSlice(activeTabId) { s -> activeTabId?.let { s.findTab(it) }?.content?.appPermissionRequestsList?.firstOrNull() }
    val sites = components.sitePermissions
    fun consume(r: PermissionRequest) { activeTabId?.let { components.store.dispatch(ContentAction.ConsumePermissionsRequest(it, r)) } }
    LaunchedEffect(request) {
        val r = request ?: return@LaunchedEffect
        sites.autoAnswer(r)?.let { allow ->
            if (allow) r.grant() else r.reject()
            consume(r)
            return@LaunchedEffect
        }
        val ask = sites.askOf(r)
        if (ask == null) {
            r.reject()
            consume(r)
            return@LaunchedEffect
        }
        val remembered = r.uri?.let { sites.remembered(it, ask) }
        if (remembered != null) {
            if (remembered) r.grant() else r.reject()
            consume(r)
            return@LaunchedEffect
        }
        overlays.permission = r to ask
        overlays.permissionCard.animateTo(Detent.Compact)
    }
    val permissionCard = overlays.permissionCard
    fun answer(allow: Boolean, always: Boolean) {
        val (r, ask) = overlays.permission ?: return
        overlays.permission = null
        permissionCard.animateTo(Detent.Closed)
        scope.launch { sites.answer(r, ask, allow, always) }
        consume(r)
    }
    // Dismissed (tap outside, Back): blocked this time, nothing remembered.
    LaunchedEffect(permissionCard.open) { if (!permissionCard.open && overlays.permission != null) answer(allow = false, always = false) }
    KoanCard(permissionCard) { interactive ->
        val (r, ask) = overlays.permission ?: return@KoanCard
        PermissionPrompt(
            host = displayHost(r.uri.orEmpty()).ifEmpty { "This site" },
            ask = ask,
            interactive = interactive,
            onBlock = { answer(allow = false, always = true) },
            onAlways = { answer(allow = true, always = true) },
            onOnce = { answer(allow = true, always = false) },
        )
    }
    // ---- a page wants an app: "Open in YouTube?" ----
    val appAsk by components.externalLinks.ask.collectAsStateWithLifecycle()
    val appLinkCard = overlays.appLinkCard
    LaunchedEffect(appAsk) { if (appAsk != null) appLinkCard.animateTo(Detent.Compact) }
    LaunchedEffect(appLinkCard.open) { if (!appLinkCard.open) components.externalLinks.ask.value = null }
    LaunchedEffect(Unit) { components.externalLinks.messages.collect { onToast(Toast(it)) } }
    KoanCard(appLinkCard) { interactive ->
        val ask = appAsk ?: return@KoanCard
        AppLinkPrompt(
            app = ask.app.label,
            icon = ask.app.icon,
            site = ask.site,
            interactive = interactive,
            onCancel = { appLinkCard.animateTo(Detent.Closed) },
            onOpen = { always ->
                appLinkCard.animateTo(Detent.Closed)
                ask.open(always)
            },
        )
    }

    // Gecko needs Android's own permission too (location, camera, microphone); it asks for it itself.
    var pendingApp by remember { mutableStateOf<PermissionRequest?>(null) }
    val askAndroid = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val r = pendingApp ?: return@rememberLauncherForActivityResult
        pendingApp = null
        if (results.values.all { it }) r.grant() else r.reject()
        activeTabId?.let { components.store.dispatch(ContentAction.ConsumeAppPermissionsRequest(it, r)) }
    }
    LaunchedEffect(appRequest) {
        val r = appRequest ?: return@LaunchedEffect
        val wanted = r.permissions.flatMap { androidPermissions(it) }.distinct()
        if (wanted.isEmpty() || wanted.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            if (wanted.isEmpty()) r.reject() else r.grant()
            activeTabId?.let { components.store.dispatch(ContentAction.ConsumeAppPermissionsRequest(it, r)) }
        } else {
            pendingApp = r
            askAndroid.launch(wanted.toTypedArray())
        }
    }
}

/** The Android permissions Gecko needs for a page's request. */
private fun androidPermissions(p: Permission): List<String> = when (p) {
    is Permission.AppLocationCoarse -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    is Permission.AppLocationFine -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    is Permission.AppCamera -> listOf(Manifest.permission.CAMERA)
    is Permission.AppAudio -> listOf(Manifest.permission.RECORD_AUDIO)
    else -> emptyList()
}
