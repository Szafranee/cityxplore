package org.cityxplore.backend.achievements.repository

import org.cityxplore.backend.achievements.entity.Achievement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AchievementRepository : JpaRepository<Achievement, UUID>
