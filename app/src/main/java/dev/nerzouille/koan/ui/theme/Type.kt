package dev.nerzouille.koan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Line height and letter spacing per text style (docs/DESIGN_SYSTEM.md "Typography").
 *
 * Every Text without its own style gets [Body]'s rhythm: the line height is relative to the font
 * size (em), so a 34sp title that wraps never overlaps itself the way Material's fixed 24sp line
 * height did. Titles use [KoanType]: tighter lines and slightly negative tracking, scaling with size.
 */

/** Text centred in its line, no trimming: a wrapped line and a single line sit the same way. */
private val Centred = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private val Body = TextStyle(
    fontSize = 16.sp,
    lineHeight = TypeTokens.BODY_LINE.em,
    letterSpacing = 0.sp,
    lineHeightStyle = Centred,
)

/** Material's default text style (bodyLarge, what [androidx.compose.material3.Text] uses) and its siblings. */
val KoanTypography = Typography().run {
    copy(
        bodyLarge = Body,
        bodyMedium = Body.copy(fontSize = 14.sp),
        bodySmall = Body.copy(fontSize = 12.sp),
        labelLarge = Body.copy(fontSize = 14.sp),
        labelMedium = Body.copy(fontSize = 12.sp),
        labelSmall = Body.copy(fontSize = 11.sp),
    )
}

object TypeTokens {
    /** Body and captions (≤ 16sp): 16sp → 21.3sp lines. */
    const val BODY_LINE = 1.33f
    /** Headlines (17–20sp: card titles, sheet headers). */
    const val HEADLINE_LINE = 1.25f
    const val HEADLINE_TRACKING = -0.006f
    /** Titles (22–30sp: page bar, tour, sign-in). */
    const val TITLE_LINE = 1.18f
    const val TITLE_TRACKING = -0.012f
    /** Large titles (34sp, settings pages). */
    const val LARGE_TITLE_LINE = 1.12f
    const val LARGE_TITLE_TRACKING = -0.016f
}

/** Title styles. Sizes come from each feature's tokens; rhythm comes from here. */
object KoanType {
    fun headline(size: TextUnit, weight: FontWeight = FontWeight.SemiBold) = style(size, weight, TypeTokens.HEADLINE_LINE, TypeTokens.HEADLINE_TRACKING)
    fun title(size: TextUnit, weight: FontWeight = FontWeight.Bold) = style(size, weight, TypeTokens.TITLE_LINE, TypeTokens.TITLE_TRACKING)
    fun largeTitle(size: TextUnit) = style(size, FontWeight.Bold, TypeTokens.LARGE_TITLE_LINE, TypeTokens.LARGE_TITLE_TRACKING)

    private fun style(size: TextUnit, weight: FontWeight, line: Float, tracking: Float) = TextStyle(
        fontSize = size,
        fontWeight = weight,
        lineHeight = line.em,
        letterSpacing = tracking.em,
        lineHeightStyle = Centred,
    )
}
