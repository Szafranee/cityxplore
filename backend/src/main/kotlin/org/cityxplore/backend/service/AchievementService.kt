package org.cityxplore.backend.service

import org.cityxplore.backend.dto.AchievementDto
import org.cityxplore.backend.dto.UserAchievementDto
import org.cityxplore.backend.entity.Achievement
import org.cityxplore.backend.entity.UserAchievement
import org.cityxplore.backend.repository.AchievementRepository
import org.cityxplore.backend.repository.UserAchievementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@Service
class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository
) {

    data class AchievementGrantResult(
        val dto: UserAchievementDto,
        val created: Boolean
    )

    // Centralized mapper to avoid duplication
    private fun Achievement.toDto() = AchievementDto(
        id = id!!,
        name = name,
        description = description,
        category = category,
        iconUrl = iconUrl,
        points = points
    )

    private fun toDto(achievement: Achievement, userAchievement: UserAchievement): UserAchievementDto =
        UserAchievementDto(
            achievement = achievement.toDto(),
            achievedAt = userAchievement.achievedAt,
            progress = userAchievement.progressData
        )

    /**
     * Retrieve all active achievements.
     */
    @Transactional(readOnly = true)
    fun getAllAchievements(): List<AchievementDto> =
        achievementRepository.findAll()
            .filter { it.isActive }
            .map { it.toDto() }

    /**
     * Grants the specified achievement to the given user (idempotent).
     *
     * If the user already has the achievement, returns the existing record with created=false.
     * Otherwise, creates it and returns created=true.
     */
    @Transactional
    fun grantAchievement(userId: UUID, achievementId: UUID): AchievementGrantResult {
        // Validate target achievement before attempting to insert
        val achievement = achievementRepository.findByIdOrNull(achievementId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found")
        if (!achievement.isActive) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not available")
        }

        // DB-level idempotent insert; returns 1 if inserted, 0 if existed
        val inserted = userAchievementRepository.insertIgnore(
            userId = userId,
            achievementId = achievementId,
            achievedAt = LocalDateTime.now(),
            progressData = null
        )

        // Fetch the record (existing or just created)
        val userAchievement: UserAchievement =
            userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
                ?: throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UserAchievement not found after upsert"
                )

        return AchievementGrantResult(
            dto = toDto(achievement, userAchievement),
            created = inserted == 1
        )
    }

    /**
     * Fetches the achievements a user has earned and returns them as DTOs.
     *
     * Only user achievement records that correspond to an existing achievement definition are included.
     * Each returned entry contains the achievement details, the timestamp when it was achieved, and optional progress
     * mapped under the "progress" key.
     */
    @Transactional(readOnly = true)
    fun getUserAchievements(userId: UUID): List<UserAchievementDto> {
        val userAchievements = userAchievementRepository.findAllByUserId(userId)
        val ids = userAchievements.map { it.achievementId }.toSet()
        val achievements = achievementRepository.findAllById(ids).associateBy { it.id }

        return userAchievements.mapNotNull { ua ->
            achievements[ua.achievementId]?.let { ach -> toDto(ach, ua) }
        }
    }

    /**
     * Retrieves the achievement details for a specific user and achievement.
     *
     * This method fetches a user's achievement record and its corresponding achievement definition.
     * Returns null if either the user's achievement record or the achievement definition is not found.
     *
     * @param userId The unique identifier of the user.
     * @param achievementId The unique identifier of the achievement.
     * @return A `UserAchievementDto` representing the user's achievement details including the achievement metadata,
     *         time of achievement, and progress data, or null if the record doesn't exist.
     */
    @Transactional(readOnly = true)
    fun getUserAchievement(userId: UUID, achievementId: UUID): UserAchievementDto? {
        val userAchievement = userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
            ?: return null
        val achievement = achievementRepository.findByIdOrNull(userAchievement.achievementId)
            ?: return null
        return toDto(achievement, userAchievement)
    }
}
