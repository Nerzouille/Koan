package dev.nerzouille.koan.thumbs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.nerzouille.koan.ui.theme.ThumbTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Page screenshots for cards and the page cover (docs/ARCHITECTURE.md "Thumbnails").
 *
 * On disk: one lossy WebP per tab, [ThumbTokens.WIDTH_PX] wide, in an LRU folder capped at
 * [ThumbTokens.DISK_CAP_BYTES] (least recently shown goes first). In memory: a small LRU of decoded
 * hardware bitmaps (GPU memory, not the Java heap). [cached] is synchronous, so a card or cover
 * whose image is in memory draws it in its first frame.
 *
 * A key is the file name; it changes with every capture, so Room's `thumbnailKey` changing is what
 * tells a card to show the new image.
 */
object ThumbnailStore {
    private var dir: File? = null
    private val diskLock = Mutex()
    private var diskBytes = -1L

    private val memory = object : LruCache<String, ImageBitmap>(ThumbTokens.MEMORY_CAP_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = (value.width * value.height * 4).coerceAtLeast(1)
    }

    fun initialize(context: Context) {
        if (dir != null) return
        dir = File(context.filesDir, "thumbs").apply { mkdirs() }
    }

    fun cached(key: String): ImageBitmap? = memory.get(key)

    /** Decodes [key] off the main thread (and caches it). Null if the file is gone. */
    suspend fun load(key: String): ImageBitmap? {
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = fileFor(key) ?: return@withContext null
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
            val bitmap = runCatching { BitmapFactory.decodeFile(file.path, opts) }.getOrNull() ?: return@withContext null
            // Recently shown = kept longest by the disk LRU.
            file.setLastModified(System.currentTimeMillis())
            bitmap.asImageBitmap().also { memory.put(key, it) }
        }
    }

    /** Software copy of the top [heightPx] rows of [key], subsampled by [sampleSize]. Off the main thread. */
    fun decodeTop(key: String, heightPx: Int, sampleSize: Int): Bitmap? {
        val file = fileFor(key) ?: return null
        return runCatching {
            val decoder = BitmapRegionDecoder.newInstance(file.path)
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                decoder.decodeRegion(Rect(0, 0, decoder.width, heightPx.coerceAtMost(decoder.height)), opts)
            } finally {
                decoder.recycle()
            }
        }.getOrNull()
    }

    /** Decodes [keys] ahead of time (e.g. the cards around the focus when the overview opens). */
    suspend fun prefetch(keys: List<String>) {
        for (key in keys) if (memory.get(key) == null) load(key)
    }

    /**
     * Scales [capture] to the thumbnail width, writes it and returns its key, and the keys the disk
     * LRU dropped to stay under its cap (their Room references must be cleared). [capture] is
     * recycled. [previous] (this tab's old key) is deleted. Null if nothing was saved, e.g. a blank
     * capture: the tab then keeps its previous screenshot, or its placeholder.
     */
    suspend fun save(tabId: String, capture: Bitmap, previous: String?): Pair<String, List<String>>? = withContext(Dispatchers.IO) {
        val folder = dir ?: return@withContext null
        if (looksBlank(capture)) {
            capture.recycle()
            return@withContext null
        }
        val scaled = scaleDown(capture, ThumbTokens.WIDTH_PX)
        if (scaled !== capture) capture.recycle()
        val key = "${hash(tabId)}-${System.currentTimeMillis()}.webp"
        val file = File(folder, key)
        val tmp = File(folder, "$key.tmp")
        val ok = runCatching {
            tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, ThumbTokens.QUALITY, it) }
            tmp.renameTo(file)
        }.getOrDefault(false)
        if (!ok) {
            tmp.delete()
            scaled.recycle()
            return@withContext null
        }
        // Its blurred copies for the pile, baked in the background, not while the overview moves.
        BakedBlur.bakeToDisk(key, scaled)
        // The fresh image goes straight to memory: the card that asks for it next skips a decode.
        scaled.copy(Bitmap.Config.HARDWARE, false)?.let { memory.put(key, it.asImageBitmap()) }
        scaled.recycle()
        previous?.let { delete(it) }
        key to trimDisk(file.length())
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        memory.remove(key)
        BakedBlur.forget(key)
        val file = fileFor(key) ?: return@withContext
        diskLock.withLock {
            val size = file.length()
            if (file.delete() && diskBytes >= 0) diskBytes -= size
        }
    }

    /**
     * Deletes saved screenshots that are blank (captured before the page painted, by builds without
     * the check in [save]) and returns their keys, whose Room references must be cleared. Runs once.
     */
    suspend fun dropBlank(): List<String> = withContext(Dispatchers.IO) {
        val folder = dir ?: return@withContext emptyList()
        val marker = File(folder.parentFile, BLANK_SCAN_MARKER)
        if (marker.exists()) return@withContext emptyList()
        val blank = folder.listFiles().orEmpty().filter { file ->
            val bitmap = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() ?: return@filter false
            looksBlank(bitmap).also { bitmap.recycle() }
        }.map { it.name }
        blank.forEach { delete(it) }
        marker.createNewFile()
        blank
    }

    /** Deletes files no tab refers to any more (tabs removed while Koan was not running). */
    suspend fun retainOnly(keys: Set<String>) = withContext(Dispatchers.IO) {
        val folder = dir ?: return@withContext
        diskLock.withLock {
            folder.listFiles()?.forEach { if (it.name !in keys) it.delete() }
            BakedBlur.retainOnly(keys)
            diskBytes = -1
        }
    }

    private suspend fun trimDisk(added: Long): List<String> = diskLock.withLock {
        val folder = dir ?: return emptyList()
        if (diskBytes < 0) {
            diskBytes = folder.listFiles()?.sumOf { it.length() } ?: 0
        } else {
            diskBytes += added
        }
        if (diskBytes <= ThumbTokens.DISK_CAP_BYTES) return emptyList()
        val dropped = mutableListOf<String>()
        val target = ThumbTokens.DISK_CAP_BYTES * 8 / 10
        for (f in folder.listFiles().orEmpty().sortedBy { it.lastModified() }) {
            if (diskBytes <= target) break
            val size = f.length()
            if (f.delete()) {
                diskBytes -= size
                memory.remove(f.name)
                BakedBlur.forget(f.name)
                dropped += f.name
            }
        }
        dropped
    }

    /** True if [bitmap] is (nearly) one flat colour: a page captured before it painted. */
    private fun looksBlank(bitmap: Bitmap): Boolean {
        val soft = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false else bitmap
        return BlankCheck.looksBlank(soft.width, soft.height) { y, row -> soft.getPixels(row, 0, soft.width, 0, y, soft.width, 1) }
            .also { if (soft !== bitmap) soft.recycle() }
    }

    private fun fileFor(key: String): File? {
        val folder = dir ?: return null
        if (key.contains('/') || key.startsWith(".")) return null
        return File(folder, key).takeIf { it.exists() }
    }

    /**
     * Halves first, then a last bilinear step: a single bilinear pass over a 2–3× reduction
     * aliases fine text into shimmer.
     */
    private fun scaleDown(src: Bitmap, width: Int): Bitmap {
        var cur = src
        while (cur.width >= width * 2) {
            val next = Bitmap.createScaledBitmap(cur, cur.width / 2, cur.height / 2, true)
            if (cur !== src) cur.recycle()
            cur = next
        }
        if (cur.width <= width) return cur
        val h = (cur.height.toLong() * width / cur.width).toInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(cur, width, h, true)
        if (cur !== src) cur.recycle()
        return out
    }

    /** Written once the one-time scan for old blank screenshots ([dropBlank]) has run. */
    private const val BLANK_SCAN_MARKER = "thumbs-blank-scan-1"

    private fun hash(id: String): String =
        MessageDigest.getInstance("SHA-1").digest(id.toByteArray()).take(10).joinToString("") { "%02x".format(it) }
}
