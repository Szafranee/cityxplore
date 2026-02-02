package app.cityxplore.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [DistanceFormatter] logic.
 *
 * Tests cover:
 * - Formatting of short distances (< 1 km)
 * - Formatting of long distances (> 1 km)
 * - Rounding logic (to nearest 10 m or 0.1 km)
 * - Different formats for stats vs display
 */
class DistanceFormatterTest {

    // ==================== Display Format Tests ====================

    @Test
    fun `formatDistanceForDisplay should format short distances in meters`() {
        assertEquals("10 m", formatDistanceForDisplay(5.0), "Should round minimal distance to 10m")
        assertEquals("50 m", formatDistanceForDisplay(48.0), "Should round 48m to 50m")
        assertEquals("100 m", formatDistanceForDisplay(102.0), "Should round 102m to 100m")
        assertEquals("990 m", formatDistanceForDisplay(994.0), "Should round 994m to 990m")
    }

    @Test
    fun `formatDistanceForDisplay should format long distances in kilometers`() {
        assertEquals("1.0 km", formatDistanceForDisplay(1005.0), "Should format 1005m as 1.0 km")
        assertEquals("1.5 km", formatDistanceForDisplay(1520.0), "Should format 1520m as 1.5 km")
        assertEquals("12.3 km", formatDistanceForDisplay(12345.0), "Should format 12345m as 12.3 km")
    }

    @Test
    fun `formatDistanceForDisplay should handle minimal values correctly`() {
        assertEquals("10 m", formatDistanceForDisplay(0.0), "Zero meters should be displayed as 10 m (min)")
        assertEquals("10 m", formatDistanceForDisplay(1.0), "1 meter should be displayed as 10 m (min)")
    }

    @Test
    fun `formatDistanceForDisplay should handle boundary at 1000m`() {
        // Just below 1000 m
        assertEquals("990 m", formatDistanceForDisplay(994.9))

        // Exactly or just above 1000 m
        assertEquals("1.0 km", formatDistanceForDisplay(1000.0))
        assertEquals("1.0 km", formatDistanceForDisplay(1004.0))
    }

    // ==================== Stats Format Tests ====================

    @Test
    fun `formatDistanceForStats should format short distances as raw meters`() {
        assertEquals("5 m", formatDistanceForStats(5.0))
        assertEquals("48 m", formatDistanceForStats(48.0))
        assertEquals("999 m", formatDistanceForStats(999.0))
    }

    @Test
    fun `formatDistanceForStats should format medium distances with 1 decimal place`() {
        assertEquals("1.0 km", formatDistanceForStats(1000.0))
        assertEquals("1.5 km", formatDistanceForStats(1500.0))
        assertEquals("9.9 km", formatDistanceForStats(9940.0))
    }

    @Test
    fun `formatDistanceForStats should format large distances without decimals`() {
        assertEquals("10 km", formatDistanceForStats(10000.0))
        assertEquals("10 km", formatDistanceForStats(10400.0)) // Rounds down/nearest?
        assertEquals("42 km", formatDistanceForStats(42195.0)) // Marathon
        assertEquals("100 km", formatDistanceForStats(100000.0))
    }
}
