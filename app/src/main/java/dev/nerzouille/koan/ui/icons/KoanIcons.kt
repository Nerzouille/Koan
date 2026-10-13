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
