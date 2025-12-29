package org.cityxplore.backend.poi.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.dto.CreatePoiPublicRequest
import org.cityxplore.backend.poi.dto.CreatePoiRequest
import org.cityxplore.backend.poi.dto.UpdatePoiRequest
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class PointOfInterestServiceTest {

    private lateinit var poiRepository: PointOfInterestRepository
    private lateinit var userPoiDiscoveryRepository: UserPoiDiscoveryRepository
    private lateinit var poiService: PointOfInterestService
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @BeforeEach
    fun setUp() {
        poiRepository = mockk()
        userPoiDiscoveryRepository = mockk()
        poiService = PointOfInterestService(poiRepository, userPoiDiscoveryRepository)
    }

    @Test
    fun `getAll should return list of active POIs`() {
        // Given
        val location1 = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val location2 = geometryFactory.createPoint(Coordinate(21.1, 52.1))

        val poi1 = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            location = location1,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val poi2 = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Park",
            description = "City park",
            category = "Nature",
            location = location2,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiRepository.findAllByIsActiveTrue() } returns listOf(poi1, poi2)

        // When
        val result = poiService.getAll()

        // Then
        assertEquals(2, result.size)
        assertEquals("Museum", result[0].name)
        assertEquals("Park", result[1].name)
        verify(exactly = 1) { poiRepository.findAllByIsActiveTrue() }
    }

    @Test
    fun `getAll should return empty list when no active POIs`() {
        // Given
        every { poiRepository.findAllByIsActiveTrue() } returns emptyList()

        // When
        val result = poiService.getAll()

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { poiRepository.findAllByIsActiveTrue() }
    }

    @Test
    fun `getById should return POI when found`() {
        // Given
        val poiId = UUID.randomUUID()
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val poi = PointOfInterest(
            id = poiId,
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            location = location,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiRepository.findByIdIsActiveTrue(poiId) } returns poi
        // No authenticated user, so existsByUserIdAndPoiId should not be called

        // When
        val result = poiService.getById(poiId)

        // Then
        assertEquals(poiId, result.id)
        assertEquals("Museum", result.name)
        assertEquals("Culture", result.category)
        assertEquals(null, result.isDiscovered) // Not authenticated = null
        verify(exactly = 1) { poiRepository.findByIdIsActiveTrue(poiId) }
    }

    @Test
    fun `getById should throw 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiRepository.findByIdIsActiveTrue(poiId) } returns null

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiService.getById(poiId)
        }

        assertEquals("404 NOT_FOUND \"POI not found\"", exception.message)
        verify(exactly = 1) { poiRepository.findByIdIsActiveTrue(poiId) }
    }

    @Test
    fun `create should save POI and return response`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "New Museum",
            description = "Brand new museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("opening_hours" to "9-17")
        )

        val savedPoi = PointOfInterest(
            id = UUID.randomUUID(),
            name = request.name,
            description = request.description,
            category = request.category,
            location = geometryFactory.createPoint(Coordinate(request.longitude!!, request.latitude!!)),
            metadata = request.metadata,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiRepository.save(any<PointOfInterest>()) } returns savedPoi

        // When
        val result = poiService.create(request)

        // Then
        assertEquals("New Museum", result.name)
        assertEquals("Culture", result.category)
        assertEquals(52.0, result.latitude)
        assertEquals(21.0, result.longitude)
        verify(exactly = 1) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `getAllPois should return all POIs including inactive`() {
        // Given
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val activePoi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Active POI",
            description = "Active",
            category = "Culture",
            location = location,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val inactivePoi = PointOfInterest(
            id = UUID.randomUUID(),
            name = "Inactive POI",
            description = "Inactive",
            category = "Culture",
            location = location,
            isActive = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiRepository.findAll() } returns listOf(activePoi, inactivePoi)

        // When
        val result = poiService.getAllPois()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.isActive == true })
        assertTrue(result.any { it.isActive == false })
        verify(exactly = 1) { poiRepository.findAll() }
    }

    @Test
    fun `getPoiById should return POI admin response when found`() {
        // Given
        val poiId = UUID.randomUUID()
        val location = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val poi = PointOfInterest(
            id = poiId,
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            location = location,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiRepository.findById(poiId) } returns Optional.of(poi)

        // When
        val result = poiService.getPoiById(poiId)

        // Then
        assertEquals(poiId, result.id)
        assertEquals("Museum", result.name)
        assertEquals(true, result.isActive)
        verify(exactly = 1) { poiRepository.findById(poiId) }
    }

    @Test
    fun `getPoiById should throw 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiRepository.findById(poiId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiService.getPoiById(poiId)
        }

        assertEquals("404 NOT_FOUND \"POI not found\"", exception.message)
        verify(exactly = 1) { poiRepository.findById(poiId) }
    }

    @Test
    fun `createPoi should create POI with valid coordinates`() {
        // Given
        val request = CreatePoiRequest(
            name = "New POI",
            description = "Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("key" to "value")
        )

        val savedPoi = PointOfInterest(
            id = UUID.randomUUID(),
            name = request.name,
            description = request.description,
            category = request.category,
            location = geometryFactory.createPoint(Coordinate(request.longitude, request.latitude)),
            metadata = request.metadata,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiRepository.save(any<PointOfInterest>()) } returns savedPoi

        // When
        val result = poiService.createPoi(request)

        // Then
        assertEquals("New POI", result.name)
        assertEquals("Culture", result.category)
        assertEquals(52.0, result.latitude)
        assertEquals(21.0, result.longitude)
        verify(exactly = 1) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `createPoi should throw exception for invalid latitude`() {
        // Given
        val request = CreatePoiRequest(
            name = "Invalid POI",
            description = "Description",
            category = "Culture",
            latitude = 100.0, // Invalid: > 90
            longitude = 21.0,
            metadata = null
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            poiService.createPoi(request)
        }

        assertEquals("Invalid coordinates", exception.message)
        verify(exactly = 0) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `createPoi should throw exception for invalid longitude`() {
        // Given
        val request = CreatePoiRequest(
            name = "Invalid POI",
            description = "Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 200.0, // Invalid: > 180
            metadata = null
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            poiService.createPoi(request)
        }

        assertEquals("Invalid coordinates", exception.message)
        verify(exactly = 0) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `updatePoi should update existing POI`() {
        // Given
        val poiId = UUID.randomUUID()
        val existingLocation = geometryFactory.createPoint(Coordinate(21.0, 52.0))
        val existingPoi = PointOfInterest(
            id = poiId,
            name = "Old Name",
            description = "Old Description",
            category = "OldCategory",
            location = existingLocation,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val updateRequest = UpdatePoiRequest(
            name = "Updated Name",
            description = "Updated Description",
            category = "UpdatedCategory",
            latitude = 52.5,
            longitude = 21.5,
            metadata = mapOf("updated" to "true")
        )

        every { poiRepository.findById(poiId) } returns Optional.of(existingPoi)
        every { poiRepository.save(any<PointOfInterest>()) } returns existingPoi

        // When
        val result = poiService.updatePoi(poiId, updateRequest)

        // Then
        assertEquals("Updated Name", result.name)
        assertEquals("UpdatedCategory", result.category)
        assertEquals(52.5, result.latitude)
        assertEquals(21.5, result.longitude)
        verify(exactly = 1) { poiRepository.findById(poiId) }
        verify(exactly = 1) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `updatePoi should throw 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        val updateRequest = UpdatePoiRequest(
            name = "Updated",
            description = "Updated",
            category = "Updated",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null
        )

        every { poiRepository.findById(poiId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiService.updatePoi(poiId, updateRequest)
        }

        assertEquals("404 NOT_FOUND \"POI not found\"", exception.message)
        verify(exactly = 1) { poiRepository.findById(poiId) }
        verify(exactly = 0) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `updatePoi should throw exception for invalid coordinates`() {
        // Given
        val poiId = UUID.randomUUID()
        val existingPoi = PointOfInterest(
            id = poiId,
            name = "POI",
            description = "Description",
            category = "Category",
            location = geometryFactory.createPoint(Coordinate(21.0, 52.0)),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val updateRequest = UpdatePoiRequest(
            name = "Updated",
            description = "Updated",
            category = "Updated",
            latitude = 95.0, // Invalid
            longitude = 21.0,
            metadata = null
        )

        every { poiRepository.findById(poiId) } returns Optional.of(existingPoi)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            poiService.updatePoi(poiId, updateRequest)
        }

        assertEquals("Invalid coordinates", exception.message)
        verify(exactly = 1) { poiRepository.findById(poiId) }
        verify(exactly = 0) { poiRepository.save(any<PointOfInterest>()) }
    }

    @Test
    fun `deletePoi should delete existing POI`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiRepository.existsById(poiId) } returns true
        every { poiRepository.deleteById(poiId) } just Runs

        // When
        poiService.deletePoi(poiId)

        // Then
        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 1) { poiRepository.deleteById(poiId) }
    }

    @Test
    fun `deletePoi should throw 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiRepository.existsById(poiId) } returns false

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiService.deletePoi(poiId)
        }

        assertEquals("404 NOT_FOUND \"POI not found\"", exception.message)
        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 0) { poiRepository.deleteById(any()) }
    }
}
