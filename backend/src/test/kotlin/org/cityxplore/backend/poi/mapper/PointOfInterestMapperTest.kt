package org.cityxplore.backend.poi.mapper

import org.cityxplore.backend.poi.dto.CreatePoiPublicRequest
import org.cityxplore.backend.poi.entity.PoiImage
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `toResponseDto should map PointOfInterest to PoiResponse`() {
        // Given
        val poiId = UUID.randomUUID()
        val creationDate = LocalDateTime.now()
        val updateDate = LocalDateTime.now()
        val poi = PointOfInterest(
            id = poiId,
            name = "Test POI",
            description = "Description",
            category = "Category",
            location = geometryFactory.createPoint(Coordinate(20.0, 10.0)),
            metadata = mapOf("key" to "value"),
            imageUrls = arrayOf(PoiImage(url = "http://example.com/img.jpg")),
            createdAt = creationDate,
            updatedAt = updateDate,
            isActive = true
        )

        // When
        val result = poi.toResponseDto()

        // Then
        assertEquals(poiId, result.id)
        assertEquals("Test POI", result.name)
        assertEquals("Description", result.description)
        assertEquals("Category", result.category)
        assertEquals(10.0, result.latitude)
        assertEquals(20.0, result.longitude)
        assertEquals(mapOf("key" to "value"), result.metadata)
        assertEquals(listOf(PoiImage(url = "http://example.com/img.jpg")), result.imageUrls)
        assertEquals(creationDate, result.createdAt)
        assertEquals(updateDate, result.updatedAt)
    }

    @Test
    fun `toResponseDto should handle null fields`() {
        // Given
        val poi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Test POI",
            description = null,
            category = "Category",
            location = null,
            metadata = null,
            imageUrls = null,
            createdAt = null,
            updatedAt = null,
            isActive = true
        )

        // When
        val result = poi.toResponseDto()

        // Then
        assertEquals(null, result.description)
        assertEquals(null, result.latitude)
        assertEquals(null, result.longitude)
        assertEquals(null, result.metadata)
        assertEquals(null, result.imageUrls)
        assertEquals(null, result.createdAt)
        assertEquals(null, result.updatedAt)
    }

    @Test
    fun `toResponseDtoList should map list of POIs`() {
        // Given
        val poi1 = PointOfInterest(
            id = UUID.randomUUID(),
            name = "POI 1",
            category = "Cat 1",
            location = geometryFactory.createPoint(Coordinate(20.0, 10.0)),
            imageUrls = arrayOf(PoiImage(url = "url1"))
        )
        val poi2 = PointOfInterest(
            id = UUID.randomUUID(),
            name = "POI 2",
            category = "Cat 2",
            location = geometryFactory.createPoint(Coordinate(30.0, 15.0)),
            imageUrls = arrayOf(PoiImage(url = "url2"))
        )
        val list = listOf(poi1, poi2)

        // When
        val result = list.toResponseDtoList()

        // Then
        assertEquals(2, result.size)
        assertEquals("POI 1", result[0].name)
        assertEquals("url1", result[0].imageUrls?.get(0)?.url)
        assertEquals("POI 2", result[1].name)
        assertEquals("url2", result[1].imageUrls?.get(0)?.url)
    }

    @Test
    fun `toResponseDtoList should return empty list for empty input`() {
        // Given
        val emptyList = emptyList<PointOfInterest>()

        // When
        val result = emptyList.toResponseDtoList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `toEntity should map CreatePoiPublicRequest to PointOfInterest`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "New POI",
            description = "Desc",
            category = "New Cat",
            latitude = 10.0,
            longitude = 20.0,
            metadata = mapOf("a" to 1),
            imageUrls = listOf(PoiImage(url = "http://example.com"))
        )

        // When
        val result = request.toEntity()

        // Then
        assertEquals("New POI", result.name)
        assertEquals("Desc", result.description)
        assertEquals("New Cat", result.category)
        assertEquals(10.0, result.location?.y)
        assertEquals(20.0, result.location?.x)
        assertEquals(mapOf("a" to 1), result.metadata)
        assertTrue(result.imageUrls?.contentEquals(arrayOf(PoiImage(url = "http://example.com"))) == true)
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
            description = "Admin Desc",
            category = "Protected",
            location = location,
            metadata = mapOf("confidential" to true),
            imageUrls = arrayOf(PoiImage(url = "http://admin.com")),
            createdAt = now,
            updatedAt = now
        )

        // When
        val result = poi.toAdminDto()

        // Then
        assertEquals(poiId, result.id)
        assertEquals("Admin POI", result.name)
        assertEquals("Admin Desc", result.description)
        assertEquals("Protected", result.category)
        assertEquals(52.0, result.latitude)
        assertEquals(21.0, result.longitude)
        assertEquals(mapOf("confidential" to true), result.metadata)
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
