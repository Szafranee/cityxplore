package app.cityxplore.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cityxplore.map.domain.MapPoi

@Composable
fun PoiCategoryMarker(
    poi: MapPoi,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val categoryColor = if (poi.isMajor) {
        if (poi.discovered) Color(255, 215, 0) // Gold
        else Color(218, 165, 32) // Goldenrod
    } else {
        if (poi.discovered) getCategoryColor(poi.category)
        // If undiscovered, on map it is dimmed, but logic in PoiMarkerBitmap is to use getCategoryColor then modify.
        // But for Details panel (which appears after click), we typically want to show the full color if we are looking at details?
        // Or if the user clicked an UNDISCOVERED POI, do we show it gray?
        // The user said "use the same markers as on the map".
        else getCategoryColor(poi.category).copy(alpha = 0.5f) // Approximation of dimmed
    }

    // Border logic from PoiMarkerBitmap:
    // White border (10% of radius). Major: thicker.

    val borderThickness = if (poi.isMajor) (size * 0.08f) else (size * 0.05f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .background(Color.White, CircleShape) // Border
            .padding(borderThickness) // Inner circle inset
            .background(categoryColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (poi.discovered || poi.isMajor) {
            val icon = if (poi.isMajor) Icons.Rounded.Star else getCategoryIcon(poi.category)

            // Icon size is approx 70% of inner circle
            val iconSize = (size - borderThickness * 2) * 0.7f

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = Color.White
            )
        } else {
            // Undiscovered X not implemented here as we likely want to see the icon in details anyway?
            // But map shows X. If we want 1:1 match:
            // Let's stick to showing the icon for details panel context as a discovered state preview?
            // Or strictly follow map?
            // User said: "instead of pin ... will be icon ... that is on the map"
            // If undiscovered map has X, and we want "what is on the map", we should draw X.
            // BUT user example: "green circle with tree". This implies the "Category" look.
            // I will assume for DETAILS panel we always show the category icon even if undiscovered,
            // because clicking it might be a way to learn what it is.
            // Wait, if it's Fog of War hidden, user can't click it?
            // If it's visible but undiscovered (grey/dimmed), user can click.
            // Let's show the Icon but maybe dimmed or just normally as it is "Details".

            val icon = getCategoryIcon(poi.category)
            val iconSize = (size - borderThickness * 2) * 0.7f
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
