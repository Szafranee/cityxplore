package app.cityxplore.map.data

import kotlinx.serialization.Serializable

/**
 * DTO for Warsaw region hexagons from backend.
 *
 * This contains all hexagons covering the Warsaw metropolitan area,
 * pre-computed on the backend.
 *
 * @property resolution H3 resolution level used (should be 10).
 * @property hexagons Set of all H3 hex indices covering Warsaw.
 * @property totalCount Total number of hexagons.
 */
@Serializable
data class WarsawHexagonsDto(
    val resolution: Int,
    val hexagons: Set<String>,
    val totalCount: Int
)

/**
 * DTO for Fog of War data from the backend API.
 *
 * The backend stores revealed hexagons as a JSON array of H3 index strings.
 *
 * @property userId The user ID (returned from backend for verification).
 * @property revealedHexagons Set of H3 hex indices that have been revealed.
 * @property lastUpdated ISO-8601 timestamp of last update.
 */
@Serializable
data class FogOfWarDto(
    val userId: String? = null,
    val revealedHexagons: Set<String> = emptySet(),
    val lastUpdated: String? = null
)

/**
 * Request body for updating revealed hexagons.
 *
 * @property hexagons Set of H3 hex indices to add to revealed list.
 */
@Serializable
data class RevealHexagonsRequest(
    val hexagons: Set<String>
)
