package app.cityxplore.social.domain.model

data class RankingEntry(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val score: Double,
    val totalPoisDiscovered: Int,
    val totalDistance: Double,
    val totalAchievementPoints: Int,
    val rank: Int
)
