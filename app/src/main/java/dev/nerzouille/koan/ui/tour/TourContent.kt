package dev.nerzouille.koan.ui.tour

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.vector.ImageVector
import dev.nerzouille.koan.R
import dev.nerzouille.koan.ui.icons.KoanIcons
import dev.nerzouille.koan.ui.settings.REPO
import dev.nerzouille.koan.ui.theme.TourTokens

/**
 * One card of the tour or of a "What's new": a title, one line, and a looping clip recorded in
 * Koan ([dark] / [light] so it matches the theme), or a still screenshot ([stillDark] /
 * [stillLight], a screen words describe well enough), or a [scene] drawn in code, framed per
 * [framing]; none = text only, then [icon] above the title. [welcome]: the tour's first card instead (the K on the brand
 * gradient, Welcome.kt).
 */
class TourCard(
    val title: String,
    val line: String,
    @param:RawRes val dark: Int? = null,
    @param:RawRes val light: Int? = null,
    val framing: Framing = Framing.Focus,
    val welcome: Boolean = false,
    val icon: ImageVector? = null,
    @param:DrawableRes val stillDark: Int? = null,
    @param:DrawableRes val stillLight: Int? = null,
    val scene: TourScene? = null,
)

/** Looping illustrations drawn in code, where a recording would show a real account. */
enum class TourScene { SignIn }

/**
 * How a clip is framed, and its shape (width / height; null = the frame's whole slot): the whole
 * screen (a gesture that changes all of it), the whole screen held sideways, or a crop on where the
 * gesture happens, shaped to fill the slot.
 */
enum class Framing(val aspect: Float?) {
    Screen(TourTokens.SCREEN_CLIP_ASPECT),
    Landscape(TourTokens.LANDSCAPE_CLIP_ASPECT),
    Focus(null),
}

/** What opens on a normal launch: the whole tour (first launch), or a version's highlights. */
sealed interface TourSheet {
    val cards: List<TourCard>

    data object Tour : TourSheet {
        override val cards get() = TourCards
    }

    /** Opens on [whatsNewIntro] (the welcome card's look), then the version's highlights. */
    data class WhatsNew(val version: String) : TourSheet {
        override val cards get() = listOf(whatsNewIntro(version)) + Highlights.getValue(version)
    }
}

/** The first-run tour (also replayed from About). */
val TourCards: List<TourCard> = listOf(
    TourCard("Welcome to Koan", "Your Zen Spaces, in your pocket.", welcome = true),
    TourCard(
        "Your Zen Spaces, on your phone",
        "Synced from your Mozilla account, read-only: Zen on your computer stays as it is.",
        R.raw.tour_spaces_dark, R.raw.tour_spaces_light,
    ),
    TourCard(
        "Swipe up for your tabs",
        "Swipe up on the bar: every tab of the space, in one stack.",
        R.raw.tour_stack_dark, R.raw.tour_stack_light,
        Framing.Screen,
    ),
    TourCard(
        "Swipe sideways to switch tabs",
        "Swipe left or right on the bar for the previous or next tab.",
        R.raw.tour_tabs_dark, R.raw.tour_tabs_light,
    ),
    TourCard(
        "Hold and slide to change space",
        "Hold the bar, then slide left or right through your spaces.",
        R.raw.tour_scrub_dark, R.raw.tour_scrub_light,
    ),
    TourCard(
        "Search shortcuts",
        "Start with @yt, @wiki and more to search one site straight away.",
        R.raw.tour_search_dark, R.raw.tour_search_light,
        Framing.Screen,
    ),
    TourCard(
        "Long presses",
        "Hold ← for earlier pages, the tab counter for Reopen closed tab, ⋯ for Settings.",
        R.raw.tour_holds_dark, R.raw.tour_holds_light,
    ),
)

/**
 * "What's new" per version ("0.4"): 3 to 5 highlights, one line on how to use each, a clip only for
 * a gesture or a flow words don't carry. A version without an entry (fix-only) shows nothing. 0.3
 * has none: it brought the tour itself.
 */
val Highlights: Map<String, List<TourCard>> = mapOf(
    "0.4" to listOf(
        TourCard("Sign in from your computer", "On the sign-in screen, scan the code Zen shows: no password to type.", scene = TourScene.SignIn),
        TourCard(
            "Search in Koan from any app",
            "Select text in any app, then pick Search in Koan in the menu.",
            R.raw.whatsnew_search_dark, R.raw.whatsnew_search_light,
            Framing.Screen,
        ),
        TourCard(
            "Reopen a closed tab",
            "Hold the tab counter, then Reopen closed tab.",
            R.raw.whatsnew_reopen_dark, R.raw.whatsnew_reopen_light,
            Framing.Screen,
        ),
        TourCard(
            "Your own search engines",
            "Add your own search engines and @shortcuts in Settings › Search engine, or add a site's engine from ⋯ › Add this search engine.",
            framing = Framing.Screen,
            stillDark = R.drawable.whatsnew_engines_dark,
            stillLight = R.drawable.whatsnew_engines_light,
        ),
    ),
    // Every card with media in the same Screen frame (the player in a Landscape one: it plays
    // sideways), taken with demo pages and public videos; the sync card stays text-only: its screen
    // shows the account's email.
    "1.0" to listOf(
        TourCard(
            "Private browsing",
            "Tap the switch at the right of the overview's header: private tabs in their own set, locked behind your fingerprint.",
            framing = Framing.Screen,
            stillDark = R.drawable.whatsnew_private_dark,
            stillLight = R.drawable.whatsnew_private_light,
        ),
        TourCard(
            "Koan's own video player",
            "Play a video full screen: double-tap to skip, hold for 2×, swipe for brightness and volume. Go home and it keeps playing in a small window.",
            // The player is always dark: one clip for both themes.
            R.raw.whatsnew_player, R.raw.whatsnew_player,
            Framing.Landscape,
        ),
        TourCard(
            "Translate a page",
            "Tap the translate icon in the address bar, or ⋯ › Translate page: pages are translated on the phone, their text never leaves it.",
            R.raw.whatsnew_translate_dark, R.raw.whatsnew_translate_light,
            Framing.Screen,
        ),
        TourCard(
            "Open in app",
            "On a site that has its own app, ⋯ › Open in [app] opens the page there.",
            R.raw.whatsnew_openapp_dark, R.raw.whatsnew_openapp_light,
            Framing.Screen,
        ),
        TourCard(
            "Two-way sync, if you want it",
            "Settings › Mozilla account › Send to your Mozilla account: passwords saved here, and history both ways. Off until you turn it on.",
            icon = KoanIcons.Key,
        ),
    ),
)

/**
 * The GitHub release page of [versionName] ("1.0.0-beta.1"): the last highlight links to its full
 * changelog. Not `/releases/latest`, which skips pre-releases (a 404 while every release is one).
 */
fun releaseUrl(versionName: String): String = "$REPO/releases/tag/v$versionName"

/** The first card of a "What's new": the welcome card's gradient and K, with the version. */
fun whatsNewIntro(version: String): TourCard =
    TourCard("What's new in $version", "A few new things since last time ✨", welcome = true)

/** "0.3.1" → "0.3": the tour and highlights go by feature version. */
fun featureVersion(versionName: String): String = versionName.split('.').take(2).joinToString(".")

/**
 * What to show on a normal launch, given the feature version whose sheet was last seen ([seen],
 * null = none yet: a new install, or an update from before the tour) and the app's [current] one.
 */
fun dueSheet(seen: String?, current: String, highlights: Map<String, List<TourCard>> = Highlights): TourSheet? {
    if (seen == null) return TourSheet.Tour
    // The newest highlights after the last seen sheet, up to this version.
    val version = highlights.keys
        .filter { compareVersions(it, seen) > 0 && compareVersions(it, current) <= 0 }
        .maxWithOrNull(::compareVersions)
        ?: return null
    return TourSheet.WhatsNew(version)
}

private fun compareVersions(a: String, b: String): Int {
    val x = a.split('.').map { it.toIntOrNull() ?: 0 }
    val y = b.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(x.size, y.size)) {
        val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
        if (c != 0) return c
    }
    return 0
}
