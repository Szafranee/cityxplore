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
    private val userRepository: UserRepository,
    private val achievementProgressService: AchievementProgressService
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
            val updated = userRepository.incrementAchievementPoints(userId, achievement.points)
            if (updated == 0) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            }
        }

        return AchievementGrantResult(
            dto = dto,
            created = inserted == 1
        )
    }

    /**
     * Fetches ALL achievements with the user's progress.
     *
     * Returns all active achievements. For achievements the user has earned,
     * includes the achievement timestamp. For achievements the user hasn't
     * started or is in progress, calculates current progress dynamically.
     *
     * This allows the client to display all achievements (locked/unlocked) in the profile
     * with accurate progress information (e.g. "42/50 POIs discovered").
     */
    @Transactional(readOnly = true)
    fun getUserAchievements(userId: UUID): List<UserAchievementResponse> {
        // Get all active achievements
        val allAchievements = achievementRepository.findAllByIsActiveTrue()

        // Get user's progress/unlocks
        val userAchievements = userAchievementRepository.findAllByUserId(userId)
        val userAchievementsMap = userAchievements.associateBy { it.achievementId }

        // Map all achievements with the user's progress
        return allAchievements.map { achievement ->
            val userAchievement = userAchievementsMap[achievement.id]

            // For unlocked achievements, use stored data; for locked, calculate current progress
            val progressData = if (userAchievement?.achievedAt != null) {
                // Achievement already unlocked - return completed status
                mapOf("completed" to true)
            } else {
                // Calculate current progress dynamically
                achievementProgressService.calculateProgress(userId, achievement)
            }

            toUserAchievementDto(achievement, userAchievement, progressData)
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
