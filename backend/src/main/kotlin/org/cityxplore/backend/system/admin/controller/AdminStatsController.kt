package org.cityxplore.backend.system.admin.controller

import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.system.admin.dto.AdminStatsDto
import org.cityxplore.backend.system.admin.service.AdminStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller exposing general statistics for admins / developers.
 */
@RestController
@RequestMapping("/api/admin/stats")
class AdminStatsController(
    private val adminStatsService: AdminStatsService,
    private val discoveryRepository: UserPoiDiscoveryRepository,
    private val userAchievementRepository: UserAchievementRepository
) {

    /**
     * Returns current platform statistics (totals and active entities) for admins.
     */
    @GetMapping
    fun getStats(): AdminStatsDto = adminStatsService.getStats()

    /**
     * Danger: destructive operation – resets selected datasets (discoveries & achievements).
     * Intended for development/testing only.
     */
    @PostMapping("/reset")
    fun resetData(): Map<String, String> {
        discoveryRepository.deleteAll()
        userAchievementRepository.deleteAll()
        return mapOf("status" to "reset complete")
    }
}
