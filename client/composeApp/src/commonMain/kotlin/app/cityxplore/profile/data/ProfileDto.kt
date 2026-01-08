package app.cityxplore.profile.data

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int,
    val totalAchievementPoints: Int
)
