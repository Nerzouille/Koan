// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import dev.nerzouille.koan.sync.SpacesSyncService
import dev.nerzouille.koan.sync.ZenSpaces
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format decoder tests, driven by the golden fixtures in
 * `shared/contract/fixtures/` (see SPEC.md). Wire-record expectations come
 * from the fixtures' `expect` blocks (mirroring the iOS suite), so a fixture
 * edit propagates to both platforms. Tests without fixture coverage
 * (assembly, partitions) keep their literals.
 */
class ZenSpacesDecoderTests {
    private fun fail(message: String): Nothing = throw AssertionError(message)

    /** Decodes a single-case fixture's `input` record through the real decoder. */
    private fun decodeFixtureInput(name: String): ZenSpaces.DecodedRecord? {
        val input = FixtureLoader.json(name).getJSONObject("input")
        return ZenSpaces.decode(input.optString("id"), input)
    }

    /** The `expect` block of a single-case fixture (canonical expectations). */
    private fun expectOf(name: String): JSONObject =
        FixtureLoader.json(name).getJSONObject("expect")

    /** String array under `key` as a Kotlin list. */
    private fun expectStrings(json: JSONObject, key: String): List<String> {
        val arr = json.getJSONArray(key)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun expectNullableString(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.getString(key)

    private fun expectNullableBool(json: JSONObject, key: String): Boolean? =
        if (json.isNull(key)) null else json.getBoolean(key)

    private fun spaceRecord(name: String): ZenSpaces.ZenSpaceRecord =
        (decodeFixtureInput(name) as? ZenSpaces.DecodedRecord.Space)?.record
            ?: fail("expected $name to decode as space")

    private fun tabRecord(name: String): ZenSpaces.ZenTabRecord =
        (decodeFixtureInput(name) as? ZenSpaces.DecodedRecord.Tab)?.record
            ?: fail("expected $name to decode as tab")

    private fun folderRecord(name: String): ZenSpaces.ZenFolderRecord =
        (decodeFixtureInput(name) as? ZenSpaces.DecodedRecord.Folder)?.record
            ?: fail("expected $name to decode as folder")

    private fun splitRecord(name: String): ZenSpaces.ZenSplitRecord =
        (decodeFixtureInput(name) as? ZenSpaces.DecodedRecord.Split)?.record
            ?: fail("expected $name to decode as split")

    // MARK: Spaces

    @Test
    fun spaceRecordDecodes() {
        val expect = expectOf("wire-space-basic")
        val space = spaceRecord("wire-space-basic")
        assertEquals(expect.getString("uuid"), space.uuid)
        assertEquals(expect.getString("name"), space.name)
        assertEquals(expectNullableString(expect, "icon"), space.icon)
        assertEquals(expectStrings(expect, "children"), space.children)
        assertEquals(expectStrings(expect, "gradientColors"), space.theme?.gradientColors)
        assertEquals(expect.getInt("dotCount"), space.theme?.dots?.size)
        assertEquals(expect.getDouble("opacity"), space.theme?.opacity)
        assertEquals(expect.getDouble("texture"), space.theme?.texture)
        assertEquals(expectNullableString(expect, "containerGuid"), space.containerGuid)
    }

    @Test
    fun spaceRecordWithRealGradientObjectsDecodes() {
        val expect = expectOf("wire-space-object-dots")
        val space = spaceRecord("wire-space-object-dots")
        assertEquals(expect.getString("uuid"), space.uuid)
        assertEquals(expect.getString("name"), space.name)
        assertEquals(expectStrings(expect, "gradientColors"), space.theme?.gradientColors)
        val dot = space.theme?.dots?.first() ?: fail("expected one canonical dot")
        val dotExpect = expect.getJSONArray("dots").getJSONObject(0)
        assertEquals(dotExpect.getString("hex"), dot.color.hexString)
        assertEquals(dotExpect.getBoolean("isCustom"), dot.isCustom)
        assertEquals(dotExpect.getBoolean("isPrimary"), dot.isPrimary)
        assertEquals(dotExpect.getString("algorithm"), dot.algorithm)
        assertEquals(dotExpect.getDouble("lightness"), dot.lightness)
        assertEquals(dotExpect.getDouble("positionX"), dot.positionX)
        assertEquals(dotExpect.getDouble("positionY"), dot.positionY)
        assertEquals(dotExpect.getString("type"), dot.type)
        assertEquals(expect.getDouble("opacity"), space.theme?.opacity)
        assertEquals(expect.getDouble("texture"), space.theme?.texture)
    }

    @Test
    fun spaceRecordWithRGBArrayDots() {
        val expect = expectOf("wire-space-rgb-dots")
        val space = spaceRecord("wire-space-rgb-dots")
        assertEquals(expect.getString("uuid"), space.uuid)
        val dotExpect = expect.getJSONArray("dots")
        assertEquals(dotExpect.length(), space.theme?.dots?.size)
        assertEquals(dotExpect.getJSONObject(0).getString("hex"), space.theme?.dots?.get(0)?.color?.hexString)
        assertEquals(dotExpect.getJSONObject(0).getBoolean("isPrimary"), space.theme?.dots?.get(0)?.isPrimary)
        assertEquals(dotExpect.getJSONObject(1).getString("hex"), space.theme?.dots?.get(1)?.color?.hexString)
        assertEquals(dotExpect.getJSONObject(1).getBoolean("isPrimary"), space.theme?.dots?.get(1)?.isPrimary)
        assertEquals(expect.getDouble("opacity"), space.theme?.opacity)
        assertEquals(expect.getDouble("texture"), space.theme?.texture)
    }

    /** Hostile (wire-space-numeric-uuid): a numeric uuid must drop the record. */
    @Test
    fun numericUuidSpaceIsDropped() {
        assertNull(decodeFixtureInput("wire-space-numeric-uuid"))
    }

    // MARK: Tabs

    @Test
    fun tabRecordDecodesWithDefaults() {
        val expect = expectOf("wire-tab-pinned-default")
        val tab = tabRecord("wire-tab-pinned-default")
        assertEquals(expect.getString("tabId"), tab.tabId)
        assertEquals(expect.getString("url"), tab.url)
        assertEquals(expect.getString("title"), tab.title)
        assertEquals(expect.getBoolean("essential"), tab.essential)
        assertEquals(expect.getString("workspaceUuid"), tab.workspaceUuid)
        assertEquals(expectNullableBool(expect, "pinned"), tab.pinned)
        assertEquals(expect.getBoolean("isNormalTab"), tab.isNormalTab)
        assertEquals(expectNullableString(expect, "folderId"), tab.folderId)
    }

    @Test
    fun normalTabRecordDecodesPinnedFalse() {
        val expect = expectOf("wire-tab-normal-pinned-false")
        val tab = tabRecord("wire-tab-normal-pinned-false")
        assertEquals(expect.getString("tabId"), tab.tabId)
        assertEquals(expect.getString("url"), tab.url)
        assertEquals(expect.getBoolean("essential"), tab.essential)
        assertEquals(expect.getBoolean("pinned"), tab.pinned)
        assertEquals(expect.getBoolean("isNormalTab"), tab.isNormalTab)
        assertEquals(expect.getString("workspaceUuid"), tab.workspaceUuid)
    }

    @Test
    fun pinnedStringFlagDecodesTolerantly() {
        val expect = expectOf("wire-tab-pinned-string-false")
        val tab = tabRecord("wire-tab-pinned-string-false")
        assertEquals(expect.getString("tabId"), tab.tabId)
        assertEquals(expect.getString("url"), tab.url)
        assertEquals(expect.getBoolean("pinned"), tab.pinned)
        assertEquals(expect.getBoolean("isNormalTab"), tab.isNormalTab)
    }

    @Test
    fun tabRecordWithoutPinnedFlagStaysPinned() {
        val cleartext = JSONObject(
            """{"id":"tab-p","kind":"tab","data":{"tabId":"tab-p","url":"https://p.de","essential":false}}""",
        )
        val tab = (ZenSpaces.decode("tab-p", cleartext) as? ZenSpaces.DecodedRecord.Tab)?.record
            ?: return kotlin.run { fail("expected tab record") }
        assertNull(tab.pinned)
        assertFalse(tab.isNormalTab)
    }

    // MARK: Folders

    @Test
    fun folderRecordDecodes() {
        val expect = expectOf("wire-folder-basic")
        val folder = folderRecord("wire-folder-basic")
        assertEquals(expect.getString("folderId"), folder.folderId)
        assertEquals(expect.getString("name"), folder.name)
        assertEquals(expectNullableString(expect, "icon"), folder.icon)
        assertEquals(expect.getString("workspaceUuid"), folder.workspaceUuid)
        assertEquals(expectNullableString(expect, "parentFolderId"), folder.parentFolderId)
        assertEquals(expectStrings(expect, "children"), folder.children)
    }

    @Test
    fun folderWithObjectIconFieldStillDecodes() {
        val expect = expectOf("wire-folder-live-object")
        val folder = folderRecord("wire-folder-live-object")
        assertEquals(expect.getString("folderId"), folder.folderId)
        assertEquals(expectStrings(expect, "children"), folder.children)
        assertEquals(expectNullableString(expect, "icon"), folder.icon)
    }

    /** Hostile (wire-folder-missing-folderid): folder-shaped for matching, never a match. */
    @Test
    fun folderMissingFolderIdNeverMatchesTarget() {
        val fixture = FixtureLoader.json("wire-folder-missing-folderid")
        val input = fixture.getJSONObject("input")
        val expect = fixture.getJSONObject("expect")
        // The general decoder drops it (missing required folderId, SPEC §2.5).
        // non-canonical, platform-local (this fixture's expect has no dropped key).
        assertNull(ZenSpaces.decode(input.optString("id"), input))
        val data = input.getJSONObject("data")
        val requests = input.getJSONArray("targetRequests")
        val namedTarget = (0 until requests.length()).mapNotNull { requests.opt(it) as? String }.single()
        assertEquals(expect.getBoolean("matchesNilTarget"), SpacesSyncService.isTargetFolder(null, data))
        assertEquals(expect.getBoolean("matchesFolder1"), SpacesSyncService.isTargetFolder(namedTarget, data))
    }

    // MARK: Splits

    @Test
    fun splitRecordDecodes() {
        val expect = expectOf("wire-split-basic")
        val split = splitRecord("wire-split-basic")
        assertEquals(expect.getString("splitId"), split.splitId)
        assertEquals(expect.getString("gridType"), split.gridType)
        assertEquals(expectStrings(expect, "tabs"), split.tabs)
        assertEquals(expect.getString("workspaceUuid"), split.workspaceUuid)
        assertEquals(expectNullableString(expect, "folderId"), split.folderId)
        assertEquals(expectNullableBool(expect, "pinned"), split.pinned)
        assertEquals(expect.getBoolean("isNormalSplit"), split.isNormalSplit)
    }

    @Test
    fun splitRecordPinnedFalseDecodes() {
        val expect = expectOf("wire-split-normal-pinned-false")
        val split = splitRecord("wire-split-normal-pinned-false")
        assertEquals(expect.getString("splitId"), split.splitId)
        assertEquals(expect.getBoolean("pinned"), split.pinned)
        assertEquals(expect.getBoolean("isNormalSplit"), split.isNormalSplit)
        assertEquals(expectStrings(expect, "tabs"), split.tabs)
        assertEquals(expect.getString("workspaceUuid"), split.workspaceUuid)
    }

    @Test
    fun splitRecordWithoutPinnedFlagStaysPinned() {
        val cleartext = JSONObject(
            """{"id":"split-p","kind":"split","data":{"splitId":"split-p","tabs":["p1","p2"]}}""",
        )
        val split = (ZenSpaces.decode("split-p", cleartext) as? ZenSpaces.DecodedRecord.Split)?.record
            ?: return kotlin.run { fail("expected split record") }
        assertNull(split.pinned)
        assertFalse(split.isNormalSplit)
    }

    // MARK: Layout & dropped records

    @Test
    fun layoutRecordDecodes() {
        val expect = expectOf("wire-layout-basic")
        val layout = (decodeFixtureInput("wire-layout-basic") as? ZenSpaces.DecodedRecord.Layout)?.record
            ?: fail("expected layout record")
        assertEquals(expectStrings(expect, "spaces"), layout.spaces)
        val essentialsExpect = expect.getJSONObject("essentials")
        assertEquals(expectStrings(essentialsExpect, "default"), layout.essentials?.get("default"))
        // Non-string entries (42, null) are dropped, the bucket survives.
        assertEquals(expectStrings(essentialsExpect, "work"), layout.essentials?.get("work"))
    }

    /** wire-ignored-records: every non-decodable cleartext shape is dropped. */
    @Test
    fun unknownKindAndGarbageAreIgnored() {
        val cases = FixtureLoader.cases("wire-ignored-records")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            assertNull(
                "case '${case.getString("id")}' must be dropped",
                ZenSpaces.decode(case.getJSONObject("input").optString("id"), case.getJSONObject("input")),
            )
        }
    }

    /** Deleted-string record still decodes (SPEC §2.1); a real bool tombstone does not. */
    @Test
    fun deletedStringRecordStillDecodes() {
        val tab = tabRecord("wire-deleted-string")
        assertEquals("tab-a", tab.tabId)
        assertEquals("https://example.com", tab.url)
        assertEquals("Example", tab.title)
    }

    // MARK: Synced prefs

    @Test
    fun prefBoolParsesSyncedValueShapes() {
        val service = SpacesSyncService
        val cases = FixtureLoader.cases("wire-prefs-normal-tabs")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val expect = case.getJSONObject("expect")
            val parsed = service.parsePrefBool(case.getJSONObject("input").opt("value"))
            assertEquals(
                "case '${case.getString("id")}'",
                if (expect.isNull("prefBool")) null else expect.getBoolean("prefBool"),
                parsed,
            )
        }
    }

    // MARK: Assembly (no fixture coverage; literals kept)

    @Test
    fun splitAssemblyPlacesMembersAndSkipsUnplaced() {
        val space = ZenSpaces.ZenSpaceRecord(
            uuid = "space-1", name = "Work", icon = null, theme = null,
            containerGuid = null, children = listOf("split-1"),
        )
        fun rec(id: String) = ZenSpaces.ZenTabRecord(
            tabId = id, url = "https://$id.de", title = id.uppercase(), icon = null,
            containerGuid = null, essential = false, workspaceUuid = "space-1",
            folderId = null, staticLabel = null, hasStaticIcon = null, defaultContainer = null,
        )
        val split = ZenSpaces.ZenSplitRecord(
            splitId = "split-1", gridType = "grid",
            tabs = listOf("tab-a", "tab-b"), workspaceUuid = "space-1", folderId = null,
        )

        val built = SpacesSyncService.makeSpace(
            record = space,
            allTabs = mapOf("tab-a" to rec("tab-a"), "tab-b" to rec("tab-b"), "tab-c" to rec("tab-c")),
            folders = emptyMap(),
            splits = mapOf("split-1" to split),
        )

        assertEquals(2, built.pinned.size)
        val first = built.pinned[0] as? ZenSpaces.ZenItem.Split
            ?: return kotlin.run {
            fail("expected split item first")
        }
        assertEquals(listOf("tab-a", "tab-b"), first.split.tabs.map { it.id })
        // Member tabs must NOT appear as individual unplaced tabs; only C remains.
        assertEquals("tab-c", built.pinned[1].id)
    }

    @Test
    fun mixedThemeShapesKeepBothSpaces() {
        val plain = JSONObject(
            """{"id":"a","kind":"space","data":{"uuid":"space-a","name":"A","theme":{"type":"gradient","gradientColors":[],"opacity":0.3},"children":[]}}""",
        )
        val exotic = JSONObject(
            """{"id":"b","kind":"space","data":{"uuid":"space-b","name":"B","theme":{"type":"solid","gradientColors":[{"c":"#fff"}],"opacity":0.7},"children":[]}}""",
        )
        val a = (ZenSpaces.decode("a", plain) as? ZenSpaces.DecodedRecord.Space)?.record ?: kotlin.run { fail("plain must decode") }
        val b = (ZenSpaces.decode("b", exotic) as? ZenSpaces.DecodedRecord.Space)?.record ?: kotlin.run { fail("exotic must decode") }
        assertEquals("space-a", a.uuid)
        assertEquals("space-b", b.uuid)
    }

    @Test
    fun snapshotAssemblyInputs() {
        // Essentials assembly: layout order first, orphans into "default".
        val essentialTab = ZenSpaces.ZenTabRecord(
            tabId = "t1", url = "https://a.de", title = "A",
            icon = null, containerGuid = null, essential = true,
            workspaceUuid = null, folderId = null, staticLabel = null,
            hasStaticIcon = null, defaultContainer = null,
        )
        assertTrue(essentialTab.essential == true)

        val layout = ZenSpaces.ZenLayoutRecord(spaces = listOf("s2"), essentials = mapOf("default" to listOf("t1")))
        val assembled = SpacesSyncService.assembleEssentials(layout, mapOf("t1" to essentialTab))
        assertEquals(1, assembled["default"]?.size)
        assertEquals("A", assembled["default"]?.first()?.title)
    }

    @Test
    fun makeSpacePartitionsPinnedAndNormalInOrder() {
        val space = ZenSpaces.ZenSpaceRecord(
            uuid = "space-1", name = "Work", icon = null, theme = null,
            containerGuid = null, children = listOf("p1", "n1", "p2", "n2"),
        )
        fun rec(id: String, pinned: Boolean?) = ZenSpaces.ZenTabRecord(
            tabId = id, url = "https://$id.de", title = id.uppercase(), icon = null,
            containerGuid = null, essential = false, workspaceUuid = "space-1",
            folderId = null, staticLabel = null, hasStaticIcon = null, defaultContainer = null,
            pinned = pinned,
        )
        val built = SpacesSyncService.makeSpace(
            record = space,
            allTabs = mapOf("p1" to rec("p1", true), "n1" to rec("n1", false), "p2" to rec("p2", null), "n2" to rec("n2", false)),
            folders = emptyMap(),
            splits = emptyMap(),
        )
        assertEquals(listOf("p1", "p2"), built.pinned.map { it.id })
        assertEquals(listOf("n1", "n2"), built.tabs.map { it.id })
    }

    @Test
    fun makeSpaceWithoutNormalTabsKeepsOldBehaviour() {
        val space = ZenSpaces.ZenSpaceRecord(
            uuid = "space-1", name = "Work", icon = null, theme = null,
            containerGuid = null, children = listOf("p1", "p2"),
        )
        fun rec(id: String) = ZenSpaces.ZenTabRecord(
            tabId = id, url = "https://$id.de", title = id.uppercase(), icon = null,
            containerGuid = null, essential = false, workspaceUuid = "space-1",
            folderId = null, staticLabel = null, hasStaticIcon = null, defaultContainer = null,
        )
        val built = SpacesSyncService.makeSpace(
            record = space,
            allTabs = mapOf("p1" to rec("p1"), "p2" to rec("p2")),
            folders = emptyMap(),
            splits = emptyMap(),
        )
        assertEquals(listOf("p1", "p2"), built.pinned.map { it.id })
        assertTrue(built.tabs.isEmpty())
    }

    @Test
    fun makeSpacePartitionsNormalSplit() {
        val space = ZenSpaces.ZenSpaceRecord(
            uuid = "space-1", name = "Work", icon = null, theme = null,
            containerGuid = null, children = listOf("split-p", "split-n"),
        )
        fun rec(id: String, pinned: Boolean?) = ZenSpaces.ZenTabRecord(
            tabId = id, url = "https://$id.de", title = id.uppercase(), icon = null,
            containerGuid = null, essential = false, workspaceUuid = "space-1",
            folderId = null, staticLabel = null, hasStaticIcon = null, defaultContainer = null,
            pinned = pinned,
        )
        val pinnedSplit = ZenSpaces.ZenSplitRecord(
            splitId = "split-p", gridType = "vsep", tabs = listOf("p1", "p2"),
            workspaceUuid = "space-1", folderId = null, pinned = true,
        )
        val normalSplit = ZenSpaces.ZenSplitRecord(
            splitId = "split-n", gridType = "vsep", tabs = listOf("n1", "n2"),
            workspaceUuid = "space-1", folderId = null, pinned = false,
        )
        val built = SpacesSyncService.makeSpace(
            record = space,
            allTabs = mapOf(
                "p1" to rec("p1", true), "p2" to rec("p2", true),
                "n1" to rec("n1", false), "n2" to rec("n2", false),
            ),
            folders = emptyMap(),
            splits = mapOf("split-p" to pinnedSplit, "split-n" to normalSplit),
        )
        assertEquals(listOf("split-p"), built.pinned.map { it.id })
        assertEquals(listOf("split-n"), built.tabs.map { it.id })
        val normal = built.tabs[0] as? ZenSpaces.ZenItem.Split
            ?: return kotlin.run { fail("expected split in normal bucket") }
        assertEquals(listOf("n1", "n2"), normal.split.tabs.map { it.id })
    }
}
