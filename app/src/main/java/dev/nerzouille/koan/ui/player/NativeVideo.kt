package dev.nerzouille.koan.ui.player

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Koan's own video (the standalone player): the platform MediaPlayer on a TextureView (so it
 * moves and clips with the UI). mp4 / webm / mkv, from a content:// file or an http(s) address.
 * Main thread only.
 */
class NativeVideo(private val context: Context, private val uri: String) : PlayerControls {
    var state by mutableStateOf(PlayerState(paused = false, waiting = true, at = SystemClock.uptimeMillis()))
        private set
    var failed by mutableStateOf(false)
        private set

    /** Fill the screen (crop) instead of fitting (letterbox). */
    private var fillState by mutableStateOf(false)
    var fill: Boolean
        get() = fillState
        set(value) {
            fillState = value
            applyTransform()
        }

    private val player = MediaPlayer()
    private var prepared = false
    private var wantPlay = true
    private var view: TextureView? = null
    private var surface: Surface? = null
    private val main = Handler(Looper.getMainLooper())
    private var rate = 1.0
    private var muted = false

    private val tick = object : Runnable {
        override fun run() {
            update()
            main.postDelayed(this, 250)
        }
    }

    init {
        player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build())
        player.setOnPreparedListener {
            prepared = true
            applyTransform()
            if (wantPlay) runCatching { it.start() }
            update()
        }
        player.setOnVideoSizeChangedListener { _, _, _ ->
            applyTransform()
            update()
        }
        player.setOnCompletionListener { update(ended = true) }
        player.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> update(waiting = true)
                MediaPlayer.MEDIA_INFO_BUFFERING_END, MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> update(waiting = false)
            }
            false
        }
        player.setOnBufferingUpdateListener { _, percent -> buffered = percent / 100.0 }
        player.setOnErrorListener { _, _, _ ->
            failed = true
            waitingNow = false
            state = state.copy(waiting = false, paused = true)
            true
        }
        player.setOnSeekCompleteListener { update() }
        runCatching {
            player.setDataSource(context, Uri.parse(uri))
            player.prepareAsync()
        }.onFailure {
            failed = true
            state = state.copy(waiting = false, paused = true)
        }
        main.post(tick)
    }

    private var buffered = 0.0
    private var waitingNow = true

    /** Played to the end (some players report position 0 afterwards): until play or seek. */
    private var endedFlag = false

    private fun update(ended: Boolean = false, waiting: Boolean? = null) {
        if (waiting != null) waitingNow = waiting
        if (ended) endedFlag = true
        if (!prepared) return
        val duration = player.duration.takeIf { it > 0 }?.div(1000.0) ?: 0.0
        val playing = runCatching { player.isPlaying }.getOrDefault(false)
        if (playing) endedFlag = false
        val atEnd = endedFlag
        state = PlayerState(
            paused = !playing,
            ended = atEnd,
            waiting = waitingNow && playing,
            time = if (atEnd) duration else player.currentPosition / 1000.0,
            at = SystemClock.uptimeMillis(),
            rate = rate,
            duration = duration,
            // Local files are all there; a stream says how much it has.
            buffered = if (uri.startsWith("http")) buffered * duration else duration,
            muted = muted,
            width = player.videoWidth,
            height = player.videoHeight,
        )
    }

    fun attach(v: TextureView) {
        view = v
        v.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                surface = Surface(st).also { runCatching { player.setSurface(it) } }
                applyTransform()
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = applyTransform()

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                runCatching { player.setSurface(null) }
                surface?.release()
                surface = null
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
        v.surfaceTexture?.let { st -> surface = Surface(st).also { runCatching { player.setSurface(it) } } }
    }

    /** Letterbox (fit) or crop (fill) the video in its view, centred. */
    private fun applyTransform() {
        val v = view ?: return
        val vw = player.videoWidth.toFloat()
        val vh = player.videoHeight.toFloat()
        val w = v.width.toFloat()
        val h = v.height.toFloat()
        if (vw <= 0 || vh <= 0 || w <= 0 || h <= 0) return
        val fit = minOf(w / vw, h / vh)
        val cover = maxOf(w / vw, h / vh)
        val s = if (fill) cover else fit
        // TextureView stretches the video to the view: scale back to its shape.
        val sx = vw * s / w
        val sy = vh * s / h
        v.setTransform(Matrix().apply { setScale(sx, sy, w / 2, h / 2) })
    }

    override fun play() {
        wantPlay = true
        if (!prepared) return
        if (endedFlag) player.seekTo(0)
        endedFlag = false
        runCatching { player.start() }
        update()
    }

    override fun pause() {
        wantPlay = false
        if (prepared) runCatching { player.pause() }
        update()
    }

    override fun seek(seconds: Double) {
        if (!prepared) return
        endedFlag = false
        val ms = (seconds * 1000).toLong().coerceAtLeast(0)
        runCatching { player.seekTo(ms, MediaPlayer.SEEK_CLOSEST) }
        state = state.copy(time = seconds, at = SystemClock.uptimeMillis(), ended = false)
    }

    override fun rate(rate: Double) {
        this.rate = rate
        if (!prepared) return
        val wasPlaying = player.isPlaying
        runCatching {
            player.playbackParams = player.playbackParams.setSpeed(rate.toFloat())
            // Setting a speed starts a paused player: keep it paused.
            if (!wasPlaying) player.pause()
        }
        update()
    }

    override fun mute(muted: Boolean) {
        this.muted = muted
        val v = if (muted) 0f else 1f
        runCatching { player.setVolume(v, v) }
        update()
    }

    fun release() {
        main.removeCallbacks(tick)
        runCatching { player.stop() }
        player.release()
        surface?.release()
        surface = null
        view = null
    }
}
