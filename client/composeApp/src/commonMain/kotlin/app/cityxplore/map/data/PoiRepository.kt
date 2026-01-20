package app.cityxplore.map.data

import app.cityxplore.core.sync.SyncQueueManager
import app.cityxplore.database.currentTimeMillis
import app.cityxplore.database.dao.PoiDao
import app.cityxplore.database.entity.PoiEntity
import app.cityxplore.database.entity.SyncOperation
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.UserDiscovery
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository interface for Point of Interest (POI) operations.
 *
 * Implements the offline-first pattern:
 * - Reading: Flow from local Room database
 * - Writing: Optimistic local update and network sync
 * - Offline: Queues operations for later sync
 *
 * @see NetworkPoiRepository
 */
interface PoiRepository {
    /**
     * Observes all Points of Interest from the local database.
     * This is the primary way to get POIs - always returns cached data instantly.
     *
     * @return Flow of the POI list that updates when data changes.
     */
    fun observePois(): Flow<List<PoiModel>>

    /**
     * Observes only discovered POIs from the local database.
     *
     * @return Flow of a discovered POI list.
     */
    fun observeDiscoveredPois(): Flow<List<PoiModel>>

    /**
     * Observes only favorite POIs from the local database.
     *
     * @return Flow of a favorite POI list.
     */
    fun observeFavoritePois(): Flow<List<PoiModel>>

    /**
     * Fetches all Points of Interest from the backend.
     * The returned POIs include discovery status for the current user.
     *
     * @return [Result] containing a list of [PoiModel] on success, or exception on failure.
     */
    suspend fun fetchPois(): Result<List<PoiModel>>

    /**
     * Refreshes POIs from the network and updates local cache.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun refreshPois(): Result<Unit>

    /**
     * Fetches all POI discoveries for the current authenticated user.
     *
     * @return [Result] containing a map of POI ID to discovery data on success, or exception on failure.
     */
    suspend fun fetchUserDiscoveries(): Result<Map<String, UserDiscovery>>

    /**
     * Marks a POI as discovered by the current user.
     * - Online: API call → local update
     * - Offline: Local optimistic update → queue for sync
     *
     * @param id The unique identifier of the POI to discover.
     * @return [Result] containing [UserPoiDiscoveryDto] with newly unlocked achievements, or exception on failure.
     */
    suspend fun discoverPoi(id: String): Result<UserPoiDiscoveryDto>

    /**
     * Toggles the favorite status of a POI for the current user.
     * - Online: API call → local update
     * - Offline: Local optimistic update → queue for sync
     *
     * @param id The unique identifier of the POI to favorite or unfavorite.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun toggleFavorite(id: String): Result<Unit>

    /**
     * Gets all POIs from the local database synchronously.
     * Used for offline operations like auto-discovery.
     *
     * @return [Result] containing a list of [PoiModel] from local cache.
     */
    suspend fun getLocalPois(): Result<List<PoiModel>>

    /**
     * Clears local POI cache (used on logout).
     */
    suspend fun clearLocalCache()
}

/**
 * Offline-first implementation of [PoiRepository].
 *
 * Key behaviors:
 * - **Reading:** Flow from local Room database
 * - **Writing:** Optimistic local update, then network sync
 * - **Offline:** Operations queued in SyncQueue for later
 *
 * @property client The HTTP client configured with authentication interceptors.
 * @property poiDao Local database access for POI caching.
 * @property syncQueueManager Manager for queuing offline operations.
 */
class NetworkPoiRepository(
    private val client: HttpClient,
    private val poiDao: PoiDao,
    private val syncQueueManager: SyncQueueManager
) : PoiRepository {

    /**
     * Observes all POIs from the local database.
     */
    override fun observePois(): Flow<List<PoiModel>> {
        return poiDao.observeAllPois().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Observes only discovered POIs from the local database.
     */
    override fun observeDiscoveredPois(): Flow<List<PoiModel>> {
        return poiDao.observeDiscoveredPois().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Observes only favorite POIs from the local database.
     */
    override fun observeFavoritePois(): Flow<List<PoiModel>> {
        return poiDao.observeFavoritePois().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Fetches all POIs from the backend API endpoint `/api/pois`.
     * Automatically filters out POIs with invalid coordinates.
     *
     * @return [Result] containing a list of valid [PoiModel] objects.
     */
    override suspend fun fetchPois(): Result<List<PoiModel>> = runCatching {
        client.get("https://api.cityxplore.app/api/pois").body<List<PoiDto>>().mapNotNull(PoiDto::toDomain)
    }

    /**
     * Refreshes POIs from the network and updates local cache.
     */
    override suspend fun refreshPois(): Result<Unit> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/pois").body<List<PoiDto>>()
        val discoveries = fetchUserDiscoveries().getOrDefault(emptyMap())

        val entities = dtos.mapNotNull { dto ->
            dto.toDomain()?.let { model ->
                val discovery = discoveries[model.id]
                PoiEntity.fromDomain(
                    model = model,
                    discovered = discovery != null,
                    discoveryDate = discovery?.discoveredAt,
                    isFavorite = discovery?.favorite ?: false
                )
            }
        }

        poiDao.upsertAll(entities)
    }

    /**
     * Fetches all POI discoveries for the current user from the backend API endpoint `/api/pois/discoveries`.
     *
     * @return [Result] containing a map of discovered POI IDs to discovery data.
     */
    override suspend fun fetchUserDiscoveries(): Result<Map<String, UserDiscovery>> = runCatching {
        client.get("https://api.cityxplore.app/api/pois/discoveries")
            .body<List<UserPoiDiscoveryDto>>()
            .associate { dto ->
                val domain = dto.toDomain()
                domain.poiId to domain
            }
    }

    /**
     * Marks a POI as discovered.
     * - Online: API call → local update
     * - Offline: Local optimistic update → queue for sync
     */
    override suspend fun discoverPoi(id: String): Result<UserPoiDiscoveryDto> {
        val now = currentTimeMillis()

        return if (syncQueueManager.isOnline()) {
            // Online: call API first
            runCatching {
                val result = client.post("https://api.cityxplore.app/api/pois/$id/discover") {
                    contentType(ContentType.Application.Json)
                    setBody(emptyMap<String, String>())
                }.body<UserPoiDiscoveryDto>()

                // Update local DB on success
                poiDao.markAsDiscovered(id, now, now)
                result
            }
        } else {
            // Offline: optimistic local update + queue
            poiDao.markAsDiscovered(id, now, 0L) // 0L indicates not synced
            syncQueueManager.enqueue(SyncOperation.DiscoverPoi(id))

            // Return a placeholder result
            Result.success(
                UserPoiDiscoveryDto(
                    poiId = id,
                    discoveredAt = "",
                    favorite = false,
                    newlyUnlockedAchievements = emptyList()
                )
            )
        }
    }

    /**
     * Toggles the favorite status of a POI.
     * - Online: API call → local update
     * - Offline: Local optimistic update → queue for sync
     */
    override suspend fun toggleFavorite(id: String): Result<Unit> {
        val now = currentTimeMillis()

        // Always update locally first (optimistic)
        poiDao.toggleFavorite(id, now)

        return if (syncQueueManager.isOnline()) {
            runCatching {
                client.post("https://api.cityxplore.app/api/pois/$id/favorite") {
                    contentType(ContentType.Application.Json)
                    setBody(emptyMap<String, String>())
                }
            }.map { }
        } else {
            // Queue for later sync
            syncQueueManager.enqueue(SyncOperation.ToggleFavorite(id))
            Result.success(Unit)
        }
    }

    /**
     * Gets all POIs from the local database synchronously.
     * Used for offline operations like auto-discovery.
     */
    override suspend fun getLocalPois(): Result<List<PoiModel>> = runCatching {
        poiDao.getAllPois().map { it.toDomain() }
    }

    /**
     * Clears local POI cache (used on logout).
     */
    override suspend fun clearLocalCache() {
        poiDao.clearAll()
    }
}
