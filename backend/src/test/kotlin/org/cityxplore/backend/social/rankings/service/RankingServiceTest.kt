package org.cityxplore.backend.social.rankings.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.social.rankings.config.RankingConfig
import org.cityxplore.backend.social.rankings.repository.RankingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit tests for RankingService.
 */
class RankingServiceTest {

    private lateinit var rankingRepository: RankingRepository
    private lateinit var rankingConfig: RankingConfig
    private lateinit var rankingService: RankingService

    @BeforeEach
    fun setUp() {
        rankingRepository = mockk()
        rankingConfig = mockk()
        rankingService = RankingService(rankingRepository, rankingConfig)

        // Default config values
        every { rankingConfig.poiWeight } returns 10.0
        every { rankingConfig.distanceWeight } returns 1.0
        every { rankingConfig.achievementWeight } returns 5.0
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ========== getGlobalRanking Tests ==========

    @Test
    fun `getGlobalRanking should return list of all users sorted by score`() {
        // Given
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val user3Id = UUID.randomUUID()

        val entries = listOf(
            RankingRepository.RankingEntry(
                userId = user1Id,
                username = "top_player",
                avatarUrl = "https://example.com/avatar1.jpg",
                totalPoisDiscovered = 100,
                totalDistance = 5000.0,
                totalAchievementPoints = 1000,
                score = 15000.0,
                rank = 1
            ),
            RankingRepository.RankingEntry(
                userId = user2Id,
                username = "second_player",
                avatarUrl = null,
                totalPoisDiscovered = 80,
                totalDistance = 4000.0,
                totalAchievementPoints = 800,
                score = 12000.0,
                rank = 2
            ),
            RankingRepository.RankingEntry(
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

        every {
            rankingRepository.calculateGlobalRanking(
                poiWeight = 10.0,
                distanceWeight = 1.0,
                achievementWeight = 5.0
            )
        } returns entries

        // When
        val result = rankingService.getGlobalRanking()

        // Then
        assertEquals(3, result.size)
        assertEquals(1, result[0].rank)
        assertEquals("top_player", result[0].username)
        assertEquals(15000.0, result[0].score)
        assertEquals(2, result[1].rank)
        assertEquals("second_player", result[1].username)
        assertEquals(3, result[2].rank)
        assertEquals("third_player", result[2].username)

        verify(exactly = 1) {
            rankingRepository.calculateGlobalRanking(
                poiWeight = 10.0,
                distanceWeight = 1.0,
                achievementWeight = 5.0
            )
        }
    }

    @Test
    fun `getGlobalRanking should return empty list when no users exist`() {
        // Given
        every {
            rankingRepository.calculateGlobalRanking(
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns emptyList()

        // When
        val result = rankingService.getGlobalRanking()

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) {
            rankingRepository.calculateGlobalRanking(
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        }
    }

    @Test
    fun `getGlobalRanking should use correct weights from config`() {
        // Given
        every { rankingConfig.poiWeight } returns 20.0
        every { rankingConfig.distanceWeight } returns 2.0
        every { rankingConfig.achievementWeight } returns 10.0

        every {
            rankingRepository.calculateGlobalRanking(
                poiWeight = 20.0,
                distanceWeight = 2.0,
                achievementWeight = 10.0
            )
        } returns emptyList()

        // When
        rankingService.getGlobalRanking()

        // Then
        verify(exactly = 1) {
            rankingRepository.calculateGlobalRanking(
                poiWeight = 20.0,
                distanceWeight = 2.0,
                achievementWeight = 10.0
            )
        }
    }

    @Test
    fun `getGlobalRanking should handle users with same score but different ranks`() {
        // Given
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()

        val entries = listOf(
            RankingRepository.RankingEntry(
                userId = user1Id,
                username = "user_one",
                avatarUrl = null,
                totalPoisDiscovered = 10,
                totalDistance = 100.0,
                totalAchievementPoints = 100,
                score = 1000.0,
                rank = 5
            ),
            RankingRepository.RankingEntry(
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

        every {
            rankingRepository.calculateGlobalRanking(any(), any(), any())
        } returns entries

        // When
        val result = rankingService.getGlobalRanking()

        // Then
        assertEquals(2, result.size)
        assertEquals(1000.0, result[0].score)
        assertEquals(1000.0, result[1].score)
        assertEquals(5, result[0].rank)
        assertEquals(6, result[1].rank)
    }

    // ========== getFriendsRanking Tests ==========

    @Test
    fun `getFriendsRanking should return ranking of user and their friends`() {
        // Given
        val userId = UUID.randomUUID()
        val friend1Id = UUID.randomUUID()
        val friend2Id = UUID.randomUUID()

        val entries = listOf(
            RankingRepository.RankingEntry(
                userId = friend1Id,
                username = "best_friend",
                avatarUrl = "https://example.com/friend1.jpg",
                totalPoisDiscovered = 50,
                totalDistance = 2500.0,
                totalAchievementPoints = 500,
                score = 7500.0,
                rank = 1
            ),
            RankingRepository.RankingEntry(
                userId = userId,
                username = "current_user",
                avatarUrl = null,
                totalPoisDiscovered = 40,
                totalDistance = 2000.0,
                totalAchievementPoints = 400,
                score = 6000.0,
                rank = 2
            ),
            RankingRepository.RankingEntry(
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

        every {
            rankingRepository.calculateFriendsRanking(
                userId = userId,
                poiWeight = 10.0,
                distanceWeight = 1.0,
                achievementWeight = 5.0
            )
        } returns entries

        // When
        val result = rankingService.getFriendsRanking(userId)

        // Then
        assertEquals(3, result.size)
        assertEquals(1, result[0].rank)
        assertEquals("best_friend", result[0].username)
        assertEquals(2, result[1].rank)
        assertEquals("current_user", result[1].username)
        assertEquals(3, result[2].rank)
        assertEquals("another_friend", result[2].username)

        verify(exactly = 1) {
            rankingRepository.calculateFriendsRanking(
                userId = userId,
                poiWeight = 10.0,
                distanceWeight = 1.0,
                achievementWeight = 5.0
            )
        }
    }

    @Test
    fun `getFriendsRanking should return only current user when they have no friends`() {
        // Given
        val userId = UUID.randomUUID()

        val entries = listOf(
            RankingRepository.RankingEntry(
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

        every {
            rankingRepository.calculateFriendsRanking(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns entries

        // When
        val result = rankingService.getFriendsRanking(userId)

        // Then
        assertEquals(1, result.size)
        assertEquals(userId, result[0].userId)
        assertEquals(1, result[0].rank)
    }

    @Test
    fun `getFriendsRanking should return empty list when user not found`() {
        // Given
        val userId = UUID.randomUUID()

        every {
            rankingRepository.calculateFriendsRanking(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns emptyList()

        // When
        val result = rankingService.getFriendsRanking(userId)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFriendsRanking should use correct weights from config`() {
        // Given
        val userId = UUID.randomUUID()
        every { rankingConfig.poiWeight } returns 15.0
        every { rankingConfig.distanceWeight } returns 1.5
        every { rankingConfig.achievementWeight } returns 7.5

        every {
            rankingRepository.calculateFriendsRanking(
                userId = userId,
                poiWeight = 15.0,
                distanceWeight = 1.5,
                achievementWeight = 7.5
            )
        } returns emptyList()

        // When
        rankingService.getFriendsRanking(userId)

        // Then
        verify(exactly = 1) {
            rankingRepository.calculateFriendsRanking(
                userId = userId,
                poiWeight = 15.0,
                distanceWeight = 1.5,
                achievementWeight = 7.5
            )
        }
    }

    // ========== getUserGlobalRank Tests ==========

    @Test
    fun `getUserGlobalRank should return user's global rank`() {
        // Given
        val userId = UUID.randomUUID()

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = "test_user",
            avatarUrl = "https://example.com/avatar.jpg",
            totalPoisDiscovered = 25,
            totalDistance = 1250.0,
            totalAchievementPoints = 250,
            score = 3750.0,
            rank = 42
        )

        every {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = 10.0,
                distanceWeight = 1.0,
                achievementWeight = 5.0
            )
        } returns entry

        // When
        val result = rankingService.getUserGlobalRank(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result!!.userId)
        assertEquals("test_user", result.username)
        assertEquals(42, result.rank)
        assertEquals(3750.0, result.score)

        verify(exactly = 1) {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = 10.0,
                distanceWeight = 1.0,
                achievementWeight = 5.0
            )
        }
    }

    @Test
    fun `getUserGlobalRank should return null when user not found`() {
        // Given
        val userId = UUID.randomUUID()

        every {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns null

        // When
        val result = rankingService.getUserGlobalRank(userId)

        // Then
        assertNull(result)

        verify(exactly = 1) {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        }
    }

    @Test
    fun `getUserGlobalRank should return null when user is inactive`() {
        // Given
        val userId = UUID.randomUUID()

        every {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns null

        // When
        val result = rankingService.getUserGlobalRank(userId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getUserGlobalRank should use correct weights from config`() {
        // Given
        val userId = UUID.randomUUID()
        every { rankingConfig.poiWeight } returns 25.0
        every { rankingConfig.distanceWeight } returns 2.5
        every { rankingConfig.achievementWeight } returns 12.5

        every {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = 25.0,
                distanceWeight = 2.5,
                achievementWeight = 12.5
            )
        } returns null

        // When
        rankingService.getUserGlobalRank(userId)

        // Then
        verify(exactly = 1) {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = 25.0,
                distanceWeight = 2.5,
                achievementWeight = 12.5
            )
        }
    }

    @Test
    fun `getUserGlobalRank should handle user with rank 1`() {
        // Given
        val userId = UUID.randomUUID()

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = "top_user",
            avatarUrl = null,
            totalPoisDiscovered = 1000,
            totalDistance = 50000.0,
            totalAchievementPoints = 5000,
            score = 100000.0,
            rank = 1
        )

        every {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns entry

        // When
        val result = rankingService.getUserGlobalRank(userId)

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.rank)
        assertEquals(100000.0, result.score)
    }

    @Test
    fun `getUserGlobalRank should handle user with zero stats`() {
        // Given
        val userId = UUID.randomUUID()

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = "new_user",
            avatarUrl = null,
            totalPoisDiscovered = 0,
            totalDistance = 0.0,
            totalAchievementPoints = 0,
            score = 0.0,
            rank = 9999
        )

        every {
            rankingRepository.findGlobalRankForUser(
                userId = userId,
                poiWeight = any(),
                distanceWeight = any(),
                achievementWeight = any()
            )
        } returns entry

        // When
        val result = rankingService.getUserGlobalRank(userId)

        // Then
        assertNotNull(result)
        assertEquals(0, result!!.totalPoisDiscovered)
        assertEquals(0.0, result.totalDistance)
        assertEquals(0, result.totalAchievementPoints)
        assertEquals(0.0, result.score)
    }
}
