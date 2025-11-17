package org.cityxplore.backend.system.admin.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for AdminStatsService.
 */
class AdminStatsServiceTest {

    private val userRepository: UserRepository = mockk()
    private val poiRepository: PointOfInterestRepository = mockk()
    private val discoveryRepository: UserPoiDiscoveryRepository = mockk()
    private val userAchievementRepository: UserAchievementRepository = mockk()

    private val adminStatsService = AdminStatsService(
        userRepository,
        poiRepository,
        discoveryRepository,
        userAchievementRepository
    )

    @Test
    fun `getStats should return correct statistics`() {
        // Given
        every { userRepository.count() } returns 100
        every { userRepository.countByIsActiveTrue() } returns 80
        every { poiRepository.count() } returns 200
        every { poiRepository.countByIsActiveTrue() } returns 150
        every { discoveryRepository.count() } returns 500
        every { userAchievementRepository.count() } returns 300

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(100, result.totalUsers)
        assertEquals(80, result.activeUsers)
        assertEquals(200, result.totalPois)
        assertEquals(150, result.activePois)
        assertEquals(500, result.totalDiscoveries)
        assertEquals(300, result.totalAchievements)

        verify(exactly = 1) { userRepository.count() }
        verify(exactly = 1) { userRepository.countByIsActiveTrue() }
        verify(exactly = 1) { poiRepository.count() }
        verify(exactly = 1) { poiRepository.countByIsActiveTrue() }
        verify(exactly = 1) { discoveryRepository.count() }
        verify(exactly = 1) { userAchievementRepository.count() }
    }

    @Test
    fun `getStats should return zero statistics when repositories are empty`() {
        // Given
        every { userRepository.count() } returns 0
        every { userRepository.countByIsActiveTrue() } returns 0
        every { poiRepository.count() } returns 0
        every { poiRepository.countByIsActiveTrue() } returns 0
        every { discoveryRepository.count() } returns 0
        every { userAchievementRepository.count() } returns 0

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(0, result.totalUsers)
        assertEquals(0, result.activeUsers)
        assertEquals(0, result.totalPois)
        assertEquals(0, result.activePois)
        assertEquals(0, result.totalDiscoveries)
        assertEquals(0, result.totalAchievements)
    }

    @Test
    fun `getStats should handle case where active count equals total count`() {
        // Given
        every { userRepository.count() } returns 50
        every { userRepository.countByIsActiveTrue() } returns 50
        every { poiRepository.count() } returns 100
        every { poiRepository.countByIsActiveTrue() } returns 100
        every { discoveryRepository.count() } returns 200
        every { userAchievementRepository.count() } returns 150

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(50, result.totalUsers)
        assertEquals(50, result.activeUsers)
        assertEquals(100, result.totalPois)
        assertEquals(100, result.activePois)
    }

    @Test
    fun `getStats should handle case where active count is less than total count`() {
        // Given
        every { userRepository.count() } returns 100
        every { userRepository.countByIsActiveTrue() } returns 30
        every { poiRepository.count() } returns 200
        every { poiRepository.countByIsActiveTrue() } returns 50
        every { discoveryRepository.count() } returns 500
        every { userAchievementRepository.count() } returns 300

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(100, result.totalUsers)
        assertEquals(30, result.activeUsers)
        assertEquals(200, result.totalPois)
        assertEquals(50, result.activePois)
    }

    @Test
    fun `getStats should handle large numbers`() {
        // Given
        every { userRepository.count() } returns 1_000_000
        every { userRepository.countByIsActiveTrue() } returns 800_000
        every { poiRepository.count() } returns 2_000_000
        every { poiRepository.countByIsActiveTrue() } returns 1_500_000
        every { discoveryRepository.count() } returns 10_000_000
        every { userAchievementRepository.count() } returns 5_000_000

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(1_000_000, result.totalUsers)
        assertEquals(800_000, result.activeUsers)
        assertEquals(2_000_000, result.totalPois)
        assertEquals(1_500_000, result.activePois)
        assertEquals(10_000_000, result.totalDiscoveries)
        assertEquals(5_000_000, result.totalAchievements)
    }

    @Test
    fun `getStats should handle zero users but non-zero other stats`() {
        // Given
        every { userRepository.count() } returns 0
        every { userRepository.countByIsActiveTrue() } returns 0
        every { poiRepository.count() } returns 100
        every { poiRepository.countByIsActiveTrue() } returns 80
        every { discoveryRepository.count() } returns 0
        every { userAchievementRepository.count() } returns 0

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(0, result.totalUsers)
        assertEquals(0, result.activeUsers)
        assertEquals(100, result.totalPois)
        assertEquals(80, result.activePois)
        assertEquals(0, result.totalDiscoveries)
        assertEquals(0, result.totalAchievements)
    }

    @Test
    fun `getStats should handle zero POIs but non-zero other stats`() {
        // Given
        every { userRepository.count() } returns 50
        every { userRepository.countByIsActiveTrue() } returns 40
        every { poiRepository.count() } returns 0
        every { poiRepository.countByIsActiveTrue() } returns 0
        every { discoveryRepository.count() } returns 0
        every { userAchievementRepository.count() } returns 100

        // When
        val result = adminStatsService.getStats()

        // Then
        assertEquals(50, result.totalUsers)
        assertEquals(40, result.activeUsers)
        assertEquals(0, result.totalPois)
        assertEquals(0, result.activePois)
        assertEquals(0, result.totalDiscoveries)
        assertEquals(100, result.totalAchievements)
    }

    @Test
    fun `getStats should call all repository methods exactly once`() {
        // Given
        every { userRepository.count() } returns 10
        every { userRepository.countByIsActiveTrue() } returns 8
        every { poiRepository.count() } returns 20
        every { poiRepository.countByIsActiveTrue() } returns 15
        every { discoveryRepository.count() } returns 50
        every { userAchievementRepository.count() } returns 30

        // When
        adminStatsService.getStats()

        // Then - verify each method was called exactly once
        verify(exactly = 1) { userRepository.count() }
        verify(exactly = 1) { userRepository.countByIsActiveTrue() }
        verify(exactly = 1) { poiRepository.count() }
        verify(exactly = 1) { poiRepository.countByIsActiveTrue() }
        verify(exactly = 1) { discoveryRepository.count() }
        verify(exactly = 1) { userAchievementRepository.count() }
    }
}
