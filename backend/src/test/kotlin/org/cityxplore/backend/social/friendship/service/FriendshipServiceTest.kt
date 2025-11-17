package org.cityxplore.backend.social.friendship.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.social.friendship.dto.FriendshipRequest
import org.cityxplore.backend.social.friendship.entity.Friendship
import org.cityxplore.backend.social.friendship.entity.FriendshipStatus
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

/**
 * Unit tests for FriendshipService.
 */
class FriendshipServiceTest {

    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var friendshipService: FriendshipService

    @BeforeEach
    fun setUp() {
        friendshipRepository = mockk()
        friendshipService = FriendshipService(friendshipRepository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ========== sendInvite Tests ==========

    @Test
    fun `sendInvite should create new friendship when no existing relationship exists`() {
        // Given
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val request = FriendshipRequest(addresseeId)

        every { friendshipRepository.findInEitherDirection(requesterId, addresseeId) } returns null

        val savedFriendship = Friendship(
            id = UUID.randomUUID(),
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { friendshipRepository.save(any()) } returns savedFriendship

        // When
        val result = friendshipService.sendInvite(requesterId, request)

        // Then
        assertNotNull(result)
        assertEquals(requesterId, result.requesterId)
        assertEquals(addresseeId, result.addresseeId)
        assertEquals(FriendshipStatus.PENDING, result.status)

        verify(exactly = 1) { friendshipRepository.findInEitherDirection(requesterId, addresseeId) }
        verify(exactly = 1) { friendshipRepository.save(any()) }
    }

    @Test
    fun `sendInvite should throw BAD_REQUEST when user tries to invite themselves`() {
        // Given
        val userId = UUID.randomUUID()
        val request = FriendshipRequest(userId)

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.sendInvite(userId, request)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("Cannot invite yourself"))

        verify(exactly = 0) { friendshipRepository.findInEitherDirection(any(), any()) }
    }

    @Test
    fun `sendInvite should throw CONFLICT when pending request already exists`() {
        // Given
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val request = FriendshipRequest(addresseeId)

        val existingFriendship = Friendship(
            id = UUID.randomUUID(),
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.PENDING
        )
        every { friendshipRepository.findInEitherDirection(requesterId, addresseeId) } returns existingFriendship

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.sendInvite(requesterId, request)
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertTrue(exception.reason!!.contains("pending"))
    }

    @Test
    fun `sendInvite should throw CONFLICT when users are already friends`() {
        // Given
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val request = FriendshipRequest(addresseeId)

        val existingFriendship = Friendship(
            id = UUID.randomUUID(),
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.ACCEPTED
        )
        every { friendshipRepository.findInEitherDirection(requesterId, addresseeId) } returns existingFriendship

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.sendInvite(requesterId, request)
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertTrue(exception.reason!!.contains("already friends"))
    }

    @Test
    fun `sendInvite should update existing DECLINED friendship to PENDING`() {
        // Given
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val request = FriendshipRequest(addresseeId)

        val existingFriendship = Friendship(
            id = UUID.randomUUID(),
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.DECLINED
        )
        every { friendshipRepository.findInEitherDirection(requesterId, addresseeId) } returns existingFriendship
        every { friendshipRepository.save(any()) } answers { firstArg() }

        // When
        val result = friendshipService.sendInvite(requesterId, request)

        // Then
        assertEquals(FriendshipStatus.PENDING, result.status)
        verify(exactly = 1) { friendshipRepository.save(existingFriendship) }
    }

    @Test
    fun `sendInvite should throw FORBIDDEN when user is blocked`() {
        // Given
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val request = FriendshipRequest(addresseeId)

        val existingFriendship = Friendship(
            id = UUID.randomUUID(),
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.BLOCKED
        )
        every { friendshipRepository.findInEitherDirection(requesterId, addresseeId) } returns existingFriendship

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.sendInvite(requesterId, request)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("Cannot send invitation"))
    }

    // ========== acceptInvite Tests ==========

    @Test
    fun `acceptInvite should update friendship status to ACCEPTED`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = currentUserId,
            status = FriendshipStatus.PENDING
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)
        every { friendshipRepository.save(any()) } answers { firstArg() }

        // When
        val result = friendshipService.acceptInvite(currentUserId, friendshipId)

        // Then
        assertEquals(FriendshipStatus.ACCEPTED, result.status)
        verify(exactly = 1) { friendshipRepository.save(friendship) }
    }

    @Test
    fun `acceptInvite should throw NOT_FOUND when friendship does not exist`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        every { friendshipRepository.findById(friendshipId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.acceptInvite(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("not found"))
    }

    @Test
    fun `acceptInvite should throw FORBIDDEN when user is not the addressee`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.PENDING
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.acceptInvite(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("Not your invitation"))
    }

    @Test
    fun `acceptInvite should throw CONFLICT when friendship is not pending`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = currentUserId,
            status = FriendshipStatus.ACCEPTED
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.acceptInvite(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertTrue(exception.reason!!.contains("not pending"))
    }

    // ========== declineInvite Tests ==========

    @Test
    fun `declineInvite should update friendship status to DECLINED`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = currentUserId,
            status = FriendshipStatus.PENDING
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)
        every { friendshipRepository.save(any()) } answers { firstArg() }

        // When
        val result = friendshipService.declineInvite(currentUserId, friendshipId)

        // Then
        assertEquals(FriendshipStatus.DECLINED, result.status)
        verify(exactly = 1) { friendshipRepository.save(friendship) }
    }

    @Test
    fun `declineInvite should throw NOT_FOUND when friendship does not exist`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        every { friendshipRepository.findById(friendshipId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.declineInvite(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `declineInvite should throw FORBIDDEN when user is not the addressee`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.PENDING
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.declineInvite(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    // ========== getFriendshipById Tests ==========

    @Test
    fun `getFriendshipById should return friendship when user is requester`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = currentUserId,
            addresseeId = addresseeId,
            status = FriendshipStatus.ACCEPTED
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)

        // When
        val result = friendshipService.getFriendshipById(currentUserId, friendshipId)

        // Then
        assertNotNull(result)
        assertEquals(friendshipId, result.id)
    }

    @Test
    fun `getFriendshipById should return friendship when user is addressee`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = currentUserId,
            status = FriendshipStatus.ACCEPTED
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)

        // When
        val result = friendshipService.getFriendshipById(currentUserId, friendshipId)

        // Then
        assertNotNull(result)
        assertEquals(friendshipId, result.id)
    }

    @Test
    fun `getFriendshipById should throw NOT_FOUND when friendship does not exist`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        every { friendshipRepository.findById(friendshipId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.getFriendshipById(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `getFriendshipById should throw FORBIDDEN when user is not part of friendship`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val friendship = Friendship(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.ACCEPTED
        )
        every { friendshipRepository.findById(friendshipId) } returns Optional.of(friendship)

        // When & Then
        val exception = assertThrows(ResponseStatusException::class.java) {
            friendshipService.getFriendshipById(currentUserId, friendshipId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("do not have access"))
    }

    // ========== getMyFriends Tests ==========

    @Test
    fun `getMyFriends should return list of accepted friendships`() {
        // Given
        val userId = UUID.randomUUID()
        val friend1Id = UUID.randomUUID()
        val friend2Id = UUID.randomUUID()

        val friendships = listOf(
            Friendship(
                id = UUID.randomUUID(),
                requesterId = userId,
                addresseeId = friend1Id,
                status = FriendshipStatus.ACCEPTED
            ),
            Friendship(
                id = UUID.randomUUID(),
                requesterId = friend2Id,
                addresseeId = userId,
                status = FriendshipStatus.ACCEPTED
            )
        )
        every { friendshipRepository.findAllAcceptedByUserId(userId) } returns friendships

        // When
        val result = friendshipService.getMyFriends(userId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == FriendshipStatus.ACCEPTED })
    }

    @Test
    fun `getMyFriends should return empty list when user has no friends`() {
        // Given
        val userId = UUID.randomUUID()

        every { friendshipRepository.findAllAcceptedByUserId(userId) } returns emptyList()

        // When
        val result = friendshipService.getMyFriends(userId)

        // Then
        assertTrue(result.isEmpty())
    }

    // ========== getPendingInvites Tests ==========

    @Test
    fun `getPendingInvites should return list of pending invitations`() {
        // Given
        val userId = UUID.randomUUID()
        val requester1Id = UUID.randomUUID()
        val requester2Id = UUID.randomUUID()

        val pendingInvites = listOf(
            Friendship(
                id = UUID.randomUUID(),
                requesterId = requester1Id,
                addresseeId = userId,
                status = FriendshipStatus.PENDING
            ),
            Friendship(
                id = UUID.randomUUID(),
                requesterId = requester2Id,
                addresseeId = userId,
                status = FriendshipStatus.PENDING
            )
        )
        every { friendshipRepository.findAllPendingByAddresseeId(userId) } returns pendingInvites

        // When
        val result = friendshipService.getPendingInvites(userId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == FriendshipStatus.PENDING })
        assertTrue(result.all { it.addresseeId == userId })
    }

    @Test
    fun `getPendingInvites should return empty list when user has no pending invitations`() {
        // Given
        val userId = UUID.randomUUID()

        every { friendshipRepository.findAllPendingByAddresseeId(userId) } returns emptyList()

        // When
        val result = friendshipService.getPendingInvites(userId)

        // Then
        assertTrue(result.isEmpty())
    }
}
