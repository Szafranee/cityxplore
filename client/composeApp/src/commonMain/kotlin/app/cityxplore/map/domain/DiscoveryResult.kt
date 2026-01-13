package app.cityxplore.map.domain

import app.cityxplore.achievements.domain.Achievement

/**
 * Result of a POI discovery operation.
 *
 * @property newlyDiscoveredPoiIds List of POI IDs that were newly discovered.
 * @property newlyUnlockedAchievements List of achievements unlocked as a result of the discovery.
 */
data class DiscoveryResult(
    val newlyDiscoveredPoiIds: List<String>,
    val newlyUnlockedAchievements: List<Achievement>
)
