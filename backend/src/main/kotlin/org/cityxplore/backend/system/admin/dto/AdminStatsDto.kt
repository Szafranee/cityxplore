package org.cityxplore.backend.system.admin.dto

/**
 * DTO holding platform-wide admin statistics (totals and active counts).
 */
data class AdminStatsDto(
    val totalUsers: Long,
    val activeUsers: Long,
    val totalPois: Long,
    val activePois: Long,
    val totalDiscoveries: Long,
    val totalAchievements: Long
)
