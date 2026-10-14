// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.caverock.androidsvg.SVG

/**
 * Rasterizes an SVG favicon to a bitmap. Many sites ship only an SVG icon, and
 * the platform cannot decode SVG (Android above API 30 only, via `ImageDecoder`).
 *
 * Uses AndroidSVG, a small pure-Java renderer, so it works on every supported
 * API level and needs no WebView. The result goes into the loader's normal
 * in-memory cache, so this runs at most once per icon per launch.
 */
internal object SvgRasterizer {
    /** Rasterize [svg] into a [size]×[size] bitmap. Null on parse/failure. */
    fun rasterize(svg: String, size: Int, cacheKey: String): Bitmap? = runCatching {
        val parsed = SVG.getFromString(svg)
        // Koan: an SVG with width/height but no viewBox renders at its intrinsic size (often 16 px)
        // in the corner of the canvas. Give it a viewBox so it scales to fill.
        if (parsed.documentViewBox == null) {
            val w = parsed.documentWidth
            val h = parsed.documentHeight
            if (w > 0f && h > 0f) parsed.setDocumentViewBox(0f, 0f, w, h)
        }
        parsed.setDocumentWidth("100%")
        parsed.setDocumentHeight("100%")
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        parsed.renderToCanvas(canvas, RectF(0f, 0f, size.toFloat(), size.toFloat()))
        if (bitmap.isBlank()) {
            bitmap.recycle()
            null
        } else {
            bitmap
        }
    }.getOrNull()

    private fun Bitmap.isBlank(): Boolean {
        var x = 0
        while (x < width) {
            var y = 0
            while (y < height) {
                if (getPixel(x, y) != 0) return false
                y += 4
            }
            x += 4
        }
        return true
    }
}
