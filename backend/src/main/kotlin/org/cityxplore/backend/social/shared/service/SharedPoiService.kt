package org.cityxplore.backend.social.shared.service

import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.cityxplore.backend.social.shared.dto.SharePoiRequest
import org.cityxplore.backend.social.shared.dto.SharedPoiResponse
import org.cityxplore.backend.social.shared.entity.SharedPoi
import org.cityxplore.backend.social.shared.mapper.SharedPoiMapper
import org.cityxplore.backend.social.shared.repository.SharedPoiRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service responsible for managing shared Points of Interest between users.
 * Handles sharing POIs, tracking views, and retrieving shared POI history.
 * POIs can only be shared between users who are accepted friends.
 */
@Service
class SharedPoiService(
    private val sharedPoiRepository: SharedPoiRepository,
    private val poiRepository: PointOfInterestRepository,
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository
) {

    /**
     * Shares a Point of Interest with another user.
     *
     * This method validates that:
     * - Either poiId OR customPoi is provided (XOR logic)
     * - The sharer is not sharing with themselves
     * - The recipient user exists
     * - The users are accepted friends
     * - If poiId is provided, the POI exists in the database
     *
     * @param sharerId the UUID of the user sharing the POI
     * @param sharePoiRequest the request containing recipient ID, either poiId or customPoi, and optional message
     * @return a SharedPoiResponse representing the created shared POI record
     * @throws ResponseStatusException if validation fails (400 BAD_REQUEST, 403 FORBIDDEN, or 404 NOT_FOUND)
     */
    @Transactional
    fun sharePoi(sharerId: UUID, sharePoiRequest: SharePoiRequest): SharedPoiResponse {
        // Validate XOR: exactly one of poiId or customPoi must be provided
        val hasPoiId = sharePoiRequest.poiId != null
        val hasCustomPoi = sharePoiRequest.customPoi != null

        if (hasPoiId == hasCustomPoi) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Exactly one of poiId or customPoi must be provided"
            )
        }

        if (sharerId == sharePoiRequest.recipientId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot share with yourself")
        }

        if (!userRepository.existsById(sharePoiRequest.recipientId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found")
        }

        if (!friendshipRepository.areFriends(sharerId, sharePoiRequest.recipientId)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can only share POIs with accepted friends"
            )
        }

        // If sharing an existing POI, validate it exists
        if (hasPoiId && !poiRepository.existsById(sharePoiRequest.poiId!!)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        }

        val shared = sharedPoiRepository.save(
            SharedPoi(
                sharerId = sharerId,
                recipientId = sharePoiRequest.recipientId,
                poiId = sharePoiRequest.poiId,
                poiData = sharePoiRequest.customPoi,
                message = sharePoiRequest.message
            )
        )

        return SharedPoiMapper.toResponse(shared)
    }

    /**
     * Retrieves details of a specific shared POI by its ID.
     *
     * This method ensures that the requesting user is either the sharer or recipient
     * of the shared POI record.
     *
     * @param currentUserId the UUID of the user requesting the shared POI details
     * @param sharedPoiId the unique identifier of the shared POI to retrieve
     * @return a SharedPoiResponse containing the shared POI details
     * @throws ResponseStatusException if the shared POI does not exist or the user has no access
     */
    @Transactional(readOnly = true)
    fun getSharedPoiById(currentUserId: UUID, sharedPoiId: UUID): SharedPoiResponse {
        val sharedPoi = sharedPoiRepository.findById(sharedPoiId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Shared POI not found") }

        if (sharedPoi.sharerId != currentUserId && sharedPoi.recipientId != currentUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this shared POI")
        }

        return SharedPoiMapper.toResponse(sharedPoi)
    }

    /**
     * Retrieves all POIs shared by a specific user.
     *
     * @param userId the UUID of the user whose shared POIs to retrieve
     * @return list of SharedPoiResponse objects representing POIs shared by the user
     */
    @Transactional(readOnly = true)
    fun getSharedByMe(userId: UUID): List<SharedPoiResponse> =
        sharedPoiRepository.findAllBySharerId(userId)
            .map { SharedPoiMapper.toResponse(it) }

    /**
     * Retrieves all POIs shared to a specific user.
     *
     * @param userId the UUID of the user who received shared POIs to retrieve
     * @return list of SharedPoiResponse objects representing POIs shared to the user
     */
    @Transactional(readOnly = true)
    fun getSharedToMe(userId: UUID): List<SharedPoiResponse> =
        sharedPoiRepository.findAllByRecipientId(userId)
            .map { SharedPoiMapper.toResponse(it) }

    /**
     * Retrieves all unviewed POIs shared to a specific user.
     *
     * @param userId the UUID of the user whose unviewed shared POIs to retrieve
     * @return list of SharedPoiResponse objects representing unviewed POIs shared to the user
     */
    @Transactional(readOnly = true)
    fun getUnviewedSharedToMe(userId: UUID): List<SharedPoiResponse> =
        sharedPoiRepository.findAllByRecipientIdAndViewedAtIsNull(userId)
            .map { SharedPoiMapper.toResponse(it) }

    /**
     * Marks a shared POI as viewed by the recipient.
     *
     * This method validates that the current user is the intended recipient
     * before updating the viewed timestamp.
     *
     * @param recipientId the UUID of the user marking the POI as viewed
     * @param sharedId the UUID of the shared POI record to mark as viewed
     * @return a SharedPoiResponse representing the updated shared POI record
     * @throws ResponseStatusException if the shared POI does not exist or user has no access
     */
    @Transactional
    fun markViewed(recipientId: UUID, sharedId: UUID): SharedPoiResponse {
        val shared = sharedPoiRepository.findById(sharedId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Shared POI not found") }

        if (shared.recipientId != recipientId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your shared POI")
        }

        shared.viewedAt = LocalDateTime.now()
        val updated = sharedPoiRepository.save(shared)

        return SharedPoiMapper.toResponse(updated)
    }

    /**
     * Deletes a shared POI record.
     *
     * This method allows the sharer to remove a POI they have shared with another user.
     * Only the user who originally shared the POI can delete it.
     *
     * @param sharerId the UUID of the user who shared the POI
     * @param sharedPoiId the UUID of the shared POI record to delete
     * @throws ResponseStatusException if the shared POI does not exist or the user is not the sharer
     */
    @Transactional
    fun deleteSharedPoi(sharerId: UUID, sharedPoiId: UUID) {
        val sharedPoi = sharedPoiRepository.findById(sharedPoiId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Shared POI not found") }

        if (sharedPoi.sharerId != sharerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete POIs that you have shared")
        }

        sharedPoiRepository.delete(sharedPoi)
    }
}
