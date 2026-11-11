package dev.nerzouille.koan.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.animationsOff
import dev.nerzouille.koan.ui.kit.pressScale
import dev.nerzouille.koan.ui.kit.rememberPresence
import dev.nerzouille.koan.ui.theme.Motion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The controls take touches only while shown (they are composed while fading out too). */
internal val LocalControlsLive = compositionLocalOf { true }

/**
 * Koan's video player (docs/VIDEO_PLAYER.md), one component for both modes: over a page's
 * fullscreen video ([PlayerMode.Web], the video is the page view under it) or over Koan's own
 * video ([PlayerMode.File] / [PlayerMode.Link], drawn by the caller under it). Always dark.
 *
 * [onDrag]: a swipe down in the centre moves the video (translationY px, scale); the caller
 * applies it. [onSwipeAway] when that swipe is released past the threshold (minimise / close).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VideoPlayer(
    state: PlayerState,
    controls: PlayerControls,
    mode: PlayerMode,
    title: String,
    subtitle: String,
    menuRows: List<MenuRow>,
    /** ✕ Close in the title pill (standalone); null: no button (web: a swipe down leaves). */
    onLeading: (() -> Unit)?,
    onSwipeAway: () -> Unit,
    onFullscreenButton: () -> Unit,
    onPip: () -> Unit,
    onBack: () -> Unit,
    onDrag: (dy: Float, scale: Float) -> Unit,
    modifier: Modifier = Modifier,
    /** Picture-in-picture or a transition: nothing but the video. */
    hidden: Boolean = false,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val noMotion = remember { animationsOff(context) }
    val live = rememberUpdatedState(state)
    val ctl = rememberUpdatedState(controls)

    var shown by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var menuOpen by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var unlockShown by remember { mutableStateOf(false) }
    var unlockSerial by remember { mutableIntStateOf(0) }
    // The bar pressed: composition reads only this flag; the fraction and the finger's x move every
    // finger event and are read in draw / layout lambdas and one small time text.
    var scrubbing by remember { mutableStateOf(false) }
    val scrubF = remember { mutableFloatStateOf(0f) } // fraction while the bar is pressed
    var scrubX by remember { mutableFloatStateOf(0f) } // finger x in this layer
    val mistBlurs = remember(density) { mistBlurs(density.density) }
    var boost by remember { mutableStateOf(false) } // long press 2×
    var seekSide by remember { mutableIntStateOf(0) } // -1 left, 1 right, 0 none
    var seekSteps by remember { mutableIntStateOf(0) }
    var seekSerial by remember { mutableIntStateOf(0) }
    var levelKind by remember { mutableStateOf<Boolean?>(null) } // true volume, false brightness
    var levelDown by remember { mutableStateOf(false) }
    var levelSerial by remember { mutableIntStateOf(0) }
    val levelTarget = remember { mutableFloatStateOf(0f) }
    val levelShown = remember { Animatable(0f) }
    val levelOn by remember { derivedStateOf { levelShown.value > 0f } }
    val levelAnim = remember { Animatable(0f) }

    fun touched() {
        lastTouch = SystemClock.uptimeMillis()
    }

    // ---- the mist (§5 controls.in / controls.out) ----
    val mist = remember { Animatable(1f) }
    val settle = remember { Animatable(1f) }
    var entering by remember { mutableStateOf(true) }
    LaunchedEffect(shown) {
        entering = shown
        if (noMotion) {
            mist.snapTo(if (shown) 1f else 0f)
            settle.snapTo(1f)
            return@LaunchedEffect
        }
        if (shown) {
            settle.snapTo(0f)
            launch { settle.animateTo(1f, PlayerTokens.ControlsSpring) }
            mist.animateTo(1f, PlayerTokens.ControlsIn)
        } else {
            mist.animateTo(0f, PlayerTokens.ControlsOut)
        }
    }
    // §5 controls.autoHide
    LaunchedEffect(shown, lastTouch, state.playing, menuOpen, scrubbing, locked) {
        if (!shown || locked || !state.playing || menuOpen || scrubbing) return@LaunchedEffect
        delay(PlayerTokens.AUTO_HIDE_MS)
        shown = false
    }
    // Locked: a tap shows Unlock for 3 s.
    LaunchedEffect(unlockSerial) {
        if (!unlockShown) return@LaunchedEffect
        delay(PlayerTokens.UNLOCK_MS)
        unlockShown = false
    }
    // Ended: the controls come back and stay (Replay).
    LaunchedEffect(state.ended) { if (state.ended) shown = true }

    // A frame clock for the progress bar, read in draw only (no recomposition per frame).
    val frameNow = remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(state.playing, state.waiting) {
        if (!state.playing || state.waiting) {
            frameNow.longValue = SystemClock.uptimeMillis()
            return@LaunchedEffect
        }
        while (isActive) withFrameMillis { frameNow.longValue = SystemClock.uptimeMillis() }
    }
    // The times, once a quarter second.
    var clockNow by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(state.playing, state.at) {
        clockNow = SystemClock.uptimeMillis()
        while (state.playing && isActive) {
            delay(250)
            clockNow = SystemClock.uptimeMillis()
        }
    }
    // §5 buffering: the spinner after 400 ms of stall.
    var spinner by remember { mutableStateOf(false) }
    LaunchedEffect(state.waiting) {
        if (!state.waiting) {
            spinner = false
            return@LaunchedEffect
        }
        delay(PlayerTokens.SPINNER_DELAY_MS)
        spinner = true
    }

    // ---- brightness and volume ----
    val activity = context as? Activity
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    // The window's brightness goes back to the system's when the player leaves.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
        }
    }
    fun brightness(): Float {
        val own = activity?.window?.attributes?.screenBrightness ?: -1f
        if (own >= 0f) return own
        val sys = runCatching { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) }.getOrDefault(128)
        return (sys / 255f).coerceIn(0f, 1f)
    }
    fun setBrightness(f: Float) {
        activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = f.coerceIn(0.01f, 1f) } }
    }
    fun volume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }
    fun setVolume(f: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (f * max).roundToInt().coerceIn(0, max), 0) }
    }
    LaunchedEffect(levelSerial) {
        if (levelKind == null) return@LaunchedEffect
        if (noMotion) levelShown.snapTo(1f) else levelShown.animateTo(1f, Motion.Fade)
    }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { levelTarget.floatValue }.collect { t ->
            launch { if (noMotion) levelAnim.snapTo(t) else levelAnim.animateTo(t, PlayerTokens.LevelFill) }
        }
    }
    LaunchedEffect(levelDown, levelSerial) {
        if (levelDown || levelKind == null) return@LaunchedEffect
        delay(PlayerTokens.LEVEL_HOLD_MS)
        if (noMotion) levelShown.snapTo(0f) else levelShown.animateTo(0f, Motion.FadeSlow)
        levelKind = null
    }
    val levelScale by animateFloatAsState(if (levelDown) 1.06f else 1f, PlayerTokens.LevelScale, label = "levelScale")

    // ---- double-tap seek feedback (§5 seek.glow) ----
    val glow = remember { Animatable(0f) }
    val glowOn by remember { derivedStateOf { glow.value > 0f } }
    val pulse = remember { Animatable(1f) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(seekSerial) {
        if (seekSide == 0) return@LaunchedEffect
        launch { pulse.snapTo(0.96f); pulse.animateTo(1f, PlayerTokens.GlowPulse) }
        launch { turn.snapTo(30f * seekSide); turn.animateTo(0f, PlayerTokens.GlyphTurn) }
        if (noMotion) glow.snapTo(1f) else glow.animateTo(1f, Motion.FadeQuick)
        delay(PlayerTokens.GLOW_HOLD_MS)
        if (noMotion) glow.snapTo(0f) else glow.animateTo(0f, tween(400, easing = LinearOutSlowInEasing))
        seekSide = 0
        seekSteps = 0
    }

    BackHandler(enabled = !hidden) {
        when {
            menuOpen -> menuOpen = false
            else -> onBack()
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        // Every pill 20dp from the screen edges; the display cutout pushes a control in only on
        // its own side, and only when that control would sit under it (PlayerInsets).
        val cut = rememberCutouts(landscape)
        val bottomPillH = if (landscape) PlayerTokens.BottomPillHeightLandscape else PlayerTokens.BottomPillHeightPortrait
        val side = PlayerTokens.Inset
        val ins = playerInsets(cut, maxWidth, maxHeight, bottomPillH, density)
        val startInset = ins.titleStart
        val endInset = ins.rightEnd
        val topInset = ins.top
        val bottomInset = ins.bottom
        // The level indicator: between the top pills and the bottom pill. Its drag is relative:
        // from where the finger lands, LEVEL_RANGE of the screen height sweeps 0 → 100.
        val levelRoom = maxHeight - topInset - PlayerTokens.RoundPill - bottomInset - bottomPillH - PlayerTokens.LevelGap * 2
        val levelHeight = levelRoom.coerceIn(PlayerTokens.LevelMin, PlayerTokens.LevelSize.height)
        val levelRangePx = PlayerTokens.LEVEL_RANGE * heightPx
        val slop = with(density) { PlayerTokens.SwipeSlop.toPx() }
        val fling = with(density) { PlayerTokens.FlingCommit.toPx() }
        val swipeY = remember { Animatable(0f) }

        fun dragOut(dy: Float) {
            val s = 1f - 0.1f * (dy / (PlayerTokens.SWIPE_DOWN_SCALE_AT * heightPx)).coerceIn(0f, 1f)
            onDrag(dy, s)
        }

        // ---- the gestures on the video (§4) ----
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(landscape, widthPx, heightPx, levelRangePx) {
                    var lastTapUp = 0L
                    var lastTapZone = 0
                    var shownBeforeTap = true
                    var lastSeekAt = 0L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        if (hidden) return@awaitEachGesture
                        val zone = when {
                            down.position.x < widthPx * PlayerTokens.SIDE_ZONE -> -1
                            down.position.x > widthPx * (1 - PlayerTokens.SIDE_ZONE) -> 1
                            else -> 0
                        }
                        touched()
                        if (menuOpen) {
                            // A tap beside the menu only closes it.
                            down.consume()
                            menuOpen = false
                            return@awaitEachGesture
                        }
                        val start = down.position
                        val velocity = VelocityTracker()
                        velocity.addPosition(down.uptimeMillis, down.position)
                        var mode = 0 // 0 undecided, 1 level, 2 swipe down, 3 long press, 4 horizontal (ignored)
                        var levelStart = 0f
                        var levelFrom = 0f
                        var lastLevelTick = -1
                        var pressJob: Job? = null
                        if (!locked && live.value.playing) {
                            pressJob = scope.launch {
                                delay(PlayerTokens.LONG_PRESS_MS)
                                if (mode == 0) {
                                    mode = 3
                                    boost = true
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    ctl.value.rate(2.0)
                                }
                            }
                        }
                        val previousRate = live.value.rate
                        var up = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            velocity.addPosition(change.uptimeMillis, change.position)
                            if (!change.pressed) {
                                up = true
                                change.consume()
                                break
                            }
                            val d = change.position - start
                            if (mode == 0 && !locked && (abs(d.x) > slop || abs(d.y) > slop)) {
                                mode = when {
                                    // Down from the centre: leave (or close); any other vertical move: the
                                    // level of the half it started on.
                                    abs(d.y) > PlayerTokens.SWIPE_RATIO * abs(d.x) && zone == 0 && d.y > 0 -> 2
                                    abs(d.y) > PlayerTokens.SWIPE_RATIO * abs(d.x) -> 1
                                    else -> 4
                                }
                                pressJob?.cancel()
                                if (mode == 1) {
                                    val right = start.x > widthPx / 2
                                    levelKind = right
                                    levelStart = if (right) volume() else brightness()
                                    levelFrom = change.position.y
                                    levelTarget.floatValue = levelStart
                                    scope.launch { levelAnim.snapTo(levelStart) }
                                    levelDown = true
                                    levelSerial++
                                }
                                if (mode == 2) shown = false
                            }
                            when (mode) {
                                1 -> {
                                    val f = (levelStart - (change.position.y - levelFrom) / levelRangePx).coerceIn(0f, 1f)
                                    levelTarget.floatValue = f
                                    if (levelKind == true) setVolume(f) else setBrightness(f)
                                    val edge = if (f <= 0f) 0 else if (f >= 1f) 1 else -1
                                    if (edge >= 0 && edge != lastLevelTick) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    lastLevelTick = edge
                                }
                                2 -> {
                                    val dy = d.y.coerceAtLeast(0f)
                                    scope.launch { swipeY.snapTo(dy) }
                                    dragOut(dy)
                                }
                            }
                            if (mode != 0) change.consume()
                        }
                        pressJob?.cancel()
                        when (mode) {
                            1 -> levelDown = false
                            2 -> {
                                val dy = swipeY.value
                                val v = velocity.calculateVelocity().y
                                if (up && (dy > PlayerTokens.SWIPE_DOWN_COMMIT * heightPx || v >= fling)) {
                                    onSwipeAway()
                                } else {
                                    scope.launch {
                                        swipeY.animateTo(0f, PlayerTokens.SwipeBack) { dragOut(value) }
                                        shown = true
                                    }
                                }
                            }
                            3 -> {
                                boost = false
                                ctl.value.rate(previousRate)
                            }
                            0 -> if (up) {
                                val now = SystemClock.uptimeMillis()
                                when {
                                    locked -> {
                                        unlockShown = true
                                        unlockSerial++
                                    }
                                    // Another tap on the same side while a seek shows: 10 s more.
                                    zone != 0 && zone == seekSide && now - lastSeekAt < PlayerTokens.DOUBLE_TAP_WINDOW_MS -> {
                                        seekSteps++
                                        seekSerial++
                                        lastSeekAt = now
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        seekBy(live.value, ctl.value, zone)
                                    }
                                    // The second tap of a pair on a side: a seek, and the first tap's toggle undone.
                                    zone != 0 && zone == lastTapZone && now - lastTapUp < PlayerTokens.TAP_PAIR_MS -> {
                                        lastTapUp = 0L
                                        shown = false
                                        if (shownBeforeTap && !live.value.playing) shown = shownBeforeTap
                                        seekSide = zone
                                        seekSteps = 1
                                        seekSerial++
                                        lastSeekAt = now
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        seekBy(live.value, ctl.value, zone)
                                    }
                                    else -> {
                                        // A tap shows or hides the controls at once.
                                        shownBeforeTap = shown
                                        shown = !shown
                                        lastTapUp = now
                                        lastTapZone = zone
                                    }
                                }
                            }
                        }
                    }
                },
        )

        // ---- scrims (with the controls) ----
        Box(
            Modifier
                .fillMaxWidth()
                .height(PlayerTokens.ScrimTop)
                .graphicsLayer { alpha = mist.value * hiddenAlpha(hidden) * (1f - levelShown.value) }
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = PlayerTokens.SCRIM_TOP_ALPHA), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(PlayerTokens.ScrimBottom)
                .graphicsLayer { alpha = mist.value * hiddenAlpha(hidden) * (1f - levelShown.value) }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = PlayerTokens.SCRIM_BOTTOM_ALPHA)))),
        )

        // ---- seek glow and label ----
        if (seekSide != 0 || glowOn) {
            val sideNow = if (seekSide != 0) seekSide else 1
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = glow.value; scaleX = pulse.value; scaleY = pulse.value }) {
                val boxW = size.width * 0.55f
                val cx = if (sideNow < 0) 0f else size.width
                val cy = size.height / 2
                val rx = boxW * 0.70f * 1.4f
                val ry = size.height * 0.62f
                scale(rx / ry, 1f, Offset(cx, cy)) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.22f),
                            0.42f to Color.White.copy(alpha = 0.10f),
                            1f to Color.Transparent,
                            center = Offset(cx, cy),
                            radius = ry,
                        ),
                        radius = ry,
                        center = Offset(cx, cy),
                    )
                }
            }
            val centreFrac = if (landscape) 0.20f else 0.29f
            val labelX = if (sideNow < 0) maxWidth * centreFrac else maxWidth * (1 - centreFrac)
            SeekLabel(
                forward = sideNow > 0,
                steps = seekSteps.coerceAtLeast(1),
                turn = { turn.value },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .centredAt(labelX)
                    .graphicsLayer { alpha = glow.value },
            )
        }

        CompositionLocalProvider(LocalControlsLive provides (shown && !hidden)) {
            val group: GraphicsLayerScope.(TransformOrigin, Float) -> Unit = { origin, inward ->
                applyMist(mist.value, settle.value, entering, origin, inward * PlayerTokens.MistShift.toPx(), noMotion, mistBlurs)
                alpha *= hiddenAlpha(hidden) * (1f - levelShown.value)
            }
            val buttonsLive = shown && !hidden
            // ---- top: title pill, round pills ----
            var rightWidth by remember { mutableIntStateOf(0) }
            val rightDp = with(density) { rightWidth.toDp() }
            val titleMax = if (landscape) maxWidth * PlayerTokens.TitleMaxLandscape else maxWidth - startInset - endInset - 8.dp - rightDp
            if (!locked) {
                TitlePill(
                    mode = mode,
                    title = title,
                    subtitle = subtitle,
                    onLeading = onLeading?.let { act -> { if (buttonsLive) act() } },
                    modifier = Modifier
                        .padding(start = startInset, top = topInset)
                        .widthIn(max = titleMax.coerceAtLeast(120.dp))
                        .graphicsLayer { group(TransformOrigin(0f, 0f), 1f) },
                )
            }
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = endInset, top = topInset)
                    .onSizeChanged { rightWidth = it.width }
                    .graphicsLayer {
                        if (locked) {
                            alpha = if (unlockShown) 1f else 0f
                        } else {
                            group(TransformOrigin(1f, 0f), 1f)
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(PlayerTokens.RoundGap),
            ) {
                if (locked) {
                    UnlockPill(onClick = {
                        if (unlockShown) {
                            locked = false
                            unlockShown = false
                            shown = true
                            touched()
                        }
                    })
                    Spacer(Modifier.width(PlayerTokens.RoundPill))
                } else {
                    if (abs(state.rate - 1.0) > 0.01 && !boost) SpeedPill(state.rate) { if (buttonsLive) { menuOpen = true; touched() } }
                    PlayerDisc(PlayerTokens.RoundPill, onClick = { if (buttonsLive) onPip() }) { PlayerIcon(KoanIcons.Pip, PlayerTokens.Glyph) }
                    PlayerDisc(PlayerTokens.RoundPill, onClick = {
                        if (buttonsLive) {
                            locked = true
                            menuOpen = false
                            shown = false
                            unlockShown = true
                            unlockSerial++
                        }
                    }) { PlayerIcon(KoanIcons.Lock, PlayerTokens.Glyph) }
                    PlayerDisc(PlayerTokens.RoundPill, onClick = { if (buttonsLive) { menuOpen = !menuOpen; touched() } }) { PlayerIcon(KoanIcons.More, PlayerTokens.Glyph) }
                }
            }

            if (!locked) {
                // ---- centre group (hidden while the bar is pressed) ----
                val centreOut by animateFloatAsState(if (scrubbing) 0f else 1f, Motion.Fade, label = "centreOut")
                if (!spinner) {
                    CentreGroup(
                        state = state,
                        landscape = landscape,
                        onBack10 = { if (buttonsLive) { touched(); seekBy(state, controls, -1) } },
                        onAhead10 = { if (buttonsLive) { touched(); seekBy(state, controls, 1) } },
                        onPlay = { if (buttonsLive) { touched(); toggle(state, controls) } },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                group(TransformOrigin.Center, 0f)
                                alpha *= centreOut
                            },
                    )
                }

                // ---- bottom pill ----
                BottomPill(
                    state = state,
                    landscape = landscape,
                    mode = mode,
                    frameNow = { frameNow.longValue },
                    clockNow = clockNow,
                    scrubbing = scrubbing,
                    scrubFraction = { scrubF.floatValue },
                    onScrub = { f, x ->
                        scrubbing = f != null
                        // Released: the fraction goes back to 0, as the bubble showed it on its way out.
                        scrubF.floatValue = f ?: 0f
                        if (f != null) scrubX = x
                        touched()
                    },
                    onSeek = { seconds -> controls.seek(seconds) },
                    onPlay = { if (buttonsLive) { touched(); toggle(state, controls) } },
                    onMute = { if (buttonsLive) { touched(); controls.mute(!state.muted) } },
                    onFullscreen = { if (buttonsLive) { touched(); onFullscreenButton() } },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = ins.bottomStart, end = ins.bottomEnd, bottom = bottomInset)
                        .graphicsLayer { group(TransformOrigin(0.5f, 1f), -1f) },
                )

                // ---- time bubble (while dragging the bar) ----
                val bubble = rememberPresence(scrubbing, enter = Motion.FadeQuick, exit = Motion.Fade, startShown = true)
                if (bubble.composed) {
                    val pillH = if (landscape) PlayerTokens.BottomPillHeightLandscape else PlayerTokens.BottomPillHeightPortrait
                    val half = PlayerTokens.BubbleSize.width / 2
                    val y = -(bottomInset + pillH + PlayerTokens.BubbleGap)
                    val minX = side + half
                    val maxX = maxWidth - side - half
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            // Follows the finger in layout only (no recomposition per move).
                            .offset {
                                val x = scrubX.coerceIn(minX.toPx(), maxX.toPx().coerceAtLeast(minX.toPx()))
                                IntOffset((x - half.toPx()).roundToInt(), y.roundToPx())
                            }
                            .size(PlayerTokens.BubbleSize.width, PlayerTokens.BubbleSize.height)
                            .graphicsLayer { alpha = bubble.progress }
                            .playerPanel(RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ScrubTime({ scrubF.floatValue }, state.duration, playerText(15.sp, FontWeight.SemiBold))
                    }
                }

                // ---- ⋯ menu ----
                val menuIn = rememberPresence(menuOpen, enter = Motion.Fade, exit = Motion.FadeQuick, startShown = true)
                if (menuIn.composed) {
                    val rows = remember(menuRows) { menuRows.map { row -> MenuRow(row.icon, row.label, row.destructive) { menuOpen = false; row.onClick() } } }
                    PlayerMenu(
                        rate = state.rate,
                        onRate = { r -> controls.rate(r); touched() },
                        rows = rows,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = endInset, top = topInset + PlayerTokens.RoundPill + 8.dp)
                            .graphicsLayer {
                                alpha = menuIn.progress
                                val s = 0.92f + 0.08f * menuIn.progress
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(1f - (PlayerTokens.RoundPill / 2 / PlayerTokens.MenuWidth), 0f)
                            }
                            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().also { touched() } } },
                    )
                }
            }
        }

        // ---- buffering spinner (stays alone when the controls hide) ----
        val spin = rememberPresence(spinner, Motion.FadeSlow, startShown = true)
        if (spin.composed && !locked) {
            PlayerSpinner(PlayerTokens.Spinner, PlayerTokens.SpinnerStroke, modifier = Modifier.align(Alignment.Center).graphicsLayer { alpha = spin.progress * hiddenAlpha(hidden) })
        }

        // ---- long-press 2× badge ----
        val badge = rememberPresence(boost, enter = Motion.FadeSlow, exit = Motion.Fade, startShown = true)
        if (badge.composed) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset + 6.dp)
                    .graphicsLayer { alpha = badge.progress }
                    .height(PlayerTokens.BadgeHeight)
                    .playerPanel(RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("2×", style = playerText(15.sp, FontWeight.Bold))
                PlayerIcon(KoanIcons.Speed, 18.dp)
            }
        }

        // ---- brightness / volume level ----
        val kind = levelKind
        if (kind != null || levelOn) {
            val right = kind ?: true
            val edge = if (landscape) PlayerTokens.LevelEdgeLandscape else PlayerTokens.LevelEdgePortrait
            LevelIndicator(
                volume = right,
                level = { levelAnim.value },
                height = levelHeight,
                modifier = Modifier
                    .align(if (right) Alignment.TopEnd else Alignment.TopStart)
                    .padding(
                        start = if (right) 0.dp else maxOf(edge, ins.levelStart),
                        end = if (right) maxOf(edge, ins.levelEnd) else 0.dp,
                        top = topInset + PlayerTokens.RoundPill + PlayerTokens.LevelGap + (levelRoom - levelHeight).coerceAtLeast(0.dp) / 2,
                    )
                    .graphicsLayer {
                        val a = levelShown.value
                        alpha = a
                        val s = (0.94f + 0.06f * a) * levelScale
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(if (right) 1f else 0f, 0.5f)
                    },
            )
        }
    }
}

private fun hiddenAlpha(hidden: Boolean) = if (hidden) 0f else 1f

/** §5 controls.in / out on one group: alpha, blur (Android 12+), scale, a short slide from inward. */
private fun GraphicsLayerScope.applyMist(m: Float, settle: Float, entering: Boolean, origin: TransformOrigin, inwardPx: Float, noMotion: Boolean, blurs: Array<RenderEffect?>) {
    alpha = m
    transformOrigin = origin
    if (noMotion) return
    val s: Float
    val blurDp: Float
    if (entering) {
        s = 0.96f + 0.04f * settle
        translationY = inwardPx * (1f - settle)
        blurDp = PlayerTokens.MistBlurIn.value * (1f - m)
    } else {
        s = 0.98f + 0.02f * m
        blurDp = PlayerTokens.MistBlurOut.value * (1f - m)
    }
    scaleX = s
    scaleY = s
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // RenderEffects are immutable: one per 0.5dp step, reused every frame (no new effect per frame).
        renderEffect = if (m < 0.999f) blurs[(blurDp * 2f).roundToInt().coerceIn(0, blurs.size - 1)] else null
    }
}

/** The mist's blurs, 0 to the largest mist radius in 0.5dp steps (step 0: none). */
private fun mistBlurs(density: Float): Array<RenderEffect?> {
    val steps = (maxOf(PlayerTokens.MistBlurIn.value, PlayerTokens.MistBlurOut.value) * 2f).roundToInt() + 1
    return Array(steps) { step ->
        if (step == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null else BlurEffect(step * 0.5f * density, step * 0.5f * density, TileMode.Decal)
    }
}

/** The bubble's time while scrubbing: recomposes when the shown second changes, not per finger move. */
@Composable
private fun ScrubTime(fraction: () -> Float, duration: Double, style: TextStyle) {
    val seconds by remember(duration) { derivedStateOf { (fraction() * duration).toLong() } }
    Text(clock(seconds.toDouble()), style = style)
}

/** The elapsed time; while scrubbing, the scrub position's (by the second, not per finger move). */
@Composable
private fun ElapsedTime(elapsed: Double, scrubbing: Boolean, fraction: () -> Float, duration: Double, style: TextStyle, modifier: Modifier = Modifier) {
    if (!scrubbing) {
        Text(clock(elapsed), style = style, maxLines = 1, modifier = modifier)
        return
    }
    val seconds by remember(duration) { derivedStateOf { (fraction() * duration).toLong() } }
    Text(clock(seconds.toDouble()), style = style, maxLines = 1, modifier = modifier)
}

private fun toggle(s: PlayerState, c: PlayerControls) {
    when {
        s.ended -> {
            c.seek(0.0)
            c.play()
        }
        s.playing -> c.pause()
        else -> c.play()
    }
}

private fun seekBy(s: PlayerState, c: PlayerControls, direction: Int) {
    val now = s.positionAt()
    val to = now + direction * PlayerTokens.SKIP_S
    c.seek(if (s.duration > 0) to.coerceIn(0.0, s.duration) else to.coerceAtLeast(0.0))
}

/** Places a child so its horizontal centre sits at [x] from the start. */
private fun Modifier.centredAt(x: Dp): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val p = measurable.measure(constraints.copy(minWidth = 0))
        layout(p.width, p.height) { p.place(IntOffset(x.roundToPx() - p.width / 2, 0)) }
    },
)

@Composable
private fun TitlePill(mode: PlayerMode, title: String, subtitle: String, onLeading: (() -> Unit)?, modifier: Modifier) {
    Row(
        modifier
            .height(PlayerTokens.PillHeight)
            .playerPanel(RoundedCornerShape(22.dp))
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onLeading != null) {
            PlayerDisc(PlayerTokens.PillHeight, onClick = onLeading, panel = false) { PlayerIcon(KoanIcons.Close, 22.dp) }
        }
        Column(Modifier.padding(start = if (onLeading != null) 2.dp else 16.dp)) {
            Text(
                if (mode == PlayerMode.File) middleEllipsis(title, 36) else title,
                style = playerText(15.sp, FontWeight.SemiBold, line = 18.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = playerText(12.sp, FontWeight.Normal, PlayerTokens.TextSecondary, line = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** "a-very-long-name….mp4": a file name keeps its end. */
internal fun middleEllipsis(name: String, max: Int): String {
    if (name.length <= max) return name
    val keepEnd = (max / 3).coerceAtLeast(6)
    return name.take(max - keepEnd - 1) + "…" + name.takeLast(keepEnd)
}

@Composable
private fun SpeedPill(rate: Double, onClick: () -> Unit) {
    Box(
        Modifier
            .height(PlayerTokens.RoundPill)
            .pressScale(LocalControlsLive.current, onClick = onClick)
            .playerPanel(RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("${speedNumber(rate)}×", style = playerText(14.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun UnlockPill(onClick: () -> Unit) {
    Row(
        Modifier
            .height(PlayerTokens.RoundPill)
            .pressScale(onClick = onClick)
            .playerPanel(RoundedCornerShape(22.dp))
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIcon(KoanIcons.Unlock, PlayerTokens.Glyph)
        Spacer(Modifier.width(8.dp))
        Text("Unlock", style = playerText(15.sp, FontWeight.SemiBold))
    }
}

@Composable
private fun CentreGroup(state: PlayerState, landscape: Boolean, onBack10: () -> Unit, onAhead10: () -> Unit, onPlay: () -> Unit, modifier: Modifier) {
    if (state.ended) {
        Row(
            modifier
                .height(PlayerTokens.ReplayHeight)
                .pressScale(LocalControlsLive.current, onClick = onPlay)
                .playerPanel(RoundedCornerShape(28.dp))
                .padding(start = 18.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIcon(KoanIcons.ReplayFilled, 24.dp)
            Spacer(Modifier.width(10.dp))
            Text("Replay", style = playerText(16.sp, FontWeight.SemiBold))
        }
        return
    }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(if (landscape) PlayerTokens.CentreGapLandscape else PlayerTokens.CentreGapPortrait),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerDisc(PlayerTokens.SideDisc, onClick = onBack10) { SkipGlyph(false, PlayerTokens.SideGlyph) }
        PlayerDisc(PlayerTokens.PlayDisc, onClick = onPlay) { PlayGlyph(glyphFor(state, false), PlayerTokens.PlayGlyph, PlayerTokens.PlayOptical) }
        PlayerDisc(PlayerTokens.SideDisc, onClick = onAhead10) { SkipGlyph(true, PlayerTokens.SideGlyph) }
    }
}

@Composable
private fun BottomPill(
    state: PlayerState,
    landscape: Boolean,
    mode: PlayerMode,
    frameNow: () -> Long,
    clockNow: Long,
    scrubbing: Boolean,
    scrubFraction: () -> Float,
    onScrub: (Float?, Float) -> Unit,
    onSeek: (Double) -> Unit,
    onPlay: () -> Unit,
    onMute: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier,
) {
    val live = LocalControlsLive.current
    val shape = RoundedCornerShape(PlayerTokens.BottomPillRadius)
    val elapsed = state.positionAt(clockNow)
    val fullscreenIcon = if (mode == PlayerMode.Web) KoanIcons.ExitFullscreen else KoanIcons.Fullscreen
    val muteIcon = if (state.muted) KoanIcons.VolumeOff else KoanIcons.Volume
    val timeStyle = playerText(14.sp, FontWeight.Medium, tracking = 0.2.sp)
    val bar: @Composable (Modifier, Dp) -> Unit = { m, touch ->
        ProgressBar(state, frameNow, scrubbing, scrubFraction, onScrub, onSeek, live, m.height(touch))
    }
    // The pill's empty parts take touches (they never reach the video under it).
    val swallow = Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
    if (landscape) {
        Row(
            modifier
                .fillMaxWidth()
                .height(PlayerTokens.BottomPillHeightLandscape)
                .playerPanel(shape)
                .then(swallow)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerDisc(PlayerTokens.RoundPill, onClick = onMute, panel = false) { PlayerIcon(muteIcon, PlayerTokens.Glyph) }
            ElapsedTime(elapsed, scrubbing, scrubFraction, state.duration, timeStyle, Modifier.widthIn(min = PlayerTokens.TimeMinWidth))
            bar(Modifier.weight(1f), 44.dp)
            Text(clock(state.duration), style = timeStyle.copy(color = PlayerTokens.TextSecondary), textAlign = TextAlign.End, modifier = Modifier.widthIn(min = PlayerTokens.TimeMinWidth))
            PlayerDisc(PlayerTokens.RoundPill, onClick = onFullscreen, panel = false) { PlayerIcon(fullscreenIcon, PlayerTokens.Glyph) }
        }
    } else {
        Column(
            modifier
                .fillMaxWidth()
                .height(PlayerTokens.BottomPillHeightPortrait)
                .playerPanel(shape)
                .then(swallow)
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
        ) {
            bar(Modifier.fillMaxWidth().padding(horizontal = 6.dp), 24.dp)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PlayerDisc(PlayerTokens.RoundPill, onClick = onPlay, panel = false) {
                    PlayGlyph(glyphFor(state, scrubbing), if (state.ended) 22.dp else 24.dp, PlayerTokens.PlayOpticalSmall)
                }
                Row(Modifier.weight(1f).padding(start = 6.dp)) {
                    ElapsedTime(elapsed, scrubbing, scrubFraction, state.duration, timeStyle)
                    Text(" / ${clock(state.duration)}", style = timeStyle.copy(color = PlayerTokens.TextSecondary), maxLines = 1)
                }
                PlayerDisc(PlayerTokens.RoundPill, onClick = onMute, panel = false) { PlayerIcon(muteIcon, PlayerTokens.Glyph) }
                PlayerDisc(PlayerTokens.RoundPill, onClick = onFullscreen, panel = false) { PlayerIcon(fullscreenIcon, PlayerTokens.Glyph) }
            }
        }
    }
}

/** §2 progress bar: no thumb, grows while pressed; a tap jumps, a drag seeks live (80 ms throttle). */
@Composable
private fun ProgressBar(
    state: PlayerState,
    frameNow: () -> Long,
    scrubbing: Boolean,
    scrubFraction: () -> Float,
    onScrub: (Float?, Float) -> Unit,
    onSeek: (Double) -> Unit,
    live: Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val thick by animateDpAsState(if (scrubbing) PlayerTokens.TrackThicknessPressed else PlayerTokens.TrackThickness, androidx.compose.animation.core.spring(0.8f, 600f), label = "track")
    val s = rememberUpdatedState(state)
    val sc = rememberUpdatedState(scrubbing)
    val scf = rememberUpdatedState(scrubFraction)
    var rootX by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootX = it.positionInRoot().x }
            .progressTrack(
                thickness = { with(density) { thick.toPx() } },
                fraction = {
                    if (sc.value) scf.value() else s.value.let { st -> if (st.duration > 0) (st.positionAt(frameNow()) / st.duration).toFloat() else 0f }
                },
                buffered = { s.value.let { st -> if (st.duration > 0) (st.buffered / st.duration).toFloat() else 0f } },
            )
            .then(
                if (!live) Modifier else Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        fun frac(x: Float) = (x / w).coerceIn(0f, 1f)
                        var f = frac(down.position.x)
                        onScrub(f, rootX + down.position.x)
                        var lastSent = 0L
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Main)
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) {
                                c.consume()
                                break
                            }
                            if (c.positionChange() != Offset.Zero) {
                                f = frac(c.position.x)
                                onScrub(f, rootX + c.position.x)
                                val now = SystemClock.uptimeMillis()
                                if (now - lastSent >= PlayerTokens.SEEK_THROTTLE_MS && s.value.duration > 0) {
                                    lastSent = now
                                    onSeek(f * s.value.duration)
                                }
                            }
                            c.consume()
                        }
                        if (s.value.duration > 0) onSeek(f * s.value.duration)
                        onScrub(null, 0f)
                    }
                },
            ),
    )
}

@Composable
private fun SeekLabel(forward: Boolean, steps: Int, turn: () -> Float, modifier: Modifier) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(36.dp)
    Row(
        modifier
            .drawBehindHalo(density)
            .height(PlayerTokens.SeekLabelHeight)
            .playerPanel(shape)
            .padding(start = 20.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkipGlyph(forward, 36.dp, Modifier.graphicsLayer { rotationZ = turn() })
        Spacer(Modifier.width(12.dp))
        val seconds = steps * PlayerTokens.SKIP_S.toInt()
        Text(if (forward) "+$seconds s" else "−$seconds s", style = playerText(PlayerTokens.SeekLabelText, FontWeight.Bold))
    }
}

/** §1 player.seekLabel.halo: a soft white glow around the label. */
private fun Modifier.drawBehindHalo(density: androidx.compose.ui.unit.Density): Modifier = this.then(
    Modifier.drawBehindCompat { size ->
        val spread = with(density) { 20.dp.toPx() }
        val blur = with(density) { 28.dp.toPx() }
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb((0.16f * 255).toInt(), 255, 255, 255)
            maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        val r = size.height / 2 + spread
        drawContext.canvas.nativeCanvas.drawRoundRect(-spread, -spread, size.width + spread, size.height + spread, r, r, paint)
    },
)

private fun Modifier.drawBehindCompat(block: androidx.compose.ui.graphics.drawscope.DrawScope.(Size) -> Unit): Modifier =
    this.drawBehind { block(size) }

