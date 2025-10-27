package org.cityxplore.backend.achievements.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

/**
 * Request DTO used to update an existing achievement definition via admin API.
 *
 * Same constraints as create; server selects the target by path variable id.
 */
data class UpdateAchievementRequest(
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
