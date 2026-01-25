package app.cityxplore.core.location

import androidx.compose.runtime.Composable

/**
 * Composable that requests location permission.
 *
 * Should be called once when the user enters the main app content.
 * Platform-specific implementations handle the actual permission request.
 */
@Composable
expect fun RequestLocationPermission(onPermissionResult: (Boolean) -> Unit)
