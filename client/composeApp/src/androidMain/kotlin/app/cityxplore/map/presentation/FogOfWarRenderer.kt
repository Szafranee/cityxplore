package app.cityxplore.map.presentation

import androidx.core.graphics.toColorInt
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.uber.h3core.H3Core

/**
 * Manager for rendering Fog of War on Mapbox map.
 *
 * Creates a FillLayer with hexagons representing unexplored areas.
 * Only unrevealed hexagons are rendered for performance.
 *
 * @property mapView The Mapbox MapView instance.
 * @property fogColor Hex color string (e.g., "#80000000" for semi-transparent black).
 */
class FogOfWarRenderer(
    private val mapView: MapView,
    private val fogColor: String = "#B0404040"
) {
    private val h3 = H3Core.newInstance()

    private companion object {
        const val FOG_SOURCE_ID = "fog-of-war-source"
        const val FOG_LAYER_ID = "fog-of-war-layer"
    }

    /**
     * Initializes the fog layer on the map.
     * Should be called once after the map style is loaded.
     */
    fun initialize(style: Style) {
        // Add empty source initially
        style.addSource(
            geoJsonSource(FOG_SOURCE_ID) {
                featureCollection(FeatureCollection.fromFeatures(emptyList()))
            }
        )

        // Add fill layer for fog
        style.addLayer(
            fillLayer(FOG_LAYER_ID, FOG_SOURCE_ID) {
                fillColor(parseColorString(fogColor))
                fillOpacity(1.0)
            }
        )
    }

    /**
     * Updates the fog layer with the current unrevealed hexagons.
     *
     * @param allWarsawHexes All hexagons covering the Warsaw region (pre-generated).
     * @param revealedHexes Set of H3 indices that have been revealed by the user.
     */
    fun updateFog(allWarsawHexes: Set<String>, revealedHexes: Set<String>) {
        val style = mapView.mapboxMap.style ?: return

        // Calculate unrevealed hexagons
        val unrevealedHexes = allWarsawHexes - revealedHexes

        // Convert hex indices to GeoJSON polygons
        val features = unrevealedHexes.mapNotNull { hexIndex ->
            try {
                val hexBoundary = h3.cellToBoundary(h3.stringToH3(hexIndex))
                val coordinates = hexBoundary.map { latLng ->
                    com.mapbox.geojson.Point.fromLngLat(latLng.lng, latLng.lat)
                }
                // Close the polygon by adding first point at the end
                val closedCoordinates = coordinates + coordinates.first()

                Feature.fromGeometry(
                    Polygon.fromLngLats(listOf(closedCoordinates))
                )
            } catch (_: Exception) {
                null // Skip invalid hex indices
            }
        }

        // Update the source with new features
        val source = style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>(FOG_SOURCE_ID)
        source?.featureCollection(FeatureCollection.fromFeatures(features))
    }

    /**
     * Clears the fog layer (shows all hexagons as revealed).
     */
    fun clearFog() {
        val style = mapView.mapboxMap.style ?: return
        val source = style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>(FOG_SOURCE_ID)
        source?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
    }

    /**
     * Parses color string (e.g., "#80000000") to Android Color int.
     */
    private fun parseColorString(colorString: String): Int {
        return try {
            colorString.toColorInt()
        } catch (_: Exception) {
            "#B0404040".toColorInt() // Fallback to default gray
        }
    }
}

/**
 * Pre-generates all hexagons covering the Warsaw region.
 * This should be done once at initialization and cached.
 *
 * @param minLat Southern boundary.
 * @param maxLat Northern boundary.
 * @param minLng Western boundary.
 * @param maxLng Eastern boundary.
 * @param resolution H3 resolution level.
 * @return Set of H3 hex indices covering the bounding box.
 */
fun generateWarsawHexagons(
    minLat: Double,
    maxLat: Double,
    minLng: Double,
    maxLng: Double,
    resolution: Int
): Set<String> {
    val h3 = H3Core.newInstance()
    val hexagons = mutableSetOf<String>()

    // Generate a grid of points within the bounding box and convert to hex indices
    val latStep = 0.01 // ~1km steps
    val lngStep = 0.01

    var lat = minLat
    while (lat <= maxLat) {
        var lng = minLng
        while (lng <= maxLng) {
            val cellIndex = h3.latLngToCell(lat, lng, resolution)
            hexagons.add(h3.h3ToString(cellIndex))
            lng += lngStep
        }
        lat += latStep
    }

    return hexagons
}
