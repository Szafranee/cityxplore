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

/**
 * Manager for rendering Fog of War on Mapbox map.
 *
 * Creates a FillLayer with hexagons representing unexplored areas.
 * Only unrevealed hexagons are rendered for performance.
 *
 * @property mapView The Mapbox MapView instance.
 * @property fogColor Hex colour string (e.g. "#80000000" for semi-transparent black).
 */
class FogOfWarRenderer(
    private val mapView: MapView,
    private val fogColor: String = "#B0404040"
) {
    private val h3Service = app.cityxplore.data.service.AndroidH3Service()

    private companion object {
        const val FOG_SOURCE_ID = "fog-of-war-source"
        const val FOG_LAYER_ID = "fog-of-war-layer"
    }

    /**
     * Initialises the fog layer on the map.
     * Should be called once after the map style is loaded.
     */
    fun initialize(style: Style) {
        // Add an empty source initially
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
                val hexBoundary = h3Service.cellToBoundary(h3Service.stringToH3(hexIndex))
                val coordinates = hexBoundary.map { (lat, lng) ->
                    com.mapbox.geojson.Point.fromLngLat(lng, lat)
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
     * Parses color string (e.g., "#80000000") to Android Color int.
     */
    private fun parseColorString(colorString: String): Int {
        return try {
            colorString.toColorInt()
        } catch (_: Exception) {
            "#B0404040".toColorInt() // Fallback to default grey
        }
    }
}
