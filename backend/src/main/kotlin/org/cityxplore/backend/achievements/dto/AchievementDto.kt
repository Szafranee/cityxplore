package org.cityxplore.backend.achievements.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * DTO representing an achievement definition.
 *
 * Used both as input (admin create/update) and output (public listing).
 */
data class AchievementDto(
    val id: UUID,
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    @field:NotBlank
    @field:Size(max = 2000)
    val description: String,
    @field:Size(max = 100)
    val category: String?,
    @field:Size(max = 500)
    val iconUrl: String?,
    @field:PositiveOrZero
    val points: Int
)
