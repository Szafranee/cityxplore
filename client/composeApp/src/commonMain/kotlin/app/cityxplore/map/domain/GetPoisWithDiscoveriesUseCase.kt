package app.cityxplore.map.domain

import app.cityxplore.map.data.PoiRepository

/**
 * Use case for fetching all Points of Interest with their discovery status.
 *
 * This use case combines POI data with user discovery data to mark which
 * POIs have been discovered by the current user.
 *
 * @property repository The repository providing POI and discovery data.
 */
class GetPoisWithDiscoveriesUseCase(
    private val repository: PoiRepository
) {
    /**
     * Executes the use case to fetch all POIs with discovery status.
     *
     * @return [Result] containing a list of [PoiModel] with updated discovery flags.
     */
    suspend operator fun invoke(): Result<List<PoiModel>> {
        val poisResult = repository.fetchPois()
        if (poisResult.isFailure) {
            return poisResult
        }

        val discoveriesResult = repository.fetchUserDiscoveries()
        if (discoveriesResult.isFailure) {
            return Result.failure(discoveriesResult.exceptionOrNull() ?: Exception("Failed to fetch discoveries"))
        }

        val pois = poisResult.getOrThrow()
        val discoveriesMap = discoveriesResult.getOrThrow()

        val updatedPois = pois.map { poi ->
            val discovery = discoveriesMap[poi.id]
            poi.copy(
                discovered = discovery != null,
                discoveryDate = discovery?.discoveredAt,
                isFavorite = discovery?.favorite ?: false
            )
        }

        return Result.success(updatedPois)
    }
}
