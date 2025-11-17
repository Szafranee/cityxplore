package org.cityxplore.backend.user.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Unit tests for MeController.
 */
@WebMvcTest(MeController::class)
class MeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `getUser should return 200 with user data from JWT`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val role = "user"

        // When & Then
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId.toString())
                            jwt.claim("email", email)
                            jwt.claim("role", role)
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value(role))
    }

    @Test
    fun `getUser should return 200 with null email when claim is missing`() {
        // Given
        val userId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId.toString())
                            jwt.claim("role", "user")
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.role").value("user"))
    }

    @Test
    fun `getUser should return 200 with null role when claim is missing`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"

        // When & Then
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId.toString())
                            jwt.claim("email", email)
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").doesNotExist())
    }

    @Test
    fun `getUser should return 200 with minimal JWT claims`() {
        // Given
        val userId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId.toString())
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.role").doesNotExist())
    }

    @Test
    fun `getUser should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getUser should handle different user IDs`() {
        // Given
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()

        // When & Then - First user
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId1.toString())
                            jwt.claim("email", "user1@example.com")
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId1.toString()))
            .andExpect(jsonPath("$.email").value("user1@example.com"))

        // When & Then - Second user
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId2.toString())
                            jwt.claim("email", "user2@example.com")
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId2.toString()))
            .andExpect(jsonPath("$.email").value("user2@example.com"))
    }

    @Test
    fun `getUser should handle admin role`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "admin@example.com"

        // When & Then
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject(userId.toString())
                            jwt.claim("email", email)
                            jwt.claim("role", "admin")
                        }
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("admin"))
    }

    @Test
    fun `getUser should return 500 when JWT subject is not a valid UUID`() {
        // When & Then
        mockMvc.perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt { jwt ->
                            jwt.subject("invalid-uuid")
                            jwt.claim("email", "test@example.com")
                        }
                )
        )
            .andExpect(status().isInternalServerError)
    }
}
