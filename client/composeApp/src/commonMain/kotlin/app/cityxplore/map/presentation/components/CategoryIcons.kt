package app.cityxplore.map.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.ui.graphics.vector.ImageVector
import app.cityxplore.map.domain.PoiCategory

/**
 * Returns the icon associated with the given [PoiCategory].
 *
 * These icons are used consistently across POI markers, chips, and details UI.
 *
 * @param category The POI category.
 * @return The [ImageVector] icon for the category.
 */
fun getCategoryIcon(category: PoiCategory): ImageVector {
    return when (category) {
        PoiCategory.HISTORICAL -> Icons.Rounded.AccountBalance
        PoiCategory.CULTURAL -> Icons.Rounded.TheaterComedy
        PoiCategory.NATURE -> Icons.Rounded.Park
        PoiCategory.FOOD -> Icons.Rounded.Restaurant
        PoiCategory.SPORTS -> Icons.Rounded.SportsSoccer
        PoiCategory.ENTERTAINMENT -> Icons.Rounded.LocalActivity
        PoiCategory.CUSTOM -> Icons.Rounded.Place
        PoiCategory.OTHER -> Icons.Rounded.MoreHoriz
        PoiCategory.UNKNOWN -> Icons.Rounded.QuestionMark
    }
}
