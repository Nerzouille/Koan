package dev.nerzouille.koan.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.theme.KoanColors
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.feature.session.SessionFeature
import mozilla.components.lib.state.ext.observeAsComposableState

/** Page view: the live engine view, ending above the bottom bar (drawn by KoanRoot). */
@Composable
fun BrowserScreen(
    components: Components,
    editing: Boolean,
    bottomPadding: Dp,
) {
    val context = LocalContext.current
    val store = components.store
    val session = components.sessionUseCases

    val engineView = remember { GeckoEngineView(context) }
    val sessionFeature = remember {
        SessionFeature(store, session.goBack, session.goForward, engineView)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> sessionFeature.start()
                Lifecycle.Event.ON_STOP -> sessionFeature.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sessionFeature.stop()
        }
    }

    val canGoBack by store.observeAsComposableState { it.selectedTab?.content?.canGoBack == true }
    BackHandler(enabled = !editing && canGoBack == true) {
        sessionFeature.onBackPressed()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KoanColors.Base)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(bottom = bottomPadding),
    ) {
        AndroidView(factory = { engineView }, modifier = Modifier.fillMaxSize())
    }
}
