package org.cityxplore.backend.system.admin.controller

import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.system.admin.dto.AdminStatsResponse
import org.cityxplore.backend.system.admin.service.AdminStatsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Controller exposing general statistics for admins / developers.
 */
@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
class AdminStatsController(
    private val adminStatsService: AdminStatsService,
    private val discoveryRepository: UserPoiDiscoveryRepository,
    private val userAchievementRepository: UserAchievementRepository,
    @Value("\${app.admin.enable-reset:false}") private val enableReset: Boolean
) {

    /**
     * Returns current platform statistics (totals and active entities) for admins.
     */
    @GetMapping
    fun getStats(): AdminStatsResponse = adminStatsService.getStats()

    /**
     * Danger: destructive operation – resets selected datasets (discoveries & achievements).
     * Intended for development/testing only. Disabled by default (app.admin.enable-reset=false).
     */
    @PostMapping("/reset")
    fun resetData(): Map<String, String> {
        if (!enableReset) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin reset is disabled")
        }
        discoveryRepository.deleteAll()
        userAchievementRepository.deleteAll()

        return mapOf("status" to "reset complete")
    }
}
