package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.cityxplore.core.rememberImagePicker
import app.cityxplore.theme.AppColors

/**
 * Dialogue for creating a custom POI before sharing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCustomPoiDialog(
    state: CreateCustomPoiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    onImageUrlChange: (String?) -> Unit,
    onImagePicked: (ByteArray?) -> Unit, // Added parameter
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    state.latitude
    state.longitude

    val imagePicker = rememberImagePicker(onImagePicked = onImagePicked)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    text = "Create Custom POI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name field
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name *") },
                    placeholder = { Text("e.g., My Secret Spot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("${state.name.length}/200")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description field
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    placeholder = { Text("Tell your friend about this place...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("${state.description.length}/1000")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category selection
                Text(
                    text = "Category *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customPoiCategories.forEach { (id, label) ->
                        CategoryChip(
                            label = label,
                            isSelected = state.category == id,
                            onClick = { onCategoryChange(id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Location section
                Text(
                    text = "Location *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Location status
                val latitude = state.latitude
                val longitude = state.longitude
                if (latitude != null && longitude != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.green.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AppColors.green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Location set",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.green
                            )
                            Text(
                                text = "${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No location selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location button - opens picker with mini-map and "My Location" option
                Button(
                    onClick = onPickLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (latitude != null && longitude != null)
                            AppColors.green.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (latitude != null && longitude != null)
                            AppColors.green
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (latitude != null && longitude != null)
                            "Change Location"
                        else
                            "Set Location",
                        color = if (latitude != null && longitude != null)
                            AppColors.green
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Photo section
                Text(
                    text = "Photo (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val currentImageUrl = state.imageUrls.firstOrNull()

                OutlinedTextField(
                    value = currentImageUrl ?: "",
                    onValueChange = {}, // Read only from user perspective
                    label = { Text("Image") },
                    placeholder = { Text("Tap to select image") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { imagePicker.launch() },
                    enabled = true, // Enabled for interactions but strictly controlled
                    readOnly = true, // Cannot type
                    trailingIcon = {
                        IconButton(onClick = { imagePicker.launch() }) {
                            Icon(Icons.Default.Image, "Select image")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        // Use default colors or customize
                    ),
                    supportingText = { Text("Tap icon to select from gallery") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onProceed,
                        enabled = state.isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.green
                        )
                    ) {
                        Text("Next: Select Friend")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = AppColors.green,
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier
            ),
        color = if (isSelected) AppColors.green.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) AppColors.green else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Formats a coordinate to 4 decimal places.
 */
private fun formatCoordinate(value: Double): String {
    val rounded = (value * 10000).toLong() / 10000.0
    return rounded.toString()
}
