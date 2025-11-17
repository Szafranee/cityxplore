package org.cityxplore.backend.achievements.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.cityxplore.backend.achievements.dto.CreateAchievementRequest
import org.cityxplore.backend.achievements.dto.UpdateAchievementRequest
import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional
import java.util.UUID

@WebMvcTest(AchievementAdminController::class)
@EnableMethodSecurity
@DisplayName("AchievementAdminController Tests")
class AchievementAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var achievementRepository: AchievementRepository

    private val testAchievementId = UUID.randomUUID()
    private val testAchievement = Achievement(
        id = testAchievementId,
        name = "First Discovery",
        description = "Discover your first POI",
        category = "Explorer",
        criteria = mapOf("type" to "discoveries", "count" to 1),
        iconUrl = "https://example.com/icons/first-discovery.png",
        points = 10,
        isActive = true
    )

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("create should create new achievement and return 201")
    fun `create should create new achievement and return 201`() {
        // Given
        val request = CreateAchievementRequest(
            name = "First Discovery",
            description = "Discover your first POI",
            category = "Explorer",
            iconUrl = "https://example.com/icons/first-discovery.png",
            points = 10
        )
        every { achievementRepository.save(any()) } returns testAchievement

        // When & Then
        mockMvc.perform(
            post("/api/admin/achievements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(testAchievementId.toString()))
            .andExpect(jsonPath("$.name").value("First Discovery"))
            .andExpect(jsonPath("$.description").value("Discover your first POI"))
            .andExpect(jsonPath("$.category").value("Explorer"))
            .andExpect(jsonPath("$.points").value(10))

        verify { achievementRepository.save(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("create should return 400 when validation fails")
    fun `create should return 400 when validation fails`() {
        // Given - name is blank
        val request = CreateAchievementRequest(
            name = "",
            description = "Discover your first POI",
            category = "Explorer",
            iconUrl = "https://example.com/icons/first-discovery.png",
            points = 10
        )

        // When & Then
        mockMvc.perform(
            post("/api/admin/achievements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.name").exists())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("create should return 400 when points are negative")
    fun `create should return 400 when points are negative`() {
        // Given
        val request = CreateAchievementRequest(
            name = "Invalid Achievement",
            description = "This has negative points",
            category = "Explorer",
            iconUrl = "https://example.com/icons/invalid.png",
            points = -5
        )

        // When & Then
        mockMvc.perform(
            post("/api/admin/achievements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.points").exists())
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("create should return 403 for non-admin user")
    fun `create should return 403 for non-admin user`() {
        // Given
        val request = CreateAchievementRequest(
            name = "First Discovery",
            description = "Discover your first POI",
            category = "Explorer",
            iconUrl = "https://example.com/icons/first-discovery.png",
            points = 10
        )

        // When & Then
        mockMvc.perform(
            post("/api/admin/achievements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access Denied"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("update should update existing achievement and return 200")
    fun `update should update existing achievement and return 200`() {
        // Given
        val request = UpdateAchievementRequest(
            name = "Updated Discovery",
            description = "Updated description",
            category = "Explorer Pro",
            iconUrl = "https://example.com/icons/updated.png",
            points = 20
        )
        val updated = testAchievement.copy(
            name = "Updated Discovery",
            description = "Updated description",
            category = "Explorer Pro",
            iconUrl = "https://example.com/icons/updated.png",
            points = 20
        )
        every { achievementRepository.findById(testAchievementId) } returns Optional.of(testAchievement)
        every { achievementRepository.save(any()) } returns updated

        // When & Then
        mockMvc.perform(
            patch("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Discovery"))
            .andExpect(jsonPath("$.description").value("Updated description"))
            .andExpect(jsonPath("$.category").value("Explorer Pro"))
            .andExpect(jsonPath("$.points").value(20))

        verify { achievementRepository.findById(testAchievementId) }
        verify { achievementRepository.save(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("update should return 404 when achievement not found")
    fun `update should return 404 when achievement not found`() {
        // Given
        val request = UpdateAchievementRequest(
            name = "Updated Discovery",
            description = "Updated description",
            category = "Explorer Pro",
            iconUrl = "https://example.com/icons/updated.png",
            points = 20
        )
        every { achievementRepository.findById(testAchievementId) } returns Optional.empty()

        // When & Then
        mockMvc.perform(
            patch("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Achievement not found"))

        verify { achievementRepository.findById(testAchievementId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("update should return 400 when validation fails")
    fun `update should return 400 when validation fails`() {
        // Given - points are negative
        val request = UpdateAchievementRequest(
            name = "Updated Discovery",
            description = "Updated description",
            category = "Explorer Pro",
            iconUrl = "https://example.com/icons/updated.png",
            points = -10
        )

        // When & Then
        mockMvc.perform(
            patch("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.points").exists())
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("update should return 403 for non-admin user")
    fun `update should return 403 for non-admin user`() {
        // Given
        val request = UpdateAchievementRequest(
            name = "Updated Discovery",
            description = "Updated description",
            category = "Explorer Pro",
            iconUrl = "https://example.com/icons/updated.png",
            points = 20
        )

        // When & Then
        mockMvc.perform(
            patch("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access Denied"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("getCategories should return list of distinct categories")
    fun `getCategories should return list of distinct categories`() {
        // Given
        val categories = listOf("Explorer", "Social", "Collector")
        every { achievementRepository.findDistinctCategories() } returns categories

        // When & Then
        mockMvc.perform(
            get("/api/admin/achievements/categories")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0]").value("Explorer"))
            .andExpect(jsonPath("$[1]").value("Social"))
            .andExpect(jsonPath("$[2]").value("Collector"))

        verify { achievementRepository.findDistinctCategories() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("getCategories should return empty list when no categories exist")
    fun `getCategories should return empty list when no categories exist`() {
        // Given
        every { achievementRepository.findDistinctCategories() } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/admin/achievements/categories")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        verify { achievementRepository.findDistinctCategories() }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("getCategories should return 403 for non-admin user")
    fun `getCategories should return 403 for non-admin user`() {
        // When & Then
        mockMvc.perform(
            get("/api/admin/achievements/categories")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access Denied"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("delete should delete achievement and return 204")
    fun `delete should delete achievement and return 204`() {
        // Given
        justRun { achievementRepository.deleteById(testAchievementId) }

        // When & Then
        mockMvc.perform(
            delete("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        verify { achievementRepository.deleteById(testAchievementId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("delete should return 404 when achievement not found")
    fun `delete should return 404 when achievement not found`() {
        // Given
        every { achievementRepository.deleteById(testAchievementId) } throws EmptyResultDataAccessException(1)

        // When & Then
        mockMvc.perform(
            delete("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Achievement not found"))

        verify { achievementRepository.deleteById(testAchievementId) }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("delete should return 403 for non-admin user")
    fun `delete should return 403 for non-admin user`() {
        // When & Then
        mockMvc.perform(
            delete("/api/admin/achievements/{id}", testAchievementId)
                .with(csrf())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access Denied"))
    }

    @Test
    @DisplayName("create should return 401 when not authenticated")
    fun `create should return 401 when not authenticated`() {
        // Given
        val request = CreateAchievementRequest(
            name = "First Discovery",
            description = "Discover your first POI",
            category = "Explorer",
            iconUrl = "https://example.com/icons/first-discovery.png",
            points = 10
        )

        // When & Then
        mockMvc.perform(
            post("/api/admin/achievements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }
}
