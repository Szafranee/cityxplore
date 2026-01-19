package org.cityxplore.backend.achievements.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.achievements.entity.UserAchievement
import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("AchievementService Tests")
class AchievementServiceTest {

    private val achievementRepository: AchievementRepository = mockk()
    private val userAchievementRepository: UserAchievementRepository = mockk()
    private val userRepository: UserRepository = mockk()

    private val achievementService = AchievementService(
        achievementRepository,
        userAchievementRepository,
        userRepository
    )

    private val testUserId = UUID.randomUUID()
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
    private val testUserAchievement = UserAchievement(
        userId = testUserId,
        achievementId = testAchievementId,
        achievedAt = LocalDateTime.now(),
        progressData = mapOf("current" to 1, "total" to 1)
    )

    @Test
    @DisplayName("getAllAchievements should return all active achievements")
    fun `getAllAchievements should return all active achievements`() {
        // Given
        val achievements = listOf(
            testAchievement,
            Achievement(
                id = UUID.randomUUID(),
                name = "Ten Discoveries",
                description = "Discover 10 POIs",
                category = "Explorer",
                criteria = mapOf("type" to "discoveries", "count" to 10),
                iconUrl = "https://example.com/icons/ten.png",
                points = 50,
                isActive = true
            )
        )
        every { achievementRepository.findAllByIsActiveTrue() } returns achievements

        // When
        val result = achievementService.getAllAchievements()

        // Then
        assertEquals(2, result.size)
        assertEquals("First Discovery", result[0].name)
        assertEquals(10, result[0].points)
        assertEquals("Ten Discoveries", result[1].name)
        assertEquals(50, result[1].points)
        verify { achievementRepository.findAllByIsActiveTrue() }
    }

    @Test
    @DisplayName("getAllAchievements should return empty list when no active achievements exist")
    fun `getAllAchievements should return empty list when no active achievements exist`() {
        // Given
        every { achievementRepository.findAllByIsActiveTrue() } returns emptyList()

        // When
        val result = achievementService.getAllAchievements()

        // Then
        assertTrue(result.isEmpty())
        verify { achievementRepository.findAllByIsActiveTrue() }
    }

    @Test
    @DisplayName("grantAchievement should create new achievement grant and increment user points")
    fun `grantAchievement should create new achievement grant and increment user points`() {
        // Given
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns testAchievement
        every { userAchievementRepository.insertIgnore(any(), any(), any(), any()) } returns 1
        every {
            userAchievementRepository.findByUserIdAndAchievementId(
                testUserId,
                testAchievementId
            )
        } returns testUserAchievement
        every { userRepository.incrementAchievementPoints(testUserId, 10) } returns 1

        // When
        val result = achievementService.grantAchievement(testUserId, testAchievementId)

        // Then
        assertNotNull(result)
        assertTrue(result.created)
        assertEquals(testAchievementId, result.dto.achievement.id)
        assertEquals("First Discovery", result.dto.achievement.name)
        assertEquals(10, result.dto.achievement.points)
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
        verify { userAchievementRepository.insertIgnore(testUserId, testAchievementId, any(), null) }
        verify { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) }
        verify { userRepository.incrementAchievementPoints(testUserId, 10) }
    }

    @Test
    @DisplayName("grantAchievement should return existing achievement and not increment points")
    fun `grantAchievement should return existing achievement and not increment points`() {
        // Given
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns testAchievement
        every { userAchievementRepository.insertIgnore(any(), any(), any(), any()) } returns 0
        every {
            userAchievementRepository.findByUserIdAndAchievementId(
                testUserId,
                testAchievementId
            )
        } returns testUserAchievement

        // When
        val result = achievementService.grantAchievement(testUserId, testAchievementId)

        // Then
        assertNotNull(result)
        assertFalse(result.created)
        assertEquals(testAchievementId, result.dto.achievement.id)
        assertEquals("First Discovery", result.dto.achievement.name)
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
        verify { userAchievementRepository.insertIgnore(testUserId, testAchievementId, any(), null) }
        verify { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) }
        verify(exactly = 0) { userRepository.incrementAchievementPoints(any(), any()) }
    }

    @Test
    @DisplayName("grantAchievement should throw exception when achievement not found")
    fun `grantAchievement should throw exception when achievement not found`() {
        // Given
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns null

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            achievementService.grantAchievement(testUserId, testAchievementId)
        }
        assertEquals("Achievement not found", exception.reason)
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
        verify(exactly = 0) { userAchievementRepository.insertIgnore(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("grantAchievement should throw exception when achievement is not active")
    fun `grantAchievement should throw exception when achievement is not active`() {
        // Given
        val inactiveAchievement = testAchievement.copy(isActive = false)
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns inactiveAchievement

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            achievementService.grantAchievement(testUserId, testAchievementId)
        }
        assertEquals("Achievement not available", exception.reason)
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
        verify(exactly = 0) { userAchievementRepository.insertIgnore(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("grantAchievement should throw exception when user not found during point increment")
    fun `grantAchievement should throw exception when user not found during point increment`() {
        // Given
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns testAchievement
        every { userAchievementRepository.insertIgnore(any(), any(), any(), any()) } returns 1
        every {
            userAchievementRepository.findByUserIdAndAchievementId(
                testUserId,
                testAchievementId
            )
        } returns testUserAchievement
        every { userRepository.incrementAchievementPoints(testUserId, 10) } returns 0

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            achievementService.grantAchievement(testUserId, testAchievementId)
        }
        assertEquals("User not found", exception.reason)
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
        verify { userAchievementRepository.insertIgnore(testUserId, testAchievementId, any(), null) }
        verify { userRepository.incrementAchievementPoints(testUserId, 10) }
    }

    @Test
    @DisplayName("grantAchievement should throw exception when user achievement not found after upsert")
    fun `grantAchievement should throw exception when user achievement not found after upsert`() {
        // Given
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns testAchievement
        every { userAchievementRepository.insertIgnore(any(), any(), any(), any()) } returns 1
        every { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) } returns null

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            achievementService.grantAchievement(testUserId, testAchievementId)
        }
        assertEquals("UserAchievement not found after upsert", exception.reason)
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
        verify { userAchievementRepository.insertIgnore(testUserId, testAchievementId, any(), null) }
        verify { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) }
    }

    @Test
    @DisplayName("getUserAchievements should return all achievements with user progress")
    fun `getUserAchievements should return all achievements with user progress`() {
        // Given - two achievements exist, user has progress on one
        val secondAchievementId = UUID.randomUUID()
        val secondAchievement = Achievement(
            id = secondAchievementId,
            name = "Second Achievement",
            description = "Description",
            category = "Category",
            criteria = mapOf("type" to "test"),
            iconUrl = null,
            points = 20,
            isActive = true
        )
        val allAchievements = listOf(testAchievement, secondAchievement)
        val userAchievements = listOf(testUserAchievement) // User only has progress on first

        every { achievementRepository.findAllByIsActiveTrue() } returns allAchievements
        every { userAchievementRepository.findAllByUserId(testUserId) } returns userAchievements

        // When
        val result = achievementService.getUserAchievements(testUserId)

        // Then - should return ALL achievements
        assertEquals(2, result.size)

        // First achievement - user has progress
        val first = result.find { it.achievement.id == testAchievementId }
        assertNotNull(first)
        assertEquals("First Discovery", first!!.achievement.name)
        assertNotNull(first.achievedAt)

        // Second achievement - user has no progress
        val second = result.find { it.achievement.id == secondAchievementId }
        assertNotNull(second)
        assertEquals("Second Achievement", second!!.achievement.name)
        assertNull(second.achievedAt) // Not unlocked
        assertNull(second.progress)   // No progress

        verify { achievementRepository.findAllByIsActiveTrue() }
        verify { userAchievementRepository.findAllByUserId(testUserId) }
    }

    @Test
    @DisplayName("getUserAchievements should return all achievements even when user has none")
    fun `getUserAchievements should return all achievements even when user has none`() {
        // Given - achievements exist but user has no progress
        val allAchievements = listOf(testAchievement)
        every { achievementRepository.findAllByIsActiveTrue() } returns allAchievements
        every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()

        // When
        val result = achievementService.getUserAchievements(testUserId)

        // Then - should still return all achievements with null progress
        assertEquals(1, result.size)
        assertEquals("First Discovery", result[0].achievement.name)
        assertNull(result[0].achievedAt)
        assertNull(result[0].progress)

        verify { achievementRepository.findAllByIsActiveTrue() }
        verify { userAchievementRepository.findAllByUserId(testUserId) }
    }

    @Test
    @DisplayName("getUserAchievements should return empty list when no achievements exist")
    fun `getUserAchievements should return empty list when no achievements exist`() {
        // Given - no achievements in system
        every { achievementRepository.findAllByIsActiveTrue() } returns emptyList()
        every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()

        // When
        val result = achievementService.getUserAchievements(testUserId)

        // Then
        assertTrue(result.isEmpty())
        verify { achievementRepository.findAllByIsActiveTrue() }
        verify { userAchievementRepository.findAllByUserId(testUserId) }
    }

    @Test
    @DisplayName("getUserAchievement should return specific user achievement when found")
    fun `getUserAchievement should return specific user achievement when found`() {
        // Given
        every {
            userAchievementRepository.findByUserIdAndAchievementId(
                testUserId,
                testAchievementId
            )
        } returns testUserAchievement
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns testAchievement

        // When
        val result = achievementService.getUserAchievement(testUserId, testAchievementId)

        // Then
        assertNotNull(result)
        assertEquals(testAchievementId, result!!.achievement.id)
        assertEquals("First Discovery", result.achievement.name)
        assertEquals(10, result.achievement.points)
        verify { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) }
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
    }

    @Test
    @DisplayName("getUserAchievement should return null when user achievement not found")
    fun `getUserAchievement should return null when user achievement not found`() {
        // Given
        every { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) } returns null

        // When
        val result = achievementService.getUserAchievement(testUserId, testAchievementId)

        // Then
        assertNull(result)
        verify { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) }
        verify(exactly = 0) { achievementRepository.findByIdOrNull(any()) }
    }

    @Test
    @DisplayName("getUserAchievement should return null when achievement definition not found")
    fun `getUserAchievement should return null when achievement definition not found`() {
        // Given
        every {
            userAchievementRepository.findByUserIdAndAchievementId(
                testUserId,
                testAchievementId
            )
        } returns testUserAchievement
        every { achievementRepository.findByIdOrNull(testAchievementId) } returns null

        // When
        val result = achievementService.getUserAchievement(testUserId, testAchievementId)

        // Then
        assertNull(result)
        verify { userAchievementRepository.findByUserIdAndAchievementId(testUserId, testAchievementId) }
        verify { achievementRepository.findByIdOrNull(testAchievementId) }
    }
}
