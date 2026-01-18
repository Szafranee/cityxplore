package app.cityxplore.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cityxplore.map.domain.PoiCategory

/**
 * Unified marker for shared POIs matching the map markers.
 * Uses category-based gradient coloring and shows discovery status.
 *
 * @param category The POI category
 * @param isDiscovered Whether this POI has been discovered
 * @param size The diameter of the marker
 */
@Composable
fun SharedPoiMarkerCompose(
    category: PoiCategory,
    isDiscovered: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val categoryColor = getCategoryColor(category)
    val greenColor = SharedPoiGreen

    // Dim colors for undiscovered
    val color1 = if (isDiscovered) categoryColor else categoryColor.copy(alpha = 0.5f)
    val color2 = if (isDiscovered) greenColor else greenColor.copy(alpha = 0.5f)

    val gradient = Brush.linearGradient(listOf(color1, color2))
    val ringGradient = Brush.linearGradient(listOf(color1, color2))

    Box(
        modifier = modifier.size(size + 12.dp), // Extra space for the ring
        contentAlignment = Alignment.Center
    ) {
        // Outer ring (gradient)
        Box(
            modifier = Modifier
                .size(size + 8.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = ringGradient,
                    shape = CircleShape
                )
        )

        // Main marker with gradient background
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(brush = gradient),
            contentAlignment = Alignment.Center
        ) {
            if (isDiscovered) {
                // Show category icon
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else {
                // Show X for undiscovered
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }

        // Friend badge (top-right corner)
        val badgeColor = if (isDiscovered) greenColor else greenColor.copy(alpha = 0.6f)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 2.dp)
                .size(size * 0.35f)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(badgeColor)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "From friend",
                tint = Color.White,
                modifier = Modifier.size(size * 0.2f)
            )
        }
    }
}

/**
 * A smaller version of the shared POI marker for use in lists and cards.
 * Uses the same styling as map markers.
 *
 * @param category The POI category
 * @param isDiscovered Whether this POI has been discovered by the recipient
 * @param size The diameter of the marker
 */
@Composable
fun SharedPoiMarkerSmall(
    category: PoiCategory,
    isDiscovered: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val categoryColor = getCategoryColor(category)
    val greenColor = SharedPoiGreen

    // Dim colors for undiscovered
    val color1 = if (isDiscovered) categoryColor else categoryColor.copy(alpha = 0.5f)
    val color2 = if (isDiscovered) greenColor else greenColor.copy(alpha = 0.5f)

    val gradient = Brush.linearGradient(listOf(color1, color2))

    Box(
        modifier = modifier.size(size + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ring (gradient border)
        Box(
            modifier = Modifier
                .size(size + 6.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(color1, color2)),
                    shape = CircleShape
                )
        )

        // Main marker
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(brush = gradient),
            contentAlignment = Alignment.Center
        ) {
            if (isDiscovered) {
                // Show category icon for discovered
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else {
                // Show X for undiscovered
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }

        // Friend badge
        val badgeColor = if (isDiscovered) greenColor else greenColor.copy(alpha = 0.6f)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(size * 0.35f)
                .clip(CircleShape)
                .background(badgeColor)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.18f)
            )
        }
    }
}

/**
 * Legacy version for backward compatibility - maps old params to new API.
 * @deprecated Use SharedPoiMarkerSmall(category, isDiscovered) instead
 */
@Composable
@Deprecated("Use SharedPoiMarkerSmall with category parameter")
fun SharedPoiMarkerSmall(
    isCustomPoi: Boolean,
    isUnread: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    // Map old params to new - treat as the OTHER category and always discovered for sent items
    SharedPoiMarkerSmall(
        category = PoiCategory.OTHER,
        isDiscovered = true,
        modifier = modifier,
        size = size
    )
}
