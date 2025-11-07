package org.cityxplore.backend.social.rankings.dto

import java.util.UUID

/**
 * Response DTO representing a single entry in a ranking.
 *
 * @property userId unique identifier of the user
 * @property username display the name of the user
 * @property avatarUrl optional URL to the user's avatar image
 * @property score calculated ranking score based on user's activities
 * @property totalPoisDiscovered total number of unique POIs discovered by the user
 * @property totalDistance total distance travelled by the user (in meters)
 * @property totalAchievementPoints sum of points from all unlocked achievements
 * @property rank position in the ranking (1-based)
 */
data class RankingEntryResponse(
    val userId: UUID,
    val username: String,
    val avatarUrl: String?,
    val score: Double,
    val totalPoisDiscovered: Int,
    val totalDistance: Double,
    val totalAchievementPoints: Int,
    val rank: Int
)
