package app.cityxplore.profile.domain

import kotlinx.serialization.Serializable

interface ProfileRepository {
    suspend fun createProfile(username: String, avatarUrl: String?): Result<Unit>
}

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int
)
