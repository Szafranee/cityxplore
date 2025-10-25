package org.cityxplore.backend.poi.repository

import org.cityxplore.backend.poi.entity.PointOfInterest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PointOfInterestRepository : JpaRepository<PointOfInterest, UUID> {
    @Query("SELECT COUNT(p) FROM PointOfInterest p WHERE p.isActive = true")
    fun countByIsActiveTrue(): Long
}
