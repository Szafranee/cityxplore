package app.cityxplore.profile.domain

import kotlinx.serialization.Serializable

/**
 * Repository interface for user profile operations.
 *
 * This repository handles user profile management, including creation,
 * retrieval, and updates of user profile data from the backend.
 *
 * @see app.cityxplore.profile.data.ProfileRepositoryImpl
 */
interface ProfileRepository {
    /**
     * Creates or updates a user profile with the specified username and avatar URL.
     *
     * @param username The desired username for the profile.
     * @param avatarUrl The optional URL to the user's avatar image.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun createProfile(username: String, avatarUrl: String?): Result<Unit>

    /**
     * Retrieves the current user's profile data from the backend.
     *
     * @return [Result] containing the [UserProfile] on success, or exception on failure.
     */
    suspend fun getProfile(): Result<UserProfile>
}

/**
 * Represents a user's profile data, including statistics and personal information.
 *
 * @property id The unique identifier of the user.
 * @property username The user's chosen username.
 * @property avatarUrl The URL to the user's avatar image, or `null` if not set.
 * @property totalDistance The cumulative distance travelled by the user in meters.
 * @property totalPoisDiscovered The total number of Points of Interest discovered by the user.
 */
@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int
)
