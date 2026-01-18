package org.cityxplore.backend.social.shared.service

import io.github.jan.supabase.SupabaseClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.cityxplore.backend.social.shared.dto.CustomPoiData
import org.cityxplore.backend.social.shared.dto.SharePoiRequest
import org.cityxplore.backend.social.shared.entity.SharedPoi
import org.cityxplore.backend.social.shared.repository.SharedPoiRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

/**
 * Unit tests for SharedPoiService.
 */
class SharedPoiServiceTest {

    private lateinit var sharedPoiRepository: SharedPoiRepository
    private lateinit var poiRepository: PointOfInterestRepository
    private lateinit var userRepository: UserRepository
    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var sharedPoiService: SharedPoiService

    @BeforeEach
    fun setup() {
        sharedPoiRepository = mockk()
        poiRepository = mockk()
        userRepository = mockk()
        friendshipRepository = mockk()
        supabaseClient = mockk()

        sharedPoiService = SharedPoiService(
            sharedPoiRepository = sharedPoiRepository,
            poiRepository = poiRepository,
            userRepository = userRepository,
            friendshipRepository = friendshipRepository,
            supabaseClient = supabaseClient
        )
    }

    // ========== sharePoi Tests ==========

    @Test
    fun `sharePoi should successfully share existing POI with friend`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val message = "Check out this place!"

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = poiId,
            customPoi = null,
            message = message
        )

        val savedSharedPoi = SharedPoi(
            id = UUID.randomUUID(),
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = message,
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { userRepository.existsById(recipientId) } returns true
        every { friendshipRepository.areFriends(sharerId, recipientId) } returns true
        every { sharedPoiRepository.countBySharerIdAndRecipientId(sharerId, recipientId) } returns 0
        every { poiRepository.existsById(poiId) } returns true
        every { sharedPoiRepository.save(any()) } returns savedSharedPoi
        every { userRepository.findById(any()) } returns Optional.empty()

        // When
        val result = sharedPoiService.sharePoi(sharerId, request)

        // Then
        assertNotNull(result)
        assertEquals(savedSharedPoi.id, result.id)
        assertEquals(sharerId, result.sharerId)
        assertEquals(recipientId, result.recipientId)
        assertEquals(poiId, result.poiId)
        assertEquals(message, result.message)
        assertNull(result.viewedAt)

        verify(exactly = 1) { userRepository.existsById(recipientId) }
        verify(exactly = 1) { friendshipRepository.areFriends(sharerId, recipientId) }
        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 1) { sharedPoiRepository.save(any()) }
    }

    @Test
    fun `sharePoi should successfully share custom POI with friend`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()

        val customPoi = CustomPoiData(
            name = "Hidden Gem",
            description = "A secret spot I found",
            category = "Hidden Spots",
            latitude = 52.2297,
            longitude = 21.0122
        )

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = null,
            customPoi = customPoi,
            message = "You'll love this place!"
        )

        val savedSharedPoi = SharedPoi(
            id = UUID.randomUUID(),
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = null,
            poiData = customPoi,
            message = "You'll love this place!",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { userRepository.existsById(recipientId) } returns true
        every { friendshipRepository.areFriends(sharerId, recipientId) } returns true
        every { sharedPoiRepository.countBySharerIdAndRecipientId(sharerId, recipientId) } returns 0
        every { sharedPoiRepository.save(any()) } returns savedSharedPoi
        every { userRepository.findById(any()) } returns Optional.empty()

        // When
        val result = sharedPoiService.sharePoi(sharerId, request)

        // Then
        assertNotNull(result)
        assertEquals(savedSharedPoi.id, result.id)
        assertNull(result.poiId)
        assertNotNull(result.poiData)
        assertEquals(customPoi.name, result.poiData?.name)
        assertEquals(customPoi.latitude, result.poiData?.latitude)
        assertEquals(customPoi.longitude, result.poiData?.longitude)

        verify(exactly = 1) { userRepository.existsById(recipientId) }
        verify(exactly = 1) { friendshipRepository.areFriends(sharerId, recipientId) }
        verify(exactly = 0) { poiRepository.existsById(any()) }
        verify(exactly = 1) { sharedPoiRepository.save(any()) }
    }

    @Test
    fun `sharePoi should throw exception when both poiId and customPoi are provided`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val customPoi = CustomPoiData(
            name = "Test",
            description = "Test",
            category = "Test",
            latitude = 52.2297,
            longitude = 21.0122
        )

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = poiId,
            customPoi = customPoi,
            message = null
        )

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.sharePoi(sharerId, request)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("Exactly one of poiId or customPoi must be provided"))
    }

    @Test
    fun `sharePoi should throw exception when neither poiId nor customPoi are provided`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = null,
            customPoi = null,
            message = null
        )

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.sharePoi(sharerId, request)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("Exactly one of poiId or customPoi must be provided"))
    }

    @Test
    fun `sharePoi should throw exception when trying to share with yourself`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val request = SharePoiRequest(
            recipientId = userId,
            poiId = poiId,
            customPoi = null,
            message = null
        )

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.sharePoi(userId, request)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("Cannot share with yourself"))
    }

    @Test
    fun `sharePoi should throw exception when recipient does not exist`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = poiId,
            customPoi = null,
            message = null
        )

        every { userRepository.existsById(recipientId) } returns false

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.sharePoi(sharerId, request)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Recipient user not found"))
    }

    @Test
    fun `sharePoi should throw exception when users are not friends`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = poiId,
            customPoi = null,
            message = null
        )

        every { userRepository.existsById(recipientId) } returns true
        every { friendshipRepository.areFriends(sharerId, recipientId) } returns false

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.sharePoi(sharerId, request)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("You can only share POIs with accepted friends"))
    }

    @Test
    fun `sharePoi should throw exception when POI does not exist`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val request = SharePoiRequest(
            recipientId = recipientId,
            poiId = poiId,
            customPoi = null,
            message = null
        )

        every { userRepository.existsById(recipientId) } returns true
        every { friendshipRepository.areFriends(sharerId, recipientId) } returns true
        every { sharedPoiRepository.countBySharerIdAndRecipientId(sharerId, recipientId) } returns 0
        every { poiRepository.existsById(poiId) } returns false

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.sharePoi(sharerId, request)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("POI not found"))
    }

    // ========== getSharedPoiById Tests ==========

    @Test
    fun `getSharedPoiById should return shared POI when user is sharer`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = "Test message",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)
        every { userRepository.findById(any()) } returns Optional.empty()

        // When
        val result = sharedPoiService.getSharedPoiById(sharerId, sharedPoiId)

        // Then
        assertNotNull(result)
        assertEquals(sharedPoiId, result.id)
        assertEquals(sharerId, result.sharerId)
        assertEquals(recipientId, result.recipientId)

        verify(exactly = 1) { sharedPoiRepository.findById(sharedPoiId) }
    }

    @Test
    fun `getSharedPoiById should return shared POI when user is recipient`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = "Test message",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)
        every { userRepository.findById(any()) } returns Optional.empty()

        // When
        val result = sharedPoiService.getSharedPoiById(recipientId, sharedPoiId)

        // Then
        assertNotNull(result)
        assertEquals(sharedPoiId, result.id)
        assertEquals(sharerId, result.sharerId)
        assertEquals(recipientId, result.recipientId)

        verify(exactly = 1) { sharedPoiRepository.findById(sharedPoiId) }
    }

    @Test
    fun `getSharedPoiById should throw exception when shared POI not found`() {
        // Given
        val userId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.getSharedPoiById(userId, sharedPoiId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Shared POI not found"))
    }

    @Test
    fun `getSharedPoiById should throw exception when user is neither sharer nor recipient`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = UUID.randomUUID(),
            poiData = null,
            message = null,
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.getSharedPoiById(otherUserId, sharedPoiId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("You do not have access to this shared POI"))
    }

    // ========== getSharedByMe Tests ==========

    @Test
    fun `getSharedByMe should return list of shared POIs`() {
        // Given
        val sharerId = UUID.randomUUID()

        val sharedPois = listOf(
            SharedPoi(
                id = UUID.randomUUID(),
                sharerId = sharerId,
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "POI 1",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            ),
            SharedPoi(
                id = UUID.randomUUID(),
                sharerId = sharerId,
                recipientId = UUID.randomUUID(),
                poiId = null,
                poiData = CustomPoiData(
                    name = "Test",
                    description = "Desc",
                    category = "Test",
                    latitude = 52.0,
                    longitude = 21.0
                ),
                message = "POI 2",
                sharedAt = LocalDateTime.now(),
                viewedAt = LocalDateTime.now()
            )
        )

        every { sharedPoiRepository.findAllBySharerId(sharerId) } returns sharedPois
        every { userRepository.findById(any()) } returns Optional.empty()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns emptyList()

        // When
        val result = sharedPoiService.getSharedByMe(sharerId)

        // Then
        assertEquals(2, result.size)
        assertEquals("POI 1", result[0].message)
        assertEquals("POI 2", result[1].message)
        assertNotNull(result[1].poiData)
        assertNotNull(result[1].viewedAt)

        verify(exactly = 1) { sharedPoiRepository.findAllBySharerId(sharerId) }
    }

    @Test
    fun `getSharedByMe should return empty list when no POIs shared`() {
        // Given
        val userId = UUID.randomUUID()

        every { sharedPoiRepository.findAllBySharerId(userId) } returns emptyList()
        every { userRepository.findById(any()) } returns Optional.empty()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns emptyList()

        // When
        val result = sharedPoiService.getSharedByMe(userId)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { sharedPoiRepository.findAllBySharerId(userId) }
    }

    // ========== getSharedToMe Tests ==========

    @Test
    fun `getSharedToMe should return list of shared POIs`() {
        // Given
        val recipientId = UUID.randomUUID()

        val sharedPois = listOf(
            SharedPoi(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = recipientId,
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "From friend 1",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            ),
            SharedPoi(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = recipientId,
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "From friend 2",
                sharedAt = LocalDateTime.now(),
                viewedAt = LocalDateTime.now()
            )
        )

        every { sharedPoiRepository.findAllByRecipientId(recipientId) } returns sharedPois
        every { userRepository.findById(any()) } returns Optional.empty()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns emptyList()

        // When
        val result = sharedPoiService.getSharedToMe(recipientId)

        // Then
        assertEquals(2, result.size)
        assertEquals("From friend 1", result[0].message)
        assertEquals("From friend 2", result[1].message)

        verify(exactly = 1) { sharedPoiRepository.findAllByRecipientId(recipientId) }
    }

    @Test
    fun `getSharedToMe should return empty list when no POIs received`() {
        // Given
        val userId = UUID.randomUUID()

        every { sharedPoiRepository.findAllByRecipientId(userId) } returns emptyList()
        every { userRepository.findById(any()) } returns Optional.empty()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns emptyList()

        // When
        val result = sharedPoiService.getSharedToMe(userId)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { sharedPoiRepository.findAllByRecipientId(userId) }
    }

    // ========== getUnviewedSharedToMe Tests ==========

    @Test
    fun `getUnviewedSharedToMe should return only unviewed POIs`() {
        // Given
        val recipientId = UUID.randomUUID()

        val unviewedPois = listOf(
            SharedPoi(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = recipientId,
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "Unviewed 1",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            ),
            SharedPoi(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = recipientId,
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "Unviewed 2",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            )
        )

        every { sharedPoiRepository.findAllByRecipientIdAndViewedAtIsNull(recipientId) } returns unviewedPois
        every { userRepository.findById(any()) } returns Optional.empty()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns emptyList()

        // When
        val result = sharedPoiService.getUnviewedSharedToMe(recipientId)

        // Then
        assertEquals(2, result.size)
        result.forEach { assertNull(it.viewedAt) }

        verify(exactly = 1) { sharedPoiRepository.findAllByRecipientIdAndViewedAtIsNull(recipientId) }
    }

    @Test
    fun `getUnviewedSharedToMe should return empty list when all POIs viewed`() {
        // Given
        val userId = UUID.randomUUID()

        every { sharedPoiRepository.findAllByRecipientIdAndViewedAtIsNull(userId) } returns emptyList()
        every { userRepository.findById(any()) } returns Optional.empty()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns emptyList()

        // When
        val result = sharedPoiService.getUnviewedSharedToMe(userId)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { sharedPoiRepository.findAllByRecipientIdAndViewedAtIsNull(userId) }
    }

    // ========== markViewed Tests ==========

    @Test
    fun `markViewed should successfully mark POI as viewed`() {
        // Given
        val recipientId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = UUID.randomUUID(),
            recipientId = recipientId,
            poiId = UUID.randomUUID(),
            poiData = null,
            message = "Test",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        val updatedSharedPoi = sharedPoi.copy(viewedAt = LocalDateTime.now())

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)
        every { sharedPoiRepository.save(any()) } returns updatedSharedPoi
        every { userRepository.findById(any()) } returns Optional.empty()


        // When
        val result = sharedPoiService.markViewed(recipientId, sharedPoiId)

        // Then
        assertNotNull(result)
        assertEquals(sharedPoiId, result.id)
        assertNotNull(result.viewedAt)

        verify(exactly = 1) { sharedPoiRepository.findById(sharedPoiId) }
        verify(exactly = 1) { sharedPoiRepository.save(any()) }
    }

    @Test
    fun `markViewed should throw exception when shared POI not found`() {
        // Given
        val recipientId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.markViewed(recipientId, sharedPoiId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Shared POI not found"))
    }

    @Test
    fun `markViewed should throw exception when user is not recipient`() {
        // Given
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = UUID.randomUUID(),
            poiData = null,
            message = null,
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.markViewed(otherUserId, sharedPoiId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("Not your shared POI"))
    }

    // ========== deleteSharedPoi Tests ==========

    @Test
    fun `deleteSharedPoi should successfully delete shared POI when user is sharer`() {
        // Given
        val sharerId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = sharerId,
            recipientId = UUID.randomUUID(),
            poiId = UUID.randomUUID(),
            poiData = null,
            message = null,
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)
        every { sharedPoiRepository.delete(sharedPoi) } returns Unit

        // When
        sharedPoiService.deleteSharedPoi(sharerId, sharedPoiId)

        // Then
        verify(exactly = 1) { sharedPoiRepository.findById(sharedPoiId) }
        verify(exactly = 1) { sharedPoiRepository.delete(sharedPoi) }
    }

    @Test
    fun `deleteSharedPoi should throw exception when shared POI not found`() {
        // Given
        val sharerId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.deleteSharedPoi(sharerId, sharedPoiId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Shared POI not found"))
    }

    @Test
    fun `deleteSharedPoi should throw exception when user is not sharer`() {
        // Given
        val sharerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val sharedPoi = SharedPoi(
            id = sharedPoiId,
            sharerId = sharerId,
            recipientId = UUID.randomUUID(),
            poiId = UUID.randomUUID(),
            poiData = null,
            message = null,
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiRepository.findById(sharedPoiId) } returns Optional.of(sharedPoi)

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            sharedPoiService.deleteSharedPoi(otherUserId, sharedPoiId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("You can only delete POIs that you have shared"))
    }
}
