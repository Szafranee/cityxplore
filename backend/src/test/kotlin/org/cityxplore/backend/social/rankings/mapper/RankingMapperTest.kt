package org.cityxplore.backend.social.rankings.mapper

import org.cityxplore.backend.social.rankings.repository.RankingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit tests for RankingMapper.
 */
class RankingMapperTest {

    @Test
    fun `toResponse should map RankingEntry with all fields correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val username = "john_doe"
        val avatarUrl = "https://example.com/avatars/john.jpg"
        val totalPoisDiscovered = 42
        val totalDistance = 1500.75
        val totalAchievementPoints = 350
        val score = 5000.25
        val rank = 1

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = username,
            avatarUrl = avatarUrl,
            totalPoisDiscovered = totalPoisDiscovered,
            totalDistance = totalDistance,
            totalAchievementPoints = totalAchievementPoints,
            score = score,
            rank = rank
        )

        // When
        val response = RankingMapper.toResponse(entry)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(username, response.username)
        assertEquals(avatarUrl, response.avatarUrl)
        assertEquals(totalPoisDiscovered, response.totalPoisDiscovered)
        assertEquals(totalDistance, response.totalDistance)
        assertEquals(totalAchievementPoints, response.totalAchievementPoints)
        assertEquals(score, response.score)
        assertEquals(rank, response.rank)
    }

    @Test
    fun `toResponse should map RankingEntry with null avatarUrl correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val username = "jane_doe"
        val totalPoisDiscovered = 10
        val totalDistance = 250.0
        val totalAchievementPoints = 100
        val score = 1250.0
        val rank = 15

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = username,
            avatarUrl = null,
            totalPoisDiscovered = totalPoisDiscovered,
            totalDistance = totalDistance,
            totalAchievementPoints = totalAchievementPoints,
            score = score,
            rank = rank
        )

        // When
        val response = RankingMapper.toResponse(entry)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(username, response.username)
        assertNull(response.avatarUrl)
        assertEquals(totalPoisDiscovered, response.totalPoisDiscovered)
        assertEquals(totalDistance, response.totalDistance)
        assertEquals(totalAchievementPoints, response.totalAchievementPoints)
        assertEquals(score, response.score)
        assertEquals(rank, response.rank)
    }

    @Test
    fun `toResponse should map RankingEntry with zero values correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val username = "new_user"
        val avatarUrl = "https://example.com/default-avatar.jpg"
        val totalPoisDiscovered = 0
        val totalDistance = 0.0
        val totalAchievementPoints = 0
        val score = 0.0
        val rank = 999

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = username,
            avatarUrl = avatarUrl,
            totalPoisDiscovered = totalPoisDiscovered,
            totalDistance = totalDistance,
            totalAchievementPoints = totalAchievementPoints,
            score = score,
            rank = rank
        )

        // When
        val response = RankingMapper.toResponse(entry)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(username, response.username)
        assertEquals(avatarUrl, response.avatarUrl)
        assertEquals(0, response.totalPoisDiscovered)
        assertEquals(0.0, response.totalDistance)
        assertEquals(0, response.totalAchievementPoints)
        assertEquals(0.0, response.score)
        assertEquals(999, response.rank)
    }

    @Test
    fun `toResponse should map RankingEntry with large values correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val username = "top_explorer"
        val avatarUrl = "https://example.com/avatars/explorer.jpg"
        val totalPoisDiscovered = 9999
        val totalDistance = 999999.99
        val totalAchievementPoints = 50000
        val score = 1500000.0
        val rank = 1

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = username,
            avatarUrl = avatarUrl,
            totalPoisDiscovered = totalPoisDiscovered,
            totalDistance = totalDistance,
            totalAchievementPoints = totalAchievementPoints,
            score = score,
            rank = rank
        )

        // When
        val response = RankingMapper.toResponse(entry)

        // Then
        assertEquals(userId, response.userId)
        assertEquals(username, response.username)
        assertEquals(avatarUrl, response.avatarUrl)
        assertEquals(9999, response.totalPoisDiscovered)
        assertEquals(999999.99, response.totalDistance)
        assertEquals(50000, response.totalAchievementPoints)
        assertEquals(1500000.0, response.score)
        assertEquals(1, response.rank)
    }

    @Test
    fun `toResponse should preserve decimal precision for distance and score`() {
        // Given
        val userId = UUID.randomUUID()
        val username = "precision_user"
        val avatarUrl = "https://example.com/avatars/precision.jpg"
        val totalPoisDiscovered = 25
        val totalDistance = 1234.56789
        val totalAchievementPoints = 150
        val score = 5678.91234
        val rank = 10

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = username,
            avatarUrl = avatarUrl,
            totalPoisDiscovered = totalPoisDiscovered,
            totalDistance = totalDistance,
            totalAchievementPoints = totalAchievementPoints,
            score = score,
            rank = rank
        )

        // When
        val response = RankingMapper.toResponse(entry)

        // Then
        assertEquals(1234.56789, response.totalDistance)
        assertEquals(5678.91234, response.score)
    }

    @Test
    fun `toResponse should map RankingEntry with special characters in username correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val username = "user_with-special.chars"
        val avatarUrl = "https://example.com/avatars/special.jpg"
        val totalPoisDiscovered = 5
        val totalDistance = 100.0
        val totalAchievementPoints = 50
        val score = 750.0
        val rank = 50

        val entry = RankingRepository.RankingEntry(
            userId = userId,
            username = username,
            avatarUrl = avatarUrl,
            totalPoisDiscovered = totalPoisDiscovered,
            totalDistance = totalDistance,
            totalAchievementPoints = totalAchievementPoints,
            score = score,
            rank = rank
        )

        // When
        val response = RankingMapper.toResponse(entry)

        // Then
        assertEquals("user_with-special.chars", response.username)
    }

    @Test
    fun `toResponse should map multiple entries with different ranks correctly`() {
        // Given
        val entries = listOf(
            RankingRepository.RankingEntry(
                userId = UUID.randomUUID(),
                username = "first_place",
                avatarUrl = "https://example.com/avatars/1.jpg",
                totalPoisDiscovered = 100,
                totalDistance = 5000.0,
                totalAchievementPoints = 1000,
                score = 15000.0,
                rank = 1
            ),
            RankingRepository.RankingEntry(
                userId = UUID.randomUUID(),
                username = "second_place",
                avatarUrl = "https://example.com/avatars/2.jpg",
                totalPoisDiscovered = 80,
                totalDistance = 4000.0,
                totalAchievementPoints = 800,
                score = 12000.0,
                rank = 2
            ),
            RankingRepository.RankingEntry(
                userId = UUID.randomUUID(),
                username = "third_place",
                avatarUrl = null,
                totalPoisDiscovered = 60,
                totalDistance = 3000.0,
                totalAchievementPoints = 600,
                score = 9000.0,
                rank = 3
            )
        )

        // When
        val responses = entries.map { RankingMapper.toResponse(it) }

        // Then
        assertEquals(3, responses.size)
        assertEquals(1, responses[0].rank)
        assertEquals("first_place", responses[0].username)
        assertEquals(2, responses[1].rank)
        assertEquals("second_place", responses[1].username)
        assertEquals(3, responses[2].rank)
        assertEquals("third_place", responses[2].username)
        assertNull(responses[2].avatarUrl)
    }

    @Test
    fun `toResponse should map RankingEntry with tied scores but different ranks`() {
        // Given
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val score = 1000.0

        val entry1 = RankingRepository.RankingEntry(
            userId = userId1,
            username = "user_one",
            avatarUrl = null,
            totalPoisDiscovered = 10,
            totalDistance = 100.0,
            totalAchievementPoints = 100,
            score = score,
            rank = 5
        )

        val entry2 = RankingRepository.RankingEntry(
            userId = userId2,
            username = "user_two",
            avatarUrl = null,
            totalPoisDiscovered = 10,
            totalDistance = 100.0,
            totalAchievementPoints = 100,
            score = score,
            rank = 6
        )

        // When
        val response1 = RankingMapper.toResponse(entry1)
        val response2 = RankingMapper.toResponse(entry2)

        // Then
        assertEquals(score, response1.score)
        assertEquals(score, response2.score)
        assertEquals(5, response1.rank)
        assertEquals(6, response2.rank)
    }
}
