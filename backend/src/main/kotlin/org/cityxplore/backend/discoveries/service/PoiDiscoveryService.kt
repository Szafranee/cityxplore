package org.cityxplore.backend.discoveries.service

import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryDto
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.cityxplore.backend.discoveries.mapper.toDto
import org.cityxplore.backend.discoveries.mapper.toDtoList
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Service class responsible for managing Point of Interest (POI) discovery by users.
 * It provides functionality for users to discover POIs and retrieve their discovered POIs.
 *
 * @property poiRepository Repository used to interact with the persistence layer for Point of Interest (POI) data.
 * @property userPoiRepository Repository used to manage the relationships between users and discovered POIs.
 */
@Service
class PoiDiscoveryService(
    private val poiRepository: PointOfInterestRepository,
    private val userPoiRepository: UserPoiDiscoveryRepository
) {

    /**
     * Marks a Point of Interest (POI) as discovered for a specific user. The method ensures that the POI
     * exists and has not yet been discovered by the user before adding the discovery.
     * If the POI does not exist, a `ResponseStatusException` with a 404 status is thrown.
     * If the user has already discovered the POI, a `ResponseStatusException`
     * with a 409 status is thrown.
     *
     * @param userId The unique identifier of the user discovering the POI.
     * @param poiId The unique identifier of the Point of Interest to be discovered.
     * @return A `UserPoiDiscoveryDto` object representing the details of the discovered POI.
     * @throws org.springframework.web.server.ResponseStatusException if the POI does not exist or has already been discovered by the user.
     */
    @Transactional
    fun discoverPoi(userId: UUID, poiId: UUID): UserPoiDiscoveryDto {
        if (!poiRepository.existsById(poiId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        }

        val isPoiAlreadyDiscovered = userPoiRepository.existsByUserIdAndPoiId(userId, poiId)
        if (isPoiAlreadyDiscovered) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already discovered")
        }

        val discovery = userPoiRepository.save(
            UserPoiDiscovery(
                userId = userId,
                poiId = poiId
            )
        )

        return discovery.toDto()
    }

    /**
     * Retrieves all Points of Interest (POIs) discovered by a specific user.
     *
     * @param userId The unique identifier of the user whose POI discoveries are to be retrieved.
     * @return A list of `UserPoiDiscoveryDto` objects representing the discovered POIs.
     */
    @Transactional(readOnly = true)
    fun getUserDiscoveries(userId: UUID): List<UserPoiDiscoveryDto> =
        userPoiRepository.findAllByUserId(userId).toDtoList()
}
