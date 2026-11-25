package dev.nerzouille.koan.ui.player

import dev.nerzouille.koan.engine.WebFile

/*
 * A page's video files, one list for two ⋯ rows (docs/plans/PLAYER_TRACKS.md › Quality):
 * - Quality: the renditions the phone can switch to while the video plays a plain file;
 * - Download video: every file, to pick which one to save.
 */

/** "webm", "mp4" (mp4, m4v and mov together): the files the player can swap between. */
internal fun fileFamily(f: WebFile): String {
    val t = f.type.substringBefore(';').trim()
    if (t.isNotEmpty()) return familyOf(t.substringAfter('/'))
    return familyOf(f.src.substringBefore('#').substringBefore('?').substringAfterLast('.', "").lowercase())
}

private fun familyOf(ext: String) = when (ext) {
    "mp4", "m4v", "mov", "quicktime" -> "mp4"
    else -> ext
}

/** "1080p", "Original", else the file's name. */
internal fun fileLabel(f: WebFile): String = when {
    f.original -> "Original"
    f.height > 0 -> "${f.height}p"
    else -> f.src.substringBefore('?').substringBefore('#').substringAfterLast('/')
        .let { raw -> runCatching { java.net.URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrDefault(raw) }
        .ifEmpty { "Video" }
}

/** "1080p · WebM" for the download list. */
internal fun downloadLabel(f: WebFile): String {
    val family = fileFamily(f).uppercase().let { if (it == "WEBM") "WebM" else it }
    return if (family.isEmpty()) fileLabel(f) else "${fileLabel(f)} · $family"
}

/**
 * Quality: the files of the same family as the one playing ([src], a plain file), largest first,
 * the playing one checked. Empty (no row) for a stream, when [src] isn't one of [files], or with
 * fewer than two.
 */
fun qualityChoices(files: List<WebFile>, src: String, kind: String, pick: (WebFile) -> Unit): List<MenuChoice> {
    if (kind == "stream" || src.isEmpty()) return emptyList()
    val current = files.firstOrNull { it.src == src } ?: return emptyList()
    val family = fileFamily(current)
    val same = files.filter { fileFamily(it) == family }.distinctBy { it.src }
        .sortedWith(compareByDescending<WebFile> { it.original }.thenByDescending { it.height })
    if (same.size < 2) return emptyList()
    return same.map { f -> MenuChoice(fileLabel(f), f.src == src) { pick(f) } }
}

/** Download video's choice: every file, original first then largest; empty (a plain row) with fewer than two. */
fun downloadChoices(files: List<WebFile>, pick: (WebFile) -> Unit): List<MenuChoice> {
    val all = files.distinctBy { it.src }.sortedWith(compareByDescending<WebFile> { it.original }.thenByDescending { it.height })
    if (all.size < 2) return emptyList()
    return all.map { f -> MenuChoice(downloadLabel(f), selected = false) { pick(f) } }
}
