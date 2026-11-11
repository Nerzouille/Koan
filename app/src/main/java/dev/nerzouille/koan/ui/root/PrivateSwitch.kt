package dev.nerzouille.koan.ui.root

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.fragment.app.FragmentActivity
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.engine.TabManager
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.browser.TabSwipe
import dev.nerzouille.koan.ui.kit.animationsOff
import dev.nerzouille.koan.ui.overview.findTab
import dev.nerzouille.koan.ui.privacy.PrivateFlip
import dev.nerzouille.koan.ui.privacy.unlockPrivate
import dev.nerzouille.koan.ui.sheet.SheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.findTab

/**
 * Private mode on and off (OVERVIEW_PRIVATE.md §4, §6), moved out of the root as it was: the flip,
 * the fingerprint, and what the switch resets. Built by the root on each composition, like the
 * local functions it replaces, so it always sees the current tab manager and sheets.
 */
internal class PrivateSwitch(
    private val components: Components,
    private val root: RootState,
    private val privateOnState: MutableState<Boolean>,
    private val tabs: () -> TabManager,
    private val tabSwipe: TabSwipe,
    private val overlays: Overlays,
    private val drawer: SheetState,
    private val flip: PrivateFlip,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    private val privateMode = components.privateMode
    private val privateTabs = components.privateTabs
    private val fragmentActivity = context as? FragmentActivity

    /** Switches what is shown; the flip calls it once the old tabs have left. */
    fun show(on: Boolean) {
        if (privateOnState.value == on) return
        // A video left fullscreen in the mode being left would turn the screen when it shows again.
        dev.nerzouille.koan.ui.browser.leaveFullscreen(components)
        root.editing = false
        root.startPage = false
        tabSwipe.cancel()
        tabs().cancelBlank()
        overlays.dismissAll()
        drawer.snapClosed()
        if (on) root.normalSpaceUuid = root.currentSpaceUuid else root.currentSpaceUuid = root.normalSpaceUuid
        privateOnState.value = on
        privateMode.on.value = on
        // The engine shows this mode's current tab (under the overview, or on the page).
        val manager = if (on) privateTabs.tabs else components.tabs
        val list = if (on) privateTabs.spaces.spaces.value else components.spaces.spaces.value
        val row = manager.current.value?.let { id -> list.firstNotNullOfOrNull { it.findTab(id) } }
        if (row != null) {
            manager.open(row, captureLeaving = false)
        } else if (!on && components.store.state.selectedTabId?.let { components.store.state.findTab(it) }?.content?.private == true) {
            // No normal tab to show: a private page must not stay selected under normal mode.
            components.store.state.tabs.firstOrNull { !it.content.private }?.let { components.tabsUseCases.selectTab(it.id) }
        }
    }
    /** The header's toggle: the fingerprint first if the private tabs are locked, then the flip. */
    fun toggle() {
        if (flip.running) return
        val on = !flip.target
        scope.launch {
            if (on && privateMode.needsUnlock()) {
                if (fragmentActivity == null || !unlockPrivate(fragmentActivity)) return@launch
            }
            if (on) privateMode.locked.value = false
            flip.reduced = animationsOff(context)
            flip.run(on) { show(on) }
        }
    }
    /** Back to normal tabs at once (a link from another app, "Back to normal tabs" on the lock). */
    fun leave() {
        if (!privateOnState.value) return
        flip.snap(false)
        show(false)
    }
}
