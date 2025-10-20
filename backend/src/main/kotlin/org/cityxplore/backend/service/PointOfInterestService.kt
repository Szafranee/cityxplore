package org.cityxplore.backend.service

import org.cityxplore.backend.dto.PointOfInterestCreateRequest
import org.cityxplore.backend.dto.PointOfInterestResponseDto
import org.cityxplore.backend.mapper.toEntity
import org.cityxplore.backend.mapper.toResponseDto
import org.cityxplore.backend.mapper.toResponseDtoList
import org.cityxplore.backend.repository.PointOfInterestRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Service class for managing Points of Interest (POIs). It handles business logic related to POI data
 * such as retrieving all POIs, retrieving a POI by its unique identifier, and creating a new POI.
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
}
