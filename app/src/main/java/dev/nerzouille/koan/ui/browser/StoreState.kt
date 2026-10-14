package dev.nerzouille.koan.ui.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.ext.flow
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore

/**
 * A slice of the engine state as Compose state; recomposes only when the slice changes. Unlike
 * observeAsComposableState, [map] is re-applied when [key] changes (e.g. the current tab id).
 */
@Composable
fun <R> BrowserStore.observeSlice(key: Any?, map: (BrowserState) -> R): State<R> =
    produceState(map(state), this, key) {
        flow().map(map).distinctUntilChanged().collect { value = it }
    }
