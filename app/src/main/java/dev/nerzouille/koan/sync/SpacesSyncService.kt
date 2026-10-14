// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import dev.nerzouille.koan.favicon.FaviconResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI

/**
 * Read half of Z-Sync's `SpacesSyncService`: pulls the `spaces` and `prefs` collections and builds a
 * [ZenSpaces.ZenSnapshot]. Koan ports no write path before M6 (CLAUDE.md rule 4), so nothing here
 * can modify the owner's desktop Spaces.
 */
object SpacesSyncService {
    const val collection = "spaces"

    /** Firefox Sync collection holding the synced `prefs` map (one record). */
    const val PREFS_COLLECTION = "prefs"

    /** Desktop setting "Include unpinned tabs" (`zen.spaces-sync.normal-tabs`). */
    const val NORMAL_TABS_PREF_KEY = "zen.spaces-sync.normal-tabs"

    /**
     * Desktop setting "Enable container-specific essentials". Not in Zen's
     * synced-prefs list today, so callers must tolerate it being absent.
     */
    const val SEPARATE_ESSENTIALS_PREF_KEY = "zen.workspaces.separate-essentials"

    // MARK: - Read

    suspend fun loadSnapshot(client: SyncClient): ZenSpaces.ZenSnapshot = withContext(Dispatchers.IO) {
        val prefs = syncedPrefs(client)
        val spaceRecords = client.getRecords(collection)

        val spacesById = linkedMapOf<String, ZenSpaces.ZenSpaceRecord>()
        val tabsById = linkedMapOf<String, ZenSpaces.ZenTabRecord>()
        val foldersById = linkedMapOf<String, ZenSpaces.ZenFolderRecord>()
        val splitsById = linkedMapOf<String, ZenSpaces.ZenSplitRecord>()
        var layout: ZenSpaces.ZenLayoutRecord? = null
        var decryptFailures = 0
        var skippedKinds = 0
        var gatedNormalItems = 0
        var observedNormalItems = 0

        for (record in spaceRecords) {
            val id = record.optString("id")
            if (id.isEmpty()) continue
            val cleartext = try {
                client.decryptRecord(collection, record)
            } catch (_: Exception) {
                decryptFailures++
                continue
            }
            if ((cleartext.opt("deleted") as? Boolean) == true) continue
            when (val decoded = ZenSpaces.decode(id, cleartext)) {
                is ZenSpaces.DecodedRecord.Space -> spacesById[decoded.record.uuid] = decoded.record
                is ZenSpaces.DecodedRecord.Tab -> {
                    // Records linger server-side after the option is turned off
                    // (held back, not tombstoned). Hide them when the synced
                    // pref says the option is off.
                    val tabRecord = decoded.record
                    if (tabRecord.pinned == false) observedNormalItems++
                    if (prefs.normalTabs || tabRecord.pinned != false) {
                        tabsById[tabRecord.tabId] = tabRecord
                    } else {
                        gatedNormalItems++
                    }
                }
                is ZenSpaces.DecodedRecord.Folder -> foldersById[decoded.record.folderId] = decoded.record
                is ZenSpaces.DecodedRecord.Split -> {
                    // Split records carry the same opt-in flag (from their
                    // first member); gate them like normal tab records.
                    val splitRecord = decoded.record
                    if (splitRecord.pinned == false) observedNormalItems++
                    if (prefs.normalTabs || splitRecord.pinned != false) {
                        splitsById[splitRecord.splitId] = splitRecord
                    } else {
                        gatedNormalItems++
                    }
                }
                is ZenSpaces.DecodedRecord.Layout -> layout = decoded.record
                null -> skippedKinds++
            }
        }

        val order = layout?.spaces ?: spacesById.keys.toList()
        val seen = mutableSetOf<String>()
        val orderedIds = order.filter { spacesById.containsKey(it) && seen.add(it) }
        val unorderedIds = spacesById.keys.sorted().filter { !seen.contains(it) }

        val spaces = (orderedIds + unorderedIds).map { id ->
            makeSpace(spacesById.getValue(id), allTabs = tabsById, folders = foldersById, splits = splitsById)
        }

        val essentials = assembleEssentials(layout, tabsById)

        android.util.Log.i(
            "SpacesSync",
            loadLogSummary(
                recordCount = spaceRecords.size,
                spaces = spaces.size,
                tabs = tabsById.size,
                folders = foldersById.size,
                splits = splitsById.size,
                essentials = essentials.values.sumOf { it.size },
                normalTabsOn = prefs.normalTabs,
                separateEssentials = prefs.separateEssentials,
                decryptFailures = decryptFailures,
                skippedKinds = skippedKinds,
                gatedNormalItems = gatedNormalItems,
            ),
        )

        // A normal record observed in `spaces` proves the browser supports
        // normal-tab sync (records linger after the option is turned off), so
        // `absent` with observed normal items is honestly `disabled`.
        val capability = if (
            prefs.normalTabsCapability == ZenSpaces.NormalTabsCapability.ABSENT &&
            observedNormalItems > 0
        ) {
            ZenSpaces.NormalTabsCapability.DISABLED
        } else {
            prefs.normalTabsCapability
        }

        ZenSpaces.ZenSnapshot(
            spaces = spaces,
            essentials = essentials,
            fetchedAtMillis = System.currentTimeMillis(),
            separateEssentialsPref = prefs.separateEssentials,
            normalTabsCapability = capability,
        )
    }

    /** The Zen preferences this app consumes from the synced `prefs` record. */
    internal data class SyncedPrefs(
        /** "Include unpinned tabs" (`zen.spaces-sync.normal-tabs`), default true. */
        val normalTabs: Boolean = true,
        /**
         * "Enable container-specific essentials" when Zen synced it; null when
         * absent (Zen does not mark it for sync today).
         */
        val separateEssentials: Boolean? = null,
        /**
         * Tri-state write gate (`wire-prefs-normal-tabs-capability`); does not
         * change [normalTabs], which keeps the display read default.
         */
        val normalTabsCapability: ZenSpaces.NormalTabsCapability = ZenSpaces.NormalTabsCapability.ABSENT,
    )

    /**
     * Reads the synced `prefs` record once and parses every pref this app
     * consumes. Unreadable records keep the defaults: a present `pinned:false`
     * record still renders, and essentials fall back to inference.
     */
    internal fun syncedPrefs(client: SyncClient): SyncedPrefs {
        var prefs = SyncedPrefs()
        val records = try {
            client.getRecords(PREFS_COLLECTION)
        } catch (_: Exception) {
            return prefs
        }
        for (record in records) {
            val cleartext = try {
                client.decryptRecord(PREFS_COLLECTION, record)
            } catch (_: Exception) {
                continue
            }
            val values = cleartext.optJSONObject("value") ?: continue
            // Last key-bearing record wins; records without the key never
            // downgrade a capability a readable record already proved.
            val derived = deriveNormalTabsCapability(values)
            if (derived != ZenSpaces.NormalTabsCapability.ABSENT) {
                prefs = prefs.copy(normalTabsCapability = derived)
            }
            parsePrefBool(values.opt(NORMAL_TABS_PREF_KEY))?.let { prefs = prefs.copy(normalTabs = it) }
            parsePrefBool(values.opt(SEPARATE_ESSENTIALS_PREF_KEY))?.let { prefs = prefs.copy(separateEssentials = it) }
        }
        return prefs
    }

    /**
     * Derives the normal-tabs write capability from one decrypted prefs
     * `value` map. Null means no readable `value` object was seen; a present
     * key that does not parse true is [ZenSpaces.NormalTabsCapability.DISABLED].
     */
    internal fun deriveNormalTabsCapability(values: JSONObject?): ZenSpaces.NormalTabsCapability = when {
        values == null -> ZenSpaces.NormalTabsCapability.ABSENT
        !values.has(NORMAL_TABS_PREF_KEY) -> ZenSpaces.NormalTabsCapability.ABSENT
        parsePrefBool(values.opt(NORMAL_TABS_PREF_KEY)) == true -> ZenSpaces.NormalTabsCapability.ENABLED
        else -> ZenSpaces.NormalTabsCapability.DISABLED
    }

    /** Convenience for the normal-tabs flag alone (legacy call sites/tests). */
    internal fun normalTabsEnabled(client: SyncClient): Boolean = syncedPrefs(client).normalTabs

    /**
     * D2 target-folder matching (SPEC §3.5): a nil/empty request never
     * matches a folder, and a candidate matches only when its `data.folderId`
     * is a strict non-empty JSON string exactly equal to the requested id —
     * never via `optString`'s number/bool coercion.
     */
    internal fun isTargetFolder(folderId: String?, data: JSONObject): Boolean =
        !folderId.isNullOrEmpty() && ZenSpaces.optStringOrNull(data, "folderId") == folderId

    /**
     * One-line read summary matching the iOS fields: record count, decrypt
     * failures, ignored records, the per-kind buckets, the synced prefs and how
     * many normal items were gated by the normal-tabs pref.
     */
    internal fun loadLogSummary(
        recordCount: Int,
        spaces: Int,
        tabs: Int,
        folders: Int,
        splits: Int,
        essentials: Int,
        normalTabsOn: Boolean,
        separateEssentials: Boolean?,
        decryptFailures: Int,
        skippedKinds: Int,
        gatedNormalItems: Int,
    ): String =
        "spaces sync: $recordCount records, $decryptFailures decrypt failures, $skippedKinds ignored → " +
            "spaces: $spaces, tabs: $tabs, folders: $folders, splits: $splits, essentials: $essentials, " +
            "normal-tabs pref: $normalTabsOn, separate-essentials pref: $separateEssentials, " +
            "gated normal items: $gatedNormalItems"

    /** Tolerant bool parse for a synced pref value (JSON bool, number or string). */
    internal fun parsePrefBool(raw: Any?): Boolean? = when (raw) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> when (raw.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
        else -> null
    }

    /** Essentials grouped by container bucket ("default" for tabs without one). */
    internal fun assembleEssentials(
        layout: ZenSpaces.ZenLayoutRecord?,
        allTabs: Map<String, ZenSpaces.ZenTabRecord>,
    ): Map<String, List<ZenSpaces.ZenTab>> {
        val byBucket = linkedMapOf<String, MutableList<ZenSpaces.ZenTab>>()
        val placed = mutableSetOf<String>()
        for ((bucket, ids) in layout?.essentials ?: emptyMap()) {
            val tabs = ids.mapNotNull { id ->
                val record = allTabs[id] ?: return@mapNotNull null
                if (record.essential != true) return@mapNotNull null
                makeTab(record)
            }
            if (tabs.isNotEmpty()) {
                byBucket.getOrPut(bucket) { mutableListOf() }.addAll(tabs)
                placed.addAll(tabs.map { it.id })
            }
        }
        val orphans = allTabs.values
            .filter { it.essential == true && !placed.contains(it.tabId) }
            .mapNotNull { makeTab(it) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        if (orphans.isNotEmpty()) {
            byBucket.getOrPut("default") { mutableListOf() }.addAll(orphans)
        }
        return byBucket
    }

    internal fun makeSpace(
        record: ZenSpaces.ZenSpaceRecord,
        allTabs: Map<String, ZenSpaces.ZenTabRecord>,
        folders: Map<String, ZenSpaces.ZenFolderRecord>,
        splits: Map<String, ZenSpaces.ZenSplitRecord> = emptyMap(),
    ): ZenSpaces.ZenSpace {
        val items = buildItems(
            ids = record.children ?: emptyList(),
            allTabs = allTabs,
            folders = folders,
            splits = splits,
            topLevel = true,
            depth = 0,
        )
        // Partition by the record's wire `pinned` flag, keeping the synced
        // `children` order inside each bucket. Folders stay pinned; splits go
        // with their members (a split of normal tabs reports `pinned: false`).
        val pinnedItems = mutableListOf<ZenSpaces.ZenItem>()
        val normalItems = mutableListOf<ZenSpaces.ZenItem>()
        for (item in items) {
            val isNormal = when (item) {
                is ZenSpaces.ZenItem.Tab -> allTabs[item.tab.id]?.isNormalTab == true
                is ZenSpaces.ZenItem.Split -> splits[item.split.id]?.isNormalSplit == true
                else -> false
            }
            if (isNormal) normalItems.add(item) else pinnedItems.add(item)
        }
        // Also capture tabs assigned to this space that aren't yet in children.
        val placedTabIds = buildSet {
            for (item in items) {
                when (item) {
                    is ZenSpaces.ZenItem.Tab -> add(item.tab.id)
                    is ZenSpaces.ZenItem.Folder -> item.folder.tabs.forEach { add(it.id) }
                    is ZenSpaces.ZenItem.Split -> item.split.tabs.forEach { add(it.id) }
                }
            }
        }
        val unplacedRecords = allTabs.values.filter {
            it.workspaceUuid == record.uuid && it.essential != true &&
                it.folderId == null && !placedTabIds.contains(it.tabId) &&
                !FaviconResolver.isLocalURL(it.url)
        }
        fun sortedTabItems(records: List<ZenSpaces.ZenTabRecord>): List<ZenSpaces.ZenItem> =
            records
                .mapNotNull { makeTab(it) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                .map { ZenSpaces.ZenItem.Tab(it) }
        pinnedItems.addAll(sortedTabItems(unplacedRecords.filter { !it.isNormalTab }))
        normalItems.addAll(sortedTabItems(unplacedRecords.filter { it.isNormalTab }))

        return ZenSpaces.ZenSpace(
            id = record.uuid,
            name = record.name ?: "",
            icon = record.icon?.takeIf { it.isNotEmpty() },
            containerGuid = record.containerGuid,
            theme = record.theme,
            pinned = pinnedItems,
            tabs = normalItems,
        )
    }

    /** Walks child ids in synced order; top-level folder members are skipped.
     *  Folders nested under another folder render inside it, not here. */
    internal fun buildItems(
        ids: List<String>,
        allTabs: Map<String, ZenSpaces.ZenTabRecord>,
        folders: Map<String, ZenSpaces.ZenFolderRecord>,
        splits: Map<String, ZenSpaces.ZenSplitRecord> = emptyMap(),
        topLevel: Boolean,
        depth: Int,
    ): List<ZenSpaces.ZenItem> {
        if (depth >= 4) return emptyList()
        val items = mutableListOf<ZenSpaces.ZenItem>()
        for (id in ids) {
            val tabRecord = allTabs[id]
            if (tabRecord != null) {
                if (topLevel && tabRecord.folderId != null) continue
                makeTab(tabRecord)?.let { items.add(ZenSpaces.ZenItem.Tab(it)) }
            } else {
                val folderRecord = folders[id]
                if (folderRecord != null) {
                    if (topLevel) {
                        val parent = folderRecord.parentFolderId
                        if (!parent.isNullOrEmpty() && folders.containsKey(parent)) continue
                    }
                    items.add(ZenSpaces.ZenItem.Folder(makeFolderTree(folderRecord, allTabs, folders, splits, depth)))
                } else {
                    val splitRecord = splits[id]
                    if (splitRecord != null) {
                        val members = (splitRecord.tabs ?: emptyList())
                            .mapNotNull { allTabs[it] }
                            .mapNotNull { makeTab(it) }
                        if (members.size >= 2) {
                            items.add(
                                ZenSpaces.ZenItem.Split(
                                    ZenSpaces.ZenSplit(
                                        id = splitRecord.splitId,
                                        gridType = splitRecord.gridType,
                                        tabs = members,
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
        return items
    }

    /**
     * Builds a folder and its nested sub-folders. Children come from the
     * folder's `children` ids; folders only linked via `parentFolderId` and
     * tabs only linked via `folderId` are picked up too, so partial sync
     * payloads still render completely. Mirrors the Swift `makeFolderTree`.
     */
    private fun makeFolderTree(
        folder: ZenSpaces.ZenFolderRecord,
        allTabs: Map<String, ZenSpaces.ZenTabRecord>,
        folders: Map<String, ZenSpaces.ZenFolderRecord>,
        splits: Map<String, ZenSpaces.ZenSplitRecord>,
        depth: Int,
    ): ZenSpaces.ZenFolder {
        val tabs = mutableListOf<ZenSpaces.ZenTab>()
        val subfolders = mutableListOf<ZenSpaces.ZenFolder>()
        val placed = mutableSetOf<String>()

        if (depth < 4) {
            for (childId in folder.children ?: emptyList()) {
                val tabRecord = allTabs[childId]
                if (tabRecord != null) {
                    makeTab(tabRecord)?.let {
                        tabs.add(it)
                        placed.add(childId)
                    }
                } else {
                    val childFolder = folders[childId]
                    if (childFolder != null && childFolder.folderId != folder.folderId) {
                        subfolders.add(makeFolderTree(childFolder, allTabs, folders, splits, depth + 1))
                        placed.add(childId)
                    }
                }
            }
            // Sub-folders assigned only via parentFolderId.
            folders.values
                .filter { it.parentFolderId == folder.folderId && !placed.contains(it.folderId) }
                .sortedBy { it.folderId }
                .forEach { subfolders.add(makeFolderTree(it, allTabs, folders, splits, depth + 1)) }
            // Tabs assigned via folderId but missing from children.
            allTabs.values
                .filter { it.folderId == folder.folderId && !placed.contains(it.tabId) }
                .mapNotNull { makeTab(it) }
                .forEach { tabs.add(it) }
        }

        return ZenSpaces.ZenFolder(
            id = folder.folderId,
            name = folder.name ?: "",
            icon = folder.icon?.takeIf { it.isNotEmpty() },
            tabs = tabs,
            subfolders = subfolders.takeIf { it.isNotEmpty() },
        )
    }

    internal fun makeTab(record: ZenSpaces.ZenTabRecord): ZenSpaces.ZenTab? {
        val rawURL = record.url.trim()
        if (rawURL.isEmpty()) return null
        if (!hasScheme(rawURL)) return null
        return ZenSpaces.ZenTab(
            id = record.tabId,
            url = rawURL,
            title = record.title ?: "",
            iconURL = null,
            icon = record.icon,
            hasStaticIcon = record.hasStaticIcon,
            staticLabel = normalizedStaticLabel(record.staticLabel),
        )
    }

    /** Mirrors Swift's `URL(string:)` scheme presence check closely enough for sync data. */
    internal fun hasScheme(urlString: String): Boolean = runCatching {
        val uri = URI(urlString)
        uri.scheme != null && uri.scheme.isNotEmpty()
    }.getOrDefault(false)

    internal fun normalizedStaticLabel(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        return trimmed.ifEmpty { null }
    }
}
