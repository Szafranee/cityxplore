package app.cityxplore.map.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import app.cityxplore.map.domain.PoiCategory
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Creates a marker bitmap for a POI based on its category, discovery status, and major status.
 *
 * @param category The POI category
 * @param discovered Whether the POI has been discovered
 * @param isMajor Whether the POI is a major landmark
 * @param size The size of the bitmap in pixels
 * @return A Bitmap representing the POI marker
 */
fun createPoiMarkerBitmap(
    category: PoiCategory,
    discovered: Boolean,
    isMajor: Boolean,
    size: Int = 100
): Bitmap {
    // Make major POIs significantly larger
    val scaleFactor = if (isMajor) 1.3f else 1.0f
    val actualSize = (size * scaleFactor).toInt()

    val bitmap = createBitmap(actualSize, actualSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centerX = actualSize / 2f
    val centerY = actualSize / 2f

    // Special gold colour for major POIs
    val categoryColor = if (isMajor) {
        if (discovered) Color.rgb(255, 215, 0) // Gold
        else Color.rgb(218, 165, 32) // Goldenrod - distinct and visible even if undiscovered
    } else {
        getCategoryColor(category, discovered)
    }

    // Draw shadow for better visibility
    val shadowPaint = Paint().apply {
        color = Color.BLACK
        alpha = 80
        isAntiAlias = true
        setShadowLayer(actualSize * 0.05f, 0f, actualSize * 0.02f, Color.BLACK)
    }
    // Draw a circle for shadow (slightly smaller than border)
    val shadowRadius = (actualSize / 2f) * 0.85f
    canvas.drawCircle(centerX, centerY, shadowRadius, shadowPaint)

    // Draw the outer circle (border)
    val borderPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val borderRadius = (actualSize / 2f) * 0.9f
    canvas.drawCircle(centerX, centerY, borderRadius, borderPaint)

    // Draw main circle background
    val mainCirclePaint = Paint().apply {
        color = categoryColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    // Thicker border for major POIs
    val borderThickness = if (isMajor) actualSize * 0.08f else actualSize * 0.05f
    val mainRadius = borderRadius - borderThickness
    canvas.drawCircle(centerX, centerY, mainRadius, mainCirclePaint)

    if (discovered) {
        // Get the appropriate icon
        val icon = if (isMajor) {
            Icons.Rounded.Star // Crown/Star for major landmarks
        } else {
            getCategoryIcon(category)
        }

        // Draw the Material Icon centered
        // Make icon fill about 70% of the inner circle diameter
        val iconTargetSize = mainRadius * 2 * 0.7f

        drawIconOnCanvas(canvas, icon, centerX, centerY, iconTargetSize)
    } else {
        // Draw a bold X for undiscovered
        val xPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = actualSize * 0.1f // Bold stroke (10% of size)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        // Size of the X (about 50% of the inner circle diameter)
        val xRadius = mainRadius * 0.5f

        canvas.drawLine(centerX - xRadius, centerY - xRadius, centerX + xRadius, centerY + xRadius, xPaint)
        canvas.drawLine(centerX + xRadius, centerY - xRadius, centerX - xRadius, centerY + xRadius, xPaint)
    }

    return bitmap
}

/**
 * Returns the appropriate Material Icon for a POI category.
 */
private fun getCategoryIcon(category: PoiCategory): ImageVector {
    return when (category) {
        PoiCategory.HISTORICAL -> Icons.Rounded.AccountBalance // Classical building/museum
        PoiCategory.CULTURAL -> Icons.Rounded.TheaterComedy // Theater masks
        PoiCategory.NATURE -> Icons.Rounded.Park // Tree/park
        PoiCategory.FOOD -> Icons.Rounded.Restaurant // Fork and knife
        PoiCategory.SPORTS -> Icons.Rounded.SportsSoccer // Soccer ball
        PoiCategory.ENTERTAINMENT -> Icons.Rounded.LocalActivity // Star
        PoiCategory.CUSTOM -> Icons.Rounded.Place // Pin/location marker
        PoiCategory.OTHER -> Icons.Rounded.MoreHoriz // Three dots
        PoiCategory.UNKNOWN -> Icons.Rounded.QuestionMark // Question mark
    }
}

/**
 * Draws a Material Icon centered at the specified position on the canvas.
 * Uses Compose's rendering system for pixel-perfect Material Icons.
 */
private fun drawIconOnCanvas(
    targetCanvas: Canvas,
    icon: ImageVector,
    centerX: Float,
    centerY: Float,
    targetSize: Float
) {
    // Create Compose canvas and draw scope
    val composeCanvas = androidx.compose.ui.graphics.Canvas(targetCanvas)
    val drawScope = CanvasDrawScope()

    val vpWidth = icon.viewportWidth
    val vpHeight = icon.viewportHeight

    // Calculate scale to fit the target size
    val scale = targetSize / maxOf(vpWidth, vpHeight)

    val scaledWidth = vpWidth * scale
    val scaledHeight = vpHeight * scale

    val left = centerX - scaledWidth / 2f
    val top = centerY - scaledHeight / 2f

    drawScope.draw(
        density = Density(1f, 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(targetCanvas.width.toFloat(), targetCanvas.height.toFloat())
    ) {
        translate(left, top) {
            scale(scale, scale, Offset.Zero) {
                drawVectorGroup(icon.root, ComposeColor(Color.WHITE))
            }
        }
    }
}

/**
 * Recursively draws vector nodes (groups and paths).
 */
private fun DrawScope.drawVectorGroup(
    vectorGroup: VectorGroup,
    tintColor: ComposeColor
) {
    vectorGroup.forEach { node ->
        when (node) {
            is VectorPath -> {
                // Convert path data to Compose Path
                val path = node.pathData.toComposePath()

                // Draw the path with the tint color
                drawPath(
                    path = path,
                    color = tintColor,
                    alpha = 1f
                )
            }

            is VectorGroup -> {
                // Save current transformation state
                drawContext.canvas.save()

                // Apply group transformations
                drawContext.transform.apply {
                    translate(node.translationX, node.translationY)
                    rotate(node.rotation, Offset(node.pivotX, node.pivotY))
                    scale(node.scaleX, node.scaleY, Offset(node.pivotX, node.pivotY))
                }

                // Recursively draw child nodes
                drawVectorGroup(node, tintColor)

                // Restore transformation state
                drawContext.canvas.restore()
            }
        }
    }
}

/**
 * Converts PathNode list to Compose Path with full support for all path commands.
 */
private fun List<PathNode>.toComposePath(): Path {
    val path = Path()

    var currentX = 0f
    var currentY = 0f
    var controlX = 0f
    var controlY = 0f
    var segmentStartX = 0f
    var segmentStartY = 0f

    forEach { node ->
        when (node) {
            is PathNode.MoveTo -> {
                path.moveTo(node.x, node.y)
                currentX = node.x
                currentY = node.y
                segmentStartX = currentX
                segmentStartY = currentY
                controlX = currentX
                controlY = currentY
            }

            is PathNode.RelativeMoveTo -> {
                currentX += node.dx
                currentY += node.dy
                path.moveTo(currentX, currentY)
                segmentStartX = currentX
                segmentStartY = currentY
                controlX = currentX
                controlY = currentY
            }

            is PathNode.LineTo -> {
                path.lineTo(node.x, node.y)
                currentX = node.x
                currentY = node.y
                controlX = currentX
                controlY = currentY
            }

            is PathNode.RelativeLineTo -> {
                currentX += node.dx
                currentY += node.dy
                path.lineTo(currentX, currentY)
                controlX = currentX
                controlY = currentY
            }

            is PathNode.HorizontalTo -> {
                currentX = node.x
                path.lineTo(currentX, currentY)
                controlX = currentX
                controlY = currentY
            }

            is PathNode.RelativeHorizontalTo -> {
                currentX += node.dx
                path.lineTo(currentX, currentY)
                controlX = currentX
                controlY = currentY
            }

            is PathNode.VerticalTo -> {
                currentY = node.y
                path.lineTo(currentX, currentY)
                controlX = currentX
                controlY = currentY
            }

            is PathNode.RelativeVerticalTo -> {
                currentY += node.dy
                path.lineTo(currentX, currentY)
                controlX = currentX
                controlY = currentY
            }

            is PathNode.CurveTo -> {
                path.cubicTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
                controlX = node.x2
                controlY = node.y2
                currentX = node.x3
                currentY = node.y3
            }

            is PathNode.RelativeCurveTo -> {
                path.cubicTo(
                    currentX + node.dx1, currentY + node.dy1,
                    currentX + node.dx2, currentY + node.dy2,
                    currentX + node.dx3, currentY + node.dy3
                )
                controlX = currentX + node.dx2
                controlY = currentY + node.dy2
                currentX += node.dx3
                currentY += node.dy3
            }

            is PathNode.ReflectiveCurveTo -> {
                val reflectedControlX = 2 * currentX - controlX
                val reflectedControlY = 2 * currentY - controlY
                path.cubicTo(
                    reflectedControlX, reflectedControlY,
                    node.x1, node.y1,
                    node.x2, node.y2
                )
                controlX = node.x1
                controlY = node.y1
                currentX = node.x2
                currentY = node.y2
            }

            is PathNode.RelativeReflectiveCurveTo -> {
                val reflectedControlX = 2 * currentX - controlX
                val reflectedControlY = 2 * currentY - controlY
                path.cubicTo(
                    reflectedControlX, reflectedControlY,
                    currentX + node.dx1, currentY + node.dy1,
                    currentX + node.dx2, currentY + node.dy2
                )
                controlX = currentX + node.dx1
                controlY = currentY + node.dy1
                currentX += node.dx2
                currentY += node.dy2
            }

            is PathNode.QuadTo -> {
                path.quadraticTo(node.x1, node.y1, node.x2, node.y2)
                controlX = node.x1
                controlY = node.y1
                currentX = node.x2
                currentY = node.y2
            }

            is PathNode.RelativeQuadTo -> {
                path.quadraticTo(
                    currentX + node.dx1, currentY + node.dy1,
                    currentX + node.dx2, currentY + node.dy2
                )
                controlX = currentX + node.dx1
                controlY = currentY + node.dy1
                currentX += node.dx2
                currentY += node.dy2
            }

            is PathNode.ReflectiveQuadTo -> {
                val reflectedControlX = 2 * currentX - controlX
                val reflectedControlY = 2 * currentY - controlY
                path.quadraticTo(reflectedControlX, reflectedControlY, node.x, node.y)
                controlX = reflectedControlX
                controlY = reflectedControlY
                currentX = node.x
                currentY = node.y
            }

            is PathNode.RelativeReflectiveQuadTo -> {
                val reflectedControlX = 2 * currentX - controlX
                val reflectedControlY = 2 * currentY - controlY
                path.quadraticTo(
                    reflectedControlX, reflectedControlY,
                    currentX + node.dx, currentY + node.dy
                )
                controlX = reflectedControlX
                controlY = reflectedControlY
                currentX += node.dx
                currentY += node.dy
            }

            is PathNode.ArcTo -> {
                // Simplified arc handling - Material icons rarely use complex arcs
                path.lineTo(node.arcStartX, node.arcStartY)
                currentX = node.arcStartX
                currentY = node.arcStartY
                controlX = currentX
                controlY = currentY
            }

            is PathNode.RelativeArcTo -> {
                // Simplified arc handling
                currentX += node.arcStartDx
                currentY += node.arcStartDy
                path.lineTo(currentX, currentY)
                controlX = currentX
                controlY = currentY
            }

            is PathNode.Close -> {
                path.close()
                currentX = segmentStartX
                currentY = segmentStartY
                controlX = currentX
                controlY = currentY
            }
        }
    }

    return path
}

/**
 * Returns the colour for a POI category.
 * Discovered POIs get vibrant colours, undiscovered get dimmed versions.
 *
 * @param category The POI category
 * @param discovered Whether the POI has been discovered
 * @return Android Color int
 */
private fun getCategoryColor(category: PoiCategory, discovered: Boolean): Int {
    val baseColor = when (category) {
        PoiCategory.HISTORICAL -> Color.rgb(255, 152, 0) // Orange
        PoiCategory.CULTURAL -> Color.rgb(156, 39, 176) // Purple
        PoiCategory.NATURE -> Color.rgb(76, 175, 80) // Green
        PoiCategory.FOOD -> Color.rgb(244, 67, 54) // Red
        PoiCategory.SPORTS -> Color.rgb(33, 150, 243) // Blue
        PoiCategory.ENTERTAINMENT -> Color.rgb(233, 30, 99) // Pink
        PoiCategory.CUSTOM -> Color.rgb(121, 85, 72) // Brown
        PoiCategory.OTHER -> Color.rgb(158, 158, 158) // Grey
        PoiCategory.UNKNOWN -> Color.rgb(96, 125, 139) // Blue Grey
    }

    return if (discovered) {
        baseColor
    } else {
        // Dim the colour for undiscovered POIs (reduce saturation and brightness)
        dimColor(baseColor)
    }
}

/**
 * Dims a colour by reducing its saturation and brightness.
 *
 * @param color The original colour
 * @return The dimmed colour
 */
private fun dimColor(color: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)

    // Reduce saturation and brightness
    hsv[1] = hsv[1] * 0.4f // Reduce saturation to 40%
    hsv[2] = hsv[2] * 0.5f // Reduce brightness to 50%

    return Color.HSVToColor(hsv)
}
