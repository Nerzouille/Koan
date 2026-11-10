package dev.nerzouille.koan.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.action.MediaSessionAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.webextensions.BuiltInWebExtensionController
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.WeakHashMap

/** A page's video as Koan's player drives it (assets/extensions/video). */
@Immutable
data class WebVideo(
    val tabId: String,
    /** The video is fullscreen in its page. */
    val fullscreen: Boolean,
    val paused: Boolean,
    val ended: Boolean,
    val waiting: Boolean,
    /** Seconds, as of [at] (uptime ms). */
    val time: Double,
    val at: Long,
    val duration: Double,
    val buffered: Double,
    val rate: Double,
    val muted: Boolean,
    val width: Int,
    val height: Int,
    val title: String,
    /** The video file's own address (http, not a blob or stream), when it has one. */
    val src: String,
)

/**
 * A small built-in extension on every page (assets/extensions/video):
 * - Koan's player over a page's fullscreen video: it hides the site's player but the video, stops
 *   the page from leaving fullscreen on its own, sends the video's state and takes Koan's
 *   commands. One video at a time per tab (the frame that went fullscreen). YouTube is left out
 *   (manifest exclude_matches): its own controls stay.
 * - The media notification's timeline: a playing video's position, length and speed go into the
 *   tab's media session (most pages never set it, so the notification showed 00:00), and its
 *   poster becomes the artwork when the page gives none (else the site's icon, feature-media).
 */
class PageVideo(private val engine: Engine, private val store: BrowserStore, private val scope: CoroutineScope) {
    private val controller = BuiltInWebExtensionController(ID, URL, PORT)

    // Main thread only (GeckoView's callbacks and the UI).
    private val registered = WeakHashMap<EngineSession, Unit>()
    private val ports = HashMap<String, Port>() // tab id → the frame driving its video
    private val pendingPosition = HashMap<String, Job>()
    private val posterFor = HashMap<String, String>() // tab id → the poster given as artwork
    private val posters = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>) = size > 4
    }

    private val _videos = MutableStateFlow<Map<String, WebVideo>>(emptyMap())

    /** Every tab's driven (fullscreen) video. */
    val videos: StateFlow<Map<String, WebVideo>> = _videos

    private val handler = object : MessageHandler {
        override fun onPortConnected(port: Port) = Unit

        override fun onPortDisconnected(port: Port) {
            val id = ports.entries.firstOrNull { it.value === port }?.key ?: return
            ports.remove(id)
            _videos.value = _videos.value - id
        }

        override fun onPortMessage(message: Any, port: Port) {
            val json = message as? JSONObject ?: return
            val id = tabOf(port) ?: return
            when (json.optString("type")) {
                "state" -> {
                    ports[id] = port
                    _videos.value = _videos.value + (id to parse(id, json))
                }
                "gone" -> if (ports[id] === port) {
                    ports.remove(id)
                    _videos.value = _videos.value - id
                }
                "media" -> onMedia(id, json)
            }
        }
    }

    private fun tabOf(port: Port): String? {
        val session = port.engineSession ?: return null
        return store.state.tabs.firstOrNull { it.engineState.engineSession === session }?.id
    }

    private fun parse(id: String, j: JSONObject) = WebVideo(
        tabId = id,
        fullscreen = j.optBoolean("fullscreen"),
        paused = j.optBoolean("paused", true),
        ended = j.optBoolean("ended"),
        waiting = j.optBoolean("waiting"),
        time = j.optDouble("time", 0.0),
        at = SystemClock.uptimeMillis(),
        duration = j.optDouble("duration", 0.0),
        buffered = j.optDouble("buffered", 0.0),
        rate = j.optDouble("rate", 1.0),
        muted = j.optBoolean("muted"),
        width = j.optInt("width"),
        height = j.optInt("height"),
        title = j.optString("title"),
        src = j.optString("src"),
    )

    /**
     * A video's position changed (play, pause, seek, speed): into the tab's media session, a beat
     * after Gecko's own update for the same event so it isn't overwritten by an empty one.
     */
    private fun onMedia(tabId: String, j: JSONObject) {
        val duration = j.optDouble("duration", 0.0).takeIf { it.isFinite() && it > 0 } ?: return
        val time = j.optDouble("time", 0.0)
        val rate = j.optDouble("rate", 1.0)
        val paused = j.optBoolean("paused")
        val sentAt = SystemClock.uptimeMillis()
        pendingPosition.remove(tabId)?.cancel()
        pendingPosition[tabId] = scope.launch(Dispatchers.Main) {
            delay(POSITION_DELAY_MS)
            val tab = store.state.findTab(tabId) ?: return@launch
            val media = tab.mediaSessionState ?: return@launch
            val elapsed = if (paused) 0.0 else (SystemClock.uptimeMillis() - sentAt) / 1000.0 * rate
            val position = (time + elapsed).coerceIn(0.0, duration)
            store.dispatch(MediaSessionAction.UpdateMediaPositionStateAction(tabId, MediaSession.PositionState(duration, position, rate)))
            val poster = j.optString("poster").takeIf { it.isNotEmpty() }
            if (poster != null && media.metadata?.getArtwork == null && posterFor[tabId] != poster) {
                posterFor[tabId] = poster
                val old = media.metadata
                store.dispatch(
                    MediaSessionAction.UpdateMediaMetadataAction(
                        tabId,
                        MediaSession.Metadata(old?.title, old?.artist, old?.album) { loadPoster(poster) },
                    ),
                )
            }
        }
    }

    /** The poster as the notification's artwork, small (a plain request to the page's own server). */
    private suspend fun loadPoster(url: String): Bitmap? {
        posters[url]?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 4000
                c.readTimeout = 4000
                try {
                    val bytes = c.inputStream.use { it.readBytes() }
                    if (bytes.size > MAX_POSTER_BYTES) return@runCatching null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= POSTER_PX) sample *= 2
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                } finally {
                    c.disconnect()
                }
            }.getOrNull()
        } ?: return null
        posters[url] = bmp
        return bmp
    }

    /** Installs the extension, then listens on every tab's page. Once, at launch. */
    fun start() {
        controller.install(engine, onSuccess = { scope.launch(Dispatchers.Main) { watch() } }, onError = {})
    }

    private suspend fun watch() {
        store.flow()
            .map { s -> s.tabs.mapNotNull { it.engineState.engineSession } to s.tabs.map { it.id }.toSet() }
            .distinctUntilChanged()
            .collect { (sessions, ids) ->
                sessions.forEach { session ->
                    if (registered.put(session, Unit) == null) controller.registerContentMessageHandler(session, handler)
                }
                // Closed tabs: their video is gone.
                if (_videos.value.keys.any { it !in ids }) _videos.value = _videos.value.filterKeys { it in ids }
                ports.keys.retainAll(ids)
                posterFor.keys.retainAll(ids)
            }
    }

    private fun send(tabId: String, message: JSONObject) {
        runCatching { ports[tabId]?.postMessage(message) }
    }

    fun play(tabId: String) = send(tabId, JSONObject().put("action", "play"))
    fun pause(tabId: String) = send(tabId, JSONObject().put("action", "pause"))
    fun seek(tabId: String, seconds: Double) = send(tabId, JSONObject().put("action", "seek").put("time", seconds))
    fun rate(tabId: String, rate: Double) = send(tabId, JSONObject().put("action", "rate").put("rate", rate))
    fun mute(tabId: String, muted: Boolean) = send(tabId, JSONObject().put("action", "mute").put("muted", muted))

    /** The page's own player back (the site's controls, or Koan let go of it). */
    fun release(tabId: String) {
        send(tabId, JSONObject().put("action", "release"))
        _videos.value = _videos.value - tabId
    }

    private companion object {
        const val ID = "video@koan.nerzouille.dev"
        const val URL = "resource://android/assets/extensions/video/"
        const val PORT = "koanVideo"
        const val POSITION_DELAY_MS = 150L
        const val MAX_POSTER_BYTES = 4_000_000
        const val POSTER_PX = 512
    }
}
