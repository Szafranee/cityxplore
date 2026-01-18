package app.cityxplore.core.utils

import kotlin.math.roundToInt

/**
 * Utility functions for formatting distance values for display.
 *
 * Provides consistent distance formatting across the entire app,
 * including POI details, profile screens, and map overlays.
 */

/**
 * Formats a distance in meters to a user-friendly string.
 *
 * Formatting rules:
 * - Distances < 1000 m: Rounded to the nearest 10 m (minimum 10 m), displayed as "X m"
 * - Distances >= 1000 m: Converted to kilometers with 1 decimal place, displayed as "X.X km"
 *
 * Examples:
 * - 45.6 -> "50 m"
 * - 5.2 -> "10 m"
 * - 1234.5 -> "1.2 km"
 * - 15678.9 -> "15.7 km"
 *
 * @param distanceMeters The distance in meters.
 * @return A formatted string with the appropriate unit (m or km).
 */
fun formatDistanceForDisplay(distanceMeters: Double): String {
    return when {
        distanceMeters < 1000 -> {
            val rounded = ((distanceMeters / 10).toInt() * 10).coerceAtLeast(10)
            "$rounded m"
        }

        else -> {
            val km = distanceMeters / 1000
            val rounded = (km * 10).roundToInt() / 10.0
            "$rounded km"
        }
    }
}

/**
 * Formats a distance in meters for profile/stats display (typically larger distances).
 *
 * This function provides more human-readable output for large distances,
 * suitable for total distance-travelled statistics.
 *
 * Formatting rules:
 * - Distances < 1000 m: Displayed as "X m"
 * - Distances >= 1000 m and < 10,000 m: "X.X km" (1 decimal place)
 * - Distances >= 10,000 m: "X km" (no decimal places)
 *
 * @param meters The distance in meters.
 * @return A formatted string with the appropriate unit.
 */
fun formatDistanceForStats(meters: Double): String {
    return when {
        meters < 1000 -> "${meters.roundToInt()} m"
        meters < 10000 -> {
            val km = (meters / 100.0).roundToInt() / 10.0
            "$km km"
        }

        else -> {
            val km = (meters / 1000).roundToInt()
            "$km km"
        }
    }
}
