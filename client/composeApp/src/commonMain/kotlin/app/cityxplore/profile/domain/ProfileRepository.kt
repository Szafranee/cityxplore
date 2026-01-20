package app.cityxplore.profile.domain

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations.
 *
 * Implements the offline-first pattern:
 * - Reading: Flow from a local database, with network refresh
 * - Writing: Optimistic local update and network sync
 *
 * @see app.cityxplore.profile.data.ProfileRepositoryImpl
 */
interface ProfileRepository {
    /**
     * Observes the current user's profile from a local database.
     * This is the primary way to get profile data - always returns cached data instantly.
     *
     * @return Flow of [UserProfile] that updates when data changes.
     */
    fun observeProfile(): Flow<UserProfile?>

    /**
     * Retrieves the current user's profile data.
     * First tries local cache, falls back to network if needed.
     *
     * @return [Result] containing the [UserProfile] on success, or exception on failure.
     */
    suspend fun getProfile(): Result<UserProfile>

    /**
     * Refreshes the profile from the network and updates local cache.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun refreshProfile(): Result<Unit>

    /**
     * Creates or updates a user profile with the specified username and avatar URL.
     *
     * @param username The desired username for the profile.
     * @param avatarUrl The optional URL to the user's avatar image.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun createProfile(username: String, avatarUrl: String?): Result<Unit>

    /**
     * Deletes the current user's account.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Uploads a user avatar to storage.
     *
     * @param imageBytes The raw bytes of the image to upload.
     * @return [Result] containing the public URL of the uploaded avatar on success.
     */
    suspend fun uploadAvatar(imageBytes: ByteArray): Result<String>

    /**
     * Initiates the email change flow.
     *
     * @param newEmail The new email address.
     * @return [Result] containing Unit on success.
     */
    suspend fun updateEmail(newEmail: String): Result<Unit>

    /**
     * Clears local profile cache (used on logout).
     */
    suspend fun clearLocalCache()
}
