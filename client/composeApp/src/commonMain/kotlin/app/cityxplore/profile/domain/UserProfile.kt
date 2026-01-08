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
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int,
    val achievementPoints: Int
) {
    // Level calculation: 1 level per 100 points
    val level: Int
        get() = achievementPoints / 100

    val nextLevelPoints: Int
        get() = (level + 1) * 100

    val trainingProgress: Float
        get() {
            val currentLevelBase = level * 100
            val pointsInLevel = achievementPoints - currentLevelBase
            val pointsNeededForLevel = 100 // Since every level is 100 points wide
            return pointsInLevel.toFloat() / pointsNeededForLevel.toFloat()
        }
}
