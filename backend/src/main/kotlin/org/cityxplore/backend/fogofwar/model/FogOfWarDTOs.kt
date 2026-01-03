package org.cityxplore.backend.fogofwar.model

import java.time.Instant

/**
 * Data class representing Warsaw hexagon bounds configuration.
 *
 * @property minLat Southern boundary latitude
 * @property maxLat Northern boundary latitude
 * @property minLng Western boundary longitude
 * @property maxLng Eastern boundary longitude
 */
data class WarsawBounds(
    val minLat: Double = 52.09,
    val maxLat: Double = 52.37,
    val minLng: Double = 20.85,
    val maxLng: Double = 21.27
)

/**
 * Response DTO for Warsaw hexagons endpoint.
 *
 * @property resolution H3 resolution level
 * @property hexagons Set of all H3 hex indices covering Warsaw
 * @property totalCount Total number of hexagons
 * @property bounds Geographic bounds of the region
 * @property generatedAt Timestamp when hexagons were generated
 */
data class WarsawHexagonsResponse(
    val resolution: Int,
    val hexagons: Set<String>,
    val totalCount: Int,
    val bounds: WarsawBounds,
    val generatedAt: Instant
)

/**
 * Response DTO for user's fog of war data.
 *
 * @property userId User's unique identifier
 * @property revealedHexagons Set of H3 hex indices revealed by the user
 * @property lastUpdated Timestamp of the last update, null if never updated
 */
data class FogOfWarResponse(
    val userId: String,
    val revealedHexagons: Set<String> = emptySet(),
    val lastUpdated: Instant? = null
)

/**
 * Request DTO for revealing new hexagons.
 *
 * @property hexagons Set of H3 hex indices to mark as revealed
 */
data class RevealHexagonsRequest(
    val hexagons: Set<String>
)

/**
 * Response DTO for reveal hexagons operation.
 *
 * @property message Success message
 * @property totalRevealed Total count of revealed hexagons after operation
 */
data class RevealHexagonsResponse(
    val message: String,
    val totalRevealed: Int
)
