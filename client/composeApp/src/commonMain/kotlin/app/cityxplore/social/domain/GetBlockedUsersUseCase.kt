package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving the list of blocked users.
 */
class GetBlockedUsersUseCase(private val repository: SocialRepository) {
    operator fun invoke(): Flow<List<Friendship>> = repository.getBlockedUsers()

    suspend fun refresh(): Result<Unit> = repository.refreshBlockedUsers()
}
