package org.cityxplore.backend.mapper

import org.cityxplore.backend.dto.PointOfInterestCreateRequest
import org.cityxplore.backend.dto.PointOfInterestResponseDto
import org.cityxplore.backend.entity.PointOfInterest
import org.springframework.data.geo.Point

fun PointOfInterest.toResponseDto(): PointOfInterestResponseDto = PointOfInterestResponseDto(
    id = this.id,
    name = this.name,
    description = this.description,
    category = this.category,
    latitude = this.location?.y,
    longitude = this.location?.x,
    metadata = this.metadata,
    imageUrls = this.imageUrls,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    isActive = this.isActive
)

fun List<PointOfInterest>.toResponseDtoList(): List<PointOfInterestResponseDto> = this.map { it.toResponseDto() }

fun PointOfInterestCreateRequest.toEntity(): PointOfInterest = PointOfInterest(
    name = this.name,
    description = this.description,
    category = this.category,
    location = if (latitude != null && longitude != null) Point(longitude, latitude) else null,
    metadata = this.metadata,
    imageUrls = this.imageUrls
)
