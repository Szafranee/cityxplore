package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cityxplore.social.domain.model.SharedPoi

/**
 * Content component showing lists of shared POIs with tabs for Received/Sent.
 */
@Composable
fun SharedPoiListContent(
    pois: List<SharedPoi>,
    deletingIds: Set<String> = emptySet(),
    isReceived: Boolean,
    onNavigate: (SharedPoi) -> Unit,
    onMarkViewed: (SharedPoi) -> Unit,
    onDelete: (SharedPoi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pois.isEmpty()) {
        EmptySharedPoisPlaceholder(isReceived = isReceived, modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = pois,
                key = { it.id }
            ) { sharedPoi ->
                SharedPoiCard(
                    sharedPoi = sharedPoi,
                    isDeleting = deletingIds.contains(sharedPoi.id),
                    isReceived = isReceived,
                    onNavigate = { onNavigate(sharedPoi) },
                    onMarkViewed = { onMarkViewed(sharedPoi) },
                    onDelete = { onDelete(sharedPoi) }
                )
            }
        }
    }
}

@Composable
private fun EmptySharedPoisPlaceholder(
    isReceived: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (isReceived) Icons.Default.Inbox else Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
                text = if (isReceived) {
                    "No POIs received yet"
                } else {
                    "No POIs shared yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = if (isReceived) {
                    "When friends share POIs with you, they'll appear here"
                } else {
                    "Share your favorite spots with friends using the + button"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
