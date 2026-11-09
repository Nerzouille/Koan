package dev.nerzouille.koan.ui.icons

import android.content.Context
import android.graphics.Bitmap
import dev.nerzouille.koan.favicon.SvgRasterizer
import org.json.JSONObject

/**
 * Space and folder icons picked from the Zia mod's icon picker (github.com/z1n-k/zia): Tabler
 * icons (MIT), stored by Zen as `resource://zia-tabler/<outline|filled>/<name>.svg`. Zia before
 * 2.42 used Phosphor icons (`chrome://sine/content/zia/icons/phosphor/<name>.svg`), mapped to
 * the closest Tabler icon with Zia's own table; 2.42–2.58 used loose files
 * (`chrome://sine/content/zia/icons/tabler/<style>/<name>.svg`). A name the bundle doesn't have
 * (a newer pack) takes the closest one by spelling. The user's own SVGs
 * (`resource://zia-own-icons/…`) live only on the computer: no icon.
 *
 * The icons are in assets/zia (made from Zia's icons/tabler-bundle.js and
 * phosphor-to-tabler.js), read once, the first time a Zia icon is drawn.
 */
object ZiaIcons {
    /** "outline"/"filled" and the icon's name, if [raw] is a Zia icon; null otherwise. */
    fun parse(raw: String): Pair<String, String>? {
        val s = raw.trim()
        val (style, name) = when {
            s.startsWith(TABLER) -> s.removePrefix(TABLER).split('/', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            s.startsWith(LOOSE) -> s.removePrefix(LOOSE).split('/', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            s.startsWith(PHOSPHOR) -> "phosphor" to s.removePrefix(PHOSPHOR)
            else -> null
        } ?: return null
        val clean = name.removeSuffix(".svg").trim()
        return if (clean.isEmpty()) null else style to clean
    }

    /** Whether [raw] points at a Zia icon (drawn here, or a computer-only one drawn as nothing). */
    fun isZia(raw: String) = parse(raw) != null || raw.trim().startsWith(OWN)

    /** The icon as an SVG document, black strokes (tinted when drawn). */
    fun svg(context: Context, raw: String): String? {
        val (styleIn, nameIn) = parse(raw) ?: return null
        val bundle = load(context) ?: return null
        var style = if (styleIn == "filled") "filled" else "outline"
        var name = if (styleIn == "phosphor") bundle.phosphor[nameIn] ?: nameIn else nameIn
        val inStyle = bundle.icons[style].orEmpty()
        val drawing = inStyle[name]
            ?: bundle.icons["outline"]?.get(name)?.also { style = "outline" }
            ?: closest(name, bundle.icons["outline"].orEmpty().keys)?.let { near ->
                name = near
                style = "outline"
                bundle.icons["outline"]?.get(near)
            }
            ?: return null
        val head = bundle.heads[style] ?: return null
        return (head + drawing + "</svg>").replace("context-fill-opacity", "1").replace("context-fill", "#000000")
    }

    fun bitmap(context: Context, raw: String, sizePx: Int): Bitmap? =
        svg(context, raw)?.let { SvgRasterizer.rasterize(it, sizePx, "zia:$raw:$sizePx") }

    /** The closest of [names] to [name] by edit distance (ties: the shorter); null if none is near. */
    fun closest(name: String, names: Collection<String>): String? {
        var best: String? = null
        var bestDist = Int.MAX_VALUE
        for (n in names) {
            val d = levenshtein(name, n)
            if (d < bestDist || (d == bestDist && best != null && n.length < best.length)) {
                best = n
                bestDist = d
            }
        }
        // Too far off to mean the same thing: better no icon than a random one.
        return best?.takeIf { bestDist <= maxOf(2, name.length / 2) }
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    private class Bundle(val heads: Map<String, String>, val icons: Map<String, Map<String, String>>, val phosphor: Map<String, String>)

    @Volatile private var bundle: Bundle? = null

    @Synchronized
    private fun load(context: Context): Bundle? {
        bundle?.let { return it }
        return runCatching {
            val assets = context.applicationContext.assets
            val root = JSONObject(assets.open("zia/tabler.json").bufferedReader().use { it.readText() })
            val head = root.getJSONObject("head")
            val icons = root.getJSONObject("icons")
            fun map(o: JSONObject) = o.keys().asSequence().associateWith { o.getString(it) }
            val map = JSONObject(assets.open("zia/phosphor-to-tabler.json").bufferedReader().use { it.readText() })
            Bundle(map(head), icons.keys().asSequence().associateWith { map(icons.getJSONObject(it)) }, map(map))
        }.getOrNull()?.also { bundle = it }
    }

    private const val TABLER = "resource://zia-tabler/"
    private const val LOOSE = "chrome://sine/content/zia/icons/tabler/"
    private const val PHOSPHOR = "chrome://sine/content/zia/icons/phosphor/"
    private const val OWN = "resource://zia-own-icons/"
}
