package dev.nerzouille.koan.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.animationsOff
import dev.nerzouille.koan.ui.kit.blockTouches
import dev.nerzouille.koan.ui.theme.Motion
import kotlinx.coroutines.launch

/** What the standalone player's ⋯ menu does outside the player. */
class StandaloneActions(
    val share: () -> Unit,
    val openWith: () -> Unit,
    val delete: () -> Unit,
    val download: () -> Unit,
    val shareLink: () -> Unit,
    val pip: () -> Unit,
    /** Settings › Picture-in-picture, from the ⋯ menu. */
    val setPipOnLeave: (Boolean) -> Unit = {},
)

/**
 * Koan's own player over the current screen (§7): a video from Downloads, "Play video" on a
 * long-pressed link, or a video file another app opened with Koan. Never a tab: closing it leaves
 * the screen under it as it was. Grows from [StandaloneVideo.from] (or the centre) and goes back.
 */
@Composable
fun StandalonePlayer(video: StandaloneVideo, actions: StandaloneActions, inPip: Boolean, pipOnLeave: Boolean = true, onClosed: () -> Unit, onPlaying: (Boolean, Pair<Int, Int>?) -> Unit, onControls: (PlayerControls?, (() -> PlayerState)?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val native = remember(video.uri) { NativeVideo(context.applicationContext, video.uri) }
    DisposableEffect(native) {
        onControls(native) { native.state }
        onDispose {
            onControls(null, null)
            native.release()
        }
    }
    val noMotion = remember { animationsOff(context) }
    val st = native.state
    LaunchedEffect(st.playing, st.width, st.height) {
        onPlaying(st.playing, if (st.width > 0 && st.height > 0) st.width to st.height else null)
    }
    // Any orientation while it is open (the phone's sensor), portrait again once closed. With the
    // phone's auto-rotate off: the video's shape once it is known (F2), as a web fullscreen does.
    val activity = context as? Activity
    val rotationLocked = rememberRotationLocked()
    val shape = if (st.width > 0 && st.height > 0) st.height > st.width else null
    // The rotate pill (auto-rotate off) turns it the other way.
    var rotated by remember { mutableStateOf(false) }
    LaunchedEffect(rotationLocked, shape, rotated) {
        activity?.requestedOrientation = when {
            !rotationLocked -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            shape == null -> return@LaunchedEffect
            else -> fullscreenOrientation(fullscreen = true, videoPortrait = shape != rotated, rotationLocked = true, turned = false)
        }
    }
    DisposableEffect(Unit) {
        val before = activity?.requestedOrientation
        val window = activity?.window
        val bars = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        bars?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window?.let { immersive(it, true) }
        onDispose {
            window?.let { immersive(it, false) }
            activity?.requestedOrientation = before ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            bars?.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            bars?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

    // Portrait: the status bar stays and the top row sits under it; landscape: both bars away.
    val orientation = androidx.compose.ui.platform.LocalConfiguration.current.orientation
    LaunchedEffect(orientation) {
        activity?.window?.let { playerBars(it, orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) }
    }

    val open = remember { Animatable(if (noMotion) 1f else 0f) }
    // Controls wait until the morph is mostly done; read through a derived state, not per frame.
    val early by remember { derivedStateOf { open.value < 0.6f } }
    val backdrop = remember { Animatable(if (noMotion) 1f else 0f) }
    val dragY = remember { mutableFloatStateOf(0f) }
    val dragScale = remember { mutableFloatStateOf(1f) }
    var closing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch { backdrop.animateTo(1f, Motion.Fade) }
        open.animateTo(1f, PlayerTokens.OpenSpring)
    }
    fun close() {
        if (closing) return
        closing = true
        native.pause()
        scope.launch {
            if (!noMotion) {
                launch { backdrop.animateTo(0f, Motion.FadeSlow) }
                open.animateTo(0f, if (video.from != null) PlayerTokens.CloseSpring else Motion.FadeSlow)
            }
            onClosed()
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Nothing under the player takes a touch.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = backdrop.value }.background(PlayerTokens.Background))
        AndroidView(
            factory = { c -> TextureView(c).also(native::attach) },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = open.value
                    val from = video.from
                    if (from != null && from.width > 0f) {
                        // From the source rect to the full screen.
                        val sx = from.width / w
                        val sy = from.height / h
                        scaleX = (sx + (1 - sx) * p) * dragScale.floatValue
                        scaleY = (sy + (1 - sy) * p) * dragScale.floatValue
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = from.left * (1 - p) + w * (1 - scaleX) / 2 * p
                        translationY = from.top * (1 - p) + h * (1 - scaleY) / 2 * p + dragY.floatValue
                        alpha = 1f
                    } else {
                        val s = (0.9f + 0.1f * p) * dragScale.floatValue
                        scaleX = s
                        scaleY = s
                        alpha = p.coerceIn(0f, 1f)
                        translationY = dragY.floatValue
                    }
                },
        )
        if (native.failed) {
            Text(
                "Koan can't play this video",
                style = playerText(15.sp, FontWeight.Medium, PlayerTokens.TextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        val rows = remember(video.mode, video.download, actions, native, pipOnLeave) { listOf(pipOnLeaveRow(pipOnLeave, actions.setPipOnLeave)) + when (video.mode) {
            PlayerMode.File -> listOf(
                MenuRow(KoanIcons.Share, "Share") { actions.share() },
                MenuRow(KoanIcons.Open, "Open with another app") { native.pause(); actions.openWith() },
            ) + if (video.download != null) listOf(MenuRow(KoanIcons.Trash, "Delete", destructive = true) { confirmDelete = true }) else emptyList()
            else -> listOf(
                MenuRow(KoanIcons.Download, "Download video") { actions.download() },
                MenuRow(KoanIcons.Share, "Share link") { actions.shareLink() },
            )
        } }
        VideoPlayer(
            onRotate = { rotated = !rotated },
            state = st,
            controls = native,
            mode = video.mode,
            title = video.title,
            subtitle = video.subtitle,
            menuRows = rows,
            onLeading = ::close,
            onSwipeAway = ::close,
            onFullscreenButton = { native.fill = !native.fill },
            onPip = actions.pip,
            onBack = ::close,
            onDrag = { dy, s ->
                dragY.floatValue = dy
                dragScale.floatValue = s
            },
            hidden = inPip || closing || early,
            modifier = Modifier.graphicsLayer { alpha = if (closing) 0f else 1f },
        )
        if (confirmDelete) {
            DeleteCard(
                name = video.title,
                onCancel = { confirmDelete = false },
                onDelete = {
                    confirmDelete = false
                    actions.delete()
                    close()
                },
            )
        }
    }
}

/** The dark confirm card before deleting the file (§2 ⋯ menu, file). */
@Composable
private fun DeleteCard(name: String, onCancel: () -> Unit, onDelete: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PlayerTokens.Background.copy(alpha = 0.5f))
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume(); onCancel() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(300.dp)
                .playerPanel(RoundedCornerShape(PlayerTokens.MenuRadius), PlayerTokens.PanelMenu)
                .blockTouches()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Delete this video?", style = playerText(17.sp, FontWeight.SemiBold))
            Text(middleEllipsis(name, 40) + " is deleted from the phone.", style = playerText(14.sp, FontWeight.Normal, PlayerTokens.TextSecondary))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardButton("Cancel", PlayerTokens.Text, Modifier.weight(1f), onCancel)
                CardButton("Delete", PlayerTokens.Destructive, Modifier.weight(1f), onDelete)
            }
        }
    }
}

@Composable
private fun CardButton(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .playerPanel(RoundedCornerShape(PlayerTokens.PillRadius), PlayerTokens.Chip)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = playerText(15.sp, FontWeight.SemiBold, color))
    }
}
