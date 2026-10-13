package dev.nerzouille.koan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
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

@Dao
interface KoanDao {
    @Query("SELECT * FROM spaces ORDER BY `order`")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM tabs ORDER BY spaceUuid, `order`")
    fun observeTabs(): Flow<List<TabEntity>>

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun spaceCount(): Int

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM tabs WHERE spaceUuid = :spaceUuid")
    suspend fun maxTabOrder(spaceUuid: String): Int

    @Query("SELECT * FROM tabs WHERE spaceUuid = :spaceUuid AND NOT pinned AND NOT essential ORDER BY `order`")
    suspend fun stackTabs(spaceUuid: String): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpaces(spaces: List<SpaceEntity>)

    @Upsert
    suspend fun upsertTabs(tabs: List<TabEntity>)

    @Query("DELETE FROM tabs WHERE koanTabId = :koanTabId")
    suspend fun deleteTab(koanTabId: String)

    @Query("UPDATE tabs SET `order` = :order WHERE koanTabId = :koanTabId")
    suspend fun setTabOrder(koanTabId: String, order: Int)

    @Transaction
    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { i, id -> setTabOrder(id, i) }
    }
}

class Converters {
    @TypeConverter
    fun longsToString(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun stringToLongs(value: String): List<Long> =
        if (value.isEmpty()) emptyList() else value.split(',').map { it.toLong() }

    // Tab ids never contain commas (UUIDs / Zen ids).
    @TypeConverter
    fun stringsToString(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun stringToStrings(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(',')
}

@Database(
    entities = [SpaceEntity::class, TabEntity::class, FolderEntity::class, SplitEntity::class, SyncMetaEntity::class],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class KoanDatabase : RoomDatabase() {
    abstract fun dao(): KoanDao

    companion object {
        fun build(context: Context): KoanDatabase =
            Room.databaseBuilder(context, KoanDatabase::class.java, "koan.db")
                // Pre-M2 data is fake; real migrations start once Zen data lands here.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
