package org.cityxplore.backend.user.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.entity.User
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userService = UserService(userRepository)
    }

    @Test
    fun `getAll should return all users mapped to UserResponse`() {
        // given
        val user1 = createTestUser(
            id = UUID.randomUUID(),
            email = "user1@example.com",
            username = "user1"
        )
        val user2 = createTestUser(
            id = UUID.randomUUID(),
            email = "user2@example.com",
            username = "user2"
        )
        every { userRepository.findAll() } returns listOf(user1, user2)

        // when
        val result = userService.getAll()

        // then
        assertEquals(2, result.size)
        assertEquals("user1@example.com", result[0].email)
        assertEquals("user2@example.com", result[1].email)
        verify(exactly = 1) { userRepository.findAll() }
    }

    @Test
    fun `getAll should return empty list when no users exist`() {
        // given
        every { userRepository.findAll() } returns emptyList()

        // when
        val result = userService.getAll()

        // then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { userRepository.findAll() }
    }

    @Test
    fun `getById should return user when found`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        )
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when
        val result = userService.getById(userId)

        // then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("test@example.com", result.email)
        assertEquals("testuser", result.username)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `getById should throw ResponseStatusException when user not found`() {
        // given
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userService.getById(userId)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("User not found", exception.reason)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `create should save user and return UserResponse`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        val savedUser = createTestUser(
            id = UUID.randomUUID(),
            email = request.email,
            username = request.username,
            avatarUrl = request.avatarUrl
        )
        every { userRepository.findByEmail(request.email) } returns null
        every { userRepository.findByUsername(request.username) } returns null
        every { userRepository.save(any()) } returns savedUser

        // when
        val result = userService.create(request)

        // then
        assertNotNull(result)
        assertEquals("new@example.com", result.email)
        assertEquals("newuser", result.username)
        assertEquals("https://example.com/avatar.jpg", result.avatarUrl)
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `create should handle user with null avatarUrl`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser",
            avatarUrl = null
        )
        val savedUser = createTestUser(
            id = UUID.randomUUID(),
            email = request.email,
            username = request.username,
            avatarUrl = null
        )
        every { userRepository.findByEmail(request.email) } returns null
        every { userRepository.findByUsername(request.username) } returns null
        every { userRepository.save(any()) } returns savedUser

        // when
        val result = userService.create(request)

        // then
        assertNotNull(result)
        assertNull(result.avatarUrl)
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `create should preserve default values for numeric fields`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser"
        )
        val savedUser = createTestUser(
            id = UUID.randomUUID(),
            email = request.email,
            username = request.username
        )
        every { userRepository.findByEmail(request.email) } returns null
        every { userRepository.findByUsername(request.username) } returns null
        every { userRepository.save(any()) } returns savedUser

        // when
        val result = userService.create(request)

        // then
        assertEquals(BigDecimal.ZERO, result.totalDistance)
        verify(exactly = 1) { userRepository.save(any()) }
    }

    private fun createTestUser(
        id: UUID,
        email: String,
        username: String,
        avatarUrl: String? = null,
        totalDistance: BigDecimal = BigDecimal.ZERO
    ) = User(
        id = id,
        email = email,
        username = username,
        avatarUrl = avatarUrl,
        createdAt = LocalDateTime.now(),
        lastActiveAt = null,
        totalDistance = totalDistance
    )
}
