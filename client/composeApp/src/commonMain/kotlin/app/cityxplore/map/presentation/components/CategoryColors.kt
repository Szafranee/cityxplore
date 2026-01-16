package app.cityxplore.map.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.cityxplore.map.domain.PoiCategory

/**
 * Returns the theme color associated with the given [PoiCategory].
 *
 * These colors are used consistently across POI markers, chips, and details UI.
 *
 * @param category The POI category.
 * @return The [Color] for the category.
 */
@Composable
fun getCategoryColor(category: PoiCategory): Color {
    return when (category) {
        PoiCategory.HISTORICAL -> Color(0xFFFF9800)    // Orange
        PoiCategory.CULTURAL -> Color(0xFF9C27B0)      // Purple
        PoiCategory.NATURE -> Color(0xFF4CAF50)        // Green
        PoiCategory.FOOD -> Color(0xFFF44336)          // Red
        PoiCategory.SPORTS -> Color(0xFF2196F3)        // Blue
        PoiCategory.ENTERTAINMENT -> Color(0xFFE91E63) // Pink
        PoiCategory.CUSTOM -> Color(0xFF795548)        // Brown
        PoiCategory.OTHER -> Color(0xFF9E9E9E)         // Grey
        PoiCategory.UNKNOWN -> Color(0xFF607D8B)       // Blue Grey
    }
}
