package dev.nerzouille.koan.ui.player

import java.util.Locale

/*
 * The ⋯ menu's track choices (docs/plans/PLAYER_TRACKS.md › Approved scope): Captions on a page's
 * video, Audio and Subtitles in the standalone player. Each row shows only with a real choice.
 */

/** A subtitle, caption or audio track as the menu names it. */
data class TrackInfo(val label: String, val language: String)

/** "English (SDH)" as the page names it, else the language's name ("English"), else "Track 2". */
fun trackLabel(t: TrackInfo, index: Int, locale: Locale = Locale.getDefault()): String {
    t.label.trim().takeIf { it.isNotEmpty() }?.let { return it }
    languageLabel(t.language, locale)?.let { return it }
    return "Track ${index + 1}"
}

/** "fr" → "French" (in [locale]); null for none or an unknown code ("und"). */
internal fun languageLabel(code: String, locale: Locale = Locale.getDefault()): String? {
    val c = code.trim()
    if (c.isEmpty() || c.equals("und", ignoreCase = true)) return null
    val name = Locale.forLanguageTag(c).getDisplayName(locale)
    if (name.isBlank() || name.equals(c, ignoreCase = true)) return null
    return name.replaceFirstChar { it.titlecase(locale) }
}

/**
 * Captions / Subtitles: Off, then each track, [selected] checked (-1: Off). [pick] gets the
 * track's index, -1 for Off. Empty without tracks (no row).
 */
fun captionChoices(tracks: List<TrackInfo>, selected: Int, pick: (Int) -> Unit, locale: Locale = Locale.getDefault()): List<MenuChoice> {
    if (tracks.isEmpty()) return emptyList()
    return listOf(MenuChoice("Off", selected !in tracks.indices) { pick(-1) }) +
        tracks.mapIndexed { i, t -> MenuChoice(trackLabel(t, i, locale), i == selected) { pick(i) } }
}

/** Audio: each track, [selected] checked; empty with fewer than two (no row). */
fun audioChoices(tracks: List<TrackInfo>, selected: Int, pick: (Int) -> Unit, locale: Locale = Locale.getDefault()): List<MenuChoice> {
    if (tracks.size < 2) return emptyList()
    return tracks.mapIndexed { i, t -> MenuChoice(trackLabel(t, i, locale), i == selected) { pick(i) } }
}

/**
 * The track to turn on for a new video: the language picked last ([remembered], until Koan
 * restarts), when nothing is on yet. -1: leave it.
 */
fun rememberedTrack(tracks: List<TrackInfo>, current: Int, remembered: String?): Int {
    if (current >= 0 || remembered.isNullOrEmpty()) return -1
    val want = remembered.substringBefore('-').lowercase()
    return tracks.indexOfFirst { it.language.substringBefore('-').lowercase() == want }
}
