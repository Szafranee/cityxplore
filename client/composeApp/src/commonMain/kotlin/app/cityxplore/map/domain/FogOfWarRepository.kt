package app.cityxplore.map.domain

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Fog of War state (revealed hexagons).
 *
 * Implements the offline-first pattern:
 * - Reading: Always returns Flow from local database
 * - Writing: Saves locally first, then syncs to backend
 * - Offline: Queues operations for later sync
 */
interface FogOfWarRepository {
    /**
     * Fetches all hexagons covering the Warsaw metropolitan area.
     * This list is static and pre-computed on the backend.
     *
     * Should be cached locally after the first fetch.
     *
     * @return Result containing a set of all Warsaw hex indices, or error.
     */
    suspend fun getWarsawHexagons(): Result<Set<String>>

    /**
     * Observes all revealed hexagon indices for the current user.
     * This is the primary way to get revealed hexagons - always returns from local DB.
     *
     * @return Flow of revealed hex indices that updates when data changes.
     */
    fun observeRevealedHexagons(): Flow<Set<String>>

    /**
     * Fetches all revealed hexagon indices for the current user.
     * Used for one-time reads when Flow is not needed.
     *
     * @return Result containing a set of H3 hex indices, or error.
     */
    suspend fun getRevealedHexagons(): Result<Set<String>>

    /**
     * Refreshes revealed hexagons from the server and stores them locally.
     * This merges server data with any locally revealed hexagons.
     *
     * @return Result indicating success or failure.
     */
    suspend fun refreshRevealedHexagons(): Result<Unit>

    /**
     * Marks the specified hexagons as revealed.
     * - Always saves locally first (optimistic update)
     * - Syncs to backend if online
     * - Queues for later sync if offline
     *
     * @param hexIndices Set of H3 hex indices to mark as revealed.
     * @return Result indicating success or failure.
     */
    suspend fun revealHexagons(hexIndices: Set<String>): Result<Unit>

    /**
     * Clears all revealed hexagons (for testing/debug purposes).
     *
     * @return Result indicating success or failure.
     */
    suspend fun clearAllRevealed(): Result<Unit>

    /**
     * Clears local cache (used on logout).
     */
    suspend fun clearLocalCache()
}
