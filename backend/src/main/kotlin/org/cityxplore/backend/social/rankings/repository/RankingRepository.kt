package org.cityxplore.backend.social.rankings.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for executing ranking-related queries.
 * Uses native SQL queries to calculate rankings based on user statistics.
 */
@Repository
class RankingRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    /**
     * Calculates global ranking for all users.
     *
     * @param poiWeight weight multiplier for discovered POIs
     * @param distanceWeight weight multiplier for travelled distance
     * @param achievementWeight weight multiplier for achievement points
     * @return list of ranking entries with calculated scores and ranks
     */
    fun calculateGlobalRanking(
        poiWeight: Int,
        distanceWeight: Double,
        achievementWeight: Int
    ): List<RankingEntry> {
        val sql = """
            WITH user_stats AS (
                SELECT 
                    u.id AS user_id,
                    u.username,
                    u.avatar_url,
                    COALESCE(u.total_pois_discovered, 0) AS total_pois_discovered,
                    COALESCE(u.total_distance, 0) AS total_distance,
                    COALESCE(SUM(a.points), 0) AS total_achievement_points
                FROM users u
                LEFT JOIN user_achievements ua ON u.id = ua.user_id
                LEFT JOIN achievements a ON ua.achievement_id = a.id
                WHERE u.is_active = true
                GROUP BY u.id, u.username, u.avatar_url, u.total_pois_discovered, u.total_distance
            ),
            ranked_users AS (
                SELECT 
                    user_id,
                    username,
                    avatar_url,
                    total_pois_discovered,
                    total_distance,
                    total_achievement_points,
                    (total_pois_discovered * ?) + 
                    (total_distance * ?) + 
                    (total_achievement_points * ?) AS score,
                    ROW_NUMBER() OVER (ORDER BY 
                        (total_pois_discovered * ?) + 
                        (total_distance * ?) + 
                        (total_achievement_points * ?) DESC
                    ) AS rank
                FROM user_stats
            )
            SELECT * FROM ranked_users
            ORDER BY rank
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RankingEntry(
                    userId = UUID.fromString(rs.getString("user_id")),
                    username = rs.getString("username"),
                    avatarUrl = rs.getString("avatar_url"),
                    totalPoisDiscovered = rs.getInt("total_pois_discovered"),
                    totalDistance = rs.getDouble("total_distance"),
                    totalAchievementPoints = rs.getInt("total_achievement_points"),
                    score = rs.getLong("score"),
                    rank = rs.getInt("rank")
                )
            },
            poiWeight, distanceWeight, achievementWeight,
            poiWeight, distanceWeight, achievementWeight
        )
    }

    /**
     * Calculates ranking for a user and their accepted friends.
     *
     * @param userId ID of the current user
     * @param poiWeight weight multiplier for discovered POIs
     * @param distanceWeight weight multiplier for travelled distance
     * @param achievementWeight weight multiplier for achievement points
     * @return list of ranking entries for the user and their friends
     */
    fun calculateFriendsRanking(
        userId: UUID,
        poiWeight: Int,
        distanceWeight: Double,
        achievementWeight: Int
    ): List<RankingEntry> {
        val sql = """
            WITH friends AS (
                SELECT 
                    CASE 
                        WHEN f.requester_id = ?::uuid THEN f.addressee_id
                        ELSE f.requester_id
                    END AS friend_id
                FROM friendships f
                WHERE (f.requester_id = ?::uuid OR f.addressee_id = ?::uuid)
                  AND f.status = 'ACCEPTED'
                UNION
                SELECT ?::uuid AS friend_id
            ),
            user_stats AS (
                SELECT 
                    u.id AS user_id,
                    u.username,
                    u.avatar_url,
                    COALESCE(u.total_pois_discovered, 0) AS total_pois_discovered,
                    COALESCE(u.total_distance, 0) AS total_distance,
                    COALESCE(SUM(a.points), 0) AS total_achievement_points
                FROM users u
                INNER JOIN friends f ON u.id = f.friend_id
                LEFT JOIN user_achievements ua ON u.id = ua.user_id
                LEFT JOIN achievements a ON ua.achievement_id = a.id
                WHERE u.is_active = true
                GROUP BY u.id, u.username, u.avatar_url, u.total_pois_discovered, u.total_distance
            ),
            ranked_users AS (
                SELECT 
                    user_id,
                    username,
                    avatar_url,
                    total_pois_discovered,
                    total_distance,
                    total_achievement_points,
                    (total_pois_discovered * ?) + 
                    (total_distance * ?) + 
                    (total_achievement_points * ?) AS score,
                    ROW_NUMBER() OVER (ORDER BY 
                        (total_pois_discovered * ?) + 
                        (total_distance * ?) + 
                        (total_achievement_points * ?) DESC
                    ) AS rank
                FROM user_stats
            )
            SELECT * FROM ranked_users
            ORDER BY rank
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RankingEntry(
                    userId = UUID.fromString(rs.getString("user_id")),
                    username = rs.getString("username"),
                    avatarUrl = rs.getString("avatar_url"),
                    totalPoisDiscovered = rs.getInt("total_pois_discovered"),
                    totalDistance = rs.getDouble("total_distance"),
                    totalAchievementPoints = rs.getInt("total_achievement_points"),
                    score = rs.getLong("score"),
                    rank = rs.getInt("rank")
                )
            },
            userId, userId, userId, userId,
            poiWeight, distanceWeight, achievementWeight,
            poiWeight, distanceWeight, achievementWeight
        )
    }

    /**
     * Data class representing a single ranking entry from the database.
     */
    data class RankingEntry(
        val userId: UUID,
        val username: String,
        val avatarUrl: String?,
        val totalPoisDiscovered: Int,
        val totalDistance: Double,
        val totalAchievementPoints: Int,
        val score: Long,
        val rank: Int
    )
}
