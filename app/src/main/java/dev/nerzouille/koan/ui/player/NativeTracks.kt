package dev.nerzouille.koan.ui.player

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One of the file's tracks: [index] in MediaPlayer's track list. */
data class NativeTrack(val index: Int, val info: TrackInfo)

/**
 * The standalone player's audio and timed-text tracks ([NativeVideo]'s MediaPlayer): embedded
 * ones, and SubRip files added with [addSubtitles]. Only *timed text* is listed: the platform
 * draws its WebVTT / CEA subtitle tracks through a hidden controller and never gives their text.
 * Main thread only.
 */
class NativeTracks(private val player: MediaPlayer) {
    var audio by mutableStateOf(emptyList<NativeTrack>())
        private set
    var text by mutableStateOf(emptyList<NativeTrack>())
        private set

    /** The playing audio track and the shown text track (MediaPlayer's indexes), -1: none. */
    var audioSelected by mutableIntStateOf(-1)
        private set
    var textSelected by mutableIntStateOf(-1)
        private set

    /** The shown text track's current line(s); null: nothing on screen. */
    var caption by mutableStateOf<String?>(null)
        private set

    private var prepared = false
    private val addedLabels = HashMap<Int, String>() // track index → the added file's name
    private val pending = ArrayList<Pair<String, String>>() // path, label (before prepared)

    init {
        player.setOnTimedTextListener { _, t -> caption = t?.text?.trim()?.takeIf { it.isNotEmpty() } }
    }

    /** The file is ready: its tracks, then the subtitle files added before. */
    fun onPrepared() {
        prepared = true
        read()
        pending.toList().also { pending.clear() }.forEach { (path, label) -> addSubtitles(path, label) }
    }

    private fun read() {
        val info = runCatching { player.trackInfo }.getOrNull() ?: return
        fun of(type: Int) = info.withIndex().filter { it.value.trackType == type }.map { (i, t) ->
            NativeTrack(i, TrackInfo(addedLabels[i].orEmpty(), t.language.orEmpty()))
        }
        audio = of(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
        text = of(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
        audioSelected = runCatching { player.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) }.getOrDefault(-1)
        textSelected = runCatching { player.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) }.getOrDefault(-1)
    }

    /** Plays the audio track [index] (one of [audio]). */
    fun selectAudio(index: Int) {
        if (!prepared || audio.none { it.index == index }) return
        runCatching { player.selectTrack(index) }
        read()
    }

    /** Shows the text track [index] (one of [text]); -1: none. */
    fun selectText(index: Int) {
        if (!prepared) return
        if (index < 0) {
            if (textSelected >= 0) runCatching { player.deselectTrack(textSelected) }
        } else if (text.any { it.index == index }) {
            runCatching { player.selectTrack(index) }
        }
        caption = null
        read()
        // Some players keep the index after a deselect: Off is Off.
        if (index < 0) textSelected = -1
    }

    /** Adds a SubRip file ([path], on this phone) as a text track named [label], and shows it. */
    fun addSubtitles(path: String, label: String) {
        if (!prepared) {
            pending += path to label
            return
        }
        val before = runCatching { player.trackInfo.size }.getOrNull()
        val added = before != null && runCatching { player.addTimedTextSource(path, MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP) }.isSuccess
        val after = if (added) runCatching { player.trackInfo.size }.getOrNull() else null
        val newIndex = if (before != null && after != null && after > before) after - 1 else null
        newIndex?.let { addedLabels[it] = label }
        read()
        newIndex?.let(::selectText)
    }
}
