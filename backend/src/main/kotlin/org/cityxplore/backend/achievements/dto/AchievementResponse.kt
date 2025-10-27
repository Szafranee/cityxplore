package org.cityxplore.backend.achievements.dto

import java.util.UUID

/**
 * Response DTO representing an achievement definition.
 *
 * Used for public listing and admin responses. Request DTOs are separated
 * into [CreateAchievementRequest] and [UpdateAchievementRequest].
 */
data class AchievementResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val category: String?,
    val iconUrl: String?,
    val points: Int
)
