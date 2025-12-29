package app.cityxplore.map.presentation

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar

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


    fun animateToLocation(point: Point, zoom: Double = 15.0) {
        mapViewRef.value?.camera?.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(zoom)
                .build(),
            MapAnimationOptions.Builder().duration(1000).build()
        )
    }

    fun centerOnLocationInstantly(point: Point, zoom: Double = 15.0) {
        mapViewRef.value?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(zoom)
                .build()
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
            val icon = if (poi.discovered) {
                createMarkerWithArrow(android.graphics.Color.BLUE, 100)
            } else {
                createMarkerWithArrow(android.graphics.Color.RED, 100)
            }

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(icon)

            manager.create(pointAnnotationOptions)
        }
    }



    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                try {
                    MapView(context).apply {
                        mapViewRef.value = this
                        mapboxMap.loadStyle("mapbox://styles/szafran00/cmdusan3600d001pj4eri2fl1")

                        compass.updateSettings { enabled = true }
                        scalebar.updateSettings { enabled = true }
                        gestures.updateSettings {
                            scrollEnabled = true
                            rotateEnabled = true
                            pitchEnabled = true
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

        // Register and clean-up listeners
        DisposableEffect(mapViewRef.value, mapState.isFollowingUser) {
            val mapView = mapViewRef.value

            // Track user location and auto-centre on the first fix
            val positionListener = OnIndicatorPositionChangedListener { point ->
                lastLocation.value = point

                if (shouldCenterOnFirstLocation.value) {
                    shouldCenterOnFirstLocation.value = false
                    locationInitialized.value = true
                    centerOnLocationInstantly(point)
                } else if (mapState.isFollowingUser) {
                    animateToLocation(point)
                }
            }

            // Disable follow mode when the user manually moves the map
            val onMoveListener = object : OnMoveListener {
                override fun onMove(detector: MoveGestureDetector): Boolean {
                    if (mapState.isFollowingUser) {
                        onAction(MapAction.ToggleFollowUser)
                    }
                    return false
                }

                override fun onMoveBegin(detector: MoveGestureDetector) {}

                override fun onMoveEnd(detector: MoveGestureDetector) {}
            }

            val mapClickListener: (Point) -> Boolean = { _ ->
                if (mapState.isFollowingUser) {
                    onAction(MapAction.ToggleFollowUser)
                }
                true
            }

            mapView?.let {
                it.location.addOnIndicatorPositionChangedListener(positionListener)
                it.gestures.addOnMoveListener(onMoveListener)
                it.mapboxMap.addOnMapClickListener(mapClickListener)
            }

            onDispose {
                mapView?.let {
                    it.location.removeOnIndicatorPositionChangedListener(positionListener)
                    it.gestures.removeOnMoveListener(onMoveListener)
                    it.gestures.removeOnMapClickListener(mapClickListener)
                }
            }
        }

        // Re-center button
        FloatingActionButton(
            onClick = {
                mapViewRef.value?.let { _ ->
                    onAction(MapAction.ToggleFollowUser)

                    val point = lastLocation.value
                    if (point != null) {
                        animateToLocation(point)
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

fun createMarkerWithArrow(color: Int, size: Int): Bitmap {
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    val centerX = size / 2f
    val centerY = size / 2f
    val circleRadius = size * 0.15f

    val outerPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, circleRadius + 8, outerPaint)

    val innerPaint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, circleRadius, innerPaint)

    return bitmap
}
