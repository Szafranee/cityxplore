package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow

class GetFriendsUseCase(private val repository: SocialRepository) {
    operator fun invoke(): Flow<List<Friendship>> = repository.getFriends()

    suspend fun refresh(): Result<Unit> = repository.refreshFriends()
}
