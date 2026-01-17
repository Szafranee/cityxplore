package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository
import kotlinx.coroutines.flow.Flow

/**
 * Usecase for getting POIs received by the current user.
 */
class GetReceivedSharedPoisUseCase(
    private val repository: SharedPoiRepository
) {
    /**
     * Observes the list of POIs shared to the current user.
     */
    operator fun invoke(): Flow<List<SharedPoi>> = repository.getReceivedPois()

    /**
     * Refreshes the received POIs from the API.
     */
    suspend fun refresh(): Result<Unit> = repository.refreshReceivedPois()
}
