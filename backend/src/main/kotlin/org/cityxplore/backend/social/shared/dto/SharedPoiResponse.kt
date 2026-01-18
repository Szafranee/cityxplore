package org.cityxplore.backend.social.shared.dto

import java.time.LocalDateTime
import java.util.UUID

/**
 * Response DTO representing a shared Point of Interest.
 *
 * Either `poiId` or `poiData` will be present, indicating whether this is
 * an existing POI or a custom POI.
 *
 * @property id unique identifier of the shared POI record
 * @property sharerId UUID of the user who shared the POI
 * @property recipientId UUID of the user receiving the shared POI
 * @property poiId optional UUID of an existing Point of Interest being shared
 * @property poiData optional custom POI data if sharing a custom POI
 * @property message optional message accompanying the shared POI
 * @property sharedAt timestamp when the POI was shared
 * @property viewedAt timestamp when the recipient viewed the shared POI, null if not yet viewed
 * @property discoveredAt timestamp when the recipient discovered the shared POI location, null if not yet discovered
 * @property sharerName display name of the user who shared the POI
 * @property sharerAvatar avatar URL of the user who shared the POI
 * @property recipientName display name of the recipient user
 * @property recipientAvatar avatar URL of the recipient user
 */
data class SharedPoiResponse(
    val id: UUID,
    val sharerId: UUID,
    val recipientId: UUID,
    val poiId: UUID?,
    val poiData: CustomPoiData?,
    val message: String?,
    val sharedAt: LocalDateTime,
    val viewedAt: LocalDateTime?,
    val discoveredAt: LocalDateTime? = null,
    val sharerName: String? = null,
    val sharerAvatar: String? = null,
    val recipientName: String? = null,
    val recipientAvatar: String? = null
)
