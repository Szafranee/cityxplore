package app.cityxplore.profile.domain

import kotlinx.serialization.Serializable

/**
 * Represents a user's profile data, including statistics and personal information.
 *
 * @property id The unique identifier of the user.
 * @property username The user's chosen username.
 * @property avatarUrl The URL to the user's avatar image, or `null` if not set.
 * @property totalDistance The cumulative distance travelled by the user in meters.
 * @property totalPoisDiscovered The total number of Points of Interest discovered by the user.
 * @property achievementPoints The total achievement points earned.
 */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int,
    val achievementPoints: Int
) {
    /**
     * Calculates the current level and progress within that level based on [achievementPoints].
     * Scaling: Level 0->1 needs 100 pts. Every subsequent level needs +25 pts more than the previous.
     */
    private val levelInfo: LevelInfo
        get() {
            var level = 0
            var cost = 100
            var remaining = achievementPoints

            while (remaining >= cost) {
                remaining -= cost
                level++
                cost += 25
            }
            return LevelInfo(level, remaining, cost)
        }

    val level: Int
        get() = levelInfo.level

    val xpInCurrentLevel: Int
        get() = levelInfo.currentXP

    val xpNeededForNextLevel: Int
        get() = levelInfo.neededXP

    val levelProgress: Float
        get() = if (xpNeededForNextLevel > 0) xpInCurrentLevel.toFloat() / xpNeededForNextLevel.toFloat() else 0f

    private data class LevelInfo(val level: Int, val currentXP: Int, val neededXP: Int)
}
