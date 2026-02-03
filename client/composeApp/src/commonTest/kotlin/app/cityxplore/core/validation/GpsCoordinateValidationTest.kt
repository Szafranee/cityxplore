package app.cityxplore.core.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validation functions for GPS coordinates.
 * These ensure data integrity for location-based features.
 */
fun isValidLatitude(latitude: Double): Boolean {
    return latitude.isFinite() && latitude >= -90.0 && latitude <= 90.0
}

fun isValidLongitude(longitude: Double): Boolean {
    return longitude.isFinite() && longitude >= -180.0 && longitude <= 180.0
}

/**
 * Comprehensive tests for GPS coordinate validation.
 *
 * Valid coordinate ranges:
 * - Latitude: -90° (South Pole) to +90° (North Pole)
 * - Longitude: -180° to +180° (wraps around at date line)
 *
 * These validations are critical for:
 * - Preventing invalid POI locations in database
 * - Detecting GPS sensor errors
 * - Ensuring map rendering works correctly
 */
class GpsCoordinateValidationTest {

    // ==================== Latitude Tests ====================

    @Test
    fun `valid latitude values should pass validation`() {
        val validLatitudes = listOf(
            0.0,            // Equator
            52.2297,        // Warsaw, Poland
            -33.8688,       // Sydney, Australia
            90.0,           // North Pole
            -90.0,          // South Pole
            45.0,           // Mid-latitude
            -45.0,          // Mid-latitude south
            0.0001,         // Near equator
            -0.0001,        // Near equator south
            89.9999,        // Near North Pole
            -89.9999        // Near South Pole
        )

        validLatitudes.forEach { lat ->
            assertTrue(
                isValidLatitude(lat),
                "Latitude $lat should be valid"
            )
        }
    }

    @Test
    fun `invalid latitude values should fail validation`() {
        val invalidLatitudes = listOf(
            90.1,                       // Beyond North Pole
            -90.1,                      // Beyond South Pole
            180.0,                      // Longitude value used as latitude
            -180.0,                     // Longitude value used as latitude
            91.0,                       // Out of range
            -91.0,                      // Out of range
            200.0,                      // Completely invalid
            -200.0,                     // Completely invalid
            Double.NaN,                 // Not a Number
            Double.POSITIVE_INFINITY,   // Infinity
            Double.NEGATIVE_INFINITY    // Negative infinity
        )

        invalidLatitudes.forEach { lat ->
            assertFalse(
                isValidLatitude(lat),
                "Latitude $lat should be invalid"
            )
        }
    }

    @Test
    fun `latitude boundary values should be handled correctly`() {
        // Exactly at boundaries
        assertTrue(isValidLatitude(90.0), "North Pole (90°) should be valid")
        assertTrue(isValidLatitude(-90.0), "South Pole (-90°) should be valid")

        // Just beyond boundaries
        assertFalse(isValidLatitude(90.0000001), "Just beyond North Pole should be invalid")
        assertFalse(isValidLatitude(-90.0000001), "Just beyond South Pole should be invalid")

        // Well beyond boundaries
        assertFalse(isValidLatitude(100.0), "100° latitude should be invalid")
        assertFalse(isValidLatitude(-100.0), "-100° latitude should be invalid")
    }

    // ==================== Longitude Tests ====================

    @Test
    fun `valid longitude values should pass validation`() {
        val validLongitudes = listOf(
            0.0,            // Prime Meridian (Greenwich)
            21.0122,        // Warsaw, Poland
            151.2093,       // Sydney, Australia
            180.0,          // Date Line (East)
            -180.0,         // Date Line (West)
            -122.4194,      // San Francisco, USA
            139.6917,       // Tokyo, Japan
            0.0001,         // Near Prime Meridian
            -0.0001,        // Near Prime Meridian west
            179.9999,       // Near Date Line east
            -179.9999       // Near Date Line west
        )

        validLongitudes.forEach { lon ->
            assertTrue(
                isValidLongitude(lon),
                "Longitude $lon should be valid"
            )
        }
    }

    @Test
    fun `invalid longitude values should fail validation`() {
        val invalidLongitudes = listOf(
            180.1,                      // Beyond date line
            -180.1,                     // Beyond date line
            181.0,                      // Out of range
            -181.0,                     // Out of range
            360.0,                      // Full circle (invalid)
            -360.0,                     // Full circle (invalid)
            200.0,                      // Completely invalid
            -200.0,                     // Completely invalid
            Double.NaN,                 // Not a Number
            Double.POSITIVE_INFINITY,   // Infinity
            Double.NEGATIVE_INFINITY    // Negative infinity
        )

        invalidLongitudes.forEach { lon ->
            assertFalse(
                isValidLongitude(lon),
                "Longitude $lon should be invalid"
            )
        }
    }

    @Test
    fun `longitude boundary values should be handled correctly`() {
        // Exactly at boundaries
        assertTrue(isValidLongitude(180.0), "180° longitude should be valid")
        assertTrue(isValidLongitude(-180.0), "-180° longitude should be valid")

        // Just beyond boundaries
        assertFalse(isValidLongitude(180.0000001), "Just beyond 180° should be invalid")
        assertFalse(isValidLongitude(-180.0000001), "Just beyond -180° should be invalid")

        // Well beyond boundaries
        assertFalse(isValidLongitude(200.0), "200° longitude should be invalid")
        assertFalse(isValidLongitude(-200.0), "-200° longitude should be invalid")
    }

    // ==================== Special Cases ====================

    @Test
    fun `zero coordinates (Gulf of Guinea) should be valid`() {
        assertTrue(isValidLatitude(0.0), "Latitude 0° should be valid")
        assertTrue(isValidLongitude(0.0), "Longitude 0° should be valid")
    }

    @Test
    fun `pole coordinates should have valid latitude with any longitude`() {
        // At poles, longitude is technically undefined but we accept any valid longitude
        assertTrue(isValidLatitude(90.0), "North Pole latitude should be valid")
        assertTrue(isValidLatitude(-90.0), "South Pole latitude should be valid")

        // Any longitude is valid (even though it doesn't matter at poles)
        assertTrue(isValidLongitude(0.0), "Longitude 0° at pole should be valid")
        assertTrue(isValidLongitude(180.0), "Longitude 180° at pole should be valid")
    }

    @Test
    fun `date line coordinates should be valid`() {
        // International Date Line (both representations)
        assertTrue(isValidLongitude(180.0), "180° (date line east) should be valid")
        assertTrue(isValidLongitude(-180.0), "-180° (date line west) should be valid")
    }

    @Test
    fun `real-world city coordinates should all be valid`() {
        val cities = mapOf(
            "Warsaw" to Pair(52.2297, 21.0122),
            "Tokyo" to Pair(35.6762, 139.6503),
            "New York" to Pair(40.7128, -74.0060),
            "Sydney" to Pair(-33.8688, 151.2093),
            "Rio de Janeiro" to Pair(-22.9068, -43.1729),
            "London" to Pair(51.5074, -0.1278),
            "Cape Town" to Pair(-33.9249, 18.4241),
            "Moscow" to Pair(55.7558, 37.6173),
            "Singapore" to Pair(1.3521, 103.8198),
            "Reykjavik" to Pair(64.1466, -21.9426)
        )

        cities.forEach { (city, coords) ->
            assertTrue(
                isValidLatitude(coords.first),
                "$city latitude (${coords.first}) should be valid"
            )
            assertTrue(
                isValidLongitude(coords.second),
                "$city longitude (${coords.second}) should be valid"
            )
        }
    }

    @Test
    fun `extreme but valid coordinates should pass`() {
        // Northernmost permanently inhabited place (Alert, Canada)
        assertTrue(isValidLatitude(82.5018))
        assertTrue(isValidLongitude(-62.3481))

        // Southernmost permanently inhabited place (Amundsen-Scott Station)
        assertTrue(isValidLatitude(-90.0))  // Exactly at South Pole
        assertTrue(isValidLongitude(0.0))

        // Westernmost point in Alaska
        assertTrue(isValidLatitude(51.3544))
        assertTrue(isValidLongitude(-179.7767))

        // Easternmost point in Kiribati
        assertTrue(isValidLatitude(-3.8))
        assertTrue(isValidLongitude(-150.2))
    }

    @Test
    fun `common GPS errors should be detected`() {
        // GPS device returned null/NaN
        assertFalse(isValidLatitude(Double.NaN))
        assertFalse(isValidLongitude(Double.NaN))

        // GPS device returned infinity (overflow error)
        assertFalse(isValidLatitude(Double.POSITIVE_INFINITY))
        assertFalse(isValidLongitude(Double.POSITIVE_INFINITY))

        // Coordinates swapped (longitude in latitude field)
        assertFalse(isValidLatitude(150.0), "Longitude value (150°) in latitude field should be invalid")

        // Out of range due to calculation error
        assertFalse(isValidLatitude(95.0), "Out of range latitude should be invalid")
        assertFalse(isValidLongitude(185.0), "Out of range longitude should be invalid")
    }
}
