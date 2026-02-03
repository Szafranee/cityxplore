package org.cityxplore.backend.achievements.service

import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service responsible for calculating current progress for achievements.
 *
 * Progress data is calculated dynamically based on the achievement criteria
 * and the user's current stats (discoveries, distance, friends, etc.).
 */
@Service
class AchievementProgressService(
    private val userRepository: UserRepository,
    private val userPoiDiscoveryRepository: UserPoiDiscoveryRepository,
    private val poiRepository: PointOfInterestRepository,
    private val friendshipRepository: FriendshipRepository
) {

    /**
     * Calculates the current progress for an achievement.
     *
     * Returns a map with progress data that matches the criteria format:
     * - For {"poi_count": 10}: returns {"poi_count": currentCount}
     * - For {"distance_km": 42}: returns {"distance_km": currentKm}
     * - For {"friend_count": 5}: returns {"friend_count": currentFriends}
     * - For {"time_range": "..."}: returns {"time_range_met": true/false}
     * - For {"poi_count": 5, "category": "Park"}: returns {"poi_count": currentCount, "category": "Park"}
     * - For {"poi_count": 5, "is_major": true}: returns {"poi_count": currentCount, "is_major": true}
     *
     * @param userId The user to calculate progress for.
     * @param achievement The achievement definition.
     * @return A map containing progress data.
     */
    fun calculateProgress(userId: UUID, achievement: Achievement): Map<String, Any?> {
        val criteria = achievement.criteria
        val progress = mutableMapOf<String, Any?>()

        // Distance criteria: {"distance_km": 42}
        criteria["distance_km"]?.let {
            val user = userRepository.findById(userId).orElse(null)
            val distanceKm = (user?.totalDistance?.toDouble() ?: 0.0) / 1000.0
            // Round to 2 decimal places
            progress["distance_km"] = (distanceKm * 100).toLong() / 100.0
        }

        // Friend count criteria: {"friend_count": 5}
        criteria["friend_count"]?.let {
            val friendCount = friendshipRepository.countAcceptedByUserId(userId).toInt()
            progress["friend_count"] = friendCount
        }

        // POI count criteria: {"poi_count": 10} with optional category and is_major filters
        if (criteria.containsKey("poi_count")) {
            val category = criteria["category"] as? String
            val isMajorOnly = criteria["is_major"] as? Boolean ?: false

            val actualCount = when {
                category != null && isMajorOnly -> countDiscoveriesByCategoryAndMajor(userId, category, true)
                category != null -> countDiscoveriesByCategory(userId, category)
                isMajorOnly -> countMajorDiscoveries(userId)
                else -> userPoiDiscoveryRepository.countByUserId(userId).toInt()
            }

            progress["poi_count"] = actualCount

            // Include filter keys in progress for context
            if (category != null) {
                progress["category"] = category
            }
            if (isMajorOnly) {
                progress["is_major"] = true
            }
        }

        // Time range criteria: {"time_range": "22:00-04:00"}
        criteria["time_range"]?.let { range ->
            val hasMet = hasDiscoveryInTimeRange(userId, range as String)
            progress["time_range_met"] = hasMet
        }

        return progress
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
     * Counts how many major POIs (landmarks) the user has discovered.
     */
    private fun countMajorDiscoveries(userId: UUID): Int {
        val discoveries = userPoiDiscoveryRepository.findAllByUserId(userId)
        val poiIds = discoveries.map { it.poiId }

        if (poiIds.isEmpty()) return 0

        val pois = poiRepository.findAllById(poiIds)
        return pois.count { poi -> poi.isMajor }
    }

    /**
     * Counts how many POIs of a specific category with major flag the user has discovered.
     */
    private fun countDiscoveriesByCategoryAndMajor(userId: UUID, category: String, isMajor: Boolean): Int {
        val discoveries = userPoiDiscoveryRepository.findAllByUserId(userId)
        val poiIds = discoveries.map { it.poiId }

        if (poiIds.isEmpty()) return 0

        val pois = poiRepository.findAllById(poiIds)
        return pois.count { poi ->
            poi.category.equals(category, ignoreCase = true) && poi.isMajor == isMajor
        }
    }

    /**
     * Checks if the user has discovered any POI within the specified time range.
     *
     * Supports overnight ranges (e.g., "22:00-04:00" means 10 PM to 4 AM).
     */
    private fun hasDiscoveryInTimeRange(userId: UUID, timeRange: String): Boolean {
        val parts = timeRange.split("-")
        if (parts.size != 2) return false

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
