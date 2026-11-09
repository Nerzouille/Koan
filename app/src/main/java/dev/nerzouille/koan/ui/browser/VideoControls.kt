package dev.nerzouille.koan.ui.browser

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.theme.VideoTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.mediasession.MediaSession
import kotlin.math.abs

/** What the fullscreen video layer shows and drives (the tab's MediaSessionState, simplified). */
class VideoUi(
    val controller: MediaSession.Controller?,
    val playing: Boolean,
    /** Seconds; 0 when the page didn't say. Position as of [positionAt] (uptime ms), moving at [rate]. */
    val duration: Double,
    val position: Double,
    val positionAt: Long,
    val rate: Double,
    val muted: Boolean,
    /** A plain file (mp4, webm) Koan can download; null for streams. */
    val downloadUrl: String?,
)

/**
 * Koan's own controls over a fullscreen video (in the spirit of the Zen "Nebula" mod): always dark
 * whatever Koan's theme, they come out of a mist (blur → sharp, fade, a slight scale) and go back
 * into it after [VideoTokens.HIDE_MS] without a touch; a tap shows them again. Play/pause, 10 s
 * back and ahead (or a double tap on the left / right), a seek bar with the time, volume, rotate,
 * leave fullscreen (or a swipe down), download for a plain video file, and "the site's controls"
 * for a site whose own buttons are needed ([onSiteControls]: this layer steps aside until the next
 * fullscreen). Without blur below Android 12; no motion at all with "Remove animations".
 */
@Composable
fun VideoControls(
    ui: VideoUi,
    onExit: () -> Unit,
    onRotate: () -> Unit,
    onDownload: (String) -> Unit,
    onSiteControls: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val noMotion = remember { animationsOff(context) }
    val mist = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var flash by remember { mutableIntStateOf(0) } // -1 back, 1 ahead: the double-tap ripple side
    var flashSerial by remember { mutableIntStateOf(0) }
    val dragDown = remember { mutableFloatStateOf(0f) }

    fun touched() {
        lastTouch = SystemClock.uptimeMillis()
        shown = true
    }

    LaunchedEffect(shown) {
        if (noMotion) mist.snapTo(if (shown) 1f else 0f) else mist.animateTo(if (shown) 1f else 0f, tween(if (shown) VideoTokens.IN_MS else VideoTokens.OUT_MS))
    }
    // Back into the mist after a while, unless paused or scrubbing.
    LaunchedEffect(lastTouch, ui.playing, scrubbing) {
        if (!ui.playing || scrubbing != null) return@LaunchedEffect
        delay(VideoTokens.HIDE_MS)
        shown = false
    }

    // Where the video is now. Gecko reports the position rarely (at play, not after a seek), so
    // the layer keeps its own clock: from the last report, a seek, or a play/pause, moved on by
    // the time since while playing.
    var basePos by remember { mutableStateOf(ui.position) }
    var baseAt by remember { mutableLongStateOf(ui.positionAt.takeIf { it > 0 } ?: SystemClock.uptimeMillis()) }
    var now by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var wasPlaying by remember { mutableStateOf(ui.playing) }
    fun current(at: Long = SystemClock.uptimeMillis()) =
        if (wasPlaying) basePos + (at - baseAt).coerceAtLeast(0L) / 1000.0 * ui.rate else basePos
    LaunchedEffect(ui.position, ui.positionAt) {
        basePos = ui.position
        baseAt = ui.positionAt.takeIf { it > 0 } ?: SystemClock.uptimeMillis()
    }
    LaunchedEffect(ui.playing) {
        val at = SystemClock.uptimeMillis()
        basePos = current(at)
        baseAt = at
        wasPlaying = ui.playing
        while (ui.playing) {
            now = SystemClock.uptimeMillis()
            delay(VideoTokens.TICK_MS)
        }
        now = SystemClock.uptimeMillis()
    }
    val position = current(now).coerceIn(0.0, if (ui.duration > 0) ui.duration else Double.MAX_VALUE)

    fun seekTo(target: Double) {
        val c = ui.controller ?: return
        val t = target.coerceIn(0.0, if (ui.duration > 0) ui.duration else target)
        basePos = t
        baseAt = SystemClock.uptimeMillis()
        now = baseAt
        c.seekTo(t, false)
    }

    fun seekBy(seconds: Double) = seekTo(current() + seconds)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragDown.floatValue.coerceAtLeast(0f) * 0.5f }
            .pointerInput(ui.controller) {
                detectTapGestures(
                    onTap = {
                        if (shown && mist.value > 0.5f) shown = false else touched()
                    },
                    onDoubleTap = { at ->
                        val third = size.width / 3f
                        when {
                            at.x < third -> { seekBy(-VideoTokens.SKIP_S); flash = -1; flashSerial++ }
                            at.x > third * 2 -> { seekBy(VideoTokens.SKIP_S); flash = 1; flashSerial++ }
                            else -> if (ui.playing) ui.controller?.pause() else ui.controller?.play()
                        }
                        touched()
                    },
                )
            }
            .pointerInput(Unit) {
                // A swipe down leaves fullscreen (the layer follows the finger a little).
                val exitPx = with(density) { VideoTokens.SwipeExit.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = Offset.Zero
                    var vertical = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        total += change.position - change.previousPosition
                        if (!vertical && abs(total.y) > viewConfiguration.touchSlop && abs(total.y) > abs(total.x) * 1.5f && total.y > 0) vertical = true
                        if (vertical) {
                            change.consume()
                            dragDown.floatValue = total.y
                        }
                    }
                    if (vertical && total.y > exitPx) onExit()
                    scope.launch {
                        val from = dragDown.floatValue
                        Animatable(from).animateTo(0f, tween(VideoTokens.OUT_MS)) { dragDown.floatValue = value }
                    }
                }
            },
    ) {
        DoubleTapFlash(flash, flashSerial, noMotion)
        // The layer itself: a dark veil, top row, centre row, seek bar.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val m = mist.value
                    alpha = m
                    val s = 1f + (1f - m) * VideoTokens.MIST_SCALE
                    scaleX = s
                    scaleY = s
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !noMotion && m < 1f) {
                        val r = (1f - m) * VideoTokens.MistBlur.toPx()
                        if (r > 0.5f) renderEffect = BlurEffect(r, r, TileMode.Decal)
                    } else {
                        renderEffect = null
                    }
                }
                .background(Brush.verticalGradient(listOf(VideoColors.Veil, Color.Transparent, Color.Transparent, VideoColors.Veil))),
        ) {
            if (mist.value > 0f) {
                // Top: leave fullscreen, then the site's controls, rotate, volume, download.
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(VideoTokens.Edge),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassButton(KoanIcons.ExitFullscreen, "Leave fullscreen", shown) { onExit() }
                    Spacer(Modifier.weight(1f))
                    GlassButton(KoanIcons.SiteControls, "The site's own controls", shown) { onSiteControls() }
                    Spacer(Modifier.width(VideoTokens.Gap))
                    ui.downloadUrl?.let { url ->
                        GlassButton(KoanIcons.Download, "Download video", shown) { touched(); onDownload(url) }
                        Spacer(Modifier.width(VideoTokens.Gap))
                    }
                    GlassButton(KoanIcons.Rotate, "Rotate", shown) { touched(); onRotate() }
                    Spacer(Modifier.width(VideoTokens.Gap))
                    GlassButton(KoanIcons.Volume, if (ui.muted) "Unmute" else "Volume", shown, dim = ui.muted) {
                        touched()
                        if (ui.muted) ui.controller?.muteAudio(false) else showVolume(context)
                    }
                }
                // Centre: 10 s back, play/pause, 10 s ahead.
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VideoTokens.CentreGap),
                ) {
                    GlassButton(KoanIcons.Reload, "Back 10 seconds", shown, size = VideoTokens.SkipButton, mirror = true, number = "10") { touched(); seekBy(-VideoTokens.SKIP_S) }
                    GlassButton(if (ui.playing) KoanIcons.Pause else KoanIcons.Play, if (ui.playing) "Pause" else "Play", shown, size = VideoTokens.PlayButton) {
                        touched()
                        if (ui.playing) ui.controller?.pause() else ui.controller?.play()
                    }
                    GlassButton(KoanIcons.Reload, "Ahead 10 seconds", shown, size = VideoTokens.SkipButton, number = "10") { touched(); seekBy(VideoTokens.SKIP_S) }
                }
                // Bottom: time, seek bar, length.
                if (ui.duration > 0) {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = VideoTokens.Edge, vertical = VideoTokens.Edge)) {
                        val shownPos = scrubbing?.let { it * ui.duration } ?: position
                        Row(Modifier.fillMaxWidth()) {
                            TimeText(clock(shownPos))
                            Spacer(Modifier.weight(1f))
                            TimeText("-" + clock((ui.duration - shownPos).coerceAtLeast(0.0)))
                        }
                        Spacer(Modifier.height(VideoTokens.Gap / 2))
                        SeekBar(
                            fraction = (shownPos / ui.duration).toFloat().coerceIn(0f, 1f),
                            enabled = shown,
                            onScrub = { f -> touched(); scrubbing = f },
                            onSeek = { f ->
                                scrubbing = null
                                seekTo(f * ui.duration)
                                touched()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The double tap's answer: a soft glow on that side with "−10 s" / "+10 s". */
@Composable
private fun DoubleTapFlash(side: Int, serial: Int, noMotion: Boolean) {
    val glow = remember { Animatable(0f) }
    LaunchedEffect(serial) {
        if (serial == 0) return@LaunchedEffect
        glow.snapTo(1f)
        if (noMotion) { delay(VideoTokens.FLASH_MS.toLong()); glow.snapTo(0f) } else glow.animateTo(0f, tween(VideoTokens.FLASH_MS))
    }
    if (side == 0) return
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = glow.value }
            .drawBehind {
                val cx = if (side < 0) 0f else size.width
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(cx, size.height / 2), radius = size.height * 0.7f),
                    radius = size.height * 0.7f,
                    center = Offset(cx, size.height / 2),
                )
            },
        contentAlignment = if (side < 0) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            if (side < 0) "−10 s" else "+10 s",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}

/** A round frosted button. Taps only while the controls are out ([enabled]). */
@Composable
private fun GlassButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp = VideoTokens.Button,
    dim: Boolean = false,
    /** Back: the circular arrow turns the other way. */
    mirror: Boolean = false,
    /** "10" in the middle of a skip button's arrow. */
    number: String? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(VideoColors.Glass)
            .pointerInput(enabled) { if (enabled) detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, label,
            tint = if (dim) VideoColors.TextDim else Color.White,
            modifier = Modifier.size(size * (if (number != null) 0.62f else 0.46f)).graphicsLayer { if (mirror) scaleX = -1f },
        )
        if (number != null) Text(number, color = Color.White, fontSize = (size.value * 0.2f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimeText(text: String) {
    Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"))
}

/** The seek bar: a thin track that thickens under the finger; drag to scrub, let go to seek. */
@Composable
private fun SeekBar(fraction: Float, enabled: Boolean, onScrub: (Float) -> Unit, onSeek: (Float) -> Unit) {
    var active by remember { mutableStateOf(false) }
    var x by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(VideoTokens.SeekTouch)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onTap = { at -> onSeek((at.x / size.width).coerceIn(0f, 1f)) })
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { at -> active = true; x = at.x; onScrub((x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { active = false; onSeek((x / size.width).coerceIn(0f, 1f)) },
                    onDragCancel = { active = false; onSeek((x / size.width).coerceIn(0f, 1f)) },
                ) { change, dx ->
                    change.consume()
                    x = (x + dx).coerceIn(0f, size.width.toFloat())
                    onScrub(x / size.width)
                }
            }
            .drawBehind {
                val h = if (active) VideoTokens.SeekActive.toPx() else VideoTokens.SeekTrack.toPx()
                val y = (size.height - h) / 2
                val r = CornerRadius(h / 2)
                drawRoundRect(VideoColors.Track, Offset(0f, y), Size(size.width, h), r)
                drawRoundRect(Color.White, Offset(0f, y), Size(size.width * fraction, h), r)
                drawCircle(Color.White, radius = (if (active) VideoTokens.KnobActive else VideoTokens.Knob).toPx(), center = Offset(size.width * fraction, size.height / 2))
            },
    )
}

/** Always dark, whatever Koan's theme: the video decides the colours. */
private object VideoColors {
    val Veil = Color(0x99000000)
    val Glass = Color(0x33FFFFFF)
    val Track = Color(0x40FFFFFF)
    val TextDim = Color(0x80FFFFFF)
}

private fun clock(seconds: Double): String {
    val s = seconds.toLong()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** The system's volume slider (the page's own volume stays as it is). */
private fun showVolume(context: Context) {
    context.getSystemService(AudioManager::class.java)?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
}

/** "Remove animations" (Accessibility) or animator scale 0: no motion, things just appear. */
fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** A video Koan can download as a file: http(s), ending in .mp4 / .webm (not a stream, a blob, a manifest). */
fun downloadableVideo(source: String?): String? {
    val url = source?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return null
    val path = url.substringBefore('#').substringBefore('?').lowercase()
    return url.takeIf { path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".m4v") || path.endsWith(".mov") }
}
