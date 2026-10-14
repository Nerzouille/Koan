package dev.nerzouille.koan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Dao
interface KoanDao {
    @Query("SELECT * FROM spaces ORDER BY `order`, uuid")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    // koanTabId breaks ties so equal orders (local reorder vs a new Zen tab) never swap between emissions.
    @Query("SELECT * FROM tabs ORDER BY spaceUuid, `order`, koanTabId")
    fun observeTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM folders ORDER BY spaceUuid, `order`, folderId")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM spaces")
    suspend fun spaces(): List<SpaceEntity>

    @Query("SELECT * FROM tabs")
    suspend fun tabs(): List<TabEntity>

    @Query("SELECT * FROM folders")
    suspend fun folders(): List<FolderEntity>

    @Query("SELECT * FROM splits")
    suspend fun splits(): List<SplitEntity>

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM tabs WHERE spaceUuid = :spaceUuid")
    suspend fun maxTabOrder(spaceUuid: String): Int

    @Query("SELECT * FROM tabs WHERE spaceUuid = :spaceUuid AND NOT pinned AND NOT essential AND NOT closed ORDER BY `order`, koanTabId")
    suspend fun stackTabs(spaceUuid: String): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpaces(spaces: List<SpaceEntity>)

    @Upsert
    suspend fun upsertSpaces(spaces: List<SpaceEntity>)

    @Upsert
    suspend fun upsertTabs(tabs: List<TabEntity>)

    @Upsert
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Upsert
    suspend fun upsertSplits(splits: List<SplitEntity>)

    @Delete
    suspend fun deleteSpaces(spaces: List<SpaceEntity>)

    @Delete
    suspend fun deleteTabs(tabs: List<TabEntity>)

    @Delete
    suspend fun deleteFolders(folders: List<FolderEntity>)

    @Delete
    suspend fun deleteSplits(splits: List<SplitEntity>)

    @Query("DELETE FROM tabs WHERE koanTabId = :koanTabId")
    suspend fun deleteTab(koanTabId: String)

    /** Phone tabs go; Zen tabs are only hidden (see TabEntity.closed). */
    @Transaction
    suspend fun closeTabs(ids: List<String>) {
        markZenClosed(ids)
        deleteLocal(ids)
    }

    @Query("UPDATE tabs SET closed = 1 WHERE koanTabId IN (:ids) AND origin = 'ZEN'")
    suspend fun markZenClosed(ids: List<String>)

    @Query("DELETE FROM tabs WHERE koanTabId IN (:ids) AND origin = 'LOCAL'")
    suspend fun deleteLocal(ids: List<String>)

    @Query("DELETE FROM tabs WHERE koanTabId LIKE :prefix || '%'")
    suspend fun deleteTabsWithPrefix(prefix: String)

    @Query("UPDATE tabs SET `order` = :order WHERE koanTabId = :koanTabId")
    suspend fun setTabOrder(koanTabId: String, order: Int)

    @Query("UPDATE folders SET collapsed = :collapsed WHERE folderId = :folderId")
    suspend fun setFolderCollapsed(folderId: String, collapsed: Boolean)

    @Query("SELECT * FROM sync_meta WHERE collection = :collection")
    suspend fun syncMeta(collection: String): SyncMetaEntity?

    @Upsert
    suspend fun upsertSyncMeta(meta: SyncMetaEntity)

    /** [ordered] = (koanTabId, order) pairs written in one transaction. */
    @Transaction
    suspend fun setTabOrders(ordered: List<Pair<String, Int>>) {
        ordered.forEach { (id, order) -> setTabOrder(id, order) }
    }

    /** Applies one Zen pull, already diffed against Room (see SpacesSync), atomically. */
    @Transaction
    suspend fun applyZenDiff(diff: ZenDiff) {
        if (diff.spacesToUpsert.isNotEmpty()) upsertSpaces(diff.spacesToUpsert)
        if (diff.tabsToUpsert.isNotEmpty()) upsertTabs(diff.tabsToUpsert)
        if (diff.foldersToUpsert.isNotEmpty()) upsertFolders(diff.foldersToUpsert)
        if (diff.splitsToUpsert.isNotEmpty()) upsertSplits(diff.splitsToUpsert)
        if (diff.tabsToDelete.isNotEmpty()) deleteTabs(diff.tabsToDelete)
        if (diff.foldersToDelete.isNotEmpty()) deleteFolders(diff.foldersToDelete)
        if (diff.splitsToDelete.isNotEmpty()) deleteSplits(diff.splitsToDelete)
        if (diff.spacesToDelete.isNotEmpty()) deleteSpaces(diff.spacesToDelete)
    }

    /** Sign-out: every row that came from Zen goes; phone-only tabs move to [fallbackSpace]. */
    @Transaction
    suspend fun dropZenData(fallbackSpace: SpaceEntity) {
        upsertSpaces(listOf(fallbackSpace))
        upsertTabs(tabs().filter { it.origin == TabOrigin.LOCAL && it.spaceUuid != fallbackSpace.uuid }.map { it.copy(spaceUuid = fallbackSpace.uuid) })
        deleteTabs(tabs().filter { it.origin == TabOrigin.ZEN })
        deleteFolders(folders())
        deleteSplits(splits())
        deleteSpaces(spaces().filter { it.zenOrigin })
        upsertSyncMeta(SyncMetaEntity("spaces", lastModified = 0, lastSyncAt = 0))
    }
}

class Converters {
    @TypeConverter
    fun longsToString(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun stringToLongs(value: String): List<Long> =
        if (value.isEmpty()) emptyList() else value.split(',').map { it.toLong() }

    // JSON, not a joined string: Zen ids are opaque and may contain any character.
    @TypeConverter
    fun stringsToString(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun stringToStrings(value: String): List<String> =
        JSONArray(value).let { a -> List(a.length()) { a.getString(it) } }
}

@Database(
    entities = [SpaceEntity::class, TabEntity::class, FolderEntity::class, SplitEntity::class, SyncMetaEntity::class],
    version = 2,
)
@TypeConverters(Converters::class)
abstract class KoanDatabase : RoomDatabase() {
    abstract fun dao(): KoanDao

    companion object {
        fun build(context: Context): KoanDatabase =
            Room.databaseBuilder(context, KoanDatabase::class.java, "koan.db")
                // v1 only ever held M1's fake data. From v2 on, rows are real Zen data and phone
                // tabs: every schema change needs a real Migration.
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
                .build()
    }
}
