package app.cityxplore.map.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.cityxplore.map.domain.PoiCategory

@Composable
fun getCategoryColor(category: PoiCategory): Color {
    return when (category) {
        PoiCategory.HISTORICAL -> Color(255, 152, 0) // Orange
        PoiCategory.CULTURAL -> Color(156, 39, 176) // Purple
        PoiCategory.NATURE -> Color(76, 175, 80) // Green
        PoiCategory.FOOD -> Color(244, 67, 54) // Red
        PoiCategory.SPORTS -> Color(33, 150, 243) // Blue
        PoiCategory.ENTERTAINMENT -> Color(233, 30, 99) // Pink
        PoiCategory.CUSTOM -> Color(121, 85, 72) // Brown
        PoiCategory.OTHER -> Color(158, 158, 158) // Grey
        PoiCategory.UNKNOWN -> Color(96, 125, 139) // Blue Grey
    }
}
