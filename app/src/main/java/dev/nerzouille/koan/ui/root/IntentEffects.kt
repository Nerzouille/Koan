package dev.nerzouille.koan.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.Incoming
import dev.nerzouille.koan.engine.Opener
import dev.nerzouille.koan.engine.TabManager
import dev.nerzouille.koan.search.SearchEngine
import dev.nerzouille.koan.search.resolveInput
import dev.nerzouille.koan.search.resolveShared
import dev.nerzouille.koan.ui.NO_CARD
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.browser.TabSwipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Links and text other apps send (default browser, share), moved out of the root as they were: a
 * new normal tab in the current space, then the page view once that tab is the current one.
 */
@Composable
internal fun IncomingLinks(
    components: Components,
    root: RootState,
    tabsStarted: Boolean,
    spacesEmpty: Boolean,
    searchEngine: SearchEngine,
    activeTabId: () -> String?,
    tabSwipe: TabSwipe,
    tabs: () -> TabManager,
    overlays: Overlays,
    leavePrivate: () -> Unit,
    openNewTab: (url: String, select: Boolean, nextToCurrent: Boolean, fromApp: Opener.App?) -> String?,
) {
    LaunchedEffect(tabsStarted, spacesEmpty, root.placeRestored) {
        if (!tabsStarted || spacesEmpty || !root.placeRestored) return@LaunchedEffect
        components.incoming.collect { incoming ->
            if (incoming == null) return@collect
            // A link from another app opens as a normal tab, never in Private.
            leavePrivate()
            components.incoming.value = null
            tabSwipe.cancel()
            tabs().cancelBlank()
            overlays.dismissAll()
            overlays.menu.snapClosed()
            overlays.settingsOpen = false
            overlays.historyOpen = false
            overlays.downloadsOpen = false
            val url = when (incoming.kind) {
                Incoming.Kind.Url -> incoming.text
                Incoming.Kind.Shared -> resolveShared(incoming.text, searchEngine)
                Incoming.Kind.Selected -> resolveInput(incoming.text, searchEngine)
            }
            root.landing = openNewTab(url, true, false, Opener.App(incoming.referrer, url))
        }
    }
    // The app's tab is created a moment later (TabManager): until it is the current one, nothing
    // may put the overview up (no current tab, at launch), and then it shows, in the page view.
    LaunchedEffect(root.landing) {
        val id = root.landing ?: return@LaunchedEffect
        withTimeoutOrNull(LANDING_TIMEOUT_MS) { snapshotFlow { activeTabId() }.first { it == id } }
        if (activeTabId() == id && root.overviewOpen) {
            root.heroPick = NO_CARD
            root.overviewOpen = false
        }
        root.landing = null
    }
}

/** How long an incoming link's tab may take to become the current one (it is a Room write). */
private const val LANDING_TIMEOUT_MS = 5_000L
