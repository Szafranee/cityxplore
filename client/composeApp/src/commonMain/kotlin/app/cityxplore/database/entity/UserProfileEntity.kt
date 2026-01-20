package app.cityxplore.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.cityxplore.database.currentTimeMillis
import app.cityxplore.profile.domain.UserProfile

/**
 * Room entity for caching user profile data locally.
 *
 * This entity serves as the single source of truth for user profile information,
 * allowing offline access to profile data.
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int,
    val achievementPoints: Int,
    val lastSyncedAt: Long
) {
    /**
     * Converts this entity to a domain model.
     */
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        username = username,
        email = email,
        avatarUrl = avatarUrl,
        totalDistance = totalDistance,
        totalPoisDiscovered = totalPoisDiscovered,
        achievementPoints = achievementPoints
    )

    companion object {
        /**
         * Creates an entity from a domain model.
         */
        fun fromDomain(profile: UserProfile): UserProfileEntity =
            UserProfileEntity(
                id = profile.id,
                username = profile.username,
                email = profile.email,
                avatarUrl = profile.avatarUrl,
                totalDistance = profile.totalDistance,
                totalPoisDiscovered = profile.totalPoisDiscovered,
                achievementPoints = profile.achievementPoints,
                lastSyncedAt = currentTimeMillis()
            )
    }
}
