package dev.nerzouille.koan.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small stroked icon set (24dp grid) so we don't need material-icons-extended. */
object KoanIcons {
    val Back: ImageVector by lazy { stroked("back") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) } }
    val Forward: ImageVector by lazy { stroked("forward") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) } }
    val Close: ImageVector by lazy {
        stroked("close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    }
    val Reload: ImageVector by lazy {
        stroked("reload") {
            // 300° arc + arrow head
            moveTo(19f, 12f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 16.5f, y1 = 6.6f)
            moveTo(19f, 4.5f); lineTo(19f, 9f); lineTo(14.5f, 9f)
        }
    }
    val History: ImageVector by lazy {
        stroked("history") {
            moveTo(20f, 12f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4f, y1 = 12f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 20f, y1 = 12f)
            moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
        }
    }
    val Plus: ImageVector by lazy {
        stroked("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    }
    val Search: ImageVector by lazy {
        stroked("search") {
            moveTo(17.5f, 10.5f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3.5f, y1 = 10.5f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 17.5f, y1 = 10.5f)
            moveTo(15.5f, 15.5f); lineTo(20.5f, 20.5f)
        }
    }
    val Pin: ImageVector by lazy {
        stroked("pin") {
            moveTo(9f, 4f); lineTo(15f, 4f); moveTo(10f, 4f); lineTo(10f, 10f); lineTo(7f, 14f); lineTo(17f, 14f)
            lineTo(14f, 10f); lineTo(14f, 4f); moveTo(12f, 14f); lineTo(12f, 20f)
        }
    }
    val Folder: ImageVector by lazy {
        stroked("folder") {
            moveTo(4f, 7f); lineTo(4f, 18f); lineTo(20f, 18f); lineTo(20f, 9f); lineTo(12f, 9f)
            lineTo(10f, 6f); lineTo(4f, 6f); lineTo(4f, 7f)
        }
    }
    val ChevronDown: ImageVector by lazy { stroked("chevron-down") { moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f) } }
    val ChevronRight: ImageVector by lazy { stroked("chevron-right") { moveTo(10f, 7f); lineTo(15f, 12f); lineTo(10f, 17f) } }
    val Check: ImageVector by lazy { stroked("check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) } }
    val Link: ImageVector by lazy {
        stroked("link") {
            moveTo(10f, 14f); lineTo(14f, 10f)
            moveTo(11f, 7f); lineTo(13f, 5f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 11f)
            lineTo(17f, 13f)
            moveTo(13f, 17f); lineTo(11f, 19f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5f, y1 = 13f)
            lineTo(7f, 11f)
        }
    }
    val Star: ImageVector by lazy {
        stroked("star") {
            moveTo(12f, 4f); lineTo(14.4f, 9f); lineTo(19.8f, 9.6f); lineTo(15.8f, 13.3f); lineTo(16.9f, 18.7f)
            lineTo(12f, 16f); lineTo(7.1f, 18.7f); lineTo(8.2f, 13.3f); lineTo(4.2f, 9.6f); lineTo(9.6f, 9f); close()
        }
    }
    val StarFilled: ImageVector by lazy {
        ImageVector.Builder("star-filled", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4f); lineTo(14.4f, 9f); lineTo(19.8f, 9.6f); lineTo(15.8f, 13.3f); lineTo(16.9f, 18.7f)
                lineTo(12f, 16f); lineTo(7.1f, 18.7f); lineTo(8.2f, 13.3f); lineTo(4.2f, 9.6f); lineTo(9.6f, 9f); close()
            }
        }.build()
    }
    val ArrowUp: ImageVector by lazy { stroked("arrow-up") { moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f) } }
    val Share: ImageVector by lazy {
        stroked("share") {
            moveTo(12f, 4f); lineTo(12f, 15f); moveTo(8f, 8f); lineTo(12f, 4f); lineTo(16f, 8f)
            moveTo(8f, 11f); lineTo(6f, 11f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 11f); lineTo(16f, 11f)
        }
    }
    val Lock: ImageVector by lazy {
        stroked("lock") {
            moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 20f); lineTo(6f, 20f); close()
            moveTo(8.5f, 11f); lineTo(8.5f, 8f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.5f, y1 = 8f)
            lineTo(15.5f, 11f)
        }
    }
    val Desktop: ImageVector by lazy {
        stroked("desktop") {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 16f); lineTo(4f, 16f); close()
            moveTo(9f, 20f); lineTo(15f, 20f); moveTo(12f, 16f); lineTo(12f, 20f)
        }
    }
    val Reader: ImageVector by lazy {
        stroked("reader") {
            moveTo(5f, 6f); lineTo(19f, 6f); moveTo(5f, 10f); lineTo(19f, 10f)
            moveTo(5f, 14f); lineTo(19f, 14f); moveTo(5f, 18f); lineTo(13f, 18f)
        }
    }
    val Essentials: ImageVector by lazy {
        stroked("essentials") {
            for ((x, y) in listOf(5f to 5f, 13f to 5f, 5f to 13f, 13f to 13f)) {
                moveTo(x, y); lineTo(x + 6f, y); lineTo(x + 6f, y + 6f); lineTo(x, y + 6f); close()
            }
        }
    }
    val Download: ImageVector by lazy {
        stroked("download") { moveTo(12f, 4f); lineTo(12f, 15f); moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f); moveTo(5f, 20f); lineTo(19f, 20f) }
    }
    val Settings: ImageVector by lazy {
        stroked("settings") {
            // A gear: eight teeth around a ring, and the hub.
            val teeth = 8
            val step = 2 * Math.PI / teeth
            fun at(r: Float, a: Double) = (12f + r * Math.cos(a).toFloat()) to (12f + r * Math.sin(a).toFloat())
            for (i in 0 until teeth) {
                val c = i * step
                val (x0, y0) = at(7.2f, c - step * 0.32)
                val (x1, y1) = at(9.6f, c - step * 0.18)
                val (x2, y2) = at(9.6f, c + step * 0.18)
                val (x3, y3) = at(7.2f, c + step * 0.32)
                val (x4, y4) = at(7.2f, c + step * 0.68)
                if (i == 0) moveTo(x0, y0)
                lineTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3)
                arcTo(7.2f, 7.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = x4, y1 = y4)
            }
            close()
            circle(12f, 12f, 2.8f)
        }
    }
    val Location: ImageVector by lazy {
        stroked("location") {
            moveTo(12f, 21f)
            curveTo(8f, 16.5f, 5.5f, 13f, 5.5f, 9.5f)
            arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18.5f, y1 = 9.5f)
            curveTo(18.5f, 13f, 16f, 16.5f, 12f, 21f)
            close()
            circle(12f, 9.5f, 2.3f)
        }
    }
    val Camera: ImageVector by lazy {
        stroked("camera") {
            moveTo(3.5f, 7.5f); lineTo(15.5f, 7.5f); lineTo(15.5f, 17f); lineTo(3.5f, 17f); close()
            moveTo(15.5f, 11f); lineTo(20.5f, 8f); lineTo(20.5f, 16.5f); lineTo(15.5f, 13.5f)
        }
    }
    val Mic: ImageVector by lazy {
        stroked("mic") {
            moveTo(9f, 6.5f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15f, y1 = 6.5f)
            lineTo(15f, 11.5f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 9f, y1 = 11.5f)
            close()
            moveTo(6f, 11f); curveTo(6f, 18f, 18f, 18f, 18f, 11f); moveTo(12f, 16.5f); lineTo(12f, 20.5f)
        }
    }
    val Database: ImageVector by lazy {
        stroked("database") {
            moveTo(5f, 6f); curveTo(5f, 3.5f, 19f, 3.5f, 19f, 6f); curveTo(19f, 8.5f, 5f, 8.5f, 5f, 6f)
            lineTo(5f, 18f); curveTo(5f, 20.5f, 19f, 20.5f, 19f, 18f); lineTo(19f, 6f)
            moveTo(5f, 12f); curveTo(5f, 14.5f, 19f, 14.5f, 19f, 12f)
        }
    }
    val Pause: ImageVector by lazy { stroked("pause") { moveTo(9f, 6f); lineTo(9f, 18f); moveTo(15f, 6f); lineTo(15f, 18f) } }
    val Play: ImageVector by lazy { stroked("play") { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() } }
    val File: ImageVector by lazy {
        stroked("file") {
            moveTo(6f, 3f); lineTo(14f, 3f); lineTo(19f, 8f); lineTo(19f, 21f); lineTo(6f, 21f); close()
            moveTo(14f, 3f); lineTo(14f, 8f); lineTo(19f, 8f)
        }
    }
    val Trash: ImageVector by lazy {
        stroked("trash") {
            moveTo(4f, 7f); lineTo(20f, 7f); moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
            moveTo(6f, 7f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 7f)
        }
    }
    val Open: ImageVector by lazy {
        stroked("open") {
            moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); moveTo(20f, 4f); lineTo(11f, 13f)
            moveTo(18f, 14f); lineTo(18f, 20f); lineTo(4f, 20f); lineTo(4f, 6f); lineTo(10f, 6f)
        }
    }
    val Retry: ImageVector get() = Reload
    val Info: ImageVector by lazy {
        stroked("info") {
            circle(12f, 12f, 8.5f)
            moveTo(12f, 11f); lineTo(12f, 16f); moveTo(12f, 8f); lineTo(12f, 8.01f)
        }
    }
    val Key: ImageVector by lazy {
        stroked("key") {
            circle(8f, 15f, 3.5f)
            moveTo(10.5f, 12.5f); lineTo(19f, 4f); moveTo(16f, 7f); lineTo(18.5f, 9.5f); moveTo(14f, 9f); lineTo(16f, 11f)
        }
    }
    val Globe: ImageVector by lazy {
        stroked("globe") {
            circle(12f, 12f, 8.5f)
            moveTo(3.5f, 12f); lineTo(20.5f, 12f)
            moveTo(12f, 3.5f)
            curveTo(9.5f, 6f, 8.5f, 9f, 8.5f, 12f); curveTo(8.5f, 15f, 9.5f, 18f, 12f, 20.5f)
            moveTo(12f, 3.5f)
            curveTo(14.5f, 6f, 15.5f, 9f, 15.5f, 12f); curveTo(15.5f, 15f, 14.5f, 18f, 12f, 20.5f)
        }
    }
    val Contrast: ImageVector by lazy {
        ImageVector.Builder("contrast", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2f) { circle(12f, 12f, 8.5f) }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3.5f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 12f, y1 = 20.5f)
                close()
            }
        }.build()
    }
    val Book: ImageVector by lazy {
        stroked("book") {
            moveTo(12f, 6.5f); curveTo(10f, 5f, 7f, 4.5f, 4f, 5f); lineTo(4f, 18.5f); curveTo(7f, 18f, 10f, 18.5f, 12f, 20f)
            curveTo(14f, 18.5f, 17f, 18f, 20f, 18.5f); lineTo(20f, 5f); curveTo(17f, 4.5f, 14f, 5f, 12f, 6.5f); lineTo(12f, 20f)
        }
    }
    val Shield: ImageVector by lazy {
        stroked("shield") {
            moveTo(12f, 3.5f); lineTo(19f, 6f); lineTo(19f, 11f)
            curveTo(19f, 15.5f, 16f, 18.8f, 12f, 20.5f)
            curveTo(8f, 18.8f, 5f, 15.5f, 5f, 11f)
            lineTo(5f, 6f); close()
        }
    }
    val More: ImageVector by lazy {
        ImageVector.Builder("more", 24.dp, 24.dp, 24f, 24f).apply {
            for (x in listOf(5f, 12f, 19f)) {
                path(fill = SolidColor(Color.White)) {
                    moveTo(x - 1.6f, 12f)
                    arcTo(1.6f, 1.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x + 1.6f, y1 = 12f)
                    arcTo(1.6f, 1.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x - 1.6f, y1 = 12f)
                    close()
                }
            }
        }.build()
    }

    private fun stroked(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()
}

/** A full circle as two arcs (path builders have no circle). */
private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
    arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
    close()
}
