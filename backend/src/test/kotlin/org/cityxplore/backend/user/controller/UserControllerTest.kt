package org.cityxplore.backend.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.config.JpaAuditingConfiguration
import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.dto.UserResponse
import org.cityxplore.backend.user.service.UserService
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(
    controllers = [UserController::class],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [JpaAuditingConfiguration::class]
    )]
)
@ImportAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userService: UserService

    @Test
    @WithMockUser
    fun `getAllUsers should return list of users`() {
        // given
        val user1 = createUserResponse(
            id = UUID.randomUUID(),
            email = "user1@example.com",
            username = "user1"
        )
        val user2 = createUserResponse(
            id = UUID.randomUUID(),
            email = "user2@example.com",
            username = "user2"
        )
        every { userService.getAll() } returns listOf(user1, user2)

        // when & then
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].email").value("user1@example.com"))
            .andExpect(jsonPath("$[1].email").value("user2@example.com"))

        verify(exactly = 1) { userService.getAll() }
    }

    @Test
    @WithMockUser
    fun `getAllUsers should return empty list when no users exist`() {
        // given
        every { userService.getAll() } returns emptyList()

        // when & then
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { userService.getAll() }
    }

    @Test
    @WithMockUser
    fun `getUser should return user when found`() {
        // given
        val userId = UUID.randomUUID()
        val userResponse = createUserResponse(
            id = userId,
            email = "test@example.com",
            username = "testuser"
        )
        every { userService.getById(userId) } returns userResponse

        // when & then
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.username").value("testuser"))

        verify(exactly = 1) { userService.getById(userId) }
    }

    @Test
    @WithMockUser
    fun `getUser should return 404 when user not found`() {
        // given
        val userId = UUID.randomUUID()
        every { userService.getById(userId) } throws ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // when & then
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isNotFound)

        verify(exactly = 1) { userService.getById(userId) }
    }

    @Test
    @WithMockUser
    fun `createUser should create user and return 201 with Location header`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        val createdUserId = UUID.randomUUID()
        val createdUser = createUserResponse(
            id = createdUserId,
            email = request.email,
            username = request.username,
            avatarUrl = request.avatarUrl
        )
        every { userService.create(any()) } returns createdUser

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/users/$createdUserId")))
            .andExpect(jsonPath("$.id").value(createdUserId.toString()))
            .andExpect(jsonPath("$.email").value("new@example.com"))
            .andExpect(jsonPath("$.username").value("newuser"))

        verify(exactly = 1) { userService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createUser should return 400 when email is invalid`() {
        // given
        val request = mapOf(
            "email" to "invalid-email",
            "username" to "testuser"
        )

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createUser should return 400 when email is blank`() {
        // given
        val request = mapOf(
            "email" to "",
            "username" to "testuser"
        )

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createUser should return 400 when username is blank`() {
        // given
        val request = mapOf(
            "email" to "test@example.com",
            "username" to ""
        )

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createUser should accept null avatarUrl`() {
        // given
        val request = UserCreateRequest(
            email = "new@example.com",
            username = "newuser",
            avatarUrl = null
        )
        val createdUserId = UUID.randomUUID()
        val createdUser = createUserResponse(
            id = createdUserId,
            email = request.email,
            username = request.username,
            avatarUrl = null
        )
        every { userService.create(any()) } returns createdUser

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.avatarUrl").doesNotExist())

        verify(exactly = 1) { userService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createUser should return 400 when email exceeds max length`() {
        // given
        val longEmail = "a".repeat(300) + "@example.com"
        val request = mapOf(
            "email" to longEmail,
            "username" to "testuser"
        )

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createUser should return 400 when username exceeds max length`() {
        // given
        val longUsername = "a".repeat(201)
        val request = mapOf(
            "email" to "test@example.com",
            "username" to longUsername
        )

        // when & then
        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { userService.create(any()) }
    }

    private fun createUserResponse(
        id: UUID,
        email: String,
        username: String,
        avatarUrl: String? = null
    ) = UserResponse(
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
