package org.cityxplore.backend.achievements.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.achievements.entity.UserAchievement
import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.achievements.service.AchievementService.AchievementGrantResult
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.cityxplore.backend.user.entity.User
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@DisplayName("AchievementEvaluationService Tests")
class AchievementEvaluationServiceTest {

    private val achievementRepository: AchievementRepository = mockk()
    private val userAchievementRepository: UserAchievementRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val achievementService: AchievementService = mockk()
    private val userPoiDiscoveryRepository: UserPoiDiscoveryRepository = mockk()
    private val poiRepository: PointOfInterestRepository = mockk()
    private val friendshipRepository: FriendshipRepository = mockk()

    private lateinit var evaluationService: AchievementEvaluationService

    private val testUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        evaluationService = AchievementEvaluationService(
            achievementRepository,
            userAchievementRepository,
            userRepository,
            achievementService,
            userPoiDiscoveryRepository,
            poiRepository,
            friendshipRepository
        )
    }

    private fun createTestUser(
        totalDistance: Double = 0.0,
        isActive: Boolean = true
    ): User {
        return User(
            id = testUserId,
            email = "test@example.com",
            username = "testuser",
            totalDistance = BigDecimal.valueOf(totalDistance),
            isActive = isActive
        )
    }

    private fun createDistanceAchievement(
        id: UUID = UUID.randomUUID(),
        name: String,
        distanceKm: Int,
        points: Int = 100
    ): Achievement {
        return Achievement(
            id = id,
            name = name,
            description = "Travel $distanceKm km",
            category = "Explorer",
            criteria = mapOf("distance_km" to distanceKm),
            iconUrl = null,
            points = points,
            isActive = true
        )
    }

    private fun createCountAchievement(
        id: UUID = UUID.randomUUID(),
        name: String,
        count: Int,
        category: String? = null,
        points: Int = 50
    ): Achievement {
        // Updated to use "poi_count" instead of "count" as per logic change
        val criteria = mutableMapOf<String, Any?>("poi_count" to count)
        if (category != null) {
            criteria["category"] = category
        }
        return Achievement(
            id = id,
            name = name,
            description = "Discover $count POIs",
            category = "Explorer",
            criteria = criteria,
            iconUrl = null,
            points = points,
            isActive = true
        )
    }

    @Nested
    @DisplayName("evaluateDistanceAchievements")
    inner class EvaluateDistanceAchievements {

        @Test
        @DisplayName("should grant achievement when distance threshold is reached")
        fun `should grant achievement when distance threshold is reached`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser(totalDistance = 5500.0) // 5.5 km
            val achievement = createDistanceAchievement(achievementId, "5K Walker", distanceKm = 5)

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)
            every { achievementService.grantAchievement(testUserId, achievementId) } returns
                    AchievementGrantResult(dto = mockk(), created = true)

            // When
            val result = evaluationService.evaluateDistanceAchievements(testUserId)

            // Then
            assertEquals(1, result.size)
            assertEquals(achievementId, result[0])
            verify { achievementService.grantAchievement(testUserId, achievementId) }
        }

        @Test
        @DisplayName("should not grant achievement when distance is below threshold")
        fun `should not grant achievement when distance is below threshold`() {
            // Given
            val user = createTestUser(totalDistance = 3000.0) // 3 km
            val achievement = createDistanceAchievement(UUID.randomUUID(), "5K Walker", distanceKm = 5)

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)

            // When
            val result = evaluationService.evaluateDistanceAchievements(testUserId)

            // Then
            assertTrue(result.isEmpty())
            verify(exactly = 0) { achievementService.grantAchievement(any(), any()) }
        }

        @Test
        @DisplayName("should not duplicate already earned achievements")
        fun `should not duplicate already earned achievements`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser(totalDistance = 10000.0)
            val achievement = createDistanceAchievement(achievementId, "5K Walker", distanceKm = 5)
            val existingUserAchievement = UserAchievement(
                userId = testUserId,
                achievementId = achievementId,
                achievedAt = LocalDateTime.now()
            )

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns listOf(existingUserAchievement)
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)

            // When
            val result = evaluationService.evaluateDistanceAchievements(testUserId)

            // Then
            assertTrue(result.isEmpty())
            verify(exactly = 0) { achievementService.grantAchievement(any(), any()) }
        }

        @Test
        @DisplayName("should return empty list for inactive user")
        fun `should return empty list for inactive user`() {
            // Given
            val user = createTestUser(totalDistance = 100000.0, isActive = false)

            every { userRepository.findById(testUserId) } returns Optional.of(user)

            // When
            val result = evaluationService.evaluateDistanceAchievements(testUserId)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("should return empty list for non-existent user")
        fun `should return empty list for non-existent user`() {
            // Given
            every { userRepository.findById(testUserId) } returns Optional.empty()

            // When
            val result = evaluationService.evaluateDistanceAchievements(testUserId)

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("evaluateDiscoveryAchievements")
    inner class EvaluateDiscoveryAchievements {

        @Test
        @DisplayName("should grant achievement when POI count is reached")
        fun `should grant achievement when POI count is reached`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser()
            val achievement = createCountAchievement(achievementId, "10 POIs", count = 10)

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)
            every { userPoiDiscoveryRepository.countByUserId(testUserId) } returns 10L
            every { achievementService.grantAchievement(testUserId, achievementId) } returns
                    AchievementGrantResult(dto = mockk(), created = true)

            // When
            val result = evaluationService.evaluateDiscoveryAchievements(testUserId)

            // Then
            assertEquals(1, result.size)
            assertEquals(achievementId, result[0])
        }

        @Test
        @DisplayName("should grant category-specific achievement")
        fun `should grant category-specific achievement`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser()
            val achievement = createCountAchievement(
                achievementId,
                "Park Lover",
                count = 3,
                category = "Park"
            )

            val poiId1 = UUID.randomUUID()
            val poiId2 = UUID.randomUUID()
            val poiId3 = UUID.randomUUID()

            val discoveries = listOf(
                UserPoiDiscovery(userId = testUserId, poiId = poiId1),
                UserPoiDiscovery(userId = testUserId, poiId = poiId2),
                UserPoiDiscovery(userId = testUserId, poiId = poiId3)
            )

            val pois = listOf(
                PointOfInterest(id = poiId1, name = "Park 1", category = "Park"),
                PointOfInterest(id = poiId2, name = "Park 2", category = "Park"),
                PointOfInterest(id = poiId3, name = "Park 3", category = "Park")
            )

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)
            every { userPoiDiscoveryRepository.findAllByUserId(testUserId) } returns discoveries
            every { poiRepository.findAllById(any()) } returns pois
            every { achievementService.grantAchievement(testUserId, achievementId) } returns
                    AchievementGrantResult(dto = mockk(), created = true)

            // When
            val result = evaluationService.evaluateDiscoveryAchievements(testUserId)

            // Then
            assertEquals(1, result.size)
            assertEquals(achievementId, result[0])
        }

        @Test
        @DisplayName("should not grant category achievement with insufficient discoveries")
        fun `should not grant category achievement with insufficient discoveries`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser()
            val achievement = createCountAchievement(
                achievementId,
                "Park Lover",
                count = 5,
                category = "Park"
            )

            val poiId1 = UUID.randomUUID()
            val poiId2 = UUID.randomUUID()

            val discoveries = listOf(
                UserPoiDiscovery(userId = testUserId, poiId = poiId1),
                UserPoiDiscovery(userId = testUserId, poiId = poiId2)
            )

            val pois = listOf(
                PointOfInterest(id = poiId1, name = "Park 1", category = "Park"),
                PointOfInterest(id = poiId2, name = "Museum", category = "Cultural")
            )

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)
            every { userPoiDiscoveryRepository.findAllByUserId(testUserId) } returns discoveries
            every { poiRepository.findAllById(any()) } returns pois

            // When
            val result = evaluationService.evaluateDiscoveryAchievements(testUserId)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("should grant time-range achievement for night discovery")
        fun `should grant time-range achievement for night discovery`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser()
            val achievement = Achievement(
                id = achievementId,
                name = "Night Owl",
                description = "Discover a POI at night",
                category = "Special",
                criteria = mapOf("time_range" to "22:00-04:00"),
                iconUrl = null,
                points = 100,
                isActive = true
            )

            // Discovery at 23:00 (11 PM)
            val discovery = UserPoiDiscovery(
                userId = testUserId,
                poiId = UUID.randomUUID(),
                discoveredAt = LocalDateTime.of(2026, 1, 12, 23, 0)
            )

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)
            every { userPoiDiscoveryRepository.findAllByUserId(testUserId) } returns listOf(discovery)
            every { achievementService.grantAchievement(testUserId, achievementId) } returns
                    AchievementGrantResult(dto = mockk(), created = true)

            // When
            val result = evaluationService.evaluateDiscoveryAchievements(testUserId)

            // Then
            assertEquals(1, result.size)
            assertEquals(achievementId, result[0])
        }

        @Test
        @DisplayName("should not grant time-range achievement for daytime discovery")
        fun `should not grant time-range achievement for daytime discovery`() {
            // Given
            val achievementId = UUID.randomUUID()
            val user = createTestUser()
            val achievement = Achievement(
                id = achievementId,
                name = "Night Owl",
                description = "Discover a POI at night",
                category = "Special",
                criteria = mapOf("time_range" to "22:00-04:00"),
                iconUrl = null,
                points = 100,
                isActive = true
            )

            // Discovery at 14:00 (2 PM)
            val discovery = UserPoiDiscovery(
                userId = testUserId,
                poiId = UUID.randomUUID(),
                discoveredAt = LocalDateTime.of(2026, 1, 12, 14, 0)
            )

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement)
            every { userPoiDiscoveryRepository.findAllByUserId(testUserId) } returns listOf(discovery)

            // When
            val result = evaluationService.evaluateDiscoveryAchievements(testUserId)

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Multiple achievements")
    inner class MultipleAchievements {

        @Test
        @DisplayName("should grant multiple achievements at once")
        fun `should grant multiple achievements at once`() {
            // Given
            val achievementId1 = UUID.randomUUID()
            val achievementId2 = UUID.randomUUID()
            val user = createTestUser(totalDistance = 15000.0) // 15 km

            val achievement1 = createDistanceAchievement(achievementId1, "5K Walker", distanceKm = 5)
            val achievement2 = createDistanceAchievement(achievementId2, "10K Runner", distanceKm = 10)

            every { userRepository.findById(testUserId) } returns Optional.of(user)
            every { userAchievementRepository.findAllByUserId(testUserId) } returns emptyList()
            every { achievementRepository.findAllByIsActiveTrue() } returns listOf(achievement1, achievement2)
            every { achievementService.grantAchievement(testUserId, achievementId1) } returns
                    AchievementGrantResult(dto = mockk(), created = true)
            every { achievementService.grantAchievement(testUserId, achievementId2) } returns
                    AchievementGrantResult(dto = mockk(), created = true)

            // When
            val result = evaluationService.evaluateDistanceAchievements(testUserId)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.contains(achievementId1))
            assertTrue(result.contains(achievementId2))
        }
    }
}
