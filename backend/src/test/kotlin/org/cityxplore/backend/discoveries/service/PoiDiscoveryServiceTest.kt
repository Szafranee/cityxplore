package org.cityxplore.backend.discoveries.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

class PoiDiscoveryServiceTest {

    private lateinit var poiRepository: PointOfInterestRepository
    private lateinit var userPoiRepository: UserPoiDiscoveryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var poiDiscoveryService: PoiDiscoveryService

    @BeforeEach
    fun setUp() {
        poiRepository = mockk()
        userPoiRepository = mockk()
        userRepository = mockk()
        poiDiscoveryService = PoiDiscoveryService(
            poiRepository,
            userPoiRepository,
            userRepository
        )
    }

    @Test
    fun `discoverPoi should create discovery when POI exists and not yet discovered`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = LocalDateTime.now(),
            isFavorite = false
        )

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiRepository.save(any()) } returns discovery
        every { userRepository.incrementPoisDiscovered(userId) } returns 1

        // When
        val result = poiDiscoveryService.discoverPoi(userId, poiId)

        // Then
        assertNotNull(result)
        assertEquals(poiId, result.poiId)
        assertEquals(false, result.favorite)
        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 1) { userPoiRepository.save(any()) }
        verify(exactly = 1) { userRepository.incrementPoisDiscovered(userId) }
    }

    @Test
    fun `discoverPoi should throw ResponseStatusException when POI does not exist`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every { poiRepository.existsById(poiId) } returns false

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiDiscoveryService.discoverPoi(userId, poiId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("POI not found", exception.reason)
        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 0) { userPoiRepository.save(any()) }
    }

    @Test
    fun `discoverPoi should throw ResponseStatusException when POI already discovered`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiRepository.save(any()) } throws DataIntegrityViolationException("Duplicate")

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiDiscoveryService.discoverPoi(userId, poiId)
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals("Already discovered", exception.reason)
        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 1) { userPoiRepository.save(any()) }
    }

    @Test
    fun `discoverPoi should throw ResponseStatusException when user not found after discovery`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = LocalDateTime.now(),
            isFavorite = false
        )

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiRepository.save(any()) } returns discovery
        every { userRepository.incrementPoisDiscovered(userId) } returns 0

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiDiscoveryService.discoverPoi(userId, poiId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("User not found", exception.reason)
        verify(exactly = 1) { userRepository.incrementPoisDiscovered(userId) }
    }

    @Test
    fun `getUserDiscoveries should return list of discoveries for user`() {
        // Given
        val userId = UUID.randomUUID()
        val discoveries = listOf(
            UserPoiDiscovery(
                id = UUID.randomUUID(),
                userId = userId,
                poiId = UUID.randomUUID(),
                discoveredAt = LocalDateTime.now().minusDays(2),
                isFavorite = true
            ),
            UserPoiDiscovery(
                id = UUID.randomUUID(),
                userId = userId,
                poiId = UUID.randomUUID(),
                discoveredAt = LocalDateTime.now().minusDays(1),
                isFavorite = false
            )
        )

        every { userPoiRepository.findAllByUserId(userId) } returns discoveries

        // When
        val result = poiDiscoveryService.getUserDiscoveries(userId)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(discoveries[0].poiId, result[0].poiId)
        assertEquals(true, result[0].favorite)
        assertEquals(discoveries[1].poiId, result[1].poiId)
        assertEquals(false, result[1].favorite)
        verify(exactly = 1) { userPoiRepository.findAllByUserId(userId) }
    }

    @Test
    fun `getUserDiscoveries should return empty list when user has no discoveries`() {
        // Given
        val userId = UUID.randomUUID()

        every { userPoiRepository.findAllByUserId(userId) } returns emptyList()

        // When
        val result = poiDiscoveryService.getUserDiscoveries(userId)

        // Then
        assertNotNull(result)
        assertTrue(result.isEmpty())
        verify(exactly = 1) { userPoiRepository.findAllByUserId(userId) }
    }

    @Test
    fun `getUserDiscovery should return discovery when found`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = LocalDateTime.now(),
            isFavorite = true
        )

        every { userPoiRepository.findByUserIdAndPoiId(userId, poiId) } returns discovery

        // When
        val result = poiDiscoveryService.getUserDiscovery(userId, poiId)

        // Then
        assertNotNull(result)
        assertEquals(poiId, result.poiId)
        assertEquals(true, result.favorite)
        verify(exactly = 1) { userPoiRepository.findByUserIdAndPoiId(userId, poiId) }
    }

    @Test
    fun `getUserDiscovery should throw ResponseStatusException when discovery not found`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every { userPoiRepository.findByUserIdAndPoiId(userId, poiId) } returns null

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            poiDiscoveryService.getUserDiscovery(userId, poiId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("Discovery not found", exception.reason)
        verify(exactly = 1) { userPoiRepository.findByUserIdAndPoiId(userId, poiId) }
    }

    @Test
    fun `discoverPoi should correctly map response`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()

        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = true
        )

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiRepository.save(any()) } returns discovery
        every { userRepository.incrementPoisDiscovered(userId) } returns 1

        // When
        val result = poiDiscoveryService.discoverPoi(userId, poiId)

        // Then
        assertNotNull(result)
        assertEquals(poiId, result.poiId)
        assertEquals(true, result.favorite)
        assertEquals(discoveredAt, result.discoveredAt)
    }
}
