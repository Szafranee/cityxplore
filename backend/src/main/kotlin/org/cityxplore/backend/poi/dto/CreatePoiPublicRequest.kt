package org.cityxplore.backend.poi.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.cityxplore.backend.poi.entity.PoiImage

/**
 * Request DTO for creating a new Point of Interest via public API.
 *
 * Coordinates must be provided together (both present) or omitted together.
 */
data class CreatePoiPublicRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val category: String,
    @field:DecimalMin(value = "-90.0", inclusive = true, message = "latitude must be >= -90")
    @field:DecimalMax(value = "90.0", inclusive = true, message = "latitude must be <= 90")
    val latitude: Double? = null,
    @field:DecimalMin(value = "-180.0", inclusive = true, message = "longitude must be >= -180")
    @field:DecimalMax(value = "180.0", inclusive = true, message = "longitude must be <= 180")
    val longitude: Double? = null,
    val metadata: Map<String, Any?>? = null,
    @field:Size(max = 5, message = "a maximum of 5 images are allowed")
    val imageUrls: List<PoiImage>? = null
) {
    @AssertTrue(message = "latitude and longitude must be provided together or both omitted")
    fun areCoordinatesPaired(): Boolean =
        (latitude == null && longitude == null) || (latitude != null && longitude != null)
}
