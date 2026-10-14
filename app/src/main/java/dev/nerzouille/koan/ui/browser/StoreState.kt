package dev.nerzouille.koan.ui.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun <R> BrowserStore.observeSlice(key: Any?, map: (BrowserState) -> R): State<R> {
    // Recomputed in the same frame the key changes: a stale slice for one frame was enough for the
    // new-tab sheet to start with the previous page's url.
    val slice = remember(this, key) { mutableStateOf(map(state)) }
    LaunchedEffect(slice) {
        flow().map(map).distinctUntilChanged().collect { slice.value = it }
    }
    return slice
}
