package app.cityxplore.map.domain

/**
 * Defines a geographic region for fog of war.
 *
 * Each region has a unique identifier, a GeoJSON boundary file, and an H3 resolution.
 * The client will use these to generate the hex grid offline via polyfill.
 *
 * @property id Unique region identifier (e.g., "warsaw", "krakow").
 * @property displayName Human-readable name for UI.
 * @property boundaryAssetPath Path to the GeoJSON file in androidMain/assets (e.g., "warsaw_simple.geojson").
 * @property h3Resolution H3 resolution for this region (higher = smaller hexes).
 */
data class RegionDefinition(
    val id: String,
    val displayName: String,
    val boundaryAssetPath: String,
    val h3Resolution: Int = 10
) {
    companion object {
        /**
         * Warsaw region definition.
         * Covers the city boundaries as defined in warsaw_simple.geojson.
         */
        val WARSAW = RegionDefinition(
            id = "warsaw",
            displayName = "Warsaw",
            boundaryAssetPath = "warsaw_simple.geojson",
            h3Resolution = 10
        )

        /**
         * All available regions.
         * Add new cities here after placing their GeoJSON in assets.
         */
        val ALL_REGIONS = listOf(WARSAW)

        /**
         * Get a region by its ID.
         */
        fun getById(id: String): RegionDefinition? = ALL_REGIONS.find { it.id == id }
    }
}
