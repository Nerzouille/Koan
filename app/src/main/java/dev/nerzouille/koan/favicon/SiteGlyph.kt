package dev.nerzouille.koan.favicon

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A site's icon as the Passwords pages draw it: only one Koan already has ([FaviconLoader.cachedOnly]),
 * cut to what it really draws ([src]: transparent margins off, so every icon fills the same box),
 * whether it is an opaque tile ([tile]), and whether it can be seen at all in grey on a light or a
 * dark page (a white logo on nothing is invisible on a light page: the key shows instead).
 */
class SiteGlyph(
    val image: ImageBitmap,
    val src: IntOffset,
    val size: IntSize,
    val tile: Boolean,
    val seenOnLight: Boolean,
    val seenOnDark: Boolean,
) {
    companion object {
        private val memory = LruCache<String, Result>(256)

        private class Result(val glyph: SiteGlyph?)

        fun cached(url: String): SiteGlyph? = FaviconResolver.cacheKey(url, null, null)?.let { memory.get(it) }?.glyph

        /** Null: no usable icon on the phone. Never fetches. */
        suspend fun load(url: String): SiteGlyph? {
            val key = FaviconResolver.cacheKey(url, null, null) ?: return null
            memory.get(key)?.let { return it.glyph }
            val glyph = withContext(Dispatchers.IO) { FaviconLoader.cachedOnly(url)?.let(::analyse) }
            // A miss isn't kept: the site may get an icon later (a visit).
            if (glyph != null) memory.put(key, Result(glyph))
            return glyph
        }

        /** Grey as drawn: luminance, contrast scaled by [contrast] around mid-grey. */
        private fun grey(argb: Int, contrast: Float): Float {
            val r = (argb shr 16 and 0xFF) / 255f
            val g = (argb shr 8 and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val l = 0.2126f * r + 0.7152f * g + 0.0722f * b
            return contrast * l + (1f - contrast) / 2f
        }

        internal fun analyse(bitmap: Bitmap, contrast: Float = GREY_CONTRAST): SiteGlyph? {
            val w = bitmap.width
            val h = bitmap.height
            if (w == 0 || h == 0) return null
            val px = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
            var l = w; var t = h; var r = -1; var b = -1
            for (y in 0 until h) for (x in 0 until w) {
                if ((px[y * w + x] ushr 24) > 16) {
                    if (x < l) l = x
                    if (x > r) r = x
                    if (y < t) t = y
                    if (y > b) b = y
                }
            }
            if (r < 0) return null
            fun at(x: Int, y: Int) = px[y * w + x]
            val corners = listOf(at(l, t), at(r, t), at(l, b), at(r, b))
            // An opaque square (or rounded square: its corners just inside): its own background.
            val inset = ((r - l) * 0.12f).toInt()
            val inner = listOf(at(l + inset, t + inset), at(r - inset, t + inset), at(l + inset, b - inset), at(r - inset, b - inset))
            val tile = (corners + inner).count { (it ushr 24) > 200 } >= 6
            var light = 0
            var dark = 0
            var opaque = 0
            val base = if (tile) inner.map { grey(it, contrast) }.average().toFloat() else 0f
            for (y in t..b) for (x in l..r) {
                val c = at(x, y)
                if ((c ushr 24) < 128) continue
                opaque++
                val g = grey(c, contrast)
                if (tile) {
                    // The tile shows on any page; what matters is its drawing inside.
                    if (kotlin.math.abs(g - base) > MIN_DIFF) { light++; dark++ }
                } else {
                    if (LIGHT_PAGE - g > MIN_DIFF) light++
                    if (g - DARK_PAGE > MIN_DIFF) dark++
                }
            }
            if (opaque == 0) return null
            val area = ((r - l + 1) * (b - t + 1)).toFloat()
            return SiteGlyph(
                bitmap.asImageBitmap(),
                IntOffset(l, t),
                IntSize(r - l + 1, b - t + 1),
                tile,
                seenOnLight = light / area > MIN_SHARE,
                seenOnDark = dark / area > MIN_SHARE,
            )
        }

        /** Same as PasswordTokens.GREY_CONTRAST (kept here so this stays free of the UI). */
        private const val GREY_CONTRAST = 0.8f
        private const val LIGHT_PAGE = 0.95f
        private const val DARK_PAGE = 0.08f
        private const val MIN_DIFF = 0.22f
        private const val MIN_SHARE = 0.06f
    }
}
