package app.cityxplore.map.domain

import app.cityxplore.achievements.data.toDomain
import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.location.Location
import app.cityxplore.core.utils.calculateDistance
import app.cityxplore.map.data.PoiRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

/**
 * Automatically discovers POIs when the user enters their proximity range.
 *
 * Monitors user location and triggers discovery for POIs within the defined radius.
 * Handles edge cases like concurrent discoveries and network errors gracefully.
 *
 * Uses local POI cache for discovery checks, enabling offline discovery.
 *
 * @property poiRepository Repository to access local POI data
 * @property discoverPoiUseCase Use case to mark a POI as discovered
 */
class AutoDiscoverPoisUseCase(
    private val poiRepository: PoiRepository,
    private val discoverPoiUseCase: DiscoverPoiUseCase
) {
    companion object {
        const val DISCOVERY_RADIUS_METERS = 200.0
    }

    /**
     * Discovers all undiscovered POIs within range of the current location.
     * Uses local cached POI data for offline support.
     *
     * @param currentLocation User's current GPS coordinates
     * @return Result containing the discovery result with newly discovered POI IDs and unlocked achievements, or error if operation fails
     */
    suspend fun checkAndDiscoverNearbyPois(currentLocation: Location): Result<DiscoveryResult> {
        return try {
            // Use local POIs for offline support
            val poisResult = poiRepository.getLocalPois()
            if (poisResult.isFailure) {
                return Result.failure(poisResult.exceptionOrNull() ?: Exception("Failed to get local POIs"))
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
}
