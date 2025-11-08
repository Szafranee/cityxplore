package org.cityxplore.backend.achievements.mapper

import org.cityxplore.backend.achievements.dto.CreateAchievementRequest
import org.cityxplore.backend.achievements.dto.UpdateAchievementRequest
import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.achievements.entity.UserAchievement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for AchievementMapper extension functions.
 */
class AchievementMapperTest {

    @Test
    fun `toDto should map Achievement to AchievementResponse correctly`() {
        // Given
        val achievementId = UUID.randomUUID()
        val achievement = Achievement(
            id = achievementId,
            name = "Explorer",
            description = "Discover 10 POIs",
            category = "Discovery",
            criteria = mapOf("type" to "discoveries", "count" to 10),
            iconUrl = "https://example.com/icons/explorer.png",
            points = 100,
            isActive = true
        )

        // When
        val response = achievement.toDto()

        // Then
        assertEquals(achievementId, response.id)
        assertEquals("Explorer", response.name)
        assertEquals("Discover 10 POIs", response.description)
        assertEquals("Discovery", response.category)
        assertEquals("https://example.com/icons/explorer.png", response.iconUrl)
        assertEquals(100, response.points)
    }

    @Test
    fun `toDto should map Achievement with null optional fields correctly`() {
        // Given
        val achievementId = UUID.randomUUID()
        val achievement = Achievement(
            id = achievementId,
            name = "Basic Achievement",
            description = "Complete basic task",
            category = null,
            criteria = emptyMap(),
            iconUrl = null,
            points = 10,
            isActive = true
        )

        // When
        val response = achievement.toDto()

        // Then
        assertEquals(achievementId, response.id)
        assertEquals("Basic Achievement", response.name)
        assertEquals("Complete basic task", response.description)
        assertNull(response.category)
        assertNull(response.iconUrl)
        assertEquals(10, response.points)
    }

    @Test
    fun `toEntity should create Achievement from CreateAchievementRequest correctly`() {
        // Given
        val request = CreateAchievementRequest(
            name = "New Achievement",
            description = "This is a new achievement",
            category = "Special",
            iconUrl = "https://example.com/icons/new.png",
            points = 50
        )

        // When
        val achievement = request.toEntity()

        // Then
        assertNull(achievement.id) // ID should be null before persistence
        assertEquals("New Achievement", achievement.name)
        assertEquals("This is a new achievement", achievement.description)
        assertEquals("Special", achievement.category)
        assertTrue(achievement.criteria.isEmpty()) // Should be empty map
        assertEquals("https://example.com/icons/new.png", achievement.iconUrl)
        assertEquals(50, achievement.points)
        assertTrue(achievement.isActive) // Should default to true
    }

    @Test
    fun `toEntity should handle request with null optional fields`() {
        // Given
        val request = CreateAchievementRequest(
            name = "Minimal Achievement",
            description = "Minimal description",
            category = null,
            iconUrl = null,
            points = 0
        )

        // When
        val achievement = request.toEntity()

        // Then
        assertNull(achievement.id)
        assertEquals("Minimal Achievement", achievement.name)
        assertEquals("Minimal description", achievement.description)
        assertNull(achievement.category)
        assertTrue(achievement.criteria.isEmpty())
        assertNull(achievement.iconUrl)
        assertEquals(0, achievement.points)
        assertTrue(achievement.isActive)
    }

    @Test
    fun `applyTo should update existing Achievement correctly`() {
        // Given
        val existingId = UUID.randomUUID()
        val existingAchievement = Achievement(
            id = existingId,
            name = "Old Name",
            description = "Old description",
            category = "Old Category",
            criteria = mapOf("old" to "criteria"),
            iconUrl = "https://example.com/icons/old.png",
            points = 10,
            isActive = true
        )

        val updateRequest = UpdateAchievementRequest(
            name = "Updated Name",
            description = "Updated description",
            category = "Updated Category",
            iconUrl = "https://example.com/icons/updated.png",
            points = 20
        )

        // When
        val updatedAchievement = updateRequest.applyTo(existingAchievement)

        // Then
        assertEquals(existingId, updatedAchievement.id) // ID should remain the same
        assertEquals("Updated Name", updatedAchievement.name)
        assertEquals("Updated description", updatedAchievement.description)
        assertEquals("Updated Category", updatedAchievement.category)
        assertEquals("https://example.com/icons/updated.png", updatedAchievement.iconUrl)
        assertEquals(20, updatedAchievement.points)
        // Criteria and isActive should remain unchanged
        assertEquals(existingAchievement.criteria, updatedAchievement.criteria)
        assertEquals(existingAchievement.isActive, updatedAchievement.isActive)
    }

    @Test
    fun `applyTo should update with null optional fields correctly`() {
        // Given
        val existingId = UUID.randomUUID()
        val existingAchievement = Achievement(
            id = existingId,
            name = "Old Name",
            description = "Old description",
            category = "Old Category",
            criteria = mapOf("key" to "value"),
            iconUrl = "https://example.com/icons/old.png",
            points = 10,
            isActive = false
        )

        val updateRequest = UpdateAchievementRequest(
            name = "New Name",
            description = "New description",
            category = null,
            iconUrl = null,
            points = 15
        )

        // When
        val updatedAchievement = updateRequest.applyTo(existingAchievement)

        // Then
        assertEquals(existingId, updatedAchievement.id)
        assertEquals("New Name", updatedAchievement.name)
        assertEquals("New description", updatedAchievement.description)
        assertNull(updatedAchievement.category)
        assertNull(updatedAchievement.iconUrl)
        assertEquals(15, updatedAchievement.points)
        // Original criteria and isActive should be preserved
        assertEquals(existingAchievement.criteria, updatedAchievement.criteria)
        assertFalse(updatedAchievement.isActive)
    }

    @Test
    fun `toUserAchievementDto should map Achievement and UserAchievement correctly`() {
        // Given
        val achievementId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val achievedAt = LocalDateTime.now()
        val progressData = mapOf("current" to 5, "total" to 10)

        val achievement = Achievement(
            id = achievementId,
            name = "Progress Achievement",
            description = "Track your progress",
            category = "Progress",
            criteria = mapOf("type" to "progress"),
            iconUrl = "https://example.com/icons/progress.png",
            points = 75,
            isActive = true
        )

        val userAchievement = UserAchievement(
            id = UUID.randomUUID(),
            userId = userId,
            achievementId = achievementId,
            achievedAt = achievedAt,
            progressData = progressData
        )

        // When
        val response = toUserAchievementDto(achievement, userAchievement)

        // Then
        // Check achievement part
        assertEquals(achievementId, response.achievement.id)
        assertEquals("Progress Achievement", response.achievement.name)
        assertEquals("Track your progress", response.achievement.description)
        assertEquals("Progress", response.achievement.category)
        assertEquals("https://example.com/icons/progress.png", response.achievement.iconUrl)
        assertEquals(75, response.achievement.points)

        // Check user achievement part
        assertEquals(achievedAt, response.achievedAt)
        assertEquals(progressData, response.progress)
    }

    @Test
    fun `toUserAchievementDto should handle null progress data correctly`() {
        // Given
        val achievementId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val achievedAt = LocalDateTime.now()

        val achievement = Achievement(
            id = achievementId,
            name = "Simple Achievement",
            description = "Simple achievement without progress",
            category = "Simple",
            criteria = emptyMap(),
            iconUrl = null,
            points = 10,
            isActive = true
        )

        val userAchievement = UserAchievement(
            id = UUID.randomUUID(),
            userId = userId,
            achievementId = achievementId,
            achievedAt = achievedAt,
            progressData = null
        )

        // When
        val response = toUserAchievementDto(achievement, userAchievement)

        // Then
        assertEquals(achievementId, response.achievement.id)
        assertEquals("Simple Achievement", response.achievement.name)
        assertEquals(achievedAt, response.achievedAt)
        assertNull(response.progress)
    }

    @Test
    fun `toUserAchievementDto should handle empty progress data correctly`() {
        // Given
        val achievementId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val achievedAt = LocalDateTime.now()

        val achievement = Achievement(
            id = achievementId,
            name = "Empty Progress Achievement",
            description = "Achievement with empty progress",
            category = "Test",
            criteria = emptyMap(),
            iconUrl = null,
            points = 20,
            isActive = true
        )

        val userAchievement = UserAchievement(
            id = UUID.randomUUID(),
            userId = userId,
            achievementId = achievementId,
            achievedAt = achievedAt,
            progressData = emptyMap()
        )

        // When
        val response = toUserAchievementDto(achievement, userAchievement)

        // Then
        assertEquals(achievementId, response.achievement.id)
        assertEquals(achievedAt, response.achievedAt)
        assertNotNull(response.progress)
        assertTrue(response.progress!!.isEmpty())
    }

    @Test
    fun `toEntity should create isActive as true by default`() {
        // Given
        val request = CreateAchievementRequest(
            name = "Test",
            description = "Test description",
            category = "Test",
            iconUrl = null,
            points = 10
        )

        // When
        val achievement = request.toEntity()

        // Then
        assertTrue(achievement.isActive)
    }

    @Test
    fun `toEntity should create empty criteria map by default`() {
        // Given
        val request = CreateAchievementRequest(
            name = "Test",
            description = "Test description",
            category = "Test",
            iconUrl = null,
            points = 10
        )

        // When
        val achievement = request.toEntity()

        // Then
        assertNotNull(achievement.criteria)
        assertTrue(achievement.criteria.isEmpty())
    }
}
