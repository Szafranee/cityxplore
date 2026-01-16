package app.cityxplore.map.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cityxplore.map.domain.MapPoi

/** Gold color for major landmarks. */
private val MajorLandmarkColor = Color(0xFFFFD700)

/**
 * A composable marker icon for POIs, matching the style used in POI details.
 *
 * Displays a circular marker with:
 * - Category-colored border
 * - Dark background with subtle category color tint (for discovered POIs)
 * - Category icon (or star for major landmarks)
 * - "X" overlay for undiscovered POIs
 *
 * @param poi The [MapPoi] to display.
 * @param modifier Modifier for the marker container.
 * @param size The diameter of the marker.
 */
@Composable
fun PoiCategoryMarker(
    poi: MapPoi,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val categoryColor = getCategoryColor(poi.category)
    val themeColor = if (poi.isMajor) MajorLandmarkColor else categoryColor

    // Background matches PoiDetailsContent style: dark surface with 20% category color tint
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = if (poi.discovered) {
        themeColor.copy(alpha = 0.2f).compositeOver(surfaceColor)
    } else {
        // Dimmed variant for undiscovered POIs
        themeColor.copy(alpha = 0.5f).compositeOver(Color.Black)
    }

    val borderThickness = size * 0.03f
    val borderColor = if (poi.isMajor) MajorLandmarkColor else categoryColor

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .background(borderColor, CircleShape)
            .padding(borderThickness)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (poi.discovered) {
            DiscoveredPoiIcon(
                poi = poi,
                themeColor = themeColor,
                iconSize = (size - borderThickness * 2) * 0.6f
            )
        } else {
            UndiscoveredPoiOverlay(
                themeColor = themeColor,
                size = size,
                borderThickness = borderThickness
            )
        }
    }
}

/**
 * Icon displayed for discovered POIs.
 */
@Composable
private fun DiscoveredPoiIcon(
    poi: MapPoi,
    themeColor: Color,
    iconSize: Dp
) {
    val icon = if (poi.isMajor) Icons.Rounded.Star else getCategoryIcon(poi.category)

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(iconSize),
        tint = themeColor
    )
}

/**
 * "X" overlay drawn on undiscovered POIs.
 */
@Composable
private fun UndiscoveredPoiOverlay(
    themeColor: Color,
    size: Dp,
    borderThickness: Dp
) {
    // X color should contrast with the dark background - use lightened theme color
    val xColor = themeColor.copy(alpha = 0.7f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = size.toPx() * 0.1f
        val radius = size.toPx() / 2f - borderThickness.toPx()
        val xRadius = radius * 0.5f
        val center = size.toPx() / 2f

        drawLine(
            color = xColor,
            start = Offset(center - xRadius, center - xRadius),
            end = Offset(center + xRadius, center + xRadius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = xColor,
            start = Offset(center + xRadius, center - xRadius),
            end = Offset(center - xRadius, center + xRadius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
