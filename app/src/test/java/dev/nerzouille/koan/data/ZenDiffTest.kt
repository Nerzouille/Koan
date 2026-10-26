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

    /** Space [space]'s stack, in order, once [diff] is applied to [room]. */
    private fun stackAfter(room: List<TabEntity>, diff: ZenDiff, space: String = "a"): List<String> {
        val rows = room.associateBy { it.koanTabId }.toMutableMap()
        diff.tabsToDelete.forEach { rows.remove(it.koanTabId) }
        diff.tabsToUpsert.forEach { rows[it.koanTabId] = it }
        diff.tabOrders.forEach { (id, order) -> rows[id] = rows.getValue(id).copy(order = order) }
        diff.cardOrders.forEach { (id, card) -> rows[id] = rows.getValue(id).copy(cardOrder = card) }
        return rows.values.filter { it.spaceUuid == space && it.inStack }
            .sortedWith(compareBy({ it.stackOrder }, { it.koanTabId })).map { it.koanTabId }
    }

    @Test
    fun phoneTabsOfAVanishedSpaceMoveToTheFirstSpace() {
        val b = space("b", order = 1)
        val local = pulledTabs + listOf(localTab("p1", "b", 3), localTab("p2", "b", 1))
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs), listOf(a, b), local, emptyList(), emptyList())
        assertEquals(listOf(b), diff.spacesToDelete)
        // At the front of a's stack, after its last tab, keeping their relative order.
        assertEquals(listOf("zen:t1", "zen:t2", "zen:t3", "p2", "p1"), stackAfter(local, diff))
        assertTrue(diff.tabsToUpsert.all { it.spaceUuid == "a" })
    }

    @Test
    fun thePhoneOnlySpaceFoldsIntoZenOnFirstSync() {
        val phone = LocalSpace.entity()
        val local = listOf(localTab("before-sign-in", phone.uuid, 0), localTab("newest", phone.uuid, 8))
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs), listOf(phone), local, emptyList(), emptyList())
        assertEquals(listOf(phone), diff.spacesToDelete)
        assertEquals(listOf(a), diff.spacesToUpsert)
        assertEquals("a", diff.tabsToUpsert.single { it.koanTabId == "before-sign-in" }.spaceUuid)
        // The phone's tabs stay at the front, Zen's merge in behind them.
        assertEquals(listOf("zen:t1", "zen:t2", "zen:t3", "before-sign-in", "newest"), stackAfter(local, diff))
    }

    @Test
    fun aPhoneTabStaysAtTheFrontWhenZenAddsATab() {
        val room = pulledTabs + localTab("phone", "a", 17)
        // Zen's new tab at the bottom of its sidebar: order 0, every other Zen tab shifts.
        val shifted = listOf(zenTab("new", "a", 0)) + pulledTabs.map { it.copy(order = it.order + 8, zenOrder = it.zenOrder + 8) }
        val diff = ZenDiff.between(rows(listOf(a), shifted), listOf(a), room, emptyList(), emptyList())
        assertEquals(listOf("zen:new", "zen:t1", "zen:t2", "zen:t3", "phone"), stackAfter(room, diff))
        assertTrue("phone rows are never rewritten whole", diff.tabsToUpsert.none { it.koanTabId == "phone" })

        // At the top of Zen's sidebar: the highest order, still behind the phone's tab.
        val top = pulledTabs + zenTab("new", "a", 24)
        val diff2 = ZenDiff.between(rows(listOf(a), top), listOf(a), room, emptyList(), emptyList())
        assertEquals(listOf("zen:t1", "zen:t2", "zen:t3", "zen:new", "phone"), stackAfter(room, diff2))
    }

    @Test
    fun aPhoneTabKeepsItsNeighbourWhenZenReorders() {
        // Opened from t1 on the phone, so it sits right after it.
        val room = pulledTabs + localTab("from-t1", "a", 1)
        val zenMoved = listOf(zenTab("t2", "a", 0), zenTab("t3", "a", 8), zenTab("t1", "a", 16))
        val diff = ZenDiff.between(rows(listOf(a), zenMoved), listOf(a), room, emptyList(), emptyList())
        assertEquals(listOf("zen:t2", "zen:t3", "zen:t1", "from-t1"), stackAfter(room, diff))
    }

    @Test
    fun aZenTabClosedInZenMovesNothingElse() {
        val room = pulledTabs + localTab("phone", "a", 17)
        val diff = ZenDiff.between(rows(listOf(a), pulledTabs.take(2)), listOf(a), room, emptyList(), emptyList())
        assertEquals(listOf("zen:t3"), diff.tabsToDelete.map { it.koanTabId })
        assertTrue(diff.tabOrders.isEmpty() && diff.tabsToUpsert.isEmpty())
    }

    @Test
    fun aSplitStaysTogetherAroundPhoneTabs() {
        val split = listOf(zenTab("s1", "a", 8).copy(splitId = "s"), zenTab("s2", "a", 9).copy(splitId = "s"))
        val room = listOf(zenTab("t1", "a", 0)) + split + localTab("phone", "a", 10)
        val pulled = listOf(zenTab("new", "a", 0), zenTab("t1", "a", 8)) +
            split.map { it.copy(order = it.order + 8, zenOrder = it.zenOrder + 8) }
        val diff = ZenDiff.between(rows(listOf(a), pulled), listOf(a), room, emptyList(), emptyList())
        assertEquals(listOf("zen:new", "zen:t1", "zen:s1", "zen:s2", "phone"), stackAfter(room, diff))
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
