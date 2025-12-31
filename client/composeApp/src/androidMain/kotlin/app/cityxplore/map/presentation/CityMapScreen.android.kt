package app.cityxplore.map.presentation

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import app.cityxplore.domain.service.H3Service
import app.cityxplore.theme.AppColors
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import org.koin.compose.koinInject

@SuppressLint("MissingPermission")
@Composable
actual fun CityXploreMapScreen(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier,
    onProfileClick: () -> Unit,
) {
    when (state) {
        MapUiState.Loading -> LoadingMap()
        is MapUiState.Error -> ErrorMap(state.message)
        is MapUiState.Ready -> ReadyMap(
            mapState = state,
            modifier = modifier,
            onAction = onAction,
            onProfileClick = onProfileClick
        )
    }
}

@Composable
private fun LoadingMap() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMap(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message)
    }
}

@Composable
private fun ReadyMap(
    mapState: MapUiState.Ready,
    modifier: Modifier,
    onAction: (MapAction) -> Unit,
    onProfileClick: () -> Unit,
) {
    val context = LocalContext.current

    val h3Service = koinInject<H3Service>()

    // Check for Mapbox token
    val appInfo = remember {
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    }
    val mapboxToken = remember {
        appInfo.metaData?.getString("com.mapbox.token")
    }

    if (mapboxToken.isNullOrBlank() || mapboxToken == "null") {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Mapbox Token is missing! Check local.properties.")
        }
        return
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            onAction(MapAction.PermissionGranted)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val lastLocation = remember { mutableStateOf<Point?>(null) }
    val locationInitialized = remember { mutableStateOf(false) }
    val shouldCenterOnFirstLocation = remember { mutableStateOf(true) }

    // Fog of War
    val fogRenderer = remember { mutableStateOf<FogOfWarRenderer?>(null) }
    val fogInitialized = remember { mutableStateOf(false) }
    val fogInitError = remember { mutableStateOf(false) }

    fun animateToLocation(point: Point, zoom: Double? = null) {
        mapViewRef.value?.camera?.easeTo(
            CameraOptions.Builder()
                .center(point)
                .apply { zoom?.let { zoom(it) } }
                .build(),
            MapAnimationOptions.Builder().duration(300).build()
        )
    }

    // Remember the annotation manager for the current map view
    val annotationManager = remember(mapViewRef.value) {
        mapViewRef.value?.annotations?.createPointAnnotationManager()
    }

    // Update POI markers when the map view or POI list changes
    LaunchedEffect(mapViewRef.value, mapState.pois) {
        val manager = annotationManager ?: return@LaunchedEffect

        // Clear existing markers
        manager.deleteAll()

        // Create new markers for each POI
        mapState.pois.forEach { poi ->
            val point = Point.fromLngLat(poi.longitude, poi.latitude)
            val icon = createPoiMarkerBitmap(
                category = poi.category,
                discovered = poi.discovered,
                isMajor = poi.isMajor
            )

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(icon)

            manager.create(pointAnnotationOptions)
        }
    }

    // Show notification for newly discovered POIs
    LaunchedEffect(mapState.newlyDiscoveredPoiIds) {
        if (mapState.newlyDiscoveredPoiIds.isNotEmpty()) {
            val discoveredNames = mapState.newlyDiscoveredPoiIds.mapNotNull { id ->
                mapState.pois.find { it.id == id }?.name
            }

            if (discoveredNames.isNotEmpty()) {
                val message = if (discoveredNames.size == 1) {
                    "🎉 Discovered: ${discoveredNames.first()}!"
                } else {
                    "🎉 Discovered ${discoveredNames.size} new POIs!"
                }

                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                // Auto-dismiss notifications after showing
                mapState.newlyDiscoveredPoiIds.forEach { poiId ->
                    onAction(MapAction.DismissDiscoveryNotification(poiId))
                }
            }
        }
    }



    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                try {
                    MapView(context).apply {
                        mapViewRef.value = this
                        mapboxMap.loadStyle("mapbox://styles/szafran00/cmdusan3600d001pj4eri2fl1") { style ->
                            val renderer = FogOfWarRenderer(this, h3Service)
                            val success = renderer.initialize(style)
                            if (success) {
                                fogRenderer.value = renderer
                                fogInitialized.value = true
                            } else {
                                fogInitError.value = true
                            }
                        }

                        compass.updateSettings { enabled = true }
                        scalebar.updateSettings { enabled = true }
                        gestures.updateSettings {
                            scrollEnabled = true
                            rotateEnabled = true
                            pitchEnabled = false
                            pinchScrollEnabled = true
                        }

                        location.updateSettings {
                            enabled = true
                            pulsingEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    android.widget.TextView(context).apply {
                        text = "Error initializing map: ${e.message}"
                        setTextColor(android.graphics.Color.RED)
                        gravity = android.view.Gravity.CENTER
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(mapState.revealedHexagons, mapState.warsawHexagons, fogRenderer.value) {
            val renderer = fogRenderer.value
            if (renderer != null && mapState.warsawHexagons.isNotEmpty()) {
                renderer.updateFog(mapState.warsawHexagons, mapState.revealedHexagons)
            }
        }

        // Show error notification if fog initialization failed
        LaunchedEffect(fogInitError.value) {
            if (fogInitError.value) {
                Toast.makeText(
                    context,
                    "⚠️ Fog of War could not be loaded. Map will work without fog effect.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


        // Register and clean-up listeners
        DisposableEffect(mapViewRef.value, mapState.isFollowingUser) {
            val mapView = mapViewRef.value

            // Track user location and auto-centre on the first fix
            val positionListener = OnIndicatorPositionChangedListener { point ->
                lastLocation.value = point

                if (shouldCenterOnFirstLocation.value) {
                    shouldCenterOnFirstLocation.value = false
                    locationInitialized.value = true
                    // First center with zoom
                    mapView?.mapboxMap?.setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(15.0)
                            .build()
                    )
                } else if (mapState.isFollowingUser) {
                    // Smooth follow using setCamera (puck provides interpolation)
                    mapView?.mapboxMap?.setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .build()
                    )
                }
            }

            // Disable follow mode when the user manually moves the map
            val onMoveListener = object : OnMoveListener {
                override fun onMove(detector: MoveGestureDetector): Boolean {
                    // Only disable follow mode if it's a single-finger pan
                    // Multi-pointer moves are usually part of a zoom/rotate gesture
                    if (detector.pointersCount == 1) {
                        if (mapState.isFollowingUser) {
                            onAction(MapAction.ToggleFollowUser)
                        }
                    }
                    return false
                }

                override fun onMoveBegin(detector: MoveGestureDetector) {}

                override fun onMoveEnd(detector: MoveGestureDetector) {}
            }

            mapView?.let {
                it.location.addOnIndicatorPositionChangedListener(positionListener)
                it.gestures.addOnMoveListener(onMoveListener)
            }

            onDispose {
                mapView?.let {
                    it.location.removeOnIndicatorPositionChangedListener(positionListener)
                    it.gestures.removeOnMoveListener(onMoveListener)
                }
            }
        }

        // Re-center button
        FloatingActionButton(
            onClick = {
                mapViewRef.value?.let { _ ->
                    // Enable follow mode if not already enabled
                    if (!mapState.isFollowingUser) {
                        onAction(MapAction.ToggleFollowUser)
                    }

                    val point = lastLocation.value
                    if (point != null) {
                        animateToLocation(point, zoom = 15.0)
                    } else {
                        Toast.makeText(context, "Waiting for location...", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            containerColor = AppColors.green,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
        }
    }
}
