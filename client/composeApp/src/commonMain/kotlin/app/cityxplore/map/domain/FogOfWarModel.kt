package app.cityxplore.map.domain

/**
 * Represents the Fog of War state - hexagons that have been revealed by the user.
 *
 * Uses H3 hexagonal grid system for efficient spatial indexing.
 * Each H3 index uniquely identifies a hexagon at a specific resolution level.
 *
 * @property revealedHexIndices Set of H3 hex indices (as strings) that have been explored.
 */
data class FogOfWarModel(
    val revealedHexIndices: Set<String>
)

/**
 * Configuration for Fog of War visualisation and behaviour.
 *
 * @property h3Resolution H3 resolution level (0-15). Higher = smaller hexagons.
 *                        Resolution 10 = ~66m diameter hexagons (recommended for urban areas).
 * @property revealRadiusMeters The radius around the user's location within which hexagons are revealed.
 * @property fogColor Hex color string for unrevealed areas (e.g. "#80000000" for semi-transparent black).
 * @property warsawBounds Geographic bounds of the Warsaw region (lat/lng min/max) for hex generation.
 */
data class FogOfWarConfiguration(
    val h3Resolution: Int = 10,
    val revealRadiusMeters: Double = 200.0,
    val fogColor: String = "#B0404040", // Semi-transparent dark gray
    val warsawBounds: GeoBounds = GeoBounds.WARSAW
)

/**
 * Geographic bounding box defining a rectangular region.
 *
 * @property minLat Southern boundary (latitude).
 * @property maxLat Northern boundary (latitude).
 * @property minLng Western boundary (longitude).
 * @property maxLng Eastern boundary (longitude).
 */
data class GeoBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
) {
    companion object {
        /**
         * Warsaw and surrounding area bounds.
         * Covers the metropolitan area + suburbs for testing.
         */
        val WARSAW = GeoBounds(
            minLat = 52.09,   // South
            maxLat = 52.37,   // North
            minLng = 20.85,   // West
            maxLng = 21.27    // East
        )
    }
}
