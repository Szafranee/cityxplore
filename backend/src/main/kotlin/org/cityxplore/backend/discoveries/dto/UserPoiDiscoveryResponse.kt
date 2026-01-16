package org.cityxplore.backend.discoveries.dto

import org.cityxplore.backend.achievements.dto.AchievementResponse
import java.time.LocalDateTime
import java.util.UUID

/**
 * Lightweight response DTO for exposing a user's POI discovery in API responses.
 * Intentionally omits internal identifiers like userId and database id.
 */
data class UserPoiDiscoveryResponse(
    val poiId: UUID,
    val discoveredAt: LocalDateTime,
    val favorite: Boolean,
    val newlyUnlockedAchievements: List<AchievementResponse> = emptyList()
)
