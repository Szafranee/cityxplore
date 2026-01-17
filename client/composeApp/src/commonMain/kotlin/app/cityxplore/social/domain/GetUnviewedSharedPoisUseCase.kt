package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository
import kotlinx.coroutines.flow.Flow

/**
 * Usecase for getting unviewed POIs received by the current user.
 * Useful for showing notification badges.
 */
class GetUnviewedSharedPoisUseCase(
    private val repository: SharedPoiRepository
) {
    /**
     * Observes the list of unviewed POIs shared to the current user.
     */
    operator fun invoke(): Flow<List<SharedPoi>> = repository.getUnviewedPois()

    /**
     * Returns the count of unviewed shared POIs.
     */
    fun count(): Flow<Int> = repository.getUnviewedCount()

    /**
     * Refreshes the unviewed POIs from the API.
     */
    suspend fun refresh(): Result<Unit> = repository.refreshUnviewedPois()
}
