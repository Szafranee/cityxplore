package org.cityxplore.backend.discoveries.dto

import java.time.LocalDateTime
import java.util.UUID

/**
 * Lightweight DTO for exposing a user's POI discovery in API responses.
 * Intentionally omits internal identifiers like userId and database id.
 */
data class UserPoiDiscoveryDto(
    val poiId: UUID,
    val discoveredAt: LocalDateTime,
    val favorite: Boolean
)
