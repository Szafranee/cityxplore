package org.cityxplore.backend.system.admin.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.system.admin.dto.AdminStatsResponse
import org.cityxplore.backend.system.admin.service.AdminStatsService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Unit tests for AdminStatsController.
 */
@WebMvcTest(AdminStatsController::class)
@EnableMethodSecurity
@TestPropertySource(properties = ["app.admin.enable-reset=true"])
class AdminStatsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var adminStatsService: AdminStatsService

    @MockkBean
    private lateinit var discoveryRepository: UserPoiDiscoveryRepository

    @MockkBean
    private lateinit var userAchievementRepository: UserAchievementRepository

    @Test
    fun `getStats should return 200 with statistics when admin user`() {
        // Given
        val stats = AdminStatsResponse(
            totalUsers = 100,
            activeUsers = 80,
            totalPois = 200,
            activePois = 150,
            totalDiscoveries = 500,
            totalAchievements = 300
        )

        every { adminStatsService.getStats() } returns stats

        // When & Then
        mockMvc.perform(
            get("/api/admin/stats")
                .with(user("admin").roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalUsers").value(100))
            .andExpect(jsonPath("$.activeUsers").value(80))
            .andExpect(jsonPath("$.totalPois").value(200))
            .andExpect(jsonPath("$.activePois").value(150))
            .andExpect(jsonPath("$.totalDiscoveries").value(500))
            .andExpect(jsonPath("$.totalAchievements").value(300))

        verify(exactly = 1) { adminStatsService.getStats() }
    }

    @Test
    fun `getStats should return 403 when non-admin user`() {
        // Given
        every { adminStatsService.getStats() } returns AdminStatsResponse(0, 0, 0, 0, 0, 0)

        // When & Then
        mockMvc.perform(
            get("/api/admin/stats")
                .with(user("user").roles("USER"))
        )
            .andExpect(status().isForbidden)

        verify(exactly = 0) { adminStatsService.getStats() }
    }

    @Test
    fun `getStats should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/api/admin/stats"))
            .andExpect(status().isUnauthorized)

        verify(exactly = 0) { adminStatsService.getStats() }
    }

    @Test
    fun `getStats should return zero statistics when empty`() {
        // Given
        val emptyStats = AdminStatsResponse(
            totalUsers = 0,
            activeUsers = 0,
            totalPois = 0,
            activePois = 0,
            totalDiscoveries = 0,
            totalAchievements = 0
        )

        every { adminStatsService.getStats() } returns emptyStats

        // When & Then
        mockMvc.perform(
            get("/api/admin/stats")
                .with(user("admin").roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalUsers").value(0))
            .andExpect(jsonPath("$.activeUsers").value(0))
            .andExpect(jsonPath("$.totalPois").value(0))
            .andExpect(jsonPath("$.activePois").value(0))
            .andExpect(jsonPath("$.totalDiscoveries").value(0))
            .andExpect(jsonPath("$.totalAchievements").value(0))
    }

    @Test
    fun `resetData should return 200 when admin user and reset enabled`() {
        // Given
        justRun { discoveryRepository.deleteAll() }
        justRun { userAchievementRepository.deleteAll() }

        // When & Then
        mockMvc.perform(
            post("/api/admin/stats/reset")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("reset complete"))

        verify(exactly = 1) { discoveryRepository.deleteAll() }
        verify(exactly = 1) { userAchievementRepository.deleteAll() }
    }

    @Test
    fun `resetData should return 403 when non-admin user`() {
        // Given
        justRun { discoveryRepository.deleteAll() }
        justRun { userAchievementRepository.deleteAll() }

        // When & Then
        mockMvc.perform(
            post("/api/admin/stats/reset")
                .with(user("user").roles("USER"))
                .with(csrf())
        )
            .andExpect(status().isForbidden)

        verify(exactly = 0) { discoveryRepository.deleteAll() }
        verify(exactly = 0) { userAchievementRepository.deleteAll() }
    }

    @Test
    fun `resetData should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(
            post("/api/admin/stats/reset")
                .with(csrf())
        )
            .andExpect(status().isUnauthorized)

        verify(exactly = 0) { discoveryRepository.deleteAll() }
        verify(exactly = 0) { userAchievementRepository.deleteAll() }
    }
}

/**
 * Tests for AdminStatsController with reset disabled.
 */
@WebMvcTest(AdminStatsController::class)
@TestPropertySource(properties = ["app.admin.enable-reset=false"])
class AdminStatsControllerResetDisabledTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var adminStatsService: AdminStatsService

    @MockkBean
    private lateinit var discoveryRepository: UserPoiDiscoveryRepository

    @MockkBean
    private lateinit var userAchievementRepository: UserAchievementRepository

    @Test
    fun `resetData should return 403 when reset is disabled even for admin`() {
        // When & Then
        mockMvc.perform(
            post("/api/admin/stats/reset")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Admin reset is disabled"))

        verify(exactly = 0) { discoveryRepository.deleteAll() }
        verify(exactly = 0) { userAchievementRepository.deleteAll() }
    }

    @Test
    fun `getStats should still work when reset is disabled`() {
        // Given
        val stats = AdminStatsResponse(
            totalUsers = 50,
            activeUsers = 40,
            totalPois = 100,
            activePois = 80,
            totalDiscoveries = 250,
            totalAchievements = 150
        )

        every { adminStatsService.getStats() } returns stats

        // When & Then
        mockMvc.perform(
            get("/api/admin/stats")
                .with(user("admin").roles("ADMIN"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalUsers").value(50))
            .andExpect(jsonPath("$.activeUsers").value(40))

        verify(exactly = 1) { adminStatsService.getStats() }
    }
}
