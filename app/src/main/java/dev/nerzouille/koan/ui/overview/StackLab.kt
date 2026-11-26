package dev.nerzouille.koan.ui.overview

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.nerzouille.koan.BuildConfig
import dev.nerzouille.koan.ui.theme.KoanPalette
import dev.nerzouille.koan.ui.theme.StackTokens

/** The stack's looks: Cast (owner, 2026-11-26) and Before, kept in dev builds to compare. */
enum class StackStyle(val label: String, val short: String) {
    /** The stack exactly as in beta.1: its dim, its key shadow and its contact shadow. */
    Before("Before", "Before"),

    /**
     * Light from above: each card casts a soft shadow around it and onto the card behind, has a
     * thin darker edge (its thickness) and a header a little lighter at the top.
     */
    Cast("Cast shadows", "Cast"),
}

/**
 * The stack's look: Cast in every build. Dev and debug builds (never a release build) can switch
 * to Before in Settings › Debug › Stack style, kept in the app's own preferences. In both looks
 * the cards themselves are beta.1's (same dim, same header grey). [style] is read in composition
 * or in layers: switching never recomposes a frame of the stack.
 */
object StackLab {
    val available: Boolean = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "dev"

    var style by mutableStateOf(StackStyle.Cast)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun load(context: Context) {
        if (!available || prefs != null) return
        val p = context.getSharedPreferences("stack_lab", Context.MODE_PRIVATE)
        prefs = p
        style = StackStyle.entries.firstOrNull { it.name == p.getString("style", null) } ?: StackStyle.Cast
    }

    fun select(s: StackStyle) {
        style = s
        prefs?.edit { putString("style", s.name) }
    }

    /** After StackGeometry.applyTo, for a card at depth [d]: Before's key shadow (beta.1). */
    fun adjust(layer: GraphicsLayerScope, d: Float, lift: Float, density: Float, palette: KoanPalette) = with(layer) {
        if (style != StackStyle.Before) return@with
        shadowElevation = (palette.CardElevation.value + lift * StackTokens.LiftShadow.value) * density
        // Back cards' key shadow softens with depth (beta.1).
        val key = (1f - LabTokens.KEY_SHADOW_STEP * d.coerceAtLeast(0f)).coerceAtLeast(LabTokens.KEY_SHADOW_MIN)
        val c = palette.CardShadowColor.copy(alpha = palette.CardShadowColor.alpha * key)
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

    /** Cast's shadows are their design values × this (owner, 2026-11-26). */
    const val CAST_STRENGTH = 0.45f

    private val Ink = Color(0xFF1C1C30)

    // Cast: a soft shadow all around each card (0/10/30/+2), on the card behind and the background…
    val CastAroundLight = Ink.copy(alpha = 0.30f * CAST_STRENGTH)
    val CastAroundDark = Color.Black.copy(alpha = 0.80f * CAST_STRENGTH)
    val CastBlur = 30.dp
    val CastY = 10.dp
    val CastSpread = 2.dp

    // …and a contact shadow rising from its top edge onto the card behind.
    val CastContactLight = Ink.copy(alpha = 0.30f * CAST_STRENGTH)
    val CastContactDark = Color.Black.copy(alpha = 0.85f * CAST_STRENGTH)
    val CastContactLength = 30.dp
    const val CAST_CONTACT_MAX_FRACTION = 0.7f

    // Cast's thickness (tuned at strength 1.00): a band this tall, just above each card's top edge.
    val Thickness = 1.5.dp
    val ThicknessLight = Ink.copy(alpha = 0.18f)
    val ThicknessDark = Color.Black.copy(alpha = 0.70f)

    // Cast's surface light on the headers (tuned at strength 0.67): lighter at the top, darker at the bottom.
    val SurfaceTopLight = Color.White.copy(alpha = 0.08f)
    val SurfaceBottomLight = Ink.copy(alpha = 0.04f)
    val SurfaceTopDark = Color.White.copy(alpha = 0.04f)
    val SurfaceBottomDark = Color.Black.copy(alpha = 0.107f)
}
