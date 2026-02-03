package app.cityxplore.core.location

import app.cityxplore.core.utils.calculateDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.TimeSource

/**
 * Tracks distance travelled by the user and buffers it for periodic sync to the backend.
 *
 * Key features:
 * - Accumulates distance between consecutive GPS points using the Haversine formula
 * - Filters out GPS anomalies (teleportation, jumps) based on speed thresholds
 * - Buffers distance until sync threshold is reached
 * - Provides a way to consume and reset the buffer for syncing
 *
 * Usage:
 * 1. Call [onNewLocation] for each GPS update
 * 2. When it returns true, call [consumeBufferedDistance] to get the distance to sync
 * 3. Send the distance to the backend
 *
 * @see app.cityxplore.core.utils.calculateDistance
 */
class DistanceTracker {
    companion object {
        /** Minimum distance to accumulate before triggering a sync (in meters) */
        const val SYNC_THRESHOLD_METERS = 100.0

        /** Maximum speed threshold for filtering GPS jumps (~180 km/h in m/s) */
        const val MAX_SPEED_MPS = 50.0

        /** Minimum distance between points to count (filters GPS jitter) */
        const val MIN_DISTANCE_METERS = 1.0
    }

    private val timeSource = TimeSource.Monotonic
    private var lastLocation: Location? = null
    private var lastMark: TimeSource.Monotonic.ValueTimeMark? = null

    private val _bufferedDistance = MutableStateFlow(0.0)

    /**
     * The current buffered distance in meters.
     * Observe this to show real-time distance tracking in the UI.
     */
    val bufferedDistance: StateFlow<Double> = _bufferedDistance.asStateFlow()

    /**
     * Processes a new location update and accumulates valid distance.
     *
     * The method:
     * 1. Calculates distance from the last known location using Haversine formula
     * 2. Filters out GPS anomalies (too fast movement, too small movement)
     * 3. Adds valid distance to the buffer
     *
     * @param location The new GPS location
     * @return true if the sync threshold was reached and distance should be synced
     */
    fun onNewLocation(location: Location): Boolean {
        val now = timeSource.markNow()
        val previous = lastLocation
        val previousMark = lastMark

        // Update state for next call
        lastLocation = location
        lastMark = now

        // First location - nothing to calculate yet
        if (previous == null || previousMark == null) {
            return false
        }

        // Calculate distance using Haversine formula
        val distance = calculateDistance(
            previous.latitude, previous.longitude,
            location.latitude, location.longitude
        )

        // Filter: Ignore tiny movements (GPS jitter)
        if (distance < MIN_DISTANCE_METERS) {
            return false
        }

        // Filter: Ignore unrealistic speeds (GPS jumps/teleportation)
        val elapsed = now - previousMark
        val timeDeltaSeconds = elapsed.inWholeMilliseconds / 1000.0
        if (timeDeltaSeconds > 0) {
            val speed = distance / timeDeltaSeconds
            if (speed > MAX_SPEED_MPS) {
                // Likely GPS jump, ignore this point
                return false
            }
        }

        // Valid distance - add to buffer
        _bufferedDistance.value += distance

        // Check if we should trigger a sync
        return _bufferedDistance.value >= SYNC_THRESHOLD_METERS
    }

    /**
     * Consumes the buffered distance and resets the buffer.
     *
     * Call this method when syncing distance to the backend.
     * The returned value should be sent to the API.
     *
     * @return The accumulated distance in meters
     */
    fun consumeBufferedDistance(): Double {
        val distance = _bufferedDistance.value
        _bufferedDistance.value = 0.0
        return distance
    }

    /**
     * Resets the tracker state completely.
     *
     * Call this on logout or when tracking should be restarted fresh.
     */
    fun reset() {
        lastLocation = null
        lastMark = null
        _bufferedDistance.value = 0.0
    }
}
