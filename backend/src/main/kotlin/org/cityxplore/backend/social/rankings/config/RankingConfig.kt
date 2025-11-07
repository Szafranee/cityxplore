package org.cityxplore.backend.social.rankings.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for ranking score calculation.
 * These weights determine how much each activity contributes to the overall score.
 *
 * Score formula:
 * score = (totalPoisDiscovered * poiWeight) +
 *         (totalDistance * distanceWeight) +
 *         (totalAchievementPoints * achievementWeight)
 *
 * @property poiWeight weight for discovered POIs (default: 100 points per POI)
 * @property distanceWeight weight for travelled distance (default: 0.01 points per meter = 10 points per km)
 * @property achievementWeight weight for achievement points (default: 1 point per achievement point)
 */
@Configuration
@ConfigurationProperties(prefix = "app.rankings")
data class RankingConfig(
    var poiWeight: Int = 100,
    var distanceWeight: Double = 0.01,
    var achievementWeight: Int = 1
)
