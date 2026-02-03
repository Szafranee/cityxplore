package app.cityxplore.core.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * iOS implementation of location permission request.
 * On iOS, location permission is typically requested when the location manager starts.
 * This is a no-op placeholder - actual implementation would use CoreLocation.
 */
@Composable
actual fun RequestLocationPermission(onPermissionResult: (Boolean) -> Unit) {
    // iOS handles location permissions differently through CoreLocation
    // For now, assume granted and let the location service handle it
    LaunchedEffect(Unit) {
        onPermissionResult(true)
    }
}
