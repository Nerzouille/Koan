package dev.nerzouille.koan.engine

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.lib.state.ext.flow
import dev.nerzouille.koan.R
import dev.nerzouille.koan.data.Settings

/**
 * Picture-in-picture: leaving Koan (home gesture) while the current tab's video plays fullscreen
 * keeps it playing in a small system window, the video only (the page is fullscreen, so Gecko
 * shows the video alone), at its own aspect ratio, with 10 s back, play/pause and 10 s ahead. A tap
 * on the window comes back to the tab. Setting "Picture-in-picture" (on by default).
 *
 * Android 12+ enters on its own as the home gesture starts (setAutoEnterEnabled, the smooth
 * animation); Android 11 enters from onUserLeaveHint ([onUserLeaveHint]).
 */
class PictureInPicture(private val activity: Activity, private val store: BrowserStore, private val settings: Settings, scope: CoroutineScope) {
    /** Koan is in its PiP window: the page view shows the video only (no controls, no dock). */
    val inPip = MutableStateFlow(false)

    @Volatile private var eligible = false
    @Volatile private var params: PictureInPictureParams? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val media = store.state.selectedTab?.mediaSessionState ?: return
            val c = media.controller
            when (intent.getIntExtra(EXTRA, 0)) {
                PLAY_PAUSE -> if (media.playbackState == MediaSession.PlaybackState.PLAYING) c.pause() else c.play()
                BACK -> media.positionState.let { c.seekTo((it.position - 10).coerceAtLeast(0.0), false) }
                AHEAD -> media.positionState.let { p -> c.seekTo(if (p.duration > 0) (p.position + 10).coerceAtMost(p.duration) else p.position + 10, false) }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(activity, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            combine(store.flow(), settings.pictureInPicture) { s, on -> on to s.selectedTab }
                .collect { (on, tab) ->
                    val media = tab?.mediaSessionState
                    val video = media?.elementMetadata?.let { it.width > 0 && it.height > 0 } == true
                    val playing = media?.playbackState == MediaSession.PlaybackState.PLAYING
                    eligible = on && tab?.content?.fullScreen == true && video && playing
                    update(media)
                }
        }
    }

    private var last: String? = null

    private fun update(media: mozilla.components.browser.state.state.MediaSessionState?) {
        val meta = media?.elementMetadata
        val ratio = meta?.takeIf { it.width > 0 && it.height > 0 }?.let { clampRatio(it.width.toInt(), it.height.toInt()) } ?: Rational(16, 9)
        val playing = media?.playbackState == MediaSession.PlaybackState.PLAYING
        val key = "$eligible|$ratio|$playing"
        if (key == last) return
        last = key
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .setActions(
                listOf(
                    action(BACK, R.drawable.pip_back10, "Back 10 seconds"),
                    if (playing) action(PLAY_PAUSE, R.drawable.pip_pause, "Pause") else action(PLAY_PAUSE, R.drawable.pip_play, "Play"),
                    action(AHEAD, R.drawable.pip_ahead10, "Ahead 10 seconds"),
                ),
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(eligible).setSeamlessResizeEnabled(true)
        params = builder.build()
        activity.runOnUiThread { runCatching { activity.setPictureInPictureParams(params!!) } }
    }

    /** Android 11 has no auto-enter: the home gesture lands here first. */
    fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        val p = params ?: return
        if (eligible) runCatching { activity.enterPictureInPictureMode(p) }
    }

    fun onModeChanged(inPictureInPicture: Boolean) {
        inPip.value = inPictureInPicture
    }

    fun release() = runCatching { activity.unregisterReceiver(receiver) }

    private fun action(what: Int, icon: Int, title: String): RemoteAction {
        val intent = Intent(ACTION).setPackage(activity.packageName).putExtra(EXTRA, what)
        val pending = PendingIntent.getBroadcast(activity, what, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return RemoteAction(Icon.createWithResource(activity, icon), title, title, pending)
    }

    private companion object {
        const val ACTION = "dev.nerzouille.koan.PIP"
        const val EXTRA = "what"
        const val PLAY_PAUSE = 1
        const val BACK = 2
        const val AHEAD = 3

        /** Android refuses ratios beyond 2.39:1 either way. */
        fun clampRatio(w: Int, h: Int): Rational {
            val r = w.toFloat() / h
            return when {
                r > 2.39f -> Rational(239, 100)
                r < 1 / 2.39f -> Rational(100, 239)
                else -> Rational(w, h)
            }
        }
    }
}
