package dev.nerzouille.koan.sync

/**
 * CLAUDE.md rule 4: Koan never writes the Zen `spaces` collection unless the `writeBackEnabled`
 * setting is on (Settings, default off, not shown in the UI until M6). Writing can scramble the
 * owner's desktop Spaces. Checked by [SyncClient] on every request that is not a read, whatever
 * method built it, so no write path can skip it.
 */
object SpacesWriteGate {
    const val COLLECTION = "spaces"

    /** Mirrors Settings.writeBackEnabled (Components keeps it in step). Off until then. */
    @Volatile
    var writeBackEnabled: Boolean = false

    /** True when [method] on storage [path] would change the Zen spaces collection. */
    fun touchesSpaces(method: String, path: String): Boolean {
        if (method.uppercase() in READS) return false
        val p = path.substringBefore('?').trimEnd('/')
        return p == "/storage/$COLLECTION" || p.startsWith("/storage/$COLLECTION/") || p == "/storage"
    }

    /** Throws [SyncError.WriteBackDisabled] when the request would write `spaces` while it's off. */
    fun check(method: String, path: String) {
        if (!writeBackEnabled && touchesSpaces(method, path)) throw SyncError.WriteBackDisabled
    }

    private val READS = setOf("GET", "HEAD")
}
