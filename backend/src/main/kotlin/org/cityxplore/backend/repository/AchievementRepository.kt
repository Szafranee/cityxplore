package org.cityxplore.backend.repository

import org.cityxplore.backend.entity.Achievement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AchievementRepository : JpaRepository<Achievement, UUID>
