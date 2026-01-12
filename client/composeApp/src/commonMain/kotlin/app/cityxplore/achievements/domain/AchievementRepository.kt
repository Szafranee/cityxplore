package app.cityxplore.achievements.domain

/**
 * Repository interface for managing achievement data.
 *
 * Provides access to achievement definitions and user-specific achievement progress.
 * Implementations should handle data fetching from remote sources and local caching.
 */
interface AchievementRepository {
    /**
     * Retrieves achievements for the currently authenticated user.
     *
     * @return Result containing a list of achievements with the user's progress and unlock status.
     */
    suspend fun getMyAchievements(): Result<List<Achievement>>

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
}
