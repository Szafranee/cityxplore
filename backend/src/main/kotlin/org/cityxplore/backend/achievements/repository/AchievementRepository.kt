package org.cityxplore.backend.achievements.repository

import org.cityxplore.backend.achievements.entity.Achievement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AchievementRepository : JpaRepository<Achievement, UUID> {

    @Query("SELECT DISTINCT a.category FROM Achievement a WHERE a.category IS NOT NULL")
    fun findDistinctCategories(): List<String>

    @Query("SELECT a FROM Achievement a WHERE a.isActive = true")
    fun findAllByIsActiveTrue(): List<Achievement>
}
