package dev.nerzouille.koan.ui.tour

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A tour clip: a looping, muted MP4 from res/raw, cropped to fill (centred). Played with the
 * platform's MediaPlayer on a TextureView, so the frame's rounded clip applies to it. Decoded only
 * while its card is composed (the one on screen, and its neighbour during a swipe).
 */
@Composable
fun TourVideo(@RawRes res: Int, modifier: Modifier) {
    key(res) {
        AndroidView(
            factory = { ClipView(it, res) },
            modifier = modifier,
            onRelease = { it.release() },
        )
    }
}

private class ClipView(context: Context, @RawRes private val res: Int) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var clipWidth = 0
    private var clipHeight = 0

    init {
        surfaceTextureListener = this
        // Nothing until the first frame: no black flash.
        alpha = 0f
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        val s = Surface(texture)
        surface = s
        val afd = context.resources.openRawResourceFd(res)
        player = MediaPlayer().apply {
            afd.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            setSurface(s)
            isLooping = true
            setVolume(0f, 0f)
            setOnVideoSizeChangedListener { _, w, h ->
                clipWidth = w
                clipHeight = h
                crop()
            }
            setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) animate().alpha(1f).setDuration(FADE_MS).start()
                false
            }
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = crop()

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}

    /** Scales the video to cover the view, centred (the view stretches it to fit by default). */
    private fun crop() {
        if (clipWidth == 0 || clipHeight == 0 || width == 0 || height == 0) return
        val cover = maxOf(width.toFloat() / clipWidth, height.toFloat() / clipHeight)
        val sx = clipWidth * cover / width
        val sy = clipHeight * cover / height
        setTransform(Matrix().apply { setScale(sx, sy, width / 2f, height / 2f) })
    }

    fun release() {
        player?.release()
        player = null
        surface?.release()
        surface = null
    }

    private companion object {
        const val FADE_MS = 150L
    }
}
