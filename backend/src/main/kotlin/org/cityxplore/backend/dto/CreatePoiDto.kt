package org.cityxplore.backend.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO for creating or updating a Point of Interest via admin endpoints.
 */
data class CreatePoiDto(
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,

    @field:Size(max = 2000)
    val description: String?,

    @field:NotBlank
    @field:Size(max = 100)
    val category: String,

    @field:DecimalMin(value = "-90.0")
    @field:DecimalMax(value = "90.0")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0")
    @field:DecimalMax(value = "180.0")
    val longitude: Double,

    val metadata: Map<String, Any>?
)
