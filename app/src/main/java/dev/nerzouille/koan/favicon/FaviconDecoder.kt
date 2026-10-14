// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.favicon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream

/**
 * Port of `FaviconDecoder` (Shared/ZenTheme.swift): picks the largest frame
 * so multi-resolution ICOs don't decode as a 16x16 smudge — or
 * fail outright, which plain `BitmapFactory` does on some ICO payloads.
 *
 * The ICO directory parsing is pure and unit-tested on the JVM; only final
 * pixel decoding touches Android graphics.
 */
object FaviconDecoder {

    fun image(data: ByteArray): Bitmap? {
        if (!isPlausible(data)) return null
        if (isIco(data)) {
            // Decode just the largest frame's bytes when we can isolate them.
            extractLargestFrameBytes(data)?.let { frame ->
                BitmapFactory.decodeByteArray(frame, 0, frame.size)?.let {
                    it.density = Bitmap.DENSITY_NONE
                    return it
                }
            }
            BitmapFactory.decodeByteArray(data, 0, data.size)?.let {
                it.density = Bitmap.DENSITY_NONE
                return it
            }
            return null
        }
        return BitmapFactory.decodeByteArray(data, 0, data.size)?.apply { density = Bitmap.DENSITY_NONE }
    }

    /** Mirrors the Swift `guard data.count > 8` guard. */
    internal fun isPlausible(data: ByteArray): Boolean = data.size > 8

    internal fun isPng(data: ByteArray): Boolean =
        data.size > 8 &&
            data[0] == 0x89.toByte() && data[1] == 0x50.toByte() &&
            data[2] == 0x4E.toByte() && data[3] == 0x47.toByte()

    /** ICONDIR: reserved==0, type==1. */
    internal fun isIco(data: ByteArray): Boolean =
        data.size > 6 &&
            data[0].toInt() == 0 && data[1].toInt() == 0 &&
            data[2].toInt() == 1 && data[3].toInt() == 0

    internal class IcoEntry(
        val width: Int,
        val height: Int,
        val byteSize: Int,
        val offset: Int,
    ) {
        val area: Int get() = width * height
    }

    internal fun parseIcoDirectory(data: ByteArray): List<IcoEntry> {
        if (!isIco(data)) return emptyList()
        val count = readUShort(data, 4)
        val entries = mutableListOf<IcoEntry>()
        for (i in 0 until count) {
            val base = 6 + i * 16
            if (base + 16 > data.size) break
            val w = data[base].toInt() and 0xFF
            val h = data[base + 1].toInt() and 0xFF
            entries += IcoEntry(
                width = if (w == 0) 256 else w,
                height = if (h == 0) 256 else h,
                byteSize = readUInt(data, base + 8),
                offset = readUInt(data, base + 12),
            )
        }
        return entries
    }

    /** Bytes of the largest entry (area first, size as tiebreak); null if malformed. */
    internal fun extractLargestFrameBytes(data: ByteArray): ByteArray? {
        val best = parseIcoDirectory(data).maxWithOrNull(
            compareBy({ it.area }, { it.byteSize })
        ) ?: return null
        if (best.offset < 0 || best.byteSize <= 0 || best.offset + best.byteSize > data.size) return null
        return data.copyOfRange(best.offset, best.offset + best.byteSize)
    }

    private fun readUShort(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8))

    private fun readUInt(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
}
