package dev.nerzouille.koan.ui.overview

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.StackTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The stack looks to compare on the phone (Settings › Debug › Stack style, dev builds only). */
enum class StackStyle(val label: String, val short: String) {
    /** The stack exactly as in beta.1: its dim, its key shadow and its contact shadow. */
    Before("Before", "Before"),

    /** Stronger shadows that land on the card behind and on the background around the pile. */
    Cast("Cast shadows", "Cast"),

    /** The shadows become a soft fog that grows with depth: back cards sink into it. */
    Haze("Haze", "Haze"),

    /** Back cards lean away more and more, like a carousel (perspective rotation). */
    Tilt("Tilt", "Tilt"),
}

/**
 * Dev builds only, never a release build: four stack looks to switch between live on the owner's
 * phone, with a strength (each look its own) and, for [StackStyle.Tilt], an angle. Every other
 * build draws the shipping look (one ambient light) and [active] is null there. In every look the
 * cards themselves are beta.1's (same dim, same header grey); only the depth cue changes.
 * Kept in the app's own preferences, so a look survives a restart.
 */
object StackLab {
    val available: Boolean = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "dev"

    var style by mutableStateOf(StackStyle.Before)
        private set
    private val strengths = mutableStateMapOf<StackStyle, Float>()
    var angle by mutableStateOf(LabTokens.TILT_ANGLE)
        private set

    /** The tuning panel over the overview. */
    var panel by mutableStateOf(false)
        private set

    /** The look the stack draws; null = the shipping look. Read in composition or in layers. */
    val active: StackStyle? get() = if (available) style else null

    /** The style in use's strength, 1 = as designed. */
    val strength: Float get() = strengths[style] ?: 1f

    private var prefs: android.content.SharedPreferences? = null

    fun load(context: Context) {
        if (!available || prefs != null) return
        val p = context.getSharedPreferences("stack_lab", Context.MODE_PRIVATE)
        prefs = p
        style = StackStyle.entries.firstOrNull { it.name == p.getString("style", null) } ?: StackStyle.Before
        StackStyle.entries.forEach { strengths[it] = p.getFloat("strength_${it.name}", 1f) }
        angle = p.getFloat("angle", LabTokens.TILT_ANGLE)
        panel = p.getBoolean("panel", false)
    }

    fun select(s: StackStyle) {
        style = s
        prefs?.edit()?.putString("style", s.name)?.apply()
    }

    fun changeStrength(v: Float) { strengths[style] = v }

    fun changeAngle(v: Float) { angle = v }

    fun showPanel(on: Boolean) {
        panel = on
        prefs?.edit()?.putBoolean("panel", on)?.apply()
    }

    /** A slider let go: keep its value. */
    fun save() {
        prefs?.edit()?.putFloat("strength_${style.name}", strength)?.putFloat("angle", angle)?.apply()
    }

    // ── The cards, as in beta.1 ──────────────────────────────────────────────────────────────

    /** Thumbnail dim at depth [d] (beta.1), or null for the shipping look. */
    fun cardDim(d: Float, palette: KoanPalette): Float? {
        if (active == null) return null
        return (beta1Dim(d) * gain(palette)).coerceAtMost(max(palette))
    }

    /** Header dim at depth [d] (beta.1: half the thumbnail's, capped), or null for the shipping look. */
    fun headerDim(d: Float, palette: KoanPalette): Float? {
        if (active == null) return null
        val header = (beta1Dim(d) * LabTokens.HEADER_DIM_SHARE).coerceAtMost(LabTokens.HEADER_DIM_MAX)
        return (header * gain(palette)).coerceAtMost(max(palette))
    }

    /** The colour back cards dim towards (beta.1: black in dark). */
    fun dimColor(palette: KoanPalette): Color = when {
        active == null -> palette.DimColor
        palette === KoanPalette.Night -> LabTokens.NightDim
        palette.isDark -> Color.Black
        else -> palette.DimColor
    }


    // ── The card layer ───────────────────────────────────────────────────────────────────────

    /**
     * After StackGeometry.applyTo, for a card at depth [d] scaled [s] (top-centre origin),
     * [heightPx] its unscaled height: beta.1's key shadow (Before, Tilt) and Tilt's lean.
     */
    fun adjust(
        layer: GraphicsLayerScope,
        d: Float,
        lift: Float,
        s: Float,
        heightPx: Float,
        density: Float,
        palette: KoanPalette,
    ) = with(layer) {
        val style = active ?: return@with
        if (style == StackStyle.Before || style == StackStyle.Tilt) {
            shadowElevation = (palette.CardElevation.value + lift * StackTokens.LiftShadow.value) * density
            // Back cards' key shadow softens with depth (beta.1).
            val key = (1f - LabTokens.KEY_SHADOW_STEP * d.coerceAtLeast(0f)).coerceAtLeast(LabTokens.KEY_SHADOW_MIN)
            val k = if (style == StackStyle.Before) strength else 1f
            val c = palette.CardShadowColor.copy(alpha = (palette.CardShadowColor.alpha * key * k).coerceAtMost(1f))
            ambientShadowColor = c
            spotShadowColor = c
        }
        if (style == StackStyle.Tilt) lean(this, d, s, heightPx, density)
    }

    /**
     * Tilt: the card leans back about its bottom edge by [angle] per slot of depth, so its top
     * edge goes away (narrower). Strength = perspective (a closer camera). The top edge is put
     * back where the pile wants it, so each card still shows the same sliver.
     */
    private fun lean(layer: GraphicsLayerScope, d: Float, s: Float, heightPx: Float, density: Float) = with(layer) {
        val theta = angle * d.coerceIn(0f, LabTokens.TILT_MAX_DEPTH)
        if (theta < LabTokens.TILT_MIN_ANGLE) return@with
        // The layer's camera sits cameraDistance × 72 px away (not scaled by density): scale it
        // here so the perspective looks the same on every screen.
        val camera = LabTokens.TILT_CAMERA * density / strength.coerceAtLeast(LabTokens.MIN_STRENGTH)
        cameraDistance = camera
        val rad = theta * PI.toFloat() / 180f
        val h = heightPx * s
        val dPx = camera * LabTokens.CAMERA_UNIT_PX
        val k = dPx / (dPx + h * sin(rad))
        transformOrigin = BottomCentre
        // Top origin: the top edge was at translationY. Bottom origin: it is at
        // translationY + heightPx − h·cos·k; keep it where it was.
        translationY += h * cos(rad) * k - heightPx
        rotationX = LabTokens.TILT_SIGN * theta
    }
}

private fun beta1Dim(d: Float) = if (d >= 0f) DepthCurve.lerp(LabTokens.Dim, d) else StackTokens.PEEK_DIM * DepthCurve.drop(d)
private fun gain(p: KoanPalette) = if (p.isDark) 1f else LabTokens.LIGHT_DIM_GAIN
private fun max(p: KoanPalette) = if (p.isDark) 1f else LabTokens.LIGHT_DIM_MAX
private val BottomCentre = TransformOrigin(0.5f, 1f)

/** The stack looks' values (dev builds only, see [StackLab]). */
object LabTokens {
    // beta.1's cards.
    val Dim = floatArrayOf(0f, 0.16f, 0.28f, 0.38f, 0.48f, 0.58f, 0.66f)
    const val LIGHT_DIM_GAIN = 1.25f
    const val LIGHT_DIM_MAX = 0.85f
    const val HEADER_DIM_SHARE = 0.5f
    const val HEADER_DIM_MAX = 0.2f
    val NightDim = Color(0xFF050412)
    const val KEY_SHADOW_STEP = 0.1f
    const val KEY_SHADOW_MIN = 0.6f

    // beta.1's contact shadow (Before, Tilt).
    val ContactLight = Color(0x2E1C1C30)
    val ContactDark = Color(0xA6000000)
    val ContactLengthLight = 16.dp
    val ContactLengthDark = 18.dp
    const val CONTACT_MAX_FRACTION = 0.35f

    // Cast shadows: a longer, stronger contact shadow, and a soft shadow all around each card.
    val CastContactLight = Color(0x4D1C1C30)
    val CastContactDark = Color(0xD9000000)
    val CastContactLength = 30.dp
    const val CAST_CONTACT_MAX_FRACTION = 0.7f
    val CastInk = Color(0xFF1C1C30)
    val CastBlur = 30.dp
    val CastY = 10.dp
    val CastSpread = 2.dp
    const val CAST_ALPHA_LIGHT = 0.30f
    const val CAST_ALPHA_DARK = 0.80f

    // Haze: a fog around each card, over the card behind it, denser deeper in the pile.
    val HazeLight = Color.White
    val HazeBlur = 36.dp
    val HazeY = (-18).dp
    val HazeSpread = 10.dp

    /** How dense the fog is over a card at d = 0, 1, 2 … (held past the last). */
    val Haze = floatArrayOf(0f, 0.35f, 0.55f, 0.7f, 0.8f)

    // Tilt.
    const val TILT_ANGLE = 6f
    const val TILT_ANGLE_MAX = 20f
    const val TILT_MAX_DEPTH = 4f
    const val TILT_CAMERA = 8f
    const val CAMERA_UNIT_PX = 72f
    const val TILT_SIGN = 1f
    const val TILT_MIN_ANGLE = 0.01f

    const val MIN_STRENGTH = 0.25f
    const val MAX_STRENGTH = 2f
}
