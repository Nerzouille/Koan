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
class PictureInPicture(
    private val activity: Activity,
    private val store: BrowserStore,
    private val settings: Settings,
    scope: CoroutineScope,
    /** The page videos as Koan's player sees them (engine/PageVideo): their size and play state when Gecko's media session has none. */
    private val videos: kotlinx.coroutines.flow.Flow<Map<String, WebVideo>> = kotlinx.coroutines.flow.flowOf(emptyMap()),
) {
    /** Koan is in its PiP window: the page view shows the video only (no controls, no dock). */
    val inPip = MutableStateFlow(false)

    @Volatile private var eligible = false
    @Volatile private var webEligible = false
    @Volatile private var webMedia: mozilla.components.browser.state.state.MediaSessionState? = null
    @Volatile private var standaloneOn = false
    @Volatile private var standalonePlaying = false
    @Volatile private var standaloneShape: Pair<Int, Int>? = null
    @Volatile private var enabled = true
    @Volatile private var pageShape: Pair<Int, Int>? = null

    /** Koan's standalone player (ui/player): open, playing, its video's size. */
    fun setStandalone(open: Boolean, playing: Boolean, shape: Pair<Int, Int>?) {
        standaloneOn = open
        standalonePlaying = playing
        standaloneShape = shape
        refresh()
    }

    private fun refresh() {
        eligible = enabled && ((standaloneOn && standalonePlaying) || (!standaloneOn && webEligible))
        update(webMedia)
    }

    /** The player's PiP button: into the window now. */
    fun enterNow() {
        eligible = true
        update(webMedia)
        val p = params ?: return
        runCatching { activity.enterPictureInPictureMode(p) }
        refresh()
    }
    @Volatile private var params: PictureInPictureParams? = null

    /**
     * "Audio only" (headphones, web videos): the PiP window closes, the video leaves fullscreen and
     * its sound keeps playing in the background with the media notification (MainActivity sets it).
     */
    @Volatile var audioOnly: (() -> Unit)? = null

    /** The standalone player's buttons, for the PiP window's actions while it is open. */
    @Volatile var standaloneControls: dev.nerzouille.koan.ui.player.PlayerControls? = null
    @Volatile var standaloneState: (() -> dev.nerzouille.koan.ui.player.PlayerState)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sc = standaloneControls
            val ss = standaloneState?.invoke()
            if (standaloneOn && sc != null && ss != null) {
                val t = ss.positionAt()
                when (intent.getIntExtra(EXTRA, 0)) {
                    PLAY_PAUSE -> if (ss.playing) sc.pause() else sc.play()
                    BACK -> sc.seek((t - 10).coerceAtLeast(0.0))
                    AHEAD -> sc.seek(if (ss.duration > 0) (t + 10).coerceAtMost(ss.duration) else t + 10)
                }
                return
            }
            val media = store.state.selectedTab?.mediaSessionState ?: return
            val c = media.controller
            when (intent.getIntExtra(EXTRA, 0)) {
                AUDIO -> audioOnly?.invoke()
                PLAY_PAUSE -> if (media.playbackState == MediaSession.PlaybackState.PLAYING) c.pause() else c.play()
                BACK -> media.positionState.let { c.seekTo((it.position - 10).coerceAtLeast(0.0), false) }
                AHEAD -> media.positionState.let { p -> c.seekTo(if (p.duration > 0) (p.position + 10).coerceAtMost(p.duration) else p.position + 10, false) }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(activity, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            combine(store.flow(), settings.pictureInPicture, videos) { s, on, v -> Triple(on, s.selectedTab, s.selectedTab?.id?.let { v[it] }) }
                .collect { (on, tab, page) ->
                    val media = tab?.mediaSessionState
                    enabled = on
                    webEligible = webPipEligible(
                        fullScreen = tab?.content?.fullScreen == true,
                        mediaSized = media?.elementMetadata?.let { it.width > 0 && it.height > 0 } == true,
                        mediaPlaying = media?.playbackState == MediaSession.PlaybackState.PLAYING,
                        page = page,
                    )
                    webMedia = media
                    pageShape = page?.takeIf { it.width > 0 && it.height > 0 }?.let { it.width to it.height }
                    refresh()
                }
        }
    }

    private var last: String? = null

    private fun update(media: mozilla.components.browser.state.state.MediaSessionState?) {
        val meta = media?.elementMetadata
        val shape = standaloneShape
        val ratio = when {
            standaloneOn && shape != null -> clampRatio(shape.first, shape.second)
            else -> meta?.takeIf { it.width > 0 && it.height > 0 }?.let { clampRatio(it.width.toInt(), it.height.toInt()) }
                ?: pageShape?.let { clampRatio(it.first, it.second) }
                ?: Rational(16, 9)
        }
        val playing = if (standaloneOn) standalonePlaying else media?.playbackState == MediaSession.PlaybackState.PLAYING || webEligible
        val key = "$eligible|$ratio|$playing|$standaloneOn"
        if (key == last) return
        last = key
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .setActions(actions(playing))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(eligible).setSeamlessResizeEnabled(true)
        params = builder.build()
        activity.runOnUiThread { runCatching { activity.setPictureInPictureParams(params!!) } }
    }

    /**
     * 10 s back, play/pause, 10 s ahead, and for a web video "Audio only". A phone shows 3 actions
     * at most on some systems: then audio only, play/pause and 10 s ahead.
     */
    private fun actions(playing: Boolean): List<RemoteAction> {
        val play = if (playing) action(PLAY_PAUSE, R.drawable.pip_pause, "Pause") else action(PLAY_PAUSE, R.drawable.pip_play, "Play")
        val back = action(BACK, R.drawable.pip_back10, "Back 10 seconds")
        val ahead = action(AHEAD, R.drawable.pip_ahead10, "Ahead 10 seconds")
        if (standaloneOn) return listOf(back, play, ahead)
        val audio = action(AUDIO, R.drawable.pip_audio, "Audio only")
        val max = runCatching { activity.maxNumPictureInPictureActions }.getOrDefault(3)
        return if (max >= 4) listOf(back, play, ahead, audio) else listOf(audio, play, ahead)
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
        const val AUDIO = 4

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

/**
 * A fullscreen web video goes to PiP on leaving Koan only while it plays (owner, 2026-11-12: never
 * a paused one). Gecko's media session sometimes has no size or play state for a page's video
 * (fixes F5): then Koan's own view of it ([page], the video extension) counts too.
 */
internal fun webPipEligible(fullScreen: Boolean, mediaSized: Boolean, mediaPlaying: Boolean, page: WebVideo?): Boolean {
    if (!fullScreen) return false
    val sized = mediaSized || page != null && page.width > 0 && page.height > 0
    val playing = mediaPlaying || page != null && page.fullscreen && !page.paused && !page.ended
    return sized && playing
}
