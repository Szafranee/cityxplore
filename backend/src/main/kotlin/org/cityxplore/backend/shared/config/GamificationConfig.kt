package org.cityxplore.backend.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration for gamification XP points.
 *
 * These points are added directly to totalAchievementPoints for activities,
 * separate from achievement-based points.
 */
@Component
@ConfigurationProperties(prefix = "app.gamification")
class GamificationConfig {
    /**
     * XP points awarded for each POI discovery.
     */
    var pointsPerPoiDiscovery: Int = 10

    /**
     * XP points awarded for every 100 meters travelled.
     */
    var pointsPer100Meters: Int = 1
}
