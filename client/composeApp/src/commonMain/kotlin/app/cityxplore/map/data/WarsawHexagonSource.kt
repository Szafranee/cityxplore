package app.cityxplore.map.data

import app.cityxplore.map.domain.RegionDefinition

/**
 * Platform-specific loader for region hex grids.
 *
 * This generates H3 hex indices covering a region's boundary via polyfill.
 *
 * Android actual reads GeoJSON from assets and uses the H3 library.
 * iOS can be implemented later (or return an empty set to fall back to backend).
 *
 * @param region The region definition (contains assetPath and resolution).
 * @return Set of H3 indices as strings covering the region.
 */
internal expect suspend fun loadRegionHexagons(region: RegionDefinition): Set<String>

/**
 * Platform-specific function to clear the hexagon cache.
 * Called on logout to ensure fresh computation on the next login.
 */
internal expect fun clearHexagonCache()

/**
 * Legacy wrapper for Warsaw-specific hexagons.
 * Delegates to [loadRegionHexagons] with the Warsaw region definition.
 *
 * @param resolution H3 resolution (overrides the default in RegionDefinition.WARSAW).
 */
internal suspend fun loadWarsawHexagons(resolution: Int): Set<String> {
    val warsawRegion = RegionDefinition.WARSAW.copy(h3Resolution = resolution)
    return loadRegionHexagons(warsawRegion)
}
