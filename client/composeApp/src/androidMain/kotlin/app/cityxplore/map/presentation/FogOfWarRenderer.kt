package app.cityxplore.map.presentation

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import app.cityxplore.domain.service.H3Service
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for rendering Fog of War on Mapbox map.
 *
 * Two-layer approach.
 *
 * Critical rule: a hex must be visible in the AT MOST ONE layer with non-zero opacity at any moment.
 * Otherwise, opacity sums up and the user sees "double fog".
 *
 * Additionally: avoid restarting fade animation from BASE on every update, because that creates a visible
 * opacity jump ("blink") when discoveries happen frequently.
 */
class FogOfWarRenderer(
    private val mapView: MapView,
    private val h3Service: H3Service,
    private val fogColor: String = "#404040"
) {
    private var lastRevealedHexes: Set<String> = emptySet()

    private var fadeAnimator: ValueAnimator? = null
    private var currentFadingOpacity: Double = 0.0

    // Cached features map to avoid expensive regeneration
    // Maps H3 index -> Feature
    private var currentFogFeatures: Map<String, Feature> = emptyMap()

    // Hexes currently fading out (keep track of IDs to manage lifecycle, features looked up or created)
    private var currentFadingFeatures: MutableList<Feature> = mutableListOf()

    private companion object {
        const val FOG_SOURCE_ID = "fog-of-war-source"
        const val FOG_LAYER_ID = "fog-of-war-layer"
        const val FADING_SOURCE_ID = "fog-of-war-fading-source"
        const val FADING_LAYER_ID = "fog-of-war-fading-layer"
        const val FADE_DURATION_MS = 5000L

        const val BASE_FOG_OPACITY = 0.7
    }

    private var isInitialized = false

    fun initialize(style: Style): Boolean {
        return try {
            val fogColorInt = parseColorString(fogColor)

            style.addSource(
                geoJsonSource(FOG_SOURCE_ID) {
                    featureCollection(FeatureCollection.fromFeatures(emptyList()))
                    tolerance(0.0)
                    buffer(0)
                }
            )
            style.addLayer(
                fillLayer(FOG_LAYER_ID, FOG_SOURCE_ID) {
                    fillColor(fogColorInt)
                    fillOpacity(BASE_FOG_OPACITY)
                    fillAntialias(false)
                    minZoom(5.0)
                }
            )

            style.addSource(
                geoJsonSource(FADING_SOURCE_ID) {
                    featureCollection(FeatureCollection.fromFeatures(emptyList()))
                    tolerance(0.0)
                    buffer(0)
                }
            )
            style.addLayer(
                fillLayer(FADING_LAYER_ID, FADING_SOURCE_ID) {
                    fillColor(fogColorInt)
                    fillOpacity(0.0)
                    fillAntialias(false)
                    visibility(Visibility.VISIBLE)
                    minZoom(5.0)
                }
            )

            currentFadingOpacity = 0.0
            isInitialized = true
            true
        } catch (e: Exception) {
            android.util.Log.e("FogOfWarRenderer", "Failed to initialize fog: ${e.message}", e)
            false
        }
    }

    private var isFirstUpdate = true

    private fun clampOpacity(value: Double): Double {
        return value.coerceIn(0.0, BASE_FOG_OPACITY)
    }

    suspend fun updateFog(allWarsawHexes: Set<String>, revealedHexes: Set<String>) {

        if (!isInitialized) {
            return
        }

        withContext(Dispatchers.Main) {
            val style = mapView.mapboxMap.style ?: return@withContext

            if (isFirstUpdate) {
                // Initial generation - expensive but done only once
                val fogHexes = allWarsawHexes - revealedHexes

                // Compute features on Default dispatcher
                val featureMap = withContext(Dispatchers.Default) {
                    fogHexes.asSequence()
                        .mapNotNull { hexId ->
                            createHexFeature(hexId)?.let { feature -> hexId to feature }
                        }
                        .toMap()
                }

                // Update state only after a successful computation
                currentFogFeatures = featureMap
                lastRevealedHexes = revealedHexes
                isFirstUpdate = false

                currentFadingFeatures.clear()
                currentFadingOpacity = 0.0

                style.getSourceAs<GeoJsonSource>(FOG_SOURCE_ID)
                    ?.featureCollection(FeatureCollection.fromFeatures(currentFogFeatures.values.toList()))

                style.getSourceAs<GeoJsonSource>(FADING_SOURCE_ID)
                    ?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
                style.getLayerAs<FillLayer>(FADING_LAYER_ID)
                    ?.fillOpacity(0.0)

                return@withContext
            }

            // Differential update
            val newlyRevealed = revealedHexes - lastRevealedHexes
            if (newlyRevealed.isEmpty()) {
                // No changes, just update lastRevealed to be in sync
                lastRevealedHexes = revealedHexes
                return@withContext
            }

            lastRevealedHexes = revealedHexes

            // Identify features to move from Fog to Fading
            val featuresToFade = newlyRevealed.mapNotNull { hexId ->
                currentFogFeatures[hexId] ?: createHexFeature(hexId)
            }

            // Remove from static fog (Map operation is O(N_removed) << O(N_total))
            currentFogFeatures = currentFogFeatures - newlyRevealed

            // Add to the fading list
            currentFadingFeatures.addAll(featuresToFade)

            val fogSource = style.getSourceAs<GeoJsonSource>(FOG_SOURCE_ID)
            val fadingSource = style.getSourceAs<GeoJsonSource>(FADING_SOURCE_ID)
            val fadingLayer = style.getLayerAs<FillLayer>(FADING_LAYER_ID)

            // Update sources with cached features
            fogSource?.featureCollection(FeatureCollection.fromFeatures(currentFogFeatures.values.toList()))
            fadingSource?.featureCollection(FeatureCollection.fromFeatures(currentFadingFeatures))

            // If fade is already running, do nothing besides keeping the current opacity.
            if (fadeAnimator?.isRunning == true) {
                fadingLayer?.fillOpacity(clampOpacity(currentFadingOpacity))
                return@withContext
            }

            // Start a new fade
            val startOpacity = when {
                currentFadingOpacity in 0.0001..BASE_FOG_OPACITY -> currentFadingOpacity
                currentFadingOpacity <= 0.0001 -> BASE_FOG_OPACITY
                else -> BASE_FOG_OPACITY
            }

            fadeAnimator = ValueAnimator.ofFloat(startOpacity.toFloat(), 0.0f).apply {
                duration = ((FADE_DURATION_MS * (startOpacity / BASE_FOG_OPACITY))).toLong().coerceAtLeast(1L)
                interpolator = LinearInterpolator()

                addUpdateListener { animator ->
                    currentFadingOpacity = clampOpacity((animator.animatedValue as Float).toDouble())
                    fadingLayer?.fillOpacity(currentFadingOpacity)
                }

                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        currentFadingFeatures.clear()
                        currentFadingOpacity = 0.0

                        fadingSource?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
                        fadingLayer?.fillOpacity(0.0)
                    }

                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        // Maintain state on cancel
                    }
                })

                start()
            }
        }
    }

    private fun createHexFeature(hexIndex: String): Feature? {
        return try {
            val hexBoundary = h3Service.cellToBoundary(h3Service.stringToH3(hexIndex))
            val coordinates = hexBoundary.map { (lat, lng) ->
                com.mapbox.geojson.Point.fromLngLat(lng, lat)
            }
            val closedCoordinates = coordinates + coordinates.first()
            Feature.fromGeometry(Polygon.fromLngLats(listOf(closedCoordinates)))
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses color string (e.g., "#80000000") to Android Color int.
     */
    private fun parseColorString(colorString: String): Int {
        return try {
            colorString.toColorInt()
        } catch (_: Exception) {
            "#404040".toColorInt()
        }
    }
}
