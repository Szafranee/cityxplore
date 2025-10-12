package org.cityxplore.backend.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank

/**
 * DTO for creating a new Point of Interest via API.
 */
data class PointOfInterestCreateRequest(
    @field:NotBlank
    val name: String,
    val description: String? = null,
    @field:NotBlank
    val category: String,
    @field:DecimalMin(value = "-90.0", inclusive = true, message = "latitude must be >= -90")
    @field:DecimalMax(value = "90.0", inclusive = true, message = "latitude must be <= 90")
    val latitude: Double? = null,
    @field:DecimalMin(value = "-180.0", inclusive = true, message = "longitude must be >= -180")
    @field:DecimalMax(value = "180.0", inclusive = true, message = "longitude must be <= 180")
    val longitude: Double? = null,
    val metadata: String? = null,
    val imageUrls: String? = null
) {
    @AssertTrue(message = "latitude and longitude must be provided together or both omitted")
    fun areCoordinatesPaired(): Boolean =
        (latitude == null && longitude == null) || (latitude != null && longitude != null)
}
