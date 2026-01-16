package app.cityxplore.map.domain

import app.cityxplore.achievements.data.toDomain
import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.location.Location
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Automatically discovers POIs when user enters their proximity range.
 *
 * Monitors user location and triggers discovery for POIs within the defined radius.
 * Handles edge cases like concurrent discoveries and network errors gracefully.
 *
 * @property getPoisUseCase Use case to fetch all POIs with their discovery status
 * @property discoverPoiUseCase Use case to mark a POI as discovered
 */
class AutoDiscoverPoisUseCase(
    private val getPoisUseCase: GetPoisWithDiscoveriesUseCase,
    private val discoverPoiUseCase: DiscoverPoiUseCase
) {
    companion object {
        const val DISCOVERY_RADIUS_METERS = 200.0
    }

    /**
     * Discovers all undiscovered POIs within range of the current location.
     *
     * @param currentLocation User's current GPS coordinates
     * @return Result containing discovery result with newly discovered POI IDs and unlocked achievements, or error if operation fails
     */
    suspend fun checkAndDiscoverNearbyPois(currentLocation: Location): Result<DiscoveryResult> {
        return try {
            val poisResult = getPoisUseCase()
            if (poisResult.isFailure) {
                return Result.failure(poisResult.exceptionOrNull() ?: Exception("Failed to fetch POIs"))
            }

            val pois = poisResult.getOrThrow()
            val discoveredIds = mutableListOf<String>()
            val allAchievements = mutableListOf<Achievement>()

            pois.filter { !it.discovered }.forEach { poi ->
                val distance = calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    poi.latitude,
                    poi.longitude
                )

                if (distance <= DISCOVERY_RADIUS_METERS) {
                    val discoverResult = discoverPoiUseCase(poi.id)
                    if (discoverResult.isSuccess) {
                        discoveredIds.add(poi.id)
                        val discoveryDto = discoverResult.getOrThrow()
                        allAchievements.addAll(discoveryDto.newlyUnlockedAchievements.map { achievementDto ->
                            achievementDto.toDomain()
                        })
                    } else {
                        handleDiscoveryError(poi, discoverResult.exceptionOrNull())
                    }
                }
            }

            Result.success(DiscoveryResult(discoveredIds, allAchievements.distinctBy { it.id }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Handles discovery errors with proper logging.
     *
     * 409 Conflict responses are ignored silently as they indicate
     * the POI was already discovered (race condition between devices).
     */
    private fun handleDiscoveryError(poi: PoiModel, exception: Throwable?) {
        val isAlreadyDiscovered = exception is ClientRequestException &&
                exception.response.status == HttpStatusCode.Conflict

        if (!isAlreadyDiscovered) {
            println("Failed to discover POI ${poi.id} (${poi.name}): ${exception?.message}")
            exception?.printStackTrace()
        }
    }

    /**
     * Calculates great-circle distance between two geographic coordinates.
     *
     * Uses Haversine formula for accuracy over short distances.
     *
     * @return Distance in meters
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

    private fun toRadians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0
}
