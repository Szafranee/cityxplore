package app.cityxplore.map.domain

import app.cityxplore.core.location.Location

/**
 * Use case for updating the Fog of War based on the user's current location.
 *
 * This calculates which hexagons should be revealed within the configured
 * radius around the user and persists them via the repository.
 *
 * The H3 algorithm efficiently computes the hexagons in a circular area
 * using the `gridDisk` function.
 *
 * Only hexagons that are part of the pre-defined region (e.g. Warsaw)
 * are actually revealed. This prevents revealing hexes outside the playable area.
 *
 * @property repository Repository for persisting revealed hexagons and fetching region hexes.
 * @property config Configuration for fog of war (resolution, radius, etc.).
 */
class UpdateFogOfWarUseCase(
    private val repository: FogOfWarRepository,
    private val config: FogOfWarConfiguration = FogOfWarConfiguration()
) {
    /**
     * Cached set of all hexagons in the playable region (e.g. Warsaw).
     * Loaded lazily on first use and reused for subsequent calls.
     */
    private var regionHexagonsCache: Set<String>? = null

    /**
     * Updates the fog of war based on the user's location.
     *
     * Calculates hexagons within [FogOfWarConfiguration.revealRadiusMeters] and marks them as revealed.
     * Only hexagons that belong to the defined region are revealed - hexagons outside
     * the playable area are ignored to prevent fog clearing effects outside boundaries.
     *
     * Only new hexagons are sent to the repository to minimise unnecessary updates.
     *
     * @param location The user's current location.
     * @return Result indicating success or failure, with a newly revealed hex count.
     */
    suspend operator fun invoke(location: Location): Result<Int> = runCatching {
        // Get currently revealed hexagons
        val currentRevealed = repository.getRevealedHexagons().getOrThrow()

        // Get (or cache) all hexagons in the playable region
        val regionHexagons = regionHexagonsCache ?: run {
            val hexes = repository.getWarsawHexagons().getOrThrow()
            if (hexes.isEmpty()) {
                return@runCatching 0 // Can't reveal if we don't know the region
            }
            regionHexagonsCache = hexes
            hexes
        }

        // Double-check cache isn't empty (safety)
        if (regionHexagons.isEmpty()) {
            println("UpdateFogOfWarUseCase: Region hexagons cache is empty, skipping")
            return@runCatching 0
        }

        // Calculate hexagons to reveal around user location
        val hexesToReveal = HexCalculator.calculateHexagonsToReveal(
            latitude = location.latitude,
            longitude = location.longitude,
            radiusMeters = config.revealRadiusMeters,
            resolution = config.h3Resolution
        )

        // Filter to only hexes that are part of the playable region
        val hexesInRegion = hexesToReveal.intersect(regionHexagons)

        // Only persist new hexagons
        val newHexagons = hexesInRegion - currentRevealed
        if (newHexagons.isNotEmpty()) {
            repository.revealHexagons(newHexagons).getOrThrow()
        }

        newHexagons.size
    }

    /**
     * Clears the internal cache. Called when the user logs out.
     */
    fun clearCache() {
        regionHexagonsCache = null
    }
}

/**
 * Platform-specific hex calculator.
 * Android implementation uses the H3 library, iOS is a stub for now.
 */
internal expect object HexCalculator {
    /**
     * Calculates hexagons to reveal around a centre point.
     *
     * @param latitude Center latitude.
     * @param longitude Center longitude.
     * @param radiusMeters Radius to reveal in meters.
     * @param resolution H3 resolution (0-15).
     * @return Set of H3 index strings.
     */
    fun calculateHexagonsToReveal(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        resolution: Int
    ): Set<String>
}
