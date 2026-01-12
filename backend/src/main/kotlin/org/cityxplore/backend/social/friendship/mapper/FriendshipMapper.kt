package org.cityxplore.backend.social.friendship.mapper

import org.cityxplore.backend.social.friendship.dto.FriendshipResponse
import org.cityxplore.backend.social.friendship.entity.Friendship

/**
 * Mapper responsible for converting Friendship domain entities to API responses.
 *
 * Keep mapping concerns outside services/controllers to keep business logic clean and reusable.
 */
object FriendshipMapper {

    /** Maps a Friendship entity to a FriendshipResponse DTO. */
    fun toResponse(entity: Friendship): FriendshipResponse = FriendshipResponse(
        id = entity.id
            ?: throw IllegalArgumentException("Friendship entity must be persisted before mapping to response"),
        requesterId = entity.requesterId,
        addresseeId = entity.addresseeId,
        status = entity.status,
        blockedBy = entity.blockedBy,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )
}
