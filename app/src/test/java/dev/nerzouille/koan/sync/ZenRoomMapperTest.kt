package dev.nerzouille.koan.sync

import dev.nerzouille.koan.data.TabOrigin
import dev.nerzouille.koan.sync.ZenRoomMapper.ORDER_STRIDE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenRoomMapperTest {
    private fun tab(id: String, icon: String? = null, static: Boolean? = null) =
        ZenSpaces.ZenTab(id = id, url = "https://$id.example/", title = "Tab $id", icon = icon, hasStaticIcon = static)

    private fun item(id: String) = ZenSpaces.ZenItem.Tab(tab(id))

    private val inlineIcon = "data:image/png;base64,iVBORw0KGgo="
    private val emojiIcon = "data:image/svg+xml;base64,PHN2Zz48dGV4dD7wn4yIPC90ZXh0Pjwvc3ZnPg=="

    private val work = ZenSpaces.ZenSpace(
        id = "space-work",
        name = "Work",
        icon = "chrome://browser/skin/zen-icons/selectable/briefcase.svg",
        containerGuid = null,
        theme = ZenSpaces.ZenSpaceTheme(
            type = "gradient",
            dots = listOf(
                ZenSpaces.ZenThemeDot(ZenSpaces.ZenColorValue(0.0, 0.0, 1.0)),
                ZenSpaces.ZenThemeDot(ZenSpaces.ZenColorValue(1.0, 0.0, 0.0), isPrimary = true),
            ),
            opacity = null, texture = null, lightness = null,
        ),
        pinned = listOf(
            item("p1"),
            ZenSpaces.ZenItem.Folder(
                ZenSpaces.ZenFolder(
                    id = "f1", name = "Docs", icon = null, tabs = listOf(tab("f1a"), tab("f1b")),
                    subfolders = listOf(ZenSpaces.ZenFolder("f2", "Inner", null, listOf(tab("f2a")))),
                ),
            ),
            ZenSpaces.ZenItem.Split(ZenSpaces.ZenSplit("sp-pinned", "vsep", listOf(tab("ps1"), tab("ps2")))),
        ),
        tabs = listOf(
            ZenSpaces.ZenItem.Tab(tab("n1", icon = inlineIcon)),
            ZenSpaces.ZenItem.Split(ZenSpaces.ZenSplit("sp", "hsep", listOf(tab("s1"), tab("s2"), tab("s3")))),
            ZenSpaces.ZenItem.Tab(tab("n2", icon = emojiIcon, static = true)),
        ),
    )
    private val home = ZenSpaces.ZenSpace(id = "space-home", name = "Home", icon = "🏡", containerGuid = null, theme = null, pinned = emptyList())

    private val snapshot = ZenSpaces.ZenSnapshot(
        spaces = listOf(work, home),
        essentials = mapOf("default" to listOf(tab("e1"), tab("e2"))),
        fetchedAtMillis = 0,
    )

    private val rows = ZenRoomMapper.map(snapshot)
    private fun row(id: String) = rows.tabs.single { it.koanTabId == id }

    @Test
    fun spacesKeepZenOrderIconAndPrimaryFirstGradient() {
        assertEquals(listOf("space-work", "space-home"), rows.spaces.map { it.uuid })
        assertEquals(listOf(0, 1), rows.spaces.map { it.order })
        assertTrue(rows.spaces.all { it.zenOrigin })
        assertEquals(work.icon, rows.spaces[0].icon)
        assertEquals(listOf(0xFFFF0000L, 0xFF0000FFL), rows.spaces[0].gradientColors)
        assertEquals(emptyList<Long>(), rows.spaces[1].gradientColors)
    }

    @Test
    fun sharedEssentialsAppearInEverySpaceWithTheirOwnRow() {
        val essentials = rows.tabs.filter { it.essential }
        assertEquals(
            setOf("zen:space-work:e1", "zen:space-work:e2", "zen:space-home:e1", "zen:space-home:e2"),
            essentials.map { it.koanTabId }.toSet(),
        )
        assertTrue(essentials.all { it.pinned && it.zenTabId in setOf("e1", "e2") })
        assertEquals(listOf(0, ORDER_STRIDE), essentials.filter { it.spaceUuid == "space-home" }.map { it.order })
    }

    @Test
    fun pinnedTabsFoldersAndSplitsShareOneOrderScale() {
        assertEquals(0, row("zen:p1").order)
        assertTrue(row("zen:p1").pinned)
        val folder = rows.folders.single { it.folderId == "f1" }
        assertEquals(ORDER_STRIDE, folder.order)
        assertNull(folder.parentFolderId)
        assertFalse("folders start open on the phone", rows.folders.any { it.collapsed })
        assertEquals("f1", rows.folders.single { it.folderId == "f2" }.parentFolderId)
        assertEquals(listOf("f1", "f1"), listOf(row("zen:f1a").folderId, row("zen:f1b").folderId))
        assertEquals("f2", row("zen:f2a").folderId)
        assertEquals(listOf(2 * ORDER_STRIDE, 2 * ORDER_STRIDE + 1), listOf(row("zen:ps1").order, row("zen:ps2").order))
        assertTrue(row("zen:ps1").pinned)
    }

    @Test
    fun normalTabsAndSplitMembersStayTogether() {
        val stack = rows.tabs.filter { it.spaceUuid == "space-work" && !it.pinned }.sortedBy { it.order }
        assertEquals(listOf("zen:n1", "zen:s1", "zen:s2", "zen:s3", "zen:n2"), stack.map { it.koanTabId })
        assertEquals(listOf("sp", "sp", "sp"), stack.subList(1, 4).map { it.splitId })
        assertEquals(listOf("zen:s1", "zen:s2", "zen:s3"), rows.splits.single { it.splitId == "sp" }.tabIds)
        assertTrue(stack.all { it.origin == TabOrigin.ZEN && it.zenOrder == it.order })
    }

    @Test
    fun inlineFaviconsBecomeKeysAndStaticIconsStayRaw() {
        val n1 = row("zen:n1")
        assertTrue(n1.iconUrl!!.startsWith(ZenRoomMapper.INLINE_ICON_PREFIX))
        assertFalse(n1.staticIcon)
        assertEquals(inlineIcon, rows.inlineIcons[n1.iconUrl])
        val n2 = row("zen:n2")
        assertEquals(emojiIcon, n2.iconUrl)
        assertTrue(n2.staticIcon)
        assertNull(row("zen:p1").iconUrl)
        assertEquals(1, rows.inlineIcons.size)
    }

    @Test
    fun mappingIsDeterministic() {
        assertEquals(rows, ZenRoomMapper.map(snapshot))
    }
}
