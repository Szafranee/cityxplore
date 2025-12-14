package app.cityxplore.map.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import app.cityxplore.theme.AppColors
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
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
    rememberCoroutineScope()

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val lastLocation = remember { mutableStateOf<Point?>(null) }
    val locationInitialized = remember { mutableStateOf(false) }
    val isFollowingUser = remember(mapState.isFollowingUser) { mutableStateOf(mapState.isFollowingUser) }
    val shouldCenterOnFirstLocation = remember { mutableStateOf(true) }

    remember {
        val bitmap = createMarkerWithArrow(color = AppColors.green.toArgb(), size = 150)
        ImageHolder.from(bitmap)
    }

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

    // Update markers when POIs change
    // Note: In a real app, we should use a more efficient way to update markers
    // instead of clearing and adding all of them every time.
    if (mapViewRef.value != null) {
        val mapView = mapViewRef.value!!
        val annotationManager = remember(mapView) {
            mapView.annotations.createPointAnnotationManager()
        }

        // Simple diffing or just clear and add for now
        annotationManager.deleteAll()
        mapState.pois.forEach { poi ->
            val point = Point.fromLngLat(poi.longitude, poi.latitude)
            val icon = if (poi.discovered) {
                // Use a different icon for discovered
                createMarkerWithArrow(android.graphics.Color.BLUE, 100)
            } else {
                createMarkerWithArrow(android.graphics.Color.RED, 100)
            }

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(icon)

            annotationManager.create(pointAnnotationOptions)
        }
    }


    val positionListener = remember {
        OnIndicatorPositionChangedListener { point ->
            lastLocation.value = point

            if (shouldCenterOnFirstLocation.value) {
                shouldCenterOnFirstLocation.value = false
                locationInitialized.value = true
                centerOnLocationInstantly(point)
            } else if (isFollowingUser.value) {
                animateToLocation(point)
            }
        }
    }

    val onMoveListener = remember {
        object : OnMoveListener {
            override fun onMove(detector: MoveGestureDetector): Boolean {
                if (isFollowingUser.value) {
                    isFollowingUser.value = false
                    onAction(MapAction.ToggleFollowUser)
                }
                return false
            }

            override fun onMoveBegin(detector: MoveGestureDetector) {}

            override fun onMoveEnd(detector: MoveGestureDetector) {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    mapViewRef.value = this
                    // Load style - using standard street style for now
                    mapboxMap.loadStyle("mapbox://styles/mapbox/streets-v12")

                    location.addOnIndicatorPositionChangedListener(positionListener)

                    mapboxMap.addOnMapClickListener {
                        if (isFollowingUser.value) {
                            isFollowingUser.value = false
                            onAction(MapAction.ToggleFollowUser)
                        }
                        true
                    }

                    gestures.addOnMoveListener(onMoveListener)

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
            },
            modifier = Modifier.fillMaxSize()
        )

        // User profile banner placeholder
        FloatingActionButton(
            onClick = {
                mapViewRef.value?.let { mapView ->
                    isFollowingUser.value = true
                    onAction(MapAction.ToggleFollowUser)

                    val point = lastLocation.value
                    if (point != null) {
                        animateToLocation(point)
                    } else {
                        Toast.makeText(context, "Waiting for location...", Toast.LENGTH_SHORT).show()
                    }
                }
            },
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
