package app.cityxplore.journal.domain

import app.cityxplore.map.data.PoiRepository

/**
 * Use case for toggling the favorite status of a Point of Interest (POI).
 *
 * This use case encapsulates the logic for adding or removing a POI from the user's favorites list.
 * It interacts with the [PoiRepository] to persist the change.
 *
 * @property poiRepository The repository used to perform the toggle operation.
 */
class ToggleFavoriteUseCase(
    private val poiRepository: PoiRepository
) {
    /**
     * Executes the use case to toggle the favorite status for the specified POI.
     *
     * @param poiId The unique identifier of the POI.
     * @return [Result] indicating success ([Unit]) or failure (exception).
     */
    suspend operator fun invoke(poiId: String): Result<Unit> {
        return poiRepository.toggleFavorite(poiId)
    }
}
