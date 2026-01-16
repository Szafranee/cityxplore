package org.cityxplore.backend.shared.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GamificationConfigTest {

    @Test
    fun `should have default values for XP points`() {
        // given
        val config = GamificationConfig()

        // when & then
        assertEquals(10, config.pointsPerPoiDiscovery, "Default points per POI discovery should be 10")
        assertEquals(1, config.pointsPer100Meters, "Default points per 100m should be 1")
    }

    @Test
    fun `should allow modification of points configuration`() {
        // given
        val config = GamificationConfig()

        // when
        config.pointsPerPoiDiscovery = 20
        config.pointsPer100Meters = 2

        // then
        assertEquals(20, config.pointsPerPoiDiscovery)
        assertEquals(2, config.pointsPer100Meters)
    }

    @Test
    fun `should calculate XP correctly with custom config`() {
        // given
        val config = GamificationConfig().apply {
            pointsPerPoiDiscovery = 15
            pointsPer100Meters = 2
        }

        // when
        val poiXp = config.pointsPerPoiDiscovery
        val distanceXp = (350.0 / 100.0 * config.pointsPer100Meters).toInt() // 350m

        // then
        assertEquals(15, poiXp, "POI discovery should award 15 XP")
        assertEquals(7, distanceXp, "350m should award 7 XP (350/100 * 2)")
    }

    @Test
    fun `should support zero points configuration`() {
        // given
        val config = GamificationConfig().apply {
            pointsPerPoiDiscovery = 0
            pointsPer100Meters = 0
        }

        // when & then
        assertEquals(0, config.pointsPerPoiDiscovery)
        assertEquals(0, config.pointsPer100Meters)
    }
}
