package org.cityxplore.backend.user.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.user.dto.UpdateUserProfileRequest
import org.cityxplore.backend.user.entity.User
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class UserProfileServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userProfileService: UserProfileService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userProfileService = UserProfileService(userRepository)
    }

    @Test
    fun `getUserProfile should return user profile when user exists`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "testuser",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when
        val result = userProfileService.getUserProfile(userId)

        // then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("test@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("https://example.com/avatar.jpg", result.avatarUrl)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `getUserProfile should throw ResponseStatusException when user not found`() {
        // given
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.getUserProfile(userId)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("User not found", exception.reason)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `updateUserProfile should update username when provided`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "oldusername"
        )
        val patch = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user

        // when
        val result = userProfileService.updateUserProfile(userId, patch)

        // then
        assertEquals("newusername", result.username)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should update avatarUrl when provided`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "testuser",
            avatarUrl = "https://example.com/old.jpg"
        )
        val patch = UpdateUserProfileRequest(
            username = null,
            avatarUrl = "https://example.com/new.jpg"
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user

        // when
        val result = userProfileService.updateUserProfile(userId, patch)

        // then
        assertEquals("https://example.com/new.jpg", result.avatarUrl)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should update both username and avatarUrl when both provided`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "oldusername",
            avatarUrl = "https://example.com/old.jpg"
        )
        val patch = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = "https://example.com/new.jpg"
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user

        // when
        val result = userProfileService.updateUserProfile(userId, patch)

        // then
        assertEquals("newusername", result.username)
        assertEquals("https://example.com/new.jpg", result.avatarUrl)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should not update fields when nulls provided`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "originalusername",
            avatarUrl = "https://example.com/original.jpg"
        )
        val patch = UpdateUserProfileRequest(
            username = null,
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user

        // when
        val result = userProfileService.updateUserProfile(userId, patch)

        // then
        assertEquals("originalusername", result.username)
        assertEquals("https://example.com/original.jpg", result.avatarUrl)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should trim and validate username`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "oldusername"
        )
        val patch = UpdateUserProfileRequest(
            username = "  newusername  ",
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user

        // when
        val result = userProfileService.updateUserProfile(userId, patch)

        // then
        assertEquals("newusername", result.username)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should not update username when it is empty after trim`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "originalusername"
        )
        val patch = UpdateUserProfileRequest(
            username = "   ",
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user

        // when
        val result = userProfileService.updateUserProfile(userId, patch)

        // then
        assertEquals("originalusername", result.username)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should throw ResponseStatusException when user not found`() {
        // given
        val userId = UUID.randomUUID()
        val patch = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.updateUserProfile(userId, patch)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("User not found", exception.reason)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `updateUserProfile should throw ResponseStatusException with CONFLICT when username already taken`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "oldusername"
        )
        val patch = UpdateUserProfileRequest(
            username = "takenusername",
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } throws DataIntegrityViolationException("username unique constraint violation")

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.updateUserProfile(userId, patch)
        }
        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals("Username already taken", exception.reason)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateUserProfile should throw ResponseStatusException with CONFLICT for generic constraint violation`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "oldusername"
        )
        val patch = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = null
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } throws DataIntegrityViolationException("some constraint violation")

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.updateUserProfile(userId, patch)
        }
        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals("Constraint violation occurred", exception.reason)
        verify(exactly = 1) { userRepository.save(user) }
    }

    private fun createTestUser(
        id: UUID,
        email: String,
        username: String,
        avatarUrl: String? = null
    ) = User(
        id = id,
        email = email,
        username = username,
        avatarUrl = avatarUrl,
        createdAt = LocalDateTime.now(),
        lastActiveAt = null,
        totalDistance = BigDecimal.ZERO,
        totalPoisDiscovered = 0
    )
}
