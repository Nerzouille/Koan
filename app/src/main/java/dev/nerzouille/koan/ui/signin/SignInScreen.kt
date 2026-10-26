package dev.nerzouille.koan.ui.signin

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.nerzouille.koan.ui.kit.PrimaryButton
import dev.nerzouille.koan.ui.kit.QuietButton
import dev.nerzouille.koan.sync.PairingFlow
import android.Manifest
import android.app.Activity
import android.provider.Settings
import android.content.ClipboardManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SignInTokens
import kotlinx.coroutines.launch
import java.io.IOException
import dev.nerzouille.koan.ui.topSafePadding

/** What the sign-in screen shows: the choice, Mozilla's page (email and password), or sign-in from a computer. */
private sealed interface SignInMode {
    /** First: which of the two ways. */
    data object Choice : SignInMode
    data object Password : SignInMode
    /** Scan the computer's code (once the camera is allowed), or paste its link. */
    data object Computer : SignInMode
    /** Mozilla's pairing page for a scanned or pasted link. */
    class Pairing(val flow: PairingFlow) : SignInMode
}

/**
 * Mozilla sign-in, dressed as part of Koan: dark header, the accounts page fading in only once it
 * has painted (never a white flash), a thin progress line, and quiet notices under the header.
 * Two ways in: Mozilla's page with email and password ([onLogin]: fetches the keys, stores the
 * session), or from a computer, by scanning its "Connect another device" code ([onPairing]: swaps
 * the code for tokens and the Sync key). Both throw on failure. [pairLink]: a pairing link Koan
 * was opened with (the system camera): goes straight to it.
 */
@Composable
fun SignInScreen(
    onLogin: suspend (FxAWebLogin) -> Unit,
    onPairing: suspend (PairingFlow, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    pairLink: String? = null,
) {
    val scope = rememberCoroutineScope()
    var finishing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    val pageAlpha = remember { Animatable(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var mode by remember { mutableStateOf<SignInMode>(SignInMode.Choice) }
    val context = LocalContext.current

    fun pair(link: String) {
        val flow = runCatching { PairingFlow(link) }.getOrNull()
            ?: return run { notice = "That isn't the code from \"Connect another device\". Try again." }
        notice = null
        progress = 0f
        mode = SignInMode.Pairing(flow)
    }
    LaunchedEffect(pairLink) { if (pairLink != null) pair(pairLink) }

    BackHandler {
        val web = webView
        when {
            finishing -> Unit
            web != null && web.canGoBack() -> web.goBack()
            mode is SignInMode.Pairing -> mode = SignInMode.Computer
            mode != SignInMode.Choice -> { notice = null; mode = SignInMode.Choice }
            else -> onClose()
        }
    }
    // Each page fades in on its own, once painted.
    LaunchedEffect(mode) { pageAlpha.snapTo(0f) }
    // One page at a time: leaving it (or the screen) ends it, and only it.
    val release = { view: WebView ->
        if (webView === view) webView = null
        view.destroy()
    }

    fun failure(e: Throwable) = when (e) {
        is IOException -> "No connection. Check it and try again."
        is SyncError.StorageUnavailable -> "Couldn't store the session securely on this phone."
        else -> "Mozilla didn't accept the sign-in. Try again."
    }

    fun finish(login: FxAWebLogin) {
        if (finishing) return
        finishing = true
        notice = null
        scope.launch {
            runCatching { onLogin(login) }.onFailure { e ->
                finishing = false
                notice = failure(e)
            }
        }
    }

    fun finishPairing(flow: PairingFlow, code: String) {
        if (finishing) return
        finishing = true
        notice = null
        scope.launch {
            runCatching { onPairing(flow, code) }.onFailure { e ->
                finishing = false
                notice = failure(e)
                mode = SignInMode.Computer
            }
        }
    }

    fun cameraGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var camera by remember { mutableStateOf(cameraGranted()) }
    // Refused for good ("Never allow"): Android won't ask again, only its settings can allow it.
    var cameraBlocked by remember { mutableStateOf(false) }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        camera = granted
        if (!granted) {
            cameraBlocked = (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
            notice = "Koan needs the camera to scan the code. You can paste the link instead."
        }
    }
    fun allowCamera() {
        notice = null
        if (!cameraBlocked) return askCamera.launch(Manifest.permission.CAMERA)
        val settings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        runCatching { context.startActivity(settings) }
    }
    // Back from the app's settings with the camera allowed: the viewfinder starts.
    LifecycleResumeEffect(Unit) {
        if (!camera && cameraGranted()) camera = true
        onPauseOrDispose { }
    }
    // The camera is only asked for on a tap: choosing this way in, or "Allow the camera".
    fun fromComputer() {
        notice = null
        mode = SignInMode.Computer
        camera = cameraGranted()
        if (!camera) askCamera.launch(Manifest.permission.CAMERA)
    }
    fun withPassword() {
        notice = null
        mode = SignInMode.Password
    }
    fun paste() {
        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text != null && PairingFlow.isPairingLink(text)) {
            pair(text)
        } else {
            notice = "No pairing link in the clipboard. Copy the link of the code (e.g. with Google Lens), then try again."
        }
    }

    // Login UI stays neutral: never the brand gradient or a space colour.
    val accent = KoanTheme.colors.TextSecondary
    // Mozilla's pages follow Koan's theme, not the system's.
    val dark = KoanTheme.colors.isDark
    Column(
        modifier
            .fillMaxSize()
            .background(KoanTheme.colors.Base)
            .topSafePadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(SignInTokens.HeaderHeight)
                .padding(horizontal = SignInTokens.HeaderPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(SignInTokens.CloseTouch)
                    .clip(CircleShape)
                    .clickable(enabled = !finishing, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(KoanIcons.Close, "Close", tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SignInTokens.CloseGlyph))
            }
            // The choice page has its own big title.
            if (mode != SignInMode.Choice) {
                Spacer(Modifier.width(SignInTokens.HeaderPadding))
                Column {
                    Text("Sync your Zen Spaces", color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.TitleSize, fontWeight = FontWeight.Bold)
                    Text("Mozilla account", color = KoanTheme.colors.TextSecondary, fontSize = SignInTokens.SubtitleSize)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(SignInTokens.ProgressHeight)
                .drawBehind {
                    val p = if (finishing) 1f else progress
                    if (p in 0.01f..0.99f || finishing) {
                        drawRect(accent, size = Size(size.width * p, size.height), alpha = 0.8f)
                    }
                },
        )
        // Mozilla's page needs crypto.randomUUID (WebView 92+): an older one would only show a blank page.
        // Checked again on resume, after a trip to the Play Store.
        var webViewTooOld by remember { mutableStateOf(oldWebView()) }
        LifecycleResumeEffect(Unit) {
            webViewTooOld = oldWebView()
            onPauseOrDispose { }
        }
        val outdated = webViewTooOld
        if (outdated != null) {
            OutdatedWebView(outdated, Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        val current = mode
        Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
            val painted = { scope.launch { if (pageAlpha.targetValue < 1f) pageAlpha.animateTo(1f, tween(SignInTokens.FADE_MS)) }; Unit }
            when (current) {
                SignInMode.Choice -> Choice(onComputer = ::fromComputer, onPassword = ::withPassword)
                SignInMode.Password -> WebPage(
                    switchText = "Sign in from your computer instead",
                    showSwitch = !finishing,
                    onSwitch = ::fromComputer,
                ) {
                    AndroidView(
                        factory = { context ->
                            signInWebView(
                                themedForWeb(context, dark),
                                onLogin = ::finish,
                                onUnverified = { notice = "Confirm the sign-in from the email Mozilla just sent, then come back here." },
                                onPainted = painted,
                                onProgress = { progress = it / 100f },
                                onError = { notice = it },
                            ).also { webView = it }
                        },
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pageAlpha.value },
                        onRelease = release,
                    )
                }
                SignInMode.Computer -> FromComputer(
                    camera = camera,
                    enabled = !finishing,
                    onCode = { pair(it) },
                    onCameraError = {
                        camera = false
                        notice = "Couldn't use the camera. You can paste the link instead."
                    },
                    onAllowCamera = ::allowCamera,
                    onPaste = ::paste,
                    onPassword = ::withPassword,
                )
                is SignInMode.Pairing -> key(current.flow) {
                    WebPage(
                        switchText = "Use email and password instead",
                        showSwitch = !finishing,
                        onSwitch = ::withPassword,
                    ) {
                        AndroidView(
                            factory = { context ->
                                pairingWebView(
                                    themedForWeb(context, dark),
                                    current.flow,
                                    onCode = { code -> finishPairing(current.flow, code) },
                                    onPainted = painted,
                                    onProgress = { progress = it / 100f },
                                    onError = { notice = it },
                                ).also { webView = it }
                            },
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pageAlpha.value },
                            onRelease = release,
                        )
                    }
                }
            }
            notice?.let { text ->
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(SignInTokens.NoticeMargin)
                        .clip(RoundedCornerShape(SignInTokens.NoticeRadius))
                        .background(KoanTheme.colors.Notice)
                        .clickable { notice = null }
                        .padding(SignInTokens.NoticePadding),
                ) {
                    Text(text, color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.NoticeSize)
                }
            }
            if (finishing) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(SignInTokens.NoticeMargin)
                        .clip(CircleShape)
                        .background(KoanTheme.colors.Notice)
                        .padding(SignInTokens.NoticePadding),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // The page stays usable: a first login may still need the email confirmation.
                    Text("Connecting to your Zen Spaces…", color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.NoticeSize)
                }
            }
        }
    }
}

/** The system WebView's package if it is too old for Mozilla's sign-in page, else null. */
private fun oldWebView(): String? {
    val pkg = WebView.getCurrentWebViewPackage() ?: return null
    val major = pkg.versionName?.substringBefore('.')?.toIntOrNull() ?: return null
    return pkg.packageName.takeIf { major < SignInTokens.MIN_WEBVIEW }
}

/** Instead of a blank page: what to update, and a button to its Play Store page. */
@Composable
private fun OutdatedWebView(webViewPackage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier.padding(horizontal = SignInTokens.OutdatedPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Your Android System WebView is out of date. Update it from the Play Store to sign in.",
            color = KoanTheme.colors.TextPrimary,
            fontSize = SignInTokens.OutdatedSize,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SignInTokens.OutdatedGap))
        PrimaryButton("Open Play Store", onClick = {
            val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$webViewPackage"))
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$webViewPackage"))
            runCatching { context.startActivity(market) }.recoverCatching { context.startActivity(web) }
        })
    }
}
