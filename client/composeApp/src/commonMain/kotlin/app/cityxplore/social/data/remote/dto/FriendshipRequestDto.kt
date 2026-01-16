package app.cityxplore.social.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipRequestDto(
    val addresseeId: String
)
