package app.cityxplore.profile.domain

import app.cityxplore.achievements.domain.Achievement

/**
 * Repository interface for syncing distance to the backend.
 *
 * Handles distance updates and returns newly unlocked achievements.
 */
interface DistanceSyncRepository {
    /**
     * Syncs accumulated distance to the backend.
     *
     * @param distanceMeters The distance to sync in meters.
     * @return Result containing list of newly unlocked achievements, or failure if sync failed.
     */
    suspend fun syncDistance(distanceMeters: Double): Result<DistanceSyncResult>
}

/**
 * Result of a distance sync operation.
 *
 * @property newlyUnlockedAchievements Achievements unlocked as a result of this distance update.
 */
data class DistanceSyncResult(
    val newlyUnlockedAchievements: List<Achievement>
)
