package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.SharePoiRequest
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository

/**
 * Usecase for sharing a POI with another user.
 * Validates the request and delegates to the repository.
 */
class SharePoiUseCase(
    private val repository: SharedPoiRepository
) {
    /**
     * Shares a POI with the specified recipient.
     * @param request The share request containing recipient, POI data, and optional message.
     * @return Result containing the created SharedPoi on success.
     */
    suspend operator fun invoke(request: SharePoiRequest): Result<SharedPoi> {
        // Validation is already enforced by SharePoiRequest's init block
        return repository.sharePoi(request)
    }
}
