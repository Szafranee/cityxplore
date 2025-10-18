package org.cityxplore.backend.repository

import org.cityxplore.backend.entity.PointOfInterest
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PointOfInterestRepository : JpaRepository<PointOfInterest, UUID>
