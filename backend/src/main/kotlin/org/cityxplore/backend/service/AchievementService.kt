package org.cityxplore.backend.service

import org.cityxplore.backend.dto.AchievementDto
import org.cityxplore.backend.dto.UserAchievementDto
import org.cityxplore.backend.entity.UserAchievement
import org.cityxplore.backend.repository.AchievementRepository
import org.cityxplore.backend.repository.UserAchievementRepository
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
    /**
             * Retrieve all active achievements.
             *
             * @return A list of AchievementDto representing achievements with `isActive == true`.
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
     * Grants the specified achievement to the given user.
     *
     * Attempts to create a new UserAchievement linking the user and achievement, then returns a DTO
     * representing the granted achievement and the timestamp when it was recorded.
     *
     * @param userId UUID of the user to receive the achievement.
     * @param achievementId UUID of the achievement to grant.
     * @return A UserAchievementDto containing the granted AchievementDto, the `achievedAt` timestamp, and `progress` set to `null`.
     * @throws org.springframework.web.server.ResponseStatusException with HTTP 404 if the achievement does not exist.
     * @throws org.springframework.web.server.ResponseStatusException with HTTP 409 if the user already has the achievement.
     */
    @Transactional
    fun grantAchievement(userId: UUID, achievementId: UUID): UserAchievementDto {
        if (!achievementRepository.existsById(achievementId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found")
        }

        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already achieved")
        }

        val userAchievement = userAchievementRepository.save(
            UserAchievement(userId = userId, achievementId = achievementId)
        )
        val achievement = achievementRepository.findById(achievementId).get()

        return UserAchievementDto(
            achievement = AchievementDto(
                id = achievement.id!!,
                name = achievement.name,
                description = achievement.description,
                category = achievement.category,
                iconUrl = achievement.iconUrl,
                points = achievement.points
            ),
            achievedAt = userAchievement.achievedAt,
            progress = null
        )
    }

    /**
     * Fetches the achievements a user has earned and returns them as DTOs.
     *
     * Only user achievement records that correspond to an existing achievement definition are included.
     * Each returned entry contains the achievement details, the timestamp when it was achieved, and optional progress mapped under the "data" key.
     *
     * @param userId The UUID of the user whose achievements are requested.
     * @return A list of UserAchievementDto objects representing the user's earned achievements.
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