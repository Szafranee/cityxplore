package org.cityxplore.backend.discoveries.mapper

import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryResponse
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery

fun UserPoiDiscovery.toDto(): UserPoiDiscoveryResponse = UserPoiDiscoveryResponse(
    poiId = this.poiId,
    discoveredAt = this.discoveredAt,
    favorite = this.isFavorite,
    newlyUnlockedAchievements = emptyList() // No achievements for existing discoveries
)

fun List<UserPoiDiscovery>.toDtoList(): List<UserPoiDiscoveryResponse> = this.map { it.toDto() }
