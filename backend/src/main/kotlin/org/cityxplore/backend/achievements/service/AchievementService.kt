package org.cityxplore.backend.achievements.service

import org.cityxplore.backend.achievements.dto.AchievementResponse
import org.cityxplore.backend.achievements.dto.UserAchievementResponse
import org.cityxplore.backend.achievements.entity.UserAchievement
import org.cityxplore.backend.achievements.mapper.toDto
import org.cityxplore.backend.achievements.mapper.toUserAchievementDto
import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.user.repository.UserRepository
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
    private val userAchievementRepository: UserAchievementRepository,
    private val userRepository: UserRepository
) {

    data class AchievementGrantResult(
        val dto: UserAchievementResponse,
        val created: Boolean
    )

    /**
     * Retrieve all active achievements.
     */
    @Transactional(readOnly = true)
    fun getAllAchievements(): List<AchievementResponse> =
        achievementRepository.findAllByIsActiveTrue()
            .map { it.toDto() }

    /**
     * Grants the specified achievement to the given user (idempotent).
     *
     * If the user already has the achievement, returns the existing record with created=false.
     * Otherwise, creates it and returns created=true.
     * After a successful grant (new achievement), increments the user's totalAchievementPoints counter.
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

        val dto = toUserAchievementDto(achievement, userAchievement)

        // Increment user's total achievement points ONLY if this is a new achievement
        if (inserted == 1) {
            val user = userRepository.findById(userId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
            user.totalAchievementPoints = user.totalAchievementPoints + achievement.points
            userRepository.save(user)
        }

        return AchievementGrantResult(
            dto = dto,
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
    fun getUserAchievements(userId: UUID): List<UserAchievementResponse> {
        val userAchievements = userAchievementRepository.findAllByUserId(userId)
        val ids = userAchievements.map { it.achievementId }.toSet()
        val achievements = achievementRepository.findAllById(ids).associateBy { it.id }

        val result = userAchievements.mapNotNull { ua ->
            achievements[ua.achievementId]?.let { ach -> toUserAchievementDto(ach, ua) }
        }

        return result
    }

    /**
     * Retrieves the achievement details for a specific user and achievement.
     *
     * This method fetches a user's achievement record and its corresponding achievement definition.
     * Returns null if either the user's achievement record or the achievement definition is not found.
     *
     * @param userId The unique identifier of the user.
     * @param achievementId The unique identifier of the achievement.
     * @return A `UserAchievementResponse` representing the user's achievement details including the achievement metadata,
     *         time of achievement, and progress data, or null if the record doesn't exist.
     */
    @Transactional(readOnly = true)
    fun getUserAchievement(userId: UUID, achievementId: UUID): UserAchievementResponse? {
        val userAchievement = userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
            ?: return null
        val achievement = achievementRepository.findByIdOrNull(userAchievement.achievementId)
            ?: return null
        val dto = toUserAchievementDto(achievement, userAchievement)

        return dto
    }
}
