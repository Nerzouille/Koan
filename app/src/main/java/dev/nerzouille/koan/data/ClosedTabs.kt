package dev.nerzouille.koan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A tab closed on the phone, for History's "Recently closed": its row as it was ([tab], so it comes
 * back in its space and place) and the engine's saved state (page history, scroll). Only the
 * newest [KEEP] are kept.
 */
@Entity(tableName = "closed_tabs", indices = [Index("closedAt")])
data class ClosedTabEntity(
    @PrimaryKey val koanTabId: String,
    @Embedded(prefix = "tab_") val tab: TabEntity,
    val closedAt: Long,
    /** The engine's state as JSON (GeckoView's own format); null = never opened on the phone. */
    val engineState: String?,
) {
    companion object {
        const val KEEP = 25
    }
}
