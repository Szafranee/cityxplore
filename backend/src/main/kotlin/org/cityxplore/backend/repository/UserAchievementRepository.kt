package org.cityxplore.backend.repository

import org.cityxplore.backend.entity.UserAchievement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    fun findAllByUserId(userId: UUID): List<UserAchievement>

    fun existsByUserIdAndAchievementId(userId: UUID, achievementId: UUID): Boolean

    fun findByUserIdAndAchievementId(userId: UUID, achievementId: UUID): UserAchievement?
}