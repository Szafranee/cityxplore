package org.cityxplore.backend.storage.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.cityxplore.backend.storage.service.StorageService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException

/**
 * Unit tests for StorageController.
 */
@WebMvcTest(StorageController::class)
@EnableMethodSecurity
class StorageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var storageService: StorageService

    @Test
    fun `getSignedUrl should return 200 with signed URL when valid parameters provided`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val expiresIn = 3600
        val expectedSignedUrl = "https://storage.example.com/signed-url"

        every { storageService.createSignedUrl(bucket, path, expiresIn) } returns expectedSignedUrl

        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", bucket)
                .param("path", path)
                .param("expiresIn", expiresIn.toString())
                .with(jwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signedUrl").value(expectedSignedUrl))

        verify(exactly = 1) { storageService.createSignedUrl(bucket, path, expiresIn) }
    }

    @Test
    fun `getSignedUrl should use default expiresIn when not provided`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val defaultExpiresIn = 3600
        val expectedSignedUrl = "https://storage.example.com/signed-url"

        every { storageService.createSignedUrl(bucket, path, defaultExpiresIn) } returns expectedSignedUrl

        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", bucket)
                .param("path", path)
                .with(jwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signedUrl").value(expectedSignedUrl))

        verify(exactly = 1) { storageService.createSignedUrl(bucket, path, defaultExpiresIn) }
    }

    @Test
    fun `getSignedUrl should return 400 when bucket is blank`() {
        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", "")
                .param("path", "images/test.jpg")
                .with(jwt())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getSignedUrl should return 400 when path is blank`() {
        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", "test-bucket")
                .param("path", "")
                .with(jwt())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getSignedUrl should return 400 when expiresIn is less than 60`() {
        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", "test-bucket")
                .param("path", "images/test.jpg")
                .param("expiresIn", "30")
                .with(jwt())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getSignedUrl should return 400 when expiresIn is greater than 86400`() {
        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", "test-bucket")
                .param("path", "images/test.jpg")
                .param("expiresIn", "100000")
                .with(jwt())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getSignedUrl should return 200 with minimum valid expiresIn`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val expiresIn = 60
        val expectedSignedUrl = "https://storage.example.com/signed-url"

        every { storageService.createSignedUrl(bucket, path, expiresIn) } returns expectedSignedUrl

        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", bucket)
                .param("path", path)
                .param("expiresIn", expiresIn.toString())
                .with(jwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signedUrl").value(expectedSignedUrl))
    }

    @Test
    fun `getSignedUrl should return 200 with maximum valid expiresIn`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val expiresIn = 86400
        val expectedSignedUrl = "https://storage.example.com/signed-url"

        every { storageService.createSignedUrl(bucket, path, expiresIn) } returns expectedSignedUrl

        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", bucket)
                .param("path", path)
                .param("expiresIn", expiresIn.toString())
                .with(jwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.signedUrl").value(expectedSignedUrl))
    }

    @Test
    fun `getSignedUrl should handle service errors`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        every { storageService.createSignedUrl(bucket, path, 3600) } throws
                ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Storage error")

        // When & Then
        mockMvc.perform(
            get("/api/storage/url")
                .param("bucket", bucket)
                .param("path", path)
                .with(jwt())
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `deleteFile should return 204 when file successfully deleted`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        justRun { storageService.deleteFile(bucket, path) }

        // When & Then
        mockMvc.perform(
            delete("/api/storage")
                .param("bucket", bucket)
                .param("path", path)
                .with(csrf())
                .with(jwt().authorities(org.springframework.security.core.authority.SimpleGrantedAuthority("storage:write")))
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { storageService.deleteFile(bucket, path) }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `deleteFile should return 403 when user lacks storage write authority`() {
        // When & Then
        mockMvc.perform(
            delete("/api/storage")
                .param("bucket", "test-bucket")
                .param("path", "images/test.jpg")
                .with(csrf())
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteFile should return 400 when bucket is blank`() {
        // When & Then
        mockMvc.perform(
            delete("/api/storage")
                .param("bucket", "")
                .param("path", "images/test.jpg")
                .with(csrf())
                .with(jwt().authorities(org.springframework.security.core.authority.SimpleGrantedAuthority("storage:write")))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `deleteFile should return 400 when path is blank`() {
        // When & Then
        mockMvc.perform(
            delete("/api/storage")
                .param("bucket", "test-bucket")
                .param("path", "")
                .with(csrf())
                .with(jwt().authorities(org.springframework.security.core.authority.SimpleGrantedAuthority("storage:write")))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `deleteFile should handle service errors`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        every { storageService.deleteFile(bucket, path) } throws
                ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "File not found")

        // When & Then
        mockMvc.perform(
            delete("/api/storage")
                .param("bucket", bucket)
                .param("path", path)
                .with(csrf())
                .with(jwt().authorities(org.springframework.security.core.authority.SimpleGrantedAuthority("storage:write")))
        )
            .andExpect(status().isNotFound)
    }
}
