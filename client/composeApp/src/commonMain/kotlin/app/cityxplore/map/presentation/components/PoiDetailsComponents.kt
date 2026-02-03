package app.cityxplore.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cityxplore.core.utils.formatDistanceForDisplay
import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.theme.AppColors

/**
 * A circular marker displaying the POI category, styled like the map marker.
 * Used in POI details to show visual consistency with the map.
 *
 * Supports both regular POIs and shared POIs with different visual styles:
 * - Regular POIs: Simple circular background with category icon
 * - Shared POIs: Gradient background with friend badge indicator
 * - Major landmarks: Gold color scheme regardless of category
 *
 * @param category The POI category
 * @param isMajor Whether this is a major landmark (shows gold star)
 * @param isSharedPoi Whether this is a shared POI (uses a gradient background)
 * @param isDiscovered Whether the POI has been discovered (affects styling for shared POIs)
 * @param size The size of the marker
 *
 * @see getCategoryColor for category-specific colors
 * @see getCategoryIcon for category-specific icons
 */
@Composable
fun PoiCategoryMarker(
    category: PoiCategory,
    isMajor: Boolean = false,
    isSharedPoi: Boolean = false,
    isDiscovered: Boolean = true,
    size: Dp = 80.dp
) {
    val displayColor = if (isMajor) AppColors.majorLandmarkGold else getCategoryColor(category)

    if (isSharedPoi) {
        val color1 = if (isDiscovered) displayColor else displayColor.copy(alpha = 0.5f)
        val color2 = if (isDiscovered) AppColors.sharedPoiGreen else AppColors.sharedPoiGreen.copy(alpha = 0.5f)
        val ringGradient = Brush.linearGradient(listOf(color1, color2))
        val gradient = Brush.linearGradient(listOf(color1, color2))

        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // Ring
            Box(
                modifier = Modifier
                    .size(size)
                    .padding(2.dp) // Gap between content and ring
                    .border(width = 2.dp, brush = ringGradient, shape = CircleShape)
            )

            // Inner Circle
            Box(
                modifier = Modifier
                    .size(size - 8.dp) // Adjust size to fit inside ring
                    .background(brush = gradient, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isDiscovered) {
                    val iconVector = if (isMajor) Icons.Rounded.Star else getCategoryIcon(category)
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((size - 8.dp) * 0.5f)
                    )
                } else {
                    // Draw custom X for undiscovered to match map style (thicker lines)
                    val markerSize = size // Capture size to avoid shadowing in DrawScope
                    androidx.compose.foundation.Canvas(modifier = Modifier.size((markerSize - 8.dp) * 0.5f)) {
                        val totalSizePx = markerSize.toPx()
                        val strokeWidth = totalSizePx * 0.1f
                        val xRadius = totalSizePx * 0.15f
                        val centerOffset = this.center

                        val pathColor = Color(0xFFC8C8C8)

                        drawLine(
                            color = pathColor,
                            start = centerOffset + androidx.compose.ui.geometry.Offset(-xRadius, -xRadius),
                            end = centerOffset + androidx.compose.ui.geometry.Offset(xRadius, xRadius),
                            strokeWidth = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        drawLine(
                            color = pathColor,
                            start = centerOffset + androidx.compose.ui.geometry.Offset(xRadius, -xRadius),
                            end = centerOffset + androidx.compose.ui.geometry.Offset(-xRadius, xRadius),
                            strokeWidth = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            // Friend Badge (Top Right)
            val badgeColor = if (isDiscovered) AppColors.sharedPoiGreen else AppColors.sharedPoiGreen.copy(alpha = 0.6f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 0.dp, y = 0.dp) // Adjust as needed
                    .size(size * 0.3f)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.2f)
                )
            }
        }
    } else {
        // Regular POI Marker
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = displayColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMajor) Icons.Rounded.Star else getCategoryIcon(category),
                contentDescription = null,
                tint = displayColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/**
 * Card displaying distance to a POI with clear labelling.
 *
 * @param distanceMeters Distance in meters
 * @param modifier Modifier for the card
 */
@Composable
fun PoiDistanceCard(
    distanceMeters: Double,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatDistanceForDisplay(distanceMeters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "away",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card prompting user to get closer to discover the POI.
 *
 * @param discoveryRadiusMeters The radius within which POIs are discovered
 * @param modifier Modifier for the card
 */
@Composable
fun PoiDiscoverPrompt(
    discoveryRadiusMeters: Int = 100,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨ Get within ${discoveryRadiusMeters}m to discover!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock the name, photos, description, and other details by exploring this location.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Banner displaying information about who shared a POI.
 *
 * Shows the sharer's avatar (or initial placeholder), name, and optional message.
 * Used in shared POI details to indicate the social context.
 *
 * @param sharerName The name of the person who shared the POI
 * @param sharerAvatar URL to the sharer's avatar image (nullable)
 * @param message Optional message from the sharer
 * @param modifier Modifier for the banner
 *
 * @see SharedPoiDetailsContent
 */
@Composable
fun SharedByBanner(
    sharerName: String?,
    sharerAvatar: String?,
    message: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.sharedPoiGreen.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (sharerAvatar != null) {
                coil3.compose.AsyncImage(
                    model = sharerAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppColors.sharedPoiGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sharerName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Shared by ${sharerName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.sharedPoiGreen
                )
                if (message != null) {
                    Text(
                        text = "\"$message\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Category badge chip showing the POI category with icon.
 *
 * Displays a styled badge with:
 * - Category-specific color and icon for regular POIs
 * - Gold color and star icon for major landmarks
 * - Green color scheme for shared POIs
 *
 * @param category The POI category
 * @param isMajor Whether this is a major landmark (overrides category styling)
 * @param isSharedPoi Whether this is a shared POI (uses green accent color)
 *
 * @see PoiCategoryMarker for the circular marker variant
 */
@Composable
fun PoiCategoryBadge(
    category: PoiCategory,
    isMajor: Boolean = false,
    isSharedPoi: Boolean = false
) {
    val displayColor = when {
        isMajor -> AppColors.majorLandmarkGold
        isSharedPoi -> AppColors.sharedPoiGreen
        else -> getCategoryColor(category)
    }
    val displayText = when {
        isMajor -> "Major Landmark"
        else -> getCategoryDisplayName(category)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = displayColor.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isMajor) Icons.Rounded.Star else getCategoryIcon(category),
                contentDescription = null,
                tint = displayColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = displayColor
            )
        }
    }
}

/**
 * A shared button component for "Show on map" functionality.
 * Consistent styling across all POI detail views.
 *
 * @param onClick Action to perform when clicked. If null, nothing happens (but checking for null is typically done by caller).
 * @param modifier Modifier for the button.
 */
@Composable
fun ShowOnMapButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (onClick != null) {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.green,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Show on map")
        }
    }
}
