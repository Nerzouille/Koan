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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.nerzouille.koan.search.HistoryFilter
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

    @Query("SELECT * FROM tabs WHERE koanTabId = :koanTabId")
    suspend fun tab(koanTabId: String): TabEntity?

    /** Stack position of the space's last card (normal tabs and live pinned ones), -1 if none. */
    @Query(
        "SELECT COALESCE(MAX(COALESCE(cardOrder, `order`)), -1) FROM tabs " +
            "WHERE spaceUuid = :spaceUuid AND NOT closed AND ((NOT pinned AND NOT essential) OR cardOrder IS NOT NULL)",
    )
    suspend fun maxStackOrder(spaceUuid: String): Int

    @Query("UPDATE tabs SET `order` = `order` + :by WHERE spaceUuid = :spaceUuid AND NOT pinned AND NOT essential AND `order` > :after")
    suspend fun shiftStackOrders(spaceUuid: String, after: Int, by: Int)

    @Query("UPDATE tabs SET cardOrder = cardOrder + :by WHERE spaceUuid = :spaceUuid AND cardOrder > :after")
    suspend fun shiftCardOrders(spaceUuid: String, after: Int, by: Int)

    /** Inserts [tab] into its space's stack right after position [after] (the card in front of it). */
    @Transaction
    suspend fun insertInStack(tab: TabEntity, after: Int) {
        shiftStackOrders(tab.spaceUuid, after, 1)
        shiftCardOrders(tab.spaceUuid, after, 1)
        upsertTabs(listOf(if (tab.pinned || tab.essential) tab.copy(cardOrder = after + 1) else tab.copy(order = after + 1)))
    }

    @Query("UPDATE tabs SET cardOrder = :cardOrder WHERE koanTabId = :koanTabId")
    suspend fun setCardOrder(koanTabId: String, cardOrder: Int?)

    /** What the engine reports about a page; [dirty] marks it as navigated on the phone. */
    @Query(
        "UPDATE tabs SET url = :url, title = :title, iconUrl = :iconUrl, staticIcon = :staticIcon, dirty = 1 " +
            "WHERE koanTabId = :koanTabId",
    )
    suspend fun setPage(koanTabId: String, url: String, title: String, iconUrl: String?, staticIcon: Boolean)

    @Query("UPDATE tabs SET lastAccessed = :at WHERE koanTabId = :koanTabId")
    suspend fun setLastAccessed(koanTabId: String, at: Long)

    @Query("UPDATE tabs SET thumbnailKey = :key WHERE koanTabId = :koanTabId")
    suspend fun setThumbnail(koanTabId: String, key: String?)

    @Query("UPDATE tabs SET thumbnailKey = NULL WHERE thumbnailKey IN (:keys)")
    suspend fun clearThumbnails(keys: List<String>)

    /** New tabs that never got a url (the app died with the search sheet open). */
    @Query("DELETE FROM tabs WHERE url = '' AND origin = 'LOCAL'")
    suspend fun deleteBlankTabs()

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

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM tabs WHERE spaceUuid = :spaceUuid AND essential")
    suspend fun maxEssentialOrder(spaceUuid: String): Int

    /** A tab becomes an essential (or stops being one) in place; [order] is its new list position. */
    @Query("UPDATE tabs SET essential = :essential, `order` = :order, cardOrder = :cardOrder WHERE koanTabId = :koanTabId")
    suspend fun setEssential(koanTabId: String, essential: Boolean, order: Int, cardOrder: Int?)

    /** Pages whose url or title contains [q], most used first. */
    @Query(
        "SELECT * FROM visits WHERE NOT hidden AND (url LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%') " +
            "ORDER BY visitCount DESC, lastVisit DESC LIMIT :limit",
    )
    suspend fun searchVisits(q: String, limit: Int): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE NOT hidden ORDER BY lastVisit DESC LIMIT :limit")
    suspend fun recentVisits(limit: Int): List<VisitEntity>

    /** The History sheet: newest first; [q] (may be empty) narrows on the url or the title. */
    @Query(
        "SELECT * FROM visits WHERE NOT hidden AND (:q = '' OR url LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%') " +
            "ORDER BY lastVisit DESC LIMIT :limit",
    )
    fun observeHistory(q: String, limit: Int): Flow<List<VisitEntity>>

    /** Which of [urls] were visited (visited links). */
    @Query("SELECT url FROM visits WHERE url IN (:urls) AND NOT hidden")
    suspend fun visitedAmong(urls: List<String>): List<String>

    /** Removed from History: the phone's own visit goes; a synced one is only hidden here (no write-back). */
    @Transaction
    suspend fun removeVisit(url: String) {
        deleteLocalVisit(url)
        hideSyncedVisit(url)
    }

    @Query("DELETE FROM visits WHERE url = :url AND NOT fromSync")
    suspend fun deleteLocalVisit(url: String)

    /** Undo of [removeVisit]: the row as it was, shown again. */
    suspend fun restoreVisit(visit: VisitEntity) = replaceVisit(visit.copy(hidden = false))

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceVisit(visit: VisitEntity)

    @Query("UPDATE visits SET hidden = 1 WHERE url = :url AND fromSync")
    suspend fun hideSyncedVisit(url: String)

    /** Clear browsing data: the phone's history goes, the synced history is hidden. */
    @Transaction
    suspend fun clearHistory() {
        deleteAllLocalVisits()
        hideAllSyncedVisits()
        deleteClosedTabs()
    }

    @Query("DELETE FROM visits WHERE NOT fromSync")
    suspend fun deleteAllLocalVisits()

    // ---- recently closed tabs ----

    @Query("SELECT * FROM closed_tabs ORDER BY closedAt DESC")
    fun observeClosedTabs(): Flow<List<ClosedTabEntity>>

    @Query("SELECT * FROM closed_tabs WHERE koanTabId = :koanTabId")
    suspend fun closedTab(koanTabId: String): ClosedTabEntity?

    /** Records [closed] and drops what falls beyond the newest [keep]; returns the dropped rows. */
    @Transaction
    suspend fun addClosedTab(closed: ClosedTabEntity, keep: Int): List<ClosedTabEntity> {
        upsertClosedTab(closed)
        val beyond = closedTabsBeyond(keep)
        beyond.forEach { deleteClosedTab(it.koanTabId) }
        return beyond
    }

    @Upsert
    suspend fun upsertClosedTab(closed: ClosedTabEntity)

    @Query("SELECT * FROM closed_tabs ORDER BY closedAt DESC LIMIT -1 OFFSET :keep")
    suspend fun closedTabsBeyond(keep: Int): List<ClosedTabEntity>

    @Query("DELETE FROM closed_tabs WHERE koanTabId = :koanTabId")
    suspend fun deleteClosedTab(koanTabId: String)

    @Query("DELETE FROM closed_tabs")
    suspend fun deleteClosedTabs()

    /** Screenshots a recently closed tab still needs if it comes back. */
    @Query("SELECT tab_thumbnailKey FROM closed_tabs WHERE tab_thumbnailKey IS NOT NULL")
    suspend fun closedTabThumbnails(): List<String>

    @Query("UPDATE visits SET hidden = 1 WHERE fromSync")
    suspend fun hideAllSyncedVisits()

    /** One more visit of [url] (a new row the first time). */
    @Transaction
    suspend fun recordVisit(url: String, title: String, at: Long) {
        if (bumpVisit(url, title, at) == 0) insertVisit(VisitEntity(url, title, at, 1))
    }

    // Visited here (again): shown again if it was removed, and the phone's own from now on.
    @Query(
        "UPDATE visits SET visitCount = visitCount + 1, lastVisit = :at, title = CASE WHEN :title = '' THEN title ELSE :title END, " +
            "hidden = 0, fromSync = 0 WHERE url = :url",
    )
    suspend fun bumpVisit(url: String, title: String, at: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVisit(visit: VisitEntity)

    /** A page's title arrived after its visit was recorded. */
    @Query("UPDATE visits SET title = :title WHERE url = :url")
    suspend fun setVisitTitle(url: String, title: String)

    // ---- from the Mozilla account (read-only copies) ----

    /** History pulled from Sync: a new page counts as synced; a known one keeps the larger figures. */
    @Transaction
    suspend fun mergeSyncedVisits(visits: List<VisitEntity>) {
        visits.forEach { v ->
            insertVisit(v.copy(fromSync = true))
            mergeVisit(v.url, v.title, v.lastVisit, v.visitCount)
        }
    }

    @Query(
        "UPDATE visits SET lastVisit = MAX(lastVisit, :last), visitCount = MAX(visitCount, :count), " +
            "title = CASE WHEN title = '' THEN :title ELSE title END WHERE url = :url",
    )
    suspend fun mergeVisit(url: String, title: String, last: Long, count: Int)

    @Query("DELETE FROM visits WHERE fromSync")
    suspend fun deleteSyncedVisits()

    /** Bookmarks pulled from Sync replace the previous pull's (ids "sync:…"); the phone's stay. */
    @Transaction
    suspend fun replaceSyncedBookmarks(bookmarks: List<BookmarkEntity>) {
        deleteSyncedBookmarks()
        bookmarks.forEach { insertBookmarkIfNew(it) }
    }

    @Query("DELETE FROM bookmarks WHERE id LIKE 'sync:%'")
    suspend fun deleteSyncedBookmarks()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarkIfNew(bookmark: BookmarkEntity)

    @Query("SELECT url FROM bookmarks WHERE id LIKE 'sync:%'")
    fun observeSyncedBookmarkUrls(): Flow<List<String>>

    /** Logins pulled from Sync replace the previous pull's; the phone's own stay. */
    @Transaction
    suspend fun replaceSyncedLogins(logins: List<LoginEntity>) {
        deleteSyncedLogins()
        upsertLogins(logins)
    }

    @Query("DELETE FROM logins WHERE NOT local")
    suspend fun deleteSyncedLogins()

    @Upsert
    suspend fun upsertLogins(logins: List<LoginEntity>)

    @Query("SELECT * FROM logins ORDER BY origin")
    fun observeLogins(): Flow<List<LoginEntity>>

    @Query("SELECT * FROM logins ORDER BY origin")
    suspend fun allLogins(): List<LoginEntity>

    @Query("SELECT * FROM logins WHERE origin = :origin OR origin LIKE '%.' || :baseDomain OR origin LIKE '%//' || :baseDomain")
    suspend fun loginsFor(origin: String, baseDomain: String): List<LoginEntity>

    @Query("UPDATE logins SET timeLastUsed = :at WHERE guid = :guid")
    suspend fun touchLogin(guid: String, at: Long)

    @Query("SELECT * FROM bookmarks WHERE url LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchBookmarks(q: String, limit: Int): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentBookmarks(limit: Int): List<BookmarkEntity>

    @Query("SELECT url FROM bookmarks")
    fun observeBookmarkedUrls(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

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
        // The account's passwords, bookmarks and history leave with it; the phone's own stay.
        deleteSyncedLogins()
        deleteSyncedBookmarks()
        deleteSyncedVisits()
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
    entities = [
        SpaceEntity::class, TabEntity::class, FolderEntity::class, SplitEntity::class, SyncMetaEntity::class,
        BookmarkEntity::class, VisitEntity::class, LoginEntity::class, ClosedTabEntity::class,
    ],
    version = 8,
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()

        /** M3: live pinned tabs' stack position, and the url Zen last sent (see ZenDiff). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tabs ADD COLUMN cardOrder INTEGER")
                db.execSQL("ALTER TABLE tabs ADD COLUMN zenUrl TEXT")
                db.execSQL("UPDATE tabs SET zenUrl = url WHERE origin = 'ZEN'")
            }
        }

        /** M5: bookmarks saved on the phone. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookmarks` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_url` ON `bookmarks` (`url`)")
            }
        }

        /** M5: pages visited on the phone, for search. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `visits` (`url` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`lastVisit` INTEGER NOT NULL, `visitCount` INTEGER NOT NULL, PRIMARY KEY(`url`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_visits_lastVisit` ON `visits` (`lastVisit`)")
            }
        }

        /** M5: the Mozilla account's logins (sealed), and which history came from it. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN `fromSync` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `logins` (`guid` TEXT NOT NULL, `origin` TEXT NOT NULL, " +
                        "`formActionOrigin` TEXT, `httpRealm` TEXT, `usernameField` TEXT NOT NULL, " +
                        "`passwordField` TEXT NOT NULL, `secret` BLOB NOT NULL, `timeCreated` INTEGER NOT NULL, " +
                        "`timeLastUsed` INTEGER NOT NULL, `timePasswordChanged` INTEGER NOT NULL, " +
                        "`local` INTEGER NOT NULL, PRIMARY KEY(`guid`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logins_origin` ON `logins` (`origin`)")
            }
        }

        /** Beta: history rows removed on the phone, and the redirect pages older builds recorded. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN `hidden` INTEGER NOT NULL DEFAULT 0")
                val redirects = ArrayList<String>()
                db.query("SELECT url, title FROM visits").use { c ->
                    while (c.moveToNext()) {
                        val url = c.getString(0)
                        if (HistoryFilter.isRedirect(url, c.getString(1))) redirects += url
                    }
                }
                redirects.forEach { db.execSQL("DELETE FROM visits WHERE url = ?", arrayOf(it)) }
            }
        }

        /** Recently closed tabs (History). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `closed_tabs` (`koanTabId` TEXT NOT NULL, `closedAt` INTEGER NOT NULL, " +
                        "`engineState` TEXT, `tab_koanTabId` TEXT NOT NULL, `tab_zenTabId` TEXT, `tab_spaceUuid` TEXT NOT NULL, " +
                        "`tab_folderId` TEXT, `tab_splitId` TEXT, `tab_url` TEXT NOT NULL, `tab_title` TEXT NOT NULL, " +
                        "`tab_iconUrl` TEXT, `tab_staticIcon` INTEGER NOT NULL, `tab_pinned` INTEGER NOT NULL, " +
                        "`tab_essential` INTEGER NOT NULL, `tab_order` INTEGER NOT NULL, `tab_zenOrder` INTEGER NOT NULL, " +
                        "`tab_lastAccessed` INTEGER NOT NULL, `tab_thumbnailKey` TEXT, `tab_origin` TEXT NOT NULL, " +
                        "`tab_dirty` INTEGER NOT NULL, `tab_closed` INTEGER NOT NULL, `tab_cardOrder` INTEGER, " +
                        "`tab_zenUrl` TEXT, PRIMARY KEY(`koanTabId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_closed_tabs_closedAt` ON `closed_tabs` (`closedAt`)")
            }
        }
    }
}
