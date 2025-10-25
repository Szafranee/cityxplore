package org.cityxplore.backend.poi.service

import org.cityxplore.backend.poi.dto.CreatePoiDto
import org.cityxplore.backend.poi.dto.PointOfInterestCreateRequest
import org.cityxplore.backend.poi.dto.PointOfInterestDto
import org.cityxplore.backend.poi.dto.PointOfInterestResponseDto
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.cityxplore.backend.poi.mapper.toEntity
import org.cityxplore.backend.poi.mapper.toResponseDto
import org.cityxplore.backend.poi.mapper.toResponseDtoList
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service class for managing Points of Interest (POIs). It handles business logic related to POI data
 * such as retrieving all POIs, retrieving a POI by its unique identifier, and creating a new POI.
 *
 * Admin helpers (getAllPois, createPoi, ...) expose richer DTOs used by admin controllers.
 *
 * @property poiRepository Repository used to interact with the persistence layer for POI data.
 */
@Service
class PointOfInterestService(
    private val poiRepository: PointOfInterestRepository
) {

    /**
     * Retrieves all Points of Interest (POIs) from the repository and maps them to a list of response DTOs.
     *
     * @return A list of `PointOfInterestResponseDto` objects representing all Points of Interest.
     */
    @Transactional(readOnly = true)
    fun getAll(): List<PointOfInterestResponseDto> =
        poiRepository.findAll().toResponseDtoList()

    /**
     * Retrieves a Point of Interest (POI) by its unique identifier.
     * Throws a `ResponseStatusException` with a 404 status if the POI is not found.
     *
     * @param id The unique identifier of the Point of Interest to retrieve.
     * @return A `PointOfInterestResponseDto` object representing the retrieved Point of Interest.
     */
    @Transactional(readOnly = true)
    fun getById(id: UUID): PointOfInterestResponseDto =
        poiRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found") }
            .toResponseDto()

    /**
     * Creates a new Point of Interest (POI) and saves it to the database.
     *
     * @param request The details of the POI to be created, encapsulated in a `PointOfInterestCreateRequest` object.
     * @return A `PointOfInterestResponseDto` object representing the created POI.
     */
    @Transactional
    fun create(request: PointOfInterestCreateRequest): PointOfInterestResponseDto =
        poiRepository.save(request.toEntity()).toResponseDto()

    @Transactional(readOnly = true)
    fun getAllPois(): List<PointOfInterestDto> =
        poiRepository.findAll()
            .filter { it.isActive }
            .map { it.toDto() }

    @Transactional(readOnly = true)
    fun getPoiById(id: UUID): PointOfInterestDto =
        poiRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found") }
            .toDto()

    @Transactional
    fun createPoi(createPoiDto: CreatePoiDto): PointOfInterestDto {
        val poi = poiRepository.save(
            PointOfInterest(
                name = createPoiDto.name,
                description = createPoiDto.description,
                category = createPoiDto.category,
                // PostGIS + latitude/longitude (x: lon, y: lat)
                location = GeometryFactory().createPoint(
                    Coordinate(
                        createPoiDto.longitude,
                        createPoiDto.latitude
                    )
                ),
                metadata = createPoiDto.metadata,
                createdAt = LocalDateTime.now(),
                isActive = true
            )
        )
        return poi.toDto()
    }

    @Transactional
    fun updatePoi(id: UUID, createPoiDto: CreatePoiDto): PointOfInterestDto {
        val existing = poiRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found") }

        val saved = poiRepository.save(
            existing.copy(
                name = createPoiDto.name,
                description = createPoiDto.description,
                category = createPoiDto.category,
                location = GeometryFactory().createPoint(
                    Coordinate(
                        createPoiDto.longitude,
                        createPoiDto.latitude
                    )
                ),
                updatedAt = LocalDateTime.now(),
                metadata = createPoiDto.metadata
            )
        )
        return saved.toDto()
    }

    @Transactional
    fun deletePoi(id: UUID) {
        if (!poiRepository.existsById(id))
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        poiRepository.deleteById(id)
    }
}

private fun PointOfInterest.toDto(): PointOfInterestDto =
    PointOfInterestDto(
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
