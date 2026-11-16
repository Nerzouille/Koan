package dev.nerzouille.koan.privacy

import dev.nerzouille.koan.data.SecretBox
import java.io.File

/**
 * One file on disk, sealed with [box] (a Keystore key on the phone), never in plain text: Private's
 * tabs and bookmarks. A write goes to a temporary file first, then replaces the old one, so a kill
 * mid-write keeps the previous version. A file that can't be opened (bad bytes, another key) reads
 * as missing.
 */
class SealedFile(private val file: File, private val box: SecretBox) {
    fun read(): String? = runCatching {
        if (!file.exists()) return null
        String(box.open(file.readBytes()), Charsets.UTF_8)
    }.getOrNull()

    fun write(text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeBytes(box.seal(text.toByteArray(Charsets.UTF_8)))
        if (!tmp.renameTo(file)) tmp.delete()
    }

    fun delete() {
        file.delete()
        File(file.path + ".tmp").delete()
    }
}
