package org.cityxplore.backend.social.friendship.repository

import org.cityxplore.backend.social.friendship.entity.Friendship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

/**
 * Repository interface for managing `Friendship` entities.
 * Provides methods for querying friendships by user IDs, obtaining friendship statuses,
 * and retrieving accepted or pending friendship relations.
 */
interface FriendshipRepository : JpaRepository<Friendship, UUID> {

    /**
     * Retrieves a list of accepted friendship relations for a specific user.
     *
     * @param userId the UUID of the user for whom accepted friendships are fetched.
     * @return a list of `Friendship` entities that match the provided user ID and have a status of `ACCEPTED`.
     */
    @Query("SELECT f FROM Friendship f WHERE (f.requesterId = :userId OR f.addresseeId = :userId) AND f.status = 'ACCEPTED'")
    fun findAllAcceptedByUserId(userId: UUID): List<Friendship>

    /**
     * Finds a Friendship entity between two users, identified by their respective IDs,
     * in either direction (requester and addressee roles can be reversed).
     *
     * @param userA the UUID of the first user
     * @param userB the UUID of the second user
     * @return the Friendship entity if a match is found, or null if no such Friendship exists
     */
    @Query(
        """
        SELECT f FROM Friendship f
        WHERE (f.requesterId = :userA AND f.addresseeId = :userB)
           OR (f.requesterId = :userB AND f.addresseeId = :userA)
        """
    )
    fun findInEitherDirection(userA: UUID, userB: UUID): Friendship?

    /**
     * Finds all pending friendship requests for a specific addressee.
     *
     * @param addresseeId The unique identifier of the user who is the addressee of the pending friendship requests.
     * @return A list of Friendship entities that represent the pending friendship requests for the given addressee.
     */
    @Query("SELECT f FROM Friendship f WHERE f.addresseeId = :addresseeId AND f.status = 'PENDING'")
    fun findAllPendingByAddresseeId(addresseeId: UUID): List<Friendship>

    /**
     * Checks if two users have an accepted friendship.
     *
     * @param userA the UUID of the first user
     * @param userB the UUID of the second user
     * @return true if an accepted friendship exists between the two users, false otherwise
     */
    @Query(
        """
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM Friendship f
        WHERE ((f.requesterId = :userA AND f.addresseeId = :userB)
           OR (f.requesterId = :userB AND f.addresseeId = :userA))
           AND f.status = 'ACCEPTED'
        """
    )
    fun areFriends(userA: UUID, userB: UUID): Boolean
}
