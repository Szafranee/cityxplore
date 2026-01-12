package app.cityxplore.social.domain

import app.cityxplore.social.domain.repository.SocialRepository

class ManageFriendshipUseCase(private val repository: SocialRepository) {
    suspend fun deleteFriend(friendshipId: String): Result<Unit> = repository.deleteFriend(friendshipId)
    suspend fun blockFriend(friendshipId: String): Result<Unit> = repository.blockFriend(friendshipId)
    suspend fun unblockFriend(friendshipId: String): Result<Unit> = repository.unblockFriend(friendshipId)
}
