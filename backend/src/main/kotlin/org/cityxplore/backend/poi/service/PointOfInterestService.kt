package org.cityxplore.backend.poi.service

import org.cityxplore.backend.poi.dto.CreatePoiPublicRequest
import org.cityxplore.backend.poi.dto.CreatePoiRequest
import org.cityxplore.backend.poi.dto.PoiAdminResponse
import org.cityxplore.backend.poi.dto.PoiResponse
import org.cityxplore.backend.poi.dto.UpdatePoiRequest
import org.cityxplore.backend.poi.entity.PointOfInterest
import org.cityxplore.backend.poi.mapper.toAdminDto
import org.cityxplore.backend.poi.mapper.toEntity
import org.cityxplore.backend.poi.mapper.toResponseDto
import org.cityxplore.backend.poi.mapper.toResponseDtoList
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
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
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    /**
     * Retrieves all Points of Interest (POIs) from the repository and maps them to a list of response DTOs.
     *
     * @return A list of `PoiResponse` objects representing all Points of Interest.
     */
    @Transactional(readOnly = true)
    fun getAll(): List<PoiResponse> =
        poiRepository.findAllByIsActiveTrue()
            .toResponseDtoList()

    /**
     * Retrieves a Point of Interest (POI) by its unique identifier.
     * Throws a `ResponseStatusException` with a 404 status if the POI is not found.
     *
     * @param id The unique identifier of the Point of Interest to retrieve.
     * @return A `PoiResponse` object representing the retrieved Point of Interest.
     */
    @Transactional(readOnly = true)
    fun getById(id: UUID): PoiResponse =
        poiRepository.findByIdIsActiveTrue(id)
            ?.toResponseDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")

    /**
     * Creates a new Point of Interest (POI) and saves it to the database.
     *
     * @param request The details of the POI to be created, encapsulated in a `CreatePoiPublicRequest` object.
     * @return A `PoiResponse` object representing the created POI.
     */
    @Transactional
    fun create(request: CreatePoiPublicRequest): PoiResponse =
        poiRepository.save(request.toEntity()).toResponseDto()

    @Transactional(readOnly = true)
    fun getAllPois(): List<PoiAdminResponse> =
        poiRepository.findAll()
            .filter { it.isActive }
            .map { it.toAdminDto() }

    @Transactional(readOnly = true)
    fun getPoiById(id: UUID): PoiAdminResponse =
        poiRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found") }
            .toAdminDto()

    @Transactional
    fun createPoi(createPoi: CreatePoiRequest): PoiAdminResponse {
        require(createPoi.longitude in -180.0..180.0 && createPoi.latitude in -90.0..90.0) {
            "Invalid coordinates"
        }

        val saved = poiRepository.save(
            PointOfInterest(
                name = createPoi.name,
                description = createPoi.description,
                category = createPoi.category,
                location = geometryFactory.createPoint(Coordinate(createPoi.longitude, createPoi.latitude)),
                metadata = createPoi.metadata,
                isActive = true
            )
        )

        return saved.toAdminDto()
    }

    @Transactional
    fun updatePoi(id: UUID, updatePoi: UpdatePoiRequest): PoiAdminResponse {
        val existing = poiRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found") }

        require(updatePoi.longitude in -180.0..180.0 && updatePoi.latitude in -90.0..90.0) {
            "Invalid coordinates"
        }

        existing.apply {
            name = updatePoi.name
            description = updatePoi.description
            category = updatePoi.category
            location = geometryFactory.createPoint(Coordinate(updatePoi.longitude, updatePoi.latitude))
            metadata = updatePoi.metadata
        }

        return poiRepository.save(existing).toAdminDto()
    }

    @Transactional
    fun deletePoi(id: UUID) {
        if (!poiRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        }

        poiRepository.deleteById(id)
    }
}
