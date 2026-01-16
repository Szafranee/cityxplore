package org.cityxplore.backend.user.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive

/**
 * Request DTO for adding travelled distance to user's total.
 *
 * @property distanceMeters The distance to add in meters. Must be positive and max 500m per request.
 */
data class AddDistanceRequest(
    @field:Positive(message = "Distance must be positive")
    @field:Max(value = 500, message = "Distance cannot exceed 500 meters per request")
    val distanceMeters: Double
)
