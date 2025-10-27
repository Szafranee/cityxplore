package org.cityxplore.backend.poi.dto

import java.time.LocalDateTime
import java.util.UUID

/**
 * Response DTO for exposing Point of Interest data in API responses.
 */
data class PoiResponse(
    val id: UUID?,
    val name: String,
    val description: String?,
    val category: String,
    val latitude: Double?,
    val longitude: Double?,
    val metadata: Map<String, Any?>?,
    val imageUrls: List<String>?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val isActive: Boolean
)
