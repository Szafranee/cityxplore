package org.cityxplore.backend.user.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.admin.AdminApi
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.UploadOptionBuilder
import io.github.jan.supabase.storage.storage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.cityxplore.backend.user.dto.UpdateUserProfileRequest
import org.cityxplore.backend.user.entity.User
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class UserProfileServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var storagePlugin: Storage
    private lateinit var authPlugin: Auth
    private lateinit var adminApi: AdminApi
    private lateinit var bucketApi: BucketApi
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var userProfileService: UserProfileService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        supabaseClient = mockk()
        storagePlugin = mockk()
        authPlugin = mockk()
        adminApi = mockk()
        bucketApi = mockk()
        transactionTemplate = mockk()

        // Mock Supabase plugins extension functions
        // Ensure to mock the file class where extension properties are defined
        mockkStatic("io.github.jan.supabase.storage.StorageKt")
        mockkStatic("io.github.jan.supabase.auth.AuthKt")

        // Mock Java static method
        mockkStatic("org.springframework.transaction.support.TransactionSynchronizationManager")

        // Mock extension properties
        every { supabaseClient.storage } returns storagePlugin
        every { supabaseClient.auth } returns authPlugin
        every { authPlugin.admin } returns adminApi
        every { storagePlugin.from(any()) } returns bucketApi

        // Mock TransactionTemplate and TransactionSynchronizationManager
        // Use generic casting to avoid type inference issues
        every { transactionTemplate.execute(any<TransactionCallback<Any>>()) } answers {
            val callback = firstArg<TransactionCallback<Any>>()
            callback.doInTransaction(mockk())
        }
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        every { TransactionSynchronizationManager.registerSynchronization(any()) } just Runs

        userProfileService = UserProfileService(userRepository, supabaseClient, transactionTemplate)
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

    @Test
    fun `getUserProfile should throw 404 when user exists but is inactive (soft deleted)`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        ).apply { isActive = false }
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.getUserProfile(userId)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `updateUserProfile should throw 404 when user exists but is inactive`() {
        // given
        val userId = UUID.randomUUID()
        val patch = UpdateUserProfileRequest("newname", null)
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        ).apply { isActive = false }
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.updateUserProfile(userId, patch)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `uploadUserAvatar should upload file and update user profile`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        )
        val fileBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) // JPEG magic bytes
        val fileName = "avatar.jpg"
        val publicUrl = "https://supabase.co/storage/v1/object/public/user-avatars/$userId/timestamp_avatar.jpg"

        val multipartFile = mockk<MultipartFile>()
        every { multipartFile.isEmpty } returns false
        every { multipartFile.contentType } returns "image/jpeg"
        every { multipartFile.size } returns 1024
        every { multipartFile.originalFilename } returns fileName
        every { multipartFile.bytes } returns fileBytes
        every { multipartFile.inputStream } returns ByteArrayInputStream(fileBytes)

        every { userRepository.existsById(userId) } returns true
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }

        // Mock Supabase storage calls
        coEvery {
            bucketApi.upload(
                any<String>(),
                any<ByteArray>(),
                any<UploadOptionBuilder.() -> Unit>()
            )
        } returns mockk()

        // Mock publicUrl extension
        every { bucketApi.publicUrl(any<String>()) } returns publicUrl

        // Mock delete
        coEvery { bucketApi.delete(any<String>()) } just Runs // Mock delete old avatar

        // when
        val result = userProfileService.uploadUserAvatar(userId, multipartFile)

        // then
        assertEquals(publicUrl, result.avatarUrl)
        verify(exactly = 1) { userRepository.save(user) }
        coVerify(exactly = 1) {
            bucketApi.upload(
                any<String>(),
                fileBytes,
                any<UploadOptionBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun `uploadUserAvatar should throw 400 for empty file`() {
        // given
        val userId = UUID.randomUUID()
        createTestUser(userId, "t@t.com", "u").apply { isActive = true }
        every { userRepository.existsById(userId) } returns true

        val multipartFile = mockk<MultipartFile>()
        every { multipartFile.isEmpty } returns true

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.uploadUserAvatar(userId, multipartFile)
        }
        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("File is empty", exception.reason)
    }

    @Test
    fun `uploadUserAvatar should throw 400 for invalid file format`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "t@t.com", "u")
        every { userRepository.existsById(userId) } returns true
        every { userRepository.findById(userId) } returns Optional.of(user)

        "invalid".toByteArray()
        val multipartFile = mockk<MultipartFile>()
        every { multipartFile.isEmpty } returns false
        every { multipartFile.contentType } returns "text/plain" // Invalid content type

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.uploadUserAvatar(userId, multipartFile)
        }
        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("Invalid content type"))
    }

    @Test
    fun `deleteUserAccount should soft delete user and delete from Supabase Auth`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "user@example.com", "username").apply {
            isActive = true
            avatarUrl = "http://url"
        }
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }
        coEvery { adminApi.deleteUser(any()) } just Runs

        // when
        userProfileService.deleteUserAccount(userId)

        // then
        assertFalse(user.isActive)
        assertNotNull(user.deletedAt)

        // Use capturing or verification if user object is not updated in place (but it should be)
        // If save(user) is called with modified object, it references the same object in most mocked scenarios unless copy is made.

        assertTrue(user.email.startsWith("deleted-"), "Email should be anonymized, was: ${user.email}")
        assertTrue(user.username.startsWith("deleted-"), "Username should be anonymized, was: ${user.username}")
        assertNull(user.avatarUrl)

        verify(exactly = 1) { userRepository.save(user) }
        coVerify(exactly = 1) { adminApi.deleteUser(userId.toString()) }
    }

    @Test
    fun `deleteUserAccount should ignore if user is already deleted`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "user@example.com", "username").apply {
            isActive = false
            deletedAt = LocalDateTime.now()
        }
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when
        userProfileService.deleteUserAccount(userId)

        // then
        verify(exactly = 0) { userRepository.save(any()) }
        coVerify(exactly = 0) { adminApi.deleteUser(any()) }
    }

    @Test
    fun `deleteUserAccount should not fail if Supabase Auth deletion fails`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "user@example.com", "username")
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }
        coEvery { adminApi.deleteUser(any()) } throws RuntimeException("Supabase down")

        // when
        userProfileService.deleteUserAccount(userId)

        // then
        assertFalse(user.isActive) // DB soft delete should still happen
        verify(exactly = 1) { userRepository.save(user) }
        coVerify(exactly = 1) { adminApi.deleteUser(userId.toString()) }
    }

    // ========== addDistance Tests ==========

    @Test
    fun `addDistance should increment user total distance`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "test@example.com", "testuser")
        val distanceMeters = 150.0

        every { userRepository.incrementDistance(userId, any()) } returns 1
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when
        val result = userProfileService.addDistance(userId, distanceMeters)

        // then
        verify { userRepository.incrementDistance(userId, any()) }
        assertNotNull(result)
        assertEquals(userId, result.id)
    }

    @Test
    fun `addDistance should throw NOT_FOUND when user does not exist`() {
        // given
        val userId = UUID.randomUUID()
        val distanceMeters = 100.0

        every { userRepository.incrementDistance(userId, any()) } returns 0

        // when & then
        val exception = assertThrows<ResponseStatusException> {
            userProfileService.addDistance(userId, distanceMeters)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("User not found", exception.reason)
    }

    @Test
    fun `addDistance should round distance to 2 decimal places`() {
        // given
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "test@example.com", "testuser")
        val distanceMeters = 123.456789

        every { userRepository.incrementDistance(userId, BigDecimal.valueOf(123.46)) } returns 1
        every { userRepository.findById(userId) } returns Optional.of(user)

        // when
        userProfileService.addDistance(userId, distanceMeters)

        // then
        verify { userRepository.incrementDistance(userId, BigDecimal.valueOf(123.46)) }
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
