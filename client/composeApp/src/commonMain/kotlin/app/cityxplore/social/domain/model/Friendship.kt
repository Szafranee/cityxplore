package app.cityxplore.social.domain.model

import kotlinx.datetime.LocalDateTime

data class Friendship(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: FriendshipStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // Helper fields for UI that might be enriched later, but for now strict to backend response + maybe display logic
    val otherUserId: String? = null, // Resolved ID of the other user in the friendship
    val otherUserAvatar: String? = null, // Will be populated if possible
    val otherUserName: String? = null
)
