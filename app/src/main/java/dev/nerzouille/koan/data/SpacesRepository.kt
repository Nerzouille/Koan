package dev.nerzouille.koan.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** A space with its tabs split the way the overview shows them. */
data class SpaceWithTabs(
    val space: SpaceEntity,
    val essentials: List<TabEntity>,
    val pinned: List<TabEntity>,
    /** Normal tabs, oldest first (the most recent sits at the bottom of the stack). */
    val stack: List<TabEntity>,
)

class SpacesRepository(private val dao: KoanDao, private val scope: CoroutineScope) {

    val spaces: StateFlow<List<SpaceWithTabs>> =
        combine(dao.observeSpaces(), dao.observeTabs()) { spaces, tabs ->
            val bySpace = tabs.groupBy { it.spaceUuid }
            spaces.map { space ->
                val own = bySpace[space.uuid].orEmpty()
                SpaceWithTabs(
                    space = space,
                    essentials = own.filter { it.essential },
                    pinned = own.filter { it.pinned && !it.essential },
                    stack = own.filter { !it.pinned && !it.essential },
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun seedIfEmpty() = scope.launch {
        if (dao.spaceCount() == 0) {
            dao.insertSpaces(SeedData.spaces())
            dao.upsertTabs(SeedData.tabs())
        }
    }

    fun closeTab(koanTabId: String) = scope.launch { dao.deleteTab(koanTabId) }

    /** [orderedIds] = the space's stack tabs in their new order. */
    fun reorderStack(orderedIds: List<String>) = scope.launch { dao.reorder(orderedIds) }

    /** Adds a tab at the bottom of the space's stack and returns its id right away. */
    fun addTab(spaceUuid: String, url: String, title: String): String {
        val id = UUID.randomUUID().toString()
        scope.launch {
            val order = dao.maxTabOrder(spaceUuid) + 1
            dao.upsertTabs(
                listOf(
                    TabEntity(id, spaceUuid = spaceUuid, url = url, title = title, order = order, lastAccessed = System.currentTimeMillis()),
                ),
            )
        }
        return id
    }

    fun fillWithFakeTabs(spaceUuid: String, upTo: Int = 50) = scope.launch {
        val existing = dao.stackTabs(spaceUuid).size
        if (existing < upTo) {
            dao.upsertTabs(SeedData.filler(spaceUuid, dao.maxTabOrder(spaceUuid) + 1, upTo - existing))
        }
    }
}
