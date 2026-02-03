package app.cityxplore.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Composable that requests location permission on Android devices.
 *
 * Should be called once when the user enters the main app content.
 * Requests both fine and coarse location permissions.
 *
 * @param onPermissionResult Callback with the permission result (true if granted).
 */
@Composable
actual fun RequestLocationPermission(onPermissionResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    var hasRequestedPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        println("Permissions result: $permissions")
        onPermissionResult(isLocationGranted)
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermission) {
            val hasFineLocation = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarseLocation = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

            // Should usually ask for everything we need at startup
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            // Add notification permission for Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (!hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (hasFineLocation && hasCoarseLocation && permissionsToRequest.isEmpty()) {
                // Already have all necessary permissions
                println("Location permission already granted")
                onPermissionResult(true)
            } else {
                // Request permissions
                hasRequestedPermission = true
                println("Requesting permissions: $permissionsToRequest")
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                } else {
                    onPermissionResult(true)
                }
            }
        }
    }
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
