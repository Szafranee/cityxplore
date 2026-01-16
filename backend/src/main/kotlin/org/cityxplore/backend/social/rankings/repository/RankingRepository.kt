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
        poiWeight: Double,
        distanceWeight: Double,
        achievementWeight: Double
    ): List<RankingEntry> {
        val sql = """
            WITH ranked_users AS (
                SELECT 
                    u.id AS user_id,
                    u.username,
                    u.avatar_url,
                    COALESCE(COUNT(DISTINCT upd.poi_id), 0) AS total_pois_discovered,
                    COALESCE(u.total_distance, 0) AS total_distance,
                    COALESCE(u.total_achievement_points, 0) AS total_achievement_points,
                    (COALESCE(COUNT(DISTINCT upd.poi_id), 0) * ?) + 
                    (COALESCE(u.total_distance, 0) * ?) + 
                    (COALESCE(u.total_achievement_points, 0) * ?) AS score,
                    ROW_NUMBER() OVER (ORDER BY 
                        (COALESCE(COUNT(DISTINCT upd.poi_id), 0) * ?) + 
                        (COALESCE(u.total_distance, 0) * ?) + 
                        (COALESCE(u.total_achievement_points, 0) * ?) DESC
                    ) AS rank
                FROM users u
                LEFT JOIN user_poi_discoveries upd ON upd.user_id = u.id
                WHERE u.is_active = true
                GROUP BY u.id, u.username, u.avatar_url, u.total_distance, u.total_achievement_points
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
                    score = rs.getDouble("score"),
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
        poiWeight: Double,
        distanceWeight: Double,
        achievementWeight: Double
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
            ranked_users AS (
                SELECT 
                    u.id AS user_id,
                    u.username,
                    u.avatar_url,
                    COALESCE(COUNT(DISTINCT upd.poi_id), 0) AS total_pois_discovered,
                    COALESCE(u.total_distance, 0) AS total_distance,
                    COALESCE(u.total_achievement_points, 0) AS total_achievement_points,
                    (COALESCE(COUNT(DISTINCT upd.poi_id), 0) * ?) + 
                    (COALESCE(u.total_distance, 0) * ?) + 
                    (COALESCE(u.total_achievement_points, 0) * ?) AS score,
                    ROW_NUMBER() OVER (ORDER BY 
                        (COALESCE(COUNT(DISTINCT upd.poi_id), 0) * ?) + 
                        (COALESCE(u.total_distance, 0) * ?) + 
                        (COALESCE(u.total_achievement_points, 0) * ?) DESC
                    ) AS rank
                FROM users u
                INNER JOIN friends f ON u.id = f.friend_id
                LEFT JOIN user_poi_discoveries upd ON upd.user_id = u.id
                WHERE u.is_active = true
                GROUP BY u.id, u.username, u.avatar_url, u.total_distance, u.total_achievement_points
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
                    score = rs.getDouble("score"),
                    rank = rs.getInt("rank")
                )
            },
            userId, userId, userId, userId,
            poiWeight, distanceWeight, achievementWeight,
            poiWeight, distanceWeight, achievementWeight
        )
    }

    /**
     * Calculates the global rank for a specific user without materialising the entire leaderboard.
     *
     * This is optimised to compute only the rank and stats for the requested user,
     * avoiding the need to load all users into memory.
     *
     * @param userId the UUID of the user to find
     * @param poiWeight weight multiplier for discovered POIs
     * @param distanceWeight weight multiplier for travelled distance
     * @param achievementWeight weight multiplier for achievement points
     * @return the ranking entry for the user, or null if user not found or inactive
     */
    fun findGlobalRankForUser(
        userId: UUID,
        poiWeight: Double,
        distanceWeight: Double,
        achievementWeight: Double
    ): RankingEntry? {
        val sql = """
            WITH all_scores AS (
                SELECT 
                    u.id AS user_id,
                    (COALESCE(COUNT(DISTINCT upd.poi_id), 0) * ?) + 
                    (COALESCE(u.total_distance, 0) * ?) + 
                    (COALESCE(u.total_achievement_points, 0) * ?) AS score
                FROM users u
                LEFT JOIN user_poi_discoveries upd ON upd.user_id = u.id
                WHERE u.is_active = true
                GROUP BY u.id, u.total_distance, u.total_achievement_points
            ),
            target_user AS (
                SELECT 
                    u.id AS user_id,
                    u.username,
                    u.avatar_url,
                    COALESCE(COUNT(DISTINCT upd.poi_id), 0) AS total_pois_discovered,
                    COALESCE(u.total_distance, 0) AS total_distance,
                    COALESCE(u.total_achievement_points, 0) AS total_achievement_points,
                    (COALESCE(COUNT(DISTINCT upd.poi_id), 0) * ?) + 
                    (COALESCE(u.total_distance, 0) * ?) + 
                    (COALESCE(u.total_achievement_points, 0) * ?) AS score
                FROM users u
                LEFT JOIN user_poi_discoveries upd ON upd.user_id = u.id
                WHERE u.id = ?::uuid AND u.is_active = true
                GROUP BY u.id, u.username, u.avatar_url, u.total_distance, u.total_achievement_points
            )
            SELECT 
                tu.user_id,
                tu.username,
                tu.avatar_url,
                tu.total_pois_discovered,
                tu.total_distance,
                tu.total_achievement_points,
                tu.score,
                (SELECT COUNT(*) + 1 
                 FROM all_scores 
                 WHERE score > tu.score) AS rank
            FROM target_user tu
        """.trimIndent()

        val results = jdbcTemplate.query(
            sql,
            { rs, _ ->
                RankingEntry(
                    userId = UUID.fromString(rs.getString("user_id")),
                    username = rs.getString("username"),
                    avatarUrl = rs.getString("avatar_url"),
                    totalPoisDiscovered = rs.getInt("total_pois_discovered"),
                    totalDistance = rs.getDouble("total_distance"),
                    totalAchievementPoints = rs.getInt("total_achievement_points"),
                    score = rs.getDouble("score"),
                    rank = rs.getInt("rank")
                )
            },
            poiWeight, distanceWeight, achievementWeight,
            poiWeight, distanceWeight, achievementWeight,
            userId
        )

        return results.firstOrNull()
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
        val score: Double,
        val rank: Int
    )
}
