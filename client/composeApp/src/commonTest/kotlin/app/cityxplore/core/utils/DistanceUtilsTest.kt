package app.cityxplore.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for the Haversine distance calculation algorithm.
 *
 * These tests verify:
 * - Distance calculation accuracy for various real-world locations
 * - Edge cases (same location, antipodal points)
 * - Boundary conditions (poles, date line)
 * - Known distance validation against real geographic data
 *
 * The Haversine formula is critical for:
 * - POI discovery (100m radius check)
 * - User statistics (total distance traveled)
 * - Rankings and achievements
 */
class DistanceUtilsTest {

    @Test
    fun `calculateDistance should return 0 for identical coordinates`() {
        val distance = calculateDistance(52.2297, 21.0122, 52.2297, 21.0122)
        assertEquals(0.0, distance, 0.01, "Distance between identical points should be 0")
    }

    @Test
    fun `calculateDistance Warsaw Old Town to Palace of Culture should be approximately 2000m`() {
        // Warsaw Old Town (Castle Square)
        val oldTownLat = 52.2480
        val oldTownLon = 21.0130

        // Palace of Culture and Science
        val palaceLat = 52.2321
        val palaceLon = 21.0069

        val distance = calculateDistance(oldTownLat, oldTownLon, palaceLat, palaceLon)

        // Expected ~1800-2100m based on real geographic distance
        assertTrue(
            distance > 1700.0 && distance < 2200.0,
            "Distance between Warsaw Old Town and Palace should be ~2000m, was: $distance"
        )
    }

    @Test
    fun `calculateDistance should handle large distances correctly`() {
        // Warsaw, Poland
        val warsawLat = 52.2297
        val warsawLon = 21.0122

        // Berlin, Germany
        val berlinLat = 52.5200
        val berlinLon = 13.4050

        val distance = calculateDistance(warsawLat, warsawLon, berlinLat, berlinLon)

        // Expected ~515-525 km
        assertTrue(
            distance > 500000.0 && distance < 530000.0,
            "Distance Warsaw-Berlin should be ~515km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance should work for very short distances under 100m`() {
        // Two points ~50m apart in Warsaw
        val lat1 = 52.2297
        val lon1 = 21.0122
        val lat2 = 52.2301  // ~44m north
        val lon2 = 21.0122

        val distance = calculateDistance(lat1, lon1, lat2, lon2)

        // Expected ~44m
        assertTrue(
            distance > 40.0 && distance < 50.0,
            "Distance should be ~44m, was: $distance"
        )
    }

    @Test
    fun `calculateDistance should be symmetric`() {
        val lat1 = 52.2297
        val lon1 = 21.0122
        val lat2 = 52.2400
        val lon2 = 21.0200

        val distance1 = calculateDistance(lat1, lon1, lat2, lon2)
        val distance2 = calculateDistance(lat2, lon2, lat1, lon1)

        assertEquals(
            distance1, distance2, 0.01,
            "Distance should be the same regardless of direction"
        )
    }

    @Test
    fun `calculateDistance across equator should work correctly`() {
        // Point in Northern Hemisphere (Kenya)
        val northLat = 1.0
        val northLon = 36.8219

        // Point in Southern Hemisphere (Tanzania)
        val southLat = -1.0
        val southLon = 36.8219

        val distance = calculateDistance(northLat, northLon, southLat, southLon)

        // Expected ~222km (1 degree of latitude ≈ 111km)
        assertTrue(
            distance > 220000.0 && distance < 225000.0,
            "Distance across equator should be ~222km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance across prime meridian should work correctly`() {
        // Greenwich, UK (on Prime Meridian)
        val greenwichLat = 51.4779
        val greenwichLon = 0.0

        // Point slightly west
        val westLat = 51.4779
        val westLon = -0.1

        val distance = calculateDistance(greenwichLat, greenwichLon, westLat, westLon)

        // Expected ~7km
        assertTrue(
            distance > 6000.0 && distance < 8000.0,
            "Distance across prime meridian should be ~7km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance to North Pole should work`() {
        // Warsaw
        val warsawLat = 52.2297
        val warsawLon = 21.0122

        // North Pole
        val poleLat = 90.0
        val poleLon = 0.0  // Longitude doesn't matter at pole

        val distance = calculateDistance(warsawLat, warsawLon, poleLat, poleLon)

        // Expected ~4200km
        assertTrue(
            distance > 4000000.0 && distance < 4400000.0,
            "Distance Warsaw to North Pole should be ~4200km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance across date line should work correctly`() {
        // Point just west of date line
        val westLat = 0.0
        val westLon = 179.9

        // Point just east of date line
        val eastLat = 0.0
        val eastLon = -179.9

        val distance = calculateDistance(westLat, westLon, eastLat, eastLon)

        // Expected ~22km (0.2 degrees at equator)
        assertTrue(
            distance > 20000.0 && distance < 25000.0,
            "Distance across date line should be ~22km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance should handle negative coordinates`() {
        // Sydney, Australia (negative coordinates)
        val sydneyLat = -33.8688
        val sydneyLon = 151.2093

        // Melbourne, Australia
        val melbourneLat = -37.8136
        val melbourneLon = 144.9631

        val distance = calculateDistance(sydneyLat, sydneyLon, melbourneLat, melbourneLon)

        // Expected ~713km
        assertTrue(
            distance > 700000.0 && distance < 730000.0,
            "Distance Sydney-Melbourne should be ~713km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance at high latitudes should account for longitude compression`() {
        // Two points at high latitude (near Iceland)
        val lat = 65.0
        val lon1 = 0.0
        val lon2 = 1.0  // 1 degree of longitude

        val distance = calculateDistance(lat, lon1, lat, lon2)

        // At 65°N, 1 degree of longitude ≈ 47km (compressed from ~111km at equator)
        assertTrue(
            distance > 45000.0 && distance < 50000.0,
            "Distance should account for longitude compression at high latitudes, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance one degree of latitude should be approximately 111km`() {
        // Test at equator
        val lat1 = 0.0
        val lon = 0.0
        val lat2 = 1.0

        val distance = calculateDistance(lat1, lon, lat2, lon)

        // One degree of latitude is approximately 111.32 km everywhere on Earth
        assertTrue(
            distance > 110000.0 && distance < 112000.0,
            "One degree of latitude should be ~111km, was: ${distance / 1000}km"
        )
    }

    @Test
    fun `calculateDistance for POI discovery range (100m) should be accurate`() {
        // Test critical distance for POI discovery (100m)
        val lat1 = 52.2297
        val lon1 = 21.0122

        // Point approximately 100m north
        // 1 degree latitude ≈ 111,320 m
        // So 100m ≈ 0.0009 degrees
        val lat2 = 52.2297 + 0.0009
        val lon2 = 21.0122

        val distance = calculateDistance(lat1, lon1, lat2, lon2)

        // Should be very close to 100m
        assertTrue(
            distance > 95.0 && distance < 105.0,
            "100m test distance should be accurate for POI discovery, was: $distance"
        )
    }

    @Test
    fun `calculateDistance for very close points (GPS jitter range) should work`() {
        // Points 5 meters apart (typical GPS accuracy)
        val lat1 = 52.2297
        val lon1 = 21.0122
        val lat2 = 52.2297 + 0.000045  // ~5m
        val lon2 = 21.0122

        val distance = calculateDistance(lat1, lon1, lat2, lon2)

        assertTrue(
            distance > 4.0 && distance < 6.0,
            "Should accurately measure GPS jitter distances, was: $distance"
        )
    }

    @Test
    fun `calculateDistance maximum possible distance on Earth should be approximately 20000km`() {
        // Antipodal points (opposite sides of Earth)
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = 0.0
        val lon2 = 180.0

        val distance = calculateDistance(lat1, lon1, lat2, lon2)

        // Half the Earth's circumference ≈ 20,037 km
        assertTrue(
            distance > 19900000.0 && distance < 20200000.0,
            "Maximum distance (antipodal points) should be ~20,000km, was: ${distance / 1000}km"
        )
    }
}
