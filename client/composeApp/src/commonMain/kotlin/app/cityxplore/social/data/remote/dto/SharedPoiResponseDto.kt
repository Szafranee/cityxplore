package app.cityxplore.social.data.remote.dto

import app.cityxplore.social.domain.model.SharedPoi
import kotlinx.serialization.Serializable

/**
 * Response DTO representing a shared Point of Interest.
 * Either poiId or poiData will be present, indicating whether this is
 * an existing POI or a custom POI.
 */
@Serializable
data class SharedPoiResponseDto(
    val id: String,
    val sharerId: String,
    val recipientId: String,
    val poiId: String? = null,
    val poiData: CustomPoiDataDto? = null,
    val message: String? = null,
    val sharedAt: String,
    val viewedAt: String? = null,
    val discoveredAt: String? = null,
    val sharerName: String? = null,
    val sharerAvatar: String? = null,
    val recipientName: String? = null,
    val recipientAvatar: String? = null
) {
    /**
     * Maps this DTO to a domain model.
     */
    fun toDomain(): SharedPoi = SharedPoi(
        id = id,
        sharerId = sharerId,
        recipientId = recipientId,
        poiId = poiId,
        customPoi = poiData?.toDomain(),
        message = message,
        sharedAt = sharedAt,
        viewedAt = viewedAt,
        discoveredAt = discoveredAt,
        isCustomPoi = poiData != null,
        sharerName = sharerName,
        sharerAvatar = sharerAvatar,
        recipientName = recipientName,
        recipientAvatar = recipientAvatar
    )
}
