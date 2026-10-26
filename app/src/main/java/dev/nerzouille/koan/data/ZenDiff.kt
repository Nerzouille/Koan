package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.ZenRoomMapper
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
    /** Phone-only rows that only move in their stack: (koanTabId → order), written alone. */
    val tabOrders: Map<String, Int> = emptyMap(),
    /** The same for pinned tabs' cards: (koanTabId → cardOrder). */
    val cardOrders: Map<String, Int> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = spacesToUpsert.isEmpty() && spacesToDelete.isEmpty() && tabsToUpsert.isEmpty() &&
            tabsToDelete.isEmpty() && foldersToUpsert.isEmpty() && foldersToDelete.isEmpty() &&
            splitsToUpsert.isEmpty() && splitsToDelete.isEmpty() && tabOrders.isEmpty() && cardOrders.isEmpty()

    companion object {
        /**
         * Compares a Zen pull with what Room holds. Only rows that really changed are written, so an
         * unchanged desktop costs no Room emission and the overview never redraws for nothing.
         *
         * Phone state survives a pull: phone-only tabs keep their page and their place in the stack
         * (see [stackPositions]; they move to the front of the first space if theirs disappeared,
         * e.g. the phone-only space at first sign-in), Zen tabs keep the phone's arrangement until
         * Zen itself reorders them,
         * opening history / thumbnails stay attached, and a Zen tab closed on the phone stays closed.
         * Phone-side folder collapse is kept. A tab navigated on the phone ([TabEntity.dirty]) keeps
         * its page until Zen itself navigates it, and always while it is open on the phone ([live]).
         */
        fun between(
            pulled: ZenRows,
            spaces: List<SpaceEntity>,
            tabs: List<TabEntity>,
            folders: List<FolderEntity>,
            splits: List<SplitEntity>,
            live: Set<String> = emptySet(),
        ): ZenDiff {
            val pulledSpaceIds = pulled.spaces.mapTo(HashSet()) { it.uuid }
            // Includes the phone-only space used before sign-in: its tabs join the first Zen space.
            val spaceDeletes = spaces.filter { it.uuid !in pulledSpaceIds }
            val goneSpaceIds = spaceDeletes.mapTo(HashSet()) { it.uuid }
            val currentSpaces = spaces.associateBy { it.uuid }
            val spaceUpserts = pulled.spaces.filter { currentSpaces[it.uuid] != it }

            val currentTabs = tabs.associateBy { it.koanTabId }
            val mergedZen = pulled.tabs.map { new ->
                val old = currentTabs[new.koanTabId] ?: return@map new
                val zenNavigated = old.zenUrl != null && old.zenUrl != new.url
                val keepPhonePage = old.dirty && (!zenNavigated || old.koanTabId in live)
                val page = if (keepPhonePage) new.copy(url = old.url, title = old.title, iconUrl = old.iconUrl, staticIcon = old.staticIcon) else new
                page.copy(
                    // Stack cards: placed by stackPositions. Drawer rows: Zen moved it → follow
                    // Zen; otherwise keep any phone-side reorder.
                    order = if (old.zenOrder == new.zenOrder || zenPlaced(old) && zenPlaced(new)) old.order else new.order,
                    lastAccessed = old.lastAccessed,
                    thumbnailKey = old.thumbnailKey,
                    dirty = keepPhonePage,
                    closed = old.closed,
                    cardOrder = old.cardOrder,
                )
            }
            val pulledTabIds = pulled.tabs.mapTo(HashSet()) { it.koanTabId }
            val tabDeletes = tabs.filter { it.origin == TabOrigin.ZEN && it.koanTabId !in pulledTabIds }
            // Phone-only tabs whose space is gone go to the first space.
            val home = pulled.spaces.firstOrNull()?.uuid
            val moved = if (home == null) emptyMap() else {
                tabs.filter { it.origin == TabOrigin.LOCAL && it.spaceUuid in goneSpaceIds }
                    .associate { it.koanTabId to it.copy(spaceUuid = home) }
            }
            val locals = tabs.filter { it.origin == TabOrigin.LOCAL }.map { moved[it.koanTabId] ?: it }

            val positions = HashMap<String, Int>()
            pulled.spaces.forEach { space ->
                val before = tabs.filter { it.spaceUuid == space.uuid && it.inStack }.sortedWith(compareBy({ it.stackOrder }, { it.koanTabId }))
                val after = (mergedZen + locals).filter { it.spaceUuid == space.uuid && it.inStack }
                stackPositions(before, after)?.let(positions::putAll)
            }
            fun placed(t: TabEntity): TabEntity {
                val p = positions[t.koanTabId] ?: return t
                return if (t.pinned || t.essential) t.copy(cardOrder = p) else t.copy(order = p)
            }
            val tabUpserts = mergedZen.map(::placed).filter { it != currentTabs[it.koanTabId] } + moved.values.map(::placed)
            val tabOrders = HashMap<String, Int>()
            val cardOrders = HashMap<String, Int>()
            locals.filter { it.koanTabId !in moved }.forEach { t ->
                val p = placed(t)
                when {
                    p.cardOrder != t.cardOrder -> cardOrders[t.koanTabId] = p.cardOrder ?: return@forEach
                    p.order != t.order -> tabOrders[t.koanTabId] = p.order
                }
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
                tabOrders = tabOrders,
                cardOrders = cardOrders,
            )
        }

        /** A normal Zen tab: Zen decides where it sits in the stack. Other cards are the phone's. */
        private fun zenPlaced(t: TabEntity) = t.origin == TabOrigin.ZEN && !t.pinned && !t.essential

        /**
         * One space's stack after a pull: [before] = its cards as Room has them, in stack order;
         * [after] = its cards once the pull is applied. Returns each card's new position (order, or
         * cardOrder for a pinned tab's card), or null when the current positions still hold.
         *
         * Zen's cards keep the phone's arrangement while Zen's own sequence is unchanged, and follow
         * Zen once it reorders them; a new Zen tab goes right after its predecessor in Zen. Every
         * other card (phone-only tabs, pinned tabs' cards) stays right after the Zen card it
         * followed, after any new Zen tab that lands there too, so the cards opened on the phone
         * never move under the user. Cards new to this space (from a vanished one) go to the front.
         */
        private fun stackPositions(before: List<TabEntity>, after: List<TabEntity>): Map<String, Int>? {
            val afterById = after.associateBy { it.koanTabId }
            val unit = after.associate { it.koanTabId to (it.splitId ?: it.koanTabId) }
            val zenOrder = compareBy<TabEntity>({ it.zenOrder }, { it.koanTabId })
            val stayedRows = before.filter { zenPlaced(it) && afterById[it.koanTabId]?.let(::zenPlaced) == true }
            val stayed = stayedRows.mapTo(HashSet()) { it.koanTabId }
            val byZen = after.filter(::zenPlaced).sortedWith(zenOrder)
            val zenNow = byZen.map { it.koanTabId }.filter { it in stayed }
            val zenThen = stayedRows.sortedWith(zenOrder).map { it.koanTabId }
            val seq = (if (zenNow == zenThen) stayedRows.map { it.koanTabId } else zenNow).toMutableList()
            var prev: String? = null
            byZen.forEach { t ->
                if (t.koanTabId !in stayed) seq.add(prev?.let { seq.indexOf(it) + 1 } ?: 0, t.koanTabId)
                prev = t.koanTabId
            }

            // Every other card, by the Zen card (its split, if any) it followed; null = none.
            val anchored = LinkedHashMap<String?, MutableList<String>>()
            var anchor: String? = null
            before.forEach { t ->
                val now = afterById[t.koanTabId] ?: return@forEach
                if (t.koanTabId in stayed) anchor = unit[t.koanTabId] else if (!zenPlaced(now)) anchored.getOrPut(anchor) { mutableListOf() } += t.koanTabId
            }
            val known = before.mapTo(HashSet()) { it.koanTabId }
            val arriving = after.filter { !zenPlaced(it) && it.koanTabId !in known }.sortedWith(compareBy({ it.stackOrder }, { it.koanTabId }))

            val units = mutableListOf<List<String>>()
            fun phoneCards(key: String?) = anchored.remove(key)?.forEach { units += listOf(it) }
            var current: String? = null
            var i = 0
            while (i < seq.size) {
                val key = unit.getValue(seq[i])
                var end = i + 1
                while (end < seq.size && unit.getValue(seq[end]) == key) end++
                val members = seq.subList(i, end).toList()
                if (members.any { it in stayed }) {
                    phoneCards(current)
                    current = key
                }
                units += members
                i = end
            }
            phoneCards(current)
            anchored.values.forEach { ids -> ids.forEach { units += listOf(it) } }
            arriving.forEach { units += listOf(it.koanTabId) }

            val order = units.flatten()
            // Nothing new and nothing moved: the current positions still hold.
            if (order == before.map { it.koanTabId }.filter { it in afterById }) return null
            val positions = HashMap<String, Int>()
            units.forEachIndexed { index, ids -> ids.forEachIndexed { m, id -> positions[id] = index * ZenRoomMapper.ORDER_STRIDE + m } }
            return positions
        }
    }
}
