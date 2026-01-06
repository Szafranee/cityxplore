package app.cityxplore.map.data

import app.cityxplore.map.domain.RegionDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun loadRegionHexagons(region: RegionDefinition): Set<String> =
    withContext(Dispatchers.Default) {
        // Not implemented yet on iOS. Keep empty so common code can fall back to backend.
        emptySet()
    }
