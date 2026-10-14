package dev.nerzouille.koan.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One entry of the drawer's pinned list: a pinned tab, or a folder with its rows. */
sealed interface PinnedEntry {
    data class Tab(val tab: TabEntity) : PinnedEntry

    /**
     * [depth] 0 = top level. Nested folders are flattened one level and indented (docs/SYNC.md).
     * [hidden] = an enclosing folder is collapsed.
     */
    data class Folder(val folder: FolderEntity, val depth: Int, val tabs: List<TabEntity>, val hidden: Boolean = false) : PinnedEntry
}

/** One card of the stack: a tab, or a Zen split view shown as a single card. */
data class StackItem(val id: String, val tabs: List<TabEntity>) {
    val first: TabEntity get() = tabs.first()
    val isSplit: Boolean get() = tabs.size > 1
}

/** A space with its tabs split the way the overview shows them. */
data class SpaceWithTabs(
    val space: SpaceEntity,
    val essentials: List<TabEntity>,
    /** Pinned tabs and folders in Zen sidebar order. */
    val pinned: List<PinnedEntry>,
    /**
     * Normal tabs, plus pinned/essential tabs opened on the phone, oldest first (the most recent sits
     * at the bottom of the stack).
     */
    val stack: List<StackItem>,
) {
    /** Every pinned tab, folders included, in drawer order. */
    val pinnedTabs: List<TabEntity>
        get() = pinned.flatMap { e ->
            when (e) {
                is PinnedEntry.Tab -> listOf(e.tab)
                is PinnedEntry.Folder -> e.tabs
            }
        }
}

class SpacesRepository(private val dao: KoanDao, private val scope: CoroutineScope) {

    val spaces: StateFlow<List<SpaceWithTabs>> =
        combine(dao.observeSpaces(), dao.observeTabs(), dao.observeFolders()) { spaces, tabs, folders ->
            val tabsBySpace = tabs.groupBy { it.spaceUuid }
            val foldersBySpace = folders.groupBy { it.spaceUuid }
            spaces.map { space ->
                val own = tabsBySpace[space.uuid].orEmpty().filterNot { it.closed }
                SpaceWithTabs(
                    space = space,
                    essentials = own.filter { it.essential },
                    pinned = SpaceLayout.pinnedEntries(own.filter { it.pinned && !it.essential }, foldersBySpace[space.uuid].orEmpty()),
                    stack = SpaceLayout.stackItems(own.filter { it.inStack }.sortedBy { it.stackOrder }),
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** No Zen account yet: keep one phone-only space so there is somewhere to open tabs. */
    fun ensureLocalSpace() = scope.launch {
        if (dao.spaces().isEmpty()) dao.upsertSpaces(listOf(LocalSpace.entity()))
    }

    /** Closing a split card closes every member. Zen tabs are hidden, not deleted (TabEntity.closed). */
    fun closeTabs(ids: List<String>) = scope.launch { dao.closeTabs(ids) }

    /** [ordered] = the space's stack cards in their new order; split members stay together. */
    fun reorderStack(ordered: List<StackItem>) = scope.launch {
        dao.setTabOrders(
            ordered.flatMapIndexed { i, item ->
                item.tabs.mapIndexed { m, tab -> tab.koanTabId to i * STRIDE + m }
            },
        )
    }

    fun setFolderCollapsed(folderId: String, collapsed: Boolean) = scope.launch { dao.setFolderCollapsed(folderId, collapsed) }

    /** Dev: fills the space's stack with fake phone tabs, for the 50/150-card performance checks. */
    fun fillWithFakeTabs(spaceUuid: String, upTo: Int) = scope.launch {
        val existing = dao.stackTabs(spaceUuid).size
        if (existing < upTo) {
            dao.upsertTabs(SeedData.filler(spaceUuid, dao.maxTabOrder(spaceUuid) + 1, upTo - existing))
        }
    }

    fun removeFakeTabs() = scope.launch { dao.deleteTabsWithPrefix(SeedData.FAKE_PREFIX) }

    private companion object {
        const val STRIDE = dev.nerzouille.koan.sync.ZenRoomMapper.ORDER_STRIDE
    }
}

/** How a space's rows become the stack's cards and the drawer's pinned list. */
internal object SpaceLayout {
    /** Consecutive members of one split become one card; everything else is one card per tab. */
    fun stackItems(tabs: List<TabEntity>): List<StackItem> {
        val items = mutableListOf<StackItem>()
        val splitIndex = HashMap<String, Int>()
        for (tab in tabs) {
            val split = tab.splitId
            if (split == null) {
                items += StackItem(tab.koanTabId, listOf(tab))
                continue
            }
            val at = splitIndex[split]
            if (at == null) {
                splitIndex[split] = items.size
                items += StackItem("split:$split", listOf(tab))
            } else {
                items[at] = items[at].copy(tabs = items[at].tabs + tab)
            }
        }
        return items
    }

    fun pinnedEntries(tabs: List<TabEntity>, folders: List<FolderEntity>): List<PinnedEntry> {
        val byFolder = tabs.filter { it.folderId != null }.groupBy { it.folderId }
        val folderIds = folders.mapTo(HashSet()) { it.folderId }
        val children = folders.filter { it.parentFolderId != null && it.parentFolderId in folderIds }.groupBy { it.parentFolderId }

        fun entries(folder: FolderEntity, depth: Int, hidden: Boolean): List<PinnedEntry> {
            val own = PinnedEntry.Folder(folder, depth.coerceAtMost(1), byFolder[folder.folderId].orEmpty(), hidden)
            val inner = hidden || folder.collapsed
            return listOf(own) + children[folder.folderId].orEmpty().sortedBy { it.order }.flatMap { entries(it, depth + 1, inner) }
        }

        // Top level: loose pinned tabs (and tabs whose folder vanished) and root folders, by order.
        val loose = tabs.filter { it.folderId == null || it.folderId !in folderIds }
            .map { it.order to listOf<PinnedEntry>(PinnedEntry.Tab(it)) }
        val roots = folders.filter { it.parentFolderId == null || it.parentFolderId !in folderIds }
            .map { it.order to entries(it, 0, hidden = false) }
        return (loose + roots).sortedBy { it.first }.flatMap { it.second }
    }
}
