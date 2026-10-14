// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import dev.nerzouille.koan.sync.ZenSpaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.json.JSONObject

/**
 * Port of `FaviconLoader` (Shared/ZenTheme.swift): in-memory decoded-image
 * cache, a small disk cache, and coalesced in-flight fetches. AsyncImage-style
 * loaders are not used: they cancel in lazy pager pages and do not share
 * results across the duplicate Essentials grids of same-container spaces.
 */
object FaviconLoader {
    private const val DISK_DIR = "zen-favicons"
    private const val TIMEOUT_MS = 12_000
    private const val DOCUMENT_TIMEOUT_MS = 5_000
    private const val DOCUMENT_BYTES = 65_536
    private const val MAX_REDIRECTS = 4
    private const val MAX_CONCURRENT = 6
    private const val MAX_FAVICON_BYTES = 512 * 1024
    private const val INDEX_FILE = "index.json"
    /** Bytes of cached icon files kept on disk; older files are evicted past this. */
    private const val DISK_MAX_BYTES = 40L * 1024 * 1024
    /** Cached icons older than this are ignored and re-fetched (site favicons change). */
    private const val DISK_TTL_MS = 30L * 24 * 60 * 60 * 1000
    /** Upper bound on `<link>` candidates tried for one tab (hostile pages). */
    private const val MAX_CANDIDATES = 8
    /** A failed key is retried after this, so one blip does not hide an icon for the session. */
    private const val FAILED_TTL_MS = 60_000L
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    private val executor: ExecutorService = Executors.newFixedThreadPool(MAX_CONCURRENT) { r ->
        Thread(r, "favicon-loader").apply { isDaemon = true; priority = Thread.MIN_PRIORITY + 1 }
    }
    /** Runs prefetch independently of [executor], so it cannot starve its own workers. */
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var diskDir: File? = null

    fun initialize(context: Context) {
        if (diskDir == null) {
            synchronized(this) {
                if (diskDir == null) {
                    diskDir = File(context.cacheDir, DISK_DIR).apply { mkdirs() }
                }
            }
        }
    }

    /** iOS `NSCache` parity: a ~20 MB byte budget, not a fixed count. */
    private val memoryCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    private val inflight = ConcurrentHashMap<String, Future<Bitmap?>>()
    /** cacheKey → time of last failure; entries expire after [FAILED_TTL_MS]. */
    private val failures = ConcurrentHashMap<String, Long>()
    private val resolved = ConcurrentHashMap<String, String>()
    @Volatile private var indexLoaded = false

    fun cached(cacheKey: String): Bitmap? = memoryCache.get(cacheKey)

    suspend fun image(pageURL: String, directURL: String?, dataURL: String? = null): Bitmap? {
        val key = FaviconResolver.cacheKey(pageURL, directURL, dataURL) ?: return null
        cached(key)?.let { return it }
        return withContext(Dispatchers.IO) { imageBlocking(pageURL, directURL, dataURL, key) }
    }

    /** Blocking fetch used from IO contexts; coalesces concurrent callers. */
    fun imageBlocking(pageURL: String, directURL: String?, dataURL: String?, cacheKey: String? = null): Bitmap? {
        val key: String = cacheKey ?: FaviconResolver.cacheKey(pageURL, directURL, dataURL) ?: return null
        cached(key)?.let { return it }
        failures[key]?.let { when {
            System.currentTimeMillis() - it < FAILED_TTL_MS -> return null
            else -> failures.remove(key)
        } }
        val future = inflight[key] ?: synchronized(this) {
            inflight.getOrPut(key) {
                executor.submit<Bitmap?> {
                    try {
                        val fetched = load(pageURL, directURL, dataURL, key)
                        if (fetched != null) {
                            memoryCache.put(key, fetched)
                            failures.remove(key)
                        } else {
                            failures[key] = System.currentTimeMillis()
                        }
                        fetched
                    } finally {
                        synchronized(this) { inflight.remove(key) }
                    }
                }
            }
        }
        return runCatching { future.get() }.getOrNull()
    }

    /** Prefetch a bounded 6-wide window over every synced tab that needs an icon. */
    fun prefetch(snapshot: ZenSpaces.ZenSnapshot) {
        val requests = FaviconResolver.requests(snapshot)
        if (requests.isEmpty()) return
        failures.clear()
        pruneIndex(requests)
        prefetchScope.launch {
            val next = java.util.concurrent.atomic.AtomicInteger(0)
            coroutineScope {
                repeat(minOf(MAX_CONCURRENT, requests.size)) {
                    launch(Dispatchers.IO) {
                        while (true) {
                            val index = next.getAndIncrement()
                            if (index >= requests.size) break
                            val request = requests[index]
                            imageBlocking(request.pageURL, request.directURL, request.dataURL, request.cacheKey)
                        }
                    }
                }
            }
        }
    }

    private fun load(pageURL: String, directURL: String?, dataURL: String?, cacheKey: String): Bitmap? {
        // A synced data: icon is a real image. Raster payloads (PNG/ICO) decode
        // in memory: instant, offline. An SVG payload is kept as a last resort
        // below, because a live network image is usually crisper.
        val svgData = dataURL?.takeIf { isSvg(it) }
        if (dataURL != null && svgData == null) {
            decodeDataURL(dataURL, cacheKey)?.let { return it }
        }
        FaviconResolver.url(pageURL, directURL)?.let { direct -> fetchCandidate(direct)?.let { return it } }
        val page = FaviconDiscovery.documentURL(pageURL)
        // The persisted index is keyed by site, so the next launch skips the HTML
        // fetch for every tab on that site.
        val siteKey = FaviconResolver.cacheKey(pageURL, null, null) ?: cacheKey
        if (page != null && !FaviconDiscovery.isLoopback(page)) {
            remembered(siteKey)?.let { known ->
                // SVG-aware: a remembered .svg URL is rasterized (from its cache).
                fetchCandidate(known)?.let { return it }
            }
            val candidates = if (FaviconDiscovery.hasKnownIcon(page)) {
                // Signed-in pages hide the real icon. One small file, no HTML.
                FaviconDiscovery.candidates(null, null, page)
            } else {
                val document = fetchDocument(page)
                FaviconDiscovery.candidates(document?.html, document?.finalURL, page)
            }
            for (candidate in candidates.take(MAX_CANDIDATES)) {
                val image = fetchCandidate(candidate) ?: continue
                remember(siteKey, candidate)
                return image
            }
        }
        // Nothing fetchable: render the synced SVG icon locally.
        svgData?.let { svg ->
            decodeDataURL(svg, cacheKey)?.let { return it }
        }
        // Last resort: the site blocks scraping or has no discoverable icon
        // (login redirect, 403, no <link>). Ask a public logo service for the
        // domain — but never for a private/LAN host.
        // Koan: DuckDuckGo's icon service instead of Google's (owner's choice, 2026-10-14). It
        // answers 404 for unknown domains, so a miss still falls back to the letter tile.
        val host = page?.host?.lowercase().orEmpty()
        if (page != null && host.isNotEmpty() && !FaviconDiscovery.isLoopback(page) && !isPrivateHost(host)) {
            val service = "https://icons.duckduckgo.com/ip3/$host.ico"
            fetch(service)?.let {
                remember(siteKey, service)
                return it
            }
        }
        return null
    }

    /** Loopback, RFC1918/LAN, `.local`/`.internal` and single-label hosts: never sent to a logo service. */
    private fun isPrivateHost(host: String): Boolean {
        val h = host.removePrefix("[").removeSuffix("]").lowercase()
        if (h.isEmpty()) return true
        if (h == "localhost") return true
        // Bare IPv4 / IPv6 literal, or a private IPv4 range.
        if (h.all { it.isDigit() || it == '.' }) {
            val parts = h.split('.').mapNotNull { it.toIntOrNull() }
            if (parts.size == 4) {
                val (a, b) = parts[0] to parts[1]
                return a == 10 || a == 127 || (a == 192 && b == 168) || (a == 172 && b in 16..31) ||
                    (a == 169 && b == 254) || (a == 100 && b in 64..127) || a == 0
            }
            return true
        }
        if (h.contains(':')) return true // IPv6 literal
        // Non-public suffixes / single-label hostnames.
        val suffixes = listOf(".local", ".internal", ".lan", ".home", ".home.arpa", ".localhost")
        if (suffixes.any { h.endsWith(it) }) return true
        return !h.contains('.') // single-label intranet name
    }

    /**
     * Fetch one candidate icon. Tries the disk cache, then one download, and
     * accepts raster bytes or SVG markup — the caller does not have to know
     * which it is, so a declared SVG served from a non-`.svg` URL still works.
     */
    private fun fetchCandidate(urlString: String): Bitmap? {
        diskCachedBitmap(urlString)?.let { return it }
        val bytes = downloadBytes(urlString) ?: return null
        FaviconDecoder.image(bytes)?.let {
            storeDiskImage(urlString, bytes)
            return it
        }
        if (bytes.looksLikeSvg()) {
            val svg = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
            if (svg != null) {
                val png = SvgRasterizer.rasterize(svg, size = 128, cacheKey = urlString)
                if (png != null) {
                    storeDiskPng(urlString, png)
                    return png
                }
            }
        }
        return null
    }

    private fun ByteArray.looksLikeSvg(): Boolean {
        val prefix = String(this, 0, minOf(size, 2048), Charsets.ISO_8859_1).lowercase()
        return prefix.contains("<svg") || (prefix.contains("<?xml") && prefix.contains("svg"))
    }

    private fun remembered(cacheKey: String): String? {
        loadIndex()
        return resolved[cacheKey]
    }

    private fun remember(cacheKey: String, iconURL: String) {
        if (cacheKey.isEmpty()) return
        val dir = diskDir ?: return
        // One writer at a time, atomic replace: six loader threads otherwise
        // interleave whole-file rewrites and corrupt the index.
        synchronized(this) {
            loadIndex()
            resolved[cacheKey] = iconURL
            writeIndexLocked(dir)
        }
    }

    /** Drop index entries for sites no longer present in the snapshot. */
    private fun pruneIndex(requests: List<FaviconRequest>) {
        val dir = diskDir ?: return
        val keep = requests.mapNotNull { FaviconResolver.cacheKey(it.pageURL, null, null) }.toSet()
        synchronized(this) {
            loadIndex()
            if (resolved.keys.retainAll(keep)) writeIndexLocked(dir)
        }
    }

    private fun writeIndexLocked(dir: File) {
        val objectJson = JSONObject()
        for ((key, value) in resolved) objectJson.put(key, value)
        runCatching {
            val tmp = File(dir, "$INDEX_FILE.tmp")
            tmp.writeText(objectJson.toString())
            if (!tmp.renameTo(File(dir, INDEX_FILE))) {
                File(dir, INDEX_FILE).writeText(objectJson.toString())
            }
        }
    }

    private fun loadIndex() {
        if (indexLoaded) return
        synchronized(this) {
            if (indexLoaded) return
            val dir = diskDir ?: return
            val file = File(dir, INDEX_FILE)
            if (file.exists()) {
                val parsed = runCatching { JSONObject(file.readText()) }.getOrNull()
                if (parsed != null) {
                    val keys = parsed.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        parsed.optString(key).takeIf { it.isNotEmpty() }?.let { resolved[key] = it }
                    }
                }
            }
            indexLoaded = true
        }
    }

    /** First 64KB only. Icon links are in `<head>`; synced tabs are not opened. */
    private fun fetchDocument(page: java.net.URI): Document? {
        var current = page.toString()
        repeat(MAX_REDIRECTS + 1) {
            val uri = runCatching { java.net.URI(current) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return null
            val connection = runCatching { uri.toURL().openConnection() as HttpURLConnection }.getOrNull()
                ?: return null
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = DOCUMENT_TIMEOUT_MS
                connection.readTimeout = DOCUMENT_TIMEOUT_MS
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: return null
                    current = runCatching { uri.resolve(location).toString() }.getOrNull() ?: return null
                    return@repeat
                }
                if (code !in 200..299) return null
                val type = connection.contentType?.lowercase().orEmpty()
                if (type.isNotEmpty() && !type.contains("html") && !type.contains("xml") && !type.contains("text/")) {
                    return null
                }
                val bytes = connection.inputStream.use { stream ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    while (out.size() < DOCUMENT_BYTES) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, minOf(read, DOCUMENT_BYTES - out.size()))
                    }
                    out.toByteArray()
                }
                val html = HtmlText.htmlString(bytes, connection.contentType)
                val finalURL = runCatching { java.net.URI(current) }.getOrNull() ?: page
                return Document(html, finalURL)
            } catch (_: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private class Document(val html: String, val finalURL: java.net.URI)

    /** Raster-only fetch (direct URL, logo service). SVG candidates use [fetchCandidate]. */
    private fun fetch(urlString: String): Bitmap? {
        val url = runCatching { URL(urlString) }.getOrNull() ?: return null
        if (url.protocol != "https" && url.protocol != "http") return null
        diskCachedBitmap(urlString)?.let { return it }
        val data = downloadBytes(urlString) ?: return null
        return FaviconDecoder.image(data)?.also { storeDiskImage(urlString, data) }
    }

    /** Decoded bitmap from the per-URL disk cache, honouring the TTL. Null on miss/corrupt. */
    private fun diskCachedBitmap(urlString: String): Bitmap? {
        val dir = diskDir ?: return null
        val file = File(dir, diskKey(urlString))
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > DISK_TTL_MS) {
            runCatching { file.delete() }
            return null
        }
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val bitmap = FaviconDecoder.image(bytes)
        if (bitmap == null) runCatching { file.delete() }
        return bitmap
    }

    private fun storeDiskImage(urlString: String, bytes: ByteArray) = writeDiskFile(urlString, bytes)

    private fun storeDiskPng(urlString: String, bitmap: Bitmap) {
        val out = ByteArrayOutputStream()
        if (runCatching { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }.getOrDefault(false)) {
            writeDiskFile(urlString, out.toByteArray())
        }
    }

    private fun writeDiskFile(urlString: String, bytes: ByteArray) {
        val dir = diskDir ?: return
        runCatching {
            val file = File(dir, diskKey(urlString))
            file.writeBytes(bytes)
            file.setLastModified(System.currentTimeMillis())
            trimDiskCache(dir)
        }
    }

    /** Keeps the icon cache under [DISK_MAX_BYTES], dropping the oldest files first. */
    private fun trimDiskCache(dir: File) {
        val files = dir.listFiles { f -> f.isFile && f.name != INDEX_FILE && !f.name.endsWith(".tmp") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= DISK_MAX_BYTES) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (total <= DISK_MAX_BYTES) break
            val len = f.length()
            if (runCatching { f.delete() }.getOrDefault(false)) total -= len
        }
    }

    /** Capped GET (512 KB) with redirects. Shared by raster decode and SVG. */
    private fun downloadBytes(urlString: String): ByteArray? {
        val url = runCatching { URL(urlString) }.getOrNull() ?: return null
        if (url.protocol != "https" && url.protocol != "http") return null
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            try {
                val code = connection.responseCode
                if (code !in 200..299) return null
                // Capped read: a hostile server must not exhaust memory.
                connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (out.size() + n > MAX_FAVICON_BYTES) return null
                        out.write(buffer, 0, n)
                    }
                    out.toByteArray()
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun diskKey(url: String): String =
        MessageDigest.getInstance("MD5").digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) } + ".bin"

    /** Decode an inline synced icon. Raster (PNG/ICO/…) or SVG. */
    fun dataBytes(dataURL: String): ByteArray? {
        val comma = dataURL.indexOf(',')
        if (comma < 0) return null
        val header = dataURL.substring(0, comma)
        val payload = dataURL.substring(comma + 1)
        val bytes = runCatching {
            if (header.contains(";base64")) {
                android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
            } else {
                percentDecodeToBytes(payload)
            }
        }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > MAX_FAVICON_BYTES) return null
        return bytes
    }

    /** Percent-decode without turning `+` into a space (unlike `URLDecoder`). */
    private fun percentDecodeToBytes(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '%' && i + 3 <= text.length) {
                val hex = text.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) {
                    out.write(hex)
                    i += 3
                    continue
                }
            }
            out.write(c.toString().toByteArray(Charsets.UTF_8))
            i++
        }
        return out.toByteArray()
    }

    fun isSvg(dataURL: String): Boolean =
        dataURL.substringBefore(';').substringBefore(',').contains("svg", ignoreCase = true)

    /** Decode a synced `data:image/…` favicon. Raster first, then SVG. */
    private fun decodeDataURL(dataURL: String, cacheKey: String): Bitmap? {
        val bytes = dataBytes(dataURL) ?: return null
        FaviconDecoder.image(bytes)?.let { return it }
        if (!isSvg(dataURL)) return null
        val svg = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
        return SvgRasterizer.rasterize(svg, size = 128, cacheKey = cacheKey)
    }
}
