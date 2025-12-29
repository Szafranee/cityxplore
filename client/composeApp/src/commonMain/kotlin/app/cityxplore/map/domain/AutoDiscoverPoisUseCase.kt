package app.cityxplore.map.domain

import app.cityxplore.core.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Use case for automatically discovering POIs based on the user's location.
 *
 * This use case monitors user location and triggers POI discovery when
 * the user is within the discovery radius (50 meters by default).
 *
 * @property getPoisUseCase Usecase to fetch all POIs with discovery status.
 * @property discoverPoiUseCase Usecase to discover a specific POI.
 */
class AutoDiscoverPoisUseCase(
    private val getPoisUseCase: GetPoisWithDiscoveriesUseCase,
    private val discoverPoiUseCase: DiscoverPoiUseCase
) {
    companion object {
        /**
         * Discovery radius in meters. User must be within this distance to discover a POI.
         */
        const val DISCOVERY_RADIUS_METERS = 100.0
    }

    /**
     * Checks if any undiscovered POIs are within discovery range and discovers them.
     *
     * @param currentLocation Current user location.
     * @return List of newly discovered POI IDs.
     */
    suspend fun checkAndDiscoverNearbyPois(currentLocation: Location): Result<List<String>> {
        return try {
            // Fetch all POIs
            val poisResult = getPoisUseCase()
            if (poisResult.isFailure) {
                return Result.failure(poisResult.exceptionOrNull() ?: Exception("Failed to fetch POIs"))
            }

            val pois = poisResult.getOrThrow()
            val discoveredIds = mutableListOf<String>()

            // Check each undiscovered POI
            pois.filter { !it.discovered }.forEach { poi ->
                val distance = calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    poi.latitude,
                    poi.longitude
                )

                if (distance <= DISCOVERY_RADIUS_METERS) {
                    // Attempt to discover the POI
                    val discoverResult = discoverPoiUseCase(poi.id)
                    if (discoverResult.isSuccess) {
                        discoveredIds.add(poi.id)
                    }
                    // Ignore 409 Conflict (already discovered) errors silently
                }
            }

            Result.success(discoveredIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates distance between two points using Haversine formula.
     *
     * @return Distance in meters.
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0

        val lat1Rad = toRadians(lat1)
        val lat2Rad = toRadians(lat2)
        val deltaLatRad = toRadians(lat2 - lat1)
        val deltaLonRad = toRadians(lon2 - lon1)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    /**
     * Converts degrees to radians.
     */
    private fun toRadians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0
}
