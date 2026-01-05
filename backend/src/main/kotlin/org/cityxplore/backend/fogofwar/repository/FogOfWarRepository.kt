package org.cityxplore.backend.fogofwar.repository

import org.cityxplore.backend.fogofwar.entity.FogOfWarEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for fog of war data persistence.
 *
 * Provides CRUD operations for user fog of war progress.
 */
@Repository
interface FogOfWarRepository : JpaRepository<FogOfWarEntity, UUID> {
    /**
     * Find fog of war data by user ID.
     *
     * @param userId User's unique identifier
     * @return FogOfWarEntity if found, null otherwise
     */
    fun findByUserId(userId: UUID): FogOfWarEntity?

    /**
     * Delete fog of war data for a specific user.
     *
     * @param userId User's unique identifier
     */
    fun deleteByUserId(userId: UUID)
}
