package dev.nerzouille.koan.data

import dev.nerzouille.koan.sync.ZenRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenDiffTest {
    private fun space(uuid: String, order: Int = 0, zen: Boolean = true) =
        SpaceEntity(uuid, uuid, "", emptyList(), order = order, zenOrigin = zen)

    private fun zenTab(id: String, space: String, order: Int) = TabEntity(
        koanTabId = "zen:$id", zenTabId = id, spaceUuid = space, url = "https://$id/", title = id,
        order = order, zenOrder = order, origin = TabOrigin.ZEN,
    )

    private fun localTab(id: String, space: String, order: Int) =
        TabEntity(koanTabId = id, spaceUuid = space, url = "https://$id/", title = id, order = order)

    private fun rows(spaces: List<SpaceEntity>, tabs: List<TabEntity>, folders: List<FolderEntity> = emptyList()) =
        ZenRows(spaces, tabs, folders, emptyList(), emptyMap())

    private val a = space("a")
    private val pulledTabs = listOf(zenTab("t1", "a", 0), zenTab("t2", "a", 8), zenTab("t3", "a", 16))

    @Test
    fun unchangedPullWritesNothing() {
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs), listOf(a), pulledTabs, emptyList(), emptyList())
        assertTrue(diff.isEmpty)
    }

    @Test
    fun phoneSideReorderSurvivesUntilZenMovesTheTab() {
        // The user moved t3 to the top on the phone (orders rewritten 0, 8, 16 → t3, t1, t2).
        val local = listOf(pulledTabs[0].copy(order = 8), pulledTabs[1].copy(order = 16), pulledTabs[2].copy(order = 0))
        assertTrue(ZenDiff.between(rows(listOf(a), pulledTabs), listOf(a), local, emptyList(), emptyList()).isEmpty)

        // Now Zen itself moves t1 to the end: Zen wins for that tab.
        val zenMoved = listOf(zenTab("t2", "a", 0), zenTab("t3", "a", 8), zenTab("t1", "a", 16))
        val diff = ZenDiff.between(rows(listOf(a), zenMoved), listOf(a), local, emptyList(), emptyList())
        assertEquals(mapOf("zen:t1" to 16, "zen:t2" to 0, "zen:t3" to 8), diff.tabsToUpsert.associate { it.koanTabId to it.order })
    }

    @Test
    fun phoneStateIsKeptOnChangedRows() {
        val local = pulledTabs.map { it.copy(lastAccessed = 42, thumbnailKey = "thumb", closed = it.zenTabId == "t2") }
        val renamed = pulledTabs.map { if (it.zenTabId == "t2") it.copy(title = "New title") else it }
        val diff = ZenDiff.between(rows(listOf(a), renamed), listOf(a), local, emptyList(), emptyList())
        val t2 = diff.tabsToUpsert.single()
        assertEquals("New title", t2.title)
        assertEquals(42L, t2.lastAccessed)
        assertEquals("thumb", t2.thumbnailKey)
        assertTrue("closed on the phone stays closed", t2.closed)
    }

    @Test
    fun zenTabsGoneFromZenAreDeletedButPhoneTabsStay() {
        val local = pulledTabs + localTab("phone", "a", 24)
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs.take(2)), listOf(a), local, emptyList(), emptyList())
        assertEquals(listOf("zen:t3"), diff.tabsToDelete.map { it.koanTabId })
        assertTrue(diff.tabsToUpsert.isEmpty())
    }

    @Test
    fun phoneTabsOfAVanishedSpaceMoveToTheFirstSpace() {
        val b = space("b", order = 1)
        val local = pulledTabs + listOf(localTab("p1", "b", 3), localTab("p2", "b", 1))
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs), listOf(a, b), local, emptyList(), emptyList())
        assertEquals(listOf(b), diff.spacesToDelete)
        // Appended after a's last tab (order 16), keeping their relative order.
        assertEquals(listOf("p2" to 17, "p1" to 18), diff.tabsToUpsert.map { it.koanTabId to it.order })
        assertTrue(diff.tabsToUpsert.all { it.spaceUuid == "a" })
    }

    @Test
    fun thePhoneOnlySpaceFoldsIntoZenOnFirstSync() {
        val phone = LocalSpace.entity()
        val local = listOf(localTab("before-sign-in", phone.uuid, 0))
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs), listOf(phone), local, emptyList(), emptyList())
        assertEquals(listOf(phone), diff.spacesToDelete)
        assertEquals(listOf(a), diff.spacesToUpsert)
        assertEquals("a", diff.tabsToUpsert.single { it.koanTabId == "before-sign-in" }.spaceUuid)
    }

    @Test
    fun anEmptyPullNeverDeletesSpaces() {
        val diff = ZenDiff.between(rows(emptyList(), emptyList()), listOf(a), pulledTabs, emptyList(), emptyList())
        assertTrue(diff.spacesToDelete.isEmpty())
    }

    @Test
    fun folderCollapseIsAPhoneSetting() {
        val folder = FolderEntity("f", "a", name = "Docs", order = 0)
        val diff = ZenDiff.between(
            rows(listOf(a), pulledTabs, listOf(folder.copy(name = "Docs 2"))),
            listOf(a), pulledTabs, listOf(folder.copy(collapsed = true)), emptyList(),
        )
        assertEquals(listOf(folder.copy(name = "Docs 2", collapsed = true)), diff.foldersToUpsert)
    }

    // ---- M3: pages navigated on the phone ----

    private fun phoneNavigated(t: TabEntity) =
        t.copy(url = "https://phone/", title = "Phone page", iconUrl = null, dirty = true, zenUrl = t.url)

    @Test
    fun phoneNavigationSurvivesAPullWhenZenDidNotNavigate() {
        val local = listOf(phoneNavigated(pulledTabs[0]), pulledTabs[1], pulledTabs[2])
        val pulled = pulledTabs.map { it.copy(zenUrl = it.url) }
        val diff = ZenDiff.between(rows(listOf(a), pulled), listOf(a), local, emptyList(), emptyList())
        assertTrue(diff.tabsToUpsert.none { it.koanTabId == "zen:t1" && it.url != "https://phone/" })
    }

    @Test
    fun zenNavigationWinsOverAPhonePageThatIsNotOpen() {
        val local = listOf(phoneNavigated(pulledTabs[0]), pulledTabs[1], pulledTabs[2])
        val moved = pulledTabs[0].copy(url = "https://zen-moved/", zenUrl = "https://zen-moved/")
        val diff = ZenDiff.between(rows(listOf(a), listOf(moved) + pulledTabs.drop(1)), listOf(a), local, emptyList(), emptyList())
        val t1 = diff.tabsToUpsert.single { it.koanTabId == "zen:t1" }
        assertEquals("https://zen-moved/", t1.url)
        assertEquals(false, t1.dirty)
    }

    @Test
    fun aTabOpenOnThePhoneKeepsItsPageEvenWhenZenNavigates() {
        val local = listOf(phoneNavigated(pulledTabs[0]), pulledTabs[1], pulledTabs[2])
        val moved = pulledTabs[0].copy(url = "https://zen-moved/", zenUrl = "https://zen-moved/")
        val diff = ZenDiff.between(
            rows(listOf(a), listOf(moved) + pulledTabs.drop(1)), listOf(a), local, emptyList(), emptyList(), live = setOf("zen:t1"),
        )
        val t1 = diff.tabsToUpsert.single { it.koanTabId == "zen:t1" }
        assertEquals("https://phone/", t1.url)
        assertEquals("https://zen-moved/", t1.zenUrl)
        assertEquals(true, t1.dirty)
    }

    @Test
    fun aLivePinnedTabKeepsItsCard() {
        val pinned = zenTab("p", "a", 0).copy(pinned = true)
        val local = listOf(pinned.copy(cardOrder = 40))
        val diff = ZenDiff.between(rows(listOf(a), listOf(pinned.copy(title = "renamed"))), listOf(a), local, emptyList(), emptyList())
        assertEquals(40, diff.tabsToUpsert.single().cardOrder)
    }
}
