package app.cityxplore.social.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipDto(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
