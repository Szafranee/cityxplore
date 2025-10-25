package org.cityxplore.backend.service

import org.cityxplore.backend.dto.AdminStatsDto
import org.cityxplore.backend.repository.PointOfInterestRepository
import org.cityxplore.backend.repository.UserAchievementRepository
import org.cityxplore.backend.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminStatsService(
    private val userRepository: UserRepository,
    private val poiRepository: PointOfInterestRepository,
    private val discoveryRepository: UserPoiDiscoveryRepository,
    private val userAchievementRepository: UserAchievementRepository
) {

    @Transactional(readOnly = true)
    fun getStats(): AdminStatsDto {
        val totalUsers = userRepository.count()
        val activeUsers = userRepository.countByIsActiveTrue()
        val totalPois = poiRepository.count()
        val activePois = poiRepository.countByIsActiveTrue()
        val totalDiscoveries = discoveryRepository.count()
        val totalAchievements = userAchievementRepository.count()
        return AdminStatsDto(
            totalUsers, activeUsers,
            totalPois, activePois,
            totalDiscoveries, totalAchievements
        )
    }
}
