package app.cityxplore.profile.data

import app.cityxplore.profile.domain.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int,
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
