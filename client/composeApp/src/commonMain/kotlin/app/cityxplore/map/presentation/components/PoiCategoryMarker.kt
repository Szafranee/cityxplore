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
        else getCategoryColor(poi.category).copy(alpha = 0.5f)
    }

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
