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
import androidx.compose.material3.Button
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

/**
 * Smart discovery notification component.
 *
 * Behavior:
 * - Single POI discovered: Shows card with POI name and "View Details" button
 * - Multiple POIs discovered: Shows list of POI names with dismiss all button
 *
 * @param discoveredPoiIds Set of POI IDs that were just discovered.
 * @param pois Full list of POIs (to look up names).
 * @param onViewDetails Called when user taps "View Details" (single POI mode).
 * @param onDismiss Called when user dismisses a single POI notification.
 * @param onDismissAll Called when user dismisses all notifications.
 */
@Composable
fun DiscoveryNotification(
    discoveredPoiIds: Set<String>,
    pois: List<MapPoi>,
    onViewDetails: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveredPois = pois.filter { it.id in discoveredPoiIds }

    if (discoveredPois.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (discoveredPois.size == 1) {
            // Single discovery - show card with "View Details" button
            SinglePoiDiscovery(
                poi = discoveredPois.first(),
                onViewDetails = onViewDetails,
                onDismiss = onDismiss
            )
        } else {
            // Multiple discoveries - show list
            MultiplePoiDiscovery(
                pois = discoveredPois,
                onDismissAll = onDismissAll
            )
        }
    }
}

@Composable
private fun SinglePoiDiscovery(
    poi: MapPoi,
    onViewDetails: (String) -> Unit,
    onDismiss: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 New Place Discovered!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = { onDismiss(poi.id) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = poi.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = poi.category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onViewDetails(poi.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Details")
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

        Spacer(Modifier.height(8.dp))

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
