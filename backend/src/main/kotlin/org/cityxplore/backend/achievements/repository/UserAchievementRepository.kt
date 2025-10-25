package org.cityxplore.backend.achievements.repository

import org.cityxplore.backend.achievements.entity.UserAchievement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    fun findAllByUserId(userId: UUID): List<UserAchievement>

    fun existsByUserIdAndAchievementId(userId: UUID, achievementId: UUID): Boolean

    fun findByUserIdAndAchievementId(userId: UUID, achievementId: UUID): UserAchievement?

    @Transactional
    @Modifying
    @Query(
        value = """
        INSERT INTO user_achievements (user_id, achievement_id, achieved_at, progress_data)
        VALUES (:userId, :achievementId, :achievedAt, CAST(:progressData AS jsonb))
        ON CONFLICT (user_id, achievement_id) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertIgnore(
        userId: UUID,
        achievementId: UUID,
        achievedAt: LocalDateTime,
        progressData: String?
    ): Int
}
