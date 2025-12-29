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
    isActive = this.isActive,
    isMajor = this.isMajor,
    isDiscovered = null // Will be calculated in service layer if user is authenticated
)

fun List<PointOfInterest>.toResponseDtoList(): List<PoiResponse> = this.map { it.toResponseDto() }

fun CreatePoiPublicRequest.toEntity(): PointOfInterest = PointOfInterest(
    name = this.name,
    description = this.description,
    category = this.category,
    location = if (latitude != null && longitude != null) {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Invalid lat/lon" }
        GEOMETRY_FACTORY.createPoint(Coordinate(longitude, latitude))
    } else null,
    metadata = this.metadata,
    imageUrls = this.imageUrls
)

// Admin-facing mapping with explicit lat/lon and simplified metadata exposure
fun PointOfInterest.toAdminDto(): PoiAdminResponse = PoiAdminResponse(
    id = id ?: error("POI must have an ID for admin response"),
    name = name,
    description = description,
    category = category,
    latitude = location?.y ?: throw IllegalStateException("POI missing location"),
    longitude = location?.x ?: throw IllegalStateException("POI missing location"),
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)
