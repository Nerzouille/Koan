package dev.nerzouille.koan.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.pressScale
import dev.nerzouille.koan.ui.kit.rememberPresence
import dev.nerzouille.koan.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Every number in the player: tabular figures. */
internal fun playerText(size: TextUnit, weight: FontWeight, color: Color = PlayerTokens.Text, line: TextUnit = TextUnit.Unspecified, tracking: TextUnit = TextUnit.Unspecified) =
    TextStyle(color = color, fontSize = size, fontWeight = weight, lineHeight = line, letterSpacing = tracking, fontFeatureSettings = "tnum")

/**
 * The player's panel (pills and discs). Koan's video is a Gecko or a texture surface the UI can't
 * sample, so there is no live backdrop blur: the no-blur panel (§1 `player.panel.noBlur`) always.
 */
internal fun Modifier.playerPanel(shape: Shape, color: Color = PlayerTokens.PanelNoBlur): Modifier =
    this.clip(shape).background(color, shape).border(1.dp, PlayerTokens.Stroke, shape)

/** A press that scales the button down (§5 disc press) and acts on release inside it. */
internal fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = this.then(
    if (!enabled) Modifier else Modifier.pointerInput(onClick) {
        awaitEachGesture {
            awaitFirstDown().consume()
            val up = waitForUpOrCancellation()
            if (up != null) {
                up.consume()
                onClick()
            }
        }
    },
)

/** A round 44dp button (or a disc of [size]) on the panel. */
@Composable
internal fun PlayerDisc(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    panel: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .pressScale(LocalControlsLive.current, onClick = onClick)
            .then(if (panel) Modifier.playerPanel(CircleShape) else Modifier.clip(CircleShape)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun PlayerIcon(icon: ImageVector, size: Dp, modifier: Modifier = Modifier, tint: Color = PlayerTokens.Text) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/**
 * replay-10 / forward-10: the ring (r 8 around the glyph's centre) and a bold "10" centred on the
 * ring's centre. Sizes in dp, not sp: the label never grows with the system font size.
 */
@Composable
internal fun SkipGlyph(forward: Boolean, size: Dp, modifier: Modifier = Modifier, label: String = "10") {
    val density = LocalDensity.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        PlayerIcon(if (forward) KoanIcons.SkipAheadFilled else KoanIcons.SkipBackFilled, size)
        val unit = size.value / 24f
        val fontSize = with(density) { (7.5f * unit).dp.toSp() }
        Text(
            label,
            style = playerText(fontSize, FontWeight.ExtraBold, line = fontSize).copy(
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = SKIP_LABEL_NUDGE * unit),
        )
    }
}

/** Digits sit above the middle of their line box: this lowers them onto the ring's centre (measured). */
private val SKIP_LABEL_NUDGE = 0.dp

/** Play / pause / replay, crossfading with a small pop (§5 playPause). */
@Composable
internal fun PlayGlyph(state: GlyphState, size: Dp, optical: Dp = 0.dp) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(state) {
        pop.snapTo(0.8f)
        pop.animateTo(1f, PlayerTokens.PlayPop)
    }
    Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
        for (g in GlyphState.entries) {
            val a = rememberPresence(g == state, Motion.FadeQuick, startShown = true)
            if (a.composed) {
                val icon = when (g) {
                    GlyphState.Play -> KoanIcons.PlayFilled
                    GlyphState.Pause -> KoanIcons.PauseFilled
                    GlyphState.Replay -> KoanIcons.ReplayFilled
                }
                PlayerIcon(
                    icon,
                    if (g == GlyphState.Replay) size * 0.92f else size,
                    Modifier
                        .offset(x = if (g == GlyphState.Play) optical else 0.dp)
                        .graphicsLayer { alpha = a.progress },
                )
            }
        }
    }
}

enum class GlyphState { Play, Pause, Replay }

internal fun glyphFor(s: PlayerState, dragging: Boolean) = when {
    s.ended -> GlyphState.Replay
    dragging || s.playing -> GlyphState.Pause
    else -> GlyphState.Play
}

/** The buffering spinner (§1 `player.spinner`): 75 % arc, round caps, no gradient. */
@Composable
internal fun PlayerSpinner(size: Dp, stroke: Dp, color: Color = PlayerTokens.Text, modifier: Modifier = Modifier) {
    val turn = rememberInfiniteTransition(label = "spin")
    val angle by turn.animateFloat(0f, 360f, infiniteRepeatable(tween(PlayerTokens.SPINNER_TURN_MS, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    Canvas(modifier.size(size)) {
        val w = stroke.toPx()
        rotate(angle) {
            drawArc(color, 0f, 270f, false, topLeft = Offset(w / 2, w / 2), size = Size(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
    }
}

/** m:ss, or h:mm:ss from an hour. */
internal fun clock(seconds: Double): String {
    val s = seconds.coerceAtLeast(0.0).toLong()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** The ⋯ menu's rows (beside the speed row). */
class MenuRow(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    /** False: greyed out, a tap does nothing ([subtitle] says why). */
    val enabled: Boolean = true,
    val subtitle: String? = null,
    /** A switch row: "On" / "Off" at the end (null = an action). */
    val on: Boolean? = null,
    val onClick: () -> Unit,
) {
    /** The same row (state, greyed out, reason) doing [also] first, then its own action. */
    fun after(also: () -> Unit) = MenuRow(icon, label, destructive, enabled, subtitle, on) { also(); onClick() }
}

/**
 * The ⋯ row for Settings › Picture-in-picture (the same setting): leaving Koan in fullscreen opens
 * PiP. Its state at the end ("On" / "Off", like "Normal" on Playback speed), its icon crossed out when off.
 */
fun pipOnLeaveRow(on: Boolean, onChange: (Boolean) -> Unit) =
    MenuRow(if (on) dev.nerzouille.koan.ui.icons.KoanIcons.Pip else dev.nerzouille.koan.ui.icons.KoanIcons.PipOff, "PiP on leave", on = on) { onChange(!on) }

/** §2 ⋯ menu: the speed row and its chips, then [rows]. */
@Composable
internal fun PlayerMenu(rate: Double, onRate: (Double) -> Unit, rows: List<MenuRow>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(PlayerTokens.MenuRadius)
    Column(
        modifier
            .width(PlayerTokens.MenuWidth)
            .graphicsLayer {
                shadowElevation = PlayerTokens.MenuShadow.toPx() / 2
                this.shape = shape
                clip = false
            }
            .playerPanel(shape, PlayerTokens.PanelMenu)
            .padding(vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(PlayerTokens.MenuRow).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerIcon(KoanIcons.Speed, 18.dp)
            Spacer(Modifier.width(12.dp))
            Text("Playback speed", style = playerText(15.sp, FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(speedLabel(rate), style = playerText(15.sp, FontWeight.Medium, PlayerTokens.TextSecondary))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (s in PlayerTokens.Speeds) {
                val selected = kotlin.math.abs(s - rate) < 0.01
                Box(
                    Modifier
                        .weight(1f)
                        .height(PlayerTokens.ChipHeight)
                        .clip(RoundedCornerShape(PlayerTokens.ChipRadius))
                        .background(if (selected) Color.White else PlayerTokens.Chip)
                        .pressable { onRate(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        speedNumber(s),
                        style = playerText(13.sp, if (selected) FontWeight.Bold else FontWeight.Medium, if (selected) Color.Black else PlayerTokens.Text),
                        maxLines = 1,
                    )
                }
            }
        }
        for (row in rows) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(PlayerTokens.Stroke))
            val color = when {
                !row.enabled -> PlayerTokens.TextSecondary
                row.destructive -> PlayerTokens.Destructive
                else -> PlayerTokens.Text
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(PlayerTokens.MenuRow)
                    .then(if (row.enabled) Modifier.pressable(onClick = row.onClick) else Modifier.graphicsLayer { alpha = PlayerTokens.DISABLED_ALPHA })
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerIcon(row.icon, 18.dp, tint = color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.label, style = playerText(15.sp, FontWeight.Medium, color), maxLines = 1)
                    row.subtitle?.let { Text(it, style = playerText(12.sp, FontWeight.Normal, PlayerTokens.TextSecondary), maxLines = 1) }
                }
                row.on?.let { Text(if (it) "On" else "Off", style = playerText(15.sp, FontWeight.Medium, PlayerTokens.TextSecondary), maxLines = 1) }
            }
        }
    }
}

internal fun speedNumber(s: Double) = if (s == s.toLong().toDouble()) "${s.toLong()}" else s.toString().trimEnd('0')
internal fun speedLabel(s: Double) = if (kotlin.math.abs(s - 1.0) < 0.01) "Normal" else "${speedNumber(s)}×"

/**
 * §2 level indicator: brightness (sun) or volume (speaker), the value on top, the track in the
 * middle. [level] is the animated value (0…1), read only while drawing.
 */
@Composable
internal fun LevelIndicator(volume: Boolean, level: () -> Float, height: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(PlayerTokens.LevelRadius)
    // Fixed parts first; the track takes what is left, so the glyph always sits inside the pill.
    Column(
        modifier
            .size(PlayerTokens.LevelSize.width, height)
            .playerPanel(shape)
            .padding(top = PlayerTokens.LevelPadTop, bottom = PlayerTokens.LevelPadBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(PlayerTokens.LevelNumberLine), contentAlignment = Alignment.Center) { LevelNumber(level) }
        Spacer(Modifier.height(PlayerTokens.LevelInnerGap))
        Canvas(Modifier.width(PlayerTokens.LevelTrack.width).weight(1f)) {
            val r = CornerRadius(6.dp.toPx())
            drawRoundRect(PlayerTokens.Track, cornerRadius = r)
            val f = level().coerceIn(0f, 1f)
            val h = size.height * f
            if (h > 0f) drawRoundRect(Color.White, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), cornerRadius = r)
        }
        Spacer(Modifier.height(PlayerTokens.LevelInnerGap))
        Canvas(Modifier.size(PlayerTokens.LevelGlyph)) {
            val f = level().coerceIn(0f, 1f)
            val u = size.width / 24f
            val stroke = Stroke(2f * u, cap = StrokeCap.Round)
            if (!volume) {
                // Sun: core r = 3 + 0.6 f; 8 rays from 5.6 to 7 + 4.5 f; turns 45° × f.
                val c = Offset(12f * u, 12f * u)
                drawCircle(Color.White, (3f + 0.6f * f) * u, c, style = stroke)
                for (i in 0 until 8) {
                    val a = (i * 45f + 45f * f) * PI.toFloat() / 180f
                    val r1 = 5.6f * u
                    val r2 = (7f + 3.5f * f) * u
                    drawLine(Color.White, c + Offset(cos(a) * r1, sin(a) * r1), c + Offset(cos(a) * r2, sin(a) * r2), 2f * u, StrokeCap.Round)
                }
            } else {
                val body = androidx.compose.ui.graphics.Path().apply {
                    moveTo(3f * u, 9.5f * u); lineTo(6.5f * u, 9.5f * u); lineTo(11f * u, 5.5f * u)
                    lineTo(11f * u, 18.5f * u); lineTo(6.5f * u, 14.5f * u); lineTo(3f * u, 14.5f * u); close()
                }
                drawPath(body, Color.White, style = Stroke(2f * u, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val waves = when {
                    f <= 0.001f -> 0
                    f <= 0.33f -> 1
                    f <= 0.66f -> 2
                    else -> 3
                }
                if (waves == 0) {
                    drawLine(Color.White, Offset(15f * u, 9.5f * u), Offset(20f * u, 14.5f * u), 2f * u, StrokeCap.Round)
                    drawLine(Color.White, Offset(20f * u, 9.5f * u), Offset(15f * u, 14.5f * u), 2f * u, StrokeCap.Round)
                }
                for (w in 1..waves) {
                    val rr = (2.5f + 3f * (w - 1)) * u
                    drawArc(Color.White, -50f, 100f, false, topLeft = Offset(11.5f * u - rr, 12f * u - rr), size = Size(2 * rr, 2 * rr), style = stroke)
                }
            }
        }
    }
}

@Composable
private fun LevelNumber(level: () -> Float) {
    // The number shows the animated value, rounded: recomposes only when it changes.
    val n by remember { androidx.compose.runtime.derivedStateOf { (level().coerceIn(0f, 1f) * 100).toInt() } }
    Text("$n", style = playerText(16.sp, FontWeight.Bold, line = 20.sp))
}

/** The track (and buffered / played ranges), drawn from [fraction] / [buffered] at draw time. */
internal fun Modifier.progressTrack(thickness: () -> Float, fraction: () -> Float, buffered: () -> Float): Modifier = drawBehind {
    val h = thickness()
    val top = (size.height - h) / 2
    val r = CornerRadius(h / 2)
    drawRoundRect(PlayerTokens.Track, Offset(0f, top), Size(size.width, h), r)
    val b = buffered().coerceIn(0f, 1f) * size.width
    if (b > 0f) drawRoundRect(PlayerTokens.TrackBuffered, Offset(0f, top), Size(b, h), r)
    val p = fraction().coerceIn(0f, 1f) * size.width
    if (p > 0f) drawRoundRect(PlayerTokens.TrackPlayed, Offset(0f, top), Size(maxOf(p, h), h), r)
}
