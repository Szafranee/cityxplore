package app.cityxplore.map.data

import app.cityxplore.map.domain.PoiModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Repository interface for Point of Interest (POI) operations.
 *
 * This repository handles fetching POI data from the backend and managing
 * POI discovery operations.
 *
 * @see NetworkPoiRepository
 */
interface PoiRepository {
    /**
     * Fetches all Points of Interest from the backend.
     * The returned POIs include discovery status for the current user.
     *
     * @return [Result] containing a list of [PoiModel] on success, or exception on failure.
     */
    suspend fun fetchPois(): Result<List<PoiModel>>

    /**
     * Fetches all POI discoveries for the current authenticated user.
     *
     * @return [Result] containing a map of POI ID to discovery timestamp on success, or exception on failure.
     */
    suspend fun fetchUserDiscoveries(): Result<Map<String, Long>>

    /**
     * Marks a POI as discovered by the current user.
     *
     * @param id The unique identifier of the POI to discover.
     * @return [Result] containing [Unit] on success, or exception on failure (e.g., 409 if already discovered).
     */
    suspend fun discoverPoi(id: String): Result<Unit>
}

/**
 * Production implementation of [PoiRepository] using a Ktor HTTP client.
 *
 * This implementation communicates with the CityXplore backend API to fetch
 * POI data and manage discovery operations.
 *
 * @property client The HTTP client configured with authentication interceptors.
 */
class NetworkPoiRepository(
    private val client: HttpClient
) : PoiRepository {
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
     * Fetches all POI discoveries for the current user from the backend API endpoint `/api/pois/discoveries`.
     *
     * @return [Result] containing a map of discovered POI IDs to timestamps.
     */
    override suspend fun fetchUserDiscoveries(): Result<Map<String, Long>> = runCatching {
        client.get("https://api.cityxplore.app/api/pois/discoveries")
            .body<List<UserPoiDiscoveryDto>>()
            .associate { dto ->
                val domain = dto.toDomain()
                domain.poiId to domain.discoveredAt
            }
    }

    /**
     * Sends a discovery request to the backend API endpoint `/api/pois/{id}/discover`.
     * The backend returns 200 on success, or 409 if the POI was already discovered.
     *
     * @param id The unique identifier of the POI to discover.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    override suspend fun discoverPoi(id: String): Result<Unit> = runCatching {
        client.post("https://api.cityxplore.app/api/pois/$id/discover") {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }
    }.map { }
}
