package org.cityxplore.backend.discoveries.repository

import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserPoiDiscoveryRepository : JpaRepository<UserPoiDiscovery, UUID> {
    fun findAllByUserId(userId: UUID): List<UserPoiDiscovery>
    fun existsByUserIdAndPoiId(userId: UUID, poiId: UUID): Boolean
    fun findByUserIdAndPoiId(userId: UUID, poiId: UUID): UserPoiDiscovery?
    fun countByUserId(userId: UUID): Long
}
