package app.cityxplore.social.domain.model

import kotlinx.datetime.LocalDateTime

data class Friendship(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: FriendshipStatus,
    val blockedBy: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val otherUserId: String? = null, // Resolved ID of the other user in the friendship
    val otherUserAvatar: String? = null,
    val otherUserName: String? = null
)
