package org.cityxplore.backend.achievements.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.cityxplore.backend.achievements.dto.AchievementResponse
import org.cityxplore.backend.achievements.dto.UserAchievementResponse
import org.cityxplore.backend.achievements.service.AchievementService
import org.cityxplore.backend.achievements.service.AchievementService.AchievementGrantResult
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(AchievementController::class)
@DisplayName("AchievementController Tests")
class AchievementControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var achievementService: AchievementService


    private val testAchievementId = UUID.randomUUID()
    private val testUserId = UUID.randomUUID()
    private val testAchievementResponse = AchievementResponse(
        id = testAchievementId,
        name = "First Discovery",
        description = "Discover your first POI",
        category = "Explorer",
        iconUrl = "https://example.com/icons/first-discovery.png",
        points = 10
    )
    private val testUserAchievementResponse = UserAchievementResponse(
        achievement = testAchievementResponse,
        achievedAt = LocalDateTime.now(),
        progress = mapOf("current" to 1, "total" to 1)
    )

    @Test
    @DisplayName("getAllAchievements should return all achievements")
    fun `getAllAchievements should return all achievements`() {
        // Given
        val achievements = listOf(
            testAchievementResponse,
            AchievementResponse(
                id = UUID.randomUUID(),
                name = "Ten Discoveries",
                description = "Discover 10 POIs",
                category = "Explorer",
                iconUrl = "https://example.com/icons/ten-discoveries.png",
                points = 50
            )
        )
        every { achievementService.getAllAchievements() } returns achievements

        // When & Then
        mockMvc.perform(
            get("/api/achievements")
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("First Discovery"))
            .andExpect(jsonPath("$[0].points").value(10))
            .andExpect(jsonPath("$[1].name").value("Ten Discoveries"))
            .andExpect(jsonPath("$[1].points").value(50))
    }

    @Test
    @DisplayName("getAllAchievements should return empty list when no achievements exist")
    fun `getAllAchievements should return empty list when no achievements exist`() {
        // Given
        every { achievementService.getAllAchievements() } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/achievements")
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @DisplayName("getUserAchievements should return user's achievements when authenticated")
    fun `getUserAchievements should return user's achievements when authenticated`() {
        // Given
        val userAchievements = listOf(testUserAchievementResponse)
        every { achievementService.getUserAchievements(testUserId) } returns userAchievements

        // When & Then
        mockMvc.perform(
            get("/api/achievements/mine")
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].achievement.name").value("First Discovery"))
            .andExpect(jsonPath("$[0].achievement.points").value(10))
    }

    @Test
    @DisplayName("getUserAchievements should return 401 when not authenticated")
    fun `getUserAchievements should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/achievements/mine")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("getUserAchievement should return specific achievement when found")
    fun `getUserAchievement should return specific achievement when found`() {
        // Given
        every {
            achievementService.getUserAchievement(
                testUserId,
                testAchievementId
            )
        } returns testUserAchievementResponse

        // When & Then
        mockMvc.perform(
            get("/api/achievements/mine/{achievementId}", testAchievementId)
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.achievement.id").value(testAchievementId.toString()))
            .andExpect(jsonPath("$.achievement.name").value("First Discovery"))
    }

    @Test
    @DisplayName("getUserAchievement should return 404 when achievement not found")
    fun `getUserAchievement should return 404 when achievement not found`() {
        // Given
        every { achievementService.getUserAchievement(testUserId, testAchievementId) } returns null

        // When & Then
        mockMvc.perform(
            get("/api/achievements/mine/{achievementId}", testAchievementId)
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("grantAchievement should return 201 when achievement is newly granted")
    fun `grantAchievement should return 201 when achievement is newly granted`() {
        // Given
        val result = AchievementGrantResult(dto = testUserAchievementResponse, created = true)
        every { achievementService.grantAchievement(testUserId, testAchievementId) } returns result

        // When & Then
        mockMvc.perform(
            post("/api/achievements/{achievementId}/grant", testAchievementId)
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.achievement.id").value(testAchievementId.toString()))
            .andExpect(jsonPath("$.achievement.name").value("First Discovery"))
    }

    @Test
    @DisplayName("grantAchievement should return 200 when achievement already exists")
    fun `grantAchievement should return 200 when achievement already exists`() {
        // Given
        val result = AchievementGrantResult(dto = testUserAchievementResponse, created = false)
        every { achievementService.grantAchievement(testUserId, testAchievementId) } returns result

        // When & Then
        mockMvc.perform(
            post("/api/achievements/{achievementId}/grant", testAchievementId)
                .with(jwt().jwt { it.subject(testUserId.toString()) })
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.achievement.id").value(testAchievementId.toString()))
            .andExpect(jsonPath("$.achievement.name").value("First Discovery"))
    }

    @Test
    @DisplayName("grantAchievement should return 401 when not authenticated")
    fun `grantAchievement should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(
            post("/api/achievements/{achievementId}/grant", testAchievementId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }
}
