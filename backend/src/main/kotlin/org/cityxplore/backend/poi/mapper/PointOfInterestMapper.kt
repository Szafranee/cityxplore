package org.cityxplore.backend.poi.mapper

import org.cityxplore.backend.poi.dto.CreatePoiPublicRequest
import org.cityxplore.backend.poi.dto.PoiAdminResponse
import org.cityxplore.backend.poi.dto.PoiResponse
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

private val GEOMETRY_FACTORY = GeometryFactory(PrecisionModel(), 4326)

fun PointOfInterest.toResponseDto(): PoiResponse = PoiResponse(
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

fun List<PointOfInterest>.toResponseDtoList(): List<PoiResponse> = this.map { it.toResponseDto() }

fun CreatePoiPublicRequest.toEntity(): PointOfInterest = PointOfInterest(
    name = this.name,
    description = this.description,
    category = this.category,
    location = if (latitude != null && longitude != null) GEOMETRY_FACTORY.createPoint(
        Coordinate(longitude, latitude)
    ) else null,
    metadata = this.metadata,
    imageUrls = this.imageUrls
)

// Admin-facing mapping with explicit lat/lon and simplified metadata exposure
fun PointOfInterest.toAdminDto(): PoiAdminResponse = PoiAdminResponse(
    id = id,
    name = name,
    description = description,
    category = category,
    latitude = location?.y ?: 0.0,
    longitude = location?.x ?: 0.0,
    metadata = metadata?.let { mapOf("raw" to it) },
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)
