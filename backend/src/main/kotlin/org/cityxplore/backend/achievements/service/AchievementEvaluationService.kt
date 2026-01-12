package org.cityxplore.backend.achievements.service

import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.cityxplore.backend.achievements.repository.UserAchievementRepository
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.user.entity.User
import org.cityxplore.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for evaluating and granting achievements based on user progress.
 *
 * Achievements are evaluated in an event-driven manner:
 * - After POI discovery: evaluates discovery-based achievements (count, category, time_range)
 * - After distance sync: evaluates distance-based achievements (distance_km)
 *
 * Each achievement has criteria defined in JSON format, e.g.:
 * - {"count": 10} - discover X POIs
 * - {"category": "Park", "count": 5} - discover X POIs of category Y
 * - {"distance_km": 42} - travel X kilometers
 * - {"time_range": "22:00-04:00"} - discover POI during specific hours
 */
@Service
class AchievementEvaluationService(
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository,
    private val userRepository: UserRepository,
    private val achievementService: AchievementService,
    private val userPoiDiscoveryRepository: UserPoiDiscoveryRepository,
    private val poiRepository: PointOfInterestRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Evaluates distance-based achievements after a distance sync.
     *
     * @param userId The user to evaluate achievements for.
     * @return List of newly granted achievement IDs.
     */
    @Transactional
    fun evaluateDistanceAchievements(userId: UUID): List<UUID> {
        return evaluateAchievementsByType(userId, "distance_km")
    }

    /**
     * Evaluates discovery-based achievements after a POI discovery.
     *
     * @param userId The user to evaluate achievements for.
     * @return List of newly granted achievement IDs.
     */
    @Transactional
    fun evaluateDiscoveryAchievements(userId: UUID): List<UUID> {
        return evaluateAchievementsByType(userId, "count", "category", "time_range")
    }

    /**
     * Evaluates achievements that match any of the specified criteria types.
     */
    private fun evaluateAchievementsByType(userId: UUID, vararg criteriaTypes: String): List<UUID> {
        val user = userRepository.findById(userId).orElse(null) ?: return emptyList()

        if (!user.isActive) {
            return emptyList()
        }

        val earnedIds = userAchievementRepository.findAllByUserId(userId)
            .map { it.achievementId }
            .toSet()

        val allAchievements = achievementRepository.findAllByIsActiveTrue()

        val relevantAchievements = allAchievements.filter { achievement ->
            criteriaTypes.any { type -> achievement.criteria.containsKey(type) }
        }

        val newlyGranted = mutableListOf<UUID>()

        for (achievement in relevantAchievements) {
            if (achievement.id in earnedIds) continue

            if (checkCriteria(userId, user, achievement.criteria)) {
                try {
                    val result = achievementService.grantAchievement(userId, achievement.id!!)
                    if (result.created) {
                        newlyGranted.add(achievement.id!!)
                        logger.info(
                            "User {} earned achievement: {} (+{} pts)",
                            userId,
                            achievement.name,
                            achievement.points
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to grant achievement {} to user {}: {}", achievement.id, userId, e.message)
                }
            }
        }

        return newlyGranted
    }

    /**
     * Checks if a user meets the criteria for an achievement.
     *
     * Supported criteria:
     * - distance_km: Total distance travelled in kilometers
     * - count: Amount POIs discovered (optionally filtered by category)
     * - category: POI category filter (used with count)
     * - time_range: Time of day when POI was discovered (e.g., "22:00-04:00")
     */
    private fun checkCriteria(userId: UUID, user: User, criteria: Map<String, Any?>): Boolean {
        // Distance criteria: {"distance_km": 42}
        criteria["distance_km"]?.let { requiredKm ->
            val requiredMeters = (requiredKm as Number).toDouble() * 1000
            if (user.totalDistance.toDouble() < requiredMeters) {
                return false
            }
        }

        // Count criteria: {"count": 10} or {"count": 5, "category": "Park"}
        criteria["count"]?.let { requiredCount ->
            val category = criteria["category"] as? String
            val actualCount = if (category != null) {
                countDiscoveriesByCategory(userId, category)
            } else {
                user.totalPoisDiscovered
            }
            if (actualCount < (requiredCount as Number).toInt()) {
                return false
            }
        }

        // Time range criteria: {"time_range": "22:00-04:00"}
        criteria["time_range"]?.let { range ->
            if (!hasDiscoveryInTimeRange(userId, range as String)) {
                return false
            }
        }

        return true
    }

    /**
     * Counts how many POIs of a specific category the user has discovered.
     */
    private fun countDiscoveriesByCategory(userId: UUID, category: String): Int {
        val discoveries = userPoiDiscoveryRepository.findAllByUserId(userId)
        val poiIds = discoveries.map { it.poiId }

        if (poiIds.isEmpty()) return 0

        val pois = poiRepository.findAllById(poiIds)
        return pois.count { poi ->
            poi.category.equals(category, ignoreCase = true)
        }
    }

    /**
     * Checks if the user has discovered any POI within the specified time range.
     *
     * Supports overnight ranges (e.g., "22:00-04:00" means 10 PM to 4 AM).
     */
    private fun hasDiscoveryInTimeRange(userId: UUID, timeRange: String): Boolean {
        val parts = timeRange.split("-")
        if (parts.size != 2) {
            logger.warn("Invalid time_range format: {}", timeRange)
            return false
        }

        val startHour = parts[0].substringBefore(":").toIntOrNull() ?: return false
        val endHour = parts[1].substringBefore(":").toIntOrNull() ?: return false

        val discoveries = userPoiDiscoveryRepository.findAllByUserId(userId)

        return discoveries.any { discovery ->
            val hour = discovery.discoveredAt.hour
            if (startHour > endHour) {
                // Overnight range (e.g., 22:00-04:00)
                hour !in endHour..<startHour
            } else {
                // Same-day range (e.g., 09:00-17:00)
                hour in startHour until endHour
            }
        }
    }
}
