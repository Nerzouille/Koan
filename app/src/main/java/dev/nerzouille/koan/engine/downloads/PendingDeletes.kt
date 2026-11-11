package dev.nerzouille.koan.engine.downloads

/**
 * Removals waiting for their Undo (audit 3.29): a key is [hold]ed when its Undo toast shows,
 * [cancel]led by Undo, and [commit]ted when that toast leaves the screen (timed out, swiped away,
 * covered, or replaced by another toast). Only keys still held commit, once each.
 */
class PendingDeletes<K> {
    private val held = LinkedHashSet<K>()

    fun hold(key: K) {
        held += key
    }

    /** Undo: true when [key] was still waiting (nothing happens to it). */
    fun cancel(key: K): Boolean = held.remove(key)

    /** The toast is gone: the keys of [keys] still held, now to remove for good. */
    fun commit(keys: Collection<K>): List<K> = keys.filter { held.remove(it) }

    val waiting: Set<K> get() = held
}
