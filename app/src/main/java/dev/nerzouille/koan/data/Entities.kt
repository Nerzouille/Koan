package dev.nerzouille.koan.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Data model from docs/ARCHITECTURE.md. Room is the single source of truth for the UI. */

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    /** Emoji or Zen icon name. */
    val icon: String,
    /** ARGB colours of the Zen theme gradient, top to bottom. Empty = default near-black. */
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
    val folderId: String? = null,
    val url: String,
    val title: String,
    val iconUrl: String? = null,
    val pinned: Boolean = false,
    val essential: Boolean = false,
    val order: Int,
    val lastAccessed: Long = 0,
    val thumbnailKey: String? = null,
    val origin: TabOrigin = TabOrigin.LOCAL,
    val dirty: Boolean = false,
)

@Entity(tableName = "folders", indices = [Index("spaceUuid")])
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val spaceUuid: String,
    val parentFolderId: String? = null,
    val name: String,
    val icon: String? = null,
    val collapsed: Boolean = false,
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
