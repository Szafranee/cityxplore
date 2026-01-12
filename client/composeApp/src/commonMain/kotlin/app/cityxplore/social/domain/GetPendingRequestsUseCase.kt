package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow

class GetPendingRequestsUseCase(private val repository: SocialRepository) {
    operator fun invoke(): Flow<List<Friendship>> = repository.getPendingRequests()

    suspend fun refresh(): Result<Unit> = repository.refreshPendingRequests()
}
