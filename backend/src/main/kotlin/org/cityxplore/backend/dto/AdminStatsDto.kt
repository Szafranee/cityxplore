package org.cityxplore.backend.dto

data class AdminStatsDto(
    val totalUsers: Long,
    val activeUsers: Long,
    val totalPois: Long,
    val activePois: Long,
    val totalDiscoveries: Long,
    val totalAchievements: Long
)
