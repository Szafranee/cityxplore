package app.cityxplore.social.domain

import app.cityxplore.social.domain.repository.SharedPoiRepository

/**
 * Usecase for deleting a shared POI.
 * Only the sharer can delete a shared POI.
 */
class DeleteSharedPoiUseCase(
    private val repository: SharedPoiRepository
) {
    /**
     * Deletes the specified shared POI.
     * @param sharedPoiId The ID of the shared POI to delete.
     * @return Result indicating success or failure.
     */
    suspend operator fun invoke(sharedPoiId: String): Result<Unit> {
        return repository.deleteSharedPoi(sharedPoiId)
    }
}
