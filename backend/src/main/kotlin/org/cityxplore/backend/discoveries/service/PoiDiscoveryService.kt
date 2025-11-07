package org.cityxplore.backend.discoveries.service

import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryResponse
import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.cityxplore.backend.discoveries.mapper.toDto
import org.cityxplore.backend.discoveries.mapper.toDtoList
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
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
    private val userPoiRepository: UserPoiDiscoveryRepository,
    private val userRepository: UserRepository
) {

    /**
     * Marks a Point of Interest (POI) as discovered for a specific user. The method ensures that the POI
     * exists and has not yet been discovered by the user before adding the discovery.
     * After a successful discovery, increments the user's totalPoisDiscovered counter.
     * If the POI does not exist, a `ResponseStatusException` with a 404 status is thrown.
     * If the user has already discovered the POI, a `ResponseStatusException`
     * with a 409 status is thrown.
     *
     * @param userId The unique identifier of the user discovering the POI.
     * @param poiId The unique identifier of the Point of Interest to be discovered.
     * @return A `UserPoiDiscoveryResponse` object representing the details of the discovered POI.
     * @throws org.springframework.web.server.ResponseStatusException if the POI does not exist or has already been discovered by the user.
     */
    @Transactional
    fun discoverPoi(userId: UUID, poiId: UUID): UserPoiDiscoveryResponse {
        if (!poiRepository.existsById(poiId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        }

        val discovery = try {
            userPoiRepository.save(
                UserPoiDiscovery(
                    userId = userId,
                    poiId = poiId
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already discovered", e)
        }

        // Increment user's total POIs discovered counter
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        user.totalPoisDiscovered = user.totalPoisDiscovered + 1
        userRepository.save(user)

        return discovery.toDto()
    }


    /**
     * Retrieves all Points of Interest (POIs) discovered by a specific user.
     *
     * @param userId The unique identifier of the user whose POI discoveries are to be retrieved.
     * @return A list of `UserPoiDiscoveryResponse` objects representing the discovered POIs.
     */
    @Transactional(readOnly = true)
    fun getUserDiscoveries(userId: UUID): List<UserPoiDiscoveryResponse> =
        userPoiRepository.findAllByUserId(userId).toDtoList()


    /**
     * Retrieves a specific Point of Interest (POI) discovery for a given user.
     * If the discovery is not found, a `ResponseStatusException` with a 404 status is thrown.
     *
     * @param userId The unique identifier of the user whose POI discovery is being retrieved.
     * @param poiId The unique identifier of the POI being retrieved.
     * @return A `UserPoiDiscoveryResponse` object containing the details of the discovered POI.
     * @throws org.springframework.web.server.ResponseStatusException if the discovery is not found.
     */
    @Transactional(readOnly = true)
    fun getUserDiscovery(userId: UUID, poiId: UUID): UserPoiDiscoveryResponse =
        userPoiRepository.findByUserIdAndPoiId(userId, poiId)
            ?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Discovery not found")
}
