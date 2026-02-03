package app.cityxplore.map.data

import android.content.Context
import android.util.Log
import app.cityxplore.map.domain.RegionDefinition
import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "RegionHexagonCache"

internal actual suspend fun loadRegionHexagons(region: RegionDefinition): Set<String> =
    withContext(Dispatchers.Default) {
        RegionHexagonCache.getOrCompute(region = region)
    }

internal actual fun clearHexagonCache() {
    RegionHexagonCache.clearCache()
}

private object RegionHexagonCache : KoinComponent {

    private val context: Context by inject()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Lazy H3 initialisation with proper error handling
    private var h3Instance: H3Core? = null

    @Synchronized
    private fun getH3(): H3Core {
        h3Instance?.let { return it }

        val instance = runCatching {
            System.loadLibrary("h3-java")
            H3Core.newSystemInstance()
        }.getOrElse { error ->
            Log.w(TAG, "Failed to load system H3 library, falling back to bundled: ${error.message}")
            H3Core.newInstance()
        }

        h3Instance = instance
        return instance
    }

    // Simple in-memory cache (clears on app restart)
    private val memoryCache = mutableMapOf<String, Set<String>>()

    @Synchronized
    fun getOrCompute(region: RegionDefinition): Set<String> {
        val cacheKey = "${region.id}_${region.h3Resolution}"

        // Check in-memory cache - return only if not empty
        memoryCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Cache hit for $cacheKey: ${cached.size} hexes")
                return cached
            } else {
                // Cache has an empty set - remove it and recompute
                Log.w(TAG, "Cache had empty set for $cacheKey, recomputing...")
                memoryCache.remove(cacheKey)
            }
        }

        Log.d(TAG, "Computing hexagons for $cacheKey...")

        // Cache miss - compute via polyfill
        val geoJsonText = runCatching {
            context.assets.open(region.boundaryAssetPath).bufferedReader().use { it.readText() }
        }.getOrElse { e ->
            Log.e(TAG, "Failed to read GeoJSON asset: ${region.boundaryAssetPath}", e)
            return emptySet()
        }

        Log.d(TAG, "Read GeoJSON: ${geoJsonText.length} chars")

        val featureCollection = runCatching {
            json.decodeFromString<GeoJsonFeatureCollection>(geoJsonText)
        }.getOrElse { e ->
            Log.e(TAG, "Failed to parse GeoJSON", e)
            return emptySet()
        }

        val polygon = featureCollection.features.firstOrNull()?.geometry
        if (polygon == null) {
            Log.e(TAG, "No polygon geometry found in GeoJSON")
            return emptySet()
        }

        val outerRing: List<LatLng> = polygon.coordinates
            .firstOrNull()
            ?.mapNotNull { coord ->
                if (coord.size < 2) return@mapNotNull null
                LatLng(coord[1], coord[0])
            }
            ?: emptyList()

        if (outerRing.isEmpty()) {
            Log.e(TAG, "Empty outer ring from polygon coordinates")
            return emptySet()
        }

        Log.d(TAG, "Outer ring has ${outerRing.size} points")

        val ring = if (outerRing.size >= 2 && outerRing.first() == outerRing.last()) {
            outerRing.dropLast(1)
        } else {
            outerRing
        }

        val hexes = runCatching {
            val h3 = getH3()
            h3.polygonToCells(ring, emptyList(), region.h3Resolution)
                .map { h3.h3ToString(it) }
                .toSet()
        }.getOrElse { e ->
            Log.e(TAG, "H3 polygonToCells failed", e)
            emptySet()
        }

        Log.d(TAG, "Generated ${hexes.size} hexagons for $cacheKey")

        // Only cache if we got valid data
        if (hexes.isNotEmpty()) {
            memoryCache[cacheKey] = hexes
        }

        return hexes
    }

    /**
     * Clears the in-memory cache. Called on logout to ensure fresh data on the next login.
     */
    @Synchronized
    fun clearCache() {
        Log.d(TAG, "Clearing hexagon cache")
        memoryCache.clear()
        h3Instance = null
    }
}

@Serializable
private data class GeoJsonFeatureCollection(
    val type: String,
    val features: List<GeoJsonFeature>
)

@Serializable
private data class GeoJsonFeature(
    val type: String,
    val geometry: GeoJsonPolygon
)

@Serializable
private data class GeoJsonPolygon(
    val type: String,
    val coordinates: List<List<List<Double>>>
)
