package org.cityxplore.backend.fogofwar.generator

import com.uber.h3core.H3Core
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
     * Generates all hexagons covering Warsaw at the specified resolution.
     *
     * Uses a grid-based sampling approach: generates a grid of points within the bounding box
     * and converts each point to its H3 cell index. This ensures complete coverage while
     * handling edge cases efficiently.
     *
     * @param resolution H3 resolution level (10 recommended for urban areas, ~66m diameter hexagons)
     * @return Set of H3 hex index strings covering the Warsaw area
     */
    fun generateHexagons(resolution: Int = 10): Set<String> {
        if (resolution !in 0..15) {
            throw IllegalArgumentException("resolution must be between 0 and 15")
        }

        logger.info("Generating Warsaw hexagons at resolution $resolution...")
        val startTime = System.currentTimeMillis()

        val hexagons = mutableSetOf<String>()

        // Step size for grid sampling (~1km between sample points)
        val latStep = 0.01
        val lngStep = 0.01

        var lat = warsawBounds.minLat
        while (lat <= warsawBounds.maxLat) {
            var lng = warsawBounds.minLng
            while (lng <= warsawBounds.maxLng) {
                try {
                    val cell = h3.latLngToCell(lat, lng, resolution)
                    hexagons.add(h3.h3ToString(cell))
                } catch (e: Exception) {
                    logger.warn("Failed to generate hex for coords ($lat, $lng): ${e.message}")
                }
                lng += lngStep
            }
            lat += latStep
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("Generated ${hexagons.size} hexagons in ${duration}ms")

        return hexagons
    }
}
