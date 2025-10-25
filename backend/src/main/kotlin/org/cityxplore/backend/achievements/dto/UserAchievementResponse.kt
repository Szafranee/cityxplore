package org.cityxplore.backend.achievements.dto

import java.time.LocalDateTime

/**
 * Response DTO that pairs an achievement with the user's progress/state.
 */
data class UserAchievementResponse(
    val achievement: AchievementResponse,
    val achievedAt: LocalDateTime?,
    val progress: Map<String, Any?>?
)
