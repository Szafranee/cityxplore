package org.cityxplore.backend.service

import org.cityxplore.backend.dto.AchievementDto
import org.cityxplore.backend.dto.UserAchievementDto
import org.cityxplore.backend.entity.UserAchievement
import org.cityxplore.backend.repository.AchievementRepository
import org.cityxplore.backend.repository.UserAchievementRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
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

    /**
     * Retrieve all active achievements.
     */
    @Transactional(readOnly = true)
    fun getAllAchievements(): List<AchievementDto> =
        achievementRepository.findAll()
            .filter { it.isActive }
            .map {
                AchievementDto(
                    id = it.id!!,
                    name = it.name,
                    description = it.description,
                    category = it.category,
                    iconUrl = it.iconUrl,
                    points = it.points
                )
            }

    /**
     * Grants the specified achievement to the given user (idempotent).
     *
     * If the user already has the achievement, returns the existing record with created=false.
     * Otherwise creates it and returns created=true.
     */
    @Transactional
    fun grantAchievement(userId: UUID, achievementId: UUID): AchievementGrantResult {
        // Fast-path: already granted -> return existing as 200 OK
        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            val existing = userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
                ?: throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UserAchievement exists but not found"
                )
            val achievement = achievementRepository.findByIdOrNull(achievementId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found")
            return AchievementGrantResult(dto = toDto(achievement, existing), created = false)
        }

        // Validate target achievement before attempting to create
        val achievement = achievementRepository.findByIdOrNull(achievementId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found")
        if (!achievement.isActive) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not available")
        }

        // Try to create; handle race with unique constraint
        return try {
            val saved = userAchievementRepository.save(
                UserAchievement(userId = userId, achievementId = achievementId)
            )
            AchievementGrantResult(
                dto = toDto(achievement, saved),
                created = true
            )
        } catch (ex: DataIntegrityViolationException) {
            val existing = userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
                ?: throw ex
            AchievementGrantResult(
                dto = toDto(achievement, existing),
                created = false
            )
        }
    }

    private fun toDto(achievement: org.cityxplore.backend.entity.Achievement, ua: UserAchievement): UserAchievementDto =
        UserAchievementDto(
            achievement = AchievementDto(
                id = achievement.id!!,
                name = achievement.name,
                description = achievement.description,
                category = achievement.category,
                iconUrl = achievement.iconUrl,
                points = achievement.points
            ),
            achievedAt = ua.achievedAt,
            progress = ua.progressData?.let { mapOf("data" to it) }
        )

    /**
     * Fetches the achievements a user has earned and returns them as DTOs.
     *
     * Only user achievement records that correspond to an existing achievement definition are included.
     * Each returned entry contains the achievement details, the timestamp when it was achieved, and optional progress mapped under the "data" key.
     */
    @Transactional(readOnly = true)
    fun getUserAchievements(userId: UUID): List<UserAchievementDto> {
        val achievements = achievementRepository.findAll().associateBy { it.id }

        return userAchievementRepository.findAllByUserId(userId).mapNotNull { userAchievement ->
            achievements[userAchievement.achievementId]?.let { achievement ->
                UserAchievementDto(
                    achievement = AchievementDto(
                        id = achievement.id!!,
                        name = achievement.name,
                        description = achievement.description,
                        category = achievement.category,
                        iconUrl = achievement.iconUrl,
                        points = achievement.points
                    ),
                    achievedAt = userAchievement.achievedAt,
                    progress = userAchievement.progressData?.let { mapOf("data" to it) }
                )
            }
        }
    }
}
