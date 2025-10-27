package org.cityxplore.backend.user.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Response DTO for exposing User data in API responses.
 */
data class UserResponse(
    val id: UUID?,
    val email: String,
    val username: String,
    val avatarUrl: String?,
    val createdAt: LocalDateTime?,
    val lastActiveAt: LocalDateTime?,
    val totalDistance: BigDecimal,
    val totalPoisDiscovered: Int
)
