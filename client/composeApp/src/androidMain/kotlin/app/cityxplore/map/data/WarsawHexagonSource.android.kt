package app.cityxplore.map.data

import android.content.Context
import app.cityxplore.map.domain.RegionDefinition
import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal actual suspend fun loadRegionHexagons(region: RegionDefinition): Set<String> =
    withContext(Dispatchers.Default) {
        RegionHexagonCache.getOrCompute(region = region)
    }

private object RegionHexagonCache : KoinComponent {

    private val context: Context by inject()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val h3: H3Core by lazy {
        runCatching {
            System.loadLibrary("h3-java")
            H3Core.newSystemInstance()
        }.getOrElse {
            H3Core.newInstance()
        }
    }

    // Simple in-memory cache (clears on app restart)
    private val memoryCache = mutableMapOf<String, Set<String>>()

    @Synchronized
    fun getOrCompute(region: RegionDefinition): Set<String> {
        val cacheKey = "${region.id}_${region.h3Resolution}"

        // Check in-memory cache
        memoryCache[cacheKey]?.let { return it }

        // Cache miss - compute via polyfill
        val geoJsonText = runCatching {
            context.assets.open(region.boundaryAssetPath).bufferedReader().use { it.readText() }
        }.getOrElse {
            return emptySet()
        }

        val featureCollection = runCatching {
            json.decodeFromString<GeoJsonFeatureCollection>(geoJsonText)
        }.getOrElse { return emptySet() }

        val polygon = featureCollection.features.firstOrNull()?.geometry ?: return emptySet()

        val outerRing: List<LatLng> = polygon.coordinates
            .firstOrNull()
            ?.mapNotNull { coord ->
                if (coord.size < 2) return@mapNotNull null
                LatLng(coord[1], coord[0])
            }
            ?: emptyList()

        if (outerRing.isEmpty()) return emptySet()

        val ring = if (outerRing.size >= 2 && outerRing.first() == outerRing.last()) {
            outerRing.dropLast(1)
        } else {
            outerRing
        }

        val hexes = runCatching {
            h3.polygonToCells(ring, emptyList(), region.h3Resolution)
                .map { h3.h3ToString(it) }
                .toSet()
        }.getOrElse {
            emptySet()
        }

        memoryCache[cacheKey] = hexes
        return hexes
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
