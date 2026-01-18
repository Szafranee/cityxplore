package app.cityxplore.social.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request DTO for sharing a Point of Interest with another user.
 * Either poiId (for existing POIs) or customPoi (for custom POIs) must be provided, but not both.
 */
@Serializable
data class SharePoiRequestDto(
    val recipientId: String,
    val poiId: String? = null,
    val customPoi: CustomPoiDataDto? = null,
    val message: String? = null
)
