package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository
import kotlinx.coroutines.flow.Flow

/**
 * Usecase for getting POIs sent by the current user.
 */
class GetSentSharedPoisUseCase(
    private val repository: SharedPoiRepository
) {
    /**
     * Observes the list of POIs shared by the current user.
     */
    operator fun invoke(): Flow<List<SharedPoi>> = repository.getSentPois()

    /**
     * Refreshes the sent POIs from the API.
     */
    suspend fun refresh(): Result<Unit> = repository.refreshSentPois()
}
