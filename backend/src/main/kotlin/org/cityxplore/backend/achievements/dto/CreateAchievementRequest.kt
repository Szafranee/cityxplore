package org.cityxplore.backend.achievements.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

/**
 * Request DTO used to create a new achievement definition via admin API.
 *
 * Validation mirrors domain constraints and keeps controller logic minimal.
 */
data class CreateAchievementRequest(
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
