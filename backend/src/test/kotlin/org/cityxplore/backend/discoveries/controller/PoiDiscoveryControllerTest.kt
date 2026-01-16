package org.cityxplore.backend.discoveries.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.achievements.service.AchievementEvaluationService
import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryResponse
import org.cityxplore.backend.discoveries.service.PoiDiscoveryService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(PoiDiscoveryController::class)
class PoiDiscoveryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var poiDiscoveryService: PoiDiscoveryService

    @MockkBean
    private lateinit var achievementEvaluationService: AchievementEvaluationService

    /**
     * Creates a JWT mock with a valid UUID as the subject claim.
     * This is needed because JwtUtils.extractUserId expects a UUID in the 'sub' claim.
     */
    private fun jwtWithSubject(userId: UUID) = jwt().jwt { builder ->
        builder.subject(userId.toString())
    }

    @Test
    fun `discoverPoi should return 201 when successfully discovering a POI`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()

        val discoveryResponse = UserPoiDiscoveryResponse(
            poiId = poiId,
            discoveredAt = discoveredAt,
            favorite = false
        )

        every { poiDiscoveryService.discoverPoi(userId, poiId) } returns discoveryResponse

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.poiId").value(poiId.toString()))
            .andExpect(jsonPath("$.favorite").value(false))

        verify(exactly = 1) { poiDiscoveryService.discoverPoi(userId, poiId) }
    }

    @Test
    fun `discoverPoi should return 404 when POI does not exist`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        every {
            poiDiscoveryService.discoverPoi(
                userId,
                poiId
            )
        } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "POI not found"
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("POI not found"))

        verify(exactly = 1) { poiDiscoveryService.discoverPoi(userId, poiId) }
    }

    @Test
    fun `discoverPoi should return 409 when POI already discovered`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every {
            poiDiscoveryService.discoverPoi(
                userId,
                poiId
            )
        } throws ResponseStatusException(
            HttpStatus.CONFLICT,
            "Already discovered"
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Already discovered"))

        verify(exactly = 1) { poiDiscoveryService.discoverPoi(userId, poiId) }
    }

    @Test
    fun `discoverPoi should return 404 when user not found after discovery`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every {
            poiDiscoveryService.discoverPoi(
                userId,
                poiId
            )
        } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "User not found"
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("User not found"))

        verify(exactly = 1) { poiDiscoveryService.discoverPoi(userId, poiId) }
    }

    @Test
    fun `getUserDiscoveries should return list of user discoveries`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId1 = UUID.randomUUID()
        val poiId2 = UUID.randomUUID()
        val discoveredAt1 = LocalDateTime.now().minusDays(2)
        val discoveredAt2 = LocalDateTime.now().minusDays(1)

        val discoveries = listOf(
            UserPoiDiscoveryResponse(
                poiId = poiId1,
                discoveredAt = discoveredAt1,
                favorite = true
            ),
            UserPoiDiscoveryResponse(
                poiId = poiId2,
                discoveredAt = discoveredAt2,
                favorite = false
            )
        )

        every { poiDiscoveryService.getUserDiscoveries(userId) } returns discoveries

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries")
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].poiId").value(poiId1.toString()))
            .andExpect(jsonPath("$[0].favorite").value(true))
            .andExpect(jsonPath("$[1].poiId").value(poiId2.toString()))
            .andExpect(jsonPath("$[1].favorite").value(false))

        verify(exactly = 1) { poiDiscoveryService.getUserDiscoveries(userId) }
    }

    @Test
    fun `getUserDiscoveries should return empty list when user has no discoveries`() {
        // Given
        val userId = UUID.randomUUID()

        every { poiDiscoveryService.getUserDiscoveries(userId) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries")
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { poiDiscoveryService.getUserDiscoveries(userId) }
    }

    @Test
    fun `getUserDiscovery should return specific discovery when found`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()

        val discovery = UserPoiDiscoveryResponse(
            poiId = poiId,
            discoveredAt = discoveredAt,
            favorite = true
        )

        every { poiDiscoveryService.getUserDiscovery(userId, poiId) } returns discovery

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries/{poiId}", poiId)
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.poiId").value(poiId.toString()))
            .andExpect(jsonPath("$.favorite").value(true))

        verify(exactly = 1) { poiDiscoveryService.getUserDiscovery(userId, poiId) }
    }

    @Test
    fun `getUserDiscovery should return 404 when discovery not found`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every {
            poiDiscoveryService.getUserDiscovery(
                userId,
                poiId
            )
        } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Discovery not found"
        )

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries/{poiId}", poiId)
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Discovery not found"))

        verify(exactly = 1) { poiDiscoveryService.getUserDiscovery(userId, poiId) }
    }

    @Test
    fun `discoverPoi should require authentication`() {
        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getUserDiscoveries should require authentication`() {
        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getUserDiscovery should require authentication`() {
        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries/{poiId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }
}
