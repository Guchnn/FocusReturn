package com.guchnn.focusreturn

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Tab icons for the bottom navigation.
 *
 * Miuix ships `MiuixIcons` (156 extended + 7 basic glyphs), but the container
 * object lives in a module whose package we could not pin down offline, and a
 * missing icon import is a compile error. These four glyphs are built locally
 * from primitives so the navigation bar has zero external icon dependency.
 */
object TabIcons {

    private fun build(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    /** Circle helper: two arcs make a full circle. */
    private fun androidx.compose.ui.graphics.vector.PathBuilder.circle(
        cx: Float, cy: Float, r: Float,
    ) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + r, y1 = cy)
        close()
    }

    /** 焦点：圆环（取景框）。 */
    val Focus: ImageVector by lazy {
        build("tab_focus") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 8.5f)
                circle(12f, 12f, 4f)
            }
        }
    }

    /** 样式：圆环 + 右侧实心半格（对比/外观）。 */
    val Style: ImageVector by lazy {
        build("tab_style") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 8.5f)
                circle(12f, 12f, 4f)
                moveTo(12f, 2.5f)
                lineTo(21f, 2.5f)
                lineTo(21f, 21.5f)
                lineTo(12f, 21.5f)
                close()
            }
        }
    }

    /** 高级：两条滑轨 + 两个滑块。 */
    val Advanced: ImageVector by lazy {
        build("tab_advanced") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            ) {
                moveTo(3.5f, 8.5f)
                lineTo(20.5f, 8.5f)
                moveTo(3.5f, 15.5f)
                lineTo(20.5f, 15.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                circle(9f, 8.5f, 2.6f)
                circle(15f, 15.5f, 2.6f)
            }
        }
    }

    /** 关于：圆环 + 中间竖条（信息）。 */
    val About: ImageVector by lazy {
        build("tab_about") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 8.5f)
                circle(12f, 12f, 6.5f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            ) {
                moveTo(12f, 11f)
                lineTo(12f, 16.5f)
                moveTo(12f, 7.5f)
                lineTo(12f, 8f)
            }
        }
    }

    /**
     * 重启系统界面：经典「刷新 / 重启」字形（Material refresh 同构）。
     *
     * 圆环在**右上角留出约 90° 缺口**，缺口处一枚明显大于线宽的实心箭头指向
     * 顺时针前进方向 —— 一眼就能认出是「重启」，而不是一个空圆圈。
     */
    val Restart: ImageVector by lazy {
        build("restart") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.4f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            ) {
                // 圆心(12,12) 半径 8：自 10° 顺时针扫 270° 到 280°，右上留 90° 缺口。
                moveTo(19.88f, 13.39f)
                arcTo(8f, 8f, 0f, true, true, 13.39f, 4.12f)
            }
            // 实心箭头，接在环的末端、指向顺时针前进方向（右偏上）。
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.53f, 4.85f)
                lineTo(12.96f, 6.58f)
                lineTo(13.82f, 1.66f)
                close()
            }
        }
    }

    /** 展开/收起指示：下箭头（Material expand_more 字形），展开时旋转 180°。 */
    val Chevron: ImageVector by lazy {
        build("chevron") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.59f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6f, 10f)
                lineTo(12f, 16f)
                lineTo(18f, 10f)
                close()
            }
        }
    }
}
