package app.cityxplore.social.presentation.sharedpois

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific interactive map for picking a location.
 * The user can tap on the map to select a location or drag the marker.
 *
 * @param latitude Current selected latitude, or null if none selected
 * @param longitude Current selected longitude, or null if none selected
 * @param userLatitude User's current latitude for initial centering
 * @param userLongitude User's current longitude for initial centering
 * @param onLocationSelected Callback when user taps on map to select location
 * @param modifier Modifier for the map view
 */
@Composable
expect fun LocationPickerMapView(
    latitude: Double?,
    longitude: Double?,
    userLatitude: Double?,
    userLongitude: Double?,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier
)
