package app.cityxplore.core.sync

import app.cityxplore.core.CityXploreDispatchers
import app.cityxplore.core.connectivity.ConnectivityObserver
import app.cityxplore.core.connectivity.NetworkStatus
import app.cityxplore.core.sync.SyncQueueManager.Companion.MAX_RETRY_COUNT
import app.cityxplore.database.currentTimeMillis
import app.cityxplore.database.dao.SyncQueueDao
import app.cityxplore.database.entity.SyncOperation
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    /** Mutex to prevent concurrent processing of pending operations */
    private val processingMutex = Mutex()

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
     * Uses mutex to prevent concurrent processing from multiple callers.
     */
    suspend fun processPendingOperations() = processingMutex.withLock {
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
                    if (isTerminalError(error)) {
                        // Terminal error - operation should not be retried, remove from queue
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

    /**
     * Determines if an error should be treated as terminal (operation should be dropped).
     *
     * Terminal errors include:
     * - HTTP 409 Conflict (operation already processed - idempotency)
     * - UnsupportedOperationException (operation type not supported offline)
     */
    private fun isTerminalError(error: Throwable): Boolean {
        // Check for HTTP 409 Conflict using structured type
        if (error is ClientRequestException) {
            return error.response.status == HttpStatusCode.Conflict
        }

        // UnsupportedOperationException means the operation should never be retried
        // (e.g., social features that require network)
        if (error is UnsupportedOperationException) {
            return true
        }

        // Fallback string-based check for wrapped errors
        val message = error.message ?: return false
        return message.contains("409") ||
                message.contains("Conflict") ||
                message.contains("UnsupportedOperationException")
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
