package app.cityxplore.map.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.theme.AppColors

/**
 * Smart discovery notification component.
 *
 * Behavior:
 * - Single POI discovered: Shows a full-screen modal popup with image and details
 * - Multiple POIs discovered: Shows card at bottom with a list of POI names
 *
 * @param discoveredPoiIds Set of POI IDs that were just discovered.
 * @param pois Full list of POIs (to look up names).
 * @param onViewDetails Called when the user taps "View Details" (single POI mode).
 * @param onDismiss Called when the user dismisses a single POI notification.
 * @param onDismissAll Called when the user dismisses all notifications.
 */
@Composable
fun DiscoveryNotification(
    discoveredPoiIds: Set<String> = emptySet(),
    pois: List<MapPoi> = emptyList(),
    discoveredSharedPoiIds: Set<String> = emptySet(),
    sharedPois: List<SharedPoi> = emptyList(),
    onViewDetails: (String) -> Unit = {},
    onDismiss: (String) -> Unit = {},
    onDismissAll: () -> Unit = {},
    onViewSharedDetails: (String) -> Unit = {},
    onDismissShared: (String) -> Unit = {},
    onDismissAllShared: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 1. Handle Regular POIs
    val discoveredPois = pois.filter { it.id in discoveredPoiIds }
    if (discoveredPois.isNotEmpty()) {
        if (discoveredPois.size == 1) {
            val poi = discoveredPois.first()
            PoiDiscoveryPopup(
                poi = poi,
                onViewDetails = { onViewDetails(poi.id) },
                onDismiss = { onDismiss(poi.id) }
            )
        } else {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.orange),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                MultiplePoiDiscovery(
                    pois = discoveredPois,
                    onDismissAll = onDismissAll
                )
            }
        }
    }

    // 2. Handle Shared POIs
    val discoveredSharedPois = sharedPois.filter { it.id in discoveredSharedPoiIds }
    if (discoveredSharedPois.isNotEmpty()) {
        if (discoveredSharedPois.size == 1) {
            val sharedPoi = discoveredSharedPois.first()
            SharedPoiDiscoveryPopup(
                sharedPoi = sharedPoi,
                onViewDetails = { onViewSharedDetails(sharedPoi.id) },
                onDismiss = { onDismissShared(sharedPoi.id) }
            )
        } else {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = if (discoveredPois.size > 1) 100.dp else 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.green),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                MultipleSharedPoiDiscovery(
                    pois = discoveredSharedPois,
                    onDismissAll = onDismissAllShared
                )
            }
        }
    }
}

@Composable
private fun MultipleSharedPoiDiscovery(
    pois: List<SharedPoi>,
    onDismissAll: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 ${pois.size} Shared Places Discovered!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = onDismissAll) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss all",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        pois.forEach { poi ->
            val name = poi.customPoi?.name ?: "Unknown Place"
            val sharer = poi.sharerName ?: "?"
            Text(
                text = "• $name (from $sharer)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MultiplePoiDiscovery(
    pois: List<MapPoi>,
    onDismissAll: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 ${pois.size} New Places Discovered!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer // Orange specific contrast
            )
            IconButton(onClick = onDismissAll) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss all",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        pois.forEach { poi ->
            Text(
                text = "• ${poi.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
