package dev.nerzouille.koan.sync

import dev.nerzouille.koan.data.FolderEntity
import dev.nerzouille.koan.data.SpaceEntity
import dev.nerzouille.koan.data.SplitEntity
import dev.nerzouille.koan.data.TabEntity
import dev.nerzouille.koan.data.TabOrigin
import java.security.MessageDigest

/** Everything one Zen pull contributes to Room, plus the inline favicons to store on disk. */
data class ZenRows(
    val spaces: List<SpaceEntity>,
    val tabs: List<TabEntity>,
    val folders: List<FolderEntity>,
    val splits: List<SplitEntity>,
    /** Icon key → `data:image/…` URL, for tabs whose favicon Zen synced inline. */
    val inlineIcons: Map<String, String>,
)

/**
 * Maps a [ZenSpaces.ZenSnapshot] to Room rows (docs/SYNC.md "Mapping into Koan"). Pure and
 * deterministic: the same snapshot always yields the same rows, so a pull that changed nothing on
 * the desktop writes nothing.
 */
object ZenRoomMapper {
    /**
     * Gap between consecutive items' `order`, so a split's members (≤ 4 in Zen) sort together
     * right after it: member m of item i has order i × STRIDE + m.
     */
    const val ORDER_STRIDE = 8

    const val INLINE_ICON_PREFIX = "inline:"

    /** Room id of a Zen tab. Essentials repeat across spaces, so theirs include the space. */
    fun tabId(zenTabId: String): String = "zen:$zenTabId"
    fun essentialId(spaceUuid: String, zenTabId: String): String = "zen:$spaceUuid:$zenTabId"

    fun map(snapshot: ZenSpaces.ZenSnapshot): ZenRows {
        val tabs = mutableListOf<TabEntity>()
        val folders = mutableListOf<FolderEntity>()
        val splits = mutableListOf<SplitEntity>()
        val icons = linkedMapOf<String, String>()
        // A tab id is unique in Zen; if a broken payload repeats one, the first placement wins.
        val placed = mutableSetOf<String>()

        fun tab(
            t: ZenSpaces.ZenTab,
            space: String,
            order: Int,
            pinned: Boolean,
            essential: Boolean = false,
            folderId: String? = null,
            splitId: String? = null,
        ) {
            val id = if (essential) essentialId(space, t.id) else tabId(t.id)
            if (!placed.add(id)) return
            val icon = t.icon?.trim().orEmpty()
            val static = t.hasStaticIcon == true && icon.isNotEmpty()
            val iconRef = when {
                static -> icon
                icon.startsWith("data:image/") -> inlineIconKey(icon).also { icons[it] = icon }
                else -> null
            }
            tabs += TabEntity(
                koanTabId = id,
                zenTabId = t.id,
                spaceUuid = space,
                folderId = folderId,
                splitId = splitId,
                url = t.url,
                title = t.displayTitle,
                iconUrl = iconRef,
                staticIcon = static,
                pinned = pinned || essential,
                essential = essential,
                order = order,
                zenOrder = order,
                origin = TabOrigin.ZEN,
            )
        }

        fun split(s: ZenSpaces.ZenSplit, space: String, itemIndex: Int, pinned: Boolean, folderId: String? = null) {
            val members = s.tabs.take(ORDER_STRIDE)
            splits += SplitEntity(s.id, space, members.map { tabId(it.id) }, s.gridType.orEmpty())
            members.forEachIndexed { m, t ->
                tab(t, space, itemIndex * ORDER_STRIDE + m, pinned, folderId = folderId, splitId = s.id)
            }
        }

        fun folder(f: ZenSpaces.ZenFolder, space: String, order: Int, parent: String?) {
            folders += FolderEntity(f.id, space, parent, f.name, f.icon, order = order)
            f.tabs.forEachIndexed { i, t -> tab(t, space, i * ORDER_STRIDE, pinned = true, folderId = f.id) }
            val offset = f.tabs.size
            f.subfolders.orEmpty().forEachIndexed { i, sub -> folder(sub, space, (offset + i) * ORDER_STRIDE, f.id) }
        }

        val spaces = snapshot.spaces.mapIndexed { index, space ->
            snapshot.essentialsFor(space).forEachIndexed { i, t ->
                tab(t, space.id, i * ORDER_STRIDE, pinned = true, essential = true)
            }
            space.pinned.forEachIndexed { i, item ->
                when (item) {
                    is ZenSpaces.ZenItem.Tab -> tab(item.tab, space.id, i * ORDER_STRIDE, pinned = true)
                    is ZenSpaces.ZenItem.Folder -> folder(item.folder, space.id, i * ORDER_STRIDE, parent = null)
                    is ZenSpaces.ZenItem.Split -> split(item.split, space.id, i, pinned = true)
                }
            }
            space.tabs.forEachIndexed { i, item ->
                when (item) {
                    is ZenSpaces.ZenItem.Tab -> tab(item.tab, space.id, i * ORDER_STRIDE, pinned = false)
                    is ZenSpaces.ZenItem.Split -> split(item.split, space.id, i, pinned = false)
                    // Zen folders only hold pinned tabs; one in the normal list would be a broken
                    // payload. Keep its tabs reachable in the drawer rather than dropping them.
                    is ZenSpaces.ZenItem.Folder -> folder(item.folder, space.id, (space.pinned.size + i) * ORDER_STRIDE, parent = null)
                }
            }
            SpaceEntity(
                uuid = space.id,
                name = space.name,
                icon = space.icon.orEmpty(),
                gradientColors = gradient(space.theme),
                containerGuid = space.containerGuid,
                order = index,
                zenOrigin = true,
            )
        }
        return ZenRows(spaces, tabs, folders, splits, icons)
    }

    /** Theme dots as ARGB, the primary dot first (Zen draws the primary as the base colour). */
    internal fun gradient(theme: ZenSpaces.ZenSpaceTheme?): List<Long> {
        val dots = theme?.dots.orEmpty()
        if (dots.isEmpty()) return emptyList()
        val primary = dots.indexOfFirst { it.isPrimary }
        val ordered = if (primary > 0) listOf(dots[primary]) + dots.filterIndexed { i, _ -> i != primary } else dots
        return ordered.map { it.color.toArgb().toLong() and 0xFFFFFFFFL }
    }

    /** Stable, short key for an inline icon: the same image always maps to the same file. */
    fun inlineIconKey(dataUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(dataUrl.toByteArray(Charsets.UTF_8))
        return INLINE_ICON_PREFIX + digest.joinToString("") { "%02x".format(it) }
    }
}
