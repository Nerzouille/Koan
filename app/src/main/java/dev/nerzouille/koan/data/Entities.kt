package dev.nerzouille.koan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Data model from docs/ARCHITECTURE.md. Room is the single source of truth for the UI. */

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    /** Zen icon: emoji, emoji-as-SVG data URL or `chrome://…/selectable/<name>.svg`. Empty = none. */
    val icon: String,
    /** ARGB colours of the Zen theme gradient, primary first. Empty = default near-black. */
    val gradientColors: List<Long>,
    val containerGuid: String? = null,
    val order: Int,
    val zenOrigin: Boolean = false,
)

enum class TabOrigin { ZEN, LOCAL }

@Entity(
    tableName = "tabs",
    indices = [Index("spaceUuid"), Index("zenTabId")],
)
data class TabEntity(
    @PrimaryKey val koanTabId: String,
    val zenTabId: String? = null,
    val spaceUuid: String,
    /** Pinned tabs only: the folder holding this row in the drawer. */
    val folderId: String? = null,
    /** Member of a Zen split view: the split is one card, its members never get their own. */
    val splitId: String? = null,
    val url: String,
    val title: String,
    /**
     * Favicon reference: a key in [dev.nerzouille.koan.ui.icons.IconStore] for icons Zen synced
     * inline, or the raw Zen static icon when [staticIcon]. Null = fetch the site's favicon.
     */
    val iconUrl: String? = null,
    /** [iconUrl] is a Zen static icon (emoji or selectable SVG), not a favicon. */
    val staticIcon: Boolean = false,
    val pinned: Boolean = false,
    val essential: Boolean = false,
    /** Position in its list: the stack, the drawer's pinned list (shared with folders) or a folder. */
    val order: Int,
    /** [order] as Zen sent it on the last pull; -1 for phone-only tabs. See SpacesSync. */
    val zenOrder: Int = -1,
    val lastAccessed: Long = 0,
    val thumbnailKey: String? = null,
    val origin: TabOrigin = TabOrigin.LOCAL,
    /** Navigated on the phone: its url/title/icon are the phone's, not Zen's (see [zenUrl]). */
    val dirty: Boolean = false,
    /**
     * Closed on the phone but still open in Zen (no write-back before M6): hidden here, kept so a
     * sync doesn't bring it back. Dropped once Zen closes it too.
     */
    val closed: Boolean = false,
    /**
     * Pinned/essential tabs only: opened on the phone, so it also has a card in the stack, at this
     * position (same scale as normal tabs' [order]). Null = only its drawer row.
     */
    val cardOrder: Int? = null,
    /** The url Zen sent on the last pull; a pull only overrides phone navigation when Zen's changed. */
    val zenUrl: String? = null,
) {
    /** Shown as a card in the stack: a normal tab, or a pinned/essential tab opened on the phone. */
    val inStack: Boolean get() = !pinned && !essential || cardOrder != null

    /** Position in the stack (only meaningful when [inStack]). */
    val stackOrder: Int get() = cardOrder ?: order
}

@Entity(tableName = "folders", indices = [Index("spaceUuid")])
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val spaceUuid: String,
    val parentFolderId: String? = null,
    val name: String,
    val icon: String? = null,
    val collapsed: Boolean = false,
    /** Position among its siblings: top-level pinned rows, or the parent folder's children. */
    val order: Int,
)

@Entity(tableName = "splits", indices = [Index("spaceUuid")])
data class SplitEntity(
    @PrimaryKey val splitId: String,
    val spaceUuid: String,
    val tabIds: List<String>,
    val gridType: String,
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val collection: String,
    val lastModified: Long,
    val lastSyncAt: Long,
)

/** A page bookmarked on the phone (M5: local only, never synced). One row per url. */
@Entity(tableName = "bookmarks", indices = [Index(value = ["url"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val createdAt: Long,
)

/** A page visited on the phone, or in the account's history (read-only copy). One row per url. */
@Entity(tableName = "visits", indices = [Index("lastVisit")])
data class VisitEntity(
    @PrimaryKey val url: String,
    val title: String,
    val lastVisit: Long,
    val visitCount: Int,
    /** First known from the Mozilla account's history (removed on sign-out), not visited here. */
    @ColumnInfo(defaultValue = "0") val fromSync: Boolean = false,
    /** Removed from History on the phone while still in the account's history: kept so a sync doesn't bring it back. */
    @ColumnInfo(defaultValue = "0") val hidden: Boolean = false,
)

/**
 * A saved login. Only where it is used is readable; the username and password are sealed
 * together ([secret], Android Keystore AES-GCM, see LoginStore). [local]: saved on this phone,
 * never synced (M5); otherwise a read-only copy of the Mozilla account's.
 */
@Entity(tableName = "logins", indices = [Index("origin")])
data class LoginEntity(
    @PrimaryKey val guid: String,
    /** "https://example.com": the site the login is for. */
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val usernameField: String,
    val passwordField: String,
    val secret: ByteArray,
    val timeCreated: Long,
    val timeLastUsed: Long,
    val timePasswordChanged: Long,
    val local: Boolean,
) {
    override fun equals(other: Any?) = other is LoginEntity && other.guid == guid && other.secret.contentEquals(secret) &&
        other.origin == origin && other.timeLastUsed == timeLastUsed && other.timePasswordChanged == timePasswordChanged

    override fun hashCode() = guid.hashCode()
}
