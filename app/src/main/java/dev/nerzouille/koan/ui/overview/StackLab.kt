package dev.nerzouille.koan.ui.overview

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.StackTokens

/** The stack's looks: Cast (the default, owner 2026-11-26) and Before, kept to compare. */
enum class StackStyle(val label: String, val short: String) {
    /** The stack exactly as in beta.1: its dim, its key shadow and its contact shadow. */
    Before("Before", "Before"),

    /** Stronger shadows that land on the card behind and on the background around the pile. */
    Cast("Cast shadows", "Cast"),
}

/** A subtle 3D touch on top of Cast, each with its own switch and strength (dev panel). */
enum class Feel(val label: String) {
    /** A thin darker edge just above each card's top edge: the card's thickness, in the strips. */
    Thickness("Thickness"),

    /** Headers lit from above: a little lighter at the top, a little darker at the bottom. */
    Surface("Surface light"),
}

/**
 * The stack's look. Every build draws Cast at [LabTokens.CAST_STRENGTH]. Dev builds (never a
 * release build) can switch to Before and tune each look's strength and each [Feel] from
 * Settings › Debug › Stack style or the panel over the overview; that is kept in the app's own
 * preferences. In both looks the cards themselves are beta.1's (same dim, same header grey).
 * Read in composition, draw or layers only: changing a value never recomposes a frame of the stack.
 */
object StackLab {
    val available: Boolean = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "dev"

    var style by mutableStateOf(StackStyle.Cast)
        private set
    private val strengths = mutableStateMapOf(StackStyle.Before to 1f, StackStyle.Cast to LabTokens.CAST_STRENGTH)
    private val feels = mutableStateMapOf<Feel, Boolean>()
    private val feelStrengths = mutableStateMapOf<Feel, Float>()

    /** The tuning panel over the overview. */
    var panel by mutableStateOf(false)
        private set

    /** The look the stack draws. */
    val active: StackStyle get() = style

    /** The look in use's strength, 1 = as designed. */
    val strength: Float get() = strengths[style] ?: 1f

    private var prefs: android.content.SharedPreferences? = null

    fun load(context: Context) {
        if (!available || prefs != null) return
        val p = context.getSharedPreferences("stack_lab", Context.MODE_PRIVATE)
        prefs = p
        style = StackStyle.entries.firstOrNull { it.name == p.getString("style", null) } ?: StackStyle.Cast
        StackStyle.entries.forEach { strengths[it] = p.getFloat("strength_${it.name}", strengths[it] ?: 1f) }
        Feel.entries.forEach {
            feels[it] = p.getBoolean("feel_${it.name}", false)
            feelStrengths[it] = p.getFloat("feel_strength_${it.name}", 1f)
        }
        panel = p.getBoolean("panel", false)
    }

    fun select(s: StackStyle) {
        style = s
        prefs?.edit { putString("style", s.name) }
    }

    fun changeStrength(v: Float) { strengths[style] = v }

    fun showPanel(on: Boolean) {
        panel = on
        prefs?.edit { putBoolean("panel", on) }
    }

    /** How much of [f] the stack draws: 0 when off, or in Before; else its strength (1 = as designed). */
    fun feel(f: Feel): Float = if (style == StackStyle.Cast && feels[f] == true) feelStrengths[f] ?: 1f else 0f

    fun feelOn(f: Feel): Boolean = feels[f] == true

    fun feelStrength(f: Feel): Float = feelStrengths[f] ?: 1f

    /** Turn [f] on or off ([on]), or move its strength ([strength]). */
    fun tune(f: Feel, on: Boolean? = null, strength: Float? = null) {
        if (on != null) {
            feels[f] = on
            prefs?.edit { putBoolean("feel_${f.name}", on) }
        }
        if (strength != null) feelStrengths[f] = strength
    }

    /** A slider let go: keep its value. */
    fun save() {
        prefs?.edit {
            putFloat("strength_${style.name}", strength)
            Feel.entries.forEach { putFloat("feel_strength_${it.name}", feelStrength(it)) }
        }
    }

    /** After StackGeometry.applyTo, for a card at depth [d]: Before's key shadow (beta.1). */
    fun adjust(layer: GraphicsLayerScope, d: Float, lift: Float, density: Float, palette: KoanPalette) = with(layer) {
        if (style != StackStyle.Before) return@with
        shadowElevation = (palette.CardElevation.value + lift * StackTokens.LiftShadow.value) * density
        // Back cards' key shadow softens with depth (beta.1).
        val key = (1f - LabTokens.KEY_SHADOW_STEP * d.coerceAtLeast(0f)).coerceAtLeast(LabTokens.KEY_SHADOW_MIN)
        val c = palette.CardShadowColor.copy(alpha = (palette.CardShadowColor.alpha * key * strength).coerceAtMost(1f))
        ambientShadowColor = c
        spotShadowColor = c
    }
}

/** The stack looks' values (see [StackLab]). */
object LabTokens {
    // beta.1's key shadow (Before).
    const val KEY_SHADOW_STEP = 0.1f
    const val KEY_SHADOW_MIN = 0.6f

    // beta.1's contact shadow (Before).
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

    /** Cast's strength in every build (owner, 2026-11-26). */
    const val CAST_STRENGTH = 0.45f

    // Thickness: the edge of the card in front, a band this tall above its top edge.
    val Thickness = 1.5.dp
    val ThicknessLight = Color(0x2E1C1C30)
    val ThicknessDark = Color(0xB3000000)

    // Surface light on the headers: lighter at the top, darker at the bottom. These are at
    // strength 2; strength 1 draws half.
    val SurfaceTopLight = Color(0x3DFFFFFF)
    val SurfaceBottomLight = Color(0x1F1C1C30)
    val SurfaceTopDark = Color(0x1FFFFFFF)
    val SurfaceBottomDark = Color(0x52000000)
    const val SURFACE_FULL = 2f

    const val MIN_STRENGTH = 0.25f
    const val MAX_STRENGTH = 2f
}
