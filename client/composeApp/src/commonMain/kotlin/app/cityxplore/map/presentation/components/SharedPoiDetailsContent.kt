package app.cityxplore.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cityxplore.core.location.Location
import app.cityxplore.core.utils.calculateDistance
import app.cityxplore.map.domain.AutoDiscoverPoisUseCase
import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.theme.AppColors
import coil3.compose.AsyncImage

/**
 * Bottom sheet content for displaying details of a Shared POI.
 *
 * For undiscovered POIs: Shows marker (like on the map), category, distance, and prompt to discover.
 * For discovered POIs: Shows full details including image and description.
 *
 * Both states show the "Shared by" banner with the sharer's info.
 */
@Composable
fun SharedPoiDetailsContent(
    sharedPoi: SharedPoi,
    userLocation: Location?,
    modifier: Modifier = Modifier,
    isSentByMe: Boolean = false,
    onShowOnMap: (() -> Unit)? = null
) {
    val isDiscovered = sharedPoi.isDiscovered || isSentByMe
    val title = sharedPoi.customPoi?.name ?: "Unknown Place"
    val categoryString = sharedPoi.customPoi?.category ?: "OTHER"
    val description = sharedPoi.customPoi?.description
    val imageUrl = sharedPoi.customPoi?.imageUrls?.firstOrNull()
    val coords = sharedPoi.coordinates

    // Parse category to PoiCategory enum
    val category = try {
        PoiCategory.valueOf(categoryString.uppercase())
    } catch (_: IllegalArgumentException) {
        PoiCategory.OTHER
    }

    // Calculate distance if we have user location and POI coordinates
    val distanceMeters = if (userLocation != null && coords != null) {
        calculateDistance(
            userLocation.latitude, userLocation.longitude,
            coords.first, coords.second
        )
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Sharer info banner (always shown)
        SharedByBanner(sharedPoi = sharedPoi)

        Spacer(modifier = Modifier.height(16.dp))

        if (isDiscovered) {
            // DISCOVERED: Show full details
            DiscoveredSharedPoiContent(
                title = title,
                category = category,
                description = description,
                imageUrl = imageUrl,
                onShowOnMap = onShowOnMap
            )
        } else {
            // UNDISCOVERED: Show locked content similar to regular POI
            UndiscoveredSharedPoiContent(
                title = title,
                category = category,
                imageUrl = imageUrl,
                distanceMeters = distanceMeters,
                onShowOnMap = onShowOnMap
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Banner showing who shared the POI and their message.
 */
@Composable
private fun SharedByBanner(sharedPoi: SharedPoi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.green.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (sharedPoi.sharerAvatar != null) {
                AsyncImage(
                    model = sharedPoi.sharerAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppColors.green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sharedPoi.sharerName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Shared by ${sharedPoi.sharerName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.green
                )
                if (sharedPoi.message != null) {
                    Text(
                        text = "\"${sharedPoi.message}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Content for discovered shared POIs - shows full details.
 */
@Composable
private fun DiscoveredSharedPoiContent(
    title: String,
    category: PoiCategory,
    description: String?,
    imageUrl: String?,
    onShowOnMap: (() -> Unit)? = null
) {
    // Header with gradient marker and title
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gradient marker (like on map)
        PoiCategoryMarker(
            category = category,
            isSharedPoi = true,
            isDiscovered = true,
            size = 48.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (onShowOnMap != null) {
            IconButton(onClick = onShowOnMap) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Show on map",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Category badge
    PoiCategoryBadge(
        category = category,
        isSharedPoi = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Image
    if (imageUrl != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Description
    if (!description.isNullOrBlank()) {
        Text(
            text = "About this place",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
        )
    }
}

/**
 * Content for undiscovered shared POIs - similar to UndiscoveredPoiContent for regular POIs.
 * Shows marker, category, distance, blurred image, and discovery prompt.
 */
@Composable
private fun UndiscoveredSharedPoiContent(
    title: String,
    category: PoiCategory,
    imageUrl: String?,
    distanceMeters: Double?,
    onShowOnMap: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large gradient marker (like on a map but bigger)
        PoiCategoryMarker(
            category = category,
            isSharedPoi = true,
            isDiscovered = false,
            size = 100.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Mystery Title
        Text(
            text = "🔒 $title",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category badge
        PoiCategoryBadge(
            category = category,
            isSharedPoi = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Distance card
        if (distanceMeters != null) {
            PoiDistanceCard(distanceMeters = distanceMeters)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Show on Map button (if available)
        if (onShowOnMap != null) {
            Button(
                onClick = onShowOnMap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Blurred image (if available)
        if (imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .blur(20.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
                // Lock icon overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Visit to unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Discovery prompt
        PoiDiscoverPrompt(
            discoveryRadiusMeters = AutoDiscoverPoisUseCase.DISCOVERY_RADIUS_METERS.toInt()
        )
    }
}
