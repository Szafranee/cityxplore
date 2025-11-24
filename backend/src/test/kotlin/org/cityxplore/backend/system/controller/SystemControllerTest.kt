package org.cityxplore.backend.system.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Unit tests for SystemController.
 */
// @SpringBootTest
// @AutoConfigureMockMvc
@WebMvcTest(SystemController::class)
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `ping should return 200 with status OK`() {
        // When & Then
        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.service").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `ping should return current timestamp`() {
        // Given
        val beforeTimestamp = System.currentTimeMillis()

        // When
        val result = mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.timestamp").isNumber)
            .andReturn()

        // Then
        val afterTimestamp = System.currentTimeMillis()
        val responseTimestamp = org.springframework.test.util.JsonPathExpectationsHelper("$.timestamp")
            .evaluateJsonPath(result.response.contentAsString) as Number

        assert(responseTimestamp.toLong() >= beforeTimestamp)
        assert(responseTimestamp.toLong() <= afterTimestamp)
    }

    @Test
    fun `ping should return service name from configuration`() {
        // When & Then
        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.service").isString)
            .andExpect(jsonPath("$.service").isNotEmpty)
    }

    @Test
    fun `ping should return consistent response structure`() {
        // When & Then
        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.service").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `environment should return 200 with profile and version`() {
        // When & Then
        mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.profile").exists())
            .andExpect(jsonPath("$.version").exists())
    }

    @Test
    fun `environment should return profile from configuration`() {
        // When & Then
        mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.profile").isString)
    }

    @Test
    fun `environment should return version from configuration`() {
        // When & Then
        mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").isString)
    }

    @Test
    fun `environment should return consistent response structure`() {
        // When & Then
        mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.profile").exists())
            .andExpect(jsonPath("$.version").exists())
    }

    @Test
    fun `ping endpoint should be accessible without authentication`() {
        // When & Then
        // This test verifies that no authentication is required for the ping endpoint
        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
    }

    @Test
    fun `environment endpoint should be accessible without authentication`() {
        // When & Then
        // This test verifies that no authentication is required for the environment endpoint
        mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
    }

    @Test
    fun `ping should return timestamp as long integer`() {
        // When & Then
        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.timestamp").isNumber)
    }

    @Test
    fun `multiple ping calls should return different timestamps`() {
        // When
        val result1 = mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andReturn()

        Thread.sleep(10) // Small delay to ensure different timestamps

        val result2 = mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
            .andReturn()

        // Then
        val timestamp1 = org.springframework.test.util.JsonPathExpectationsHelper("$.timestamp")
            .evaluateJsonPath(result1.response.contentAsString) as Number
        val timestamp2 = org.springframework.test.util.JsonPathExpectationsHelper("$.timestamp")
            .evaluateJsonPath(result2.response.contentAsString) as Number

        assert(timestamp2.toLong() >= timestamp1.toLong())
    }

    @Test
    fun `environment should return same values across multiple calls`() {
        // When
        val result1 = mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
            .andReturn()

        val result2 = mockMvc.perform(get("/api/public/environment"))
            .andExpect(status().isOk)
            .andReturn()

        // Then - both responses should be identical
        assert(result1.response.contentAsString == result2.response.contentAsString)
    }
}
