package org.cityxplore.backend.controller

import jakarta.validation.Valid
import org.cityxplore.backend.dto.PointOfInterestCreateRequest
import org.cityxplore.backend.dto.PointOfInterestResponseDto
import org.cityxplore.backend.service.PointOfInterestService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * Controller class handling endpoints related to Points of Interest (POIs).
 *
 * This class handles providing API operations around the `PointOfInterest`
 * entity, such as retrieving all POIs, fetching a single POI by its ID, and creating
 * new POIs.
 *
 * @param poiService The service used for accessing and persisting `PointOfInterest` entities.
 */
@RestController
@RequestMapping("/api/pois")
class PointOfInterestController(
    private val poiService: PointOfInterestService
) {

    /**
     * Retrieves all Points of Interest (POIs) from the service.
     *
     * @return a list of all `PointOfInterest` entities.
     */
    @GetMapping
    fun getAllPOIs(): List<PointOfInterestResponseDto> = poiService.getAll()

    /**
     * Retrieves a specific Point of Interest (POI) by its unique identifier.
     *
     * @param id The unique identifier of the POI to be retrieved.
     * @return The `PointOfInterest` entity matching the provided ID.
     * @throws java.util.NoSuchElementException If no `PointOfInterest` with the specified ID is found.
     */
    @GetMapping("/{id}")
    fun getPOI(@PathVariable id: UUID): PointOfInterestResponseDto = poiService.getById(id)

    /**
     * Creates a new Point of Interest (POI) and saves it to the repository.
     *
     * @param poiCreateRequest The request object containing the details of the POI to be created.
     * @return The newly created Point of Interest as a response DTO after being persisted in the repository.
     */
    @PostMapping
    fun createPOI(@Valid @RequestBody poiCreateRequest: PointOfInterestCreateRequest): ResponseEntity<PointOfInterestResponseDto> {
        val created = poiService.create(poiCreateRequest)
        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }
}