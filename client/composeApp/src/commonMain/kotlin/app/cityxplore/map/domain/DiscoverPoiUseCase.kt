package app.cityxplore.map.domain

import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.data.UserPoiDiscoveryDto

/**
 * Use case for discovering a Point of Interest.
 *
 * This use case handles the business logic of discovering a POI,
 * including validation and error handling.
 *
 * @property repository The repository providing POI discovery operations.
 */
class DiscoverPoiUseCase(
    private val repository: PoiRepository
) {
    /**
     * Executes the use case to discover a POI by its ID.
     *
     * @param poiId The unique identifier of the POI to discover.
     * @return [Result] containing [UserPoiDiscoveryDto] with newly unlocked achievements on success, or exception on failure.
     */
    suspend operator fun invoke(poiId: String): Result<UserPoiDiscoveryDto> {
        return repository.discoverPoi(poiId)
    }
}
