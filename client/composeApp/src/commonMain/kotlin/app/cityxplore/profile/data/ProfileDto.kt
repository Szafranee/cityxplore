package app.cityxplore.profile.data

import app.cityxplore.profile.domain.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String? = null,
    val totalDistance: Double = 0.0,
    val totalPoisDiscovered: Int = 0,
    val totalAchievementPoints: Int = 0
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        email = email,
        username = username,
        avatarUrl = avatarUrl,
        totalDistance = totalDistance,
        totalPoisDiscovered = totalPoisDiscovered,
        achievementPoints = totalAchievementPoints
    )
}
