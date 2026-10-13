package dev.nerzouille.koan.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.ui.theme.KoanColors
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.feature.session.SessionFeature
import mozilla.components.lib.state.ext.observeAsComposableState

/** Page view: the live engine view on top, our own toolbar at the bottom. */
@Composable
fun BrowserScreen(
    components: Components,
    tabCount: Int,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onTabs: () -> Unit,
    onDebugFill: () -> Unit,
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

    val tab by store.observeAsComposableState { it.selectedTab }

    val content = tab?.content
    BackHandler(enabled = !editing && content?.canGoBack == true) {
        sessionFeature.onBackPressed()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(KoanColors.Base)
            .imePadding(),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            AndroidView(factory = { engineView }, modifier = Modifier.fillMaxSize())
        }
        BrowserToolbar(
            url = content?.url.orEmpty(),
            loading = content?.loading == true,
            progress = (content?.progress ?: 0) / 100f,
            canGoBack = content?.canGoBack == true,
            canGoForward = content?.canGoForward == true,
            tabCount = tabCount,
            editing = editing,
            onEditingChange = onEditingChange,
            onSubmit = { input ->
                onEditingChange(false)
                session.loadUrl(resolveInput(input))
                engineView.asView().requestFocus()
            },
            onBack = { sessionFeature.onBackPressed() },
            onForward = { session.goForward(store.state.selectedTabId) },
            onReload = { session.reload() },
            onStop = { session.stopLoading() },
            onTabs = onTabs,
            onDebugFill = onDebugFill,
            modifier = Modifier
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    // Swipe up on the toolbar opens the overview.
                    var total = 0f
                    detectVerticalDragGestures(onDragStart = { total = 0f }) { change, dy ->
                        total += dy
                        if (total < -40.dp.toPx()) {
                            total = Float.POSITIVE_INFINITY
                            onTabs()
                        }
                        change.consume()
                    }
                },
        )
    }
}
