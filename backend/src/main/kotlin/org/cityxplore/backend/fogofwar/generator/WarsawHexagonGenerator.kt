package org.cityxplore.backend.fogofwar.generator

import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import org.cityxplore.backend.fogofwar.model.WarsawBounds
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Generator for Warsaw region hexagons using H3 library.
 *
 * Generates all H3 hexagons covering the Warsaw metropolitan area at a specified resolution.
 * Uses a grid-based approach to ensure complete coverage of the bounding box.
 */
@Component
class WarsawHexagonGenerator {

    private val logger = LoggerFactory.getLogger(WarsawHexagonGenerator::class.java)
    private val warsawBounds = WarsawBounds()
    private val h3 = H3Core.newInstance()

    /**
     * Generates a set of hexagon indices covering the Warsaw metropolitan area
     * at the specified resolution using the H3 library.
     *
     * @param resolution The resolution level for the hexagons, must be in the range [0, 15].
     *                   Higher resolutions result in smaller hexagons. Defaults to 10.
     * @return A set of hexagon indices as strings representing the generated hexagons.
     *         Returns an empty set if the generation fails.
     * @throws IllegalArgumentException If the resolution is outside the range [0, 15].
     */
    fun generateHexagons(resolution: Int = 10): Set<String> {
        require(resolution in 0..15) { "Resolution must be between 0 and 15" }


        logger.info("Generating Warsaw hexagons at resolution $resolution...")
        val startTime = System.currentTimeMillis()

        // Define the bounding box as a closed polygon
        val boundary = listOf(
            LatLng(warsawBounds.minLat, warsawBounds.minLng),
            LatLng(warsawBounds.minLat, warsawBounds.maxLng),
            LatLng(warsawBounds.maxLat, warsawBounds.maxLng),
            LatLng(warsawBounds.maxLat, warsawBounds.minLng),
            LatLng(warsawBounds.minLat, warsawBounds.minLng)
        )

        val hexagons = try {
            h3.polygonToCells(boundary, null, resolution)
                .map { h3.h3ToString(it) }
                .toSet()
        } catch (e: Exception) {
            logger.error("Failed to generate hexagons: ${e.message}", e)
            emptySet()
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("Generated ${hexagons.size} hexagons in ${duration}ms")

        return hexagons
    }
}
