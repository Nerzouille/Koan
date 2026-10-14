package dev.nerzouille.koan.data

import android.util.Log
import dev.nerzouille.koan.favicon.IconStore
import dev.nerzouille.koan.sync.SpacesSyncService
import dev.nerzouille.koan.sync.SyncError
import dev.nerzouille.koan.sync.ZenRoomMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** What the overview shows about sync: quietly, never as a dialog. */
sealed interface SyncStatus {
    /** Nothing to report. [lastSyncAt] = 0 if this install never synced. */
    data class Idle(val lastSyncAt: Long) : SyncStatus

    /** A pull is running. [first] = nothing from Zen in Room yet (show placeholder cards). */
    data class Syncing(val first: Boolean) : SyncStatus

    /** The last pull failed for a reason that fixes itself (offline, server hiccup). */
    data class Failed(val lastSyncAt: Long, val offline: Boolean) : SyncStatus

    /** The Mozilla session is no longer valid: "Sign in again" in the drawer. */
    data object NeedsSignIn : SyncStatus
}

/**
 * Pulls Zen's Spaces into Room (read-only: nothing here writes to Sync, CLAUDE.md rule 4).
 * Schedule (docs/ARCHITECTURE.md "Sync scheduling"): app start, back to the foreground after more
 * than 2 min, every 15 min while in the foreground, and on demand.
 */
class SpacesSync(
    private val accounts: AccountStore,
    private val dao: KoanDao,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle(0))
    val status: StateFlow<SyncStatus> = _status

    private val running = Mutex()
    private var lastAttemptAt = 0L
    private var periodic: Job? = null

    private var started = false

    /**
     * Process start (also after process death): read the account and last sync time, decode the
     * icons Room already knows so the overview opens without pop-in. The first pull comes from
     * [onForeground], which follows right after.
     */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            accounts.load()
            if (_status.value == SyncStatus.Idle(0)) {
                lastSyncAt = dao.syncMeta(COLLECTION)?.lastSyncAt ?: 0
                _status.value = SyncStatus.Idle(lastSyncAt)
            }
            IconStore.prewarm(dao.tabs().filterNot { it.closed }.map { it.url to it.iconUrl })
        }
    }

    fun onForeground() {
        periodic?.cancel()
        periodic = scope.launch {
            if (System.currentTimeMillis() - lastAttemptAt > FOREGROUND_AFTER_MS) pull()
            while (isActive) {
                delay(PERIOD_MS)
                pull()
            }
        }
    }

    fun onBackground() {
        periodic?.cancel()
        periodic = null
    }

    /** "Sync now" (badge tap, drawer row) and right after sign-in. */
    fun syncNow() = scope.launch { pull() }

    private var lastSyncAt = 0L

    private suspend fun pull() {
        if (!running.tryLock()) return
        try {
            if (accounts.load() == null) return
            lastAttemptAt = System.currentTimeMillis()
            if (lastSyncAt == 0L) lastSyncAt = dao.syncMeta(COLLECTION)?.lastSyncAt ?: 0
            val first = dao.spaces().none { it.zenOrigin }
            _status.value = SyncStatus.Syncing(first)
            _status.value = try {
                pullOnce(retryAuth = true)
                SyncStatus.Idle(lastSyncAt)
            } catch (e: SyncError.SessionExpired) {
                SyncStatus.NeedsSignIn
            } catch (e: SyncError.TotpRequired) {
                SyncStatus.NeedsSignIn
            } catch (e: SyncError.NotSignedIn) {
                SyncStatus.Idle(lastSyncAt)
            } catch (e: IOException) {
                SyncStatus.Failed(lastSyncAt, offline = true)
            } catch (e: Exception) {
                // Class only: messages can carry server responses, never log more than the type.
                Log.w(TAG, "spaces pull failed: ${e.javaClass.simpleName}")
                SyncStatus.Failed(lastSyncAt, offline = false)
            }
        } finally {
            running.unlock()
        }
    }

    private suspend fun pullOnce(retryAuth: Boolean) {
        val client = accounts.connect()
        val snapshot = try {
            SpacesSyncService.loadSnapshot(client)
        } catch (e: SyncError.Network) {
            // Storage answered 401: the token-server credentials expired early. Fetch new ones once.
            if (retryAuth && e.message.orEmpty().startsWith("HTTP 401")) {
                accounts.invalidateCredentials()
                return pullOnce(retryAuth = false)
            }
            throw e
        }
        val rows = ZenRoomMapper.map(snapshot)
        if (rows.spaces.isEmpty()) {
            // Zen always has at least one space; an empty pull means a broken or partial
            // collection. Keep what Room has rather than emptying the overview.
            Log.w(TAG, "spaces pull returned no spaces; keeping local data")
        } else {
            val inUse = rows.tabs.mapNotNullTo(HashSet()) { it.iconUrl?.takeIf { k -> k.startsWith(ZenRoomMapper.INLINE_ICON_PREFIX) } }
            IconStore.storeInline(rows.inlineIcons, inUse)
            val diff = ZenDiff.between(rows, dao.spaces(), dao.tabs(), dao.folders(), dao.splits())
            if (!diff.isEmpty) dao.applyZenDiff(diff)
            IconStore.prewarm(rows.tabs.map { it.url to it.iconUrl })
        }
        lastSyncAt = System.currentTimeMillis()
        dao.upsertSyncMeta(SyncMetaEntity(COLLECTION, lastModified = snapshot.fetchedAtMillis, lastSyncAt = lastSyncAt))
    }

    /** Sign-out: forget the session and every Zen row; phone-only tabs stay. */
    fun signOut() = scope.launch {
        running.withLock {
            accounts.signOut()
            dao.dropZenData(LocalSpace.entity())
            lastSyncAt = 0
            _status.value = SyncStatus.Idle(0)
        }
    }

    private companion object {
        const val TAG = "KoanSync"
        const val COLLECTION = "spaces"
        const val FOREGROUND_AFTER_MS = 2 * 60_000L
        const val PERIOD_MS = 15 * 60_000L
    }
}

/** The phone-only space that holds tabs while no Zen account is connected. */
object LocalSpace {
    const val UUID = "koan-local"

    fun entity() = SpaceEntity(uuid = UUID, name = "Phone", icon = "📱", gradientColors = emptyList(), order = 0)
}
