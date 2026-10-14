package dev.nerzouille.koan.thumbs

import kotlin.math.abs

/** Tells a page that has painted from a blank one (one flat colour), for screenshots. */
internal object BlankCheck {
    /** Blank if fewer than this share of the scanned pixels differ from the background. */
    private const val MAX_SHARE = 0.0005

    /** Per-channel difference (0–255) that counts as a different pixel (WebP noise stays below). */
    private const val TOLERANCE = 24

    /**
     * True if the [width]×[height] image is (nearly) one flat colour. [readRow] fills a row of ARGB
     * pixels. Every 8th row is scanned in full, so even a few lines of text count as content.
     */
    fun looksBlank(width: Int, height: Int, readRow: (y: Int, row: IntArray) -> Unit): Boolean {
        if (width <= 0 || height <= 0) return true
        val row = IntArray(width)
        // The background: the most common colour on a coarse grid.
        val counts = HashMap<Int, Int>()
        for (y in 0 until height step 32) {
            readRow(y, row)
            for (x in 0 until width step 32) counts.merge(row[x], 1, Int::plus)
        }
        val background = counts.maxByOrNull { it.value }?.key ?: return true
        var scanned = 0L
        var differing = 0L
        for (y in 0 until height step 8) {
            readRow(y, row)
            for (p in row) if (farFrom(p, background)) differing++
            scanned += width
        }
        return differing < scanned * MAX_SHARE
    }

    private fun farFrom(a: Int, b: Int): Boolean {
        val dr = abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF))
        val dg = abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF))
        val db = abs((a and 0xFF) - (b and 0xFF))
        return maxOf(dr, dg, db) > TOLERANCE
    }
}
