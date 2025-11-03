package org.cityxplore.backend.social.shared.mapper

import org.cityxplore.backend.social.shared.dto.SharedPoiResponse
import org.cityxplore.backend.social.shared.entity.SharedPoi

/**
 * Mapper object responsible for converting SharedPoi entities to DTOs.
 */
object SharedPoiMapper {

    /**
     * Converts a SharedPoi entity to a SharedPoiResponse DTO.
     *
     * @param sharedPoi the SharedPoi entity to convert
     * @return the corresponding SharedPoiResponse DTO
     * @throws NullPointerException if the entity ID is null
     */
    fun toResponse(sharedPoi: SharedPoi): SharedPoiResponse =
        SharedPoiResponse(
            id = sharedPoi.id!!,
            sharerId = sharedPoi.sharerId,
            recipientId = sharedPoi.recipientId,
            poiId = sharedPoi.poiId,
            poiData = sharedPoi.poiData,
            message = sharedPoi.message,
            sharedAt = sharedPoi.sharedAt,
            viewedAt = sharedPoi.viewedAt
        )
}
