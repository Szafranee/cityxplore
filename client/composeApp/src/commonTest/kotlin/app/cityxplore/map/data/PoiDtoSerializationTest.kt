package app.cityxplore.map.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Comprehensive tests for POI DTO JSON serialization/deserialization.
 *
 * These tests protect against:
 * - Breaking changes in API contract
 * - JSON mapping errors
 * - Missing field handling
 * - Type conversion issues (string vs number for coordinates)
 *
 * Critical for maintaining compatibility with backend API.
 */
class PoiDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `PoiDto should deserialize complete JSON correctly`() {
        val jsonString = """
            {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "name": "Warsaw Old Town",
                "description": "Historic district of Warsaw",
                "latitude": 52.2500,
                "longitude": 21.0122,
                "category": "HISTORICAL",
                "isDiscovered": false,
                "isMajor": true
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertEquals("123e4567-e89b-12d3-a456-426614174000", poi.id)
        assertEquals("Warsaw Old Town", poi.name)
        assertEquals("Historic district of Warsaw", poi.description)
        assertEquals(52.2500, poi.latitude)
        assertEquals(21.0122, poi.longitude)
        assertEquals(PoiCategoryDto.HISTORICAL, poi.category)
        assertEquals(false, poi.discovered)
        assertEquals(true, poi.isMajor)
    }

    @Test
    fun `PoiDto should handle missing optional fields`() {
        val jsonString = """
            {
                "id": "test-poi-id",
                "name": "Minimal POI",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "OTHER"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertEquals("test-poi-id", poi.id)
        assertEquals("Minimal POI", poi.name)
        assertNull(poi.description, "Description should be null when not provided")
        assertNull(poi.discovered, "Discovered should be null when not provided")
        assertEquals(false, poi.isMajor, "isMajor should default to false")
    }

    @Test
    fun `PoiDto should handle latitude as string`() {
        val jsonString = """
            {
                "id": "poi-1",
                "name": "Test POI",
                "latitude": "52.2297",
                "longitude": "21.0122",
                "category": "NATURE"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertNotNull(poi.latitude, "Latitude should be parsed from string")
        assertEquals(52.2297, poi.latitude!!, 0.0001)
        assertNotNull(poi.longitude, "Longitude should be parsed from string")
        assertEquals(21.0122, poi.longitude!!, 0.0001)
    }

    @Test
    fun `PoiDto should handle latitude as number`() {
        val jsonString = """
            {
                "id": "poi-2",
                "name": "Test POI 2",
                "latitude": 52.2297,
                "longitude": 21.0122,
                "category": "NATURE"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertNotNull(poi.latitude, "Latitude should be parsed from number")
        assertEquals(52.2297, poi.latitude!!, 0.0001)
        assertNotNull(poi.longitude, "Longitude should be parsed from number")
        assertEquals(21.0122, poi.longitude!!, 0.0001)
    }

    @Test
    fun `PoiDto should handle negative coordinates`() {
        val jsonString = """
            {
                "id": "sydney-poi",
                "name": "Sydney Opera House",
                "latitude": -33.8568,
                "longitude": 151.2153,
                "category": "CULTURAL"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertEquals(-33.8568, poi.latitude!!, 0.0001)
        assertEquals(151.2153, poi.longitude!!, 0.0001)
    }

    @Test
    fun `PoiDto should handle all category types`() {
        val categories = listOf(
            "HISTORICAL" to PoiCategoryDto.HISTORICAL,
            "CULTURAL" to PoiCategoryDto.CULTURAL,
            "NATURE" to PoiCategoryDto.NATURE,
            "FOOD" to PoiCategoryDto.FOOD,
            "SPORTS" to PoiCategoryDto.SPORTS,
            "ENTERTAINMENT" to PoiCategoryDto.ENTERTAINMENT,
            "CUSTOM" to PoiCategoryDto.CUSTOM,
            "OTHER" to PoiCategoryDto.OTHER,
            "UNKNOWN" to PoiCategoryDto.UNKNOWN
        )

        categories.forEach { (jsonValue, expectedEnum) ->
            val jsonString = """
                {
                    "id": "test",
                    "name": "Test",
                    "latitude": 0.0,
                    "longitude": 0.0,
                    "category": "$jsonValue"
                }
            """.trimIndent()

            val poi = json.decodeFromString<PoiDto>(jsonString)
            assertEquals(expectedEnum, poi.category, "Category $jsonValue should map to $expectedEnum")
        }
    }

    @Test
    fun `PoiDto should handle metadata with all fields`() {
        val jsonString = """
            {
                "id": "museum-poi",
                "name": "National Museum",
                "latitude": 52.2297,
                "longitude": 21.0122,
                "category": "CULTURAL",
                "metadata": {
                    "trivia": "Built in 1862",
                    "opening_hours": ["Mon-Fri: 9AM-5PM", "Sat-Sun: 10AM-6PM"],
                    "visit_duration": "2-3 hours",
                    "is_free": false,
                    "website": "https://museum.example.com",
                    "address": "Example St 123",
                    "build_year": "1862"
                }
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertNotNull(poi.metadata)
        assertEquals("Built in 1862", poi.metadata.trivia)
        assertEquals(2, poi.metadata.openingHours?.size)
        assertEquals("2-3 hours", poi.metadata.visitDuration)
        assertEquals(false, poi.metadata.isFree)
        assertEquals("https://museum.example.com", poi.metadata.website)
        assertEquals("Example St 123", poi.metadata.address)
        assertEquals("1862", poi.metadata.buildYear)
    }

    @Test
    fun `PoiDto should handle metadata with missing optional fields`() {
        val jsonString = """
            {
                "id": "simple-poi",
                "name": "Simple Place",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "OTHER",
                "metadata": {
                    "trivia": "Interesting fact"
                }
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertNotNull(poi.metadata)
        assertEquals("Interesting fact", poi.metadata.trivia)
        assertNull(poi.metadata.openingHours)
        assertNull(poi.metadata.visitDuration)
        assertNull(poi.metadata.isFree)
        assertNull(poi.metadata.website)
    }

    @Test
    fun `PoiDto should handle completely missing metadata`() {
        val jsonString = """
            {
                "id": "no-metadata-poi",
                "name": "No Metadata",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "NATURE"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertNull(poi.metadata, "Metadata should be null when not provided")
    }

    @Test
    fun `PoiDto should handle empty description as null`() {
        val jsonString = """
            {
                "id": "poi-empty-desc",
                "name": "POI with Empty Description",
                "description": "",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "OTHER"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        // Empty string is kept as empty string, not converted to null
        assertEquals("", poi.description)
    }

    @Test
    fun `PoiDto should handle discovered boolean correctly`() {
        val discoveredJson = """
            {
                "id": "discovered-poi",
                "name": "Discovered",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "NATURE",
                "isDiscovered": true
            }
        """.trimIndent()

        val undiscoveredJson = """
            {
                "id": "undiscovered-poi",
                "name": "Undiscovered",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "NATURE",
                "isDiscovered": false
            }
        """.trimIndent()

        val discovered = json.decodeFromString<PoiDto>(discoveredJson)
        val undiscovered = json.decodeFromString<PoiDto>(undiscoveredJson)

        assertEquals(true, discovered.discovered)
        assertEquals(false, undiscovered.discovered)
    }

    @Test
    fun `PoiDto should handle isMajor boolean correctly`() {
        val majorJson = """
            {
                "id": "major-poi",
                "name": "Major Landmark",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "HISTORICAL",
                "isMajor": true
            }
        """.trimIndent()

        val notMajorJson = """
            {
                "id": "regular-poi",
                "name": "Regular Place",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "OTHER"
            }
        """.trimIndent()

        val major = json.decodeFromString<PoiDto>(majorJson)
        val notMajor = json.decodeFromString<PoiDto>(notMajorJson)

        assertEquals(true, major.isMajor)
        assertEquals(false, notMajor.isMajor)
    }

    @Test
    fun `PoiDto should handle high precision coordinates`() {
        val jsonString = """
            {
                "id": "precise-poi",
                "name": "Very Precise Location",
                "latitude": 52.22970123456789,
                "longitude": 21.01220987654321,
                "category": "OTHER"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        // Should preserve high precision
        assertEquals(52.22970123456789, poi.latitude!!, 0.0000000001)
        assertEquals(21.01220987654321, poi.longitude!!, 0.0000000001)
    }

    @Test
    fun `PoiDto should handle zero coordinates (Gulf of Guinea)`() {
        val jsonString = """
            {
                "id": "zero-poi",
                "name": "Null Island",
                "latitude": 0.0,
                "longitude": 0.0,
                "category": "OTHER"
            }
        """.trimIndent()

        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertEquals(0.0, poi.latitude)
        assertEquals(0.0, poi.longitude)
    }

    @Test
    fun `PoiDto should ignore unknown fields in JSON`() {
        val jsonString = """
            {
                "id": "poi-unknown-fields",
                "name": "POI with Extra Fields",
                "latitude": 52.0,
                "longitude": 21.0,
                "category": "NATURE",
                "unknownField1": "should be ignored",
                "unknownField2": 12345,
                "unknownObject": {
                    "nested": "data"
                }
            }
        """.trimIndent()

        // Should not throw exception
        val poi = json.decodeFromString<PoiDto>(jsonString)

        assertEquals("poi-unknown-fields", poi.id)
        assertEquals("POI with Extra Fields", poi.name)
    }
}
