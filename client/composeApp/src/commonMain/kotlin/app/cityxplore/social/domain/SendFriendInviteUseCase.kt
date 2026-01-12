package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.repository.SocialRepository

class SendFriendInviteUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(username: String): Result<Friendship> =
        repository.sendFriendInvite(username)
}
