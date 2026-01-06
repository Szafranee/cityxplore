package app.cityxplore.map.domain

/**
 * Repository for managing Fog of War state (revealed hexagons).
 *
 * Handles persistence and synchronisation of explored areas.
 * The implementation should sync with the backend for cross-device persistence.
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
     * Fetches all revealed hexagon indices for the current user.
     *
     * @return Result containing set of H3 hex indices, or error.
     */
    suspend fun getRevealedHexagons(): Result<Set<String>>

    /**
     * Marks the specified hexagons as revealed.
     * Should sync with the backend.
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
}
