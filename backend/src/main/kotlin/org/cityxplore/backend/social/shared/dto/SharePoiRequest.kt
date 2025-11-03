package org.cityxplore.backend.social.shared.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Request DTO for sharing a Point of Interest with another user.
 *
 * Either `poiId` (for existing POIs) or `customPoi` (for custom POIs) must be provided, but not both.
 *
 * @property recipientId the UUID of the user receiving the shared POI
 * @property poiId optional UUID of an existing Point of Interest being shared
 * @property customPoi optional custom POI data for sharing a non-existing POI
 * @property message optional message accompanying the shared POI
 */
data class SharePoiRequest(
    val recipientId: UUID,

    val poiId: UUID? = null,

    @field:Valid
    val customPoi: CustomPoiData? = null,

    @field:Size(max = 500, message = "Message cannot exceed 500 characters")
    val message: String? = null
)
