package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository

/**
 * Usecase for marking a shared POI as viewed.
 */
class MarkSharedPoiViewedUseCase(
    private val repository: SharedPoiRepository
) {
    /**
     * Marks the specified shared POI as viewed.
     * @param sharedPoiId The ID of the shared POI to mark as viewed.
     * @return Result containing the updated SharedPoi on success.
     */
    suspend operator fun invoke(sharedPoiId: String): Result<SharedPoi> {
        return repository.markViewed(sharedPoiId)
    }
}
