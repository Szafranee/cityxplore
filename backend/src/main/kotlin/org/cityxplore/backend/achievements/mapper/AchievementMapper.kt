package org.cityxplore.backend.achievements.mapper

import org.cityxplore.backend.achievements.dto.AchievementResponse
import org.cityxplore.backend.achievements.dto.CreateAchievementRequest
import org.cityxplore.backend.achievements.dto.UpdateAchievementRequest
import org.cityxplore.backend.achievements.dto.UserAchievementResponse
import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.achievements.entity.UserAchievement

/**
 * Centralised mappers for Achievement domain to keep controllers and services lean.
 */
fun Achievement.toDto(): AchievementResponse = AchievementResponse(
    id = id!!,
    name = name,
    description = description,
    category = category,
    iconUrl = iconUrl,
    points = points
)

fun CreateAchievementRequest.toEntity(): Achievement = Achievement(
    name = name,
    description = description,
    category = category,
    criteria = emptyMap(),
    iconUrl = iconUrl,
    points = points,
    isActive = true
)

fun UpdateAchievementRequest.applyTo(existing: Achievement): Achievement = existing.copy(
    name = name,
    description = description,
    category = category,
    iconUrl = iconUrl,
    points = points
)

fun toUserAchievementDto(
    achievement: Achievement,
    userAchievement: UserAchievement?
): UserAchievementResponse =
    UserAchievementResponse(
        achievement = achievement.toDto(),
        achievedAt = userAchievement?.achievedAt,
        progress = userAchievement?.progressData
    )
