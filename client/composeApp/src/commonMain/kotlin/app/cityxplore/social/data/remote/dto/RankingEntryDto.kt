package app.cityxplore.social.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RankingEntryDto(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val score: Double,
    val totalPoisDiscovered: Int,
    val totalDistance: Double,
    val totalAchievementPoints: Int,
    val rank: Int
)
