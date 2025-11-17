package org.cityxplore.backend.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.config.JpaAuditingConfiguration
import org.cityxplore.backend.user.dto.UpdateUserProfileRequest
import org.cityxplore.backend.user.dto.UserProfileResponse
import org.cityxplore.backend.user.service.UserProfileService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(
    controllers = [UserProfileController::class],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [JpaAuditingConfiguration::class]
    )]
)
@ImportAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
class UserProfileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userProfileService: UserProfileService

    @Test
    fun `getMyProfile should return user profile for authenticated user`() {
        // given
        val userId = UUID.randomUUID()
        val userProfile = createUserProfileResponse(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        )
        every { userProfileService.getUserProfile(userId) } returns userProfile

        // when & then
        mockMvc.perform(
            get("/api/users/me")
                .with(jwt().jwt { it.subject(userId.toString()) })
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.username").value("testuser"))

        verify(exactly = 1) { userProfileService.getUserProfile(userId) }
    }

    @Test
    fun `getMyProfile should return 404 when user not found`() {
        // given
        val userId = UUID.randomUUID()
        every { userProfileService.getUserProfile(userId) } throws
                ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // when & then
        mockMvc.perform(
            get("/api/users/me")
                .with(jwt().jwt { it.subject(userId.toString()) })
        )
            .andExpect(status().isNotFound)

        verify(exactly = 1) { userProfileService.getUserProfile(userId) }
    }

    @Test
    fun `getMyProfile should return 401 when not authenticated`() {
        // when & then
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)

        verify(exactly = 0) { userProfileService.getUserProfile(any()) }
    }

    @Test
    fun `updateMyProfile should update user profile successfully`() {
        // given
        val userId = UUID.randomUUID()
        val request = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = "https://example.com/new-avatar.jpg"
        )
        val updatedProfile = createUserProfileResponse(
            id = userId,
            email = "test@example.com",
            username = "newusername",
            avatarUrl = "https://example.com/new-avatar.jpg"
        )
        every { userProfileService.updateUserProfile(userId, any()) } returns updatedProfile

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("newusername"))
            .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"))

        verify(exactly = 1) { userProfileService.updateUserProfile(userId, any()) }
    }

    @Test
    fun `updateMyProfile should update only username when avatarUrl is null`() {
        // given
        val userId = UUID.randomUUID()
        val request = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = null
        )
        val updatedProfile = createUserProfileResponse(
            id = userId,
            email = "test@example.com",
            username = "newusername",
            avatarUrl = "https://example.com/old-avatar.jpg"
        )
        every { userProfileService.updateUserProfile(userId, any()) } returns updatedProfile

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("newusername"))

        verify(exactly = 1) { userProfileService.updateUserProfile(userId, any()) }
    }

    @Test
    fun `updateMyProfile should update only avatarUrl when username is null`() {
        // given
        val userId = UUID.randomUUID()
        val request = UpdateUserProfileRequest(
            username = null,
            avatarUrl = "https://example.com/new-avatar.jpg"
        )
        val updatedProfile = createUserProfileResponse(
            id = userId,
            email = "test@example.com",
            username = "testuser",
            avatarUrl = "https://example.com/new-avatar.jpg"
        )
        every { userProfileService.updateUserProfile(userId, any()) } returns updatedProfile

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"))

        verify(exactly = 1) { userProfileService.updateUserProfile(userId, any()) }
    }

    @Test
    fun `updateMyProfile should accept both fields as null`() {
        // given
        val userId = UUID.randomUUID()
        val request = UpdateUserProfileRequest(
            username = null,
            avatarUrl = null
        )
        val updatedProfile = createUserProfileResponse(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        )
        every { userProfileService.updateUserProfile(userId, any()) } returns updatedProfile

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        verify(exactly = 1) { userProfileService.updateUserProfile(userId, any()) }
    }

    @Test
    fun `updateMyProfile should return 404 when user not found`() {
        // given
        val userId = UUID.randomUUID()
        val request = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = null
        )
        every { userProfileService.updateUserProfile(userId, any()) } throws
                ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)

        verify(exactly = 1) { userProfileService.updateUserProfile(userId, any()) }
    }

    @Test
    fun `updateMyProfile should return 409 when username already taken`() {
        // given
        val userId = UUID.randomUUID()
        val request = UpdateUserProfileRequest(
            username = "takenusername",
            avatarUrl = null
        )
        every { userProfileService.updateUserProfile(userId, any()) } throws
                ResponseStatusException(HttpStatus.CONFLICT, "Username already taken")

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)

        verify(exactly = 1) { userProfileService.updateUserProfile(userId, any()) }
    }

    @Test
    fun `updateMyProfile should return 401 when not authenticated`() {
        // given
        val request = UpdateUserProfileRequest(
            username = "newusername",
            avatarUrl = null
        )

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)

        verify(exactly = 0) { userProfileService.updateUserProfile(any(), any()) }
    }

    @Test
    fun `updateMyProfile should return 400 when username exceeds max length`() {
        // given
        val userId = UUID.randomUUID()
        val longUsername = "a".repeat(201)
        val request = mapOf(
            "username" to longUsername,
            "avatarUrl" to null
        )

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userProfileService.updateUserProfile(any(), any()) }
    }

    @Test
    fun `updateMyProfile should return 400 when avatarUrl exceeds max length`() {
        // given
        val userId = UUID.randomUUID()
        val longAvatarUrl = "https://example.com/" + "a".repeat(500)
        val request = mapOf(
            "username" to null,
            "avatarUrl" to longAvatarUrl
        )

        // when & then
        mockMvc.perform(
            patch("/api/users/me")
                .with(csrf())
                .with(jwt().jwt { it.subject(userId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userProfileService.updateUserProfile(any(), any()) }
    }

    private fun createUserProfileResponse(
        id: UUID,
        email: String,
        username: String,
        avatarUrl: String? = null
    ) = UserProfileResponse(
        id = id,
        email = email,
        username = username,
        avatarUrl = avatarUrl,
        totalDistance = BigDecimal.ZERO,
        totalPoisDiscovered = 0,
        createdAt = LocalDateTime.now()
    )
}
