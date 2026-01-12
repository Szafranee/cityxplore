package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.repository.SocialRepository

class RespondToFriendInviteUseCase(private val repository: SocialRepository) {
    suspend fun accept(friendshipId: String): Result<Friendship> =
        repository.acceptFriendInvite(friendshipId)

    suspend fun decline(friendshipId: String): Result<Friendship> =
        repository.declineFriendInvite(friendshipId)
}
