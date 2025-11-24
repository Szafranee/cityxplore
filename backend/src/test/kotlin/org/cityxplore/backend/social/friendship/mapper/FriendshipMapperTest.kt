package org.cityxplore.backend.social.friendship.mapper

import org.cityxplore.backend.social.friendship.entity.Friendship
import org.cityxplore.backend.social.friendship.entity.FriendshipStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for FriendshipMapper.
 */
class FriendshipMapperTest {

    @Test
    fun `toResponse should map Friendship with PENDING status correctly`() {
        // Given
        val id = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val createdAt = LocalDateTime.now().minusDays(1)
        val updatedAt = LocalDateTime.now()

        val friendship = Friendship(
            id = id,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.PENDING,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val response = FriendshipMapper.toResponse(friendship)

        // Then
        assertEquals(id, response.id)
        assertEquals(requesterId, response.requesterId)
        assertEquals(addresseeId, response.addresseeId)
        assertEquals(FriendshipStatus.PENDING, response.status)
        assertEquals(createdAt, response.createdAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `toResponse should map Friendship with ACCEPTED status correctly`() {
        // Given
        val id = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val createdAt = LocalDateTime.now().minusDays(5)
        val updatedAt = LocalDateTime.now().minusDays(3)

        val friendship = Friendship(
            id = id,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.ACCEPTED,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val response = FriendshipMapper.toResponse(friendship)

        // Then
        assertEquals(id, response.id)
        assertEquals(requesterId, response.requesterId)
        assertEquals(addresseeId, response.addresseeId)
        assertEquals(FriendshipStatus.ACCEPTED, response.status)
        assertEquals(createdAt, response.createdAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `toResponse should map Friendship with DECLINED status correctly`() {
        // Given
        val id = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val createdAt = LocalDateTime.now().minusDays(2)
        val updatedAt = LocalDateTime.now().minusDays(1)

        val friendship = Friendship(
            id = id,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.DECLINED,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val response = FriendshipMapper.toResponse(friendship)

        // Then
        assertEquals(id, response.id)
        assertEquals(requesterId, response.requesterId)
        assertEquals(addresseeId, response.addresseeId)
        assertEquals(FriendshipStatus.DECLINED, response.status)
        assertEquals(createdAt, response.createdAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `toResponse should map Friendship with BLOCKED status correctly`() {
        // Given
        val id = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val createdAt = LocalDateTime.now().minusDays(10)
        val updatedAt = LocalDateTime.now().minusDays(5)

        val friendship = Friendship(
            id = id,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.BLOCKED,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val response = FriendshipMapper.toResponse(friendship)

        // Then
        assertEquals(id, response.id)
        assertEquals(requesterId, response.requesterId)
        assertEquals(addresseeId, response.addresseeId)
        assertEquals(FriendshipStatus.BLOCKED, response.status)
        assertEquals(createdAt, response.createdAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `toResponse should throw IllegalArgumentException when Friendship id is null`() {
        // Given
        val friendship = Friendship(
            id = null,
            requesterId = UUID.randomUUID(),
            addresseeId = UUID.randomUUID(),
            status = FriendshipStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FriendshipMapper.toResponse(friendship)
        }
        assertEquals("Friendship entity must be persisted before mapping to response", exception.message)
    }

    @Test
    fun `toResponse should preserve timestamp precision`() {
        // Given
        val id = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456789)
        val updatedAt = LocalDateTime.of(2024, 1, 20, 14, 45, 30, 987654321)

        val friendship = Friendship(
            id = id,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.ACCEPTED,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val response = FriendshipMapper.toResponse(friendship)

        // Then
        assertEquals(createdAt, response.createdAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `toResponse should map when requester and addressee are same user`() {
        // Given - edge case where someone tries to befriend themselves
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val createdAt = LocalDateTime.now()
        val updatedAt = LocalDateTime.now()

        val friendship = Friendship(
            id = id,
            requesterId = userId,
            addresseeId = userId,
            status = FriendshipStatus.PENDING,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val response = FriendshipMapper.toResponse(friendship)

        // Then
        assertEquals(id, response.id)
        assertEquals(userId, response.requesterId)
        assertEquals(userId, response.addresseeId)
        assertEquals(FriendshipStatus.PENDING, response.status)
    }
}
