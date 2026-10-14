package dev.nerzouille.koan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceLayoutTest {
    private fun tab(id: String, order: Int, folder: String? = null, split: String? = null) = TabEntity(
        koanTabId = id, spaceUuid = "s", url = "https://$id/", title = id, order = order,
        folderId = folder, splitId = split, pinned = folder != null,
    )

    @Test
    fun splitMembersBecomeOneCardAtTheirPlace() {
        val items = SpaceLayout.stackItems(
            listOf(tab("a", 0), tab("s1", 8, split = "x"), tab("s2", 9, split = "x"), tab("b", 16)),
        )
        assertEquals(listOf("a", "split:x", "b"), items.map { it.id })
        assertEquals(listOf("s1", "s2"), items[1].tabs.map { it.koanTabId })
    }

    @Test
    fun pinnedListInterleavesTabsAndFoldersAndHidesCollapsedChildren() {
        val outer = FolderEntity("f", "s", name = "Outer", order = 8, collapsed = true)
        val inner = FolderEntity("g", "s", parentFolderId = "f", name = "Inner", order = 8)
        val entries = SpaceLayout.pinnedEntries(
            listOf(tab("p0", 0), tab("p2", 16), tab("fa", 0, folder = "f"), tab("ga", 0, folder = "g")),
            listOf(inner, outer),
        )
        assertEquals(
            listOf("tab p0", "folder f d0 shown", "folder g d1 hidden", "tab p2"),
            entries.map {
                when (it) {
                    is PinnedEntry.Tab -> "tab ${it.tab.koanTabId}"
                    is PinnedEntry.Folder -> "folder ${it.folder.folderId} d${it.depth} ${if (it.hidden) "hidden" else "shown"}"
                }
            },
        )
        assertEquals(listOf("fa"), (entries[1] as PinnedEntry.Folder).tabs.map { it.koanTabId })
    }
}
