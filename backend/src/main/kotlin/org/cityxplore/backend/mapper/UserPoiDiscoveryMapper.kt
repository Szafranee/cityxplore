package org.cityxplore.backend.mapper

import org.cityxplore.backend.dto.UserPoiDiscoveryDto
import org.cityxplore.backend.entity.UserPoiDiscovery

fun UserPoiDiscovery.toDto(): UserPoiDiscoveryDto = UserPoiDiscoveryDto(
    poiId = this.poiId,
    discoveredAt = this.discoveredAt,
    favorite = this.isFavorite
)

fun List<UserPoiDiscovery>.toDtoList(): List<UserPoiDiscoveryDto> = this.map { it.toDto() }
