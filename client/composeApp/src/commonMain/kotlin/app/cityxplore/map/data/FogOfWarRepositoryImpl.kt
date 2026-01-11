package app.cityxplore.map.data

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
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Implementation of [FogOfWarRepository] using backend API.
 *
 * Endpoints:
 * - GET /api/fog-of-war/warsaw-hexagons - Fetch all Warsaw hexagons (static)
 * - GET /api/fog-of-war/me - Fetch user's revealed hexagons
 * - POST /api/fog-of-war/reveal - Add new revealed hexagons
 * - DELETE /api/fog-of-war/me - Clear all (for testing)
 *
 * @property httpClient Ktor client with authentication configured.
 */
class FogOfWarRepositoryImpl(
    private val httpClient: HttpClient,
    private val config: FogOfWarConfiguration = FogOfWarConfiguration()
) : FogOfWarRepository {

    override suspend fun getWarsawHexagons(): Result<Set<String>> = runCatching {
        val localHexes = loadWarsawHexagons(resolution = config.h3Resolution)
        if (localHexes.isNotEmpty()) {
            return@runCatching localHexes
        }

        val response = httpClient.get("https://api.cityxplore.app/api/fog-of-war/warsaw-hexagons")

        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch Warsaw hexagons: ${response.status}")
        }

        val dto = response.body<WarsawHexagonsDto>()
        dto.hexagons
    }

    override suspend fun getRevealedHexagons(): Result<Set<String>> = runCatching {
        val response = httpClient.get("https://api.cityxplore.app/api/fog-of-war/me")

        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch fog of war: ${response.status}")
        }

        val dto = response.body<FogOfWarDto>()
        dto.revealedHexagons
    }

    override suspend fun revealHexagons(hexIndices: Set<String>): Result<Unit> = runCatching {
        val response = httpClient.post("https://api.cityxplore.app/api/fog-of-war/reveal") {
            contentType(ContentType.Application.Json)
            setBody(RevealHexagonsRequest(hexagons = hexIndices))
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("Failed to reveal hexagons: ${response.status} - $errorBody")
        }
    }

    override suspend fun clearAllRevealed(): Result<Unit> = runCatching {
        val response = httpClient.request("https://api.cityxplore.app/api/fog-of-war/me") {
            method = io.ktor.http.HttpMethod.Delete
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to clear fog of war: ${response.status}")
        }
    }
}
