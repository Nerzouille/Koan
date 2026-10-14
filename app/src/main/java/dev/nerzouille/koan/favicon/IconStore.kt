package dev.nerzouille.koan.favicon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.nerzouille.koan.sync.ZenRoomMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Favicons for cards, rows and tiles. Three sources, in order:
 * - icons Zen synced inline (`data:` URLs): normalised to ≤ [MAX_PX] PNG files at sync time, so
 *   showing one is a plain file decode and Room only holds a short key;
 * - the site's own favicon, fetched by Z-Sync's [FaviconLoader] (its own disk cache);
 * - nothing: callers draw a letter tile.
 *
 * Decoded icons live in a small memory cache; [cached] is synchronous so a composable that finds its
 * icon there draws it in the first frame (no pop-in on recomposition or page switches).
 */
object IconStore {
    const val MAX_PX = 128
    private const val FILE_VERSION = 2

    /**
     * A decoded icon. [dark] = mostly dark pixels (GitHub's black octocat…): it would vanish on
     * Koan's dark surfaces, so it is drawn on a light backing.
     */
    class Icon(val image: ImageBitmap, val dark: Boolean)

    private var dir: File? = null

    private val memory = object : LruCache<String, Icon>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Icon) = (value.image.width * value.image.height * 4).coerceAtLeast(1)
    }

    fun initialize(context: Context) {
        if (dir != null) return
        dir = File(context.filesDir, "icons").apply { mkdirs() }
        FaviconLoader.initialize(context)
    }

    /** Memory key for a tab icon: the inline key, or the site's key. Null = no icon possible. */
    fun keyFor(url: String, icon: String?): String? =
        icon?.takeIf { it.startsWith(ZenRoomMapper.INLINE_ICON_PREFIX) }
            ?: FaviconResolver.cacheKey(url, null, null)

    fun cached(key: String): Icon? = memory.get(key)

    /** Loads (and caches) a tab's icon off the main thread. Null if it has none. */
    suspend fun load(url: String, icon: String?): Icon? {
        val key = keyFor(url, icon) ?: return null
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bitmap = if (key.startsWith(ZenRoomMapper.INLINE_ICON_PREFIX)) {
                fileFor(key)?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                    // Synced inline icon not on disk (cleared storage): try the site instead.
                    ?: FaviconLoader.image(url, null, null)?.let(::normalized)
            } else {
                FaviconLoader.image(url, null, null)?.let(::normalized)
            }
            bitmap?.let { Icon(it.asImageBitmap(), isDark(it)) }?.also { memory.put(key, it) }
        }
    }

    /** Decodes [refs] (url to icon) into memory ahead of time, e.g. right after startup or a sync. */
    suspend fun prewarm(refs: List<Pair<String, String?>>) {
        for ((url, icon) in refs.distinctBy { keyFor(it.first, it.second) }) {
            val key = keyFor(url, icon) ?: continue
            if (memory.get(key) != null) continue
            // Only local files here: network fetches happen lazily for what is on screen.
            if (key.startsWith(ZenRoomMapper.INLINE_ICON_PREFIX)) load(url, icon)
        }
    }

    /**
     * Writes Zen's inline icons to disk (once per distinct image) and deletes files no tab uses any
     * more. Runs on IO during a sync, never on the main thread.
     */
    suspend fun storeInline(icons: Map<String, String>, inUse: Set<String>) = withContext(Dispatchers.IO) {
        val folder = dir ?: return@withContext
        for ((key, dataUrl) in icons) {
            val file = fileFor(key) ?: continue
            if (file.exists()) continue
            val bitmap = decodeDataUrl(dataUrl, key) ?: continue
            val tmp = File(folder, file.name + ".tmp")
            runCatching {
                tmp.outputStream().use { normalized(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) }
                tmp.renameTo(file)
            }
        }
        val keep = inUse.mapNotNullTo(HashSet()) { fileFor(it)?.name }
        folder.listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    private fun fileFor(key: String): File? {
        val folder = dir ?: return null
        val hash = key.removePrefix(ZenRoomMapper.INLINE_ICON_PREFIX).takeIf { h -> h.isNotEmpty() && h.all { it.isLetterOrDigit() } }
            ?: return null
        // The version changes whenever stored icons are processed differently, so they get rebuilt.
        return File(folder, "$hash.$FILE_VERSION.png")
    }

    private fun decodeDataUrl(dataUrl: String, key: String): Bitmap? {
        val bytes = FaviconLoader.dataBytes(dataUrl) ?: return null
        FaviconDecoder.image(bytes)?.let { return it }
        if (!FaviconLoader.isSvg(dataUrl)) return null
        val svg = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
        return SvgRasterizer.rasterize(svg, size = MAX_PX, cacheKey = key)
    }

    /**
     * Crops an icon whose visible part fills less than half its canvas (padded or mis-sized
     * favicons would otherwise show as a speck), then caps it at [MAX_PX].
     */
    private fun normalized(bitmap: Bitmap): Bitmap {
        val content = opaqueBounds(bitmap)
        val cropped = if (content != null && content.width() < bitmap.width / 2 && content.height() < bitmap.height / 2) {
            val pad = (maxOf(content.width(), content.height()) * 0.08f).toInt()
            val l = (content.left - pad).coerceAtLeast(0)
            val t = (content.top - pad).coerceAtLeast(0)
            val r = (content.right + pad).coerceAtMost(bitmap.width)
            val b = (content.bottom + pad).coerceAtMost(bitmap.height)
            Bitmap.createBitmap(bitmap, l, t, r - l, b - t)
        } else {
            bitmap
        }
        val longest = maxOf(cropped.width, cropped.height)
        if (longest == MAX_PX) return cropped
        val f = MAX_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(cropped, (cropped.width * f).toInt().coerceAtLeast(1), (cropped.height * f).toInt().coerceAtLeast(1), true)
    }

    /** Bounds of the pixels that are not (nearly) transparent; null if there are none. */
    private fun opaqueBounds(bitmap: Bitmap): android.graphics.Rect? {
        val w = bitmap.width
        val h = bitmap.height
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
        return if (r < 0) null else android.graphics.Rect(l, t, r + 1, b + 1)
    }

    /** Mostly-dark visible pixels: average luminance of the opaque ones below ~0.25. */
    private fun isDark(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        val px = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
        var sum = 0.0
        var n = 0
        for (c in px) {
            if ((c ushr 24) < 128) continue
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val bl = c and 0xFF
            sum += (0.2126 * r + 0.7152 * g + 0.0722 * bl) / 255.0
            n++
        }
        return n > 0 && sum / n < 0.25
    }
}
