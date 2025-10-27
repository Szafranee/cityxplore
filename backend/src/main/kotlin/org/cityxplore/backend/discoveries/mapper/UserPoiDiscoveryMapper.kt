package org.cityxplore.backend.discoveries.mapper

import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryResponse
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery

fun UserPoiDiscovery.toDto(): UserPoiDiscoveryResponse = UserPoiDiscoveryResponse(
    poiId = this.poiId,
    discoveredAt = this.discoveredAt,
    favorite = this.isFavorite
)

fun List<UserPoiDiscovery>.toDtoList(): List<UserPoiDiscoveryResponse> = this.map { it.toDto() }
