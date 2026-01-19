package app.cityxplore.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.cityxplore.database.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for sync queue operations.
 *
 * Manages offline operations that need to be synced when connectivity is restored.
 */
@Dao
interface SyncQueueDao {

    /**
     * Observes all pending sync operations.
     */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun observePendingOperations(): Flow<List<SyncQueueEntity>>

    /**
     * Gets all pending sync operations ordered by creation time.
     */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<SyncQueueEntity>

    /**
     * Gets pending operations with retry count below the threshold.
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount < :maxRetries ORDER BY createdAt ASC")
    suspend fun getPendingWithRetryLimit(maxRetries: Int = 5): List<SyncQueueEntity>

    /**
     * Gets operations of a specific type.
     */
    @Query("SELECT * FROM sync_queue WHERE operationType = :type ORDER BY createdAt ASC")
    suspend fun getByType(type: String): List<SyncQueueEntity>

    /**
     * Gets a single operation by ID.
     */
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: Long): SyncQueueEntity?

    /**
     * Inserts a new sync operation.
     */
    @Insert
    suspend fun insert(operation: SyncQueueEntity): Long

    /**
     * Updates an existing operation (e.g. after retry).
     */
    @Update
    suspend fun update(operation: SyncQueueEntity)

    /**
     * Increments the retry count and records the error.
     */
    @Query(
        """
        UPDATE sync_queue 
        SET retryCount = retryCount + 1, 
            lastError = :error,
            lastAttemptAt = :attemptAt 
        WHERE id = :id
    """
    )
    suspend fun incrementRetry(id: Long, error: String?, attemptAt: Long)

    /**
     * Deletes a successfully processed operation.
     */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Deletes all operations of a specific type (e.g. clear all pending discoveries).
     */
    @Query("DELETE FROM sync_queue WHERE operationType = :type")
    suspend fun deleteByType(type: String)

    /**
     * Gets the count of pending operations.
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingCount(): Int

    /**
     * Observes the count of pending operations.
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observePendingCount(): Flow<Int>

    /**
     * Clears all sync queue data.
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()

    /**
     * Clears operations that have exceeded the maximum retry count.
     */
    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun clearFailedOperations(maxRetries: Int = 5)
}
