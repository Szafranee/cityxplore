package org.cityxplore.backend.social.friendship.dto

import org.cityxplore.backend.social.friendship.entity.FriendshipStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * Response representation of a friendship relation or invite.
 */
data class FriendshipResponse(
    val id: UUID,
    val requesterId: UUID,
    val addresseeId: UUID,
    val status: FriendshipStatus,
    val blockedBy: UUID? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
