package org.cityxplore.backend.social.shared.repository

import org.cityxplore.backend.social.shared.entity.SharedPoi
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Repository interface for accessing SharedPoi entities.
 * Provides methods for querying shared POIs by sharer and recipient.
 */
interface SharedPoiRepository : JpaRepository<SharedPoi, UUID> {

    /**
     * Finds all POIs shared by a specific user.
     *
     * @param sharerId the UUID of the user who shared the POIs
     * @return list of shared POI entities created by the specified user
     */
    fun findAllBySharerId(sharerId: UUID): List<SharedPoi>

    /**
     * Finds all POIs shared to a specific user.
     *
     * @param recipientId the UUID of the user who received the POIs
     * @return list of shared POI entities addressed to the specified user
     */
    fun findAllByRecipientId(recipientId: UUID): List<SharedPoi>

    /**
     * Finds all unviewed POIs shared to a specific user.
     *
     * @param recipientId the UUID of the user who received the POIs
     * @return list of shared POI entities that have not been viewed yet
     */
    fun findAllByRecipientIdAndViewedAtIsNull(recipientId: UUID): List<SharedPoi>

    /**
     * Counts the number of POIs shared from one user to another.
     *
     * @param sharerId the UUID of the user who shared the POIs
     * @param recipientId the UUID of the user who received the POIs
     * @return count of shared POIs between these two users
     */
    fun countBySharerIdAndRecipientId(sharerId: UUID, recipientId: UUID): Long
}
