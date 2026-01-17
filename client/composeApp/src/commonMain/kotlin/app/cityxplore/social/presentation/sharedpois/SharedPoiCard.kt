package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cityxplore.map.presentation.components.SharedPoiMarkerSmall
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.theme.AppColors

/**
 * Card component displaying a shared POI with actions.
 */
@Composable
fun SharedPoiCard(
    sharedPoi: SharedPoi,
    isReceived: Boolean,
    onNavigate: () -> Unit,
    onMarkViewed: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnviewed = isReceived && !sharedPoi.isViewed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnviewed) {
                AppColors.green.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Shared POI Marker with ring and gradient
            SharedPoiMarkerSmall(
                isCustomPoi = sharedPoi.isCustomPoi,
                isUnread = isUnviewed,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name
                Text(
                    text = sharedPoi.customPoi?.name ?: "Shared POI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Category tag
                val category = sharedPoi.customPoi?.category
                if (category != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.green.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = category.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.green
                        )
                    }
                }

                // Description
                val description = sharedPoi.customPoi?.description
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // User info - show who sent or received the POI
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isReceived) AppColors.green else AppColors.cyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isReceived) {
                        // Show who sent it
                        Text(
                            text = "From: ${sharedPoi.sharerName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.green,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        // Show who received it
                        Text(
                            text = "To: ${sharedPoi.recipientName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.cyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Message from sharer
                val message = sharedPoi.message
                if (message != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"$message\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Timestamp
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(sharedPoi.sharedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isReceived && !sharedPoi.isViewed) {
                    IconButton(
                        onClick = onMarkViewed,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Mark as viewed",
                            tint = AppColors.green
                        )
                    }
                }

                if (!isReceived) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AppColors.red
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats an ISO timestamp to a user-friendly format.
 */
private fun formatTimestamp(isoTimestamp: String): String {
    return try {
        // Simple formatting - extract date part
        val datePart = isoTimestamp.substringBefore("T")
        val parts = datePart.split("-")
        if (parts.size == 3) {
            "${parts[2]}.${parts[1]}.${parts[0]}"
        } else {
            isoTimestamp
        }
    } catch (_: Exception) {
        isoTimestamp
    }
}
