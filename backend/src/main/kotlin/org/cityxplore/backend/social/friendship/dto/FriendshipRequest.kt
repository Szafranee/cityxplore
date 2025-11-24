package org.cityxplore.backend.social.friendship.dto

import java.util.UUID

/**
 * Request payload to invite another user to become friends.
 */
data class FriendshipRequest(
    val addresseeId: UUID
)
