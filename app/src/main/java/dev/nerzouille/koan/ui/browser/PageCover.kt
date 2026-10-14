package dev.nerzouille.koan.ui.browser

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.thumbs.ThumbnailStore
import dev.nerzouille.koan.ui.overview.Favicon
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.Motion
import dev.nerzouille.koan.ui.theme.PageTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import mozilla.components.lib.state.ext.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.store.BrowserStore

/** What the cover shows while a tab comes up: its last screenshot, else its favicon and domain. */
@Immutable
data class CoverTarget(
    val id: String,
    val url: String,
    val icon: String?,
    val staticIcon: Boolean,
    val thumb: String?,
    /** A new tab waiting for its url: covered (plain surface) until it loads something. */
    val blank: Boolean,
) {
    companion object {
        fun of(tab: TabEntity, blank: Boolean) = CoverTarget(tab.koanTabId, tab.url, tab.iconUrl, tab.staticIcon, tab.thumbnailKey, blank)
    }
}

/**
 * Drawn over the engine view: never a white flash. When the page view switches to a tab, the cover
 * shows that tab's screenshot at once (or its placeholder), and fades out once the live page has
 * painted. [zoomIn]: the switch came from tapping a card, so the screenshot grows into place.
 * [target] null = no tab at all: plain surface.
 */
@Composable
fun PageCover(target: CoverTarget?, store: BrowserStore, zoomIn: () -> Boolean, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }
    val id = target?.id
    val blank = target?.blank == true
    LaunchedEffect(id, blank) {
        alpha.snapTo(1f)
        if (target == null || blank) return@LaunchedEffect
        if (zoomIn()) {
            launch {
                scale.snapTo(PageTokens.OPEN_ZOOM_FROM)
                scale.animateTo(1f, Motion.Overview)
            }
        }
        withTimeoutOrNull(PageTokens.COVER_MAX_MS) { awaitPainted(store, target.id) }
        alpha.animateTo(0f, tween(PageTokens.COVER_FADE_MS))
    }
    val palette = KoanTheme.colors
    val current = target
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(palette.Base),
    ) {
        if (current == null || current.blank) return@Box
        val image = rememberThumbnail(current.thumb)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        ) {
            if (image != null) {
                Image(
                    image,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CoverPlaceholder(current)
            }
        }
    }
}

/** A never-opened tab: the card placeholder, full screen (big favicon, domain). */
@Composable
private fun CoverPlaceholder(target: CoverTarget) {
    Column(
        Modifier.fillMaxSize().background(KoanTheme.colors.Card),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(CardTokens.PlaceholderTop * 4))
        Favicon(target.url, CardTokens.PlaceholderFavicon, icon = target.icon, staticIcon = target.staticIcon)
        Spacer(Modifier.height(CardTokens.PlaceholderGap))
        Text(displayHost(target.url), color = KoanTheme.colors.TextSecondary, fontSize = CardTokens.PlaceholderDomainSize, maxLines = 1)
    }
}

/** A screenshot from memory in the first frame, or decoded off the main thread. */
@Composable
fun rememberThumbnail(key: String?): ImageBitmap? {
    var image by remember(key) { mutableStateOf(key?.let(ThumbnailStore::cached)) }
    if (image == null && key != null) {
        LaunchedEffect(key) { image = ThumbnailStore.load(key) }
    }
    return image
}

/**
 * Until tab [id] is selected and has painted. A tab coming up from nothing (never opened,
 * suspended) must first report its paint reset, so a stale "painted" from before it was suspended
 * never lifts the cover early. A live tab switches surfaces in a frame or two; if GeckoView reports
 * a reset for it, that is waited out too.
 */
private suspend fun awaitPainted(store: BrowserStore, id: String) {
    val start = store.state.findTab(id)
    val live = start?.engineState?.engineSession != null && start.content.firstContentfulPaint
    if (live) {
        store.flow().first { it.selectedTabId == id }
        val reset = withTimeoutOrNull(LIVE_RESET_WAIT_MS) { store.flow().first { it.findTab(id)?.content?.firstContentfulPaint == false } }
        if (reset == null) return
    } else {
        store.flow().first { it.findTab(id)?.content?.firstContentfulPaint != true }
    }
    store.flow().first { s ->
        s.selectedTabId == id && s.findTab(id)?.let { it.engineState.engineSession != null && it.content.firstContentfulPaint } == true
    }
    // The paint is reported before it reaches the screen.
    delay(PAINT_TO_SCREEN_MS)
}

private const val LIVE_RESET_WAIT_MS = 120L
private const val PAINT_TO_SCREEN_MS = 32L
