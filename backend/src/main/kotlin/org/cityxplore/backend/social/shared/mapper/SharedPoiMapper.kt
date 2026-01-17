package org.cityxplore.backend.social.shared.mapper

import org.cityxplore.backend.social.shared.dto.SharedPoiResponse
import org.cityxplore.backend.social.shared.entity.SharedPoi
import org.cityxplore.backend.user.entity.User

/**
 * Mapper object responsible for converting SharedPoi entities to DTOs.
 */
object SharedPoiMapper {

    /**
     * Converts a SharedPoi entity to a SharedPoiResponse DTO.
     *
     * @param sharedPoi the SharedPoi entity to convert
     * @param sharer optional User entity of the sharer for name/avatar
     * @param recipient optional User entity of the recipient for name/avatar
     * @return the corresponding SharedPoiResponse DTO
     * @throws NullPointerException if the entity ID is null
     */
    fun toResponse(
        sharedPoi: SharedPoi,
        sharer: User? = null,
        recipient: User? = null
    ): SharedPoiResponse =
        SharedPoiResponse(
            id = sharedPoi.id!!,
            sharerId = sharedPoi.sharerId,
            recipientId = sharedPoi.recipientId,
            poiId = sharedPoi.poiId,
            poiData = sharedPoi.poiData,
            message = sharedPoi.message,
            sharedAt = sharedPoi.sharedAt,
            viewedAt = sharedPoi.viewedAt,
            sharerName = sharer?.username,
            sharerAvatar = sharer?.avatarUrl,
            recipientName = recipient?.username,
            recipientAvatar = recipient?.avatarUrl
        )
}
