package org.cityxplore.backend.social.friendship.service

import org.cityxplore.backend.social.friendship.dto.FriendshipRequest
import org.cityxplore.backend.social.friendship.dto.FriendshipResponse
import org.cityxplore.backend.social.friendship.entity.Friendship
import org.cityxplore.backend.social.friendship.entity.FriendshipStatus
import org.cityxplore.backend.social.friendship.mapper.FriendshipMapper
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service responsible for handling friendship-related operations.
 * Provides functionality for sending and responding to friendship invitations,
 * as well as retrieving friendship-related data.
 */
@Service
class FriendshipService(
    private val friendshipRepository: FriendshipRepository
) {

    /**
     * Sends a friendship invitation request from one user to another.
     *
     * This method checks for existing relationships or pending requests between the two users.
     * If a conflict is detected (e.g., already friends, a pending request exists, etc.),
     * it throws an appropriate exception.
     *
     * @param requesterId the UUID of the user sending the invite
     * @param friendshipRequest the friendship request information including the addressee ID
     * @return a FriendshipResponse object representing the created friendship request
     * @throws ResponseStatusException if the invite is invalid or conflicts with an existing state
     */
    @Transactional
    fun sendInvite(requesterId: UUID, friendshipRequest: FriendshipRequest): FriendshipResponse {
        if (requesterId == friendshipRequest.addresseeId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite yourself")
        }

        val addresseeId = friendshipRequest.addresseeId
        val existingFriendship = friendshipRepository
            .findInEitherDirection(requesterId, addresseeId)
        if (existingFriendship != null) {
            when (existingFriendship.status) {
                FriendshipStatus.PENDING -> throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A friendship request is already pending"
                )

                FriendshipStatus.ACCEPTED -> throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You are already friends with this user"
                )

                FriendshipStatus.DECLINED -> {
                    // Allow re-invite by updating the existing record
                    existingFriendship.status = FriendshipStatus.PENDING
                    existingFriendship.updatedAt = LocalDateTime.now()
                    val updated = friendshipRepository.save(existingFriendship)

                    return FriendshipMapper.toResponse(updated)
                }

                FriendshipStatus.BLOCKED -> throw ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot send invitation to this user"
                )
            }
        }

        val friendship = friendshipRepository.save(
            Friendship(
                requesterId = requesterId,
                addresseeId = addresseeId,
                status = FriendshipStatus.PENDING
            )
        )

        return FriendshipMapper.toResponse(friendship)
    }

    /**
     * Accepts a pending friendship invitation for the current user.
     *
     * @param currentUserId the ID of the user accepting the invitation
     * @param friendshipId the ID of the friendship invitation to accept
     * @return a `FriendshipResponse` representing the accepted friendship
     * @throws ResponseStatusException if the friendship invitation does not exist,
     *         if it does not belong to the current user, or if its status is not PENDING
     */
    @Transactional
    fun acceptInvite(currentUserId: UUID, friendshipId: UUID): FriendshipResponse {
        val friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found") }

        if (friendship.addresseeId != currentUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your invitation to accept")
        }
        if (friendship.status != FriendshipStatus.PENDING) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Invitation is not pending")
        }

        friendship.status = FriendshipStatus.ACCEPTED
        friendship.updatedAt = LocalDateTime.now()
        val saved = friendshipRepository.save(friendship)

        return FriendshipMapper.toResponse(saved)
    }

    /**
     * Declines a friendship invitation for the current user.
     *
     * @param currentUserId the ID of the user declining the invitation
     * @param friendshipId the ID of the friendship relation or invite to be declined
     * @return a response representation of the declined friendship invite
     * @throws ResponseStatusException if the friendship does not exist, is not pending,
     *                                 or the current user is not the addressee of the invite
     */
    @Transactional
    fun declineInvite(currentUserId: UUID, friendshipId: UUID): FriendshipResponse {
        val friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found") }

        if (friendship.addresseeId != currentUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your invitation to decline")
        }
        if (friendship.status != FriendshipStatus.PENDING) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Invitation is not pending")
        }

        friendship.status = FriendshipStatus.DECLINED
        friendship.updatedAt = LocalDateTime.now()
        val saved = friendshipRepository.save(friendship)

        return FriendshipMapper.toResponse(saved)
    }

    /**
     * Retrieves details of a specific friendship by its ID.
     *
     * This method ensures that the requesting user is either the requester or addressee
     * of the friendship relation.
     *
     * @param currentUserId the ID of the user requesting the friendship details
     * @param friendshipId the unique identifier of the friendship to retrieve
     * @return a FriendshipResponse containing the friendship details
     * @throws ResponseStatusException if the friendship does not exist or user has no access
     */
    @Transactional(readOnly = true)
    fun getFriendshipById(currentUserId: UUID, friendshipId: UUID): FriendshipResponse {
        val friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found") }

        if (friendship.requesterId != currentUserId && friendship.addresseeId != currentUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this friendship")
        }

        return FriendshipMapper.toResponse(friendship)
    }

    /**
     * Retrieves a list of accepted friendships for the user with the given ID.
     *
     * @param userId The unique identifier of the user whose friends are to be retrieved.
     * @return A list of friendship responses representing the accepted friendships of the user.
     */
    @Transactional(readOnly = true)
    fun getMyFriends(userId: UUID): List<FriendshipResponse> =
        friendshipRepository.findAllAcceptedByUserId(userId)
            .map { FriendshipMapper.toResponse(it) }

    /**
     * Retrieves a list of pending friendship invitations addressed to a specific user.
     *
     * @param userId The unique identifier of the user to whom the pending invitations are addressed.
     * @return A list of `FriendshipResponse` objects representing the pending invitations.
     */
    @Transactional(readOnly = true)
    fun getPendingInvites(userId: UUID): List<FriendshipResponse> =
        friendshipRepository
            .findAllPendingByAddresseeId(userId)
            .map { FriendshipMapper.toResponse(it) }
}
