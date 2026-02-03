package app.cityxplore.achievements.domain

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing achievement data.
 *
 * Implements offline-first pattern:
 * - Reading: Flow from local Room database
 * - Refreshing: Network → local cache
 *
 * Provides access to achievement definitions and user-specific achievement progress.
 */
interface AchievementRepository {
    /**
     * Observes achievements for the current user from local database.
     * This is the primary way to get achievements - always returns cached data instantly.
     *
     * @return Flow of achievements that updates when data changes.
     */
    fun observeMyAchievements(): Flow<List<Achievement>>

    /**
     * Retrieves achievements for the currently authenticated user.
     * First tries local cache, then refreshes from network if needed.
     *
     * @return Result containing a list of achievements with the user's progress and unlock status.
     */
    suspend fun getMyAchievements(): Result<List<Achievement>>

    /**
     * Refreshes achievements from network and updates local cache.
     *
     * @return Result indicating success or failure.
     */
    suspend fun refreshMyAchievements(): Result<Unit>

    /**
     * Retrieves all available achievement definitions.
     *
     * @return Result containing a list of all achievement definitions in the system.
     */
    suspend fun getAllAchievements(): Result<List<Achievement>>

    /**
     * Retrieves achievements for a specific user.
     *
     * Allows viewing other users' achievements (requires authentication).
     *
     * @param userId The unique identifier of the user whose achievements to fetch.
     * @return Result containing a list of achievements for the specified user.
     */
    suspend fun getUserAchievements(userId: String): Result<List<Achievement>>

    /**
     * Clears local achievement cache (used on logout).
     */
    suspend fun clearLocalCache()
}
