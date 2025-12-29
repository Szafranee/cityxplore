package org.cityxplore.backend.poi.service

import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
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
import org.springframework.security.core.context.SecurityContextHolder
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
 * @property userPoiDiscoveryRepository Repository for user POI discoveries.
 */
@Service
class PointOfInterestService(
    private val poiRepository: PointOfInterestRepository,
    private val userPoiDiscoveryRepository: UserPoiDiscoveryRepository
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    /**
     * Retrieves all Points of Interest (POIs) from the repository and maps them to a list of response DTOs.
     * If user is authenticated, includes isDiscovered flag for each POI.
     *
     * @return A list of `PoiResponse` objects representing all Points of Interest.
     */
    @Transactional(readOnly = true)
    fun getAll(): List<PoiResponse> {
        val pois = poiRepository.findAllByIsActiveTrue()
        val userId = getCurrentUserId()

        return if (userId != null) {
            // User is authenticated - fetch their discoveries
            val discoveredPoiIds = userPoiDiscoveryRepository.findAllByUserId(userId)
                .map { it.poiId }
                .toSet()

            pois.map { poi ->
                poi.toResponseDto().copy(
                    isDiscovered = discoveredPoiIds.contains(poi.id)
                )
            }
        } else {
            // User not authenticated - return without discovery status
            pois.toResponseDtoList()
        }
    }

    /**
     * Helper function to extract the current user ID from Security Context.
     * Returns null if the user is not authenticated or if the principal is not a valid UUID.
     */
    private fun getCurrentUserId(): UUID? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication?.principal is String) {
                UUID.fromString(authentication.principal as String)
            } else {
                null
            }
        } catch (_: IllegalArgumentException) {
            // Invalid UUID format - return null
            null
        }
    }

    /**
     * Retrieves a Point of Interest (POI) by its unique identifier.
     * If user is authenticated, includes isDiscovered flag.
     * Throws a `ResponseStatusException` with a 404 status if the POI is not found.
     *
     * @param id The unique identifier of the Point of Interest to retrieve.
     * @return A `PoiResponse` object representing the retrieved Point of Interest.
     */
    @Transactional(readOnly = true)
    fun getById(id: UUID): PoiResponse {
        val poi = poiRepository.findByIdIsActiveTrue(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")

        val userId = getCurrentUserId()

        return if (userId != null) {
            // User is authenticated - check if they discovered this POI
            val isDiscovered = userPoiDiscoveryRepository.existsByUserIdAndPoiId(userId, id)
            poi.toResponseDto().copy(isDiscovered = isDiscovered)
        } else {
            // User not authenticated - return without discovery status
            poi.toResponseDto()
        }
    }

    /**
     * Creates a new Point of Interest (POI) and saves it to the database.
     *
     * @param request The details of the POI to be created, encapsulated in a `CreatePoiPublicRequest` object.
     * @return A `PoiResponse` object representing the created POI.
     */
    @Transactional
    fun create(request: CreatePoiPublicRequest): PoiResponse =
        poiRepository.save(request.toEntity()).toResponseDto()

    /**
     * Retrieves all Points of Interest (POIs) from the repository that are active and maps them
     * to a list of admin-facing response DTOs.
     *
     * @return A list of `PoiAdminResponse` objects representing all active Points of Interest
     * in an admin-friendly format.
     */
    @Transactional(readOnly = true)
    fun getAllPois(): List<PoiAdminResponse> =
        poiRepository.findAll()
            .map { it.toAdminDto() }

    /**
     * Retrieves a Point of Interest (POI) by its unique identifier and maps it to an admin-facing DTO.
     * Throws a `ResponseStatusException` with a 404 status if the POI is not found.
     *
     * @param id The unique identifier of the Point of Interest to retrieve.
     * @return A `PoiAdminResponse` object representing the retrieved Point of Interest.
     */
    @Transactional(readOnly = true)
    fun getPoiById(id: UUID): PoiAdminResponse =
        poiRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found") }
            .toAdminDto()

    /**
     * Creates a new Point of Interest (POI) and saves it in the repository. The POI is populated with
     * details such as name, description, category, geographical location, and optional metadata.
     *
     * @param createPoi The details of the POI to be created, encapsulated in a CreatePoiRequest object.
     *                  It includes the name, description, category, latitude, longitude, and metadata.
     * @return A PoiAdminResponse object representing the created Point of Interest, including its
     *         administrative-level information and audit metadata.
     * @throws IllegalArgumentException If the provided latitude is not in the range -90.0 to 90.0 or
     *                                  if the longitude is not in the range -180.0 to 180.0.
     */
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

    /**
     * Updates an existing Point of Interest (POI) with the provided details.
     * Throws a `ResponseStatusException` with a 404 status if the POI is not found.
     * Validates that the provided latitude and longitude are within the valid range.
     *
     * @param id The unique identifier of the Point of Interest to update.
     * @param updatePoi The details to update the Point of Interest, encapsulated in an `UpdatePoiRequest` object.
     * @return A `PoiAdminResponse` object representing the updated Point of Interest.
     */
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

    /**
     * Deletes a Point of Interest (POI) by its unique identifier.
     * Throws a `ResponseStatusException` with a 404 status if the POI is not found.
     *
     * @param id The unique identifier of the Point of Interest to delete.
     */
    @Transactional
    fun deletePoi(id: UUID) {
        if (!poiRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        }

        poiRepository.deleteById(id)
    }
}
