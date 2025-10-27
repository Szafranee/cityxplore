package org.cityxplore.backend.user.dto

/**
 * Lightweight response DTO for the "/api/me" endpoint.
 *
 * Only exposes safe, basic identity fields derived from JWT claims.
 */
data class MeResponse(
    val userId: String?,
    val email: String?,
    val role: String?
)
