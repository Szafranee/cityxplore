package org.cityxplore.backend.social.shared.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO representing custom Point of Interest data that can be shared between users.
 * This is used when sharing a POI that doesn't exist in the main points_of_interest table.
 *
 * @property name the name of the custom POI
 * @property description optional description of the custom POI
 * @property category the category of the POI (e.g., "restaurant", "viewpoint")
 * @property latitude the latitude coordinate
 * @property longitude the longitude coordinate
 * @property imageUrls optional list of image URLs associated with the POI
 */
data class CustomPoiData(
    @field:NotBlank(message = "POI name is required")
    @field:Size(max = 200, message = "POI name cannot exceed 200 characters")
    val name: String,

    @field:Size(max = 1000, message = "POI description cannot exceed 1000 characters")
    val description: String? = null,

    @field:NotBlank(message = "POI category is required")
    @field:Size(max = 50, message = "POI category cannot exceed 50 characters")
    val category: String,

    val latitude: Double,

    val longitude: Double,

    val imageUrls: List<String>? = null
)
