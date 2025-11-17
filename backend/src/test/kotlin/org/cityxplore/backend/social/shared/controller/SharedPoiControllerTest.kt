package org.cityxplore.backend.social.shared.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.social.shared.dto.CustomPoiData
import org.cityxplore.backend.social.shared.dto.SharedPoiResponse
import org.cityxplore.backend.social.shared.service.SharedPoiService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Integration tests for SharedPoiController.
 */
@WebMvcTest(SharedPoiController::class)
class SharedPoiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var sharedPoiService: SharedPoiService

    /**
     * Creates a JWT mock with a valid UUID as the subject claim.
     * This is needed because JwtUtils.extractUserId expects a UUID in the 'sub' claim.
     */
    private fun jwtWithSubject(userId: UUID = UUID.randomUUID()) = jwt().jwt { builder ->
        builder.subject(userId.toString())
    }

    // ========== sharePoi Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `sharePoi should return 201 when sharing existing POI with friend`() {
        // Given
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val response = SharedPoiResponse(
            id = sharedPoiId,
            sharerId = UUID.randomUUID(),
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = "Check this out!",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiService.sharePoi(any(), any()) } returns response

        val requestBody = """
            {
                "recipientId": "$recipientId",
                "poiId": "$poiId",
                "message": "Check this out!"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(sharedPoiId.toString()))
            .andExpect(jsonPath("$.recipientId").value(recipientId.toString()))
            .andExpect(jsonPath("$.poiId").value(poiId.toString()))
            .andExpect(jsonPath("$.message").value("Check this out!"))
            .andExpect(jsonPath("$.viewedAt").isEmpty)

        verify(exactly = 1) { sharedPoiService.sharePoi(any(), any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `sharePoi should return 201 when sharing custom POI with friend`() {
        // Given
        val recipientId = UUID.randomUUID()
        val sharedPoiId = UUID.randomUUID()

        val customPoi = CustomPoiData(
            name = "Hidden Gem",
            description = "A secret spot",
            category = "Hidden Spots",
            latitude = 52.2297,
            longitude = 21.0122
        )

        val response = SharedPoiResponse(
            id = sharedPoiId,
            sharerId = UUID.randomUUID(),
            recipientId = recipientId,
            poiId = null,
            poiData = customPoi,
            message = "My favorite place!",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiService.sharePoi(any(), any()) } returns response

        val requestBody = """
            {
                "recipientId": "$recipientId",
                "customPoi": {
                    "name": "Hidden Gem",
                    "description": "A secret spot",
                    "category": "Hidden Spots",
                    "latitude": 52.2297,
                    "longitude": 21.0122
                },
                "message": "My favorite place!"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(sharedPoiId.toString()))
            .andExpect(jsonPath("$.poiId").isEmpty)
            .andExpect(jsonPath("$.poiData.name").value("Hidden Gem"))
            .andExpect(jsonPath("$.poiData.latitude").value(52.2297))
            .andExpect(jsonPath("$.poiData.longitude").value(21.0122))

        verify(exactly = 1) { sharedPoiService.sharePoi(any(), any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `sharePoi should return 400 when both poiId and customPoi are provided`() {
        // Given
        every { sharedPoiService.sharePoi(any(), any()) } throws ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Exactly one of poiId or customPoi must be provided"
        )

        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val requestBody = """
            {
                "recipientId": "$recipientId",
                "poiId": "$poiId",
                "customPoi": {
                    "name": "Test",
                    "description": "Test",
                    "category": "Custom",
                    "latitude": 52.0,
                    "longitude": 21.0
                }
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Exactly one of poiId or customPoi must be provided"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `sharePoi should return 400 when neither poiId nor customPoi are provided`() {
        // Given
        every { sharedPoiService.sharePoi(any(), any()) } throws ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Exactly one of poiId or customPoi must be provided"
        )

        val recipientId = UUID.randomUUID()

        val requestBody = """
            {
                "recipientId": "$recipientId"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "user")
    fun `sharePoi should return 403 when users are not friends`() {
        // Given
        every { sharedPoiService.sharePoi(any(), any()) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only share POIs with accepted friends"
        )

        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val requestBody = """
            {
                "recipientId": "$recipientId",
                "poiId": "$poiId"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("You can only share POIs with accepted friends"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `sharePoi should return 404 when POI not found`() {
        // Given
        every { sharedPoiService.sharePoi(any(), any()) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "POI not found"
        )

        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val requestBody = """
            {
                "recipientId": "$recipientId",
                "poiId": "$poiId"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("POI not found"))
    }

    @Test
    fun `sharePoi should return 401 when user is not authenticated`() {
        // When & Then
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val requestBody = """
            {
                "recipientId": "$recipientId",
                "poiId": "$poiId"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/shared-pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
    }

    // ========== getSharedPoi Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getSharedPoi should return 200 with shared POI details`() {
        // Given
        val sharedPoiId = UUID.randomUUID()
        val response = SharedPoiResponse(
            id = sharedPoiId,
            sharerId = UUID.randomUUID(),
            recipientId = UUID.randomUUID(),
            poiId = UUID.randomUUID(),
            poiData = null,
            message = "Test message",
            sharedAt = LocalDateTime.now(),
            viewedAt = null
        )

        every { sharedPoiService.getSharedPoiById(any(), sharedPoiId) } returns response

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/$sharedPoiId")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(sharedPoiId.toString()))
            .andExpect(jsonPath("$.message").value("Test message"))

        verify(exactly = 1) { sharedPoiService.getSharedPoiById(any(), sharedPoiId) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getSharedPoi should return 404 when shared POI not found`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.getSharedPoiById(any(), sharedPoiId) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Shared POI not found"
        )

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/$sharedPoiId")
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "user")
    fun `getSharedPoi should return 403 when user has no access`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.getSharedPoiById(any(), sharedPoiId) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You do not have access to this shared POI"
        )

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/$sharedPoiId")
                .with(jwtWithSubject())
        )
            .andExpect(status().isForbidden)
    }

    // ========== getSharedByMe Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getSharedByMe should return 200 with list of shared POIs`() {
        // Given
        val sharedPois = listOf(
            SharedPoiResponse(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "POI 1",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            ),
            SharedPoiResponse(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "POI 2",
                sharedAt = LocalDateTime.now(),
                viewedAt = LocalDateTime.now()
            )
        )

        every { sharedPoiService.getSharedByMe(any()) } returns sharedPois

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/sent")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].message").value("POI 1"))
            .andExpect(jsonPath("$[1].message").value("POI 2"))

        verify(exactly = 1) { sharedPoiService.getSharedByMe(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getSharedByMe should return empty list when no POIs shared`() {
        // Given
        every { sharedPoiService.getSharedByMe(any()) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/sent")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ========== getSharedToMe Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getSharedToMe should return 200 with list of received POIs`() {
        // Given
        val sharedPois = listOf(
            SharedPoiResponse(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "From friend 1",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            ),
            SharedPoiResponse(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "From friend 2",
                sharedAt = LocalDateTime.now(),
                viewedAt = LocalDateTime.now()
            )
        )

        every { sharedPoiService.getSharedToMe(any()) } returns sharedPois

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/received")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].message").value("From friend 1"))
            .andExpect(jsonPath("$[1].message").value("From friend 2"))

        verify(exactly = 1) { sharedPoiService.getSharedToMe(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getSharedToMe should return empty list when no POIs received`() {
        // Given
        every { sharedPoiService.getSharedToMe(any()) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/received")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ========== getUnviewedSharedToMe Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getUnviewedSharedToMe should return only unviewed POIs`() {
        // Given
        val unviewedPois = listOf(
            SharedPoiResponse(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "Unviewed 1",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            ),
            SharedPoiResponse(
                id = UUID.randomUUID(),
                sharerId = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                poiId = UUID.randomUUID(),
                poiData = null,
                message = "Unviewed 2",
                sharedAt = LocalDateTime.now(),
                viewedAt = null
            )
        )

        every { sharedPoiService.getUnviewedSharedToMe(any()) } returns unviewedPois

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/received/unviewed")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].viewedAt").isEmpty)
            .andExpect(jsonPath("$[1].viewedAt").isEmpty)

        verify(exactly = 1) { sharedPoiService.getUnviewedSharedToMe(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getUnviewedSharedToMe should return empty list when all POIs viewed`() {
        // Given
        every { sharedPoiService.getUnviewedSharedToMe(any()) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/shared-pois/received/unviewed")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ========== markViewed Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `markViewed should return 200 and update viewedAt timestamp`() {
        // Given
        val sharedPoiId = UUID.randomUUID()
        val viewedAt = LocalDateTime.now()

        val response = SharedPoiResponse(
            id = sharedPoiId,
            sharerId = UUID.randomUUID(),
            recipientId = UUID.randomUUID(),
            poiId = UUID.randomUUID(),
            poiData = null,
            message = "Test",
            sharedAt = LocalDateTime.now().minusHours(1),
            viewedAt = viewedAt
        )

        every { sharedPoiService.markViewed(any(), sharedPoiId) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois/$sharedPoiId/viewed")
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(sharedPoiId.toString()))
            .andExpect(jsonPath("$.viewedAt").isNotEmpty)

        verify(exactly = 1) { sharedPoiService.markViewed(any(), sharedPoiId) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `markViewed should return 404 when shared POI not found`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.markViewed(any(), sharedPoiId) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Shared POI not found"
        )

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois/$sharedPoiId/viewed")
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "user")
    fun `markViewed should return 403 when user is not recipient`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.markViewed(any(), sharedPoiId) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Not your shared POI"
        )

        // When & Then
        mockMvc.perform(
            post("/api/shared-pois/$sharedPoiId/viewed")
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isForbidden)
    }

    // ========== deleteSharedPoi Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `deleteSharedPoi should return 204 when deleted successfully`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.deleteSharedPoi(any(), sharedPoiId) } returns Unit

        // When & Then
        mockMvc.perform(
            delete("/api/shared-pois/$sharedPoiId")
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { sharedPoiService.deleteSharedPoi(any(), sharedPoiId) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `deleteSharedPoi should return 404 when shared POI not found`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.deleteSharedPoi(any(), sharedPoiId) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Shared POI not found"
        )

        // When & Then
        mockMvc.perform(
            delete("/api/shared-pois/$sharedPoiId")
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "user")
    fun `deleteSharedPoi should return 403 when user is not sharer`() {
        // Given
        val sharedPoiId = UUID.randomUUID()

        every { sharedPoiService.deleteSharedPoi(any(), sharedPoiId) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only delete POIs that you have shared"
        )

        // When & Then
        mockMvc.perform(
            delete("/api/shared-pois/$sharedPoiId")
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteSharedPoi should return 401 when user is not authenticated`() {
        // When & Then
        val sharedPoiId = UUID.randomUUID()

        mockMvc.perform(
            delete("/api/shared-pois/$sharedPoiId")
                .with(csrf())
        )
            .andExpect(status().isUnauthorized)
    }
}
