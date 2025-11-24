package org.cityxplore.backend.user.mapper

import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class UserMapperTest {

    @Test
    fun `toUserResponse should map User entity to UserResponse correctly`() {
        // given
        val userId = UUID.randomUUID()
        val createdAt = LocalDateTime.now()
        val lastActiveAt = LocalDateTime.now().minusDays(1)
        val user = User(
            id = userId,
            email = "test@example.com",
            username = "testuser",
            avatarUrl = "https://example.com/avatar.jpg",
            createdAt = createdAt,
            lastActiveAt = lastActiveAt,
            totalDistance = BigDecimal("123.45"),
            totalPoisDiscovered = 10
        )

        // when
        val result = user.toUserResponse()

        // then
        assertEquals(userId, result.id)
        assertEquals("test@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("https://example.com/avatar.jpg", result.avatarUrl)
        assertEquals(createdAt, result.createdAt)
        assertEquals(lastActiveAt, result.lastActiveAt)
        assertEquals(BigDecimal("123.45"), result.totalDistance)
        assertEquals(10, result.totalPoisDiscovered)
    }

    @Test
    fun `toUserResponse should handle null avatarUrl`() {
        // given
        val user = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            username = "testuser",
            avatarUrl = null,
            createdAt = LocalDateTime.now()
        )

        // when
        val result = user.toUserResponse()

        // then
        assertNull(result.avatarUrl)
    }

    @Test
    fun `toUserResponse should handle null lastActiveAt`() {
        // given
        val user = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            username = "testuser",
            createdAt = LocalDateTime.now(),
            lastActiveAt = null
        )

        // when
        val result = user.toUserResponse()

        // then
        assertNull(result.lastActiveAt)
    }

    @Test
    fun `toUserResponse should handle zero values for numeric fields`() {
        // given
        val user = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            username = "testuser",
            createdAt = LocalDateTime.now(),
            totalDistance = BigDecimal.ZERO,
            totalPoisDiscovered = 0
        )

        // when
        val result = user.toUserResponse()

        // then
        assertEquals(BigDecimal.ZERO, result.totalDistance)
        assertEquals(0, result.totalPoisDiscovered)
    }

    @Test
    fun `toUserResponseList should map list of Users to list of UserResponses`() {
        // given
        val user1 = User(
            id = UUID.randomUUID(),
            email = "user1@example.com",
            username = "user1",
            createdAt = LocalDateTime.now()
        )
        val user2 = User(
            id = UUID.randomUUID(),
            email = "user2@example.com",
            username = "user2",
            createdAt = LocalDateTime.now()
        )
        val users = listOf(user1, user2)

        // when
        val result = users.toUserResponseList()

        // then
        assertEquals(2, result.size)
        assertEquals("user1@example.com", result[0].email)
        assertEquals("user2@example.com", result[1].email)
    }

    @Test
    fun `toUserResponseList should return empty list for empty input`() {
        // given
        val users = emptyList<User>()

        // when
        val result = users.toUserResponseList()

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toEntity should map UserCreateRequest to User entity correctly`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser",
            avatarUrl = "https://example.com/avatar.jpg"
        )

        // when
        val result = request.toEntity()

        // then
        assertNull(result.id) // Entity not yet persisted
        assertEquals("new@example.com", result.email)
        assertEquals("newuser", result.username)
        assertEquals("https://example.com/avatar.jpg", result.avatarUrl)
        assertEquals(BigDecimal.ZERO, result.totalDistance)
        assertEquals(0, result.totalPoisDiscovered)
        assertTrue(result.isActive)
        assertEquals(0, result.totalAchievementPoints)
    }

    @Test
    fun `toEntity should handle null avatarUrl in UserCreateRequest`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser",
            avatarUrl = null
        )

        // when
        val result = request.toEntity()

        // then
        assertNull(result.avatarUrl)
    }

    @Test
    fun `toDto should map User entity to UserProfileResponse correctly`() {
        // given
        val userId = UUID.randomUUID()
        val createdAt = LocalDateTime.now()
        val user = User(
            id = userId,
            email = "test@example.com",
            username = "testuser",
            avatarUrl = "https://example.com/avatar.jpg",
            createdAt = createdAt,
            totalDistance = BigDecimal("456.78"),
            totalPoisDiscovered = 20
        )

        // when
        val result = user.toDto()

        // then
        assertEquals(userId, result.id)
        assertEquals("test@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("https://example.com/avatar.jpg", result.avatarUrl)
        assertEquals(BigDecimal("456.78"), result.totalDistance)
        assertEquals(20, result.totalPoisDiscovered)
        assertEquals(createdAt, result.createdAt)
    }

    @Test
    fun `toDto should throw IllegalArgumentException when User id is null`() {
        // given
        val user = User(
            id = null,
            email = "test@example.com",
            username = "testuser",
            createdAt = LocalDateTime.now()
        )

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            user.toDto()
        }
        assertEquals("Cannot map transient User entity to DTO", exception.message)
    }

    @Test
    fun `toDto should handle null avatarUrl in User entity`() {
        // given
        val user = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            username = "testuser",
            avatarUrl = null,
            createdAt = LocalDateTime.now()
        )

        // when
        val result = user.toDto()

        // then
        assertNull(result.avatarUrl)
    }

    @Test
    fun `toDto should handle zero values for numeric fields`() {
        // given
        val user = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            username = "testuser",
            createdAt = LocalDateTime.now(),
            totalDistance = BigDecimal.ZERO,
            totalPoisDiscovered = 0
        )

        // when
        val result = user.toDto()

        // then
        assertEquals(BigDecimal.ZERO, result.totalDistance)
        assertEquals(0, result.totalPoisDiscovered)
    }
}
