package app.cityxplore.social.presentation.sharedpois

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.cityxplore.theme.AppColors

/**
 * Dialogue for picking a location with an interactive map and "My Location" button.
 */
@Composable
fun LocationPickerDialog(
    currentLatitude: Double?,
    currentLongitude: Double?,
    userLatitude: Double?,
    userLongitude: Double?,
    onDismiss: () -> Unit,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit
) {
    var latitudeText by remember {
        mutableStateOf(currentLatitude?.toString() ?: "")
    }
    var longitudeText by remember {
        mutableStateOf(currentLongitude?.toString() ?: "")
    }
    var latitudeError by remember { mutableStateOf<String?>(null) }
    var longitudeError by remember { mutableStateOf<String?>(null) }

    // Parsed coordinates for map
    val parsedLat = latitudeText.toDoubleOrNull()
    val parsedLng = longitudeText.toDoubleOrNull()
    val hasValidCoordinates = parsedLat != null && parsedLng != null &&
            parsedLat in -90.0..90.0 && parsedLng in -180.0..180.0

    /**
     * Validates input fields and sets error messages.
     * Returns true if both coordinates are valid.
     */
    fun validateInputs(): Boolean {
        val lat = latitudeText.toDoubleOrNull()
        val lng = longitudeText.toDoubleOrNull()

        latitudeError = when {
            latitudeText.isBlank() -> "Required"
            lat == null -> "Invalid number"
            lat < -90 || lat > 90 -> "Must be between -90 and 90"
            else -> null
        }

        longitudeError = when {
            longitudeText.isBlank() -> "Required"
            lng == null -> "Invalid number"
            lng < -180 || lng > 180 -> "Must be between -180 and 180"
            else -> null
        }

        return latitudeError == null && longitudeError == null && lat != null && lng != null
    }

    fun validateAndSubmit() {
        if (validateInputs()) {
            val lat = latitudeText.toDoubleOrNull()
            val lng = longitudeText.toDoubleOrNull()
            if (lat != null && lng != null) {
                onLocationSelected(lat, lng)
            }
        }
    }

    fun useMyLocation() {
        if (userLatitude != null && userLongitude != null) {
            latitudeText = formatCoordinate(userLatitude)
            longitudeText = formatCoordinate(userLongitude)
            latitudeError = null
            longitudeError = null
        }
    }

    fun onMapLocationSelected(lat: Double, lng: Double) {
        latitudeText = formatCoordinate(lat)
        longitudeText = formatCoordinate(lng)
        latitudeError = null
        longitudeError = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Pick Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    LocationPickerMapView(
                        latitude = parsedLat?.takeIf { hasValidCoordinates },
                        longitude = parsedLng?.takeIf { hasValidCoordinates },
                        userLatitude = userLatitude,
                        userLongitude = userLongitude,
                        onLocationSelected = ::onMapLocationSelected,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // My Location button overlay in the top-right corner
                    OutlinedButton(
                        onClick = { useMyLocation() },
                        enabled = userLatitude != null && userLongitude != null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (userLatitude != null && userLongitude != null) AppColors.green else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "My Location",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Coordinate inputs in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = {
                            latitudeText = it
                            latitudeError = null
                        },
                        label = { Text("Latitude") },
                        placeholder = { Text("52.23") },
                        isError = latitudeError != null,
                        supportingText = latitudeError?.let { { Text(it, maxLines = 1) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = {
                            longitudeText = it
                            longitudeError = null
                        },
                        label = { Text("Longitude") },
                        placeholder = { Text("21.01") },
                        isError = longitudeError != null,
                        supportingText = longitudeError?.let { { Text(it, maxLines = 1) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        onClick = { validateAndSubmit() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.green
                        )
                    ) {
                        Text("Set Location")
                    }
                }
            }
        }
    }
}

/**
 * Formats coordinate for text field (full precision).
 */
private fun formatCoordinate(value: Double): String {
    return value.toString()
}
