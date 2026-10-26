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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
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

/**
 * Mozilla sign-in, dressed as part of Koan: dark header, the accounts page fading in only once it
 * has painted (never a white flash), a thin progress line, and quiet notices under the header.
 * [onLogin] finishes the login (fetches the keys, stores the session) and throws on failure.
 */
@Composable
fun SignInScreen(onLogin: suspend (FxAWebLogin) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var finishing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    val pageAlpha = remember { Animatable(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val web = webView
        if (web != null && web.canGoBack() && !finishing) web.goBack() else if (!finishing) onClose()
    }
    DisposableEffect(Unit) { onDispose { webView?.destroy() } }

    fun finish(login: FxAWebLogin) {
        if (finishing) return
        finishing = true
        notice = null
        scope.launch {
            runCatching { onLogin(login) }.onFailure { e ->
                finishing = false
                notice = when (e) {
                    is IOException -> "No connection. Check it and try again."
                    is SyncError.StorageUnavailable -> "Couldn't store the session securely on this phone."
                    else -> "Mozilla didn't accept the sign-in. Try again."
                }
            }
        }
    }

    // Login UI stays neutral: never the brand gradient or a space colour.
    val accent = KoanTheme.colors.TextSecondary
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
            Spacer(Modifier.width(SignInTokens.HeaderPadding))
            Column {
                Text("Connect your Zen Spaces", color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.TitleSize, fontWeight = FontWeight.Bold)
                Text("Mozilla account · read-only", color = KoanTheme.colors.TextSecondary, fontSize = SignInTokens.SubtitleSize)
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
        Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
            AndroidView(
                factory = { context ->
                    signInWebView(
                        context,
                        onLogin = ::finish,
                        onUnverified = { notice = "Confirm the sign-in from the email Mozilla just sent, then come back here." },
                        onPainted = { scope.launch { if (pageAlpha.targetValue < 1f) pageAlpha.animateTo(1f, tween(SignInTokens.FADE_MS)) } },
                        onProgress = { progress = it / 100f },
                        onError = { notice = it },
                    ).also { webView = it }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pageAlpha.value },
            )
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
