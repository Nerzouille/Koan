// Ported from Z-Sync (https://github.com/Kjell6/Z-Sync), MIT, © 2026 Kjell Behrends

package dev.nerzouille.koan.sync

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

/**
 * Port of `Shared/ZenSpaces.swift` — wire format of Zen's `spaces` Sync
 * collection (PR #13984 / #13598) plus the app-facing snapshot model.
 * All decoders are tolerant exactly like the Swift `try?` chains.
 */
object ZenSpaces {
    // MARK: - Color value

    class ZenColorValue(
        red: Double,
        green: Double,
        blue: Double,
        alpha: Double = 1.0,
    ) {
        val red = clamp01(red)
        val green = clamp01(green)
        val blue = clamp01(blue)
        val alpha = clamp01(alpha)

        val hexString: String
            get() {
                val r = Math.round(red * 255).toInt()
                val g = Math.round(green * 255).toInt()
                val b = Math.round(blue * 255).toInt()
                return String.format("#%02x%02x%02x", r, g, b)
            }

        fun toArgb(): Int {
            val r = Math.round(red * 255).toInt()
            val g = Math.round(green * 255).toInt()
            val b = Math.round(blue * 255).toInt()
            val a = Math.round(alpha * 255).toInt()
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        override fun equals(other: Any?): Boolean =
            other is ZenColorValue && other.red == red && other.green == green && other.blue == blue && other.alpha == alpha

        override fun hashCode(): Int = (Math.round(red * 255).toInt() * 31 + Math.round(green * 255).toInt()) * 31 + Math.round(blue * 255).toInt()

        companion object {
            private fun clamp01(v: Double): Double = v.coerceIn(0.0, 1.0)

            fun from255(r: Double, g: Double, b: Double, alpha: Double = 1.0) =
                ZenColorValue(r / 255.0, g / 255.0, b / 255.0, alpha)

            fun fromHex(hex: String): ZenColorValue? {
                var text = hex.trim()
                if (text.startsWith("#")) text = text.substring(1)
                if (text.length != 6 && text.length != 8 && text.length != 3) return null
                if (text.length == 3) {
                    text = text.map { "$it$it" }.joinToString("")
                }
                val value = text.toLongOrNull(16) ?: return null
                return if (text.length == 8) {
                    ZenColorValue(
                        red = ((value shr 24) and 0xFF).toDouble() / 255.0,
                        green = ((value shr 16) and 0xFF).toDouble() / 255.0,
                        blue = ((value shr 8) and 0xFF).toDouble() / 255.0,
                        alpha = (value and 0xFF).toDouble() / 255.0,
                    )
                } else {
                    ZenColorValue(
                        red = ((value shr 16) and 0xFF).toDouble() / 255.0,
                        green = ((value shr 8) and 0xFF).toDouble() / 255.0,
                        blue = (value and 0xFF).toDouble() / 255.0,
                    )
                }
            }

            fun fromRgbString(rgbString: String): ZenColorValue? {
                val digits = rgbString.split(Regex("[^0-9]+"))
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toDoubleOrNull() }
                if (digits.size < 3) return null
                val a = if (digits.size >= 4) digits[3] else 1.0
                return from255(digits[0], digits[1], digits[2], if (a <= 1.0) a else a / 255.0)
            }
        }
    }

    // MARK: - Theme dot (the "messy" decoder)

    class ZenThemeDot(
        val color: ZenColorValue,
        val isCustom: Boolean = false,
        val isPrimary: Boolean = false,
        val algorithm: String? = null,
        val lightness: Double? = null,
        val positionX: Double? = null,
        val positionY: Double? = null,
        val type: String? = null,
    ) {
        companion object {
            /** Mirrors Swift's custom `init(from:)` including all fallbacks. */
            fun decodeAny(value: Any?): ZenThemeDot? {
                when (value) {
                    is String -> {
                        val color = ZenColorValue.fromHex(value) ?: ZenColorValue.fromRgbString(value)
                        return color?.let { ZenThemeDot(color = it) }
                    }
                    is JSONArray -> {
                        if (value.length() >= 3 && value.optDouble(0, Double.NaN).let { !it.isNaN() }) {
                            return ZenThemeDot(
                                color = ZenColorValue.from255(value.getDouble(0), value.getDouble(1), value.getDouble(2))
                            )
                        }
                        return null
                    }
                    is JSONObject -> {
                        var resolved = ZenColorValue(0.5, 0.5, 0.5)
                        val c = value.opt("c")
                        when (c) {
                            is JSONArray -> if (c.length() >= 3) {
                                resolved = ZenColorValue.from255(c.getDouble(0), c.getDouble(1), c.getDouble(2))
                            }
                            is String -> {
                                resolved = ZenColorValue.fromHex(c) ?: ZenColorValue.fromRgbString(c) ?: resolved
                            }
                            else -> {}
                        }
                        val custom = value.optBoolean("isCustom", false)
                        val primary = value.optBoolean("isPrimary", false)
                        val algo = optStringOrNull(value, "algorithm")
                        val light = when (val l = value.opt("lightness")) {
                            is Number -> l.toDouble()
                            is String -> l.toDoubleOrNull()
                            else -> null
                        }
                        var posX: Double? = null
                        var posY: Double? = null
                        val pos = value.optJSONObject("position")
                        if (pos != null) {
                            posX = pos.optDouble("x", Double.NaN).takeUnless { it.isNaN() }
                            posY = pos.optDouble("y", Double.NaN).takeUnless { it.isNaN() }
                        }
                        return ZenThemeDot(
                            color = resolved,
                            isCustom = custom,
                            isPrimary = primary,
                            algorithm = algo,
                            lightness = light,
                            positionX = posX,
                            positionY = posY,
                            type = optStringOrNull(value, "type"),
                        )
                    }
                    else -> return null
                }
            }
        }

        override fun equals(other: Any?): Boolean = other is ZenThemeDot && other.color == color &&
            other.isCustom == isCustom && other.isPrimary == isPrimary &&
            other.algorithm == algorithm && other.lightness == lightness &&
            other.positionX == positionX && other.positionY == positionY && other.type == type

        override fun hashCode(): Int = color.hashCode()
    }

    // MARK: - Theme

    class ZenSpaceTheme(
        val type: String?,
        val dots: List<ZenThemeDot>,
        val opacity: Double?,
        val texture: Double?,
        val lightness: Double?,
    ) {
        /** Hex accent colors; null when there are no dots (Swift semantics). */
        val gradientColors: List<String>?
            get() = if (dots.isEmpty()) null else dots.map { it.color.hexString }

        val isDarkTheme: Boolean?
            get() {
                if (dots.isEmpty()) return null
                var total = 0.0
                for (dot in dots) {
                    total += 0.299 * dot.color.red + 0.587 * dot.color.green + 0.114 * dot.color.blue
                }
                return (total / dots.size) < 0.50
            }

        companion object {
            fun decode(obj: JSONObject): ZenSpaceTheme {
                val type = optStringOrNull(obj, "type")
                val opacity = optDoubleOrNull(obj, "opacity")
                val texture = optDoubleOrNull(obj, "texture")
                val lightness = optDoubleOrNull(obj, "lightness")

                val dots = mutableListOf<ZenThemeDot>()
                when (val gc = obj.opt("gradientColors")) {
                    is JSONArray -> {
                        for (i in 0 until gc.length()) {
                            val dot = ZenThemeDot.decodeAny(gc.opt(i))
                            if (dot != null) dots.add(dot)
                        }
                    }
                    else -> {}
                }
                return ZenSpaceTheme(type, dots, opacity, texture, lightness)
            }

            fun fromGradientColors(hexes: List<String>?, opacity: Double? = null, texture: Double? = null, lightness: Double? = null): ZenSpaceTheme =
                ZenSpaceTheme(
                    type = null,
                    dots = (hexes ?: emptyList()).mapNotNull { h ->
                        ZenColorValue.fromHex(h)?.let { ZenThemeDot(color = it) }
                    },
                    opacity = opacity,
                    texture = texture,
                    lightness = lightness,
                )
        }

        override fun equals(other: Any?): Boolean = other is ZenSpaceTheme &&
            other.dots == dots && other.opacity == opacity && other.texture == texture && other.lightness == lightness && other.type == type

        override fun hashCode(): Int = dots.hashCode()
    }

    // MARK: - Records

    enum class RecordKind { container, space, tab, folder, split, layout }

    /**
     * Whether the browser on the other side can sync normal (unpinned) tabs,
     * derived from the synced `zen.spaces-sync.normal-tabs` pref
     * (`wire-prefs-normal-tabs-capability`):
     *
     * - [ENABLED]: the pref is present and parses true.
     * - [DISABLED]: the pref is present but parses false/null/unparseable.
     * - [ABSENT]: no readable prefs record, or the key is missing.
     *
     * A `pinned:false` record observed in `spaces` proves support; callers
     * upgrade [ABSENT] to [DISABLED] in that case.
     */
    enum class NormalTabsCapability { ABSENT, DISABLED, ENABLED }

    class ZenSpaceRecord(
        val uuid: String,
        val name: String?,
        val icon: String?,
        val theme: ZenSpaceTheme?,
        val containerGuid: String?,
        val children: List<String>?,
    )

    class ZenTabRecord(
        val tabId: String,
        val url: String,
        val title: String?,
        val icon: String?,
        val containerGuid: String?,
        val essential: Boolean?,
        val workspaceUuid: String?,
        val folderId: String?,
        val staticLabel: String?,
        val hasStaticIcon: Boolean?,
        val defaultContainer: Boolean?,
        /**
         * `false` marks a normal (unpinned) tab. `true`/null means pinned, so
         * records from before the normal-tabs feature stay pinned.
         */
        val pinned: Boolean? = null,
    ) {
        /** A record from Zen's `zen.spaces-sync.normal-tabs` projection. */
        val isNormalTab: Boolean get() = pinned == false
    }

    class ZenFolderRecord(
        val folderId: String,
        val name: String?,
        val icon: String?,
        val workspaceUuid: String?,
        val parentFolderId: String?,
        val children: List<String>?,
    )

    class ZenSplitRecord(
        val splitId: String,
        val gridType: String?,
        val tabs: List<String>?,
        val workspaceUuid: String?,
        val folderId: String?,
        /**
         * `false` marks a split of normal (unpinned) tabs (from the first
         * member); `true`/null means pinned.
         */
        val pinned: Boolean? = null,
    ) {
        val isNormalSplit: Boolean get() = pinned == false
    }

    class ZenLayoutRecord(
        val spaces: List<String>?,
        val essentials: Map<String, List<String>>?,
    )

    // MARK: - App-facing model

    data class ZenTab(
        val id: String,
        val url: String,
        val title: String,
        val iconURL: String? = null,
        val icon: String? = null,
        val hasStaticIcon: Boolean? = null,
        /** Zen custom tab name (`data.staticLabel`). Null/empty falls back to `title`. */
        val staticLabel: String? = null,
    ) {
        /** `staticLabel` when set, then page title, then host, then the raw URL. */
        val displayTitle: String
            get() {
                val label = staticLabel?.trim().orEmpty()
                if (label.isNotEmpty()) return label
                val raw = title.trim()
                if (raw.isNotEmpty()) return raw
                val host = runCatching { URI(url).host }.getOrNull().orEmpty()
                return host.ifEmpty { url }
            }
    }

    data class ZenFolder(
        val id: String,
        val name: String,
        val icon: String?,
        val tabs: List<ZenTab>,
        /** Folders nested inside this one, in sidebar order. Null = none. */
        val subfolders: List<ZenFolder>? = null,
    )

    data class ZenSplit(
        val id: String,
        val gridType: String?,
        val tabs: List<ZenTab>,
    )

    sealed class ZenItem {
        abstract val id: String

        data class Tab(val tab: ZenTab) : ZenItem() {
            override val id: String get() = tab.id
        }

        data class Folder(val folder: ZenFolder) : ZenItem() {
            override val id: String get() = folder.id
        }

        data class Split(val split: ZenSplit) : ZenItem() {
            override val id: String get() = split.id
        }
    }

    data class ZenSpace(
        val id: String,
        val name: String,
        val icon: String?,
        val containerGuid: String?,
        val theme: ZenSpaceTheme?,
        val pinned: List<ZenItem>,
        /** Normal (unpinned) tabs synced via `zen.spaces-sync.normal-tabs`. */
        val tabs: List<ZenItem> = emptyList(),
    ) {
        val themeColors: List<String> get() = theme?.gradientColors ?: emptyList()
        val themeOpacity: Double? get() = theme?.opacity
    }

    data class ZenSnapshot(
        val spaces: List<ZenSpace>,
        val essentials: Map<String, List<ZenTab>>,
        val fetchedAtMillis: Long,
        /**
         * Synced `zen.workspaces.separate-essentials` value. Zen does not sync
         * that pref today, so this is normally null and [AUTOMATIC] falls back
         * to [inferContainerSpecific].
         */
        val separateEssentialsPref: Boolean? = null,
        /**
         * Normal-tabs write capability derived from the synced prefs record.
         * Defaults to [NormalTabsCapability.ABSENT] so caches written before
         * this field existed still decode.
         */
        val normalTabsCapability: NormalTabsCapability = NormalTabsCapability.ABSENT,
    ) {
        constructor(spaces: List<ZenSpace>) :
            this(spaces, emptyMap(), 0L)

        fun space(id: String): ZenSpace? = spaces.firstOrNull { it.id == id }

        /**
         * True when the home screen has anything to show: a pinned tab (incl.
         * pinned folders/splits), a normal tab, or an essential. When false
         * while spaces exist, Zen's "Sync your sidebar across devices" is
         * likely off on desktop.
         */
        val hasSyncedTabs: Boolean
            get() = spaces.any { it.pinned.isNotEmpty() || it.tabs.isNotEmpty() } ||
                essentials.values.any { it.isNotEmpty() }

        // MARK: Essentials grouping

        /** Essentials to show on [space] under [grouping]. */
        fun essentialsFor(
            space: ZenSpace,
            grouping: EssentialsGrouping = EssentialsGrouping.AUTOMATIC,
        ): List<ZenTab> =
            if (isContainerSpecific(grouping)) containerSpecificEssentials(space) else sharedEssentials()

        /**
         * Whether container grouping applies under [grouping]. AUTOMATIC uses
         * the synced pref when present, otherwise infers from the buckets.
         */
        fun isContainerSpecific(grouping: EssentialsGrouping): Boolean = when (grouping) {
            EssentialsGrouping.AUTOMATIC -> separateEssentialsPref ?: inferContainerSpecific(essentials)
            EssentialsGrouping.CONTAINER_SPECIFIC -> true
            EssentialsGrouping.SHARED -> false
        }

        /**
         * Container-specific lookup mirrors desktop `_shouldShowTab`: a space
         * with a container gets its own bucket; a container-less space gets
         * the default bucket plus buckets no space uses (orphan containers),
         * so those essentials stay reachable.
         */
        private fun containerSpecificEssentials(space: ZenSpace): List<ZenTab> {
            val guid = space.containerGuid
            if (!guid.isNullOrEmpty()) return essentials[guid] ?: emptyList()
            val usedContainers = spaces.mapNotNull { it.containerGuid }.filter { it.isNotEmpty() }.toSet()
            val orphanKeys = essentials.keys
                .filter { it != "default" && it !in usedContainers }
                .sorted()
            return mergedBuckets(listOf("default") + orphanKeys)
        }

        /**
         * Shared grouping: every essential, default bucket first, remaining
         * buckets in stable key order (the wire format loses cross-bucket order).
         */
        private fun sharedEssentials(): List<ZenTab> {
            val otherKeys = essentials.keys.filter { it != "default" }.sorted()
            return mergedBuckets(listOf("default") + otherKeys)
        }

        /**
         * Merges buckets in [keys] order (default first, then stable key order).
         * De-duplicates by id everywhere (existing behavior) and by URL *across*
         * buckets, keeping the first occurrence. A genuine duplicate URL inside a
         * single bucket is real Zen data and must stay visible, so URLs are only
         * compared against buckets already merged.
         */
        private fun mergedBuckets(keys: List<String>): List<ZenTab> {
            val result = mutableListOf<ZenTab>()
            val seen = mutableSetOf<String>()
            val priorURLs = mutableSetOf<String>()
            for (key in keys) {
                val bucketURLs = mutableSetOf<String>()
                for (tab in essentials[key].orEmpty()) {
                    if (!seen.add(tab.id)) continue
                    if (tab.url in priorURLs) continue
                    bucketURLs.add(tab.url)
                    result.add(tab)
                }
                priorURLs.addAll(bucketURLs)
            }
            return result
        }

        companion object {
            /** Swift reference date (2001-01-01) offset in millis. */
            const val REFERENCE_EPOCH_MILLIS = 978307200000L
            val empty = ZenSnapshot(emptyList(), emptyMap(), Long.MIN_VALUE)

            /**
             * Separate essentials only when at least one lives in a real
             * container bucket. All-default data (the common case when the
             * pref is off) shows on every space.
             */
            fun inferContainerSpecific(buckets: Map<String, List<ZenTab>>): Boolean =
                buckets.any { (key, tabs) -> key != "default" && tabs.isNotEmpty() }
        }
    }

    /**
     * How essentials map onto spaces. Zen Desktop's
     * `zen.workspaces.separate-essentials` preference switches between
     * container-specific and shared; [AUTOMATIC] follows the synced pref when
     * Zen sends it and otherwise infers from the wire buckets.
     */
    enum class EssentialsGrouping(val storageValue: String) {
        AUTOMATIC("automatic"),
        CONTAINER_SPECIFIC("container-specific"),
        SHARED("shared"),
        ;

        companion object {
            fun fromStorage(value: String?): EssentialsGrouping =
                entries.firstOrNull { it.storageValue == value } ?: AUTOMATIC
        }
    }

    // MARK: - Decoder

    sealed class DecodedRecord {
        data class Space(val record: ZenSpaceRecord) : DecodedRecord()
        data class Tab(val record: ZenTabRecord) : DecodedRecord()
        data class Folder(val record: ZenFolderRecord) : DecodedRecord()
        data class Split(val record: ZenSplitRecord) : DecodedRecord()
        data class Layout(val record: ZenLayoutRecord) : DecodedRecord()
    }

    /** Turns one decrypted record payload into a typed value; nil for tombstones/unknown/malformed. */
    fun decode(id: String, cleartext: JSONObject): DecodedRecord? {
        // `deleted` MUST be the JSON boolean true (SPEC §2.1); the string
        // "true" is not a tombstone, so optBoolean's coercion is wrong here.
        if ((cleartext.opt("deleted") as? Boolean) == true) return null
        val kindRaw = cleartext.optString("kind")
        val kind = RecordKind.entries.firstOrNull { it.name == kindRaw } ?: return null
        val data = cleartext.opt("data") as? JSONObject ?: return null
        return try {
            when (kind) {
                RecordKind.space -> DecodedRecord.Space(decodeSpace(data) ?: return null)
                RecordKind.tab -> DecodedRecord.Tab(decodeTab(data) ?: return null)
                RecordKind.folder -> DecodedRecord.Folder(decodeFolder(data) ?: return null)
                RecordKind.split -> DecodedRecord.Split(decodeSplit(data) ?: return null)
                RecordKind.layout -> DecodedRecord.Layout(decodeLayout(data))
                RecordKind.container -> return null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSpace(data: JSONObject): ZenSpaceRecord? {
        // Strict read (SPEC §3.1/§3.3): `uuid` must be a JSON string; numbers
        // and booleans are not coerced (wire-space-numeric-uuid).
        val uuid = optStringOrNull(data, "uuid")
        if (uuid.isNullOrEmpty()) return null
        return ZenSpaceRecord(
            uuid = uuid,
            name = optStringOrNull(data, "name"),
            icon = optStringOrNull(data, "icon"),
            theme = data.optJSONObject("theme")?.let { runCatching { ZenSpaceTheme.decode(it) }.getOrNull() },
            containerGuid = optStringOrNull(data, "containerGuid"),
            children = optStringList(data, "children"),
        )
    }

    private fun decodeTab(data: JSONObject): ZenTabRecord? {
        val tabId = optStringOrNull(data, "tabId") ?: return null
        val url = optStringOrNull(data, "url") ?: return null
        return ZenTabRecord(
            tabId = tabId,
            url = url,
            title = optStringOrNull(data, "title"),
            icon = optStringOrNull(data, "icon"),
            containerGuid = optStringOrNull(data, "containerGuid"),
            essential = parseFlag(data, "essential"),
            workspaceUuid = optStringOrNull(data, "workspaceUuid"),
            folderId = optStringOrNull(data, "folderId"),
            staticLabel = optStringOrNull(data, "staticLabel"),
            hasStaticIcon = if (data.has("hasStaticIcon") && !data.isNull("hasStaticIcon")) data.optBoolean("hasStaticIcon") else null,
            defaultContainer = if (data.has("defaultContainer") && !data.isNull("defaultContainer")) data.optBoolean("defaultContainer") else null,
            pinned = parseFlag(data, "pinned"),
        )
    }

    /** Decodes a flag sent as either a JSON bool or a string ("true"/"1"). */
    internal fun parseFlag(obj: JSONObject, key: String): Boolean? =
        when (val value = obj.opt(key)) {
            is Boolean -> value
            is String -> when (value) {
                "true", "1" -> true
                "false", "0" -> false
                else -> null
            }
            else -> null
        }

    private fun decodeFolder(data: JSONObject): ZenFolderRecord? {
        val folderId = optStringOrNull(data, "folderId") ?: return null
        return ZenFolderRecord(
            folderId = folderId,
            name = optStringOrNull(data, "name"),
            icon = optStringOrNull(data, "icon"),
            workspaceUuid = optStringOrNull(data, "workspaceUuid"),
            parentFolderId = optStringOrNull(data, "parentFolderId"),
            children = optStringList(data, "children"),
        )
    }

    private fun decodeSplit(data: JSONObject): ZenSplitRecord? {
        val splitId = optStringOrNull(data, "splitId") ?: return null
        return ZenSplitRecord(
            splitId = splitId,
            gridType = optStringOrNull(data, "gridType"),
            tabs = optStringList(data, "tabs"),
            workspaceUuid = optStringOrNull(data, "workspaceUuid"),
            folderId = optStringOrNull(data, "folderId"),
            pinned = parseFlag(data, "pinned"),
        )
    }

    private fun decodeLayout(data: JSONObject): ZenLayoutRecord {
        val spaces = optStringList(data, "spaces")
        var essentials: Map<String, List<String>>? = null
        val rawEssentials = data.opt("essentials")
        if (rawEssentials is JSONObject) {
            val map = linkedMapOf<String, List<String>>()
            for (key in rawEssentials.keys()) {
                when (val arr = rawEssentials.opt(key)) {
                    is JSONArray -> {
                        val ids = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            (arr.opt(i) as? String)?.let { ids.add(it) }
                        }
                        map[key] = ids
                    }
                    else -> {}
                }
            }
            essentials = map.ifEmpty { null }
        }
        return ZenLayoutRecord(spaces = spaces, essentials = essentials)
    }

    // MARK: - tolerant helpers (mirroring Swift's `try? decodeIfPresent`)

    internal fun optStringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val v = obj.opt(key)
        return if (v is String) v else null
    }

    internal fun optDoubleOrNull(obj: JSONObject, key: String): Double? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return when (val v = obj.opt(key)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    private fun optStringList(obj: JSONObject, key: String): List<String>? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val arr = obj.opt(key) as? JSONArray ?: return null
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            (arr.opt(i) as? String)?.let { out.add(it) }
        }
        return out
    }

    // MARK: - Snapshot cache codec
    //
    // Device-local and NON-CONTRACTUAL (SPEC §8): the local snapshot cache is
    // exempt from contract versioning and requires no cross-platform
    // compatibility. Android writes items as `{"type":"tab","tab":{…}}`
    // (discriminated shape) while iOS Codable writes `{"tab":{"_0":{…}}}`.
    // Never "fix" this to match the other platform without bumping the
    // contract version.

    fun encodeSnapshot(snapshot: ZenSnapshot): JSONObject {
        val root = JSONObject()
        val spaces = JSONArray()
        for (space in snapshot.spaces) {
            val s = JSONObject()
            s.put("id", space.id)
            s.put("name", space.name)
            if (space.icon != null) s.put("icon", space.icon!!)
            if (space.containerGuid != null) s.put("containerGuid", space.containerGuid!!)
            if (space.theme != null) {
                val t = JSONObject()
                space.theme!!.type?.let { t.put("type", it) }
                val dots = JSONArray()
                space.theme!!.dots.forEach { dots.put(it.color.hexString) }
                t.put("gradientColors", dots)
                space.theme!!.opacity?.let { t.put("opacity", it) }
                space.theme!!.texture?.let { t.put("texture", it) }
                space.theme!!.lightness?.let { t.put("lightness", it) }
                s.put("theme", t)
            }
            val colors = JSONArray()
            space.themeColors.forEach { colors.put(it) }
            s.put("themeColors", colors)
            space.themeOpacity?.let { s.put("themeOpacity", it) }
            s.put("pinned", encodeItems(space.pinned))
            s.put("tabs", encodeItems(space.tabs))
            spaces.put(s)
        }
        root.put("spaces", spaces)

        val essentials = JSONObject()
        for ((bucket, tabs) in snapshot.essentials) {
            val arr = JSONArray()
            tabs.forEach { arr.put(encodeTab(it)) }
            essentials.put(bucket, arr)
        }
        root.put("essentials", essentials)
        snapshot.separateEssentialsPref?.let { root.put("separateEssentialsPref", it) }
        root.put("normalTabsCapability", snapshot.normalTabsCapability.name.lowercase())
        root.put("fetchedAt", (snapshot.fetchedAtMillis - ZenSnapshot.REFERENCE_EPOCH_MILLIS) / 1000.0)
        return root
    }

    private fun encodeItems(items: List<ZenItem>): JSONArray {
        val out = JSONArray()
        for (item in items) {
            val entry = JSONObject()
            when (item) {
                is ZenItem.Tab -> {
                    entry.put("type", "tab")
                    entry.put("tab", encodeTab(item.tab))
                }
                is ZenItem.Folder -> {
                    entry.put("type", "folder")
                    entry.put("folder", encodeFolder(item.folder))
                }
                is ZenItem.Split -> {
                    entry.put("type", "split")
                    val s = JSONObject()
                    s.put("id", item.split.id)
                    item.split.gridType?.let { s.put("gridType", it) }
                    val stabs = JSONArray()
                    item.split.tabs.forEach { stabs.put(encodeTab(it)) }
                    s.put("tabs", stabs)
                    entry.put("split", s)
                }
            }
            out.put(entry)
        }
        return out
    }

    private fun encodeFolder(folder: ZenFolder): JSONObject {
        val f = JSONObject()
        f.put("id", folder.id)
        f.put("name", folder.name)
        folder.icon?.let { f.put("icon", it) }
        val ftabs = JSONArray()
        folder.tabs.forEach { ftabs.put(encodeTab(it)) }
        f.put("tabs", ftabs)
        folder.subfolders?.takeIf { it.isNotEmpty() }?.let { subs ->
            val arr = JSONArray()
            subs.forEach { arr.put(encodeFolder(it)) }
            f.put("subfolders", arr)
        }
        return f
    }

    private fun encodeTab(tab: ZenTab): JSONObject {
        val t = JSONObject()
        t.put("id", tab.id)
        t.put("url", tab.url)
        t.put("title", tab.title)
        tab.iconURL?.let { t.put("iconURL", it) }
        tab.icon?.let { t.put("icon", it) }
        tab.hasStaticIcon?.let { t.put("hasStaticIcon", it) }
        tab.staticLabel?.let { t.put("staticLabel", it) }
        return t
    }

    fun decodeSnapshot(root: JSONObject): ZenSnapshot? {
        return try {
            val spaces = mutableListOf<ZenSpace>()
            val spacesArr = root.optJSONArray("spaces") ?: JSONArray()
            for (i in 0 until spacesArr.length()) {
                val s = spacesArr.optJSONObject(i) ?: continue
                val id = s.optString("id")
                if (id.isEmpty()) continue
                val theme = s.optJSONObject("theme")?.let { runCatching { ZenSpaceTheme.decode(it) }.getOrNull() }
                    ?: run {
                        // Legacy shape: flat themeColors + themeOpacity.
                        val hexes = mutableListOf<String>()
                        s.optJSONArray("themeColors")?.let { arr ->
                            for (j in 0 until arr.length()) (arr.opt(j) as? String)?.let { hexes.add(it) }
                        }
                        if (hexes.isNotEmpty()) {
                            ZenSpaceTheme.fromGradientColors(hexes, optDoubleOrNull(s, "themeOpacity"))
                        } else null
                    }
                val pinned = decodeItems(s.optJSONArray("pinned"))
                val tabs = decodeItems(s.optJSONArray("tabs"))
                spaces.add(
                    ZenSpace(
                        id = id,
                        name = s.optString("name", ""),
                        icon = optStringOrNull(s, "icon"),
                        containerGuid = optStringOrNull(s, "containerGuid"),
                        theme = theme,
                        pinned = pinned,
                        tabs = tabs,
                    )
                )
            }

            val essentials = linkedMapOf<String, List<ZenTab>>()
            root.optJSONObject("essentials")?.let { eo ->
                for (key in eo.keys()) {
                    val list = mutableListOf<ZenTab>()
                    eo.optJSONArray(key)?.let { arr ->
                        for (i in 0 until arr.length()) {
                            arr.optJSONObject(i)?.let { to -> decodeCachedTab(to)?.let { list.add(it) } }
                        }
                    }
                    if (list.isNotEmpty()) essentials[key] = list
                }
            }

            val fetchedRefSeconds = root.optDouble("fetchedAt", -63115200000.0)
            val fetchedAtMillis = if (fetchedRefSeconds.isNaN()) {
                Long.MIN_VALUE
            } else {
                (fetchedRefSeconds * 1000.0).toLong() + ZenSnapshot.REFERENCE_EPOCH_MILLIS
            }
            val capabilityRaw = optStringOrNull(root, "normalTabsCapability")
            val capability = NormalTabsCapability.entries
                .firstOrNull { it.name.equals(capabilityRaw, ignoreCase = true) }
                ?: NormalTabsCapability.ABSENT
            ZenSnapshot(spaces, essentials, fetchedAtMillis, root.opt("separateEssentialsPref") as? Boolean, capability)
        } catch (_: Exception) {
            null
        }
    }

    /** Decodes one cached item array (`pinned` / `tabs`) from the snapshot JSON. */
    private fun decodeItems(arr: JSONArray?): List<ZenItem> {
        val items = mutableListOf<ZenItem>()
        arr ?: return items
        for (j in 0 until arr.length()) {
            val entry = arr.optJSONObject(j) ?: continue
            val tabObj = entry.optJSONObject("tab")
            val folderObj = entry.optJSONObject("folder")
            val splitObj = entry.optJSONObject("split")
            when {
                tabObj != null -> decodeCachedTab(tabObj)?.let { items.add(ZenItem.Tab(it)) }
                folderObj != null -> decodeCachedFolder(folderObj)?.let {
                    items.add(ZenItem.Folder(it))
                }
                splitObj != null -> {
                    val sid = splitObj.optString("id")
                    if (sid.isNotEmpty()) {
                        val tabs = mutableListOf<ZenTab>()
                        splitObj.optJSONArray("tabs")?.let { ta ->
                            for (k in 0 until ta.length()) {
                                ta.optJSONObject(k)?.let { to -> decodeCachedTab(to)?.let { tabs.add(it) } }
                            }
                        }
                        items.add(
                            ZenItem.Split(
                                ZenSplit(
                                    id = sid,
                                    gridType = optStringOrNull(splitObj, "gridType"),
                                    tabs = tabs,
                                )
                            )
                        )
                    }
                }
            }
        }
        return items
    }

    private fun decodeCachedFolder(folderObj: JSONObject): ZenFolder? {
        val fid = folderObj.optString("id")
        if (fid.isEmpty()) return null
        val tabs = mutableListOf<ZenTab>()
        folderObj.optJSONArray("tabs")?.let { ta ->
            for (k in 0 until ta.length()) {
                ta.optJSONObject(k)?.let { to -> decodeCachedTab(to)?.let { tabs.add(it) } }
            }
        }
        val subfolders = mutableListOf<ZenFolder>()
        folderObj.optJSONArray("subfolders")?.let { sa ->
            for (k in 0 until sa.length()) {
                sa.optJSONObject(k)?.let { fo -> decodeCachedFolder(fo)?.let { subfolders.add(it) } }
            }
        }
        return ZenFolder(
            id = fid,
            name = folderObj.optString("name", ""),
            icon = optStringOrNull(folderObj, "icon"),
            tabs = tabs,
            subfolders = subfolders.takeIf { it.isNotEmpty() },
        )
    }

    private fun decodeCachedTab(t: JSONObject): ZenTab? {
        val id = optStringOrNull(t, "id") ?: return null
        val url = optStringOrNull(t, "url") ?: return null
        return ZenTab(
            id = id,
            url = url,
            title = t.optString("title", ""),
            iconURL = optStringOrNull(t, "iconURL"),
            icon = optStringOrNull(t, "icon"),
            hasStaticIcon = if (t.has("hasStaticIcon") && !t.isNull("hasStaticIcon")) t.optBoolean("hasStaticIcon") else null,
            staticLabel = optStringOrNull(t, "staticLabel"),
        )
    }

    fun newTabRecordId(): String = UUID.randomUUID().toString().lowercase()
}
