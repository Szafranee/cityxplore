package app.cityxplore.social.domain.repository

import app.cityxplore.social.domain.model.SharePoiRequest
import app.cityxplore.social.domain.model.SharedPoi
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing shared POIs.
 * Follows the offline-first pattern with Flow for reactive updates.
 */
interface SharedPoiRepository {

    // Read operations (observe cached data)

    /**
     * Observes POIs sent by the current user.
     */
    fun getSentPois(): Flow<List<SharedPoi>>

    /**
     * Observes POIs received by the current user.
     */
    fun getReceivedPois(): Flow<List<SharedPoi>>

    /**
     * Observes unviewed POIs received by the current user.
     */
    fun getUnviewedPois(): Flow<List<SharedPoi>>

    /**
     * Returns the count of unviewed shared POIs.
     */
    fun getUnviewedCount(): Flow<Int>

    // Refresh operations (fetch from API and update cache)

    /**
     * Refreshes the sent POIs list from the API.
     */
    suspend fun refreshSentPois(): Result<Unit>

    /**
     * Refreshes the received POIs list from the API.
     */
    suspend fun refreshReceivedPois(): Result<Unit>

    /**
     * Refreshes the unviewed POIs list from the API.
     */
    suspend fun refreshUnviewedPois(): Result<Unit>

    // Write operations

    /**
     * Shares a POI with another user.
     * @param request The share request containing recipient, POI data, and optional message.
     * @return Result containing the created SharedPoi on success.
     */
    suspend fun sharePoi(request: SharePoiRequest): Result<SharedPoi>

    /**
     * Marks a shared POI as viewed.
     * @param sharedPoiId The ID of the shared POI to mark as viewed.
     * @return Result containing the updated SharedPoi on success.
     */
    suspend fun markViewed(sharedPoiId: String): Result<SharedPoi>

    /**
     * Marks a shared POI as discovered.
     * Called when the recipient gets close to the shared POI location.
     * Note: Unlike regular POI discoveries, shared POI discoveries do NOT grant XP.
     * @param sharedPoiId The ID of the shared POI to mark as discovered.
     * @return Result containing the updated SharedPoi on success.
     */
    suspend fun discoverSharedPoi(sharedPoiId: String): Result<SharedPoi>

    /**
     * Deletes a shared POI record.
     * Only the sharer can delete a shared POI.
     * @param sharedPoiId The ID of the shared POI to delete.
     * @return Result indicating success or failure.
     */
    suspend fun deleteSharedPoi(sharedPoiId: String): Result<Unit>

    /**
     * Gets a single shared POI by ID.
     * @param sharedPoiId The ID of the shared POI.
     * @return Result containing the SharedPoi on success.
     */
    suspend fun getSharedPoiById(sharedPoiId: String): Result<SharedPoi>

    /**
     * Uploads an image for a custom POI.
     *
     * @param imageBytes The raw bytes of the image to upload.
     * @return [Result] containing the public URL of the uploaded image on success.
     */
    suspend fun uploadPoiImage(imageBytes: ByteArray): Result<String>

    /**
     * Clears all locally cached data.
     * Should be called when the user signs out.
     */
    fun clearCache()
}
