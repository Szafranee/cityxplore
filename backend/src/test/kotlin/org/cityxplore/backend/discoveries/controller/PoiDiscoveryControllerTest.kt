package org.cityxplore.backend.discoveries.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(PoiDiscoveryController::class)
@Import(org.cityxplore.backend.discoveries.service.PoiDiscoveryService::class)
class PoiDiscoveryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userPoiDiscoveryRepository: UserPoiDiscoveryRepository

    @MockkBean
    private lateinit var poiRepository: PointOfInterestRepository

    @MockkBean
    private lateinit var userRepository: UserRepository

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

        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = false
        )

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiDiscoveryRepository.save(any()) } returns discovery
        every { userRepository.incrementPoisDiscovered(userId) } returns 1

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

        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 1) { userPoiDiscoveryRepository.save(any()) }
        verify(exactly = 1) { userRepository.incrementPoisDiscovered(userId) }
    }

    @Test
    fun `discoverPoi should return 404 when POI does not exist`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        every { poiRepository.existsById(poiId) } returns false

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("POI not found"))

        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 0) { userPoiDiscoveryRepository.save(any()) }
    }

    @Test
    fun `discoverPoi should return 409 when POI already discovered`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiDiscoveryRepository.save(any()) } throws DataIntegrityViolationException("Duplicate")

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Already discovered"))

        verify(exactly = 1) { poiRepository.existsById(poiId) }
        verify(exactly = 1) { userPoiDiscoveryRepository.save(any()) }
    }

    @Test
    fun `discoverPoi should return 404 when user not found after discovery`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()

        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = false
        )

        every { poiRepository.existsById(poiId) } returns true
        every { userPoiDiscoveryRepository.save(any()) } returns discovery
        every { userRepository.incrementPoisDiscovered(userId) } returns 0

        // When & Then
        mockMvc.perform(
            post("/api/pois/{poiId}/discover", poiId)
                .with(jwtWithSubject(userId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("User not found"))

        verify(exactly = 1) { userRepository.incrementPoisDiscovered(userId) }
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
            UserPoiDiscovery(
                id = UUID.randomUUID(),
                userId = userId,
                poiId = poiId1,
                discoveredAt = discoveredAt1,
                isFavorite = true
            ),
            UserPoiDiscovery(
                id = UUID.randomUUID(),
                userId = userId,
                poiId = poiId2,
                discoveredAt = discoveredAt2,
                isFavorite = false
            )
        )

        every { userPoiDiscoveryRepository.findAllByUserId(userId) } returns discoveries

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

        verify(exactly = 1) { userPoiDiscoveryRepository.findAllByUserId(userId) }
    }

    @Test
    fun `getUserDiscoveries should return empty list when user has no discoveries`() {
        // Given
        val userId = UUID.randomUUID()

        every { userPoiDiscoveryRepository.findAllByUserId(userId) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries")
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { userPoiDiscoveryRepository.findAllByUserId(userId) }
    }

    @Test
    fun `getUserDiscovery should return specific discovery when found`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()

        val discovery = UserPoiDiscovery(
            id = UUID.randomUUID(),
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = true
        )

        every { userPoiDiscoveryRepository.findByUserIdAndPoiId(userId, poiId) } returns discovery

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries/{poiId}", poiId)
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.poiId").value(poiId.toString()))
            .andExpect(jsonPath("$.favorite").value(true))

        verify(exactly = 1) { userPoiDiscoveryRepository.findByUserIdAndPoiId(userId, poiId) }
    }

    @Test
    fun `getUserDiscovery should return 404 when discovery not found`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        every { userPoiDiscoveryRepository.findByUserIdAndPoiId(userId, poiId) } returns null

        // When & Then
        mockMvc.perform(
            get("/api/pois/discoveries/{poiId}", poiId)
                .with(jwtWithSubject(userId))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Discovery not found"))

        verify(exactly = 1) { userPoiDiscoveryRepository.findByUserIdAndPoiId(userId, poiId) }
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
