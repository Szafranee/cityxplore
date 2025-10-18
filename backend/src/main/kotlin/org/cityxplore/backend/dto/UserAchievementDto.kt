package org.cityxplore.backend.dto

import java.time.LocalDateTime

data class UserAchievementDto(
    val achievement: AchievementDto,
    val achievedAt: LocalDateTime?,
    val progress: Map<String, Any?>?
)
