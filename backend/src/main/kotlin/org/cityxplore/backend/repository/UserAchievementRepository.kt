package org.cityxplore.backend.repository

import org.cityxplore.backend.entity.UserAchievement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    /**
 * Retrieves all UserAchievement records for the specified user.
 *
 * @param userId UUID of the user whose achievements should be returned.
 * @return A list of UserAchievement entities associated with the given userId; an empty list if none exist.
 */
fun findAllByUserId(userId: UUID): List<UserAchievement>
    /**
 * Checks whether an association between the specified user and achievement exists.
 *
 * @param userId The UUID of the user.
 * @param achievementId The UUID of the achievement.
 * @return `true` if a UserAchievement with the given userId and achievementId exists, `false` otherwise.
 */
fun existsByUserIdAndAchievementId(userId: UUID, achievementId: UUID): Boolean
}