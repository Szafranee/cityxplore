package app.cityxplore.core

import app.cityxplore.core.location.DistanceTracker
import app.cityxplore.map.domain.AutoDiscoverPoisUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for critical business constants.
 *
 * These tests protect against accidental changes to core business rules.
 * Any failure here indicates a breaking change that requires:
 * - Product owner approval
 * - Database migration (for stored values)
 * - User communication (affects gameplay balance)
 *
 * DO NOT MODIFY THESE VALUES WITHOUT EXPLICIT APPROVAL.
 */
class BusinessRulesRegressionTest {

    @Test
    fun `POI discovery radius must remain at 100 meters`() {
        val expectedRadius = 100.0

        assertEquals(
            expectedRadius,
            AutoDiscoverPoisUseCase.DISCOVERY_RADIUS_METERS,
            "POI discovery radius must remain at 100m as per product requirements. " +
                    "This value affects user experience and game balance. " +
                    "Change requires approval and user communication."
        )
    }

    @Test
    fun `Distance sync threshold must remain at 100 meters`() {
        val expectedThreshold = 100.0

        assertEquals(
            expectedThreshold,
            DistanceTracker.SYNC_THRESHOLD_METERS,
            "Distance sync threshold must remain at 100m to balance accuracy vs server load. " +
                    "Changing this affects statistics accuracy and backend performance."
        )
    }

    @Test
    fun `GPS jitter filter threshold must remain at 1 meter`() {
        val expectedMinDistance = 1.0

        assertEquals(
            expectedMinDistance,
            DistanceTracker.MIN_DISTANCE_METERS,
            "GPS jitter filter must remain at 1m to prevent noise in distance tracking. " +
                    "This is critical for accurate user statistics."
        )
    }

    @Test
    fun `Maximum GPS speed threshold must remain at 50 meters per second`() {
        val expectedMaxSpeed = 50.0

        assertEquals(
            expectedMaxSpeed,
            DistanceTracker.MAX_SPEED_MPS,
            "Maximum GPS speed threshold protects against GPS errors and cheating. " +
                    "Value of 50 m/s (180 km/h) allows for realistic high-speed movement."
        )
    }

    @Test
    fun `Haversine Earth radius must be 6371000 meters`() {
        // This is derived from the implementation in DistanceUtils
        // Earth's mean radius in meters

        // We can't directly test this without exposing it, but we can validate
        // that a known distance is calculated correctly
        val oneDegreeLatitudeDistance = app.cityxplore.core.utils.calculateDistance(
            0.0, 0.0,  // Equator, prime meridian
            1.0, 0.0   // One degree north
        )

        // One degree of latitude calculation:
        // Radius = 6,371,000 meters
        // Circumference = 2 * PI * R
        // 1 degree = Circumference / 360 = (2 * PI * 6371000) / 360 ≈ 111,194.9 meters
        val theoreticalDistance = 111194.9

        assertEquals(
            theoreticalDistance,
            oneDegreeLatitudeDistance,
            1.0, // 1 meter tolerance // 100 m tolerance for floating point
            "Distance calculation must use correct Earth radius (6,371 km). " +
                    "This ensures all distance-based features are geographically accurate."
        )
    }
}
