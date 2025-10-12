package org.cityxplore.backend.repository

import org.cityxplore.backend.entity.UserPoiDiscovery
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserPoiDiscoveryRepository : JpaRepository<UserPoiDiscovery, UUID> {
    fun findAllByUserId(userId: UUID): List<UserPoiDiscovery>
    fun existsByUserIdAndPoiId(userId: UUID, poiId: UUID): Boolean
}