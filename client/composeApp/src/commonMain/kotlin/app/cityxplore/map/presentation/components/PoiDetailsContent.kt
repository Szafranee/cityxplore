package app.cityxplore.map.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.map.domain.PhotoSource
import app.cityxplore.map.domain.PoiPhoto
import coil3.compose.AsyncImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Composable function for displaying detailed information about a POI.
 *
 * This content is typically shown in a bottom sheet or modal.
 * It includes photos, metadata (discovery status, opening hours, etc.), description, and trivia.
 *
 * @param poi The [MapPoi] object containing the data to display.
 * @param onToggleFavorite Callback function invoked when the favorite button is clicked. If null, the button is hidden.
 */
@Composable
fun PoiDetailsContent(
    poi: MapPoi,
    onToggleFavorite: ((String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        if (poi.photos.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(poi.photos) { photo ->
                    PoiPhotoItem(
                        photo = photo,
                        modifier = Modifier
                            .height(200.dp)
                            .then(
                                if (poi.photos.size == 1) Modifier.fillParentMaxWidth()
                                else Modifier.aspectRatio(4f / 3f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        val uriHandler = LocalUriHandler.current
        var showOpeningHoursDialog by remember { mutableStateOf(false) }

        if (showOpeningHoursDialog && poi.metadata.openingHours?.weekdayText?.isNotEmpty() == true) {
            AlertDialog(
                onDismissRequest = { showOpeningHoursDialog = false },
                confirmButton = {
                    TextButton(onClick = { showOpeningHoursDialog = false }) {
                        Text("Close")
                    }
                },
                title = { Text("Opening Hours") },
                text = {
                    Column {
                        poi.metadata.openingHours.weekdayText.forEach { dayText ->
                            Text(
                                text = dayText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            )
        }

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getCategoryIcon(poi.category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (onToggleFavorite != null) {
                IconButton(onClick = { onToggleFavorite(poi.id) }) {
                    Icon(
                        imageVector = if (poi.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (poi.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (poi.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata Chips Container
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Discovery Status
            AssistChip(
                onClick = {},
                label = { Text(if (poi.discovered) "Discovered" else "Undiscovered") },
                leadingIcon = {
                    Icon(
                        if (poi.discovered) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                        tint = if (poi.discovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // Opening Hours
            if (poi.metadata.openingHours?.openNow != null) {
                val isOpen = poi.metadata.openingHours.openNow
                AssistChip(
                    label = { Text(if (isOpen) "Open Now" else "Closed") },
                    onClick = { showOpeningHoursDialog = true },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                        leadingIconContentColor = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                )
            }

            // Visit Duration
            if (poi.metadata.visitDuration != null) {
                AssistChip(
                    label = { Text(poi.metadata.visitDuration) },
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = "Visit Duration",
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }

            // Price / Free
            if (poi.metadata.isFree != null) {
                AssistChip(
                    label = { Text(if (poi.metadata.isFree) "Free Entry" else "Paid") },
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            if (poi.metadata.isFree) Icons.Filled.MoneyOff else Icons.Filled.AttachMoney,
                            contentDescription = "Price",
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }

            // Build Year
            if (poi.metadata.buildYear != null) {
                AssistChip(
                    label = { Text(poi.metadata.buildYear) },
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = "Build Year",
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
        }


        if (poi.discovered && poi.discoveryDate != null && poi.discoveryDate != 0L) {
            Spacer(modifier = Modifier.height(8.dp))
            val date = Instant.fromEpochMilliseconds(poi.discoveryDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            Text(
                text = "Discovered on: $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trivia Section
        if (poi.metadata.trivia != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Trivia",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Did you know?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = poi.metadata.trivia,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (!poi.description.isNullOrBlank()) {
            Text(
                text = poi.description,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "No description available for this place.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Website Button
        if (poi.metadata.website != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { uriHandler.openUri(poi.metadata.website) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Visit Website")
            }
        }


        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PoiPhotoItem(photo: PoiPhoto, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AsyncImage(
            model = photo.url,
            contentDescription = "Photo by ${photo.author}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Attribution Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .padding(8.dp)
        ) {
            Column {
                if (photo.source == PhotoSource.GOOGLE_PLACES && photo.attributions != null) {
                    // Google Attribution
                    // Simple parse: remove tags
                    val textRaw = photo.attributions.replace(Regex("<.*?>"), "")
                    Text(text = "Photo: $textRaw", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text(
                        text = "Powered by Google",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(
                                8f,
                                androidx.compose.ui.unit.TextUnitType.Sp
                            )
                        ),
                        color = Color.LightGray
                    )
                } else if (photo.source == PhotoSource.WIKIMEDIA) {
                    // Wikimedia Attribution
                    val text = "Author: ${photo.author ?: "Unknown"}\n(${photo.license ?: "Unknown License"})"
                    Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White)
                } else if (photo.author != null) {
                    Text(
                        text = "Photo: ${photo.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
