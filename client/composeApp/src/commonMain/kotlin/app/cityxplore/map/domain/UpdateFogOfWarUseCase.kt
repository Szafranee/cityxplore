package app.cityxplore.map.domain

import app.cityxplore.core.location.Location

/**
 * Use case for updating the Fog of War based on user's current location.
 *
 * This calculates which hexagons should be revealed within the configured
 * radius around the user and persists them via the repository.
 *
 * The H3 algorithm efficiently computes the hexagons in a circular area
 * using the `gridDisk` function.
 *
 * @property repository Repository for persisting revealed hexagons.
 * @property config Configuration for fog of war (resolution, radius, etc.).
 */
class UpdateFogOfWarUseCase(
    private val repository: FogOfWarRepository,
    private val config: FogOfWarConfiguration = FogOfWarConfiguration()
) {
    /**
     * Updates the fog of war based on the user's location.
     *
     * Calculates hexagons within [FogOfWarConfiguration.revealRadiusMeters] and marks them as revealed.
     * Only new hexagons are sent to the repository to minimise unnecessary updates.
     *
     * @param location The user's current location.
     * @return Result indicating success or failure, with a newly revealed hex count.
     */
    suspend operator fun invoke(location: Location): Result<Int> = runCatching {
        // Get currently revealed hexagons
        val currentRevealed = repository.getRevealedHexagons().getOrThrow()

        // Calculate hexagons to reveal around user location
        val hexesToReveal = HexCalculator.calculateHexagonsToReveal(
            latitude = location.latitude,
            longitude = location.longitude,
            radiusMeters = config.revealRadiusMeters,
            resolution = config.h3Resolution
        )

        // Only persist new hexagons
        val newHexagons = hexesToReveal - currentRevealed
        if (newHexagons.isNotEmpty()) {
            repository.revealHexagons(newHexagons).getOrThrow()
        }

        newHexagons.size
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
