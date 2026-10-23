package dev.nerzouille.koan

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * What this phone can afford, read once at startup: live blur (RenderEffect, Android 12+) and a
 * memory tier from its RAM. Uninitialized, it is [MemoryTier.High]: today's behaviour.
 */
object DeviceProfile {
    /** RenderEffect blur exists (API 31). Below it, blur that would be live is a stronger dim instead. */
    val liveBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var tier = MemoryTier.High
        private set

    fun initialize(context: Context) {
        val am = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        tier = tierFor(info.totalMem, am.isLowRamDevice)
    }

    /**
     * The tier for [totalMem] bytes. Android reports less than the marketed size (a 6 GB phone shows
     * ~5.5 GiB, a 4 GB one ~3.6 GiB), hence the thresholds.
     */
    internal fun tierFor(totalMem: Long, lowRam: Boolean): MemoryTier = when {
        lowRam -> MemoryTier.Low
        totalMem >= 5L * GIB -> MemoryTier.High
        totalMem >= 3L * GIB + GIB / 4 -> MemoryTier.Mid
        else -> MemoryTier.Low
    }

    private const val GIB = 1024L * 1024 * 1024
}

/**
 * Budgets by RAM (docs/ARCHITECTURE.md "Tab lifecycle and memory"). [High] is what the app was
 * built and measured with; the others trade sharpness and warm tabs for not being killed.
 */
enum class MemoryTier(
    /** Engine sessions kept live: the current tab + the most recently used. */
    val liveSessions: Int,
    /** Screenshot width on disk. 720 is ~4.7 MB per decoded image at a phone's aspect. */
    val thumbWidthPx: Int,
    /** Decoded screenshots kept in memory (GPU). */
    val thumbMemoryBytes: Int,
    /** Pre-blurred copies kept in memory (GPU): ~1.1 MB per card at 3 levels and 720 px. */
    val bakedMemoryBytes: Int,
    /** Pre-blurred copies per screenshot for the pile (thumbs/BakedBlur). */
    val bakedLevels: Int,
    /** Placeholder cards deep in the pile are captured and baked like screenshots. */
    val bakePlaceholders: Boolean,
    /**
     * Blurred copies of the overview baked when the drawer opens, crossfaded with the drag. One
     * alone reads as a sharp image fading out, not as a blur growing: a half-way copy makes it read
     * as blur. Each one is a full-screen bitmap while the drawer is open.
     */
    val drawerBlurLevels: Int,
) {
    // ≥ 6 GB: unchanged.
    High(4, 720, 64 * MB, 24 * MB, 3, true, 2),
    // 4–6 GB.
    Mid(2, 540, 32 * MB, 12 * MB, 2, true, 2),
    // Under 4 GB, or a low-RAM device.
    Low(1, 480, 16 * MB, 6 * MB, 2, false, 1),
}

private const val MB = 1024 * 1024
