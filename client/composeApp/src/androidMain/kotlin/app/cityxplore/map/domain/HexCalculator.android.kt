package app.cityxplore.map.domain

import com.uber.h3core.H3Core
import com.uber.h3core.LengthUnit
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
            // Fallback to extracting from resources (standard behaviour)
            try {
                H3Core.newInstance()
            } catch (e2: IOException) {
                throw RuntimeException("Failed to initialize H3Core", e2)
            } catch (e3: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load H3 native library", e3)
            }

        }
    }

    /**
     * Calculates hexagons to reveal around a centre point.
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

        val edgeLengthMeters = h3.getHexagonEdgeLengthAvg(resolution, LengthUnit.m)

        // Approximate k needed to cover the radius
        // Using a slightly generous calculation to ensure coverage
        val k = (radiusMeters / (edgeLengthMeters * 0.8)).toInt().coerceAtLeast(1)

        return h3.gridDisk(centerHex, k).map { h3.h3ToString(it) }.toSet()
    }
}
