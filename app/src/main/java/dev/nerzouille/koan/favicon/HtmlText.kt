// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

/** The charset-sniffing part of Z-Sync's `PageTitle`, which is all the favicon loader needs. */
internal object HtmlText {
    private val charset = Regex("charset\\s*=\\s*[\"']?([A-Za-z0-9._-]+)", RegexOption.IGNORE_CASE)

    fun htmlString(data: ByteArray, contentType: String?): String {
        val declared = charsetName(contentType) ?: charsetName(String(data.take(2048).toByteArray(), Charsets.ISO_8859_1))
        val charset = declared?.let { runCatching { charset(it) }.getOrNull() } ?: Charsets.UTF_8
        return String(data, charset)
    }

    private fun charsetName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        return charset.find(header)?.groupValues?.getOrNull(1)
    }
}
