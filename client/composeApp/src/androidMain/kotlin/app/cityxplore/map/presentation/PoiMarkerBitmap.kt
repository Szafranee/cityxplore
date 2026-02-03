package app.cityxplore.map.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.map.presentation.components.getCategoryIcon
import app.cityxplore.theme.AppColors
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

    val themeColor = if (isMajor) {
        Color.rgb(255, 215, 0) // Gold
    } else {
        getCategoryBaseColor(category)
    }

    val backgroundColor = when {
        discovered -> {
            val detailsSurfaceColor = Color.rgb(42, 42, 42)
            blendColors(detailsSurfaceColor, themeColor, 0.2f)
        }

        else -> {
            dimColor(themeColor)
        }
    }

    // Draw shadow for better visibility
    val shadowPaint = Paint().apply {
        color = Color.BLACK
        alpha = 80
        isAntiAlias = true
        setShadowLayer(actualSize * 0.05f, 0f, actualSize * 0.02f, Color.BLACK)
    }
    val shadowRadius = (actualSize / 2f) * 0.95f
    canvas.drawCircle(centerX, centerY, shadowRadius, shadowPaint)

    val borderPaint = Paint().apply {
        color = getCategoryBaseColor(category)
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    // Border takes up ~90% of radius (leaving room for shadow)
    val borderRadius = (actualSize / 2f) * 0.9f
    canvas.drawCircle(centerX, centerY, borderRadius, borderPaint)

    // Draw main circle background
    val mainCirclePaint = Paint().apply {
        color = backgroundColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val borderThickness = actualSize * 0.03f // ~3% thickness (Thinner)
    val mainRadius = borderRadius - borderThickness
    canvas.drawCircle(centerX, centerY, mainRadius, mainCirclePaint)

    if (discovered) {
        val icon = if (isMajor) Icons.Rounded.Star else getCategoryIcon(category) // Icon size
        val iconTargetSize = mainRadius * 2 * 0.6f

        drawIconOnCanvas(canvas, icon, centerX, centerY, iconTargetSize, themeColor)
    } else {
        // Undiscovered -> Draw X
        val xColor = lightenColor(backgroundColor, 1.3f)

        val xPaint = Paint().apply {
            color = xColor
            strokeWidth = actualSize * 0.1f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val xRadius = mainRadius * 0.5f

        canvas.drawLine(centerX - xRadius, centerY - xRadius, centerX + xRadius, centerY + xRadius, xPaint)
        canvas.drawLine(centerX + xRadius, centerY - xRadius, centerX - xRadius, centerY + xRadius, xPaint)
    }

    return bitmap
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
    targetSize: Float,
    iconColor: Int
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
                drawVectorGroup(icon.root, ComposeColor(iconColor))
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
                // Save the current transformation state
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
 * Returns the base color for a POI category.
 *
 * @param category The POI category
 * @return Android Color int
 */
private fun getCategoryBaseColor(category: PoiCategory): Int {
    return when (category) {
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
}

/**
 * Blends two colors.
 *
 * @param bg Background color
 * @param fg Foreground color
 * @param ratio Ratio of foreground (0.0 = full BG, 1.0 = full FG)
 * @return Blended color
 */
private fun blendColors(bg: Int, fg: Int, ratio: Float): Int {
    val inverseRatio = 1f - ratio
    val r = (Color.red(fg) * ratio + Color.red(bg) * inverseRatio)
    val g = (Color.green(fg) * ratio + Color.green(bg) * inverseRatio)
    val b = (Color.blue(fg) * ratio + Color.blue(bg) * inverseRatio)
    return Color.rgb(r.toInt(), g.toInt(), b.toInt())
}

/**
 * Dims a color by reducing its saturation and brightness.
 * Used for undiscovered POIs on the map to show they haven't been explored yet.
 *
 * @param color The original color
 * @return The dimmed color
 */
private fun dimColor(color: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)

    // Reduce saturation and brightness
    hsv[1] = hsv[1] * 0.4f // Reduce saturation to 40%
    hsv[2] = hsv[2] * 0.5f // Reduce brightness to 50%

    return Color.HSVToColor(hsv)
}

/**
 * Desaturates a color while keeping some of the original hue.
 * Used for undiscovered shared POIs to show a muted gradient.
 *
 * @param color The original color
 * @param saturationFactor How much saturation to keep (0.0 = grayscale, 1.0 = original)
 * @return The desaturated color
 */
private fun desaturateColor(color: Int, saturationFactor: Float): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)

    // Reduce saturation
    hsv[1] = hsv[1] * saturationFactor
    // Slightly reduce brightness too for a more "undiscovered" look
    hsv[2] = hsv[2] * 0.7f

    return Color.HSVToColor(hsv)
}

/**
 * Lightens a color by increasing its brightness.
 * Used for the X symbol on undiscovered POIs to ensure visibility on a dimmed background.
 *
 * @param color The original color
 * @param factor Brightness multiplier (> 1.0 makes it lighter)
 * @return The lightened color
 */
private fun lightenColor(color: Int, factor: Float): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)

    // Increase brightness, but cap at 1.0 (max)
    hsv[2] = (hsv[2] * factor).coerceAtMost(1.0f)

    return Color.HSVToColor(hsv)
}

/**
 * Creates a marker bitmap for a shared POI.
 * Uses a gradient background (category color -> green) with friend badge.
 *
 * For discovered POIs: Full color gradient and white category icon
 * For undiscovered POIs: Desaturated gradient + grey X
 *
 * @param category The POI category (from customPoi.category)
 * @param isDiscovered Whether the shared POI has been discovered
 * @param size The size of the bitmap in pixels
 * @return A Bitmap representing the shared POI marker
 */
fun createSharedPoiMarkerBitmap(
    category: PoiCategory,
    isDiscovered: Boolean,
    size: Int = 100
): Bitmap {
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centerX = size / 2f
    val centerY = size / 2f

    val categoryColor = getCategoryBaseColor(category)
    val greenColor = AppColors.green.toArgb() // Green for shared POIs

    // Draw shadow for better visibility
    val shadowPaint = Paint().apply {
        color = Color.BLACK
        alpha = 80
        isAntiAlias = true
        setShadowLayer(size * 0.05f, 0f, size * 0.02f, Color.BLACK)
    }
    val shadowRadius = (size / 2f) * 0.95f
    canvas.drawCircle(centerX, centerY, shadowRadius, shadowPaint)

    // Gradient colors based on discovery state
    val gradientColor1: Int
    val gradientColor2: Int

    if (isDiscovered) {
        // Full color gradient for discovered
        gradientColor1 = categoryColor
        gradientColor2 = greenColor
    } else {
        // Desaturated/dimmed gradient for undiscovered
        gradientColor1 = desaturateColor(categoryColor, 0.4f)
        gradientColor2 = desaturateColor(greenColor, 0.4f)
    }

    // Ring (gradient border stroke)
    val ringPaint = Paint().apply {
        shader = android.graphics.LinearGradient(
            centerX - shadowRadius, centerY - shadowRadius,
            centerX + shadowRadius, centerY + shadowRadius,
            gradientColor1, gradientColor2,
            android.graphics.Shader.TileMode.CLAMP
        )
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = size * 0.05f // Thicker ring
    }
    val ringRadius = (size / 2f) * 0.9f
    canvas.drawCircle(centerX, centerY, ringRadius, ringPaint)

    val mainRadius = ringRadius - (size * 0.05f) // Adjust the main radius to fit inside the ring

    // Gap between ring and main circle
    val gapPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = size * 0.02f
    }
    canvas.drawCircle(centerX, centerY, mainRadius + (size * 0.01f), gapPaint)

    // Draw gradient background
    val gradientPaint = Paint().apply {
        shader = android.graphics.LinearGradient(
            centerX - mainRadius, centerY - mainRadius,
            centerX + mainRadius, centerY + mainRadius,
            gradientColor1, gradientColor2,
            android.graphics.Shader.TileMode.CLAMP
        )
        isAntiAlias = true
        style = Paint.Style.FILL
        alpha = if (isDiscovered) 255 else 200
    }
    canvas.drawCircle(centerX, centerY, mainRadius, gradientPaint)

    if (isDiscovered) {
        // Discovered: Draw WHITE category icon on gradient
        val icon = getCategoryIcon(category)
        val iconTargetSize = mainRadius * 2 * 0.6f
        drawIconOnCanvas(canvas, icon, centerX, centerY, iconTargetSize, Color.WHITE)
    } else {
        // Undiscovered: Draw grey X
        val xColor = Color.rgb(200, 200, 200)

        val xPaint = Paint().apply {
            color = xColor
            strokeWidth = size * 0.1f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val xRadius = mainRadius * 0.45f
        canvas.drawLine(centerX - xRadius, centerY - xRadius, centerX + xRadius, centerY + xRadius, xPaint)
        canvas.drawLine(centerX + xRadius, centerY - xRadius, centerX - xRadius, centerY + xRadius, xPaint)
    }

    // Draw the friend badge (green circle with person icon) - top right
    val badgeRadius = size * 0.15f
    val badgeX = centerX + mainRadius * 0.6f
    val badgeY = centerY - mainRadius * 0.6f

    // Badge background: slightly darker for undiscovered
    val badgeBgColor = if (isDiscovered) greenColor else dimColor(greenColor)
    val badgePaint = Paint().apply {
        color = badgeBgColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)

    // Badge border
    val badgeBorderPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = size * 0.02f
    }
    canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBorderPaint)

    // Simple person silhouette in badge
    val personPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    // Head
    canvas.drawCircle(badgeX, badgeY - badgeRadius * 0.25f, badgeRadius * 0.3f, personPaint)
    // Body (small arc)
    val bodyPath = android.graphics.Path().apply {
        arcTo(
            badgeX - badgeRadius * 0.5f,
            badgeY,
            badgeX + badgeRadius * 0.5f,
            badgeY + badgeRadius * 0.6f,
            180f,
            180f,
            false
        )
        close()
    }
    canvas.drawPath(bodyPath, personPaint)


    return bitmap
}
