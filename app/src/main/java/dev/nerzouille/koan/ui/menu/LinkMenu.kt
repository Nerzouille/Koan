package dev.nerzouille.koan.ui.menu

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.nerzouille.koan.favicon.SvgRasterizer
import dev.nerzouille.koan.ui.overview.Favicon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.nerzouille.koan.search.displayHost
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.sheet.SheetDivider
import dev.nerzouille.koan.ui.sheet.SheetGroup
import dev.nerzouille.koan.ui.sheet.SheetRow
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SheetTokens
import mozilla.components.concept.engine.HitResult

/** What a long press landed on: a link, an image, or an image that is also a link. */
@Immutable
data class LinkTarget(val link: String?, val image: String?, val label: String, val video: String? = null) {
    companion object {
        /** Null for what Koan has no menu for (text, phone numbers, media…). */
        fun of(hit: HitResult): LinkTarget? = when (hit) {
            is HitResult.UNKNOWN -> hit.src.takeIf(::isWeb)?.let { LinkTarget(it, null, hit.linkText ?: "") }
            is HitResult.IMAGE_SRC -> LinkTarget(hit.uri.takeIf(::isWeb), hit.src.takeIf(::isWeb), "")
            is HitResult.IMAGE -> hit.src.takeIf(::isWeb)?.let { LinkTarget(null, it, hit.title ?: "") }
            // A video: only a plain file (mp4, webm) has a menu (download, open, copy); a stream has none.
            is HitResult.VIDEO -> dev.nerzouille.koan.ui.browser.downloadableVideo(hit.src)?.let { LinkTarget(null, null, hit.title ?: "", video = it) }
            else -> null
        }

        private fun isWeb(url: String) = url.startsWith("http://") || url.startsWith("https://")

        /**
         * Google's small preview of an image (encrypted-tbn0.gstatic.com/images?q=tbn:…), shown
         * while the full one loads (Google Images): saving it would save the preview, under
         * another name and size. Koan asks the page for the full one instead (PageImages).
         */
        fun isPreview(url: String): Boolean {
            val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
            val host = uri.host?.lowercase().orEmpty()
            return host.endsWith(".gstatic.com") && host.contains("tbn") && uri.rawQuery.orEmpty().contains("tbn")
        }
    }
}

/** What the long-press menu can do with a [LinkTarget]. */
@Immutable
class LinkActions(
    val openNew: (String) -> Unit,
    val openBackground: (String) -> Unit,
    val copy: (String) -> Unit,
    val share: (String) -> Unit,
    val saveImage: (String) -> Unit,
)

/** The long-press menu, on a floating card: the link (or image) on top, then what to do with it. */
@Composable
fun LinkMenu(target: LinkTarget, interactive: Boolean, actions: LinkActions, imageModifier: Modifier = Modifier, waiting: Boolean = false) {
    val palette = KoanTheme.colors
    val shown = target.link ?: target.image ?: target.video.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(SheetTokens.Gap)) {
        // What was pressed: the image itself, then the link's site icon, title and address.
        target.image?.let { LinkImage(it, imageModifier) }
        Row(Modifier.padding(horizontal = SheetTokens.RowPadding / 2), verticalAlignment = Alignment.CenterVertically) {
            if (target.link != null) {
                Favicon(target.link, SheetTokens.RowIconBox)
                Spacer(Modifier.width(SheetTokens.RowPadding))
            }
            Column {
                Text(
                    target.label.trim().ifEmpty { displayHost(shown) },
                    color = palette.TextPrimary,
                    fontSize = SheetTokens.RowTitleSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(shown, color = palette.TextSecondary, fontSize = SheetTokens.RowSubtitleSize, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        target.link?.let { link ->
            SheetGroup {
                SheetRow(KoanIcons.Plus, "Open in new tab", interactive, onClick = { actions.openNew(link) })
                SheetDivider()
                SheetRow(KoanIcons.Essentials, "Open in background", interactive, onClick = { actions.openBackground(link) })
                SheetDivider()
                SheetRow(KoanIcons.Link, "Copy link", interactive, onClick = { actions.copy(link) })
                SheetDivider()
                SheetRow(KoanIcons.Share, "Share link", interactive, onClick = { actions.share(link) })
            }
        }
        target.video?.let { video ->
            SheetGroup {
                SheetRow(KoanIcons.Download, "Download video", interactive, onClick = { actions.saveImage(video) })
                SheetDivider()
                SheetRow(KoanIcons.Plus, "Open video in new tab", interactive, onClick = { actions.openNew(video) })
                SheetDivider()
                SheetRow(KoanIcons.Link, "Copy video link", interactive, onClick = { actions.copy(video) })
            }
        }
        target.image?.let { image ->
            SheetGroup {
                // A press on Google's small preview: Save waits for the full image at that spot.
                SheetRow(
                    KoanIcons.Download, "Save image", interactive,
                    subtitle = if (waiting) "Waiting for the full image…" else null,
                    onClick = if (waiting) null else ({ actions.saveImage(image) }),
                )
                SheetDivider()
                SheetRow(KoanIcons.Plus, "Open image in new tab", interactive, onClick = { actions.openNew(image) })
                SheetDivider()
                SheetRow(KoanIcons.Link, "Copy image link", interactive, onClick = { actions.copy(image) })
            }
        }
    }
}

/**
 * The pressed image on top of the card. Its slot (fixed height, neutral grey) is there from the
 * first frame, so the card never changes size; the image fades in inside it once it's loaded.
 */
@Composable
private fun LinkImage(url: String, modifier: Modifier) {
    val image by produceState<ImageBitmap?>(null, url) { value = withContext(Dispatchers.IO) { loadPreview(url) } }
    val shown = remember(url) { Animatable(0f) }
    LaunchedEffect(image) { if (image != null) shown.animateTo(1f, tween(SheetTokens.IMAGE_FADE_MS)) }
    Box(
        modifier
            .fillMaxWidth()
            .height(SheetTokens.LinkImageHeight)
            .clip(RoundedCornerShape(SheetTokens.GroupRadius))
            .background(KoanTheme.colors.GroupedRow),
    ) {
        image?.let { bitmap ->
            Image(
                bitmap, null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = shown.value },
            )
        }
    }
}

/** Downloads [url] (small images only) and decodes it scaled down for the preview. */
private fun loadPreview(url: String): ImageBitmap? = runCatching {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = PREVIEW_TIMEOUT_MS
        readTimeout = PREVIEW_TIMEOUT_MS
    }
    val bytes = connection.inputStream.use { input ->
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > PREVIEW_MAX_BYTES) return@runCatching null
        }
        out.toByteArray()
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= PREVIEW_MAX_PX) sample *= 2
    val raster = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    // Android can't decode SVG (logos often are): the favicon rasterizer can.
    val bitmap = raster ?: String(bytes).takeIf { "<svg" in it }?.let { SvgRasterizer.rasterize(it, PREVIEW_MAX_PX, url) }
    bitmap?.asImageBitmap()
}.getOrNull()

private const val PREVIEW_TIMEOUT_MS = 4000
private const val PREVIEW_MAX_BYTES = 4 * 1024 * 1024
private const val PREVIEW_MAX_PX = 800
