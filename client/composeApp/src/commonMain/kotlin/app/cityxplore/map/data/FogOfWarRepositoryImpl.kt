package app.cityxplore.map.data

import app.cityxplore.core.sync.SyncQueueManager
import app.cityxplore.database.dao.FogOfWarDao
import app.cityxplore.database.entity.SyncOperation
import app.cityxplore.map.domain.FogOfWarConfiguration
import app.cityxplore.map.domain.FogOfWarRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first implementation of [FogOfWarRepository].
 *
 * Key behaviors:
 * - **Reading:** Always from local Room database (Flow)
 * - **Writing:** Local first, then sync to API
 * - **Offline:** Operations queued in SyncQueue for later
 *
 * Endpoints:
 * - GET /api/fog-of-war/warsaw-hexagons - Fetch all Warsaw hexagons (static)
 * - GET /api/fog-of-war/me - Fetch user's revealed hexagons
 * - POST /api/fog-of-war/reveal - Add new revealed hexagons
 * - DELETE /api/fog-of-war/me - Clear all (for testing)
 */
class FogOfWarRepositoryImpl(
    private val httpClient: HttpClient,
    private val fogOfWarDao: FogOfWarDao,
    private val syncQueueManager: SyncQueueManager,
    private val config: FogOfWarConfiguration = FogOfWarConfiguration()
) : FogOfWarRepository {

    /**
     * Observes revealed hexagons from a local database.
     * This is the primary way to get data - UI should collect this Flow.
     */
    override fun observeRevealedHexagons(): Flow<Set<String>> {
        return fogOfWarDao.observeRevealedHexagons()
            .map { it.toSet() }
    }

    /**
     * Gets revealed hexagons synchronously from a local database.
     */
    override suspend fun getRevealedHexagons(): Result<Set<String>> = runCatching {
        fogOfWarDao.getRevealedHexagons().toSet()
    }

    /**
     * Refreshes data from the server and merges with local data.
     * Server data is authoritative, but we keep any unsynced local hexagons.
     */
    override suspend fun refreshRevealedHexagons(): Result<Unit> = runCatching {
        val response = httpClient.get("https://api.cityxplore.app/api/fog-of-war/me")

        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch fog of war: ${response.status}")
        }

        val dto = response.body<FogOfWarDto>()

        // Insert server hexagons (already synced)
        fogOfWarDao.insertFromServer(dto.revealedHexagons)

        // Now sync any locally revealed but unsynced hexagons to the server
        val unsyncedHexagons = fogOfWarDao.getUnsyncedHexagons()
        if (unsyncedHexagons.isNotEmpty()) {
            try {
                syncHexagonsToServer(unsyncedHexagons.toSet())
                // Mark as synced only after successful server sync
                fogOfWarDao.markAsSynced(unsyncedHexagons)
            } catch (_: Exception) {
                // Sync failed - hexagons remain unsynced for the next attempt
            }
        }
    }

    /**
     * Reveals hexagons with offline support.
     * - Always saves locally first (optimistic update)
     * - Tries to sync to server if online
     * - Queues for later sync if offline
     */
    override suspend fun revealHexagons(hexIndices: Set<String>): Result<Unit> = runCatching {
        if (hexIndices.isEmpty()) return@runCatching

        // 1. Always save locally first (optimistic update)
        fogOfWarDao.insertLocallyRevealed(hexIndices)

        // 2. Try to sync to server
        if (syncQueueManager.isOnline()) {
            try {
                syncHexagonsToServer(hexIndices)
                // Mark as synced on success
                fogOfWarDao.markAsSynced(hexIndices.toList())
            } catch (_: Exception) {
                // Sync failed - queue for later
                syncQueueManager.enqueue(SyncOperation.RevealHexagons(hexIndices))
            }
        } else {
            // Offline - queue for later sync
            syncQueueManager.enqueue(SyncOperation.RevealHexagons(hexIndices))
        }
    }

    /**
     * Syncs hexagons to the server API.
     */
    private suspend fun syncHexagonsToServer(hexIndices: Set<String>) {
        val response = httpClient.post("https://api.cityxplore.app/api/fog-of-war/reveal") {
            contentType(ContentType.Application.Json)
            setBody(RevealHexagonsRequest(hexagons = hexIndices))
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("Failed to reveal hexagons: ${response.status} - $errorBody")
        }
    }

    override suspend fun getWarsawHexagons(): Result<Set<String>> = runCatching {
        // Try loading from local GeoJSON first (fast, always available)
        // Retry up to 3 times if empty (the H3 library might not be ready)
        var localHexes: Set<String>
        repeat(3) { attempt ->
            localHexes = loadWarsawHexagons(resolution = config.h3Resolution)
            if (localHexes.isNotEmpty()) {
                println("FogOfWarRepository: Loaded ${localHexes.size} hexagons from local GeoJSON (attempt ${attempt + 1})")
                return@runCatching localHexes
            }
            println("FogOfWarRepository: Local GeoJSON returned empty, retry ${attempt + 1}/3")
            delay(50) // Small delay before retry
        }

        // Local failed - try backend as fallback
        println("FogOfWarRepository: Local GeoJSON failed, trying backend...")
        val response = httpClient.get("https://api.cityxplore.app/api/fog-of-war/warsaw-hexagons")

        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch Warsaw hexagons: ${response.status}")
        }

        val dto = response.body<WarsawHexagonsDto>()
        println("FogOfWarRepository: Loaded ${dto.hexagons.size} hexagons from backend")
        dto.hexagons
    }

    override suspend fun clearAllRevealed(): Result<Unit> = runCatching {
        // Clear local first
        fogOfWarDao.clearAll()

        // Then clear on server if online
        // NOTE: Intentionally not queuing this operation when offline.
        // Rationale: Clearing fog of war is a destructive action that users rarely perform.
        // If offline, the local clear takes effect immediately, and the server state will
        // naturally diverge until the next sync. The user can re-trigger the clear when online.
        // Implementing a ClearAllRevealed queue operation would add complexity for a rare use case.
        if (syncQueueManager.isOnline()) {
            val response = httpClient.request("https://api.cityxplore.app/api/fog-of-war/me") {
                method = HttpMethod.Delete
            }

            if (!response.status.isSuccess()) {
                throw Exception("Failed to clear fog of war: ${response.status}")
            }
        } else {
            println("clearAllRevealed: Offline - local cleared but server not updated. Will sync on next refresh.")
        }
    }

    override suspend fun clearLocalCache() {
        fogOfWarDao.clearAll()
    }
}
