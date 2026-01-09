package app.cityxplore.profile.domain

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

    /**
     * Deletes the current user's account.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun deleteAccount(): Result<Unit>
}
