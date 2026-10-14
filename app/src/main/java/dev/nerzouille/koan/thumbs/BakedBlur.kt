package dev.nerzouille.koan.thumbs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.LruCache
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.nerzouille.koan.ui.theme.CardTokens
import dev.nerzouille.koan.ui.theme.StackTokens
import dev.nerzouille.koan.ui.theme.ThumbTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pre-blurred copies of a card's top slice for the pile (docs/ARCHITECTURE.md "Blur strategy", the
 * baked fallback): the cards crossfade between them by depth instead of blurring live, so the GPU
 * never allocates a blur buffer per card.
 *
 * A screenshot's copies are baked when it is captured (in the background, never during the
 * overview's open or close motion) and kept on disk next to it, as one image with the levels stacked. The overview only uploads them: for the
 * cards that show blurred, once its open motion has settled, one per frame. Until then a card blurs
 * live, as before. Placeholders are captured from the screen, baked the same way, memory only.
 */
object BakedBlur {
    /** A card's blurred copies, weakest first, one GPU bitmap each. */
    private val memory = object : LruCache<String, List<ImageBitmap>>(ThumbTokens.BAKED_CACHE_BYTES) {
        override fun sizeOf(key: String, value: List<ImageBitmap>) = value.sumOf { it.width * it.height * 4 }.coerceAtLeast(1)
    }
    private var dir: File? = null

    /** Rows of a thumbnail under a card's blurred slice, and the blur radii in thumbnail px. */
    private var sliceRows = 0
    private var screenshotRadii = emptyList<Float>()

    /** All baking on one background thread, so it never takes every core from the UI. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val baking = Dispatchers.Default.limitedParallelism(1)
    private val bakes = CoroutineScope(SupervisorJob() + baking)
    private val uploads = Mutex()
    private val moving = MutableStateFlow(false)
    private val settled = MutableStateFlow(false)

    fun initialize(context: Context) {
        if (dir != null) return
        dir = File(context.filesDir, "thumbs-blur").apply { mkdirs() }
        val metrics = context.resources.displayMetrics
        val cardPx = metrics.widthPixels - 2 * StackTokens.CardSideMargin.value * metrics.density
        val toImage = ThumbTokens.WIDTH_PX / cardPx
        sliceRows = (CardTokens.BLUR_SLICE_PX * toImage).roundToInt()
        screenshotRadii = radii(metrics.density * toImage)
    }

    /** Radii (px) of the baked levels, weakest first, for [pxPerDp] px per dp of card. */
    fun radii(pxPerDp: Float): List<Float> {
        val levels = ThumbTokens.BAKED_LEVELS
        return List(levels) { i -> MAX_BLUR_DP * (i + 1) / levels * pxPerDp }
    }

    /**
     * The overview's open/close motion [running] or not, and whether it is [open]. Bakes wait while
     * it runs; uploads only happen once it has settled open.
     */
    fun onOverviewMotion(running: Boolean, open: Boolean) {
        moving.value = running
        settled.value = !running && open
    }

    fun cached(key: String): List<ImageBitmap>? = memory.get(key)

    /**
     * Bakes thumbnail [key] from [thumb] (software, full thumbnail; the caller keeps it) to disk, in
     * the background once the overview is not moving (a capture is also taken when it opens).
     */
    fun bakeToDisk(key: String, thumb: Bitmap) {
        val rows = sliceRows.coerceAtMost(thumb.height)
        if (rows < 2) return
        // Its own half-resolution copy of the slice (a scaling matrix always makes a new bitmap).
        val half = Bitmap.createBitmap(thumb, 0, 0, thumb.width, rows, Matrix().apply { setScale(0.5f, 0.5f) }, true)
        bakes.launch {
            moving.first { !it }
            write(key, bake(half, screenshotRadii))
        }
    }

    /** Thumbnail [key]'s copies: from memory, else uploaded from disk (baked there first if missing). */
    suspend fun screenshot(key: String): List<ImageBitmap>? = upload(key) {
        withContext(baking) {
            val stacked = read(key) ?: run {
                val half = ThumbnailStore.decodeTop(key, sliceRows, sampleSize = 2) ?: return@withContext null
                bake(half, screenshotRadii).also { write(key, it) }
            }
            toHardware(stacked)
        }
    }

    /** A placeholder's copies: [snapshot] (opaque, taken on the main thread) baked at [radiiPx]. */
    suspend fun capture(key: String, radiiPx: List<Float>, snapshot: suspend () -> ImageBitmap): List<ImageBitmap>? = upload(key) {
        val image = snapshot()
        withContext(baking) {
            val full = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false) ?: return@withContext null
            val half = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
            if (half !== full) full.recycle()
            toHardware(bake(half, radiiPx))
        }
    }

    /** Deletes thumbnail [key]'s copies (the thumbnail itself is gone). */
    fun forget(key: String) {
        memory.remove(key)
        fileFor(key)?.delete()
    }

    /** Deletes copies whose thumbnail is not in [keys]. */
    fun retainOnly(keys: Set<String>) {
        dir?.listFiles()?.forEach { if (it.name !in keys) it.delete() }
    }

    /**
     * Gets [key] into memory: waits for the overview to settle, then one upload per frame (several
     * would land on the same frame and the GPU upload is what hitches).
     */
    private suspend fun upload(key: String, load: suspend () -> List<ImageBitmap>?): List<ImageBitmap>? {
        memory.get(key)?.let { return it }
        settled.first { it }
        return uploads.withLock {
            memory.get(key)?.let { return@withLock it }
            withFrameNanos { }
            settled.first { it }
            load()?.also { memory.put(key, it) }
        }
    }

    /** Splits [stacked] into one GPU bitmap per level (one texture each: no bleed between levels). */
    private fun toHardware(stacked: Pair<Bitmap, Int>): List<ImageBitmap> {
        val (all, levels) = stacked
        val h = all.height / levels
        return List(levels) { i ->
            val soft = Bitmap.createBitmap(all, 0, i * h, all.width, h)
            val hard = soft.copy(Bitmap.Config.HARDWARE, false)
            if (hard != null && soft !== all) soft.recycle()
            (hard ?: soft).asImageBitmap()
        }.also { copies -> if (copies.none { it.asAndroidBitmap() === all }) all.recycle() }
    }

    private fun read(key: String): Pair<Bitmap, Int>? {
        val file = fileFor(key) ?: return null
        val bitmap = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() ?: return null
        return bitmap to ThumbTokens.BAKED_LEVELS
    }

    private fun write(key: String, stacked: Pair<Bitmap, Int>) {
        val folder = dir ?: return
        if (key.contains('/') || key.startsWith(".")) return
        val file = File(folder, key)
        val tmp = File(folder, "$key.tmp")
        runCatching {
            tmp.outputStream().use { stacked.first.compress(Bitmap.CompressFormat.WEBP_LOSSY, ThumbTokens.QUALITY, it) }
            tmp.renameTo(file)
        }.onFailure { tmp.delete() }
    }

    private fun fileFor(key: String): File? {
        val folder = dir ?: return null
        if (key.contains('/') || key.startsWith(".")) return null
        return File(folder, key).takeIf { it.exists() }
    }

    /**
     * [half] (opaque, half resolution; recycled) blurred at each of [radiiPx] (full-resolution px,
     * the same radius as a RenderEffect blur, weakest first), stacked top to bottom.
     */
    private fun bake(half: Bitmap, radiiPx: List<Float>): Pair<Bitmap, Int> {
        val w = half.width
        val h = half.height
        val pixels = IntArray(w * h)
        half.getPixels(pixels, 0, w, 0, 0, w, h)
        half.recycle()
        val out = Bitmap.createBitmap(w, h * radiiPx.size, Bitmap.Config.ARGB_8888)
        radiiPx.forEachIndexed { i, r ->
            val px = pixels.copyOf()
            gaussian(px, w, h, sigmaOf(r) / 2f)
            out.setPixels(px, 0, w, 0, i * h, w, h)
        }
        return out to radiiPx.size
    }

    private val MAX_BLUR_DP = StackTokens.BlurDp.max()

    /** HWUI's RenderEffect blur radius → Gaussian sigma (Blur::convertRadiusToSigma). */
    private fun sigmaOf(radius: Float) = if (radius > 0f) 0.57735f * radius + 0.5f else 0f

    /** Gaussian blur of opaque ARGB [px] in place, as three box blurs per axis. */
    private fun gaussian(px: IntArray, w: Int, h: Int, sigma: Float) {
        if (sigma < 0.3f) return
        val channels = Array(3) { c -> val shift = 16 - 8 * c; IntArray(w * h) { (px[it] shr shift) and 0xFF } }
        val tmp = IntArray(w * h)
        for (size in boxSizes(sigma)) {
            val r = (size - 1) / 2
            if (r == 0) continue
            for (ch in channels) {
                boxH(ch, tmp, w, h, r)
                boxV(tmp, ch, w, h, r)
            }
        }
        val (red, green, blue) = channels
        for (i in px.indices) px[i] = (0xFF shl 24) or (red[i] shl 16) or (green[i] shl 8) or blue[i]
    }

    /** Widths of three box blurs that together approximate a Gaussian of [sigma] (Kovesi). */
    private fun boxSizes(sigma: Float, n: Int = 3): IntArray {
        var wl = floor(sqrt(12f * sigma * sigma / n + 1f)).toInt()
        if (wl % 2 == 0) wl--
        val m = ((12f * sigma * sigma - n * wl * wl - 4f * n * wl - 3f * n) / (-4f * wl - 4f)).roundToInt()
        return IntArray(n) { if (it < m) wl else wl + 2 }
    }

    private fun boxH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var sum = 0
            for (k in -r..r) sum += src[row + k.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                dst[row + x] = (sum + r) / div
                sum += src[row + (x + r + 1).coerceAtMost(w - 1)] - src[row + (x - r).coerceAtLeast(0)]
            }
        }
    }

    private fun boxV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var sum = 0
            for (k in -r..r) sum += src[k.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                dst[y * w + x] = (sum + r) / div
                sum += src[(y + r + 1).coerceAtMost(h - 1) * w + x] - src[(y - r).coerceAtLeast(0) * w + x]
            }
        }
    }
}
