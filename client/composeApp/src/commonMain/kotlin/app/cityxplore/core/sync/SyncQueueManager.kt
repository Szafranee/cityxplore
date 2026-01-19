package app.cityxplore.core.sync

import app.cityxplore.core.CityXploreDispatchers
import app.cityxplore.core.connectivity.ConnectivityObserver
import app.cityxplore.core.connectivity.NetworkStatus
import app.cityxplore.core.sync.SyncQueueManager.Companion.MAX_RETRY_COUNT
import app.cityxplore.database.currentTimeMillis
import app.cityxplore.database.dao.SyncQueueDao
import app.cityxplore.database.entity.SyncOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Manages the queue of operations to be synced when connectivity is restored.
 *
 * This class is a core component of the offline-first architecture:
 * - Queues operations when offline
 * - Automatically processes the queue when connectivity is restored
 * - Handles retries and error tracking
 * - Provides idempotency (409 conflicts are treated as success)
 */
class SyncQueueManager(
    private val syncQueueDao: SyncQueueDao,
    private val connectivityObserver: ConnectivityObserver,
    private val dispatchers: CityXploreDispatchers,
    private val syncExecutor: SyncOperationExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    companion object {
        const val MAX_RETRY_COUNT = 5
    }

    init {
        // Auto-sync when connection is restored
        scope.launch {
            connectivityObserver.observe()
                .filter { it == NetworkStatus.AVAILABLE }
                .collect {
                    processPendingOperations()
                }
        }
    }

    /**
     * Checks if the device is currently online.
     */
    fun isOnline(): Boolean = connectivityObserver.isNetworkAvailable()

    /**
     * Enqueues an operation to be synced later.
     *
     * @param operation The operation to queue.
     * @return The ID of the queued operation.
     */
    suspend fun enqueue(operation: SyncOperation): Long {
        return syncQueueDao.insert(operation.toEntity())
    }

    /**
     * Observes the count of pending operations.
     */
    fun observePendingCount(): Flow<Int> = syncQueueDao.observePendingCount()

    /**
     * Processes all pending operations in the queue.
     *
     * Operations are processed in order of creation time.
     * Failed operations are retried up to [MAX_RETRY_COUNT] times.
     * 409 Conflict responses are treated as success (idempotency).
     */
    suspend fun processPendingOperations() {
        val pending = syncQueueDao.getPendingWithRetryLimit(MAX_RETRY_COUNT)

        for (entity in pending) {
            val operation = SyncOperation.fromEntity(entity)
            if (operation == null) {
                // Invalid operation, remove it
                syncQueueDao.deleteById(entity.id)
                continue
            }

            val result = syncExecutor.execute(operation)

            result.fold(
                onSuccess = {
                    // Successfully synced, remove from queue
                    syncQueueDao.deleteById(entity.id)
                },
                onFailure = { error ->
                    if (isConflictError(error)) {
                        // 409 Conflict - operation was already processed, remove from queue
                        syncQueueDao.deleteById(entity.id)
                    } else {
                        // Other error - increment retry count
                        syncQueueDao.incrementRetry(entity.id, error.message, currentTimeMillis())
                    }
                }
            )
        }

        // Clean-up operations that exceeded the retry limit
        syncQueueDao.clearFailedOperations(MAX_RETRY_COUNT)
    }

    /**
     * Manually triggers sync processing.
     */
    fun triggerSync() {
        scope.launch {
            processPendingOperations()
        }
    }

    /**
     * Clears all pending operations (e.g. on logout).
     */
    suspend fun clearAll() {
        syncQueueDao.clearAll()
    }

    private fun isConflictError(error: Throwable): Boolean {
        return error.message?.contains("409") == true ||
                error.message?.contains("Conflict") == true ||
                error.message?.contains("already") == true
    }
}

/**
 * Interface for executing sync operations against the remote API.
 *
 * Implementations should handle the actual network calls for each operation type.
 */
interface SyncOperationExecutor {
    suspend fun execute(operation: SyncOperation): Result<Unit>
}
