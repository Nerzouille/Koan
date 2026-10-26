package dev.nerzouille.koan.ui.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import dev.nerzouille.koan.DeviceProfile
import dev.nerzouille.koan.ui.tour.Sparkle
import dev.nerzouille.koan.ui.tour.animationsOff
import dev.nerzouille.koan.ui.tour.sparkles
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.nerzouille.koan.R
import dev.nerzouille.koan.sync.PairingFlow
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.kit.SecondaryButton
import dev.nerzouille.koan.ui.theme.BrandTokens
import dev.nerzouille.koan.ui.theme.KoanTheme
import dev.nerzouille.koan.ui.theme.SignInTokens

/** The first page: the K, the title, and the two ways in as cards in the thumb zone. */
@Composable
internal fun Choice(onComputer: () -> Unit, onPassword: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = SignInTokens.PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        FloatingK(KoanTheme.colors.isDark)
        Spacer(Modifier.height(SignInTokens.KGap))
        Text(
            "Sync your Zen Spaces",
            color = KoanTheme.colors.TextPrimary,
            fontSize = SignInTokens.HeroSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SignInTokens.HeroLineGap))
        Text(
            "Sign in with the Mozilla account you use in Zen.",
            color = KoanTheme.colors.TextSecondary,
            fontSize = SignInTokens.BodySize,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(0.4f))
        ChoiceCard(KoanIcons.Desktop, "From your computer", "Scan a code from Zen. No password.", recommended = true, onComputer)
        Spacer(Modifier.height(SignInTokens.ChoiceGap))
        ChoiceCard(KoanIcons.Key, "With email and password", "On Mozilla's sign-in page.", recommended = false, onPassword)
        Spacer(Modifier.height(SignInTokens.BottomGap))
    }
}

/**
 * The K, alive but quiet: the tour's welcome pieces toned down. It floats and tilts slowly among a
 * few tiny sparkles, over a softly breathing glow. Layers and draw only: nothing recomposes per
 * frame. "Remove animations": all of it still. Without live blur (Android 11): fewer sparkles.
 */
@Composable
private fun FloatingK(dark: Boolean) {
    val context = LocalContext.current
    val still = remember { animationsOff(context) }
    var clock by remember { mutableFloatStateOf(SignInTokens.STILL_CLOCK) }
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) clock += (now - last) / 1_000_000f
                last = now
            }
        }
    }
    val sparkles = remember { if (DeviceProfile.liveBlur) KSparkles else KSparkles.take(SignInTokens.SPARKLES_SIMPLE) }
    val sparkleColor = if (dark) Color.White else BrandTokens.Violet
    Box(Modifier.size(SignInTokens.KSize)) {
        Spacer(Modifier.matchParentSize().brandGlow(dark) { clock })
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer { translationY = kFloat(clock) }
                .sparkles(sparkles, sparkleColor, SignInTokens.SparkleUnit) { clock },
        )
        Image(
            painterResource(R.drawable.koan_k),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = kFloat(clock)
                    rotationZ = wave(clock, SignInTokens.K_TURN_MS) * SignInTokens.K_TURN
                },
        )
    }
}

/** Where the sparkles sit around the K (fractions of its box): few, small, slow. */
private val KSparkles = listOf(
    Sparkle(-0.14f, 0.18f, 0.8f, 3_400f, 0.0f),
    Sparkle(1.1f, 0.08f, 0.6f, 4_100f, 2.1f),
    Sparkle(1.04f, 0.92f, 0.7f, 3_700f, 4.0f),
    Sparkle(0.12f, 1.08f, 0.5f, 4_600f, 5.3f),
)

private fun wave(clock: Float, periodMs: Int): Float = sin(clock / periodMs * 2f * PI.toFloat())

private fun Density.kFloat(clock: Float): Float = wave(clock, SignInTokens.K_FLOAT_MS) * SignInTokens.KFloat.toPx()

/** A soft pink → violet → blue halo behind the K (the brand gradient, kept faint), breathing slowly. */
private fun Modifier.brandGlow(dark: Boolean, clock: () -> Float): Modifier = drawBehind {
    val breath = wave(clock(), SignInTokens.GLOW_BREATH_MS)
    val a = (if (dark) SignInTokens.GLOW_ALPHA_DARK else SignInTokens.GLOW_ALPHA_LIGHT) * (1f + breath * SignInTokens.GLOW_BREATH)
    val r = size.minDimension * SignInTokens.GLOW_RADIUS * (1f + breath * SignInTokens.GLOW_BREATH * 0.5f)
    val c = center
    val spots = listOf(
        BrandTokens.Pink to Offset(c.x - r * 0.35f, c.y + r * 0.1f),
        BrandTokens.Violet to Offset(c.x, c.y - r * 0.2f),
        BrandTokens.Blue to Offset(c.x + r * 0.35f, c.y + r * 0.15f),
    )
    for ((color, at) in spots) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), at, r), r, at)
    }
}

/** One way in: icon, title and one line, same size for both; [recommended] adds a badge on top. */
@Composable
private fun ChoiceCard(icon: ImageVector, title: String, subtitle: String, recommended: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(SignInTokens.ChoiceHeight)
                .clip(RoundedCornerShape(SignInTokens.ChoiceRadius))
                .background(KoanTheme.colors.Card)
                .border(SignInTokens.CardBorder, KoanTheme.colors.CardBorder, RoundedCornerShape(SignInTokens.ChoiceRadius))
                .clickable(onClick = onClick)
                .padding(horizontal = SignInTokens.ChoiceInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(SignInTokens.ChoiceIconBox).clip(CircleShape).background(KoanTheme.colors.Base),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = KoanTheme.colors.TextPrimary, modifier = Modifier.size(SignInTokens.ChoiceGlyph))
            }
            Spacer(Modifier.width(SignInTokens.ChoiceInset))
            Column(Modifier.weight(1f)) {
                Text(title, color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.ModeSize, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(SignInTokens.ChoiceLineGap))
                Text(subtitle, color = KoanTheme.colors.TextSecondary, fontSize = SignInTokens.SubtitleSize, maxLines = 1)
            }
            Spacer(Modifier.width(SignInTokens.ChoiceLineGap))
            Icon(KoanIcons.ChevronRight, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(SignInTokens.ModeGlyph))
        }
        if (recommended) {
            Text(
                "Recommended",
                color = KoanTheme.colors.OnPrimary,
                fontSize = SignInTokens.BadgeSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -SignInTokens.ChoiceInset, y = -SignInTokens.BadgeLift)
                    .clip(CircleShape)
                    .background(KoanTheme.colors.Primary)
                    .padding(horizontal = SignInTokens.BadgePaddingH, vertical = SignInTokens.BadgePaddingV),
            )
        }
    }
}

/**
 * Sign-in from a computer, all on one screen: the square viewfinder (or "Allow the camera" in its
 * place), three steps, then "Paste the link instead" and the switch to email and password.
 */
@Composable
internal fun FromComputer(
    camera: Boolean,
    enabled: Boolean,
    onCode: (String) -> Unit,
    onCameraError: () -> Unit,
    onAllowCamera: () -> Unit,
    onPaste: () -> Unit,
    onPassword: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = SignInTokens.PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth(SignInTokens.VIEWFINDER_WIDTH)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(SignInTokens.ScanRadius))
                .background(if (camera) Color.Black else KoanTheme.colors.Card),
            contentAlignment = Alignment.Center,
        ) {
            if (camera) {
                QrScanner(
                    accept = PairingFlow::isPairingLink,
                    onCode = { if (enabled && PairingFlow.isPairingLink(it)) onCode(it) },
                    onError = onCameraError,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(KoanIcons.Camera, null, tint = KoanTheme.colors.TextSecondary, modifier = Modifier.size(SignInTokens.ChoiceGlyph))
                    Spacer(Modifier.height(SignInTokens.HeroLineGap))
                    SecondaryButton("Allow the camera", onClick = { if (enabled) onAllowCamera() }, surface = KoanTheme.colors.Base)
                }
            }
            Box(Modifier.fillMaxSize().aimBrackets(if (camera) Color.White else KoanTheme.colors.TextSecondary))
        }
        Spacer(Modifier.height(SignInTokens.StepsGap))
        Column(Modifier.fillMaxWidth(SignInTokens.VIEWFINDER_WIDTH), verticalArrangement = Arrangement.spacedBy(SignInTokens.StepGap)) {
            Step(1, "In Zen, open Settings › Sync.")
            Step(2, "Click \"Connect another device\".")
            Step(3, "Scan the code.")
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton(
            "Paste the link instead",
            onClick = { if (enabled) onPaste() },
            modifier = Modifier.fillMaxWidth().border(SignInTokens.CardBorder, KoanTheme.colors.CardBorder, CircleShape),
            surface = KoanTheme.colors.Card,
        )
        SwitchLink("Use email and password instead", enabled, onPassword)
        Spacer(Modifier.height(SignInTokens.LinkBottom))
    }
}

/** Corner brackets showing where to aim, inset in the viewfinder. */
private fun Modifier.aimBrackets(color: Color): Modifier = drawWithContent {
    drawContent()
    val inset = SignInTokens.BracketInset.toPx()
    val len = SignInTokens.BracketLength.toPx()
    val radius = SignInTokens.BracketRadius.toPx()
    val stroke = Stroke(SignInTokens.BracketStroke.toPx(), cap = StrokeCap.Round)
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    // Each corner: one leg, a quarter circle, the other leg.
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        val path = Path().apply {
            moveTo(x, y + dy * len)
            lineTo(x, y + dy * radius)
            quadraticTo(x, y, x + dx * radius, y)
            lineTo(x + dx * len, y)
        }
        drawPath(path, color, style = stroke)
    }
    corner(l, t, 1f, 1f)
    corner(r, t, -1f, 1f)
    corner(l, b, 1f, -1f)
    corner(r, b, -1f, -1f)
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(SignInTokens.StepDot).clip(CircleShape).background(KoanTheme.colors.Card),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.StepNumberSize, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(SignInTokens.StepTextGap))
        Text(text, color = KoanTheme.colors.TextPrimary, fontSize = SignInTokens.BodySize)
    }
}

/** A Mozilla page, with the switch to the other way in under it (hidden while typing). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WebPage(switchText: String, showSwitch: Boolean, onSwitch: () -> Unit, page: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { page() }
        if (showSwitch && !WindowInsets.isImeVisible) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                SwitchLink(switchText, enabled = true, onSwitch)
                Spacer(Modifier.height(SignInTokens.LinkBottom))
            }
        }
    }
}

/** The small text button that switches to the other way in. */
@Composable
private fun ColumnScope.SwitchLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = KoanTheme.colors.TextSecondary,
        fontSize = SignInTokens.LinkSize,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = SignInTokens.LinkPaddingH, vertical = SignInTokens.LinkPaddingV),
    )
}
