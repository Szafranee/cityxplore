package app.cityxplore.core.sync

import app.cityxplore.database.entity.SyncOperation
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.domain.FogOfWarRepository
import app.cityxplore.profile.domain.DistanceSyncRepository

/**
 * Default implementation of [SyncOperationExecutor].
 *
 * Executes queued sync operations against the remote API when connectivity is restored.
 * Each operation type is handled by its corresponding repository.
 *
 * Uses [Lazy] injection to break the circular dependency:
 * PoiRepository -> SyncQueueManager -> SyncOperationExecutor -> PoiRepository
 *
 * **Note:** Social features (FriendInvite, SharePoi) are NOT supported offline.
 * They require immediate network connectivity and are not queued for later sync.
 */
class DefaultSyncOperationExecutor(
    private val poiRepository: Lazy<PoiRepository>,
    private val fogOfWarRepository: Lazy<FogOfWarRepository>,
    private val distanceSyncRepository: Lazy<DistanceSyncRepository>
) : SyncOperationExecutor {

    override suspend fun execute(operation: SyncOperation): Result<Unit> {
        return when (operation) {
            is SyncOperation.DiscoverPoi -> {
                poiRepository.value.discoverPoi(operation.poiId).map { }
            }

            is SyncOperation.ToggleFavorite -> {
                poiRepository.value.toggleFavorite(operation.poiId)
            }

            is SyncOperation.RevealHexagons -> {
                fogOfWarRepository.value.revealHexagons(operation.hexagonIds)
            }

            is SyncOperation.SyncDistance -> {
                distanceSyncRepository.value.syncDistance(operation.distanceMeters).map { }
            }

            // Social features are not supported offline - these should never be queued
            is SyncOperation.FriendInvite,
            is SyncOperation.SharePoi -> {
                Result.failure(UnsupportedOperationException("Social features require network connectivity"))
            }
        }
    }
}
