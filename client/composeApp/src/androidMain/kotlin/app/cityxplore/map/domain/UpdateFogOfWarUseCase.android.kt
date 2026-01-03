package app.cityxplore.map.domain

import com.uber.h3core.H3Core

/**
 * Android-specific implementation of hex calculation using Uber H3 library.
 *
 * Converts user location to H3 index and uses gridDisk to get surrounding hexagons
 * within the specified radius.
 */
internal actual object HexCalculator {
    actual fun calculateHexagonsToReveal(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        resolution: Int
    ): Set<String> {
        val h3 = H3Core.newInstance()

        // Convert lat/lng to H3 cell index
        val centerCell = h3.latLngToCell(latitude, longitude, resolution)

        // Calculate k-ring radius based on distance
        // At resolution 10, hexagons are ~66m diameter edge-to-edge
        // For 200m radius, we need approximately k=3 rings
        val kRings = calculateKRings(radiusMeters, resolution)

        // Get all hexagons within k rings (includes center)
        val hexagons = h3.gridDisk(centerCell, kRings)

        return hexagons.map { h3.h3ToString(it) }.toSet()
    }

    /**
     * Calculates the number of k-rings needed to cover the desired radius.
     *
     * This is an approximation based on H3 hexagon edge lengths at different resolutions.
     * Resolution 10: ~66m diameter → ~33m radius per hex
     * Resolution 9: ~175m diameter → ~87.5m radius per hex
     */
    private fun calculateKRings(radiusMeters: Double, resolution: Int): Int {
        // Average hex edge length in meters for different resolutions
        val hexRadiusMeters = when (resolution) {
            8 -> 230.0
            9 -> 87.5
            10 -> 33.0
            11 -> 12.5
            12 -> 4.7
            else -> 33.0 // Default to resolution 10
        }

        // Calculate how many rings we need
        val kRings = (radiusMeters / hexRadiusMeters).toInt() + 1

        // Clamp to reasonable values (max 10 rings to avoid performance issues)
        return kRings.coerceIn(1, 10)
    }
}
