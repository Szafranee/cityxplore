package app.cityxplore.core.location

import kotlinx.coroutines.flow.Flow

/**
 * Data class representing a geographic location.
 *
 * @property latitude The latitude coordinate in degrees (range: -90.0 to 90.0).
 * @property longitude The longitude coordinate in degrees (range: -180.0 to 180.0).
 */
data class Location(val latitude: Double, val longitude: Double)

/**
 * Platform-specific service for accessing device location.
 *
 * Implementations use:
 * - Android: Google Play Services FusedLocationProviderClient
 * - iOS: CoreLocation CLLocationManager
 *
 * @see app.cityxplore.core.location.AndroidLocationService
 * @see app.cityxplore.core.location.IosLocationService
 */
interface LocationService {
    /**
     * Observes continuous location updates as a Flow.
     * Emits new [Location] objects as the user moves.
     *
     * @return A Flow emitting location updates. May throw exceptions if permissions are denied or hardware is unavailable.
     */
    fun observeLocation(): Flow<Location>

    /**
     * Retrieves the current location as a one-time request.
     *
     * @return The current [Location], or `null` if location is unavailable.
     */
    suspend fun getCurrentLocation(): Location?
}
