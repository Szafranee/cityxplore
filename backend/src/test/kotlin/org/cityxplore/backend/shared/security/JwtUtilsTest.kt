package org.cityxplore.backend.shared.security

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Unit tests for JwtUtils.
 */
class JwtUtilsTest {

    @Test
    fun `extractUserId should return valid UUID when sub claim is valid`() {
        // Given
        val expectedUserId = UUID.randomUUID()
        val jwt = mockk<Jwt> {
            every { subject } returns expectedUserId.toString()
        }

        // When
        val result = JwtUtils.extractUserId(jwt)

        // Then
        assertEquals(expectedUserId, result)
    }

    @Test
    fun `extractUserId should throw OAuth2AuthenticationException when sub claim is missing`() {
        // Given
        val jwt = mockk<Jwt> {
            every { subject } returns null
        }

        // When & Then
        val exception = assertThrows<OAuth2AuthenticationException> {
            JwtUtils.extractUserId(jwt)
        }
        assertEquals("invalid_token", exception.error.errorCode)
        assertEquals("Missing 'sub' claim", exception.error.description)
    }

    @Test
    fun `extractUserId should throw OAuth2AuthenticationException when sub claim is not a valid UUID`() {
        // Given
        val jwt = mockk<Jwt> {
            every { subject } returns "not-a-uuid"
        }

        // When & Then
        val exception = assertThrows<OAuth2AuthenticationException> {
            JwtUtils.extractUserId(jwt)
        }
        assertEquals("invalid_token", exception.error.errorCode)
        assertEquals("Invalid 'sub' claim: expected UUID", exception.error.description)
    }

    @Test
    fun `extractUserId should throw OAuth2AuthenticationException when sub claim is empty string`() {
        // Given
        val jwt = mockk<Jwt> {
            every { subject } returns ""
        }

        // When & Then
        val exception = assertThrows<OAuth2AuthenticationException> {
            JwtUtils.extractUserId(jwt)
        }
        assertEquals("invalid_token", exception.error.errorCode)
        assertEquals("Invalid 'sub' claim: expected UUID", exception.error.description)
    }

    @Test
    fun `extractUserId should handle UUID with different formats correctly`() {
        // Given
        val uuidString = "123e4567-e89b-12d3-a456-426614174000"
        val expectedUserId = UUID.fromString(uuidString)
        val jwt = mockk<Jwt> {
            every { subject } returns uuidString
        }

        // When
        val result = JwtUtils.extractUserId(jwt)

        // Then
        assertEquals(expectedUserId, result)
    }

    @Test
    fun `extractUserId should throw OAuth2AuthenticationException for malformed UUID with correct length`() {
        // Given
        val jwt = mockk<Jwt> {
            every { subject } returns "123e4567-e89b-12d3-a456-42661417400g" // 'g' is invalid in UUID
        }

        // When & Then
        val exception = assertThrows<OAuth2AuthenticationException> {
            JwtUtils.extractUserId(jwt)
        }
        assertEquals("invalid_token", exception.error.errorCode)
    }

    @Test
    fun `extractUserId should throw OAuth2AuthenticationException for UUID without dashes`() {
        // Given
        val jwt = mockk<Jwt> {
            every { subject } returns "123e4567e89b12d3a456426614174000"
        }

        // When & Then
        val exception = assertThrows<OAuth2AuthenticationException> {
            JwtUtils.extractUserId(jwt)
        }
        assertEquals("invalid_token", exception.error.errorCode)
    }

    @Test
    fun `extractUserId should work with uppercase UUID`() {
        // Given
        val uuidString = "123E4567-E89B-12D3-A456-426614174000"
        val expectedUserId = UUID.fromString(uuidString)
        val jwt = mockk<Jwt> {
            every { subject } returns uuidString
        }

        // When
        val result = JwtUtils.extractUserId(jwt)

        // Then
        assertEquals(expectedUserId, result)
    }

    @Test
    fun `extractUserId should work with mixed case UUID`() {
        // Given
        val uuidString = "123e4567-E89B-12d3-A456-426614174000"
        val expectedUserId = UUID.fromString(uuidString)
        val jwt = mockk<Jwt> {
            every { subject } returns uuidString
        }

        // When
        val result = JwtUtils.extractUserId(jwt)

        // Then
        assertEquals(expectedUserId, result)
    }
}
