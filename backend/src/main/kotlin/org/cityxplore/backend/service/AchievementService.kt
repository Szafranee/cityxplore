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
