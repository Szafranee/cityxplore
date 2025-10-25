package org.cityxplore.backend.poi.dto

import java.time.LocalDateTime
import java.util.UUID

/**
 * Admin-facing POI DTO including audit metadata.
 */
data class PointOfInterestDto(
    val id: UUID?,
    val name: String,
    val description: String?,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val metadata: Map<String, Any>?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val isActive: Boolean
)
