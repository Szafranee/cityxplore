package org.cityxplore.backend.poi.mapper

import org.cityxplore.backend.poi.dto.CreatePoiPublicRequest
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDateTime
import java.util.UUID

class PointOfInterestMapperTest {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @Test
    fun `toResponseDto should map POI entity to PoiResponse`() {
        // Given
        val poiId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))

        val poi = PointOfInterest(
            id = poiId,
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            location = location,
            metadata = mapOf("opening_hours" to "9-17"),
            imageUrls = listOf("https://example.com/img1.jpg"),
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        // When
        val result = poi.toResponseDto()

        // Then
        assertEquals(poiId, result.id)
        assertEquals("Museum", result.name)
        assertEquals("Historic museum", result.description)
        assertEquals("Culture", result.category)
        assertEquals(52.0, result.latitude)
        assertEquals(21.0, result.longitude)
        assertEquals(mapOf("opening_hours" to "9-17"), result.metadata)
        assertEquals(listOf("https://example.com/img1.jpg"), result.imageUrls)
        assertEquals(true, result.isActive)
        assertEquals(now, result.createdAt)
        assertEquals(now, result.updatedAt)
    }

    @Test
    fun `toResponseDto should handle null location`() {
        // Given
        val poi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "POI without location",
            description = "Description",
            category = "Category",
            location = null,
            isActive = true
        )

        // When
        val result = poi.toResponseDto()

        // Then
        assertNull(result.latitude)
        assertNull(result.longitude)
        assertEquals("POI without location", result.name)
    }

    @Test
    fun `toResponseDto should handle null optional fields`() {
        // Given
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val poi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Minimal POI",
            description = null,
            category = "Category",
            location = location,
            metadata = null,
            imageUrls = null,
            isActive = true
        )

        // When
        val result = poi.toResponseDto()

        // Then
        assertNull(result.description)
        assertNull(result.metadata)
        assertNull(result.imageUrls)
        assertEquals("Minimal POI", result.name)
    }

    @Test
    fun `toResponseDtoList should map list of POI entities`() {
        // Given
        val location1 = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val location2 = geometryFactory.createPoint(Coordinate(21.1, 52.1))

        val poi1 = PointOfInterest(
            id = UUID.randomUUID(),
            name = "POI 1",
            description = "Description 1",
            category = "Category1",
            location = location1,
            isActive = true
        )

        val poi2 = PointOfInterest(
            id = UUID.randomUUID(),
            name = "POI 2",
            description = "Description 2",
            category = "Category2",
            location = location2,
            isActive = true
        )

        val poiList = listOf(poi1, poi2)

        // When
        val result = poiList.toResponseDtoList()

        // Then
        assertEquals(2, result.size)
        assertEquals("POI 1", result[0].name)
        assertEquals("POI 2", result[1].name)
        assertEquals(52.0, result[0].latitude)
        assertEquals(52.1, result[1].latitude)
    }

    @Test
    fun `toResponseDtoList should return empty list for empty input`() {
        // Given
        val emptyList = emptyList<PointOfInterest>()

        // When
        val result = emptyList.toResponseDtoList()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toEntity should map CreatePoiPublicRequest to PointOfInterest`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "New POI",
            description = "New Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("key" to "value"),
            imageUrls = listOf("https://example.com/img.jpg")
        )

        // When
        val result = request.toEntity()

        // Then
        assertEquals("New POI", result.name)
        assertEquals("New Description", result.description)
        assertEquals("Culture", result.category)
        assertNotNull(result.location)
        assertEquals(52.0, result.location?.y)
        assertEquals(21.0, result.location?.x)
        assertEquals(mapOf("key" to "value"), result.metadata)
        assertEquals(listOf("https://example.com/img.jpg"), result.imageUrls)
    }

    @Test
    fun `toEntity should handle null location coordinates`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "POI without location",
            description = "Description",
            category = "Category",
            latitude = null,
            longitude = null,
            metadata = null,
            imageUrls = null
        )

        // When
        val result = request.toEntity()

        // Then
        assertNull(result.location)
        assertEquals("POI without location", result.name)
    }

    @Test
    fun `toEntity should throw exception for invalid latitude`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "Invalid POI",
            description = "Description",
            category = "Category",
            latitude = 100.0, // Invalid: > 90
            longitude = 21.0,
            metadata = null,
            imageUrls = null
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            request.toEntity()
        }

        assertEquals("Invalid lat/lon", exception.message)
    }

    @Test
    fun `toEntity should throw exception for invalid longitude`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "Invalid POI",
            description = "Description",
            category = "Category",
            latitude = 52.0,
            longitude = 200.0, // Invalid: > 180
            metadata = null,
            imageUrls = null
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            request.toEntity()
        }

        assertEquals("Invalid lat/lon", exception.message)
    }

    @Test
    fun `toEntity should handle partial location (only latitude)`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "Partial location",
            description = "Description",
            category = "Category",
            latitude = 52.0,
            longitude = null,
            metadata = null,
            imageUrls = null
        )

        // When
        val result = request.toEntity()

        // Then
        assertNull(result.location)
    }

    @Test
    fun `toAdminDto should map POI entity to PoiAdminResponse`() {
        // Given
        val poiId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))

        val poi = PointOfInterest(
            id = poiId,
            name = "Admin POI",
            description = "Admin Description",
            category = "AdminCategory",
            location = location,
            metadata = mapOf("admin_key" to "admin_value"),
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        // When
        val result = poi.toAdminDto()

        // Then
        assertEquals(poiId, result.id)
        assertEquals("Admin POI", result.name)
        assertEquals("Admin Description", result.description)
        assertEquals("AdminCategory", result.category)
        assertEquals(52.0, result.latitude)
        assertEquals(21.0, result.longitude)
        assertEquals(mapOf("admin_key" to "admin_value"), result.metadata)
        assertEquals(true, result.isActive)
        assertEquals(now, result.createdAt)
        assertEquals(now, result.updatedAt)
    }

    @Test
    fun `toAdminDto should throw exception when POI has no ID`() {
        // Given
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val poi = PointOfInterest(
            id = null,
            name = "POI without ID",
            description = "Description",
            category = "Category",
            location = location,
            isActive = true
        )

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            poi.toAdminDto()
        }

        assertEquals("POI must have an ID for admin response", exception.message)
    }

    @Test
    fun `toAdminDto should throw exception when POI has no location`() {
        // Given
        val poi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "POI without location",
            description = "Description",
            category = "Category",
            location = null,
            isActive = true
        )

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            poi.toAdminDto()
        }

        assertEquals("POI missing location", exception.message)
    }

    @Test
    fun `toAdminDto should handle inactive POI`() {
        // Given
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val poi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Inactive POI",
            description = "Description",
            category = "Category",
            location = location,
            isActive = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // When
        val result = poi.toAdminDto()

        // Then
        assertEquals(false, result.isActive)
        assertEquals("Inactive POI", result.name)
    }
}
