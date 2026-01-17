package app.cityxplore.social.presentation.sharedpois

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.cityxplore.theme.AppColors
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar

/**
 * Android implementation of LocationPickerMapView using Mapbox.
 */
@Composable
actual fun LocationPickerMapView(
    latitude: Double?,
    longitude: Double?,
    userLatitude: Double?,
    userLongitude: Double?,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current

    // Check for Mapbox token
    val appInfo = remember {
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    }
    val mapboxToken = remember {
        appInfo.metaData?.getString("com.mapbox.token")
    }

    if (mapboxToken.isNullOrBlank() || mapboxToken == "null") {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1A202C)),
            contentAlignment = Alignment.Center
        ) {
            Text("Map unavailable", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val selectedPoint = remember {
        mutableStateOf<Point?>(
            if (latitude != null && longitude != null) Point.fromLngLat(longitude, latitude) else null
        )
    }

    // Update selected point when props change
    LaunchedEffect(latitude, longitude) {
        selectedPoint.value = if (latitude != null && longitude != null) {
            Point.fromLngLat(longitude, latitude)
        } else {
            null
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef.value = this

                    mapboxMap.loadStyle("mapbox://styles/mapbox/dark-v11") { _ ->
                        // Set initial camera position
                        val initialCenter = when {
                            latitude != null && longitude != null -> Point.fromLngLat(longitude, latitude)
                            userLatitude != null && userLongitude != null -> Point.fromLngLat(
                                userLongitude,
                                userLatitude
                            )

                            else -> Point.fromLngLat(21.0122, 52.2297) // Warsaw default
                        }

                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(initialCenter)
                                .zoom(14.0)
                                .build()
                        )
                    }

                    // Configure map settings for picker mode
                    compass.updateSettings { enabled = false }
                    scalebar.updateSettings { enabled = false }
                    gestures.updateSettings {
                        scrollEnabled = true
                        rotateEnabled = false
                        pitchEnabled = false
                        pinchScrollEnabled = true
                    }

                    // Add click listener for location selection
                    gestures.addOnMapClickListener { point ->
                        selectedPoint.value = point
                        onLocationSelected(point.latitude(), point.longitude())

                        // Animate to selected point
                        camera.easeTo(
                            CameraOptions.Builder()
                                .center(point)
                                .build(),
                            MapAnimationOptions.Builder().duration(200).build()
                        )
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center marker overlay (always visible in center)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Marker pin with glow effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.green.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Selected location",
                    tint = AppColors.green,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Instruction hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Tap to select location",
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
    }
}
