package dev.nerzouille.koan.ui.signin

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import dev.nerzouille.koan.ui.theme.KoanColors
import dev.nerzouille.koan.ui.theme.SignInTokens
import kotlinx.coroutines.launch
import java.io.IOException

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

    Column(
        modifier
            .fillMaxSize()
            .background(KoanColors.Base)
            .statusBarsPadding(),
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
                Icon(KoanIcons.Close, "Close", tint = KoanColors.TextPrimary, modifier = Modifier.size(SignInTokens.CloseGlyph))
            }
            Spacer(Modifier.width(SignInTokens.HeaderPadding))
            Column {
                Text("Connect your Zen Spaces", color = KoanColors.TextPrimary, fontSize = SignInTokens.TitleSize, fontWeight = FontWeight.Bold)
                Text("Mozilla account · read-only", color = KoanColors.TextSecondary, fontSize = SignInTokens.SubtitleSize)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(SignInTokens.ProgressHeight)
                .drawBehind {
                    val p = if (finishing) 1f else progress
                    if (p in 0.01f..0.99f || finishing) {
                        drawRect(KoanColors.DefaultAccent, size = Size(size.width * p, size.height), alpha = 0.8f)
                    }
                },
        )
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
                        .background(KoanColors.Notice)
                        .clickable { notice = null }
                        .padding(SignInTokens.NoticePadding),
                ) {
                    Text(text, color = KoanColors.TextPrimary, fontSize = SignInTokens.NoticeSize)
                }
            }
            if (finishing) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(SignInTokens.NoticeMargin)
                        .clip(CircleShape)
                        .background(KoanColors.Notice)
                        .padding(SignInTokens.NoticePadding),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // The page stays usable: a first login may still need the email confirmation.
                    Text("Connecting to your Zen Spaces…", color = KoanColors.TextPrimary, fontSize = SignInTokens.NoticeSize)
                }
            }
        }
    }
}
