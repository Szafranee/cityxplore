package org.cityxplore.backend.discoveries.mapper

import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryDto
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery

fun UserPoiDiscovery.toDto(): UserPoiDiscoveryDto = UserPoiDiscoveryDto(
    poiId = this.poiId,
    discoveredAt = this.discoveredAt,
    favorite = this.isFavorite
)

fun List<UserPoiDiscovery>.toDtoList(): List<UserPoiDiscoveryDto> = this.map { it.toDto() }
