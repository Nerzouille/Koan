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

    private var dir: File? = null

    private val memory = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = (value.width * value.height * 4).coerceAtLeast(1)
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

    fun cached(key: String): ImageBitmap? = memory.get(key)

    /** Loads (and caches) a tab's icon off the main thread. Null if it has none. */
    suspend fun load(url: String, icon: String?): ImageBitmap? {
        val key = keyFor(url, icon) ?: return null
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bitmap = if (key.startsWith(ZenRoomMapper.INLINE_ICON_PREFIX)) {
                fileFor(key)?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                    // Synced inline icon not on disk (cleared storage): try the site instead.
                    ?: FaviconLoader.image(url, null, null)?.let(::scaled)
            } else {
                FaviconLoader.image(url, null, null)?.let(::scaled)
            }
            bitmap?.asImageBitmap()?.also { memory.put(key, it) }
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
                tmp.outputStream().use { scaled(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) }
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
        return File(folder, "$hash.png")
    }

    private fun decodeDataUrl(dataUrl: String, key: String): Bitmap? {
        val bytes = FaviconLoader.dataBytes(dataUrl) ?: return null
        FaviconDecoder.image(bytes)?.let { return it }
        if (!FaviconLoader.isSvg(dataUrl)) return null
        val svg = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
        return SvgRasterizer.rasterize(svg, size = MAX_PX, cacheKey = key)
    }

    private fun scaled(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_PX) return bitmap
        val f = MAX_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * f).toInt().coerceAtLeast(1), (bitmap.height * f).toInt().coerceAtLeast(1), true)
    }
}
