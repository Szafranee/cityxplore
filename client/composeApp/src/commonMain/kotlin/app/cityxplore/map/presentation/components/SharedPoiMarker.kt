package app.cityxplore.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import app.cityxplore.theme.AppColors

/**
 * Special marker for shared POIs that stands out from regular POIs.
 * Features:
 * - Animated ring around the marker
 * - Gradient background (green to cyan)
 * - Friend badge in corner
 * - Optional unread indicator
 *
 * @param isCustomPoi Whether this is a custom POI (vs existing POI)
 * @param isUnread Whether this shared POI hasn't been viewed yet
 * @param modifier Modifier for the marker container
 * @param size The diameter of the marker
 */
@Composable
fun SharedPoiMarker(
    isCustomPoi: Boolean,
    isUnread: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val primaryColor = if (isCustomPoi) AppColors.orange else AppColors.blue
    val secondaryColor = AppColors.green

    Box(
        modifier = modifier.size(size + 16.dp), // Extra space for the ring
        contentAlignment = Alignment.Center
    ) {
        // Outer ring (pulsing effect would be added in platform-specific code)
        Box(
            modifier = Modifier
                .size(size + 12.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.4f),
                            primaryColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    shape = CircleShape
                )
        )

        // Main marker with gradient background
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            secondaryColor
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Shared POI",
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }

        // Friend badge (top-right corner)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 2.dp)
                .size(20.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(AppColors.green)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "From friend",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }

        // Unread indicator (red dot at top-left)
        if (isUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(12.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(AppColors.red)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * A smaller version of the shared POI marker for use in lists and cards.
 */
@Composable
fun SharedPoiMarkerSmall(
    isCustomPoi: Boolean,
    isUnread: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val primaryColor = if (isCustomPoi) AppColors.orange else AppColors.blue
    val secondaryColor = AppColors.green

    Box(
        modifier = modifier.size(size + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ring
        Box(
            modifier = Modifier
                .size(size + 6.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    shape = CircleShape
                )
        )

        // Main marker
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }

        // Friend badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(AppColors.green)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(8.dp)
            )
        }

        // Unread indicator
        if (isUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AppColors.red)
                    .border(0.5.dp, Color.White, CircleShape)
            )
        }
    }
}
