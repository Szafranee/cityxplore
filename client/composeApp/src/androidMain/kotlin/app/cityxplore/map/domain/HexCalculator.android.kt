package app.cityxplore.map.domain

import com.uber.h3core.H3Core
import java.io.IOException

/**
 * Android implementation of [HexCalculator] using the official Uber H3 Java library.
 *
 * Handles native library loading and provides H3 calculations.
 */
internal actual object HexCalculator {
    private val h3: H3Core by lazy {
        try {
            // Attempt to load the native library manually first
            System.loadLibrary("h3-java")
            H3Core.newSystemInstance()
        } catch (_: UnsatisfiedLinkError) {
            // Fallback to extracting from resources (standard behavior)
            try {
                H3Core.newInstance()
            } catch (e2: IOException) {
                throw RuntimeException("Failed to initialize H3Core", e2)
            }
        }
    }

    /**
     * Calculates hexagons to reveal around a center point.
     *
     * @param latitude Center latitude.
     * @param longitude Center longitude.
     * @param radiusMeters Radius to reveal in meters.
     * @param resolution H3 resolution (0-15).
     * @return Set of H3 index strings.
     */
    actual fun calculateHexagonsToReveal(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        resolution: Int
    ): Set<String> {
        val centerHex = h3.latLngToCell(latitude, longitude, resolution)

        // Approximate edge length in meters for common resolutions
        // Source: https://h3geo.org/docs/core-library/restable/
        val edgeLengthMeters = when (resolution) {
            9 -> 174.375668
            10 -> 65.907807
            11 -> 24.910561
            12 -> 9.415526
            else -> 66.0 // Default to res 10
        }

        // Approximate k needed to cover the radius
        // Using a slightly generous calculation to ensure coverage
        val k = (radiusMeters / (edgeLengthMeters * 0.8)).toInt().coerceAtLeast(1)

        return h3.gridDisk(centerHex, k).map { h3.h3ToString(it) }.toSet()
    }
}
