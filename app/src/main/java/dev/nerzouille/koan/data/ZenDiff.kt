package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.ZenRows

/** The Room writes one Zen pull needs. Empty = the desktop changed nothing we show. */
data class ZenDiff(
    val spacesToUpsert: List<SpaceEntity> = emptyList(),
    val spacesToDelete: List<SpaceEntity> = emptyList(),
    val tabsToUpsert: List<TabEntity> = emptyList(),
    val tabsToDelete: List<TabEntity> = emptyList(),
    val foldersToUpsert: List<FolderEntity> = emptyList(),
    val foldersToDelete: List<FolderEntity> = emptyList(),
    val splitsToUpsert: List<SplitEntity> = emptyList(),
    val splitsToDelete: List<SplitEntity> = emptyList(),
) {
    val isEmpty: Boolean
        get() = spacesToUpsert.isEmpty() && spacesToDelete.isEmpty() && tabsToUpsert.isEmpty() &&
            tabsToDelete.isEmpty() && foldersToUpsert.isEmpty() && foldersToDelete.isEmpty() &&
            splitsToUpsert.isEmpty() && splitsToDelete.isEmpty()

    companion object {
        /**
         * Compares a Zen pull with what Room holds. Only rows that really changed are written, so an
         * unchanged desktop costs no Room emission and the overview never redraws for nothing.
         *
         * Phone state survives a pull: phone-only tabs are never touched (they move to the first
         * space if theirs disappeared), a Zen tab keeps its local order until Zen itself moves it,
         * opening history / thumbnails stay attached, and a Zen tab closed on the phone stays closed.
         * Phone-side folder collapse is kept.
         */
        fun between(
            pulled: ZenRows,
            spaces: List<SpaceEntity>,
            tabs: List<TabEntity>,
            folders: List<FolderEntity>,
            splits: List<SplitEntity>,
        ): ZenDiff {
            val pulledSpaceIds = pulled.spaces.mapTo(HashSet()) { it.uuid }
            // Includes the phone-only space used before sign-in: its tabs join the first Zen space.
            val spaceDeletes = spaces.filter { it.uuid !in pulledSpaceIds }
            val goneSpaceIds = spaceDeletes.mapTo(HashSet()) { it.uuid }
            val currentSpaces = spaces.associateBy { it.uuid }
            val spaceUpserts = pulled.spaces.filter { currentSpaces[it.uuid] != it }

            val currentTabs = tabs.associateBy { it.koanTabId }
            val tabUpserts = pulled.tabs.mapNotNull { new ->
                val old = currentTabs[new.koanTabId] ?: return@mapNotNull new
                val merged = new.copy(
                    // Zen moved it → follow Zen; otherwise keep any phone-side reorder.
                    order = if (old.zenOrder == new.zenOrder) old.order else new.order,
                    lastAccessed = old.lastAccessed,
                    thumbnailKey = old.thumbnailKey,
                    dirty = old.dirty,
                    closed = old.closed,
                )
                merged.takeIf { it != old }
            }.toMutableList()
            val pulledTabIds = pulled.tabs.mapTo(HashSet()) { it.koanTabId }
            val tabDeletes = tabs.filter { it.origin == TabOrigin.ZEN && it.koanTabId !in pulledTabIds }
            // Phone-only tabs whose space is gone go to the first space, at the bottom of its stack.
            val home = pulled.spaces.firstOrNull()?.uuid
            if (home != null) {
                var next = (tabs.filter { it.spaceUuid == home }.maxOfOrNull { it.order } ?: -1) + 1
                tabs.filter { it.origin == TabOrigin.LOCAL && it.spaceUuid in goneSpaceIds }
                    .sortedBy { it.order }
                    .forEach { tabUpserts += it.copy(spaceUuid = home, order = next++) }
            }

            val currentFolders = folders.associateBy { it.folderId }
            val folderUpserts = pulled.folders.mapNotNull { new ->
                val old = currentFolders[new.folderId]
                val merged = if (old == null) new else new.copy(collapsed = old.collapsed)
                merged.takeIf { it != old }
            }
            val pulledFolderIds = pulled.folders.mapTo(HashSet()) { it.folderId }
            val folderDeletes = folders.filter { it.folderId !in pulledFolderIds }

            val currentSplits = splits.associateBy { it.splitId }
            val splitUpserts = pulled.splits.filter { currentSplits[it.splitId] != it }
            val pulledSplitIds = pulled.splits.mapTo(HashSet()) { it.splitId }
            val splitDeletes = splits.filter { it.splitId !in pulledSplitIds }

            return ZenDiff(
                spacesToUpsert = spaceUpserts,
                // A space only goes once nothing is left in it; with no Zen space at all, keep it.
                spacesToDelete = if (home == null) emptyList() else spaceDeletes,
                tabsToUpsert = tabUpserts,
                tabsToDelete = tabDeletes,
                foldersToUpsert = folderUpserts,
                foldersToDelete = folderDeletes,
                splitsToUpsert = splitUpserts,
                splitsToDelete = splitDeletes,
            )
        }
    }
}
