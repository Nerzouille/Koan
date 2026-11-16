package dev.nerzouille.koan.ui.overlays

import androidx.compose.runtime.Composable
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.engine.Components
import dev.nerzouille.koan.ui.Overlays
import dev.nerzouille.koan.ui.tour.TourOverlay
import dev.nerzouille.koan.ui.tour.featureVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The tour / "What's new": full screen, on top of everything; leaving it counts as seen. */
@Composable
internal fun TourOverlays(
    components: Components,
    overlays: Overlays,
    scope: CoroutineScope,
    onOpenNew: (url: String, select: Boolean, nextToCurrent: Boolean) -> Unit,
) {
    val closeTour = {
        overlays.tourOpen = false
        scope.launch { components.settings.setTourSeen(featureVersion(BuildConfig.VERSION_NAME)) }
    }
    TourOverlay(
        overlays.tourOpen,
        overlays.tourSheet,
        onOpenUrl = { url ->
            closeTour()
            overlays.settingsOpen = false
            onOpenNew(url, true, false)
        },
        onClosed = { closeTour() },
    )
}
