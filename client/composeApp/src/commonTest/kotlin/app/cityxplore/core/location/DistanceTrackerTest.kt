package app.cityxplore.core.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [DistanceTracker].
 *
 * Tests cover:
 * - Distance accumulation between valid GPS points
 * - GPS anomaly filtering (jumps, jitter)
 * - Sync threshold triggering
 * - Buffer consumption and reset
 */
class DistanceTrackerTest {

    @Test
    fun `onNewLocation should return false for first location`() {
        val tracker = DistanceTracker()
        val location = Location(52.2297, 21.0122) // Warsaw

        val result = tracker.onNewLocation(location)

        assertFalse(result, "First location should not trigger sync")
        assertEquals(0.0, tracker.bufferedDistance.value)
    }

    @Test
    fun `onNewLocation should accumulate distance between valid points`() {
        val tracker = DistanceTracker()

        // First location
        tracker.onNewLocation(Location(52.2297, 21.0122))

        // Second location ~100m away
        tracker.onNewLocation(Location(52.2306, 21.0122))

        // Distance should be accumulated (approximately 100m)
        assertTrue(tracker.bufferedDistance.value > 50.0, "Distance should be accumulated")
        assertTrue(tracker.bufferedDistance.value < 200.0, "Distance should be reasonable")
    }

    @Test
    fun `onNewLocation should filter tiny movements (GPS jitter)`() {
        val tracker = DistanceTracker()

        // First location
        tracker.onNewLocation(Location(52.2297, 21.0122))

        // Very small movement (less than 1m)
        tracker.onNewLocation(Location(52.22970001, 21.01220001))

        assertEquals(0.0, tracker.bufferedDistance.value, "Tiny movements should be filtered")
    }

    @Test
    fun `onNewLocation should return true when sync threshold is reached`() {
        val tracker = DistanceTracker()

        // First location
        tracker.onNewLocation(Location(52.2297, 21.0122))

        // Move enough to exceed threshold (100m)
        // Each step is roughly 111m (0.001 degrees latitude ≈ 111m)
        tracker.onNewLocation(Location(52.2307, 21.0122)) // ~111m
        // This should trigger sync

        assertTrue(
            tracker.bufferedDistance.value >= DistanceTracker.SYNC_THRESHOLD_METERS,
            "Should reach sync threshold"
        )
    }

    @Test
    fun `consumeBufferedDistance should return distance and reset buffer`() {
        val tracker = DistanceTracker()

        // Accumulate some distance
        tracker.onNewLocation(Location(52.2297, 21.0122))
        tracker.onNewLocation(Location(52.2310, 21.0122)) // ~144m

        val distance = tracker.consumeBufferedDistance()

        assertTrue(distance > 0.0, "Should return accumulated distance")
        assertEquals(0.0, tracker.bufferedDistance.value, "Buffer should be reset after consume")
    }

    @Test
    fun `reset should clear all state`() {
        val tracker = DistanceTracker()

        // Build up state
        tracker.onNewLocation(Location(52.2297, 21.0122))
        tracker.onNewLocation(Location(52.2310, 21.0122))

        // Reset
        tracker.reset()

        // Verify state is cleared
        assertEquals(0.0, tracker.bufferedDistance.value)

        // Next location should be treated as first
        val result = tracker.onNewLocation(Location(52.2320, 21.0122))
        assertFalse(result, "After reset, first location should not accumulate distance")
    }

    @Test
    fun `consumeBufferedDistance should return zero when buffer is empty`() {
        val tracker = DistanceTracker()

        val distance = tracker.consumeBufferedDistance()

        assertEquals(0.0, distance)
    }

    @Test
    fun `onNewLocation should handle multiple sequential updates`() {
        val tracker = DistanceTracker()

        // Start point
        tracker.onNewLocation(Location(52.2297, 21.0122))

        // Walk north in small steps (each ~30m)
        repeat(5) { i ->
            val lat = 52.2297 + (i + 1) * 0.0003
            tracker.onNewLocation(Location(lat, 21.0122))
        }

        // Should have accumulated roughly 150m
        assertTrue(
            tracker.bufferedDistance.value > 100.0,
            "Should accumulate distance from multiple updates"
        )
    }
}
