package org.cityxplore.backend.social.rankings.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.social.rankings.dto.RankingEntryResponse
import org.cityxplore.backend.social.rankings.service.RankingService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Integration tests for RankingController.
 */
@WebMvcTest(RankingController::class)
class RankingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var rankingService: RankingService

    /**
     * Creates a JWT mock with a valid UUID as the subject claim.
     * This is needed because JwtUtils.extractUserId expects a UUID in the 'sub' claim.
     */
    private fun jwtWithSubject(userId: UUID = UUID.randomUUID()) = jwt().jwt { builder ->
        builder.subject(userId.toString())
    }

    // ========== getGlobalRanking Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getGlobalRanking should return 200 with list of rankings`() {
        // Given
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val user3Id = UUID.randomUUID()

        val rankings = listOf(
            RankingEntryResponse(
                userId = user1Id,
                username = "top_player",
                avatarUrl = "https://example.com/avatar1.jpg",
                totalPoisDiscovered = 100,
                totalDistance = 5000.0,
                totalAchievementPoints = 1000,
                score = 15000.0,
                rank = 1
            ),
            RankingEntryResponse(
                userId = user2Id,
                username = "second_player",
                avatarUrl = null,
                totalPoisDiscovered = 80,
                totalDistance = 4000.0,
                totalAchievementPoints = 800,
                score = 12000.0,
                rank = 2
            ),
            RankingEntryResponse(
                userId = user3Id,
                username = "third_player",
                avatarUrl = "https://example.com/avatar3.jpg",
                totalPoisDiscovered = 60,
                totalDistance = 3000.0,
                totalAchievementPoints = 600,
                score = 9000.0,
                rank = 3
            )
        )

        every { rankingService.getGlobalRanking() } returns rankings

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[0].username").value("top_player"))
            .andExpect(jsonPath("$[0].score").value(15000.0))
            .andExpect(jsonPath("$[1].rank").value(2))
            .andExpect(jsonPath("$[1].username").value("second_player"))
            .andExpect(jsonPath("$[2].rank").value(3))
            .andExpect(jsonPath("$[2].username").value("third_player"))

        verify(exactly = 1) { rankingService.getGlobalRanking() }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getGlobalRanking should return 200 with empty list when no users exist`() {
        // Given
        every { rankingService.getGlobalRanking() } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { rankingService.getGlobalRanking() }
    }

    @Test
    fun `getGlobalRanking should return 401 when user is not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/rankings/global")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "user")
    fun `getGlobalRanking should return rankings with null avatarUrl`() {
        // Given
        val userId = UUID.randomUUID()
        val rankings = listOf(
            RankingEntryResponse(
                userId = userId,
                username = "no_avatar_user",
                avatarUrl = null,
                totalPoisDiscovered = 10,
                totalDistance = 500.0,
                totalAchievementPoints = 100,
                score = 1500.0,
                rank = 1
            )
        )

        every { rankingService.getGlobalRanking() } returns rankings

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].avatarUrl").isEmpty)

        verify(exactly = 1) { rankingService.getGlobalRanking() }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getGlobalRanking should handle users with tied scores`() {
        // Given
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()

        val rankings = listOf(
            RankingEntryResponse(
                userId = user1Id,
                username = "user_one",
                avatarUrl = null,
                totalPoisDiscovered = 10,
                totalDistance = 100.0,
                totalAchievementPoints = 100,
                score = 1000.0,
                rank = 5
            ),
            RankingEntryResponse(
                userId = user2Id,
                username = "user_two",
                avatarUrl = null,
                totalPoisDiscovered = 10,
                totalDistance = 100.0,
                totalAchievementPoints = 100,
                score = 1000.0,
                rank = 6
            )
        )

        every { rankingService.getGlobalRanking() } returns rankings

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].score").value(1000.0))
            .andExpect(jsonPath("$[1].score").value(1000.0))
            .andExpect(jsonPath("$[0].rank").value(5))
            .andExpect(jsonPath("$[1].rank").value(6))
    }

    // ========== getFriendsRanking Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getFriendsRanking should return 200 with list of friend rankings`() {
        // Given
        val userId = UUID.randomUUID()
        val friend1Id = UUID.randomUUID()
        val friend2Id = UUID.randomUUID()

        val rankings = listOf(
            RankingEntryResponse(
                userId = friend1Id,
                username = "best_friend",
                avatarUrl = "https://example.com/friend1.jpg",
                totalPoisDiscovered = 50,
                totalDistance = 2500.0,
                totalAchievementPoints = 500,
                score = 7500.0,
                rank = 1
            ),
            RankingEntryResponse(
                userId = userId,
                username = "current_user",
                avatarUrl = null,
                totalPoisDiscovered = 40,
                totalDistance = 2000.0,
                totalAchievementPoints = 400,
                score = 6000.0,
                rank = 2
            ),
            RankingEntryResponse(
                userId = friend2Id,
                username = "another_friend",
                avatarUrl = "https://example.com/friend2.jpg",
                totalPoisDiscovered = 30,
                totalDistance = 1500.0,
                totalAchievementPoints = 300,
                score = 4500.0,
                rank = 3
            )
        )

        every { rankingService.getFriendsRanking(any()) } returns rankings

        // When & Then
        mockMvc.perform(
            get("/api/rankings/friends")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[0].username").value("best_friend"))
            .andExpect(jsonPath("$[1].rank").value(2))
            .andExpect(jsonPath("$[1].username").value("current_user"))
            .andExpect(jsonPath("$[2].rank").value(3))
            .andExpect(jsonPath("$[2].username").value("another_friend"))

        verify(exactly = 1) { rankingService.getFriendsRanking(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getFriendsRanking should return 200 with only current user when no friends`() {
        // Given
        val userId = UUID.randomUUID()

        val rankings = listOf(
            RankingEntryResponse(
                userId = userId,
                username = "lonely_user",
                avatarUrl = null,
                totalPoisDiscovered = 10,
                totalDistance = 500.0,
                totalAchievementPoints = 100,
                score = 1500.0,
                rank = 1
            )
        )

        every { rankingService.getFriendsRanking(any()) } returns rankings

        // When & Then
        mockMvc.perform(
            get("/api/rankings/friends")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[0].username").value("lonely_user"))

        verify(exactly = 1) { rankingService.getFriendsRanking(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getFriendsRanking should return 200 with empty list when user not found`() {
        // Given
        every { rankingService.getFriendsRanking(any()) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/rankings/friends")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { rankingService.getFriendsRanking(any()) }
    }

    @Test
    fun `getFriendsRanking should return 401 when user is not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/rankings/friends")
        )
            .andExpect(status().isUnauthorized)
    }

    // ========== getMyGlobalRank Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getMyGlobalRank should return 200 with user's global rank`() {
        // Given
        val userId = UUID.randomUUID()

        val ranking = RankingEntryResponse(
            userId = userId,
            username = "test_user",
            avatarUrl = "https://example.com/avatar.jpg",
            totalPoisDiscovered = 25,
            totalDistance = 1250.0,
            totalAchievementPoints = 250,
            score = 3750.0,
            rank = 42
        )

        every { rankingService.getUserGlobalRank(any()) } returns ranking

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global/me")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.username").value("test_user"))
            .andExpect(jsonPath("$.rank").value(42))
            .andExpect(jsonPath("$.score").value(3750.0))
            .andExpect(jsonPath("$.totalPoisDiscovered").value(25))
            .andExpect(jsonPath("$.totalDistance").value(1250.0))
            .andExpect(jsonPath("$.totalAchievementPoints").value(250))

        verify(exactly = 1) { rankingService.getUserGlobalRank(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getMyGlobalRank should return 404 when user not found`() {
        // Given
        every { rankingService.getUserGlobalRank(any()) } returns null

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global/me")
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)

        verify(exactly = 1) { rankingService.getUserGlobalRank(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getMyGlobalRank should return 404 when user is inactive`() {
        // Given
        every { rankingService.getUserGlobalRank(any()) } returns null

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global/me")
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getMyGlobalRank should return 401 when user is not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/rankings/global/me")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "user")
    fun `getMyGlobalRank should return rank 1 for top user`() {
        // Given
        val userId = UUID.randomUUID()

        val ranking = RankingEntryResponse(
            userId = userId,
            username = "top_user",
            avatarUrl = null,
            totalPoisDiscovered = 1000,
            totalDistance = 50000.0,
            totalAchievementPoints = 5000,
            score = 100000.0,
            rank = 1
        )

        every { rankingService.getUserGlobalRank(any()) } returns ranking

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global/me")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rank").value(1))
            .andExpect(jsonPath("$.score").value(100000.0))
    }

    @Test
    @WithMockUser(username = "user")
    fun `getMyGlobalRank should handle user with zero stats`() {
        // Given
        val userId = UUID.randomUUID()

        val ranking = RankingEntryResponse(
            userId = userId,
            username = "new_user",
            avatarUrl = null,
            totalPoisDiscovered = 0,
            totalDistance = 0.0,
            totalAchievementPoints = 0,
            score = 0.0,
            rank = 9999
        )

        every { rankingService.getUserGlobalRank(any()) } returns ranking

        // When & Then
        mockMvc.perform(
            get("/api/rankings/global/me")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPoisDiscovered").value(0))
            .andExpect(jsonPath("$.totalDistance").value(0.0))
            .andExpect(jsonPath("$.totalAchievementPoints").value(0))
            .andExpect(jsonPath("$.score").value(0.0))
            .andExpect(jsonPath("$.rank").value(9999))
    }
}
