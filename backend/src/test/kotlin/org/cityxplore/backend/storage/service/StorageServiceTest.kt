package org.cityxplore.backend.storage.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cityxplore.backend.storage.dto.SignedUrlResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertContains

/**
 * Unit tests for StorageService.
 */
class StorageServiceTest {

    private val restTemplate: RestTemplate = mockk()
    private val storageUrl = "https://storage.example.com"
    private val serviceKey = "test-service-key"

    // We need to use reflection or create a testable version
    // For simplicity, we'll test the service behavior through its public methods
    private val storageService = StorageService(storageUrl, serviceKey).apply {
        // Set the mocked RestTemplate using reflection
        val field = this::class.java.getDeclaredField("restTemplate")
        field.isAccessible = true
        field.set(this, restTemplate)
    }

    @Test
    fun `createSignedUrl should return absolute URL when Supabase returns relative path`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val expiresIn = 3600
        val signedFragment = "/object/sign/test-bucket/images/test.jpg?token=abc123"

        val requestSlot = slot<HttpEntity<Map<String, Int>>>()

        every {
            restTemplate.postForEntity(
                any<String>(),
                capture(requestSlot),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(SignedUrlResponse(signedFragment))

        // When
        val result = storageService.createSignedUrl(bucket, path, expiresIn)

        // Then
        assertNotNull(result)
        assertTrue(result.startsWith("http"))
        assertContains(result, "storage.example.com")

        // Verify request
        val capturedRequest = requestSlot.captured
        assertEquals(expiresIn, capturedRequest.body?.get("expiresIn"))
        assertEquals("Bearer $serviceKey", capturedRequest.headers["Authorization"]?.first())
        assertEquals(serviceKey, capturedRequest.headers["apikey"]?.first())
    }

    @Test
    fun `createSignedUrl should return URL as-is when Supabase returns absolute URL`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val expiresIn = 3600
        val absoluteUrl = "https://storage.example.com/signed-url?token=xyz789"

        every {
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<Map<String, Int>>>(),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(SignedUrlResponse(absoluteUrl))

        // When
        val result = storageService.createSignedUrl(bucket, path, expiresIn)

        // Then
        assertEquals(absoluteUrl, result)
    }

    @Test
    fun `createSignedUrl should use default expiresIn when not specified`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"
        val signedUrl = "https://storage.example.com/signed-url"

        val requestSlot = slot<HttpEntity<Map<String, Int>>>()

        every {
            restTemplate.postForEntity(
                any<String>(),
                capture(requestSlot),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(SignedUrlResponse(signedUrl))

        // When
        val result = storageService.createSignedUrl(bucket, path)

        // Then
        assertNotNull(result)
        assertEquals(3600, requestSlot.captured.body?.get("expiresIn"))
    }

    @Test
    fun `createSignedUrl should throw exception when response body is null`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        every {
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<Map<String, Int>>>(),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(null)

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            storageService.createSignedUrl(bucket, path)
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.statusCode)
        assertContains(exception.reason ?: "", "Missing signedURL in response")
    }

    @Test
    fun `createSignedUrl should throw exception when signedURL is null`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        every {
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<Map<String, Int>>>(),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(SignedUrlResponse(null))

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            storageService.createSignedUrl(bucket, path)
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.statusCode)
        assertContains(exception.reason ?: "", "Missing signedURL in response")
    }

    @Test
    fun `createSignedUrl should handle path with leading slash`() {
        // Given
        val bucket = "test-bucket"
        val path = "/images/test.jpg"
        val signedUrl = "https://storage.example.com/signed-url"

        every {
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<Map<String, Int>>>(),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(SignedUrlResponse(signedUrl))

        // When
        val result = storageService.createSignedUrl(bucket, path)

        // Then
        assertNotNull(result)
    }

    @Test
    fun `createSignedUrl should handle path with multiple segments`() {
        // Given
        val bucket = "test-bucket"
        val path = "folder1/folder2/images/test.jpg"
        val signedUrl = "https://storage.example.com/signed-url"

        every {
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<Map<String, Int>>>(),
                SignedUrlResponse::class.java
            )
        } returns ResponseEntity.ok(SignedUrlResponse(signedUrl))

        // When
        val result = storageService.createSignedUrl(bucket, path)

        // Then
        assertNotNull(result)
    }

    @Test
    fun `deleteFile should successfully delete file`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        val requestSlot = slot<HttpEntity<Void>>()

        every {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                capture(requestSlot),
                String::class.java
            )
        } returns ResponseEntity.ok("")

        // When
        storageService.deleteFile(bucket, path)

        // Then
        verify(exactly = 1) {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        }

        // Verify headers
        val capturedRequest = requestSlot.captured
        assertEquals("Bearer $serviceKey", capturedRequest.headers["Authorization"]?.first())
        assertEquals(serviceKey, capturedRequest.headers["apikey"]?.first())
    }

    @Test
    fun `deleteFile should treat 404 as success`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        every {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        } throws HttpClientErrorException.NotFound.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            org.springframework.http.HttpHeaders.EMPTY,
            ByteArray(0),
            null
        )

        // When
        storageService.deleteFile(bucket, path)

        // Then
        verify(exactly = 1) {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        }
    }

    @Test
    fun `deleteFile should throw exception on other HTTP errors`() {
        // Given
        val bucket = "test-bucket"
        val path = "images/test.jpg"

        every {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        } throws HttpClientErrorException.Forbidden.create(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            org.springframework.http.HttpHeaders.EMPTY,
            ByteArray(0),
            null
        )

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            storageService.deleteFile(bucket, path)
        }
        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertContains(exception.reason ?: "", "Failed to delete file")
    }

    @Test
    fun `deleteFile should handle path with leading slash`() {
        // Given
        val bucket = "test-bucket"
        val path = "/images/test.jpg"

        every {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        } returns ResponseEntity.ok("")

        // When
        storageService.deleteFile(bucket, path)

        // Then
        verify(exactly = 1) {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        }
    }

    @Test
    fun `deleteFile should handle path with multiple segments`() {
        // Given
        val bucket = "test-bucket"
        val path = "folder1/folder2/images/test.jpg"

        every {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        } returns ResponseEntity.ok("")

        // When
        storageService.deleteFile(bucket, path)

        // Then
        verify(exactly = 1) {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.DELETE,
                any<HttpEntity<Void>>(),
                String::class.java
            )
        }
    }
}
