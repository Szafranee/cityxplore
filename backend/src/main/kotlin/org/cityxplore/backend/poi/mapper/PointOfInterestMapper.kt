package org.cityxplore.backend.poi.mapper

import org.cityxplore.backend.poi.dto.PointOfInterestCreateRequest
import org.cityxplore.backend.poi.dto.PointOfInterestResponseDto
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

private val GEOMETRY_FACTORY = GeometryFactory(PrecisionModel(), 4326)

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
    location = if (latitude != null && longitude != null) GEOMETRY_FACTORY.createPoint(
        Coordinate(longitude, latitude)
    ) else null,
    metadata = this.metadata,
    imageUrls = this.imageUrls
)
