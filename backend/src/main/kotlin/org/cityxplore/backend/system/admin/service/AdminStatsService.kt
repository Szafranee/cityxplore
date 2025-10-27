package org.cityxplore.backend.system.admin.service

import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.system.admin.dto.AdminStatsResponse
import org.cityxplore.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service aggregating admin-level statistics for the platform.
 */
@Service
class AdminStatsService(
    private val userRepository: UserRepository,
    private val poiRepository: PointOfInterestRepository,
    private val discoveryRepository: UserPoiDiscoveryRepository,
    private val userAchievementRepository: UserAchievementRepository
) {

    /**
     * Computes current totals and active counts using repository-level queries.
     */
    @Transactional(readOnly = true)
    fun getStats(): AdminStatsResponse {
        val totalUsers = userRepository.count()
        val activeUsers = userRepository.countByIsActiveTrue()
        val totalPois = poiRepository.count()
        val activePois = poiRepository.countByIsActiveTrue()
        val totalDiscoveries = discoveryRepository.count()
        val totalAchievements = userAchievementRepository.count()
        return AdminStatsResponse(
            totalUsers, activeUsers,
            totalPois, activePois,
            totalDiscoveries, totalAchievements
        )
    }
}
