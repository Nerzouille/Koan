package dev.nerzouille.koan.ui.player

/*
 * Subtitle files for the standalone player. Android's MediaPlayer reads SubRip (.srt) only, so a
 * WebVTT file (.vtt) is turned into SubRip first.
 */

/** The subtitle files that go with [videoName]: "Clip.mp4" → "Clip.srt", "Clip.vtt". */
fun sideSubtitleNames(videoName: String): List<String> {
    val base = videoName.substringBeforeLast('.', videoName)
    if (base.isEmpty()) return emptyList()
    return listOf("$base.srt", "$base.vtt")
}

/** A subtitle file's text as SubRip, from its [name] (".vtt" converted); null when it's neither. */
fun subtitleAsSrt(name: String, text: String): String? {
    val clean = text.removePrefix("﻿")
    val ext = name.substringAfterLast('.', "").lowercase()
    return when {
        ext == "vtt" || clean.trimStart().startsWith("WEBVTT") -> vttToSrt(clean).takeIf { it.isNotEmpty() }
        ext == "srt" || TIMING.containsMatchIn(clean) -> clean
        else -> null
    }
}

/**
 * WebVTT → SubRip: each cue numbered from 1, times as "00:01:02,500", the text without WebVTT's
 * tags (voices, classes, inline times) and entities decoded. The header, NOTE, STYLE and REGION
 * blocks are dropped, and so are cue settings ("align:start").
 */
fun vttToSrt(vtt: String): String {
    val blocks = vtt.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n').split(Regex("\n{2,}"))
    val out = StringBuilder()
    var n = 0
    for (block in blocks) {
        val lines = block.trim('\n').lines()
        val at = lines.indexOfFirst { "-->" in it }
        if (at < 0) continue
        val first = lines.first().trim()
        if (first.startsWith("NOTE") || first.startsWith("STYLE") || first.startsWith("REGION")) continue
        val parts = lines[at].split("-->")
        val start = srtTime(parts[0].trim()) ?: continue
        val end = srtTime(parts.getOrNull(1)?.trim()?.substringBefore(' ').orEmpty()) ?: continue
        val text = lines.drop(at + 1).map(::plainCueText).filter { it.isNotBlank() }
        if (text.isEmpty()) continue
        n++
        out.append(n).append('\n').append(start).append(" --> ").append(end).append('\n')
        text.forEach { out.append(it).append('\n') }
        out.append('\n')
    }
    return out.toString()
}

/** "01:02.5" / "1:01:02.500" → "00:01:02,500"; null when it isn't a time. */
internal fun srtTime(vtt: String): String? {
    val m = Regex("""^(?:(\d+):)?(\d{1,2}):(\d{1,2})[.,](\d{1,3})$""").matchEntire(vtt) ?: return null
    val h = m.groupValues[1].ifEmpty { "0" }.toInt()
    val min = m.groupValues[2].toInt()
    val s = m.groupValues[3].toInt()
    val ms = m.groupValues[4].padEnd(3, '0').toInt()
    return "%02d:%02d:%02d,%03d".format(h, min, s, ms)
}

private fun plainCueText(line: String): String =
    line.replace(Regex("<[^>]*>"), "")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
        .replace("&lrm;", "").replace("&rlm;", "").replace("&amp;", "&")
        .trim()

private val TIMING = Regex("""\d{2}:\d{2}:\d{2},\d{3} --> \d{2}:\d{2}:\d{2},\d{3}""")

private const val MAX_SUBTITLE_BYTES = 5_000_000

/**
 * Reads the subtitle file [uri] (a content:// address) named [name] and writes it as SubRip into
 * Koan's cache, for MediaPlayer; its path, or null when it can't be read or isn't subtitles.
 * Off the main thread.
 */
internal fun subtitleFile(context: android.content.Context, uri: String, name: String): String? = runCatching {
    val bytes = context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { dev.nerzouille.koan.engine.KoanHttp.readCapped(it, MAX_SUBTITLE_BYTES) } ?: return null
    val srt = subtitleAsSrt(name, String(bytes, Charsets.UTF_8)) ?: return null
    val dir = java.io.File(context.cacheDir, "subtitles").apply { mkdirs() }
    val file = java.io.File.createTempFile("sub", ".srt", dir)
    file.writeText(srt)
    file.absolutePath
}.getOrNull()

/** The converted files of the last video go when a new one opens. */
internal fun clearSubtitleCache(context: android.content.Context) {
    java.io.File(context.cacheDir, "subtitles").listFiles()?.forEach { it.delete() }
}

/** A picked file's name, from the provider. */
internal fun displayName(context: android.content.Context, uri: android.net.Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
