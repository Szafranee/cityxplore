package org.cityxplore.backend.dto

import java.util.UUID

data class AchievementDto(
    val id: UUID,
    val name: String,
    val description: String,
    val category: String?,
    val iconUrl: String?,
    val points: Int
)